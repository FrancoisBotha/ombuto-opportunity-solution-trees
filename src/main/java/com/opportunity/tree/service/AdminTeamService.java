package com.opportunity.tree.service;

import com.opportunity.tree.domain.Team;
import com.opportunity.tree.domain.TeamMember;
import com.opportunity.tree.domain.User;
import com.opportunity.tree.domain.enumeration.TeamRole;
import com.opportunity.tree.repository.ProductRepository;
import com.opportunity.tree.repository.TeamMemberRepository;
import com.opportunity.tree.repository.TeamRepository;
import com.opportunity.tree.repository.UserRepository;
import com.opportunity.tree.security.AuthoritiesConstants;
import com.opportunity.tree.security.SecurityUtils;
import com.opportunity.tree.service.dto.AddTeamMemberRequest;
import com.opportunity.tree.service.dto.AdminCreateTeamRequest;
import com.opportunity.tree.service.dto.AdminTeamDTO;
import com.opportunity.tree.service.dto.ChangeTeamMemberRoleRequest;
import com.opportunity.tree.service.dto.TeamMemberViewDTO;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin-only team operations (ADHOC-001). Every entry point re-checks the caller has
 * {@code ROLE_ADMIN} — the controller annotation is defence-in-depth, not the sole
 * gate (AC 8). Rule violations (unknown user, last owner, existing member, team still
 * has products) surface as {@link TeamManagementException} with a stable error key
 * so the controller can render a 4xx {@code error.<key>} response.
 */
@Service
@Transactional
public class AdminTeamService {

    private static final Logger LOG = LoggerFactory.getLogger(AdminTeamService.class);

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public AdminTeamService(
        TeamRepository teamRepository,
        TeamMemberRepository teamMemberRepository,
        ProductRepository productRepository,
        UserRepository userRepository
    ) {
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Page<AdminTeamDTO> listTeams(Pageable pageable) {
        requireAdmin();
        return teamRepository.findAll(pageable).map(this::toAdminTeamDTO);
    }

    @Transactional(readOnly = true)
    public List<TeamMemberViewDTO> listMembers(Long teamId) {
        requireAdmin();
        if (!teamRepository.existsById(teamId)) {
            throw new TeamManagementException("teamnotfound");
        }
        return teamMemberRepository.findAllByTeamId(teamId).stream().map(this::toTeamMemberViewDTO).collect(Collectors.toList());
    }

    public AdminTeamDTO createTeam(AdminCreateTeamRequest request) {
        requireAdmin();
        User owner = userRepository.findById(request.getOwnerUserId()).orElseThrow(() -> new TeamManagementException("usernotfound"));
        Instant now = Instant.now();

        Team team = new Team();
        team.setName(request.getName());
        team.setDescription(request.getDescription());
        team.setCreatedDate(now);
        team = teamRepository.save(team);

        TeamMember membership = new TeamMember();
        membership.setTeam(team);
        membership.setUser(owner);
        membership.setRole(TeamRole.OWNER);
        membership.setJoinedDate(now);
        teamMemberRepository.save(membership);

        LOG.debug("Admin created team {} on behalf of {}", team.getId(), owner.getLogin());
        return toAdminTeamDTO(team);
    }

    public TeamMemberViewDTO addMember(Long teamId, AddTeamMemberRequest request) {
        requireAdmin();
        if (request.getRole() == null) {
            throw new TeamManagementException("rolerequired");
        }
        Team team = teamRepository.findById(teamId).orElseThrow(() -> new TeamManagementException("teamnotfound"));
        User user = userRepository.findById(request.getUserId()).orElseThrow(() -> new TeamManagementException("usernotfound"));
        if (teamMemberRepository.existsByTeamIdAndUserId(teamId, user.getId())) {
            throw new TeamManagementException("memberexists");
        }
        TeamMember member = new TeamMember();
        member.setTeam(team);
        member.setUser(user);
        member.setRole(request.getRole());
        member.setJoinedDate(Instant.now());
        member = teamMemberRepository.save(member);
        return toTeamMemberViewDTO(member);
    }

    public TeamMemberViewDTO changeRole(Long teamId, String userId, ChangeTeamMemberRoleRequest request) {
        requireAdmin();
        if (request.getRole() == null) {
            throw new TeamManagementException("rolerequired");
        }
        if (!teamRepository.existsById(teamId)) {
            throw new TeamManagementException("teamnotfound");
        }
        TeamMember member = teamMemberRepository
            .findOneByTeamIdAndUserId(teamId, userId)
            .orElseThrow(() -> new TeamManagementException("membernotfound"));
        if (member.getRole() == TeamRole.OWNER && request.getRole() != TeamRole.OWNER) {
            requireNotLastOwner(teamId);
        }
        member.setRole(request.getRole());
        member = teamMemberRepository.save(member);
        return toTeamMemberViewDTO(member);
    }

    public void removeMember(Long teamId, String userId) {
        requireAdmin();
        if (!teamRepository.existsById(teamId)) {
            throw new TeamManagementException("teamnotfound");
        }
        TeamMember member = teamMemberRepository
            .findOneByTeamIdAndUserId(teamId, userId)
            .orElseThrow(() -> new TeamManagementException("membernotfound"));
        if (member.getRole() == TeamRole.OWNER) {
            requireNotLastOwner(teamId);
        }
        teamMemberRepository.delete(member);
    }

    public void deleteTeam(Long teamId) {
        requireAdmin();
        if (!teamRepository.existsById(teamId)) {
            throw new TeamManagementException("teamnotfound");
        }
        if (productRepository.countByTeamId(teamId) > 0) {
            throw new TeamManagementException("teamhasproducts");
        }
        List<TeamMember> members = teamMemberRepository.findAllByTeamId(teamId);
        if (!members.isEmpty()) {
            teamMemberRepository.deleteAll(members);
        }
        teamRepository.deleteById(teamId);
    }

    // ---------------------------------------------------------------------
    // Internal helpers
    // ---------------------------------------------------------------------

    private void requireAdmin() {
        if (!SecurityUtils.hasCurrentUserThisAuthority(AuthoritiesConstants.ADMIN)) {
            throw new AccessDeniedException("Admin role required");
        }
    }

    private void requireNotLastOwner(Long teamId) {
        long owners = teamMemberRepository.countByTeamIdAndRole(teamId, TeamRole.OWNER);
        if (owners <= 1) {
            throw new TeamManagementException("lastowner");
        }
    }

    private AdminTeamDTO toAdminTeamDTO(Team team) {
        long memberCount = teamMemberRepository.countByTeamId(team.getId());
        long productCount = productRepository.countByTeamId(team.getId());
        List<String> ownerLogins = teamMemberRepository
            .findAllByTeamId(team.getId())
            .stream()
            .filter(m -> m.getRole() == TeamRole.OWNER)
            .map(TeamMember::getUser)
            .filter(u -> u != null && u.getLogin() != null)
            .map(User::getLogin)
            .sorted()
            .collect(Collectors.toList());
        return new AdminTeamDTO(
            team.getId(),
            team.getName(),
            team.getDescription(),
            team.getCreatedDate(),
            memberCount,
            productCount,
            ownerLogins
        );
    }

    private TeamMemberViewDTO toTeamMemberViewDTO(TeamMember member) {
        User user = member.getUser();
        return new TeamMemberViewDTO(
            member.getId(),
            user != null ? user.getId() : null,
            user != null ? user.getLogin() : null,
            user != null ? user.getFirstName() : null,
            user != null ? user.getLastName() : null,
            member.getRole(),
            member.getJoinedDate()
        );
    }
}
