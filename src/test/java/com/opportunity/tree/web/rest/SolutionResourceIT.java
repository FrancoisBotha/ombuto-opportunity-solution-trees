package com.opportunity.tree.web.rest;

import static com.opportunity.tree.domain.SolutionAsserts.*;
import static com.opportunity.tree.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opportunity.tree.IntegrationTest;
import com.opportunity.tree.domain.Opportunity;
import com.opportunity.tree.domain.Solution;
import com.opportunity.tree.domain.Tag;
import com.opportunity.tree.domain.User;
import com.opportunity.tree.domain.enumeration.SolutionStatus;
import com.opportunity.tree.repository.SolutionRepository;
import com.opportunity.tree.repository.UserRepository;
import com.opportunity.tree.service.SolutionService;
import com.opportunity.tree.service.dto.SolutionDTO;
import com.opportunity.tree.service.mapper.SolutionMapper;
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
 * Integration tests for the {@link SolutionResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
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
    private static final Integer SMALLER_EFFORT = 1 - 1;

    private static final Integer DEFAULT_SORT_ORDER = 1;
    private static final Integer UPDATED_SORT_ORDER = 2;
    private static final Integer SMALLER_SORT_ORDER = 1 - 1;

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
    private MockMvc restSolutionMockMvc;

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
        if (TestUtil.findAll(em, Opportunity.class).isEmpty()) {
            opportunity = OpportunityResourceIT.createEntity(em);
            em.persist(opportunity);
            em.flush();
        } else {
            opportunity = TestUtil.findAll(em, Opportunity.class).get(0);
        }
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
        if (TestUtil.findAll(em, Opportunity.class).isEmpty()) {
            opportunity = OpportunityResourceIT.createUpdatedEntity(em);
            em.persist(opportunity);
            em.flush();
        } else {
            opportunity = TestUtil.findAll(em, Opportunity.class).get(0);
        }
        updatedSolution.setOpportunity(opportunity);
        return updatedSolution;
    }

    @BeforeEach
    void initTest() {
        solution = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedSolution != null) {
            solutionRepository.delete(insertedSolution);
            insertedSolution = null;
        }
        userRepository.deleteAll();
    }

    @Test
    @Transactional
    void createSolution() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Solution
        SolutionDTO solutionDTO = solutionMapper.toDto(solution);
        var returnedSolutionDTO = om.readValue(
            restSolutionMockMvc
                .perform(
                    post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(solutionDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            SolutionDTO.class
        );

        // Validate the Solution in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedSolution = solutionMapper.toEntity(returnedSolutionDTO);
        assertSolutionUpdatableFieldsEquals(returnedSolution, getPersistedSolution(returnedSolution));

        insertedSolution = returnedSolution;
    }

    @Test
    @Transactional
    void createSolutionWithExistingId() throws Exception {
        // Create the Solution with an existing ID
        solution.setId(1L);
        SolutionDTO solutionDTO = solutionMapper.toDto(solution);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restSolutionMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(solutionDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Solution in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkTitleIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        solution.setTitle(null);

        // Create the Solution, which fails.
        SolutionDTO solutionDTO = solutionMapper.toDto(solution);

        restSolutionMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(solutionDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkStatusIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        solution.setStatus(null);

        // Create the Solution, which fails.
        SolutionDTO solutionDTO = solutionMapper.toDto(solution);

        restSolutionMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(solutionDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkSortOrderIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        solution.setSortOrder(null);

        // Create the Solution, which fails.
        SolutionDTO solutionDTO = solutionMapper.toDto(solution);

        restSolutionMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(solutionDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkCreatedDateIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        solution.setCreatedDate(null);

        // Create the Solution, which fails.
        SolutionDTO solutionDTO = solutionMapper.toDto(solution);

        restSolutionMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(solutionDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllSolutions() throws Exception {
        // Initialize the database
        insertedSolution = solutionRepository.saveAndFlush(solution);

        // Get all the solutionList
        restSolutionMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(solution.getId().intValue())))
            .andExpect(jsonPath("$.[*].title").value(hasItem(DEFAULT_TITLE)))
            .andExpect(jsonPath("$.[*].description").value(hasItem(DEFAULT_DESCRIPTION)))
            .andExpect(jsonPath("$.[*].status").value(hasItem(DEFAULT_STATUS.toString())))
            .andExpect(jsonPath("$.[*].effort").value(hasItem(DEFAULT_EFFORT)))
            .andExpect(jsonPath("$.[*].sortOrder").value(hasItem(DEFAULT_SORT_ORDER)))
            .andExpect(jsonPath("$.[*].createdDate").value(hasItem(DEFAULT_CREATED_DATE.toString())))
            .andExpect(jsonPath("$.[*].lastModifiedDate").value(hasItem(DEFAULT_LAST_MODIFIED_DATE.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllSolutionsWithEagerRelationshipsIsEnabled() throws Exception {
        when(solutionServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restSolutionMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(solutionServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllSolutionsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(solutionServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restSolutionMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(solutionRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getSolution() throws Exception {
        // Initialize the database
        insertedSolution = solutionRepository.saveAndFlush(solution);

        // Get the solution
        restSolutionMockMvc
            .perform(get(ENTITY_API_URL_ID, solution.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(solution.getId().intValue()))
            .andExpect(jsonPath("$.title").value(DEFAULT_TITLE))
            .andExpect(jsonPath("$.description").value(DEFAULT_DESCRIPTION))
            .andExpect(jsonPath("$.status").value(DEFAULT_STATUS.toString()))
            .andExpect(jsonPath("$.effort").value(DEFAULT_EFFORT))
            .andExpect(jsonPath("$.sortOrder").value(DEFAULT_SORT_ORDER))
            .andExpect(jsonPath("$.createdDate").value(DEFAULT_CREATED_DATE.toString()))
            .andExpect(jsonPath("$.lastModifiedDate").value(DEFAULT_LAST_MODIFIED_DATE.toString()));
    }

    @Test
    @Transactional
    void getSolutionsByIdFiltering() throws Exception {
        // Initialize the database
        insertedSolution = solutionRepository.saveAndFlush(solution);

        Long id = solution.getId();

        defaultSolutionFiltering("id.equals=" + id, "id.notEquals=" + id);

        defaultSolutionFiltering("id.greaterThanOrEqual=" + id, "id.greaterThan=" + id);

        defaultSolutionFiltering("id.lessThanOrEqual=" + id, "id.lessThan=" + id);
    }

    @Test
    @Transactional
    void getAllSolutionsByTitleIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedSolution = solutionRepository.saveAndFlush(solution);

        // Get all the solutionList where title equals to
        defaultSolutionFiltering("title.equals=" + DEFAULT_TITLE, "title.equals=" + UPDATED_TITLE);
    }

    @Test
    @Transactional
    void getAllSolutionsByTitleIsInShouldWork() throws Exception {
        // Initialize the database
        insertedSolution = solutionRepository.saveAndFlush(solution);

        // Get all the solutionList where title in
        defaultSolutionFiltering("title.in=" + DEFAULT_TITLE + "," + UPDATED_TITLE, "title.in=" + UPDATED_TITLE);
    }

    @Test
    @Transactional
    void getAllSolutionsByTitleIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedSolution = solutionRepository.saveAndFlush(solution);

        // Get all the solutionList where title is not null
        defaultSolutionFiltering("title.specified=true", "title.specified=false");
    }

    @Test
    @Transactional
    void getAllSolutionsByTitleContainsSomething() throws Exception {
        // Initialize the database
        insertedSolution = solutionRepository.saveAndFlush(solution);

        // Get all the solutionList where title contains
        defaultSolutionFiltering("title.contains=" + DEFAULT_TITLE, "title.contains=" + UPDATED_TITLE);
    }

    @Test
    @Transactional
    void getAllSolutionsByTitleNotContainsSomething() throws Exception {
        // Initialize the database
        insertedSolution = solutionRepository.saveAndFlush(solution);

        // Get all the solutionList where title does not contain
        defaultSolutionFiltering("title.doesNotContain=" + UPDATED_TITLE, "title.doesNotContain=" + DEFAULT_TITLE);
    }

    @Test
    @Transactional
    void getAllSolutionsByStatusIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedSolution = solutionRepository.saveAndFlush(solution);

        // Get all the solutionList where status equals to
        defaultSolutionFiltering("status.equals=" + DEFAULT_STATUS, "status.equals=" + UPDATED_STATUS);
    }

    @Test
    @Transactional
    void getAllSolutionsByStatusIsInShouldWork() throws Exception {
        // Initialize the database
        insertedSolution = solutionRepository.saveAndFlush(solution);

        // Get all the solutionList where status in
        defaultSolutionFiltering("status.in=" + DEFAULT_STATUS + "," + UPDATED_STATUS, "status.in=" + UPDATED_STATUS);
    }

    @Test
    @Transactional
    void getAllSolutionsByStatusIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedSolution = solutionRepository.saveAndFlush(solution);

        // Get all the solutionList where status is not null
        defaultSolutionFiltering("status.specified=true", "status.specified=false");
    }

    @Test
    @Transactional
    void getAllSolutionsByEffortIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedSolution = solutionRepository.saveAndFlush(solution);

        // Get all the solutionList where effort equals to
        defaultSolutionFiltering("effort.equals=" + DEFAULT_EFFORT, "effort.equals=" + UPDATED_EFFORT);
    }

    @Test
    @Transactional
    void getAllSolutionsByEffortIsInShouldWork() throws Exception {
        // Initialize the database
        insertedSolution = solutionRepository.saveAndFlush(solution);

        // Get all the solutionList where effort in
        defaultSolutionFiltering("effort.in=" + DEFAULT_EFFORT + "," + UPDATED_EFFORT, "effort.in=" + UPDATED_EFFORT);
    }

    @Test
    @Transactional
    void getAllSolutionsByEffortIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedSolution = solutionRepository.saveAndFlush(solution);

        // Get all the solutionList where effort is not null
        defaultSolutionFiltering("effort.specified=true", "effort.specified=false");
    }

    @Test
    @Transactional
    void getAllSolutionsByEffortIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedSolution = solutionRepository.saveAndFlush(solution);

        // Get all the solutionList where effort is greater than or equal to
        defaultSolutionFiltering("effort.greaterThanOrEqual=" + DEFAULT_EFFORT, "effort.greaterThanOrEqual=" + (DEFAULT_EFFORT + 1));
    }

    @Test
    @Transactional
    void getAllSolutionsByEffortIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedSolution = solutionRepository.saveAndFlush(solution);

        // Get all the solutionList where effort is less than or equal to
        defaultSolutionFiltering("effort.lessThanOrEqual=" + DEFAULT_EFFORT, "effort.lessThanOrEqual=" + SMALLER_EFFORT);
    }

    @Test
    @Transactional
    void getAllSolutionsByEffortIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedSolution = solutionRepository.saveAndFlush(solution);

        // Get all the solutionList where effort is less than
        defaultSolutionFiltering("effort.lessThan=" + (DEFAULT_EFFORT + 1), "effort.lessThan=" + DEFAULT_EFFORT);
    }

    @Test
    @Transactional
    void getAllSolutionsByEffortIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedSolution = solutionRepository.saveAndFlush(solution);

        // Get all the solutionList where effort is greater than
        defaultSolutionFiltering("effort.greaterThan=" + SMALLER_EFFORT, "effort.greaterThan=" + DEFAULT_EFFORT);
    }

    @Test
    @Transactional
    void getAllSolutionsBySortOrderIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedSolution = solutionRepository.saveAndFlush(solution);

        // Get all the solutionList where sortOrder equals to
        defaultSolutionFiltering("sortOrder.equals=" + DEFAULT_SORT_ORDER, "sortOrder.equals=" + UPDATED_SORT_ORDER);
    }

    @Test
    @Transactional
    void getAllSolutionsBySortOrderIsInShouldWork() throws Exception {
        // Initialize the database
        insertedSolution = solutionRepository.saveAndFlush(solution);

        // Get all the solutionList where sortOrder in
        defaultSolutionFiltering("sortOrder.in=" + DEFAULT_SORT_ORDER + "," + UPDATED_SORT_ORDER, "sortOrder.in=" + UPDATED_SORT_ORDER);
    }

    @Test
    @Transactional
    void getAllSolutionsBySortOrderIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedSolution = solutionRepository.saveAndFlush(solution);

        // Get all the solutionList where sortOrder is not null
        defaultSolutionFiltering("sortOrder.specified=true", "sortOrder.specified=false");
    }

    @Test
    @Transactional
    void getAllSolutionsBySortOrderIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedSolution = solutionRepository.saveAndFlush(solution);

        // Get all the solutionList where sortOrder is greater than or equal to
        defaultSolutionFiltering(
            "sortOrder.greaterThanOrEqual=" + DEFAULT_SORT_ORDER,
            "sortOrder.greaterThanOrEqual=" + UPDATED_SORT_ORDER
        );
    }

    @Test
    @Transactional
    void getAllSolutionsBySortOrderIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedSolution = solutionRepository.saveAndFlush(solution);

        // Get all the solutionList where sortOrder is less than or equal to
        defaultSolutionFiltering("sortOrder.lessThanOrEqual=" + DEFAULT_SORT_ORDER, "sortOrder.lessThanOrEqual=" + SMALLER_SORT_ORDER);
    }

    @Test
    @Transactional
    void getAllSolutionsBySortOrderIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedSolution = solutionRepository.saveAndFlush(solution);

        // Get all the solutionList where sortOrder is less than
        defaultSolutionFiltering("sortOrder.lessThan=" + UPDATED_SORT_ORDER, "sortOrder.lessThan=" + DEFAULT_SORT_ORDER);
    }

    @Test
    @Transactional
    void getAllSolutionsBySortOrderIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedSolution = solutionRepository.saveAndFlush(solution);

        // Get all the solutionList where sortOrder is greater than
        defaultSolutionFiltering("sortOrder.greaterThan=" + SMALLER_SORT_ORDER, "sortOrder.greaterThan=" + DEFAULT_SORT_ORDER);
    }

    @Test
    @Transactional
    void getAllSolutionsByCreatedDateIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedSolution = solutionRepository.saveAndFlush(solution);

        // Get all the solutionList where createdDate equals to
        defaultSolutionFiltering("createdDate.equals=" + DEFAULT_CREATED_DATE, "createdDate.equals=" + UPDATED_CREATED_DATE);
    }

    @Test
    @Transactional
    void getAllSolutionsByCreatedDateIsInShouldWork() throws Exception {
        // Initialize the database
        insertedSolution = solutionRepository.saveAndFlush(solution);

        // Get all the solutionList where createdDate in
        defaultSolutionFiltering(
            "createdDate.in=" + DEFAULT_CREATED_DATE + "," + UPDATED_CREATED_DATE,
            "createdDate.in=" + UPDATED_CREATED_DATE
        );
    }

    @Test
    @Transactional
    void getAllSolutionsByCreatedDateIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedSolution = solutionRepository.saveAndFlush(solution);

        // Get all the solutionList where createdDate is not null
        defaultSolutionFiltering("createdDate.specified=true", "createdDate.specified=false");
    }

    @Test
    @Transactional
    void getAllSolutionsByLastModifiedDateIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedSolution = solutionRepository.saveAndFlush(solution);

        // Get all the solutionList where lastModifiedDate equals to
        defaultSolutionFiltering(
            "lastModifiedDate.equals=" + DEFAULT_LAST_MODIFIED_DATE,
            "lastModifiedDate.equals=" + UPDATED_LAST_MODIFIED_DATE
        );
    }

    @Test
    @Transactional
    void getAllSolutionsByLastModifiedDateIsInShouldWork() throws Exception {
        // Initialize the database
        insertedSolution = solutionRepository.saveAndFlush(solution);

        // Get all the solutionList where lastModifiedDate in
        defaultSolutionFiltering(
            "lastModifiedDate.in=" + DEFAULT_LAST_MODIFIED_DATE + "," + UPDATED_LAST_MODIFIED_DATE,
            "lastModifiedDate.in=" + UPDATED_LAST_MODIFIED_DATE
        );
    }

    @Test
    @Transactional
    void getAllSolutionsByLastModifiedDateIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedSolution = solutionRepository.saveAndFlush(solution);

        // Get all the solutionList where lastModifiedDate is not null
        defaultSolutionFiltering("lastModifiedDate.specified=true", "lastModifiedDate.specified=false");
    }

    @Test
    @Transactional
    void getAllSolutionsByOpportunityIsEqualToSomething() throws Exception {
        Opportunity opportunity;
        if (TestUtil.findAll(em, Opportunity.class).isEmpty()) {
            solutionRepository.saveAndFlush(solution);
            opportunity = OpportunityResourceIT.createEntity(em);
        } else {
            opportunity = TestUtil.findAll(em, Opportunity.class).get(0);
        }
        em.persist(opportunity);
        em.flush();
        solution.setOpportunity(opportunity);
        solutionRepository.saveAndFlush(solution);
        Long opportunityId = opportunity.getId();
        // Get all the solutionList where opportunity equals to opportunityId
        defaultSolutionShouldBeFound("opportunityId.equals=" + opportunityId);

        // Get all the solutionList where opportunity equals to (opportunityId + 1)
        defaultSolutionShouldNotBeFound("opportunityId.equals=" + (opportunityId + 1));
    }

    @Test
    @Transactional
    void getAllSolutionsByOwnerIsEqualToSomething() throws Exception {
        User owner;
        if (TestUtil.findAll(em, User.class).isEmpty()) {
            solutionRepository.saveAndFlush(solution);
            owner = UserResourceIT.createEntity();
        } else {
            owner = TestUtil.findAll(em, User.class).get(0);
        }
        em.persist(owner);
        em.flush();
        solution.setOwner(owner);
        solutionRepository.saveAndFlush(solution);
        String ownerId = owner.getId();
        // Get all the solutionList where owner equals to ownerId
        defaultSolutionShouldBeFound("ownerId.equals=" + ownerId);

        // Get all the solutionList where owner equals to "invalid-id"
        defaultSolutionShouldNotBeFound("ownerId.equals=" + "invalid-id");
    }

    @Test
    @Transactional
    void getAllSolutionsByTagIsEqualToSomething() throws Exception {
        Tag tag;
        if (TestUtil.findAll(em, Tag.class).isEmpty()) {
            solutionRepository.saveAndFlush(solution);
            tag = TagResourceIT.createEntity(em);
        } else {
            tag = TestUtil.findAll(em, Tag.class).get(0);
        }
        em.persist(tag);
        em.flush();
        solution.addTag(tag);
        solutionRepository.saveAndFlush(solution);
        Long tagId = tag.getId();
        // Get all the solutionList where tag equals to tagId
        defaultSolutionShouldBeFound("tagId.equals=" + tagId);

        // Get all the solutionList where tag equals to (tagId + 1)
        defaultSolutionShouldNotBeFound("tagId.equals=" + (tagId + 1));
    }

    private void defaultSolutionFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultSolutionShouldBeFound(shouldBeFound);
        defaultSolutionShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultSolutionShouldBeFound(String filter) throws Exception {
        restSolutionMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(solution.getId().intValue())))
            .andExpect(jsonPath("$.[*].title").value(hasItem(DEFAULT_TITLE)))
            .andExpect(jsonPath("$.[*].description").value(hasItem(DEFAULT_DESCRIPTION)))
            .andExpect(jsonPath("$.[*].status").value(hasItem(DEFAULT_STATUS.toString())))
            .andExpect(jsonPath("$.[*].effort").value(hasItem(DEFAULT_EFFORT)))
            .andExpect(jsonPath("$.[*].sortOrder").value(hasItem(DEFAULT_SORT_ORDER)))
            .andExpect(jsonPath("$.[*].createdDate").value(hasItem(DEFAULT_CREATED_DATE.toString())))
            .andExpect(jsonPath("$.[*].lastModifiedDate").value(hasItem(DEFAULT_LAST_MODIFIED_DATE.toString())));

        // Check, that the count call also returns 1
        restSolutionMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultSolutionShouldNotBeFound(String filter) throws Exception {
        restSolutionMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restSolutionMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingSolution() throws Exception {
        // Get the solution
        restSolutionMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingSolution() throws Exception {
        // Initialize the database
        insertedSolution = solutionRepository.saveAndFlush(solution);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the solution
        Solution updatedSolution = solutionRepository.findById(solution.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedSolution are not directly saved in db
        em.detach(updatedSolution);
        updatedSolution
            .title(UPDATED_TITLE)
            .description(UPDATED_DESCRIPTION)
            .status(UPDATED_STATUS)
            .effort(UPDATED_EFFORT)
            .sortOrder(UPDATED_SORT_ORDER)
            .createdDate(UPDATED_CREATED_DATE)
            .lastModifiedDate(UPDATED_LAST_MODIFIED_DATE);
        SolutionDTO solutionDTO = solutionMapper.toDto(updatedSolution);

        restSolutionMockMvc
            .perform(
                put(ENTITY_API_URL_ID, solutionDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(solutionDTO))
            )
            .andExpect(status().isOk());

        // Validate the Solution in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedSolutionToMatchAllProperties(updatedSolution);
    }

    @Test
    @Transactional
    void putNonExistingSolution() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        solution.setId(longCount.incrementAndGet());

        // Create the Solution
        SolutionDTO solutionDTO = solutionMapper.toDto(solution);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restSolutionMockMvc
            .perform(
                put(ENTITY_API_URL_ID, solutionDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(solutionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Solution in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchSolution() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        solution.setId(longCount.incrementAndGet());

        // Create the Solution
        SolutionDTO solutionDTO = solutionMapper.toDto(solution);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restSolutionMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(solutionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Solution in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamSolution() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        solution.setId(longCount.incrementAndGet());

        // Create the Solution
        SolutionDTO solutionDTO = solutionMapper.toDto(solution);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restSolutionMockMvc
            .perform(put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(solutionDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Solution in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateSolutionWithPatch() throws Exception {
        // Initialize the database
        insertedSolution = solutionRepository.saveAndFlush(solution);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the solution using partial update
        Solution partialUpdatedSolution = new Solution();
        partialUpdatedSolution.setId(solution.getId());

        partialUpdatedSolution
            .title(UPDATED_TITLE)
            .description(UPDATED_DESCRIPTION)
            .status(UPDATED_STATUS)
            .effort(UPDATED_EFFORT)
            .sortOrder(UPDATED_SORT_ORDER);

        restSolutionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedSolution.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedSolution))
            )
            .andExpect(status().isOk());

        // Validate the Solution in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertSolutionUpdatableFieldsEquals(createUpdateProxyForBean(partialUpdatedSolution, solution), getPersistedSolution(solution));
    }

    @Test
    @Transactional
    void fullUpdateSolutionWithPatch() throws Exception {
        // Initialize the database
        insertedSolution = solutionRepository.saveAndFlush(solution);

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

        restSolutionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedSolution.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedSolution))
            )
            .andExpect(status().isOk());

        // Validate the Solution in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertSolutionUpdatableFieldsEquals(partialUpdatedSolution, getPersistedSolution(partialUpdatedSolution));
    }

    @Test
    @Transactional
    void patchNonExistingSolution() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        solution.setId(longCount.incrementAndGet());

        // Create the Solution
        SolutionDTO solutionDTO = solutionMapper.toDto(solution);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restSolutionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, solutionDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(solutionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Solution in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchSolution() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        solution.setId(longCount.incrementAndGet());

        // Create the Solution
        SolutionDTO solutionDTO = solutionMapper.toDto(solution);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restSolutionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(solutionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Solution in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamSolution() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        solution.setId(longCount.incrementAndGet());

        // Create the Solution
        SolutionDTO solutionDTO = solutionMapper.toDto(solution);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restSolutionMockMvc
            .perform(
                patch(ENTITY_API_URL).with(csrf()).contentType("application/merge-patch+json").content(om.writeValueAsBytes(solutionDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the Solution in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteSolution() throws Exception {
        // Initialize the database
        insertedSolution = solutionRepository.saveAndFlush(solution);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the solution
        restSolutionMockMvc
            .perform(delete(ENTITY_API_URL_ID, solution.getId()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return solutionRepository.count();
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
        return solutionRepository.findById(solution.getId()).orElseThrow();
    }

    protected void assertPersistedSolutionToMatchAllProperties(Solution expectedSolution) {
        assertSolutionAllPropertiesEquals(expectedSolution, getPersistedSolution(expectedSolution));
    }

    protected void assertPersistedSolutionToMatchUpdatableProperties(Solution expectedSolution) {
        assertSolutionAllUpdatablePropertiesEquals(expectedSolution, getPersistedSolution(expectedSolution));
    }
}
