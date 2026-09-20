package com.opportunity.tree.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opportunity.tree.IntegrationTest;
import com.opportunity.tree.domain.Team;
import com.opportunity.tree.domain.User;
import com.opportunity.tree.domain.enumeration.TeamRole;
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
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * TEAMS-002 / ADHOC-001 — the last-owner rule is a read-then-write: {@code requireNotLastOwner}
 * counts the team's owners and only then demotes or removes one. Two concurrent demotions (or
 * removals) of a two-owner team both counted two owners and both succeeded, leaving the team with
 * <b>no owner at all</b> — nobody could manage its membership again.
 *
 * <p>Both services now take {@link com.opportunity.tree.service.TreeStructureLock#lockTeam} before
 * the count, so the second request reads the state the first one committed and is refused. Removing
 * either {@code structureLock.lockTeam(teamId)} call makes these tests fail with zero owners left.
 *
 * <p>{@link ConcurrentConnections} gives the context a real connection pool — with the default
 * single connection of {@code testprod} the requests would be serialised and the test would pass
 * even without the lock.
 */
@IntegrationTest
@ConcurrentConnections
@AutoConfigureMockMvc
class TeamMembershipConcurrencyIT {

    private static final String TEAM_API = "/api/team-management";
    private static final String ADMIN_API = "/api/admin/teams";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper om;

    @Autowired
    private EntityManager em;

    @Autowired
    private PlatformTransactionManager txMgr;

    @Autowired
    private DataSource dataSource;

    private TransactionTemplate tt;
    private String ownerALogin;
    private String ownerBLogin;
    private String ownerAId;
    private String ownerBId;
    private Long teamId;

    @BeforeEach
    void seed() throws Exception {
        ConcurrentConnections.Guard.assertRealConcurrency(dataSource);
        tt = new TransactionTemplate(txMgr);
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        ownerALogin = "lastowner-a-" + suffix;
        ownerBLogin = "lastowner-b-" + suffix;
        tt.executeWithoutResult(status -> {
            OstTreeTestData data = new OstTreeTestData(em);
            Team team = data.team("Last owner race " + suffix);
            User a = data.user(ownerALogin);
            User b = data.user(ownerBLogin);
            data.member(team, a, TeamRole.OWNER);
            data.member(team, b, TeamRole.OWNER);
            em.flush();
            teamId = team.getId();
            ownerAId = a.getId();
            ownerBId = b.getId();
        });
    }

    @AfterEach
    void cleanUp() {
        OstTreeTestCleanup.removeTeamsAndUsers(txMgr, em, List.of(teamId), List.of(ownerALogin, ownerBLogin));
    }

    @Test
    void twoConcurrentOwnerDemotions_leaveTheTeamWithAnOwner() throws Exception {
        List<Integer> statuses = runTogether(List.of(demoteAsOwner(ownerAId, ownerBLogin), demoteAsOwner(ownerBId, ownerALogin)));

        assertThat(ownerCount()).as("a team must never be left without an owner").isGreaterThanOrEqualTo(1);
        assertThat(statuses).as("one demotion wins, the other is refused").containsExactlyInAnyOrder(200, 400);
    }

    @Test
    void twoConcurrentOwnerRemovals_leaveTheTeamWithAnOwner() throws Exception {
        List<Integer> statuses = runTogether(List.of(removeAsOwner(ownerAId, ownerBLogin), removeAsOwner(ownerBId, ownerALogin)));

        assertThat(ownerCount()).isGreaterThanOrEqualTo(1);
        assertThat(statuses).containsExactlyInAnyOrder(204, 400);
    }

    @Test
    void twoConcurrentAdminDemotions_leaveTheTeamWithAnOwner() throws Exception {
        List<Integer> statuses = runTogether(List.of(demoteAsAdmin(ownerAId), demoteAsAdmin(ownerBId)));

        assertThat(ownerCount()).isGreaterThanOrEqualTo(1);
        assertThat(statuses).containsExactlyInAnyOrder(200, 400);
    }

    @Test
    void twoConcurrentAdminRemovals_leaveTheTeamWithAnOwner() throws Exception {
        List<Integer> statuses = runTogether(List.of(removeAsAdmin(ownerAId), removeAsAdmin(ownerBId)));

        assertThat(ownerCount()).isGreaterThanOrEqualTo(1);
        assertThat(statuses).containsExactlyInAnyOrder(204, 400);
    }

    /**
     * The two races above each stay inside one service. The invariant has to survive the mixed
     * cases too: a demotion and a removal at the same time, and the owner-facing API racing the
     * admin API. Both services take the same per-team row lock, so whichever commits first is the
     * state the other reads — but only a test that crosses the two paths shows that the lock is one
     * lock rather than two independent ones.
     */
    @Test
    void demotionRacingRemoval_leavesTheTeamWithAnOwner() throws Exception {
        assertAtMostOneSucceeds(runTogether(List.of(demoteAsOwner(ownerBId, ownerALogin), removeAsOwner(ownerAId, ownerBLogin))));
    }

    @Test
    void ownerDemotionRacingAdminRemoval_leavesTheTeamWithAnOwner() throws Exception {
        assertAtMostOneSucceeds(runTogether(List.of(demoteAsOwner(ownerBId, ownerALogin), removeAsAdmin(ownerAId))));
    }

    @Test
    void adminDemotionRacingOwnerRemoval_leavesTheTeamWithAnOwner() throws Exception {
        assertAtMostOneSucceeds(runTogether(List.of(demoteAsAdmin(ownerBId), removeAsOwner(ownerAId, ownerBLogin))));
    }

    /**
     * The loser of a mixed race is refused either as the last owner (400) or because the winner
     * took its own ownership away first (403) — which of the two depends on whether its
     * authorisation read happened before or after the winner committed. Both are correct; what must
     * never happen is that both writes land.
     */
    private void assertAtMostOneSucceeds(List<Integer> statuses) {
        assertThat(ownerCount()).as("a team must never be left without an owner").isGreaterThanOrEqualTo(1);
        assertThat(
            statuses
                .stream()
                .filter(s -> s >= 200 && s < 300)
                .count()
        )
            .as("at most one of the two membership writes may succeed, got %s", statuses)
            .isLessThanOrEqualTo(1L);
        assertThat(statuses)
            .as("the loser must be refused, not served an error page")
            .allMatch(s -> s < 500);
    }

    // Helpers ---------------------------------------------------------------

    private MockHttpServletRequestBuilder demoteAsOwner(String targetUserId, String actingLogin) throws Exception {
        return put(TEAM_API + "/teams/{id}/members/{userId}", teamId, targetUserId)
            .with(csrf())
            .with(user(actingLogin))
            .contentType(MediaType.APPLICATION_JSON)
            .content(om.writeValueAsBytes(Map.of("role", TeamRole.EDITOR.name())));
    }

    private MockHttpServletRequestBuilder removeAsOwner(String targetUserId, String actingLogin) {
        return delete(TEAM_API + "/teams/{id}/members/{userId}", teamId, targetUserId).with(csrf()).with(user(actingLogin));
    }

    private MockHttpServletRequestBuilder demoteAsAdmin(String targetUserId) throws Exception {
        return put(ADMIN_API + "/{id}/members/{userId}", teamId, targetUserId)
            .with(csrf())
            .with(user("membership-admin").authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
            .contentType(MediaType.APPLICATION_JSON)
            .content(om.writeValueAsBytes(Map.of("role", TeamRole.EDITOR.name())));
    }

    private MockHttpServletRequestBuilder removeAsAdmin(String targetUserId) {
        return delete(ADMIN_API + "/{id}/members/{userId}", teamId, targetUserId)
            .with(csrf())
            .with(user("membership-admin").authorities(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    private long ownerCount() {
        Long count = tt.execute(status ->
            em
                .createQuery("select count(m) from TeamMember m where m.team.id = :id and m.role = :role", Long.class)
                .setParameter("id", teamId)
                .setParameter("role", TeamRole.OWNER)
                .getSingleResult()
        );
        return count == null ? 0L : count;
    }

    /** Fires both requests at once (released by a latch) and returns their HTTP statuses. */
    private List<Integer> runTogether(List<MockHttpServletRequestBuilder> requests) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(requests.size());
        try {
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
                assertThat(response.getContentAsString().toLowerCase()).as("no SQL in any response").doesNotContain("select ", "update ");
                statuses.add(response.getStatus());
            }
            return statuses;
        } finally {
            pool.shutdownNow();
        }
    }
}
