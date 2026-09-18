package com.opportunity.tree.service.impl;

import com.opportunity.tree.domain.TeamMember;
import com.opportunity.tree.repository.TeamMemberRepository;
import com.opportunity.tree.service.TeamMemberService;
import com.opportunity.tree.service.dto.TeamMemberDTO;
import com.opportunity.tree.service.mapper.TeamMemberMapper;
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
    public TeamMemberDTO save(TeamMemberDTO teamMemberDTO) {
        LOG.debug("Request to save TeamMember : {}", teamMemberDTO);
        TeamMember teamMember = teamMemberMapper.toEntity(teamMemberDTO);
        teamMember = teamMemberRepository.save(teamMember);
        return teamMemberMapper.toDto(teamMember);
    }

    @Override
    public TeamMemberDTO update(TeamMemberDTO teamMemberDTO) {
        LOG.debug("Request to update TeamMember : {}", teamMemberDTO);
        TeamMember teamMember = teamMemberMapper.toEntity(teamMemberDTO);
        teamMember = teamMemberRepository.save(teamMember);
        return teamMemberMapper.toDto(teamMember);
    }

    @Override
    public Optional<TeamMemberDTO> partialUpdate(TeamMemberDTO teamMemberDTO) {
        LOG.debug("Request to partially update TeamMember : {}", teamMemberDTO);

        return teamMemberRepository
            .findById(teamMemberDTO.getId())
            .map(existingTeamMember -> {
                teamMemberMapper.partialUpdate(existingTeamMember, teamMemberDTO);

                return existingTeamMember;
            })
            .map(teamMemberRepository::save)
            .map(teamMemberMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeamMemberDTO> findAll() {
        LOG.debug("Request to get all TeamMembers");
        return teamMemberRepository.findAll().stream().map(teamMemberMapper::toDto).collect(Collectors.toCollection(LinkedList::new));
    }

    public Page<TeamMemberDTO> findAllWithEagerRelationships(Pageable pageable) {
        return teamMemberRepository.findAllWithEagerRelationships(pageable).map(teamMemberMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TeamMemberDTO> findOne(Long id) {
        LOG.debug("Request to get TeamMember : {}", id);
        return teamMemberRepository.findOneWithEagerRelationships(id).map(teamMemberMapper::toDto);
    }

    @Override
    public void delete(Long id) {
        LOG.debug("Request to delete TeamMember : {}", id);
        teamMemberRepository.deleteById(id);
    }
}
