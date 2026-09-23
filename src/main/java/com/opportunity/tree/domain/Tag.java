package com.opportunity.tree.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Tag for cross-cutting labels (persona, segment, theme).
 */
@Entity
@Table(name = "tag")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Tag implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @NotNull
    @Size(min = 1, max = 50)
    @Column(name = "name", length = 50, nullable = false)
    private String name;

    @Size(max = 7)
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$")
    @Column(name = "colour", length = 7)
    private String colour;

    /**
     * LABEL-001: the trimmed, lower-cased form of {@link #name}, kept by the application so the
     * database can enforce (team_id, normalized_name) uniqueness across case and whitespace.
     */
    @Column(name = "normalized_name", length = 50, nullable = false)
    private String normalizedName;

    /** LABEL-001: canonical form for uniqueness comparisons — trim + lower-case (Locale.ROOT). */
    public static String normalize(String name) {
        if (name == null) {
            return "";
        }
        return name.trim().toLowerCase(java.util.Locale.ROOT);
    }

    @ManyToOne(optional = false)
    @NotNull
    private Team team;

    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "tags")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = { "outcome", "parent", "owner", "interviews", "tags" }, allowSetters = true)
    private Set<Opportunity> opportunities = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "tags")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = { "opportunity", "owner", "tags" }, allowSetters = true)
    private Set<Solution> solutions = new HashSet<>();

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
        this.normalizedName = normalize(name);
    }

    public String getNormalizedName() {
        return this.normalizedName;
    }

    public void setNormalizedName(String normalizedName) {
        this.normalizedName = normalizedName;
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
