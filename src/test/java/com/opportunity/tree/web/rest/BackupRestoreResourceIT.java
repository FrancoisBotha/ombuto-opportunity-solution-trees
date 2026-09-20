package com.opportunity.tree.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.opportunity.tree.IntegrationTest;
import com.opportunity.tree.domain.Opportunity;
import com.opportunity.tree.domain.Outcome;
import com.opportunity.tree.domain.Product;
import com.opportunity.tree.domain.Solution;
import com.opportunity.tree.domain.Team;
import com.opportunity.tree.domain.enumeration.OpportunityStatus;
import com.opportunity.tree.domain.enumeration.SolutionStatus;
import com.opportunity.tree.security.AuthoritiesConstants;
import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

/** Integration tests for the admin backup restore contract (BKRST-002). */
@IntegrationTest
@AutoConfigureMockMvc
@Transactional
class BackupRestoreResourceIT {

    private static final String API = "/api/admin/backup";
    private static final String RESTORE_API = API + "/restore";
    private static final String ADMIN_LOGIN = "bkrst002-admin";
    private static final String USER_LOGIN = "bkrst002-user";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager em;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private com.opportunity.tree.service.RestoreMutex restoreMutex;

    private Team seededTeam;
    private Product seededProduct;
    private Outcome seededOutcome;
    private Opportunity seededOpportunity;
    private Solution seededSolution;

    @BeforeEach
    void seedTree() {
        seededTeam = new Team().name("BKRST-002 exported team").description("team description").createdDate(Instant.now());
        em.persist(seededTeam);

        seededProduct = new Product()
            .name("BKRST-002 exported product")
            .description("product description")
            .vision("vision")
            .archived(Boolean.FALSE)
            .sortOrder(0)
            .createdDate(Instant.now())
            .team(seededTeam);
        em.persist(seededProduct);

        seededOutcome = new Outcome().title("BKRST-002 exported outcome").sortOrder(0).createdDate(Instant.now()).product(seededProduct);
        em.persist(seededOutcome);

        seededOpportunity = new Opportunity()
            .title("BKRST-002 exported opportunity")
            .status(OpportunityStatus.UNEXPLORED)
            .valuerating(3)
            .priority(50)
            .sortOrder(0)
            .createdDate(Instant.now())
            .outcome(seededOutcome);
        em.persist(seededOpportunity);

        seededSolution = new Solution()
            .title("BKRST-002 exported solution")
            .status(SolutionStatus.CANDIDATE)
            .sortOrder(0)
            .createdDate(Instant.now())
            .opportunity(seededOpportunity);
        em.persist(seededSolution);
        em.flush();
    }

    @Test
    void restoreRequiresAuthentication() throws Exception {
        mockMvc
            .perform(multipart(RESTORE_API).file(jsonFile("{}".getBytes(StandardCharsets.UTF_8))).with(csrf()))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void restoreRequiresAdminAuthority() throws Exception {
        mockMvc
            .perform(multipart(RESTORE_API).file(jsonFile("{}".getBytes(StandardCharsets.UTF_8))).with(csrf()).with(user(USER_LOGIN)))
            .andExpect(status().isForbidden());
    }

    @Test
    void invalidJsonIsRejectedBeforeExistingDataChanges() throws Exception {
        long teamId = seededTeam.getId();

        mockMvc
            .perform(adminRestore("not JSON".getBytes(StandardCharsets.UTF_8)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.backup.invalid"))
            .andExpect(jsonPath("$.detail").value("The uploaded file is not a valid backup archive."));

        em.clear();
        assertThat(em.find(Team.class, teamId)).isNotNull();
    }

    @Test
    void jsonWithoutExpectedEnvelopeIsRejectedBeforeExistingDataChanges() throws Exception {
        long productId = seededProduct.getId();

        mockMvc
            .perform(adminRestore("{}".getBytes(StandardCharsets.UTF_8)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.backup.invalid"))
            .andExpect(jsonPath("$.detail").value("The uploaded file is not a valid backup archive."));

        em.clear();
        assertThat(em.find(Product.class, productId)).isNotNull();
    }

    @Test
    void incompatibleVersionIsRejectedBeforeExistingDataChanges() throws Exception {
        long opportunityId = seededOpportunity.getId();
        ObjectNode archive = (ObjectNode) objectMapper.readTree(exportArchive());
        archive.put("formatVersion", "unsupported-version");

        mockMvc
            .perform(adminRestore(objectMapper.writeValueAsBytes(archive)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.backup.incompatibleVersion"))
            .andExpect(jsonPath("$.detail").value("The backup archive format version is not supported."));

        em.clear();
        assertThat(em.find(Opportunity.class, opportunityId)).isNotNull();
    }

    @Test
    void exportedArchiveRoundTripsAndReturnsRestoreSummary() throws Exception {
        byte[] archiveBytes = exportArchive();
        JsonNode archive = objectMapper.readTree(archiveBytes);
        String exportedAt = archive.path("exportedAt").asText();
        int teamCount = archive.path("teams").size();
        int productCount = archive.path("products").size();
        int treeNodeCount =
            archive.path("outcomes").size() +
            archive.path("opportunities").size() +
            archive.path("solutions").size() +
            archive.path("assumptions").size() +
            archive.path("evidences").size();

        seededTeam.setName("mutated team");
        seededProduct.setName("mutated product");
        seededOutcome.setTitle("mutated outcome");
        seededOpportunity.setTitle("mutated opportunity");
        seededSolution.setTitle("mutated solution");
        em.persist(new Team().name("not in backup").createdDate(Instant.now()));
        em.flush();

        mockMvc
            .perform(adminRestore(archiveBytes))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.exportedAt").value(exportedAt))
            .andExpect(jsonPath("$.counts.teams").value(teamCount))
            .andExpect(jsonPath("$.counts.products").value(productCount))
            .andExpect(jsonPath("$.counts.treeNodes").value(treeNodeCount));

        em.clear();
        assertThat(em.find(Team.class, seededTeam.getId()).getName()).isEqualTo("BKRST-002 exported team");
        assertThat(em.find(Product.class, seededProduct.getId()).getName()).isEqualTo("BKRST-002 exported product");
        assertThat(em.find(Outcome.class, seededOutcome.getId()).getTitle()).isEqualTo("BKRST-002 exported outcome");
        assertThat(em.find(Opportunity.class, seededOpportunity.getId()).getTitle()).isEqualTo("BKRST-002 exported opportunity");
        assertThat(em.find(Solution.class, seededSolution.getId()).getTitle()).isEqualTo("BKRST-002 exported solution");
        assertThat(
            em
                .createQuery("select count(t) from Team t where t.name = :name", Long.class)
                .setParameter("name", "not in backup")
                .getSingleResult()
        ).isZero();
    }

    /**
     * BKRST fix C2. Restored rows keep their own ids (they are written with {@code replicate}), so
     * without a restart the generator is still sitting wherever it was and the first create after a
     * restore is handed an id the archive already used. The probe team carries an id far above
     * anything this database has issued, which is exactly the shape of restoring a production
     * archive onto a fresh install.
     */
    @Test
    void restoreRestartsTheIdSequenceAboveTheHighestRestoredId() throws Exception {
        long probeId = 9_000_000L;
        ObjectNode archive = (ObjectNode) objectMapper.readTree(exportArchive());
        ObjectNode probe = ((ArrayNode) archive.get("teams")).addObject();
        probe.put("id", probeId);
        probe.put("name", "BKRST sequence probe");
        probe.put("createdDate", Instant.now().toString());

        mockMvc.perform(adminRestore(objectMapper.writeValueAsBytes(archive))).andExpect(status().isOk());

        assertThat(nextSequenceValue())
            .as("sequence_generator must be past the highest restored id, or the next create collides")
            .isGreaterThan(probeId);

        // And a create after the restore really does succeed.
        em.clear();
        Team created = new Team().name("BKRST post-restore team").createdDate(Instant.now());
        em.persist(created);
        em.flush();
        assertThat(created.getId()).isNotNull();
    }

    /**
     * BKRST fix C5. {@code jhi_user} is not part of a backup, so rows are re-attached to users by
     * reference: a login the archive names but this installation does not have used to blow up deep
     * inside the insert phase, with everything already deleted. It is now a clean 400 and nothing
     * has been touched.
     */
    @Test
    void archiveReferringToAnUnknownUserIsRejectedBeforeDeleting() throws Exception {
        long teamId = seededTeam.getId();
        ObjectNode archive = (ObjectNode) objectMapper.readTree(exportArchive());
        ObjectNode member = ((ArrayNode) archive.get("teamMembers")).addObject();
        member.put("id", 9_000_001L);
        member.put("role", "OWNER");
        member.put("joinedDate", Instant.now().toString());
        member.put("teamId", teamId);
        member.put("userId", "bkrst-no-such-user");

        mockMvc
            .perform(adminRestore(objectMapper.writeValueAsBytes(archive)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.backup.unresolvedReferences"))
            .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("user bkrst-no-such-user")));

        em.clear();
        assertThat(em.find(Team.class, teamId)).isNotNull();
    }

    /**
     * BKRST fix C5, the other half: a reference the archive makes to a row it does not itself carry.
     */
    @Test
    void archiveWithADanglingInternalReferenceIsRejectedBeforeDeleting() throws Exception {
        long teamId = seededTeam.getId();
        ObjectNode archive = (ObjectNode) objectMapper.readTree(exportArchive());
        ObjectNode product = ((ArrayNode) archive.get("products")).addObject();
        product.put("id", 9_000_002L);
        product.put("name", "BKRST orphan product");
        product.put("archived", false);
        product.put("sortOrder", 0);
        product.put("createdDate", Instant.now().toString());
        product.put("teamId", 8_000_000L);

        mockMvc
            .perform(adminRestore(objectMapper.writeValueAsBytes(archive)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.backup.unresolvedReferences"))
            .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("missing team 8000000")));

        em.clear();
        assertThat(em.find(Team.class, teamId)).isNotNull();
    }

    /**
     * BKRST fix C4. A row the archive carries that this build's constraints reject used to come
     * back as a 500 quoting {@code ConstraintViolationImpl{... rootBeanClass=class
     * com.opportunity.tree.domain.Team ...}}. Nothing is asserted about the data afterwards: the
     * failure happens inside the transaction this test shares with the service, so the rollback
     * that protects the data in production only happens when the test itself ends.
     */
    @Test
    void aRowThePersistLayerRejectsIsReportedWithoutJavaInternals() throws Exception {
        ObjectNode archive = (ObjectNode) objectMapper.readTree(exportArchive());
        ObjectNode invalid = ((ArrayNode) archive.get("teams")).addObject();
        invalid.put("id", 9_000_003L);
        invalid.putNull("name");
        invalid.put("createdDate", Instant.now().toString());

        mockMvc
            .perform(adminRestore(objectMapper.writeValueAsBytes(archive)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.backup.restoreFailed"))
            .andExpect(jsonPath("$.detail").value("The backup could not be restored. No data was changed."));
    }

    /**
     * BKRST fix C6. Two restores at once would interleave one's deletes with the other's inserts.
     * Holding the mutex from the test thread is the same state the loser of that race sees.
     */
    @Test
    void aSecondRestoreIsRefusedWhileOneIsRunning() throws Exception {
        byte[] archiveBytes = exportArchive();
        assertThat(restoreMutex.tryAcquire()).as("no other restore is running in this test").isTrue();
        try {
            mockMvc
                .perform(adminRestore(archiveBytes))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("error.concurrencyFailure"));
        } finally {
            restoreMutex.release();
        }

        em.clear();
        assertThat(em.find(Team.class, seededTeam.getId())).isNotNull();
    }

    /** Reads (and consumes) sequence_generator's next value the same way the service does. */
    private long nextSequenceValue() {
        String sql = em
            .unwrap(org.hibernate.engine.spi.SessionImplementor.class)
            .getFactory()
            .getJdbcServices()
            .getDialect()
            .getSequenceSupport()
            .getSequenceNextValString("sequence_generator");
        if (sql.regionMatches(true, 0, "call ", 0, 5)) {
            sql = "select " + sql.substring(5);
        }
        return ((Number) em.createNativeQuery(sql).getSingleResult()).longValue();
    }

    private byte[] exportArchive() throws Exception {
        MvcResult result = mockMvc
            .perform(get(API).with(user(ADMIN_LOGIN).authorities(new SimpleGrantedAuthority(AuthoritiesConstants.ADMIN))))
            .andExpect(status().isOk())
            .andReturn();
        return result.getResponse().getContentAsByteArray();
    }

    private org.springframework.test.web.servlet.RequestBuilder adminRestore(byte[] content) {
        return multipart(RESTORE_API)
            .file(jsonFile(content))
            .with(csrf())
            .with(user(ADMIN_LOGIN).authorities(new SimpleGrantedAuthority(AuthoritiesConstants.ADMIN)));
    }

    private MockMultipartFile jsonFile(byte[] content) {
        return new MockMultipartFile("file", "backup.json", "application/json", content);
    }
}
