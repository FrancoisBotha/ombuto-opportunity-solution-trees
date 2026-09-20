package com.opportunity.tree.service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;

/**
 * Client-supplied payload for creating a team through {@code POST /api/team-management/teams}.
 * Only {@code name} and {@code description} are honoured — {@code createdDate} and the creator's
 * membership are set server-side (see TEAMS-002 AC 1).
 */
public class CreateTeamRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull
    @Size(min = 2, max = 100)
    private String name;

    private String description;

    public String getName() {
        return name;
    }

    /**
     * Normalises the name before Bean Validation sees it, so a whitespace-only name fails
     * {@code @Size(min = 2)} with a 400 instead of creating a team with a blank name.
     */
    public void setName(String name) {
        this.name = name == null ? null : name.trim();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
