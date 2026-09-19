package com.opportunity.tree.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opportunity.tree.IntegrationTest;
import com.opportunity.tree.domain.Opportunity;
import com.opportunity.tree.domain.Outcome;
import com.opportunity.tree.domain.Product;
import com.opportunity.tree.domain.Solution;
import com.opportunity.tree.domain.Team;
import com.opportunity.tree.domain.User;
import com.opportunity.tree.domain.enumeration.TeamRole;
import com.opportunity.tree.domain.enumeration.TreeNodeType;
import com.opportunity.tree.service.TreeNodeCascadeService;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Writes on a node racing the cascade delete of that node (step 5c eval item 14): a chat post
 * inserted after the cascade collected the node's comments used to break the parent delete with a
 * foreign-key violation — HTTP 500 carrying the SQL. Every such write now queues on the team's
 * structure lock and re-checks the node: the delete succeeds, the late write is a clean 409.
 *
 * <p>{@link StatementTrap} makes the race deterministic: the chat post is fired right after the
 * delete request has collected the opportunity's comments. {@link ConcurrentConnections} gives the
 * context a real connection pool (shares its Spring context with {@link TeamDeletedMidRequestIT}).
 */
@IntegrationTest
@ConcurrentConnections
@TestPropertySource(
    properties = "spring.jpa.properties.hibernate.session_factory.statement_inspector=com.opportunity.tree.web.rest.StatementTrap"
)
@AutoConfigureMockMvc
class TreeDeleteRaceIT {

    private static final int ROUNDS = 6;

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
    private String ownerLogin;
    private String editorLogin;
    private Long teamId;
    private Long outcomeId;

    @BeforeEach
    void seed() throws Exception {
        ConcurrentConnections.Guard.assertRealConcurrency(dataSource);
        tt = new TransactionTemplate(txMgr);
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        ownerLogin = "race-owner-" + suffix;
        editorLogin = "race-editor-" + suffix;
        tt.executeWithoutResult(status -> {
            OstTreeTestData data = new OstTreeTestData(em);
            Team team = data.team("Delete race team " + suffix);
            User owner = data.user(ownerLogin);
            User editor = data.user(editorLogin);
            data.member(team, owner, TeamRole.OWNER);
            data.member(team, editor, TeamRole.EDITOR);
            Product product = data.product(team, "race-prod-" + suffix, 0);
            Outcome outcome = data.outcome(product, "Race outcome", 0);
            em.flush();
            teamId = team.getId();
            outcomeId = outcome.getId();
        });
    }

    @AfterEach
    void cleanup() {
        StatementTrap.disarm();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(ownerLogin, "n/a", List.of()));
        try {
            tt.executeWithoutResult(status -> {
                for (Long id : em
                    .createQuery("select p.id from Product p where p.team.id = :id", Long.class)
                    .setParameter("id", teamId)
                    .getResultList()) {
                    cascadeService.deleteNode(TreeNodeType.PRODUCT, id);
                }
                List<String> logins = List.of(ownerLogin, editorLogin);
                em.createQuery("delete from NodeHistory h where h.author.login in :l").setParameter("l", logins).executeUpdate();
                em.createQuery("delete from Comment c where c.author.login in :l").setParameter("l", logins).executeUpdate();
                em.createQuery("delete from TeamMember tm where tm.team.id = :id").setParameter("id", teamId).executeUpdate();
                em.createQuery("delete from Team t where t.id = :id").setParameter("id", teamId).executeUpdate();
                em.createQuery("delete from User u where u.login in :l").setParameter("l", logins).executeUpdate();
            });
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void aChatPostArrivingWhileTheNodeIsBeingDeletedIsA409AndTheDeleteSucceeds() throws Exception {
        Long[] ids = opportunityWithSolution("Deterministic");
        Long opportunityId = ids[0];
        ExecutorService other = Executors.newSingleThreadExecutor();
        List<Future<MockHttpServletResponse>> late = new ArrayList<>();
        try {
            // Right after the delete collected the opportunity's comments, the editor posts one.
            StatementTrap.arm("from comment where opportunity_id", () -> {
                Future<MockHttpServletResponse> post = other.submit(() ->
                    mvc
                        .perform(
                            json(post("/api/tree/nodes/opportunity/{id}/comments", opportunityId), Map.of("body", "Too late"), editorLogin)
                        )
                        .andReturn()
                        .getResponse()
                );
                late.add(post);
                try {
                    // The unfixed post commits at once; the fixed one queues on the team lock (held here).
                    post.get(1500, TimeUnit.MILLISECONDS);
                } catch (TimeoutException queued) {
                    // expected with the lock: it completes after this delete commits
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            });
            MockHttpServletResponse deleted = mvc
                .perform(delete("/api/tree/nodes/opportunity/{id}", opportunityId).with(csrf()).with(user(ownerLogin)))
                .andReturn()
                .getResponse();
            StatementTrap.disarm();
            assertThat(StatementTrap.fired()).as("the post was fired in the middle of the delete").isTrue();

            MockHttpServletResponse posted = late.get(0).get(30, TimeUnit.SECONDS);
            assertNoSql(deleted);
            assertNoSql(posted);
            assertThat(deleted.getStatus()).as("delete: " + deleted.getContentAsString()).isEqualTo(204);
            assertThat(posted.getStatus()).as("late post: " + posted.getContentAsString()).isEqualTo(409);
            assertThat(posted.getContentAsString()).contains("error.concurrencyFailure");
            assertThat(countOpportunities(opportunityId)).isZero();
        } finally {
            other.shutdownNow();
        }
    }

    @Test
    void concurrentWritesOnANodeAndItsChildWhileItIsDeletedNeverFailWith500() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(6);
        try {
            for (int round = 0; round < ROUNDS; round++) {
                Long[] ids = opportunityWithSolution("Round " + round);
                Long opp = ids[0];
                Long sol = ids[1];
                List<MockHttpServletRequestBuilder> requests = List.of(
                    delete("/api/tree/nodes/opportunity/{id}", opp).with(csrf()).with(user(ownerLogin)),
                    json(post("/api/tree/nodes/opportunity/{id}/comments", opp), Map.of("body", "On the opportunity"), editorLogin),
                    json(post("/api/tree/nodes/solution/{id}/comments", sol), Map.of("body", "On the solution"), editorLogin),
                    json(patch("/api/tree/nodes/opportunity/{id}", opp), Map.of("status", "EXPLORING", "priority", 80), editorLogin),
                    json(patch("/api/tree/nodes/solution/{id}", sol), Map.of("status", "BUILDING"), ownerLogin),
                    json(post("/api/tree/nodes/opportunity/{id}/comments", opp), Map.of("body", "Second post"), ownerLogin)
                );
                List<MockHttpServletResponse> responses = runTogether(pool, requests);
                for (MockHttpServletResponse response : responses) {
                    assertNoSql(response);
                    assertThat(response.getStatus())
                        .as("round " + round + ": " + response.getContentAsString())
                        .isIn(200, 201, 204, 403, 409);
                }
                assertThat(responses.get(0).getStatus()).as("round " + round + ": the delete").isEqualTo(204);
                assertThat(countOpportunities(opp)).isZero();
            }
        } finally {
            pool.shutdownNow();
        }
    }

    /** A fresh opportunity (with a solution) under the outcome: [opportunityId, solutionId]. */
    private Long[] opportunityWithSolution(String title) {
        return tt.execute(status -> {
            OstTreeTestData data = new OstTreeTestData(em);
            Outcome outcome = em.find(Outcome.class, outcomeId);
            Opportunity opportunity = data.opportunity(outcome, null, title, 0);
            Solution solution = data.solution(opportunity, title + " solution", 0);
            em.flush();
            return new Long[] { opportunity.getId(), solution.getId() };
        });
    }

    private long countOpportunities(Long id) {
        Long count = tt.execute(status ->
            em.createQuery("select count(o) from Opportunity o where o.id = :id", Long.class).setParameter("id", id).getSingleResult()
        );
        return count == null ? 0 : count;
    }

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder builder, Object body, String login) throws Exception {
        return builder.with(csrf()).with(user(login)).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(body));
    }

    private static void assertNoSql(MockHttpServletResponse response) throws Exception {
        assertThat(response.getContentAsString().toLowerCase())
            .as("no SQL in any response")
            .doesNotContain("delete from", "insert into", "constraint", "fk_", "sql");
    }

    /** Fires all requests at once (released by a latch) and returns the responses in order. */
    private List<MockHttpServletResponse> runTogether(ExecutorService pool, List<MockHttpServletRequestBuilder> requests) throws Exception {
        CountDownLatch ready = new CountDownLatch(requests.size());
        CountDownLatch go = new CountDownLatch(1);
        List<Future<MockHttpServletResponse>> futures = new ArrayList<>();
        for (MockHttpServletRequestBuilder request : requests) {
            futures.add(
                pool.submit(() -> {
                    ready.countDown();
                    go.await(10, TimeUnit.SECONDS);
                    return mvc.perform(request).andReturn().getResponse();
                })
            );
        }
        ready.await(10, TimeUnit.SECONDS);
        go.countDown();
        List<MockHttpServletResponse> responses = new ArrayList<>();
        for (Future<MockHttpServletResponse> f : futures) {
            responses.add(f.get(60, TimeUnit.SECONDS));
        }
        return responses;
    }
}
