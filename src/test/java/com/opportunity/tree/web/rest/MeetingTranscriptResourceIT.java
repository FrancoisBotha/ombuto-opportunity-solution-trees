package com.opportunity.tree.web.rest;

import static com.opportunity.tree.domain.MeetingTranscriptAsserts.*;
import static com.opportunity.tree.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opportunity.tree.IntegrationTest;
import com.opportunity.tree.domain.Assumption;
import com.opportunity.tree.domain.Evidence;
import com.opportunity.tree.domain.MeetingTranscript;
import com.opportunity.tree.domain.Opportunity;
import com.opportunity.tree.domain.Outcome;
import com.opportunity.tree.domain.Product;
import com.opportunity.tree.domain.Solution;
import com.opportunity.tree.domain.User;
import com.opportunity.tree.domain.enumeration.MeetingTranscriptSource;
import com.opportunity.tree.repository.MeetingTranscriptRepository;
import com.opportunity.tree.repository.UserRepository;
import com.opportunity.tree.service.MeetingTranscriptService;
import com.opportunity.tree.service.dto.MeetingTranscriptDTO;
import com.opportunity.tree.service.mapper.MeetingTranscriptMapper;
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
 * Integration tests for the {@link MeetingTranscriptResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = "ROLE_ADMIN")
class MeetingTranscriptResourceIT {

    private static final String DEFAULT_TITLE = "AAAAAAAAAA";
    private static final String UPDATED_TITLE = "BBBBBBBBBB";

    private static final LocalDate DEFAULT_MEETING_DATE = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_MEETING_DATE = LocalDate.now(ZoneId.systemDefault());
    private static final LocalDate SMALLER_MEETING_DATE = LocalDate.ofEpochDay(-1L);

    private static final String DEFAULT_ATTENDEES = "AAAAAAAAAA";
    private static final String UPDATED_ATTENDEES = "BBBBBBBBBB";

    private static final String DEFAULT_BODY = "AAAAAAAAAA";
    private static final String UPDATED_BODY = "BBBBBBBBBB";

    private static final MeetingTranscriptSource DEFAULT_SOURCE = MeetingTranscriptSource.PASTED;
    private static final MeetingTranscriptSource UPDATED_SOURCE = MeetingTranscriptSource.UPLOADED;

    private static final Instant DEFAULT_CREATED_DATE = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CREATED_DATE = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_EDITED_DATE = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_EDITED_DATE = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/meeting-transcripts";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2L * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private MeetingTranscriptRepository meetingTranscriptRepository;

    @Autowired
    private UserRepository userRepository;

    @Mock
    private MeetingTranscriptRepository meetingTranscriptRepositoryMock;

    @Autowired
    private MeetingTranscriptMapper meetingTranscriptMapper;

    @Mock
    private MeetingTranscriptService meetingTranscriptServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restMeetingTranscriptMockMvc;

    private MeetingTranscript meetingTranscript;

    private MeetingTranscript insertedMeetingTranscript;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static MeetingTranscript createEntity() {
        return new MeetingTranscript()
            .title(DEFAULT_TITLE)
            .meetingDate(DEFAULT_MEETING_DATE)
            .attendees(DEFAULT_ATTENDEES)
            .body(DEFAULT_BODY)
            .source(DEFAULT_SOURCE)
            .createdDate(DEFAULT_CREATED_DATE)
            .editedDate(DEFAULT_EDITED_DATE);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static MeetingTranscript createUpdatedEntity() {
        return new MeetingTranscript()
            .title(UPDATED_TITLE)
            .meetingDate(UPDATED_MEETING_DATE)
            .attendees(UPDATED_ATTENDEES)
            .body(UPDATED_BODY)
            .source(UPDATED_SOURCE)
            .createdDate(UPDATED_CREATED_DATE)
            .editedDate(UPDATED_EDITED_DATE);
    }

    @BeforeEach
    void initTest() {
        meetingTranscript = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedMeetingTranscript != null) {
            meetingTranscriptRepository.delete(insertedMeetingTranscript);
            insertedMeetingTranscript = null;
        }
        userRepository.deleteAll();
    }

    @Test
    @Transactional
    void createMeetingTranscript() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the MeetingTranscript
        MeetingTranscriptDTO meetingTranscriptDTO = meetingTranscriptMapper.toDto(meetingTranscript);
        var returnedMeetingTranscriptDTO = om.readValue(
            restMeetingTranscriptMockMvc
                .perform(
                    post(ENTITY_API_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsBytes(meetingTranscriptDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            MeetingTranscriptDTO.class
        );

        // Validate the MeetingTranscript in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedMeetingTranscript = meetingTranscriptMapper.toEntity(returnedMeetingTranscriptDTO);
        assertMeetingTranscriptUpdatableFieldsEquals(returnedMeetingTranscript, getPersistedMeetingTranscript(returnedMeetingTranscript));

        insertedMeetingTranscript = returnedMeetingTranscript;
    }

    @Test
    @Transactional
    void createMeetingTranscriptWithExistingId() throws Exception {
        // Create the MeetingTranscript with an existing ID
        meetingTranscript.setId(1L);
        MeetingTranscriptDTO meetingTranscriptDTO = meetingTranscriptMapper.toDto(meetingTranscript);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restMeetingTranscriptMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(meetingTranscriptDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the MeetingTranscript in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkTitleIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        meetingTranscript.setTitle(null);

        // Create the MeetingTranscript, which fails.
        MeetingTranscriptDTO meetingTranscriptDTO = meetingTranscriptMapper.toDto(meetingTranscript);

        restMeetingTranscriptMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(meetingTranscriptDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkMeetingDateIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        meetingTranscript.setMeetingDate(null);

        // Create the MeetingTranscript, which fails.
        MeetingTranscriptDTO meetingTranscriptDTO = meetingTranscriptMapper.toDto(meetingTranscript);

        restMeetingTranscriptMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(meetingTranscriptDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkSourceIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        meetingTranscript.setSource(null);

        // Create the MeetingTranscript, which fails.
        MeetingTranscriptDTO meetingTranscriptDTO = meetingTranscriptMapper.toDto(meetingTranscript);

        restMeetingTranscriptMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(meetingTranscriptDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkCreatedDateIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        meetingTranscript.setCreatedDate(null);

        // Create the MeetingTranscript, which fails.
        MeetingTranscriptDTO meetingTranscriptDTO = meetingTranscriptMapper.toDto(meetingTranscript);

        restMeetingTranscriptMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(meetingTranscriptDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllMeetingTranscripts() throws Exception {
        // Initialize the database
        insertedMeetingTranscript = meetingTranscriptRepository.saveAndFlush(meetingTranscript);

        // Get all the meetingTranscriptList
        restMeetingTranscriptMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(meetingTranscript.getId().intValue())))
            .andExpect(jsonPath("$.[*].title").value(hasItem(DEFAULT_TITLE)))
            .andExpect(jsonPath("$.[*].meetingDate").value(hasItem(DEFAULT_MEETING_DATE.toString())))
            .andExpect(jsonPath("$.[*].attendees").value(hasItem(DEFAULT_ATTENDEES)))
            .andExpect(jsonPath("$.[*].body").value(hasItem(DEFAULT_BODY)))
            .andExpect(jsonPath("$.[*].source").value(hasItem(DEFAULT_SOURCE.toString())))
            .andExpect(jsonPath("$.[*].createdDate").value(hasItem(DEFAULT_CREATED_DATE.toString())))
            .andExpect(jsonPath("$.[*].editedDate").value(hasItem(DEFAULT_EDITED_DATE.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllMeetingTranscriptsWithEagerRelationshipsIsEnabled() throws Exception {
        when(meetingTranscriptServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restMeetingTranscriptMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(meetingTranscriptServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllMeetingTranscriptsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(meetingTranscriptServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restMeetingTranscriptMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(meetingTranscriptRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getMeetingTranscript() throws Exception {
        // Initialize the database
        insertedMeetingTranscript = meetingTranscriptRepository.saveAndFlush(meetingTranscript);

        // Get the meetingTranscript
        restMeetingTranscriptMockMvc
            .perform(get(ENTITY_API_URL_ID, meetingTranscript.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(meetingTranscript.getId().intValue()))
            .andExpect(jsonPath("$.title").value(DEFAULT_TITLE))
            .andExpect(jsonPath("$.meetingDate").value(DEFAULT_MEETING_DATE.toString()))
            .andExpect(jsonPath("$.attendees").value(DEFAULT_ATTENDEES))
            .andExpect(jsonPath("$.body").value(DEFAULT_BODY))
            .andExpect(jsonPath("$.source").value(DEFAULT_SOURCE.toString()))
            .andExpect(jsonPath("$.createdDate").value(DEFAULT_CREATED_DATE.toString()))
            .andExpect(jsonPath("$.editedDate").value(DEFAULT_EDITED_DATE.toString()));
    }

    @Test
    @Transactional
    void getMeetingTranscriptsByIdFiltering() throws Exception {
        // Initialize the database
        insertedMeetingTranscript = meetingTranscriptRepository.saveAndFlush(meetingTranscript);

        Long id = meetingTranscript.getId();

        defaultMeetingTranscriptFiltering("id.equals=" + id, "id.notEquals=" + id);

        defaultMeetingTranscriptFiltering("id.greaterThanOrEqual=" + id, "id.greaterThan=" + id);

        defaultMeetingTranscriptFiltering("id.lessThanOrEqual=" + id, "id.lessThan=" + id);
    }

    @Test
    @Transactional
    void getAllMeetingTranscriptsByTitleIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedMeetingTranscript = meetingTranscriptRepository.saveAndFlush(meetingTranscript);

        // Get all the meetingTranscriptList where title equals to
        defaultMeetingTranscriptFiltering("title.equals=" + DEFAULT_TITLE, "title.equals=" + UPDATED_TITLE);
    }

    @Test
    @Transactional
    void getAllMeetingTranscriptsByTitleIsInShouldWork() throws Exception {
        // Initialize the database
        insertedMeetingTranscript = meetingTranscriptRepository.saveAndFlush(meetingTranscript);

        // Get all the meetingTranscriptList where title in
        defaultMeetingTranscriptFiltering("title.in=" + DEFAULT_TITLE + "," + UPDATED_TITLE, "title.in=" + UPDATED_TITLE);
    }

    @Test
    @Transactional
    void getAllMeetingTranscriptsByTitleIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedMeetingTranscript = meetingTranscriptRepository.saveAndFlush(meetingTranscript);

        // Get all the meetingTranscriptList where title is not null
        defaultMeetingTranscriptFiltering("title.specified=true", "title.specified=false");
    }

    @Test
    @Transactional
    void getAllMeetingTranscriptsByTitleContainsSomething() throws Exception {
        // Initialize the database
        insertedMeetingTranscript = meetingTranscriptRepository.saveAndFlush(meetingTranscript);

        // Get all the meetingTranscriptList where title contains
        defaultMeetingTranscriptFiltering("title.contains=" + DEFAULT_TITLE, "title.contains=" + UPDATED_TITLE);
    }

    @Test
    @Transactional
    void getAllMeetingTranscriptsByTitleNotContainsSomething() throws Exception {
        // Initialize the database
        insertedMeetingTranscript = meetingTranscriptRepository.saveAndFlush(meetingTranscript);

        // Get all the meetingTranscriptList where title does not contain
        defaultMeetingTranscriptFiltering("title.doesNotContain=" + UPDATED_TITLE, "title.doesNotContain=" + DEFAULT_TITLE);
    }

    @Test
    @Transactional
    void getAllMeetingTranscriptsByMeetingDateIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedMeetingTranscript = meetingTranscriptRepository.saveAndFlush(meetingTranscript);

        // Get all the meetingTranscriptList where meetingDate equals to
        defaultMeetingTranscriptFiltering("meetingDate.equals=" + DEFAULT_MEETING_DATE, "meetingDate.equals=" + UPDATED_MEETING_DATE);
    }

    @Test
    @Transactional
    void getAllMeetingTranscriptsByMeetingDateIsInShouldWork() throws Exception {
        // Initialize the database
        insertedMeetingTranscript = meetingTranscriptRepository.saveAndFlush(meetingTranscript);

        // Get all the meetingTranscriptList where meetingDate in
        defaultMeetingTranscriptFiltering(
            "meetingDate.in=" + DEFAULT_MEETING_DATE + "," + UPDATED_MEETING_DATE,
            "meetingDate.in=" + UPDATED_MEETING_DATE
        );
    }

    @Test
    @Transactional
    void getAllMeetingTranscriptsByMeetingDateIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedMeetingTranscript = meetingTranscriptRepository.saveAndFlush(meetingTranscript);

        // Get all the meetingTranscriptList where meetingDate is not null
        defaultMeetingTranscriptFiltering("meetingDate.specified=true", "meetingDate.specified=false");
    }

    @Test
    @Transactional
    void getAllMeetingTranscriptsByMeetingDateIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedMeetingTranscript = meetingTranscriptRepository.saveAndFlush(meetingTranscript);

        // Get all the meetingTranscriptList where meetingDate is greater than or equal to
        defaultMeetingTranscriptFiltering(
            "meetingDate.greaterThanOrEqual=" + DEFAULT_MEETING_DATE,
            "meetingDate.greaterThanOrEqual=" + UPDATED_MEETING_DATE
        );
    }

    @Test
    @Transactional
    void getAllMeetingTranscriptsByMeetingDateIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedMeetingTranscript = meetingTranscriptRepository.saveAndFlush(meetingTranscript);

        // Get all the meetingTranscriptList where meetingDate is less than or equal to
        defaultMeetingTranscriptFiltering(
            "meetingDate.lessThanOrEqual=" + DEFAULT_MEETING_DATE,
            "meetingDate.lessThanOrEqual=" + SMALLER_MEETING_DATE
        );
    }

    @Test
    @Transactional
    void getAllMeetingTranscriptsByMeetingDateIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedMeetingTranscript = meetingTranscriptRepository.saveAndFlush(meetingTranscript);

        // Get all the meetingTranscriptList where meetingDate is less than
        defaultMeetingTranscriptFiltering("meetingDate.lessThan=" + UPDATED_MEETING_DATE, "meetingDate.lessThan=" + DEFAULT_MEETING_DATE);
    }

    @Test
    @Transactional
    void getAllMeetingTranscriptsByMeetingDateIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedMeetingTranscript = meetingTranscriptRepository.saveAndFlush(meetingTranscript);

        // Get all the meetingTranscriptList where meetingDate is greater than
        defaultMeetingTranscriptFiltering(
            "meetingDate.greaterThan=" + SMALLER_MEETING_DATE,
            "meetingDate.greaterThan=" + DEFAULT_MEETING_DATE
        );
    }

    @Test
    @Transactional
    void getAllMeetingTranscriptsByAttendeesIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedMeetingTranscript = meetingTranscriptRepository.saveAndFlush(meetingTranscript);

        // Get all the meetingTranscriptList where attendees equals to
        defaultMeetingTranscriptFiltering("attendees.equals=" + DEFAULT_ATTENDEES, "attendees.equals=" + UPDATED_ATTENDEES);
    }

    @Test
    @Transactional
    void getAllMeetingTranscriptsByAttendeesIsInShouldWork() throws Exception {
        // Initialize the database
        insertedMeetingTranscript = meetingTranscriptRepository.saveAndFlush(meetingTranscript);

        // Get all the meetingTranscriptList where attendees in
        defaultMeetingTranscriptFiltering(
            "attendees.in=" + DEFAULT_ATTENDEES + "," + UPDATED_ATTENDEES,
            "attendees.in=" + UPDATED_ATTENDEES
        );
    }

    @Test
    @Transactional
    void getAllMeetingTranscriptsByAttendeesIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedMeetingTranscript = meetingTranscriptRepository.saveAndFlush(meetingTranscript);

        // Get all the meetingTranscriptList where attendees is not null
        defaultMeetingTranscriptFiltering("attendees.specified=true", "attendees.specified=false");
    }

    @Test
    @Transactional
    void getAllMeetingTranscriptsByAttendeesContainsSomething() throws Exception {
        // Initialize the database
        insertedMeetingTranscript = meetingTranscriptRepository.saveAndFlush(meetingTranscript);

        // Get all the meetingTranscriptList where attendees contains
        defaultMeetingTranscriptFiltering("attendees.contains=" + DEFAULT_ATTENDEES, "attendees.contains=" + UPDATED_ATTENDEES);
    }

    @Test
    @Transactional
    void getAllMeetingTranscriptsByAttendeesNotContainsSomething() throws Exception {
        // Initialize the database
        insertedMeetingTranscript = meetingTranscriptRepository.saveAndFlush(meetingTranscript);

        // Get all the meetingTranscriptList where attendees does not contain
        defaultMeetingTranscriptFiltering("attendees.doesNotContain=" + UPDATED_ATTENDEES, "attendees.doesNotContain=" + DEFAULT_ATTENDEES);
    }

    @Test
    @Transactional
    void getAllMeetingTranscriptsBySourceIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedMeetingTranscript = meetingTranscriptRepository.saveAndFlush(meetingTranscript);

        // Get all the meetingTranscriptList where source equals to
        defaultMeetingTranscriptFiltering("source.equals=" + DEFAULT_SOURCE, "source.equals=" + UPDATED_SOURCE);
    }

    @Test
    @Transactional
    void getAllMeetingTranscriptsBySourceIsInShouldWork() throws Exception {
        // Initialize the database
        insertedMeetingTranscript = meetingTranscriptRepository.saveAndFlush(meetingTranscript);

        // Get all the meetingTranscriptList where source in
        defaultMeetingTranscriptFiltering("source.in=" + DEFAULT_SOURCE + "," + UPDATED_SOURCE, "source.in=" + UPDATED_SOURCE);
    }

    @Test
    @Transactional
    void getAllMeetingTranscriptsBySourceIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedMeetingTranscript = meetingTranscriptRepository.saveAndFlush(meetingTranscript);

        // Get all the meetingTranscriptList where source is not null
        defaultMeetingTranscriptFiltering("source.specified=true", "source.specified=false");
    }

    @Test
    @Transactional
    void getAllMeetingTranscriptsByCreatedDateIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedMeetingTranscript = meetingTranscriptRepository.saveAndFlush(meetingTranscript);

        // Get all the meetingTranscriptList where createdDate equals to
        defaultMeetingTranscriptFiltering("createdDate.equals=" + DEFAULT_CREATED_DATE, "createdDate.equals=" + UPDATED_CREATED_DATE);
    }

    @Test
    @Transactional
    void getAllMeetingTranscriptsByCreatedDateIsInShouldWork() throws Exception {
        // Initialize the database
        insertedMeetingTranscript = meetingTranscriptRepository.saveAndFlush(meetingTranscript);

        // Get all the meetingTranscriptList where createdDate in
        defaultMeetingTranscriptFiltering(
            "createdDate.in=" + DEFAULT_CREATED_DATE + "," + UPDATED_CREATED_DATE,
            "createdDate.in=" + UPDATED_CREATED_DATE
        );
    }

    @Test
    @Transactional
    void getAllMeetingTranscriptsByCreatedDateIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedMeetingTranscript = meetingTranscriptRepository.saveAndFlush(meetingTranscript);

        // Get all the meetingTranscriptList where createdDate is not null
        defaultMeetingTranscriptFiltering("createdDate.specified=true", "createdDate.specified=false");
    }

    @Test
    @Transactional
    void getAllMeetingTranscriptsByEditedDateIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedMeetingTranscript = meetingTranscriptRepository.saveAndFlush(meetingTranscript);

        // Get all the meetingTranscriptList where editedDate equals to
        defaultMeetingTranscriptFiltering("editedDate.equals=" + DEFAULT_EDITED_DATE, "editedDate.equals=" + UPDATED_EDITED_DATE);
    }

    @Test
    @Transactional
    void getAllMeetingTranscriptsByEditedDateIsInShouldWork() throws Exception {
        // Initialize the database
        insertedMeetingTranscript = meetingTranscriptRepository.saveAndFlush(meetingTranscript);

        // Get all the meetingTranscriptList where editedDate in
        defaultMeetingTranscriptFiltering(
            "editedDate.in=" + DEFAULT_EDITED_DATE + "," + UPDATED_EDITED_DATE,
            "editedDate.in=" + UPDATED_EDITED_DATE
        );
    }

    @Test
    @Transactional
    void getAllMeetingTranscriptsByEditedDateIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedMeetingTranscript = meetingTranscriptRepository.saveAndFlush(meetingTranscript);

        // Get all the meetingTranscriptList where editedDate is not null
        defaultMeetingTranscriptFiltering("editedDate.specified=true", "editedDate.specified=false");
    }

    @Test
    @Transactional
    void getAllMeetingTranscriptsByAuthorIsEqualToSomething() throws Exception {
        User author;
        if (TestUtil.findAll(em, User.class).isEmpty()) {
            meetingTranscriptRepository.saveAndFlush(meetingTranscript);
            author = UserResourceIT.createEntity();
        } else {
            author = TestUtil.findAll(em, User.class).get(0);
        }
        em.persist(author);
        em.flush();
        meetingTranscript.setAuthor(author);
        meetingTranscriptRepository.saveAndFlush(meetingTranscript);
        String authorId = author.getId();
        // Get all the meetingTranscriptList where author equals to authorId
        defaultMeetingTranscriptShouldBeFound("authorId.equals=" + authorId);

        // Get all the meetingTranscriptList where author equals to "invalid-id"
        defaultMeetingTranscriptShouldNotBeFound("authorId.equals=" + "invalid-id");
    }

    @Test
    @Transactional
    void getAllMeetingTranscriptsByProductIsEqualToSomething() throws Exception {
        Product product;
        if (TestUtil.findAll(em, Product.class).isEmpty()) {
            meetingTranscriptRepository.saveAndFlush(meetingTranscript);
            product = ProductResourceIT.createEntity(em);
        } else {
            product = TestUtil.findAll(em, Product.class).get(0);
        }
        em.persist(product);
        em.flush();
        meetingTranscript.setProduct(product);
        meetingTranscriptRepository.saveAndFlush(meetingTranscript);
        Long productId = product.getId();
        // Get all the meetingTranscriptList where product equals to productId
        defaultMeetingTranscriptShouldBeFound("productId.equals=" + productId);

        // Get all the meetingTranscriptList where product equals to (productId + 1)
        defaultMeetingTranscriptShouldNotBeFound("productId.equals=" + (productId + 1));
    }

    @Test
    @Transactional
    void getAllMeetingTranscriptsByOutcomeIsEqualToSomething() throws Exception {
        Outcome outcome;
        if (TestUtil.findAll(em, Outcome.class).isEmpty()) {
            meetingTranscriptRepository.saveAndFlush(meetingTranscript);
            outcome = OutcomeResourceIT.createEntity(em);
        } else {
            outcome = TestUtil.findAll(em, Outcome.class).get(0);
        }
        em.persist(outcome);
        em.flush();
        meetingTranscript.setOutcome(outcome);
        meetingTranscriptRepository.saveAndFlush(meetingTranscript);
        Long outcomeId = outcome.getId();
        // Get all the meetingTranscriptList where outcome equals to outcomeId
        defaultMeetingTranscriptShouldBeFound("outcomeId.equals=" + outcomeId);

        // Get all the meetingTranscriptList where outcome equals to (outcomeId + 1)
        defaultMeetingTranscriptShouldNotBeFound("outcomeId.equals=" + (outcomeId + 1));
    }

    @Test
    @Transactional
    void getAllMeetingTranscriptsByOpportunityIsEqualToSomething() throws Exception {
        Opportunity opportunity;
        if (TestUtil.findAll(em, Opportunity.class).isEmpty()) {
            meetingTranscriptRepository.saveAndFlush(meetingTranscript);
            opportunity = OpportunityResourceIT.createEntity(em);
        } else {
            opportunity = TestUtil.findAll(em, Opportunity.class).get(0);
        }
        em.persist(opportunity);
        em.flush();
        meetingTranscript.setOpportunity(opportunity);
        meetingTranscriptRepository.saveAndFlush(meetingTranscript);
        Long opportunityId = opportunity.getId();
        // Get all the meetingTranscriptList where opportunity equals to opportunityId
        defaultMeetingTranscriptShouldBeFound("opportunityId.equals=" + opportunityId);

        // Get all the meetingTranscriptList where opportunity equals to (opportunityId + 1)
        defaultMeetingTranscriptShouldNotBeFound("opportunityId.equals=" + (opportunityId + 1));
    }

    @Test
    @Transactional
    void getAllMeetingTranscriptsBySolutionIsEqualToSomething() throws Exception {
        Solution solution;
        if (TestUtil.findAll(em, Solution.class).isEmpty()) {
            meetingTranscriptRepository.saveAndFlush(meetingTranscript);
            solution = SolutionResourceIT.createEntity(em);
        } else {
            solution = TestUtil.findAll(em, Solution.class).get(0);
        }
        em.persist(solution);
        em.flush();
        meetingTranscript.setSolution(solution);
        meetingTranscriptRepository.saveAndFlush(meetingTranscript);
        Long solutionId = solution.getId();
        // Get all the meetingTranscriptList where solution equals to solutionId
        defaultMeetingTranscriptShouldBeFound("solutionId.equals=" + solutionId);

        // Get all the meetingTranscriptList where solution equals to (solutionId + 1)
        defaultMeetingTranscriptShouldNotBeFound("solutionId.equals=" + (solutionId + 1));
    }

    @Test
    @Transactional
    void getAllMeetingTranscriptsByAssumptionIsEqualToSomething() throws Exception {
        Assumption assumption;
        if (TestUtil.findAll(em, Assumption.class).isEmpty()) {
            meetingTranscriptRepository.saveAndFlush(meetingTranscript);
            assumption = AssumptionResourceIT.createEntity(em);
        } else {
            assumption = TestUtil.findAll(em, Assumption.class).get(0);
        }
        em.persist(assumption);
        em.flush();
        meetingTranscript.setAssumption(assumption);
        meetingTranscriptRepository.saveAndFlush(meetingTranscript);
        Long assumptionId = assumption.getId();
        // Get all the meetingTranscriptList where assumption equals to assumptionId
        defaultMeetingTranscriptShouldBeFound("assumptionId.equals=" + assumptionId);

        // Get all the meetingTranscriptList where assumption equals to (assumptionId + 1)
        defaultMeetingTranscriptShouldNotBeFound("assumptionId.equals=" + (assumptionId + 1));
    }

    @Test
    @Transactional
    void getAllMeetingTranscriptsByEvidenceIsEqualToSomething() throws Exception {
        Evidence evidence;
        if (TestUtil.findAll(em, Evidence.class).isEmpty()) {
            meetingTranscriptRepository.saveAndFlush(meetingTranscript);
            evidence = EvidenceResourceIT.createEntity();
        } else {
            evidence = TestUtil.findAll(em, Evidence.class).get(0);
        }
        em.persist(evidence);
        em.flush();
        meetingTranscript.setEvidence(evidence);
        meetingTranscriptRepository.saveAndFlush(meetingTranscript);
        Long evidenceId = evidence.getId();
        // Get all the meetingTranscriptList where evidence equals to evidenceId
        defaultMeetingTranscriptShouldBeFound("evidenceId.equals=" + evidenceId);

        // Get all the meetingTranscriptList where evidence equals to (evidenceId + 1)
        defaultMeetingTranscriptShouldNotBeFound("evidenceId.equals=" + (evidenceId + 1));
    }

    private void defaultMeetingTranscriptFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultMeetingTranscriptShouldBeFound(shouldBeFound);
        defaultMeetingTranscriptShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultMeetingTranscriptShouldBeFound(String filter) throws Exception {
        restMeetingTranscriptMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(meetingTranscript.getId().intValue())))
            .andExpect(jsonPath("$.[*].title").value(hasItem(DEFAULT_TITLE)))
            .andExpect(jsonPath("$.[*].meetingDate").value(hasItem(DEFAULT_MEETING_DATE.toString())))
            .andExpect(jsonPath("$.[*].attendees").value(hasItem(DEFAULT_ATTENDEES)))
            .andExpect(jsonPath("$.[*].body").value(hasItem(DEFAULT_BODY)))
            .andExpect(jsonPath("$.[*].source").value(hasItem(DEFAULT_SOURCE.toString())))
            .andExpect(jsonPath("$.[*].createdDate").value(hasItem(DEFAULT_CREATED_DATE.toString())))
            .andExpect(jsonPath("$.[*].editedDate").value(hasItem(DEFAULT_EDITED_DATE.toString())));

        // Check, that the count call also returns 1
        restMeetingTranscriptMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultMeetingTranscriptShouldNotBeFound(String filter) throws Exception {
        restMeetingTranscriptMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restMeetingTranscriptMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingMeetingTranscript() throws Exception {
        // Get the meetingTranscript
        restMeetingTranscriptMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingMeetingTranscript() throws Exception {
        // Initialize the database
        insertedMeetingTranscript = meetingTranscriptRepository.saveAndFlush(meetingTranscript);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the meetingTranscript
        MeetingTranscript updatedMeetingTranscript = meetingTranscriptRepository.findById(meetingTranscript.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedMeetingTranscript are not directly saved in db
        em.detach(updatedMeetingTranscript);
        updatedMeetingTranscript
            .title(UPDATED_TITLE)
            .meetingDate(UPDATED_MEETING_DATE)
            .attendees(UPDATED_ATTENDEES)
            .body(UPDATED_BODY)
            .source(UPDATED_SOURCE)
            .createdDate(UPDATED_CREATED_DATE)
            .editedDate(UPDATED_EDITED_DATE);
        MeetingTranscriptDTO meetingTranscriptDTO = meetingTranscriptMapper.toDto(updatedMeetingTranscript);

        restMeetingTranscriptMockMvc
            .perform(
                put(ENTITY_API_URL_ID, meetingTranscriptDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(meetingTranscriptDTO))
            )
            .andExpect(status().isOk());

        // Validate the MeetingTranscript in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedMeetingTranscriptToMatchAllProperties(updatedMeetingTranscript);
    }

    @Test
    @Transactional
    void putNonExistingMeetingTranscript() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        meetingTranscript.setId(longCount.incrementAndGet());

        // Create the MeetingTranscript
        MeetingTranscriptDTO meetingTranscriptDTO = meetingTranscriptMapper.toDto(meetingTranscript);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restMeetingTranscriptMockMvc
            .perform(
                put(ENTITY_API_URL_ID, meetingTranscriptDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(meetingTranscriptDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the MeetingTranscript in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchMeetingTranscript() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        meetingTranscript.setId(longCount.incrementAndGet());

        // Create the MeetingTranscript
        MeetingTranscriptDTO meetingTranscriptDTO = meetingTranscriptMapper.toDto(meetingTranscript);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restMeetingTranscriptMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(meetingTranscriptDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the MeetingTranscript in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamMeetingTranscript() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        meetingTranscript.setId(longCount.incrementAndGet());

        // Create the MeetingTranscript
        MeetingTranscriptDTO meetingTranscriptDTO = meetingTranscriptMapper.toDto(meetingTranscript);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restMeetingTranscriptMockMvc
            .perform(
                put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(meetingTranscriptDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the MeetingTranscript in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateMeetingTranscriptWithPatch() throws Exception {
        // Initialize the database
        insertedMeetingTranscript = meetingTranscriptRepository.saveAndFlush(meetingTranscript);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the meetingTranscript using partial update
        MeetingTranscript partialUpdatedMeetingTranscript = new MeetingTranscript();
        partialUpdatedMeetingTranscript.setId(meetingTranscript.getId());

        partialUpdatedMeetingTranscript.title(UPDATED_TITLE).attendees(UPDATED_ATTENDEES).editedDate(UPDATED_EDITED_DATE);

        restMeetingTranscriptMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedMeetingTranscript.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedMeetingTranscript))
            )
            .andExpect(status().isOk());

        // Validate the MeetingTranscript in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertMeetingTranscriptUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedMeetingTranscript, meetingTranscript),
            getPersistedMeetingTranscript(meetingTranscript)
        );
    }

    @Test
    @Transactional
    void fullUpdateMeetingTranscriptWithPatch() throws Exception {
        // Initialize the database
        insertedMeetingTranscript = meetingTranscriptRepository.saveAndFlush(meetingTranscript);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the meetingTranscript using partial update
        MeetingTranscript partialUpdatedMeetingTranscript = new MeetingTranscript();
        partialUpdatedMeetingTranscript.setId(meetingTranscript.getId());

        partialUpdatedMeetingTranscript
            .title(UPDATED_TITLE)
            .meetingDate(UPDATED_MEETING_DATE)
            .attendees(UPDATED_ATTENDEES)
            .body(UPDATED_BODY)
            .source(UPDATED_SOURCE)
            .createdDate(UPDATED_CREATED_DATE)
            .editedDate(UPDATED_EDITED_DATE);

        restMeetingTranscriptMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedMeetingTranscript.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedMeetingTranscript))
            )
            .andExpect(status().isOk());

        // Validate the MeetingTranscript in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertMeetingTranscriptUpdatableFieldsEquals(
            partialUpdatedMeetingTranscript,
            getPersistedMeetingTranscript(partialUpdatedMeetingTranscript)
        );
    }

    @Test
    @Transactional
    void patchNonExistingMeetingTranscript() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        meetingTranscript.setId(longCount.incrementAndGet());

        // Create the MeetingTranscript
        MeetingTranscriptDTO meetingTranscriptDTO = meetingTranscriptMapper.toDto(meetingTranscript);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restMeetingTranscriptMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, meetingTranscriptDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(meetingTranscriptDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the MeetingTranscript in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchMeetingTranscript() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        meetingTranscript.setId(longCount.incrementAndGet());

        // Create the MeetingTranscript
        MeetingTranscriptDTO meetingTranscriptDTO = meetingTranscriptMapper.toDto(meetingTranscript);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restMeetingTranscriptMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(meetingTranscriptDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the MeetingTranscript in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamMeetingTranscript() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        meetingTranscript.setId(longCount.incrementAndGet());

        // Create the MeetingTranscript
        MeetingTranscriptDTO meetingTranscriptDTO = meetingTranscriptMapper.toDto(meetingTranscript);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restMeetingTranscriptMockMvc
            .perform(
                patch(ENTITY_API_URL)
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(meetingTranscriptDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the MeetingTranscript in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteMeetingTranscript() throws Exception {
        // Initialize the database
        insertedMeetingTranscript = meetingTranscriptRepository.saveAndFlush(meetingTranscript);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the meetingTranscript
        restMeetingTranscriptMockMvc
            .perform(delete(ENTITY_API_URL_ID, meetingTranscript.getId()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return meetingTranscriptRepository.count();
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

    protected MeetingTranscript getPersistedMeetingTranscript(MeetingTranscript meetingTranscript) {
        return meetingTranscriptRepository.findById(meetingTranscript.getId()).orElseThrow();
    }

    protected void assertPersistedMeetingTranscriptToMatchAllProperties(MeetingTranscript expectedMeetingTranscript) {
        assertMeetingTranscriptAllPropertiesEquals(expectedMeetingTranscript, getPersistedMeetingTranscript(expectedMeetingTranscript));
    }

    protected void assertPersistedMeetingTranscriptToMatchUpdatableProperties(MeetingTranscript expectedMeetingTranscript) {
        assertMeetingTranscriptAllUpdatablePropertiesEquals(
            expectedMeetingTranscript,
            getPersistedMeetingTranscript(expectedMeetingTranscript)
        );
    }
}
