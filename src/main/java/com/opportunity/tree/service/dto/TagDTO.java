package com.opportunity.tree.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A DTO for the {@link com.opportunity.tree.domain.Tag} entity.
 */
@Schema(description = "Tag for cross-cutting labels (persona, segment, theme).")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class TagDTO implements Serializable {

    private Long id;

    @NotNull(message = "must not be null")
    @Size(min = 1, max = 50)
    private String name;

    @Size(max = 7)
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$")
    private String colour;

    @NotNull
    private TeamDTO team;

    private Set<OpportunityDTO> opportunities = new HashSet<>();

    private Set<SolutionDTO> solutions = new HashSet<>();

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

    public String getColour() {
        return colour;
    }

    public void setColour(String colour) {
        this.colour = colour;
    }

    public TeamDTO getTeam() {
        return team;
    }

    public void setTeam(TeamDTO team) {
        this.team = team;
    }

    public Set<OpportunityDTO> getOpportunities() {
        return opportunities;
    }

    public void setOpportunities(Set<OpportunityDTO> opportunities) {
        this.opportunities = opportunities;
    }

    public Set<SolutionDTO> getSolutions() {
        return solutions;
    }

    public void setSolutions(Set<SolutionDTO> solutions) {
        this.solutions = solutions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TagDTO)) {
            return false;
        }

        TagDTO tagDTO = (TagDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, tagDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "TagDTO{" +
            "id=" + getId() +
            ", name='" + getName() + "'" +
            ", colour='" + getColour() + "'" +
            ", team=" + getTeam() +
            ", opportunities=" + getOpportunities() +
            ", solutions=" + getSolutions() +
            "}";
    }
}
