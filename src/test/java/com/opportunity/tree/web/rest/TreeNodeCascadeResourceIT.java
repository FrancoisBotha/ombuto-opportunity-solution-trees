package com.opportunity.tree.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.opportunity.tree.IntegrationTest;
import com.opportunity.tree.domain.Assumption;
import com.opportunity.tree.domain.Experiment;
import com.opportunity.tree.domain.Interview;
import com.opportunity.tree.domain.Opportunity;
import com.opportunity.tree.domain.Outcome;
import com.opportunity.tree.domain.Product;
import com.opportunity.tree.domain.Solution;
import com.opportunity.tree.domain.Team;
import com.opportunity.tree.domain.TeamMember;
import com.opportunity.tree.domain.User;
import com.opportunity.tree.domain.enumeration.AssumptionCategory;
import com.opportunity.tree.domain.enumeration.ExperimentStatus;
import com.opportunity.tree.domain.enumeration.OpportunityStatus;
import com.opportunity.tree.domain.enumeration.OutcomeStatus;
import com.opportunity.tree.domain.enumeration.SolutionStatus;
import com.opportunity.tree.domain.enumeration.TeamRole;
import com.opportunity.tree.repository.OpportunityRepository;
import com.opportunity.tree.repository.OutcomeRepository;
import com.opportunity.tree.repository.ProductRepository;
import com.opportunity.tree.repository.SolutionRepository;
import com.opportunity.tree.repository.TeamMemberRepository;
import com.opportunity.tree.repository.TeamRepository;
import com.opportunity.tree.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for {@link TreeNodeCascadeResource} covering the TREE-003
 * acceptance criteria: cascade delete of each node type, transactional atomicity,
 * team-scoped authorisation, and isolation from sibling / other-team nodes.
 */
@IntegrationTest
@AutoConfigureMockMvc
class TreeNodeCascadeResourceIT {

    private static final String OWNER_LOGIN = "cascade-owner";
    private static final String EDITOR_LOGIN = "cascade-editor";
    private static final String VIEWER_LOGIN = "cascade-viewer";
    private static final String OUTSIDER_LOGIN = "cascade-outsider";

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

    private Team teamA;
    private Team teamB;
    private User ownerUser;
    private User editorUser;
    private User viewerUser;
    private User outsiderUser;

    @BeforeEach
    void setUp() {
        teamA = persistTeam("Cascade Team A");
        teamB = persistTeam("Cascade Team B");

        ownerUser = persistUser(OWNER_LOGIN);
        editorUser = persistUser(EDITOR_LOGIN);
        viewerUser = persistUser(VIEWER_LOGIN);
        outsiderUser = persistUser(OUTSIDER_LOGIN);

        persistMembership(teamA, ownerUser, TeamRole.OWNER);
        persistMembership(teamA, editorUser, TeamRole.EDITOR);
        persistMembership(teamA, viewerUser, TeamRole.VIEWER);
    }

    @AfterEach
    void cleanup() {
        // Each @Test is @Transactional; the test framework rolls back the
        // whole transaction, which removes everything persisted here — no
        // explicit deleteAll needed and doing one here would trigger an
        // auto-flush of the cascade-deleted persistence context.
    }

    // ---------------------------------------------------------------
    // Fixture: two products in team A (one full 3-level tree, one sibling),
    // plus a whole tree in team B that must remain untouched.
    //
    //  teamA
    //    productA1
    //      outcome1
    //        opp1
    //          opp2
    //            opp3          <- 3 levels deep
    //              sol1
    //            sol2
    //          sol3
    //        siblingOpp        <- sibling under same outcome
    //      siblingOutcome      <- sibling under productA1
    //    productA2 (untouched sibling product)
    //  teamB
    //    productB              (untouched other-team tree)
    // ---------------------------------------------------------------
    private static class Fixture {

        Product productA1;
        Product productA2;
        Outcome outcome1;
        Outcome siblingOutcome;
        Opportunity opp1;
        Opportunity opp2;
        Opportunity opp3;
        Opportunity siblingOpp;
        Solution sol1;
        Solution sol2;
        Solution sol3;

        Product productB;
        Outcome outcomeB;
        Opportunity oppB;
        Solution solB;
    }

    private Fixture seedFullFixture() {
        Fixture f = new Fixture();
        f.productA1 = persistProduct(teamA, "A-prod-1", false);
        f.productA2 = persistProduct(teamA, "A-prod-2", false);
        f.outcome1 = persistOutcome(f.productA1, "outcome-1", 1);
        f.siblingOutcome = persistOutcome(f.productA1, "sibling-outcome", 2);
        f.opp1 = persistOpportunity(f.outcome1, null, "opp-1", 1);
        f.opp2 = persistOpportunity(f.outcome1, f.opp1, "opp-2", 1);
        f.opp3 = persistOpportunity(f.outcome1, f.opp2, "opp-3", 1);
        f.siblingOpp = persistOpportunity(f.outcome1, null, "sibling-opp", 2);
        f.sol1 = persistSolution(f.opp3, "sol-1", 1);
        f.sol2 = persistSolution(f.opp2, "sol-2", 1);
        f.sol3 = persistSolution(f.opp1, "sol-3", 1);

        f.productB = persistProduct(teamB, "B-prod", false);
        f.outcomeB = persistOutcome(f.productB, "b-outcome", 1);
        f.oppB = persistOpportunity(f.outcomeB, null, "b-opp", 1);
        f.solB = persistSolution(f.oppB, "b-sol", 1);

        em.flush();
        em.clear();
        return f;
    }

    // ---------------------------------------------------------------
    // AC1: deleting a Product cascades outcomes → opportunities → solutions.
    // ---------------------------------------------------------------

    @Test
    @Transactional
    void ownerDeletesProductCascadesEntireSubtree() throws Exception {
        Fixture f = seedFullFixture();
        mvc
            .perform(delete("/api/tree/products/{id}", f.productA1.getId()).with(user(OWNER_LOGIN)).with(csrf()))
            .andExpect(status().isNoContent());

        assertThat(productRepository.existsById(f.productA1.getId())).isFalse();
        assertThat(outcomeRepository.existsById(f.outcome1.getId())).isFalse();
        assertThat(outcomeRepository.existsById(f.siblingOutcome.getId())).isFalse();
        assertThat(opportunityRepository.existsById(f.opp1.getId())).isFalse();
        assertThat(opportunityRepository.existsById(f.opp2.getId())).isFalse();
        assertThat(opportunityRepository.existsById(f.opp3.getId())).isFalse();
        assertThat(opportunityRepository.existsById(f.siblingOpp.getId())).isFalse();
        assertThat(solutionRepository.existsById(f.sol1.getId())).isFalse();
        assertThat(solutionRepository.existsById(f.sol2.getId())).isFalse();
        assertThat(solutionRepository.existsById(f.sol3.getId())).isFalse();

        // AC6: sibling product and other-team tree are untouched.
        assertThat(productRepository.existsById(f.productA2.getId())).isTrue();
        assertThat(productRepository.existsById(f.productB.getId())).isTrue();
        assertThat(outcomeRepository.existsById(f.outcomeB.getId())).isTrue();
        assertThat(opportunityRepository.existsById(f.oppB.getId())).isTrue();
        assertThat(solutionRepository.existsById(f.solB.getId())).isTrue();
    }

    // ---------------------------------------------------------------
    // AC2: deleting an Outcome cascades all nested opportunities + solutions.
    // ---------------------------------------------------------------

    @Test
    @Transactional
    void editorDeletesOutcomeCascadesOpportunitiesAndSolutions() throws Exception {
        Fixture f = seedFullFixture();
        mvc
            .perform(delete("/api/tree/outcomes/{id}", f.outcome1.getId()).with(user(EDITOR_LOGIN)).with(csrf()))
            .andExpect(status().isNoContent());

        assertThat(outcomeRepository.existsById(f.outcome1.getId())).isFalse();
        assertThat(opportunityRepository.existsById(f.opp1.getId())).isFalse();
        assertThat(opportunityRepository.existsById(f.opp2.getId())).isFalse();
        assertThat(opportunityRepository.existsById(f.opp3.getId())).isFalse();
        assertThat(opportunityRepository.existsById(f.siblingOpp.getId())).isFalse();
        assertThat(solutionRepository.existsById(f.sol1.getId())).isFalse();
        assertThat(solutionRepository.existsById(f.sol2.getId())).isFalse();
        assertThat(solutionRepository.existsById(f.sol3.getId())).isFalse();

        // Product and sibling outcome remain.
        assertThat(productRepository.existsById(f.productA1.getId())).isTrue();
        assertThat(outcomeRepository.existsById(f.siblingOutcome.getId())).isTrue();
        assertThat(productRepository.existsById(f.productB.getId())).isTrue();
        assertThat(solutionRepository.existsById(f.solB.getId())).isTrue();
    }

    // ---------------------------------------------------------------
    // AC3: deleting an Opportunity removes its nested opportunities +
    // solutions at every level; deleting a Solution removes just the solution.
    // ---------------------------------------------------------------

    @Test
    @Transactional
    void deleteOpportunityCascadesNestedOpportunitiesAndSolutions() throws Exception {
        Fixture f = seedFullFixture();
        // Delete opp1: opp2, opp3, sol1, sol2, sol3 must go; siblingOpp stays.
        mvc
            .perform(delete("/api/tree/opportunities/{id}", f.opp1.getId()).with(user(OWNER_LOGIN)).with(csrf()))
            .andExpect(status().isNoContent());

        assertThat(opportunityRepository.existsById(f.opp1.getId())).isFalse();
        assertThat(opportunityRepository.existsById(f.opp2.getId())).isFalse();
        assertThat(opportunityRepository.existsById(f.opp3.getId())).isFalse();
        assertThat(solutionRepository.existsById(f.sol1.getId())).isFalse();
        assertThat(solutionRepository.existsById(f.sol2.getId())).isFalse();
        assertThat(solutionRepository.existsById(f.sol3.getId())).isFalse();

        // Sibling opportunity, parent outcome and other product/team intact.
        assertThat(opportunityRepository.existsById(f.siblingOpp.getId())).isTrue();
        assertThat(outcomeRepository.existsById(f.outcome1.getId())).isTrue();
        assertThat(productRepository.existsById(f.productA1.getId())).isTrue();
        assertThat(solutionRepository.existsById(f.solB.getId())).isTrue();
    }

    @Test
    @Transactional
    void deleteSolutionRemovesOnlyThatSolution() throws Exception {
        Fixture f = seedFullFixture();
        mvc
            .perform(delete("/api/tree/solutions/{id}", f.sol1.getId()).with(user(EDITOR_LOGIN)).with(csrf()))
            .andExpect(status().isNoContent());

        assertThat(solutionRepository.existsById(f.sol1.getId())).isFalse();
        // Everything else — including sibling solutions and the opportunity
        // the solution hung off — remains.
        assertThat(solutionRepository.existsById(f.sol2.getId())).isTrue();
        assertThat(solutionRepository.existsById(f.sol3.getId())).isTrue();
        assertThat(opportunityRepository.existsById(f.opp3.getId())).isTrue();
        assertThat(opportunityRepository.existsById(f.opp1.getId())).isTrue();
        assertThat(outcomeRepository.existsById(f.outcome1.getId())).isTrue();
    }

    // ---------------------------------------------------------------
    // AC5: authorisation — viewers and non-members receive 403 and nothing
    // is deleted.
    // ---------------------------------------------------------------

    @Test
    @Transactional
    void viewerCannotDeleteProductAndNothingIsRemoved() throws Exception {
        Fixture f = seedFullFixture();
        mvc
            .perform(delete("/api/tree/products/{id}", f.productA1.getId()).with(user(VIEWER_LOGIN)).with(csrf()))
            .andExpect(status().isForbidden());

        assertThat(productRepository.existsById(f.productA1.getId())).isTrue();
        assertThat(outcomeRepository.existsById(f.outcome1.getId())).isTrue();
        assertThat(opportunityRepository.existsById(f.opp3.getId())).isTrue();
        assertThat(solutionRepository.existsById(f.sol1.getId())).isTrue();
    }

    @Test
    @Transactional
    void viewerCannotDeleteOutcomeOpportunityOrSolution() throws Exception {
        Fixture f = seedFullFixture();
        mvc
            .perform(delete("/api/tree/outcomes/{id}", f.outcome1.getId()).with(user(VIEWER_LOGIN)).with(csrf()))
            .andExpect(status().isForbidden());
        mvc
            .perform(delete("/api/tree/opportunities/{id}", f.opp1.getId()).with(user(VIEWER_LOGIN)).with(csrf()))
            .andExpect(status().isForbidden());
        mvc
            .perform(delete("/api/tree/solutions/{id}", f.sol1.getId()).with(user(VIEWER_LOGIN)).with(csrf()))
            .andExpect(status().isForbidden());

        assertThat(outcomeRepository.existsById(f.outcome1.getId())).isTrue();
        assertThat(opportunityRepository.existsById(f.opp1.getId())).isTrue();
        assertThat(solutionRepository.existsById(f.sol1.getId())).isTrue();
    }

    @Test
    @Transactional
    void nonMemberCannotDeleteAnyNodeAndNothingIsRemoved() throws Exception {
        Fixture f = seedFullFixture();
        mvc
            .perform(delete("/api/tree/products/{id}", f.productA1.getId()).with(user(OUTSIDER_LOGIN)).with(csrf()))
            .andExpect(status().isForbidden());
        mvc
            .perform(delete("/api/tree/outcomes/{id}", f.outcome1.getId()).with(user(OUTSIDER_LOGIN)).with(csrf()))
            .andExpect(status().isForbidden());
        mvc
            .perform(delete("/api/tree/opportunities/{id}", f.opp1.getId()).with(user(OUTSIDER_LOGIN)).with(csrf()))
            .andExpect(status().isForbidden());
        mvc
            .perform(delete("/api/tree/solutions/{id}", f.sol1.getId()).with(user(OUTSIDER_LOGIN)).with(csrf()))
            .andExpect(status().isForbidden());

        assertThat(productRepository.existsById(f.productA1.getId())).isTrue();
        assertThat(outcomeRepository.existsById(f.outcome1.getId())).isTrue();
        assertThat(opportunityRepository.existsById(f.opp1.getId())).isTrue();
        assertThat(solutionRepository.existsById(f.sol1.getId())).isTrue();
    }

    // ---------------------------------------------------------------
    // AC6: a caller from another team cannot delete nodes in team A even if
    // they belong to some team (member of team B here).
    // ---------------------------------------------------------------

    @Test
    @Transactional
    void otherTeamMemberCannotDeleteThisTeamsProduct() throws Exception {
        Fixture f = seedFullFixture();
        // Make outsider an owner of team B — still not a member of team A.
        persistMembership(teamB, outsiderUser, TeamRole.OWNER);
        mvc
            .perform(delete("/api/tree/products/{id}", f.productA1.getId()).with(user(OUTSIDER_LOGIN)).with(csrf()))
            .andExpect(status().isForbidden());
        assertThat(productRepository.existsById(f.productA1.getId())).isTrue();
    }

    // ---------------------------------------------------------------
    // AC7: FK dependents that live outside the tree hierarchy must not
    // block the cascade. These tests seed the two shapes the schema
    // permits: an interview attached to the product being deleted, and
    // an assumption on one solution that is also linked (via
    // rel_experiment__assumption) to an experiment on a sibling solution.
    // ---------------------------------------------------------------

    @Test
    @Transactional
    void deleteProductWithInterviewSucceeds() throws Exception {
        Fixture f = seedFullFixture();
        Product product = productRepository.findById(f.productA1.getId()).orElseThrow();
        Interview interview = new Interview().title("kickoff").interviewDate(LocalDate.now()).createdDate(Instant.now()).product(product);
        em.persist(interview);
        em.flush();
        em.clear();

        mvc
            .perform(delete("/api/tree/products/{id}", f.productA1.getId()).with(user(OWNER_LOGIN)).with(csrf()))
            .andExpect(status().isNoContent());

        assertThat(productRepository.existsById(f.productA1.getId())).isFalse();
        Long remainingInterviews = em
            .createQuery("select count(i) from Interview i where i.id = :id", Long.class)
            .setParameter("id", interview.getId())
            .getSingleResult();
        assertThat(remainingInterviews).isZero();
    }

    @Test
    @Transactional
    void deleteSolutionWithAssumptionLinkedToOtherSolutionsExperimentSucceeds() throws Exception {
        Fixture f = seedFullFixture();
        Solution sol1 = solutionRepository.findById(f.sol1.getId()).orElseThrow();
        Solution sol2 = solutionRepository.findById(f.sol2.getId()).orElseThrow();

        // Assumption belongs to sol1 (the deletion target).
        Assumption assumption = new Assumption()
            .statement("users need this")
            .category(AssumptionCategory.DESIRABILITY)
            .importance(3)
            .evidence(1)
            .createdDate(Instant.now())
            .solution(sol1);
        em.persist(assumption);
        // Experiment belongs to a sibling solution (sol2) but references
        // sol1's assumption through the join table — the previous cascade
        // implementation ignored this case and hit an FK violation.
        Experiment experiment = new Experiment()
            .title("landing page")
            .status(ExperimentStatus.PLANNED)
            .createdDate(Instant.now())
            .solution(sol2);
        Set<Assumption> assumptions = new HashSet<>();
        assumptions.add(assumption);
        experiment.setAssumptions(assumptions);
        em.persist(experiment);
        em.flush();
        em.clear();

        mvc
            .perform(delete("/api/tree/solutions/{id}", f.sol1.getId()).with(user(OWNER_LOGIN)).with(csrf()))
            .andExpect(status().isNoContent());

        assertThat(solutionRepository.existsById(f.sol1.getId())).isFalse();
        Long remainingAssumptions = em
            .createQuery("select count(a) from Assumption a where a.id = :id", Long.class)
            .setParameter("id", assumption.getId())
            .getSingleResult();
        assertThat(remainingAssumptions).isZero();
        // Sibling solution and its experiment remain — only join rows are gone.
        assertThat(solutionRepository.existsById(f.sol2.getId())).isTrue();
        Long remainingExperiments = em
            .createQuery("select count(e) from Experiment e where e.id = :id", Long.class)
            .setParameter("id", experiment.getId())
            .getSingleResult();
        assertThat(remainingExperiments).isEqualTo(1L);
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

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
        Product p = new Product().name(name).description("d").archived(archived).createdDate(Instant.now()).team(team);
        em.persist(p);
        return p;
    }

    private Outcome persistOutcome(Product product, String title, int sortOrder) {
        Outcome o = new Outcome()
            .title(title)
            .status(OutcomeStatus.ACTIVE)
            .sortOrder(sortOrder)
            .createdDate(Instant.now())
            .product(product);
        em.persist(o);
        return o;
    }

    private Opportunity persistOpportunity(Outcome outcome, Opportunity parent, String title, int sortOrder) {
        Opportunity op = new Opportunity()
            .title(title)
            .status(OpportunityStatus.IDENTIFIED)
            .valuerating(3)
            .complexity(3)
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
            .status(SolutionStatus.IDEA)
            .sortOrder(sortOrder)
            .createdDate(Instant.now())
            .opportunity(opportunity);
        em.persist(s);
        return s;
    }
}
