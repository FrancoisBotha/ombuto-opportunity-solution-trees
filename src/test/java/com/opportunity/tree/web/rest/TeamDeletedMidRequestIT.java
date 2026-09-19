package com.opportunity.tree.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.opportunity.tree.IntegrationTest;
import com.opportunity.tree.domain.Team;
import com.opportunity.tree.domain.User;
import com.opportunity.tree.domain.enumeration.TeamRole;
import com.opportunity.tree.security.AuthoritiesConstants;
import com.opportunity.tree.service.AdminTeamService;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * A team deleted while another request of one of its members is running must never fail that
 * request (C18). TeamAccessService used to load the caller's TeamMember entities and then each
 * Team in a separate statement; a team deleted in between was an ObjectNotFoundException → HTTP 500
 * on a request that had nothing to do with that team.
 *
 * <p>{@link StatementTrap} makes the race deterministic: the admin delete of team A (the real
 * AdminTeamService path, on another connection) commits right after the request's first statement
 * that reads {@code team_member}, i.e. between "load the memberships" and anything that follows.
 * The request is the member's tree of team B, which must still be a 200.
 */
@IntegrationTest
@ConcurrentConnections
@TestPropertySource(
    properties = "spring.jpa.properties.hibernate.session_factory.statement_inspector=com.opportunity.tree.web.rest.StatementTrap"
)
@AutoConfigureMockMvc
class TeamDeletedMidRequestIT {

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private PlatformTransactionManager txMgr;

    @Autowired
    private AdminTeamService adminTeamService;

    @Autowired
    private DataSource dataSource;

    private TransactionTemplate tt;
    private String login;
    private Long doomedTeamId;
    private Long keptTeamId;

    @BeforeEach
    void seed() throws Exception {
        ConcurrentConnections.Guard.assertRealConcurrency(dataSource);
        tt = new TransactionTemplate(txMgr);
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        login = "gone-team-" + suffix;
        tt.executeWithoutResult(status -> {
            OstTreeTestData data = new OstTreeTestData(em);
            User member = data.user(login);
            Team doomed = data.team("Doomed team " + suffix);
            Team kept = data.team("Kept team " + suffix);
            // Team A is deleted mid-request; team B is the one the request reads.
            data.member(doomed, member, TeamRole.VIEWER);
            data.member(kept, member, TeamRole.OWNER);
            data.product(kept, "Kept product " + suffix, 0);
            em.flush();
            doomedTeamId = doomed.getId();
            keptTeamId = kept.getId();
        });
    }

    @AfterEach
    void cleanup() {
        StatementTrap.disarm();
        tt.executeWithoutResult(status -> {
            List<Long> teams = List.of(doomedTeamId, keptTeamId);
            em.createQuery("delete from Product p where p.team.id in :ids").setParameter("ids", teams).executeUpdate();
            em.createQuery("delete from TeamMember tm where tm.team.id in :ids").setParameter("ids", teams).executeUpdate();
            em.createQuery("delete from Team t where t.id in :ids").setParameter("ids", teams).executeUpdate();
            em.createQuery("delete from User u where u.login = :l").setParameter("l", login).executeUpdate();
        });
    }

    @Test
    void aTeamDeletedBetweenLoadingTheMembershipsAndResolvingTheTeamsIsNot500() throws Exception {
        ExecutorService other = Executors.newSingleThreadExecutor();
        try {
            StatementTrap.arm("team_member", () -> deleteTeamAsAdmin(other, doomedTeamId));
            MockHttpServletResponse response = mvc
                .perform(get("/api/teams/{id}/tree", keptTeamId).with(user(login)))
                .andReturn()
                .getResponse();
            StatementTrap.disarm();

            assertThat(StatementTrap.fired()).as("the team was deleted in the middle of the request").isTrue();
            assertThat(teamExists(doomedTeamId)).as("team A is gone").isFalse();
            assertThat(response.getStatus()).as(response.getContentAsString()).isEqualTo(200);
            assertThat(response.getContentAsString()).contains("Kept product");
        } finally {
            other.shutdownNow();
        }
    }

    @Test
    void afterTheDeleteTheDeletedTeamIsForbiddenAndTheOtherStillReadable() throws Exception {
        ExecutorService other = Executors.newSingleThreadExecutor();
        try {
            deleteTeamAsAdmin(other, doomedTeamId);
        } finally {
            other.shutdownNow();
        }
        assertThat(
            mvc.perform(get("/api/teams/{id}/tree", doomedTeamId).with(user(login))).andReturn().getResponse().getStatus()
        ).isEqualTo(403);
        assertThat(mvc.perform(get("/api/teams/{id}/tree", keptTeamId).with(user(login))).andReturn().getResponse().getStatus()).isEqualTo(
            200
        );
    }

    /** The admin endpoint's service path, committed on another thread (its own connection and transaction). */
    private void deleteTeamAsAdmin(ExecutorService other, Long teamId) {
        try {
            other
                .submit(() -> {
                    SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                            "admin",
                            "n/a",
                            List.of(new SimpleGrantedAuthority(AuthoritiesConstants.ADMIN))
                        )
                    );
                    try {
                        adminTeamService.deleteTeam(teamId);
                    } finally {
                        SecurityContextHolder.clearContext();
                    }
                })
                .get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new IllegalStateException("admin delete of team " + teamId + " failed", e);
        }
    }

    private boolean teamExists(Long id) {
        return Boolean.TRUE.equals(tt.execute(status -> em.find(Team.class, id) != null));
    }
}
