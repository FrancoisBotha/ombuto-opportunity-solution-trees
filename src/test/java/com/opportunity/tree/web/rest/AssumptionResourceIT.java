package com.opportunity.tree.web.rest;

import static com.opportunity.tree.domain.AssumptionAsserts.*;
import static com.opportunity.tree.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opportunity.tree.IntegrationTest;
import com.opportunity.tree.domain.Assumption;
import com.opportunity.tree.domain.Solution;
import com.opportunity.tree.domain.enumeration.AssumptionCategory;
import com.opportunity.tree.repository.AssumptionRepository;
import com.opportunity.tree.repository.EntityManager;
import com.opportunity.tree.service.AssumptionService;
import com.opportunity.tree.service.dto.AssumptionDTO;
import com.opportunity.tree.service.mapper.AssumptionMapper;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

/**
 * Integration tests for the {@link AssumptionResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureWebTestClient(timeout = IntegrationTest.DEFAULT_ENTITY_TIMEOUT)
@WithMockUser
class AssumptionResourceIT {

    private static final String DEFAULT_STATEMENT = "AAAAAAAAAA";
    private static final String UPDATED_STATEMENT = "BBBBBBBBBB";

    private static final AssumptionCategory DEFAULT_CATEGORY = AssumptionCategory.DESIRABILITY;
    private static final AssumptionCategory UPDATED_CATEGORY = AssumptionCategory.VIABILITY;

    private static final Integer DEFAULT_IMPORTANCE = 1;
    private static final Integer UPDATED_IMPORTANCE = 2;

    private static final Integer DEFAULT_EVIDENCE = 1;
    private static final Integer UPDATED_EVIDENCE = 2;

    private static final Boolean DEFAULT_VALIDATED = false;
    private static final Boolean UPDATED_VALIDATED = true;

    private static final Instant DEFAULT_CREATED_DATE = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CREATED_DATE = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/assumptions";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2L * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private AssumptionRepository assumptionRepository;

    @Mock
    private AssumptionRepository assumptionRepositoryMock;

    @Autowired
    private AssumptionMapper assumptionMapper;

    @Mock
    private AssumptionService assumptionServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private WebTestClient webTestClient;

    private Assumption assumption;

    private Assumption insertedAssumption;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Assumption createEntity(EntityManager em) {
        Assumption assumption = new Assumption()
            .statement(DEFAULT_STATEMENT)
            .category(DEFAULT_CATEGORY)
            .importance(DEFAULT_IMPORTANCE)
            .evidence(DEFAULT_EVIDENCE)
            .validated(DEFAULT_VALIDATED)
            .createdDate(DEFAULT_CREATED_DATE);
        // Add required entity
        Solution solution;
        solution = em.insert(SolutionResourceIT.createEntity(em)).block();
        assumption.setSolution(solution);
        return assumption;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Assumption createUpdatedEntity(EntityManager em) {
        Assumption updatedAssumption = new Assumption()
            .statement(UPDATED_STATEMENT)
            .category(UPDATED_CATEGORY)
            .importance(UPDATED_IMPORTANCE)
            .evidence(UPDATED_EVIDENCE)
            .validated(UPDATED_VALIDATED)
            .createdDate(UPDATED_CREATED_DATE);
        // Add required entity
        Solution solution;
        solution = em.insert(SolutionResourceIT.createUpdatedEntity(em)).block();
        updatedAssumption.setSolution(solution);
        return updatedAssumption;
    }

    public static void deleteEntities(EntityManager em) {
        try {
            em.deleteAll(Assumption.class).block();
        } catch (Exception e) {
            // It can fail, if other entities are still referring this - it will be removed later.
        }
        SolutionResourceIT.deleteEntities(em);
    }

    @BeforeEach
    void setupCsrf() {
        webTestClient = webTestClient.mutateWith(csrf());
    }

    @BeforeEach
    void initTest() {
        assumption = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedAssumption != null) {
            assumptionRepository.delete(insertedAssumption).block();
            insertedAssumption = null;
        }
        deleteEntities(em);
    }

    @Test
    void createAssumption() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Assumption
        AssumptionDTO assumptionDTO = assumptionMapper.toDto(assumption);
        var returnedAssumptionDTO = webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(assumptionDTO))
            .exchange()
            .expectStatus()
            .isCreated()
            .expectBody(AssumptionDTO.class)
            .returnResult()
            .getResponseBody();

        // Validate the Assumption in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedAssumption = assumptionMapper.toEntity(returnedAssumptionDTO);
        assertAssumptionUpdatableFieldsEquals(returnedAssumption, getPersistedAssumption(returnedAssumption));

        insertedAssumption = returnedAssumption;
    }

    @Test
    void createAssumptionWithExistingId() throws Exception {
        // Create the Assumption with an existing ID
        assumption.setId(1L);
        AssumptionDTO assumptionDTO = assumptionMapper.toDto(assumption);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(assumptionDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Assumption in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    void checkStatementIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        assumption.setStatement(null);

        // Create the Assumption, which fails.
        AssumptionDTO assumptionDTO = assumptionMapper.toDto(assumption);

        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(assumptionDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    void checkCategoryIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        assumption.setCategory(null);

        // Create the Assumption, which fails.
        AssumptionDTO assumptionDTO = assumptionMapper.toDto(assumption);

        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(assumptionDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    void checkImportanceIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        assumption.setImportance(null);

        // Create the Assumption, which fails.
        AssumptionDTO assumptionDTO = assumptionMapper.toDto(assumption);

        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(assumptionDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    void checkEvidenceIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        assumption.setEvidence(null);

        // Create the Assumption, which fails.
        AssumptionDTO assumptionDTO = assumptionMapper.toDto(assumption);

        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(assumptionDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    void checkCreatedDateIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        assumption.setCreatedDate(null);

        // Create the Assumption, which fails.
        AssumptionDTO assumptionDTO = assumptionMapper.toDto(assumption);

        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(assumptionDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    void getAllAssumptionsAsStream() {
        // Initialize the database
        assumptionRepository.save(assumption).block();

        List<Assumption> assumptionList = webTestClient
            .get()
            .uri(ENTITY_API_URL)
            .accept(MediaType.APPLICATION_NDJSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentTypeCompatibleWith(MediaType.APPLICATION_NDJSON)
            .returnResult(AssumptionDTO.class)
            .getResponseBody()
            .map(assumptionMapper::toEntity)
            .filter(assumption::equals)
            .collectList()
            .block(Duration.ofSeconds(5));

        assertThat(assumptionList).isNotNull();
        assertThat(assumptionList).hasSize(1);
        Assumption testAssumption = assumptionList.get(0);

        // Test fails because reactive api returns an empty object instead of null
        // assertAssumptionAllPropertiesEquals(assumption, testAssumption);
        assertAssumptionUpdatableFieldsEquals(assumption, testAssumption);
    }

    @Test
    void getAllAssumptions() {
        // Initialize the database
        insertedAssumption = assumptionRepository.save(assumption).block();

        // Get all the assumptionList
        webTestClient
            .get()
            .uri(ENTITY_API_URL + "?sort=id,desc")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.[*].id")
            .value(hasItem(assumption.getId().intValue()))
            .jsonPath("$.[*].statement")
            .value(hasItem(DEFAULT_STATEMENT))
            .jsonPath("$.[*].category")
            .value(hasItem(DEFAULT_CATEGORY.toString()))
            .jsonPath("$.[*].importance")
            .value(hasItem(DEFAULT_IMPORTANCE))
            .jsonPath("$.[*].evidence")
            .value(hasItem(DEFAULT_EVIDENCE))
            .jsonPath("$.[*].validated")
            .value(hasItem(DEFAULT_VALIDATED))
            .jsonPath("$.[*].createdDate")
            .value(hasItem(DEFAULT_CREATED_DATE.toString()));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllAssumptionsWithEagerRelationshipsIsEnabled() {
        when(assumptionServiceMock.findAllWithEagerRelationships(any())).thenReturn(Flux.empty());

        webTestClient.get().uri(ENTITY_API_URL + "?eagerload=true").exchange().expectStatus().isOk();

        verify(assumptionServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllAssumptionsWithEagerRelationshipsIsNotEnabled() {
        when(assumptionServiceMock.findAllWithEagerRelationships(any())).thenReturn(Flux.empty());

        webTestClient.get().uri(ENTITY_API_URL + "?eagerload=false").exchange().expectStatus().isOk();
        verify(assumptionRepositoryMock, times(1)).findAllWithEagerRelationships(any());
    }

    @Test
    void getAssumption() {
        // Initialize the database
        insertedAssumption = assumptionRepository.save(assumption).block();

        // Get the assumption
        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, assumption.getId())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.id")
            .value(is(assumption.getId().intValue()))
            .jsonPath("$.statement")
            .value(is(DEFAULT_STATEMENT))
            .jsonPath("$.category")
            .value(is(DEFAULT_CATEGORY.toString()))
            .jsonPath("$.importance")
            .value(is(DEFAULT_IMPORTANCE))
            .jsonPath("$.evidence")
            .value(is(DEFAULT_EVIDENCE))
            .jsonPath("$.validated")
            .value(is(DEFAULT_VALIDATED))
            .jsonPath("$.createdDate")
            .value(is(DEFAULT_CREATED_DATE.toString()));
    }

    @Test
    void getNonExistingAssumption() {
        // Get the assumption
        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, Long.MAX_VALUE)
            .accept(MediaType.APPLICATION_PROBLEM_JSON)
            .exchange()
            .expectStatus()
            .isNotFound();
    }

    @Test
    void putExistingAssumption() throws Exception {
        // Initialize the database
        insertedAssumption = assumptionRepository.save(assumption).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the assumption
        Assumption updatedAssumption = assumptionRepository.findById(assumption.getId()).block();
        updatedAssumption
            .statement(UPDATED_STATEMENT)
            .category(UPDATED_CATEGORY)
            .importance(UPDATED_IMPORTANCE)
            .evidence(UPDATED_EVIDENCE)
            .validated(UPDATED_VALIDATED)
            .createdDate(UPDATED_CREATED_DATE);
        AssumptionDTO assumptionDTO = assumptionMapper.toDto(updatedAssumption);

        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, assumptionDTO.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(assumptionDTO))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Assumption in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedAssumptionToMatchAllProperties(updatedAssumption);
    }

    @Test
    void putNonExistingAssumption() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        assumption.setId(longCount.incrementAndGet());

        // Create the Assumption
        AssumptionDTO assumptionDTO = assumptionMapper.toDto(assumption);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, assumptionDTO.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(assumptionDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Assumption in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void putWithIdMismatchAssumption() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        assumption.setId(longCount.incrementAndGet());

        // Create the Assumption
        AssumptionDTO assumptionDTO = assumptionMapper.toDto(assumption);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, longCount.incrementAndGet())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(assumptionDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Assumption in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void putWithMissingIdPathParamAssumption() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        assumption.setId(longCount.incrementAndGet());

        // Create the Assumption
        AssumptionDTO assumptionDTO = assumptionMapper.toDto(assumption);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(assumptionDTO))
            .exchange()
            .expectStatus()
            .isEqualTo(405);

        // Validate the Assumption in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void partialUpdateAssumptionWithPatch() throws Exception {
        // Initialize the database
        insertedAssumption = assumptionRepository.save(assumption).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the assumption using partial update
        Assumption partialUpdatedAssumption = new Assumption();
        partialUpdatedAssumption.setId(assumption.getId());

        partialUpdatedAssumption.statement(UPDATED_STATEMENT).validated(UPDATED_VALIDATED);

        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, partialUpdatedAssumption.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(partialUpdatedAssumption))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Assumption in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertAssumptionUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedAssumption, assumption),
            getPersistedAssumption(assumption)
        );
    }

    @Test
    void fullUpdateAssumptionWithPatch() throws Exception {
        // Initialize the database
        insertedAssumption = assumptionRepository.save(assumption).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the assumption using partial update
        Assumption partialUpdatedAssumption = new Assumption();
        partialUpdatedAssumption.setId(assumption.getId());

        partialUpdatedAssumption
            .statement(UPDATED_STATEMENT)
            .category(UPDATED_CATEGORY)
            .importance(UPDATED_IMPORTANCE)
            .evidence(UPDATED_EVIDENCE)
            .validated(UPDATED_VALIDATED)
            .createdDate(UPDATED_CREATED_DATE);

        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, partialUpdatedAssumption.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(partialUpdatedAssumption))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Assumption in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertAssumptionUpdatableFieldsEquals(partialUpdatedAssumption, getPersistedAssumption(partialUpdatedAssumption));
    }

    @Test
    void patchNonExistingAssumption() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        assumption.setId(longCount.incrementAndGet());

        // Create the Assumption
        AssumptionDTO assumptionDTO = assumptionMapper.toDto(assumption);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, assumptionDTO.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(assumptionDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Assumption in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void patchWithIdMismatchAssumption() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        assumption.setId(longCount.incrementAndGet());

        // Create the Assumption
        AssumptionDTO assumptionDTO = assumptionMapper.toDto(assumption);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, longCount.incrementAndGet())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(assumptionDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Assumption in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void patchWithMissingIdPathParamAssumption() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        assumption.setId(longCount.incrementAndGet());

        // Create the Assumption
        AssumptionDTO assumptionDTO = assumptionMapper.toDto(assumption);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(assumptionDTO))
            .exchange()
            .expectStatus()
            .isEqualTo(405);

        // Validate the Assumption in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void deleteAssumption() {
        // Initialize the database
        insertedAssumption = assumptionRepository.save(assumption).block();

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the assumption
        webTestClient
            .delete()
            .uri(ENTITY_API_URL_ID, assumption.getId())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNoContent();

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return assumptionRepository.count().block();
    }

    protected void assertIncrementedRepositoryCount(long countBefore) {
        assertThat(countBefore + 1).isEqualTo(getRepositoryCount());
    }

    protected void assertDecrementedRepositoryCount(long countBefore) {
        assertThat(countBefore - 1).isEqualTo(getRepositoryCount());
    }

    protected void assertSameRepositoryCount(long countBefore) {
        assertThat(countBefore).isEqualTo(getRepositoryCount());
    }

    protected Assumption getPersistedAssumption(Assumption assumption) {
        return assumptionRepository.findById(assumption.getId()).block();
    }

    protected void assertPersistedAssumptionToMatchAllProperties(Assumption expectedAssumption) {
        // Test fails because reactive api returns an empty object instead of null
        // assertAssumptionAllPropertiesEquals(expectedAssumption, getPersistedAssumption(expectedAssumption));
        assertAssumptionUpdatableFieldsEquals(expectedAssumption, getPersistedAssumption(expectedAssumption));
    }

    protected void assertPersistedAssumptionToMatchUpdatableProperties(Assumption expectedAssumption) {
        // Test fails because reactive api returns an empty object instead of null
        // assertAssumptionAllUpdatablePropertiesEquals(expectedAssumption, getPersistedAssumption(expectedAssumption));
        assertAssumptionUpdatableFieldsEquals(expectedAssumption, getPersistedAssumption(expectedAssumption));
    }
}
