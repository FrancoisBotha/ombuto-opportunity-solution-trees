package com.opportunity.tree.web.rest;

import com.opportunity.tree.service.TeamManagementException;
import com.opportunity.tree.service.TeamManagementService;
import com.opportunity.tree.service.dto.AddTeamMemberRequest;
import com.opportunity.tree.service.dto.ChangeTeamMemberRoleRequest;
import com.opportunity.tree.service.dto.CreateTeamRequest;
import com.opportunity.tree.service.dto.MyTeamDTO;
import com.opportunity.tree.service.dto.TeamMemberViewDTO;
import com.opportunity.tree.service.dto.UpdateTeamRequest;
import com.opportunity.tree.service.dto.UserSearchResultDTO;
import com.opportunity.tree.web.rest.errors.BadRequestAlertException;
import jakarta.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST facade for the team-management flows introduced by TEAMS-002. Every endpoint delegates
 * authorisation to {@code TeamManagementService} / {@code TeamAccessService} — this class only
 * translates HTTP concerns and re-raises rule violations as {@link BadRequestAlertException}.
 */
@RestController
@RequestMapping("/api/team-management")
public class TeamManagementResource {

    private static final Logger LOG = LoggerFactory.getLogger(TeamManagementResource.class);

    private static final String ENTITY_TEAM = "team";
    private static final String ENTITY_MEMBER = "teamMember";

    private final TeamManagementService teamManagementService;

    public TeamManagementResource(TeamManagementService teamManagementService) {
        this.teamManagementService = teamManagementService;
    }

    @PostMapping("/teams")
    public ResponseEntity<MyTeamDTO> createTeam(@Valid @RequestBody CreateTeamRequest request) throws URISyntaxException {
        LOG.debug("REST request to create Team : {}", request.getName());
        MyTeamDTO created = call(() -> teamManagementService.createTeam(request), ENTITY_TEAM);
        return ResponseEntity.created(new URI("/api/team-management/teams/" + created.getId())).body(created);
    }

    @GetMapping("/my-teams")
    public List<MyTeamDTO> listMyTeams() {
        return teamManagementService.listMyTeams();
    }

    @GetMapping("/teams/{id}")
    public MyTeamDTO getTeam(@PathVariable("id") Long id) {
        return teamManagementService.getTeam(id);
    }

    @PutMapping("/teams/{id}")
    public MyTeamDTO updateTeam(@PathVariable("id") Long id, @Valid @RequestBody UpdateTeamRequest request) {
        return call(() -> teamManagementService.updateTeam(id, request), ENTITY_TEAM);
    }

    @GetMapping("/teams/{id}/members")
    public List<TeamMemberViewDTO> listMembers(@PathVariable("id") Long id) {
        return teamManagementService.listMembers(id);
    }

    @PostMapping("/teams/{id}/members")
    public ResponseEntity<TeamMemberViewDTO> addMember(@PathVariable("id") Long id, @Valid @RequestBody AddTeamMemberRequest request)
        throws URISyntaxException {
        TeamMemberViewDTO created = call(() -> teamManagementService.addMember(id, request), ENTITY_MEMBER);
        return ResponseEntity.created(new URI("/api/team-management/teams/" + id + "/members/" + created.getUserId())).body(created);
    }

    @PutMapping("/teams/{id}/members/{userId}")
    public TeamMemberViewDTO changeRole(
        @PathVariable("id") Long id,
        @PathVariable("userId") String userId,
        @Valid @RequestBody ChangeTeamMemberRoleRequest request
    ) {
        return call(() -> teamManagementService.changeRole(id, userId, request), ENTITY_MEMBER);
    }

    @DeleteMapping("/teams/{id}/members/{userId}")
    public ResponseEntity<Void> removeMember(@PathVariable("id") Long id, @PathVariable("userId") String userId) {
        call(
            () -> {
                teamManagementService.removeMember(id, userId);
                return null;
            },
            ENTITY_MEMBER
        );
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/teams/{id}/user-search")
    public List<UserSearchResultDTO> searchUsers(
        @PathVariable("id") Long id,
        @RequestParam(name = "q", required = false, defaultValue = "") String q,
        @RequestParam(name = "limit", required = false, defaultValue = "20") int limit
    ) {
        return teamManagementService.searchUsers(id, q, limit);
    }

    private <T> T call(java.util.function.Supplier<T> action, String entity) {
        try {
            return action.get();
        } catch (TeamManagementException ex) {
            throw new BadRequestAlertException(ex.getErrorKey(), entity, ex.getErrorKey());
        }
    }
}
