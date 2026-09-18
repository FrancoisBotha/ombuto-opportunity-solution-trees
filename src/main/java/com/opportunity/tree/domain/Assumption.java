package com.opportunity.tree.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.opportunity.tree.domain.enumeration.AssumptionCategory;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Something that must be true for a solution to work (assumption mapping).
 */
@Table("assumption")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Assumption implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @Column("id")
    private Long id;

    @NotNull(message = "must not be null")
    @Size(min = 2, max = 500)
    @Column("statement")
    private String statement;

    @NotNull(message = "must not be null")
    @Column("category")
    private AssumptionCategory category;

    @NotNull(message = "must not be null")
    @Min(value = 1)
    @Max(value = 5)
    @Column("importance")
    private Integer importance;

    @NotNull(message = "must not be null")
    @Min(value = 1)
    @Max(value = 5)
    @Column("evidence")
    private Integer evidence;

    @Column("validated")
    private Boolean validated;

    @NotNull(message = "must not be null")
    @Column("created_date")
    private Instant createdDate;

    @org.springframework.data.annotation.Transient
    @JsonIgnoreProperties(value = { "opportunity", "owner", "tags" }, allowSetters = true)
    private Solution solution;

    @org.springframework.data.annotation.Transient
    @JsonIgnoreProperties(value = { "solution", "assumptions" }, allowSetters = true)
    private Set<Experiment> experiments = new HashSet<>();

    @Column("solution_id")
    private Long solutionId;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public Assumption id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStatement() {
        return this.statement;
    }

    public Assumption statement(String statement) {
        this.setStatement(statement);
        return this;
    }

    public void setStatement(String statement) {
        this.statement = statement;
    }

    public AssumptionCategory getCategory() {
        return this.category;
    }

    public Assumption category(AssumptionCategory category) {
        this.setCategory(category);
        return this;
    }

    public void setCategory(AssumptionCategory category) {
        this.category = category;
    }

    public Integer getImportance() {
        return this.importance;
    }

    public Assumption importance(Integer importance) {
        this.setImportance(importance);
        return this;
    }

    public void setImportance(Integer importance) {
        this.importance = importance;
    }

    public Integer getEvidence() {
        return this.evidence;
    }

    public Assumption evidence(Integer evidence) {
        this.setEvidence(evidence);
        return this;
    }

    public void setEvidence(Integer evidence) {
        this.evidence = evidence;
    }

    public Boolean getValidated() {
        return this.validated;
    }

    public Assumption validated(Boolean validated) {
        this.setValidated(validated);
        return this;
    }

    public void setValidated(Boolean validated) {
        this.validated = validated;
    }

    public Instant getCreatedDate() {
        return this.createdDate;
    }

    public Assumption createdDate(Instant createdDate) {
        this.setCreatedDate(createdDate);
        return this;
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    public Solution getSolution() {
        return this.solution;
    }

    public void setSolution(Solution solution) {
        this.solution = solution;
        this.solutionId = solution != null ? solution.getId() : null;
    }

    public Assumption solution(Solution solution) {
        this.setSolution(solution);
        return this;
    }

    public Set<Experiment> getExperiments() {
        return this.experiments;
    }

    public void setExperiments(Set<Experiment> experiments) {
        if (this.experiments != null) {
            this.experiments.forEach(i -> i.removeAssumption(this));
        }
        if (experiments != null) {
            experiments.forEach(i -> i.addAssumption(this));
        }
        this.experiments = experiments;
    }

    public Assumption experiments(Set<Experiment> experiments) {
        this.setExperiments(experiments);
        return this;
    }

    public Assumption addExperiment(Experiment experiment) {
        this.experiments.add(experiment);
        experiment.getAssumptions().add(this);
        return this;
    }

    public Assumption removeExperiment(Experiment experiment) {
        this.experiments.remove(experiment);
        experiment.getAssumptions().remove(this);
        return this;
    }

    public Long getSolutionId() {
        return this.solutionId;
    }

    public void setSolutionId(Long solution) {
        this.solutionId = solution;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Assumption)) {
            return false;
        }
        return getId() != null && getId().equals(((Assumption) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Assumption{" +
            "id=" + getId() +
            ", statement='" + getStatement() + "'" +
            ", category='" + getCategory() + "'" +
            ", importance=" + getImportance() +
            ", evidence=" + getEvidence() +
            ", validated='" + getValidated() + "'" +
            ", createdDate='" + getCreatedDate() + "'" +
            "}";
    }
}
