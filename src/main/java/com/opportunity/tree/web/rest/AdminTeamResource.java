package com.opportunity.tree.web.rest;

import com.opportunity.tree.security.AuthoritiesConstants;
import com.opportunity.tree.service.AdminTeamService;
import com.opportunity.tree.service.TeamManagementException;
import com.opportunity.tree.service.dto.AddTeamMemberRequest;
import com.opportunity.tree.service.dto.AdminCreateTeamRequest;
import com.opportunity.tree.service.dto.AdminTeamDTO;
import com.opportunity.tree.service.dto.ChangeTeamMemberRoleRequest;
import com.opportunity.tree.service.dto.TeamMemberViewDTO;
import com.opportunity.tree.web.rest.errors.BadRequestAlertException;
import jakarta.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.PaginationUtil;

/**
 * Admin-only REST facade for team management (ADHOC-001). {@code /api/admin/**} is
 * already restricted to {@link AuthoritiesConstants#ADMIN} by
 * {@code SecurityConfiguration}; the {@link AdminTeamService} re-checks the role in
 * the service layer so a mis-configuration on the controller cannot bypass the
 * check (AC 8).
 */
@RestController
@RequestMapping("/api/admin/teams")
@PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
public class AdminTeamResource {

    private static final Logger LOG = LoggerFactory.getLogger(AdminTeamResource.class);

    private static final String ENTITY_TEAM = "team";
    private static final String ENTITY_MEMBER = "teamMember";

    private final AdminTeamService adminTeamService;

    public AdminTeamResource(AdminTeamService adminTeamService) {
        this.adminTeamService = adminTeamService;
    }

    @GetMapping("")
    public ResponseEntity<List<AdminTeamDTO>> listTeams(@org.springdoc.core.annotations.ParameterObject Pageable pageable) {
        LOG.debug("REST admin request to list all teams");
        Page<AdminTeamDTO> page = adminTeamService.listTeams(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/{id}/members")
    public List<TeamMemberViewDTO> listMembers(@PathVariable("id") Long id) {
        return call(() -> adminTeamService.listMembers(id), ENTITY_MEMBER);
    }

    @PostMapping("")
    public ResponseEntity<AdminTeamDTO> createTeam(@Valid @RequestBody AdminCreateTeamRequest request) throws URISyntaxException {
        LOG.debug("REST admin request to create team on behalf of {}", request.getOwnerUserId());
        AdminTeamDTO created = call(() -> adminTeamService.createTeam(request), ENTITY_TEAM);
        return ResponseEntity.created(new URI("/api/admin/teams/" + created.getId())).body(created);
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<TeamMemberViewDTO> addMember(@PathVariable("id") Long id, @Valid @RequestBody AddTeamMemberRequest request)
        throws URISyntaxException {
        TeamMemberViewDTO created = call(() -> adminTeamService.addMember(id, request), ENTITY_MEMBER);
        return ResponseEntity.created(new URI("/api/admin/teams/" + id + "/members/" + created.getUserId())).body(created);
    }

    @PutMapping("/{id}/members/{userId}")
    public TeamMemberViewDTO changeRole(
        @PathVariable("id") Long id,
        @PathVariable("userId") String userId,
        @Valid @RequestBody ChangeTeamMemberRoleRequest request
    ) {
        return call(() -> adminTeamService.changeRole(id, userId, request), ENTITY_MEMBER);
    }

    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<Void> removeMember(@PathVariable("id") Long id, @PathVariable("userId") String userId) {
        call(
            () -> {
                adminTeamService.removeMember(id, userId);
                return null;
            },
            ENTITY_MEMBER
        );
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTeam(@PathVariable("id") Long id) {
        call(
            () -> {
                adminTeamService.deleteTeam(id);
                return null;
            },
            ENTITY_TEAM
        );
        return ResponseEntity.noContent().build();
    }

    private <T> T call(java.util.function.Supplier<T> action, String entity) {
        try {
            return action.get();
        } catch (TeamManagementException ex) {
            throw new BadRequestAlertException(ex.getErrorKey(), entity, ex.getErrorKey());
        }
    }
}
