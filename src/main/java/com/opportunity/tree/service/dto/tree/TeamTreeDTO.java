package com.opportunity.tree.service.dto.tree;

import com.opportunity.tree.domain.enumeration.TeamRole;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Root document returned by {@code GET /api/teams/{teamId}/tree}.
 *
 * <p>A flat, pre-ordered list of every node in the team (products, outcomes,
 * opportunities, solutions, assumptions, evidence) plus the team's members and
 * the caller's own role, so the tree builder needs a single request.
 */
public class TeamTreeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String description;
    private String currentUserLogin;
    private TeamRole currentUserRole;
    private boolean canEdit;
    private long evidenceThisMonth;
    private List<TeamTreeMemberDTO> members = new ArrayList<>();
    private List<TreeNodeDTO> nodes = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCurrentUserLogin() {
        return currentUserLogin;
    }

    public void setCurrentUserLogin(String currentUserLogin) {
        this.currentUserLogin = currentUserLogin;
    }

    public TeamRole getCurrentUserRole() {
        return currentUserRole;
    }

    public void setCurrentUserRole(TeamRole currentUserRole) {
        this.currentUserRole = currentUserRole;
    }

    public boolean isCanEdit() {
        return canEdit;
    }

    public void setCanEdit(boolean canEdit) {
        this.canEdit = canEdit;
    }

    public long getEvidenceThisMonth() {
        return evidenceThisMonth;
    }

    public void setEvidenceThisMonth(long evidenceThisMonth) {
        this.evidenceThisMonth = evidenceThisMonth;
    }

    public List<TeamTreeMemberDTO> getMembers() {
        return members;
    }

    public void setMembers(List<TeamTreeMemberDTO> members) {
        this.members = members;
    }

    public List<TreeNodeDTO> getNodes() {
        return nodes;
    }

    public void setNodes(List<TreeNodeDTO> nodes) {
        this.nodes = nodes;
    }

    @Override
    public String toString() {
        return (
            "TeamTreeDTO{id=" +
            id +
            ", name='" +
            name +
            "', currentUserRole=" +
            currentUserRole +
            ", canEdit=" +
            canEdit +
            ", members=" +
            members.size() +
            ", nodes=" +
            nodes.size() +
            "}"
        );
    }
}
