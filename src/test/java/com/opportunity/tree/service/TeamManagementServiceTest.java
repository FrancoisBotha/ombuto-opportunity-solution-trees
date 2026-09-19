package com.opportunity.tree.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.opportunity.tree.domain.Team;
import com.opportunity.tree.domain.TeamMember;
import com.opportunity.tree.domain.User;
import com.opportunity.tree.domain.enumeration.TeamRole;
import com.opportunity.tree.repository.ProductRepository;
import com.opportunity.tree.repository.TeamMemberRepository;
import com.opportunity.tree.repository.TeamRepository;
import com.opportunity.tree.repository.UserRepository;
import com.opportunity.tree.service.broadcast.TreeChangePublisher;
import com.opportunity.tree.service.dto.AddTeamMemberRequest;
import com.opportunity.tree.service.dto.ChangeTeamMemberRoleRequest;
import com.opportunity.tree.service.dto.CreateTeamRequest;
import com.opportunity.tree.service.dto.MyTeamDTO;
import com.opportunity.tree.service.dto.TeamMemberViewDTO;
import com.opportunity.tree.service.dto.UpdateTeamRequest;
import com.opportunity.tree.service.dto.UserSearchResultDTO;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Unit tests for {@link TeamManagementService}, covering the TEAMS-002 acceptance criteria at
 * the service boundary: creator becomes owner with server-set dates (AC 1), my-teams shape (AC 2),
 * membership-gated reads (AC 3), owner-only writes with editor/viewer denied (AC 4),
 * last-owner protection (AC 5), duplicate-member 4xx (AC 6), and the user-search projection (AC 7).
 */
@ExtendWith(MockitoExtension.class)
class TeamManagementServiceTest {

    private static final String LOGIN = "alice";
    private static final String CREATOR_USER_ID = "user-alice";
    private static final String OTHER_USER_ID = "user-bob";
    private static final Long TEAM_ID = 100L;
    private static final Long OTHER_TEAM_ID = 200L;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TeamAccessService teamAccessService;

    @Mock
    private TreeChangePublisher changePublisher;

    private TeamManagementService service;

    private User creator;
    private User bob;

    @BeforeEach
    void setUp() {
        service = new TeamManagementService(
            teamRepository,
            teamMemberRepository,
            productRepository,
            userRepository,
            teamAccessService,
            changePublisher
        );
        creator = user(CREATOR_USER_ID, LOGIN, "Alice", "Anderson");
        bob = user(OTHER_USER_ID, "bob", "Bob", "Brown");
        lenient().when(userRepository.findOneByLogin(LOGIN)).thenReturn(Optional.of(creator));
        lenient().when(userRepository.findById(OTHER_USER_ID)).thenReturn(Optional.of(bob));
        lenient().when(userRepository.findById(CREATOR_USER_ID)).thenReturn(Optional.of(creator));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ---------------------------------------------------------------------
    // AC 1 — creator becomes owner; created/joined dates are server-side
    // ---------------------------------------------------------------------

    @Test
    void createTeam_makesCreatorTheOwnerAndSetsServerSideTimestamps() {
        authenticate(LOGIN);
        Team saved = team(TEAM_ID, "Alpha", "d");
        when(teamRepository.save(any(Team.class))).thenAnswer(inv -> {
            Team t = inv.getArgument(0);
            t.setId(TEAM_ID);
            return t;
        });
        when(teamMemberRepository.save(any(TeamMember.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateTeamRequest req = new CreateTeamRequest();
        req.setName("Alpha");
        req.setDescription("d");

        Instant before = Instant.now();
        MyTeamDTO dto = service.createTeam(req);
        Instant after = Instant.now();

        assertThat(dto.getRole()).isEqualTo(TeamRole.OWNER);
        assertThat(dto.getMemberCount()).isEqualTo(1L);
        assertThat(dto.getProductCount()).isEqualTo(0L);

        ArgumentCaptor<Team> teamCap = ArgumentCaptor.forClass(Team.class);
        verify(teamRepository).save(teamCap.capture());
        assertThat(teamCap.getValue().getCreatedDate()).isBetween(before, after);

        ArgumentCaptor<TeamMember> memberCap = ArgumentCaptor.forClass(TeamMember.class);
        verify(teamMemberRepository).save(memberCap.capture());
        assertThat(memberCap.getValue().getRole()).isEqualTo(TeamRole.OWNER);
        assertThat(memberCap.getValue().getUser()).isEqualTo(creator);
        assertThat(memberCap.getValue().getJoinedDate()).isBetween(before, after);
    }

    // ---------------------------------------------------------------------
    // AC 2 — my-teams shape with role + member/product counts, membership only
    // ---------------------------------------------------------------------

    @Test
    void listMyTeams_returnsOnlyCallersTeamsWithRoleAndCounts() {
        authenticate(LOGIN);
        when(teamMemberRepository.findMyTeamRows(LOGIN)).thenReturn(
            List.of(myTeamRow(TEAM_ID, "A", TeamRole.OWNER, 3L, 5L), myTeamRow(OTHER_TEAM_ID, "B", TeamRole.VIEWER, 2L, 1L))
        );

        List<MyTeamDTO> results = service.listMyTeams();

        assertThat(results).hasSize(2);
        assertThat(results.stream().map(MyTeamDTO::getRole)).containsExactly(TeamRole.OWNER, TeamRole.VIEWER);
        assertThat(results.get(0).getMemberCount()).isEqualTo(3L);
        assertThat(results.get(0).getProductCount()).isEqualTo(5L);
        assertThat(results.get(1).getMemberCount()).isEqualTo(2L);
        assertThat(results.get(1).getProductCount()).isEqualTo(1L);
    }

    @Test
    void listMyTeams_returnsEmptyWhenUnauthenticated() {
        // no auth
        assertThat(service.listMyTeams()).isEmpty();
    }

    // ---------------------------------------------------------------------
    // AC 3 — read/members require membership; non-members raise
    // ---------------------------------------------------------------------

    @Test
    void getTeam_requiresReadFromTeamAccessService() {
        authenticate(LOGIN);
        doThrow(new TeamAccessDeniedException()).when(teamAccessService).requireReadTeam(TEAM_ID);
        assertThatThrownBy(() -> service.getTeam(TEAM_ID)).isInstanceOf(TeamAccessDeniedException.class);
    }

    @Test
    void listMembers_requiresReadFromTeamAccessService() {
        authenticate(LOGIN);
        doThrow(new TeamAccessDeniedException()).when(teamAccessService).requireReadTeam(TEAM_ID);
        assertThatThrownBy(() -> service.listMembers(TEAM_ID)).isInstanceOf(TeamAccessDeniedException.class);
    }

    @Test
    void listMembers_returnsMembersForAuthorisedCaller() {
        authenticate(LOGIN);
        Team t = team(TEAM_ID, "A", null);
        when(teamMemberRepository.findAllByTeamId(TEAM_ID)).thenReturn(
            List.of(member(t, creator, TeamRole.OWNER), member(t, bob, TeamRole.EDITOR))
        );
        List<TeamMemberViewDTO> out = service.listMembers(TEAM_ID);
        assertThat(out).extracting(TeamMemberViewDTO::getLogin).containsExactly(LOGIN, "bob");
        assertThat(out).extracting(TeamMemberViewDTO::getRole).containsExactly(TeamRole.OWNER, TeamRole.EDITOR);
    }

    // ---------------------------------------------------------------------
    // AC 4 — owner-only writes; editor / viewer denied
    // ---------------------------------------------------------------------

    @Test
    void updateTeam_requiresOwner() {
        authenticate(LOGIN);
        doThrow(new TeamAccessDeniedException()).when(teamAccessService).requireOwnerTeam(TEAM_ID);
        UpdateTeamRequest req = new UpdateTeamRequest();
        req.setName("New");
        assertThatThrownBy(() -> service.updateTeam(TEAM_ID, req)).isInstanceOf(TeamAccessDeniedException.class);
        verify(teamRepository, never()).save(any());
    }

    @Test
    void addMember_requiresOwner() {
        authenticate(LOGIN);
        doThrow(new TeamAccessDeniedException()).when(teamAccessService).requireOwnerTeam(TEAM_ID);
        AddTeamMemberRequest req = new AddTeamMemberRequest();
        req.setUserId(OTHER_USER_ID);
        req.setRole(TeamRole.EDITOR);
        assertThatThrownBy(() -> service.addMember(TEAM_ID, req)).isInstanceOf(TeamAccessDeniedException.class);
        verify(teamMemberRepository, never()).save(any());
    }

    @Test
    void changeRole_requiresOwner() {
        authenticate(LOGIN);
        doThrow(new TeamAccessDeniedException()).when(teamAccessService).requireOwnerTeam(TEAM_ID);
        ChangeTeamMemberRoleRequest req = new ChangeTeamMemberRoleRequest();
        req.setRole(TeamRole.VIEWER);
        assertThatThrownBy(() -> service.changeRole(TEAM_ID, OTHER_USER_ID, req)).isInstanceOf(TeamAccessDeniedException.class);
    }

    @Test
    void removeMember_requiresOwner() {
        authenticate(LOGIN);
        doThrow(new TeamAccessDeniedException()).when(teamAccessService).requireOwnerTeam(TEAM_ID);
        assertThatThrownBy(() -> service.removeMember(TEAM_ID, OTHER_USER_ID)).isInstanceOf(TeamAccessDeniedException.class);
    }

    @Test
    void addMember_setsJoinedDateServerSide_andPersistsRequestedRole() {
        authenticate(LOGIN);
        Team t = team(TEAM_ID, "A", null);
        when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(t));
        when(teamMemberRepository.existsByTeamIdAndUserId(TEAM_ID, OTHER_USER_ID)).thenReturn(false);
        when(teamMemberRepository.save(any(TeamMember.class))).thenAnswer(inv -> {
            TeamMember m = inv.getArgument(0);
            m.setId(11L);
            return m;
        });

        AddTeamMemberRequest req = new AddTeamMemberRequest();
        req.setUserId(OTHER_USER_ID);
        req.setRole(TeamRole.EDITOR);
        Instant before = Instant.now();
        TeamMemberViewDTO dto = service.addMember(TEAM_ID, req);
        Instant after = Instant.now();

        assertThat(dto.getRole()).isEqualTo(TeamRole.EDITOR);
        assertThat(dto.getJoinedDate()).isBetween(before, after);
        assertThat(dto.getUserId()).isEqualTo(OTHER_USER_ID);
    }

    @Test
    void changeRole_persistsNewRole() {
        authenticate(LOGIN);
        TeamMember existing = member(team(TEAM_ID, "A", null), bob, TeamRole.EDITOR);
        existing.setId(11L);
        when(teamMemberRepository.findOneByTeamIdAndUserId(TEAM_ID, OTHER_USER_ID)).thenReturn(Optional.of(existing));
        when(teamMemberRepository.save(any(TeamMember.class))).thenAnswer(inv -> inv.getArgument(0));

        ChangeTeamMemberRoleRequest req = new ChangeTeamMemberRoleRequest();
        req.setRole(TeamRole.VIEWER);
        TeamMemberViewDTO dto = service.changeRole(TEAM_ID, OTHER_USER_ID, req);

        assertThat(dto.getRole()).isEqualTo(TeamRole.VIEWER);
    }

    // ---------------------------------------------------------------------
    // AC 5 — last owner cannot be removed or demoted
    // ---------------------------------------------------------------------

    @Test
    void removeMember_lastOwner_isRejectedWithClearErrorKey() {
        authenticate(LOGIN);
        TeamMember lastOwner = member(team(TEAM_ID, "A", null), creator, TeamRole.OWNER);
        lastOwner.setId(9L);
        when(teamMemberRepository.findOneByTeamIdAndUserId(TEAM_ID, CREATOR_USER_ID)).thenReturn(Optional.of(lastOwner));
        when(teamMemberRepository.countByTeamIdAndRole(TEAM_ID, TeamRole.OWNER)).thenReturn(1L);

        assertThatThrownBy(() -> service.removeMember(TEAM_ID, CREATOR_USER_ID))
            .isInstanceOf(TeamManagementException.class)
            .hasFieldOrPropertyWithValue("errorKey", "lastowner");
        verify(teamMemberRepository, never()).delete(any());
    }

    @Test
    void changeRole_demotingLastOwner_isRejectedWithClearErrorKey() {
        authenticate(LOGIN);
        TeamMember lastOwner = member(team(TEAM_ID, "A", null), creator, TeamRole.OWNER);
        lastOwner.setId(9L);
        when(teamMemberRepository.findOneByTeamIdAndUserId(TEAM_ID, CREATOR_USER_ID)).thenReturn(Optional.of(lastOwner));
        when(teamMemberRepository.countByTeamIdAndRole(TEAM_ID, TeamRole.OWNER)).thenReturn(1L);

        ChangeTeamMemberRoleRequest req = new ChangeTeamMemberRoleRequest();
        req.setRole(TeamRole.EDITOR);

        assertThatThrownBy(() -> service.changeRole(TEAM_ID, CREATOR_USER_ID, req))
            .isInstanceOf(TeamManagementException.class)
            .hasFieldOrPropertyWithValue("errorKey", "lastowner");
    }

    @Test
    void removeMember_secondOwner_isAllowed_whenAnotherOwnerRemains() {
        authenticate(LOGIN);
        TeamMember owner2 = member(team(TEAM_ID, "A", null), bob, TeamRole.OWNER);
        owner2.setId(9L);
        when(teamMemberRepository.findOneByTeamIdAndUserId(TEAM_ID, OTHER_USER_ID)).thenReturn(Optional.of(owner2));
        when(teamMemberRepository.countByTeamIdAndRole(TEAM_ID, TeamRole.OWNER)).thenReturn(2L);

        service.removeMember(TEAM_ID, OTHER_USER_ID);

        verify(teamMemberRepository).delete(owner2);
    }

    // ---------------------------------------------------------------------
    // AC 6 — duplicate member returns a 4xx (TeamManagementException), not a 500
    // ---------------------------------------------------------------------

    @Test
    void addMember_duplicate_isRejectedWithMemberExistsKey() {
        authenticate(LOGIN);
        when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(team(TEAM_ID, "A", null)));
        when(teamMemberRepository.existsByTeamIdAndUserId(TEAM_ID, OTHER_USER_ID)).thenReturn(true);

        AddTeamMemberRequest req = new AddTeamMemberRequest();
        req.setUserId(OTHER_USER_ID);
        req.setRole(TeamRole.EDITOR);

        assertThatThrownBy(() -> service.addMember(TEAM_ID, req))
            .isInstanceOf(TeamManagementException.class)
            .hasFieldOrPropertyWithValue("errorKey", "memberexists");
        verify(teamMemberRepository, never()).save(any());
    }

    // ---------------------------------------------------------------------
    // AC 7 — user search returns only synced users, projected to id/login/name
    // ---------------------------------------------------------------------

    @Test
    void searchUsers_requiresOwner_andReturnsMinimalProjection() {
        authenticate(LOGIN);
        Page<User> page = new PageImpl<>(List.of(bob));
        when(userRepository.searchByLoginOrName(eq("bo"), any(Pageable.class))).thenReturn(page);

        List<UserSearchResultDTO> results = service.searchUsers(TEAM_ID, "bo", 10);

        verify(teamAccessService).requireOwnerTeam(TEAM_ID);
        assertThat(results)
            .singleElement()
            .satisfies(u -> {
                assertThat(u.getId()).isEqualTo(OTHER_USER_ID);
                assertThat(u.getLogin()).isEqualTo("bob");
                assertThat(u.getName()).isEqualTo("Bob Brown");
            });
    }

    @Test
    void searchUsers_deniedForNonOwner() {
        authenticate(LOGIN);
        doThrow(new TeamAccessDeniedException()).when(teamAccessService).requireOwnerTeam(TEAM_ID);
        assertThatThrownBy(() -> service.searchUsers(TEAM_ID, "b", 10)).isInstanceOf(TeamAccessDeniedException.class);
    }

    // ---------------------------------------------------------------------
    // AC 9 partial — multi-team: same user can be owner in one, viewer in another
    // ---------------------------------------------------------------------

    @Test
    void sameUserCanBeOwnerInOneTeamAndViewerInAnother_throughMyTeams() {
        authenticate(LOGIN);
        when(teamMemberRepository.findMyTeamRows(LOGIN)).thenReturn(
            List.of(myTeamRow(TEAM_ID, "A", TeamRole.OWNER, 1L, 0L), myTeamRow(OTHER_TEAM_ID, "B", TeamRole.VIEWER, 1L, 0L))
        );

        List<MyTeamDTO> teams = service.listMyTeams();

        assertThat(teams).hasSize(2);
        MyTeamDTO first = teams.get(0);
        MyTeamDTO second = teams.get(1);
        assertThat(first.getId()).isEqualTo(TEAM_ID);
        assertThat(first.getRole()).isEqualTo(TeamRole.OWNER);
        assertThat(second.getId()).isEqualTo(OTHER_TEAM_ID);
        assertThat(second.getRole()).isEqualTo(TeamRole.VIEWER);
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private static Object[] myTeamRow(Long id, String name, TeamRole role, long members, long products) {
        return new Object[] { id, name, null, Instant.parse("2026-01-01T00:00:00Z"), role, members, products };
    }

    private void authenticate(String login) {
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(new UsernamePasswordAuthenticationToken(login, "n/a"));
        SecurityContextHolder.setContext(ctx);
    }

    private static Team team(Long id, String name, String description) {
        Team t = new Team();
        t.setId(id);
        t.setName(name);
        t.setDescription(description);
        t.setCreatedDate(Instant.ofEpochSecond(1_700_000_000L));
        return t;
    }

    private static User user(String id, String login, String first, String last) {
        User u = new User();
        u.setId(id);
        u.setLogin(login);
        u.setFirstName(first);
        u.setLastName(last);
        u.setActivated(true);
        return u;
    }

    private static TeamMember member(Team team, User user, TeamRole role) {
        TeamMember m = new TeamMember();
        m.setTeam(team);
        m.setUser(user);
        m.setRole(role);
        m.setJoinedDate(Instant.ofEpochSecond(1_700_000_100L));
        return m;
    }
}
