package com.opportunity.tree.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.opportunity.tree.repository.OpportunityRepository;
import com.opportunity.tree.repository.OutcomeRepository;
import com.opportunity.tree.repository.ProductRepository;
import com.opportunity.tree.repository.SolutionRepository;
import com.opportunity.tree.repository.TeamMemberRepository;
import com.opportunity.tree.repository.TeamRepository;
import com.opportunity.tree.repository.UserRepository;
import com.opportunity.tree.service.TeamTreeService;
import com.opportunity.tree.service.dto.tree.OpportunityTreeNodeDTO;
import com.opportunity.tree.service.dto.tree.OutcomeTreeNodeDTO;
import com.opportunity.tree.service.dto.tree.ProductTreeNodeDTO;
import com.opportunity.tree.service.dto.tree.TeamTreeDTO;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for {@link TeamTreeResource} / {@link TeamTreeService}
 * covering the TREE-001 acceptance criteria.
 */
@IntegrationTest
@AutoConfigureMockMvc
class TeamTreeResourceIT {

    private static final String OWNER_LOGIN = "tree-owner";
    private static final String EDITOR_LOGIN = "tree-editor";
    private static final String VIEWER_LOGIN = "tree-viewer";
    private static final String OUTSIDER_LOGIN = "tree-outsider";

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TeamMemberRepository teamMemberRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OutcomeRepository outcomeRepository;

    @Autowired
    private OpportunityRepository opportunityRepository;

    @Autowired
    private SolutionRepository solutionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeamTreeService teamTreeService;

    private Team teamA;
    private Team teamB;
    private User ownerUser;
    private User editorUser;
    private User viewerUser;
    private User outsiderUser;

    @BeforeEach
    void setUp() {
        // Ensure a clean slate — other tests may leave rows behind.
        solutionRepository.deleteAll();
        opportunityRepository.deleteAll();
        outcomeRepository.deleteAll();
        productRepository.deleteAll();
        teamMemberRepository.deleteAll();
        teamRepository.deleteAll();

        teamA = persistTeam("Tree Team A");
        teamB = persistTeam("Tree Team B");

        ownerUser = persistUser(OWNER_LOGIN);
        editorUser = persistUser(EDITOR_LOGIN);
        viewerUser = persistUser(VIEWER_LOGIN);
        outsiderUser = persistUser(OUTSIDER_LOGIN);

        persistMembership(teamA, ownerUser, TeamRole.OWNER);
        persistMembership(teamA, editorUser, TeamRole.EDITOR);
        persistMembership(teamA, viewerUser, TeamRole.VIEWER);
        persistMembership(teamB, ownerUser, TeamRole.OWNER);
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
        solutionRepository.deleteAll();
        opportunityRepository.deleteAll();
        outcomeRepository.deleteAll();
        productRepository.deleteAll();
        teamMemberRepository.deleteAll();
        teamRepository.deleteAll();
        userRepository.delete(ownerUser);
        userRepository.delete(editorUser);
        userRepository.delete(viewerUser);
        userRepository.delete(outsiderUser);
    }

    // ---------------------------------------------------------------
    // Criterion 5: authorisation via TeamAccessService
    // ---------------------------------------------------------------

    @Test
    @Transactional
    void unauthenticatedCallerGets401() throws Exception {
        // No .with(user(...)) — filter chain must reject unauthenticated.
        mvc.perform(get("/api/teams/{teamId}/tree", teamA.getId())).andExpect(status().isUnauthorized());
    }

    @Test
    @Transactional
    void ownerCanReadTree() throws Exception {
        seedSmallTree(teamA);
        mvc
            .perform(get("/api/teams/{teamId}/tree", teamA.getId()).with(user(OWNER_LOGIN)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.currentUserRole").value("OWNER"))
            .andExpect(jsonPath("$.canEdit").value(true))
            .andExpect(jsonPath("$.products[0].outcomes[0].opportunities[0].solutions[0].id").exists());
    }

    @Test
    @Transactional
    void editorCanReadTreeAndCanEdit() throws Exception {
        seedSmallTree(teamA);
        mvc
            .perform(get("/api/teams/{teamId}/tree", teamA.getId()).with(user(EDITOR_LOGIN)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.currentUserRole").value("EDITOR"))
            .andExpect(jsonPath("$.canEdit").value(true));
    }

    @Test
    @Transactional
    void viewerCanReadTreeButCannotEdit() throws Exception {
        seedSmallTree(teamA);
        mvc
            .perform(get("/api/teams/{teamId}/tree", teamA.getId()).with(user(VIEWER_LOGIN)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.currentUserRole").value("VIEWER"))
            .andExpect(jsonPath("$.canEdit").value(false));
    }

    @Test
    @Transactional
    void nonMemberGets403() throws Exception {
        seedSmallTree(teamA);
        mvc.perform(get("/api/teams/{teamId}/tree", teamA.getId()).with(user(OUTSIDER_LOGIN))).andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------
    // Criteria 1, 2, 3, 6: response shape, ordering, id/status fields,
    // and only nodes from the requested team.
    // ---------------------------------------------------------------

    @Test
    @Transactional
    void treeContainsAllTypesOrderedBySortOrderAndOnlyForRequestedTeam() {
        // Team A: 2 products, each with outcomes → opportunities → solutions.
        Product productA1 = persistProduct(teamA, "A1", false);
        Product productA2 = persistProduct(teamA, "A2", true); // archived
        Outcome outcomeA1a = persistOutcome(productA1, "A1-outcome-b", 20);
        Outcome outcomeA1b = persistOutcome(productA1, "A1-outcome-a", 10);
        Opportunity opp1 = persistOpportunity(outcomeA1a, null, "opp-b", 20);
        Opportunity opp2 = persistOpportunity(outcomeA1a, null, "opp-a", 10);
        Opportunity childOpp = persistOpportunity(outcomeA1a, opp1, "child", 5);
        Solution sol1 = persistSolution(opp1, "sol-b", 20);
        Solution sol2 = persistSolution(opp1, "sol-a", 10);

        // Team B: unrelated product & outcome the caller must never see.
        Product productB = persistProduct(teamB, "B1", false);
        persistOutcome(productB, "B-outcome", 1);

        em.flush();
        em.clear();

        authenticate(OWNER_LOGIN);
        TeamTreeDTO tree = teamTreeService.getTreeForTeam(teamA.getId());

        assertThat(tree.getId()).isEqualTo(teamA.getId());
        assertThat(tree.getProducts()).hasSize(2);

        // Criterion 6: response only contains team A nodes.
        assertThat(tree.getProducts())
            .extracting(ProductTreeNodeDTO::getId)
            .containsExactlyInAnyOrder(productA1.getId(), productA2.getId())
            .doesNotContain(productB.getId());

        ProductTreeNodeDTO productDto = tree
            .getProducts()
            .stream()
            .filter(p -> p.getId().equals(productA1.getId()))
            .findFirst()
            .orElseThrow();
        assertThat(productDto.getArchived()).isFalse();

        // Criterion 2: outcomes ordered by sortOrder ASC.
        assertThat(productDto.getOutcomes()).extracting(OutcomeTreeNodeDTO::getId).containsExactly(outcomeA1b.getId(), outcomeA1a.getId());

        OutcomeTreeNodeDTO outcomeDto = productDto
            .getOutcomes()
            .stream()
            .filter(o -> o.getId().equals(outcomeA1a.getId()))
            .findFirst()
            .orElseThrow();

        // Criterion 2 + 3: opportunities ordered by sortOrder ASC and status is present.
        assertThat(outcomeDto.getOpportunities()).extracting(OpportunityTreeNodeDTO::getId).containsExactly(opp2.getId(), opp1.getId());
        assertThat(outcomeDto.getOpportunities()).allSatisfy(op -> assertThat(op.getStatus()).isNotNull());

        // Nested child opportunity attached under its parent.
        OpportunityTreeNodeDTO opp1Dto = outcomeDto
            .getOpportunities()
            .stream()
            .filter(o -> o.getId().equals(opp1.getId()))
            .findFirst()
            .orElseThrow();
        assertThat(opp1Dto.getChildren()).extracting(OpportunityTreeNodeDTO::getId).containsExactly(childOpp.getId());
        assertThat(opp1Dto.getChildren().get(0).getParentId()).isEqualTo(opp1.getId());

        // Criterion 2: solutions ordered by sortOrder ASC.
        assertThat(opp1Dto.getSolutions())
            .extracting(s -> s.getId())
            .containsExactly(sol2.getId(), sol1.getId());
    }

    @Test
    @Transactional
    void archivedFlagAndStatusesArePresentOnEveryNode() {
        Product p = persistProduct(teamA, "PP", true);
        Outcome o = persistOutcome(p, "OO", 1);
        Opportunity op = persistOpportunity(o, null, "Op", 1);
        persistSolution(op, "Sol", 1);
        em.flush();
        em.clear();

        authenticate(OWNER_LOGIN);
        TeamTreeDTO tree = teamTreeService.getTreeForTeam(teamA.getId());
        ProductTreeNodeDTO pd = tree.getProducts().get(0);
        assertThat(pd.getArchived()).isTrue();
        assertThat(pd.getOutcomes()).hasSize(1);
        assertThat(pd.getOutcomes().get(0).getOpportunities().get(0).getStatus()).isNotNull();
        assertThat(pd.getOutcomes().get(0).getOpportunities().get(0).getSolutions().get(0).getStatus()).isNotNull();
    }

    // ---------------------------------------------------------------
    // Criterion 7: bounded number of queries independent of node count.
    // Uses a 3-level-deep opportunity nesting.
    // ---------------------------------------------------------------

    @Test
    @Transactional
    void queryCountIsBoundedForDeeplyNestedTree() {
        Product p = persistProduct(teamA, "PP", false);
        Outcome o = persistOutcome(p, "OO", 1);
        Opportunity level1 = persistOpportunity(o, null, "L1", 1);
        Opportunity level2 = persistOpportunity(o, level1, "L2", 1);
        Opportunity level3 = persistOpportunity(o, level2, "L3", 1);
        Opportunity level4 = persistOpportunity(o, level3, "L4", 1);
        persistSolution(level3, "s1", 1);
        persistSolution(level4, "s2", 1);
        // Extra fan-out to prove query count is independent of node count.
        for (int i = 0; i < 20; i++) {
            Opportunity leaf = persistOpportunity(o, level1, "leaf" + i, 100 + i);
            persistSolution(leaf, "leaf-sol" + i, 1);
        }
        em.flush();
        em.clear();

        Statistics stats = em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();

        authenticate(OWNER_LOGIN);
        TeamTreeDTO tree = teamTreeService.getTreeForTeam(teamA.getId());

        long queries = stats.getPrepareStatementCount();
        assertThat(queries).as("Tree assembly must use a bounded number of queries; observed %d", queries).isLessThanOrEqualTo(10);

        // Deepest node still reachable.
        OpportunityTreeNodeDTO l1 = tree.getProducts().get(0).getOutcomes().get(0).getOpportunities().get(0);
        OpportunityTreeNodeDTO l2 = l1
            .getChildren()
            .stream()
            .filter(c -> c.getId().equals(level2.getId()))
            .findFirst()
            .orElseThrow();
        OpportunityTreeNodeDTO l3 = l2.getChildren().get(0);
        assertThat(l3.getChildren()).extracting(OpportunityTreeNodeDTO::getId).contains(level4.getId());
    }

    // ---------------------------------------------------------------
    // Criterion 8: ~500 nodes loads in well under 2 seconds.
    // ---------------------------------------------------------------

    @Test
    @Transactional
    void largeTreeLoadsQuickly() {
        // ~500 nodes: 5 products × (5 outcomes × (10 opportunities × 1 solution)) ≈ 505.
        int productCount = 5;
        int outcomesPerProduct = 5;
        int opportunitiesPerOutcome = 10;
        int total = 0;
        for (int pi = 0; pi < productCount; pi++) {
            Product product = persistProduct(teamA, "P" + pi, false);
            total++;
            for (int oi = 0; oi < outcomesPerProduct; oi++) {
                Outcome outcome = persistOutcome(product, "O" + pi + "-" + oi, oi);
                total++;
                for (int qi = 0; qi < opportunitiesPerOutcome; qi++) {
                    Opportunity opp = persistOpportunity(outcome, null, "Op" + pi + "-" + oi + "-" + qi, qi);
                    total++;
                    persistSolution(opp, "Sol" + pi + "-" + oi + "-" + qi, 1);
                    total++;
                }
            }
        }
        em.flush();
        em.clear();

        assertThat(total).isGreaterThanOrEqualTo(500);

        authenticate(OWNER_LOGIN);
        long start = System.nanoTime();
        TeamTreeDTO tree = teamTreeService.getTreeForTeam(teamA.getId());
        long elapsedMs = (System.nanoTime() - start) / 1_000_000L;

        assertThat(tree.getProducts()).hasSize(productCount);
        assertThat(elapsedMs).as("500-node tree must load well under 2 s; took %d ms", elapsedMs).isLessThan(2000L);
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private void seedSmallTree(Team team) {
        Product product = persistProduct(team, "Prod", false);
        Outcome outcome = persistOutcome(product, "OO", 1);
        Opportunity opp = persistOpportunity(outcome, null, "Op", 1);
        persistSolution(opp, "Sol", 1);
        em.flush();
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

    private Product persistProduct(Team team, String name, boolean archived) {
        Product p = new Product().name(name).description("d").archived(archived).sortOrder(0).createdDate(Instant.now()).team(team);
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
        var auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(login, "n/a", List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
