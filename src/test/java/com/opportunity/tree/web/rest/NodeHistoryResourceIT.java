package com.opportunity.tree.web.rest;

import static com.opportunity.tree.domain.NodeHistoryAsserts.*;
import static com.opportunity.tree.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opportunity.tree.IntegrationTest;
import com.opportunity.tree.domain.NodeHistory;
import com.opportunity.tree.domain.enumeration.HistoryEventType;
import com.opportunity.tree.domain.enumeration.TreeNodeType;
import com.opportunity.tree.repository.NodeHistoryRepository;
import com.opportunity.tree.repository.UserRepository;
import com.opportunity.tree.service.NodeHistoryService;
import com.opportunity.tree.service.dto.NodeHistoryDTO;
import com.opportunity.tree.service.mapper.NodeHistoryMapper;
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
 * Integration tests for the {@link NodeHistoryResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = "ROLE_ADMIN")
class NodeHistoryResourceIT {

    private static final TreeNodeType DEFAULT_NODE_TYPE = TreeNodeType.PRODUCT;
    private static final TreeNodeType UPDATED_NODE_TYPE = TreeNodeType.OUTCOME;

    private static final Long DEFAULT_NODE_ID = 1L;
    private static final Long UPDATED_NODE_ID = 2L;

    private static final HistoryEventType DEFAULT_EVENT_TYPE = HistoryEventType.CREATED;
    private static final HistoryEventType UPDATED_EVENT_TYPE = HistoryEventType.STATUS_CHANGED;

    private static final String DEFAULT_SUMMARY = "AAAAAAAAAA";
    private static final String UPDATED_SUMMARY = "BBBBBBBBBB";

    private static final Instant DEFAULT_CREATED_DATE = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CREATED_DATE = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/node-histories";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2L * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private NodeHistoryRepository nodeHistoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Mock
    private NodeHistoryRepository nodeHistoryRepositoryMock;

    @Autowired
    private NodeHistoryMapper nodeHistoryMapper;

    @Mock
    private NodeHistoryService nodeHistoryServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restNodeHistoryMockMvc;

    private NodeHistory nodeHistory;

    private NodeHistory insertedNodeHistory;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static NodeHistory createEntity() {
        return new NodeHistory()
            .nodeType(DEFAULT_NODE_TYPE)
            .nodeId(DEFAULT_NODE_ID)
            .eventType(DEFAULT_EVENT_TYPE)
            .summary(DEFAULT_SUMMARY)
            .createdDate(DEFAULT_CREATED_DATE);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static NodeHistory createUpdatedEntity() {
        return new NodeHistory()
            .nodeType(UPDATED_NODE_TYPE)
            .nodeId(UPDATED_NODE_ID)
            .eventType(UPDATED_EVENT_TYPE)
            .summary(UPDATED_SUMMARY)
            .createdDate(UPDATED_CREATED_DATE);
    }

    @BeforeEach
    void initTest() {
        nodeHistory = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedNodeHistory != null) {
            nodeHistoryRepository.delete(insertedNodeHistory);
            insertedNodeHistory = null;
        }
        userRepository.deleteAll();
    }

    @Test
    @Transactional
    void createNodeHistory() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the NodeHistory
        NodeHistoryDTO nodeHistoryDTO = nodeHistoryMapper.toDto(nodeHistory);
        var returnedNodeHistoryDTO = om.readValue(
            restNodeHistoryMockMvc
                .perform(
                    post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(nodeHistoryDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            NodeHistoryDTO.class
        );

        // Validate the NodeHistory in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedNodeHistory = nodeHistoryMapper.toEntity(returnedNodeHistoryDTO);
        assertNodeHistoryUpdatableFieldsEquals(returnedNodeHistory, getPersistedNodeHistory(returnedNodeHistory));

        insertedNodeHistory = returnedNodeHistory;
    }

    @Test
    @Transactional
    void createNodeHistoryWithExistingId() throws Exception {
        // Create the NodeHistory with an existing ID
        nodeHistory.setId(1L);
        NodeHistoryDTO nodeHistoryDTO = nodeHistoryMapper.toDto(nodeHistory);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restNodeHistoryMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(nodeHistoryDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the NodeHistory in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkNodeTypeIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        nodeHistory.setNodeType(null);

        // Create the NodeHistory, which fails.
        NodeHistoryDTO nodeHistoryDTO = nodeHistoryMapper.toDto(nodeHistory);

        restNodeHistoryMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(nodeHistoryDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkNodeIdIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        nodeHistory.setNodeId(null);

        // Create the NodeHistory, which fails.
        NodeHistoryDTO nodeHistoryDTO = nodeHistoryMapper.toDto(nodeHistory);

        restNodeHistoryMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(nodeHistoryDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkEventTypeIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        nodeHistory.setEventType(null);

        // Create the NodeHistory, which fails.
        NodeHistoryDTO nodeHistoryDTO = nodeHistoryMapper.toDto(nodeHistory);

        restNodeHistoryMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(nodeHistoryDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkSummaryIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        nodeHistory.setSummary(null);

        // Create the NodeHistory, which fails.
        NodeHistoryDTO nodeHistoryDTO = nodeHistoryMapper.toDto(nodeHistory);

        restNodeHistoryMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(nodeHistoryDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkCreatedDateIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        nodeHistory.setCreatedDate(null);

        // Create the NodeHistory, which fails.
        NodeHistoryDTO nodeHistoryDTO = nodeHistoryMapper.toDto(nodeHistory);

        restNodeHistoryMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(nodeHistoryDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllNodeHistories() throws Exception {
        // Initialize the database
        insertedNodeHistory = nodeHistoryRepository.saveAndFlush(nodeHistory);

        // Get all the nodeHistoryList
        restNodeHistoryMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(nodeHistory.getId().intValue())))
            .andExpect(jsonPath("$.[*].nodeType").value(hasItem(DEFAULT_NODE_TYPE.toString())))
            .andExpect(jsonPath("$.[*].nodeId").value(hasItem(DEFAULT_NODE_ID.intValue())))
            .andExpect(jsonPath("$.[*].eventType").value(hasItem(DEFAULT_EVENT_TYPE.toString())))
            .andExpect(jsonPath("$.[*].summary").value(hasItem(DEFAULT_SUMMARY)))
            .andExpect(jsonPath("$.[*].createdDate").value(hasItem(DEFAULT_CREATED_DATE.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllNodeHistoriesWithEagerRelationshipsIsEnabled() throws Exception {
        when(nodeHistoryServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restNodeHistoryMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(nodeHistoryServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllNodeHistoriesWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(nodeHistoryServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restNodeHistoryMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(nodeHistoryRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getNodeHistory() throws Exception {
        // Initialize the database
        insertedNodeHistory = nodeHistoryRepository.saveAndFlush(nodeHistory);

        // Get the nodeHistory
        restNodeHistoryMockMvc
            .perform(get(ENTITY_API_URL_ID, nodeHistory.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(nodeHistory.getId().intValue()))
            .andExpect(jsonPath("$.nodeType").value(DEFAULT_NODE_TYPE.toString()))
            .andExpect(jsonPath("$.nodeId").value(DEFAULT_NODE_ID.intValue()))
            .andExpect(jsonPath("$.eventType").value(DEFAULT_EVENT_TYPE.toString()))
            .andExpect(jsonPath("$.summary").value(DEFAULT_SUMMARY))
            .andExpect(jsonPath("$.createdDate").value(DEFAULT_CREATED_DATE.toString()));
    }

    @Test
    @Transactional
    void getNonExistingNodeHistory() throws Exception {
        // Get the nodeHistory
        restNodeHistoryMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingNodeHistory() throws Exception {
        // Initialize the database
        insertedNodeHistory = nodeHistoryRepository.saveAndFlush(nodeHistory);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the nodeHistory
        NodeHistory updatedNodeHistory = nodeHistoryRepository.findById(nodeHistory.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedNodeHistory are not directly saved in db
        em.detach(updatedNodeHistory);
        updatedNodeHistory
            .nodeType(UPDATED_NODE_TYPE)
            .nodeId(UPDATED_NODE_ID)
            .eventType(UPDATED_EVENT_TYPE)
            .summary(UPDATED_SUMMARY)
            .createdDate(UPDATED_CREATED_DATE);
        NodeHistoryDTO nodeHistoryDTO = nodeHistoryMapper.toDto(updatedNodeHistory);

        restNodeHistoryMockMvc
            .perform(
                put(ENTITY_API_URL_ID, nodeHistoryDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(nodeHistoryDTO))
            )
            .andExpect(status().isOk());

        // Validate the NodeHistory in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedNodeHistoryToMatchAllProperties(updatedNodeHistory);
    }

    @Test
    @Transactional
    void putNonExistingNodeHistory() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        nodeHistory.setId(longCount.incrementAndGet());

        // Create the NodeHistory
        NodeHistoryDTO nodeHistoryDTO = nodeHistoryMapper.toDto(nodeHistory);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restNodeHistoryMockMvc
            .perform(
                put(ENTITY_API_URL_ID, nodeHistoryDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(nodeHistoryDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the NodeHistory in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchNodeHistory() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        nodeHistory.setId(longCount.incrementAndGet());

        // Create the NodeHistory
        NodeHistoryDTO nodeHistoryDTO = nodeHistoryMapper.toDto(nodeHistory);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restNodeHistoryMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(nodeHistoryDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the NodeHistory in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamNodeHistory() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        nodeHistory.setId(longCount.incrementAndGet());

        // Create the NodeHistory
        NodeHistoryDTO nodeHistoryDTO = nodeHistoryMapper.toDto(nodeHistory);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restNodeHistoryMockMvc
            .perform(put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(nodeHistoryDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the NodeHistory in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateNodeHistoryWithPatch() throws Exception {
        // Initialize the database
        insertedNodeHistory = nodeHistoryRepository.saveAndFlush(nodeHistory);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the nodeHistory using partial update
        NodeHistory partialUpdatedNodeHistory = new NodeHistory();
        partialUpdatedNodeHistory.setId(nodeHistory.getId());

        partialUpdatedNodeHistory.nodeId(UPDATED_NODE_ID).summary(UPDATED_SUMMARY);

        restNodeHistoryMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedNodeHistory.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedNodeHistory))
            )
            .andExpect(status().isOk());

        // Validate the NodeHistory in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertNodeHistoryUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedNodeHistory, nodeHistory),
            getPersistedNodeHistory(nodeHistory)
        );
    }

    @Test
    @Transactional
    void fullUpdateNodeHistoryWithPatch() throws Exception {
        // Initialize the database
        insertedNodeHistory = nodeHistoryRepository.saveAndFlush(nodeHistory);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the nodeHistory using partial update
        NodeHistory partialUpdatedNodeHistory = new NodeHistory();
        partialUpdatedNodeHistory.setId(nodeHistory.getId());

        partialUpdatedNodeHistory
            .nodeType(UPDATED_NODE_TYPE)
            .nodeId(UPDATED_NODE_ID)
            .eventType(UPDATED_EVENT_TYPE)
            .summary(UPDATED_SUMMARY)
            .createdDate(UPDATED_CREATED_DATE);

        restNodeHistoryMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedNodeHistory.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedNodeHistory))
            )
            .andExpect(status().isOk());

        // Validate the NodeHistory in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertNodeHistoryUpdatableFieldsEquals(partialUpdatedNodeHistory, getPersistedNodeHistory(partialUpdatedNodeHistory));
    }

    @Test
    @Transactional
    void patchNonExistingNodeHistory() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        nodeHistory.setId(longCount.incrementAndGet());

        // Create the NodeHistory
        NodeHistoryDTO nodeHistoryDTO = nodeHistoryMapper.toDto(nodeHistory);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restNodeHistoryMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, nodeHistoryDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(nodeHistoryDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the NodeHistory in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchNodeHistory() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        nodeHistory.setId(longCount.incrementAndGet());

        // Create the NodeHistory
        NodeHistoryDTO nodeHistoryDTO = nodeHistoryMapper.toDto(nodeHistory);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restNodeHistoryMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(nodeHistoryDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the NodeHistory in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamNodeHistory() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        nodeHistory.setId(longCount.incrementAndGet());

        // Create the NodeHistory
        NodeHistoryDTO nodeHistoryDTO = nodeHistoryMapper.toDto(nodeHistory);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restNodeHistoryMockMvc
            .perform(
                patch(ENTITY_API_URL).with(csrf()).contentType("application/merge-patch+json").content(om.writeValueAsBytes(nodeHistoryDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the NodeHistory in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteNodeHistory() throws Exception {
        // Initialize the database
        insertedNodeHistory = nodeHistoryRepository.saveAndFlush(nodeHistory);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the nodeHistory
        restNodeHistoryMockMvc
            .perform(delete(ENTITY_API_URL_ID, nodeHistory.getId()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return nodeHistoryRepository.count();
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

    protected NodeHistory getPersistedNodeHistory(NodeHistory nodeHistory) {
        return nodeHistoryRepository.findById(nodeHistory.getId()).orElseThrow();
    }

    protected void assertPersistedNodeHistoryToMatchAllProperties(NodeHistory expectedNodeHistory) {
        assertNodeHistoryAllPropertiesEquals(expectedNodeHistory, getPersistedNodeHistory(expectedNodeHistory));
    }

    protected void assertPersistedNodeHistoryToMatchUpdatableProperties(NodeHistory expectedNodeHistory) {
        assertNodeHistoryAllUpdatablePropertiesEquals(expectedNodeHistory, getPersistedNodeHistory(expectedNodeHistory));
    }
}
