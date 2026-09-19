package com.opportunity.tree.service.impl;

import com.opportunity.tree.domain.Outcome;
import com.opportunity.tree.domain.Product;
import com.opportunity.tree.repository.OutcomeRepository;
import com.opportunity.tree.repository.ProductRepository;
import com.opportunity.tree.service.NodeWriteRuleException;
import com.opportunity.tree.service.OutcomeService;
import com.opportunity.tree.service.TeamAccessDeniedException;
import com.opportunity.tree.service.TeamAccessService;
import com.opportunity.tree.service.dto.OutcomeDTO;
import com.opportunity.tree.service.dto.ProductDTO;
import com.opportunity.tree.service.mapper.OutcomeMapper;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.opportunity.tree.domain.Outcome}.
 *
 * <p>Enforces TREE-002 node write rules: authorisation via {@link TeamAccessService},
 * server-set {@code createdDate}/{@code lastModifiedDate}/{@code sortOrder}, and
 * parent validation (an Outcome requires a Product parent).
 */
@Service
@Transactional
public class OutcomeServiceImpl implements OutcomeService {

    private static final Logger LOG = LoggerFactory.getLogger(OutcomeServiceImpl.class);

    private static final String ENTITY_NAME = "outcome";

    private final OutcomeRepository outcomeRepository;
    private final ProductRepository productRepository;
    private final OutcomeMapper outcomeMapper;
    private final TeamAccessService teamAccessService;

    public OutcomeServiceImpl(
        OutcomeRepository outcomeRepository,
        ProductRepository productRepository,
        OutcomeMapper outcomeMapper,
        TeamAccessService teamAccessService
    ) {
        this.outcomeRepository = outcomeRepository;
        this.productRepository = productRepository;
        this.outcomeMapper = outcomeMapper;
        this.teamAccessService = teamAccessService;
    }

    @Override
    public OutcomeDTO save(OutcomeDTO outcomeDTO) {
        LOG.debug("Request to save Outcome : {}", outcomeDTO);
        Long productId = productIdOf(outcomeDTO);
        if (productId == null) {
            throw new NodeWriteRuleException("An outcome must have a product parent", ENTITY_NAME, "parentmissing");
        }
        Product product = productRepository
            .findById(productId)
            .orElseThrow(() -> new NodeWriteRuleException("Parent product does not exist", ENTITY_NAME, "parentmissing"));
        teamAccessService.requireEditProduct(product.getId());

        Outcome outcome = outcomeMapper.toEntity(outcomeDTO);
        outcome.setId(null);
        outcome.setProduct(product);
        Instant now = Instant.now();
        outcome.setCreatedDate(now);
        outcome.setLastModifiedDate(now);
        outcome.setSortOrder(nextOutcomeSortOrder(product.getId()));
        outcome = outcomeRepository.save(outcome);
        return outcomeMapper.toDto(outcome);
    }

    @Override
    public OutcomeDTO update(OutcomeDTO outcomeDTO) {
        LOG.debug("Request to update Outcome : {}", outcomeDTO);
        Outcome existing = outcomeRepository.findById(outcomeDTO.getId()).orElseThrow(TeamAccessDeniedException::new);
        teamAccessService.requireEditOutcome(existing.getId());

        Long targetProductId = productIdOf(outcomeDTO);
        Long existingProductId = existing.getProduct() != null ? existing.getProduct().getId() : null;
        Product product = existing.getProduct();
        if (targetProductId != null && !java.util.Objects.equals(existingProductId, targetProductId)) {
            product = productRepository
                .findById(targetProductId)
                .orElseThrow(() -> new NodeWriteRuleException("Parent product does not exist", ENTITY_NAME, "parentmissing"));
            teamAccessService.requireEditProduct(product.getId());
            if (!java.util.Objects.equals(teamIdOf(product), teamIdOf(existing.getProduct()))) {
                throw new NodeWriteRuleException("Parent must belong to the same team", ENTITY_NAME, "parentwrongteam");
            }
        }

        Outcome outcome = outcomeMapper.toEntity(outcomeDTO);
        outcome.setProduct(product);
        outcome.setCreatedDate(existing.getCreatedDate());
        outcome.setSortOrder(existing.getSortOrder());
        outcome.setLastModifiedDate(Instant.now());
        outcome = outcomeRepository.save(outcome);
        return outcomeMapper.toDto(outcome);
    }

    @Override
    public Optional<OutcomeDTO> partialUpdate(OutcomeDTO outcomeDTO) {
        LOG.debug("Request to partially update Outcome : {}", outcomeDTO);
        Outcome existing = outcomeRepository.findById(outcomeDTO.getId()).orElseThrow(TeamAccessDeniedException::new);
        teamAccessService.requireEditOutcome(existing.getId());

        // Captured before mapping: findById returns the same managed instance as
        // "existing", so the mapper would otherwise overwrite these values.
        Instant preservedCreatedDate = existing.getCreatedDate();
        Integer preservedSortOrder = existing.getSortOrder();
        Product preservedProduct = existing.getProduct();

        return outcomeRepository
            .findById(outcomeDTO.getId())
            .map(existingOutcome -> {
                outcomeMapper.partialUpdate(existingOutcome, outcomeDTO);
                existingOutcome.setCreatedDate(preservedCreatedDate);
                existingOutcome.setSortOrder(preservedSortOrder);
                existingOutcome.setProduct(preservedProduct);
                existingOutcome.setLastModifiedDate(Instant.now());
                return existingOutcome;
            })
            .map(outcomeRepository::save)
            .map(outcomeMapper::toDto);
    }

    public Page<OutcomeDTO> findAllWithEagerRelationships(Pageable pageable) {
        return outcomeRepository.findAllWithEagerRelationships(pageable).map(outcomeMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OutcomeDTO> findOne(Long id) {
        LOG.debug("Request to get Outcome : {}", id);
        if (id == null || !teamAccessService.canReadOutcome(id)) {
            return Optional.empty();
        }
        return outcomeRepository.findOneWithEagerRelationships(id).map(outcomeMapper::toDto);
    }

    @Override
    public void delete(Long id) {
        LOG.debug("Request to delete Outcome : {}", id);
        Outcome existing = outcomeRepository.findById(id).orElseThrow(TeamAccessDeniedException::new);
        teamAccessService.requireEditOutcome(existing.getId());
        outcomeRepository.deleteById(id);
    }

    private int nextOutcomeSortOrder(Long productId) {
        Integer max = outcomeRepository.findMaxSortOrderByProductId(productId);
        return (max == null ? -1 : max) + 1;
    }

    private static Long productIdOf(OutcomeDTO dto) {
        ProductDTO product = dto == null ? null : dto.getProduct();
        return product == null ? null : product.getId();
    }

    private static Long teamIdOf(Product product) {
        return product == null || product.getTeam() == null ? null : product.getTeam().getId();
    }
}
