package com.opportunity.tree.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opportunity.tree.IntegrationTest;
import com.opportunity.tree.domain.Assumption;
import com.opportunity.tree.domain.Evidence;
import com.opportunity.tree.domain.NodeHistory;
import com.opportunity.tree.domain.NodeLink;
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
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for {@link TreeNodeResource}: {@code POST /api/tree/nodes},
 * {@code PATCH /api/tree/nodes/{type}/{id}} and {@code DELETE /api/tree/nodes/{type}/{id}} —
 * the full parent × child matrix, creation defaults (incl. default links and CREATED history),
 * per-type patch fields, field applicability and ranges, history per event (none for title /
 * notes / owner / archived) and authorisation (viewer, non-member and admin non-member → 403).
 */
@IntegrationTest
@AutoConfigureMockMvc
@Transactional
class TreeNodeWriteResourceIT {

    private static final String OWNER = "write-owner";
    private static final String EDITOR = "write-editor";
    private static final String VIEWER = "write-viewer";
    private static final String OUTSIDER = "write-outsider";
    private static final String ADMIN = "write-admin";

    private static final Map<TreeNodeType, Set<TreeNodeType>> ALLOWED = Map.of(
        TreeNodeType.PRODUCT,
        EnumSet.of(TreeNodeType.OUTCOME),
        TreeNodeType.OUTCOME,
        EnumSet.of(TreeNodeType.OPPORTUNITY),
        TreeNodeType.OPPORTUNITY,
        EnumSet.of(TreeNodeType.OPPORTUNITY, TreeNodeType.SOLUTION, TreeNodeType.EVIDENCE),
        TreeNodeType.SOLUTION,
        EnumSet.of(TreeNodeType.ASSUMPTION),
        TreeNodeType.ASSUMPTION,
        EnumSet.of(TreeNodeType.EVIDENCE),
        TreeNodeType.EVIDENCE,
        EnumSet.noneOf(TreeNodeType.class)
    );

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper om;

    private OstTreeTestData data;

    private Team teamA;
    private User editorUser;
    private User outsiderUser;
    private Product product;
    private Outcome outcome;
    private Opportunity opportunity;
    private Solution solution;
    private Assumption assumption;
    private Evidence evidence;
    private Opportunity foreignOpportunity;

    @BeforeEach
    void setUp() {
        data = new OstTreeTestData(em);
        teamA = data.team("Write Team A");
        Team teamB = data.team("Write Team B");
        User owner = data.user(OWNER);
        editorUser = data.user(EDITOR);
        User viewer = data.user(VIEWER);
        outsiderUser = data.user(OUTSIDER);
        data.user(ADMIN);
        data.member(teamA, owner, TeamRole.OWNER);
        data.member(teamA, editorUser, TeamRole.EDITOR);
        data.member(teamA, viewer, TeamRole.VIEWER);
        data.member(teamB, outsiderUser, TeamRole.OWNER);

        product = data.product(teamA, "Discovery", 0);
        outcome = data.outcome(product, "Faster decisions", 0);
        opportunity = data.opportunity(outcome, null, "Research is scattered", 5);
        solution = data.solution(opportunity, "Insight library", 0);
        assumption = data.assumption(solution, "Teams will tag snippets", 0);
        evidence = data.evidence(opportunity, null, "Interview with PM", 0);

        Product foreignProduct = data.product(teamB, "Foreign", 0);
        foreignOpportunity = data.opportunity(data.outcome(foreignProduct, "Foreign outcome", 0), null, "Foreign opp", 0);
        em.flush();
        em.clear();
    }

    // ---------------------------------------------------------------------
    // Create — parent × child matrix
    // ---------------------------------------------------------------------

    @Test
    void createEnforcesTheFullParentChildMatrix() throws Exception {
        for (TreeNodeType parentType : TreeNodeType.values()) {
            for (TreeNodeType childType : TreeNodeType.values()) {
                ResultActions result = mvc.perform(
                    json(post("/api/tree/nodes"), create(childType, parentType, idOf(parentType), null)).with(user(EDITOR))
                );
                if (ALLOWED.get(parentType).contains(childType)) {
                    result
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.type").value(childType.name()))
                        .andExpect(jsonPath("$.parentKey").value(key(parentType, idOf(parentType))));
                } else {
                    result.andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("error.invalidparent"));
                }
            }
        }
    }

    @Test
    void evidenceUnderSolutionIsRejected() throws Exception {
        long before = count("Evidence");
        mvc
            .perform(
                json(post("/api/tree/nodes"), create(TreeNodeType.EVIDENCE, TreeNodeType.SOLUTION, solution.getId(), null)).with(
                    user(OWNER)
                )
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.invalidparent"));
        assertThat(count("Evidence")).isEqualTo(before);
    }

    // ---------------------------------------------------------------------
    // Create — defaults
    // ---------------------------------------------------------------------

    @Test
    void createOpportunityAppliesDefaultsLinksAndCreatedHistory() throws Exception {
        JsonNode node = createOk(TreeNodeType.OPPORTUNITY, TreeNodeType.OUTCOME, outcome.getId(), null);
        long id = node.get("id").asLong();

        assertThat(node.get("key").asText()).isEqualTo("opportunity-" + id);
        assertThat(node.get("title").asText()).isEqualTo("New opportunity");
        assertThat(node.get("status").asText()).isEqualTo("UNEXPLORED");
        assertThat(node.get("priority").asInt()).isEqualTo(50);
        assertThat(node.get("valueRating").asInt()).isEqualTo(3);
        assertThat(node.get("confidence").isNull()).isTrue();
        // Existing top-level opportunity under the outcome has sortOrder 5 → max + 1.
        assertThat(node.get("sortOrder").asInt()).isEqualTo(6);
        assertThat(node.get("createdDate").isNull()).isFalse();
        assertThat(node.get("lastModifiedDate").isNull()).isFalse();
        assertThat(linkNames(node)).containsExactly("Confluence", "Jira Initiative", "Jira Epic");
        assertThat(node.get("links").get(0).get("url").asText()).isEqualTo("https://ombuto.atlassian.net/wiki/discovery/opportunity-" + id);
        assertThat(node.get("links").get(1).get("url").asText()).isEqualTo("https://ombuto.atlassian.net/browse/INIT-000");

        List<NodeHistory> history = data.history(TreeNodeType.OPPORTUNITY, id);
        assertThat(history).hasSize(1);
        assertThat(history.get(0).getEventType()).isEqualTo(HistoryEventType.CREATED);
        assertThat(history.get(0).getSummary()).isEqualTo("Node created as opportunity");
        assertThat(history.get(0).getAuthor().getLogin()).isEqualTo(OWNER);
    }

    @Test
    void createAppliesPerTypeDefaults() throws Exception {
        JsonNode out = createOk(TreeNodeType.OUTCOME, TreeNodeType.PRODUCT, product.getId(), null);
        assertThat(out.get("title").asText()).isEqualTo("New outcome");
        assertThat(out.get("status").isNull()).isTrue();
        assertThat(out.get("sortOrder").asInt()).isEqualTo(1);
        assertThat(linkNames(out)).containsExactly("Confluence");

        JsonNode sol = createOk(TreeNodeType.SOLUTION, TreeNodeType.OPPORTUNITY, opportunity.getId(), null);
        assertThat(sol.get("title").asText()).isEqualTo("New solution");
        assertThat(sol.get("status").asText()).isEqualTo("CANDIDATE");
        assertThat(sol.get("sortOrder").asInt()).isEqualTo(1);
        assertThat(linkNames(sol)).containsExactly("Confluence", "Jira Initiative", "Jira Epic");

        JsonNode asm = createOk(TreeNodeType.ASSUMPTION, TreeNodeType.SOLUTION, solution.getId(), null);
        assertThat(asm.get("title").asText()).isEqualTo("New assumption");
        assertThat(asm.get("status").asText()).isEqualTo("UNTESTED");
        assertThat(asm.get("confidence").asInt()).isEqualTo(40);
        assertThat(asm.get("priority").isNull()).isTrue();
        assertThat(linkNames(asm)).containsExactly("Confluence");

        JsonNode ev = createOk(TreeNodeType.EVIDENCE, TreeNodeType.ASSUMPTION, assumption.getId(), null);
        assertThat(ev.get("title").asText()).isEqualTo("New snippet");
        assertThat(ev.get("parentKey").asText()).isEqualTo("assumption-" + assumption.getId());
        assertThat(ev.get("sortOrder").asInt()).isZero();
        assertThat(linkNames(ev)).containsExactly("Confluence", "Jira Ticket");

        JsonNode evOpp = createOk(TreeNodeType.EVIDENCE, TreeNodeType.OPPORTUNITY, opportunity.getId(), "  Quote from PM  ");
        assertThat(evOpp.get("title").asText()).isEqualTo("Quote from PM");
        assertThat(evOpp.get("sortOrder").asInt()).isEqualTo(1);

        for (JsonNode n : List.of(out, sol, asm, ev, evOpp)) {
            TreeNodeType type = TreeNodeType.valueOf(n.get("type").asText());
            assertThat(data.history(type, n.get("id").asLong(), HistoryEventType.CREATED)).hasSize(1);
        }
    }

    @Test
    void nestedOpportunityInheritsTheParentsOutcome() throws Exception {
        JsonNode child = createOk(TreeNodeType.OPPORTUNITY, TreeNodeType.OPPORTUNITY, opportunity.getId(), "Recruiting pain");
        assertThat(child.get("parentKey").asText()).isEqualTo("opportunity-" + opportunity.getId());
        assertThat(child.get("sortOrder").asInt()).isZero();
        Opportunity saved = em.find(Opportunity.class, child.get("id").asLong());
        assertThat(saved.getOutcome().getId()).isEqualTo(outcome.getId());
        assertThat(saved.getParent().getId()).isEqualTo(opportunity.getId());
    }

    @Test
    void createRejectsInvalidTitleAndUnknownTypes() throws Exception {
        mvc
            .perform(
                json(post("/api/tree/nodes"), create(TreeNodeType.SOLUTION, TreeNodeType.OPPORTUNITY, opportunity.getId(), "x")).with(
                    user(OWNER)
                )
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.invalidtitle"));
        mvc
            .perform(
                json(
                    post("/api/tree/nodes"),
                    create(TreeNodeType.SOLUTION, TreeNodeType.OPPORTUNITY, opportunity.getId(), "y".repeat(201))
                ).with(user(OWNER))
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.invalidtitle"));
        mvc
            .perform(
                json(post("/api/tree/nodes"), Map.of("type", "widget", "parentType", "outcome", "parentId", outcome.getId())).with(
                    user(OWNER)
                )
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.unknowntype"));
        mvc
            .perform(
                json(post("/api/tree/nodes"), Map.of("type", "opportunity", "parentType", "gadget", "parentId", outcome.getId())).with(
                    user(OWNER)
                )
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.unknowntype"));
        mvc
            .perform(json(post("/api/tree/nodes"), Map.of("type", "opportunity", "parentType", "outcome")).with(user(OWNER)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.parentmissing"));
    }

    @Test
    void createRejectsAFractionalParentIdInsteadOfTruncatingIt() throws Exception {
        long before = count("Solution");
        for (String parentId : List.of(opportunity.getId() + ".7", opportunity.getId() + ".5", "1e300", "18446744073709551666")) {
            mvc
                .perform(
                    post("/api/tree/nodes")
                        .with(csrf())
                        .with(user(OWNER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\": \"solution\", \"parentType\": \"opportunity\", \"parentId\": " + parentId + "}")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("error.parentmissing"));
        }
        assertThat(count("Solution")).isEqualTo(before);
        // A whole number written as a decimal is still that id.
        mvc
            .perform(
                post("/api/tree/nodes")
                    .with(csrf())
                    .with(user(OWNER))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"type\": \"solution\", \"parentType\": \"opportunity\", \"parentId\": " + opportunity.getId() + ".0}")
            )
            .andExpect(status().isCreated());
    }

    @Test
    void createIsAcceptedWithLowerCaseTypesAndReturnsLocation() throws Exception {
        mvc
            .perform(
                json(
                    post("/api/tree/nodes"),
                    Map.of("type", "solution", "parentType", "opportunity", "parentId", opportunity.getId())
                ).with(user(EDITOR))
            )
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/api/tree/nodes/solution/")))
            .andExpect(jsonPath("$.type").value("SOLUTION"));
    }

    @Test
    void viewerNonMemberAndAdminNonMemberCannotCreate() throws Exception {
        long before = count("Opportunity");
        long solutionsBefore = count("Solution");
        for (MockHttpServletRequestBuilder req : List.of(
            json(post("/api/tree/nodes"), create(TreeNodeType.OPPORTUNITY, TreeNodeType.OUTCOME, outcome.getId(), null)).with(user(VIEWER)),
            json(post("/api/tree/nodes"), create(TreeNodeType.OPPORTUNITY, TreeNodeType.OUTCOME, outcome.getId(), null)).with(
                user(OUTSIDER)
            ),
            json(post("/api/tree/nodes"), create(TreeNodeType.OPPORTUNITY, TreeNodeType.OUTCOME, outcome.getId(), null)).with(
                user(ADMIN).roles("ADMIN", "USER")
            ),
            // Unknown parent id → the same 403 (no existence leak).
            json(post("/api/tree/nodes"), create(TreeNodeType.OPPORTUNITY, TreeNodeType.OUTCOME, Long.MAX_VALUE, null)).with(user(OWNER)),
            // Editor of team A cannot create under team B's opportunity.
            json(post("/api/tree/nodes"), create(TreeNodeType.SOLUTION, TreeNodeType.OPPORTUNITY, foreignOpportunity.getId(), null)).with(
                user(EDITOR)
            )
        )) {
            mvc.perform(req).andExpect(status().isForbidden());
        }
        assertThat(count("Opportunity")).isEqualTo(before);
        assertThat(count("Solution")).isEqualTo(solutionsBefore);
    }

    @Test
    void viewerAndNonMemberGet403OnCreateUnderEveryParentType() throws Exception {
        Map<String, Long> before = new HashMap<>();
        for (String entity : List.of("Outcome", "Opportunity", "Solution", "Assumption", "Evidence")) {
            before.put(entity, count(entity));
        }
        for (TreeNodeType parentType : TreeNodeType.values()) {
            for (TreeNodeType childType : ALLOWED.get(parentType)) {
                for (String login : List.of(VIEWER, OUTSIDER)) {
                    mvc
                        .perform(json(post("/api/tree/nodes"), create(childType, parentType, idOf(parentType), null)).with(user(login)))
                        .andExpect(status().isForbidden());
                }
            }
        }
        before.forEach((entity, n) -> assertThat(count(entity)).as(entity).isEqualTo(n));
    }

    @Test
    void viewerWithAnInvalidTitleGets403Not400() throws Exception {
        // Access is checked before the body is validated, as on PATCH.
        mvc
            .perform(
                json(post("/api/tree/nodes"), create(TreeNodeType.SOLUTION, TreeNodeType.OPPORTUNITY, opportunity.getId(), "x")).with(
                    user(VIEWER)
                )
            )
            .andExpect(status().isForbidden());
        mvc
            .perform(
                json(post("/api/tree/nodes"), create(TreeNodeType.SOLUTION, TreeNodeType.OPPORTUNITY, opportunity.getId(), "x")).with(
                    user(OUTSIDER)
                )
            )
            .andExpect(status().isForbidden());
    }

    @Test
    void aWholeBranchCanBeBuiltThroughTheApiMoreThanThreeLevelsDeep() throws Exception {
        JsonNode newOutcome = createOk(TreeNodeType.OUTCOME, TreeNodeType.PRODUCT, product.getId(), "Deep outcome");
        JsonNode opp = createOk(TreeNodeType.OPPORTUNITY, TreeNodeType.OUTCOME, newOutcome.get("id").asLong(), "Level 2");
        JsonNode nested = createOk(TreeNodeType.OPPORTUNITY, TreeNodeType.OPPORTUNITY, opp.get("id").asLong(), "Level 3");
        JsonNode nested2 = createOk(TreeNodeType.OPPORTUNITY, TreeNodeType.OPPORTUNITY, nested.get("id").asLong(), "Level 4");
        JsonNode sol = createOk(TreeNodeType.SOLUTION, TreeNodeType.OPPORTUNITY, nested2.get("id").asLong(), "Level 5");
        JsonNode ass = createOk(TreeNodeType.ASSUMPTION, TreeNodeType.SOLUTION, sol.get("id").asLong(), "Level 6");
        JsonNode ev = createOk(TreeNodeType.EVIDENCE, TreeNodeType.ASSUMPTION, ass.get("id").asLong(), "Level 7");

        assertThat(newOutcome.get("parentKey").asText()).isEqualTo(key(TreeNodeType.PRODUCT, product.getId()));
        assertThat(opp.get("parentKey").asText()).isEqualTo(newOutcome.get("key").asText());
        assertThat(nested.get("parentKey").asText()).isEqualTo(opp.get("key").asText());
        assertThat(nested2.get("parentKey").asText()).isEqualTo(nested.get("key").asText());
        assertThat(sol.get("parentKey").asText()).isEqualTo(nested2.get("key").asText());
        assertThat(ass.get("parentKey").asText()).isEqualTo(sol.get("key").asText());
        assertThat(ev.get("parentKey").asText()).isEqualTo(ass.get("key").asText());
        // Nested opportunities share the top-level opportunity's outcome.
        assertThat(em.find(Opportunity.class, nested2.get("id").asLong()).getOutcome().getId()).isEqualTo(newOutcome.get("id").asLong());
    }

    @Test
    void productDeleteThroughTheProductApiCascadesItsLinksAndTree() throws Exception {
        // A ROLE_ADMIN user who owns the team (the generated admin screen's DELETE /api/products/{id}).
        User admin = em.createQuery("select u from User u where u.login = :l", User.class).setParameter("l", ADMIN).getSingleResult();
        data.member(em.find(Team.class, teamA.getId()), admin, TeamRole.OWNER);
        Product doomed = data.product(em.find(Team.class, teamA.getId()), "Doomed", 1);
        em.flush();
        JsonNode doomedOutcome = createOk(TreeNodeType.OUTCOME, TreeNodeType.PRODUCT, doomed.getId(), "Doomed outcome");
        mvc
            .perform(
                json(
                    post("/api/tree/nodes/product/{id}/links", doomed.getId()),
                    Map.of("name", "Space", "url", "https://x.example.com")
                ).with(user(OWNER))
            )
            .andExpect(status().isCreated());

        mvc.perform(delete("/api/products/{id}", doomed.getId()).with(user(VIEWER)).with(csrf())).andExpect(status().isForbidden());
        mvc
            .perform(delete("/api/products/{id}", doomed.getId()).with(user(ADMIN).roles("ADMIN", "USER")).with(csrf()))
            .andExpect(status().isNoContent());
        em.clear();
        assertThat(em.find(Product.class, doomed.getId())).isNull();
        assertThat(em.find(Outcome.class, doomedOutcome.get("id").asLong())).isNull();
        assertThat(
            em
                .createQuery("select count(l) from NodeLink l where l.product.id = :id", Long.class)
                .setParameter("id", doomed.getId())
                .getSingleResult()
        ).isZero();
        // The rest of team A's tree is untouched.
        assertThat(em.find(Product.class, product.getId())).isNotNull();
    }

    @Test
    void productCreatedThroughProductApiGetsItsDefaultLink() throws Exception {
        Map<String, Object> body = Map.of("name", "Brand new product", "archived", false, "team", Map.of("id", teamA.getId()));
        String json = mvc
            .perform(json(post("/api/products"), body).with(user(OWNER)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
        long productId = om.readTree(json).get("id").asLong();
        List<NodeLink> links = em
            .createQuery("select l from NodeLink l where l.product.id = :id", NodeLink.class)
            .setParameter("id", productId)
            .getResultList();
        assertThat(links).extracting(NodeLink::getName).containsExactly("Product space");
        assertThat(links.get(0).getUrl()).isEqualTo("https://ombuto.atlassian.net/wiki/spaces/product-" + productId);
    }

    // ---------------------------------------------------------------------
    // Patch — per type
    // ---------------------------------------------------------------------

    @Test
    void patchOpportunityFieldsAndHistory() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("title", "Research is scattered everywhere");
        body.put("notes", "From the Q3 interviews");
        body.put("status", "validated");
        body.put("priority", 72);
        body.put("valueRating", 5);
        mvc
            .perform(mergePatch("opportunity", opportunity.getId(), body).with(user(EDITOR)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.key").value("opportunity-" + opportunity.getId()))
            .andExpect(jsonPath("$.title").value("Research is scattered everywhere"))
            .andExpect(jsonPath("$.notes").value("From the Q3 interviews"))
            .andExpect(jsonPath("$.status").value("VALIDATED"))
            .andExpect(jsonPath("$.priority").value(72))
            .andExpect(jsonPath("$.valueRating").value(5))
            .andExpect(jsonPath("$.lastModifiedDate").isNotEmpty());

        assertThat(summaries(TreeNodeType.OPPORTUNITY, opportunity.getId())).containsExactly(
            "Status changed to “validated”",
            "Priority set to high",
            "Value set to $$$$$"
        );
        assertThat(data.history(TreeNodeType.OPPORTUNITY, opportunity.getId(), HistoryEventType.STATUS_CHANGED)).hasSize(1);
        assertThat(data.history(TreeNodeType.OPPORTUNITY, opportunity.getId(), HistoryEventType.PRIORITY_CHANGED)).hasSize(1);
        assertThat(data.history(TreeNodeType.OPPORTUNITY, opportunity.getId(), HistoryEventType.VALUE_CHANGED)).hasSize(1);
    }

    @Test
    void priorityWithinTheSameBandAndUnchangedValuesWriteNoHistory() throws Exception {
        // 50 → 53 stays in band 5; status and value are set to their current values.
        mvc
            .perform(
                mergePatch("opportunity", opportunity.getId(), Map.of("priority", 53, "status", "UNEXPLORED", "valueRating", 3)).with(
                    user(OWNER)
                )
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.priority").value(53));
        assertThat(data.history(TreeNodeType.OPPORTUNITY, opportunity.getId())).isEmpty();

        // 53 → 56 crosses into band 6.
        mvc.perform(mergePatch("opportunity", opportunity.getId(), Map.of("priority", 56)).with(user(OWNER))).andExpect(status().isOk());
        assertThat(summaries(TreeNodeType.OPPORTUNITY, opportunity.getId())).containsExactly("Priority set to medium");
    }

    @Test
    void titleNotesOwnerAndArchivedWriteNoHistory() throws Exception {
        mvc
            .perform(
                mergePatch("product", product.getId(), Map.of("title", "Discovery Canvas", "notes", "n", "archived", true)).with(
                    user(OWNER)
                )
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Discovery Canvas"))
            .andExpect(jsonPath("$.archived").value(true));
        assertThat(em.find(Product.class, product.getId()).getName()).isEqualTo("Discovery Canvas");

        mvc
            .perform(mergePatch("outcome", outcome.getId(), Map.of("title", "Faster calls", "notes", "x")).with(user(OWNER)))
            .andExpect(status().isOk());
        mvc
            .perform(mergePatch("solution", solution.getId(), Map.of("title", "Library v2", "notes", "x")).with(user(OWNER)))
            .andExpect(status().isOk());
        mvc
            .perform(mergePatch("evidence", evidence.getId(), Map.of("title", "Interview #2", "notes", "x")).with(user(OWNER)))
            .andExpect(status().isOk());
        mvc
            .perform(
                mergePatch("assumption", assumption.getId(), Map.of("title", "Teams tag", "notes", "x", "ownerLogin", EDITOR)).with(
                    user(OWNER)
                )
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ownerLogin").value(EDITOR));

        assertThat(data.history(TreeNodeType.PRODUCT, product.getId())).isEmpty();
        assertThat(data.history(TreeNodeType.OUTCOME, outcome.getId())).isEmpty();
        assertThat(data.history(TreeNodeType.SOLUTION, solution.getId())).isEmpty();
        assertThat(data.history(TreeNodeType.EVIDENCE, evidence.getId())).isEmpty();
        assertThat(data.history(TreeNodeType.ASSUMPTION, assumption.getId())).isEmpty();
    }

    @Test
    void patchAssumptionStatementConfidenceStatusAndOwner() throws Exception {
        mvc
            .perform(
                mergePatch(
                    "assumption",
                    assumption.getId(),
                    Map.of("title", "Teams will tag", "confidence", 75, "status", "supported")
                ).with(user(EDITOR))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Teams will tag"))
            .andExpect(jsonPath("$.confidence").value(75))
            .andExpect(jsonPath("$.status").value("SUPPORTED"));
        assertThat(em.find(Assumption.class, assumption.getId()).getStatement()).isEqualTo("Teams will tag");
        assertThat(summaries(TreeNodeType.ASSUMPTION, assumption.getId())).containsExactly(
            "Status changed to “supported”",
            "Confidence set to 75%"
        );

        mvc
            .perform(mergePatch("assumption", assumption.getId(), Map.of("ownerLogin", EDITOR)).with(user(EDITOR)))
            .andExpect(jsonPath("$.ownerLogin").value(EDITOR));
        Map<String, Object> clear = new HashMap<>();
        clear.put("ownerLogin", null);
        mvc
            .perform(mergePatch("assumption", assumption.getId(), clear).with(user(EDITOR)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ownerLogin").value(nullValue()));

        for (String login : List.of(OUTSIDER, "no-such-user")) {
            mvc
                .perform(mergePatch("assumption", assumption.getId(), Map.of("ownerLogin", login)).with(user(EDITOR)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("error.ownernotmember"));
        }
    }

    @Test
    void patchSolutionStatusWritesHistory() throws Exception {
        mvc
            .perform(
                patch("/api/tree/nodes/solution/{id}", solution.getId())
                    .with(csrf())
                    .with(user(OWNER))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"status\":\"BUILDING\"}")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("BUILDING"));
        assertThat(summaries(TreeNodeType.SOLUTION, solution.getId())).containsExactly("Status changed to “building”");
    }

    @Test
    void patchRejectsInapplicableAndUnknownFields() throws Exception {
        Object[][] cases = {
            { "solution", solution.getId(), "priority", 10 },
            { "solution", solution.getId(), "confidence", 10 },
            { "opportunity", opportunity.getId(), "confidence", 10 },
            { "opportunity", opportunity.getId(), "ownerLogin", EDITOR },
            { "opportunity", opportunity.getId(), "archived", true },
            { "outcome", outcome.getId(), "status", "EXPLORING" },
            { "outcome", outcome.getId(), "valueRating", 2 },
            { "product", product.getId(), "status", "EXPLORING" },
            { "evidence", evidence.getId(), "priority", 10 },
            { "assumption", assumption.getId(), "valueRating", 2 },
        };
        for (Object[] c : cases) {
            mvc
                .perform(mergePatch((String) c[0], (Long) c[1], Map.of((String) c[2], c[3])).with(user(OWNER)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("error.fieldnotapplicable"));
        }
        mvc
            .perform(mergePatch("opportunity", opportunity.getId(), Map.of("sortOrder", 3)).with(user(OWNER)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.unknownfield"));
    }

    @Test
    void patchValidatesRanges() throws Exception {
        Object[][] cases = {
            { "assumption", assumption.getId(), "confidence", 101, "error.invalidconfidence" },
            { "assumption", assumption.getId(), "confidence", -1, "error.invalidconfidence" },
            { "opportunity", opportunity.getId(), "priority", 0, "error.invalidpriority" },
            { "opportunity", opportunity.getId(), "priority", 101, "error.invalidpriority" },
            { "opportunity", opportunity.getId(), "valueRating", 0, "error.invalidvaluerating" },
            { "opportunity", opportunity.getId(), "valueRating", 6, "error.invalidvaluerating" },
            { "opportunity", opportunity.getId(), "status", "SHIPPED", "error.invalidstatus" },
            { "assumption", assumption.getId(), "status", "bogus", "error.invalidstatus" },
            { "opportunity", opportunity.getId(), "title", "", "error.invalidtitle" },
            { "opportunity", opportunity.getId(), "title", "z".repeat(201), "error.invalidtitle" },
            { "product", product.getId(), "title", "z".repeat(101), "error.invalidtitle" },
            { "product", product.getId(), "archived", "yes", "error.invalidarchived" },
        };
        for (Object[] c : cases) {
            mvc
                .perform(mergePatch((String) c[0], (Long) c[1], Map.of((String) c[2], c[3])).with(user(OWNER)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value((String) c[4]));
        }
        // Boundaries are accepted; a 500-character assumption statement is valid.
        mvc
            .perform(mergePatch("assumption", assumption.getId(), Map.of("confidence", 0, "title", "a".repeat(500))).with(user(OWNER)))
            .andExpect(status().isOk());
        mvc
            .perform(mergePatch("opportunity", opportunity.getId(), Map.of("priority", 100, "valueRating", 1)).with(user(OWNER)))
            .andExpect(status().isOk());
        Map<String, Object> clearNotes = new HashMap<>();
        clearNotes.put("notes", null);
        mvc
            .perform(mergePatch("opportunity", opportunity.getId(), clearNotes).with(user(OWNER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notes").value(nullValue()));
    }

    @Test
    void numbersOutsideTheLongRangeAreRejectedNotWrapped() throws Exception {
        // 2^64 + 50 would wrap to 50 with Number.longValue().
        for (String body : List.of(
            "{\"priority\": 18446744073709551666}",
            "{\"priority\": -18446744073709551566}",
            "{\"valueRating\": 18446744073709551619}",
            "{\"priority\": 50.5}",
            "{\"priority\": 1e300}"
        )) {
            mvc
                .perform(
                    patch("/api/tree/nodes/{type}/{id}", "opportunity", opportunity.getId())
                        .with(csrf())
                        .with(user(OWNER))
                        .contentType("application/merge-patch+json")
                        .content(body)
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                    jsonPath("$.message").value(body.contains("valueRating") ? "error.invalidvaluerating" : "error.invalidpriority")
                );
        }
        // Integral values written as decimals are still fine.
        mvc
            .perform(
                patch("/api/tree/nodes/{type}/{id}", "opportunity", opportunity.getId())
                    .with(csrf())
                    .with(user(OWNER))
                    .contentType("application/merge-patch+json")
                    .content("{\"priority\": 60.0}")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.priority").value(60));
    }

    @Test
    void patchAdvancesLastModifiedDateAndKeepsCreatedDate() throws Exception {
        java.time.Instant old = java.time.Instant.parse("2020-01-02T03:04:05Z");
        for (String entity : List.of("Outcome", "Opportunity", "Solution", "Assumption", "Evidence")) {
            em
                .createQuery("update " + entity + " x set x.createdDate = :d, x.lastModifiedDate = :d")
                .setParameter("d", old)
                .executeUpdate();
        }
        em.clear();
        java.time.Instant beforePatch = java.time.Instant.now().minusSeconds(1);
        for (TreeNodeType type : List.of(
            TreeNodeType.OUTCOME,
            TreeNodeType.OPPORTUNITY,
            TreeNodeType.SOLUTION,
            TreeNodeType.ASSUMPTION,
            TreeNodeType.EVIDENCE
        )) {
            String json = mvc
                .perform(mergePatch(type.name().toLowerCase(Locale.ROOT), idOf(type), Map.of("notes", "touched")).with(user(EDITOR)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
            JsonNode node = om.readTree(json);
            assertThat(java.time.Instant.parse(node.get("createdDate").asText())).as(type + " createdDate").isEqualTo(old);
            assertThat(java.time.Instant.parse(node.get("lastModifiedDate").asText())).as(type + " lastModifiedDate").isAfter(beforePatch);
        }
    }

    @Test
    void viewerNonMemberAndAdminNonMemberCannotPatch() throws Exception {
        for (MockHttpServletRequestBuilder req : List.of(
            mergePatch("opportunity", opportunity.getId(), Map.of("status", "PARKED")).with(user(VIEWER)),
            mergePatch("opportunity", opportunity.getId(), Map.of("status", "PARKED")).with(user(OUTSIDER)),
            mergePatch("opportunity", opportunity.getId(), Map.of("status", "PARKED")).with(user(ADMIN).roles("ADMIN", "USER")),
            mergePatch("opportunity", Long.MAX_VALUE, Map.of("status", "PARKED")).with(user(OWNER)),
            // Even an inapplicable field is a 403 for a viewer — access is checked first.
            mergePatch("opportunity", opportunity.getId(), Map.of("confidence", 1)).with(user(VIEWER))
        )) {
            mvc.perform(req).andExpect(status().isForbidden());
        }
        assertThat(em.find(Opportunity.class, opportunity.getId()).getStatus().name()).isEqualTo("UNEXPLORED");
        assertThat(data.history(TreeNodeType.OPPORTUNITY, opportunity.getId())).isEmpty();
    }

    @Test
    void unknownTypeInPathIsBadRequest() throws Exception {
        mvc
            .perform(mergePatch("widget", opportunity.getId(), Map.of("title", "Hello")).with(user(OWNER)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.unknowntype"));
        mvc
            .perform(delete("/api/tree/nodes/widget/{id}", opportunity.getId()).with(user(OWNER)).with(csrf()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.unknowntype"));
    }

    // ---------------------------------------------------------------------
    // Delete — through the new endpoint (full cascade coverage in TreeNodeCascadeResourceIT)
    // ---------------------------------------------------------------------

    @Test
    void createdNodeCanBeDeletedWithItsLinksAndHistory() throws Exception {
        JsonNode node = createOk(TreeNodeType.SOLUTION, TreeNodeType.OPPORTUNITY, opportunity.getId(), "Throwaway");
        long id = node.get("id").asLong();
        mvc.perform(delete("/api/tree/nodes/SOLUTION/{id}", id).with(user(VIEWER)).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(delete("/api/tree/nodes/solution/{id}", id).with(user(EDITOR)).with(csrf())).andExpect(status().isNoContent());
        em.clear();
        assertThat(em.find(Solution.class, id)).isNull();
        assertThat(data.history(TreeNodeType.SOLUTION, id)).isEmpty();
        assertThat(
            em.createQuery("select count(l) from NodeLink l where l.solution.id = :id", Long.class).setParameter("id", id).getSingleResult()
        ).isZero();
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private Long idOf(TreeNodeType type) {
        return switch (type) {
            case PRODUCT -> product.getId();
            case OUTCOME -> outcome.getId();
            case OPPORTUNITY -> opportunity.getId();
            case SOLUTION -> solution.getId();
            case ASSUMPTION -> assumption.getId();
            case EVIDENCE -> evidence.getId();
        };
    }

    private static String key(TreeNodeType type, Long id) {
        return type.name().toLowerCase(Locale.ROOT) + "-" + id;
    }

    private static Map<String, Object> create(TreeNodeType type, TreeNodeType parentType, Long parentId, String title) {
        Map<String, Object> body = new HashMap<>();
        body.put("type", type.name());
        body.put("parentType", parentType.name());
        body.put("parentId", parentId);
        if (title != null) {
            body.put("title", title);
        }
        return body;
    }

    private JsonNode createOk(TreeNodeType type, TreeNodeType parentType, Long parentId, String title) throws Exception {
        String json = mvc
            .perform(json(post("/api/tree/nodes"), create(type, parentType, parentId, title)).with(user(OWNER)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
        return om.readTree(json);
    }

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder builder, Object body) throws Exception {
        return builder.with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(body));
    }

    private MockHttpServletRequestBuilder mergePatch(String type, Long id, Map<String, Object> body) throws Exception {
        return patch("/api/tree/nodes/{type}/{id}", type, id)
            .with(csrf())
            .contentType("application/merge-patch+json")
            .content(om.writeValueAsBytes(body));
    }

    private static List<String> linkNames(JsonNode node) {
        return java.util.stream.StreamSupport.stream(node.get("links").spliterator(), false)
            .map(l -> l.get("name").asText())
            .toList();
    }

    private List<String> summaries(TreeNodeType type, Long id) {
        return data.history(type, id).stream().map(NodeHistory::getSummary).toList();
    }

    private long count(String entity) {
        return em.createQuery("select count(e) from " + entity + " e", Long.class).getSingleResult();
    }
}
