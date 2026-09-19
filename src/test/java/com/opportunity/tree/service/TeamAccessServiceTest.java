package com.opportunity.tree.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.opportunity.tree.domain.Team;
import com.opportunity.tree.domain.enumeration.TeamRole;
import com.opportunity.tree.domain.enumeration.TreeNodeType;
import com.opportunity.tree.repository.TreeAccessLookupRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Unit tests for {@link TeamAccessService}. Covers owner, editor, viewer and
 * non-member outcomes for team- and node-level checks (all six node types via
 * {@code requireRead/EditNode}), plus the
 * multi-team case where the same user is owner in one team and viewer in another.
 */
@ExtendWith(MockitoExtension.class)
class TeamAccessServiceTest {

    private static final String LOGIN = "alice";

    private static final Long TEAM_A = 100L;
    private static final Long TEAM_B = 200L;
    private static final Long OTHER_TEAM = 999L;

    private static final Long PRODUCT_A = 10L;
    private static final Long OUTCOME_A = 20L;
    private static final Long OPPORTUNITY_A = 30L;
    private static final Long SOLUTION_A = 40L;
    private static final Long ASSUMPTION_A = 50L;

    @Mock
    private TreeAccessLookupRepository treeAccessLookupRepository;

    private TeamAccessService service;

    private Team teamA;
    private Team teamB;

    @BeforeEach
    void setUp() {
        service = new TeamAccessService(treeAccessLookupRepository);

        teamA = team(TEAM_A);
        teamB = team(TEAM_B);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ---------------------------------------------------------------------
    // Team-level role semantics
    // ---------------------------------------------------------------------

    @Test
    void ownerHasReadEditAndOwnerOnTheirTeam() {
        authenticate();
        stubMembership(TeamRole.OWNER, teamA);

        assertThat(service.canReadTeam(TEAM_A)).isTrue();
        assertThat(service.canEditTeam(TEAM_A)).isTrue();
        assertThat(service.isTeamOwner(TEAM_A)).isTrue();
    }

    @Test
    void editorHasReadAndEditButNotOwner() {
        authenticate();
        stubMembership(TeamRole.EDITOR, teamA);

        assertThat(service.canReadTeam(TEAM_A)).isTrue();
        assertThat(service.canEditTeam(TEAM_A)).isTrue();
        assertThat(service.isTeamOwner(TEAM_A)).isFalse();
    }

    @Test
    void viewerHasReadOnly() {
        authenticate();
        stubMembership(TeamRole.VIEWER, teamA);

        assertThat(service.canReadTeam(TEAM_A)).isTrue();
        assertThat(service.canEditTeam(TEAM_A)).isFalse();
        assertThat(service.isTeamOwner(TEAM_A)).isFalse();
    }

    @Test
    void nonMemberHasNothing() {
        authenticate();
        stubNoMemberships();

        assertThat(service.canReadTeam(TEAM_A)).isFalse();
        assertThat(service.canEditTeam(TEAM_A)).isFalse();
        assertThat(service.isTeamOwner(TEAM_A)).isFalse();
    }

    @Test
    void unauthenticatedUserHasNothing() {
        // No SecurityContext authentication set
        assertThat(service.canReadTeam(TEAM_A)).isFalse();
        assertThat(service.canEditTeam(TEAM_A)).isFalse();
        assertThat(service.isTeamOwner(TEAM_A)).isFalse();
        assertThat(service.getCurrentUserTeamIds()).isEmpty();
    }

    // ---------------------------------------------------------------------
    // Multi-team: owner in team A, viewer in team B
    // ---------------------------------------------------------------------

    @Test
    void userCanBeOwnerInOneTeamAndViewerInAnother() {
        authenticate();
        Object[] ownerInA = membership(TeamRole.OWNER, teamA);
        Object[] viewerInB = membership(TeamRole.VIEWER, teamB);
        when(treeAccessLookupRepository.findTeamRolesOfUser(LOGIN)).thenReturn(List.of(ownerInA, viewerInB));

        assertThat(service.canEditTeam(TEAM_A)).isTrue();
        assertThat(service.isTeamOwner(TEAM_A)).isTrue();

        assertThat(service.canReadTeam(TEAM_B)).isTrue();
        assertThat(service.canEditTeam(TEAM_B)).isFalse();
        assertThat(service.isTeamOwner(TEAM_B)).isFalse();

        assertThat(service.getCurrentUserTeamIds()).containsExactlyInAnyOrder(TEAM_A, TEAM_B);
    }

    // ---------------------------------------------------------------------
    // Enforcing variants
    // ---------------------------------------------------------------------

    @Test
    void requireVariantsThrowForNonMember() {
        authenticate();
        stubNoMemberships();

        assertThatThrownBy(() -> service.requireReadTeam(TEAM_A)).isInstanceOf(TeamAccessDeniedException.class);
        assertThatThrownBy(() -> service.requireEditTeam(TEAM_A)).isInstanceOf(TeamAccessDeniedException.class);
        assertThatThrownBy(() -> service.requireOwnerTeam(TEAM_A)).isInstanceOf(TeamAccessDeniedException.class);
    }

    @Test
    void requireVariantsPassForOwner() {
        authenticate();
        stubMembership(TeamRole.OWNER, teamA);

        service.requireReadTeam(TEAM_A);
        service.requireEditTeam(TEAM_A);
        service.requireOwnerTeam(TEAM_A);
    }

    @Test
    void requireEditThrowsForViewer() {
        authenticate();
        stubMembership(TeamRole.VIEWER, teamA);

        service.requireReadTeam(TEAM_A); // read is fine
        assertThatThrownBy(() -> service.requireEditTeam(TEAM_A)).isInstanceOf(TeamAccessDeniedException.class);
        assertThatThrownBy(() -> service.requireOwnerTeam(TEAM_A)).isInstanceOf(TeamAccessDeniedException.class);
    }

    @Test
    void requireOwnerThrowsForEditor() {
        authenticate();
        stubMembership(TeamRole.EDITOR, teamA);

        service.requireEditTeam(TEAM_A);
        assertThatThrownBy(() -> service.requireOwnerTeam(TEAM_A)).isInstanceOf(TeamAccessDeniedException.class);
    }

    // ---------------------------------------------------------------------
    // Membership listing
    // ---------------------------------------------------------------------

    @Test
    void getCurrentUserTeamIdsReturnsAllMembershipTeams() {
        authenticate();
        when(treeAccessLookupRepository.findTeamRolesOfUser(LOGIN)).thenReturn(
            List.<Object[]>of(membership(TeamRole.OWNER, teamA), membership(TeamRole.VIEWER, teamB))
        );

        assertThat(service.getCurrentUserTeamIds()).containsExactlyInAnyOrder(TEAM_A, TEAM_B);
    }

    @Test
    void getCurrentUserRoleReturnsEmptyForNonMember() {
        authenticate();
        stubNoMemberships();

        assertThat(service.getCurrentUserRole(TEAM_A)).isEmpty();
    }

    // ---------------------------------------------------------------------
    // Generic node checks (all six node types) + link / question / comment
    // ---------------------------------------------------------------------

    private static final Long EVIDENCE_A = 60L;
    private static final Long LINK_A = 70L;
    private static final Long QUESTION_A = 80L;
    private static final Long COMMENT_A = 90L;
    private static final Long MISSING = 424242L;

    private void stubAllNodesInTeamA() {
        lenient().when(treeAccessLookupRepository.findTeamIdOfProduct(PRODUCT_A)).thenReturn(Optional.of(TEAM_A));
        lenient().when(treeAccessLookupRepository.findTeamIdOfOutcome(OUTCOME_A)).thenReturn(Optional.of(TEAM_A));
        lenient().when(treeAccessLookupRepository.findTeamIdOfOpportunity(OPPORTUNITY_A)).thenReturn(Optional.of(TEAM_A));
        lenient().when(treeAccessLookupRepository.findTeamIdOfSolution(SOLUTION_A)).thenReturn(Optional.of(TEAM_A));
        lenient().when(treeAccessLookupRepository.findTeamIdOfAssumption(ASSUMPTION_A)).thenReturn(Optional.of(TEAM_A));
        lenient().when(treeAccessLookupRepository.findTeamIdOfEvidence(EVIDENCE_A)).thenReturn(Optional.of(TEAM_A));
    }

    private Long idOf(TreeNodeType type) {
        return switch (type) {
            case PRODUCT -> PRODUCT_A;
            case OUTCOME -> OUTCOME_A;
            case OPPORTUNITY -> OPPORTUNITY_A;
            case SOLUTION -> SOLUTION_A;
            case ASSUMPTION -> ASSUMPTION_A;
            case EVIDENCE -> EVIDENCE_A;
        };
    }

    @Test
    void genericNodeChecksAllowEditorToReadAndEditEveryType() {
        authenticate();
        stubMembership(TeamRole.EDITOR, teamA);
        stubAllNodesInTeamA();

        for (TreeNodeType type : TreeNodeType.values()) {
            assertThat(service.canReadNode(type, idOf(type))).as("read %s", type).isTrue();
            assertThat(service.canEditNode(type, idOf(type))).as("edit %s", type).isTrue();
            assertThat(service.requireReadNode(type, idOf(type))).isEqualTo(TEAM_A);
            assertThat(service.requireEditNode(type, idOf(type))).isEqualTo(TEAM_A);
        }
    }

    @Test
    void genericNodeChecksLetViewerReadButNotEditEveryType() {
        authenticate();
        stubMembership(TeamRole.VIEWER, teamA);
        stubAllNodesInTeamA();

        for (TreeNodeType type : TreeNodeType.values()) {
            assertThat(service.requireReadNode(type, idOf(type))).isEqualTo(TEAM_A);
            assertThatThrownBy(() -> service.requireEditNode(type, idOf(type)))
                .as("edit %s", type)
                .isInstanceOf(TeamAccessDeniedException.class);
        }
    }

    @Test
    void genericNodeChecksDenyNonMemberForEveryType() {
        authenticate();
        stubNoMemberships();
        stubAllNodesInTeamA();

        for (TreeNodeType type : TreeNodeType.values()) {
            assertThat(service.canReadNode(type, idOf(type))).isFalse();
            assertThatThrownBy(() -> service.requireReadNode(type, idOf(type))).isInstanceOf(TeamAccessDeniedException.class);
            assertThatThrownBy(() -> service.requireEditNode(type, idOf(type))).isInstanceOf(TeamAccessDeniedException.class);
        }
    }

    @Test
    void missingNodeIsIndistinguishableFromNonMembership() {
        authenticate();
        lenient()
            .when(treeAccessLookupRepository.findTeamRolesOfUser(LOGIN))
            .thenReturn(List.<Object[]>of(membership(TeamRole.OWNER, teamA)));
        when(treeAccessLookupRepository.findTeamIdOfEvidence(MISSING)).thenReturn(Optional.empty());
        when(treeAccessLookupRepository.findTeamIdOfOpportunity(OPPORTUNITY_A)).thenReturn(Optional.of(OTHER_TEAM));

        Throwable missing = org.assertj.core.api.Assertions.catchThrowable(() -> service.requireReadNode(TreeNodeType.EVIDENCE, MISSING));
        Throwable foreign = org.assertj.core.api.Assertions.catchThrowable(() ->
            service.requireReadNode(TreeNodeType.OPPORTUNITY, OPPORTUNITY_A)
        );
        assertThat(missing).isInstanceOf(TeamAccessDeniedException.class);
        assertThat(foreign).isInstanceOf(TeamAccessDeniedException.class);
        assertThat(missing.getMessage()).isEqualTo(foreign.getMessage());
        assertThatThrownBy(() -> service.requireEditNode(null, 1L)).isInstanceOf(TeamAccessDeniedException.class);
        assertThatThrownBy(() -> service.requireEditNode(TreeNodeType.PRODUCT, null)).isInstanceOf(TeamAccessDeniedException.class);
    }

    @Test
    void adminAuthorityGrantsNoImplicitNodeAccess() {
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(
            new UsernamePasswordAuthenticationToken(
                LOGIN,
                "n/a",
                List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"))
            )
        );
        SecurityContextHolder.setContext(ctx);
        stubNoMemberships();
        stubAllNodesInTeamA();

        assertThatThrownBy(() -> service.requireReadNode(TreeNodeType.PRODUCT, PRODUCT_A)).isInstanceOf(TeamAccessDeniedException.class);
        assertThatThrownBy(() -> service.requireReadTeam(TEAM_A)).isInstanceOf(TeamAccessDeniedException.class);
    }

    @Test
    void linkQuestionAndCommentResolveToTheirNode() {
        authenticate();
        stubMembership(TeamRole.EDITOR, teamA);
        stubAllNodesInTeamA();
        when(treeAccessLookupRepository.findNodeIdsOfLink(LINK_A)).thenReturn(
            List.<Object[]>of(new Object[] { null, null, null, null, ASSUMPTION_A, null })
        );
        when(treeAccessLookupRepository.findOpportunityIdOfQuestion(QUESTION_A)).thenReturn(Optional.of(OPPORTUNITY_A));
        when(treeAccessLookupRepository.findNodeIdsOfComment(COMMENT_A)).thenReturn(
            List.<Object[]>of(new Object[] { null, null, null, null, EVIDENCE_A })
        );

        assertThat(service.requireEditLink(LINK_A)).isEqualTo(new TreeNodeRef(TreeNodeType.ASSUMPTION, ASSUMPTION_A));
        assertThat(service.requireEditQuestion(QUESTION_A)).isEqualTo(new TreeNodeRef(TreeNodeType.OPPORTUNITY, OPPORTUNITY_A));
        assertThat(service.requireEditComment(COMMENT_A)).isEqualTo(new TreeNodeRef(TreeNodeType.EVIDENCE, EVIDENCE_A));
        assertThat(service.requireReadComment(COMMENT_A).key()).isEqualTo("evidence-" + EVIDENCE_A);
    }

    @Test
    void viewerCanReadButNotEditLinksQuestionsAndComments() {
        authenticate();
        stubMembership(TeamRole.VIEWER, teamA);
        stubAllNodesInTeamA();
        when(treeAccessLookupRepository.findNodeIdsOfLink(LINK_A)).thenReturn(
            List.<Object[]>of(new Object[] { PRODUCT_A, null, null, null, null, null })
        );
        when(treeAccessLookupRepository.findOpportunityIdOfQuestion(QUESTION_A)).thenReturn(Optional.of(OPPORTUNITY_A));
        when(treeAccessLookupRepository.findNodeIdsOfComment(COMMENT_A)).thenReturn(
            List.<Object[]>of(new Object[] { OUTCOME_A, null, null, null, null })
        );

        assertThat(service.requireReadLink(LINK_A).type()).isEqualTo(TreeNodeType.PRODUCT);
        assertThat(service.requireReadQuestion(QUESTION_A).type()).isEqualTo(TreeNodeType.OPPORTUNITY);
        assertThat(service.requireReadComment(COMMENT_A).type()).isEqualTo(TreeNodeType.OUTCOME);
        assertThatThrownBy(() -> service.requireEditLink(LINK_A)).isInstanceOf(TeamAccessDeniedException.class);
        assertThatThrownBy(() -> service.requireEditQuestion(QUESTION_A)).isInstanceOf(TeamAccessDeniedException.class);
        assertThatThrownBy(() -> service.requireEditComment(COMMENT_A)).isInstanceOf(TeamAccessDeniedException.class);
    }

    @Test
    void missingLinkQuestionOrCommentIsDenied() {
        authenticate();
        lenient()
            .when(treeAccessLookupRepository.findTeamRolesOfUser(LOGIN))
            .thenReturn(List.<Object[]>of(membership(TeamRole.OWNER, teamA)));
        when(treeAccessLookupRepository.findNodeIdsOfLink(MISSING)).thenReturn(List.of());
        when(treeAccessLookupRepository.findOpportunityIdOfQuestion(MISSING)).thenReturn(Optional.empty());
        when(treeAccessLookupRepository.findNodeIdsOfComment(MISSING)).thenReturn(List.of());

        assertThatThrownBy(() -> service.requireReadLink(MISSING)).isInstanceOf(TeamAccessDeniedException.class);
        assertThatThrownBy(() -> service.requireReadQuestion(MISSING)).isInstanceOf(TeamAccessDeniedException.class);
        assertThatThrownBy(() -> service.requireReadComment(MISSING)).isInstanceOf(TeamAccessDeniedException.class);
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private void authenticate() {
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(new UsernamePasswordAuthenticationToken(LOGIN, "n/a"));
        SecurityContextHolder.setContext(ctx);
    }

    private void stubMembership(TeamRole role, Team team) {
        when(treeAccessLookupRepository.findTeamRolesOfUser(LOGIN)).thenReturn(List.<Object[]>of(membership(role, team)));
    }

    private void stubNoMemberships() {
        when(treeAccessLookupRepository.findTeamRolesOfUser(LOGIN)).thenReturn(List.of());
    }

    /** A row of TreeAccessLookupRepository.findTeamRolesOfUser: [teamId, role]. */
    private static Object[] membership(TeamRole role, Team team) {
        return new Object[] { team.getId(), role };
    }

    private static Team team(Long id) {
        Team t = new Team();
        t.setId(id);
        t.setName("Team " + id);
        return t;
    }
}
