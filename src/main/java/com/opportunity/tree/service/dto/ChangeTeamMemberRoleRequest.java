package com.opportunity.tree.service.dto;

import com.opportunity.tree.domain.enumeration.TeamRole;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

public class ChangeTeamMemberRoleRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull
    private TeamRole role;

    public TeamRole getRole() {
        return role;
    }

    public void setRole(TeamRole role) {
        this.role = role;
    }
}
