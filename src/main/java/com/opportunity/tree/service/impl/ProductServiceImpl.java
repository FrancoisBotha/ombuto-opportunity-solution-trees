package com.opportunity.tree.service.impl;

import com.opportunity.tree.domain.Product;
import com.opportunity.tree.repository.ProductRepository;
import com.opportunity.tree.service.NodeWriteRuleException;
import com.opportunity.tree.service.ProductService;
import com.opportunity.tree.service.TeamAccessDeniedException;
import com.opportunity.tree.service.TeamAccessService;
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
 */
@Service
@Transactional
public class ProductServiceImpl implements ProductService {

    private static final Logger LOG = LoggerFactory.getLogger(ProductServiceImpl.class);

    private final ProductRepository productRepository;

    private final ProductMapper productMapper;

    private final TeamAccessService teamAccessService;

    public ProductServiceImpl(ProductRepository productRepository, ProductMapper productMapper, TeamAccessService teamAccessService) {
        this.productRepository = productRepository;
        this.productMapper = productMapper;
        this.teamAccessService = teamAccessService;
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
        // sortOrder is server-set: append after the team's existing products.
        product.setSortOrder(productRepository.findMaxSortOrderByTeamId(targetTeamId) + 1);
        product = productRepository.save(product);
        return productMapper.toDto(product);
    }

    @Override
    public ProductDTO update(ProductDTO productDTO) {
        LOG.debug("Request to update Product : {}", productDTO);
        Product existing = productRepository.findById(productDTO.getId()).orElseThrow(TeamAccessDeniedException::new);
        Long existingTeamId = existing.getTeam() != null ? existing.getTeam().getId() : null;
        teamAccessService.requireEditProduct(existing.getId());

        Long targetTeamId = teamIdOf(productDTO);
        if (!Objects.equals(existingTeamId, targetTeamId)) {
            // Moving a product to a different team requires edit rights on the target team.
            teamAccessService.requireEditTeam(targetTeamId);
        }

        Product product = productMapper.toEntity(productDTO);
        product.setCreatedDate(existing.getCreatedDate());
        if (product.getSortOrder() == null) {
            product.setSortOrder(existing.getSortOrder());
        }
        product = productRepository.save(product);
        return productMapper.toDto(product);
    }

    @Override
    public Optional<ProductDTO> partialUpdate(ProductDTO productDTO) {
        LOG.debug("Request to partially update Product : {}", productDTO);
        Product existing = productRepository.findById(productDTO.getId()).orElseThrow(TeamAccessDeniedException::new);
        teamAccessService.requireEditProduct(existing.getId());

        Long targetTeamId = teamIdOf(productDTO);
        Long existingTeamId = existing.getTeam() != null ? existing.getTeam().getId() : null;
        if (targetTeamId != null && !Objects.equals(existingTeamId, targetTeamId)) {
            teamAccessService.requireEditTeam(targetTeamId);
        }

        return productRepository
            .findById(productDTO.getId())
            .map(existingProduct -> {
                Instant preservedCreatedDate = existingProduct.getCreatedDate();
                productMapper.partialUpdate(existingProduct, productDTO);
                existingProduct.setCreatedDate(preservedCreatedDate);
                return existingProduct;
            })
            .map(productRepository::save)
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
        if (id == null || !teamAccessService.canReadProduct(id)) {
            // Non-member and non-existing map to the same empty Optional so the
            // controller returns 404 either way and existence is not revealed.
            return Optional.empty();
        }
        return productRepository.findOneWithEagerRelationships(id).map(productMapper::toDto);
    }

    @Override
    public void delete(Long id) {
        LOG.debug("Request to delete Product : {}", id);
        Product existing = productRepository.findById(id).orElseThrow(TeamAccessDeniedException::new);
        teamAccessService.requireEditProduct(existing.getId());
        productRepository.deleteById(id);
    }

    private static Long teamIdOf(ProductDTO dto) {
        TeamDTO team = dto == null ? null : dto.getTeam();
        return team == null ? null : team.getId();
    }
}
