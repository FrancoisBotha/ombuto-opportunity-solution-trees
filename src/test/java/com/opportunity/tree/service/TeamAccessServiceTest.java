package com.opportunity.tree.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.opportunity.tree.domain.Assumption;
import com.opportunity.tree.domain.Opportunity;
import com.opportunity.tree.domain.Outcome;
import com.opportunity.tree.domain.Product;
import com.opportunity.tree.domain.Solution;
import com.opportunity.tree.domain.Team;
import com.opportunity.tree.domain.TeamMember;
import com.opportunity.tree.domain.User;
import com.opportunity.tree.domain.enumeration.TeamRole;
import com.opportunity.tree.repository.AssumptionRepository;
import com.opportunity.tree.repository.OpportunityRepository;
import com.opportunity.tree.repository.OutcomeRepository;
import com.opportunity.tree.repository.ProductRepository;
import com.opportunity.tree.repository.SolutionRepository;
import com.opportunity.tree.repository.TeamMemberRepository;
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
 * non-member outcomes for team-, product- and node-level checks, plus the
 * multi-team case where the same user is owner in one team and viewer in another.
 */
@ExtendWith(MockitoExtension.class)
class TeamAccessServiceTest {

    private static final String LOGIN = "alice";

    private static final Long TEAM_A = 100L;
    private static final Long TEAM_B = 200L;
    private static final Long OTHER_TEAM = 999L;

    private static final Long PRODUCT_A = 10L;
    private static final Long PRODUCT_OTHER = 11L;
    private static final Long OUTCOME_A = 20L;
    private static final Long OPPORTUNITY_A = 30L;
    private static final Long SOLUTION_A = 40L;
    private static final Long ASSUMPTION_A = 50L;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OutcomeRepository outcomeRepository;

    @Mock
    private OpportunityRepository opportunityRepository;

    @Mock
    private SolutionRepository solutionRepository;

    @Mock
    private AssumptionRepository assumptionRepository;

    private TeamAccessService service;

    private Team teamA;
    private Team teamB;
    private Team otherTeam;
    private Product productA;
    private Product productOther;
    private Outcome outcomeA;
    private Opportunity opportunityA;
    private Solution solutionA;
    private Assumption assumptionA;

    @BeforeEach
    void setUp() {
        service = new TeamAccessService(
            teamMemberRepository,
            productRepository,
            outcomeRepository,
            opportunityRepository,
            solutionRepository,
            assumptionRepository
        );

        teamA = team(TEAM_A);
        teamB = team(TEAM_B);
        otherTeam = team(OTHER_TEAM);

        productA = new Product().id(PRODUCT_A).name("Prod A").archived(false);
        productA.setTeam(teamA);
        productOther = new Product().id(PRODUCT_OTHER).name("Prod Other").archived(false);
        productOther.setTeam(otherTeam);

        outcomeA = new Outcome().id(OUTCOME_A);
        outcomeA.setProduct(productA);
        opportunityA = new Opportunity().id(OPPORTUNITY_A);
        opportunityA.setOutcome(outcomeA);
        solutionA = new Solution().id(SOLUTION_A);
        solutionA.setOpportunity(opportunityA);
        assumptionA = new Assumption().id(ASSUMPTION_A);
        assumptionA.setSolution(solutionA);

        // Stubbed with lenient() because not every test exercises every lookup.
        lenient().when(productRepository.findById(eq(PRODUCT_A))).thenReturn(Optional.of(productA));
        lenient().when(productRepository.findById(eq(PRODUCT_OTHER))).thenReturn(Optional.of(productOther));
        lenient().when(outcomeRepository.findById(eq(OUTCOME_A))).thenReturn(Optional.of(outcomeA));
        lenient().when(opportunityRepository.findById(eq(OPPORTUNITY_A))).thenReturn(Optional.of(opportunityA));
        lenient().when(solutionRepository.findById(eq(SOLUTION_A))).thenReturn(Optional.of(solutionA));
        lenient().when(assumptionRepository.findById(eq(ASSUMPTION_A))).thenReturn(Optional.of(assumptionA));
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
    // Product-level checks
    // ---------------------------------------------------------------------

    @Test
    void productChecksResolveToOwningTeamForOwner() {
        authenticate();
        stubMembership(TeamRole.OWNER, teamA);

        assertThat(service.canReadProduct(PRODUCT_A)).isTrue();
        assertThat(service.canEditProduct(PRODUCT_A)).isTrue();
        assertThat(service.isProductOwner(PRODUCT_A)).isTrue();
    }

    @Test
    void productChecksDenyEditorAccessToOtherTeamsProduct() {
        authenticate();
        stubMembership(TeamRole.EDITOR, teamA);

        assertThat(service.canReadProduct(PRODUCT_OTHER)).isFalse();
        assertThat(service.canEditProduct(PRODUCT_OTHER)).isFalse();
        assertThat(service.isProductOwner(PRODUCT_OTHER)).isFalse();
    }

    @Test
    void productChecksAllowViewerToReadNotEdit() {
        authenticate();
        stubMembership(TeamRole.VIEWER, teamA);

        assertThat(service.canReadProduct(PRODUCT_A)).isTrue();
        assertThat(service.canEditProduct(PRODUCT_A)).isFalse();
        assertThat(service.isProductOwner(PRODUCT_A)).isFalse();
    }

    @Test
    void productChecksDenyNonMember() {
        authenticate();
        stubNoMemberships();

        assertThat(service.canReadProduct(PRODUCT_A)).isFalse();
        assertThat(service.canEditProduct(PRODUCT_A)).isFalse();
    }

    @Test
    void missingProductIsIndistinguishableFromNoMembership() {
        authenticate();
        when(productRepository.findById(eq(12345L))).thenReturn(Optional.empty());

        assertThat(service.canReadProduct(12345L)).isFalse();
        assertThatThrownBy(() -> service.requireReadProduct(12345L)).isInstanceOf(TeamAccessDeniedException.class);
    }

    // ---------------------------------------------------------------------
    // Node-level checks (outcome, opportunity, solution, assumption)
    // ---------------------------------------------------------------------

    @Test
    void nodeChecksResolveThroughFullChainForEditor() {
        authenticate();
        stubMembership(TeamRole.EDITOR, teamA);

        assertThat(service.canEditOutcome(OUTCOME_A)).isTrue();
        assertThat(service.canEditOpportunity(OPPORTUNITY_A)).isTrue();
        assertThat(service.canEditSolution(SOLUTION_A)).isTrue();
        assertThat(service.canEditAssumption(ASSUMPTION_A)).isTrue();
    }

    @Test
    void nodeChecksDenyEditForViewer() {
        authenticate();
        stubMembership(TeamRole.VIEWER, teamA);

        assertThat(service.canReadOutcome(OUTCOME_A)).isTrue();
        assertThat(service.canEditOutcome(OUTCOME_A)).isFalse();
        assertThat(service.canEditOpportunity(OPPORTUNITY_A)).isFalse();
        assertThat(service.canEditSolution(SOLUTION_A)).isFalse();
        assertThat(service.canEditAssumption(ASSUMPTION_A)).isFalse();
    }

    @Test
    void nodeChecksDenyAllForNonMember() {
        authenticate();
        stubNoMemberships();

        assertThat(service.canReadOutcome(OUTCOME_A)).isFalse();
        assertThat(service.canReadOpportunity(OPPORTUNITY_A)).isFalse();
        assertThat(service.canReadSolution(SOLUTION_A)).isFalse();
        assertThat(service.canReadAssumption(ASSUMPTION_A)).isFalse();
    }

    @Test
    void ownerCanOwnAllNodesInTheirTeam() {
        authenticate();
        stubMembership(TeamRole.OWNER, teamA);

        assertThat(service.isOutcomeOwner(OUTCOME_A)).isTrue();
        assertThat(service.isOpportunityOwner(OPPORTUNITY_A)).isTrue();
        assertThat(service.isSolutionOwner(SOLUTION_A)).isTrue();
        assertThat(service.isAssumptionOwner(ASSUMPTION_A)).isTrue();
    }

    // ---------------------------------------------------------------------
    // Multi-team: owner in team A, viewer in team B
    // ---------------------------------------------------------------------

    @Test
    void userCanBeOwnerInOneTeamAndViewerInAnother() {
        authenticate();
        TeamMember ownerInA = membership(TeamRole.OWNER, teamA);
        TeamMember viewerInB = membership(TeamRole.VIEWER, teamB);
        when(teamMemberRepository.findAllByUserLogin(LOGIN)).thenReturn(List.of(ownerInA, viewerInB));

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
        assertThatThrownBy(() -> service.requireReadProduct(PRODUCT_A)).isInstanceOf(TeamAccessDeniedException.class);
        assertThatThrownBy(() -> service.requireEditOutcome(OUTCOME_A)).isInstanceOf(TeamAccessDeniedException.class);
    }

    @Test
    void requireVariantsPassForOwner() {
        authenticate();
        stubMembership(TeamRole.OWNER, teamA);

        service.requireReadTeam(TEAM_A);
        service.requireEditTeam(TEAM_A);
        service.requireOwnerTeam(TEAM_A);
        service.requireReadProduct(PRODUCT_A);
        service.requireEditProduct(PRODUCT_A);
        service.requireOwnerProduct(PRODUCT_A);
        service.requireReadOutcome(OUTCOME_A);
        service.requireEditOutcome(OUTCOME_A);
    }

    @Test
    void requireEditThrowsForViewer() {
        authenticate();
        stubMembership(TeamRole.VIEWER, teamA);

        service.requireReadTeam(TEAM_A); // read is fine
        assertThatThrownBy(() -> service.requireEditTeam(TEAM_A)).isInstanceOf(TeamAccessDeniedException.class);
        assertThatThrownBy(() -> service.requireOwnerTeam(TEAM_A)).isInstanceOf(TeamAccessDeniedException.class);
        assertThatThrownBy(() -> service.requireEditOpportunity(OPPORTUNITY_A)).isInstanceOf(TeamAccessDeniedException.class);
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
        when(teamMemberRepository.findAllByUserLogin(LOGIN)).thenReturn(
            List.of(membership(TeamRole.OWNER, teamA), membership(TeamRole.VIEWER, teamB))
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
    // Helpers
    // ---------------------------------------------------------------------

    private void authenticate() {
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(new UsernamePasswordAuthenticationToken(LOGIN, "n/a"));
        SecurityContextHolder.setContext(ctx);
    }

    private void stubMembership(TeamRole role, Team team) {
        when(teamMemberRepository.findAllByUserLogin(LOGIN)).thenReturn(List.of(membership(role, team)));
    }

    private void stubNoMemberships() {
        when(teamMemberRepository.findAllByUserLogin(LOGIN)).thenReturn(List.of());
    }

    private TeamMember membership(TeamRole role, Team team) {
        TeamMember tm = new TeamMember();
        tm.setRole(role);
        tm.setTeam(team);
        User user = new User();
        user.setLogin(LOGIN);
        tm.setUser(user);
        return tm;
    }

    private static Team team(Long id) {
        Team t = new Team();
        t.setId(id);
        t.setName("Team " + id);
        return t;
    }
}
