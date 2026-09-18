package com.opportunity.tree.web.rest;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opportunity.tree.IntegrationTest;
import com.opportunity.tree.domain.Opportunity;
import com.opportunity.tree.domain.Outcome;
import com.opportunity.tree.domain.Product;
import com.opportunity.tree.domain.Solution;
import com.opportunity.tree.domain.Team;
import com.opportunity.tree.domain.TeamMember;
import com.opportunity.tree.domain.User;
import com.opportunity.tree.domain.enumeration.OpportunityStatus;
import com.opportunity.tree.domain.enumeration.OutcomeStatus;
import com.opportunity.tree.domain.enumeration.SolutionStatus;
import com.opportunity.tree.domain.enumeration.TeamRole;
import com.opportunity.tree.repository.UserRepository;
import com.opportunity.tree.service.dto.OpportunityDTO;
import com.opportunity.tree.service.dto.OutcomeDTO;
import com.opportunity.tree.service.dto.SolutionDTO;
import com.opportunity.tree.service.mapper.OpportunityMapper;
import com.opportunity.tree.service.mapper.OutcomeMapper;
import com.opportunity.tree.service.mapper.SolutionMapper;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/**
 * Role matrix for tree node writes (TREE-002): creating and updating an Outcome,
 * Opportunity or Solution succeeds for team owners and editors and returns 403
 * for viewers and non-members. None of the users has ROLE_ADMIN, so this also
 * verifies the write endpoints are reachable by ordinary team members.
 */
@IntegrationTest
@AutoConfigureMockMvc
class TreeNodeWriteAccessIT {

    private static final String OWNER_LOGIN = "node-owner";
    private static final String EDITOR_LOGIN = "node-editor";
    private static final String VIEWER_LOGIN = "node-viewer";
    private static final String NON_MEMBER_LOGIN = "node-outsider";

    private static final List<String> DENIED = List.of(VIEWER_LOGIN, NON_MEMBER_LOGIN);
    private static final List<String> ALLOWED = List.of(OWNER_LOGIN, EDITOR_LOGIN);

    @Autowired
    private ObjectMapper om;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OutcomeMapper outcomeMapper;

    @Autowired
    private OpportunityMapper opportunityMapper;

    @Autowired
    private SolutionMapper solutionMapper;

    private Outcome outcome;
    private Opportunity opportunity;
    private Solution solution;

    @BeforeEach
    void seed() {
        Team team = new Team().name("node-team").description("d").createdDate(Instant.now());
        em.persist(team);

        persistMembership(team, persistUser(OWNER_LOGIN), TeamRole.OWNER);
        persistMembership(team, persistUser(EDITOR_LOGIN), TeamRole.EDITOR);
        persistMembership(team, persistUser(VIEWER_LOGIN), TeamRole.VIEWER);
        persistUser(NON_MEMBER_LOGIN);

        Instant now = Instant.now();
        Product product = new Product().name("Discovery").description("desc").archived(Boolean.FALSE).createdDate(now).team(team);
        em.persist(product);

        outcome = new Outcome().title("Outcome").status(OutcomeStatus.DRAFT).sortOrder(0).createdDate(now).lastModifiedDate(now);
        outcome.setProduct(product);
        em.persist(outcome);

        opportunity = new Opportunity()
            .title("Opportunity")
            .status(OpportunityStatus.IDENTIFIED)
            .valuerating(3)
            .complexity(3)
            .sortOrder(0)
            .createdDate(now)
            .lastModifiedDate(now);
        opportunity.setOutcome(outcome);
        em.persist(opportunity);

        solution = new Solution().title("Solution").status(SolutionStatus.IDEA).sortOrder(0).createdDate(now).lastModifiedDate(now);
        solution.setOpportunity(opportunity);
        em.persist(solution);
        em.flush();
    }

    // ---------------------------------------------------------------
    // Create
    // ---------------------------------------------------------------

    @Test
    @Transactional
    void createOutcomeRoleMatrix() throws Exception {
        OutcomeDTO dto = outcomeMapper.toDto(outcome);
        dto.setId(null);
        assertWriteMatrix(() -> post("/api/outcomes").contentType(MediaType.APPLICATION_JSON).content(json(dto)), 201);
    }

    @Test
    @Transactional
    void createOpportunityRoleMatrix() throws Exception {
        OpportunityDTO dto = opportunityMapper.toDto(opportunity);
        dto.setId(null);
        assertWriteMatrix(() -> post("/api/opportunities").contentType(MediaType.APPLICATION_JSON).content(json(dto)), 201);
    }

    @Test
    @Transactional
    void createNestedOpportunityRoleMatrix() throws Exception {
        OpportunityDTO dto = opportunityMapper.toDto(opportunity);
        dto.setId(null);
        dto.setParent(opportunityMapper.toDto(opportunity));
        assertWriteMatrix(() -> post("/api/opportunities").contentType(MediaType.APPLICATION_JSON).content(json(dto)), 201);
    }

    @Test
    @Transactional
    void createSolutionRoleMatrix() throws Exception {
        SolutionDTO dto = solutionMapper.toDto(solution);
        dto.setId(null);
        assertWriteMatrix(() -> post("/api/solutions").contentType(MediaType.APPLICATION_JSON).content(json(dto)), 201);
    }

    // ---------------------------------------------------------------
    // Update (PUT) and partial update (PATCH)
    // ---------------------------------------------------------------

    @Test
    @Transactional
    void updateOutcomeRoleMatrix() throws Exception {
        OutcomeDTO dto = outcomeMapper.toDto(outcome);
        dto.setStatus(OutcomeStatus.ACTIVE);
        assertWriteMatrix(() -> put("/api/outcomes/{id}", dto.getId()).contentType(MediaType.APPLICATION_JSON).content(json(dto)), 200);

        OutcomeDTO patch = new OutcomeDTO();
        patch.setId(outcome.getId());
        patch.setStatus(OutcomeStatus.ACHIEVED);
        assertWriteMatrix(
            () -> patch("/api/outcomes/{id}", patch.getId()).contentType("application/merge-patch+json").content(json(patch)),
            200
        );
    }

    @Test
    @Transactional
    void updateOpportunityRoleMatrix() throws Exception {
        OpportunityDTO dto = opportunityMapper.toDto(opportunity);
        dto.setStatus(OpportunityStatus.EXPLORING);
        assertWriteMatrix(
            () -> put("/api/opportunities/{id}", dto.getId()).contentType(MediaType.APPLICATION_JSON).content(json(dto)),
            200
        );

        OpportunityDTO patch = new OpportunityDTO();
        patch.setId(opportunity.getId());
        patch.setStatus(OpportunityStatus.PARKED);
        assertWriteMatrix(
            () -> patch("/api/opportunities/{id}", patch.getId()).contentType("application/merge-patch+json").content(json(patch)),
            200
        );
    }

    @Test
    @Transactional
    void updateSolutionRoleMatrix() throws Exception {
        SolutionDTO dto = solutionMapper.toDto(solution);
        dto.setStatus(SolutionStatus.TESTING);
        assertWriteMatrix(() -> put("/api/solutions/{id}", dto.getId()).contentType(MediaType.APPLICATION_JSON).content(json(dto)), 200);

        SolutionDTO patch = new SolutionDTO();
        patch.setId(solution.getId());
        patch.setStatus(SolutionStatus.VALIDATED);
        assertWriteMatrix(
            () -> patch("/api/solutions/{id}", patch.getId()).contentType("application/merge-patch+json").content(json(patch)),
            200
        );
    }

    // ---------------------------------------------------------------
    // Write-rule violations surface as 400, not 500
    // ---------------------------------------------------------------

    @Test
    @Transactional
    void invalidParentReturnsBadRequest() throws Exception {
        SolutionDTO dto = solutionMapper.toDto(solution);
        dto.setId(null);
        dto.getOpportunity().setId(Long.MAX_VALUE);
        mvc
            .perform(post("/api/solutions").with(user(OWNER_LOGIN)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json(dto)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    void opportunityCannotBecomeItsOwnParent() throws Exception {
        OpportunityDTO dto = opportunityMapper.toDto(opportunity);
        dto.setParent(opportunityMapper.toDto(opportunity));
        mvc
            .perform(
                put("/api/opportunities/{id}", dto.getId())
                    .with(user(OWNER_LOGIN))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json(dto))
            )
            .andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    /** Viewers and non-members get 403; owners and editors get the success status. */
    private void assertWriteMatrix(Supplier<MockHttpServletRequestBuilder> request, int successStatus) throws Exception {
        for (String login : DENIED) {
            mvc.perform(request.get().with(user(login)).with(csrf())).andExpect(status().isForbidden());
        }
        for (String login : ALLOWED) {
            mvc.perform(request.get().with(user(login)).with(csrf())).andExpect(status().is(successStatus));
        }
    }

    private byte[] json(Object dto) {
        try {
            return om.writeValueAsBytes(dto);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
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
}
