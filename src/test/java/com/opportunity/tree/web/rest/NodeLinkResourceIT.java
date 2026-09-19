package com.opportunity.tree.web.rest;

import static com.opportunity.tree.domain.NodeLinkAsserts.*;
import static com.opportunity.tree.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opportunity.tree.IntegrationTest;
import com.opportunity.tree.domain.NodeLink;
import com.opportunity.tree.repository.NodeLinkRepository;
import com.opportunity.tree.service.NodeLinkService;
import com.opportunity.tree.service.dto.NodeLinkDTO;
import com.opportunity.tree.service.mapper.NodeLinkMapper;
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
 * Integration tests for the {@link NodeLinkResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = "ROLE_ADMIN")
class NodeLinkResourceIT {

    private static final String DEFAULT_NAME = "AAAAAAAAAA";
    private static final String UPDATED_NAME = "BBBBBBBBBB";

    private static final String DEFAULT_URL = "https://k'";
    private static final String UPDATED_URL = "https://*I 5";

    private static final Integer DEFAULT_SORT_ORDER = 1;
    private static final Integer UPDATED_SORT_ORDER = 2;

    private static final Instant DEFAULT_CREATED_DATE = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CREATED_DATE = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/node-links";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2L * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private NodeLinkRepository nodeLinkRepository;

    @Mock
    private NodeLinkRepository nodeLinkRepositoryMock;

    @Autowired
    private NodeLinkMapper nodeLinkMapper;

    @Mock
    private NodeLinkService nodeLinkServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restNodeLinkMockMvc;

    private NodeLink nodeLink;

    private NodeLink insertedNodeLink;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static NodeLink createEntity() {
        return new NodeLink().name(DEFAULT_NAME).url(DEFAULT_URL).sortOrder(DEFAULT_SORT_ORDER).createdDate(DEFAULT_CREATED_DATE);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static NodeLink createUpdatedEntity() {
        return new NodeLink().name(UPDATED_NAME).url(UPDATED_URL).sortOrder(UPDATED_SORT_ORDER).createdDate(UPDATED_CREATED_DATE);
    }

    @BeforeEach
    void initTest() {
        nodeLink = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedNodeLink != null) {
            nodeLinkRepository.delete(insertedNodeLink);
            insertedNodeLink = null;
        }
    }

    @Test
    @Transactional
    void createNodeLink() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the NodeLink
        NodeLinkDTO nodeLinkDTO = nodeLinkMapper.toDto(nodeLink);
        var returnedNodeLinkDTO = om.readValue(
            restNodeLinkMockMvc
                .perform(
                    post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(nodeLinkDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            NodeLinkDTO.class
        );

        // Validate the NodeLink in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedNodeLink = nodeLinkMapper.toEntity(returnedNodeLinkDTO);
        assertNodeLinkUpdatableFieldsEquals(returnedNodeLink, getPersistedNodeLink(returnedNodeLink));

        insertedNodeLink = returnedNodeLink;
    }

    @Test
    @Transactional
    void createNodeLinkWithExistingId() throws Exception {
        // Create the NodeLink with an existing ID
        nodeLink.setId(1L);
        NodeLinkDTO nodeLinkDTO = nodeLinkMapper.toDto(nodeLink);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restNodeLinkMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(nodeLinkDTO)))
            .andExpect(status().isBadRequest());

        // Validate the NodeLink in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkNameIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        nodeLink.setName(null);

        // Create the NodeLink, which fails.
        NodeLinkDTO nodeLinkDTO = nodeLinkMapper.toDto(nodeLink);

        restNodeLinkMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(nodeLinkDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkUrlIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        nodeLink.setUrl(null);

        // Create the NodeLink, which fails.
        NodeLinkDTO nodeLinkDTO = nodeLinkMapper.toDto(nodeLink);

        restNodeLinkMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(nodeLinkDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkSortOrderIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        nodeLink.setSortOrder(null);

        // Create the NodeLink, which fails.
        NodeLinkDTO nodeLinkDTO = nodeLinkMapper.toDto(nodeLink);

        restNodeLinkMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(nodeLinkDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkCreatedDateIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        nodeLink.setCreatedDate(null);

        // Create the NodeLink, which fails.
        NodeLinkDTO nodeLinkDTO = nodeLinkMapper.toDto(nodeLink);

        restNodeLinkMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(nodeLinkDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllNodeLinks() throws Exception {
        // Initialize the database
        insertedNodeLink = nodeLinkRepository.saveAndFlush(nodeLink);

        // Get all the nodeLinkList
        restNodeLinkMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(nodeLink.getId().intValue())))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME)))
            .andExpect(jsonPath("$.[*].url").value(hasItem(DEFAULT_URL)))
            .andExpect(jsonPath("$.[*].sortOrder").value(hasItem(DEFAULT_SORT_ORDER)))
            .andExpect(jsonPath("$.[*].createdDate").value(hasItem(DEFAULT_CREATED_DATE.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllNodeLinksWithEagerRelationshipsIsEnabled() throws Exception {
        when(nodeLinkServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restNodeLinkMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(nodeLinkServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllNodeLinksWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(nodeLinkServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restNodeLinkMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(nodeLinkRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getNodeLink() throws Exception {
        // Initialize the database
        insertedNodeLink = nodeLinkRepository.saveAndFlush(nodeLink);

        // Get the nodeLink
        restNodeLinkMockMvc
            .perform(get(ENTITY_API_URL_ID, nodeLink.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(nodeLink.getId().intValue()))
            .andExpect(jsonPath("$.name").value(DEFAULT_NAME))
            .andExpect(jsonPath("$.url").value(DEFAULT_URL))
            .andExpect(jsonPath("$.sortOrder").value(DEFAULT_SORT_ORDER))
            .andExpect(jsonPath("$.createdDate").value(DEFAULT_CREATED_DATE.toString()));
    }

    @Test
    @Transactional
    void getNonExistingNodeLink() throws Exception {
        // Get the nodeLink
        restNodeLinkMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingNodeLink() throws Exception {
        // Initialize the database
        insertedNodeLink = nodeLinkRepository.saveAndFlush(nodeLink);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the nodeLink
        NodeLink updatedNodeLink = nodeLinkRepository.findById(nodeLink.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedNodeLink are not directly saved in db
        em.detach(updatedNodeLink);
        updatedNodeLink.name(UPDATED_NAME).url(UPDATED_URL).sortOrder(UPDATED_SORT_ORDER).createdDate(UPDATED_CREATED_DATE);
        NodeLinkDTO nodeLinkDTO = nodeLinkMapper.toDto(updatedNodeLink);

        restNodeLinkMockMvc
            .perform(
                put(ENTITY_API_URL_ID, nodeLinkDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(nodeLinkDTO))
            )
            .andExpect(status().isOk());

        // Validate the NodeLink in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedNodeLinkToMatchAllProperties(updatedNodeLink);
    }

    @Test
    @Transactional
    void putNonExistingNodeLink() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        nodeLink.setId(longCount.incrementAndGet());

        // Create the NodeLink
        NodeLinkDTO nodeLinkDTO = nodeLinkMapper.toDto(nodeLink);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restNodeLinkMockMvc
            .perform(
                put(ENTITY_API_URL_ID, nodeLinkDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(nodeLinkDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the NodeLink in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchNodeLink() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        nodeLink.setId(longCount.incrementAndGet());

        // Create the NodeLink
        NodeLinkDTO nodeLinkDTO = nodeLinkMapper.toDto(nodeLink);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restNodeLinkMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(nodeLinkDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the NodeLink in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamNodeLink() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        nodeLink.setId(longCount.incrementAndGet());

        // Create the NodeLink
        NodeLinkDTO nodeLinkDTO = nodeLinkMapper.toDto(nodeLink);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restNodeLinkMockMvc
            .perform(put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(nodeLinkDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the NodeLink in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateNodeLinkWithPatch() throws Exception {
        // Initialize the database
        insertedNodeLink = nodeLinkRepository.saveAndFlush(nodeLink);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the nodeLink using partial update
        NodeLink partialUpdatedNodeLink = new NodeLink();
        partialUpdatedNodeLink.setId(nodeLink.getId());

        partialUpdatedNodeLink.name(UPDATED_NAME).url(UPDATED_URL).createdDate(UPDATED_CREATED_DATE);

        restNodeLinkMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedNodeLink.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedNodeLink))
            )
            .andExpect(status().isOk());

        // Validate the NodeLink in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertNodeLinkUpdatableFieldsEquals(createUpdateProxyForBean(partialUpdatedNodeLink, nodeLink), getPersistedNodeLink(nodeLink));
    }

    @Test
    @Transactional
    void fullUpdateNodeLinkWithPatch() throws Exception {
        // Initialize the database
        insertedNodeLink = nodeLinkRepository.saveAndFlush(nodeLink);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the nodeLink using partial update
        NodeLink partialUpdatedNodeLink = new NodeLink();
        partialUpdatedNodeLink.setId(nodeLink.getId());

        partialUpdatedNodeLink.name(UPDATED_NAME).url(UPDATED_URL).sortOrder(UPDATED_SORT_ORDER).createdDate(UPDATED_CREATED_DATE);

        restNodeLinkMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedNodeLink.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedNodeLink))
            )
            .andExpect(status().isOk());

        // Validate the NodeLink in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertNodeLinkUpdatableFieldsEquals(partialUpdatedNodeLink, getPersistedNodeLink(partialUpdatedNodeLink));
    }

    @Test
    @Transactional
    void patchNonExistingNodeLink() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        nodeLink.setId(longCount.incrementAndGet());

        // Create the NodeLink
        NodeLinkDTO nodeLinkDTO = nodeLinkMapper.toDto(nodeLink);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restNodeLinkMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, nodeLinkDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(nodeLinkDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the NodeLink in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchNodeLink() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        nodeLink.setId(longCount.incrementAndGet());

        // Create the NodeLink
        NodeLinkDTO nodeLinkDTO = nodeLinkMapper.toDto(nodeLink);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restNodeLinkMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(nodeLinkDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the NodeLink in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamNodeLink() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        nodeLink.setId(longCount.incrementAndGet());

        // Create the NodeLink
        NodeLinkDTO nodeLinkDTO = nodeLinkMapper.toDto(nodeLink);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restNodeLinkMockMvc
            .perform(
                patch(ENTITY_API_URL).with(csrf()).contentType("application/merge-patch+json").content(om.writeValueAsBytes(nodeLinkDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the NodeLink in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteNodeLink() throws Exception {
        // Initialize the database
        insertedNodeLink = nodeLinkRepository.saveAndFlush(nodeLink);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the nodeLink
        restNodeLinkMockMvc
            .perform(delete(ENTITY_API_URL_ID, nodeLink.getId()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return nodeLinkRepository.count();
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

    protected NodeLink getPersistedNodeLink(NodeLink nodeLink) {
        return nodeLinkRepository.findById(nodeLink.getId()).orElseThrow();
    }

    protected void assertPersistedNodeLinkToMatchAllProperties(NodeLink expectedNodeLink) {
        assertNodeLinkAllPropertiesEquals(expectedNodeLink, getPersistedNodeLink(expectedNodeLink));
    }

    protected void assertPersistedNodeLinkToMatchUpdatableProperties(NodeLink expectedNodeLink) {
        assertNodeLinkAllUpdatablePropertiesEquals(expectedNodeLink, getPersistedNodeLink(expectedNodeLink));
    }
}
