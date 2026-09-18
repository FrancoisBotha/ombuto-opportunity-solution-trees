package com.opportunity.tree.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opportunity.tree.IntegrationTest;
import com.opportunity.tree.domain.Product;
import com.opportunity.tree.domain.Team;
import com.opportunity.tree.domain.TeamMember;
import com.opportunity.tree.domain.User;
import com.opportunity.tree.domain.enumeration.TeamRole;
import com.opportunity.tree.repository.ProductRepository;
import com.opportunity.tree.repository.TeamMemberRepository;
import com.opportunity.tree.repository.TeamRepository;
import com.opportunity.tree.repository.UserRepository;
import com.opportunity.tree.security.AuthoritiesConstants;
import com.opportunity.tree.service.dto.AddTeamMemberRequest;
import com.opportunity.tree.service.dto.AdminCreateTeamRequest;
import com.opportunity.tree.service.dto.ChangeTeamMemberRoleRequest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for {@link AdminTeamResource} (ADHOC-001 AC 9).
 */
@IntegrationTest
@AutoConfigureMockMvc
@Transactional
class AdminTeamResourceIT {

    private static final String API = "/api/admin/teams";
    private static final String ADMIN_LOGIN = "adhoc001-admin";
    private static final String USER_LOGIN = "adhoc001-user";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TeamMemberRepository teamMemberRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    private User owner;
    private User other;
    private User newOwner;

    @BeforeEach
    void seedUsers() {
        owner = persistUser("adhoc001-owner");
        other = persistUser("adhoc001-other");
        newOwner = persistUser("adhoc001-new-owner");
    }

    @AfterEach
    void cleanup() {
        productRepository.deleteAll();
        teamMemberRepository.deleteAll();
        teamRepository.deleteAll();
        userRepository.deleteAll();
    }

    // AC 1 — non-admin authenticated user gets 403 on every endpoint
    @Test
    void nonAdminForbiddenOnEveryEndpoint() throws Exception {
        Team team = seedTeamWith(owner, TeamRole.OWNER);

        mockMvc.perform(get(API).with(user(USER_LOGIN))).andExpect(status().isForbidden());
        mockMvc.perform(get(API + "/" + team.getId() + "/members").with(user(USER_LOGIN))).andExpect(status().isForbidden());

        AdminCreateTeamRequest create = new AdminCreateTeamRequest();
        create.setName("Nope");
        create.setDescription("d");
        create.setOwnerUserId(owner.getId());
        mockMvc
            .perform(
                post(API).with(csrf()).with(user(USER_LOGIN)).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(create))
            )
            .andExpect(status().isForbidden());

        AddTeamMemberRequest add = new AddTeamMemberRequest();
        add.setUserId(other.getId());
        add.setRole(TeamRole.EDITOR);
        mockMvc
            .perform(
                post(API + "/" + team.getId() + "/members")
                    .with(csrf())
                    .with(user(USER_LOGIN))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(add))
            )
            .andExpect(status().isForbidden());

        ChangeTeamMemberRoleRequest chg = new ChangeTeamMemberRoleRequest();
        chg.setRole(TeamRole.VIEWER);
        mockMvc
            .perform(
                put(API + "/" + team.getId() + "/members/" + owner.getId())
                    .with(csrf())
                    .with(user(USER_LOGIN))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(chg))
            )
            .andExpect(status().isForbidden());

        mockMvc
            .perform(delete(API + "/" + team.getId() + "/members/" + owner.getId()).with(csrf()).with(user(USER_LOGIN)))
            .andExpect(status().isForbidden());

        mockMvc.perform(delete(API + "/" + team.getId()).with(csrf()).with(user(USER_LOGIN))).andExpect(status().isForbidden());
    }

    // AC 1 — anonymous gets 401 on list endpoint
    @Test
    void anonymousUnauthorized() throws Exception {
        mockMvc.perform(get(API)).andExpect(status().isUnauthorized());
    }

    // AC 2 — admin lists ALL teams even when they are not a member of any of them
    @Test
    void adminListsAllTeamsIncludingForeign() throws Exception {
        Team a = seedTeamWith(owner, TeamRole.OWNER);
        Team b = seedTeamWith(other, TeamRole.OWNER);

        mockMvc
            .perform(
                get(API + "?sort=name,asc").with(
                    user(ADMIN_LOGIN).authorities(
                        new org.springframework.security.core.authority.SimpleGrantedAuthority(AuthoritiesConstants.ADMIN)
                    )
                )
            )
            .andExpect(status().isOk())
            .andExpect(
                jsonPath(
                    "$[?(@.id == " + a.getId() + ")].ownerLogins",
                    org.hamcrest.Matchers.hasItem(org.hamcrest.Matchers.hasItem(owner.getLogin()))
                )
            )
            .andExpect(
                jsonPath(
                    "$[?(@.id == " + b.getId() + ")].ownerLogins",
                    org.hamcrest.Matchers.hasItem(org.hamcrest.Matchers.hasItem(other.getLogin()))
                )
            )
            .andExpect(jsonPath("$[?(@.id == " + a.getId() + ")].memberCount", org.hamcrest.Matchers.hasItem(1)))
            .andExpect(jsonPath("$[?(@.id == " + a.getId() + ")].productCount", org.hamcrest.Matchers.hasItem(0)));
    }

    // AC 3 — admin who is not a member reads members of a foreign team
    @Test
    void adminListsMembersOfForeignTeam() throws Exception {
        Team team = seedTeamWith(owner, TeamRole.OWNER);
        seedMembership(team, other, TeamRole.EDITOR);

        mockMvc
            .perform(get(API + "/" + team.getId() + "/members").with(admin()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(2)));
    }

    // AC 4 — create-on-behalf makes the named user (not the admin) the owner
    @Test
    void createOnBehalf_makesNamedUserOwner() throws Exception {
        AdminCreateTeamRequest req = new AdminCreateTeamRequest();
        req.setName("OnBehalf");
        req.setDescription("d");
        req.setOwnerUserId(newOwner.getId());

        mockMvc
            .perform(post(API).with(csrf()).with(admin()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("OnBehalf"))
            .andExpect(jsonPath("$.ownerLogins[0]").value(newOwner.getLogin()));

        List<Team> teams = teamRepository.findAll();
        assertThat(teams).hasSize(1);
        List<TeamMember> members = teamMemberRepository.findAllByTeamId(teams.get(0).getId());
        assertThat(members)
            .singleElement()
            .satisfies(m -> {
                assertThat(m.getUser().getLogin()).isEqualTo(newOwner.getLogin());
                assertThat(m.getRole()).isEqualTo(TeamRole.OWNER);
                assertThat(m.getJoinedDate()).isNotNull();
            });
        assertThat(teams.get(0).getCreatedDate()).isNotNull();
    }

    // AC 4 — unknown ownerUserId → 4xx with usernotfound key (not 500)
    @Test
    void createOnBehalf_unknownOwner_rejectedWith4xx() throws Exception {
        AdminCreateTeamRequest req = new AdminCreateTeamRequest();
        req.setName("Ghost");
        req.setDescription("d");
        req.setOwnerUserId("no-such-user-id");

        mockMvc
            .perform(post(API).with(csrf()).with(admin()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(req)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.usernotfound"));
    }

    // AC 5 — admin adds / re-roles / removes a member of a team they don't belong to
    @Test
    void adminManagesMembersOfForeignTeam() throws Exception {
        Team team = seedTeamWith(owner, TeamRole.OWNER);

        AddTeamMemberRequest add = new AddTeamMemberRequest();
        add.setUserId(other.getId());
        add.setRole(TeamRole.EDITOR);
        mockMvc
            .perform(
                post(API + "/" + team.getId() + "/members")
                    .with(csrf())
                    .with(admin())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(add))
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.role").value("EDITOR"));

        ChangeTeamMemberRoleRequest chg = new ChangeTeamMemberRoleRequest();
        chg.setRole(TeamRole.VIEWER);
        mockMvc
            .perform(
                put(API + "/" + team.getId() + "/members/" + other.getId())
                    .with(csrf())
                    .with(admin())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(chg))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.role").value("VIEWER"));

        mockMvc
            .perform(delete(API + "/" + team.getId() + "/members/" + other.getId()).with(csrf()).with(admin()))
            .andExpect(status().isNoContent());

        assertThat(teamMemberRepository.existsByTeamIdAndUserId(team.getId(), other.getId())).isFalse();
    }

    // AC 6 — last-owner protection still applies to admins; existing member rejected
    @Test
    void lastOwnerAndMemberExistsProtectionsApplyToAdmins() throws Exception {
        Team team = seedTeamWith(owner, TeamRole.OWNER);

        mockMvc
            .perform(delete(API + "/" + team.getId() + "/members/" + owner.getId()).with(csrf()).with(admin()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.lastowner"));

        ChangeTeamMemberRoleRequest demote = new ChangeTeamMemberRoleRequest();
        demote.setRole(TeamRole.EDITOR);
        mockMvc
            .perform(
                put(API + "/" + team.getId() + "/members/" + owner.getId())
                    .with(csrf())
                    .with(admin())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(demote))
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.lastowner"));

        // memberexists: adding the current owner again
        AddTeamMemberRequest addAgain = new AddTeamMemberRequest();
        addAgain.setUserId(owner.getId());
        addAgain.setRole(TeamRole.VIEWER);
        mockMvc
            .perform(
                post(API + "/" + team.getId() + "/members")
                    .with(csrf())
                    .with(admin())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(addAgain))
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.memberexists"));
    }

    // AC 7 — delete empty team succeeds and removes memberships
    @Test
    void deleteEmptyTeamSucceeds() throws Exception {
        Team team = seedTeamWith(owner, TeamRole.OWNER);

        mockMvc.perform(delete(API + "/" + team.getId()).with(csrf()).with(admin())).andExpect(status().isNoContent());

        assertThat(teamRepository.findById(team.getId())).isEmpty();
        assertThat(teamMemberRepository.findAllByTeamId(team.getId())).isEmpty();
    }

    // AC 7 — delete team with products rejected; nothing is deleted
    @Test
    void deleteTeamWithProductsRejected() throws Exception {
        Team team = seedTeamWith(owner, TeamRole.OWNER);
        Product product = new Product();
        product.setName("P1");
        product.setArchived(false);
        product.setCreatedDate(Instant.now());
        product.setTeam(team);
        productRepository.saveAndFlush(product);

        mockMvc
            .perform(delete(API + "/" + team.getId()).with(csrf()).with(admin()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.teamhasproducts"));

        assertThat(teamRepository.findById(team.getId())).isPresent();
        assertThat(teamMemberRepository.findAllByTeamId(team.getId())).isNotEmpty();
        assertThat(productRepository.countByTeamId(team.getId())).isEqualTo(1);
    }

    // AC 8 — the membership-scoped /api/team-management endpoints still give 403 to a non-member admin
    @Test
    void teamManagementEndpointsUnchangedForNonMemberAdmin() throws Exception {
        Team team = seedTeamWith(owner, TeamRole.OWNER);

        mockMvc.perform(get("/api/team-management/teams/" + team.getId()).with(admin())).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/team-management/teams/" + team.getId() + "/members").with(admin())).andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------------

    private static org.springframework.test.web.servlet.request.RequestPostProcessor admin() {
        return user(ADMIN_LOGIN).authorities(
            new org.springframework.security.core.authority.SimpleGrantedAuthority(AuthoritiesConstants.ADMIN)
        );
    }

    private User persistUser(String loginPrefix) {
        User u = new User();
        u.setId(UUID.randomUUID().toString());
        u.setLogin(loginPrefix + "-" + UUID.randomUUID().toString().substring(0, 6));
        u.setActivated(true);
        u.setFirstName("First");
        u.setLastName("Last");
        u.setEmail(u.getLogin() + "@example.com");
        u.setLangKey("en");
        return userRepository.saveAndFlush(u);
    }

    private Team seedTeamWith(User ownerUser, TeamRole role) {
        Team team = new Team();
        team.setName("Team-" + UUID.randomUUID().toString().substring(0, 6));
        team.setDescription("d");
        team.setCreatedDate(Instant.now());
        team = teamRepository.saveAndFlush(team);
        seedMembership(team, ownerUser, role);
        return team;
    }

    private TeamMember seedMembership(Team team, User user, TeamRole role) {
        TeamMember m = new TeamMember();
        m.setTeam(team);
        m.setUser(user);
        m.setRole(role);
        m.setJoinedDate(Instant.now());
        return teamMemberRepository.saveAndFlush(m);
    }
}
