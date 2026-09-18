package com.opportunity.tree.web.rest;

import static com.opportunity.tree.domain.AssumptionAsserts.*;
import static com.opportunity.tree.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opportunity.tree.IntegrationTest;
import com.opportunity.tree.domain.Assumption;
import com.opportunity.tree.domain.Solution;
import com.opportunity.tree.domain.enumeration.AssumptionCategory;
import com.opportunity.tree.repository.AssumptionRepository;
import com.opportunity.tree.service.AssumptionService;
import com.opportunity.tree.service.dto.AssumptionDTO;
import com.opportunity.tree.service.mapper.AssumptionMapper;
import jakarta.persistence.EntityManager;
import java.time.Instant;
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
 * Integration tests for the {@link AssumptionResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = "ROLE_ADMIN")
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
    private MockMvc restAssumptionMockMvc;

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
        if (TestUtil.findAll(em, Solution.class).isEmpty()) {
            solution = SolutionResourceIT.createEntity(em);
            em.persist(solution);
            em.flush();
        } else {
            solution = TestUtil.findAll(em, Solution.class).get(0);
        }
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
        if (TestUtil.findAll(em, Solution.class).isEmpty()) {
            solution = SolutionResourceIT.createUpdatedEntity(em);
            em.persist(solution);
            em.flush();
        } else {
            solution = TestUtil.findAll(em, Solution.class).get(0);
        }
        updatedAssumption.setSolution(solution);
        return updatedAssumption;
    }

    @BeforeEach
    void initTest() {
        assumption = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedAssumption != null) {
            assumptionRepository.delete(insertedAssumption);
            insertedAssumption = null;
        }
    }

    @Test
    @Transactional
    void createAssumption() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Assumption
        AssumptionDTO assumptionDTO = assumptionMapper.toDto(assumption);
        var returnedAssumptionDTO = om.readValue(
            restAssumptionMockMvc
                .perform(
                    post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(assumptionDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            AssumptionDTO.class
        );

        // Validate the Assumption in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedAssumption = assumptionMapper.toEntity(returnedAssumptionDTO);
        assertAssumptionUpdatableFieldsEquals(returnedAssumption, getPersistedAssumption(returnedAssumption));

        insertedAssumption = returnedAssumption;
    }

    @Test
    @Transactional
    void createAssumptionWithExistingId() throws Exception {
        // Create the Assumption with an existing ID
        assumption.setId(1L);
        AssumptionDTO assumptionDTO = assumptionMapper.toDto(assumption);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restAssumptionMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(assumptionDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Assumption in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkStatementIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        assumption.setStatement(null);

        // Create the Assumption, which fails.
        AssumptionDTO assumptionDTO = assumptionMapper.toDto(assumption);

        restAssumptionMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(assumptionDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkCategoryIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        assumption.setCategory(null);

        // Create the Assumption, which fails.
        AssumptionDTO assumptionDTO = assumptionMapper.toDto(assumption);

        restAssumptionMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(assumptionDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkImportanceIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        assumption.setImportance(null);

        // Create the Assumption, which fails.
        AssumptionDTO assumptionDTO = assumptionMapper.toDto(assumption);

        restAssumptionMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(assumptionDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkEvidenceIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        assumption.setEvidence(null);

        // Create the Assumption, which fails.
        AssumptionDTO assumptionDTO = assumptionMapper.toDto(assumption);

        restAssumptionMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(assumptionDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkCreatedDateIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        assumption.setCreatedDate(null);

        // Create the Assumption, which fails.
        AssumptionDTO assumptionDTO = assumptionMapper.toDto(assumption);

        restAssumptionMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(assumptionDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllAssumptions() throws Exception {
        // Initialize the database
        insertedAssumption = assumptionRepository.saveAndFlush(assumption);

        // Get all the assumptionList
        restAssumptionMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(assumption.getId().intValue())))
            .andExpect(jsonPath("$.[*].statement").value(hasItem(DEFAULT_STATEMENT)))
            .andExpect(jsonPath("$.[*].category").value(hasItem(DEFAULT_CATEGORY.toString())))
            .andExpect(jsonPath("$.[*].importance").value(hasItem(DEFAULT_IMPORTANCE)))
            .andExpect(jsonPath("$.[*].evidence").value(hasItem(DEFAULT_EVIDENCE)))
            .andExpect(jsonPath("$.[*].validated").value(hasItem(DEFAULT_VALIDATED)))
            .andExpect(jsonPath("$.[*].createdDate").value(hasItem(DEFAULT_CREATED_DATE.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllAssumptionsWithEagerRelationshipsIsEnabled() throws Exception {
        when(assumptionServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restAssumptionMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(assumptionServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllAssumptionsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(assumptionServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restAssumptionMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(assumptionRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getAssumption() throws Exception {
        // Initialize the database
        insertedAssumption = assumptionRepository.saveAndFlush(assumption);

        // Get the assumption
        restAssumptionMockMvc
            .perform(get(ENTITY_API_URL_ID, assumption.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(assumption.getId().intValue()))
            .andExpect(jsonPath("$.statement").value(DEFAULT_STATEMENT))
            .andExpect(jsonPath("$.category").value(DEFAULT_CATEGORY.toString()))
            .andExpect(jsonPath("$.importance").value(DEFAULT_IMPORTANCE))
            .andExpect(jsonPath("$.evidence").value(DEFAULT_EVIDENCE))
            .andExpect(jsonPath("$.validated").value(DEFAULT_VALIDATED))
            .andExpect(jsonPath("$.createdDate").value(DEFAULT_CREATED_DATE.toString()));
    }

    @Test
    @Transactional
    void getNonExistingAssumption() throws Exception {
        // Get the assumption
        restAssumptionMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingAssumption() throws Exception {
        // Initialize the database
        insertedAssumption = assumptionRepository.saveAndFlush(assumption);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the assumption
        Assumption updatedAssumption = assumptionRepository.findById(assumption.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedAssumption are not directly saved in db
        em.detach(updatedAssumption);
        updatedAssumption
            .statement(UPDATED_STATEMENT)
            .category(UPDATED_CATEGORY)
            .importance(UPDATED_IMPORTANCE)
            .evidence(UPDATED_EVIDENCE)
            .validated(UPDATED_VALIDATED)
            .createdDate(UPDATED_CREATED_DATE);
        AssumptionDTO assumptionDTO = assumptionMapper.toDto(updatedAssumption);

        restAssumptionMockMvc
            .perform(
                put(ENTITY_API_URL_ID, assumptionDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(assumptionDTO))
            )
            .andExpect(status().isOk());

        // Validate the Assumption in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedAssumptionToMatchAllProperties(updatedAssumption);
    }

    @Test
    @Transactional
    void putNonExistingAssumption() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        assumption.setId(longCount.incrementAndGet());

        // Create the Assumption
        AssumptionDTO assumptionDTO = assumptionMapper.toDto(assumption);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restAssumptionMockMvc
            .perform(
                put(ENTITY_API_URL_ID, assumptionDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(assumptionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Assumption in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchAssumption() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        assumption.setId(longCount.incrementAndGet());

        // Create the Assumption
        AssumptionDTO assumptionDTO = assumptionMapper.toDto(assumption);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAssumptionMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(assumptionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Assumption in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamAssumption() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        assumption.setId(longCount.incrementAndGet());

        // Create the Assumption
        AssumptionDTO assumptionDTO = assumptionMapper.toDto(assumption);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAssumptionMockMvc
            .perform(put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(assumptionDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Assumption in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateAssumptionWithPatch() throws Exception {
        // Initialize the database
        insertedAssumption = assumptionRepository.saveAndFlush(assumption);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the assumption using partial update
        Assumption partialUpdatedAssumption = new Assumption();
        partialUpdatedAssumption.setId(assumption.getId());

        partialUpdatedAssumption
            .category(UPDATED_CATEGORY)
            .evidence(UPDATED_EVIDENCE)
            .validated(UPDATED_VALIDATED)
            .createdDate(UPDATED_CREATED_DATE);

        restAssumptionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedAssumption.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedAssumption))
            )
            .andExpect(status().isOk());

        // Validate the Assumption in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertAssumptionUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedAssumption, assumption),
            getPersistedAssumption(assumption)
        );
    }

    @Test
    @Transactional
    void fullUpdateAssumptionWithPatch() throws Exception {
        // Initialize the database
        insertedAssumption = assumptionRepository.saveAndFlush(assumption);

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

        restAssumptionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedAssumption.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedAssumption))
            )
            .andExpect(status().isOk());

        // Validate the Assumption in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertAssumptionUpdatableFieldsEquals(partialUpdatedAssumption, getPersistedAssumption(partialUpdatedAssumption));
    }

    @Test
    @Transactional
    void patchNonExistingAssumption() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        assumption.setId(longCount.incrementAndGet());

        // Create the Assumption
        AssumptionDTO assumptionDTO = assumptionMapper.toDto(assumption);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restAssumptionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, assumptionDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(assumptionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Assumption in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchAssumption() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        assumption.setId(longCount.incrementAndGet());

        // Create the Assumption
        AssumptionDTO assumptionDTO = assumptionMapper.toDto(assumption);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAssumptionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(assumptionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Assumption in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamAssumption() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        assumption.setId(longCount.incrementAndGet());

        // Create the Assumption
        AssumptionDTO assumptionDTO = assumptionMapper.toDto(assumption);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAssumptionMockMvc
            .perform(
                patch(ENTITY_API_URL).with(csrf()).contentType("application/merge-patch+json").content(om.writeValueAsBytes(assumptionDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the Assumption in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteAssumption() throws Exception {
        // Initialize the database
        insertedAssumption = assumptionRepository.saveAndFlush(assumption);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the assumption
        restAssumptionMockMvc
            .perform(delete(ENTITY_API_URL_ID, assumption.getId()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return assumptionRepository.count();
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
        return assumptionRepository.findById(assumption.getId()).orElseThrow();
    }

    protected void assertPersistedAssumptionToMatchAllProperties(Assumption expectedAssumption) {
        assertAssumptionAllPropertiesEquals(expectedAssumption, getPersistedAssumption(expectedAssumption));
    }

    protected void assertPersistedAssumptionToMatchUpdatableProperties(Assumption expectedAssumption) {
        assertAssumptionAllUpdatablePropertiesEquals(expectedAssumption, getPersistedAssumption(expectedAssumption));
    }
}
