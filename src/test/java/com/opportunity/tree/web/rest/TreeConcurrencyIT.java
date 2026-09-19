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
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Tree writes with real, committed transactions (no test-managed transaction):
 * <ul>
 *   <li>two moves between the same two parents, run concurrently, never deadlock into a 500 and
 *   leave dense sort orders (they queue on the team's structure lock);</li>
 *   <li>concurrent creates under one parent (a double-click) get distinct sort orders — likewise
 *   concurrent product creates in one team, link adds on one node and open-question adds on one
 *   opportunity;</li>
 *   <li>two products changing team in opposite directions at once never deadlock (both teams are
 *   locked lower id first);</li>
 *   <li>the generated admin DELETE of a node that still has children is a clean 409 without SQL.</li>
 * </ul>
 *
 * <p>{@link ConcurrentConnections} gives the context a real connection pool: under {@code testprod}
 * the default pool of one connection would serialise the requests and these tests would pass even
 * without the structure lock.
 */
@IntegrationTest
@ConcurrentConnections
@AutoConfigureMockMvc
class TreeConcurrencyIT {

    private static final int ROUNDS = 4;
    private static final int PARALLEL_CREATES = 6;

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
    private Long otherTeamId;
    private Long movingProductId;
    private Long otherMovingProductId;
    private Long productId;
    private Long outcome1Id;
    private Long outcome2Id;
    private Long oppBId;
    private Long oppCId;

    @BeforeEach
    void seed() throws Exception {
        ConcurrentConnections.Guard.assertRealConcurrency(dataSource);
        tt = new TransactionTemplate(txMgr);
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        ownerLogin = "conc-owner-" + suffix;
        editorLogin = "conc-editor-" + suffix;
        tt.executeWithoutResult(status -> {
            OstTreeTestData data = new OstTreeTestData(em);
            Team team = data.team("Concurrency Team " + suffix);
            User owner = data.user(ownerLogin);
            User editor = data.user(editorLogin);
            data.member(team, owner, TeamRole.OWNER);
            data.member(team, editor, TeamRole.EDITOR);
            Product product = data.product(team, "conc-prod-" + suffix, 0);
            Product moving = data.product(team, "conc-moving-" + suffix, 1);
            Team otherTeam = data.team("Concurrency Team B " + suffix);
            data.member(otherTeam, owner, TeamRole.EDITOR);
            data.member(otherTeam, editor, TeamRole.OWNER);
            Product otherMoving = data.product(otherTeam, "conc-other-moving-" + suffix, 0);
            Outcome o1 = data.outcome(product, "O1", 0);
            Outcome o2 = data.outcome(product, "O2", 1);
            Opportunity b = data.opportunity(o1, null, "Opp B", 0);
            Opportunity c = data.opportunity(o1, null, "Opp C", 1);
            data.opportunity(o1, null, "Opp A", 2);
            data.opportunity(o2, null, "Opp D", 0);
            em.flush();
            teamId = team.getId();
            otherTeamId = otherTeam.getId();
            movingProductId = moving.getId();
            otherMovingProductId = otherMoving.getId();
            productId = product.getId();
            outcome1Id = o1.getId();
            outcome2Id = o2.getId();
            oppBId = b.getId();
            oppCId = c.getId();
        });
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(ownerLogin, "n/a", List.of()));
        try {
            tt.executeWithoutResult(status -> {
                // The seeded products plus any created through the API by a test.
                List<Long> teamIds = List.of(teamId, otherTeamId);
                for (Long id : em
                    .createQuery("select p.id from Product p where p.team.id in :ids", Long.class)
                    .setParameter("ids", teamIds)
                    .getResultList()) {
                    cascadeService.deleteNode(TreeNodeType.PRODUCT, id);
                }
                em.createQuery("delete from TeamMember tm where tm.team.id in :ids").setParameter("ids", teamIds).executeUpdate();
                em.createQuery("delete from Team t where t.id in :ids").setParameter("ids", teamIds).executeUpdate();
                em
                    .createQuery("delete from User u where u.login in :l")
                    .setParameter("l", List.of(ownerLogin, editorLogin))
                    .executeUpdate();
            });
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void concurrentMovesBetweenTheSameParentsNeverFailWith500AndStayDense() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            for (int round = 0; round < ROUNDS; round++) {
                // Even rounds move B and C from O1 to O2, odd rounds move them back.
                Long target = round % 2 == 0 ? outcome2Id : outcome1Id;
                List<Integer> statuses = runTogether(
                    pool,
                    List.of(moveRequest(oppBId, target, ownerLogin), moveRequest(oppCId, target, editorLogin))
                );
                assertThat(statuses)
                    .as("round " + round)
                    .allSatisfy(s -> assertThat(s).isIn(200, 409));
                assertThat(statuses).as("round " + round + ": the structure lock makes both succeed").containsOnly(200);
                assertDense(outcome1Id);
                assertDense(outcome2Id);
            }
        } finally {
            pool.shutdownNow();
        }
        // After an even number of rounds both are back under O1.
        assertThat(topLevelTitles(outcome1Id)).containsExactlyInAnyOrder("Opp A", "Opp B", "Opp C");
        assertThat(topLevelTitles(outcome2Id)).containsExactly("Opp D");
    }

    @Test
    void concurrentCreatesUnderOneParentGetDistinctSortOrders() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(PARALLEL_CREATES);
        List<MockHttpServletRequestBuilder> requests = new ArrayList<>();
        for (int i = 0; i < PARALLEL_CREATES; i++) {
            requests.add(
                json(
                    post("/api/tree/nodes"),
                    Map.of("type", "solution", "parentType", "opportunity", "parentId", oppBId, "title", "Double click " + i),
                    i % 2 == 0 ? ownerLogin : editorLogin
                )
            );
        }
        try {
            List<Integer> statuses = runTogether(pool, requests);
            assertThat(statuses).containsOnly(201);
        } finally {
            pool.shutdownNow();
        }
        List<Integer> sortOrders = tt.execute(status ->
            em
                .createQuery("select s.sortOrder from Solution s where s.opportunity.id = :id order by s.sortOrder", Integer.class)
                .setParameter("id", oppBId)
                .getResultList()
        );
        assertThat(sortOrders).containsExactlyElementsOf(IntStream.range(0, PARALLEL_CREATES).boxed().toList());
    }

    @Test
    void concurrentProductCreatesInOneTeamGetDistinctSortOrders() throws Exception {
        Map<String, Object> team = Map.of("id", teamId, "name", "Concurrency Team", "createdDate", "2026-01-01T00:00:00Z");
        List<MockHttpServletRequestBuilder> requests = new ArrayList<>();
        for (int i = 0; i < PARALLEL_CREATES; i++) {
            requests.add(
                json(
                    post("/api/products"),
                    Map.of("name", "Parallel product " + i, "archived", false, "team", team),
                    i % 2 == 0 ? ownerLogin : editorLogin
                )
            );
        }
        ExecutorService pool = Executors.newFixedThreadPool(PARALLEL_CREATES);
        try {
            assertThat(runTogether(pool, requests)).containsOnly(201);
        } finally {
            pool.shutdownNow();
        }
        List<Integer> sortOrders = tt.execute(status ->
            em
                .createQuery("select p.sortOrder from Product p where p.team.id = :id order by p.sortOrder", Integer.class)
                .setParameter("id", teamId)
                .getResultList()
        );
        // The two seeded products have sortOrder 0 and 1; the new ones append after them.
        assertThat(sortOrders).containsExactlyElementsOf(IntStream.rangeClosed(0, PARALLEL_CREATES + 1).boxed().toList());
    }

    @Test
    void concurrentLinkAddsOnOneNodeGetDistinctSortOrders() throws Exception {
        List<MockHttpServletRequestBuilder> requests = new ArrayList<>();
        for (int i = 0; i < PARALLEL_CREATES; i++) {
            requests.add(
                json(
                    post("/api/tree/nodes/opportunity/{id}/links", oppBId),
                    Map.of("name", "Link " + i, "url", "https://example.com/" + i),
                    i % 2 == 0 ? ownerLogin : editorLogin
                )
            );
        }
        ExecutorService pool = Executors.newFixedThreadPool(PARALLEL_CREATES);
        try {
            assertThat(runTogether(pool, requests)).containsOnly(201);
        } finally {
            pool.shutdownNow();
        }
        List<Integer> sortOrders = tt.execute(status ->
            em
                .createQuery("select l.sortOrder from NodeLink l where l.opportunity.id = :id order by l.sortOrder", Integer.class)
                .setParameter("id", oppBId)
                .getResultList()
        );
        assertThat(sortOrders).containsExactlyElementsOf(IntStream.range(0, PARALLEL_CREATES).boxed().toList());
    }

    @Test
    void concurrentOpenQuestionAddsOnOneOpportunityGetDistinctSortOrders() throws Exception {
        List<MockHttpServletRequestBuilder> requests = new ArrayList<>();
        for (int i = 0; i < PARALLEL_CREATES; i++) {
            requests.add(
                json(
                    post("/api/tree/opportunities/{id}/questions", oppBId),
                    Map.of("text", "Question " + i),
                    i % 2 == 0 ? ownerLogin : editorLogin
                )
            );
        }
        ExecutorService pool = Executors.newFixedThreadPool(PARALLEL_CREATES);
        try {
            assertThat(runTogether(pool, requests)).containsOnly(201);
        } finally {
            pool.shutdownNow();
        }
        List<Integer> sortOrders = tt.execute(status ->
            em
                .createQuery("select q.sortOrder from OpenQuestion q where q.opportunity.id = :id order by q.sortOrder", Integer.class)
                .setParameter("id", oppBId)
                .getResultList()
        );
        assertThat(sortOrders).containsExactlyElementsOf(IntStream.range(0, PARALLEL_CREATES).boxed().toList());
    }

    @Test
    void oppositeConcurrentProductTeamChangesNeverDeadlock() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            for (int round = 0; round < ROUNDS; round++) {
                // Even rounds swap the two products' teams, odd rounds swap them back.
                boolean out = round % 2 == 0;
                List<Integer> statuses = runTogether(
                    pool,
                    List.of(
                        teamChange(movingProductId, out ? otherTeamId : teamId, ownerLogin),
                        teamChange(otherMovingProductId, out ? teamId : otherTeamId, editorLogin)
                    )
                );
                assertThat(statuses).as("round " + round + ": both teams are locked in id order, so both succeed").containsOnly(200);
                assertThat(teamOf(movingProductId)).isEqualTo(out ? otherTeamId : teamId);
                assertThat(teamOf(otherMovingProductId)).isEqualTo(out ? teamId : otherTeamId);
                assertDistinctProductSortOrders(teamId);
                assertDistinctProductSortOrders(otherTeamId);
            }
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void adminDeleteOfANodeWithChildrenIsAConflictWithoutSql() throws Exception {
        // B gets a child solution through the API; the generated admin endpoint cannot delete B then.
        mvc
            .perform(json(post("/api/tree/nodes"), Map.of("type", "solution", "parentType", "opportunity", "parentId", oppBId), ownerLogin))
            .andReturn();
        MockHttpServletResponse response = mvc
            .perform(delete("/api/opportunities/{id}", oppBId).with(user(ownerLogin).roles("ADMIN", "USER")).with(csrf()))
            .andReturn()
            .getResponse();

        assertThat(response.getStatus()).isEqualTo(409);
        String body = response.getContentAsString();
        assertThat(body).contains("error.dataintegrity");
        assertThat(body.toLowerCase()).doesNotContain("delete from", "violates", "constraint", "sql", "opportunity where");
        Long stillThere = tt.execute(status ->
            em.createQuery("select count(o) from Opportunity o where o.id = :id", Long.class).setParameter("id", oppBId).getSingleResult()
        );
        assertThat(stillThere).isEqualTo(1);
    }

    // ---------------------------------------------------------------------

    private MockHttpServletRequestBuilder moveRequest(Long nodeId, Long outcomeId, String login) throws Exception {
        return json(
            post("/api/tree/nodes/move"),
            Map.of("nodeType", "opportunity", "nodeId", nodeId, "parentType", "outcome", "parentId", outcomeId, "position", 0),
            login
        );
    }

    private MockHttpServletRequestBuilder teamChange(Long productId, Long toTeamId, String login) {
        return patch("/api/products/{id}", productId)
            .with(csrf())
            .with(user(login))
            .contentType("application/merge-patch+json")
            .content("{\"id\": " + productId + ", \"team\": {\"id\": " + toTeamId + "}}");
    }

    private Long teamOf(Long productId) {
        return tt.execute(status ->
            em.createQuery("select p.team.id from Product p where p.id = :id", Long.class).setParameter("id", productId).getSingleResult()
        );
    }

    private void assertDistinctProductSortOrders(Long team) {
        List<Integer> sortOrders = tt.execute(status ->
            em
                .createQuery("select p.sortOrder from Product p where p.team.id = :id", Integer.class)
                .setParameter("id", team)
                .getResultList()
        );
        assertThat(sortOrders).as("product sort orders in team " + team).doesNotHaveDuplicates();
    }

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder builder, Object body, String login) throws Exception {
        return builder.with(csrf()).with(user(login)).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(body));
    }

    /** Fires all requests at once (released by a latch) and returns their HTTP statuses. */
    private List<Integer> runTogether(ExecutorService pool, List<MockHttpServletRequestBuilder> requests) throws Exception {
        CountDownLatch ready = new CountDownLatch(requests.size());
        CountDownLatch go = new CountDownLatch(1);
        List<Future<MockHttpServletResponse>> futures = new ArrayList<>();
        for (MockHttpServletRequestBuilder request : requests) {
            Callable<MockHttpServletResponse> call = () -> {
                ready.countDown();
                go.await(10, TimeUnit.SECONDS);
                return mvc.perform(request).andReturn().getResponse();
            };
            futures.add(pool.submit(call));
        }
        ready.await(10, TimeUnit.SECONDS);
        go.countDown();
        List<Integer> statuses = new ArrayList<>();
        for (Future<MockHttpServletResponse> f : futures) {
            MockHttpServletResponse response = f.get(60, TimeUnit.SECONDS);
            assertThat(response.getContentAsString().toLowerCase())
                .as("no SQL in any response")
                .doesNotContain("deadlock", "update opportunity");
            statuses.add(response.getStatus());
        }
        return statuses;
    }

    private void assertDense(Long outcomeId) {
        List<Integer> sortOrders = tt.execute(status ->
            em
                .createQuery(
                    "select o.sortOrder from Opportunity o where o.outcome.id = :id and o.parent is null order by o.sortOrder",
                    Integer.class
                )
                .setParameter("id", outcomeId)
                .getResultList()
        );
        assertThat(sortOrders)
            .as("sort orders under outcome " + outcomeId)
            .containsExactlyElementsOf(IntStream.range(0, sortOrders.size()).boxed().toList());
    }

    private List<String> topLevelTitles(Long outcomeId) {
        return tt.execute(status ->
            em
                .createQuery("select o.title from Opportunity o where o.outcome.id = :id and o.parent is null", String.class)
                .setParameter("id", outcomeId)
                .getResultList()
        );
    }
}
