package com.opportunity.tree.service;

import com.opportunity.tree.domain.Team;
import com.opportunity.tree.domain.TeamMember;
import com.opportunity.tree.domain.User;
import com.opportunity.tree.domain.enumeration.TeamRole;
import com.opportunity.tree.repository.ProductRepository;
import com.opportunity.tree.repository.TeamMemberRepository;
import com.opportunity.tree.repository.TeamRepository;
import com.opportunity.tree.repository.UserRepository;
import com.opportunity.tree.security.SecurityUtils;
import com.opportunity.tree.service.broadcast.MembershipChangedPayload;
import com.opportunity.tree.service.broadcast.TreeChangePublisher;
import com.opportunity.tree.service.broadcast.TreeChangeType;
import com.opportunity.tree.service.dto.AddTeamMemberRequest;
import com.opportunity.tree.service.dto.ChangeTeamMemberRoleRequest;
import com.opportunity.tree.service.dto.CreateTeamRequest;
import com.opportunity.tree.service.dto.MyTeamDTO;
import com.opportunity.tree.service.dto.TeamMemberViewDTO;
import com.opportunity.tree.service.dto.UpdateTeamRequest;
import com.opportunity.tree.service.dto.UserSearchResultDTO;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Encapsulates the team and membership operations exposed by TEAMS-002, keeping every
 * authorisation decision on the server side of the service boundary (AC 8 / NFR-001).
 *
 * <p>All read/edit/owner checks go through {@link TeamAccessService}. Duplicate membership,
 * last-owner protection and other rule violations surface as {@link TeamManagementException},
 * which controllers translate into a 4xx {@code error.<key>} response.
 */
@Service
@Transactional
public class TeamManagementService {

    private static final Logger LOG = LoggerFactory.getLogger(TeamManagementService.class);

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final TeamAccessService teamAccessService;
    private final TreeChangePublisher changePublisher;
    private final TreeStructureLock structureLock;

    public TeamManagementService(
        TeamRepository teamRepository,
        TeamMemberRepository teamMemberRepository,
        ProductRepository productRepository,
        UserRepository userRepository,
        TeamAccessService teamAccessService,
        TreeChangePublisher changePublisher,
        TreeStructureLock structureLock
    ) {
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.teamAccessService = teamAccessService;
        this.changePublisher = changePublisher;
        this.structureLock = structureLock;
    }

    // ---------------------------------------------------------------------
    // Team creation and update
    // ---------------------------------------------------------------------

    public MyTeamDTO createTeam(CreateTeamRequest request) {
        User creator = requireCurrentUser();
        Instant now = Instant.now();

        Team team = new Team();
        team.setName(request.getName());
        team.setDescription(request.getDescription());
        team.setCreatedDate(now);
        team = teamRepository.save(team);

        TeamMember membership = new TeamMember();
        membership.setTeam(team);
        membership.setUser(creator);
        membership.setRole(TeamRole.OWNER);
        membership.setJoinedDate(now);
        teamMemberRepository.save(membership);

        LOG.debug("Team {} created by {} as OWNER", team.getId(), creator.getLogin());
        return new MyTeamDTO(team.getId(), team.getName(), team.getDescription(), team.getCreatedDate(), TeamRole.OWNER, 1L, 0L);
    }

    public MyTeamDTO updateTeam(Long teamId, UpdateTeamRequest request) {
        teamAccessService.requireOwnerTeam(teamId);
        Team team = teamRepository.findById(teamId).orElseThrow(() -> new TeamManagementException("teamnotfound"));
        team.setName(request.getName());
        team.setDescription(request.getDescription());
        team = teamRepository.save(team);
        return toMyTeamDTO(team, TeamRole.OWNER);
    }

    // ---------------------------------------------------------------------
    // Team reads
    // ---------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<MyTeamDTO> listMyTeams() {
        String login = SecurityUtils.getCurrentUserLogin().orElse(null);
        if (login == null) {
            return List.of();
        }
        // One projection statement (teams + counts), never a lazy Team load per membership: a team
        // deleted while this runs must not turn the whole list into a 500.
        return teamMemberRepository
            .findMyTeamRows(login)
            .stream()
            .map(row ->
                new MyTeamDTO(
                    (Long) row[0],
                    (String) row[1],
                    (String) row[2],
                    (Instant) row[3],
                    (TeamRole) row[4],
                    ((Number) row[5]).longValue(),
                    ((Number) row[6]).longValue()
                )
            )
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MyTeamDTO getTeam(Long teamId) {
        teamAccessService.requireReadTeam(teamId);
        Team team = teamRepository.findById(teamId).orElseThrow(TeamAccessDeniedException::new);
        TeamRole role = teamAccessService.getCurrentUserRole(teamId).orElseThrow(TeamAccessDeniedException::new);
        return toMyTeamDTO(team, role);
    }

    @Transactional(readOnly = true)
    public List<TeamMemberViewDTO> listMembers(Long teamId) {
        teamAccessService.requireReadTeam(teamId);
        return teamMemberRepository.findAllByTeamId(teamId).stream().map(this::toTeamMemberViewDTO).collect(Collectors.toList());
    }

    // ---------------------------------------------------------------------
    // Membership management (owner-only)
    // ---------------------------------------------------------------------

    public TeamMemberViewDTO addMember(Long teamId, AddTeamMemberRequest request) {
        teamAccessService.requireOwnerTeam(teamId);
        if (request.getRole() == null) {
            throw new TeamManagementException("rolerequired");
        }
        // Under the same per-team lock as every other membership write, so the MEMBERSHIP_CHANGED
        // seq below is ordered against the team's tree events (epic §11 risk 1).
        structureLock.lockTeam(teamId);
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
        changePublisher.publish(
            TreeChangeType.MEMBERSHIP_CHANGED,
            teamId,
            new MembershipChangedPayload(user.getLogin(), member.getRole(), false)
        );
        return toTeamMemberViewDTO(member);
    }

    public TeamMemberViewDTO changeRole(Long teamId, String userId, ChangeTeamMemberRoleRequest request) {
        teamAccessService.requireOwnerTeam(teamId);
        if (request.getRole() == null) {
            throw new TeamManagementException("rolerequired");
        }
        // Before reading the membership rows: the last-owner count below is a read-then-write and
        // two concurrent demotions would otherwise both see two owners and leave the team with none.
        structureLock.lockTeam(teamId);
        TeamMember member = teamMemberRepository
            .findOneByTeamIdAndUserId(teamId, userId)
            .orElseThrow(() -> new TeamManagementException("membernotfound"));
        if (member.getRole() == TeamRole.OWNER && request.getRole() != TeamRole.OWNER) {
            requireNotLastOwner(teamId);
        }
        member.setRole(request.getRole());
        member = teamMemberRepository.save(member);
        User user = member.getUser();
        String login = user == null ? null : user.getLogin();
        changePublisher.publish(TreeChangeType.MEMBERSHIP_CHANGED, teamId, new MembershipChangedPayload(login, member.getRole(), false));
        return toTeamMemberViewDTO(member);
    }

    public void removeMember(Long teamId, String userId) {
        teamAccessService.requireOwnerTeam(teamId);
        // See changeRole: the last-owner count must be read under the team's structure lock.
        structureLock.lockTeam(teamId);
        TeamMember member = teamMemberRepository
            .findOneByTeamIdAndUserId(teamId, userId)
            .orElseThrow(() -> new TeamManagementException("membernotfound"));
        if (member.getRole() == TeamRole.OWNER) {
            requireNotLastOwner(teamId);
        }
        User user = member.getUser();
        String login = user == null ? null : user.getLogin();
        teamMemberRepository.delete(member);
        // Broadcast on the team topic — the removed member's client sees its own login in the
        // payload, updates canEdit / currentUserRole, and unsubscribes so no further tree events
        // on this team reach it (FR-037, RTC-002 AC #6).
        changePublisher.publish(TreeChangeType.MEMBERSHIP_CHANGED, teamId, new MembershipChangedPayload(login, null, true));
    }

    // ---------------------------------------------------------------------
    // User search (owner-only member picker)
    // ---------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<UserSearchResultDTO> searchUsers(Long teamId, String query, int limit) {
        teamAccessService.requireOwnerTeam(teamId);
        String q = query == null ? "" : query.trim();
        int size = Math.min(Math.max(limit, 1), 50);
        Pageable pageable = PageRequest.of(0, size, Sort.by("login").ascending());
        return userRepository
            .searchByLoginOrName(q, pageable)
            .getContent()
            .stream()
            .map(u -> new UserSearchResultDTO(u.getId(), u.getLogin(), displayName(u)))
            .collect(Collectors.toList());
    }

    // ---------------------------------------------------------------------
    // Internal helpers
    // ---------------------------------------------------------------------

    private void requireNotLastOwner(Long teamId) {
        long owners = teamMemberRepository.countByTeamIdAndRole(teamId, TeamRole.OWNER);
        if (owners <= 1) {
            throw new TeamManagementException("lastowner");
        }
    }

    private User requireCurrentUser() {
        String login = SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new TeamManagementException("notauthenticated"));
        return userRepository.findOneByLogin(login).orElseThrow(() -> new TeamManagementException("usernotsynced"));
    }

    private MyTeamDTO toMyTeamDTO(Team team, TeamRole role) {
        long memberCount = teamMemberRepository.countByTeamId(team.getId());
        long productCount = productRepository.countByTeamId(team.getId());
        return new MyTeamDTO(team.getId(), team.getName(), team.getDescription(), team.getCreatedDate(), role, memberCount, productCount);
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

    private String displayName(User u) {
        String first = u.getFirstName();
        String last = u.getLastName();
        if ((first == null || first.isBlank()) && (last == null || last.isBlank())) {
            return u.getLogin();
        }
        return ((first == null ? "" : first) + " " + (last == null ? "" : last)).trim();
    }
}
