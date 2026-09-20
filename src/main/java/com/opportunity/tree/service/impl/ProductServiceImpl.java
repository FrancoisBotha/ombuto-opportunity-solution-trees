package com.opportunity.tree.service.impl;

import com.opportunity.tree.domain.Product;
import com.opportunity.tree.domain.enumeration.TreeNodeType;
import com.opportunity.tree.repository.ProductRepository;
import com.opportunity.tree.repository.TeamRepository;
import com.opportunity.tree.service.DefaultNodeLinks;
import com.opportunity.tree.service.NodeWriteRuleException;
import com.opportunity.tree.service.ProductService;
import com.opportunity.tree.service.TeamAccessDeniedException;
import com.opportunity.tree.service.TeamAccessService;
import com.opportunity.tree.service.TreeNodeCascadeService;
import com.opportunity.tree.service.TreeNodeDtoAssembler;
import com.opportunity.tree.service.TreeNodeRef;
import com.opportunity.tree.service.TreeStructureLock;
import com.opportunity.tree.service.broadcast.NodeDeletedPayload;
import com.opportunity.tree.service.broadcast.TreeChangePublisher;
import com.opportunity.tree.service.broadcast.TreeChangeType;
import com.opportunity.tree.service.dto.ProductDTO;
import com.opportunity.tree.service.dto.TeamDTO;
import com.opportunity.tree.service.mapper.ProductMapper;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.opportunity.tree.domain.Product}.
 *
 * <p>Every read and write is routed through {@link TeamAccessService} so that
 * only members of a product's team can list, read, create, edit or archive it
 * (FR-006, FR-007, NFR-001). Non-members are denied uniformly whether or not
 * the product exists (NFR-002). A product's owning team cannot be changed to
 * a team the caller cannot edit, which blocks moving a product across teams
 * via the payload.
 *
 * <p>Create appends the product under the team's {@link TreeStructureLock}, so concurrent creates
 * get distinct sort orders. Changing a product's team (allowed when the caller can edit both
 * teams, TEAMS-003) locks both teams, lower id first, re-checks the product is still in its old
 * team and appends it after the new team's products. sortOrder is server-owned on PUT and PATCH:
 * any other edit keeps the product's current position, read under its team's lock.
 */
@Service
@Transactional
public class ProductServiceImpl implements ProductService {

    private static final Logger LOG = LoggerFactory.getLogger(ProductServiceImpl.class);

    private final ProductRepository productRepository;

    private final ProductMapper productMapper;

    private final TeamAccessService teamAccessService;

    private final DefaultNodeLinks defaultNodeLinks;

    private final TreeNodeCascadeService treeNodeCascadeService;

    private final TreeStructureLock structureLock;

    private final TeamRepository teamRepository;

    private final TreeChangePublisher changePublisher;

    private final TreeNodeDtoAssembler dtoAssembler;

    public ProductServiceImpl(
        ProductRepository productRepository,
        ProductMapper productMapper,
        TeamAccessService teamAccessService,
        DefaultNodeLinks defaultNodeLinks,
        TreeNodeCascadeService treeNodeCascadeService,
        TreeStructureLock structureLock,
        TeamRepository teamRepository,
        TreeChangePublisher changePublisher,
        TreeNodeDtoAssembler dtoAssembler
    ) {
        this.productRepository = productRepository;
        this.productMapper = productMapper;
        this.teamAccessService = teamAccessService;
        this.defaultNodeLinks = defaultNodeLinks;
        this.treeNodeCascadeService = treeNodeCascadeService;
        this.structureLock = structureLock;
        this.teamRepository = teamRepository;
        this.changePublisher = changePublisher;
        this.dtoAssembler = dtoAssembler;
    }

    /**
     * Broadcasts a product write so other sessions with the team's tree open follow it without a
     * reload (FR-029/FR-030). A product is a tree node like any other, but it is created, renamed
     * and archived through the generated {@code /api/products} CRUD rather than
     * {@code /api/tree/nodes}, so the publish has to happen here. Every caller is already holding
     * the team's {@link TreeStructureLock}, which is what keeps {@code seq} ordered against the
     * tree writes on the same team.
     */
    private void publishProduct(TreeChangeType type, Long teamId, Long productId) {
        if (teamId == null || productId == null) {
            return;
        }
        productRepository.flush();
        changePublisher.publish(type, teamId, dtoAssembler.toDto(TreeNodeType.PRODUCT, productId));
    }

    @Override
    public ProductDTO save(ProductDTO productDTO) {
        LOG.debug("Request to save Product : {}", productDTO);
        Long targetTeamId = teamIdOf(productDTO);
        if (targetTeamId == null) {
            throw new NodeWriteRuleException("A product must have a team", "product", "parentmissing");
        }
        teamAccessService.requireEditTeam(targetTeamId);
        Product product = productMapper.toEntity(productDTO);
        product.setId(null);
        if (product.getArchived() == null) {
            product.setArchived(Boolean.FALSE);
        }
        product.setCreatedDate(Instant.now());
        // sortOrder is server-set: append after the team's existing products — under the team's
        // structure lock, or concurrent creates (a double-click) get the same sortOrder.
        structureLock.lockTeam(targetTeamId);
        product.setSortOrder(productRepository.findMaxSortOrderByTeamId(targetTeamId) + 1);
        product = productRepository.save(product);
        // OST: every new product gets its default "Product space" link.
        defaultNodeLinks.addDefaults(product);
        publishProduct(TreeChangeType.NODE_CREATED, targetTeamId, product.getId());
        return productMapper.toDto(product);
    }

    @Override
    public ProductDTO update(ProductDTO productDTO) {
        LOG.debug("Request to update Product : {}", productDTO);
        Product existing = productRepository.findById(productDTO.getId()).orElseThrow(TeamAccessDeniedException::new);
        Long existingTeamId = existing.getTeam() != null ? existing.getTeam().getId() : null;
        teamAccessService.requireEditNode(TreeNodeType.PRODUCT, existing.getId());

        Long targetTeamId = teamIdOf(productDTO);
        boolean teamChanges = !Objects.equals(existingTeamId, targetTeamId);
        if (teamChanges) {
            // Moving a product to a different team requires edit rights on the target team.
            teamAccessService.requireEditTeam(targetTeamId);
        }

        // sortOrder is server-owned (products are reordered through the tree move endpoint): a team
        // change appends the product to the new team, otherwise the current value is kept — read
        // under the team's lock, so a PUT never writes back a position a concurrent reorder changed.
        int sortOrder = teamChanges
            ? lockTeamChange(existing.getId(), existingTeamId, targetTeamId)
            : lockedSortOrder(existing, existingTeamId);
        Product product = productMapper.toEntity(productDTO);
        product.setCreatedDate(existing.getCreatedDate());
        product.setSortOrder(sortOrder);
        product = productRepository.save(product);
        publishProductMoveOrUpdate(existingTeamId, targetTeamId, product.getId(), teamChanges);
        return productMapper.toDto(product);
    }

    /**
     * A product that stays in its team is a plain update; one that changes team leaves the old
     * team's tree (its members must drop it and its subtree) and appears in the new one, so both
     * topics get an event of their own.
     */
    private void publishProductMoveOrUpdate(Long existingTeamId, Long targetTeamId, Long productId, boolean teamChanges) {
        if (!teamChanges) {
            publishProduct(TreeChangeType.NODE_UPDATED, existingTeamId, productId);
            return;
        }
        if (existingTeamId != null) {
            changePublisher.publish(
                TreeChangeType.NODE_DELETED,
                existingTeamId,
                new NodeDeletedPayload(TreeNodeRef.key(TreeNodeType.PRODUCT, productId), List.of())
            );
        }
        publishProduct(TreeChangeType.NODE_CREATED, targetTeamId, productId);
    }

    @Override
    public Optional<ProductDTO> partialUpdate(ProductDTO productDTO) {
        LOG.debug("Request to partially update Product : {}", productDTO);
        Product existing = productRepository.findById(productDTO.getId()).orElseThrow(TeamAccessDeniedException::new);
        teamAccessService.requireEditNode(TreeNodeType.PRODUCT, existing.getId());

        Long targetTeamId = teamIdOf(productDTO);
        Long existingTeamId = existing.getTeam() != null ? existing.getTeam().getId() : null;
        boolean teamChanges = targetTeamId != null && !Objects.equals(existingTeamId, targetTeamId);
        if (teamChanges) {
            teamAccessService.requireEditTeam(targetTeamId);
        }
        // sortOrder is server-owned, as in update(): appended on a team change, else kept (read under the lock).
        int sortOrder = teamChanges
            ? lockTeamChange(existing.getId(), existingTeamId, targetTeamId)
            : lockedSortOrder(existing, existingTeamId);

        return productRepository
            .findById(productDTO.getId())
            .map(existingProduct -> {
                Instant preservedCreatedDate = existingProduct.getCreatedDate();
                // The team is re-pointed by id below, never merged field by field: the generated
                // partialUpdate would copy the DTO's team id/name into the product's current, managed
                // Team entity ("identifier of an instance of Team was altered" on flush).
                TeamDTO requestedTeam = productDTO.getTeam();
                productDTO.setTeam(null);
                try {
                    productMapper.partialUpdate(existingProduct, productDTO);
                } finally {
                    productDTO.setTeam(requestedTeam);
                }
                existingProduct.setCreatedDate(preservedCreatedDate);
                existingProduct.setSortOrder(sortOrder);
                if (teamChanges) {
                    existingProduct.setTeam(teamRepository.getReferenceById(targetTeamId));
                }
                return existingProduct;
            })
            .map(productRepository::save)
            .map(saved -> {
                publishProductMoveOrUpdate(existingTeamId, teamChanges ? targetTeamId : existingTeamId, saved.getId(), teamChanges);
                return saved;
            })
            .map(productMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductDTO> findAll() {
        LOG.debug("Request to get all Products");
        return findAllForCurrentUser();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductDTO> findAllForCurrentUser() {
        LOG.debug("Request to get all Products for current user");
        Set<Long> teamIds = teamAccessService.getCurrentUserTeamIds();
        if (teamIds.isEmpty()) {
            return Collections.emptyList();
        }
        return productRepository
            .findAllByTeamIdIn(teamIds)
            .stream()
            .map(productMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductDTO> findAllByTeam(Long teamId) {
        LOG.debug("Request to get all Products for team {}", teamId);
        teamAccessService.requireReadTeam(teamId);
        return productRepository
            .findAllByTeamId(teamId)
            .stream()
            .map(productMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    public Page<ProductDTO> findAllWithEagerRelationships(Pageable pageable) {
        return productRepository.findAllWithEagerRelationships(pageable).map(productMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProductDTO> findOne(Long id) {
        LOG.debug("Request to get Product : {}", id);
        if (id == null || !teamAccessService.canReadNode(TreeNodeType.PRODUCT, id)) {
            // Non-member and non-existing map to the same empty Optional so the
            // controller returns 404 either way and existence is not revealed.
            return Optional.empty();
        }
        return productRepository.findOneWithEagerRelationships(id).map(productMapper::toDto);
    }

    @Override
    public void delete(Long id) {
        LOG.debug("Request to delete Product : {}", id);
        // OST: every product has default links (and usually a subtree), so a plain deleteById
        // fails on foreign keys. The cascade service authorises (OWNER/EDITOR of the product's
        // team; 403 for anyone else or an unknown id) and removes the whole subtree.
        treeNodeCascadeService.deleteNode(TreeNodeType.PRODUCT, id);
    }

    /**
     * A product changing team touches two teams' product lists: lock both (lower id first, so two
     * opposite moves cannot deadlock), re-check the product is still in its old team (it may have
     * been deleted or moved while this request waited) and return the sortOrder that appends it
     * after the new team's products.
     */
    private int lockTeamChange(Long productId, Long fromTeamId, Long toTeamId) {
        structureLock.lockTeams(fromTeamId, toTeamId);
        structureLock.requireNode(TreeNodeType.PRODUCT, productId, fromTeamId);
        return productRepository.findMaxSortOrderByTeamId(toTeamId) + 1;
    }

    /**
     * A product staying in its team keeps its sortOrder: lock the team, re-check the product is still
     * there and return its committed sortOrder (not the value loaded before the wait).
     */
    private int lockedSortOrder(Product existing, Long teamId) {
        if (teamId == null) {
            return existing.getSortOrder();
        }
        structureLock.lockTeam(teamId);
        structureLock.requireNode(TreeNodeType.PRODUCT, existing.getId(), teamId);
        return productRepository.findSortOrderById(existing.getId());
    }

    private static Long teamIdOf(ProductDTO dto) {
        TeamDTO team = dto == null ? null : dto.getTeam();
        return team == null ? null : team.getId();
    }
}
