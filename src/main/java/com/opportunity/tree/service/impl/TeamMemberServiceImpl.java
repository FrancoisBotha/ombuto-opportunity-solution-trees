package com.opportunity.tree.service.impl;

import com.opportunity.tree.repository.TeamMemberRepository;
import com.opportunity.tree.service.TeamMemberService;
import com.opportunity.tree.service.dto.TeamMemberDTO;
import com.opportunity.tree.service.mapper.TeamMemberMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service Implementation for managing {@link com.opportunity.tree.domain.TeamMember}.
 */
@Service
@Transactional
public class TeamMemberServiceImpl implements TeamMemberService {

    private static final Logger LOG = LoggerFactory.getLogger(TeamMemberServiceImpl.class);

    private final TeamMemberRepository teamMemberRepository;

    private final TeamMemberMapper teamMemberMapper;

    public TeamMemberServiceImpl(TeamMemberRepository teamMemberRepository, TeamMemberMapper teamMemberMapper) {
        this.teamMemberRepository = teamMemberRepository;
        this.teamMemberMapper = teamMemberMapper;
    }

    @Override
    public Mono<TeamMemberDTO> save(TeamMemberDTO teamMemberDTO) {
        LOG.debug("Request to save TeamMember : {}", teamMemberDTO);
        return teamMemberRepository.save(teamMemberMapper.toEntity(teamMemberDTO)).map(teamMemberMapper::toDto);
    }

    @Override
    public Mono<TeamMemberDTO> update(TeamMemberDTO teamMemberDTO) {
        LOG.debug("Request to update TeamMember : {}", teamMemberDTO);
        return teamMemberRepository.save(teamMemberMapper.toEntity(teamMemberDTO)).map(teamMemberMapper::toDto);
    }

    @Override
    public Mono<TeamMemberDTO> partialUpdate(TeamMemberDTO teamMemberDTO) {
        LOG.debug("Request to partially update TeamMember : {}", teamMemberDTO);

        return teamMemberRepository
            .findById(teamMemberDTO.getId())
            .map(existingTeamMember -> {
                teamMemberMapper.partialUpdate(existingTeamMember, teamMemberDTO);

                return existingTeamMember;
            })
            .flatMap(teamMemberRepository::save)
            .map(teamMemberMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Flux<TeamMemberDTO> findAll() {
        LOG.debug("Request to get all TeamMembers");
        return teamMemberRepository.findAll().map(teamMemberMapper::toDto);
    }

    public Flux<TeamMemberDTO> findAllWithEagerRelationships(Pageable pageable) {
        return teamMemberRepository.findAllWithEagerRelationships(pageable).map(teamMemberMapper::toDto);
    }

    public Mono<Long> countAll() {
        return teamMemberRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public Mono<TeamMemberDTO> findOne(Long id) {
        LOG.debug("Request to get TeamMember : {}", id);
        return teamMemberRepository.findOneWithEagerRelationships(id).map(teamMemberMapper::toDto);
    }

    @Override
    public Mono<Void> delete(Long id) {
        LOG.debug("Request to delete TeamMember : {}", id);
        return teamMemberRepository.deleteById(id);
    }
}
