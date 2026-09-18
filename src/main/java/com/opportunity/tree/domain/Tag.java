package com.opportunity.tree.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Tag for cross-cutting labels (persona, segment, theme).
 */
@Table("tag")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Tag implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @Column("id")
    private Long id;

    @NotNull(message = "must not be null")
    @Size(min = 1, max = 50)
    @Column("name")
    private String name;

    @Size(max = 7)
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$")
    @Column("colour")
    private String colour;

    @org.springframework.data.annotation.Transient
    private Team team;

    @org.springframework.data.annotation.Transient
    @JsonIgnoreProperties(value = { "outcome", "parent", "owner", "interviews", "tags" }, allowSetters = true)
    private Set<Opportunity> opportunities = new HashSet<>();

    @org.springframework.data.annotation.Transient
    @JsonIgnoreProperties(value = { "opportunity", "owner", "tags" }, allowSetters = true)
    private Set<Solution> solutions = new HashSet<>();

    @Column("team_id")
    private Long teamId;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public Tag id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public Tag name(String name) {
        this.setName(name);
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getColour() {
        return this.colour;
    }

    public Tag colour(String colour) {
        this.setColour(colour);
        return this;
    }

    public void setColour(String colour) {
        this.colour = colour;
    }

    public Team getTeam() {
        return this.team;
    }

    public void setTeam(Team team) {
        this.team = team;
        this.teamId = team != null ? team.getId() : null;
    }

    public Tag team(Team team) {
        this.setTeam(team);
        return this;
    }

    public Set<Opportunity> getOpportunities() {
        return this.opportunities;
    }

    public void setOpportunities(Set<Opportunity> opportunities) {
        if (this.opportunities != null) {
            this.opportunities.forEach(i -> i.removeTag(this));
        }
        if (opportunities != null) {
            opportunities.forEach(i -> i.addTag(this));
        }
        this.opportunities = opportunities;
    }

    public Tag opportunities(Set<Opportunity> opportunities) {
        this.setOpportunities(opportunities);
        return this;
    }

    public Tag addOpportunity(Opportunity opportunity) {
        this.opportunities.add(opportunity);
        opportunity.getTags().add(this);
        return this;
    }

    public Tag removeOpportunity(Opportunity opportunity) {
        this.opportunities.remove(opportunity);
        opportunity.getTags().remove(this);
        return this;
    }

    public Set<Solution> getSolutions() {
        return this.solutions;
    }

    public void setSolutions(Set<Solution> solutions) {
        if (this.solutions != null) {
            this.solutions.forEach(i -> i.removeTag(this));
        }
        if (solutions != null) {
            solutions.forEach(i -> i.addTag(this));
        }
        this.solutions = solutions;
    }

    public Tag solutions(Set<Solution> solutions) {
        this.setSolutions(solutions);
        return this;
    }

    public Tag addSolution(Solution solution) {
        this.solutions.add(solution);
        solution.getTags().add(this);
        return this;
    }

    public Tag removeSolution(Solution solution) {
        this.solutions.remove(solution);
        solution.getTags().remove(this);
        return this;
    }

    public Long getTeamId() {
        return this.teamId;
    }

    public void setTeamId(Long team) {
        this.teamId = team;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Tag)) {
            return false;
        }
        return getId() != null && getId().equals(((Tag) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Tag{" +
            "id=" + getId() +
            ", name='" + getName() + "'" +
            ", colour='" + getColour() + "'" +
            "}";
    }
}
