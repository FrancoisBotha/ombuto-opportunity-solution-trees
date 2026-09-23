package com.opportunity.tree.service.impl;

import com.opportunity.tree.domain.Tag;
import com.opportunity.tree.repository.TagRepository;
import com.opportunity.tree.service.TagService;
import com.opportunity.tree.service.dto.TagDTO;
import com.opportunity.tree.service.mapper.TagMapper;
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
 * Service Implementation for managing {@link com.opportunity.tree.domain.Tag}.
 */
@Service
@Transactional
public class TagServiceImpl implements TagService {

    private static final Logger LOG = LoggerFactory.getLogger(TagServiceImpl.class);

    private final TagRepository tagRepository;

    private final TagMapper tagMapper;

    public TagServiceImpl(TagRepository tagRepository, TagMapper tagMapper) {
        this.tagRepository = tagRepository;
        this.tagMapper = tagMapper;
    }

    @Override
    public TagDTO save(TagDTO tagDTO) {
        LOG.debug("Request to save Tag : {}", tagDTO);
        Tag tag = tagMapper.toEntity(tagDTO);
        // LABEL-001: keep normalized_name in sync with name so the DB uniqueness constraint holds.
        tag.setNormalizedName(Tag.normalize(tag.getName()));
        tag = tagRepository.save(tag);
        return tagMapper.toDto(tag);
    }

    @Override
    public TagDTO update(TagDTO tagDTO) {
        LOG.debug("Request to update Tag : {}", tagDTO);
        Tag tag = tagMapper.toEntity(tagDTO);
        tag.setNormalizedName(Tag.normalize(tag.getName()));
        tag = tagRepository.save(tag);
        return tagMapper.toDto(tag);
    }

    @Override
    public Optional<TagDTO> partialUpdate(TagDTO tagDTO) {
        LOG.debug("Request to partially update Tag : {}", tagDTO);

        return tagRepository
            .findById(tagDTO.getId())
            .map(existingTag -> {
                tagMapper.partialUpdate(existingTag, tagDTO);

                return existingTag;
            })
            .map(tagRepository::save)
            .map(tagMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TagDTO> findAll() {
        LOG.debug("Request to get all Tags");
        return tagRepository.findAll().stream().map(tagMapper::toDto).collect(Collectors.toCollection(LinkedList::new));
    }

    public Page<TagDTO> findAllWithEagerRelationships(Pageable pageable) {
        return tagRepository.findAllWithEagerRelationships(pageable).map(tagMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TagDTO> findOne(Long id) {
        LOG.debug("Request to get Tag : {}", id);
        return tagRepository.findOneWithEagerRelationships(id).map(tagMapper::toDto);
    }

    @Override
    public void delete(Long id) {
        LOG.debug("Request to delete Tag : {}", id);
        // LABEL-001: clear the join-table entries on both node sides before delete so the FK
        // constraint does not block the removal. The tag owns the inverse side of the M2M.
        tagRepository
            .findOneWithEagerRelationships(id)
            .ifPresent(tag -> {
                for (com.opportunity.tree.domain.Opportunity o : new java.util.ArrayList<>(tag.getOpportunities())) {
                    o.getTags().remove(tag);
                }
                for (com.opportunity.tree.domain.Solution s : new java.util.ArrayList<>(tag.getSolutions())) {
                    s.getTags().remove(tag);
                }
                tag.getOpportunities().clear();
                tag.getSolutions().clear();
                tagRepository.delete(tag);
            });
    }
}
