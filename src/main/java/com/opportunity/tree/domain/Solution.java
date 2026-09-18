package com.opportunity.tree.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.opportunity.tree.domain.enumeration.SolutionStatus;
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
 * A Solution.
 */
@Table("solution")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Solution implements Serializable {

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
    private SolutionStatus status;

    @Min(value = 1)
    @Max(value = 5)
    @Column("effort")
    private Integer effort;

    @NotNull(message = "must not be null")
    @Column("sort_order")
    private Integer sortOrder;

    @NotNull(message = "must not be null")
    @Column("created_date")
    private Instant createdDate;

    @Column("last_modified_date")
    private Instant lastModifiedDate;

    @org.springframework.data.annotation.Transient
    @JsonIgnoreProperties(value = { "outcome", "parent", "owner", "interviews", "tags" }, allowSetters = true)
    private Opportunity opportunity;

    @org.springframework.data.annotation.Transient
    private User owner;

    @org.springframework.data.annotation.Transient
    @JsonIgnoreProperties(value = { "team", "opportunities", "solutions" }, allowSetters = true)
    private Set<Tag> tags = new HashSet<>();

    @Column("opportunity_id")
    private Long opportunityId;

    @Column("owner_id")
    private String ownerId;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public Solution id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return this.title;
    }

    public Solution title(String title) {
        this.setTitle(title);
        return this;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return this.description;
    }

    public Solution description(String description) {
        this.setDescription(description);
        return this;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public SolutionStatus getStatus() {
        return this.status;
    }

    public Solution status(SolutionStatus status) {
        this.setStatus(status);
        return this;
    }

    public void setStatus(SolutionStatus status) {
        this.status = status;
    }

    public Integer getEffort() {
        return this.effort;
    }

    public Solution effort(Integer effort) {
        this.setEffort(effort);
        return this;
    }

    public void setEffort(Integer effort) {
        this.effort = effort;
    }

    public Integer getSortOrder() {
        return this.sortOrder;
    }

    public Solution sortOrder(Integer sortOrder) {
        this.setSortOrder(sortOrder);
        return this;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Instant getCreatedDate() {
        return this.createdDate;
    }

    public Solution createdDate(Instant createdDate) {
        this.setCreatedDate(createdDate);
        return this;
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    public Instant getLastModifiedDate() {
        return this.lastModifiedDate;
    }

    public Solution lastModifiedDate(Instant lastModifiedDate) {
        this.setLastModifiedDate(lastModifiedDate);
        return this;
    }

    public void setLastModifiedDate(Instant lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public Opportunity getOpportunity() {
        return this.opportunity;
    }

    public void setOpportunity(Opportunity opportunity) {
        this.opportunity = opportunity;
        this.opportunityId = opportunity != null ? opportunity.getId() : null;
    }

    public Solution opportunity(Opportunity opportunity) {
        this.setOpportunity(opportunity);
        return this;
    }

    public User getOwner() {
        return this.owner;
    }

    public void setOwner(User user) {
        this.owner = user;
        this.ownerId = user != null ? user.getId() : null;
    }

    public Solution owner(User user) {
        this.setOwner(user);
        return this;
    }

    public Set<Tag> getTags() {
        return this.tags;
    }

    public void setTags(Set<Tag> tags) {
        this.tags = tags;
    }

    public Solution tags(Set<Tag> tags) {
        this.setTags(tags);
        return this;
    }

    public Solution addTag(Tag tag) {
        this.tags.add(tag);
        return this;
    }

    public Solution removeTag(Tag tag) {
        this.tags.remove(tag);
        return this;
    }

    public Long getOpportunityId() {
        return this.opportunityId;
    }

    public void setOpportunityId(Long opportunity) {
        this.opportunityId = opportunity;
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
        if (!(o instanceof Solution)) {
            return false;
        }
        return getId() != null && getId().equals(((Solution) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Solution{" +
            "id=" + getId() +
            ", title='" + getTitle() + "'" +
            ", description='" + getDescription() + "'" +
            ", status='" + getStatus() + "'" +
            ", effort=" + getEffort() +
            ", sortOrder=" + getSortOrder() +
            ", createdDate='" + getCreatedDate() + "'" +
            ", lastModifiedDate='" + getLastModifiedDate() + "'" +
            "}";
    }
}
