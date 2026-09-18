package com.opportunity.tree.web.rest;

import static com.opportunity.tree.domain.TeamMemberAsserts.*;
import static com.opportunity.tree.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opportunity.tree.IntegrationTest;
import com.opportunity.tree.domain.Team;
import com.opportunity.tree.domain.TeamMember;
import com.opportunity.tree.domain.User;
import com.opportunity.tree.domain.enumeration.TeamRole;
import com.opportunity.tree.repository.TeamMemberRepository;
import com.opportunity.tree.repository.UserRepository;
import com.opportunity.tree.service.TeamMemberService;
import com.opportunity.tree.service.dto.TeamMemberDTO;
import com.opportunity.tree.service.mapper.TeamMemberMapper;
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
 * Integration tests for the {@link TeamMemberResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = "ROLE_ADMIN")
class TeamMemberResourceIT {

    private static final TeamRole DEFAULT_ROLE = TeamRole.OWNER;
    private static final TeamRole UPDATED_ROLE = TeamRole.EDITOR;

    private static final Instant DEFAULT_JOINED_DATE = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_JOINED_DATE = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/team-members";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2L * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private TeamMemberRepository teamMemberRepository;

    @Autowired
    private UserRepository userRepository;

    @Mock
    private TeamMemberRepository teamMemberRepositoryMock;

    @Autowired
    private TeamMemberMapper teamMemberMapper;

    @Mock
    private TeamMemberService teamMemberServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restTeamMemberMockMvc;

    private TeamMember teamMember;

    private TeamMember insertedTeamMember;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static TeamMember createEntity(EntityManager em) {
        TeamMember teamMember = new TeamMember().role(DEFAULT_ROLE).joinedDate(DEFAULT_JOINED_DATE);
        // Add required entity
        Team team;
        if (TestUtil.findAll(em, Team.class).isEmpty()) {
            team = TeamResourceIT.createEntity();
            em.persist(team);
            em.flush();
        } else {
            team = TestUtil.findAll(em, Team.class).get(0);
        }
        teamMember.setTeam(team);
        // Add required entity
        User user = UserResourceIT.createEntity();
        em.persist(user);
        em.flush();
        teamMember.setUser(user);
        return teamMember;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static TeamMember createUpdatedEntity(EntityManager em) {
        TeamMember updatedTeamMember = new TeamMember().role(UPDATED_ROLE).joinedDate(UPDATED_JOINED_DATE);
        // Add required entity
        Team team;
        if (TestUtil.findAll(em, Team.class).isEmpty()) {
            team = TeamResourceIT.createUpdatedEntity();
            em.persist(team);
            em.flush();
        } else {
            team = TestUtil.findAll(em, Team.class).get(0);
        }
        updatedTeamMember.setTeam(team);
        // Add required entity
        User user = UserResourceIT.createEntity();
        em.persist(user);
        em.flush();
        updatedTeamMember.setUser(user);
        return updatedTeamMember;
    }

    @BeforeEach
    void initTest() {
        teamMember = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedTeamMember != null) {
            teamMemberRepository.delete(insertedTeamMember);
            insertedTeamMember = null;
        }
        userRepository.deleteAll();
    }

    @Test
    @Transactional
    void createTeamMember() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the TeamMember
        TeamMemberDTO teamMemberDTO = teamMemberMapper.toDto(teamMember);
        var returnedTeamMemberDTO = om.readValue(
            restTeamMemberMockMvc
                .perform(
                    post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(teamMemberDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            TeamMemberDTO.class
        );

        // Validate the TeamMember in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedTeamMember = teamMemberMapper.toEntity(returnedTeamMemberDTO);
        assertTeamMemberUpdatableFieldsEquals(returnedTeamMember, getPersistedTeamMember(returnedTeamMember));

        insertedTeamMember = returnedTeamMember;
    }

    @Test
    @Transactional
    void createTeamMemberWithExistingId() throws Exception {
        // Create the TeamMember with an existing ID
        teamMember.setId(1L);
        TeamMemberDTO teamMemberDTO = teamMemberMapper.toDto(teamMember);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restTeamMemberMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(teamMemberDTO)))
            .andExpect(status().isBadRequest());

        // Validate the TeamMember in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkRoleIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        teamMember.setRole(null);

        // Create the TeamMember, which fails.
        TeamMemberDTO teamMemberDTO = teamMemberMapper.toDto(teamMember);

        restTeamMemberMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(teamMemberDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkJoinedDateIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        teamMember.setJoinedDate(null);

        // Create the TeamMember, which fails.
        TeamMemberDTO teamMemberDTO = teamMemberMapper.toDto(teamMember);

        restTeamMemberMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(teamMemberDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllTeamMembers() throws Exception {
        // Initialize the database
        insertedTeamMember = teamMemberRepository.saveAndFlush(teamMember);

        // Get all the teamMemberList
        restTeamMemberMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(teamMember.getId().intValue())))
            .andExpect(jsonPath("$.[*].role").value(hasItem(DEFAULT_ROLE.toString())))
            .andExpect(jsonPath("$.[*].joinedDate").value(hasItem(DEFAULT_JOINED_DATE.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllTeamMembersWithEagerRelationshipsIsEnabled() throws Exception {
        when(teamMemberServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restTeamMemberMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(teamMemberServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllTeamMembersWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(teamMemberServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restTeamMemberMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(teamMemberRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getTeamMember() throws Exception {
        // Initialize the database
        insertedTeamMember = teamMemberRepository.saveAndFlush(teamMember);

        // Get the teamMember
        restTeamMemberMockMvc
            .perform(get(ENTITY_API_URL_ID, teamMember.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(teamMember.getId().intValue()))
            .andExpect(jsonPath("$.role").value(DEFAULT_ROLE.toString()))
            .andExpect(jsonPath("$.joinedDate").value(DEFAULT_JOINED_DATE.toString()));
    }

    @Test
    @Transactional
    void getNonExistingTeamMember() throws Exception {
        // Get the teamMember
        restTeamMemberMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingTeamMember() throws Exception {
        // Initialize the database
        insertedTeamMember = teamMemberRepository.saveAndFlush(teamMember);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the teamMember
        TeamMember updatedTeamMember = teamMemberRepository.findById(teamMember.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedTeamMember are not directly saved in db
        em.detach(updatedTeamMember);
        updatedTeamMember.role(UPDATED_ROLE).joinedDate(UPDATED_JOINED_DATE);
        TeamMemberDTO teamMemberDTO = teamMemberMapper.toDto(updatedTeamMember);

        restTeamMemberMockMvc
            .perform(
                put(ENTITY_API_URL_ID, teamMemberDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(teamMemberDTO))
            )
            .andExpect(status().isOk());

        // Validate the TeamMember in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedTeamMemberToMatchAllProperties(updatedTeamMember);
    }

    @Test
    @Transactional
    void putNonExistingTeamMember() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        teamMember.setId(longCount.incrementAndGet());

        // Create the TeamMember
        TeamMemberDTO teamMemberDTO = teamMemberMapper.toDto(teamMember);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restTeamMemberMockMvc
            .perform(
                put(ENTITY_API_URL_ID, teamMemberDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(teamMemberDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the TeamMember in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchTeamMember() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        teamMember.setId(longCount.incrementAndGet());

        // Create the TeamMember
        TeamMemberDTO teamMemberDTO = teamMemberMapper.toDto(teamMember);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restTeamMemberMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(teamMemberDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the TeamMember in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamTeamMember() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        teamMember.setId(longCount.incrementAndGet());

        // Create the TeamMember
        TeamMemberDTO teamMemberDTO = teamMemberMapper.toDto(teamMember);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restTeamMemberMockMvc
            .perform(put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(teamMemberDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the TeamMember in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateTeamMemberWithPatch() throws Exception {
        // Initialize the database
        insertedTeamMember = teamMemberRepository.saveAndFlush(teamMember);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the teamMember using partial update
        TeamMember partialUpdatedTeamMember = new TeamMember();
        partialUpdatedTeamMember.setId(teamMember.getId());

        partialUpdatedTeamMember.role(UPDATED_ROLE);

        restTeamMemberMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedTeamMember.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedTeamMember))
            )
            .andExpect(status().isOk());

        // Validate the TeamMember in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertTeamMemberUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedTeamMember, teamMember),
            getPersistedTeamMember(teamMember)
        );
    }

    @Test
    @Transactional
    void fullUpdateTeamMemberWithPatch() throws Exception {
        // Initialize the database
        insertedTeamMember = teamMemberRepository.saveAndFlush(teamMember);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the teamMember using partial update
        TeamMember partialUpdatedTeamMember = new TeamMember();
        partialUpdatedTeamMember.setId(teamMember.getId());

        partialUpdatedTeamMember.role(UPDATED_ROLE).joinedDate(UPDATED_JOINED_DATE);

        restTeamMemberMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedTeamMember.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedTeamMember))
            )
            .andExpect(status().isOk());

        // Validate the TeamMember in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertTeamMemberUpdatableFieldsEquals(partialUpdatedTeamMember, getPersistedTeamMember(partialUpdatedTeamMember));
    }

    @Test
    @Transactional
    void patchNonExistingTeamMember() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        teamMember.setId(longCount.incrementAndGet());

        // Create the TeamMember
        TeamMemberDTO teamMemberDTO = teamMemberMapper.toDto(teamMember);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restTeamMemberMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, teamMemberDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(teamMemberDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the TeamMember in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchTeamMember() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        teamMember.setId(longCount.incrementAndGet());

        // Create the TeamMember
        TeamMemberDTO teamMemberDTO = teamMemberMapper.toDto(teamMember);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restTeamMemberMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(teamMemberDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the TeamMember in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamTeamMember() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        teamMember.setId(longCount.incrementAndGet());

        // Create the TeamMember
        TeamMemberDTO teamMemberDTO = teamMemberMapper.toDto(teamMember);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restTeamMemberMockMvc
            .perform(
                patch(ENTITY_API_URL).with(csrf()).contentType("application/merge-patch+json").content(om.writeValueAsBytes(teamMemberDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the TeamMember in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteTeamMember() throws Exception {
        // Initialize the database
        insertedTeamMember = teamMemberRepository.saveAndFlush(teamMember);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the teamMember
        restTeamMemberMockMvc
            .perform(delete(ENTITY_API_URL_ID, teamMember.getId()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return teamMemberRepository.count();
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

    protected TeamMember getPersistedTeamMember(TeamMember teamMember) {
        return teamMemberRepository.findById(teamMember.getId()).orElseThrow();
    }

    protected void assertPersistedTeamMemberToMatchAllProperties(TeamMember expectedTeamMember) {
        assertTeamMemberAllPropertiesEquals(expectedTeamMember, getPersistedTeamMember(expectedTeamMember));
    }

    protected void assertPersistedTeamMemberToMatchUpdatableProperties(TeamMember expectedTeamMember) {
        assertTeamMemberAllUpdatablePropertiesEquals(expectedTeamMember, getPersistedTeamMember(expectedTeamMember));
    }
}
