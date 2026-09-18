package com.opportunity.tree.web.rest;

import static com.opportunity.tree.domain.OutcomeAsserts.*;
import static com.opportunity.tree.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opportunity.tree.IntegrationTest;
import com.opportunity.tree.domain.Outcome;
import com.opportunity.tree.domain.Product;
import com.opportunity.tree.domain.TeamMember;
import com.opportunity.tree.domain.User;
import com.opportunity.tree.domain.enumeration.OutcomeStatus;
import com.opportunity.tree.domain.enumeration.TeamRole;
import com.opportunity.tree.repository.OutcomeRepository;
import com.opportunity.tree.repository.TeamMemberRepository;
import com.opportunity.tree.repository.UserRepository;
import com.opportunity.tree.service.OutcomeService;
import com.opportunity.tree.service.dto.OutcomeDTO;
import com.opportunity.tree.service.mapper.OutcomeMapper;
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
 * Integration tests for the {@link OutcomeResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = "ROLE_ADMIN")
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
    private static final LocalDate SMALLER_START_DATE = LocalDate.ofEpochDay(-1L);

    private static final LocalDate DEFAULT_TARGET_DATE = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_TARGET_DATE = LocalDate.now(ZoneId.systemDefault());
    private static final LocalDate SMALLER_TARGET_DATE = LocalDate.ofEpochDay(-1L);

    private static final Integer DEFAULT_SORT_ORDER = 1;
    private static final Integer UPDATED_SORT_ORDER = 2;
    private static final Integer SMALLER_SORT_ORDER = 1 - 1;

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

    @Autowired
    private TeamMemberRepository teamMemberRepository;

    private TeamMember insertedMembership;

    @Mock
    private OutcomeRepository outcomeRepositoryMock;

    @Autowired
    private OutcomeMapper outcomeMapper;

    @Mock
    private OutcomeService outcomeServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restOutcomeMockMvc;

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
        if (TestUtil.findAll(em, Product.class).isEmpty()) {
            product = ProductResourceIT.createEntity(em);
            em.persist(product);
            em.flush();
        } else {
            product = TestUtil.findAll(em, Product.class).get(0);
        }
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
        if (TestUtil.findAll(em, Product.class).isEmpty()) {
            product = ProductResourceIT.createUpdatedEntity(em);
            em.persist(product);
            em.flush();
        } else {
            product = TestUtil.findAll(em, Product.class).get(0);
        }
        updatedOutcome.setProduct(product);
        return updatedOutcome;
    }

    @BeforeEach
    void initTest() {
        outcome = createEntity(em);
        // The mock user ("user" — the @WithMockUser default) must be an OWNER of the
        // outcome's team so the team-scoped access checks in OutcomeServiceImpl let
        // the generated CRUD calls through. Without this seed every write would 403.
        User user = userRepository
            .findOneByLogin("user")
            .orElseGet(() -> {
                User u = UserResourceIT.createEntity();
                u.setLogin("user");
                em.persist(u);
                em.flush();
                return u;
            });
        TeamMember membership = new TeamMember().role(TeamRole.OWNER).joinedDate(Instant.now());
        membership.setTeam(outcome.getProduct().getTeam());
        membership.setUser(user);
        em.persist(membership);
        em.flush();
        insertedMembership = membership;
    }

    @AfterEach
    void cleanup() {
        if (insertedOutcome != null) {
            outcomeRepository.delete(insertedOutcome);
            insertedOutcome = null;
        }
        if (insertedMembership != null) {
            teamMemberRepository.delete(insertedMembership);
            insertedMembership = null;
        }
        userRepository.deleteAll();
    }

    @Test
    @Transactional
    void createOutcome() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Outcome
        OutcomeDTO outcomeDTO = outcomeMapper.toDto(outcome);
        var returnedOutcomeDTO = om.readValue(
            restOutcomeMockMvc
                .perform(
                    post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(outcomeDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            OutcomeDTO.class
        );

        // Validate the Outcome in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedOutcome = outcomeMapper.toEntity(returnedOutcomeDTO);
        assertOutcomeUpdatableFieldsEquals(returnedOutcome, getPersistedOutcome(returnedOutcome));

        insertedOutcome = returnedOutcome;
    }

    @Test
    @Transactional
    void createOutcomeWithExistingId() throws Exception {
        // Create the Outcome with an existing ID
        outcome.setId(1L);
        OutcomeDTO outcomeDTO = outcomeMapper.toDto(outcome);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restOutcomeMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(outcomeDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Outcome in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkTitleIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        outcome.setTitle(null);

        // Create the Outcome, which fails.
        OutcomeDTO outcomeDTO = outcomeMapper.toDto(outcome);

        restOutcomeMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(outcomeDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkStatusIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        outcome.setStatus(null);

        // Create the Outcome, which fails.
        OutcomeDTO outcomeDTO = outcomeMapper.toDto(outcome);

        restOutcomeMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(outcomeDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void createOutcomeIgnoresClientSuppliedServerSetFields() throws Exception {
        // An existing sibling, so append-last is observable
        Outcome sibling = outcomeRepository.saveAndFlush(createEntity(em).sortOrder(7));

        // sortOrder, createdDate and lastModifiedDate are server-set (TREE-002)
        outcome.sortOrder(99).createdDate(DEFAULT_CREATED_DATE).lastModifiedDate(DEFAULT_LAST_MODIFIED_DATE);
        OutcomeDTO outcomeDTO = outcomeMapper.toDto(outcome);

        var returnedOutcomeDTO = om.readValue(
            restOutcomeMockMvc
                .perform(
                    post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(outcomeDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            OutcomeDTO.class
        );

        Outcome persisted = outcomeRepository.findById(returnedOutcomeDTO.getId()).orElseThrow();
        assertThat(persisted.getSortOrder()).isEqualTo(sibling.getSortOrder() + 1);
        assertThat(persisted.getCreatedDate()).isAfter(DEFAULT_CREATED_DATE);
        assertThat(persisted.getLastModifiedDate()).isEqualTo(persisted.getCreatedDate());
    }

    @Test
    @Transactional
    void createOutcomeWithoutServerSetFields() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        outcome.sortOrder(null).createdDate(null).lastModifiedDate(null);
        OutcomeDTO outcomeDTO = outcomeMapper.toDto(outcome);

        restOutcomeMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(outcomeDTO)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.sortOrder").isNumber())
            .andExpect(jsonPath("$.createdDate").isNotEmpty())
            .andExpect(jsonPath("$.lastModifiedDate").isNotEmpty());

        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void getAllOutcomes() throws Exception {
        // Initialize the database
        insertedOutcome = outcomeRepository.saveAndFlush(outcome);

        // Get all the outcomeList
        restOutcomeMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(outcome.getId().intValue())))
            .andExpect(jsonPath("$.[*].title").value(hasItem(DEFAULT_TITLE)))
            .andExpect(jsonPath("$.[*].description").value(hasItem(DEFAULT_DESCRIPTION)))
            .andExpect(jsonPath("$.[*].metric").value(hasItem(DEFAULT_METRIC)))
            .andExpect(jsonPath("$.[*].targetValue").value(hasItem(DEFAULT_TARGET_VALUE)))
            .andExpect(jsonPath("$.[*].currentValue").value(hasItem(DEFAULT_CURRENT_VALUE)))
            .andExpect(jsonPath("$.[*].status").value(hasItem(DEFAULT_STATUS.toString())))
            .andExpect(jsonPath("$.[*].startDate").value(hasItem(DEFAULT_START_DATE.toString())))
            .andExpect(jsonPath("$.[*].targetDate").value(hasItem(DEFAULT_TARGET_DATE.toString())))
            .andExpect(jsonPath("$.[*].sortOrder").value(hasItem(DEFAULT_SORT_ORDER)))
            .andExpect(jsonPath("$.[*].createdDate").value(hasItem(DEFAULT_CREATED_DATE.toString())))
            .andExpect(jsonPath("$.[*].lastModifiedDate").value(hasItem(DEFAULT_LAST_MODIFIED_DATE.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllOutcomesWithEagerRelationshipsIsEnabled() throws Exception {
        when(outcomeServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restOutcomeMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(outcomeServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllOutcomesWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(outcomeServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restOutcomeMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(outcomeRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getOutcome() throws Exception {
        // Initialize the database
        insertedOutcome = outcomeRepository.saveAndFlush(outcome);

        // Get the outcome
        restOutcomeMockMvc
            .perform(get(ENTITY_API_URL_ID, outcome.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(outcome.getId().intValue()))
            .andExpect(jsonPath("$.title").value(DEFAULT_TITLE))
            .andExpect(jsonPath("$.description").value(DEFAULT_DESCRIPTION))
            .andExpect(jsonPath("$.metric").value(DEFAULT_METRIC))
            .andExpect(jsonPath("$.targetValue").value(DEFAULT_TARGET_VALUE))
            .andExpect(jsonPath("$.currentValue").value(DEFAULT_CURRENT_VALUE))
            .andExpect(jsonPath("$.status").value(DEFAULT_STATUS.toString()))
            .andExpect(jsonPath("$.startDate").value(DEFAULT_START_DATE.toString()))
            .andExpect(jsonPath("$.targetDate").value(DEFAULT_TARGET_DATE.toString()))
            .andExpect(jsonPath("$.sortOrder").value(DEFAULT_SORT_ORDER))
            .andExpect(jsonPath("$.createdDate").value(DEFAULT_CREATED_DATE.toString()))
            .andExpect(jsonPath("$.lastModifiedDate").value(DEFAULT_LAST_MODIFIED_DATE.toString()));
    }

    @Test
    @Transactional
    void getOutcomesByIdFiltering() throws Exception {
        // Initialize the database
        insertedOutcome = outcomeRepository.saveAndFlush(outcome);

        Long id = outcome.getId();

        defaultOutcomeFiltering("id.equals=" + id, "id.notEquals=" + id);

        defaultOutcomeFiltering("id.greaterThanOrEqual=" + id, "id.greaterThan=" + id);

        defaultOutcomeFiltering("id.lessThanOrEqual=" + id, "id.lessThan=" + id);
    }

    @Test
    @Transactional
    void getAllOutcomesByTitleIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedOutcome = outcomeRepository.saveAndFlush(outcome);

        // Get all the outcomeList where title equals to
        defaultOutcomeFiltering("title.equals=" + DEFAULT_TITLE, "title.equals=" + UPDATED_TITLE);
    }

    @Test
    @Transactional
    void getAllOutcomesByTitleIsInShouldWork() throws Exception {
        // Initialize the database
        insertedOutcome = outcomeRepository.saveAndFlush(outcome);

        // Get all the outcomeList where title in
        defaultOutcomeFiltering("title.in=" + DEFAULT_TITLE + "," + UPDATED_TITLE, "title.in=" + UPDATED_TITLE);
    }

    @Test
    @Transactional
    void getAllOutcomesByTitleIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedOutcome = outcomeRepository.saveAndFlush(outcome);

        // Get all the outcomeList where title is not null
        defaultOutcomeFiltering("title.specified=true", "title.specified=false");
    }

    @Test
    @Transactional
    void getAllOutcomesByTitleContainsSomething() throws Exception {
        // Initialize the database
        insertedOutcome = outcomeRepository.saveAndFlush(outcome);

        // Get all the outcomeList where title contains
        defaultOutcomeFiltering("title.contains=" + DEFAULT_TITLE, "title.contains=" + UPDATED_TITLE);
    }

    @Test
    @Transactional
    void getAllOutcomesByTitleNotContainsSomething() throws Exception {
        // Initialize the database
        insertedOutcome = outcomeRepository.saveAndFlush(outcome);

        // Get all the outcomeList where title does not contain
        defaultOutcomeFiltering("title.doesNotContain=" + UPDATED_TITLE, "title.doesNotContain=" + DEFAULT_TITLE);
    }

    @Test
    @Transactional
    void getAllOutcomesByMetricIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedOutcome = outcomeRepository.saveAndFlush(outcome);

        // Get all the outcomeList where metric equals to
        defaultOutcomeFiltering("metric.equals=" + DEFAULT_METRIC, "metric.equals=" + UPDATED_METRIC);
    }

    @Test
    @Transactional
    void getAllOutcomesByMetricIsInShouldWork() throws Exception {
        // Initialize the database
        insertedOutcome = outcomeRepository.saveAndFlush(outcome);

        // Get all the outcomeList where metric in
        defaultOutcomeFiltering("metric.in=" + DEFAULT_METRIC + "," + UPDATED_METRIC, "metric.in=" + UPDATED_METRIC);
    }

    @Test
    @Transactional
    void getAllOutcomesByMetricIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedOutcome = outcomeRepository.saveAndFlush(outcome);

        // Get all the outcomeList where metric is not null
        defaultOutcomeFiltering("metric.specified=true", "metric.specified=false");
    }

    @Test
    @Transactional
    void getAllOutcomesByMetricContainsSomething() throws Exception {
        // Initialize the database
        insertedOutcome = outcomeRepository.saveAndFlush(outcome);

        // Get all the outcomeList where metric contains
        defaultOutcomeFiltering("metric.contains=" + DEFAULT_METRIC, "metric.contains=" + UPDATED_METRIC);
    }

    @Test
    @Transactional
    void getAllOutcomesByMetricNotContainsSomething() throws Exception {
        // Initialize the database
        insertedOutcome = outcomeRepository.saveAndFlush(outcome);

        // Get all the outcomeList where metric does not contain
        defaultOutcomeFiltering("metric.doesNotContain=" + UPDATED_METRIC, "metric.doesNotContain=" + DEFAULT_METRIC);
    }

    @Test
    @Transactional
    void getAllOutcomesByTargetValueIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedOutcome = outcomeRepository.saveAndFlush(outcome);

        // Get all the outcomeList where targetValue equals to
        defaultOutcomeFiltering("targetValue.equals=" + DEFAULT_TARGET_VALUE, "targetValue.equals=" + UPDATED_TARGET_VALUE);
    }

    @Test
    @Transactional
    void getAllOutcomesByTargetValueIsInShouldWork() throws Exception {
        // Initialize the database
        insertedOutcome = outcomeRepository.saveAndFlush(outcome);

        // Get all the outcomeList where targetValue in
        defaultOutcomeFiltering(
            "targetValue.in=" + DEFAULT_TARGET_VALUE + "," + UPDATED_TARGET_VALUE,
            "targetValue.in=" + UPDATED_TARGET_VALUE
        );
    }

    @Test
    @Transactional
    void getAllOutcomesByTargetValueIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedOutcome = outcomeRepository.saveAndFlush(outcome);

        // Get all the outcomeList where targetValue is not null
        defaultOutcomeFiltering("targetValue.specified=true", "targetValue.specified=false");
    }

    @Test
    @Transactional
    void getAllOutcomesByTargetValueContainsSomething() throws Exception {
        // Initialize the database
        insertedOutcome = outcomeRepository.saveAndFlush(outcome);

        // Get all the outcomeList where targetValue contains
        defaultOutcomeFiltering("targetValue.contains=" + DEFAULT_TARGET_VALUE, "targetValue.contains=" + UPDATED_TARGET_VALUE);
    }

    @Test
    @Transactional
    void getAllOutcomesByTargetValueNotContainsSomething() throws Exception {
        // Initialize the database
        insertedOutcome = outcomeRepository.saveAndFlush(outcome);

        // Get all the outcomeList where targetValue does not contain
        defaultOutcomeFiltering("targetValue.doesNotContain=" + UPDATED_TARGET_VALUE, "targetValue.doesNotContain=" + DEFAULT_TARGET_VALUE);
    }

    @Test
    @Transactional
    void getAllOutcomesByCurrentValueIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedOutcome = outcomeRepository.saveAndFlush(outcome);

        // Get all the outcomeList where currentValue equals to
        defaultOutcomeFiltering("currentValue.equals=" + DEFAULT_CURRENT_VALUE, "currentValue.equals=" + UPDATED_CURRENT_VALUE);
    }

    @Test
    @Transactional
    void getAllOutcomesByCurrentValueIsInShouldWork() throws Exception {
        // Initialize the database
        insertedOutcome = outcomeRepository.saveAndFlush(outcome);

        // Get all the outcomeList where currentValue in
        defaultOutcomeFiltering(
            "currentValue.in=" + DEFAULT_CURRENT_VALUE + "," + UPDATED_CURRENT_VALUE,
            "currentValue.in=" + UPDATED_CURRENT_VALUE
        );
    }

    @Test
    @Transactional
    void getAllOutcomesByCurrentValueIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedOutcome = outcomeRepository.saveAndFlush(outcome);

        // Get all the outcomeList where currentValue is not null
        defaultOutcomeFiltering("currentValue.specified=true", "currentValue.specified=false");
    }

    @Test
    @Transactional
    void getAllOutcomesByCurrentValueContainsSomething() throws Exception {
        // Initialize the database
        insertedOutcome = outcomeRepository.saveAndFlush(outcome);

        // Get all the outcomeList where currentValue contains
        defaultOutcomeFiltering("currentValue.contains=" + DEFAULT_CURRENT_VALUE, "currentValue.contains=" + UPDATED_CURRENT_VALUE);
    }

    @Test
    @Transactional
    void getAllOutcomesByCurrentValueNotContainsSomething() throws Exception {
        // Initialize the database
        insertedOutcome = outcomeRepository.saveAndFlush(outcome);

        // Get all the outcomeList where currentValue does not contain
        defaultOutcomeFiltering(
            "currentValue.doesNotContain=" + UPDATED_CURRENT_VALUE,
            "currentValue.doesNotContain=" + DEFAULT_CURRENT_VALUE
        );
    }

    @Test
    @Transactional
    void getAllOutcomesByStatusIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedOutcome = outcomeRepository.saveAndFlush(outcome);

        // Get all the outcomeList where status equals to
        defaultOutcomeFiltering("status.equals=" + DEFAULT_STATUS, "status.equals=" + UPDATED_STATUS);
    }

    @Test
    @Transactional
    void getAllOutcomesByStatusIsInShouldWork() throws Exception {
        // Initialize the database
        insertedOutcome = outcomeRepository.saveAndFlush(outcome);

        // Get all the outcomeList where status in
        defaultOutcomeFiltering("status.in=" + DEFAULT_STATUS + "," + UPDATED_STATUS, "status.in=" + UPDATED_STATUS);
    }

    @Test
    @Transactional
    void getAllOutcomesByStatusIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedOutcome = outcomeRepository.saveAndFlush(outcome);

        // Get all the outcomeList where status is not null
        defaultOutcomeFiltering("status.specified=true", "status.specified=false");
    }

    @Test
    @Transactional
    void getAllOutcomesByStartDateIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedOutcome = outcomeRepository.saveAndFlush(outcome);

        // Get all the outcomeList where startDate equals to
        defaultOutcomeFiltering("startDate.equals=" + DEFAULT_START_DATE, "startDate.equals=" + UPDATED_START_DATE);
    }

    @Test
    @Transactional
    void getAllOutcomesByStartDateIsInShouldWork() throws Exception {
        // Initialize the database
        insertedOutcome = outcomeRepository.saveAndFlush(outcome);

        // Get all the outcomeList where startDate in
        defaultOutcomeFiltering("startDate.in=" + DEFAULT_START_DATE + "," + UPDATED_START_DATE, "startDate.in=" + UPDATED_START_DATE);
    }

    @Test
    @Transactional
    void getAllOutcomesByStartDateIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedOutcome = outcomeRepository.saveAndFlush(outcome);

        // Get all the outcomeList where startDate is not null
        defaultOutcomeFiltering("startDate.specified=true", "startDate.specified=false");
    }

    @Test
    @Transactional
    void getAllOutcomesByStartDateIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedOutcome = outcomeRepository.saveAndFlush(outcome);

        // Get all the outcomeList where startDate is greater than or equal to
        defaultOutcomeFiltering("startDate.greaterThanOrEqual=" + DEFAULT_START_DATE, "startDate.greaterThanOrEqual=" + UPDATED_START_DATE);
    }

    @Test
    @Transactional
    void getAllOutcomesByStartDateIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedOutcome = outcomeRepository.saveAndFlush(outcome);

        // Get all the outcomeList where startDate is less than or equal to
        defaultOutcomeFiltering("startDate.lessThanOrEqual=" + DEFAULT_START_DATE, "startDate.lessThanOrEqual=" + SMALLER_START_DATE);
    }

    @Test
    @Transactional
    void getAllOutcomesByStartDateIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedOutcome = outcomeRepository.saveAndFlush(outcome);

        // Get all the outcomeList where startDate is less than
        defaultOutcomeFiltering("startDate.lessThan=" + UPDATED_START_DATE, "startDate.lessThan=" + DEFAULT_START_DATE);
    }

    @Test
    @Transactional
    void getAllOutcomesByStartDateIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedOutcome = outcomeRepository.saveAndFlush(outcome);

        // Get all the outcomeList where startDate is greater than
        defaultOutcomeFiltering("startDate.greaterThan=" + SMALLER_START_DATE, "startDate.greaterThan=" + DEFAULT_START_DATE);
    }

    @Test
    @Transactional
    void getAllOutcomesByTargetDateIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedOutcome = outcomeRepository.saveAndFlush(outcome);

        // Get all the outcomeList where targetDate equals to
        defaultOutcomeFiltering("targetDate.equals=" + DEFAULT_TARGET_DATE, "targetDate.equals=" + UPDATED_TARGET_DATE);
    }

    @Test
    @Transactional
    void getAllOutcomesByTargetDateIsInShouldWork() throws Exception {
        // Initialize the database
        insertedOutcome = outcomeRepository.saveAndFlush(outcome);

        // Get all the outcomeList where targetDate in
        defaultOutcomeFiltering("targetDate.in=" + DEFAULT_TARGET_DATE + "," + UPDATED_TARGET_DATE, "targetDate.in=" + UPDATED_TARGET_DATE);
    }

    @Test
    @Transactional
    void getAllOutcomesByTargetDateIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedOutcome = outcomeRepository.saveAndFlush(outcome);

        // Get all the outcomeList where targetDate is not null
        defaultOutcomeFiltering("targetDate.specified=true", "targetDate.specified=false");
    }

    @Test
    @Transactional
    void getAllOutcomesByTargetDateIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedOutcome = outcomeRepository.saveAndFlush(outcome);

        // Get all the outcomeList where targetDate is greater than or equal to
        defaultOutcomeFiltering(
            "targetDate.greaterThanOrEqual=" + DEFAULT_TARGET_DATE,
            "targetDate.greaterThanOrEqual=" + UPDATED_TARGET_DATE
        );
    }

    @Test
    @Transactional
    void getAllOutcomesByTargetDateIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedOutcome = outcomeRepository.saveAndFlush(outcome);

        // Get all the outcomeList where targetDate is less than or equal to
        defaultOutcomeFiltering("targetDate.lessThanOrEqual=" + DEFAULT_TARGET_DATE, "targetDate.lessThanOrEqual=" + SMALLER_TARGET_DATE);
    }

    @Test
    @Transactional
    void getAllOutcomesByTargetDateIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedOutcome = outcomeRepository.saveAndFlush(outcome);

        // Get all the outcomeList where targetDate is less than
        defaultOutcomeFiltering("targetDate.lessThan=" + UPDATED_TARGET_DATE, "targetDate.lessThan=" + DEFAULT_TARGET_DATE);
    }

    @Test
    @Transactional
    void getAllOutcomesByTargetDateIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedOutcome = outcomeRepository.saveAndFlush(outcome);

        // Get all the outcomeList where targetDate is greater than
        defaultOutcomeFiltering("targetDate.greaterThan=" + SMALLER_TARGET_DATE, "targetDate.greaterThan=" + DEFAULT_TARGET_DATE);
    }

    @Test
    @Transactional
    void getAllOutcomesBySortOrderIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedOutcome = outcomeRepository.saveAndFlush(outcome);

        // Get all the outcomeList where sortOrder equals to
        defaultOutcomeFiltering("sortOrder.equals=" + DEFAULT_SORT_ORDER, "sortOrder.equals=" + UPDATED_SORT_ORDER);
    }

    @Test
    @Transactional
    void getAllOutcomesBySortOrderIsInShouldWork() throws Exception {
        // Initialize the database
        insertedOutcome = outcomeRepository.saveAndFlush(outcome);

        // Get all the outcomeList where sortOrder in
        defaultOutcomeFiltering("sortOrder.in=" + DEFAULT_SORT_ORDER + "," + UPDATED_SORT_ORDER, "sortOrder.in=" + UPDATED_SORT_ORDER);
    }

    @Test
    @Transactional
    void getAllOutcomesBySortOrderIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedOutcome = outcomeRepository.saveAndFlush(outcome);

        // Get all the outcomeList where sortOrder is not null
        defaultOutcomeFiltering("sortOrder.specified=true", "sortOrder.specified=false");
    }

    @Test
    @Transactional
    void getAllOutcomesBySortOrderIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedOutcome = outcomeRepository.saveAndFlush(outcome);

        // Get all the outcomeList where sortOrder is greater than or equal to
        defaultOutcomeFiltering("sortOrder.greaterThanOrEqual=" + DEFAULT_SORT_ORDER, "sortOrder.greaterThanOrEqual=" + UPDATED_SORT_ORDER);
    }

    @Test
    @Transactional
    void getAllOutcomesBySortOrderIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedOutcome = outcomeRepository.saveAndFlush(outcome);

        // Get all the outcomeList where sortOrder is less than or equal to
        defaultOutcomeFiltering("sortOrder.lessThanOrEqual=" + DEFAULT_SORT_ORDER, "sortOrder.lessThanOrEqual=" + SMALLER_SORT_ORDER);
    }

    @Test
    @Transactional
    void getAllOutcomesBySortOrderIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedOutcome = outcomeRepository.saveAndFlush(outcome);

        // Get all the outcomeList where sortOrder is less than
        defaultOutcomeFiltering("sortOrder.lessThan=" + UPDATED_SORT_ORDER, "sortOrder.lessThan=" + DEFAULT_SORT_ORDER);
    }

    @Test
    @Transactional
    void getAllOutcomesBySortOrderIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedOutcome = outcomeRepository.saveAndFlush(outcome);

        // Get all the outcomeList where sortOrder is greater than
        defaultOutcomeFiltering("sortOrder.greaterThan=" + SMALLER_SORT_ORDER, "sortOrder.greaterThan=" + DEFAULT_SORT_ORDER);
    }

    @Test
    @Transactional
    void getAllOutcomesByCreatedDateIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedOutcome = outcomeRepository.saveAndFlush(outcome);

        // Get all the outcomeList where createdDate equals to
        defaultOutcomeFiltering("createdDate.equals=" + DEFAULT_CREATED_DATE, "createdDate.equals=" + UPDATED_CREATED_DATE);
    }

    @Test
    @Transactional
    void getAllOutcomesByCreatedDateIsInShouldWork() throws Exception {
        // Initialize the database
        insertedOutcome = outcomeRepository.saveAndFlush(outcome);

        // Get all the outcomeList where createdDate in
        defaultOutcomeFiltering(
            "createdDate.in=" + DEFAULT_CREATED_DATE + "," + UPDATED_CREATED_DATE,
            "createdDate.in=" + UPDATED_CREATED_DATE
        );
    }

    @Test
    @Transactional
    void getAllOutcomesByCreatedDateIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedOutcome = outcomeRepository.saveAndFlush(outcome);

        // Get all the outcomeList where createdDate is not null
        defaultOutcomeFiltering("createdDate.specified=true", "createdDate.specified=false");
    }

    @Test
    @Transactional
    void getAllOutcomesByLastModifiedDateIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedOutcome = outcomeRepository.saveAndFlush(outcome);

        // Get all the outcomeList where lastModifiedDate equals to
        defaultOutcomeFiltering(
            "lastModifiedDate.equals=" + DEFAULT_LAST_MODIFIED_DATE,
            "lastModifiedDate.equals=" + UPDATED_LAST_MODIFIED_DATE
        );
    }

    @Test
    @Transactional
    void getAllOutcomesByLastModifiedDateIsInShouldWork() throws Exception {
        // Initialize the database
        insertedOutcome = outcomeRepository.saveAndFlush(outcome);

        // Get all the outcomeList where lastModifiedDate in
        defaultOutcomeFiltering(
            "lastModifiedDate.in=" + DEFAULT_LAST_MODIFIED_DATE + "," + UPDATED_LAST_MODIFIED_DATE,
            "lastModifiedDate.in=" + UPDATED_LAST_MODIFIED_DATE
        );
    }

    @Test
    @Transactional
    void getAllOutcomesByLastModifiedDateIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedOutcome = outcomeRepository.saveAndFlush(outcome);

        // Get all the outcomeList where lastModifiedDate is not null
        defaultOutcomeFiltering("lastModifiedDate.specified=true", "lastModifiedDate.specified=false");
    }

    @Test
    @Transactional
    void getAllOutcomesByProductIsEqualToSomething() throws Exception {
        Product product;
        if (TestUtil.findAll(em, Product.class).isEmpty()) {
            outcomeRepository.saveAndFlush(outcome);
            product = ProductResourceIT.createEntity(em);
        } else {
            product = TestUtil.findAll(em, Product.class).get(0);
        }
        em.persist(product);
        em.flush();
        outcome.setProduct(product);
        outcomeRepository.saveAndFlush(outcome);
        Long productId = product.getId();
        // Get all the outcomeList where product equals to productId
        defaultOutcomeShouldBeFound("productId.equals=" + productId);

        // Get all the outcomeList where product equals to (productId + 1)
        defaultOutcomeShouldNotBeFound("productId.equals=" + (productId + 1));
    }

    @Test
    @Transactional
    void getAllOutcomesByOwnerIsEqualToSomething() throws Exception {
        User owner;
        if (TestUtil.findAll(em, User.class).isEmpty()) {
            outcomeRepository.saveAndFlush(outcome);
            owner = UserResourceIT.createEntity();
        } else {
            owner = TestUtil.findAll(em, User.class).get(0);
        }
        em.persist(owner);
        em.flush();
        outcome.setOwner(owner);
        outcomeRepository.saveAndFlush(outcome);
        String ownerId = owner.getId();
        // Get all the outcomeList where owner equals to ownerId
        defaultOutcomeShouldBeFound("ownerId.equals=" + ownerId);

        // Get all the outcomeList where owner equals to "invalid-id"
        defaultOutcomeShouldNotBeFound("ownerId.equals=" + "invalid-id");
    }

    private void defaultOutcomeFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultOutcomeShouldBeFound(shouldBeFound);
        defaultOutcomeShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultOutcomeShouldBeFound(String filter) throws Exception {
        restOutcomeMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(outcome.getId().intValue())))
            .andExpect(jsonPath("$.[*].title").value(hasItem(DEFAULT_TITLE)))
            .andExpect(jsonPath("$.[*].description").value(hasItem(DEFAULT_DESCRIPTION)))
            .andExpect(jsonPath("$.[*].metric").value(hasItem(DEFAULT_METRIC)))
            .andExpect(jsonPath("$.[*].targetValue").value(hasItem(DEFAULT_TARGET_VALUE)))
            .andExpect(jsonPath("$.[*].currentValue").value(hasItem(DEFAULT_CURRENT_VALUE)))
            .andExpect(jsonPath("$.[*].status").value(hasItem(DEFAULT_STATUS.toString())))
            .andExpect(jsonPath("$.[*].startDate").value(hasItem(DEFAULT_START_DATE.toString())))
            .andExpect(jsonPath("$.[*].targetDate").value(hasItem(DEFAULT_TARGET_DATE.toString())))
            .andExpect(jsonPath("$.[*].sortOrder").value(hasItem(DEFAULT_SORT_ORDER)))
            .andExpect(jsonPath("$.[*].createdDate").value(hasItem(DEFAULT_CREATED_DATE.toString())))
            .andExpect(jsonPath("$.[*].lastModifiedDate").value(hasItem(DEFAULT_LAST_MODIFIED_DATE.toString())));

        // Check, that the count call also returns 1
        restOutcomeMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultOutcomeShouldNotBeFound(String filter) throws Exception {
        restOutcomeMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restOutcomeMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingOutcome() throws Exception {
        // Get the outcome
        restOutcomeMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingOutcome() throws Exception {
        // Initialize the database
        insertedOutcome = outcomeRepository.saveAndFlush(outcome);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the outcome
        Outcome updatedOutcome = outcomeRepository.findById(outcome.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedOutcome are not directly saved in db
        em.detach(updatedOutcome);
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

        restOutcomeMockMvc
            .perform(
                put(ENTITY_API_URL_ID, outcomeDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(outcomeDTO))
            )
            .andExpect(status().isOk());

        // Validate the Outcome in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        expectServerSetFields(updatedOutcome);
        assertPersistedOutcomeToMatchAllProperties(updatedOutcome);
    }

    @Test
    @Transactional
    void putNonExistingOutcome() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        outcome.setId(longCount.incrementAndGet());

        // Create the Outcome
        OutcomeDTO outcomeDTO = outcomeMapper.toDto(outcome);

        // A non-existent id must return the same 403 as an id the caller cannot
        // edit, so existence is never revealed (NFR-002).
        restOutcomeMockMvc
            .perform(
                put(ENTITY_API_URL_ID, outcomeDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(outcomeDTO))
            )
            .andExpect(status().isForbidden());

        // Validate the Outcome in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchOutcome() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        outcome.setId(longCount.incrementAndGet());

        // Create the Outcome
        OutcomeDTO outcomeDTO = outcomeMapper.toDto(outcome);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restOutcomeMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(outcomeDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Outcome in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamOutcome() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        outcome.setId(longCount.incrementAndGet());

        // Create the Outcome
        OutcomeDTO outcomeDTO = outcomeMapper.toDto(outcome);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restOutcomeMockMvc
            .perform(put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(outcomeDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Outcome in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateOutcomeWithPatch() throws Exception {
        // Initialize the database
        insertedOutcome = outcomeRepository.saveAndFlush(outcome);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the outcome using partial update
        Outcome partialUpdatedOutcome = new Outcome();
        partialUpdatedOutcome.setId(outcome.getId());

        partialUpdatedOutcome
            .metric(UPDATED_METRIC)
            .targetValue(UPDATED_TARGET_VALUE)
            .currentValue(UPDATED_CURRENT_VALUE)
            .targetDate(UPDATED_TARGET_DATE)
            .createdDate(UPDATED_CREATED_DATE);

        restOutcomeMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedOutcome.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedOutcome))
            )
            .andExpect(status().isOk());

        // Validate the Outcome in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        expectServerSetFields(partialUpdatedOutcome);
        assertOutcomeUpdatableFieldsEquals(createUpdateProxyForBean(partialUpdatedOutcome, outcome), getPersistedOutcome(outcome));
    }

    @Test
    @Transactional
    void fullUpdateOutcomeWithPatch() throws Exception {
        // Initialize the database
        insertedOutcome = outcomeRepository.saveAndFlush(outcome);

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

        restOutcomeMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedOutcome.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedOutcome))
            )
            .andExpect(status().isOk());

        // Validate the Outcome in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        expectServerSetFields(partialUpdatedOutcome);
        assertOutcomeUpdatableFieldsEquals(partialUpdatedOutcome, getPersistedOutcome(partialUpdatedOutcome));
    }

    @Test
    @Transactional
    void patchNonExistingOutcome() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        outcome.setId(longCount.incrementAndGet());

        // Create the Outcome
        OutcomeDTO outcomeDTO = outcomeMapper.toDto(outcome);

        // A non-existent id must return the same 403 as an id the caller cannot
        // edit, so existence is never revealed (NFR-002).
        restOutcomeMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, outcomeDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(outcomeDTO))
            )
            .andExpect(status().isForbidden());

        // Validate the Outcome in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchOutcome() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        outcome.setId(longCount.incrementAndGet());

        // Create the Outcome
        OutcomeDTO outcomeDTO = outcomeMapper.toDto(outcome);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restOutcomeMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(outcomeDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Outcome in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamOutcome() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        outcome.setId(longCount.incrementAndGet());

        // Create the Outcome
        OutcomeDTO outcomeDTO = outcomeMapper.toDto(outcome);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restOutcomeMockMvc
            .perform(
                patch(ENTITY_API_URL).with(csrf()).contentType("application/merge-patch+json").content(om.writeValueAsBytes(outcomeDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the Outcome in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteOutcome() throws Exception {
        // Initialize the database
        insertedOutcome = outcomeRepository.saveAndFlush(outcome);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the outcome
        restOutcomeMockMvc
            .perform(delete(ENTITY_API_URL_ID, outcome.getId()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    /**
     * sortOrder and createdDate are server-owned and lastModifiedDate is stamped by the
     * server on every update (TREE-002), whatever the client sent.
     */
    private void expectServerSetFields(Outcome expected) {
        Outcome persisted = getPersistedOutcome(expected);
        assertThat(persisted.getLastModifiedDate()).isAfter(DEFAULT_LAST_MODIFIED_DATE);
        expected.sortOrder(DEFAULT_SORT_ORDER).createdDate(DEFAULT_CREATED_DATE).lastModifiedDate(persisted.getLastModifiedDate());
    }

    protected long getRepositoryCount() {
        return outcomeRepository.count();
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
        return outcomeRepository.findById(outcome.getId()).orElseThrow();
    }

    protected void assertPersistedOutcomeToMatchAllProperties(Outcome expectedOutcome) {
        assertOutcomeAllPropertiesEquals(expectedOutcome, getPersistedOutcome(expectedOutcome));
    }

    protected void assertPersistedOutcomeToMatchUpdatableProperties(Outcome expectedOutcome) {
        assertOutcomeAllUpdatablePropertiesEquals(expectedOutcome, getPersistedOutcome(expectedOutcome));
    }
}
