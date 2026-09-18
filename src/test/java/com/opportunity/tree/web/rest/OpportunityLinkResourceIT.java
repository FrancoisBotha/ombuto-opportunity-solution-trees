package com.opportunity.tree.web.rest;

import static com.opportunity.tree.domain.OpportunityLinkAsserts.*;
import static com.opportunity.tree.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opportunity.tree.IntegrationTest;
import com.opportunity.tree.domain.Opportunity;
import com.opportunity.tree.domain.OpportunityLink;
import com.opportunity.tree.domain.enumeration.LinkType;
import com.opportunity.tree.repository.OpportunityLinkRepository;
import com.opportunity.tree.service.OpportunityLinkService;
import com.opportunity.tree.service.dto.OpportunityLinkDTO;
import com.opportunity.tree.service.mapper.OpportunityLinkMapper;
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
 * Integration tests for the {@link OpportunityLinkResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = "ROLE_ADMIN")
class OpportunityLinkResourceIT {

    private static final String DEFAULT_NAME = "AAAAAAAAAA";
    private static final String UPDATED_NAME = "BBBBBBBBBB";

    private static final String DEFAULT_URL = "https://i%}S\" ";
    private static final String UPDATED_URL = "https://{A%";

    private static final LinkType DEFAULT_TYPE = LinkType.PROTOTYPE;
    private static final LinkType UPDATED_TYPE = LinkType.TICKET;

    private static final Integer DEFAULT_SORT_ORDER = 1;
    private static final Integer UPDATED_SORT_ORDER = 2;

    private static final String ENTITY_API_URL = "/api/opportunity-links";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2L * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private OpportunityLinkRepository opportunityLinkRepository;

    @Mock
    private OpportunityLinkRepository opportunityLinkRepositoryMock;

    @Autowired
    private OpportunityLinkMapper opportunityLinkMapper;

    @Mock
    private OpportunityLinkService opportunityLinkServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restOpportunityLinkMockMvc;

    private OpportunityLink opportunityLink;

    private OpportunityLink insertedOpportunityLink;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static OpportunityLink createEntity(EntityManager em) {
        OpportunityLink opportunityLink = new OpportunityLink()
            .name(DEFAULT_NAME)
            .url(DEFAULT_URL)
            .type(DEFAULT_TYPE)
            .sortOrder(DEFAULT_SORT_ORDER);
        // Add required entity
        Opportunity opportunity;
        if (TestUtil.findAll(em, Opportunity.class).isEmpty()) {
            opportunity = OpportunityResourceIT.createEntity(em);
            em.persist(opportunity);
            em.flush();
        } else {
            opportunity = TestUtil.findAll(em, Opportunity.class).get(0);
        }
        opportunityLink.setOpportunity(opportunity);
        return opportunityLink;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static OpportunityLink createUpdatedEntity(EntityManager em) {
        OpportunityLink updatedOpportunityLink = new OpportunityLink()
            .name(UPDATED_NAME)
            .url(UPDATED_URL)
            .type(UPDATED_TYPE)
            .sortOrder(UPDATED_SORT_ORDER);
        // Add required entity
        Opportunity opportunity;
        if (TestUtil.findAll(em, Opportunity.class).isEmpty()) {
            opportunity = OpportunityResourceIT.createUpdatedEntity(em);
            em.persist(opportunity);
            em.flush();
        } else {
            opportunity = TestUtil.findAll(em, Opportunity.class).get(0);
        }
        updatedOpportunityLink.setOpportunity(opportunity);
        return updatedOpportunityLink;
    }

    @BeforeEach
    void initTest() {
        opportunityLink = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedOpportunityLink != null) {
            opportunityLinkRepository.delete(insertedOpportunityLink);
            insertedOpportunityLink = null;
        }
    }

    @Test
    @Transactional
    void createOpportunityLink() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the OpportunityLink
        OpportunityLinkDTO opportunityLinkDTO = opportunityLinkMapper.toDto(opportunityLink);
        var returnedOpportunityLinkDTO = om.readValue(
            restOpportunityLinkMockMvc
                .perform(
                    post(ENTITY_API_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsBytes(opportunityLinkDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            OpportunityLinkDTO.class
        );

        // Validate the OpportunityLink in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedOpportunityLink = opportunityLinkMapper.toEntity(returnedOpportunityLinkDTO);
        assertOpportunityLinkUpdatableFieldsEquals(returnedOpportunityLink, getPersistedOpportunityLink(returnedOpportunityLink));

        insertedOpportunityLink = returnedOpportunityLink;
    }

    @Test
    @Transactional
    void createOpportunityLinkWithExistingId() throws Exception {
        // Create the OpportunityLink with an existing ID
        opportunityLink.setId(1L);
        OpportunityLinkDTO opportunityLinkDTO = opportunityLinkMapper.toDto(opportunityLink);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restOpportunityLinkMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(opportunityLinkDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the OpportunityLink in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkNameIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        opportunityLink.setName(null);

        // Create the OpportunityLink, which fails.
        OpportunityLinkDTO opportunityLinkDTO = opportunityLinkMapper.toDto(opportunityLink);

        restOpportunityLinkMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(opportunityLinkDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkUrlIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        opportunityLink.setUrl(null);

        // Create the OpportunityLink, which fails.
        OpportunityLinkDTO opportunityLinkDTO = opportunityLinkMapper.toDto(opportunityLink);

        restOpportunityLinkMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(opportunityLinkDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkTypeIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        opportunityLink.setType(null);

        // Create the OpportunityLink, which fails.
        OpportunityLinkDTO opportunityLinkDTO = opportunityLinkMapper.toDto(opportunityLink);

        restOpportunityLinkMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(opportunityLinkDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkSortOrderIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        opportunityLink.setSortOrder(null);

        // Create the OpportunityLink, which fails.
        OpportunityLinkDTO opportunityLinkDTO = opportunityLinkMapper.toDto(opportunityLink);

        restOpportunityLinkMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(opportunityLinkDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllOpportunityLinks() throws Exception {
        // Initialize the database
        insertedOpportunityLink = opportunityLinkRepository.saveAndFlush(opportunityLink);

        // Get all the opportunityLinkList
        restOpportunityLinkMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(opportunityLink.getId().intValue())))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME)))
            .andExpect(jsonPath("$.[*].url").value(hasItem(DEFAULT_URL)))
            .andExpect(jsonPath("$.[*].type").value(hasItem(DEFAULT_TYPE.toString())))
            .andExpect(jsonPath("$.[*].sortOrder").value(hasItem(DEFAULT_SORT_ORDER)));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllOpportunityLinksWithEagerRelationshipsIsEnabled() throws Exception {
        when(opportunityLinkServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restOpportunityLinkMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(opportunityLinkServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllOpportunityLinksWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(opportunityLinkServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restOpportunityLinkMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(opportunityLinkRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getOpportunityLink() throws Exception {
        // Initialize the database
        insertedOpportunityLink = opportunityLinkRepository.saveAndFlush(opportunityLink);

        // Get the opportunityLink
        restOpportunityLinkMockMvc
            .perform(get(ENTITY_API_URL_ID, opportunityLink.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(opportunityLink.getId().intValue()))
            .andExpect(jsonPath("$.name").value(DEFAULT_NAME))
            .andExpect(jsonPath("$.url").value(DEFAULT_URL))
            .andExpect(jsonPath("$.type").value(DEFAULT_TYPE.toString()))
            .andExpect(jsonPath("$.sortOrder").value(DEFAULT_SORT_ORDER));
    }

    @Test
    @Transactional
    void getNonExistingOpportunityLink() throws Exception {
        // Get the opportunityLink
        restOpportunityLinkMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingOpportunityLink() throws Exception {
        // Initialize the database
        insertedOpportunityLink = opportunityLinkRepository.saveAndFlush(opportunityLink);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the opportunityLink
        OpportunityLink updatedOpportunityLink = opportunityLinkRepository.findById(opportunityLink.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedOpportunityLink are not directly saved in db
        em.detach(updatedOpportunityLink);
        updatedOpportunityLink.name(UPDATED_NAME).url(UPDATED_URL).type(UPDATED_TYPE).sortOrder(UPDATED_SORT_ORDER);
        OpportunityLinkDTO opportunityLinkDTO = opportunityLinkMapper.toDto(updatedOpportunityLink);

        restOpportunityLinkMockMvc
            .perform(
                put(ENTITY_API_URL_ID, opportunityLinkDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(opportunityLinkDTO))
            )
            .andExpect(status().isOk());

        // Validate the OpportunityLink in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedOpportunityLinkToMatchAllProperties(updatedOpportunityLink);
    }

    @Test
    @Transactional
    void putNonExistingOpportunityLink() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        opportunityLink.setId(longCount.incrementAndGet());

        // Create the OpportunityLink
        OpportunityLinkDTO opportunityLinkDTO = opportunityLinkMapper.toDto(opportunityLink);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restOpportunityLinkMockMvc
            .perform(
                put(ENTITY_API_URL_ID, opportunityLinkDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(opportunityLinkDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the OpportunityLink in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchOpportunityLink() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        opportunityLink.setId(longCount.incrementAndGet());

        // Create the OpportunityLink
        OpportunityLinkDTO opportunityLinkDTO = opportunityLinkMapper.toDto(opportunityLink);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restOpportunityLinkMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(opportunityLinkDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the OpportunityLink in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamOpportunityLink() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        opportunityLink.setId(longCount.incrementAndGet());

        // Create the OpportunityLink
        OpportunityLinkDTO opportunityLinkDTO = opportunityLinkMapper.toDto(opportunityLink);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restOpportunityLinkMockMvc
            .perform(
                put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(opportunityLinkDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the OpportunityLink in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateOpportunityLinkWithPatch() throws Exception {
        // Initialize the database
        insertedOpportunityLink = opportunityLinkRepository.saveAndFlush(opportunityLink);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the opportunityLink using partial update
        OpportunityLink partialUpdatedOpportunityLink = new OpportunityLink();
        partialUpdatedOpportunityLink.setId(opportunityLink.getId());

        partialUpdatedOpportunityLink.name(UPDATED_NAME).sortOrder(UPDATED_SORT_ORDER);

        restOpportunityLinkMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedOpportunityLink.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedOpportunityLink))
            )
            .andExpect(status().isOk());

        // Validate the OpportunityLink in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertOpportunityLinkUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedOpportunityLink, opportunityLink),
            getPersistedOpportunityLink(opportunityLink)
        );
    }

    @Test
    @Transactional
    void fullUpdateOpportunityLinkWithPatch() throws Exception {
        // Initialize the database
        insertedOpportunityLink = opportunityLinkRepository.saveAndFlush(opportunityLink);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the opportunityLink using partial update
        OpportunityLink partialUpdatedOpportunityLink = new OpportunityLink();
        partialUpdatedOpportunityLink.setId(opportunityLink.getId());

        partialUpdatedOpportunityLink.name(UPDATED_NAME).url(UPDATED_URL).type(UPDATED_TYPE).sortOrder(UPDATED_SORT_ORDER);

        restOpportunityLinkMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedOpportunityLink.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedOpportunityLink))
            )
            .andExpect(status().isOk());

        // Validate the OpportunityLink in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertOpportunityLinkUpdatableFieldsEquals(
            partialUpdatedOpportunityLink,
            getPersistedOpportunityLink(partialUpdatedOpportunityLink)
        );
    }

    @Test
    @Transactional
    void patchNonExistingOpportunityLink() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        opportunityLink.setId(longCount.incrementAndGet());

        // Create the OpportunityLink
        OpportunityLinkDTO opportunityLinkDTO = opportunityLinkMapper.toDto(opportunityLink);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restOpportunityLinkMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, opportunityLinkDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(opportunityLinkDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the OpportunityLink in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchOpportunityLink() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        opportunityLink.setId(longCount.incrementAndGet());

        // Create the OpportunityLink
        OpportunityLinkDTO opportunityLinkDTO = opportunityLinkMapper.toDto(opportunityLink);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restOpportunityLinkMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(opportunityLinkDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the OpportunityLink in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamOpportunityLink() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        opportunityLink.setId(longCount.incrementAndGet());

        // Create the OpportunityLink
        OpportunityLinkDTO opportunityLinkDTO = opportunityLinkMapper.toDto(opportunityLink);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restOpportunityLinkMockMvc
            .perform(
                patch(ENTITY_API_URL)
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(opportunityLinkDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the OpportunityLink in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteOpportunityLink() throws Exception {
        // Initialize the database
        insertedOpportunityLink = opportunityLinkRepository.saveAndFlush(opportunityLink);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the opportunityLink
        restOpportunityLinkMockMvc
            .perform(delete(ENTITY_API_URL_ID, opportunityLink.getId()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return opportunityLinkRepository.count();
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

    protected OpportunityLink getPersistedOpportunityLink(OpportunityLink opportunityLink) {
        return opportunityLinkRepository.findById(opportunityLink.getId()).orElseThrow();
    }

    protected void assertPersistedOpportunityLinkToMatchAllProperties(OpportunityLink expectedOpportunityLink) {
        assertOpportunityLinkAllPropertiesEquals(expectedOpportunityLink, getPersistedOpportunityLink(expectedOpportunityLink));
    }

    protected void assertPersistedOpportunityLinkToMatchUpdatableProperties(OpportunityLink expectedOpportunityLink) {
        assertOpportunityLinkAllUpdatablePropertiesEquals(expectedOpportunityLink, getPersistedOpportunityLink(expectedOpportunityLink));
    }
}
