package com.opportunity.tree.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opportunity.tree.IntegrationTest;
import com.opportunity.tree.domain.Product;
import com.opportunity.tree.domain.Team;
import com.opportunity.tree.domain.TeamMember;
import com.opportunity.tree.domain.User;
import com.opportunity.tree.domain.enumeration.TeamRole;
import com.opportunity.tree.repository.ProductRepository;
import com.opportunity.tree.repository.TeamMemberRepository;
import com.opportunity.tree.repository.UserRepository;
import com.opportunity.tree.service.dto.ProductDTO;
import com.opportunity.tree.service.dto.TeamDTO;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the team-scoped product API (TEAMS-003). Covers owner,
 * editor, viewer and non-member roles across list, create, edit and archive
 * (acceptance criteria 1–7). Also verifies a product cannot be moved to a team
 * the caller cannot edit via the update payload (criterion 5).
 */
@IntegrationTest
@AutoConfigureMockMvc
class TeamScopedProductAccessIT {

    private static final String OWNER_LOGIN = "prod-owner";
    private static final String EDITOR_LOGIN = "prod-editor";
    private static final String VIEWER_LOGIN = "prod-viewer";
    private static final String NON_MEMBER_LOGIN = "prod-outsider";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeamMemberRepository teamMemberRepository;

    @Autowired
    private ProductRepository productRepository;

    private Team team;
    private Team otherTeam;
    private Product product;
    private Product archivedProduct;

    private User ownerUser;
    private User editorUser;
    private User viewerUser;
    private User outsiderUser;

    @BeforeEach
    void seed() {
        team = persistTeam("team-a");
        otherTeam = persistTeam("team-b");

        ownerUser = persistUser(OWNER_LOGIN);
        editorUser = persistUser(EDITOR_LOGIN);
        viewerUser = persistUser(VIEWER_LOGIN);
        outsiderUser = persistUser(NON_MEMBER_LOGIN);

        persistMembership(team, ownerUser, TeamRole.OWNER);
        persistMembership(team, editorUser, TeamRole.EDITOR);
        persistMembership(team, viewerUser, TeamRole.VIEWER);

        product = new Product()
            .name("Discovery")
            .description("desc")
            .archived(Boolean.FALSE)
            .sortOrder(0)
            .createdDate(Instant.now())
            .team(team);
        em.persist(product);

        archivedProduct = new Product()
            .name("Legacy")
            .description("old")
            .archived(Boolean.TRUE)
            .sortOrder(1)
            .createdDate(Instant.now())
            .team(team);
        em.persist(archivedProduct);
        em.flush();
    }

    @AfterEach
    void cleanup() {
        teamMemberRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.delete(ownerUser);
        userRepository.delete(editorUser);
        userRepository.delete(viewerUser);
        userRepository.delete(outsiderUser);
    }

    // ---------------------------------------------------------------
    // Criterion 1: any member can list team products including archived
    // ---------------------------------------------------------------

    @Test
    @Transactional
    void ownerListsTeamProductsIncludingArchived() throws Exception {
        mvc
            .perform(get("/api/teams/{teamId}/products", team.getId()).with(user(OWNER_LOGIN)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[?(@.name=='Legacy')].archived").value(hasItem(true)))
            .andExpect(jsonPath("$[?(@.name=='Discovery')].archived").value(hasItem(false)));
    }

    @Test
    @Transactional
    void editorListsTeamProducts() throws Exception {
        mvc
            .perform(get("/api/teams/{teamId}/products", team.getId()).with(user(EDITOR_LOGIN)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @Transactional
    void viewerListsTeamProducts() throws Exception {
        mvc
            .perform(get("/api/teams/{teamId}/products", team.getId()).with(user(VIEWER_LOGIN)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)));
    }

    // ---------------------------------------------------------------
    // Criterion 4: non-member cannot list, read or modify
    // ---------------------------------------------------------------

    @Test
    @Transactional
    void nonMemberCannotListTeamProducts() throws Exception {
        mvc.perform(get("/api/teams/{teamId}/products", team.getId()).with(user(NON_MEMBER_LOGIN))).andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    void nonMemberGetsEmptyOwnProductList() throws Exception {
        mvc.perform(get("/api/products").with(user(NON_MEMBER_LOGIN))).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @Transactional
    void nonMemberCannotReadProduct() throws Exception {
        // Same response as for a non-existent id — 404 with no body to distinguish.
        mvc.perform(get("/api/products/{id}", product.getId()).with(user(NON_MEMBER_LOGIN))).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void nonMemberCannotEditProduct() throws Exception {
        ProductDTO dto = toDto(product);
        dto.setName("hacked");
        mvc
            .perform(
                put("/api/products/{id}", dto.getId())
                    .with(user(NON_MEMBER_LOGIN))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(dto))
            )
            .andExpect(status().isForbidden());
        assertThat(productRepository.findById(product.getId()).orElseThrow().getName()).isEqualTo("Discovery");
    }

    @Test
    @Transactional
    void nonMemberCannotArchiveProduct() throws Exception {
        ProductDTO dto = toDto(product);
        dto.setArchived(Boolean.TRUE);
        mvc
            .perform(
                patch("/api/products/{id}", dto.getId())
                    .with(user(NON_MEMBER_LOGIN))
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(dto))
            )
            .andExpect(status().isForbidden());
        assertThat(productRepository.findById(product.getId()).orElseThrow().getArchived()).isFalse();
    }

    // Criterion 4 (NFR-002): a non-member PUT/PATCH on a non-existent id and on
    // another team's product must return the same status, so existence is not
    // revealed. Both must be 403 — a different code (e.g. 400) for one branch
    // would leak whether the id exists.

    @Test
    @Transactional
    void nonMemberPutOnNonExistentAndOnOthersProductReturnSameStatus() throws Exception {
        long missingId = product.getId() + 999_999L;

        ProductDTO missingDto = new ProductDTO();
        missingDto.setId(missingId);
        missingDto.setName("ghost");
        missingDto.setDescription("d");
        missingDto.setArchived(Boolean.FALSE);
        missingDto.setCreatedDate(Instant.now());
        TeamDTO t = new TeamDTO();
        t.setId(team.getId());
        missingDto.setTeam(t);

        int missingStatus = mvc
            .perform(
                put("/api/products/{id}", missingId)
                    .with(user(NON_MEMBER_LOGIN))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(missingDto))
            )
            .andReturn()
            .getResponse()
            .getStatus();

        ProductDTO existing = toDto(product);
        existing.setName("hacked");
        int existingStatus = mvc
            .perform(
                put("/api/products/{id}", existing.getId())
                    .with(user(NON_MEMBER_LOGIN))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(existing))
            )
            .andReturn()
            .getResponse()
            .getStatus();

        assertThat(missingStatus).isEqualTo(existingStatus).isEqualTo(403);
    }

    @Test
    @Transactional
    void nonMemberPatchOnNonExistentAndOnOthersProductReturnSameStatus() throws Exception {
        long missingId = product.getId() + 999_999L;

        ProductDTO missingDto = new ProductDTO();
        missingDto.setId(missingId);
        missingDto.setArchived(Boolean.TRUE);

        int missingStatus = mvc
            .perform(
                patch("/api/products/{id}", missingId)
                    .with(user(NON_MEMBER_LOGIN))
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(missingDto))
            )
            .andReturn()
            .getResponse()
            .getStatus();

        ProductDTO existing = new ProductDTO();
        existing.setId(product.getId());
        existing.setArchived(Boolean.TRUE);
        int existingStatus = mvc
            .perform(
                patch("/api/products/{id}", existing.getId())
                    .with(user(NON_MEMBER_LOGIN))
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(existing))
            )
            .andReturn()
            .getResponse()
            .getStatus();

        assertThat(missingStatus).isEqualTo(existingStatus).isEqualTo(403);
    }

    @Test
    @Transactional
    void nonMemberCannotCreateProductInTeam() throws Exception {
        ProductDTO dto = newProductDtoForTeam(team.getId(), "new");
        mvc
            .perform(
                post("/api/products")
                    .with(user(NON_MEMBER_LOGIN))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(dto))
            )
            .andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------
    // Criterion 2: owners and editors can create, edit and archive
    // ---------------------------------------------------------------

    @Test
    @Transactional
    void ownerCanCreateProduct() throws Exception {
        ProductDTO dto = newProductDtoForTeam(team.getId(), "owner-made");
        mvc
            .perform(
                post("/api/products")
                    .with(user(OWNER_LOGIN))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(dto))
            )
            .andExpect(status().isCreated());
    }

    @Test
    @Transactional
    void editorCanCreateProduct() throws Exception {
        ProductDTO dto = newProductDtoForTeam(team.getId(), "editor-made");
        mvc
            .perform(
                post("/api/products")
                    .with(user(EDITOR_LOGIN))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(dto))
            )
            .andExpect(status().isCreated());
    }

    @Test
    @Transactional
    void ownerCanEditProduct() throws Exception {
        ProductDTO dto = toDto(product);
        dto.setDescription("owner-edit");
        mvc
            .perform(
                put("/api/products/{id}", dto.getId())
                    .with(user(OWNER_LOGIN))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(dto))
            )
            .andExpect(status().isOk());
        assertThat(productRepository.findById(product.getId()).orElseThrow().getDescription()).isEqualTo("owner-edit");
    }

    @Test
    @Transactional
    void editorCanArchiveAndUnarchiveProduct() throws Exception {
        ProductDTO archive = toDto(product);
        archive.setArchived(Boolean.TRUE);
        mvc
            .perform(
                patch("/api/products/{id}", archive.getId())
                    .with(user(EDITOR_LOGIN))
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(archive))
            )
            .andExpect(status().isOk());
        assertThat(productRepository.findById(product.getId()).orElseThrow().getArchived()).isTrue();

        ProductDTO restore = toDto(product);
        restore.setArchived(Boolean.FALSE);
        mvc
            .perform(
                patch("/api/products/{id}", restore.getId())
                    .with(user(EDITOR_LOGIN))
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(restore))
            )
            .andExpect(status().isOk());
        assertThat(productRepository.findById(product.getId()).orElseThrow().getArchived()).isFalse();
    }

    @Test
    @Transactional
    void editorCanEditProduct() throws Exception {
        ProductDTO dto = toDto(product);
        dto.setDescription("edited");
        mvc
            .perform(
                put("/api/products/{id}", dto.getId())
                    .with(user(EDITOR_LOGIN))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(dto))
            )
            .andExpect(status().isOk());
        assertThat(productRepository.findById(product.getId()).orElseThrow().getDescription()).isEqualTo("edited");
    }

    @Test
    @Transactional
    void ownerCanArchiveAndUnarchiveProduct() throws Exception {
        ProductDTO archive = toDto(product);
        archive.setArchived(Boolean.TRUE);
        mvc
            .perform(
                patch("/api/products/{id}", archive.getId())
                    .with(user(OWNER_LOGIN))
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(archive))
            )
            .andExpect(status().isOk());
        assertThat(productRepository.findById(product.getId()).orElseThrow().getArchived()).isTrue();

        ProductDTO restore = toDto(product);
        restore.setArchived(Boolean.FALSE);
        mvc
            .perform(
                patch("/api/products/{id}", restore.getId())
                    .with(user(OWNER_LOGIN))
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(restore))
            )
            .andExpect(status().isOk());
        assertThat(productRepository.findById(product.getId()).orElseThrow().getArchived()).isFalse();
    }

    // ---------------------------------------------------------------
    // Criterion 3: viewers get 403 on every write
    // ---------------------------------------------------------------

    @Test
    @Transactional
    void viewerCannotCreateProduct() throws Exception {
        ProductDTO dto = newProductDtoForTeam(team.getId(), "viewer-tried");
        mvc
            .perform(
                post("/api/products")
                    .with(user(VIEWER_LOGIN))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(dto))
            )
            .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    void viewerCannotEditProduct() throws Exception {
        ProductDTO dto = toDto(product);
        dto.setName("nope");
        mvc
            .perform(
                put("/api/products/{id}", dto.getId())
                    .with(user(VIEWER_LOGIN))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(dto))
            )
            .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    void viewerCannotArchiveProduct() throws Exception {
        ProductDTO dto = toDto(product);
        dto.setArchived(Boolean.TRUE);
        mvc
            .perform(
                patch("/api/products/{id}", dto.getId())
                    .with(user(VIEWER_LOGIN))
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(dto))
            )
            .andExpect(status().isForbidden());
        assertThat(productRepository.findById(product.getId()).orElseThrow().getArchived()).isFalse();
    }

    @Test
    @Transactional
    void viewerCannotDeleteProduct() throws Exception {
        mvc.perform(delete("/api/products/{id}", product.getId()).with(user(VIEWER_LOGIN)).with(csrf())).andExpect(status().isForbidden());
        assertThat(productRepository.existsById(product.getId())).isTrue();
    }

    // ---------------------------------------------------------------
    // Criterion 5: a product's team cannot be changed to a team the caller
    // cannot edit via the payload.
    // ---------------------------------------------------------------

    @Test
    @Transactional
    void ownerCannotMoveProductToTeamTheyCannotEdit() throws Exception {
        ProductDTO dto = toDto(product);
        TeamDTO otherTeamDto = new TeamDTO();
        otherTeamDto.setId(otherTeam.getId());
        otherTeamDto.setName(otherTeam.getName());
        dto.setTeam(otherTeamDto);
        mvc
            .perform(
                put("/api/products/{id}", dto.getId())
                    .with(user(OWNER_LOGIN))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(dto))
            )
            .andExpect(status().isForbidden());
        // Product still belongs to the original team.
        Product reloaded = productRepository.findById(product.getId()).orElseThrow();
        assertThat(reloaded.getTeam().getId()).isEqualTo(team.getId());
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private Team persistTeam(String name) {
        Team t = new Team().name(name).description("d").createdDate(Instant.now());
        em.persist(t);
        em.flush();
        return t;
    }

    private User persistUser(String login) {
        return userRepository
            .findOneByLogin(login)
            .orElseGet(() -> {
                User u = new User();
                u.setId(UUID.randomUUID().toString());
                u.setLogin(login);
                u.setActivated(true);
                u.setEmail(login + "@example.com");
                u.setFirstName(login);
                u.setLastName("test");
                u.setLangKey("en");
                em.persist(u);
                em.flush();
                return u;
            });
    }

    private void persistMembership(Team t, User u, TeamRole role) {
        TeamMember tm = new TeamMember().role(role).joinedDate(Instant.now());
        tm.setTeam(t);
        tm.setUser(u);
        em.persist(tm);
        em.flush();
    }

    private static ProductDTO toDto(Product p) {
        ProductDTO dto = new ProductDTO();
        dto.setId(p.getId());
        dto.setName(p.getName());
        dto.setDescription(p.getDescription());
        dto.setVision(p.getVision());
        dto.setArchived(p.getArchived());
        dto.setCreatedDate(p.getCreatedDate());
        TeamDTO t = new TeamDTO();
        t.setId(p.getTeam().getId());
        t.setName(p.getTeam().getName());
        dto.setTeam(t);
        return dto;
    }

    private ProductDTO newProductDtoForTeam(Long teamId, String name) {
        ProductDTO dto = new ProductDTO();
        dto.setName(name);
        dto.setDescription("d");
        dto.setArchived(Boolean.FALSE);
        dto.setCreatedDate(Instant.now());
        TeamDTO t = new TeamDTO();
        t.setId(teamId);
        dto.setTeam(t);
        return dto;
    }
}
