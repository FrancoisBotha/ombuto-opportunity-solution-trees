package com.opportunity.tree.service.impl;

import com.opportunity.tree.domain.MeetingTranscript;
import com.opportunity.tree.repository.MeetingTranscriptRepository;
import com.opportunity.tree.service.MeetingTranscriptService;
import com.opportunity.tree.service.dto.MeetingTranscriptDTO;
import com.opportunity.tree.service.mapper.MeetingTranscriptMapper;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.opportunity.tree.domain.MeetingTranscript}.
 */
@Service
@Transactional
public class MeetingTranscriptServiceImpl implements MeetingTranscriptService {

    private static final Logger LOG = LoggerFactory.getLogger(MeetingTranscriptServiceImpl.class);

    private final MeetingTranscriptRepository meetingTranscriptRepository;

    private final MeetingTranscriptMapper meetingTranscriptMapper;

    public MeetingTranscriptServiceImpl(
        MeetingTranscriptRepository meetingTranscriptRepository,
        MeetingTranscriptMapper meetingTranscriptMapper
    ) {
        this.meetingTranscriptRepository = meetingTranscriptRepository;
        this.meetingTranscriptMapper = meetingTranscriptMapper;
    }

    @Override
    public MeetingTranscriptDTO save(MeetingTranscriptDTO meetingTranscriptDTO) {
        LOG.debug("Request to save MeetingTranscript : {}", meetingTranscriptDTO);
        MeetingTranscript meetingTranscript = meetingTranscriptMapper.toEntity(meetingTranscriptDTO);
        meetingTranscript = meetingTranscriptRepository.save(meetingTranscript);
        return meetingTranscriptMapper.toDto(meetingTranscript);
    }

    @Override
    public MeetingTranscriptDTO update(MeetingTranscriptDTO meetingTranscriptDTO) {
        LOG.debug("Request to update MeetingTranscript : {}", meetingTranscriptDTO);
        MeetingTranscript meetingTranscript = meetingTranscriptMapper.toEntity(meetingTranscriptDTO);
        meetingTranscript = meetingTranscriptRepository.save(meetingTranscript);
        return meetingTranscriptMapper.toDto(meetingTranscript);
    }

    @Override
    public Optional<MeetingTranscriptDTO> partialUpdate(MeetingTranscriptDTO meetingTranscriptDTO) {
        LOG.debug("Request to partially update MeetingTranscript : {}", meetingTranscriptDTO);

        return meetingTranscriptRepository
            .findById(meetingTranscriptDTO.getId())
            .map(existingMeetingTranscript -> {
                meetingTranscriptMapper.partialUpdate(existingMeetingTranscript, meetingTranscriptDTO);

                return existingMeetingTranscript;
            })
            .map(meetingTranscriptRepository::save)
            .map(meetingTranscriptMapper::toDto);
    }

    public Page<MeetingTranscriptDTO> findAllWithEagerRelationships(Pageable pageable) {
        return meetingTranscriptRepository.findAllWithEagerRelationships(pageable).map(meetingTranscriptMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MeetingTranscriptDTO> findOne(Long id) {
        LOG.debug("Request to get MeetingTranscript : {}", id);
        return meetingTranscriptRepository.findOneWithEagerRelationships(id).map(meetingTranscriptMapper::toDto);
    }

    @Override
    public void delete(Long id) {
        LOG.debug("Request to delete MeetingTranscript : {}", id);
        meetingTranscriptRepository.deleteById(id);
    }
}
