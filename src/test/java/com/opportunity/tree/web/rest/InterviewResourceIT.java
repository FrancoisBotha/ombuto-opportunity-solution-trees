package com.opportunity.tree.web.rest;

import static com.opportunity.tree.domain.InterviewAsserts.*;
import static com.opportunity.tree.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opportunity.tree.IntegrationTest;
import com.opportunity.tree.domain.Interview;
import com.opportunity.tree.domain.Product;
import com.opportunity.tree.repository.EntityManager;
import com.opportunity.tree.repository.InterviewRepository;
import com.opportunity.tree.repository.UserRepository;
import com.opportunity.tree.service.InterviewService;
import com.opportunity.tree.service.dto.InterviewDTO;
import com.opportunity.tree.service.mapper.InterviewMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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
 * Integration tests for the {@link InterviewResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureWebTestClient(timeout = IntegrationTest.DEFAULT_ENTITY_TIMEOUT)
@WithMockUser
class InterviewResourceIT {

    private static final String DEFAULT_TITLE = "AAAAAAAAAA";
    private static final String UPDATED_TITLE = "BBBBBBBBBB";

    private static final String DEFAULT_PARTICIPANT = "AAAAAAAAAA";
    private static final String UPDATED_PARTICIPANT = "BBBBBBBBBB";

    private static final LocalDate DEFAULT_INTERVIEW_DATE = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_INTERVIEW_DATE = LocalDate.now(ZoneId.systemDefault());

    private static final String DEFAULT_NOTES = "AAAAAAAAAA";
    private static final String UPDATED_NOTES = "BBBBBBBBBB";

    private static final String DEFAULT_RECORDING_URL = "AAAAAAAAAA";
    private static final String UPDATED_RECORDING_URL = "BBBBBBBBBB";

    private static final Instant DEFAULT_CREATED_DATE = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CREATED_DATE = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/interviews";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2L * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private InterviewRepository interviewRepository;

    @Autowired
    private UserRepository userRepository;

    @Mock
    private InterviewRepository interviewRepositoryMock;

    @Autowired
    private InterviewMapper interviewMapper;

    @Mock
    private InterviewService interviewServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private WebTestClient webTestClient;

    private Interview interview;

    private Interview insertedInterview;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Interview createEntity(EntityManager em) {
        Interview interview = new Interview()
            .title(DEFAULT_TITLE)
            .participant(DEFAULT_PARTICIPANT)
            .interviewDate(DEFAULT_INTERVIEW_DATE)
            .notes(DEFAULT_NOTES)
            .recordingUrl(DEFAULT_RECORDING_URL)
            .createdDate(DEFAULT_CREATED_DATE);
        // Add required entity
        Product product;
        product = em.insert(ProductResourceIT.createEntity(em)).block();
        interview.setProduct(product);
        return interview;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Interview createUpdatedEntity(EntityManager em) {
        Interview updatedInterview = new Interview()
            .title(UPDATED_TITLE)
            .participant(UPDATED_PARTICIPANT)
            .interviewDate(UPDATED_INTERVIEW_DATE)
            .notes(UPDATED_NOTES)
            .recordingUrl(UPDATED_RECORDING_URL)
            .createdDate(UPDATED_CREATED_DATE);
        // Add required entity
        Product product;
        product = em.insert(ProductResourceIT.createUpdatedEntity(em)).block();
        updatedInterview.setProduct(product);
        return updatedInterview;
    }

    public static void deleteEntities(EntityManager em) {
        try {
            em.deleteAll(Interview.class).block();
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
        interview = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedInterview != null) {
            interviewRepository.delete(insertedInterview).block();
            insertedInterview = null;
        }
        deleteEntities(em);
        userRepository.deleteAllUserAuthorities().block();
        userRepository.deleteAll().block();
    }

    @Test
    void createInterview() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Interview
        InterviewDTO interviewDTO = interviewMapper.toDto(interview);
        var returnedInterviewDTO = webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(interviewDTO))
            .exchange()
            .expectStatus()
            .isCreated()
            .expectBody(InterviewDTO.class)
            .returnResult()
            .getResponseBody();

        // Validate the Interview in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedInterview = interviewMapper.toEntity(returnedInterviewDTO);
        assertInterviewUpdatableFieldsEquals(returnedInterview, getPersistedInterview(returnedInterview));

        insertedInterview = returnedInterview;
    }

    @Test
    void createInterviewWithExistingId() throws Exception {
        // Create the Interview with an existing ID
        interview.setId(1L);
        InterviewDTO interviewDTO = interviewMapper.toDto(interview);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(interviewDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Interview in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    void checkTitleIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        interview.setTitle(null);

        // Create the Interview, which fails.
        InterviewDTO interviewDTO = interviewMapper.toDto(interview);

        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(interviewDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    void checkInterviewDateIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        interview.setInterviewDate(null);

        // Create the Interview, which fails.
        InterviewDTO interviewDTO = interviewMapper.toDto(interview);

        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(interviewDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    void checkCreatedDateIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        interview.setCreatedDate(null);

        // Create the Interview, which fails.
        InterviewDTO interviewDTO = interviewMapper.toDto(interview);

        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(interviewDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    void getAllInterviews() {
        // Initialize the database
        insertedInterview = interviewRepository.save(interview).block();

        // Get all the interviewList
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
            .value(hasItem(interview.getId().intValue()))
            .jsonPath("$.[*].title")
            .value(hasItem(DEFAULT_TITLE))
            .jsonPath("$.[*].participant")
            .value(hasItem(DEFAULT_PARTICIPANT))
            .jsonPath("$.[*].interviewDate")
            .value(hasItem(DEFAULT_INTERVIEW_DATE.toString()))
            .jsonPath("$.[*].notes")
            .value(hasItem(DEFAULT_NOTES))
            .jsonPath("$.[*].recordingUrl")
            .value(hasItem(DEFAULT_RECORDING_URL))
            .jsonPath("$.[*].createdDate")
            .value(hasItem(DEFAULT_CREATED_DATE.toString()));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllInterviewsWithEagerRelationshipsIsEnabled() {
        when(interviewServiceMock.findAllWithEagerRelationships(any())).thenReturn(Flux.empty());

        webTestClient.get().uri(ENTITY_API_URL + "?eagerload=true").exchange().expectStatus().isOk();

        verify(interviewServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllInterviewsWithEagerRelationshipsIsNotEnabled() {
        when(interviewServiceMock.findAllWithEagerRelationships(any())).thenReturn(Flux.empty());

        webTestClient.get().uri(ENTITY_API_URL + "?eagerload=false").exchange().expectStatus().isOk();
        verify(interviewRepositoryMock, times(1)).findAllWithEagerRelationships(any());
    }

    @Test
    void getInterview() {
        // Initialize the database
        insertedInterview = interviewRepository.save(interview).block();

        // Get the interview
        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, interview.getId())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.id")
            .value(is(interview.getId().intValue()))
            .jsonPath("$.title")
            .value(is(DEFAULT_TITLE))
            .jsonPath("$.participant")
            .value(is(DEFAULT_PARTICIPANT))
            .jsonPath("$.interviewDate")
            .value(is(DEFAULT_INTERVIEW_DATE.toString()))
            .jsonPath("$.notes")
            .value(is(DEFAULT_NOTES))
            .jsonPath("$.recordingUrl")
            .value(is(DEFAULT_RECORDING_URL))
            .jsonPath("$.createdDate")
            .value(is(DEFAULT_CREATED_DATE.toString()));
    }

    @Test
    void getNonExistingInterview() {
        // Get the interview
        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, Long.MAX_VALUE)
            .accept(MediaType.APPLICATION_PROBLEM_JSON)
            .exchange()
            .expectStatus()
            .isNotFound();
    }

    @Test
    void putExistingInterview() throws Exception {
        // Initialize the database
        insertedInterview = interviewRepository.save(interview).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the interview
        Interview updatedInterview = interviewRepository.findById(interview.getId()).block();
        updatedInterview
            .title(UPDATED_TITLE)
            .participant(UPDATED_PARTICIPANT)
            .interviewDate(UPDATED_INTERVIEW_DATE)
            .notes(UPDATED_NOTES)
            .recordingUrl(UPDATED_RECORDING_URL)
            .createdDate(UPDATED_CREATED_DATE);
        InterviewDTO interviewDTO = interviewMapper.toDto(updatedInterview);

        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, interviewDTO.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(interviewDTO))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Interview in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedInterviewToMatchAllProperties(updatedInterview);
    }

    @Test
    void putNonExistingInterview() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        interview.setId(longCount.incrementAndGet());

        // Create the Interview
        InterviewDTO interviewDTO = interviewMapper.toDto(interview);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, interviewDTO.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(interviewDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Interview in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void putWithIdMismatchInterview() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        interview.setId(longCount.incrementAndGet());

        // Create the Interview
        InterviewDTO interviewDTO = interviewMapper.toDto(interview);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, longCount.incrementAndGet())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(interviewDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Interview in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void putWithMissingIdPathParamInterview() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        interview.setId(longCount.incrementAndGet());

        // Create the Interview
        InterviewDTO interviewDTO = interviewMapper.toDto(interview);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(interviewDTO))
            .exchange()
            .expectStatus()
            .isEqualTo(405);

        // Validate the Interview in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void partialUpdateInterviewWithPatch() throws Exception {
        // Initialize the database
        insertedInterview = interviewRepository.save(interview).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the interview using partial update
        Interview partialUpdatedInterview = new Interview();
        partialUpdatedInterview.setId(interview.getId());

        partialUpdatedInterview.interviewDate(UPDATED_INTERVIEW_DATE).createdDate(UPDATED_CREATED_DATE);

        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, partialUpdatedInterview.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(partialUpdatedInterview))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Interview in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertInterviewUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedInterview, interview),
            getPersistedInterview(interview)
        );
    }

    @Test
    void fullUpdateInterviewWithPatch() throws Exception {
        // Initialize the database
        insertedInterview = interviewRepository.save(interview).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the interview using partial update
        Interview partialUpdatedInterview = new Interview();
        partialUpdatedInterview.setId(interview.getId());

        partialUpdatedInterview
            .title(UPDATED_TITLE)
            .participant(UPDATED_PARTICIPANT)
            .interviewDate(UPDATED_INTERVIEW_DATE)
            .notes(UPDATED_NOTES)
            .recordingUrl(UPDATED_RECORDING_URL)
            .createdDate(UPDATED_CREATED_DATE);

        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, partialUpdatedInterview.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(partialUpdatedInterview))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Interview in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertInterviewUpdatableFieldsEquals(partialUpdatedInterview, getPersistedInterview(partialUpdatedInterview));
    }

    @Test
    void patchNonExistingInterview() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        interview.setId(longCount.incrementAndGet());

        // Create the Interview
        InterviewDTO interviewDTO = interviewMapper.toDto(interview);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, interviewDTO.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(interviewDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Interview in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void patchWithIdMismatchInterview() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        interview.setId(longCount.incrementAndGet());

        // Create the Interview
        InterviewDTO interviewDTO = interviewMapper.toDto(interview);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, longCount.incrementAndGet())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(interviewDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Interview in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void patchWithMissingIdPathParamInterview() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        interview.setId(longCount.incrementAndGet());

        // Create the Interview
        InterviewDTO interviewDTO = interviewMapper.toDto(interview);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(interviewDTO))
            .exchange()
            .expectStatus()
            .isEqualTo(405);

        // Validate the Interview in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void deleteInterview() {
        // Initialize the database
        insertedInterview = interviewRepository.save(interview).block();

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the interview
        webTestClient
            .delete()
            .uri(ENTITY_API_URL_ID, interview.getId())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNoContent();

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return interviewRepository.count().block();
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

    protected Interview getPersistedInterview(Interview interview) {
        return interviewRepository.findById(interview.getId()).block();
    }

    protected void assertPersistedInterviewToMatchAllProperties(Interview expectedInterview) {
        // Test fails because reactive api returns an empty object instead of null
        // assertInterviewAllPropertiesEquals(expectedInterview, getPersistedInterview(expectedInterview));
        assertInterviewUpdatableFieldsEquals(expectedInterview, getPersistedInterview(expectedInterview));
    }

    protected void assertPersistedInterviewToMatchUpdatableProperties(Interview expectedInterview) {
        // Test fails because reactive api returns an empty object instead of null
        // assertInterviewAllUpdatablePropertiesEquals(expectedInterview, getPersistedInterview(expectedInterview));
        assertInterviewUpdatableFieldsEquals(expectedInterview, getPersistedInterview(expectedInterview));
    }
}
