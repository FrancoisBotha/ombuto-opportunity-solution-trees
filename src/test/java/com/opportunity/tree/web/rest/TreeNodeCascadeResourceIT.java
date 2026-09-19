package com.opportunity.tree.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.opportunity.tree.IntegrationTest;
import com.opportunity.tree.domain.Assumption;
import com.opportunity.tree.domain.Comment;
import com.opportunity.tree.domain.Evidence;
import com.opportunity.tree.domain.Interview;
import com.opportunity.tree.domain.NodeHistory;
import com.opportunity.tree.domain.NodeLink;
import com.opportunity.tree.domain.OpenQuestion;
import com.opportunity.tree.domain.Opportunity;
import com.opportunity.tree.domain.Outcome;
import com.opportunity.tree.domain.Product;
import com.opportunity.tree.domain.Solution;
import com.opportunity.tree.domain.Team;
import com.opportunity.tree.domain.TeamMember;
import com.opportunity.tree.domain.User;
import com.opportunity.tree.domain.enumeration.AssumptionStatus;
import com.opportunity.tree.domain.enumeration.HistoryEventType;
import com.opportunity.tree.domain.enumeration.OpportunityStatus;
import com.opportunity.tree.domain.enumeration.SolutionStatus;
import com.opportunity.tree.domain.enumeration.TeamRole;
import com.opportunity.tree.domain.enumeration.TreeNodeType;
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
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the cascade delete endpoint {@code DELETE /api/tree/nodes/{type}/{id}}
 * ({@link TreeNodeResource} → {@code TreeNodeCascadeService}) covering the TREE-003
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
            .perform(delete("/api/tree/nodes/product/{id}", f.productA1.getId()).with(user(OWNER_LOGIN)).with(csrf()))
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
            .perform(delete("/api/tree/nodes/outcome/{id}", f.outcome1.getId()).with(user(EDITOR_LOGIN)).with(csrf()))
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
            .perform(delete("/api/tree/nodes/opportunity/{id}", f.opp1.getId()).with(user(OWNER_LOGIN)).with(csrf()))
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
            .perform(delete("/api/tree/nodes/solution/{id}", f.sol1.getId()).with(user(EDITOR_LOGIN)).with(csrf()))
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
            .perform(delete("/api/tree/nodes/product/{id}", f.productA1.getId()).with(user(VIEWER_LOGIN)).with(csrf()))
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
            .perform(delete("/api/tree/nodes/outcome/{id}", f.outcome1.getId()).with(user(VIEWER_LOGIN)).with(csrf()))
            .andExpect(status().isForbidden());
        mvc
            .perform(delete("/api/tree/nodes/opportunity/{id}", f.opp1.getId()).with(user(VIEWER_LOGIN)).with(csrf()))
            .andExpect(status().isForbidden());
        mvc
            .perform(delete("/api/tree/nodes/solution/{id}", f.sol1.getId()).with(user(VIEWER_LOGIN)).with(csrf()))
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
            .perform(delete("/api/tree/nodes/product/{id}", f.productA1.getId()).with(user(OUTSIDER_LOGIN)).with(csrf()))
            .andExpect(status().isForbidden());
        mvc
            .perform(delete("/api/tree/nodes/outcome/{id}", f.outcome1.getId()).with(user(OUTSIDER_LOGIN)).with(csrf()))
            .andExpect(status().isForbidden());
        mvc
            .perform(delete("/api/tree/nodes/opportunity/{id}", f.opp1.getId()).with(user(OUTSIDER_LOGIN)).with(csrf()))
            .andExpect(status().isForbidden());
        mvc
            .perform(delete("/api/tree/nodes/solution/{id}", f.sol1.getId()).with(user(OUTSIDER_LOGIN)).with(csrf()))
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
            .perform(delete("/api/tree/nodes/product/{id}", f.productA1.getId()).with(user(OUTSIDER_LOGIN)).with(csrf()))
            .andExpect(status().isForbidden());
        assertThat(productRepository.existsById(f.productA1.getId())).isTrue();
    }

    // ---------------------------------------------------------------
    // AC7: FK dependents that live outside the tree hierarchy must not
    // block the cascade. These tests seed the two shapes the schema
    // permits: an interview attached to the product being deleted, and the
    // assumptions, evidence, open questions and node links hanging off
    // deleted opportunities and solutions.
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
            .perform(delete("/api/tree/nodes/product/{id}", f.productA1.getId()).with(user(OWNER_LOGIN)).with(csrf()))
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
    void deleteSolutionRemovesItsAssumptionsWithTheirEvidenceAndLinks() throws Exception {
        Fixture f = seedFullFixture();
        Solution sol1 = solutionRepository.findById(f.sol1.getId()).orElseThrow();

        Assumption assumption = new Assumption()
            .statement("users need this")
            .status(AssumptionStatus.UNTESTED)
            .confidence(40)
            .sortOrder(0)
            .createdDate(Instant.now())
            .solution(sol1);
        em.persist(assumption);
        Evidence evidence = new Evidence().title("interview snippet").sortOrder(0).createdDate(Instant.now()).assumption(assumption);
        em.persist(evidence);
        NodeLink assumptionLink = new NodeLink()
            .name("Doc")
            .url("https://example.com/doc")
            .sortOrder(0)
            .createdDate(Instant.now())
            .assumption(assumption);
        em.persist(assumptionLink);
        NodeLink evidenceLink = new NodeLink()
            .name("Recording")
            .url("https://example.com/rec")
            .sortOrder(0)
            .createdDate(Instant.now())
            .evidence(evidence);
        em.persist(evidenceLink);
        em.flush();
        em.clear();

        mvc
            .perform(delete("/api/tree/nodes/solution/{id}", f.sol1.getId()).with(user(OWNER_LOGIN)).with(csrf()))
            .andExpect(status().isNoContent());

        assertThat(solutionRepository.existsById(f.sol1.getId())).isFalse();
        assertThat(countById("Assumption", assumption.getId())).isZero();
        assertThat(countById("Evidence", evidence.getId())).isZero();
        assertThat(countById("NodeLink", assumptionLink.getId())).isZero();
        assertThat(countById("NodeLink", evidenceLink.getId())).isZero();
        // Sibling solution is untouched.
        assertThat(solutionRepository.existsById(f.sol2.getId())).isTrue();
    }

    @Test
    @Transactional
    void deleteOpportunityRemovesItsEvidenceOpenQuestionsAndLinks() throws Exception {
        Fixture f = seedFullFixture();
        Opportunity opp = opportunityRepository.findById(f.opp1.getId()).orElseThrow();

        Evidence evidence = new Evidence().title("interview snippet").sortOrder(0).createdDate(Instant.now()).opportunity(opp);
        em.persist(evidence);
        OpenQuestion question = new OpenQuestion().questionText("Who else?").done(false).sortOrder(0).createdDate(Instant.now());
        question.setOpportunity(opp);
        em.persist(question);
        NodeLink link = new NodeLink().name("Doc").url("https://example.com/doc").sortOrder(0).createdDate(Instant.now()).opportunity(opp);
        em.persist(link);
        em.flush();
        em.clear();

        mvc
            .perform(delete("/api/tree/nodes/opportunity/{id}", f.opp1.getId()).with(user(OWNER_LOGIN)).with(csrf()))
            .andExpect(status().isNoContent());

        assertThat(opportunityRepository.existsById(f.opp1.getId())).isFalse();
        assertThat(countById("Evidence", evidence.getId())).isZero();
        assertThat(countById("OpenQuestion", question.getId())).isZero();
        assertThat(countById("NodeLink", link.getId())).isZero();
    }

    // ---------------------------------------------------------------
    // Assumption / evidence entry points and NodeHistory clean-up.
    // ---------------------------------------------------------------

    private Assumption persistAssumption(Solution solution) {
        Assumption assumption = new Assumption()
            .statement("users need this")
            .status(AssumptionStatus.UNTESTED)
            .confidence(40)
            .sortOrder(0)
            .createdDate(Instant.now())
            .solution(solution);
        em.persist(assumption);
        return assumption;
    }

    private void persistHistory(TreeNodeType type, Long nodeId) {
        em.persist(
            new NodeHistory()
                .nodeType(type)
                .nodeId(nodeId)
                .eventType(HistoryEventType.CREATED)
                .summary("Created")
                .createdDate(Instant.now())
        );
    }

    private Long historyCount(TreeNodeType type, Long nodeId) {
        return em
            .createQuery("select count(h) from NodeHistory h where h.nodeType = :type and h.nodeId = :id", Long.class)
            .setParameter("type", type)
            .setParameter("id", nodeId)
            .getSingleResult();
    }

    @Test
    @Transactional
    void editorDeletesAssumptionWithItsEvidenceLinksCommentsAndHistory() throws Exception {
        Fixture f = seedFullFixture();
        Solution sol1 = solutionRepository.findById(f.sol1.getId()).orElseThrow();
        Assumption assumption = persistAssumption(sol1);
        Assumption sibling = persistAssumption(sol1);
        Evidence evidence = new Evidence().title("result").sortOrder(0).createdDate(Instant.now()).assumption(assumption);
        em.persist(evidence);
        NodeLink link = new NodeLink()
            .name("Doc")
            .url("https://example.com/doc")
            .sortOrder(0)
            .createdDate(Instant.now())
            .assumption(assumption);
        em.persist(link);
        Comment comment = new Comment().body("hmm").createdDate(Instant.now()).author(ownerUser).assumption(assumption);
        em.persist(comment);
        em.flush();
        persistHistory(TreeNodeType.ASSUMPTION, assumption.getId());
        persistHistory(TreeNodeType.EVIDENCE, evidence.getId());
        persistHistory(TreeNodeType.ASSUMPTION, sibling.getId());
        persistHistory(TreeNodeType.SOLUTION, sol1.getId());
        em.flush();
        em.clear();

        mvc
            .perform(delete("/api/tree/nodes/assumption/{id}", assumption.getId()).with(user(EDITOR_LOGIN)).with(csrf()))
            .andExpect(status().isNoContent());

        assertThat(countById("Assumption", assumption.getId())).isZero();
        assertThat(countById("Evidence", evidence.getId())).isZero();
        assertThat(countById("NodeLink", link.getId())).isZero();
        assertThat(countById("Comment", comment.getId())).isZero();
        assertThat(historyCount(TreeNodeType.ASSUMPTION, assumption.getId())).isZero();
        assertThat(historyCount(TreeNodeType.EVIDENCE, evidence.getId())).isZero();
        // Sibling assumption, parent solution and their history are untouched.
        assertThat(countById("Assumption", sibling.getId())).isEqualTo(1);
        assertThat(historyCount(TreeNodeType.ASSUMPTION, sibling.getId())).isEqualTo(1);
        assertThat(historyCount(TreeNodeType.SOLUTION, sol1.getId())).isEqualTo(1);
        assertThat(solutionRepository.existsById(f.sol1.getId())).isTrue();
    }

    @Test
    @Transactional
    void ownerDeletesEvidenceUnderOpportunityWithItsLinksCommentsAndHistory() throws Exception {
        Fixture f = seedFullFixture();
        Opportunity opp = opportunityRepository.findById(f.opp1.getId()).orElseThrow();
        Evidence evidence = new Evidence().title("snippet").sortOrder(0).createdDate(Instant.now()).opportunity(opp);
        em.persist(evidence);
        Evidence sibling = new Evidence().title("other").sortOrder(1).createdDate(Instant.now()).opportunity(opp);
        em.persist(sibling);
        NodeLink link = new NodeLink()
            .name("Rec")
            .url("https://example.com/rec")
            .sortOrder(0)
            .createdDate(Instant.now())
            .evidence(evidence);
        em.persist(link);
        Comment comment = new Comment().body("nice").createdDate(Instant.now()).author(ownerUser).evidence(evidence);
        em.persist(comment);
        em.flush();
        persistHistory(TreeNodeType.EVIDENCE, evidence.getId());
        persistHistory(TreeNodeType.EVIDENCE, sibling.getId());
        em.flush();
        em.clear();

        mvc
            .perform(delete("/api/tree/nodes/evidence/{id}", evidence.getId()).with(user(OWNER_LOGIN)).with(csrf()))
            .andExpect(status().isNoContent());

        assertThat(countById("Evidence", evidence.getId())).isZero();
        assertThat(countById("NodeLink", link.getId())).isZero();
        assertThat(countById("Comment", comment.getId())).isZero();
        assertThat(historyCount(TreeNodeType.EVIDENCE, evidence.getId())).isZero();
        assertThat(countById("Evidence", sibling.getId())).isEqualTo(1);
        assertThat(historyCount(TreeNodeType.EVIDENCE, sibling.getId())).isEqualTo(1);
        assertThat(opportunityRepository.existsById(f.opp1.getId())).isTrue();
    }

    @Test
    @Transactional
    void viewerAndNonMemberCannotDeleteAssumptionOrEvidence() throws Exception {
        Fixture f = seedFullFixture();
        Solution sol1 = solutionRepository.findById(f.sol1.getId()).orElseThrow();
        Assumption assumption = persistAssumption(sol1);
        Evidence evidence = new Evidence().title("result").sortOrder(0).createdDate(Instant.now()).assumption(assumption);
        em.persist(evidence);
        em.flush();
        em.clear();

        for (String login : List.of(VIEWER_LOGIN, OUTSIDER_LOGIN)) {
            mvc
                .perform(delete("/api/tree/nodes/assumption/{id}", assumption.getId()).with(user(login)).with(csrf()))
                .andExpect(status().isForbidden());
            mvc
                .perform(delete("/api/tree/nodes/evidence/{id}", evidence.getId()).with(user(login)).with(csrf()))
                .andExpect(status().isForbidden());
        }
        // Unknown ids are indistinguishable from foreign ones.
        mvc
            .perform(delete("/api/tree/nodes/evidence/{id}", Long.MAX_VALUE).with(user(OWNER_LOGIN)).with(csrf()))
            .andExpect(status().isForbidden());

        assertThat(countById("Assumption", assumption.getId())).isEqualTo(1);
        assertThat(countById("Evidence", evidence.getId())).isEqualTo(1);
    }

    @Test
    @Transactional
    void deleteProductRemovesHistoryOfEveryNodeInTheBranchOnly() throws Exception {
        Fixture f = seedFullFixture();
        Solution sol1 = solutionRepository.findById(f.sol1.getId()).orElseThrow();
        Assumption assumption = persistAssumption(sol1);
        Evidence evidence = new Evidence().title("result").sortOrder(0).createdDate(Instant.now()).assumption(assumption);
        em.persist(evidence);
        em.flush();
        persistHistory(TreeNodeType.PRODUCT, f.productA1.getId());
        persistHistory(TreeNodeType.OUTCOME, f.outcome1.getId());
        persistHistory(TreeNodeType.OPPORTUNITY, f.opp3.getId());
        persistHistory(TreeNodeType.SOLUTION, f.sol1.getId());
        persistHistory(TreeNodeType.ASSUMPTION, assumption.getId());
        persistHistory(TreeNodeType.EVIDENCE, evidence.getId());
        // Untouched: sibling product, other team, and a same-id row of another type.
        persistHistory(TreeNodeType.PRODUCT, f.productA2.getId());
        persistHistory(TreeNodeType.OPPORTUNITY, f.oppB.getId());
        em.flush();
        em.clear();

        mvc
            .perform(delete("/api/tree/nodes/product/{id}", f.productA1.getId()).with(user(OWNER_LOGIN)).with(csrf()))
            .andExpect(status().isNoContent());

        assertThat(historyCount(TreeNodeType.PRODUCT, f.productA1.getId())).isZero();
        assertThat(historyCount(TreeNodeType.OUTCOME, f.outcome1.getId())).isZero();
        assertThat(historyCount(TreeNodeType.OPPORTUNITY, f.opp3.getId())).isZero();
        assertThat(historyCount(TreeNodeType.SOLUTION, f.sol1.getId())).isZero();
        assertThat(historyCount(TreeNodeType.ASSUMPTION, assumption.getId())).isZero();
        assertThat(historyCount(TreeNodeType.EVIDENCE, evidence.getId())).isZero();
        assertThat(historyCount(TreeNodeType.PRODUCT, f.productA2.getId())).isEqualTo(1);
        assertThat(historyCount(TreeNodeType.OPPORTUNITY, f.oppB.getId())).isEqualTo(1);
    }

    private Long countById(String entity, Long id) {
        return em
            .createQuery("select count(e) from " + entity + " e where e.id = :id", Long.class)
            .setParameter("id", id)
            .getSingleResult();
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
}
