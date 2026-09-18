package com.opportunity.tree.web.rest;

import com.opportunity.tree.service.TeamTreeService;
import com.opportunity.tree.service.dto.tree.TeamTreeDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing the whole-tree read endpoint (TREE-001, FR-010).
 *
 * <p>Returns a team's team, products, outcomes, opportunities and solutions in
 * a single JSON document. Authorised through {@code TeamAccessService}: any
 * member (owner, editor, viewer) gets 200; non-members get 403; unauthenticated
 * callers get 401 from the security filter chain.
 */
@RestController
@RequestMapping("/api/teams")
public class TeamTreeResource {

    private static final Logger LOG = LoggerFactory.getLogger(TeamTreeResource.class);

    private final TeamTreeService teamTreeService;

    public TeamTreeResource(TeamTreeService teamTreeService) {
        this.teamTreeService = teamTreeService;
    }

    @GetMapping("/{teamId}/tree")
    public TeamTreeDTO getTeamTree(@PathVariable("teamId") Long teamId) {
        LOG.debug("REST request to get whole tree for team {}", teamId);
        return teamTreeService.getTreeForTeam(teamId);
    }
}
