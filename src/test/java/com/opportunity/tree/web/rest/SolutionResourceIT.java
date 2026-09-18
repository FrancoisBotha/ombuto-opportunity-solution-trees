package com.opportunity.tree.web.rest;

import static com.opportunity.tree.domain.SolutionAsserts.*;
import static com.opportunity.tree.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opportunity.tree.IntegrationTest;
import com.opportunity.tree.domain.Opportunity;
import com.opportunity.tree.domain.Solution;
import com.opportunity.tree.domain.enumeration.SolutionStatus;
import com.opportunity.tree.repository.EntityManager;
import com.opportunity.tree.repository.SolutionRepository;
import com.opportunity.tree.repository.UserRepository;
import com.opportunity.tree.service.SolutionService;
import com.opportunity.tree.service.dto.SolutionDTO;
import com.opportunity.tree.service.mapper.SolutionMapper;
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
 * Integration tests for the {@link SolutionResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureWebTestClient(timeout = IntegrationTest.DEFAULT_ENTITY_TIMEOUT)
@WithMockUser
class SolutionResourceIT {

    private static final String DEFAULT_TITLE = "AAAAAAAAAA";
    private static final String UPDATED_TITLE = "BBBBBBBBBB";

    private static final String DEFAULT_DESCRIPTION = "AAAAAAAAAA";
    private static final String UPDATED_DESCRIPTION = "BBBBBBBBBB";

    private static final SolutionStatus DEFAULT_STATUS = SolutionStatus.IDEA;
    private static final SolutionStatus UPDATED_STATUS = SolutionStatus.ASSUMPTION_MAPPING;

    private static final Integer DEFAULT_EFFORT = 1;
    private static final Integer UPDATED_EFFORT = 2;

    private static final Integer DEFAULT_SORT_ORDER = 1;
    private static final Integer UPDATED_SORT_ORDER = 2;

    private static final Instant DEFAULT_CREATED_DATE = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CREATED_DATE = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_LAST_MODIFIED_DATE = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_LAST_MODIFIED_DATE = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/solutions";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2L * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private SolutionRepository solutionRepository;

    @Autowired
    private UserRepository userRepository;

    @Mock
    private SolutionRepository solutionRepositoryMock;

    @Autowired
    private SolutionMapper solutionMapper;

    @Mock
    private SolutionService solutionServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private WebTestClient webTestClient;

    private Solution solution;

    private Solution insertedSolution;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Solution createEntity(EntityManager em) {
        Solution solution = new Solution()
            .title(DEFAULT_TITLE)
            .description(DEFAULT_DESCRIPTION)
            .status(DEFAULT_STATUS)
            .effort(DEFAULT_EFFORT)
            .sortOrder(DEFAULT_SORT_ORDER)
            .createdDate(DEFAULT_CREATED_DATE)
            .lastModifiedDate(DEFAULT_LAST_MODIFIED_DATE);
        // Add required entity
        Opportunity opportunity;
        opportunity = em.insert(OpportunityResourceIT.createEntity(em)).block();
        solution.setOpportunity(opportunity);
        return solution;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Solution createUpdatedEntity(EntityManager em) {
        Solution updatedSolution = new Solution()
            .title(UPDATED_TITLE)
            .description(UPDATED_DESCRIPTION)
            .status(UPDATED_STATUS)
            .effort(UPDATED_EFFORT)
            .sortOrder(UPDATED_SORT_ORDER)
            .createdDate(UPDATED_CREATED_DATE)
            .lastModifiedDate(UPDATED_LAST_MODIFIED_DATE);
        // Add required entity
        Opportunity opportunity;
        opportunity = em.insert(OpportunityResourceIT.createUpdatedEntity(em)).block();
        updatedSolution.setOpportunity(opportunity);
        return updatedSolution;
    }

    public static void deleteEntities(EntityManager em) {
        try {
            em.deleteAll("rel_solution__tag").block();
            em.deleteAll(Solution.class).block();
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
        solution = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedSolution != null) {
            solutionRepository.delete(insertedSolution).block();
            insertedSolution = null;
        }
        deleteEntities(em);
        userRepository.deleteAllUserAuthorities().block();
        userRepository.deleteAll().block();
    }

    @Test
    void createSolution() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Solution
        SolutionDTO solutionDTO = solutionMapper.toDto(solution);
        var returnedSolutionDTO = webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(solutionDTO))
            .exchange()
            .expectStatus()
            .isCreated()
            .expectBody(SolutionDTO.class)
            .returnResult()
            .getResponseBody();

        // Validate the Solution in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedSolution = solutionMapper.toEntity(returnedSolutionDTO);
        assertSolutionUpdatableFieldsEquals(returnedSolution, getPersistedSolution(returnedSolution));

        insertedSolution = returnedSolution;
    }

    @Test
    void createSolutionWithExistingId() throws Exception {
        // Create the Solution with an existing ID
        solution.setId(1L);
        SolutionDTO solutionDTO = solutionMapper.toDto(solution);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(solutionDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Solution in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    void checkTitleIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        solution.setTitle(null);

        // Create the Solution, which fails.
        SolutionDTO solutionDTO = solutionMapper.toDto(solution);

        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(solutionDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    void checkStatusIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        solution.setStatus(null);

        // Create the Solution, which fails.
        SolutionDTO solutionDTO = solutionMapper.toDto(solution);

        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(solutionDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    void checkSortOrderIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        solution.setSortOrder(null);

        // Create the Solution, which fails.
        SolutionDTO solutionDTO = solutionMapper.toDto(solution);

        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(solutionDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    void checkCreatedDateIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        solution.setCreatedDate(null);

        // Create the Solution, which fails.
        SolutionDTO solutionDTO = solutionMapper.toDto(solution);

        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(solutionDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    void getAllSolutions() {
        // Initialize the database
        insertedSolution = solutionRepository.save(solution).block();

        // Get all the solutionList
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
            .value(hasItem(solution.getId().intValue()))
            .jsonPath("$.[*].title")
            .value(hasItem(DEFAULT_TITLE))
            .jsonPath("$.[*].description")
            .value(hasItem(DEFAULT_DESCRIPTION))
            .jsonPath("$.[*].status")
            .value(hasItem(DEFAULT_STATUS.toString()))
            .jsonPath("$.[*].effort")
            .value(hasItem(DEFAULT_EFFORT))
            .jsonPath("$.[*].sortOrder")
            .value(hasItem(DEFAULT_SORT_ORDER))
            .jsonPath("$.[*].createdDate")
            .value(hasItem(DEFAULT_CREATED_DATE.toString()))
            .jsonPath("$.[*].lastModifiedDate")
            .value(hasItem(DEFAULT_LAST_MODIFIED_DATE.toString()));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllSolutionsWithEagerRelationshipsIsEnabled() {
        when(solutionServiceMock.findAllWithEagerRelationships(any())).thenReturn(Flux.empty());

        webTestClient.get().uri(ENTITY_API_URL + "?eagerload=true").exchange().expectStatus().isOk();

        verify(solutionServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllSolutionsWithEagerRelationshipsIsNotEnabled() {
        when(solutionServiceMock.findAllWithEagerRelationships(any())).thenReturn(Flux.empty());

        webTestClient.get().uri(ENTITY_API_URL + "?eagerload=false").exchange().expectStatus().isOk();
        verify(solutionRepositoryMock, times(1)).findAllWithEagerRelationships(any());
    }

    @Test
    void getSolution() {
        // Initialize the database
        insertedSolution = solutionRepository.save(solution).block();

        // Get the solution
        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, solution.getId())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.id")
            .value(is(solution.getId().intValue()))
            .jsonPath("$.title")
            .value(is(DEFAULT_TITLE))
            .jsonPath("$.description")
            .value(is(DEFAULT_DESCRIPTION))
            .jsonPath("$.status")
            .value(is(DEFAULT_STATUS.toString()))
            .jsonPath("$.effort")
            .value(is(DEFAULT_EFFORT))
            .jsonPath("$.sortOrder")
            .value(is(DEFAULT_SORT_ORDER))
            .jsonPath("$.createdDate")
            .value(is(DEFAULT_CREATED_DATE.toString()))
            .jsonPath("$.lastModifiedDate")
            .value(is(DEFAULT_LAST_MODIFIED_DATE.toString()));
    }

    @Test
    void getNonExistingSolution() {
        // Get the solution
        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, Long.MAX_VALUE)
            .accept(MediaType.APPLICATION_PROBLEM_JSON)
            .exchange()
            .expectStatus()
            .isNotFound();
    }

    @Test
    void putExistingSolution() throws Exception {
        // Initialize the database
        insertedSolution = solutionRepository.save(solution).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the solution
        Solution updatedSolution = solutionRepository.findById(solution.getId()).block();
        updatedSolution
            .title(UPDATED_TITLE)
            .description(UPDATED_DESCRIPTION)
            .status(UPDATED_STATUS)
            .effort(UPDATED_EFFORT)
            .sortOrder(UPDATED_SORT_ORDER)
            .createdDate(UPDATED_CREATED_DATE)
            .lastModifiedDate(UPDATED_LAST_MODIFIED_DATE);
        SolutionDTO solutionDTO = solutionMapper.toDto(updatedSolution);

        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, solutionDTO.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(solutionDTO))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Solution in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedSolutionToMatchAllProperties(updatedSolution);
    }

    @Test
    void putNonExistingSolution() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        solution.setId(longCount.incrementAndGet());

        // Create the Solution
        SolutionDTO solutionDTO = solutionMapper.toDto(solution);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, solutionDTO.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(solutionDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Solution in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void putWithIdMismatchSolution() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        solution.setId(longCount.incrementAndGet());

        // Create the Solution
        SolutionDTO solutionDTO = solutionMapper.toDto(solution);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, longCount.incrementAndGet())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(solutionDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Solution in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void putWithMissingIdPathParamSolution() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        solution.setId(longCount.incrementAndGet());

        // Create the Solution
        SolutionDTO solutionDTO = solutionMapper.toDto(solution);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(solutionDTO))
            .exchange()
            .expectStatus()
            .isEqualTo(405);

        // Validate the Solution in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void partialUpdateSolutionWithPatch() throws Exception {
        // Initialize the database
        insertedSolution = solutionRepository.save(solution).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the solution using partial update
        Solution partialUpdatedSolution = new Solution();
        partialUpdatedSolution.setId(solution.getId());

        partialUpdatedSolution.description(UPDATED_DESCRIPTION).effort(UPDATED_EFFORT).lastModifiedDate(UPDATED_LAST_MODIFIED_DATE);

        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, partialUpdatedSolution.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(partialUpdatedSolution))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Solution in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertSolutionUpdatableFieldsEquals(createUpdateProxyForBean(partialUpdatedSolution, solution), getPersistedSolution(solution));
    }

    @Test
    void fullUpdateSolutionWithPatch() throws Exception {
        // Initialize the database
        insertedSolution = solutionRepository.save(solution).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the solution using partial update
        Solution partialUpdatedSolution = new Solution();
        partialUpdatedSolution.setId(solution.getId());

        partialUpdatedSolution
            .title(UPDATED_TITLE)
            .description(UPDATED_DESCRIPTION)
            .status(UPDATED_STATUS)
            .effort(UPDATED_EFFORT)
            .sortOrder(UPDATED_SORT_ORDER)
            .createdDate(UPDATED_CREATED_DATE)
            .lastModifiedDate(UPDATED_LAST_MODIFIED_DATE);

        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, partialUpdatedSolution.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(partialUpdatedSolution))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Solution in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertSolutionUpdatableFieldsEquals(partialUpdatedSolution, getPersistedSolution(partialUpdatedSolution));
    }

    @Test
    void patchNonExistingSolution() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        solution.setId(longCount.incrementAndGet());

        // Create the Solution
        SolutionDTO solutionDTO = solutionMapper.toDto(solution);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, solutionDTO.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(solutionDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Solution in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void patchWithIdMismatchSolution() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        solution.setId(longCount.incrementAndGet());

        // Create the Solution
        SolutionDTO solutionDTO = solutionMapper.toDto(solution);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, longCount.incrementAndGet())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(solutionDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Solution in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void patchWithMissingIdPathParamSolution() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        solution.setId(longCount.incrementAndGet());

        // Create the Solution
        SolutionDTO solutionDTO = solutionMapper.toDto(solution);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(solutionDTO))
            .exchange()
            .expectStatus()
            .isEqualTo(405);

        // Validate the Solution in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void deleteSolution() {
        // Initialize the database
        insertedSolution = solutionRepository.save(solution).block();

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the solution
        webTestClient
            .delete()
            .uri(ENTITY_API_URL_ID, solution.getId())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNoContent();

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return solutionRepository.count().block();
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

    protected Solution getPersistedSolution(Solution solution) {
        return solutionRepository.findById(solution.getId()).block();
    }

    protected void assertPersistedSolutionToMatchAllProperties(Solution expectedSolution) {
        // Test fails because reactive api returns an empty object instead of null
        // assertSolutionAllPropertiesEquals(expectedSolution, getPersistedSolution(expectedSolution));
        assertSolutionUpdatableFieldsEquals(expectedSolution, getPersistedSolution(expectedSolution));
    }

    protected void assertPersistedSolutionToMatchUpdatableProperties(Solution expectedSolution) {
        // Test fails because reactive api returns an empty object instead of null
        // assertSolutionAllUpdatablePropertiesEquals(expectedSolution, getPersistedSolution(expectedSolution));
        assertSolutionUpdatableFieldsEquals(expectedSolution, getPersistedSolution(expectedSolution));
    }
}
