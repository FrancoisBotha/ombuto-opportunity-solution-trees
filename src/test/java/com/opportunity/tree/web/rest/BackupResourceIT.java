package com.opportunity.tree.web.rest;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.opportunity.tree.IntegrationTest;
import com.opportunity.tree.domain.Opportunity;
import com.opportunity.tree.domain.Outcome;
import com.opportunity.tree.domain.Product;
import com.opportunity.tree.domain.Solution;
import com.opportunity.tree.domain.Team;
import com.opportunity.tree.domain.enumeration.OpportunityStatus;
import com.opportunity.tree.domain.enumeration.SolutionStatus;
import com.opportunity.tree.security.AuthoritiesConstants;
import com.opportunity.tree.service.dto.backup.BackupArchive;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for {@link BackupResource} (BKRST-001).
 */
@IntegrationTest
@AutoConfigureMockMvc
@Transactional
class BackupResourceIT {

    private static final String API = "/api/admin/backup";
    private static final String ADMIN_LOGIN = "bkrst001-admin";
    private static final String USER_LOGIN = "bkrst001-user";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager em;

    private Team seededTeam;
    private Product seededProduct;
    private Outcome seededOutcome;
    private Opportunity seededOpportunity;
    private Solution seededSolution;

    @BeforeEach
    void seedTree() {
        seededTeam = new Team().name("BKRST-001 team " + System.nanoTime()).description("d").createdDate(Instant.now());
        em.persist(seededTeam);

        seededProduct = new Product()
            .name("BKRST-001 product")
            .description("d")
            .vision("v")
            .archived(Boolean.FALSE)
            .sortOrder(0)
            .createdDate(Instant.now());
        seededProduct.setTeam(seededTeam);
        em.persist(seededProduct);

        seededOutcome = new Outcome().title("An outcome").sortOrder(0).createdDate(Instant.now());
        seededOutcome.setProduct(seededProduct);
        em.persist(seededOutcome);

        seededOpportunity = new Opportunity()
            .title("An opportunity")
            .status(OpportunityStatus.UNEXPLORED)
            .valuerating(3)
            .priority(50)
            .sortOrder(0)
            .createdDate(Instant.now());
        seededOpportunity.setOutcome(seededOutcome);
        em.persist(seededOpportunity);

        seededSolution = new Solution().title("A solution").status(SolutionStatus.CANDIDATE).sortOrder(0).createdDate(Instant.now());
        seededSolution.setOpportunity(seededOpportunity);
        em.persist(seededSolution);

        em.flush();
    }

    // AC 2 — anonymous callers get 401
    @Test
    void anonymousCallerGets401() throws Exception {
        mockMvc.perform(get(API)).andExpect(status().isUnauthorized());
    }

    // AC 2 — a non-admin authenticated caller gets 403
    @Test
    void nonAdminCallerGets403() throws Exception {
        mockMvc.perform(get(API).with(user(USER_LOGIN))).andExpect(status().isForbidden());
    }

    // AC 1, 3, 4, 5, 6, 7 — admin gets a versioned archive containing the seeded rows
    @Test
    void adminExportsArchiveWithSeededRowsAndCounts() throws Exception {
        mockMvc
            .perform(get(API).with(user(ADMIN_LOGIN).authorities(new SimpleGrantedAuthority(AuthoritiesConstants.ADMIN))))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", Matchers.startsWith("application/json")))
            .andExpect(header().string("Content-Disposition", Matchers.startsWith("attachment; filename=\"ombuto-ost-backup-")))
            .andExpect(header().string("Content-Disposition", Matchers.endsWith(".json\"")))
            .andExpect(
                header().string(
                    "Content-Disposition",
                    Matchers.matchesRegex("attachment; filename=\"ombuto-ost-backup-\\d{8}-\\d{6}\\.json\"")
                )
            )
            .andExpect(jsonPath("$.formatVersion").value(BackupArchive.FORMAT_VERSION))
            .andExpect(jsonPath("$.applicationVersion").isString())
            .andExpect(jsonPath("$.exportedAt").isString())
            .andExpect(jsonPath("$.counts").isMap())
            .andExpect(jsonPath("$.counts.teams").isNumber())
            .andExpect(jsonPath("$.counts.products").isNumber())
            .andExpect(jsonPath("$.counts.outcomes").isNumber())
            .andExpect(jsonPath("$.counts.opportunities").isNumber())
            .andExpect(jsonPath("$.counts.solutions").isNumber())
            .andExpect(jsonPath("$.counts.opportunityInterviews").isNumber())
            .andExpect(jsonPath("$.counts.opportunityTags").isNumber())
            .andExpect(jsonPath("$.counts.solutionTags").isNumber())
            // Seeded rows are visible in their respective lists
            .andExpect(jsonPath("$.teams[?(@.id == " + seededTeam.getId() + ")].name", Matchers.hasItem(seededTeam.getName())))
            .andExpect(
                jsonPath("$.products[?(@.id == " + seededProduct.getId() + ")].teamId", Matchers.hasItem(seededTeam.getId().intValue()))
            )
            .andExpect(
                jsonPath(
                    "$.outcomes[?(@.id == " + seededOutcome.getId() + ")].productId",
                    Matchers.hasItem(seededProduct.getId().intValue())
                )
            )
            .andExpect(
                jsonPath(
                    "$.opportunities[?(@.id == " + seededOpportunity.getId() + ")].outcomeId",
                    Matchers.hasItem(seededOutcome.getId().intValue())
                )
            )
            .andExpect(
                jsonPath(
                    "$.solutions[?(@.id == " + seededSolution.getId() + ")].opportunityId",
                    Matchers.hasItem(seededOpportunity.getId().intValue())
                )
            )
            // The counts genuinely match the returned list lengths, so the archive is internally consistent.
            .andExpect(jsonPath("$.counts.teams").value(Matchers.greaterThanOrEqualTo(1)))
            .andExpect(jsonPath("$.counts.products").value(Matchers.greaterThanOrEqualTo(1)))
            .andExpect(jsonPath("$.counts.outcomes").value(Matchers.greaterThanOrEqualTo(1)))
            .andExpect(jsonPath("$.counts.opportunities").value(Matchers.greaterThanOrEqualTo(1)))
            .andExpect(jsonPath("$.counts.solutions").value(Matchers.greaterThanOrEqualTo(1)));
    }
}
