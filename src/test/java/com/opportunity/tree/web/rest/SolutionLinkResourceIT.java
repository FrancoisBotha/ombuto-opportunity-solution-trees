package com.opportunity.tree.web.rest;

import static com.opportunity.tree.domain.SolutionLinkAsserts.*;
import static com.opportunity.tree.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opportunity.tree.IntegrationTest;
import com.opportunity.tree.domain.Solution;
import com.opportunity.tree.domain.SolutionLink;
import com.opportunity.tree.domain.enumeration.LinkType;
import com.opportunity.tree.repository.EntityManager;
import com.opportunity.tree.repository.SolutionLinkRepository;
import com.opportunity.tree.service.SolutionLinkService;
import com.opportunity.tree.service.dto.SolutionLinkDTO;
import com.opportunity.tree.service.mapper.SolutionLinkMapper;
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
 * Integration tests for the {@link SolutionLinkResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureWebTestClient(timeout = IntegrationTest.DEFAULT_ENTITY_TIMEOUT)
@WithMockUser
class SolutionLinkResourceIT {

    private static final String DEFAULT_NAME = "AAAAAAAAAA";
    private static final String UPDATED_NAME = "BBBBBBBBBB";

    private static final String DEFAULT_URL = "https://C";
    private static final String UPDATED_URL = "https://$]}";

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
    private WebTestClient webTestClient;

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
        solution = em.insert(SolutionResourceIT.createEntity(em)).block();
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
        solution = em.insert(SolutionResourceIT.createUpdatedEntity(em)).block();
        updatedSolutionLink.setSolution(solution);
        return updatedSolutionLink;
    }

    public static void deleteEntities(EntityManager em) {
        try {
            em.deleteAll(SolutionLink.class).block();
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
        solutionLink = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedSolutionLink != null) {
            solutionLinkRepository.delete(insertedSolutionLink).block();
            insertedSolutionLink = null;
        }
        deleteEntities(em);
    }

    @Test
    void createSolutionLink() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the SolutionLink
        SolutionLinkDTO solutionLinkDTO = solutionLinkMapper.toDto(solutionLink);
        var returnedSolutionLinkDTO = webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(solutionLinkDTO))
            .exchange()
            .expectStatus()
            .isCreated()
            .expectBody(SolutionLinkDTO.class)
            .returnResult()
            .getResponseBody();

        // Validate the SolutionLink in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedSolutionLink = solutionLinkMapper.toEntity(returnedSolutionLinkDTO);
        assertSolutionLinkUpdatableFieldsEquals(returnedSolutionLink, getPersistedSolutionLink(returnedSolutionLink));

        insertedSolutionLink = returnedSolutionLink;
    }

    @Test
    void createSolutionLinkWithExistingId() throws Exception {
        // Create the SolutionLink with an existing ID
        solutionLink.setId(1L);
        SolutionLinkDTO solutionLinkDTO = solutionLinkMapper.toDto(solutionLink);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(solutionLinkDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the SolutionLink in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    void checkNameIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        solutionLink.setName(null);

        // Create the SolutionLink, which fails.
        SolutionLinkDTO solutionLinkDTO = solutionLinkMapper.toDto(solutionLink);

        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(solutionLinkDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    void checkUrlIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        solutionLink.setUrl(null);

        // Create the SolutionLink, which fails.
        SolutionLinkDTO solutionLinkDTO = solutionLinkMapper.toDto(solutionLink);

        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(solutionLinkDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    void checkTypeIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        solutionLink.setType(null);

        // Create the SolutionLink, which fails.
        SolutionLinkDTO solutionLinkDTO = solutionLinkMapper.toDto(solutionLink);

        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(solutionLinkDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    void checkSortOrderIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        solutionLink.setSortOrder(null);

        // Create the SolutionLink, which fails.
        SolutionLinkDTO solutionLinkDTO = solutionLinkMapper.toDto(solutionLink);

        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(solutionLinkDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    void getAllSolutionLinksAsStream() {
        // Initialize the database
        solutionLinkRepository.save(solutionLink).block();

        List<SolutionLink> solutionLinkList = webTestClient
            .get()
            .uri(ENTITY_API_URL)
            .accept(MediaType.APPLICATION_NDJSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentTypeCompatibleWith(MediaType.APPLICATION_NDJSON)
            .returnResult(SolutionLinkDTO.class)
            .getResponseBody()
            .map(solutionLinkMapper::toEntity)
            .filter(solutionLink::equals)
            .collectList()
            .block(Duration.ofSeconds(5));

        assertThat(solutionLinkList).isNotNull();
        assertThat(solutionLinkList).hasSize(1);
        SolutionLink testSolutionLink = solutionLinkList.get(0);

        // Test fails because reactive api returns an empty object instead of null
        // assertSolutionLinkAllPropertiesEquals(solutionLink, testSolutionLink);
        assertSolutionLinkUpdatableFieldsEquals(solutionLink, testSolutionLink);
    }

    @Test
    void getAllSolutionLinks() {
        // Initialize the database
        insertedSolutionLink = solutionLinkRepository.save(solutionLink).block();

        // Get all the solutionLinkList
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
            .value(hasItem(solutionLink.getId().intValue()))
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
    void getAllSolutionLinksWithEagerRelationshipsIsEnabled() {
        when(solutionLinkServiceMock.findAllWithEagerRelationships(any())).thenReturn(Flux.empty());

        webTestClient.get().uri(ENTITY_API_URL + "?eagerload=true").exchange().expectStatus().isOk();

        verify(solutionLinkServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllSolutionLinksWithEagerRelationshipsIsNotEnabled() {
        when(solutionLinkServiceMock.findAllWithEagerRelationships(any())).thenReturn(Flux.empty());

        webTestClient.get().uri(ENTITY_API_URL + "?eagerload=false").exchange().expectStatus().isOk();
        verify(solutionLinkRepositoryMock, times(1)).findAllWithEagerRelationships(any());
    }

    @Test
    void getSolutionLink() {
        // Initialize the database
        insertedSolutionLink = solutionLinkRepository.save(solutionLink).block();

        // Get the solutionLink
        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, solutionLink.getId())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.id")
            .value(is(solutionLink.getId().intValue()))
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
    void getNonExistingSolutionLink() {
        // Get the solutionLink
        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, Long.MAX_VALUE)
            .accept(MediaType.APPLICATION_PROBLEM_JSON)
            .exchange()
            .expectStatus()
            .isNotFound();
    }

    @Test
    void putExistingSolutionLink() throws Exception {
        // Initialize the database
        insertedSolutionLink = solutionLinkRepository.save(solutionLink).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the solutionLink
        SolutionLink updatedSolutionLink = solutionLinkRepository.findById(solutionLink.getId()).block();
        updatedSolutionLink.name(UPDATED_NAME).url(UPDATED_URL).type(UPDATED_TYPE).sortOrder(UPDATED_SORT_ORDER);
        SolutionLinkDTO solutionLinkDTO = solutionLinkMapper.toDto(updatedSolutionLink);

        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, solutionLinkDTO.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(solutionLinkDTO))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the SolutionLink in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedSolutionLinkToMatchAllProperties(updatedSolutionLink);
    }

    @Test
    void putNonExistingSolutionLink() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        solutionLink.setId(longCount.incrementAndGet());

        // Create the SolutionLink
        SolutionLinkDTO solutionLinkDTO = solutionLinkMapper.toDto(solutionLink);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, solutionLinkDTO.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(solutionLinkDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the SolutionLink in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void putWithIdMismatchSolutionLink() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        solutionLink.setId(longCount.incrementAndGet());

        // Create the SolutionLink
        SolutionLinkDTO solutionLinkDTO = solutionLinkMapper.toDto(solutionLink);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, longCount.incrementAndGet())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(solutionLinkDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the SolutionLink in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void putWithMissingIdPathParamSolutionLink() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        solutionLink.setId(longCount.incrementAndGet());

        // Create the SolutionLink
        SolutionLinkDTO solutionLinkDTO = solutionLinkMapper.toDto(solutionLink);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(solutionLinkDTO))
            .exchange()
            .expectStatus()
            .isEqualTo(405);

        // Validate the SolutionLink in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void partialUpdateSolutionLinkWithPatch() throws Exception {
        // Initialize the database
        insertedSolutionLink = solutionLinkRepository.save(solutionLink).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the solutionLink using partial update
        SolutionLink partialUpdatedSolutionLink = new SolutionLink();
        partialUpdatedSolutionLink.setId(solutionLink.getId());

        partialUpdatedSolutionLink.name(UPDATED_NAME).url(UPDATED_URL);

        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, partialUpdatedSolutionLink.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(partialUpdatedSolutionLink))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the SolutionLink in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertSolutionLinkUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedSolutionLink, solutionLink),
            getPersistedSolutionLink(solutionLink)
        );
    }

    @Test
    void fullUpdateSolutionLinkWithPatch() throws Exception {
        // Initialize the database
        insertedSolutionLink = solutionLinkRepository.save(solutionLink).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the solutionLink using partial update
        SolutionLink partialUpdatedSolutionLink = new SolutionLink();
        partialUpdatedSolutionLink.setId(solutionLink.getId());

        partialUpdatedSolutionLink.name(UPDATED_NAME).url(UPDATED_URL).type(UPDATED_TYPE).sortOrder(UPDATED_SORT_ORDER);

        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, partialUpdatedSolutionLink.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(partialUpdatedSolutionLink))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the SolutionLink in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertSolutionLinkUpdatableFieldsEquals(partialUpdatedSolutionLink, getPersistedSolutionLink(partialUpdatedSolutionLink));
    }

    @Test
    void patchNonExistingSolutionLink() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        solutionLink.setId(longCount.incrementAndGet());

        // Create the SolutionLink
        SolutionLinkDTO solutionLinkDTO = solutionLinkMapper.toDto(solutionLink);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, solutionLinkDTO.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(solutionLinkDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the SolutionLink in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void patchWithIdMismatchSolutionLink() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        solutionLink.setId(longCount.incrementAndGet());

        // Create the SolutionLink
        SolutionLinkDTO solutionLinkDTO = solutionLinkMapper.toDto(solutionLink);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, longCount.incrementAndGet())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(solutionLinkDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the SolutionLink in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void patchWithMissingIdPathParamSolutionLink() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        solutionLink.setId(longCount.incrementAndGet());

        // Create the SolutionLink
        SolutionLinkDTO solutionLinkDTO = solutionLinkMapper.toDto(solutionLink);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(solutionLinkDTO))
            .exchange()
            .expectStatus()
            .isEqualTo(405);

        // Validate the SolutionLink in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void deleteSolutionLink() {
        // Initialize the database
        insertedSolutionLink = solutionLinkRepository.save(solutionLink).block();

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the solutionLink
        webTestClient
            .delete()
            .uri(ENTITY_API_URL_ID, solutionLink.getId())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNoContent();

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return solutionLinkRepository.count().block();
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
        return solutionLinkRepository.findById(solutionLink.getId()).block();
    }

    protected void assertPersistedSolutionLinkToMatchAllProperties(SolutionLink expectedSolutionLink) {
        // Test fails because reactive api returns an empty object instead of null
        // assertSolutionLinkAllPropertiesEquals(expectedSolutionLink, getPersistedSolutionLink(expectedSolutionLink));
        assertSolutionLinkUpdatableFieldsEquals(expectedSolutionLink, getPersistedSolutionLink(expectedSolutionLink));
    }

    protected void assertPersistedSolutionLinkToMatchUpdatableProperties(SolutionLink expectedSolutionLink) {
        // Test fails because reactive api returns an empty object instead of null
        // assertSolutionLinkAllUpdatablePropertiesEquals(expectedSolutionLink, getPersistedSolutionLink(expectedSolutionLink));
        assertSolutionLinkUpdatableFieldsEquals(expectedSolutionLink, getPersistedSolutionLink(expectedSolutionLink));
    }
}
