package com.opportunity.tree.web.rest;

import static com.opportunity.tree.domain.ExperimentAsserts.*;
import static com.opportunity.tree.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opportunity.tree.IntegrationTest;
import com.opportunity.tree.domain.Experiment;
import com.opportunity.tree.domain.Solution;
import com.opportunity.tree.domain.enumeration.ExperimentResult;
import com.opportunity.tree.domain.enumeration.ExperimentStatus;
import com.opportunity.tree.repository.EntityManager;
import com.opportunity.tree.repository.ExperimentRepository;
import com.opportunity.tree.service.ExperimentService;
import com.opportunity.tree.service.dto.ExperimentDTO;
import com.opportunity.tree.service.mapper.ExperimentMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
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
 * Integration tests for the {@link ExperimentResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureWebTestClient(timeout = IntegrationTest.DEFAULT_ENTITY_TIMEOUT)
@WithMockUser
class ExperimentResourceIT {

    private static final String DEFAULT_TITLE = "AAAAAAAAAA";
    private static final String UPDATED_TITLE = "BBBBBBBBBB";

    private static final String DEFAULT_HYPOTHESIS = "AAAAAAAAAA";
    private static final String UPDATED_HYPOTHESIS = "BBBBBBBBBB";

    private static final String DEFAULT_METHOD = "AAAAAAAAAA";
    private static final String UPDATED_METHOD = "BBBBBBBBBB";

    private static final String DEFAULT_SUCCESS_CRITERIA = "AAAAAAAAAA";
    private static final String UPDATED_SUCCESS_CRITERIA = "BBBBBBBBBB";

    private static final ExperimentStatus DEFAULT_STATUS = ExperimentStatus.PLANNED;
    private static final ExperimentStatus UPDATED_STATUS = ExperimentStatus.RUNNING;

    private static final ExperimentResult DEFAULT_RESULT = ExperimentResult.SUPPORTED;
    private static final ExperimentResult UPDATED_RESULT = ExperimentResult.REFUTED;

    private static final String DEFAULT_LEARNINGS = "AAAAAAAAAA";
    private static final String UPDATED_LEARNINGS = "BBBBBBBBBB";

    private static final LocalDate DEFAULT_START_DATE = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_START_DATE = LocalDate.now(ZoneId.systemDefault());

    private static final LocalDate DEFAULT_END_DATE = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_END_DATE = LocalDate.now(ZoneId.systemDefault());

    private static final Instant DEFAULT_CREATED_DATE = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CREATED_DATE = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/experiments";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2L * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private ExperimentRepository experimentRepository;

    @Mock
    private ExperimentRepository experimentRepositoryMock;

    @Autowired
    private ExperimentMapper experimentMapper;

    @Mock
    private ExperimentService experimentServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private WebTestClient webTestClient;

    private Experiment experiment;

    private Experiment insertedExperiment;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Experiment createEntity(EntityManager em) {
        Experiment experiment = new Experiment()
            .title(DEFAULT_TITLE)
            .hypothesis(DEFAULT_HYPOTHESIS)
            .method(DEFAULT_METHOD)
            .successCriteria(DEFAULT_SUCCESS_CRITERIA)
            .status(DEFAULT_STATUS)
            .result(DEFAULT_RESULT)
            .learnings(DEFAULT_LEARNINGS)
            .startDate(DEFAULT_START_DATE)
            .endDate(DEFAULT_END_DATE)
            .createdDate(DEFAULT_CREATED_DATE);
        // Add required entity
        Solution solution;
        solution = em.insert(SolutionResourceIT.createEntity(em)).block();
        experiment.setSolution(solution);
        return experiment;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Experiment createUpdatedEntity(EntityManager em) {
        Experiment updatedExperiment = new Experiment()
            .title(UPDATED_TITLE)
            .hypothesis(UPDATED_HYPOTHESIS)
            .method(UPDATED_METHOD)
            .successCriteria(UPDATED_SUCCESS_CRITERIA)
            .status(UPDATED_STATUS)
            .result(UPDATED_RESULT)
            .learnings(UPDATED_LEARNINGS)
            .startDate(UPDATED_START_DATE)
            .endDate(UPDATED_END_DATE)
            .createdDate(UPDATED_CREATED_DATE);
        // Add required entity
        Solution solution;
        solution = em.insert(SolutionResourceIT.createUpdatedEntity(em)).block();
        updatedExperiment.setSolution(solution);
        return updatedExperiment;
    }

    public static void deleteEntities(EntityManager em) {
        try {
            em.deleteAll("rel_experiment__assumption").block();
            em.deleteAll(Experiment.class).block();
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
        experiment = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedExperiment != null) {
            experimentRepository.delete(insertedExperiment).block();
            insertedExperiment = null;
        }
        deleteEntities(em);
    }

    @Test
    void createExperiment() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Experiment
        ExperimentDTO experimentDTO = experimentMapper.toDto(experiment);
        var returnedExperimentDTO = webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(experimentDTO))
            .exchange()
            .expectStatus()
            .isCreated()
            .expectBody(ExperimentDTO.class)
            .returnResult()
            .getResponseBody();

        // Validate the Experiment in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedExperiment = experimentMapper.toEntity(returnedExperimentDTO);
        assertExperimentUpdatableFieldsEquals(returnedExperiment, getPersistedExperiment(returnedExperiment));

        insertedExperiment = returnedExperiment;
    }

    @Test
    void createExperimentWithExistingId() throws Exception {
        // Create the Experiment with an existing ID
        experiment.setId(1L);
        ExperimentDTO experimentDTO = experimentMapper.toDto(experiment);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(experimentDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Experiment in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    void checkTitleIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        experiment.setTitle(null);

        // Create the Experiment, which fails.
        ExperimentDTO experimentDTO = experimentMapper.toDto(experiment);

        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(experimentDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    void checkStatusIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        experiment.setStatus(null);

        // Create the Experiment, which fails.
        ExperimentDTO experimentDTO = experimentMapper.toDto(experiment);

        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(experimentDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    void checkCreatedDateIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        experiment.setCreatedDate(null);

        // Create the Experiment, which fails.
        ExperimentDTO experimentDTO = experimentMapper.toDto(experiment);

        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(experimentDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    void getAllExperiments() {
        // Initialize the database
        insertedExperiment = experimentRepository.save(experiment).block();

        // Get all the experimentList
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
            .value(hasItem(experiment.getId().intValue()))
            .jsonPath("$.[*].title")
            .value(hasItem(DEFAULT_TITLE))
            .jsonPath("$.[*].hypothesis")
            .value(hasItem(DEFAULT_HYPOTHESIS))
            .jsonPath("$.[*].method")
            .value(hasItem(DEFAULT_METHOD))
            .jsonPath("$.[*].successCriteria")
            .value(hasItem(DEFAULT_SUCCESS_CRITERIA))
            .jsonPath("$.[*].status")
            .value(hasItem(DEFAULT_STATUS.toString()))
            .jsonPath("$.[*].result")
            .value(hasItem(DEFAULT_RESULT.toString()))
            .jsonPath("$.[*].learnings")
            .value(hasItem(DEFAULT_LEARNINGS))
            .jsonPath("$.[*].startDate")
            .value(hasItem(DEFAULT_START_DATE.toString()))
            .jsonPath("$.[*].endDate")
            .value(hasItem(DEFAULT_END_DATE.toString()))
            .jsonPath("$.[*].createdDate")
            .value(hasItem(DEFAULT_CREATED_DATE.toString()));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllExperimentsWithEagerRelationshipsIsEnabled() {
        when(experimentServiceMock.findAllWithEagerRelationships(any())).thenReturn(Flux.empty());

        webTestClient.get().uri(ENTITY_API_URL + "?eagerload=true").exchange().expectStatus().isOk();

        verify(experimentServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllExperimentsWithEagerRelationshipsIsNotEnabled() {
        when(experimentServiceMock.findAllWithEagerRelationships(any())).thenReturn(Flux.empty());

        webTestClient.get().uri(ENTITY_API_URL + "?eagerload=false").exchange().expectStatus().isOk();
        verify(experimentRepositoryMock, times(1)).findAllWithEagerRelationships(any());
    }

    @Test
    void getExperiment() {
        // Initialize the database
        insertedExperiment = experimentRepository.save(experiment).block();

        // Get the experiment
        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, experiment.getId())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.id")
            .value(is(experiment.getId().intValue()))
            .jsonPath("$.title")
            .value(is(DEFAULT_TITLE))
            .jsonPath("$.hypothesis")
            .value(is(DEFAULT_HYPOTHESIS))
            .jsonPath("$.method")
            .value(is(DEFAULT_METHOD))
            .jsonPath("$.successCriteria")
            .value(is(DEFAULT_SUCCESS_CRITERIA))
            .jsonPath("$.status")
            .value(is(DEFAULT_STATUS.toString()))
            .jsonPath("$.result")
            .value(is(DEFAULT_RESULT.toString()))
            .jsonPath("$.learnings")
            .value(is(DEFAULT_LEARNINGS))
            .jsonPath("$.startDate")
            .value(is(DEFAULT_START_DATE.toString()))
            .jsonPath("$.endDate")
            .value(is(DEFAULT_END_DATE.toString()))
            .jsonPath("$.createdDate")
            .value(is(DEFAULT_CREATED_DATE.toString()));
    }

    @Test
    void getNonExistingExperiment() {
        // Get the experiment
        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, Long.MAX_VALUE)
            .accept(MediaType.APPLICATION_PROBLEM_JSON)
            .exchange()
            .expectStatus()
            .isNotFound();
    }

    @Test
    void putExistingExperiment() throws Exception {
        // Initialize the database
        insertedExperiment = experimentRepository.save(experiment).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the experiment
        Experiment updatedExperiment = experimentRepository.findById(experiment.getId()).block();
        updatedExperiment
            .title(UPDATED_TITLE)
            .hypothesis(UPDATED_HYPOTHESIS)
            .method(UPDATED_METHOD)
            .successCriteria(UPDATED_SUCCESS_CRITERIA)
            .status(UPDATED_STATUS)
            .result(UPDATED_RESULT)
            .learnings(UPDATED_LEARNINGS)
            .startDate(UPDATED_START_DATE)
            .endDate(UPDATED_END_DATE)
            .createdDate(UPDATED_CREATED_DATE);
        ExperimentDTO experimentDTO = experimentMapper.toDto(updatedExperiment);

        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, experimentDTO.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(experimentDTO))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Experiment in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedExperimentToMatchAllProperties(updatedExperiment);
    }

    @Test
    void putNonExistingExperiment() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        experiment.setId(longCount.incrementAndGet());

        // Create the Experiment
        ExperimentDTO experimentDTO = experimentMapper.toDto(experiment);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, experimentDTO.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(experimentDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Experiment in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void putWithIdMismatchExperiment() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        experiment.setId(longCount.incrementAndGet());

        // Create the Experiment
        ExperimentDTO experimentDTO = experimentMapper.toDto(experiment);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, longCount.incrementAndGet())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(experimentDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Experiment in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void putWithMissingIdPathParamExperiment() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        experiment.setId(longCount.incrementAndGet());

        // Create the Experiment
        ExperimentDTO experimentDTO = experimentMapper.toDto(experiment);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(experimentDTO))
            .exchange()
            .expectStatus()
            .isEqualTo(405);

        // Validate the Experiment in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void partialUpdateExperimentWithPatch() throws Exception {
        // Initialize the database
        insertedExperiment = experimentRepository.save(experiment).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the experiment using partial update
        Experiment partialUpdatedExperiment = new Experiment();
        partialUpdatedExperiment.setId(experiment.getId());

        partialUpdatedExperiment
            .title(UPDATED_TITLE)
            .successCriteria(UPDATED_SUCCESS_CRITERIA)
            .result(UPDATED_RESULT)
            .learnings(UPDATED_LEARNINGS)
            .createdDate(UPDATED_CREATED_DATE);

        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, partialUpdatedExperiment.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(partialUpdatedExperiment))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Experiment in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertExperimentUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedExperiment, experiment),
            getPersistedExperiment(experiment)
        );
    }

    @Test
    void fullUpdateExperimentWithPatch() throws Exception {
        // Initialize the database
        insertedExperiment = experimentRepository.save(experiment).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the experiment using partial update
        Experiment partialUpdatedExperiment = new Experiment();
        partialUpdatedExperiment.setId(experiment.getId());

        partialUpdatedExperiment
            .title(UPDATED_TITLE)
            .hypothesis(UPDATED_HYPOTHESIS)
            .method(UPDATED_METHOD)
            .successCriteria(UPDATED_SUCCESS_CRITERIA)
            .status(UPDATED_STATUS)
            .result(UPDATED_RESULT)
            .learnings(UPDATED_LEARNINGS)
            .startDate(UPDATED_START_DATE)
            .endDate(UPDATED_END_DATE)
            .createdDate(UPDATED_CREATED_DATE);

        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, partialUpdatedExperiment.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(partialUpdatedExperiment))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Experiment in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertExperimentUpdatableFieldsEquals(partialUpdatedExperiment, getPersistedExperiment(partialUpdatedExperiment));
    }

    @Test
    void patchNonExistingExperiment() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        experiment.setId(longCount.incrementAndGet());

        // Create the Experiment
        ExperimentDTO experimentDTO = experimentMapper.toDto(experiment);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, experimentDTO.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(experimentDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Experiment in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void patchWithIdMismatchExperiment() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        experiment.setId(longCount.incrementAndGet());

        // Create the Experiment
        ExperimentDTO experimentDTO = experimentMapper.toDto(experiment);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, longCount.incrementAndGet())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(experimentDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Experiment in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void patchWithMissingIdPathParamExperiment() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        experiment.setId(longCount.incrementAndGet());

        // Create the Experiment
        ExperimentDTO experimentDTO = experimentMapper.toDto(experiment);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(experimentDTO))
            .exchange()
            .expectStatus()
            .isEqualTo(405);

        // Validate the Experiment in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void deleteExperiment() {
        // Initialize the database
        insertedExperiment = experimentRepository.save(experiment).block();

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the experiment
        webTestClient
            .delete()
            .uri(ENTITY_API_URL_ID, experiment.getId())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNoContent();

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return experimentRepository.count().block();
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

    protected Experiment getPersistedExperiment(Experiment experiment) {
        return experimentRepository.findById(experiment.getId()).block();
    }

    protected void assertPersistedExperimentToMatchAllProperties(Experiment expectedExperiment) {
        // Test fails because reactive api returns an empty object instead of null
        // assertExperimentAllPropertiesEquals(expectedExperiment, getPersistedExperiment(expectedExperiment));
        assertExperimentUpdatableFieldsEquals(expectedExperiment, getPersistedExperiment(expectedExperiment));
    }

    protected void assertPersistedExperimentToMatchUpdatableProperties(Experiment expectedExperiment) {
        // Test fails because reactive api returns an empty object instead of null
        // assertExperimentAllUpdatablePropertiesEquals(expectedExperiment, getPersistedExperiment(expectedExperiment));
        assertExperimentUpdatableFieldsEquals(expectedExperiment, getPersistedExperiment(expectedExperiment));
    }
}
