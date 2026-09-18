package com.opportunity.tree.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.opportunity.tree.domain.enumeration.OpportunityStatus;
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
 * Customer need, pain point or desire. Nested via parent to form sub-opportunities.
 */
@Table("opportunity")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Opportunity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @Column("id")
    private Long id;

    @NotNull(message = "must not be null")
    @Size(min = 2, max = 200)
    @Column("title")
    private String title;

    @Column("description")
    private String description;

    @NotNull(message = "must not be null")
    @Column("status")
    private OpportunityStatus status;

    @NotNull(message = "must not be null")
    @Min(value = 1)
    @Max(value = 5)
    @Column("value")
    private Integer value;

    @NotNull(message = "must not be null")
    @Min(value = 1)
    @Max(value = 5)
    @Column("complexity")
    private Integer complexity;

    @NotNull(message = "must not be null")
    @Column("sort_order")
    private Integer sortOrder;

    @NotNull(message = "must not be null")
    @Column("created_date")
    private Instant createdDate;

    @Column("last_modified_date")
    private Instant lastModifiedDate;

    @org.springframework.data.annotation.Transient
    @JsonIgnoreProperties(value = { "product", "owner" }, allowSetters = true)
    private Outcome outcome;

    @org.springframework.data.annotation.Transient
    @JsonIgnoreProperties(value = { "outcome", "parent", "owner", "interviews", "tags" }, allowSetters = true)
    private Opportunity parent;

    @org.springframework.data.annotation.Transient
    private User owner;

    @org.springframework.data.annotation.Transient
    @JsonIgnoreProperties(value = { "product", "interviewer", "opportunities" }, allowSetters = true)
    private Set<Interview> interviews = new HashSet<>();

    @org.springframework.data.annotation.Transient
    @JsonIgnoreProperties(value = { "team", "opportunities", "solutions" }, allowSetters = true)
    private Set<Tag> tags = new HashSet<>();

    @Column("outcome_id")
    private Long outcomeId;

    @Column("parent_id")
    private Long parentId;

    @Column("owner_id")
    private String ownerId;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public Opportunity id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return this.title;
    }

    public Opportunity title(String title) {
        this.setTitle(title);
        return this;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return this.description;
    }

    public Opportunity description(String description) {
        this.setDescription(description);
        return this;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public OpportunityStatus getStatus() {
        return this.status;
    }

    public Opportunity status(OpportunityStatus status) {
        this.setStatus(status);
        return this;
    }

    public void setStatus(OpportunityStatus status) {
        this.status = status;
    }

    public Integer getValue() {
        return this.value;
    }

    public Opportunity value(Integer value) {
        this.setValue(value);
        return this;
    }

    public void setValue(Integer value) {
        this.value = value;
    }

    public Integer getComplexity() {
        return this.complexity;
    }

    public Opportunity complexity(Integer complexity) {
        this.setComplexity(complexity);
        return this;
    }

    public void setComplexity(Integer complexity) {
        this.complexity = complexity;
    }

    public Integer getSortOrder() {
        return this.sortOrder;
    }

    public Opportunity sortOrder(Integer sortOrder) {
        this.setSortOrder(sortOrder);
        return this;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Instant getCreatedDate() {
        return this.createdDate;
    }

    public Opportunity createdDate(Instant createdDate) {
        this.setCreatedDate(createdDate);
        return this;
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    public Instant getLastModifiedDate() {
        return this.lastModifiedDate;
    }

    public Opportunity lastModifiedDate(Instant lastModifiedDate) {
        this.setLastModifiedDate(lastModifiedDate);
        return this;
    }

    public void setLastModifiedDate(Instant lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public Outcome getOutcome() {
        return this.outcome;
    }

    public void setOutcome(Outcome outcome) {
        this.outcome = outcome;
        this.outcomeId = outcome != null ? outcome.getId() : null;
    }

    public Opportunity outcome(Outcome outcome) {
        this.setOutcome(outcome);
        return this;
    }

    public Opportunity getParent() {
        return this.parent;
    }

    public void setParent(Opportunity opportunity) {
        this.parent = opportunity;
        this.parentId = opportunity != null ? opportunity.getId() : null;
    }

    public Opportunity parent(Opportunity opportunity) {
        this.setParent(opportunity);
        return this;
    }

    public User getOwner() {
        return this.owner;
    }

    public void setOwner(User user) {
        this.owner = user;
        this.ownerId = user != null ? user.getId() : null;
    }

    public Opportunity owner(User user) {
        this.setOwner(user);
        return this;
    }

    public Set<Interview> getInterviews() {
        return this.interviews;
    }

    public void setInterviews(Set<Interview> interviews) {
        this.interviews = interviews;
    }

    public Opportunity interviews(Set<Interview> interviews) {
        this.setInterviews(interviews);
        return this;
    }

    public Opportunity addInterview(Interview interview) {
        this.interviews.add(interview);
        return this;
    }

    public Opportunity removeInterview(Interview interview) {
        this.interviews.remove(interview);
        return this;
    }

    public Set<Tag> getTags() {
        return this.tags;
    }

    public void setTags(Set<Tag> tags) {
        this.tags = tags;
    }

    public Opportunity tags(Set<Tag> tags) {
        this.setTags(tags);
        return this;
    }

    public Opportunity addTag(Tag tag) {
        this.tags.add(tag);
        return this;
    }

    public Opportunity removeTag(Tag tag) {
        this.tags.remove(tag);
        return this;
    }

    public Long getOutcomeId() {
        return this.outcomeId;
    }

    public void setOutcomeId(Long outcome) {
        this.outcomeId = outcome;
    }

    public Long getParentId() {
        return this.parentId;
    }

    public void setParentId(Long opportunity) {
        this.parentId = opportunity;
    }

    public String getOwnerId() {
        return this.ownerId;
    }

    public void setOwnerId(String user) {
        this.ownerId = user;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Opportunity)) {
            return false;
        }
        return getId() != null && getId().equals(((Opportunity) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Opportunity{" +
            "id=" + getId() +
            ", title='" + getTitle() + "'" +
            ", description='" + getDescription() + "'" +
            ", status='" + getStatus() + "'" +
            ", value=" + getValue() +
            ", complexity=" + getComplexity() +
            ", sortOrder=" + getSortOrder() +
            ", createdDate='" + getCreatedDate() + "'" +
            ", lastModifiedDate='" + getLastModifiedDate() + "'" +
            "}";
    }
}
