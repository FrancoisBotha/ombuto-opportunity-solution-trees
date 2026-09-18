package com.opportunity.tree.service.criteria;

import com.opportunity.tree.domain.enumeration.SolutionStatus;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link com.opportunity.tree.domain.Solution} entity. This class is used
 * in {@link com.opportunity.tree.web.rest.SolutionResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /solutions?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class SolutionCriteria implements Serializable, Criteria {

    /**
     * Class for filtering SolutionStatus
     */
    public static class SolutionStatusFilter extends Filter<SolutionStatus> {

        public SolutionStatusFilter() {}

        public SolutionStatusFilter(SolutionStatusFilter filter) {
            super(filter);
        }

        @Override
        public SolutionStatusFilter copy() {
            return new SolutionStatusFilter(this);
        }
    }

    @Serial
    private static final long serialVersionUID = 1L;

    private LongFilter id;

    private StringFilter title;

    private SolutionStatusFilter status;

    private IntegerFilter effort;

    private IntegerFilter sortOrder;

    private InstantFilter createdDate;

    private InstantFilter lastModifiedDate;

    private LongFilter opportunityId;

    private StringFilter ownerId;

    private LongFilter tagId;

    private Boolean distinct;

    public SolutionCriteria() {}

    public SolutionCriteria(SolutionCriteria other) {
        this.id = other.optionalId().map(LongFilter::copy).orElse(null);
        this.title = other.optionalTitle().map(StringFilter::copy).orElse(null);
        this.status = other.optionalStatus().map(SolutionStatusFilter::copy).orElse(null);
        this.effort = other.optionalEffort().map(IntegerFilter::copy).orElse(null);
        this.sortOrder = other.optionalSortOrder().map(IntegerFilter::copy).orElse(null);
        this.createdDate = other.optionalCreatedDate().map(InstantFilter::copy).orElse(null);
        this.lastModifiedDate = other.optionalLastModifiedDate().map(InstantFilter::copy).orElse(null);
        this.opportunityId = other.optionalOpportunityId().map(LongFilter::copy).orElse(null);
        this.ownerId = other.optionalOwnerId().map(StringFilter::copy).orElse(null);
        this.tagId = other.optionalTagId().map(LongFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public SolutionCriteria copy() {
        return new SolutionCriteria(this);
    }

    public LongFilter getId() {
        return id;
    }

    public Optional<LongFilter> optionalId() {
        return Optional.ofNullable(id);
    }

    public LongFilter id() {
        if (id == null) {
            setId(new LongFilter());
        }
        return id;
    }

    public void setId(LongFilter id) {
        this.id = id;
    }

    public StringFilter getTitle() {
        return title;
    }

    public Optional<StringFilter> optionalTitle() {
        return Optional.ofNullable(title);
    }

    public StringFilter title() {
        if (title == null) {
            setTitle(new StringFilter());
        }
        return title;
    }

    public void setTitle(StringFilter title) {
        this.title = title;
    }

    public SolutionStatusFilter getStatus() {
        return status;
    }

    public Optional<SolutionStatusFilter> optionalStatus() {
        return Optional.ofNullable(status);
    }

    public SolutionStatusFilter status() {
        if (status == null) {
            setStatus(new SolutionStatusFilter());
        }
        return status;
    }

    public void setStatus(SolutionStatusFilter status) {
        this.status = status;
    }

    public IntegerFilter getEffort() {
        return effort;
    }

    public Optional<IntegerFilter> optionalEffort() {
        return Optional.ofNullable(effort);
    }

    public IntegerFilter effort() {
        if (effort == null) {
            setEffort(new IntegerFilter());
        }
        return effort;
    }

    public void setEffort(IntegerFilter effort) {
        this.effort = effort;
    }

    public IntegerFilter getSortOrder() {
        return sortOrder;
    }

    public Optional<IntegerFilter> optionalSortOrder() {
        return Optional.ofNullable(sortOrder);
    }

    public IntegerFilter sortOrder() {
        if (sortOrder == null) {
            setSortOrder(new IntegerFilter());
        }
        return sortOrder;
    }

    public void setSortOrder(IntegerFilter sortOrder) {
        this.sortOrder = sortOrder;
    }

    public InstantFilter getCreatedDate() {
        return createdDate;
    }

    public Optional<InstantFilter> optionalCreatedDate() {
        return Optional.ofNullable(createdDate);
    }

    public InstantFilter createdDate() {
        if (createdDate == null) {
            setCreatedDate(new InstantFilter());
        }
        return createdDate;
    }

    public void setCreatedDate(InstantFilter createdDate) {
        this.createdDate = createdDate;
    }

    public InstantFilter getLastModifiedDate() {
        return lastModifiedDate;
    }

    public Optional<InstantFilter> optionalLastModifiedDate() {
        return Optional.ofNullable(lastModifiedDate);
    }

    public InstantFilter lastModifiedDate() {
        if (lastModifiedDate == null) {
            setLastModifiedDate(new InstantFilter());
        }
        return lastModifiedDate;
    }

    public void setLastModifiedDate(InstantFilter lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public LongFilter getOpportunityId() {
        return opportunityId;
    }

    public Optional<LongFilter> optionalOpportunityId() {
        return Optional.ofNullable(opportunityId);
    }

    public LongFilter opportunityId() {
        if (opportunityId == null) {
            setOpportunityId(new LongFilter());
        }
        return opportunityId;
    }

    public void setOpportunityId(LongFilter opportunityId) {
        this.opportunityId = opportunityId;
    }

    public StringFilter getOwnerId() {
        return ownerId;
    }

    public Optional<StringFilter> optionalOwnerId() {
        return Optional.ofNullable(ownerId);
    }

    public StringFilter ownerId() {
        if (ownerId == null) {
            setOwnerId(new StringFilter());
        }
        return ownerId;
    }

    public void setOwnerId(StringFilter ownerId) {
        this.ownerId = ownerId;
    }

    public LongFilter getTagId() {
        return tagId;
    }

    public Optional<LongFilter> optionalTagId() {
        return Optional.ofNullable(tagId);
    }

    public LongFilter tagId() {
        if (tagId == null) {
            setTagId(new LongFilter());
        }
        return tagId;
    }

    public void setTagId(LongFilter tagId) {
        this.tagId = tagId;
    }

    public Boolean getDistinct() {
        return distinct;
    }

    public Optional<Boolean> optionalDistinct() {
        return Optional.ofNullable(distinct);
    }

    public Boolean distinct() {
        if (distinct == null) {
            setDistinct(true);
        }
        return distinct;
    }

    public void setDistinct(Boolean distinct) {
        this.distinct = distinct;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final SolutionCriteria that = (SolutionCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(title, that.title) &&
            Objects.equals(status, that.status) &&
            Objects.equals(effort, that.effort) &&
            Objects.equals(sortOrder, that.sortOrder) &&
            Objects.equals(createdDate, that.createdDate) &&
            Objects.equals(lastModifiedDate, that.lastModifiedDate) &&
            Objects.equals(opportunityId, that.opportunityId) &&
            Objects.equals(ownerId, that.ownerId) &&
            Objects.equals(tagId, that.tagId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, status, effort, sortOrder, createdDate, lastModifiedDate, opportunityId, ownerId, tagId, distinct);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "SolutionCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalTitle().map(f -> "title=" + f + ", ").orElse("") +
            optionalStatus().map(f -> "status=" + f + ", ").orElse("") +
            optionalEffort().map(f -> "effort=" + f + ", ").orElse("") +
            optionalSortOrder().map(f -> "sortOrder=" + f + ", ").orElse("") +
            optionalCreatedDate().map(f -> "createdDate=" + f + ", ").orElse("") +
            optionalLastModifiedDate().map(f -> "lastModifiedDate=" + f + ", ").orElse("") +
            optionalOpportunityId().map(f -> "opportunityId=" + f + ", ").orElse("") +
            optionalOwnerId().map(f -> "ownerId=" + f + ", ").orElse("") +
            optionalTagId().map(f -> "tagId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
