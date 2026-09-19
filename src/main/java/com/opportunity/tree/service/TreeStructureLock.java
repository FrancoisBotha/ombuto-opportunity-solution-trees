package com.opportunity.tree.service;

import com.opportunity.tree.domain.Team;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serialises structural writes (create, move, delete) within one team's tree.
 *
 * <p>Those writes read a parent's children and then renumber or append to them
 * ({@code sortOrder}). Run concurrently, two moves between the same parents update the same
 * sibling rows in different orders and deadlock, and two creates under one parent both compute
 * {@code max(sortOrder) + 1} from the same snapshot. Taking a pessimistic write lock on the owning
 * {@link Team} row first — a single lock per team, so there is no lock-ordering problem — makes
 * them queue instead. Every node belongs to exactly one team and a move never crosses teams, so
 * the team row covers both the old and the new parent.
 *
 * <p>Callers must take the lock after authorisation (which only reads scalar ids) and
 * <em>before</em> loading any node entity or sibling list, so that everything they read is
 * the state committed by the previous holder. The lock is released when the caller's
 * transaction ends.
 */
@Service
public class TreeStructureLock {

    private static final Logger LOG = LoggerFactory.getLogger(TreeStructureLock.class);

    @PersistenceContext
    private EntityManager em;

    /** Blocks until this transaction holds the structure lock of the given team. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void lockTeam(Long teamId) {
        LOG.debug("Locking tree structure of team {}", teamId);
        Team team = em.find(Team.class, teamId, LockModeType.PESSIMISTIC_WRITE);
        if (team == null) {
            throw new TeamAccessDeniedException();
        }
    }
}
