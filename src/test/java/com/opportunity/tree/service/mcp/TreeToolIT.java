package com.opportunity.tree.service.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.opportunity.tree.IntegrationTest;
import com.opportunity.tree.domain.Opportunity;
import com.opportunity.tree.domain.Outcome;
import com.opportunity.tree.domain.Product;
import com.opportunity.tree.domain.Solution;
import com.opportunity.tree.domain.Team;
import com.opportunity.tree.domain.TeamMember;
import com.opportunity.tree.domain.User;
import com.opportunity.tree.domain.enumeration.OpportunityStatus;
import com.opportunity.tree.domain.enumeration.SolutionStatus;
import com.opportunity.tree.domain.enumeration.TeamRole;
import com.opportunity.tree.repository.UserRepository;
import com.opportunity.tree.service.TeamAccessDeniedException;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for {@link TreeTool} that wire the real {@code ProductService} and
 * {@code TeamTreeService} beans (which in turn go through {@code TeamAccessService}),
 * seed two teams, and exercise every acceptance criterion the mock-only
 * {@link TreeToolTest} cannot verify: that a member of one team sees only that team's
 * data, that a caller in no team gets an empty list, and that a cross-team id yields
 * {@link TeamAccessDeniedException} — the same exception (and therefore the same HTTP
 * 403 shape) as an unknown id, so ids cannot be probed for existence (NFR-002).
 *
 * <p>ROLE_OVERVIEW does not exist in this codebase (see acceptance criterion 9 and the
 * repository grep in the MCPSRV-003 ticket notes), so the overview leg is not
 * applicable here.
 */
@IntegrationTest
@Transactional
class TreeToolIT {

    private static final String MEMBER_A_LOGIN = "mcp-member-a";
    private static final String MEMBER_B_LOGIN = "mcp-member-b";
    private static final String OUTSIDER_LOGIN = "mcp-outsider";

    @Autowired
    private EntityManager em;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TreeTool tool;

    private Team teamA;
    private Team teamB;
    private Product productA1;
    private Product productA2;
    private Product productB1;

    @BeforeEach
    void seed() {
        teamA = persistTeam("Alpha");
        teamB = persistTeam("Bravo");

        User memberA = persistUser(MEMBER_A_LOGIN);
        User memberB = persistUser(MEMBER_B_LOGIN);
        persistUser(OUTSIDER_LOGIN);

        persistMembership(teamA, memberA, TeamRole.OWNER);
        persistMembership(teamB, memberB, TeamRole.OWNER);

        productA1 = persistProduct(teamA, "Checkout", 0);
        productA2 = persistProduct(teamA, "Search", 1);
        productB1 = persistProduct(teamB, "Onboarding", 0);

        Outcome outcomeA = persistOutcome(productA1, "Higher activation", 0);
        Opportunity oppA = persistOpportunity(outcomeA, null, "Signup friction", 0);
        persistSolution(oppA, "One-tap signup", 0);

        Outcome outcomeB = persistOutcome(productB1, "Reduce churn", 0);
        persistOpportunity(outcomeB, null, "Confusing empty state", 0);

        em.flush();
        em.clear();
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    // ---------------------------------------------------------------
    // AC 9: list_products — one-team member sees only that team's data
    // ---------------------------------------------------------------

    @Test
    void listProducts_memberOfTeamASeesOnlyTeamAProducts() {
        authenticate(MEMBER_A_LOGIN);

        TreeTool.ProductListResponse response = tool.listProducts();

        assertThat(response.products()).extracting(TreeTool.ProductSummary::teamId).containsOnly(teamA.getId());
        assertThat(response.products()).extracting(TreeTool.ProductSummary::title).containsExactlyInAnyOrder("Checkout", "Search");
        // Team B's product must not leak.
        assertThat(response.products()).extracting(TreeTool.ProductSummary::id).doesNotContain(productB1.getId());
    }

    // ---------------------------------------------------------------
    // AC 9: list_products — caller in no team gets an empty list
    // ---------------------------------------------------------------

    @Test
    void listProducts_forCallerInNoTeam_returnsEmptyList() {
        authenticate(OUTSIDER_LOGIN);

        TreeTool.ProductListResponse response = tool.listProducts();

        assertThat(response.total()).isZero();
        assertThat(response.products()).isEmpty();
    }

    // ---------------------------------------------------------------
    // AC 9: get_tree — one-team member sees only that team's data
    // ---------------------------------------------------------------

    @Test
    void getTree_memberOfTeamASeesOnlyTeamATree() {
        authenticate(MEMBER_A_LOGIN);

        TreeTool.TreeResponse response = tool.getTree(teamA.getId(), null);

        assertThat(response.overflow()).isFalse();
        assertThat(response.teamId()).isEqualTo(teamA.getId());
        assertThat(response.teamName()).isEqualTo("Alpha");
        assertThat(response.nodes())
            .extracting(TreeTool.TreeNode::title)
            .contains("Checkout", "Search", "Higher activation", "Signup friction");
        // Team B fixture titles must not leak.
        assertThat(response.nodes())
            .extracting(TreeTool.TreeNode::title)
            .doesNotContain("Onboarding", "Reduce churn", "Confusing empty state");
    }

    // ---------------------------------------------------------------
    // AC 9: get_tree — caller in no team is refused (403 shape)
    // ---------------------------------------------------------------

    @Test
    void getTree_forCallerInNoTeam_isRefused() {
        authenticate(OUTSIDER_LOGIN);

        assertThatThrownBy(() -> tool.getTree(teamA.getId(), null)).isInstanceOf(TeamAccessDeniedException.class);
    }

    // ---------------------------------------------------------------
    // AC 3/9: get_tree — a team the caller does not belong to is refused
    // ---------------------------------------------------------------

    @Test
    void getTree_forTeamTheCallerDoesNotBelongTo_isRefused() {
        authenticate(MEMBER_A_LOGIN);

        assertThatThrownBy(() -> tool.getTree(teamB.getId(), null)).isInstanceOf(TeamAccessDeniedException.class);
    }

    // ---------------------------------------------------------------
    // AC 4/9: cross-team id and unknown id are refused with the same
    // shape as a non-member team, so ids cannot be probed for existence
    // ---------------------------------------------------------------

    @Test
    void getTree_withProductIdFromAnotherTeam_isRefusedSameAsUnknownId() {
        authenticate(MEMBER_A_LOGIN);

        // productB1 belongs to team B — the caller is only a member of team A.
        Throwable crossTeam = catchThrowable(() -> tool.getTree(teamA.getId(), productB1.getId()));
        // An id that exists nowhere.
        Throwable unknown = catchThrowable(() -> tool.getTree(teamA.getId(), 424_242L));

        assertThat(crossTeam).isInstanceOf(TeamAccessDeniedException.class);
        assertThat(unknown).isInstanceOf(TeamAccessDeniedException.class);
        assertThat(crossTeam.getMessage()).isEqualTo(unknown.getMessage());
    }

    // ---------------------------------------------------------------
    // AC 2: get_tree with a productId returns only that product's branch
    // (and confirms teamA's own products are still callable by id).
    // ---------------------------------------------------------------

    @Test
    void getTree_withProductIdOwnedByTheTeam_returnsOnlyThatBranch() {
        authenticate(MEMBER_A_LOGIN);

        TreeTool.TreeResponse response = tool.getTree(teamA.getId(), productA1.getId());

        assertThat(response.overflow()).isFalse();
        assertThat(response.nodes())
            .extracting(TreeTool.TreeNode::title)
            .contains("Checkout", "Higher activation", "Signup friction", "One-tap signup");
        // The other product in team A is excluded when a productId is passed.
        assertThat(response.nodes()).extracting(TreeTool.TreeNode::title).doesNotContain("Search");
        // And nothing from team B appears either.
        assertThat(response.nodes()).extracting(TreeTool.TreeNode::title).doesNotContain("Onboarding");
        // productA2 is the sibling product; a productId scoped call excludes it.
        assertThat(response.nodes()).extracting(TreeTool.TreeNode::id).doesNotContain(productA2.getId());
    }

    @Test
    void getTree_exposesNestedOpportunityPriorityAndAllHighestScoreTies() {
        Outcome outcome = em
            .createQuery("select o from Outcome o where o.product.id = :id", Outcome.class)
            .setParameter("id", productA1.getId())
            .getSingleResult();
        Opportunity parent = em
            .createQuery("select o from Opportunity o where o.outcome.id = :id", Opportunity.class)
            .setParameter("id", outcome.getId())
            .getSingleResult();
        parent.setPriority(80);
        Opportunity nested = persistOpportunity(outcome, parent, "Nested priority", 1);
        nested.setPriority(95);
        Opportunity sibling = persistOpportunity(outcome, null, "Tied priority", 2);
        sibling.setPriority(95);
        em.flush();
        em.clear();
        authenticate(MEMBER_A_LOGIN);

        TreeTool.TreeResponse response = tool.getTree(teamA.getId(), productA1.getId(), 0);
        int highest = response
            .nodes()
            .stream()
            .filter(n -> "OPPORTUNITY".equals(n.type()))
            .mapToInt(TreeTool.TreeNode::priority)
            .max()
            .orElseThrow();
        assertThat(highest).isEqualTo(95);
        assertThat(
            response
                .nodes()
                .stream()
                .filter(n -> "OPPORTUNITY".equals(n.type()) && n.priority() == highest)
                .map(TreeTool.TreeNode::title)
        ).containsExactlyInAnyOrder("Nested priority", "Tied priority");
        assertThat(
            response
                .nodes()
                .stream()
                .filter(n -> n.id().equals(nested.getId()) && "OPPORTUNITY".equals(n.type()))
                .findFirst()
                .orElseThrow()
                .parentId()
        ).isEqualTo(parent.getId());
    }

    @Test
    void getTree_largeProductCanBeReadCompletelyInStablePages() {
        for (int i = 0; i < 1001; i++) {
            persistOutcome(productA1, "Bulk outcome " + i, i + 10);
        }
        em.flush();
        em.clear();
        authenticate(MEMBER_A_LOGIN);

        TreeTool.TreeResponse first = tool.getTree(teamA.getId(), productA1.getId(), 0);
        TreeTool.TreeResponse second = tool.getTree(teamA.getId(), productA1.getId(), first.nodeCount());
        assertThat(first.nodeCount()).isEqualTo(1000);
        assertThat(first.hasMore()).isTrue();
        assertThat(second.hasMore()).isFalse();
        assertThat(first.totalNodes()).isEqualTo((long) first.nodeCount() + second.nodeCount());
        assertThat(
            first
                .nodes()
                .stream()
                .map(n -> n.type() + ":" + n.id())
                .toList()
        ).doesNotContainAnyElementsOf(
            second
                .nodes()
                .stream()
                .map(n -> n.type() + ":" + n.id())
                .toList()
        );
        assertThat(tool.getTree(teamA.getId(), null).overflow()).isTrue();
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private static Throwable catchThrowable(Runnable r) {
        try {
            r.run();
            return null;
        } catch (Throwable t) {
            return t;
        }
    }

    private Team persistTeam(String name) {
        Team t = new Team().name(name).description("d").createdDate(Instant.now());
        em.persist(t);
        em.flush();
        return t;
    }

    private User persistUser(String login) {
        return userRepository
            .findOneByLogin(login)
            .orElseGet(() -> {
                User u = new User();
                u.setId(UUID.randomUUID().toString());
                u.setLogin(login);
                u.setActivated(true);
                u.setEmail(login + "@example.com");
                u.setFirstName(login);
                u.setLastName("test");
                u.setLangKey("en");
                em.persist(u);
                em.flush();
                return u;
            });
    }

    private void persistMembership(Team t, User u, TeamRole role) {
        TeamMember tm = new TeamMember().role(role).joinedDate(Instant.now());
        tm.setTeam(t);
        tm.setUser(u);
        em.persist(tm);
        em.flush();
    }

    private Product persistProduct(Team team, String name, int sortOrder) {
        Product p = new Product().name(name).description("d").archived(false).sortOrder(sortOrder).createdDate(Instant.now()).team(team);
        em.persist(p);
        return p;
    }

    private Outcome persistOutcome(Product product, String title, int sortOrder) {
        Outcome o = new Outcome().title(title).sortOrder(sortOrder).createdDate(Instant.now()).product(product);
        em.persist(o);
        return o;
    }

    private Opportunity persistOpportunity(Outcome outcome, Opportunity parent, String title, int sortOrder) {
        Opportunity op = new Opportunity()
            .title(title)
            .status(OpportunityStatus.UNEXPLORED)
            .valuerating(3)
            .priority(50)
            .sortOrder(sortOrder)
            .createdDate(Instant.now())
            .outcome(outcome)
            .parent(parent);
        em.persist(op);
        return op;
    }

    private Solution persistSolution(Opportunity opportunity, String title, int sortOrder) {
        Solution s = new Solution()
            .title(title)
            .status(SolutionStatus.CANDIDATE)
            .sortOrder(sortOrder)
            .createdDate(Instant.now())
            .opportunity(opportunity);
        em.persist(s);
        return s;
    }

    private static void authenticate(String login) {
        var auth = new UsernamePasswordAuthenticationToken(login, "n/a", List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
