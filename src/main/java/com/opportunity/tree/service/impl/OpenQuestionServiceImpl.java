package com.opportunity.tree.service.impl;

import com.opportunity.tree.domain.OpenQuestion;
import com.opportunity.tree.repository.OpenQuestionRepository;
import com.opportunity.tree.service.OpenQuestionService;
import com.opportunity.tree.service.dto.OpenQuestionDTO;
import com.opportunity.tree.service.mapper.OpenQuestionMapper;
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
 * Service Implementation for managing {@link com.opportunity.tree.domain.OpenQuestion}.
 */
@Service
@Transactional
public class OpenQuestionServiceImpl implements OpenQuestionService {

    private static final Logger LOG = LoggerFactory.getLogger(OpenQuestionServiceImpl.class);

    private final OpenQuestionRepository openQuestionRepository;

    private final OpenQuestionMapper openQuestionMapper;

    public OpenQuestionServiceImpl(OpenQuestionRepository openQuestionRepository, OpenQuestionMapper openQuestionMapper) {
        this.openQuestionRepository = openQuestionRepository;
        this.openQuestionMapper = openQuestionMapper;
    }

    @Override
    public OpenQuestionDTO save(OpenQuestionDTO openQuestionDTO) {
        LOG.debug("Request to save OpenQuestion : {}", openQuestionDTO);
        OpenQuestion openQuestion = openQuestionMapper.toEntity(openQuestionDTO);
        openQuestion = openQuestionRepository.save(openQuestion);
        return openQuestionMapper.toDto(openQuestion);
    }

    @Override
    public OpenQuestionDTO update(OpenQuestionDTO openQuestionDTO) {
        LOG.debug("Request to update OpenQuestion : {}", openQuestionDTO);
        OpenQuestion openQuestion = openQuestionMapper.toEntity(openQuestionDTO);
        openQuestion = openQuestionRepository.save(openQuestion);
        return openQuestionMapper.toDto(openQuestion);
    }

    @Override
    public Optional<OpenQuestionDTO> partialUpdate(OpenQuestionDTO openQuestionDTO) {
        LOG.debug("Request to partially update OpenQuestion : {}", openQuestionDTO);

        return openQuestionRepository
            .findById(openQuestionDTO.getId())
            .map(existingOpenQuestion -> {
                openQuestionMapper.partialUpdate(existingOpenQuestion, openQuestionDTO);

                return existingOpenQuestion;
            })
            .map(openQuestionRepository::save)
            .map(openQuestionMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OpenQuestionDTO> findAll() {
        LOG.debug("Request to get all OpenQuestions");
        return openQuestionRepository.findAll().stream().map(openQuestionMapper::toDto).collect(Collectors.toCollection(LinkedList::new));
    }

    public Page<OpenQuestionDTO> findAllWithEagerRelationships(Pageable pageable) {
        return openQuestionRepository.findAllWithEagerRelationships(pageable).map(openQuestionMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OpenQuestionDTO> findOne(Long id) {
        LOG.debug("Request to get OpenQuestion : {}", id);
        return openQuestionRepository.findOneWithEagerRelationships(id).map(openQuestionMapper::toDto);
    }

    @Override
    public void delete(Long id) {
        LOG.debug("Request to delete OpenQuestion : {}", id);
        openQuestionRepository.deleteById(id);
    }
}
