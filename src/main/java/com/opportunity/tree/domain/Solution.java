package com.opportunity.tree.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.opportunity.tree.domain.enumeration.SolutionStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * A Solution.
 */
@Entity
@Table(name = "solution")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Solution implements Serializable {

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
    @Column(name = "description")
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SolutionStatus status;

    @NotNull
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @NotNull
    @Column(name = "created_date", nullable = false)
    private Instant createdDate;

    @Column(name = "last_modified_date")
    private Instant lastModifiedDate;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "outcome", "parent", "owner", "interviews", "tags" }, allowSetters = true)
    private Opportunity opportunity;

    @ManyToOne(fetch = FetchType.LAZY)
    private User owner;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "rel_solution__tag",
        joinColumns = @JoinColumn(name = "solution_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = { "team", "opportunities", "solutions" }, allowSetters = true)
    private Set<Tag> tags = new HashSet<>();

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
            ", sortOrder=" + getSortOrder() +
            ", createdDate='" + getCreatedDate() + "'" +
            ", lastModifiedDate='" + getLastModifiedDate() + "'" +
            "}";
    }
}
