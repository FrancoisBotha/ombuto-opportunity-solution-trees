package com.opportunity.tree.web.rest;

import static com.opportunity.tree.domain.ProductAsserts.*;
import static com.opportunity.tree.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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
import com.opportunity.tree.service.ProductService;
import com.opportunity.tree.service.dto.ProductDTO;
import com.opportunity.tree.service.mapper.ProductMapper;
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
 * Integration tests for the {@link ProductResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class ProductResourceIT {

    private static final String DEFAULT_NAME = "AAAAAAAAAA";
    private static final String UPDATED_NAME = "BBBBBBBBBB";

    private static final String DEFAULT_DESCRIPTION = "AAAAAAAAAA";
    private static final String UPDATED_DESCRIPTION = "BBBBBBBBBB";

    private static final String DEFAULT_VISION = "AAAAAAAAAA";
    private static final String UPDATED_VISION = "BBBBBBBBBB";

    private static final Boolean DEFAULT_ARCHIVED = false;
    private static final Boolean UPDATED_ARCHIVED = true;

    private static final Integer DEFAULT_SORT_ORDER = 1;
    private static final Integer UPDATED_SORT_ORDER = 2;

    private static final Instant DEFAULT_CREATED_DATE = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CREATED_DATE = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/products";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2L * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeamMemberRepository teamMemberRepository;

    private TeamMember insertedMembership;

    private User insertedUser;

    @Mock
    private ProductRepository productRepositoryMock;

    @Autowired
    private ProductMapper productMapper;

    @Mock
    private ProductService productServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restProductMockMvc;

    private Product product;

    private Product insertedProduct;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Product createEntity(EntityManager em) {
        Product product = new Product()
            .name(DEFAULT_NAME)
            .description(DEFAULT_DESCRIPTION)
            .vision(DEFAULT_VISION)
            .archived(DEFAULT_ARCHIVED)
            .sortOrder(DEFAULT_SORT_ORDER)
            .createdDate(DEFAULT_CREATED_DATE);
        // Add required entity
        Team team;
        if (TestUtil.findAll(em, Team.class).isEmpty()) {
            team = TeamResourceIT.createEntity();
            em.persist(team);
            em.flush();
        } else {
            team = TestUtil.findAll(em, Team.class).get(0);
        }
        product.setTeam(team);
        return product;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Product createUpdatedEntity(EntityManager em) {
        Product updatedProduct = new Product()
            .name(UPDATED_NAME)
            .description(UPDATED_DESCRIPTION)
            .vision(UPDATED_VISION)
            .archived(UPDATED_ARCHIVED)
            .sortOrder(UPDATED_SORT_ORDER)
            .createdDate(UPDATED_CREATED_DATE);
        // Add required entity
        Team team;
        if (TestUtil.findAll(em, Team.class).isEmpty()) {
            team = TeamResourceIT.createUpdatedEntity();
            em.persist(team);
            em.flush();
        } else {
            team = TestUtil.findAll(em, Team.class).get(0);
        }
        updatedProduct.setTeam(team);
        return updatedProduct;
    }

    @BeforeEach
    void initTest() {
        product = createEntity(em);
        // The mock user ("user" — the @WithMockUser default) must be an OWNER of the
        // product's team so the team-scoped access checks in ProductServiceImpl let
        // the generated CRUD calls through. Without this seed every write would 403.
        insertedUser = userRepository
            .findOneByLogin("user")
            .orElseGet(() -> {
                User u = UserResourceIT.createEntity();
                u.setLogin("user");
                em.persist(u);
                em.flush();
                return u;
            });
        TeamMember membership = new TeamMember().role(TeamRole.OWNER).joinedDate(Instant.now());
        membership.setTeam(product.getTeam());
        membership.setUser(insertedUser);
        em.persist(membership);
        em.flush();
        insertedMembership = membership;
    }

    @AfterEach
    void cleanup() {
        if (insertedProduct != null) {
            productRepository.delete(insertedProduct);
            insertedProduct = null;
        }
        if (insertedMembership != null) {
            teamMemberRepository.delete(insertedMembership);
            insertedMembership = null;
        }
        if (insertedUser != null) {
            userRepository.delete(insertedUser);
            insertedUser = null;
        }
    }

    @Test
    @Transactional
    void createProduct() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Product
        ProductDTO productDTO = productMapper.toDto(product);
        var returnedProductDTO = om.readValue(
            restProductMockMvc
                .perform(
                    post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(productDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            ProductDTO.class
        );

        // Validate the Product in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedProduct = productMapper.toEntity(returnedProductDTO);
        assertProductUpdatableFieldsEquals(returnedProduct, getPersistedProduct(returnedProduct));

        insertedProduct = returnedProduct;
    }

    @Test
    @Transactional
    void createProductWithExistingId() throws Exception {
        // Create the Product with an existing ID
        product.setId(1L);
        ProductDTO productDTO = productMapper.toDto(product);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restProductMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(productDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Product in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkNameIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        product.setName(null);

        // Create the Product, which fails.
        ProductDTO productDTO = productMapper.toDto(product);

        restProductMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(productDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkArchivedIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        product.setArchived(null);

        // Create the Product, which fails.
        ProductDTO productDTO = productMapper.toDto(product);

        restProductMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(productDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void createProductSetsCreatedDateServerSide() throws Exception {
        // createdDate is server-set (TREE-002): a missing value is fine and a
        // client-supplied one is ignored.
        product.setCreatedDate(null);
        ProductDTO productDTO = productMapper.toDto(product);

        var returnedProductDTO = om.readValue(
            restProductMockMvc
                .perform(
                    post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(productDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            ProductDTO.class
        );
        assertThat(productRepository.findById(returnedProductDTO.getId()).orElseThrow().getCreatedDate()).isAfter(DEFAULT_CREATED_DATE);

        product.setCreatedDate(DEFAULT_CREATED_DATE);
        returnedProductDTO = om.readValue(
            restProductMockMvc
                .perform(
                    post(ENTITY_API_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsBytes(productMapper.toDto(product)))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            ProductDTO.class
        );
        assertThat(productRepository.findById(returnedProductDTO.getId()).orElseThrow().getCreatedDate()).isAfter(DEFAULT_CREATED_DATE);
    }

    @Test
    @Transactional
    void getAllProducts() throws Exception {
        // Initialize the database
        insertedProduct = productRepository.saveAndFlush(product);

        // Get all the productList
        restProductMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(product.getId().intValue())))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME)))
            .andExpect(jsonPath("$.[*].description").value(hasItem(DEFAULT_DESCRIPTION)))
            .andExpect(jsonPath("$.[*].vision").value(hasItem(DEFAULT_VISION)))
            .andExpect(jsonPath("$.[*].archived").value(hasItem(DEFAULT_ARCHIVED)))
            .andExpect(jsonPath("$.[*].sortOrder").value(hasItem(DEFAULT_SORT_ORDER)))
            .andExpect(jsonPath("$.[*].createdDate").value(hasItem(DEFAULT_CREATED_DATE.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllProductsWithEagerRelationshipsIsEnabled() throws Exception {
        when(productServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restProductMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(productServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllProductsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(productServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restProductMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(productRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getProduct() throws Exception {
        // Initialize the database
        insertedProduct = productRepository.saveAndFlush(product);

        // Get the product
        restProductMockMvc
            .perform(get(ENTITY_API_URL_ID, product.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(product.getId().intValue()))
            .andExpect(jsonPath("$.name").value(DEFAULT_NAME))
            .andExpect(jsonPath("$.description").value(DEFAULT_DESCRIPTION))
            .andExpect(jsonPath("$.vision").value(DEFAULT_VISION))
            .andExpect(jsonPath("$.archived").value(DEFAULT_ARCHIVED))
            .andExpect(jsonPath("$.sortOrder").value(DEFAULT_SORT_ORDER))
            .andExpect(jsonPath("$.createdDate").value(DEFAULT_CREATED_DATE.toString()));
    }

    @Test
    @Transactional
    void getNonExistingProduct() throws Exception {
        // Get the product
        restProductMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingProduct() throws Exception {
        // Initialize the database
        insertedProduct = productRepository.saveAndFlush(product);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the product
        Product updatedProduct = productRepository.findById(product.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedProduct are not directly saved in db
        em.detach(updatedProduct);
        updatedProduct
            .name(UPDATED_NAME)
            .description(UPDATED_DESCRIPTION)
            .vision(UPDATED_VISION)
            .archived(UPDATED_ARCHIVED)
            .sortOrder(UPDATED_SORT_ORDER)
            .createdDate(UPDATED_CREATED_DATE);
        ProductDTO productDTO = productMapper.toDto(updatedProduct);

        restProductMockMvc
            .perform(
                put(ENTITY_API_URL_ID, productDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(productDTO))
            )
            .andExpect(status().isOk());

        // Validate the Product in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        // createdDate is server-owned: the client-supplied value is ignored (TREE-002)
        updatedProduct.createdDate(DEFAULT_CREATED_DATE);
        assertPersistedProductToMatchAllProperties(updatedProduct);
    }

    @Test
    @Transactional
    void putNonExistingProduct() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        product.setId(longCount.incrementAndGet());

        // Create the Product
        ProductDTO productDTO = productMapper.toDto(product);

        // A non-existent id must return the same 403 as an id the caller cannot
        // edit, so existence is never revealed (NFR-002).
        restProductMockMvc
            .perform(
                put(ENTITY_API_URL_ID, productDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(productDTO))
            )
            .andExpect(status().isForbidden());

        // Validate the Product in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchProduct() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        product.setId(longCount.incrementAndGet());

        // Create the Product
        ProductDTO productDTO = productMapper.toDto(product);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restProductMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(productDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Product in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamProduct() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        product.setId(longCount.incrementAndGet());

        // Create the Product
        ProductDTO productDTO = productMapper.toDto(product);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restProductMockMvc
            .perform(put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(productDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Product in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateProductWithPatch() throws Exception {
        // Initialize the database
        insertedProduct = productRepository.saveAndFlush(product);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the product using partial update
        Product partialUpdatedProduct = new Product();
        partialUpdatedProduct.setId(product.getId());

        partialUpdatedProduct
            .description(UPDATED_DESCRIPTION)
            .archived(UPDATED_ARCHIVED)
            .sortOrder(UPDATED_SORT_ORDER)
            .createdDate(UPDATED_CREATED_DATE);

        restProductMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedProduct.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedProduct))
            )
            .andExpect(status().isOk());

        // Validate the Product in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        // createdDate is server-owned: the client-supplied value is ignored (TREE-002)
        partialUpdatedProduct.createdDate(DEFAULT_CREATED_DATE);
        assertProductUpdatableFieldsEquals(createUpdateProxyForBean(partialUpdatedProduct, product), getPersistedProduct(product));
    }

    @Test
    @Transactional
    void fullUpdateProductWithPatch() throws Exception {
        // Initialize the database
        insertedProduct = productRepository.saveAndFlush(product);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the product using partial update
        Product partialUpdatedProduct = new Product();
        partialUpdatedProduct.setId(product.getId());

        partialUpdatedProduct
            .name(UPDATED_NAME)
            .description(UPDATED_DESCRIPTION)
            .vision(UPDATED_VISION)
            .archived(UPDATED_ARCHIVED)
            .sortOrder(UPDATED_SORT_ORDER)
            .createdDate(UPDATED_CREATED_DATE);

        restProductMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedProduct.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedProduct))
            )
            .andExpect(status().isOk());

        // Validate the Product in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        // createdDate is server-owned: the client-supplied value is ignored (TREE-002)
        partialUpdatedProduct.createdDate(DEFAULT_CREATED_DATE);
        assertProductUpdatableFieldsEquals(partialUpdatedProduct, getPersistedProduct(partialUpdatedProduct));
    }

    @Test
    @Transactional
    void patchNonExistingProduct() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        product.setId(longCount.incrementAndGet());

        // Create the Product
        ProductDTO productDTO = productMapper.toDto(product);

        // A non-existent id must return the same 403 as an id the caller cannot
        // edit, so existence is never revealed (NFR-002).
        restProductMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, productDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(productDTO))
            )
            .andExpect(status().isForbidden());

        // Validate the Product in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchProduct() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        product.setId(longCount.incrementAndGet());

        // Create the Product
        ProductDTO productDTO = productMapper.toDto(product);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restProductMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(productDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Product in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamProduct() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        product.setId(longCount.incrementAndGet());

        // Create the Product
        ProductDTO productDTO = productMapper.toDto(product);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restProductMockMvc
            .perform(
                patch(ENTITY_API_URL).with(csrf()).contentType("application/merge-patch+json").content(om.writeValueAsBytes(productDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the Product in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteProduct() throws Exception {
        // Initialize the database
        insertedProduct = productRepository.saveAndFlush(product);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the product
        restProductMockMvc
            .perform(delete(ENTITY_API_URL_ID, product.getId()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return productRepository.count();
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

    protected Product getPersistedProduct(Product product) {
        return productRepository.findById(product.getId()).orElseThrow();
    }

    protected void assertPersistedProductToMatchAllProperties(Product expectedProduct) {
        assertProductAllPropertiesEquals(expectedProduct, getPersistedProduct(expectedProduct));
    }

    protected void assertPersistedProductToMatchUpdatableProperties(Product expectedProduct) {
        assertProductAllUpdatablePropertiesEquals(expectedProduct, getPersistedProduct(expectedProduct));
    }
}
