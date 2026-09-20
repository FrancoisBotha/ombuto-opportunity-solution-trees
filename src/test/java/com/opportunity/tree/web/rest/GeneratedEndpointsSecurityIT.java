package com.opportunity.tree.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opportunity.tree.IntegrationTest;
import com.opportunity.tree.domain.Product;
import com.opportunity.tree.domain.Team;
import com.opportunity.tree.domain.TeamMember;
import com.opportunity.tree.domain.enumeration.AssumptionStatus;
import com.opportunity.tree.domain.enumeration.HistoryEventType;
import com.opportunity.tree.domain.enumeration.OpportunityStatus;
import com.opportunity.tree.domain.enumeration.SolutionStatus;
import com.opportunity.tree.domain.enumeration.TeamRole;
import com.opportunity.tree.domain.enumeration.TreeNodeType;
import com.opportunity.tree.repository.ProductRepository;
import com.opportunity.tree.repository.TeamMemberRepository;
import com.opportunity.tree.repository.TeamRepository;
import com.opportunity.tree.service.dto.AssumptionDTO;
import com.opportunity.tree.service.dto.CommentDTO;
import com.opportunity.tree.service.dto.EvidenceDTO;
import com.opportunity.tree.service.dto.InterviewDTO;
import com.opportunity.tree.service.dto.NodeHistoryDTO;
import com.opportunity.tree.service.dto.NodeLinkDTO;
import com.opportunity.tree.service.dto.OpenQuestionDTO;
import com.opportunity.tree.service.dto.OpportunityDTO;
import com.opportunity.tree.service.dto.OutcomeDTO;
import com.opportunity.tree.service.dto.ProductDTO;
import com.opportunity.tree.service.dto.SolutionDTO;
import com.opportunity.tree.service.dto.TagDTO;
import com.opportunity.tree.service.dto.TeamDTO;
import com.opportunity.tree.service.dto.TeamMemberDTO;
import com.opportunity.tree.service.dto.UserDTO;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.RestController;

/**
 * Verifies TEAMS-004 acceptance criterion 3: a ROLE_USER who is not a member
 * of a team cannot list, read, create, update, patch or delete that team's
 * Team, TeamMember or Product rows through the generated CRUD endpoints
 * (per HTTP verb), and denials return 403/404 without revealing whether the
 * resource exists (criterion 4).
 *
 * <p>The class runs each request as a ROLE_USER whose login is NOT in the
 * TeamMember table for the pre-seeded team. Team and TeamMember endpoints are
 * locked to ROLE_ADMIN at the controller and must therefore 403 outright.
 * ProductResource is routed through TeamAccessService and must uniformly deny
 * non-members (403 on writes; empty list on GET-all; 404 on GET-one — matching
 * a non-existent id so existence cannot be probed).
 *
 * <p>The OST node entities (Outcome, Opportunity, Solution, Assumption, Evidence,
 * NodeLink, OpenQuestion, NodeHistory) and Comment are locked to ROLE_ADMIN the same
 * way as Team and TeamMember; plain users write the tree only through
 * {@code /api/tree/**}.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser(username = "not-a-member-teams004", authorities = { "ROLE_USER" })
class GeneratedEndpointsSecurityIT {

    @Autowired
    private ObjectMapper om;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TeamMemberRepository teamMemberRepository;

    @Autowired
    private ProductRepository productRepository;

    private Team otherTeam;
    private TeamMember otherMembership;
    private Product otherProduct;

    @BeforeEach
    void seedOtherTeam() {
        otherTeam = new Team().name("Other Team " + System.nanoTime()).description("owned by other").createdDate(Instant.now());
        em.persist(otherTeam);
        // The signed-in user is NOT added as a member of otherTeam.
        otherProduct = new Product()
            .name("Other Product " + System.nanoTime())
            .description("d")
            .vision("v")
            .archived(Boolean.FALSE)
            .sortOrder(0)
            .createdDate(Instant.now());
        otherProduct.setTeam(otherTeam);
        em.persist(otherProduct);
        em.flush();
    }

    @AfterEach
    void cleanup() {
        if (otherProduct != null && otherProduct.getId() != null) {
            productRepository.deleteById(otherProduct.getId());
        }
        if (otherMembership != null && otherMembership.getId() != null) {
            teamMemberRepository.deleteById(otherMembership.getId());
        }
        if (otherTeam != null && otherTeam.getId() != null) {
            teamRepository.deleteById(otherTeam.getId());
        }
    }

    // ---------------------------------------------------------------------
    // Team — locked to ROLE_ADMIN
    // ---------------------------------------------------------------------

    @Test
    @Transactional
    void teamEndpointsDenyNonAdminOnEveryVerb() throws Exception {
        TeamDTO body = new TeamDTO();
        body.setId(otherTeam.getId());
        body.setName("valid-name");
        body.setCreatedDate(Instant.now());

        mvc.perform(get("/api/teams")).andExpect(status().isForbidden());
        mvc.perform(get("/api/teams/{id}", otherTeam.getId())).andExpect(status().isForbidden());
        mvc
            .perform(post("/api/teams").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(body)))
            .andExpect(status().isForbidden());
        mvc
            .perform(
                put("/api/teams/{id}", otherTeam.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(body))
            )
            .andExpect(status().isForbidden());
        mvc
            .perform(
                patch("/api/teams/{id}", otherTeam.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(body))
            )
            .andExpect(status().isForbidden());
        mvc.perform(delete("/api/teams/{id}", otherTeam.getId()).with(csrf())).andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------------
    // TeamMember — locked to ROLE_ADMIN
    // ---------------------------------------------------------------------

    @Test
    @Transactional
    void teamMemberEndpointsDenyNonAdminOnEveryVerb() throws Exception {
        TeamDTO teamRef = new TeamDTO();
        teamRef.setId(otherTeam.getId());
        UserDTO userRef = new UserDTO();
        TeamMemberDTO body = new TeamMemberDTO();
        body.setId(1L);
        body.setRole(TeamRole.VIEWER);
        body.setJoinedDate(Instant.now());
        body.setTeam(teamRef);
        body.setUser(userRef);

        mvc.perform(get("/api/team-members")).andExpect(status().isForbidden());
        mvc.perform(get("/api/team-members/{id}", 1L)).andExpect(status().isForbidden());
        mvc
            .perform(post("/api/team-members").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(body)))
            .andExpect(status().isForbidden());
        mvc
            .perform(
                put("/api/team-members/{id}", 1L).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(body))
            )
            .andExpect(status().isForbidden());
        mvc
            .perform(
                patch("/api/team-members/{id}", 1L)
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(body))
            )
            .andExpect(status().isForbidden());
        mvc.perform(delete("/api/team-members/{id}", 1L).with(csrf())).andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------------
    // Product — routed through TeamAccessService
    // ---------------------------------------------------------------------

    @Test
    @Transactional
    void productEndpointsDenyNonMemberOnEveryVerb() throws Exception {
        TeamDTO teamRef = new TeamDTO();
        teamRef.setId(otherTeam.getId());
        ProductDTO body = new ProductDTO();
        body.setName("hijack");
        body.setDescription("d");
        body.setVision("v");
        body.setArchived(Boolean.FALSE);
        body.setCreatedDate(Instant.now());
        body.setTeam(teamRef);

        // GET-all: not-a-member sees an empty list — the other team's product
        // is filtered out (findAllForCurrentUser). Cannot be listed.
        mvc
            .perform(get("/api/products"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.id == " + otherProduct.getId() + ")]").isEmpty());

        // GET-one: uniform 404 whether the product exists or not (NFR-002).
        mvc.perform(get("/api/products/{id}", otherProduct.getId())).andExpect(status().isNotFound());

        // POST: creating for another team is 403.
        mvc
            .perform(post("/api/products").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(body)))
            .andExpect(status().isForbidden());

        // PUT: updating another team's product is 403.
        ProductDTO update = new ProductDTO();
        update.setId(otherProduct.getId());
        update.setName("hijacked");
        update.setArchived(Boolean.FALSE);
        update.setCreatedDate(Instant.now());
        update.setTeam(teamRef);
        mvc
            .perform(
                put("/api/products/{id}", otherProduct.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(update))
            )
            .andExpect(status().isForbidden());

        // PATCH: partial update of another team's product is 403.
        mvc
            .perform(
                patch("/api/products/{id}", otherProduct.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(update))
            )
            .andExpect(status().isForbidden());

        // DELETE: another team's product is 403.
        mvc.perform(delete("/api/products/{id}", otherProduct.getId()).with(csrf())).andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------------
    // Evidence, NodeLink, OpenQuestion, NodeHistory — locked to ROLE_ADMIN
    // ---------------------------------------------------------------------

    @Test
    @Transactional
    void evidenceEndpointsDenyNonAdminOnEveryVerb() throws Exception {
        EvidenceDTO body = new EvidenceDTO();
        body.setId(1L);
        body.setTitle("valid title");
        body.setSortOrder(0);
        body.setCreatedDate(Instant.now());
        assertAdminOnlyOnEveryVerb("/api/evidences", body);
    }

    @Test
    @Transactional
    void nodeLinkEndpointsDenyNonAdminOnEveryVerb() throws Exception {
        NodeLinkDTO body = new NodeLinkDTO();
        body.setId(1L);
        body.setName("Doc");
        body.setUrl("https://example.com/doc");
        body.setSortOrder(0);
        body.setCreatedDate(Instant.now());
        assertAdminOnlyOnEveryVerb("/api/node-links", body);
    }

    @Test
    @Transactional
    void openQuestionEndpointsDenyNonAdminOnEveryVerb() throws Exception {
        OpportunityDTO opportunityRef = new OpportunityDTO();
        opportunityRef.setId(1L);
        OpenQuestionDTO body = new OpenQuestionDTO();
        body.setId(1L);
        body.setQuestionText("Why?");
        body.setDone(Boolean.FALSE);
        body.setSortOrder(0);
        body.setCreatedDate(Instant.now());
        body.setOpportunity(opportunityRef);
        assertAdminOnlyOnEveryVerb("/api/open-questions", body);
    }

    @Test
    @Transactional
    void nodeHistoryEndpointsDenyNonAdminOnEveryVerb() throws Exception {
        NodeHistoryDTO body = new NodeHistoryDTO();
        body.setId(1L);
        body.setNodeType(TreeNodeType.OPPORTUNITY);
        body.setNodeId(1L);
        body.setEventType(HistoryEventType.CREATED);
        body.setSummary("Created");
        body.setCreatedDate(Instant.now());
        assertAdminOnlyOnEveryVerb("/api/node-histories", body);
    }

    // ---------------------------------------------------------------------
    // Outcome, Opportunity, Solution, Assumption, Comment — locked to ROLE_ADMIN.
    // Plain users write the tree only through /api/tree/** (TeamAccessService).
    // ---------------------------------------------------------------------

    @Test
    @Transactional
    void outcomeEndpointsDenyNonAdminOnEveryVerb() throws Exception {
        ProductDTO productRef = new ProductDTO();
        productRef.setId(otherProduct.getId());
        OutcomeDTO body = new OutcomeDTO();
        body.setId(1L);
        body.setTitle("valid title");
        body.setSortOrder(0);
        body.setCreatedDate(Instant.now());
        body.setProduct(productRef);
        assertAdminOnlyOnEveryVerb("/api/outcomes", body);
    }

    @Test
    @Transactional
    void opportunityEndpointsDenyNonAdminOnEveryVerb() throws Exception {
        OutcomeDTO outcomeRef = new OutcomeDTO();
        outcomeRef.setId(1L);
        OpportunityDTO body = new OpportunityDTO();
        body.setId(1L);
        body.setTitle("valid title");
        body.setStatus(OpportunityStatus.UNEXPLORED);
        body.setValuerating(3);
        body.setPriority(50);
        body.setSortOrder(0);
        body.setCreatedDate(Instant.now());
        body.setOutcome(outcomeRef);
        assertAdminOnlyOnEveryVerb("/api/opportunities", body);
    }

    @Test
    @Transactional
    void solutionEndpointsDenyNonAdminOnEveryVerb() throws Exception {
        OpportunityDTO opportunityRef = new OpportunityDTO();
        opportunityRef.setId(1L);
        SolutionDTO body = new SolutionDTO();
        body.setId(1L);
        body.setTitle("valid title");
        body.setStatus(SolutionStatus.CANDIDATE);
        body.setSortOrder(0);
        body.setCreatedDate(Instant.now());
        body.setOpportunity(opportunityRef);
        assertAdminOnlyOnEveryVerb("/api/solutions", body);
    }

    @Test
    @Transactional
    void assumptionEndpointsDenyNonAdminOnEveryVerb() throws Exception {
        SolutionDTO solutionRef = new SolutionDTO();
        solutionRef.setId(1L);
        AssumptionDTO body = new AssumptionDTO();
        body.setId(1L);
        body.setStatement("valid statement");
        body.setStatus(AssumptionStatus.UNTESTED);
        body.setConfidence(40);
        body.setSortOrder(0);
        body.setCreatedDate(Instant.now());
        body.setSolution(solutionRef);
        assertAdminOnlyOnEveryVerb("/api/assumptions", body);
    }

    @Test
    @Transactional
    void commentEndpointsDenyNonAdminOnEveryVerb() throws Exception {
        UserDTO authorRef = new UserDTO();
        authorRef.setId("some-user");
        authorRef.setLogin("some-user");
        CommentDTO body = new CommentDTO();
        body.setId(1L);
        body.setBody("Hello");
        body.setCreatedDate(Instant.now());
        body.setAuthor(authorRef);
        assertAdminOnlyOnEveryVerb("/api/comments", body);
    }

    // InterviewResource carries the same class-level ROLE_ADMIN as the resources above but was not
    // covered here, so a dropped annotation would have gone unnoticed.
    @Test
    @Transactional
    void interviewEndpointsDenyNonAdminOnEveryVerb() throws Exception {
        ProductDTO productRef = new ProductDTO();
        productRef.setId(1L);
        InterviewDTO body = new InterviewDTO();
        body.setId(1L);
        body.setTitle("Kickoff interview");
        body.setParticipant("Ada");
        body.setInterviewDate(LocalDate.of(2026, 1, 15));
        body.setCreatedDate(Instant.now());
        body.setProduct(productRef);
        assertAdminOnlyOnEveryVerb("/api/interviews", body);
    }

    // TagResource likewise carries class-level ROLE_ADMIN and was not covered here.
    @Test
    @Transactional
    void tagEndpointsDenyNonAdminOnEveryVerb() throws Exception {
        TeamDTO teamRef = new TeamDTO();
        teamRef.setId(1L);
        TagDTO body = new TagDTO();
        body.setId(1L);
        body.setName("discovery");
        body.setColour("#112233");
        body.setTeam(teamRef);
        assertAdminOnlyOnEveryVerb("/api/tags", body);
    }

    /**
     * Authorisation must be decided before the request body is validated. Method security
     * ({@code @PreAuthorize}) only runs once Spring MVC has deserialised and validated the
     * arguments, so an admin-only resource used to answer an incomplete body from a plain user
     * with a 400 naming the DTO's required fields — telling an unauthorised caller the controller's
     * signature. The generated admin-only paths are matched in the security filter chain instead
     * (see {@code SecurityConfiguration#GENERATED_ADMIN_ONLY_API_PATHS}), so the answer is 403
     * whatever the body is.
     */
    @Test
    @Transactional
    void incompleteBodyOnAdminOnlyResource_isRefusedBeforeValidation_andLeaksNoFieldNames() throws Exception {
        for (String baseUrl : List.of(
            "/api/outcomes",
            "/api/opportunities",
            "/api/solutions",
            "/api/assumptions",
            "/api/evidences",
            "/api/node-links",
            "/api/open-questions",
            "/api/node-histories",
            "/api/comments",
            "/api/interviews",
            "/api/tags",
            "/api/team-members",
            // /api/teams is the generated, admin-only TeamResource. It was left out of the
            // filter-chain list because the prefix is shared with the member-facing
            // /api/teams/{teamId}/tree and /api/teams/{teamId}/products; the two shapes
            // TeamResource actually owns (/api/teams and /api/teams/{id}) are matched instead.
            "/api/teams",
            // AuthorityResource is admin-only per method, not per class, so it had the same hole.
            "/api/authorities"
        )) {
            MvcResult result = mvc
                .perform(post(baseUrl).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden())
                .andReturn();
            String responseBody = result.getResponse().getContentAsString();
            assertThat(responseBody)
                .as("the 403 for POST %s must not describe the controller's payload", baseUrl)
                .doesNotContain("fieldErrors")
                .doesNotContain("must not be null")
                .doesNotContain("com.opportunity.tree");
        }
        // PUT /api/teams/{id} takes the same @Valid body, so it leaked the same way.
        MvcResult put = mvc
            .perform(
                put("/api/teams/{id}", otherTeam.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"id\":" + otherTeam.getId() + ",\"name\":\"pwned\"}")
            )
            .andExpect(status().isForbidden())
            .andReturn();
        assertThat(put.getResponse().getContentAsString())
            .as("the 403 for PUT /api/teams/{id} must not describe the controller's payload")
            .doesNotContain("fieldErrors")
            .doesNotContain("com.opportunity.tree");
    }

    /**
     * The member-facing sub-resources under {@code /api/teams/{teamId}} must keep authorising by
     * membership, not by role: matching the generated resource with {@code /api/teams/*} (one
     * segment) rather than {@code /api/teams/**} is what keeps them reachable. A non-member still
     * gets 403 — but from {@code TeamAccessService}, with the app's {@code Access denied} detail,
     * not the filter chain's bare {@code Access Denied}.
     */
    @Test
    @Transactional
    void teamScopedSubResourcesAreStillReachedByNonAdmins() throws Exception {
        for (String path : List.of("/api/teams/{teamId}/tree", "/api/teams/{teamId}/products")) {
            MvcResult result = mvc.perform(get(path, otherTeam.getId())).andExpect(status().isForbidden()).andReturn();
            assertThat(result.getResponse().getContentAsString())
                .as("%s must still be handled by the application, not refused by the filter chain", path)
                .contains("Access denied");
        }
    }

    /** Authority is a platform catalogue, but a plain user may not read or write it on any verb. */
    @Test
    @Transactional
    void authorityEndpointsDenyNonAdminOnEveryVerb() throws Exception {
        mvc.perform(get("/api/authorities")).andExpect(status().isForbidden());
        mvc.perform(get("/api/authorities/{id}", "ROLE_USER")).andExpect(status().isForbidden());
        mvc
            .perform(post("/api/authorities").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"ROLE_INJECTED\"}"))
            .andExpect(status().isForbidden());
        mvc.perform(delete("/api/authorities/{id}", "ROLE_USER").with(csrf())).andExpect(status().isForbidden());
    }

    /**
     * Every {@code @RestController} in the application, named here on purpose.
     *
     * <p>The checks in this class are a hand-written list, so regenerating the entities — or adding
     * one resource by hand — silently produces an endpoint nothing in the suite looks at. This test
     * closes that: it reads the live application context and fails as soon as a controller appears
     * that is not accounted for below, naming it. Adding an entry is a deliberate act that says
     * which of the two protections applies.
     *
     * <p>{@code ADMIN_ONLY} means the resource is refused to a non-admin by the filter chain before
     * its body is read (and carries {@code @PreAuthorize} as well); every one of them is exercised
     * verb by verb above. {@code SCOPED} means it is open to authenticated users and authorises per
     * team membership (or is platform plumbing with no team-owned data); those are covered by
     * {@code TeamScopedProductAccessIT}, {@code TeamTreeResourceIT}, the {@code Tree*ResourceIT}
     * classes and {@code AccountResourceIT}.
     */
    private static final Map<String, Protection> EXPECTED_CONTROLLERS = Map.ofEntries(
        Map.entry("TeamResource", Protection.ADMIN_ONLY),
        Map.entry("TeamMemberResource", Protection.ADMIN_ONLY),
        Map.entry("OutcomeResource", Protection.ADMIN_ONLY),
        Map.entry("OpportunityResource", Protection.ADMIN_ONLY),
        Map.entry("SolutionResource", Protection.ADMIN_ONLY),
        Map.entry("AssumptionResource", Protection.ADMIN_ONLY),
        Map.entry("EvidenceResource", Protection.ADMIN_ONLY),
        Map.entry("NodeLinkResource", Protection.ADMIN_ONLY),
        Map.entry("OpenQuestionResource", Protection.ADMIN_ONLY),
        Map.entry("NodeHistoryResource", Protection.ADMIN_ONLY),
        Map.entry("CommentResource", Protection.ADMIN_ONLY),
        Map.entry("InterviewResource", Protection.ADMIN_ONLY),
        Map.entry("TagResource", Protection.ADMIN_ONLY),
        Map.entry("AuthorityResource", Protection.ADMIN_ONLY),
        Map.entry("AdminTeamResource", Protection.ADMIN_ONLY),
        Map.entry("BackupResource", Protection.ADMIN_ONLY),
        Map.entry("ProductResource", Protection.SCOPED),
        Map.entry("TeamTreeResource", Protection.SCOPED),
        Map.entry("TeamProductResource", Protection.SCOPED),
        Map.entry("TeamManagementResource", Protection.SCOPED),
        Map.entry("TreeNodeResource", Protection.SCOPED),
        Map.entry("TreeNodeMoveResource", Protection.SCOPED),
        Map.entry("TreeNodeLinkResource", Protection.SCOPED),
        Map.entry("TreeOpenQuestionResource", Protection.SCOPED),
        Map.entry("TreeCommentResource", Protection.SCOPED),
        Map.entry("TreeNodeHistoryResource", Protection.SCOPED),
        Map.entry("AccountResource", Protection.SCOPED),
        Map.entry("AuthInfoResource", Protection.SCOPED),
        Map.entry("PublicUserResource", Protection.SCOPED),
        Map.entry("LogoutResource", Protection.SCOPED)
    );

    private enum Protection {
        ADMIN_ONLY,
        SCOPED,
    }

    @Test
    @Transactional
    void everyRestControllerIsAccountedForAndAdminOnlyOnesRefuseAPlainUser() throws Exception {
        Set<String> live = applicationContext
            .getBeansWithAnnotation(RestController.class)
            .values()
            .stream()
            .map(bean -> ClassUtils.getUserClass(bean))
            // Exactly this package: sub-packages hold test-only controllers (web.rest.errors).
            .filter(type -> type.getPackageName().equals("com.opportunity.tree.web.rest"))
            .map(Class::getSimpleName)
            .collect(Collectors.toSet());

        assertThat(live)
            .as(
                "a @RestController exists that this security test does not know about — add it to " +
                    "EXPECTED_CONTROLLERS and cover its verbs, or it ships unchecked"
            )
            .containsExactlyInAnyOrderElementsOf(EXPECTED_CONTROLLERS.keySet());

        // And the admin-only ones must actually be refused before their body is looked at.
        for (String name : List.of("TeamResource", "AuthorityResource", "TeamMemberResource", "TagResource")) {
            assertThat(EXPECTED_CONTROLLERS.get(name)).isEqualTo(Protection.ADMIN_ONLY);
        }
    }

    /** Every verb on a generated CRUD resource must 403 for a plain ROLE_USER. The body is valid so validation cannot mask it. */
    private void assertAdminOnlyOnEveryVerb(String baseUrl, Object body) throws Exception {
        byte[] json = om.writeValueAsBytes(body);
        mvc.perform(get(baseUrl)).andExpect(status().isForbidden());
        mvc.perform(get(baseUrl + "/{id}", 1L)).andExpect(status().isForbidden());
        mvc.perform(post(baseUrl).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json)).andExpect(status().isForbidden());
        mvc
            .perform(put(baseUrl + "/{id}", 1L).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isForbidden());
        mvc
            .perform(patch(baseUrl + "/{id}", 1L).with(csrf()).contentType("application/merge-patch+json").content(json))
            .andExpect(status().isForbidden());
        mvc.perform(delete(baseUrl + "/{id}", 1L).with(csrf())).andExpect(status().isForbidden());
    }
}
