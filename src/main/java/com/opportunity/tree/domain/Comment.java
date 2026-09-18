package com.opportunity.tree.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Threaded comment on any tree node. Exactly one of the node relationships is set.
 */
@Table("comment")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Comment implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @Column("id")
    private Long id;

    @Column("body")
    private String body;

    @NotNull(message = "must not be null")
    @Column("created_date")
    private Instant createdDate;

    @Column("edited_date")
    private Instant editedDate;

    @org.springframework.data.annotation.Transient
    private User author;

    @org.springframework.data.annotation.Transient
    @JsonIgnoreProperties(value = { "author", "parent", "outcome", "opportunity", "solution" }, allowSetters = true)
    private Comment parent;

    @org.springframework.data.annotation.Transient
    @JsonIgnoreProperties(value = { "product", "owner" }, allowSetters = true)
    private Outcome outcome;

    @org.springframework.data.annotation.Transient
    @JsonIgnoreProperties(value = { "outcome", "parent", "owner", "interviews", "tags" }, allowSetters = true)
    private Opportunity opportunity;

    @org.springframework.data.annotation.Transient
    @JsonIgnoreProperties(value = { "opportunity", "owner", "tags" }, allowSetters = true)
    private Solution solution;

    @Column("author_id")
    private String authorId;

    @Column("parent_id")
    private Long parentId;

    @Column("outcome_id")
    private Long outcomeId;

    @Column("opportunity_id")
    private Long opportunityId;

    @Column("solution_id")
    private Long solutionId;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public Comment id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBody() {
        return this.body;
    }

    public Comment body(String body) {
        this.setBody(body);
        return this;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public Instant getCreatedDate() {
        return this.createdDate;
    }

    public Comment createdDate(Instant createdDate) {
        this.setCreatedDate(createdDate);
        return this;
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    public Instant getEditedDate() {
        return this.editedDate;
    }

    public Comment editedDate(Instant editedDate) {
        this.setEditedDate(editedDate);
        return this;
    }

    public void setEditedDate(Instant editedDate) {
        this.editedDate = editedDate;
    }

    public User getAuthor() {
        return this.author;
    }

    public void setAuthor(User user) {
        this.author = user;
        this.authorId = user != null ? user.getId() : null;
    }

    public Comment author(User user) {
        this.setAuthor(user);
        return this;
    }

    public Comment getParent() {
        return this.parent;
    }

    public void setParent(Comment comment) {
        this.parent = comment;
        this.parentId = comment != null ? comment.getId() : null;
    }

    public Comment parent(Comment comment) {
        this.setParent(comment);
        return this;
    }

    public Outcome getOutcome() {
        return this.outcome;
    }

    public void setOutcome(Outcome outcome) {
        this.outcome = outcome;
        this.outcomeId = outcome != null ? outcome.getId() : null;
    }

    public Comment outcome(Outcome outcome) {
        this.setOutcome(outcome);
        return this;
    }

    public Opportunity getOpportunity() {
        return this.opportunity;
    }

    public void setOpportunity(Opportunity opportunity) {
        this.opportunity = opportunity;
        this.opportunityId = opportunity != null ? opportunity.getId() : null;
    }

    public Comment opportunity(Opportunity opportunity) {
        this.setOpportunity(opportunity);
        return this;
    }

    public Solution getSolution() {
        return this.solution;
    }

    public void setSolution(Solution solution) {
        this.solution = solution;
        this.solutionId = solution != null ? solution.getId() : null;
    }

    public Comment solution(Solution solution) {
        this.setSolution(solution);
        return this;
    }

    public String getAuthorId() {
        return this.authorId;
    }

    public void setAuthorId(String user) {
        this.authorId = user;
    }

    public Long getParentId() {
        return this.parentId;
    }

    public void setParentId(Long comment) {
        this.parentId = comment;
    }

    public Long getOutcomeId() {
        return this.outcomeId;
    }

    public void setOutcomeId(Long outcome) {
        this.outcomeId = outcome;
    }

    public Long getOpportunityId() {
        return this.opportunityId;
    }

    public void setOpportunityId(Long opportunity) {
        this.opportunityId = opportunity;
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
        if (!(o instanceof Comment)) {
            return false;
        }
        return getId() != null && getId().equals(((Comment) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Comment{" +
            "id=" + getId() +
            ", body='" + getBody() + "'" +
            ", createdDate='" + getCreatedDate() + "'" +
            ", editedDate='" + getEditedDate() + "'" +
            "}";
    }
}
