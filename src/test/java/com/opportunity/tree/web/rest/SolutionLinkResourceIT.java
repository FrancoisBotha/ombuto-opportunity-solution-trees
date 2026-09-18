package com.opportunity.tree.web.rest;

import static com.opportunity.tree.domain.SolutionLinkAsserts.*;
import static com.opportunity.tree.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opportunity.tree.IntegrationTest;
import com.opportunity.tree.domain.Solution;
import com.opportunity.tree.domain.SolutionLink;
import com.opportunity.tree.domain.enumeration.LinkType;
import com.opportunity.tree.repository.SolutionLinkRepository;
import com.opportunity.tree.service.SolutionLinkService;
import com.opportunity.tree.service.dto.SolutionLinkDTO;
import com.opportunity.tree.service.mapper.SolutionLinkMapper;
import jakarta.persistence.EntityManager;
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
 * Integration tests for the {@link SolutionLinkResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class SolutionLinkResourceIT {

    private static final String DEFAULT_NAME = "AAAAAAAAAA";
    private static final String UPDATED_NAME = "BBBBBBBBBB";

    private static final String DEFAULT_URL = "https://{J%\"|";
    private static final String UPDATED_URL = "https://Z^";

    private static final LinkType DEFAULT_TYPE = LinkType.PROTOTYPE;
    private static final LinkType UPDATED_TYPE = LinkType.TICKET;

    private static final Integer DEFAULT_SORT_ORDER = 1;
    private static final Integer UPDATED_SORT_ORDER = 2;

    private static final String ENTITY_API_URL = "/api/solution-links";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2L * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private SolutionLinkRepository solutionLinkRepository;

    @Mock
    private SolutionLinkRepository solutionLinkRepositoryMock;

    @Autowired
    private SolutionLinkMapper solutionLinkMapper;

    @Mock
    private SolutionLinkService solutionLinkServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restSolutionLinkMockMvc;

    private SolutionLink solutionLink;

    private SolutionLink insertedSolutionLink;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static SolutionLink createEntity(EntityManager em) {
        SolutionLink solutionLink = new SolutionLink().name(DEFAULT_NAME).url(DEFAULT_URL).type(DEFAULT_TYPE).sortOrder(DEFAULT_SORT_ORDER);
        // Add required entity
        Solution solution;
        if (TestUtil.findAll(em, Solution.class).isEmpty()) {
            solution = SolutionResourceIT.createEntity(em);
            em.persist(solution);
            em.flush();
        } else {
            solution = TestUtil.findAll(em, Solution.class).get(0);
        }
        solutionLink.setSolution(solution);
        return solutionLink;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static SolutionLink createUpdatedEntity(EntityManager em) {
        SolutionLink updatedSolutionLink = new SolutionLink()
            .name(UPDATED_NAME)
            .url(UPDATED_URL)
            .type(UPDATED_TYPE)
            .sortOrder(UPDATED_SORT_ORDER);
        // Add required entity
        Solution solution;
        if (TestUtil.findAll(em, Solution.class).isEmpty()) {
            solution = SolutionResourceIT.createUpdatedEntity(em);
            em.persist(solution);
            em.flush();
        } else {
            solution = TestUtil.findAll(em, Solution.class).get(0);
        }
        updatedSolutionLink.setSolution(solution);
        return updatedSolutionLink;
    }

    @BeforeEach
    void initTest() {
        solutionLink = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedSolutionLink != null) {
            solutionLinkRepository.delete(insertedSolutionLink);
            insertedSolutionLink = null;
        }
    }

    @Test
    @Transactional
    void createSolutionLink() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the SolutionLink
        SolutionLinkDTO solutionLinkDTO = solutionLinkMapper.toDto(solutionLink);
        var returnedSolutionLinkDTO = om.readValue(
            restSolutionLinkMockMvc
                .perform(
                    post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(solutionLinkDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            SolutionLinkDTO.class
        );

        // Validate the SolutionLink in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedSolutionLink = solutionLinkMapper.toEntity(returnedSolutionLinkDTO);
        assertSolutionLinkUpdatableFieldsEquals(returnedSolutionLink, getPersistedSolutionLink(returnedSolutionLink));

        insertedSolutionLink = returnedSolutionLink;
    }

    @Test
    @Transactional
    void createSolutionLinkWithExistingId() throws Exception {
        // Create the SolutionLink with an existing ID
        solutionLink.setId(1L);
        SolutionLinkDTO solutionLinkDTO = solutionLinkMapper.toDto(solutionLink);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restSolutionLinkMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(solutionLinkDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the SolutionLink in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkNameIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        solutionLink.setName(null);

        // Create the SolutionLink, which fails.
        SolutionLinkDTO solutionLinkDTO = solutionLinkMapper.toDto(solutionLink);

        restSolutionLinkMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(solutionLinkDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkUrlIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        solutionLink.setUrl(null);

        // Create the SolutionLink, which fails.
        SolutionLinkDTO solutionLinkDTO = solutionLinkMapper.toDto(solutionLink);

        restSolutionLinkMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(solutionLinkDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkTypeIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        solutionLink.setType(null);

        // Create the SolutionLink, which fails.
        SolutionLinkDTO solutionLinkDTO = solutionLinkMapper.toDto(solutionLink);

        restSolutionLinkMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(solutionLinkDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkSortOrderIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        solutionLink.setSortOrder(null);

        // Create the SolutionLink, which fails.
        SolutionLinkDTO solutionLinkDTO = solutionLinkMapper.toDto(solutionLink);

        restSolutionLinkMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(solutionLinkDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllSolutionLinks() throws Exception {
        // Initialize the database
        insertedSolutionLink = solutionLinkRepository.saveAndFlush(solutionLink);

        // Get all the solutionLinkList
        restSolutionLinkMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(solutionLink.getId().intValue())))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME)))
            .andExpect(jsonPath("$.[*].url").value(hasItem(DEFAULT_URL)))
            .andExpect(jsonPath("$.[*].type").value(hasItem(DEFAULT_TYPE.toString())))
            .andExpect(jsonPath("$.[*].sortOrder").value(hasItem(DEFAULT_SORT_ORDER)));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllSolutionLinksWithEagerRelationshipsIsEnabled() throws Exception {
        when(solutionLinkServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restSolutionLinkMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(solutionLinkServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllSolutionLinksWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(solutionLinkServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restSolutionLinkMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(solutionLinkRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getSolutionLink() throws Exception {
        // Initialize the database
        insertedSolutionLink = solutionLinkRepository.saveAndFlush(solutionLink);

        // Get the solutionLink
        restSolutionLinkMockMvc
            .perform(get(ENTITY_API_URL_ID, solutionLink.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(solutionLink.getId().intValue()))
            .andExpect(jsonPath("$.name").value(DEFAULT_NAME))
            .andExpect(jsonPath("$.url").value(DEFAULT_URL))
            .andExpect(jsonPath("$.type").value(DEFAULT_TYPE.toString()))
            .andExpect(jsonPath("$.sortOrder").value(DEFAULT_SORT_ORDER));
    }

    @Test
    @Transactional
    void getNonExistingSolutionLink() throws Exception {
        // Get the solutionLink
        restSolutionLinkMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingSolutionLink() throws Exception {
        // Initialize the database
        insertedSolutionLink = solutionLinkRepository.saveAndFlush(solutionLink);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the solutionLink
        SolutionLink updatedSolutionLink = solutionLinkRepository.findById(solutionLink.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedSolutionLink are not directly saved in db
        em.detach(updatedSolutionLink);
        updatedSolutionLink.name(UPDATED_NAME).url(UPDATED_URL).type(UPDATED_TYPE).sortOrder(UPDATED_SORT_ORDER);
        SolutionLinkDTO solutionLinkDTO = solutionLinkMapper.toDto(updatedSolutionLink);

        restSolutionLinkMockMvc
            .perform(
                put(ENTITY_API_URL_ID, solutionLinkDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(solutionLinkDTO))
            )
            .andExpect(status().isOk());

        // Validate the SolutionLink in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedSolutionLinkToMatchAllProperties(updatedSolutionLink);
    }

    @Test
    @Transactional
    void putNonExistingSolutionLink() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        solutionLink.setId(longCount.incrementAndGet());

        // Create the SolutionLink
        SolutionLinkDTO solutionLinkDTO = solutionLinkMapper.toDto(solutionLink);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restSolutionLinkMockMvc
            .perform(
                put(ENTITY_API_URL_ID, solutionLinkDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(solutionLinkDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the SolutionLink in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchSolutionLink() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        solutionLink.setId(longCount.incrementAndGet());

        // Create the SolutionLink
        SolutionLinkDTO solutionLinkDTO = solutionLinkMapper.toDto(solutionLink);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restSolutionLinkMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(solutionLinkDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the SolutionLink in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamSolutionLink() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        solutionLink.setId(longCount.incrementAndGet());

        // Create the SolutionLink
        SolutionLinkDTO solutionLinkDTO = solutionLinkMapper.toDto(solutionLink);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restSolutionLinkMockMvc
            .perform(
                put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(solutionLinkDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the SolutionLink in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateSolutionLinkWithPatch() throws Exception {
        // Initialize the database
        insertedSolutionLink = solutionLinkRepository.saveAndFlush(solutionLink);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the solutionLink using partial update
        SolutionLink partialUpdatedSolutionLink = new SolutionLink();
        partialUpdatedSolutionLink.setId(solutionLink.getId());

        partialUpdatedSolutionLink.name(UPDATED_NAME);

        restSolutionLinkMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedSolutionLink.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedSolutionLink))
            )
            .andExpect(status().isOk());

        // Validate the SolutionLink in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertSolutionLinkUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedSolutionLink, solutionLink),
            getPersistedSolutionLink(solutionLink)
        );
    }

    @Test
    @Transactional
    void fullUpdateSolutionLinkWithPatch() throws Exception {
        // Initialize the database
        insertedSolutionLink = solutionLinkRepository.saveAndFlush(solutionLink);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the solutionLink using partial update
        SolutionLink partialUpdatedSolutionLink = new SolutionLink();
        partialUpdatedSolutionLink.setId(solutionLink.getId());

        partialUpdatedSolutionLink.name(UPDATED_NAME).url(UPDATED_URL).type(UPDATED_TYPE).sortOrder(UPDATED_SORT_ORDER);

        restSolutionLinkMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedSolutionLink.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedSolutionLink))
            )
            .andExpect(status().isOk());

        // Validate the SolutionLink in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertSolutionLinkUpdatableFieldsEquals(partialUpdatedSolutionLink, getPersistedSolutionLink(partialUpdatedSolutionLink));
    }

    @Test
    @Transactional
    void patchNonExistingSolutionLink() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        solutionLink.setId(longCount.incrementAndGet());

        // Create the SolutionLink
        SolutionLinkDTO solutionLinkDTO = solutionLinkMapper.toDto(solutionLink);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restSolutionLinkMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, solutionLinkDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(solutionLinkDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the SolutionLink in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchSolutionLink() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        solutionLink.setId(longCount.incrementAndGet());

        // Create the SolutionLink
        SolutionLinkDTO solutionLinkDTO = solutionLinkMapper.toDto(solutionLink);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restSolutionLinkMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(solutionLinkDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the SolutionLink in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamSolutionLink() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        solutionLink.setId(longCount.incrementAndGet());

        // Create the SolutionLink
        SolutionLinkDTO solutionLinkDTO = solutionLinkMapper.toDto(solutionLink);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restSolutionLinkMockMvc
            .perform(
                patch(ENTITY_API_URL)
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(solutionLinkDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the SolutionLink in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteSolutionLink() throws Exception {
        // Initialize the database
        insertedSolutionLink = solutionLinkRepository.saveAndFlush(solutionLink);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the solutionLink
        restSolutionLinkMockMvc
            .perform(delete(ENTITY_API_URL_ID, solutionLink.getId()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return solutionLinkRepository.count();
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

    protected SolutionLink getPersistedSolutionLink(SolutionLink solutionLink) {
        return solutionLinkRepository.findById(solutionLink.getId()).orElseThrow();
    }

    protected void assertPersistedSolutionLinkToMatchAllProperties(SolutionLink expectedSolutionLink) {
        assertSolutionLinkAllPropertiesEquals(expectedSolutionLink, getPersistedSolutionLink(expectedSolutionLink));
    }

    protected void assertPersistedSolutionLinkToMatchUpdatableProperties(SolutionLink expectedSolutionLink) {
        assertSolutionLinkAllUpdatablePropertiesEquals(expectedSolutionLink, getPersistedSolutionLink(expectedSolutionLink));
    }
}
