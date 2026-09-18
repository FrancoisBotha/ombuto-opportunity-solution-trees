package com.opportunity.tree.service.impl;

import com.opportunity.tree.repository.TeamRepository;
import com.opportunity.tree.service.TeamService;
import com.opportunity.tree.service.dto.TeamDTO;
import com.opportunity.tree.service.mapper.TeamMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service Implementation for managing {@link com.opportunity.tree.domain.Team}.
 */
@Service
@Transactional
public class TeamServiceImpl implements TeamService {

    private static final Logger LOG = LoggerFactory.getLogger(TeamServiceImpl.class);

    private final TeamRepository teamRepository;

    private final TeamMapper teamMapper;

    public TeamServiceImpl(TeamRepository teamRepository, TeamMapper teamMapper) {
        this.teamRepository = teamRepository;
        this.teamMapper = teamMapper;
    }

    @Override
    public Mono<TeamDTO> save(TeamDTO teamDTO) {
        LOG.debug("Request to save Team : {}", teamDTO);
        return teamRepository.save(teamMapper.toEntity(teamDTO)).map(teamMapper::toDto);
    }

    @Override
    public Mono<TeamDTO> update(TeamDTO teamDTO) {
        LOG.debug("Request to update Team : {}", teamDTO);
        return teamRepository.save(teamMapper.toEntity(teamDTO)).map(teamMapper::toDto);
    }

    @Override
    public Mono<TeamDTO> partialUpdate(TeamDTO teamDTO) {
        LOG.debug("Request to partially update Team : {}", teamDTO);

        return teamRepository
            .findById(teamDTO.getId())
            .map(existingTeam -> {
                teamMapper.partialUpdate(existingTeam, teamDTO);

                return existingTeam;
            })
            .flatMap(teamRepository::save)
            .map(teamMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Flux<TeamDTO> findAll() {
        LOG.debug("Request to get all Teams");
        return teamRepository.findAll().map(teamMapper::toDto);
    }

    public Mono<Long> countAll() {
        return teamRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public Mono<TeamDTO> findOne(Long id) {
        LOG.debug("Request to get Team : {}", id);
        return teamRepository.findById(id).map(teamMapper::toDto);
    }

    @Override
    public Mono<Void> delete(Long id) {
        LOG.debug("Request to delete Team : {}", id);
        return teamRepository.deleteById(id);
    }
}
