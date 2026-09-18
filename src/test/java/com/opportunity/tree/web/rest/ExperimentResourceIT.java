package com.opportunity.tree.web.rest;

import static com.opportunity.tree.domain.ExperimentAsserts.*;
import static com.opportunity.tree.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opportunity.tree.IntegrationTest;
import com.opportunity.tree.domain.Experiment;
import com.opportunity.tree.domain.Solution;
import com.opportunity.tree.domain.enumeration.ExperimentResult;
import com.opportunity.tree.domain.enumeration.ExperimentStatus;
import com.opportunity.tree.repository.ExperimentRepository;
import com.opportunity.tree.service.ExperimentService;
import com.opportunity.tree.service.dto.ExperimentDTO;
import com.opportunity.tree.service.mapper.ExperimentMapper;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the {@link ExperimentResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
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
    private MockMvc restExperimentMockMvc;

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
        if (TestUtil.findAll(em, Solution.class).isEmpty()) {
            solution = SolutionResourceIT.createEntity(em);
            em.persist(solution);
            em.flush();
        } else {
            solution = TestUtil.findAll(em, Solution.class).get(0);
        }
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
        if (TestUtil.findAll(em, Solution.class).isEmpty()) {
            solution = SolutionResourceIT.createUpdatedEntity(em);
            em.persist(solution);
            em.flush();
        } else {
            solution = TestUtil.findAll(em, Solution.class).get(0);
        }
        updatedExperiment.setSolution(solution);
        return updatedExperiment;
    }

    @BeforeEach
    void initTest() {
        experiment = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedExperiment != null) {
            experimentRepository.delete(insertedExperiment);
            insertedExperiment = null;
        }
    }

    @Test
    @Transactional
    void createExperiment() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Experiment
        ExperimentDTO experimentDTO = experimentMapper.toDto(experiment);
        var returnedExperimentDTO = om.readValue(
            restExperimentMockMvc
                .perform(
                    post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(experimentDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            ExperimentDTO.class
        );

        // Validate the Experiment in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedExperiment = experimentMapper.toEntity(returnedExperimentDTO);
        assertExperimentUpdatableFieldsEquals(returnedExperiment, getPersistedExperiment(returnedExperiment));

        insertedExperiment = returnedExperiment;
    }

    @Test
    @Transactional
    void createExperimentWithExistingId() throws Exception {
        // Create the Experiment with an existing ID
        experiment.setId(1L);
        ExperimentDTO experimentDTO = experimentMapper.toDto(experiment);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restExperimentMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(experimentDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Experiment in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkTitleIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        experiment.setTitle(null);

        // Create the Experiment, which fails.
        ExperimentDTO experimentDTO = experimentMapper.toDto(experiment);

        restExperimentMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(experimentDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkStatusIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        experiment.setStatus(null);

        // Create the Experiment, which fails.
        ExperimentDTO experimentDTO = experimentMapper.toDto(experiment);

        restExperimentMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(experimentDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkCreatedDateIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        experiment.setCreatedDate(null);

        // Create the Experiment, which fails.
        ExperimentDTO experimentDTO = experimentMapper.toDto(experiment);

        restExperimentMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(experimentDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllExperiments() throws Exception {
        // Initialize the database
        insertedExperiment = experimentRepository.saveAndFlush(experiment);

        // Get all the experimentList
        restExperimentMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(experiment.getId().intValue())))
            .andExpect(jsonPath("$.[*].title").value(hasItem(DEFAULT_TITLE)))
            .andExpect(jsonPath("$.[*].hypothesis").value(hasItem(DEFAULT_HYPOTHESIS)))
            .andExpect(jsonPath("$.[*].method").value(hasItem(DEFAULT_METHOD)))
            .andExpect(jsonPath("$.[*].successCriteria").value(hasItem(DEFAULT_SUCCESS_CRITERIA)))
            .andExpect(jsonPath("$.[*].status").value(hasItem(DEFAULT_STATUS.toString())))
            .andExpect(jsonPath("$.[*].result").value(hasItem(DEFAULT_RESULT.toString())))
            .andExpect(jsonPath("$.[*].learnings").value(hasItem(DEFAULT_LEARNINGS)))
            .andExpect(jsonPath("$.[*].startDate").value(hasItem(DEFAULT_START_DATE.toString())))
            .andExpect(jsonPath("$.[*].endDate").value(hasItem(DEFAULT_END_DATE.toString())))
            .andExpect(jsonPath("$.[*].createdDate").value(hasItem(DEFAULT_CREATED_DATE.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllExperimentsWithEagerRelationshipsIsEnabled() throws Exception {
        when(experimentServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restExperimentMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(experimentServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllExperimentsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(experimentServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restExperimentMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(experimentRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getExperiment() throws Exception {
        // Initialize the database
        insertedExperiment = experimentRepository.saveAndFlush(experiment);

        // Get the experiment
        restExperimentMockMvc
            .perform(get(ENTITY_API_URL_ID, experiment.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(experiment.getId().intValue()))
            .andExpect(jsonPath("$.title").value(DEFAULT_TITLE))
            .andExpect(jsonPath("$.hypothesis").value(DEFAULT_HYPOTHESIS))
            .andExpect(jsonPath("$.method").value(DEFAULT_METHOD))
            .andExpect(jsonPath("$.successCriteria").value(DEFAULT_SUCCESS_CRITERIA))
            .andExpect(jsonPath("$.status").value(DEFAULT_STATUS.toString()))
            .andExpect(jsonPath("$.result").value(DEFAULT_RESULT.toString()))
            .andExpect(jsonPath("$.learnings").value(DEFAULT_LEARNINGS))
            .andExpect(jsonPath("$.startDate").value(DEFAULT_START_DATE.toString()))
            .andExpect(jsonPath("$.endDate").value(DEFAULT_END_DATE.toString()))
            .andExpect(jsonPath("$.createdDate").value(DEFAULT_CREATED_DATE.toString()));
    }

    @Test
    @Transactional
    void getNonExistingExperiment() throws Exception {
        // Get the experiment
        restExperimentMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingExperiment() throws Exception {
        // Initialize the database
        insertedExperiment = experimentRepository.saveAndFlush(experiment);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the experiment
        Experiment updatedExperiment = experimentRepository.findById(experiment.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedExperiment are not directly saved in db
        em.detach(updatedExperiment);
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

        restExperimentMockMvc
            .perform(
                put(ENTITY_API_URL_ID, experimentDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(experimentDTO))
            )
            .andExpect(status().isOk());

        // Validate the Experiment in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedExperimentToMatchAllProperties(updatedExperiment);
    }

    @Test
    @Transactional
    void putNonExistingExperiment() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        experiment.setId(longCount.incrementAndGet());

        // Create the Experiment
        ExperimentDTO experimentDTO = experimentMapper.toDto(experiment);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restExperimentMockMvc
            .perform(
                put(ENTITY_API_URL_ID, experimentDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(experimentDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Experiment in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchExperiment() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        experiment.setId(longCount.incrementAndGet());

        // Create the Experiment
        ExperimentDTO experimentDTO = experimentMapper.toDto(experiment);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restExperimentMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(experimentDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Experiment in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamExperiment() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        experiment.setId(longCount.incrementAndGet());

        // Create the Experiment
        ExperimentDTO experimentDTO = experimentMapper.toDto(experiment);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restExperimentMockMvc
            .perform(put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(experimentDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Experiment in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateExperimentWithPatch() throws Exception {
        // Initialize the database
        insertedExperiment = experimentRepository.saveAndFlush(experiment);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the experiment using partial update
        Experiment partialUpdatedExperiment = new Experiment();
        partialUpdatedExperiment.setId(experiment.getId());

        partialUpdatedExperiment
            .title(UPDATED_TITLE)
            .hypothesis(UPDATED_HYPOTHESIS)
            .successCriteria(UPDATED_SUCCESS_CRITERIA)
            .status(UPDATED_STATUS)
            .result(UPDATED_RESULT)
            .startDate(UPDATED_START_DATE)
            .endDate(UPDATED_END_DATE)
            .createdDate(UPDATED_CREATED_DATE);

        restExperimentMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedExperiment.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedExperiment))
            )
            .andExpect(status().isOk());

        // Validate the Experiment in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertExperimentUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedExperiment, experiment),
            getPersistedExperiment(experiment)
        );
    }

    @Test
    @Transactional
    void fullUpdateExperimentWithPatch() throws Exception {
        // Initialize the database
        insertedExperiment = experimentRepository.saveAndFlush(experiment);

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

        restExperimentMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedExperiment.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedExperiment))
            )
            .andExpect(status().isOk());

        // Validate the Experiment in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertExperimentUpdatableFieldsEquals(partialUpdatedExperiment, getPersistedExperiment(partialUpdatedExperiment));
    }

    @Test
    @Transactional
    void patchNonExistingExperiment() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        experiment.setId(longCount.incrementAndGet());

        // Create the Experiment
        ExperimentDTO experimentDTO = experimentMapper.toDto(experiment);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restExperimentMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, experimentDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(experimentDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Experiment in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchExperiment() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        experiment.setId(longCount.incrementAndGet());

        // Create the Experiment
        ExperimentDTO experimentDTO = experimentMapper.toDto(experiment);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restExperimentMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(experimentDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Experiment in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamExperiment() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        experiment.setId(longCount.incrementAndGet());

        // Create the Experiment
        ExperimentDTO experimentDTO = experimentMapper.toDto(experiment);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restExperimentMockMvc
            .perform(
                patch(ENTITY_API_URL).with(csrf()).contentType("application/merge-patch+json").content(om.writeValueAsBytes(experimentDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the Experiment in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteExperiment() throws Exception {
        // Initialize the database
        insertedExperiment = experimentRepository.saveAndFlush(experiment);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the experiment
        restExperimentMockMvc
            .perform(delete(ENTITY_API_URL_ID, experiment.getId()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return experimentRepository.count();
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
        return experimentRepository.findById(experiment.getId()).orElseThrow();
    }

    protected void assertPersistedExperimentToMatchAllProperties(Experiment expectedExperiment) {
        assertExperimentAllPropertiesEquals(expectedExperiment, getPersistedExperiment(expectedExperiment));
    }

    protected void assertPersistedExperimentToMatchUpdatableProperties(Experiment expectedExperiment) {
        assertExperimentAllUpdatablePropertiesEquals(expectedExperiment, getPersistedExperiment(expectedExperiment));
    }
}
