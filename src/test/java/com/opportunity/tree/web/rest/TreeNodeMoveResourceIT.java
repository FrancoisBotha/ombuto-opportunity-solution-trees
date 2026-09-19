package com.opportunity.tree.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opportunity.tree.IntegrationTest;
import com.opportunity.tree.domain.Assumption;
import com.opportunity.tree.domain.Evidence;
import com.opportunity.tree.domain.NodeHistory;
import com.opportunity.tree.domain.Opportunity;
import com.opportunity.tree.domain.Outcome;
import com.opportunity.tree.domain.Product;
import com.opportunity.tree.domain.Solution;
import com.opportunity.tree.domain.Team;
import com.opportunity.tree.domain.User;
import com.opportunity.tree.domain.enumeration.HistoryEventType;
import com.opportunity.tree.domain.enumeration.TeamRole;
import com.opportunity.tree.domain.enumeration.TreeNodeType;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for {@link TreeNodeMoveResource} ({@code POST /api/tree/nodes/move}), ported
 * from REARR-001 and extended: every movable type (incl. assumption between solutions and
 * evidence between opportunities and assumptions), product reorder, cycle prevention, legal
 * parent types, same-team only, authorisation, dense renumbering of old and new siblings, the
 * nested-opportunity outcome propagation and MOVED history (parent change only).
 */
@IntegrationTest
@AutoConfigureMockMvc
@Transactional
class TreeNodeMoveResourceIT {

    private static final String OWNER = "move-owner";
    private static final String EDITOR = "move-editor";
    private static final String VIEWER = "move-viewer";
    private static final String OUTSIDER = "move-outsider";
    private static final String DUAL = "move-dual";
    private static final String ADMIN = "move-admin";

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper om;

    private OstTreeTestData data;

    // Team A:
    //   productA1 (0)                                   productA2 (1)
    //     outcome1 (1)                    outcome2 (2)
    //       opp1 (1)                        opp4 (1)
    //         opp2 (1)                        sol3 (1)
    //           opp3 (1)                        asm3 (0)
    //             sol1 (1)                        evA (0)
    //               asm1 (0), asm2 (1)
    //           sol2 (1)
    //         ev1 (0), ev2 (1)
    //       sib1 (2)
    // Team B: productB / outcomeB / oppB
    private Product productA1;
    private Product productA2;
    private Outcome outcome1;
    private Outcome outcome2;
    private Opportunity opp1;
    private Opportunity opp2;
    private Opportunity opp3;
    private Opportunity sib1;
    private Opportunity opp4;
    private Solution sol1;
    private Solution sol2;
    private Solution sol3;
    private Assumption asm1;
    private Assumption asm2;
    private Assumption asm3;
    private Evidence ev1;
    private Evidence ev2;
    private Evidence evA;
    private Outcome outcomeB;

    @BeforeEach
    void setUp() {
        data = new OstTreeTestData(em);
        Team teamA = data.team("Move Team A");
        Team teamB = data.team("Move Team B");
        User owner = data.user(OWNER);
        User editor = data.user(EDITOR);
        User viewer = data.user(VIEWER);
        User outsider = data.user(OUTSIDER);
        User dual = data.user(DUAL);
        data.user(ADMIN);
        data.member(teamA, owner, TeamRole.OWNER);
        data.member(teamA, editor, TeamRole.EDITOR);
        data.member(teamA, viewer, TeamRole.VIEWER);
        data.member(teamA, dual, TeamRole.EDITOR);
        data.member(teamB, outsider, TeamRole.OWNER);
        data.member(teamB, dual, TeamRole.EDITOR);

        productA1 = data.product(teamA, "A-prod-1", 0);
        productA2 = data.product(teamA, "A-prod-2", 1);
        outcome1 = data.outcome(productA1, "outcome-1", 1);
        outcome2 = data.outcome(productA1, "outcome-2", 2);
        opp1 = data.opportunity(outcome1, null, "opp-1", 1);
        opp2 = data.opportunity(outcome1, opp1, "opp-2", 1);
        opp3 = data.opportunity(outcome1, opp2, "opp-3", 1);
        sib1 = data.opportunity(outcome1, null, "sib-1", 2);
        opp4 = data.opportunity(outcome2, null, "opp-4", 1);
        sol1 = data.solution(opp3, "sol-1", 1);
        sol2 = data.solution(opp2, "sol-2", 1);
        sol3 = data.solution(opp4, "sol-3", 1);
        asm1 = data.assumption(sol1, "asm-1", 0);
        asm2 = data.assumption(sol1, "asm-2", 1);
        asm3 = data.assumption(sol3, "asm-3", 0);
        ev1 = data.evidence(opp1, null, "ev-1", 0);
        ev2 = data.evidence(opp1, null, "ev-2", 1);
        evA = data.evidence(null, asm3, "ev-A", 0);

        Product productB = data.product(teamB, "B-prod", 0);
        outcomeB = data.outcome(productB, "b-outcome", 1);
        data.opportunity(outcomeB, null, "b-opp", 1);
        em.flush();
        em.clear();
    }

    // ---------------------------------------------------------------------
    // Valid moves
    // ---------------------------------------------------------------------

    @Test
    void moveOutcomeToAnotherProductRenumbersBothSiblingSetsAndRecordsHistory() throws Exception {
        JsonNode res = moveOk(OWNER, "OUTCOME", outcome1.getId(), "PRODUCT", productA2.getId(), 0);

        assertThat(res.at("/node/key").asText()).isEqualTo("outcome-" + outcome1.getId());
        assertThat(res.at("/node/parentKey").asText()).isEqualTo("product-" + productA2.getId());
        assertThat(res.at("/node/sortOrder").asInt()).isZero();
        assertThat(siblings(res)).containsExactly(
            Map.entry("outcome-" + outcome1.getId(), 0),
            // Old parent productA1: only outcome2 remains, renumbered densely to 0.
            Map.entry("outcome-" + outcome2.getId(), 0)
        );
        assertThat(em.find(Outcome.class, outcome1.getId()).getProduct().getId()).isEqualTo(productA2.getId());
        assertThat(em.find(Outcome.class, outcome2.getId()).getSortOrder()).isZero();
        assertThat(movedSummaries(TreeNodeType.OUTCOME, outcome1.getId())).containsExactly("Moved under “A-prod-2”");
    }

    @Test
    void moveOpportunityUnderAnotherOpportunityCarriesSubtreeAndUpdatesNestedOutcome() throws Exception {
        JsonNode res = moveOk(OWNER, "opportunity", opp1.getId(), "opportunity", opp4.getId(), 0);

        assertThat(res.at("/node/parentKey").asText()).isEqualTo("opportunity-" + opp4.getId());
        em.clear();
        assertThat(em.find(Opportunity.class, opp1.getId()).getParent().getId()).isEqualTo(opp4.getId());
        assertThat(em.find(Opportunity.class, opp1.getId()).getOutcome().getId()).isEqualTo(outcome2.getId());
        assertThat(em.find(Opportunity.class, opp2.getId()).getOutcome().getId()).isEqualTo(outcome2.getId());
        assertThat(em.find(Opportunity.class, opp3.getId()).getOutcome().getId()).isEqualTo(outcome2.getId());
        // Subtree intact.
        assertThat(em.find(Opportunity.class, opp3.getId()).getParent().getId()).isEqualTo(opp2.getId());
        assertThat(em.find(Solution.class, sol1.getId()).getOpportunity().getId()).isEqualTo(opp3.getId());
        assertThat(em.find(Solution.class, sol2.getId()).getOpportunity().getId()).isEqualTo(opp2.getId());
        assertThat(em.find(Evidence.class, ev1.getId()).getOpportunity().getId()).isEqualTo(opp1.getId());
        // Old top-level siblings under outcome1: sib1 renumbered to 0.
        assertThat(em.find(Opportunity.class, sib1.getId()).getSortOrder()).isZero();
        assertThat(movedSummaries(TreeNodeType.OPPORTUNITY, opp1.getId())).containsExactly("Moved under “opp-4”");
    }

    @Test
    void moveOpportunityToAnotherOutcomeAppendsAndPropagatesOutcome() throws Exception {
        // No position → append after opp4.
        JsonNode res = moveOk(OWNER, "OPPORTUNITY", opp1.getId(), "OUTCOME", outcome2.getId(), null);

        assertThat(res.at("/node/parentKey").asText()).isEqualTo("outcome-" + outcome2.getId());
        assertThat(res.at("/node/sortOrder").asInt()).isEqualTo(1);
        assertThat(siblings(res)).containsExactly(
            Map.entry("opportunity-" + opp4.getId(), 0),
            Map.entry("opportunity-" + opp1.getId(), 1),
            Map.entry("opportunity-" + sib1.getId(), 0)
        );
        em.clear();
        Opportunity moved = em.find(Opportunity.class, opp1.getId());
        assertThat(moved.getParent()).isNull();
        assertThat(moved.getOutcome().getId()).isEqualTo(outcome2.getId());
        assertThat(em.find(Opportunity.class, opp3.getId()).getOutcome().getId()).isEqualTo(outcome2.getId());
    }

    @Test
    void moveSolutionUnderAnotherOpportunity() throws Exception {
        JsonNode res = moveOk(EDITOR, "SOLUTION", sol1.getId(), "OPPORTUNITY", opp1.getId(), 0);
        assertThat(res.at("/node/parentKey").asText()).isEqualTo("opportunity-" + opp1.getId());
        assertThat(em.find(Solution.class, sol1.getId()).getOpportunity().getId()).isEqualTo(opp1.getId());
        // The solution's assumptions come along.
        assertThat(em.find(Assumption.class, asm1.getId()).getSolution().getId()).isEqualTo(sol1.getId());
        assertThat(movedSummaries(TreeNodeType.SOLUTION, sol1.getId())).containsExactly("Moved under “opp-1”");
    }

    @Test
    void moveAssumptionBetweenSolutions() throws Exception {
        JsonNode res = moveOk(EDITOR, "ASSUMPTION", asm1.getId(), "SOLUTION", sol3.getId(), 0);

        assertThat(res.at("/node/parentKey").asText()).isEqualTo("solution-" + sol3.getId());
        assertThat(res.at("/node/type").asText()).isEqualTo("ASSUMPTION");
        assertThat(siblings(res)).containsExactly(
            Map.entry("assumption-" + asm1.getId(), 0),
            Map.entry("assumption-" + asm3.getId(), 1),
            Map.entry("assumption-" + asm2.getId(), 0)
        );
        assertThat(em.find(Assumption.class, asm1.getId()).getSolution().getId()).isEqualTo(sol3.getId());
        assertThat(movedSummaries(TreeNodeType.ASSUMPTION, asm1.getId())).containsExactly("Moved under “sol-3”");
    }

    @Test
    void moveEvidenceFromOpportunityToAssumptionAndBack() throws Exception {
        JsonNode res = moveOk(EDITOR, "EVIDENCE", ev1.getId(), "ASSUMPTION", asm3.getId(), null);
        assertThat(res.at("/node/parentKey").asText()).isEqualTo("assumption-" + asm3.getId());
        assertThat(siblings(res)).containsExactly(
            Map.entry("evidence-" + evA.getId(), 0),
            Map.entry("evidence-" + ev1.getId(), 1),
            Map.entry("evidence-" + ev2.getId(), 0)
        );
        Evidence moved = em.find(Evidence.class, ev1.getId());
        assertThat(moved.getOpportunity()).isNull();
        assertThat(moved.getAssumption().getId()).isEqualTo(asm3.getId());

        res = moveOk(EDITOR, "EVIDENCE", ev1.getId(), "OPPORTUNITY", opp4.getId(), 0);
        assertThat(res.at("/node/parentKey").asText()).isEqualTo("opportunity-" + opp4.getId());
        moved = em.find(Evidence.class, ev1.getId());
        assertThat(moved.getAssumption()).isNull();
        assertThat(moved.getOpportunity().getId()).isEqualTo(opp4.getId());
        assertThat(movedSummaries(TreeNodeType.EVIDENCE, ev1.getId())).containsExactly("Moved under “asm-3”", "Moved under “opp-4”");
    }

    @Test
    void sameParentReorderRenumbersWithoutHistory() throws Exception {
        JsonNode res = moveOk(OWNER, "OPPORTUNITY", sib1.getId(), "OUTCOME", outcome1.getId(), 0);
        assertThat(res.at("/node/sortOrder").asInt()).isZero();
        assertThat(siblings(res)).containsExactly(Map.entry("opportunity-" + sib1.getId(), 0), Map.entry("opportunity-" + opp1.getId(), 1));
        assertThat(em.find(Opportunity.class, opp1.getId()).getSortOrder()).isEqualTo(1);
        assertThat(data.history(TreeNodeType.OPPORTUNITY, sib1.getId())).isEmpty();
    }

    @Test
    void positionBeyondEndIsClampedToLast() throws Exception {
        JsonNode res = moveOk(OWNER, "OPPORTUNITY", opp4.getId(), "OUTCOME", outcome1.getId(), 99);
        assertThat(res.at("/node/sortOrder").asInt()).isEqualTo(2);
    }

    @Test
    void productReorderWithinTeam() throws Exception {
        JsonNode res = moveOk(OWNER, "PRODUCT", productA2.getId(), null, null, 0);
        assertThat(res.at("/node/key").asText()).isEqualTo("product-" + productA2.getId());
        assertThat(res.at("/node/sortOrder").asInt()).isZero();
        assertThat(siblings(res)).containsExactly(
            Map.entry("product-" + productA2.getId(), 0),
            Map.entry("product-" + productA1.getId(), 1)
        );
        assertThat(em.find(Product.class, productA1.getId()).getSortOrder()).isEqualTo(1);
        assertThat(data.history(TreeNodeType.PRODUCT, productA2.getId())).isEmpty();

        res = moveOk(OWNER, "product", productA2.getId(), null, null, 42);
        assertThat(res.at("/node/sortOrder").asInt()).isEqualTo(1);
    }

    // ---------------------------------------------------------------------
    // Rejected moves
    // ---------------------------------------------------------------------

    @Test
    void illegalParentTypesAreRejected() throws Exception {
        Object[][] cases = {
            { "EVIDENCE", ev1.getId(), "SOLUTION", sol1.getId() },
            { "OUTCOME", outcome1.getId(), "OPPORTUNITY", opp1.getId() },
            { "OUTCOME", outcome1.getId(), "OUTCOME", outcome2.getId() },
            { "OPPORTUNITY", opp1.getId(), "PRODUCT", productA1.getId() },
            { "OPPORTUNITY", opp1.getId(), "SOLUTION", sol1.getId() },
            { "SOLUTION", sol1.getId(), "OUTCOME", outcome1.getId() },
            { "SOLUTION", sol1.getId(), "SOLUTION", sol2.getId() },
            { "ASSUMPTION", asm1.getId(), "OPPORTUNITY", opp1.getId() },
            { "EVIDENCE", ev1.getId(), "EVIDENCE", ev2.getId() },
            { "PRODUCT", productA1.getId(), "PRODUCT", productA2.getId() },
        };
        for (Object[] c : cases) {
            move(OWNER, (String) c[0], (Long) c[1], (String) c[2], (Long) c[3], 0)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("error.invalidparent"));
        }
        move(OWNER, "SOLUTION", sol1.getId(), null, null, 0)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.unknowntype"));
        move(OWNER, "SOLUTION", sol1.getId(), "OPPORTUNITY", null, 0)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.parentmissing"));
        move(OWNER, "WIDGET", sol1.getId(), "OPPORTUNITY", opp1.getId(), 0)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.unknowntype"));
        move(OWNER, "SOLUTION", sol1.getId(), "OPPORTUNITY", opp1.getId(), -1)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.invalidposition"));
        assertThat(em.find(Solution.class, sol1.getId()).getOpportunity().getId()).isEqualTo(opp3.getId());
    }

    @Test
    void opportunityCannotMoveUnderItselfOrADescendant() throws Exception {
        move(OWNER, "OPPORTUNITY", opp1.getId(), "OPPORTUNITY", opp1.getId(), 0)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.cycle"));
        move(OWNER, "OPPORTUNITY", opp1.getId(), "OPPORTUNITY", opp3.getId(), 0)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.cycle"));
        Opportunity unchanged = em.find(Opportunity.class, opp1.getId());
        assertThat(unchanged.getParent()).isNull();
        assertThat(unchanged.getSortOrder()).isEqualTo(1);
        assertThat(data.history(TreeNodeType.OPPORTUNITY, opp1.getId())).isEmpty();
    }

    @Test
    void crossTeamMovesAreRejected() throws Exception {
        // A user who can edit both teams still cannot move a node across teams.
        move(DUAL, "OPPORTUNITY", sib1.getId(), "OUTCOME", outcomeB.getId(), 0)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.crossteam"));
        // An editor of team A only has no access to team B's outcome.
        move(OWNER, "OPPORTUNITY", sib1.getId(), "OUTCOME", outcomeB.getId(), 0).andExpect(status().isForbidden());
        assertThat(em.find(Opportunity.class, sib1.getId()).getOutcome().getId()).isEqualTo(outcome1.getId());
    }

    @Test
    void viewerNonMemberAndAdminNonMemberCannotMove() throws Exception {
        for (RequestPostProcessor who : List.of(
            user(VIEWER),
            user(OUTSIDER),
            SecurityMockMvcRequestPostProcessors.user(ADMIN).roles("ADMIN", "USER")
        )) {
            move(who, "SOLUTION", sol1.getId(), "OPPORTUNITY", opp1.getId(), 0).andExpect(status().isForbidden());
            move(who, "PRODUCT", productA2.getId(), null, null, 0).andExpect(status().isForbidden());
        }
        // Unknown ids look exactly like foreign ones.
        move(OWNER, "SOLUTION", Long.MAX_VALUE, "OPPORTUNITY", opp1.getId(), 0).andExpect(status().isForbidden());
        move(OWNER, "SOLUTION", sol1.getId(), "OPPORTUNITY", Long.MAX_VALUE, 0).andExpect(status().isForbidden());
        assertThat(em.find(Solution.class, sol1.getId()).getOpportunity().getId()).isEqualTo(opp3.getId());
        assertThat(em.find(Product.class, productA2.getId()).getSortOrder()).isEqualTo(1);
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private ResultActions move(String login, String nodeType, Long nodeId, String parentType, Long parentId, Integer position)
        throws Exception {
        return move(user(login), nodeType, nodeId, parentType, parentId, position);
    }

    private ResultActions move(RequestPostProcessor who, String nodeType, Long nodeId, String parentType, Long parentId, Integer position)
        throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("nodeType", nodeType);
        body.put("nodeId", nodeId);
        body.put("parentType", parentType);
        body.put("parentId", parentId);
        body.put("position", position);
        return mvc.perform(
            post("/api/tree/nodes/move").with(who).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(body))
        );
    }

    private JsonNode moveOk(String login, String nodeType, Long nodeId, String parentType, Long parentId, Integer position)
        throws Exception {
        String json = move(login, nodeType, nodeId, parentType, parentId, position)
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        return om.readTree(json);
    }

    private static List<Map.Entry<String, Integer>> siblings(JsonNode res) {
        List<Map.Entry<String, Integer>> list = new ArrayList<>();
        for (JsonNode s : res.get("siblings")) {
            list.add(Map.entry(s.get("key").asText(), s.get("sortOrder").asInt()));
        }
        return list;
    }

    private List<String> movedSummaries(TreeNodeType type, Long id) {
        return data.history(type, id, HistoryEventType.MOVED).stream().map(NodeHistory::getSummary).toList();
    }
}
