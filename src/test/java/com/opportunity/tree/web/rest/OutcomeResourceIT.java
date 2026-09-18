package com.opportunity.tree.web.rest;

import static com.opportunity.tree.domain.OutcomeAsserts.*;
import static com.opportunity.tree.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opportunity.tree.IntegrationTest;
import com.opportunity.tree.domain.Outcome;
import com.opportunity.tree.domain.Product;
import com.opportunity.tree.domain.enumeration.OutcomeStatus;
import com.opportunity.tree.repository.EntityManager;
import com.opportunity.tree.repository.OutcomeRepository;
import com.opportunity.tree.repository.UserRepository;
import com.opportunity.tree.service.OutcomeService;
import com.opportunity.tree.service.dto.OutcomeDTO;
import com.opportunity.tree.service.mapper.OutcomeMapper;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
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
 * Integration tests for the {@link OutcomeResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureWebTestClient(timeout = IntegrationTest.DEFAULT_ENTITY_TIMEOUT)
@WithMockUser
class OutcomeResourceIT {

    private static final String DEFAULT_TITLE = "AAAAAAAAAA";
    private static final String UPDATED_TITLE = "BBBBBBBBBB";

    private static final String DEFAULT_DESCRIPTION = "AAAAAAAAAA";
    private static final String UPDATED_DESCRIPTION = "BBBBBBBBBB";

    private static final String DEFAULT_METRIC = "AAAAAAAAAA";
    private static final String UPDATED_METRIC = "BBBBBBBBBB";

    private static final String DEFAULT_TARGET_VALUE = "AAAAAAAAAA";
    private static final String UPDATED_TARGET_VALUE = "BBBBBBBBBB";

    private static final String DEFAULT_CURRENT_VALUE = "AAAAAAAAAA";
    private static final String UPDATED_CURRENT_VALUE = "BBBBBBBBBB";

    private static final OutcomeStatus DEFAULT_STATUS = OutcomeStatus.DRAFT;
    private static final OutcomeStatus UPDATED_STATUS = OutcomeStatus.ACTIVE;

    private static final LocalDate DEFAULT_START_DATE = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_START_DATE = LocalDate.now(ZoneId.systemDefault());

    private static final LocalDate DEFAULT_TARGET_DATE = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_TARGET_DATE = LocalDate.now(ZoneId.systemDefault());

    private static final Integer DEFAULT_SORT_ORDER = 1;
    private static final Integer UPDATED_SORT_ORDER = 2;

    private static final Instant DEFAULT_CREATED_DATE = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CREATED_DATE = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_LAST_MODIFIED_DATE = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_LAST_MODIFIED_DATE = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/outcomes";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2L * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private OutcomeRepository outcomeRepository;

    @Autowired
    private UserRepository userRepository;

    @Mock
    private OutcomeRepository outcomeRepositoryMock;

    @Autowired
    private OutcomeMapper outcomeMapper;

    @Mock
    private OutcomeService outcomeServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private WebTestClient webTestClient;

    private Outcome outcome;

    private Outcome insertedOutcome;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Outcome createEntity(EntityManager em) {
        Outcome outcome = new Outcome()
            .title(DEFAULT_TITLE)
            .description(DEFAULT_DESCRIPTION)
            .metric(DEFAULT_METRIC)
            .targetValue(DEFAULT_TARGET_VALUE)
            .currentValue(DEFAULT_CURRENT_VALUE)
            .status(DEFAULT_STATUS)
            .startDate(DEFAULT_START_DATE)
            .targetDate(DEFAULT_TARGET_DATE)
            .sortOrder(DEFAULT_SORT_ORDER)
            .createdDate(DEFAULT_CREATED_DATE)
            .lastModifiedDate(DEFAULT_LAST_MODIFIED_DATE);
        // Add required entity
        Product product;
        product = em.insert(ProductResourceIT.createEntity(em)).block();
        outcome.setProduct(product);
        return outcome;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Outcome createUpdatedEntity(EntityManager em) {
        Outcome updatedOutcome = new Outcome()
            .title(UPDATED_TITLE)
            .description(UPDATED_DESCRIPTION)
            .metric(UPDATED_METRIC)
            .targetValue(UPDATED_TARGET_VALUE)
            .currentValue(UPDATED_CURRENT_VALUE)
            .status(UPDATED_STATUS)
            .startDate(UPDATED_START_DATE)
            .targetDate(UPDATED_TARGET_DATE)
            .sortOrder(UPDATED_SORT_ORDER)
            .createdDate(UPDATED_CREATED_DATE)
            .lastModifiedDate(UPDATED_LAST_MODIFIED_DATE);
        // Add required entity
        Product product;
        product = em.insert(ProductResourceIT.createUpdatedEntity(em)).block();
        updatedOutcome.setProduct(product);
        return updatedOutcome;
    }

    public static void deleteEntities(EntityManager em) {
        try {
            em.deleteAll(Outcome.class).block();
        } catch (Exception e) {
            // It can fail, if other entities are still referring this - it will be removed later.
        }
        ProductResourceIT.deleteEntities(em);
    }

    @BeforeEach
    void setupCsrf() {
        webTestClient = webTestClient.mutateWith(csrf());
    }

    @BeforeEach
    void initTest() {
        outcome = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedOutcome != null) {
            outcomeRepository.delete(insertedOutcome).block();
            insertedOutcome = null;
        }
        deleteEntities(em);
        userRepository.deleteAllUserAuthorities().block();
        userRepository.deleteAll().block();
    }

    @Test
    void createOutcome() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Outcome
        OutcomeDTO outcomeDTO = outcomeMapper.toDto(outcome);
        var returnedOutcomeDTO = webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(outcomeDTO))
            .exchange()
            .expectStatus()
            .isCreated()
            .expectBody(OutcomeDTO.class)
            .returnResult()
            .getResponseBody();

        // Validate the Outcome in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedOutcome = outcomeMapper.toEntity(returnedOutcomeDTO);
        assertOutcomeUpdatableFieldsEquals(returnedOutcome, getPersistedOutcome(returnedOutcome));

        insertedOutcome = returnedOutcome;
    }

    @Test
    void createOutcomeWithExistingId() throws Exception {
        // Create the Outcome with an existing ID
        outcome.setId(1L);
        OutcomeDTO outcomeDTO = outcomeMapper.toDto(outcome);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(outcomeDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Outcome in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    void checkTitleIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        outcome.setTitle(null);

        // Create the Outcome, which fails.
        OutcomeDTO outcomeDTO = outcomeMapper.toDto(outcome);

        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(outcomeDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    void checkStatusIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        outcome.setStatus(null);

        // Create the Outcome, which fails.
        OutcomeDTO outcomeDTO = outcomeMapper.toDto(outcome);

        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(outcomeDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    void checkSortOrderIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        outcome.setSortOrder(null);

        // Create the Outcome, which fails.
        OutcomeDTO outcomeDTO = outcomeMapper.toDto(outcome);

        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(outcomeDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    void checkCreatedDateIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        outcome.setCreatedDate(null);

        // Create the Outcome, which fails.
        OutcomeDTO outcomeDTO = outcomeMapper.toDto(outcome);

        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(outcomeDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    void getAllOutcomesAsStream() {
        // Initialize the database
        outcomeRepository.save(outcome).block();

        List<Outcome> outcomeList = webTestClient
            .get()
            .uri(ENTITY_API_URL)
            .accept(MediaType.APPLICATION_NDJSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentTypeCompatibleWith(MediaType.APPLICATION_NDJSON)
            .returnResult(OutcomeDTO.class)
            .getResponseBody()
            .map(outcomeMapper::toEntity)
            .filter(outcome::equals)
            .collectList()
            .block(Duration.ofSeconds(5));

        assertThat(outcomeList).isNotNull();
        assertThat(outcomeList).hasSize(1);
        Outcome testOutcome = outcomeList.get(0);

        // Test fails because reactive api returns an empty object instead of null
        // assertOutcomeAllPropertiesEquals(outcome, testOutcome);
        assertOutcomeUpdatableFieldsEquals(outcome, testOutcome);
    }

    @Test
    void getAllOutcomes() {
        // Initialize the database
        insertedOutcome = outcomeRepository.save(outcome).block();

        // Get all the outcomeList
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
            .value(hasItem(outcome.getId().intValue()))
            .jsonPath("$.[*].title")
            .value(hasItem(DEFAULT_TITLE))
            .jsonPath("$.[*].description")
            .value(hasItem(DEFAULT_DESCRIPTION))
            .jsonPath("$.[*].metric")
            .value(hasItem(DEFAULT_METRIC))
            .jsonPath("$.[*].targetValue")
            .value(hasItem(DEFAULT_TARGET_VALUE))
            .jsonPath("$.[*].currentValue")
            .value(hasItem(DEFAULT_CURRENT_VALUE))
            .jsonPath("$.[*].status")
            .value(hasItem(DEFAULT_STATUS.toString()))
            .jsonPath("$.[*].startDate")
            .value(hasItem(DEFAULT_START_DATE.toString()))
            .jsonPath("$.[*].targetDate")
            .value(hasItem(DEFAULT_TARGET_DATE.toString()))
            .jsonPath("$.[*].sortOrder")
            .value(hasItem(DEFAULT_SORT_ORDER))
            .jsonPath("$.[*].createdDate")
            .value(hasItem(DEFAULT_CREATED_DATE.toString()))
            .jsonPath("$.[*].lastModifiedDate")
            .value(hasItem(DEFAULT_LAST_MODIFIED_DATE.toString()));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllOutcomesWithEagerRelationshipsIsEnabled() {
        when(outcomeServiceMock.findAllWithEagerRelationships(any())).thenReturn(Flux.empty());

        webTestClient.get().uri(ENTITY_API_URL + "?eagerload=true").exchange().expectStatus().isOk();

        verify(outcomeServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllOutcomesWithEagerRelationshipsIsNotEnabled() {
        when(outcomeServiceMock.findAllWithEagerRelationships(any())).thenReturn(Flux.empty());

        webTestClient.get().uri(ENTITY_API_URL + "?eagerload=false").exchange().expectStatus().isOk();
        verify(outcomeRepositoryMock, times(1)).findAllWithEagerRelationships(any());
    }

    @Test
    void getOutcome() {
        // Initialize the database
        insertedOutcome = outcomeRepository.save(outcome).block();

        // Get the outcome
        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, outcome.getId())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.id")
            .value(is(outcome.getId().intValue()))
            .jsonPath("$.title")
            .value(is(DEFAULT_TITLE))
            .jsonPath("$.description")
            .value(is(DEFAULT_DESCRIPTION))
            .jsonPath("$.metric")
            .value(is(DEFAULT_METRIC))
            .jsonPath("$.targetValue")
            .value(is(DEFAULT_TARGET_VALUE))
            .jsonPath("$.currentValue")
            .value(is(DEFAULT_CURRENT_VALUE))
            .jsonPath("$.status")
            .value(is(DEFAULT_STATUS.toString()))
            .jsonPath("$.startDate")
            .value(is(DEFAULT_START_DATE.toString()))
            .jsonPath("$.targetDate")
            .value(is(DEFAULT_TARGET_DATE.toString()))
            .jsonPath("$.sortOrder")
            .value(is(DEFAULT_SORT_ORDER))
            .jsonPath("$.createdDate")
            .value(is(DEFAULT_CREATED_DATE.toString()))
            .jsonPath("$.lastModifiedDate")
            .value(is(DEFAULT_LAST_MODIFIED_DATE.toString()));
    }

    @Test
    void getNonExistingOutcome() {
        // Get the outcome
        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, Long.MAX_VALUE)
            .accept(MediaType.APPLICATION_PROBLEM_JSON)
            .exchange()
            .expectStatus()
            .isNotFound();
    }

    @Test
    void putExistingOutcome() throws Exception {
        // Initialize the database
        insertedOutcome = outcomeRepository.save(outcome).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the outcome
        Outcome updatedOutcome = outcomeRepository.findById(outcome.getId()).block();
        updatedOutcome
            .title(UPDATED_TITLE)
            .description(UPDATED_DESCRIPTION)
            .metric(UPDATED_METRIC)
            .targetValue(UPDATED_TARGET_VALUE)
            .currentValue(UPDATED_CURRENT_VALUE)
            .status(UPDATED_STATUS)
            .startDate(UPDATED_START_DATE)
            .targetDate(UPDATED_TARGET_DATE)
            .sortOrder(UPDATED_SORT_ORDER)
            .createdDate(UPDATED_CREATED_DATE)
            .lastModifiedDate(UPDATED_LAST_MODIFIED_DATE);
        OutcomeDTO outcomeDTO = outcomeMapper.toDto(updatedOutcome);

        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, outcomeDTO.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(outcomeDTO))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Outcome in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedOutcomeToMatchAllProperties(updatedOutcome);
    }

    @Test
    void putNonExistingOutcome() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        outcome.setId(longCount.incrementAndGet());

        // Create the Outcome
        OutcomeDTO outcomeDTO = outcomeMapper.toDto(outcome);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, outcomeDTO.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(outcomeDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Outcome in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void putWithIdMismatchOutcome() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        outcome.setId(longCount.incrementAndGet());

        // Create the Outcome
        OutcomeDTO outcomeDTO = outcomeMapper.toDto(outcome);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, longCount.incrementAndGet())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(outcomeDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Outcome in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void putWithMissingIdPathParamOutcome() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        outcome.setId(longCount.incrementAndGet());

        // Create the Outcome
        OutcomeDTO outcomeDTO = outcomeMapper.toDto(outcome);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(outcomeDTO))
            .exchange()
            .expectStatus()
            .isEqualTo(405);

        // Validate the Outcome in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void partialUpdateOutcomeWithPatch() throws Exception {
        // Initialize the database
        insertedOutcome = outcomeRepository.save(outcome).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the outcome using partial update
        Outcome partialUpdatedOutcome = new Outcome();
        partialUpdatedOutcome.setId(outcome.getId());

        partialUpdatedOutcome
            .title(UPDATED_TITLE)
            .startDate(UPDATED_START_DATE)
            .targetDate(UPDATED_TARGET_DATE)
            .lastModifiedDate(UPDATED_LAST_MODIFIED_DATE);

        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, partialUpdatedOutcome.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(partialUpdatedOutcome))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Outcome in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertOutcomeUpdatableFieldsEquals(createUpdateProxyForBean(partialUpdatedOutcome, outcome), getPersistedOutcome(outcome));
    }

    @Test
    void fullUpdateOutcomeWithPatch() throws Exception {
        // Initialize the database
        insertedOutcome = outcomeRepository.save(outcome).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the outcome using partial update
        Outcome partialUpdatedOutcome = new Outcome();
        partialUpdatedOutcome.setId(outcome.getId());

        partialUpdatedOutcome
            .title(UPDATED_TITLE)
            .description(UPDATED_DESCRIPTION)
            .metric(UPDATED_METRIC)
            .targetValue(UPDATED_TARGET_VALUE)
            .currentValue(UPDATED_CURRENT_VALUE)
            .status(UPDATED_STATUS)
            .startDate(UPDATED_START_DATE)
            .targetDate(UPDATED_TARGET_DATE)
            .sortOrder(UPDATED_SORT_ORDER)
            .createdDate(UPDATED_CREATED_DATE)
            .lastModifiedDate(UPDATED_LAST_MODIFIED_DATE);

        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, partialUpdatedOutcome.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(partialUpdatedOutcome))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Outcome in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertOutcomeUpdatableFieldsEquals(partialUpdatedOutcome, getPersistedOutcome(partialUpdatedOutcome));
    }

    @Test
    void patchNonExistingOutcome() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        outcome.setId(longCount.incrementAndGet());

        // Create the Outcome
        OutcomeDTO outcomeDTO = outcomeMapper.toDto(outcome);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, outcomeDTO.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(outcomeDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Outcome in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void patchWithIdMismatchOutcome() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        outcome.setId(longCount.incrementAndGet());

        // Create the Outcome
        OutcomeDTO outcomeDTO = outcomeMapper.toDto(outcome);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, longCount.incrementAndGet())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(outcomeDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Outcome in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void patchWithMissingIdPathParamOutcome() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        outcome.setId(longCount.incrementAndGet());

        // Create the Outcome
        OutcomeDTO outcomeDTO = outcomeMapper.toDto(outcome);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(outcomeDTO))
            .exchange()
            .expectStatus()
            .isEqualTo(405);

        // Validate the Outcome in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void deleteOutcome() {
        // Initialize the database
        insertedOutcome = outcomeRepository.save(outcome).block();

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the outcome
        webTestClient
            .delete()
            .uri(ENTITY_API_URL_ID, outcome.getId())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNoContent();

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return outcomeRepository.count().block();
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

    protected Outcome getPersistedOutcome(Outcome outcome) {
        return outcomeRepository.findById(outcome.getId()).block();
    }

    protected void assertPersistedOutcomeToMatchAllProperties(Outcome expectedOutcome) {
        // Test fails because reactive api returns an empty object instead of null
        // assertOutcomeAllPropertiesEquals(expectedOutcome, getPersistedOutcome(expectedOutcome));
        assertOutcomeUpdatableFieldsEquals(expectedOutcome, getPersistedOutcome(expectedOutcome));
    }

    protected void assertPersistedOutcomeToMatchUpdatableProperties(Outcome expectedOutcome) {
        // Test fails because reactive api returns an empty object instead of null
        // assertOutcomeAllUpdatablePropertiesEquals(expectedOutcome, getPersistedOutcome(expectedOutcome));
        assertOutcomeUpdatableFieldsEquals(expectedOutcome, getPersistedOutcome(expectedOutcome));
    }
}
