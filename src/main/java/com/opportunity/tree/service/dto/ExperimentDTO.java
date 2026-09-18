package com.opportunity.tree.service.dto;

import com.opportunity.tree.domain.enumeration.ExperimentResult;
import com.opportunity.tree.domain.enumeration.ExperimentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A DTO for the {@link com.opportunity.tree.domain.Experiment} entity.
 */
@Schema(description = "A test run against one or more assumptions.")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ExperimentDTO implements Serializable {

    private Long id;

    @NotNull
    @Size(min = 2, max = 200)
    private String title;

    @Lob
    private String hypothesis;

    @Size(max = 200)
    private String method;

    @Lob
    private String successCriteria;

    @NotNull
    private ExperimentStatus status;

    private ExperimentResult result;

    @Lob
    private String learnings;

    private LocalDate startDate;

    private LocalDate endDate;

    @NotNull
    private Instant createdDate;

    @NotNull
    private SolutionDTO solution;

    private Set<AssumptionDTO> assumptions = new HashSet<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getHypothesis() {
        return hypothesis;
    }

    public void setHypothesis(String hypothesis) {
        this.hypothesis = hypothesis;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getSuccessCriteria() {
        return successCriteria;
    }

    public void setSuccessCriteria(String successCriteria) {
        this.successCriteria = successCriteria;
    }

    public ExperimentStatus getStatus() {
        return status;
    }

    public void setStatus(ExperimentStatus status) {
        this.status = status;
    }

    public ExperimentResult getResult() {
        return result;
    }

    public void setResult(ExperimentResult result) {
        this.result = result;
    }

    public String getLearnings() {
        return learnings;
    }

    public void setLearnings(String learnings) {
        this.learnings = learnings;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
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

    public Set<AssumptionDTO> getAssumptions() {
        return assumptions;
    }

    public void setAssumptions(Set<AssumptionDTO> assumptions) {
        this.assumptions = assumptions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ExperimentDTO)) {
            return false;
        }

        ExperimentDTO experimentDTO = (ExperimentDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, experimentDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ExperimentDTO{" +
            "id=" + getId() +
            ", title='" + getTitle() + "'" +
            ", hypothesis='" + getHypothesis() + "'" +
            ", method='" + getMethod() + "'" +
            ", successCriteria='" + getSuccessCriteria() + "'" +
            ", status='" + getStatus() + "'" +
            ", result='" + getResult() + "'" +
            ", learnings='" + getLearnings() + "'" +
            ", startDate='" + getStartDate() + "'" +
            ", endDate='" + getEndDate() + "'" +
            ", createdDate='" + getCreatedDate() + "'" +
            ", solution=" + getSolution() +
            ", assumptions=" + getAssumptions() +
            "}";
    }
}
