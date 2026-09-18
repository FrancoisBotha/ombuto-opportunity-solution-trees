package com.opportunity.tree.service.dto;

import com.opportunity.tree.domain.enumeration.AssumptionCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A DTO for the {@link com.opportunity.tree.domain.Assumption} entity.
 */
@Schema(description = "Something that must be true for a solution to work (assumption mapping).")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class AssumptionDTO implements Serializable {

    private Long id;

    @NotNull(message = "must not be null")
    @Size(min = 2, max = 500)
    private String statement;

    @NotNull(message = "must not be null")
    private AssumptionCategory category;

    @NotNull(message = "must not be null")
    @Min(value = 1)
    @Max(value = 5)
    private Integer importance;

    @NotNull(message = "must not be null")
    @Min(value = 1)
    @Max(value = 5)
    private Integer evidence;

    private Boolean validated;

    @NotNull(message = "must not be null")
    private Instant createdDate;

    @NotNull
    private SolutionDTO solution;

    private Set<ExperimentDTO> experiments = new HashSet<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStatement() {
        return statement;
    }

    public void setStatement(String statement) {
        this.statement = statement;
    }

    public AssumptionCategory getCategory() {
        return category;
    }

    public void setCategory(AssumptionCategory category) {
        this.category = category;
    }

    public Integer getImportance() {
        return importance;
    }

    public void setImportance(Integer importance) {
        this.importance = importance;
    }

    public Integer getEvidence() {
        return evidence;
    }

    public void setEvidence(Integer evidence) {
        this.evidence = evidence;
    }

    public Boolean getValidated() {
        return validated;
    }

    public void setValidated(Boolean validated) {
        this.validated = validated;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    public SolutionDTO getSolution() {
        return solution;
    }

    public void setSolution(SolutionDTO solution) {
        this.solution = solution;
    }

    public Set<ExperimentDTO> getExperiments() {
        return experiments;
    }

    public void setExperiments(Set<ExperimentDTO> experiments) {
        this.experiments = experiments;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AssumptionDTO)) {
            return false;
        }

        AssumptionDTO assumptionDTO = (AssumptionDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, assumptionDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "AssumptionDTO{" +
            "id=" + getId() +
            ", statement='" + getStatement() + "'" +
            ", category='" + getCategory() + "'" +
            ", importance=" + getImportance() +
            ", evidence=" + getEvidence() +
            ", validated='" + getValidated() + "'" +
            ", createdDate='" + getCreatedDate() + "'" +
            ", solution=" + getSolution() +
            ", experiments=" + getExperiments() +
            "}";
    }
}
