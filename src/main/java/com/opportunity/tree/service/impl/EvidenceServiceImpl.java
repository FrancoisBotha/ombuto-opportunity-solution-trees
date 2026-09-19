package com.opportunity.tree.service.impl;

import com.opportunity.tree.domain.Evidence;
import com.opportunity.tree.repository.EvidenceRepository;
import com.opportunity.tree.service.EvidenceService;
import com.opportunity.tree.service.dto.EvidenceDTO;
import com.opportunity.tree.service.mapper.EvidenceMapper;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.opportunity.tree.domain.Evidence}.
 */
@Service
@Transactional
public class EvidenceServiceImpl implements EvidenceService {

    private static final Logger LOG = LoggerFactory.getLogger(EvidenceServiceImpl.class);

    private final EvidenceRepository evidenceRepository;

    private final EvidenceMapper evidenceMapper;

    public EvidenceServiceImpl(EvidenceRepository evidenceRepository, EvidenceMapper evidenceMapper) {
        this.evidenceRepository = evidenceRepository;
        this.evidenceMapper = evidenceMapper;
    }

    @Override
    public EvidenceDTO save(EvidenceDTO evidenceDTO) {
        LOG.debug("Request to save Evidence : {}", evidenceDTO);
        Evidence evidence = evidenceMapper.toEntity(evidenceDTO);
        evidence = evidenceRepository.save(evidence);
        return evidenceMapper.toDto(evidence);
    }

    @Override
    public EvidenceDTO update(EvidenceDTO evidenceDTO) {
        LOG.debug("Request to update Evidence : {}", evidenceDTO);
        Evidence evidence = evidenceMapper.toEntity(evidenceDTO);
        evidence = evidenceRepository.save(evidence);
        return evidenceMapper.toDto(evidence);
    }

    @Override
    public Optional<EvidenceDTO> partialUpdate(EvidenceDTO evidenceDTO) {
        LOG.debug("Request to partially update Evidence : {}", evidenceDTO);

        return evidenceRepository
            .findById(evidenceDTO.getId())
            .map(existingEvidence -> {
                evidenceMapper.partialUpdate(existingEvidence, evidenceDTO);

                return existingEvidence;
            })
            .map(evidenceRepository::save)
            .map(evidenceMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EvidenceDTO> findAll() {
        LOG.debug("Request to get all Evidences");
        return evidenceRepository.findAll().stream().map(evidenceMapper::toDto).collect(Collectors.toCollection(LinkedList::new));
    }

    public Page<EvidenceDTO> findAllWithEagerRelationships(Pageable pageable) {
        return evidenceRepository.findAllWithEagerRelationships(pageable).map(evidenceMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EvidenceDTO> findOne(Long id) {
        LOG.debug("Request to get Evidence : {}", id);
        return evidenceRepository.findOneWithEagerRelationships(id).map(evidenceMapper::toDto);
    }

    @Override
    public void delete(Long id) {
        LOG.debug("Request to delete Evidence : {}", id);
        evidenceRepository.deleteById(id);
    }
}
