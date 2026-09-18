package com.opportunity.tree.web.rest;

import static com.opportunity.tree.domain.OpportunityLinkAsserts.*;
import static com.opportunity.tree.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opportunity.tree.IntegrationTest;
import com.opportunity.tree.domain.Opportunity;
import com.opportunity.tree.domain.OpportunityLink;
import com.opportunity.tree.domain.enumeration.LinkType;
import com.opportunity.tree.repository.EntityManager;
import com.opportunity.tree.repository.OpportunityLinkRepository;
import com.opportunity.tree.service.OpportunityLinkService;
import com.opportunity.tree.service.dto.OpportunityLinkDTO;
import com.opportunity.tree.service.mapper.OpportunityLinkMapper;
import java.time.Duration;
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
 * Integration tests for the {@link OpportunityLinkResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureWebTestClient(timeout = IntegrationTest.DEFAULT_ENTITY_TIMEOUT)
@WithMockUser
class OpportunityLinkResourceIT {

    private static final String DEFAULT_NAME = "AAAAAAAAAA";
    private static final String UPDATED_NAME = "BBBBBBBBBB";

    private static final String DEFAULT_URL = "https://^1`vG";
    private static final String UPDATED_URL = "http://=A|";

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
    private WebTestClient webTestClient;

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
        opportunity = em.insert(OpportunityResourceIT.createEntity(em)).block();
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
        opportunity = em.insert(OpportunityResourceIT.createUpdatedEntity(em)).block();
        updatedOpportunityLink.setOpportunity(opportunity);
        return updatedOpportunityLink;
    }

    public static void deleteEntities(EntityManager em) {
        try {
            em.deleteAll(OpportunityLink.class).block();
        } catch (Exception e) {
            // It can fail, if other entities are still referring this - it will be removed later.
        }
        OpportunityResourceIT.deleteEntities(em);
    }

    @BeforeEach
    void setupCsrf() {
        webTestClient = webTestClient.mutateWith(csrf());
    }

    @BeforeEach
    void initTest() {
        opportunityLink = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedOpportunityLink != null) {
            opportunityLinkRepository.delete(insertedOpportunityLink).block();
            insertedOpportunityLink = null;
        }
        deleteEntities(em);
    }

    @Test
    void createOpportunityLink() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the OpportunityLink
        OpportunityLinkDTO opportunityLinkDTO = opportunityLinkMapper.toDto(opportunityLink);
        var returnedOpportunityLinkDTO = webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(opportunityLinkDTO))
            .exchange()
            .expectStatus()
            .isCreated()
            .expectBody(OpportunityLinkDTO.class)
            .returnResult()
            .getResponseBody();

        // Validate the OpportunityLink in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedOpportunityLink = opportunityLinkMapper.toEntity(returnedOpportunityLinkDTO);
        assertOpportunityLinkUpdatableFieldsEquals(returnedOpportunityLink, getPersistedOpportunityLink(returnedOpportunityLink));

        insertedOpportunityLink = returnedOpportunityLink;
    }

    @Test
    void createOpportunityLinkWithExistingId() throws Exception {
        // Create the OpportunityLink with an existing ID
        opportunityLink.setId(1L);
        OpportunityLinkDTO opportunityLinkDTO = opportunityLinkMapper.toDto(opportunityLink);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(opportunityLinkDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the OpportunityLink in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    void checkNameIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        opportunityLink.setName(null);

        // Create the OpportunityLink, which fails.
        OpportunityLinkDTO opportunityLinkDTO = opportunityLinkMapper.toDto(opportunityLink);

        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(opportunityLinkDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    void checkUrlIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        opportunityLink.setUrl(null);

        // Create the OpportunityLink, which fails.
        OpportunityLinkDTO opportunityLinkDTO = opportunityLinkMapper.toDto(opportunityLink);

        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(opportunityLinkDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    void checkTypeIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        opportunityLink.setType(null);

        // Create the OpportunityLink, which fails.
        OpportunityLinkDTO opportunityLinkDTO = opportunityLinkMapper.toDto(opportunityLink);

        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(opportunityLinkDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    void checkSortOrderIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        opportunityLink.setSortOrder(null);

        // Create the OpportunityLink, which fails.
        OpportunityLinkDTO opportunityLinkDTO = opportunityLinkMapper.toDto(opportunityLink);

        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(opportunityLinkDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    void getAllOpportunityLinksAsStream() {
        // Initialize the database
        opportunityLinkRepository.save(opportunityLink).block();

        List<OpportunityLink> opportunityLinkList = webTestClient
            .get()
            .uri(ENTITY_API_URL)
            .accept(MediaType.APPLICATION_NDJSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentTypeCompatibleWith(MediaType.APPLICATION_NDJSON)
            .returnResult(OpportunityLinkDTO.class)
            .getResponseBody()
            .map(opportunityLinkMapper::toEntity)
            .filter(opportunityLink::equals)
            .collectList()
            .block(Duration.ofSeconds(5));

        assertThat(opportunityLinkList).isNotNull();
        assertThat(opportunityLinkList).hasSize(1);
        OpportunityLink testOpportunityLink = opportunityLinkList.get(0);

        // Test fails because reactive api returns an empty object instead of null
        // assertOpportunityLinkAllPropertiesEquals(opportunityLink, testOpportunityLink);
        assertOpportunityLinkUpdatableFieldsEquals(opportunityLink, testOpportunityLink);
    }

    @Test
    void getAllOpportunityLinks() {
        // Initialize the database
        insertedOpportunityLink = opportunityLinkRepository.save(opportunityLink).block();

        // Get all the opportunityLinkList
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
            .value(hasItem(opportunityLink.getId().intValue()))
            .jsonPath("$.[*].name")
            .value(hasItem(DEFAULT_NAME))
            .jsonPath("$.[*].url")
            .value(hasItem(DEFAULT_URL))
            .jsonPath("$.[*].type")
            .value(hasItem(DEFAULT_TYPE.toString()))
            .jsonPath("$.[*].sortOrder")
            .value(hasItem(DEFAULT_SORT_ORDER));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllOpportunityLinksWithEagerRelationshipsIsEnabled() {
        when(opportunityLinkServiceMock.findAllWithEagerRelationships(any())).thenReturn(Flux.empty());

        webTestClient.get().uri(ENTITY_API_URL + "?eagerload=true").exchange().expectStatus().isOk();

        verify(opportunityLinkServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllOpportunityLinksWithEagerRelationshipsIsNotEnabled() {
        when(opportunityLinkServiceMock.findAllWithEagerRelationships(any())).thenReturn(Flux.empty());

        webTestClient.get().uri(ENTITY_API_URL + "?eagerload=false").exchange().expectStatus().isOk();
        verify(opportunityLinkRepositoryMock, times(1)).findAllWithEagerRelationships(any());
    }

    @Test
    void getOpportunityLink() {
        // Initialize the database
        insertedOpportunityLink = opportunityLinkRepository.save(opportunityLink).block();

        // Get the opportunityLink
        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, opportunityLink.getId())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.id")
            .value(is(opportunityLink.getId().intValue()))
            .jsonPath("$.name")
            .value(is(DEFAULT_NAME))
            .jsonPath("$.url")
            .value(is(DEFAULT_URL))
            .jsonPath("$.type")
            .value(is(DEFAULT_TYPE.toString()))
            .jsonPath("$.sortOrder")
            .value(is(DEFAULT_SORT_ORDER));
    }

    @Test
    void getNonExistingOpportunityLink() {
        // Get the opportunityLink
        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, Long.MAX_VALUE)
            .accept(MediaType.APPLICATION_PROBLEM_JSON)
            .exchange()
            .expectStatus()
            .isNotFound();
    }

    @Test
    void putExistingOpportunityLink() throws Exception {
        // Initialize the database
        insertedOpportunityLink = opportunityLinkRepository.save(opportunityLink).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the opportunityLink
        OpportunityLink updatedOpportunityLink = opportunityLinkRepository.findById(opportunityLink.getId()).block();
        updatedOpportunityLink.name(UPDATED_NAME).url(UPDATED_URL).type(UPDATED_TYPE).sortOrder(UPDATED_SORT_ORDER);
        OpportunityLinkDTO opportunityLinkDTO = opportunityLinkMapper.toDto(updatedOpportunityLink);

        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, opportunityLinkDTO.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(opportunityLinkDTO))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the OpportunityLink in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedOpportunityLinkToMatchAllProperties(updatedOpportunityLink);
    }

    @Test
    void putNonExistingOpportunityLink() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        opportunityLink.setId(longCount.incrementAndGet());

        // Create the OpportunityLink
        OpportunityLinkDTO opportunityLinkDTO = opportunityLinkMapper.toDto(opportunityLink);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, opportunityLinkDTO.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(opportunityLinkDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the OpportunityLink in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void putWithIdMismatchOpportunityLink() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        opportunityLink.setId(longCount.incrementAndGet());

        // Create the OpportunityLink
        OpportunityLinkDTO opportunityLinkDTO = opportunityLinkMapper.toDto(opportunityLink);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, longCount.incrementAndGet())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(opportunityLinkDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the OpportunityLink in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void putWithMissingIdPathParamOpportunityLink() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        opportunityLink.setId(longCount.incrementAndGet());

        // Create the OpportunityLink
        OpportunityLinkDTO opportunityLinkDTO = opportunityLinkMapper.toDto(opportunityLink);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(opportunityLinkDTO))
            .exchange()
            .expectStatus()
            .isEqualTo(405);

        // Validate the OpportunityLink in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void partialUpdateOpportunityLinkWithPatch() throws Exception {
        // Initialize the database
        insertedOpportunityLink = opportunityLinkRepository.save(opportunityLink).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the opportunityLink using partial update
        OpportunityLink partialUpdatedOpportunityLink = new OpportunityLink();
        partialUpdatedOpportunityLink.setId(opportunityLink.getId());

        partialUpdatedOpportunityLink.sortOrder(UPDATED_SORT_ORDER);

        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, partialUpdatedOpportunityLink.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(partialUpdatedOpportunityLink))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the OpportunityLink in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertOpportunityLinkUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedOpportunityLink, opportunityLink),
            getPersistedOpportunityLink(opportunityLink)
        );
    }

    @Test
    void fullUpdateOpportunityLinkWithPatch() throws Exception {
        // Initialize the database
        insertedOpportunityLink = opportunityLinkRepository.save(opportunityLink).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the opportunityLink using partial update
        OpportunityLink partialUpdatedOpportunityLink = new OpportunityLink();
        partialUpdatedOpportunityLink.setId(opportunityLink.getId());

        partialUpdatedOpportunityLink.name(UPDATED_NAME).url(UPDATED_URL).type(UPDATED_TYPE).sortOrder(UPDATED_SORT_ORDER);

        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, partialUpdatedOpportunityLink.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(partialUpdatedOpportunityLink))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the OpportunityLink in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertOpportunityLinkUpdatableFieldsEquals(
            partialUpdatedOpportunityLink,
            getPersistedOpportunityLink(partialUpdatedOpportunityLink)
        );
    }

    @Test
    void patchNonExistingOpportunityLink() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        opportunityLink.setId(longCount.incrementAndGet());

        // Create the OpportunityLink
        OpportunityLinkDTO opportunityLinkDTO = opportunityLinkMapper.toDto(opportunityLink);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, opportunityLinkDTO.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(opportunityLinkDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the OpportunityLink in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void patchWithIdMismatchOpportunityLink() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        opportunityLink.setId(longCount.incrementAndGet());

        // Create the OpportunityLink
        OpportunityLinkDTO opportunityLinkDTO = opportunityLinkMapper.toDto(opportunityLink);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, longCount.incrementAndGet())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(opportunityLinkDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the OpportunityLink in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void patchWithMissingIdPathParamOpportunityLink() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        opportunityLink.setId(longCount.incrementAndGet());

        // Create the OpportunityLink
        OpportunityLinkDTO opportunityLinkDTO = opportunityLinkMapper.toDto(opportunityLink);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(opportunityLinkDTO))
            .exchange()
            .expectStatus()
            .isEqualTo(405);

        // Validate the OpportunityLink in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void deleteOpportunityLink() {
        // Initialize the database
        insertedOpportunityLink = opportunityLinkRepository.save(opportunityLink).block();

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the opportunityLink
        webTestClient
            .delete()
            .uri(ENTITY_API_URL_ID, opportunityLink.getId())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNoContent();

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return opportunityLinkRepository.count().block();
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
        return opportunityLinkRepository.findById(opportunityLink.getId()).block();
    }

    protected void assertPersistedOpportunityLinkToMatchAllProperties(OpportunityLink expectedOpportunityLink) {
        // Test fails because reactive api returns an empty object instead of null
        // assertOpportunityLinkAllPropertiesEquals(expectedOpportunityLink, getPersistedOpportunityLink(expectedOpportunityLink));
        assertOpportunityLinkUpdatableFieldsEquals(expectedOpportunityLink, getPersistedOpportunityLink(expectedOpportunityLink));
    }

    protected void assertPersistedOpportunityLinkToMatchUpdatableProperties(OpportunityLink expectedOpportunityLink) {
        // Test fails because reactive api returns an empty object instead of null
        // assertOpportunityLinkAllUpdatablePropertiesEquals(expectedOpportunityLink, getPersistedOpportunityLink(expectedOpportunityLink));
        assertOpportunityLinkUpdatableFieldsEquals(expectedOpportunityLink, getPersistedOpportunityLink(expectedOpportunityLink));
    }
}
