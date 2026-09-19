package com.opportunity.tree.web.rest;

import static com.opportunity.tree.domain.OpportunityAsserts.*;
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
import com.opportunity.tree.domain.Opportunity;
import com.opportunity.tree.domain.Outcome;
import com.opportunity.tree.domain.Tag;
import com.opportunity.tree.domain.User;
import com.opportunity.tree.domain.enumeration.OpportunityStatus;
import com.opportunity.tree.repository.OpportunityRepository;
import com.opportunity.tree.repository.UserRepository;
import com.opportunity.tree.service.OpportunityService;
import com.opportunity.tree.service.dto.OpportunityDTO;
import com.opportunity.tree.service.mapper.OpportunityMapper;
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
 * Integration tests for the {@link OpportunityResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = "ROLE_ADMIN")
class OpportunityResourceIT {

    private static final String DEFAULT_TITLE = "AAAAAAAAAA";
    private static final String UPDATED_TITLE = "BBBBBBBBBB";

    private static final String DEFAULT_DESCRIPTION = "AAAAAAAAAA";
    private static final String UPDATED_DESCRIPTION = "BBBBBBBBBB";

    private static final OpportunityStatus DEFAULT_STATUS = OpportunityStatus.UNEXPLORED;
    private static final OpportunityStatus UPDATED_STATUS = OpportunityStatus.EXPLORING;

    private static final Integer DEFAULT_VALUERATING = 1;
    private static final Integer UPDATED_VALUERATING = 2;
    private static final Integer SMALLER_VALUERATING = 1 - 1;

    private static final Integer DEFAULT_PRIORITY = 1;
    private static final Integer UPDATED_PRIORITY = 2;
    private static final Integer SMALLER_PRIORITY = 1 - 1;

    private static final Integer DEFAULT_SORT_ORDER = 1;
    private static final Integer UPDATED_SORT_ORDER = 2;
    private static final Integer SMALLER_SORT_ORDER = 1 - 1;

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
    private MockMvc restOpportunityMockMvc;

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
            .valuerating(DEFAULT_VALUERATING)
            .priority(DEFAULT_PRIORITY)
            .sortOrder(DEFAULT_SORT_ORDER)
            .createdDate(DEFAULT_CREATED_DATE)
            .lastModifiedDate(DEFAULT_LAST_MODIFIED_DATE);
        // Add required entity
        Outcome outcome;
        if (TestUtil.findAll(em, Outcome.class).isEmpty()) {
            outcome = OutcomeResourceIT.createEntity(em);
            em.persist(outcome);
            em.flush();
        } else {
            outcome = TestUtil.findAll(em, Outcome.class).get(0);
        }
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
            .valuerating(UPDATED_VALUERATING)
            .priority(UPDATED_PRIORITY)
            .sortOrder(UPDATED_SORT_ORDER)
            .createdDate(UPDATED_CREATED_DATE)
            .lastModifiedDate(UPDATED_LAST_MODIFIED_DATE);
        // Add required entity
        Outcome outcome;
        if (TestUtil.findAll(em, Outcome.class).isEmpty()) {
            outcome = OutcomeResourceIT.createUpdatedEntity(em);
            em.persist(outcome);
            em.flush();
        } else {
            outcome = TestUtil.findAll(em, Outcome.class).get(0);
        }
        updatedOpportunity.setOutcome(outcome);
        return updatedOpportunity;
    }

    @BeforeEach
    void initTest() {
        opportunity = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedOpportunity != null) {
            opportunityRepository.delete(insertedOpportunity);
            insertedOpportunity = null;
        }
        userRepository.deleteAll();
    }

    @Test
    @Transactional
    void createOpportunity() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Opportunity
        OpportunityDTO opportunityDTO = opportunityMapper.toDto(opportunity);
        var returnedOpportunityDTO = om.readValue(
            restOpportunityMockMvc
                .perform(
                    post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(opportunityDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            OpportunityDTO.class
        );

        // Validate the Opportunity in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedOpportunity = opportunityMapper.toEntity(returnedOpportunityDTO);
        assertOpportunityUpdatableFieldsEquals(returnedOpportunity, getPersistedOpportunity(returnedOpportunity));

        insertedOpportunity = returnedOpportunity;
    }

    @Test
    @Transactional
    void createOpportunityWithExistingId() throws Exception {
        // Create the Opportunity with an existing ID
        opportunity.setId(1L);
        OpportunityDTO opportunityDTO = opportunityMapper.toDto(opportunity);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restOpportunityMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(opportunityDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Opportunity in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkTitleIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        opportunity.setTitle(null);

        // Create the Opportunity, which fails.
        OpportunityDTO opportunityDTO = opportunityMapper.toDto(opportunity);

        restOpportunityMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(opportunityDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkStatusIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        opportunity.setStatus(null);

        // Create the Opportunity, which fails.
        OpportunityDTO opportunityDTO = opportunityMapper.toDto(opportunity);

        restOpportunityMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(opportunityDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkValueratingIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        opportunity.setValuerating(null);

        // Create the Opportunity, which fails.
        OpportunityDTO opportunityDTO = opportunityMapper.toDto(opportunity);

        restOpportunityMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(opportunityDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkPriorityIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        opportunity.setPriority(null);

        // Create the Opportunity, which fails.
        OpportunityDTO opportunityDTO = opportunityMapper.toDto(opportunity);

        restOpportunityMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(opportunityDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkSortOrderIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        opportunity.setSortOrder(null);

        // Create the Opportunity, which fails.
        OpportunityDTO opportunityDTO = opportunityMapper.toDto(opportunity);

        restOpportunityMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(opportunityDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkCreatedDateIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        opportunity.setCreatedDate(null);

        // Create the Opportunity, which fails.
        OpportunityDTO opportunityDTO = opportunityMapper.toDto(opportunity);

        restOpportunityMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(opportunityDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllOpportunities() throws Exception {
        // Initialize the database
        insertedOpportunity = opportunityRepository.saveAndFlush(opportunity);

        // Get all the opportunityList
        restOpportunityMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(opportunity.getId().intValue())))
            .andExpect(jsonPath("$.[*].title").value(hasItem(DEFAULT_TITLE)))
            .andExpect(jsonPath("$.[*].description").value(hasItem(DEFAULT_DESCRIPTION)))
            .andExpect(jsonPath("$.[*].status").value(hasItem(DEFAULT_STATUS.toString())))
            .andExpect(jsonPath("$.[*].valuerating").value(hasItem(DEFAULT_VALUERATING)))
            .andExpect(jsonPath("$.[*].priority").value(hasItem(DEFAULT_PRIORITY)))
            .andExpect(jsonPath("$.[*].sortOrder").value(hasItem(DEFAULT_SORT_ORDER)))
            .andExpect(jsonPath("$.[*].createdDate").value(hasItem(DEFAULT_CREATED_DATE.toString())))
            .andExpect(jsonPath("$.[*].lastModifiedDate").value(hasItem(DEFAULT_LAST_MODIFIED_DATE.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllOpportunitiesWithEagerRelationshipsIsEnabled() throws Exception {
        when(opportunityServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restOpportunityMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(opportunityServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllOpportunitiesWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(opportunityServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restOpportunityMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(opportunityRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getOpportunity() throws Exception {
        // Initialize the database
        insertedOpportunity = opportunityRepository.saveAndFlush(opportunity);

        // Get the opportunity
        restOpportunityMockMvc
            .perform(get(ENTITY_API_URL_ID, opportunity.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(opportunity.getId().intValue()))
            .andExpect(jsonPath("$.title").value(DEFAULT_TITLE))
            .andExpect(jsonPath("$.description").value(DEFAULT_DESCRIPTION))
            .andExpect(jsonPath("$.status").value(DEFAULT_STATUS.toString()))
            .andExpect(jsonPath("$.valuerating").value(DEFAULT_VALUERATING))
            .andExpect(jsonPath("$.priority").value(DEFAULT_PRIORITY))
            .andExpect(jsonPath("$.sortOrder").value(DEFAULT_SORT_ORDER))
            .andExpect(jsonPath("$.createdDate").value(DEFAULT_CREATED_DATE.toString()))
            .andExpect(jsonPath("$.lastModifiedDate").value(DEFAULT_LAST_MODIFIED_DATE.toString()));
    }

    @Test
    @Transactional
    void getOpportunitiesByIdFiltering() throws Exception {
        // Initialize the database
        insertedOpportunity = opportunityRepository.saveAndFlush(opportunity);

        Long id = opportunity.getId();

        defaultOpportunityFiltering("id.equals=" + id, "id.notEquals=" + id);

        defaultOpportunityFiltering("id.greaterThanOrEqual=" + id, "id.greaterThan=" + id);

        defaultOpportunityFiltering("id.lessThanOrEqual=" + id, "id.lessThan=" + id);
    }

    @Test
    @Transactional
    void getAllOpportunitiesByTitleIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedOpportunity = opportunityRepository.saveAndFlush(opportunity);

        // Get all the opportunityList where title equals to
        defaultOpportunityFiltering("title.equals=" + DEFAULT_TITLE, "title.equals=" + UPDATED_TITLE);
    }

    @Test
    @Transactional
    void getAllOpportunitiesByTitleIsInShouldWork() throws Exception {
        // Initialize the database
        insertedOpportunity = opportunityRepository.saveAndFlush(opportunity);

        // Get all the opportunityList where title in
        defaultOpportunityFiltering("title.in=" + DEFAULT_TITLE + "," + UPDATED_TITLE, "title.in=" + UPDATED_TITLE);
    }

    @Test
    @Transactional
    void getAllOpportunitiesByTitleIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedOpportunity = opportunityRepository.saveAndFlush(opportunity);

        // Get all the opportunityList where title is not null
        defaultOpportunityFiltering("title.specified=true", "title.specified=false");
    }

    @Test
    @Transactional
    void getAllOpportunitiesByTitleContainsSomething() throws Exception {
        // Initialize the database
        insertedOpportunity = opportunityRepository.saveAndFlush(opportunity);

        // Get all the opportunityList where title contains
        defaultOpportunityFiltering("title.contains=" + DEFAULT_TITLE, "title.contains=" + UPDATED_TITLE);
    }

    @Test
    @Transactional
    void getAllOpportunitiesByTitleNotContainsSomething() throws Exception {
        // Initialize the database
        insertedOpportunity = opportunityRepository.saveAndFlush(opportunity);

        // Get all the opportunityList where title does not contain
        defaultOpportunityFiltering("title.doesNotContain=" + UPDATED_TITLE, "title.doesNotContain=" + DEFAULT_TITLE);
    }

    @Test
    @Transactional
    void getAllOpportunitiesByStatusIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedOpportunity = opportunityRepository.saveAndFlush(opportunity);

        // Get all the opportunityList where status equals to
        defaultOpportunityFiltering("status.equals=" + DEFAULT_STATUS, "status.equals=" + UPDATED_STATUS);
    }

    @Test
    @Transactional
    void getAllOpportunitiesByStatusIsInShouldWork() throws Exception {
        // Initialize the database
        insertedOpportunity = opportunityRepository.saveAndFlush(opportunity);

        // Get all the opportunityList where status in
        defaultOpportunityFiltering("status.in=" + DEFAULT_STATUS + "," + UPDATED_STATUS, "status.in=" + UPDATED_STATUS);
    }

    @Test
    @Transactional
    void getAllOpportunitiesByStatusIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedOpportunity = opportunityRepository.saveAndFlush(opportunity);

        // Get all the opportunityList where status is not null
        defaultOpportunityFiltering("status.specified=true", "status.specified=false");
    }

    @Test
    @Transactional
    void getAllOpportunitiesByValueratingIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedOpportunity = opportunityRepository.saveAndFlush(opportunity);

        // Get all the opportunityList where valuerating equals to
        defaultOpportunityFiltering("valuerating.equals=" + DEFAULT_VALUERATING, "valuerating.equals=" + UPDATED_VALUERATING);
    }

    @Test
    @Transactional
    void getAllOpportunitiesByValueratingIsInShouldWork() throws Exception {
        // Initialize the database
        insertedOpportunity = opportunityRepository.saveAndFlush(opportunity);

        // Get all the opportunityList where valuerating in
        defaultOpportunityFiltering(
            "valuerating.in=" + DEFAULT_VALUERATING + "," + UPDATED_VALUERATING,
            "valuerating.in=" + UPDATED_VALUERATING
        );
    }

    @Test
    @Transactional
    void getAllOpportunitiesByValueratingIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedOpportunity = opportunityRepository.saveAndFlush(opportunity);

        // Get all the opportunityList where valuerating is not null
        defaultOpportunityFiltering("valuerating.specified=true", "valuerating.specified=false");
    }

    @Test
    @Transactional
    void getAllOpportunitiesByValueratingIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedOpportunity = opportunityRepository.saveAndFlush(opportunity);

        // Get all the opportunityList where valuerating is greater than or equal to
        defaultOpportunityFiltering(
            "valuerating.greaterThanOrEqual=" + DEFAULT_VALUERATING,
            "valuerating.greaterThanOrEqual=" + (DEFAULT_VALUERATING + 1)
        );
    }

    @Test
    @Transactional
    void getAllOpportunitiesByValueratingIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedOpportunity = opportunityRepository.saveAndFlush(opportunity);

        // Get all the opportunityList where valuerating is less than or equal to
        defaultOpportunityFiltering(
            "valuerating.lessThanOrEqual=" + DEFAULT_VALUERATING,
            "valuerating.lessThanOrEqual=" + SMALLER_VALUERATING
        );
    }

    @Test
    @Transactional
    void getAllOpportunitiesByValueratingIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedOpportunity = opportunityRepository.saveAndFlush(opportunity);

        // Get all the opportunityList where valuerating is less than
        defaultOpportunityFiltering("valuerating.lessThan=" + (DEFAULT_VALUERATING + 1), "valuerating.lessThan=" + DEFAULT_VALUERATING);
    }

    @Test
    @Transactional
    void getAllOpportunitiesByValueratingIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedOpportunity = opportunityRepository.saveAndFlush(opportunity);

        // Get all the opportunityList where valuerating is greater than
        defaultOpportunityFiltering("valuerating.greaterThan=" + SMALLER_VALUERATING, "valuerating.greaterThan=" + DEFAULT_VALUERATING);
    }

    @Test
    @Transactional
    void getAllOpportunitiesByPriorityIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedOpportunity = opportunityRepository.saveAndFlush(opportunity);

        // Get all the opportunityList where priority equals to
        defaultOpportunityFiltering("priority.equals=" + DEFAULT_PRIORITY, "priority.equals=" + UPDATED_PRIORITY);
    }

    @Test
    @Transactional
    void getAllOpportunitiesByPriorityIsInShouldWork() throws Exception {
        // Initialize the database
        insertedOpportunity = opportunityRepository.saveAndFlush(opportunity);

        // Get all the opportunityList where priority in
        defaultOpportunityFiltering("priority.in=" + DEFAULT_PRIORITY + "," + UPDATED_PRIORITY, "priority.in=" + UPDATED_PRIORITY);
    }

    @Test
    @Transactional
    void getAllOpportunitiesByPriorityIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedOpportunity = opportunityRepository.saveAndFlush(opportunity);

        // Get all the opportunityList where priority is not null
        defaultOpportunityFiltering("priority.specified=true", "priority.specified=false");
    }

    @Test
    @Transactional
    void getAllOpportunitiesByPriorityIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedOpportunity = opportunityRepository.saveAndFlush(opportunity);

        // Get all the opportunityList where priority is greater than or equal to
        defaultOpportunityFiltering(
            "priority.greaterThanOrEqual=" + DEFAULT_PRIORITY,
            "priority.greaterThanOrEqual=" + (DEFAULT_PRIORITY + 1)
        );
    }

    @Test
    @Transactional
    void getAllOpportunitiesByPriorityIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedOpportunity = opportunityRepository.saveAndFlush(opportunity);

        // Get all the opportunityList where priority is less than or equal to
        defaultOpportunityFiltering("priority.lessThanOrEqual=" + DEFAULT_PRIORITY, "priority.lessThanOrEqual=" + SMALLER_PRIORITY);
    }

    @Test
    @Transactional
    void getAllOpportunitiesByPriorityIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedOpportunity = opportunityRepository.saveAndFlush(opportunity);

        // Get all the opportunityList where priority is less than
        defaultOpportunityFiltering("priority.lessThan=" + (DEFAULT_PRIORITY + 1), "priority.lessThan=" + DEFAULT_PRIORITY);
    }

    @Test
    @Transactional
    void getAllOpportunitiesByPriorityIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedOpportunity = opportunityRepository.saveAndFlush(opportunity);

        // Get all the opportunityList where priority is greater than
        defaultOpportunityFiltering("priority.greaterThan=" + SMALLER_PRIORITY, "priority.greaterThan=" + DEFAULT_PRIORITY);
    }

    @Test
    @Transactional
    void getAllOpportunitiesBySortOrderIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedOpportunity = opportunityRepository.saveAndFlush(opportunity);

        // Get all the opportunityList where sortOrder equals to
        defaultOpportunityFiltering("sortOrder.equals=" + DEFAULT_SORT_ORDER, "sortOrder.equals=" + UPDATED_SORT_ORDER);
    }

    @Test
    @Transactional
    void getAllOpportunitiesBySortOrderIsInShouldWork() throws Exception {
        // Initialize the database
        insertedOpportunity = opportunityRepository.saveAndFlush(opportunity);

        // Get all the opportunityList where sortOrder in
        defaultOpportunityFiltering("sortOrder.in=" + DEFAULT_SORT_ORDER + "," + UPDATED_SORT_ORDER, "sortOrder.in=" + UPDATED_SORT_ORDER);
    }

    @Test
    @Transactional
    void getAllOpportunitiesBySortOrderIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedOpportunity = opportunityRepository.saveAndFlush(opportunity);

        // Get all the opportunityList where sortOrder is not null
        defaultOpportunityFiltering("sortOrder.specified=true", "sortOrder.specified=false");
    }

    @Test
    @Transactional
    void getAllOpportunitiesBySortOrderIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedOpportunity = opportunityRepository.saveAndFlush(opportunity);

        // Get all the opportunityList where sortOrder is greater than or equal to
        defaultOpportunityFiltering(
            "sortOrder.greaterThanOrEqual=" + DEFAULT_SORT_ORDER,
            "sortOrder.greaterThanOrEqual=" + UPDATED_SORT_ORDER
        );
    }

    @Test
    @Transactional
    void getAllOpportunitiesBySortOrderIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedOpportunity = opportunityRepository.saveAndFlush(opportunity);

        // Get all the opportunityList where sortOrder is less than or equal to
        defaultOpportunityFiltering("sortOrder.lessThanOrEqual=" + DEFAULT_SORT_ORDER, "sortOrder.lessThanOrEqual=" + SMALLER_SORT_ORDER);
    }

    @Test
    @Transactional
    void getAllOpportunitiesBySortOrderIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedOpportunity = opportunityRepository.saveAndFlush(opportunity);

        // Get all the opportunityList where sortOrder is less than
        defaultOpportunityFiltering("sortOrder.lessThan=" + UPDATED_SORT_ORDER, "sortOrder.lessThan=" + DEFAULT_SORT_ORDER);
    }

    @Test
    @Transactional
    void getAllOpportunitiesBySortOrderIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedOpportunity = opportunityRepository.saveAndFlush(opportunity);

        // Get all the opportunityList where sortOrder is greater than
        defaultOpportunityFiltering("sortOrder.greaterThan=" + SMALLER_SORT_ORDER, "sortOrder.greaterThan=" + DEFAULT_SORT_ORDER);
    }

    @Test
    @Transactional
    void getAllOpportunitiesByCreatedDateIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedOpportunity = opportunityRepository.saveAndFlush(opportunity);

        // Get all the opportunityList where createdDate equals to
        defaultOpportunityFiltering("createdDate.equals=" + DEFAULT_CREATED_DATE, "createdDate.equals=" + UPDATED_CREATED_DATE);
    }

    @Test
    @Transactional
    void getAllOpportunitiesByCreatedDateIsInShouldWork() throws Exception {
        // Initialize the database
        insertedOpportunity = opportunityRepository.saveAndFlush(opportunity);

        // Get all the opportunityList where createdDate in
        defaultOpportunityFiltering(
            "createdDate.in=" + DEFAULT_CREATED_DATE + "," + UPDATED_CREATED_DATE,
            "createdDate.in=" + UPDATED_CREATED_DATE
        );
    }

    @Test
    @Transactional
    void getAllOpportunitiesByCreatedDateIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedOpportunity = opportunityRepository.saveAndFlush(opportunity);

        // Get all the opportunityList where createdDate is not null
        defaultOpportunityFiltering("createdDate.specified=true", "createdDate.specified=false");
    }

    @Test
    @Transactional
    void getAllOpportunitiesByLastModifiedDateIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedOpportunity = opportunityRepository.saveAndFlush(opportunity);

        // Get all the opportunityList where lastModifiedDate equals to
        defaultOpportunityFiltering(
            "lastModifiedDate.equals=" + DEFAULT_LAST_MODIFIED_DATE,
            "lastModifiedDate.equals=" + UPDATED_LAST_MODIFIED_DATE
        );
    }

    @Test
    @Transactional
    void getAllOpportunitiesByLastModifiedDateIsInShouldWork() throws Exception {
        // Initialize the database
        insertedOpportunity = opportunityRepository.saveAndFlush(opportunity);

        // Get all the opportunityList where lastModifiedDate in
        defaultOpportunityFiltering(
            "lastModifiedDate.in=" + DEFAULT_LAST_MODIFIED_DATE + "," + UPDATED_LAST_MODIFIED_DATE,
            "lastModifiedDate.in=" + UPDATED_LAST_MODIFIED_DATE
        );
    }

    @Test
    @Transactional
    void getAllOpportunitiesByLastModifiedDateIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedOpportunity = opportunityRepository.saveAndFlush(opportunity);

        // Get all the opportunityList where lastModifiedDate is not null
        defaultOpportunityFiltering("lastModifiedDate.specified=true", "lastModifiedDate.specified=false");
    }

    @Test
    @Transactional
    void getAllOpportunitiesByOutcomeIsEqualToSomething() throws Exception {
        Outcome outcome;
        if (TestUtil.findAll(em, Outcome.class).isEmpty()) {
            opportunityRepository.saveAndFlush(opportunity);
            outcome = OutcomeResourceIT.createEntity(em);
        } else {
            outcome = TestUtil.findAll(em, Outcome.class).get(0);
        }
        em.persist(outcome);
        em.flush();
        opportunity.setOutcome(outcome);
        opportunityRepository.saveAndFlush(opportunity);
        Long outcomeId = outcome.getId();
        // Get all the opportunityList where outcome equals to outcomeId
        defaultOpportunityShouldBeFound("outcomeId.equals=" + outcomeId);

        // Get all the opportunityList where outcome equals to (outcomeId + 1)
        defaultOpportunityShouldNotBeFound("outcomeId.equals=" + (outcomeId + 1));
    }

    @Test
    @Transactional
    void getAllOpportunitiesByParentIsEqualToSomething() throws Exception {
        Opportunity parent;
        if (TestUtil.findAll(em, Opportunity.class).isEmpty()) {
            opportunityRepository.saveAndFlush(opportunity);
            parent = OpportunityResourceIT.createEntity(em);
        } else {
            parent = TestUtil.findAll(em, Opportunity.class).get(0);
        }
        em.persist(parent);
        em.flush();
        opportunity.setParent(parent);
        opportunityRepository.saveAndFlush(opportunity);
        Long parentId = parent.getId();
        // Get all the opportunityList where parent equals to parentId
        defaultOpportunityShouldBeFound("parentId.equals=" + parentId);

        // Get all the opportunityList where parent equals to (parentId + 1)
        defaultOpportunityShouldNotBeFound("parentId.equals=" + (parentId + 1));
    }

    @Test
    @Transactional
    void getAllOpportunitiesByOwnerIsEqualToSomething() throws Exception {
        User owner;
        if (TestUtil.findAll(em, User.class).isEmpty()) {
            opportunityRepository.saveAndFlush(opportunity);
            owner = UserResourceIT.createEntity();
        } else {
            owner = TestUtil.findAll(em, User.class).get(0);
        }
        em.persist(owner);
        em.flush();
        opportunity.setOwner(owner);
        opportunityRepository.saveAndFlush(opportunity);
        String ownerId = owner.getId();
        // Get all the opportunityList where owner equals to ownerId
        defaultOpportunityShouldBeFound("ownerId.equals=" + ownerId);

        // Get all the opportunityList where owner equals to "invalid-id"
        defaultOpportunityShouldNotBeFound("ownerId.equals=" + "invalid-id");
    }

    @Test
    @Transactional
    void getAllOpportunitiesByInterviewIsEqualToSomething() throws Exception {
        Interview interview;
        if (TestUtil.findAll(em, Interview.class).isEmpty()) {
            opportunityRepository.saveAndFlush(opportunity);
            interview = InterviewResourceIT.createEntity(em);
        } else {
            interview = TestUtil.findAll(em, Interview.class).get(0);
        }
        em.persist(interview);
        em.flush();
        opportunity.addInterview(interview);
        opportunityRepository.saveAndFlush(opportunity);
        Long interviewId = interview.getId();
        // Get all the opportunityList where interview equals to interviewId
        defaultOpportunityShouldBeFound("interviewId.equals=" + interviewId);

        // Get all the opportunityList where interview equals to (interviewId + 1)
        defaultOpportunityShouldNotBeFound("interviewId.equals=" + (interviewId + 1));
    }

    @Test
    @Transactional
    void getAllOpportunitiesByTagIsEqualToSomething() throws Exception {
        Tag tag;
        if (TestUtil.findAll(em, Tag.class).isEmpty()) {
            opportunityRepository.saveAndFlush(opportunity);
            tag = TagResourceIT.createEntity(em);
        } else {
            tag = TestUtil.findAll(em, Tag.class).get(0);
        }
        em.persist(tag);
        em.flush();
        opportunity.addTag(tag);
        opportunityRepository.saveAndFlush(opportunity);
        Long tagId = tag.getId();
        // Get all the opportunityList where tag equals to tagId
        defaultOpportunityShouldBeFound("tagId.equals=" + tagId);

        // Get all the opportunityList where tag equals to (tagId + 1)
        defaultOpportunityShouldNotBeFound("tagId.equals=" + (tagId + 1));
    }

    private void defaultOpportunityFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultOpportunityShouldBeFound(shouldBeFound);
        defaultOpportunityShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultOpportunityShouldBeFound(String filter) throws Exception {
        restOpportunityMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(opportunity.getId().intValue())))
            .andExpect(jsonPath("$.[*].title").value(hasItem(DEFAULT_TITLE)))
            .andExpect(jsonPath("$.[*].description").value(hasItem(DEFAULT_DESCRIPTION)))
            .andExpect(jsonPath("$.[*].status").value(hasItem(DEFAULT_STATUS.toString())))
            .andExpect(jsonPath("$.[*].valuerating").value(hasItem(DEFAULT_VALUERATING)))
            .andExpect(jsonPath("$.[*].priority").value(hasItem(DEFAULT_PRIORITY)))
            .andExpect(jsonPath("$.[*].sortOrder").value(hasItem(DEFAULT_SORT_ORDER)))
            .andExpect(jsonPath("$.[*].createdDate").value(hasItem(DEFAULT_CREATED_DATE.toString())))
            .andExpect(jsonPath("$.[*].lastModifiedDate").value(hasItem(DEFAULT_LAST_MODIFIED_DATE.toString())));

        // Check, that the count call also returns 1
        restOpportunityMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultOpportunityShouldNotBeFound(String filter) throws Exception {
        restOpportunityMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restOpportunityMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingOpportunity() throws Exception {
        // Get the opportunity
        restOpportunityMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingOpportunity() throws Exception {
        // Initialize the database
        insertedOpportunity = opportunityRepository.saveAndFlush(opportunity);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the opportunity
        Opportunity updatedOpportunity = opportunityRepository.findById(opportunity.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedOpportunity are not directly saved in db
        em.detach(updatedOpportunity);
        updatedOpportunity
            .title(UPDATED_TITLE)
            .description(UPDATED_DESCRIPTION)
            .status(UPDATED_STATUS)
            .valuerating(UPDATED_VALUERATING)
            .priority(UPDATED_PRIORITY)
            .sortOrder(UPDATED_SORT_ORDER)
            .createdDate(UPDATED_CREATED_DATE)
            .lastModifiedDate(UPDATED_LAST_MODIFIED_DATE);
        OpportunityDTO opportunityDTO = opportunityMapper.toDto(updatedOpportunity);

        restOpportunityMockMvc
            .perform(
                put(ENTITY_API_URL_ID, opportunityDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(opportunityDTO))
            )
            .andExpect(status().isOk());

        // Validate the Opportunity in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedOpportunityToMatchAllProperties(updatedOpportunity);
    }

    @Test
    @Transactional
    void putNonExistingOpportunity() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        opportunity.setId(longCount.incrementAndGet());

        // Create the Opportunity
        OpportunityDTO opportunityDTO = opportunityMapper.toDto(opportunity);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restOpportunityMockMvc
            .perform(
                put(ENTITY_API_URL_ID, opportunityDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(opportunityDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Opportunity in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchOpportunity() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        opportunity.setId(longCount.incrementAndGet());

        // Create the Opportunity
        OpportunityDTO opportunityDTO = opportunityMapper.toDto(opportunity);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restOpportunityMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(opportunityDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Opportunity in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamOpportunity() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        opportunity.setId(longCount.incrementAndGet());

        // Create the Opportunity
        OpportunityDTO opportunityDTO = opportunityMapper.toDto(opportunity);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restOpportunityMockMvc
            .perform(put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(opportunityDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Opportunity in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateOpportunityWithPatch() throws Exception {
        // Initialize the database
        insertedOpportunity = opportunityRepository.saveAndFlush(opportunity);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the opportunity using partial update
        Opportunity partialUpdatedOpportunity = new Opportunity();
        partialUpdatedOpportunity.setId(opportunity.getId());

        partialUpdatedOpportunity
            .valuerating(UPDATED_VALUERATING)
            .priority(UPDATED_PRIORITY)
            .sortOrder(UPDATED_SORT_ORDER)
            .createdDate(UPDATED_CREATED_DATE);

        restOpportunityMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedOpportunity.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedOpportunity))
            )
            .andExpect(status().isOk());

        // Validate the Opportunity in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertOpportunityUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedOpportunity, opportunity),
            getPersistedOpportunity(opportunity)
        );
    }

    @Test
    @Transactional
    void fullUpdateOpportunityWithPatch() throws Exception {
        // Initialize the database
        insertedOpportunity = opportunityRepository.saveAndFlush(opportunity);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the opportunity using partial update
        Opportunity partialUpdatedOpportunity = new Opportunity();
        partialUpdatedOpportunity.setId(opportunity.getId());

        partialUpdatedOpportunity
            .title(UPDATED_TITLE)
            .description(UPDATED_DESCRIPTION)
            .status(UPDATED_STATUS)
            .valuerating(UPDATED_VALUERATING)
            .priority(UPDATED_PRIORITY)
            .sortOrder(UPDATED_SORT_ORDER)
            .createdDate(UPDATED_CREATED_DATE)
            .lastModifiedDate(UPDATED_LAST_MODIFIED_DATE);

        restOpportunityMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedOpportunity.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedOpportunity))
            )
            .andExpect(status().isOk());

        // Validate the Opportunity in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertOpportunityUpdatableFieldsEquals(partialUpdatedOpportunity, getPersistedOpportunity(partialUpdatedOpportunity));
    }

    @Test
    @Transactional
    void patchNonExistingOpportunity() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        opportunity.setId(longCount.incrementAndGet());

        // Create the Opportunity
        OpportunityDTO opportunityDTO = opportunityMapper.toDto(opportunity);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restOpportunityMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, opportunityDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(opportunityDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Opportunity in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchOpportunity() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        opportunity.setId(longCount.incrementAndGet());

        // Create the Opportunity
        OpportunityDTO opportunityDTO = opportunityMapper.toDto(opportunity);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restOpportunityMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(opportunityDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Opportunity in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamOpportunity() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        opportunity.setId(longCount.incrementAndGet());

        // Create the Opportunity
        OpportunityDTO opportunityDTO = opportunityMapper.toDto(opportunity);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restOpportunityMockMvc
            .perform(
                patch(ENTITY_API_URL).with(csrf()).contentType("application/merge-patch+json").content(om.writeValueAsBytes(opportunityDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the Opportunity in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteOpportunity() throws Exception {
        // Initialize the database
        insertedOpportunity = opportunityRepository.saveAndFlush(opportunity);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the opportunity
        restOpportunityMockMvc
            .perform(delete(ENTITY_API_URL_ID, opportunity.getId()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return opportunityRepository.count();
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
        return opportunityRepository.findById(opportunity.getId()).orElseThrow();
    }

    protected void assertPersistedOpportunityToMatchAllProperties(Opportunity expectedOpportunity) {
        assertOpportunityAllPropertiesEquals(expectedOpportunity, getPersistedOpportunity(expectedOpportunity));
    }

    protected void assertPersistedOpportunityToMatchUpdatableProperties(Opportunity expectedOpportunity) {
        assertOpportunityAllUpdatablePropertiesEquals(expectedOpportunity, getPersistedOpportunity(expectedOpportunity));
    }
}
