package com.opportunity.tree.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opportunity.tree.IntegrationTest;
import com.opportunity.tree.domain.Team;
import com.opportunity.tree.domain.TeamMember;
import com.opportunity.tree.domain.User;
import com.opportunity.tree.domain.enumeration.TeamRole;
import com.opportunity.tree.repository.TeamMemberRepository;
import com.opportunity.tree.repository.TeamRepository;
import com.opportunity.tree.repository.UserRepository;
import com.opportunity.tree.service.dto.AddTeamMemberRequest;
import com.opportunity.tree.service.dto.ChangeTeamMemberRoleRequest;
import com.opportunity.tree.service.dto.CreateTeamRequest;
import com.opportunity.tree.service.dto.UpdateTeamRequest;
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
 * Integration tests for the {@link TeamManagementResource} (TEAMS-002 AC 9):
 * create team → owner; owner add/change/remove member; editor/viewer denied;
 * non-member denied; last-owner protection; same user with different roles in two teams.
 */
@IntegrationTest
@AutoConfigureMockMvc
@Transactional
class TeamManagementResourceIT {

    private static final String API = "/api/team-management";

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

    private User creator;
    private User editorUser;
    private User viewerUser;
    private User outsider;

    @BeforeEach
    void seedUsers() {
        creator = persistUser("t2creator");
        editorUser = persistUser("t2editor");
        viewerUser = persistUser("t2viewer");
        outsider = persistUser("t2outsider");
    }

    @AfterEach
    void cleanup() {
        teamMemberRepository.deleteAll();
        teamRepository.deleteAll();
        userRepository.deleteAll();
    }

    // TEAMS-002 — a whitespace-only name used to be accepted (201) because @Size counted the
    // spaces. The name is trimmed before validation, so it now fails @Size(min = 2) with a 400.
    @Test
    void createTeam_withWhitespaceOnlyName_isRejected() throws Exception {
        mockMvc
            .perform(
                post(API + "/teams")
                    .with(csrf())
                    .with(user(creator.getLogin()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"   \",\"description\":\"d\"}")
            )
            .andExpect(status().isBadRequest());

        assertThat(teamRepository.findAll()).noneMatch(t -> t.getName() == null || t.getName().isBlank());
    }

    // The same normalisation must apply to a rename, and a padded name must be stored trimmed.
    @Test
    void updateTeam_withWhitespaceOnlyName_isRejected_andPaddedNameIsTrimmed() throws Exception {
        Long teamId = seedTeamWith(creator, TeamRole.OWNER).getId();

        mockMvc
            .perform(
                put(API + "/teams/{id}", teamId)
                    .with(csrf())
                    .with(user(creator.getLogin()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\" \\t \",\"description\":\"d\"}")
            )
            .andExpect(status().isBadRequest());

        mockMvc
            .perform(
                put(API + "/teams/{id}", teamId)
                    .with(csrf())
                    .with(user(creator.getLogin()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"  Trimmed Team  \",\"description\":\"d\"}")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Trimmed Team"));

        assertThat(teamRepository.findById(teamId).orElseThrow().getName()).isEqualTo("Trimmed Team");
    }

    // AC 1 + AC 9 — create team → creator is owner; createdDate/joinedDate ignored from client
    @Test
    void createTeam_makesCallerOwner_andIgnoresClientCreatedDate() throws Exception {
        CreateTeamRequest req = new CreateTeamRequest();
        req.setName("Alpha");
        req.setDescription("d");

        mockMvc
            .perform(
                post(API + "/teams")
                    .with(csrf())
                    .with(user(creator.getLogin()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Alpha\",\"description\":\"d\",\"createdDate\":\"1970-01-01T00:00:00Z\"}")
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.role").value("OWNER"))
            .andExpect(jsonPath("$.name").value("Alpha"));

        // Scope the lookup to the team this test created rather than asserting on the size of the
        // whole table: a global row count makes the test fail for reasons that have nothing to do
        // with it whenever another IT leaves a team behind.
        Team created = teamRepository
            .findAll()
            .stream()
            .filter(t -> "Alpha".equals(t.getName()))
            .reduce((a, b) -> {
                throw new AssertionError("More than one team named Alpha");
            })
            .orElseThrow(() -> new AssertionError("The created team was not persisted"));
        assertThat(created.getCreatedDate()).isAfter(Instant.EPOCH.plusSeconds(60)); // not the client value

        List<TeamMember> members = teamMemberRepository.findAllByTeamId(created.getId());
        assertThat(members)
            .singleElement()
            .satisfies(m -> {
                assertThat(m.getRole()).isEqualTo(TeamRole.OWNER);
                assertThat(m.getUser().getLogin()).isEqualTo(creator.getLogin());
                assertThat(m.getJoinedDate()).isNotNull();
            });
    }

    // AC 4 — only owners can add / change / remove members; editor + viewer receive 403
    @Test
    void ownerCanAddChangeRemoveMember_editorAndViewerDenied() throws Exception {
        Team team = seedTeamWith(creator, TeamRole.OWNER);
        seedMembership(team, editorUser, TeamRole.EDITOR);
        seedMembership(team, viewerUser, TeamRole.VIEWER);

        // Editor cannot add another member
        AddTeamMemberRequest add = new AddTeamMemberRequest();
        add.setUserId(outsider.getId());
        add.setRole(TeamRole.EDITOR);
        mockMvc
            .perform(
                post(API + "/teams/" + team.getId() + "/members")
                    .with(csrf())
                    .with(user(editorUser.getLogin()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(add))
            )
            .andExpect(status().isForbidden());

        // Viewer cannot add
        mockMvc
            .perform(
                post(API + "/teams/" + team.getId() + "/members")
                    .with(csrf())
                    .with(user(viewerUser.getLogin()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(add))
            )
            .andExpect(status().isForbidden());

        // Owner can add
        mockMvc
            .perform(
                post(API + "/teams/" + team.getId() + "/members")
                    .with(csrf())
                    .with(user(creator.getLogin()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(add))
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.role").value("EDITOR"))
            .andExpect(jsonPath("$.userId").value(outsider.getId()));

        // Owner can change the role
        ChangeTeamMemberRoleRequest chg = new ChangeTeamMemberRoleRequest();
        chg.setRole(TeamRole.VIEWER);
        mockMvc
            .perform(
                put(API + "/teams/" + team.getId() + "/members/" + outsider.getId())
                    .with(csrf())
                    .with(user(creator.getLogin()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(chg))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.role").value("VIEWER"));

        // Owner can remove
        mockMvc
            .perform(delete(API + "/teams/" + team.getId() + "/members/" + outsider.getId()).with(csrf()).with(user(creator.getLogin())))
            .andExpect(status().isNoContent());

        assertThat(teamMemberRepository.existsByTeamIdAndUserId(team.getId(), outsider.getId())).isFalse();
    }

    // AC 3 — non-member cannot read team, list members, or hit any endpoint
    @Test
    void nonMemberIsDenied() throws Exception {
        Team team = seedTeamWith(creator, TeamRole.OWNER);

        mockMvc.perform(get(API + "/teams/" + team.getId()).with(user(outsider.getLogin()))).andExpect(status().isForbidden());
        mockMvc.perform(get(API + "/teams/" + team.getId() + "/members").with(user(outsider.getLogin()))).andExpect(status().isForbidden());
    }

    // AC 5 — last owner cannot be removed or demoted; 4xx with clear error key
    @Test
    void lastOwnerCannotBeRemovedOrDemoted() throws Exception {
        Team team = seedTeamWith(creator, TeamRole.OWNER);

        mockMvc
            .perform(delete(API + "/teams/" + team.getId() + "/members/" + creator.getId()).with(csrf()).with(user(creator.getLogin())))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.lastowner"));

        ChangeTeamMemberRoleRequest demote = new ChangeTeamMemberRoleRequest();
        demote.setRole(TeamRole.EDITOR);
        mockMvc
            .perform(
                put(API + "/teams/" + team.getId() + "/members/" + creator.getId())
                    .with(csrf())
                    .with(user(creator.getLogin()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(demote))
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.lastowner"));
    }

    // AC 6 — adding an already-member returns 4xx (not 500)
    @Test
    void addingExistingMemberReturns4xx() throws Exception {
        Team team = seedTeamWith(creator, TeamRole.OWNER);
        seedMembership(team, editorUser, TeamRole.EDITOR);

        AddTeamMemberRequest req = new AddTeamMemberRequest();
        req.setUserId(editorUser.getId());
        req.setRole(TeamRole.VIEWER);
        mockMvc
            .perform(
                post(API + "/teams/" + team.getId() + "/members")
                    .with(csrf())
                    .with(user(creator.getLogin()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(req))
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.memberexists"));
    }

    // AC 9 — same user has different roles in two teams; my-teams reflects both
    @Test
    void sameUserOwnerInOneTeamViewerInAnother() throws Exception {
        Team teamOwned = seedTeamWith(creator, TeamRole.OWNER);
        Team teamViewed = seedTeamWith(editorUser, TeamRole.OWNER);
        seedMembership(teamViewed, creator, TeamRole.VIEWER);

        mockMvc
            .perform(get(API + "/my-teams").with(user(creator.getLogin())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(2)))
            .andExpect(jsonPath("$[?(@.id == " + teamOwned.getId() + ")].role", org.hamcrest.Matchers.hasItem("OWNER")))
            .andExpect(jsonPath("$[?(@.id == " + teamViewed.getId() + ")].role", org.hamcrest.Matchers.hasItem("VIEWER")));
    }

    // AC 4 — only owners can rename / re-describe
    @Test
    void updateTeam_ownerOnly() throws Exception {
        Team team = seedTeamWith(creator, TeamRole.OWNER);
        seedMembership(team, editorUser, TeamRole.EDITOR);

        UpdateTeamRequest req = new UpdateTeamRequest();
        req.setName("Renamed");
        req.setDescription("nd");

        // Editor denied
        mockMvc
            .perform(
                put(API + "/teams/" + team.getId())
                    .with(csrf())
                    .with(user(editorUser.getLogin()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(req))
            )
            .andExpect(status().isForbidden());

        // Owner succeeds
        mockMvc
            .perform(
                put(API + "/teams/" + team.getId())
                    .with(csrf())
                    .with(user(creator.getLogin()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(req))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Renamed"));
    }

    // ---------------------------------------------------------------------

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

    private Team seedTeamWith(User owner, TeamRole role) {
        Team team = new Team();
        team.setName("Team-" + UUID.randomUUID().toString().substring(0, 6));
        team.setDescription("d");
        team.setCreatedDate(Instant.now());
        team = teamRepository.saveAndFlush(team);
        seedMembership(team, owner, role);
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
