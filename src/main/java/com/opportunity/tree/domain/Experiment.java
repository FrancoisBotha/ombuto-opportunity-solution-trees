package com.opportunity.tree.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.opportunity.tree.domain.enumeration.ExperimentResult;
import com.opportunity.tree.domain.enumeration.ExperimentStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * A test run against one or more assumptions.
 */
@Entity
@Table(name = "experiment")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Experiment implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @NotNull
    @Size(min = 2, max = 200)
    @Column(name = "title", length = 200, nullable = false)
    private String title;

    @Lob
    @Column(name = "hypothesis")
    private String hypothesis;

    @Size(max = 200)
    @Column(name = "method", length = 200)
    private String method;

    @Lob
    @Column(name = "success_criteria")
    private String successCriteria;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ExperimentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "result")
    private ExperimentResult result;

    @Lob
    @Column(name = "learnings")
    private String learnings;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @NotNull
    @Column(name = "created_date", nullable = false)
    private Instant createdDate;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "opportunity", "owner", "tags" }, allowSetters = true)
    private Solution solution;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "rel_experiment__assumption",
        joinColumns = @JoinColumn(name = "experiment_id"),
        inverseJoinColumns = @JoinColumn(name = "assumption_id")
    )
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = { "solution", "experiments" }, allowSetters = true)
    private Set<Assumption> assumptions = new HashSet<>();

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public Experiment id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return this.title;
    }

    public Experiment title(String title) {
        this.setTitle(title);
        return this;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getHypothesis() {
        return this.hypothesis;
    }

    public Experiment hypothesis(String hypothesis) {
        this.setHypothesis(hypothesis);
        return this;
    }

    public void setHypothesis(String hypothesis) {
        this.hypothesis = hypothesis;
    }

    public String getMethod() {
        return this.method;
    }

    public Experiment method(String method) {
        this.setMethod(method);
        return this;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getSuccessCriteria() {
        return this.successCriteria;
    }

    public Experiment successCriteria(String successCriteria) {
        this.setSuccessCriteria(successCriteria);
        return this;
    }

    public void setSuccessCriteria(String successCriteria) {
        this.successCriteria = successCriteria;
    }

    public ExperimentStatus getStatus() {
        return this.status;
    }

    public Experiment status(ExperimentStatus status) {
        this.setStatus(status);
        return this;
    }

    public void setStatus(ExperimentStatus status) {
        this.status = status;
    }

    public ExperimentResult getResult() {
        return this.result;
    }

    public Experiment result(ExperimentResult result) {
        this.setResult(result);
        return this;
    }

    public void setResult(ExperimentResult result) {
        this.result = result;
    }

    public String getLearnings() {
        return this.learnings;
    }

    public Experiment learnings(String learnings) {
        this.setLearnings(learnings);
        return this;
    }

    public void setLearnings(String learnings) {
        this.learnings = learnings;
    }

    public LocalDate getStartDate() {
        return this.startDate;
    }

    public Experiment startDate(LocalDate startDate) {
        this.setStartDate(startDate);
        return this;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return this.endDate;
    }

    public Experiment endDate(LocalDate endDate) {
        this.setEndDate(endDate);
        return this;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public Instant getCreatedDate() {
        return this.createdDate;
    }

    public Experiment createdDate(Instant createdDate) {
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
    }

    public Experiment solution(Solution solution) {
        this.setSolution(solution);
        return this;
    }

    public Set<Assumption> getAssumptions() {
        return this.assumptions;
    }

    public void setAssumptions(Set<Assumption> assumptions) {
        this.assumptions = assumptions;
    }

    public Experiment assumptions(Set<Assumption> assumptions) {
        this.setAssumptions(assumptions);
        return this;
    }

    public Experiment addAssumption(Assumption assumption) {
        this.assumptions.add(assumption);
        return this;
    }

    public Experiment removeAssumption(Assumption assumption) {
        this.assumptions.remove(assumption);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Experiment)) {
            return false;
        }
        return getId() != null && getId().equals(((Experiment) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Experiment{" +
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
            "}";
    }
}
