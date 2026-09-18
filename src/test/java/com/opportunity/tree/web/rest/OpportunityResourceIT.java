package com.opportunity.tree.web.rest;

import static com.opportunity.tree.domain.OpportunityAsserts.*;
import static com.opportunity.tree.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opportunity.tree.IntegrationTest;
import com.opportunity.tree.domain.Opportunity;
import com.opportunity.tree.domain.Outcome;
import com.opportunity.tree.domain.enumeration.OpportunityStatus;
import com.opportunity.tree.repository.EntityManager;
import com.opportunity.tree.repository.OpportunityRepository;
import com.opportunity.tree.repository.UserRepository;
import com.opportunity.tree.service.OpportunityService;
import com.opportunity.tree.service.dto.OpportunityDTO;
import com.opportunity.tree.service.mapper.OpportunityMapper;
import java.time.Instant;
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
 * Integration tests for the {@link OpportunityResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureWebTestClient(timeout = IntegrationTest.DEFAULT_ENTITY_TIMEOUT)
@WithMockUser
class OpportunityResourceIT {

    private static final String DEFAULT_TITLE = "AAAAAAAAAA";
    private static final String UPDATED_TITLE = "BBBBBBBBBB";

    private static final String DEFAULT_DESCRIPTION = "AAAAAAAAAA";
    private static final String UPDATED_DESCRIPTION = "BBBBBBBBBB";

    private static final OpportunityStatus DEFAULT_STATUS = OpportunityStatus.IDENTIFIED;
    private static final OpportunityStatus UPDATED_STATUS = OpportunityStatus.EXPLORING;

    private static final Integer DEFAULT_VALUE = 1;
    private static final Integer UPDATED_VALUE = 2;

    private static final Integer DEFAULT_COMPLEXITY = 1;
    private static final Integer UPDATED_COMPLEXITY = 2;

    private static final Integer DEFAULT_SORT_ORDER = 1;
    private static final Integer UPDATED_SORT_ORDER = 2;

    private static final Instant DEFAULT_CREATED_DATE = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CREATED_DATE = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_LAST_MODIFIED_DATE = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_LAST_MODIFIED_DATE = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/opportunities";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2L * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private OpportunityRepository opportunityRepository;

    @Autowired
    private UserRepository userRepository;

    @Mock
    private OpportunityRepository opportunityRepositoryMock;

    @Autowired
    private OpportunityMapper opportunityMapper;

    @Mock
    private OpportunityService opportunityServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private WebTestClient webTestClient;

    private Opportunity opportunity;

    private Opportunity insertedOpportunity;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Opportunity createEntity(EntityManager em) {
        Opportunity opportunity = new Opportunity()
            .title(DEFAULT_TITLE)
            .description(DEFAULT_DESCRIPTION)
            .status(DEFAULT_STATUS)
            .value(DEFAULT_VALUE)
            .complexity(DEFAULT_COMPLEXITY)
            .sortOrder(DEFAULT_SORT_ORDER)
            .createdDate(DEFAULT_CREATED_DATE)
            .lastModifiedDate(DEFAULT_LAST_MODIFIED_DATE);
        // Add required entity
        Outcome outcome;
        outcome = em.insert(OutcomeResourceIT.createEntity(em)).block();
        opportunity.setOutcome(outcome);
        return opportunity;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Opportunity createUpdatedEntity(EntityManager em) {
        Opportunity updatedOpportunity = new Opportunity()
            .title(UPDATED_TITLE)
            .description(UPDATED_DESCRIPTION)
            .status(UPDATED_STATUS)
            .value(UPDATED_VALUE)
            .complexity(UPDATED_COMPLEXITY)
            .sortOrder(UPDATED_SORT_ORDER)
            .createdDate(UPDATED_CREATED_DATE)
            .lastModifiedDate(UPDATED_LAST_MODIFIED_DATE);
        // Add required entity
        Outcome outcome;
        outcome = em.insert(OutcomeResourceIT.createUpdatedEntity(em)).block();
        updatedOpportunity.setOutcome(outcome);
        return updatedOpportunity;
    }

    public static void deleteEntities(EntityManager em) {
        try {
            em.deleteAll("rel_opportunity__interview").block();
            em.deleteAll("rel_opportunity__tag").block();
            em.deleteAll(Opportunity.class).block();
        } catch (Exception e) {
            // It can fail, if other entities are still referring this - it will be removed later.
        }
        OutcomeResourceIT.deleteEntities(em);
    }

    @BeforeEach
    void setupCsrf() {
        webTestClient = webTestClient.mutateWith(csrf());
    }

    @BeforeEach
    void initTest() {
        opportunity = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedOpportunity != null) {
            opportunityRepository.delete(insertedOpportunity).block();
            insertedOpportunity = null;
        }
        deleteEntities(em);
        userRepository.deleteAllUserAuthorities().block();
        userRepository.deleteAll().block();
    }

    @Test
    void createOpportunity() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Opportunity
        OpportunityDTO opportunityDTO = opportunityMapper.toDto(opportunity);
        var returnedOpportunityDTO = webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(opportunityDTO))
            .exchange()
            .expectStatus()
            .isCreated()
            .expectBody(OpportunityDTO.class)
            .returnResult()
            .getResponseBody();

        // Validate the Opportunity in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedOpportunity = opportunityMapper.toEntity(returnedOpportunityDTO);
        assertOpportunityUpdatableFieldsEquals(returnedOpportunity, getPersistedOpportunity(returnedOpportunity));

        insertedOpportunity = returnedOpportunity;
    }

    @Test
    void createOpportunityWithExistingId() throws Exception {
        // Create the Opportunity with an existing ID
        opportunity.setId(1L);
        OpportunityDTO opportunityDTO = opportunityMapper.toDto(opportunity);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(opportunityDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Opportunity in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    void checkTitleIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        opportunity.setTitle(null);

        // Create the Opportunity, which fails.
        OpportunityDTO opportunityDTO = opportunityMapper.toDto(opportunity);

        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(opportunityDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    void checkStatusIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        opportunity.setStatus(null);

        // Create the Opportunity, which fails.
        OpportunityDTO opportunityDTO = opportunityMapper.toDto(opportunity);

        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(opportunityDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    void checkValueIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        opportunity.setValue(null);

        // Create the Opportunity, which fails.
        OpportunityDTO opportunityDTO = opportunityMapper.toDto(opportunity);

        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(opportunityDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    void checkComplexityIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        opportunity.setComplexity(null);

        // Create the Opportunity, which fails.
        OpportunityDTO opportunityDTO = opportunityMapper.toDto(opportunity);

        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(opportunityDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    void checkSortOrderIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        opportunity.setSortOrder(null);

        // Create the Opportunity, which fails.
        OpportunityDTO opportunityDTO = opportunityMapper.toDto(opportunity);

        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(opportunityDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    void checkCreatedDateIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        opportunity.setCreatedDate(null);

        // Create the Opportunity, which fails.
        OpportunityDTO opportunityDTO = opportunityMapper.toDto(opportunity);

        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(opportunityDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    void getAllOpportunities() {
        // Initialize the database
        insertedOpportunity = opportunityRepository.save(opportunity).block();

        // Get all the opportunityList
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
            .value(hasItem(opportunity.getId().intValue()))
            .jsonPath("$.[*].title")
            .value(hasItem(DEFAULT_TITLE))
            .jsonPath("$.[*].description")
            .value(hasItem(DEFAULT_DESCRIPTION))
            .jsonPath("$.[*].status")
            .value(hasItem(DEFAULT_STATUS.toString()))
            .jsonPath("$.[*].value")
            .value(hasItem(DEFAULT_VALUE))
            .jsonPath("$.[*].complexity")
            .value(hasItem(DEFAULT_COMPLEXITY))
            .jsonPath("$.[*].sortOrder")
            .value(hasItem(DEFAULT_SORT_ORDER))
            .jsonPath("$.[*].createdDate")
            .value(hasItem(DEFAULT_CREATED_DATE.toString()))
            .jsonPath("$.[*].lastModifiedDate")
            .value(hasItem(DEFAULT_LAST_MODIFIED_DATE.toString()));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllOpportunitiesWithEagerRelationshipsIsEnabled() {
        when(opportunityServiceMock.findAllWithEagerRelationships(any())).thenReturn(Flux.empty());

        webTestClient.get().uri(ENTITY_API_URL + "?eagerload=true").exchange().expectStatus().isOk();

        verify(opportunityServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllOpportunitiesWithEagerRelationshipsIsNotEnabled() {
        when(opportunityServiceMock.findAllWithEagerRelationships(any())).thenReturn(Flux.empty());

        webTestClient.get().uri(ENTITY_API_URL + "?eagerload=false").exchange().expectStatus().isOk();
        verify(opportunityRepositoryMock, times(1)).findAllWithEagerRelationships(any());
    }

    @Test
    void getOpportunity() {
        // Initialize the database
        insertedOpportunity = opportunityRepository.save(opportunity).block();

        // Get the opportunity
        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, opportunity.getId())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.id")
            .value(is(opportunity.getId().intValue()))
            .jsonPath("$.title")
            .value(is(DEFAULT_TITLE))
            .jsonPath("$.description")
            .value(is(DEFAULT_DESCRIPTION))
            .jsonPath("$.status")
            .value(is(DEFAULT_STATUS.toString()))
            .jsonPath("$.value")
            .value(is(DEFAULT_VALUE))
            .jsonPath("$.complexity")
            .value(is(DEFAULT_COMPLEXITY))
            .jsonPath("$.sortOrder")
            .value(is(DEFAULT_SORT_ORDER))
            .jsonPath("$.createdDate")
            .value(is(DEFAULT_CREATED_DATE.toString()))
            .jsonPath("$.lastModifiedDate")
            .value(is(DEFAULT_LAST_MODIFIED_DATE.toString()));
    }

    @Test
    void getNonExistingOpportunity() {
        // Get the opportunity
        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, Long.MAX_VALUE)
            .accept(MediaType.APPLICATION_PROBLEM_JSON)
            .exchange()
            .expectStatus()
            .isNotFound();
    }

    @Test
    void putExistingOpportunity() throws Exception {
        // Initialize the database
        insertedOpportunity = opportunityRepository.save(opportunity).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the opportunity
        Opportunity updatedOpportunity = opportunityRepository.findById(opportunity.getId()).block();
        updatedOpportunity
            .title(UPDATED_TITLE)
            .description(UPDATED_DESCRIPTION)
            .status(UPDATED_STATUS)
            .value(UPDATED_VALUE)
            .complexity(UPDATED_COMPLEXITY)
            .sortOrder(UPDATED_SORT_ORDER)
            .createdDate(UPDATED_CREATED_DATE)
            .lastModifiedDate(UPDATED_LAST_MODIFIED_DATE);
        OpportunityDTO opportunityDTO = opportunityMapper.toDto(updatedOpportunity);

        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, opportunityDTO.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(opportunityDTO))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Opportunity in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedOpportunityToMatchAllProperties(updatedOpportunity);
    }

    @Test
    void putNonExistingOpportunity() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        opportunity.setId(longCount.incrementAndGet());

        // Create the Opportunity
        OpportunityDTO opportunityDTO = opportunityMapper.toDto(opportunity);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, opportunityDTO.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(opportunityDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Opportunity in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void putWithIdMismatchOpportunity() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        opportunity.setId(longCount.incrementAndGet());

        // Create the Opportunity
        OpportunityDTO opportunityDTO = opportunityMapper.toDto(opportunity);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, longCount.incrementAndGet())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(opportunityDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Opportunity in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void putWithMissingIdPathParamOpportunity() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        opportunity.setId(longCount.incrementAndGet());

        // Create the Opportunity
        OpportunityDTO opportunityDTO = opportunityMapper.toDto(opportunity);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(opportunityDTO))
            .exchange()
            .expectStatus()
            .isEqualTo(405);

        // Validate the Opportunity in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void partialUpdateOpportunityWithPatch() throws Exception {
        // Initialize the database
        insertedOpportunity = opportunityRepository.save(opportunity).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the opportunity using partial update
        Opportunity partialUpdatedOpportunity = new Opportunity();
        partialUpdatedOpportunity.setId(opportunity.getId());

        partialUpdatedOpportunity
            .description(UPDATED_DESCRIPTION)
            .status(UPDATED_STATUS)
            .value(UPDATED_VALUE)
            .lastModifiedDate(UPDATED_LAST_MODIFIED_DATE);

        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, partialUpdatedOpportunity.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(partialUpdatedOpportunity))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Opportunity in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertOpportunityUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedOpportunity, opportunity),
            getPersistedOpportunity(opportunity)
        );
    }

    @Test
    void fullUpdateOpportunityWithPatch() throws Exception {
        // Initialize the database
        insertedOpportunity = opportunityRepository.save(opportunity).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the opportunity using partial update
        Opportunity partialUpdatedOpportunity = new Opportunity();
        partialUpdatedOpportunity.setId(opportunity.getId());

        partialUpdatedOpportunity
            .title(UPDATED_TITLE)
            .description(UPDATED_DESCRIPTION)
            .status(UPDATED_STATUS)
            .value(UPDATED_VALUE)
            .complexity(UPDATED_COMPLEXITY)
            .sortOrder(UPDATED_SORT_ORDER)
            .createdDate(UPDATED_CREATED_DATE)
            .lastModifiedDate(UPDATED_LAST_MODIFIED_DATE);

        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, partialUpdatedOpportunity.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(partialUpdatedOpportunity))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Opportunity in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertOpportunityUpdatableFieldsEquals(partialUpdatedOpportunity, getPersistedOpportunity(partialUpdatedOpportunity));
    }

    @Test
    void patchNonExistingOpportunity() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        opportunity.setId(longCount.incrementAndGet());

        // Create the Opportunity
        OpportunityDTO opportunityDTO = opportunityMapper.toDto(opportunity);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, opportunityDTO.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(opportunityDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Opportunity in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void patchWithIdMismatchOpportunity() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        opportunity.setId(longCount.incrementAndGet());

        // Create the Opportunity
        OpportunityDTO opportunityDTO = opportunityMapper.toDto(opportunity);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, longCount.incrementAndGet())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(opportunityDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Opportunity in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void patchWithMissingIdPathParamOpportunity() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        opportunity.setId(longCount.incrementAndGet());

        // Create the Opportunity
        OpportunityDTO opportunityDTO = opportunityMapper.toDto(opportunity);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(opportunityDTO))
            .exchange()
            .expectStatus()
            .isEqualTo(405);

        // Validate the Opportunity in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void deleteOpportunity() {
        // Initialize the database
        insertedOpportunity = opportunityRepository.save(opportunity).block();

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the opportunity
        webTestClient
            .delete()
            .uri(ENTITY_API_URL_ID, opportunity.getId())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNoContent();

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return opportunityRepository.count().block();
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

    protected Opportunity getPersistedOpportunity(Opportunity opportunity) {
        return opportunityRepository.findById(opportunity.getId()).block();
    }

    protected void assertPersistedOpportunityToMatchAllProperties(Opportunity expectedOpportunity) {
        // Test fails because reactive api returns an empty object instead of null
        // assertOpportunityAllPropertiesEquals(expectedOpportunity, getPersistedOpportunity(expectedOpportunity));
        assertOpportunityUpdatableFieldsEquals(expectedOpportunity, getPersistedOpportunity(expectedOpportunity));
    }

    protected void assertPersistedOpportunityToMatchUpdatableProperties(Opportunity expectedOpportunity) {
        // Test fails because reactive api returns an empty object instead of null
        // assertOpportunityAllUpdatablePropertiesEquals(expectedOpportunity, getPersistedOpportunity(expectedOpportunity));
        assertOpportunityUpdatableFieldsEquals(expectedOpportunity, getPersistedOpportunity(expectedOpportunity));
    }
}
