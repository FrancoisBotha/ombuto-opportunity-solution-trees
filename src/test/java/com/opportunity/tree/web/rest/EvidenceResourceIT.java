package com.opportunity.tree.web.rest;

import static com.opportunity.tree.domain.EvidenceAsserts.*;
import static com.opportunity.tree.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opportunity.tree.IntegrationTest;
import com.opportunity.tree.domain.Evidence;
import com.opportunity.tree.repository.EvidenceRepository;
import com.opportunity.tree.service.EvidenceService;
import com.opportunity.tree.service.dto.EvidenceDTO;
import com.opportunity.tree.service.mapper.EvidenceMapper;
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
 * Integration tests for the {@link EvidenceResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = "ROLE_ADMIN")
class EvidenceResourceIT {

    private static final String DEFAULT_TITLE = "AAAAAAAAAA";
    private static final String UPDATED_TITLE = "BBBBBBBBBB";

    private static final String DEFAULT_DESCRIPTION = "AAAAAAAAAA";
    private static final String UPDATED_DESCRIPTION = "BBBBBBBBBB";

    private static final Integer DEFAULT_SORT_ORDER = 1;
    private static final Integer UPDATED_SORT_ORDER = 2;

    private static final Instant DEFAULT_CREATED_DATE = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CREATED_DATE = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_LAST_MODIFIED_DATE = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_LAST_MODIFIED_DATE = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/evidences";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2L * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private EvidenceRepository evidenceRepository;

    @Mock
    private EvidenceRepository evidenceRepositoryMock;

    @Autowired
    private EvidenceMapper evidenceMapper;

    @Mock
    private EvidenceService evidenceServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restEvidenceMockMvc;

    private Evidence evidence;

    private Evidence insertedEvidence;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Evidence createEntity() {
        return new Evidence()
            .title(DEFAULT_TITLE)
            .description(DEFAULT_DESCRIPTION)
            .sortOrder(DEFAULT_SORT_ORDER)
            .createdDate(DEFAULT_CREATED_DATE)
            .lastModifiedDate(DEFAULT_LAST_MODIFIED_DATE);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Evidence createUpdatedEntity() {
        return new Evidence()
            .title(UPDATED_TITLE)
            .description(UPDATED_DESCRIPTION)
            .sortOrder(UPDATED_SORT_ORDER)
            .createdDate(UPDATED_CREATED_DATE)
            .lastModifiedDate(UPDATED_LAST_MODIFIED_DATE);
    }

    @BeforeEach
    void initTest() {
        evidence = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedEvidence != null) {
            evidenceRepository.delete(insertedEvidence);
            insertedEvidence = null;
        }
    }

    @Test
    @Transactional
    void createEvidence() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Evidence
        EvidenceDTO evidenceDTO = evidenceMapper.toDto(evidence);
        var returnedEvidenceDTO = om.readValue(
            restEvidenceMockMvc
                .perform(
                    post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(evidenceDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            EvidenceDTO.class
        );

        // Validate the Evidence in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedEvidence = evidenceMapper.toEntity(returnedEvidenceDTO);
        assertEvidenceUpdatableFieldsEquals(returnedEvidence, getPersistedEvidence(returnedEvidence));

        insertedEvidence = returnedEvidence;
    }

    @Test
    @Transactional
    void createEvidenceWithExistingId() throws Exception {
        // Create the Evidence with an existing ID
        evidence.setId(1L);
        EvidenceDTO evidenceDTO = evidenceMapper.toDto(evidence);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restEvidenceMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(evidenceDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Evidence in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkTitleIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        evidence.setTitle(null);

        // Create the Evidence, which fails.
        EvidenceDTO evidenceDTO = evidenceMapper.toDto(evidence);

        restEvidenceMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(evidenceDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkSortOrderIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        evidence.setSortOrder(null);

        // Create the Evidence, which fails.
        EvidenceDTO evidenceDTO = evidenceMapper.toDto(evidence);

        restEvidenceMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(evidenceDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkCreatedDateIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        evidence.setCreatedDate(null);

        // Create the Evidence, which fails.
        EvidenceDTO evidenceDTO = evidenceMapper.toDto(evidence);

        restEvidenceMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(evidenceDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllEvidences() throws Exception {
        // Initialize the database
        insertedEvidence = evidenceRepository.saveAndFlush(evidence);

        // Get all the evidenceList
        restEvidenceMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(evidence.getId().intValue())))
            .andExpect(jsonPath("$.[*].title").value(hasItem(DEFAULT_TITLE)))
            .andExpect(jsonPath("$.[*].description").value(hasItem(DEFAULT_DESCRIPTION)))
            .andExpect(jsonPath("$.[*].sortOrder").value(hasItem(DEFAULT_SORT_ORDER)))
            .andExpect(jsonPath("$.[*].createdDate").value(hasItem(DEFAULT_CREATED_DATE.toString())))
            .andExpect(jsonPath("$.[*].lastModifiedDate").value(hasItem(DEFAULT_LAST_MODIFIED_DATE.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllEvidencesWithEagerRelationshipsIsEnabled() throws Exception {
        when(evidenceServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restEvidenceMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(evidenceServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllEvidencesWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(evidenceServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restEvidenceMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(evidenceRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getEvidence() throws Exception {
        // Initialize the database
        insertedEvidence = evidenceRepository.saveAndFlush(evidence);

        // Get the evidence
        restEvidenceMockMvc
            .perform(get(ENTITY_API_URL_ID, evidence.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(evidence.getId().intValue()))
            .andExpect(jsonPath("$.title").value(DEFAULT_TITLE))
            .andExpect(jsonPath("$.description").value(DEFAULT_DESCRIPTION))
            .andExpect(jsonPath("$.sortOrder").value(DEFAULT_SORT_ORDER))
            .andExpect(jsonPath("$.createdDate").value(DEFAULT_CREATED_DATE.toString()))
            .andExpect(jsonPath("$.lastModifiedDate").value(DEFAULT_LAST_MODIFIED_DATE.toString()));
    }

    @Test
    @Transactional
    void getNonExistingEvidence() throws Exception {
        // Get the evidence
        restEvidenceMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingEvidence() throws Exception {
        // Initialize the database
        insertedEvidence = evidenceRepository.saveAndFlush(evidence);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the evidence
        Evidence updatedEvidence = evidenceRepository.findById(evidence.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedEvidence are not directly saved in db
        em.detach(updatedEvidence);
        updatedEvidence
            .title(UPDATED_TITLE)
            .description(UPDATED_DESCRIPTION)
            .sortOrder(UPDATED_SORT_ORDER)
            .createdDate(UPDATED_CREATED_DATE)
            .lastModifiedDate(UPDATED_LAST_MODIFIED_DATE);
        EvidenceDTO evidenceDTO = evidenceMapper.toDto(updatedEvidence);

        restEvidenceMockMvc
            .perform(
                put(ENTITY_API_URL_ID, evidenceDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(evidenceDTO))
            )
            .andExpect(status().isOk());

        // Validate the Evidence in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedEvidenceToMatchAllProperties(updatedEvidence);
    }

    @Test
    @Transactional
    void putNonExistingEvidence() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        evidence.setId(longCount.incrementAndGet());

        // Create the Evidence
        EvidenceDTO evidenceDTO = evidenceMapper.toDto(evidence);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restEvidenceMockMvc
            .perform(
                put(ENTITY_API_URL_ID, evidenceDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(evidenceDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Evidence in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchEvidence() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        evidence.setId(longCount.incrementAndGet());

        // Create the Evidence
        EvidenceDTO evidenceDTO = evidenceMapper.toDto(evidence);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restEvidenceMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(evidenceDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Evidence in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamEvidence() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        evidence.setId(longCount.incrementAndGet());

        // Create the Evidence
        EvidenceDTO evidenceDTO = evidenceMapper.toDto(evidence);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restEvidenceMockMvc
            .perform(put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(evidenceDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Evidence in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateEvidenceWithPatch() throws Exception {
        // Initialize the database
        insertedEvidence = evidenceRepository.saveAndFlush(evidence);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the evidence using partial update
        Evidence partialUpdatedEvidence = new Evidence();
        partialUpdatedEvidence.setId(evidence.getId());

        partialUpdatedEvidence
            .title(UPDATED_TITLE)
            .description(UPDATED_DESCRIPTION)
            .sortOrder(UPDATED_SORT_ORDER)
            .createdDate(UPDATED_CREATED_DATE);

        restEvidenceMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedEvidence.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedEvidence))
            )
            .andExpect(status().isOk());

        // Validate the Evidence in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertEvidenceUpdatableFieldsEquals(createUpdateProxyForBean(partialUpdatedEvidence, evidence), getPersistedEvidence(evidence));
    }

    @Test
    @Transactional
    void fullUpdateEvidenceWithPatch() throws Exception {
        // Initialize the database
        insertedEvidence = evidenceRepository.saveAndFlush(evidence);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the evidence using partial update
        Evidence partialUpdatedEvidence = new Evidence();
        partialUpdatedEvidence.setId(evidence.getId());

        partialUpdatedEvidence
            .title(UPDATED_TITLE)
            .description(UPDATED_DESCRIPTION)
            .sortOrder(UPDATED_SORT_ORDER)
            .createdDate(UPDATED_CREATED_DATE)
            .lastModifiedDate(UPDATED_LAST_MODIFIED_DATE);

        restEvidenceMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedEvidence.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedEvidence))
            )
            .andExpect(status().isOk());

        // Validate the Evidence in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertEvidenceUpdatableFieldsEquals(partialUpdatedEvidence, getPersistedEvidence(partialUpdatedEvidence));
    }

    @Test
    @Transactional
    void patchNonExistingEvidence() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        evidence.setId(longCount.incrementAndGet());

        // Create the Evidence
        EvidenceDTO evidenceDTO = evidenceMapper.toDto(evidence);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restEvidenceMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, evidenceDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(evidenceDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Evidence in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchEvidence() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        evidence.setId(longCount.incrementAndGet());

        // Create the Evidence
        EvidenceDTO evidenceDTO = evidenceMapper.toDto(evidence);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restEvidenceMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(evidenceDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Evidence in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamEvidence() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        evidence.setId(longCount.incrementAndGet());

        // Create the Evidence
        EvidenceDTO evidenceDTO = evidenceMapper.toDto(evidence);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restEvidenceMockMvc
            .perform(
                patch(ENTITY_API_URL).with(csrf()).contentType("application/merge-patch+json").content(om.writeValueAsBytes(evidenceDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the Evidence in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteEvidence() throws Exception {
        // Initialize the database
        insertedEvidence = evidenceRepository.saveAndFlush(evidence);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the evidence
        restEvidenceMockMvc
            .perform(delete(ENTITY_API_URL_ID, evidence.getId()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return evidenceRepository.count();
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

    protected Evidence getPersistedEvidence(Evidence evidence) {
        return evidenceRepository.findById(evidence.getId()).orElseThrow();
    }

    protected void assertPersistedEvidenceToMatchAllProperties(Evidence expectedEvidence) {
        assertEvidenceAllPropertiesEquals(expectedEvidence, getPersistedEvidence(expectedEvidence));
    }

    protected void assertPersistedEvidenceToMatchUpdatableProperties(Evidence expectedEvidence) {
        assertEvidenceAllUpdatablePropertiesEquals(expectedEvidence, getPersistedEvidence(expectedEvidence));
    }
}
