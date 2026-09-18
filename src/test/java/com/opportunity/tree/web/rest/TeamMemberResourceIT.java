package com.opportunity.tree.web.rest;

import static com.opportunity.tree.domain.TeamMemberAsserts.*;
import static com.opportunity.tree.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opportunity.tree.IntegrationTest;
import com.opportunity.tree.domain.Team;
import com.opportunity.tree.domain.TeamMember;
import com.opportunity.tree.domain.User;
import com.opportunity.tree.domain.enumeration.TeamRole;
import com.opportunity.tree.repository.EntityManager;
import com.opportunity.tree.repository.TeamMemberRepository;
import com.opportunity.tree.repository.UserRepository;
import com.opportunity.tree.service.TeamMemberService;
import com.opportunity.tree.service.dto.TeamMemberDTO;
import com.opportunity.tree.service.mapper.TeamMemberMapper;
import java.time.Duration;
import java.time.Instant;
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
 * Integration tests for the {@link TeamMemberResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureWebTestClient(timeout = IntegrationTest.DEFAULT_ENTITY_TIMEOUT)
@WithMockUser
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
    private WebTestClient webTestClient;

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
        team = em.insert(TeamResourceIT.createEntity()).block();
        teamMember.setTeam(team);
        // Add required entity
        User user = em.insert(UserResourceIT.createEntity()).block();
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
        team = em.insert(TeamResourceIT.createUpdatedEntity()).block();
        updatedTeamMember.setTeam(team);
        // Add required entity
        User user = em.insert(UserResourceIT.createEntity()).block();
        updatedTeamMember.setUser(user);
        return updatedTeamMember;
    }

    public static void deleteEntities(EntityManager em) {
        try {
            em.deleteAll(TeamMember.class).block();
        } catch (Exception e) {
            // It can fail, if other entities are still referring this - it will be removed later.
        }
        TeamResourceIT.deleteEntities(em);
        UserResourceIT.deleteEntities(em);
    }

    @BeforeEach
    void setupCsrf() {
        webTestClient = webTestClient.mutateWith(csrf());
    }

    @BeforeEach
    void initTest() {
        teamMember = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedTeamMember != null) {
            teamMemberRepository.delete(insertedTeamMember).block();
            insertedTeamMember = null;
        }
        deleteEntities(em);
        userRepository.deleteAllUserAuthorities().block();
        userRepository.deleteAll().block();
    }

    @Test
    void createTeamMember() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the TeamMember
        TeamMemberDTO teamMemberDTO = teamMemberMapper.toDto(teamMember);
        var returnedTeamMemberDTO = webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(teamMemberDTO))
            .exchange()
            .expectStatus()
            .isCreated()
            .expectBody(TeamMemberDTO.class)
            .returnResult()
            .getResponseBody();

        // Validate the TeamMember in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedTeamMember = teamMemberMapper.toEntity(returnedTeamMemberDTO);
        assertTeamMemberUpdatableFieldsEquals(returnedTeamMember, getPersistedTeamMember(returnedTeamMember));

        insertedTeamMember = returnedTeamMember;
    }

    @Test
    void createTeamMemberWithExistingId() throws Exception {
        // Create the TeamMember with an existing ID
        teamMember.setId(1L);
        TeamMemberDTO teamMemberDTO = teamMemberMapper.toDto(teamMember);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(teamMemberDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the TeamMember in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    void checkRoleIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        teamMember.setRole(null);

        // Create the TeamMember, which fails.
        TeamMemberDTO teamMemberDTO = teamMemberMapper.toDto(teamMember);

        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(teamMemberDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    void checkJoinedDateIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        teamMember.setJoinedDate(null);

        // Create the TeamMember, which fails.
        TeamMemberDTO teamMemberDTO = teamMemberMapper.toDto(teamMember);

        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(teamMemberDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    void getAllTeamMembersAsStream() {
        // Initialize the database
        teamMemberRepository.save(teamMember).block();

        List<TeamMember> teamMemberList = webTestClient
            .get()
            .uri(ENTITY_API_URL)
            .accept(MediaType.APPLICATION_NDJSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentTypeCompatibleWith(MediaType.APPLICATION_NDJSON)
            .returnResult(TeamMemberDTO.class)
            .getResponseBody()
            .map(teamMemberMapper::toEntity)
            .filter(teamMember::equals)
            .collectList()
            .block(Duration.ofSeconds(5));

        assertThat(teamMemberList).isNotNull();
        assertThat(teamMemberList).hasSize(1);
        TeamMember testTeamMember = teamMemberList.get(0);

        // Test fails because reactive api returns an empty object instead of null
        // assertTeamMemberAllPropertiesEquals(teamMember, testTeamMember);
        assertTeamMemberUpdatableFieldsEquals(teamMember, testTeamMember);
    }

    @Test
    void getAllTeamMembers() {
        // Initialize the database
        insertedTeamMember = teamMemberRepository.save(teamMember).block();

        // Get all the teamMemberList
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
            .value(hasItem(teamMember.getId().intValue()))
            .jsonPath("$.[*].role")
            .value(hasItem(DEFAULT_ROLE.toString()))
            .jsonPath("$.[*].joinedDate")
            .value(hasItem(DEFAULT_JOINED_DATE.toString()));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllTeamMembersWithEagerRelationshipsIsEnabled() {
        when(teamMemberServiceMock.findAllWithEagerRelationships(any())).thenReturn(Flux.empty());

        webTestClient.get().uri(ENTITY_API_URL + "?eagerload=true").exchange().expectStatus().isOk();

        verify(teamMemberServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllTeamMembersWithEagerRelationshipsIsNotEnabled() {
        when(teamMemberServiceMock.findAllWithEagerRelationships(any())).thenReturn(Flux.empty());

        webTestClient.get().uri(ENTITY_API_URL + "?eagerload=false").exchange().expectStatus().isOk();
        verify(teamMemberRepositoryMock, times(1)).findAllWithEagerRelationships(any());
    }

    @Test
    void getTeamMember() {
        // Initialize the database
        insertedTeamMember = teamMemberRepository.save(teamMember).block();

        // Get the teamMember
        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, teamMember.getId())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.id")
            .value(is(teamMember.getId().intValue()))
            .jsonPath("$.role")
            .value(is(DEFAULT_ROLE.toString()))
            .jsonPath("$.joinedDate")
            .value(is(DEFAULT_JOINED_DATE.toString()));
    }

    @Test
    void getNonExistingTeamMember() {
        // Get the teamMember
        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, Long.MAX_VALUE)
            .accept(MediaType.APPLICATION_PROBLEM_JSON)
            .exchange()
            .expectStatus()
            .isNotFound();
    }

    @Test
    void putExistingTeamMember() throws Exception {
        // Initialize the database
        insertedTeamMember = teamMemberRepository.save(teamMember).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the teamMember
        TeamMember updatedTeamMember = teamMemberRepository.findById(teamMember.getId()).block();
        updatedTeamMember.role(UPDATED_ROLE).joinedDate(UPDATED_JOINED_DATE);
        TeamMemberDTO teamMemberDTO = teamMemberMapper.toDto(updatedTeamMember);

        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, teamMemberDTO.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(teamMemberDTO))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the TeamMember in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedTeamMemberToMatchAllProperties(updatedTeamMember);
    }

    @Test
    void putNonExistingTeamMember() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        teamMember.setId(longCount.incrementAndGet());

        // Create the TeamMember
        TeamMemberDTO teamMemberDTO = teamMemberMapper.toDto(teamMember);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, teamMemberDTO.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(teamMemberDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the TeamMember in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void putWithIdMismatchTeamMember() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        teamMember.setId(longCount.incrementAndGet());

        // Create the TeamMember
        TeamMemberDTO teamMemberDTO = teamMemberMapper.toDto(teamMember);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, longCount.incrementAndGet())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(teamMemberDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the TeamMember in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void putWithMissingIdPathParamTeamMember() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        teamMember.setId(longCount.incrementAndGet());

        // Create the TeamMember
        TeamMemberDTO teamMemberDTO = teamMemberMapper.toDto(teamMember);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(teamMemberDTO))
            .exchange()
            .expectStatus()
            .isEqualTo(405);

        // Validate the TeamMember in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void partialUpdateTeamMemberWithPatch() throws Exception {
        // Initialize the database
        insertedTeamMember = teamMemberRepository.save(teamMember).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the teamMember using partial update
        TeamMember partialUpdatedTeamMember = new TeamMember();
        partialUpdatedTeamMember.setId(teamMember.getId());

        partialUpdatedTeamMember.joinedDate(UPDATED_JOINED_DATE);

        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, partialUpdatedTeamMember.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(partialUpdatedTeamMember))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the TeamMember in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertTeamMemberUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedTeamMember, teamMember),
            getPersistedTeamMember(teamMember)
        );
    }

    @Test
    void fullUpdateTeamMemberWithPatch() throws Exception {
        // Initialize the database
        insertedTeamMember = teamMemberRepository.save(teamMember).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the teamMember using partial update
        TeamMember partialUpdatedTeamMember = new TeamMember();
        partialUpdatedTeamMember.setId(teamMember.getId());

        partialUpdatedTeamMember.role(UPDATED_ROLE).joinedDate(UPDATED_JOINED_DATE);

        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, partialUpdatedTeamMember.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(partialUpdatedTeamMember))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the TeamMember in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertTeamMemberUpdatableFieldsEquals(partialUpdatedTeamMember, getPersistedTeamMember(partialUpdatedTeamMember));
    }

    @Test
    void patchNonExistingTeamMember() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        teamMember.setId(longCount.incrementAndGet());

        // Create the TeamMember
        TeamMemberDTO teamMemberDTO = teamMemberMapper.toDto(teamMember);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, teamMemberDTO.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(teamMemberDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the TeamMember in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void patchWithIdMismatchTeamMember() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        teamMember.setId(longCount.incrementAndGet());

        // Create the TeamMember
        TeamMemberDTO teamMemberDTO = teamMemberMapper.toDto(teamMember);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, longCount.incrementAndGet())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(teamMemberDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the TeamMember in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void patchWithMissingIdPathParamTeamMember() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        teamMember.setId(longCount.incrementAndGet());

        // Create the TeamMember
        TeamMemberDTO teamMemberDTO = teamMemberMapper.toDto(teamMember);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(teamMemberDTO))
            .exchange()
            .expectStatus()
            .isEqualTo(405);

        // Validate the TeamMember in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void deleteTeamMember() {
        // Initialize the database
        insertedTeamMember = teamMemberRepository.save(teamMember).block();

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the teamMember
        webTestClient
            .delete()
            .uri(ENTITY_API_URL_ID, teamMember.getId())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNoContent();

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return teamMemberRepository.count().block();
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
        return teamMemberRepository.findById(teamMember.getId()).block();
    }

    protected void assertPersistedTeamMemberToMatchAllProperties(TeamMember expectedTeamMember) {
        // Test fails because reactive api returns an empty object instead of null
        // assertTeamMemberAllPropertiesEquals(expectedTeamMember, getPersistedTeamMember(expectedTeamMember));
        assertTeamMemberUpdatableFieldsEquals(expectedTeamMember, getPersistedTeamMember(expectedTeamMember));
    }

    protected void assertPersistedTeamMemberToMatchUpdatableProperties(TeamMember expectedTeamMember) {
        // Test fails because reactive api returns an empty object instead of null
        // assertTeamMemberAllUpdatablePropertiesEquals(expectedTeamMember, getPersistedTeamMember(expectedTeamMember));
        assertTeamMemberUpdatableFieldsEquals(expectedTeamMember, getPersistedTeamMember(expectedTeamMember));
    }
}
