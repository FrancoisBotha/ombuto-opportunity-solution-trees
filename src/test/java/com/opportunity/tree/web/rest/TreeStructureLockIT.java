package com.opportunity.tree.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opportunity.tree.IntegrationTest;
import com.opportunity.tree.domain.Opportunity;
import com.opportunity.tree.domain.Outcome;
import com.opportunity.tree.domain.Product;
import com.opportunity.tree.domain.Team;
import com.opportunity.tree.domain.User;
import com.opportunity.tree.domain.enumeration.TeamRole;
import com.opportunity.tree.domain.enumeration.TreeNodeType;
import com.opportunity.tree.service.TreeNodeCascadeService;
import com.opportunity.tree.service.TreeStructureLock;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * What a structural write sees after waiting on the team's {@link TreeStructureLock}.
 *
 * <p>Each test holds the team lock in a transaction of its own ("the previous holder"), deletes a
 * node in it, then fires a request that passes its access check (the delete is not committed yet,
 * so the node is still visible) and queues on the lock. Once that request is seen waiting, the
 * holder commits; the request must then notice the node is gone and answer 409
 * {@code error.concurrencyFailure} — not a 500 from a null parent, and not a row with a null
 * parent. The last test holds the lock past {@link TreeStructureLock#LOCK_TIMEOUT_MS}.
 */
@IntegrationTest
@ConcurrentConnections
@AutoConfigureMockMvc
class TreeStructureLockIT {

    /** Longest the holder waits to see the request queue on the lock before failing the test. */
    private static final long QUEUE_DETECT_MS = 4000;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper om;

    @Autowired
    private PlatformTransactionManager txMgr;

    @Autowired
    private TreeNodeCascadeService cascadeService;

    @Autowired
    private DataSource dataSource;

    private TransactionTemplate tt;
    private JdbcTemplate jdbc;
    private boolean postgres;
    private String editorLogin;
    private Long teamId;
    private Long emptyProductId;
    private Long outcome1Id;
    private Long emptyOutcomeId;
    private Long oppAId;
    private Long nestedOppId;

    @BeforeEach
    void seed() throws Exception {
        ConcurrentConnections.Guard.assertRealConcurrency(dataSource);
        try (Connection c = dataSource.getConnection()) {
            postgres = c.getMetaData().getDatabaseProductName().toLowerCase().contains("postgres");
        }
        tt = new TransactionTemplate(txMgr);
        jdbc = new JdbcTemplate(dataSource);
        editorLogin = "lock-editor-" + UUID.randomUUID().toString().substring(0, 8);
        tt.executeWithoutResult(status -> {
            OstTreeTestData data = new OstTreeTestData(em);
            Team team = data.team("Lock Team " + editorLogin);
            User editor = data.user(editorLogin);
            data.member(team, editor, TeamRole.EDITOR);
            Product product = data.product(team, "lock-prod", 0);
            Product emptyProduct = data.product(team, "lock-empty-prod", 1);
            Outcome o1 = data.outcome(product, "O1", 0);
            Outcome emptyOutcome = data.outcome(product, "O-empty", 1);
            Opportunity a = data.opportunity(o1, null, "Opp A", 0);
            Opportunity b = data.opportunity(o1, null, "Opp B", 1);
            Opportunity nested = data.opportunity(o1, b, "Nested", 0);
            em.flush();
            teamId = team.getId();
            emptyProductId = emptyProduct.getId();
            outcome1Id = o1.getId();
            emptyOutcomeId = emptyOutcome.getId();
            oppAId = a.getId();
            nestedOppId = nested.getId();
        });
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(editorLogin, "n/a", List.of()));
        try {
            tt.executeWithoutResult(status -> {
                for (Long id : em
                    .createQuery("select p.id from Product p where p.team.id = :id", Long.class)
                    .setParameter("id", teamId)
                    .getResultList()) {
                    cascadeService.deleteNode(TreeNodeType.PRODUCT, id);
                }
                em.createQuery("delete from TeamMember tm where tm.team.id = :id").setParameter("id", teamId).executeUpdate();
                em.createQuery("delete from Team t where t.id = :id").setParameter("id", teamId).executeUpdate();
                em.createQuery("delete from User u where u.login = :l").setParameter("l", editorLogin).executeUpdate();
            });
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    // ---------------------------------------------------------------------
    // Node or parent deleted while the request waited on the lock
    // ---------------------------------------------------------------------

    @Test
    void createUnderANestedOpportunityDeletedMeanwhileIsAConflict() throws Exception {
        MockHttpServletResponse response = whileHolderDeletes(
            "delete from Opportunity o where o.id = " + nestedOppId,
            json(
                post("/api/tree/nodes"),
                Map.of("type", "opportunity", "parentType", "opportunity", "parentId", nestedOppId, "title", "Late")
            )
        );
        assertConcurrencyConflict(response);
        assertThat(count("select count(o) from Opportunity o where o.title = 'Late' and o.outcome.id = " + outcome1Id)).isZero();
    }

    @Test
    void createUnderAProductDeletedMeanwhileIsAConflict() throws Exception {
        MockHttpServletResponse response = whileHolderDeletes(
            "delete from Product p where p.id = " + emptyProductId,
            json(post("/api/tree/nodes"), Map.of("type", "outcome", "parentType", "product", "parentId", emptyProductId, "title", "Late"))
        );
        assertConcurrencyConflict(response);
        assertThat(count("select count(o) from Outcome o where o.title = 'Late'")).isZero();
    }

    @Test
    void createUnderAnOutcomeDeletedMeanwhileIsAConflict() throws Exception {
        MockHttpServletResponse response = whileHolderDeletes(
            "delete from Outcome o where o.id = " + emptyOutcomeId,
            json(
                post("/api/tree/nodes"),
                Map.of("type", "opportunity", "parentType", "outcome", "parentId", emptyOutcomeId, "title", "Late")
            )
        );
        assertConcurrencyConflict(response);
        assertThat(count("select count(o) from Opportunity o where o.title = 'Late'")).isZero();
    }

    @Test
    void moveOfANodeDeletedMeanwhileIsAConflict() throws Exception {
        MockHttpServletResponse response = whileHolderDeletes(
            "delete from Opportunity o where o.id = " + oppAId,
            json(
                post("/api/tree/nodes/move"),
                Map.of("nodeType", "opportunity", "nodeId", oppAId, "parentType", "outcome", "parentId", emptyOutcomeId, "position", 0)
            )
        );
        assertConcurrencyConflict(response);
        assertThat(count("select count(o) from Opportunity o where o.outcome.id = " + emptyOutcomeId)).isZero();
    }

    @Test
    void moveUnderAParentDeletedMeanwhileIsAConflict() throws Exception {
        MockHttpServletResponse response = whileHolderDeletes(
            "delete from Outcome o where o.id = " + emptyOutcomeId,
            json(
                post("/api/tree/nodes/move"),
                Map.of("nodeType", "opportunity", "nodeId", oppAId, "parentType", "outcome", "parentId", emptyOutcomeId, "position", 0)
            )
        );
        assertConcurrencyConflict(response);
        assertThat(count("select count(o) from Opportunity o where o.id = " + oppAId + " and o.outcome.id = " + outcome1Id)).isEqualTo(1);
    }

    @Test
    void linkAddOnANodeDeletedMeanwhileIsAConflict() throws Exception {
        MockHttpServletResponse response = whileHolderDeletes(
            "delete from Opportunity o where o.id = " + oppAId,
            json(post("/api/tree/nodes/opportunity/{id}/links", oppAId), Map.of("name", "Late", "url", "https://example.com"))
        );
        assertConcurrencyConflict(response);
        assertThat(count("select count(l) from NodeLink l where l.name = 'Late'")).isZero();
    }

    @Test
    void deleteOfANodeDeletedMeanwhileIsAConflict() throws Exception {
        MockHttpServletResponse response = whileHolderDeletes(
            "delete from Opportunity o where o.id = " + oppAId,
            delete("/api/tree/nodes/opportunity/{id}", oppAId).with(csrf()).with(user(editorLogin))
        );
        assertConcurrencyConflict(response);
    }

    // ---------------------------------------------------------------------
    // Lock timeout
    // ---------------------------------------------------------------------

    /**
     * A request that cannot get the lock within {@link TreeStructureLock#LOCK_TIMEOUT_MS} is a 409
     * without SQL. Both databases wait the full timeout: H2 through the {@code FOR UPDATE} wait,
     * PostgreSQL through {@code set local lock_timeout} (whose SQLState 55P03 Hibernate does not
     * translate, so TreeStructureLock does).
     */
    @Test
    void aRequestThatCannotGetTheLockInTimeIsAConflict() throws Exception {
        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(1);
        try {
            Future<?> holder = pool.submit(() ->
                tt.executeWithoutResult(status -> {
                    em.find(Team.class, teamId, LockModeType.PESSIMISTIC_WRITE);
                    locked.countDown();
                    await(release, 30_000);
                })
            );
            assertThat(locked.await(10, TimeUnit.SECONDS)).isTrue();
            long start = System.nanoTime();
            MockHttpServletResponse response;
            try {
                response = mvc
                    .perform(
                        json(
                            post("/api/tree/nodes/move"),
                            Map.of("nodeType", "opportunity", "nodeId", oppAId, "parentType", "outcome", "parentId", emptyOutcomeId)
                        )
                    )
                    .andReturn()
                    .getResponse();
            } finally {
                release.countDown();
            }
            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            holder.get(30, TimeUnit.SECONDS);

            assertConcurrencyConflict(response);
            assertThat(elapsedMs).as("gave up within a bounded time").isLessThan(TreeStructureLock.LOCK_TIMEOUT_MS + 5_000L);
            assertThat(elapsedMs).as("waited for the lock timeout").isGreaterThanOrEqualTo(TreeStructureLock.LOCK_TIMEOUT_MS - 500L);
            assertThat(count("select count(o) from Opportunity o where o.id = " + oppAId + " and o.outcome.id = " + outcome1Id)).isEqualTo(
                1
            );
        } finally {
            release.countDown();
            pool.shutdownNow();
        }
    }

    // ---------------------------------------------------------------------

    /**
     * Holds the team's structure lock in another transaction, runs {@code deleteJpql} there, fires
     * {@code request} (whose access check still sees the uncommitted-deleted row), waits until the
     * request is blocked on the lock, then commits and returns the request's response.
     */
    private MockHttpServletResponse whileHolderDeletes(String deleteJpql, MockHttpServletRequestBuilder request) throws Exception {
        CountDownLatch deleted = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<?> holder = pool.submit(() ->
                tt.executeWithoutResult(status -> {
                    em.find(Team.class, teamId, LockModeType.PESSIMISTIC_WRITE);
                    em.createQuery(deleteJpql).executeUpdate();
                    em.flush();
                    deleted.countDown();
                    await(release, 30_000);
                })
            );
            assertThat(deleted.await(10, TimeUnit.SECONDS)).as("holder took the lock").isTrue();
            int waitingBefore = sessionsWaitingForALock();
            Future<MockHttpServletResponse> call = pool.submit(() -> mvc.perform(request).andReturn().getResponse());
            try {
                long deadline = System.currentTimeMillis() + QUEUE_DETECT_MS;
                while (sessionsWaitingForALock() <= waitingBefore) {
                    assertThat(call.isDone()).as("the request must queue on the lock, not finish first").isFalse();
                    assertThat(System.currentTimeMillis()).as("request never queued on the lock").isLessThan(deadline);
                    Thread.sleep(20);
                }
            } finally {
                release.countDown();
            }
            holder.get(30, TimeUnit.SECONDS);
            return call.get(30, TimeUnit.SECONDS);
        } finally {
            release.countDown();
            pool.shutdownNow();
        }
    }

    /** Sessions of this database currently blocked on a lock held by another session. */
    private int sessionsWaitingForALock() {
        String sql = postgres
            ? "select count(*) from pg_stat_activity where datname = current_database() and wait_event_type = 'Lock'"
            : "select count(*) from information_schema.sessions where blocker_id is not null";
        Integer n = tt.execute(status -> jdbc.queryForObject(sql, Integer.class));
        return n == null ? 0 : n;
    }

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder builder, Object body) throws Exception {
        return builder.with(csrf()).with(user(editorLogin)).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(body));
    }

    private static void assertConcurrencyConflict(MockHttpServletResponse response) throws Exception {
        String body = response.getContentAsString();
        assertThat(response.getStatus()).as(body).isEqualTo(409);
        assertThat(body).contains("error.concurrencyFailure");
        assertThat(body.toLowerCase()).doesNotContain("select ", "delete from", "nullpointer", "sql");
    }

    private long count(String jpql) {
        Long n = tt.execute(status -> em.createQuery(jpql, Long.class).getSingleResult());
        return n == null ? 0 : n;
    }

    private static void await(CountDownLatch latch, long ms) {
        try {
            latch.await(ms, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
