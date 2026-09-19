package com.opportunity.tree.service.dto;

import com.opportunity.tree.domain.enumeration.AssumptionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * A DTO for the {@link com.opportunity.tree.domain.Assumption} entity.
 */
@Schema(description = "Something that must be true for a solution to work (assumption mapping).")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class AssumptionDTO implements Serializable {

    private Long id;

    @NotNull
    @Size(min = 2, max = 500)
    private String statement;

    @Lob
    private String description;

    @NotNull
    private AssumptionStatus status;

    @NotNull
    @Min(value = 0)
    @Max(value = 100)
    private Integer confidence;

    @NotNull
    private Integer sortOrder;

    @NotNull
    private Instant createdDate;

    private Instant lastModifiedDate;

    @NotNull
    private SolutionDTO solution;

    private UserDTO owner;

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public AssumptionStatus getStatus() {
        return status;
    }

    public void setStatus(AssumptionStatus status) {
        this.status = status;
    }

    public Integer getConfidence() {
        return confidence;
    }

    public void setConfidence(Integer confidence) {
        this.confidence = confidence;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(Instant lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public SolutionDTO getSolution() {
        return solution;
    }

    public void setSolution(SolutionDTO solution) {
        this.solution = solution;
    }

    public UserDTO getOwner() {
        return owner;
    }

    public void setOwner(UserDTO owner) {
        this.owner = owner;
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
            ", description='" + getDescription() + "'" +
            ", status='" + getStatus() + "'" +
            ", confidence=" + getConfidence() +
            ", sortOrder=" + getSortOrder() +
            ", createdDate='" + getCreatedDate() + "'" +
            ", lastModifiedDate='" + getLastModifiedDate() + "'" +
            ", solution=" + getSolution() +
            ", owner=" + getOwner() +
            "}";
    }
}
