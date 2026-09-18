package com.opportunity.tree.web.rest;

import static com.opportunity.tree.domain.InterviewAsserts.*;
import static com.opportunity.tree.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opportunity.tree.IntegrationTest;
import com.opportunity.tree.domain.Interview;
import com.opportunity.tree.domain.Opportunity;
import com.opportunity.tree.domain.Product;
import com.opportunity.tree.domain.User;
import com.opportunity.tree.repository.InterviewRepository;
import com.opportunity.tree.repository.UserRepository;
import com.opportunity.tree.service.InterviewService;
import com.opportunity.tree.service.dto.InterviewDTO;
import com.opportunity.tree.service.mapper.InterviewMapper;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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
 * Integration tests for the {@link InterviewResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = "ROLE_ADMIN")
class InterviewResourceIT {

    private static final String DEFAULT_TITLE = "AAAAAAAAAA";
    private static final String UPDATED_TITLE = "BBBBBBBBBB";

    private static final String DEFAULT_PARTICIPANT = "AAAAAAAAAA";
    private static final String UPDATED_PARTICIPANT = "BBBBBBBBBB";

    private static final LocalDate DEFAULT_INTERVIEW_DATE = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_INTERVIEW_DATE = LocalDate.now(ZoneId.systemDefault());
    private static final LocalDate SMALLER_INTERVIEW_DATE = LocalDate.ofEpochDay(-1L);

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
    private MockMvc restInterviewMockMvc;

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
        if (TestUtil.findAll(em, Product.class).isEmpty()) {
            product = ProductResourceIT.createEntity(em);
            em.persist(product);
            em.flush();
        } else {
            product = TestUtil.findAll(em, Product.class).get(0);
        }
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
        if (TestUtil.findAll(em, Product.class).isEmpty()) {
            product = ProductResourceIT.createUpdatedEntity(em);
            em.persist(product);
            em.flush();
        } else {
            product = TestUtil.findAll(em, Product.class).get(0);
        }
        updatedInterview.setProduct(product);
        return updatedInterview;
    }

    @BeforeEach
    void initTest() {
        interview = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedInterview != null) {
            interviewRepository.delete(insertedInterview);
            insertedInterview = null;
        }
        userRepository.deleteAll();
    }

    @Test
    @Transactional
    void createInterview() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Interview
        InterviewDTO interviewDTO = interviewMapper.toDto(interview);
        var returnedInterviewDTO = om.readValue(
            restInterviewMockMvc
                .perform(
                    post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(interviewDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            InterviewDTO.class
        );

        // Validate the Interview in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedInterview = interviewMapper.toEntity(returnedInterviewDTO);
        assertInterviewUpdatableFieldsEquals(returnedInterview, getPersistedInterview(returnedInterview));

        insertedInterview = returnedInterview;
    }

    @Test
    @Transactional
    void createInterviewWithExistingId() throws Exception {
        // Create the Interview with an existing ID
        interview.setId(1L);
        InterviewDTO interviewDTO = interviewMapper.toDto(interview);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restInterviewMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(interviewDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Interview in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkTitleIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        interview.setTitle(null);

        // Create the Interview, which fails.
        InterviewDTO interviewDTO = interviewMapper.toDto(interview);

        restInterviewMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(interviewDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkInterviewDateIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        interview.setInterviewDate(null);

        // Create the Interview, which fails.
        InterviewDTO interviewDTO = interviewMapper.toDto(interview);

        restInterviewMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(interviewDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkCreatedDateIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        interview.setCreatedDate(null);

        // Create the Interview, which fails.
        InterviewDTO interviewDTO = interviewMapper.toDto(interview);

        restInterviewMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(interviewDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllInterviews() throws Exception {
        // Initialize the database
        insertedInterview = interviewRepository.saveAndFlush(interview);

        // Get all the interviewList
        restInterviewMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(interview.getId().intValue())))
            .andExpect(jsonPath("$.[*].title").value(hasItem(DEFAULT_TITLE)))
            .andExpect(jsonPath("$.[*].participant").value(hasItem(DEFAULT_PARTICIPANT)))
            .andExpect(jsonPath("$.[*].interviewDate").value(hasItem(DEFAULT_INTERVIEW_DATE.toString())))
            .andExpect(jsonPath("$.[*].notes").value(hasItem(DEFAULT_NOTES)))
            .andExpect(jsonPath("$.[*].recordingUrl").value(hasItem(DEFAULT_RECORDING_URL)))
            .andExpect(jsonPath("$.[*].createdDate").value(hasItem(DEFAULT_CREATED_DATE.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllInterviewsWithEagerRelationshipsIsEnabled() throws Exception {
        when(interviewServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restInterviewMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(interviewServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllInterviewsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(interviewServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restInterviewMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(interviewRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getInterview() throws Exception {
        // Initialize the database
        insertedInterview = interviewRepository.saveAndFlush(interview);

        // Get the interview
        restInterviewMockMvc
            .perform(get(ENTITY_API_URL_ID, interview.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(interview.getId().intValue()))
            .andExpect(jsonPath("$.title").value(DEFAULT_TITLE))
            .andExpect(jsonPath("$.participant").value(DEFAULT_PARTICIPANT))
            .andExpect(jsonPath("$.interviewDate").value(DEFAULT_INTERVIEW_DATE.toString()))
            .andExpect(jsonPath("$.notes").value(DEFAULT_NOTES))
            .andExpect(jsonPath("$.recordingUrl").value(DEFAULT_RECORDING_URL))
            .andExpect(jsonPath("$.createdDate").value(DEFAULT_CREATED_DATE.toString()));
    }

    @Test
    @Transactional
    void getInterviewsByIdFiltering() throws Exception {
        // Initialize the database
        insertedInterview = interviewRepository.saveAndFlush(interview);

        Long id = interview.getId();

        defaultInterviewFiltering("id.equals=" + id, "id.notEquals=" + id);

        defaultInterviewFiltering("id.greaterThanOrEqual=" + id, "id.greaterThan=" + id);

        defaultInterviewFiltering("id.lessThanOrEqual=" + id, "id.lessThan=" + id);
    }

    @Test
    @Transactional
    void getAllInterviewsByTitleIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedInterview = interviewRepository.saveAndFlush(interview);

        // Get all the interviewList where title equals to
        defaultInterviewFiltering("title.equals=" + DEFAULT_TITLE, "title.equals=" + UPDATED_TITLE);
    }

    @Test
    @Transactional
    void getAllInterviewsByTitleIsInShouldWork() throws Exception {
        // Initialize the database
        insertedInterview = interviewRepository.saveAndFlush(interview);

        // Get all the interviewList where title in
        defaultInterviewFiltering("title.in=" + DEFAULT_TITLE + "," + UPDATED_TITLE, "title.in=" + UPDATED_TITLE);
    }

    @Test
    @Transactional
    void getAllInterviewsByTitleIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedInterview = interviewRepository.saveAndFlush(interview);

        // Get all the interviewList where title is not null
        defaultInterviewFiltering("title.specified=true", "title.specified=false");
    }

    @Test
    @Transactional
    void getAllInterviewsByTitleContainsSomething() throws Exception {
        // Initialize the database
        insertedInterview = interviewRepository.saveAndFlush(interview);

        // Get all the interviewList where title contains
        defaultInterviewFiltering("title.contains=" + DEFAULT_TITLE, "title.contains=" + UPDATED_TITLE);
    }

    @Test
    @Transactional
    void getAllInterviewsByTitleNotContainsSomething() throws Exception {
        // Initialize the database
        insertedInterview = interviewRepository.saveAndFlush(interview);

        // Get all the interviewList where title does not contain
        defaultInterviewFiltering("title.doesNotContain=" + UPDATED_TITLE, "title.doesNotContain=" + DEFAULT_TITLE);
    }

    @Test
    @Transactional
    void getAllInterviewsByParticipantIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedInterview = interviewRepository.saveAndFlush(interview);

        // Get all the interviewList where participant equals to
        defaultInterviewFiltering("participant.equals=" + DEFAULT_PARTICIPANT, "participant.equals=" + UPDATED_PARTICIPANT);
    }

    @Test
    @Transactional
    void getAllInterviewsByParticipantIsInShouldWork() throws Exception {
        // Initialize the database
        insertedInterview = interviewRepository.saveAndFlush(interview);

        // Get all the interviewList where participant in
        defaultInterviewFiltering(
            "participant.in=" + DEFAULT_PARTICIPANT + "," + UPDATED_PARTICIPANT,
            "participant.in=" + UPDATED_PARTICIPANT
        );
    }

    @Test
    @Transactional
    void getAllInterviewsByParticipantIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedInterview = interviewRepository.saveAndFlush(interview);

        // Get all the interviewList where participant is not null
        defaultInterviewFiltering("participant.specified=true", "participant.specified=false");
    }

    @Test
    @Transactional
    void getAllInterviewsByParticipantContainsSomething() throws Exception {
        // Initialize the database
        insertedInterview = interviewRepository.saveAndFlush(interview);

        // Get all the interviewList where participant contains
        defaultInterviewFiltering("participant.contains=" + DEFAULT_PARTICIPANT, "participant.contains=" + UPDATED_PARTICIPANT);
    }

    @Test
    @Transactional
    void getAllInterviewsByParticipantNotContainsSomething() throws Exception {
        // Initialize the database
        insertedInterview = interviewRepository.saveAndFlush(interview);

        // Get all the interviewList where participant does not contain
        defaultInterviewFiltering("participant.doesNotContain=" + UPDATED_PARTICIPANT, "participant.doesNotContain=" + DEFAULT_PARTICIPANT);
    }

    @Test
    @Transactional
    void getAllInterviewsByInterviewDateIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedInterview = interviewRepository.saveAndFlush(interview);

        // Get all the interviewList where interviewDate equals to
        defaultInterviewFiltering("interviewDate.equals=" + DEFAULT_INTERVIEW_DATE, "interviewDate.equals=" + UPDATED_INTERVIEW_DATE);
    }

    @Test
    @Transactional
    void getAllInterviewsByInterviewDateIsInShouldWork() throws Exception {
        // Initialize the database
        insertedInterview = interviewRepository.saveAndFlush(interview);

        // Get all the interviewList where interviewDate in
        defaultInterviewFiltering(
            "interviewDate.in=" + DEFAULT_INTERVIEW_DATE + "," + UPDATED_INTERVIEW_DATE,
            "interviewDate.in=" + UPDATED_INTERVIEW_DATE
        );
    }

    @Test
    @Transactional
    void getAllInterviewsByInterviewDateIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedInterview = interviewRepository.saveAndFlush(interview);

        // Get all the interviewList where interviewDate is not null
        defaultInterviewFiltering("interviewDate.specified=true", "interviewDate.specified=false");
    }

    @Test
    @Transactional
    void getAllInterviewsByInterviewDateIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedInterview = interviewRepository.saveAndFlush(interview);

        // Get all the interviewList where interviewDate is greater than or equal to
        defaultInterviewFiltering(
            "interviewDate.greaterThanOrEqual=" + DEFAULT_INTERVIEW_DATE,
            "interviewDate.greaterThanOrEqual=" + UPDATED_INTERVIEW_DATE
        );
    }

    @Test
    @Transactional
    void getAllInterviewsByInterviewDateIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedInterview = interviewRepository.saveAndFlush(interview);

        // Get all the interviewList where interviewDate is less than or equal to
        defaultInterviewFiltering(
            "interviewDate.lessThanOrEqual=" + DEFAULT_INTERVIEW_DATE,
            "interviewDate.lessThanOrEqual=" + SMALLER_INTERVIEW_DATE
        );
    }

    @Test
    @Transactional
    void getAllInterviewsByInterviewDateIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedInterview = interviewRepository.saveAndFlush(interview);

        // Get all the interviewList where interviewDate is less than
        defaultInterviewFiltering("interviewDate.lessThan=" + UPDATED_INTERVIEW_DATE, "interviewDate.lessThan=" + DEFAULT_INTERVIEW_DATE);
    }

    @Test
    @Transactional
    void getAllInterviewsByInterviewDateIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedInterview = interviewRepository.saveAndFlush(interview);

        // Get all the interviewList where interviewDate is greater than
        defaultInterviewFiltering(
            "interviewDate.greaterThan=" + SMALLER_INTERVIEW_DATE,
            "interviewDate.greaterThan=" + DEFAULT_INTERVIEW_DATE
        );
    }

    @Test
    @Transactional
    void getAllInterviewsByRecordingUrlIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedInterview = interviewRepository.saveAndFlush(interview);

        // Get all the interviewList where recordingUrl equals to
        defaultInterviewFiltering("recordingUrl.equals=" + DEFAULT_RECORDING_URL, "recordingUrl.equals=" + UPDATED_RECORDING_URL);
    }

    @Test
    @Transactional
    void getAllInterviewsByRecordingUrlIsInShouldWork() throws Exception {
        // Initialize the database
        insertedInterview = interviewRepository.saveAndFlush(interview);

        // Get all the interviewList where recordingUrl in
        defaultInterviewFiltering(
            "recordingUrl.in=" + DEFAULT_RECORDING_URL + "," + UPDATED_RECORDING_URL,
            "recordingUrl.in=" + UPDATED_RECORDING_URL
        );
    }

    @Test
    @Transactional
    void getAllInterviewsByRecordingUrlIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedInterview = interviewRepository.saveAndFlush(interview);

        // Get all the interviewList where recordingUrl is not null
        defaultInterviewFiltering("recordingUrl.specified=true", "recordingUrl.specified=false");
    }

    @Test
    @Transactional
    void getAllInterviewsByRecordingUrlContainsSomething() throws Exception {
        // Initialize the database
        insertedInterview = interviewRepository.saveAndFlush(interview);

        // Get all the interviewList where recordingUrl contains
        defaultInterviewFiltering("recordingUrl.contains=" + DEFAULT_RECORDING_URL, "recordingUrl.contains=" + UPDATED_RECORDING_URL);
    }

    @Test
    @Transactional
    void getAllInterviewsByRecordingUrlNotContainsSomething() throws Exception {
        // Initialize the database
        insertedInterview = interviewRepository.saveAndFlush(interview);

        // Get all the interviewList where recordingUrl does not contain
        defaultInterviewFiltering(
            "recordingUrl.doesNotContain=" + UPDATED_RECORDING_URL,
            "recordingUrl.doesNotContain=" + DEFAULT_RECORDING_URL
        );
    }

    @Test
    @Transactional
    void getAllInterviewsByCreatedDateIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedInterview = interviewRepository.saveAndFlush(interview);

        // Get all the interviewList where createdDate equals to
        defaultInterviewFiltering("createdDate.equals=" + DEFAULT_CREATED_DATE, "createdDate.equals=" + UPDATED_CREATED_DATE);
    }

    @Test
    @Transactional
    void getAllInterviewsByCreatedDateIsInShouldWork() throws Exception {
        // Initialize the database
        insertedInterview = interviewRepository.saveAndFlush(interview);

        // Get all the interviewList where createdDate in
        defaultInterviewFiltering(
            "createdDate.in=" + DEFAULT_CREATED_DATE + "," + UPDATED_CREATED_DATE,
            "createdDate.in=" + UPDATED_CREATED_DATE
        );
    }

    @Test
    @Transactional
    void getAllInterviewsByCreatedDateIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedInterview = interviewRepository.saveAndFlush(interview);

        // Get all the interviewList where createdDate is not null
        defaultInterviewFiltering("createdDate.specified=true", "createdDate.specified=false");
    }

    @Test
    @Transactional
    void getAllInterviewsByProductIsEqualToSomething() throws Exception {
        Product product;
        if (TestUtil.findAll(em, Product.class).isEmpty()) {
            interviewRepository.saveAndFlush(interview);
            product = ProductResourceIT.createEntity(em);
        } else {
            product = TestUtil.findAll(em, Product.class).get(0);
        }
        em.persist(product);
        em.flush();
        interview.setProduct(product);
        interviewRepository.saveAndFlush(interview);
        Long productId = product.getId();
        // Get all the interviewList where product equals to productId
        defaultInterviewShouldBeFound("productId.equals=" + productId);

        // Get all the interviewList where product equals to (productId + 1)
        defaultInterviewShouldNotBeFound("productId.equals=" + (productId + 1));
    }

    @Test
    @Transactional
    void getAllInterviewsByInterviewerIsEqualToSomething() throws Exception {
        User interviewer;
        if (TestUtil.findAll(em, User.class).isEmpty()) {
            interviewRepository.saveAndFlush(interview);
            interviewer = UserResourceIT.createEntity();
        } else {
            interviewer = TestUtil.findAll(em, User.class).get(0);
        }
        em.persist(interviewer);
        em.flush();
        interview.setInterviewer(interviewer);
        interviewRepository.saveAndFlush(interview);
        String interviewerId = interviewer.getId();
        // Get all the interviewList where interviewer equals to interviewerId
        defaultInterviewShouldBeFound("interviewerId.equals=" + interviewerId);

        // Get all the interviewList where interviewer equals to "invalid-id"
        defaultInterviewShouldNotBeFound("interviewerId.equals=" + "invalid-id");
    }

    @Test
    @Transactional
    void getAllInterviewsByOpportunityIsEqualToSomething() throws Exception {
        Opportunity opportunity;
        if (TestUtil.findAll(em, Opportunity.class).isEmpty()) {
            interviewRepository.saveAndFlush(interview);
            opportunity = OpportunityResourceIT.createEntity(em);
        } else {
            opportunity = TestUtil.findAll(em, Opportunity.class).get(0);
        }
        em.persist(opportunity);
        em.flush();
        interview.addOpportunity(opportunity);
        interviewRepository.saveAndFlush(interview);
        Long opportunityId = opportunity.getId();
        // Get all the interviewList where opportunity equals to opportunityId
        defaultInterviewShouldBeFound("opportunityId.equals=" + opportunityId);

        // Get all the interviewList where opportunity equals to (opportunityId + 1)
        defaultInterviewShouldNotBeFound("opportunityId.equals=" + (opportunityId + 1));
    }

    private void defaultInterviewFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultInterviewShouldBeFound(shouldBeFound);
        defaultInterviewShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultInterviewShouldBeFound(String filter) throws Exception {
        restInterviewMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(interview.getId().intValue())))
            .andExpect(jsonPath("$.[*].title").value(hasItem(DEFAULT_TITLE)))
            .andExpect(jsonPath("$.[*].participant").value(hasItem(DEFAULT_PARTICIPANT)))
            .andExpect(jsonPath("$.[*].interviewDate").value(hasItem(DEFAULT_INTERVIEW_DATE.toString())))
            .andExpect(jsonPath("$.[*].notes").value(hasItem(DEFAULT_NOTES)))
            .andExpect(jsonPath("$.[*].recordingUrl").value(hasItem(DEFAULT_RECORDING_URL)))
            .andExpect(jsonPath("$.[*].createdDate").value(hasItem(DEFAULT_CREATED_DATE.toString())));

        // Check, that the count call also returns 1
        restInterviewMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultInterviewShouldNotBeFound(String filter) throws Exception {
        restInterviewMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restInterviewMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingInterview() throws Exception {
        // Get the interview
        restInterviewMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingInterview() throws Exception {
        // Initialize the database
        insertedInterview = interviewRepository.saveAndFlush(interview);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the interview
        Interview updatedInterview = interviewRepository.findById(interview.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedInterview are not directly saved in db
        em.detach(updatedInterview);
        updatedInterview
            .title(UPDATED_TITLE)
            .participant(UPDATED_PARTICIPANT)
            .interviewDate(UPDATED_INTERVIEW_DATE)
            .notes(UPDATED_NOTES)
            .recordingUrl(UPDATED_RECORDING_URL)
            .createdDate(UPDATED_CREATED_DATE);
        InterviewDTO interviewDTO = interviewMapper.toDto(updatedInterview);

        restInterviewMockMvc
            .perform(
                put(ENTITY_API_URL_ID, interviewDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(interviewDTO))
            )
            .andExpect(status().isOk());

        // Validate the Interview in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedInterviewToMatchAllProperties(updatedInterview);
    }

    @Test
    @Transactional
    void putNonExistingInterview() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        interview.setId(longCount.incrementAndGet());

        // Create the Interview
        InterviewDTO interviewDTO = interviewMapper.toDto(interview);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restInterviewMockMvc
            .perform(
                put(ENTITY_API_URL_ID, interviewDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(interviewDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Interview in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchInterview() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        interview.setId(longCount.incrementAndGet());

        // Create the Interview
        InterviewDTO interviewDTO = interviewMapper.toDto(interview);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restInterviewMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(interviewDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Interview in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamInterview() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        interview.setId(longCount.incrementAndGet());

        // Create the Interview
        InterviewDTO interviewDTO = interviewMapper.toDto(interview);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restInterviewMockMvc
            .perform(put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(interviewDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Interview in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateInterviewWithPatch() throws Exception {
        // Initialize the database
        insertedInterview = interviewRepository.saveAndFlush(interview);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the interview using partial update
        Interview partialUpdatedInterview = new Interview();
        partialUpdatedInterview.setId(interview.getId());

        partialUpdatedInterview.participant(UPDATED_PARTICIPANT).notes(UPDATED_NOTES);

        restInterviewMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedInterview.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedInterview))
            )
            .andExpect(status().isOk());

        // Validate the Interview in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertInterviewUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedInterview, interview),
            getPersistedInterview(interview)
        );
    }

    @Test
    @Transactional
    void fullUpdateInterviewWithPatch() throws Exception {
        // Initialize the database
        insertedInterview = interviewRepository.saveAndFlush(interview);

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

        restInterviewMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedInterview.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedInterview))
            )
            .andExpect(status().isOk());

        // Validate the Interview in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertInterviewUpdatableFieldsEquals(partialUpdatedInterview, getPersistedInterview(partialUpdatedInterview));
    }

    @Test
    @Transactional
    void patchNonExistingInterview() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        interview.setId(longCount.incrementAndGet());

        // Create the Interview
        InterviewDTO interviewDTO = interviewMapper.toDto(interview);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restInterviewMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, interviewDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(interviewDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Interview in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchInterview() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        interview.setId(longCount.incrementAndGet());

        // Create the Interview
        InterviewDTO interviewDTO = interviewMapper.toDto(interview);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restInterviewMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(interviewDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Interview in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamInterview() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        interview.setId(longCount.incrementAndGet());

        // Create the Interview
        InterviewDTO interviewDTO = interviewMapper.toDto(interview);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restInterviewMockMvc
            .perform(
                patch(ENTITY_API_URL).with(csrf()).contentType("application/merge-patch+json").content(om.writeValueAsBytes(interviewDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the Interview in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteInterview() throws Exception {
        // Initialize the database
        insertedInterview = interviewRepository.saveAndFlush(interview);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the interview
        restInterviewMockMvc
            .perform(delete(ENTITY_API_URL_ID, interview.getId()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return interviewRepository.count();
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
        return interviewRepository.findById(interview.getId()).orElseThrow();
    }

    protected void assertPersistedInterviewToMatchAllProperties(Interview expectedInterview) {
        assertInterviewAllPropertiesEquals(expectedInterview, getPersistedInterview(expectedInterview));
    }

    protected void assertPersistedInterviewToMatchUpdatableProperties(Interview expectedInterview) {
        assertInterviewAllUpdatablePropertiesEquals(expectedInterview, getPersistedInterview(expectedInterview));
    }
}
