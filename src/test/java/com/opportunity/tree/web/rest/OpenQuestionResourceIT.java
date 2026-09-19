package com.opportunity.tree.web.rest;

import static com.opportunity.tree.domain.OpenQuestionAsserts.*;
import static com.opportunity.tree.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opportunity.tree.IntegrationTest;
import com.opportunity.tree.domain.OpenQuestion;
import com.opportunity.tree.domain.Opportunity;
import com.opportunity.tree.repository.OpenQuestionRepository;
import com.opportunity.tree.service.OpenQuestionService;
import com.opportunity.tree.service.dto.OpenQuestionDTO;
import com.opportunity.tree.service.mapper.OpenQuestionMapper;
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
 * Integration tests for the {@link OpenQuestionResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = "ROLE_ADMIN")
class OpenQuestionResourceIT {

    private static final String DEFAULT_QUESTION_TEXT = "AAAAAAAAAA";
    private static final String UPDATED_QUESTION_TEXT = "BBBBBBBBBB";

    private static final Boolean DEFAULT_DONE = false;
    private static final Boolean UPDATED_DONE = true;

    private static final Integer DEFAULT_SORT_ORDER = 1;
    private static final Integer UPDATED_SORT_ORDER = 2;

    private static final Instant DEFAULT_CREATED_DATE = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CREATED_DATE = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/open-questions";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2L * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private OpenQuestionRepository openQuestionRepository;

    @Mock
    private OpenQuestionRepository openQuestionRepositoryMock;

    @Autowired
    private OpenQuestionMapper openQuestionMapper;

    @Mock
    private OpenQuestionService openQuestionServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restOpenQuestionMockMvc;

    private OpenQuestion openQuestion;

    private OpenQuestion insertedOpenQuestion;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static OpenQuestion createEntity(EntityManager em) {
        OpenQuestion openQuestion = new OpenQuestion()
            .questionText(DEFAULT_QUESTION_TEXT)
            .done(DEFAULT_DONE)
            .sortOrder(DEFAULT_SORT_ORDER)
            .createdDate(DEFAULT_CREATED_DATE);
        // Add required entity
        Opportunity opportunity;
        if (TestUtil.findAll(em, Opportunity.class).isEmpty()) {
            opportunity = OpportunityResourceIT.createEntity(em);
            em.persist(opportunity);
            em.flush();
        } else {
            opportunity = TestUtil.findAll(em, Opportunity.class).get(0);
        }
        openQuestion.setOpportunity(opportunity);
        return openQuestion;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static OpenQuestion createUpdatedEntity(EntityManager em) {
        OpenQuestion updatedOpenQuestion = new OpenQuestion()
            .questionText(UPDATED_QUESTION_TEXT)
            .done(UPDATED_DONE)
            .sortOrder(UPDATED_SORT_ORDER)
            .createdDate(UPDATED_CREATED_DATE);
        // Add required entity
        Opportunity opportunity;
        if (TestUtil.findAll(em, Opportunity.class).isEmpty()) {
            opportunity = OpportunityResourceIT.createUpdatedEntity(em);
            em.persist(opportunity);
            em.flush();
        } else {
            opportunity = TestUtil.findAll(em, Opportunity.class).get(0);
        }
        updatedOpenQuestion.setOpportunity(opportunity);
        return updatedOpenQuestion;
    }

    @BeforeEach
    void initTest() {
        openQuestion = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedOpenQuestion != null) {
            openQuestionRepository.delete(insertedOpenQuestion);
            insertedOpenQuestion = null;
        }
    }

    @Test
    @Transactional
    void createOpenQuestion() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the OpenQuestion
        OpenQuestionDTO openQuestionDTO = openQuestionMapper.toDto(openQuestion);
        var returnedOpenQuestionDTO = om.readValue(
            restOpenQuestionMockMvc
                .perform(
                    post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(openQuestionDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            OpenQuestionDTO.class
        );

        // Validate the OpenQuestion in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedOpenQuestion = openQuestionMapper.toEntity(returnedOpenQuestionDTO);
        assertOpenQuestionUpdatableFieldsEquals(returnedOpenQuestion, getPersistedOpenQuestion(returnedOpenQuestion));

        insertedOpenQuestion = returnedOpenQuestion;
    }

    @Test
    @Transactional
    void createOpenQuestionWithExistingId() throws Exception {
        // Create the OpenQuestion with an existing ID
        openQuestion.setId(1L);
        OpenQuestionDTO openQuestionDTO = openQuestionMapper.toDto(openQuestion);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restOpenQuestionMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(openQuestionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the OpenQuestion in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkQuestionTextIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        openQuestion.setQuestionText(null);

        // Create the OpenQuestion, which fails.
        OpenQuestionDTO openQuestionDTO = openQuestionMapper.toDto(openQuestion);

        restOpenQuestionMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(openQuestionDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkDoneIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        openQuestion.setDone(null);

        // Create the OpenQuestion, which fails.
        OpenQuestionDTO openQuestionDTO = openQuestionMapper.toDto(openQuestion);

        restOpenQuestionMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(openQuestionDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkSortOrderIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        openQuestion.setSortOrder(null);

        // Create the OpenQuestion, which fails.
        OpenQuestionDTO openQuestionDTO = openQuestionMapper.toDto(openQuestion);

        restOpenQuestionMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(openQuestionDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkCreatedDateIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        openQuestion.setCreatedDate(null);

        // Create the OpenQuestion, which fails.
        OpenQuestionDTO openQuestionDTO = openQuestionMapper.toDto(openQuestion);

        restOpenQuestionMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(openQuestionDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllOpenQuestions() throws Exception {
        // Initialize the database
        insertedOpenQuestion = openQuestionRepository.saveAndFlush(openQuestion);

        // Get all the openQuestionList
        restOpenQuestionMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(openQuestion.getId().intValue())))
            .andExpect(jsonPath("$.[*].questionText").value(hasItem(DEFAULT_QUESTION_TEXT)))
            .andExpect(jsonPath("$.[*].done").value(hasItem(DEFAULT_DONE)))
            .andExpect(jsonPath("$.[*].sortOrder").value(hasItem(DEFAULT_SORT_ORDER)))
            .andExpect(jsonPath("$.[*].createdDate").value(hasItem(DEFAULT_CREATED_DATE.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllOpenQuestionsWithEagerRelationshipsIsEnabled() throws Exception {
        when(openQuestionServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restOpenQuestionMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(openQuestionServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllOpenQuestionsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(openQuestionServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restOpenQuestionMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(openQuestionRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getOpenQuestion() throws Exception {
        // Initialize the database
        insertedOpenQuestion = openQuestionRepository.saveAndFlush(openQuestion);

        // Get the openQuestion
        restOpenQuestionMockMvc
            .perform(get(ENTITY_API_URL_ID, openQuestion.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(openQuestion.getId().intValue()))
            .andExpect(jsonPath("$.questionText").value(DEFAULT_QUESTION_TEXT))
            .andExpect(jsonPath("$.done").value(DEFAULT_DONE))
            .andExpect(jsonPath("$.sortOrder").value(DEFAULT_SORT_ORDER))
            .andExpect(jsonPath("$.createdDate").value(DEFAULT_CREATED_DATE.toString()));
    }

    @Test
    @Transactional
    void getNonExistingOpenQuestion() throws Exception {
        // Get the openQuestion
        restOpenQuestionMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingOpenQuestion() throws Exception {
        // Initialize the database
        insertedOpenQuestion = openQuestionRepository.saveAndFlush(openQuestion);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the openQuestion
        OpenQuestion updatedOpenQuestion = openQuestionRepository.findById(openQuestion.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedOpenQuestion are not directly saved in db
        em.detach(updatedOpenQuestion);
        updatedOpenQuestion
            .questionText(UPDATED_QUESTION_TEXT)
            .done(UPDATED_DONE)
            .sortOrder(UPDATED_SORT_ORDER)
            .createdDate(UPDATED_CREATED_DATE);
        OpenQuestionDTO openQuestionDTO = openQuestionMapper.toDto(updatedOpenQuestion);

        restOpenQuestionMockMvc
            .perform(
                put(ENTITY_API_URL_ID, openQuestionDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(openQuestionDTO))
            )
            .andExpect(status().isOk());

        // Validate the OpenQuestion in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedOpenQuestionToMatchAllProperties(updatedOpenQuestion);
    }

    @Test
    @Transactional
    void putNonExistingOpenQuestion() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        openQuestion.setId(longCount.incrementAndGet());

        // Create the OpenQuestion
        OpenQuestionDTO openQuestionDTO = openQuestionMapper.toDto(openQuestion);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restOpenQuestionMockMvc
            .perform(
                put(ENTITY_API_URL_ID, openQuestionDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(openQuestionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the OpenQuestion in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchOpenQuestion() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        openQuestion.setId(longCount.incrementAndGet());

        // Create the OpenQuestion
        OpenQuestionDTO openQuestionDTO = openQuestionMapper.toDto(openQuestion);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restOpenQuestionMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(openQuestionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the OpenQuestion in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamOpenQuestion() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        openQuestion.setId(longCount.incrementAndGet());

        // Create the OpenQuestion
        OpenQuestionDTO openQuestionDTO = openQuestionMapper.toDto(openQuestion);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restOpenQuestionMockMvc
            .perform(
                put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(openQuestionDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the OpenQuestion in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateOpenQuestionWithPatch() throws Exception {
        // Initialize the database
        insertedOpenQuestion = openQuestionRepository.saveAndFlush(openQuestion);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the openQuestion using partial update
        OpenQuestion partialUpdatedOpenQuestion = new OpenQuestion();
        partialUpdatedOpenQuestion.setId(openQuestion.getId());

        partialUpdatedOpenQuestion
            .questionText(UPDATED_QUESTION_TEXT)
            .done(UPDATED_DONE)
            .sortOrder(UPDATED_SORT_ORDER)
            .createdDate(UPDATED_CREATED_DATE);

        restOpenQuestionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedOpenQuestion.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedOpenQuestion))
            )
            .andExpect(status().isOk());

        // Validate the OpenQuestion in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertOpenQuestionUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedOpenQuestion, openQuestion),
            getPersistedOpenQuestion(openQuestion)
        );
    }

    @Test
    @Transactional
    void fullUpdateOpenQuestionWithPatch() throws Exception {
        // Initialize the database
        insertedOpenQuestion = openQuestionRepository.saveAndFlush(openQuestion);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the openQuestion using partial update
        OpenQuestion partialUpdatedOpenQuestion = new OpenQuestion();
        partialUpdatedOpenQuestion.setId(openQuestion.getId());

        partialUpdatedOpenQuestion
            .questionText(UPDATED_QUESTION_TEXT)
            .done(UPDATED_DONE)
            .sortOrder(UPDATED_SORT_ORDER)
            .createdDate(UPDATED_CREATED_DATE);

        restOpenQuestionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedOpenQuestion.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedOpenQuestion))
            )
            .andExpect(status().isOk());

        // Validate the OpenQuestion in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertOpenQuestionUpdatableFieldsEquals(partialUpdatedOpenQuestion, getPersistedOpenQuestion(partialUpdatedOpenQuestion));
    }

    @Test
    @Transactional
    void patchNonExistingOpenQuestion() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        openQuestion.setId(longCount.incrementAndGet());

        // Create the OpenQuestion
        OpenQuestionDTO openQuestionDTO = openQuestionMapper.toDto(openQuestion);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restOpenQuestionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, openQuestionDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(openQuestionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the OpenQuestion in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchOpenQuestion() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        openQuestion.setId(longCount.incrementAndGet());

        // Create the OpenQuestion
        OpenQuestionDTO openQuestionDTO = openQuestionMapper.toDto(openQuestion);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restOpenQuestionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(openQuestionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the OpenQuestion in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamOpenQuestion() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        openQuestion.setId(longCount.incrementAndGet());

        // Create the OpenQuestion
        OpenQuestionDTO openQuestionDTO = openQuestionMapper.toDto(openQuestion);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restOpenQuestionMockMvc
            .perform(
                patch(ENTITY_API_URL)
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(openQuestionDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the OpenQuestion in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteOpenQuestion() throws Exception {
        // Initialize the database
        insertedOpenQuestion = openQuestionRepository.saveAndFlush(openQuestion);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the openQuestion
        restOpenQuestionMockMvc
            .perform(delete(ENTITY_API_URL_ID, openQuestion.getId()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return openQuestionRepository.count();
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

    protected OpenQuestion getPersistedOpenQuestion(OpenQuestion openQuestion) {
        return openQuestionRepository.findById(openQuestion.getId()).orElseThrow();
    }

    protected void assertPersistedOpenQuestionToMatchAllProperties(OpenQuestion expectedOpenQuestion) {
        assertOpenQuestionAllPropertiesEquals(expectedOpenQuestion, getPersistedOpenQuestion(expectedOpenQuestion));
    }

    protected void assertPersistedOpenQuestionToMatchUpdatableProperties(OpenQuestion expectedOpenQuestion) {
        assertOpenQuestionAllUpdatablePropertiesEquals(expectedOpenQuestion, getPersistedOpenQuestion(expectedOpenQuestion));
    }
}
