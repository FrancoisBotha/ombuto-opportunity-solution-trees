package com.opportunity.tree.service;

import com.opportunity.tree.domain.Team;
import com.opportunity.tree.domain.enumeration.TreeNodeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Timeout;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Set;
import org.hibernate.JDBCException;
import org.hibernate.dialect.lock.spi.ConnectionLockTimeoutStrategy;
import org.hibernate.dialect.lock.spi.LockTimeoutType;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serialises writes within one team's tree that read-then-write sibling lists or can race a cascade
 * delete: node create, move, product reorder and cascade delete, product create (and a product
 * changing team), appending a link or an open question, field patches (which write history), and
 * chat posts / edits / deletes (a comment inserted after the cascade collected the node's comments
 * would break that delete with a foreign-key violation).
 *
 * <p>Those writes read a parent's children and then renumber or append to them
 * ({@code sortOrder}). Run concurrently, two moves between the same parents update the same
 * sibling rows in different orders and deadlock, and two creates under one parent both compute
 * {@code max(sortOrder) + 1} from the same snapshot. Taking a pessimistic write lock on the owning
 * {@link Team} row first — a single lock per team, so there is no lock-ordering problem — makes
 * them queue instead. Every node belongs to exactly one team and a move never crosses teams, so
 * the team row covers both the old and the new parent. The only write that spans two teams (a
 * product changing team) takes both locks through {@link #lockTeams}, lower id first.
 *
 * <p>Callers must take the lock after authorisation (which only reads scalar ids) and
 * <em>before</em> loading any node entity or sibling list, so that everything they read is
 * the state committed by the previous holder. Because authorisation ran before the wait, a
 * caller must then re-check with {@link #requireNode} that the nodes it is about to touch were
 * not deleted by the previous holder. The lock is released when the caller's transaction ends.
 *
 * <p>The wait is bounded by {@link #LOCK_TIMEOUT_MS}. The timeout is passed to Hibernate as a JPA
 * {@link Timeout} find option (the typed form of the {@code jakarta.persistence.lock.timeout}
 * hint), which H2 applies to the statement itself ({@code FOR UPDATE WAIT 5}). Hibernate does not
 * apply it to {@code em.find} on PostgreSQL, whose lock timeout is a connection setting; there the
 * dialect's own connection strategy sets it ({@code set local lock_timeout}) around the find and
 * restores it afterwards. A timeout is rethrown as Spring's {@link CannotAcquireLockException},
 * which the REST layer maps to 409 {@code error.concurrencyFailure}.
 */
@Service
public class TreeStructureLock {

    private static final Logger LOG = LoggerFactory.getLogger(TreeStructureLock.class);

    /** Longest a structural write waits for another one in the same team before giving up (409). */
    public static final int LOCK_TIMEOUT_MS = 5000;

    /**
     * SQLStates of a lock wait that ran out of time: PostgreSQL {@code 55P03} (lock_not_available)
     * and H2 {@code HYT00} (error 50200, "Timeout trying to lock table").
     */
    static final Set<String> LOCK_TIMEOUT_STATES = Set.of("55P03", "HYT00");

    @PersistenceContext
    private EntityManager em;

    private final TeamAccessService teamAccessService;

    public TreeStructureLock(TeamAccessService teamAccessService) {
        this.teamAccessService = teamAccessService;
    }

    /**
     * Blocks (at most {@link #LOCK_TIMEOUT_MS}) until this transaction holds the structure lock of
     * the given team. A team that no longer exists is a {@link ConcurrencyFailureException} (409):
     * the caller was authorised on it a moment ago, so it was deleted concurrently.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void lockTeam(Long teamId) {
        LOG.debug("Locking tree structure of team {}", teamId);
        Timeout timeout = Timeout.milliseconds(LOCK_TIMEOUT_MS);
        SessionImplementor session = em.unwrap(SessionImplementor.class);
        SessionFactoryImplementor factory = session.getFactory();
        LockingSupport locking = factory.getJdbcServices().getDialect().getLockingSupport();
        Team team;
        if (locking.getMetadata().getLockTimeoutType(timeout) == LockTimeoutType.CONNECTION) {
            ConnectionLockTimeoutStrategy strategy = locking.getConnectionLockTimeoutStrategy();
            Timeout previous = session.doReturningWork(connection -> {
                Timeout before = strategy.getLockTimeout(connection, factory);
                strategy.setLockTimeout(timeout, connection, factory);
                return before;
            });
            // No finally: after a timeout the transaction is aborted (PostgreSQL rejects any further
            // statement) and rolls back, which undoes the transaction-local setting anyway.
            team = findLocked(teamId, timeout);
            session.doWork(connection -> strategy.setLockTimeout(previous, connection, factory));
        } else {
            team = findLocked(teamId, timeout);
        }
        if (team == null) {
            throw new ConcurrencyFailureException("Team " + teamId + " was deleted by a concurrent request");
        }
    }

    private Team findLocked(Long teamId, Timeout timeout) {
        try {
            return em.find(Team.class, teamId, LockModeType.PESSIMISTIC_WRITE, timeout);
        } catch (RuntimeException e) {
            // Hibernate reports a lock timeout (PostgreSQL 55P03, H2 HYT00) on this find as a
            // PessimisticEntityLockException wrapping a plain JDBCException, and this is a service,
            // not a repository, so Spring does not translate it: without this it would be a 500
            // carrying the SQL text.
            if (isLockTimeout(e)) {
                throw new CannotAcquireLockException("Timed out waiting for the tree structure lock of team " + teamId, e);
            }
            throw e;
        }
    }

    static boolean isLockTimeout(Throwable error) {
        for (Throwable t = error; t != null; t = t.getCause() == t ? null : t.getCause()) {
            String state = t instanceof JDBCException j ? j.getSQLState() : t instanceof SQLException sql ? sql.getSQLState() : null;
            if (state != null && LOCK_TIMEOUT_STATES.contains(state)) {
                return true;
            }
        }
        return false;
    }

    /** Locks two teams (equal or not) in a fixed order — lower id first — so two such writes cannot deadlock. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void lockTeams(Long teamId, Long otherTeamId) {
        if (Objects.equals(teamId, otherTeamId)) {
            lockTeam(teamId);
            return;
        }
        lockTeam(Math.min(teamId, otherTeamId));
        lockTeam(Math.max(teamId, otherTeamId));
    }

    /**
     * Re-checks, under the team lock, that a node the caller was authorised on before waiting
     * still exists in that team. A node deleted meanwhile is a {@link ConcurrencyFailureException}
     * (409 {@code error.concurrencyFailure}) rather than a 403: the caller has just been shown to be
     * an editor of the team, so telling them the node is gone leaks nothing (NFR-002 still holds for
     * ids the caller could never access, which fail the access check with 403 first).
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void requireNode(TreeNodeType type, Long id, Long teamId) {
        // A query (not em.find) so the check reads the database, not this persistence context.
        if (!teamAccessService.teamIdForNode(type, id).map(teamId::equals).orElse(false)) {
            throw new ConcurrencyFailureException(type + " " + id + " was deleted by a concurrent request");
        }
    }
}
