package com.opportunity.tree.service.criteria;

import com.opportunity.tree.domain.enumeration.OpportunityStatus;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link com.opportunity.tree.domain.Opportunity} entity. This class is used
 * in {@link com.opportunity.tree.web.rest.OpportunityResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /opportunities?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class OpportunityCriteria implements Serializable, Criteria {

    /**
     * Class for filtering OpportunityStatus
     */
    public static class OpportunityStatusFilter extends Filter<OpportunityStatus> {

        public OpportunityStatusFilter() {}

        public OpportunityStatusFilter(OpportunityStatusFilter filter) {
            super(filter);
        }

        @Override
        public OpportunityStatusFilter copy() {
            return new OpportunityStatusFilter(this);
        }
    }

    @Serial
    private static final long serialVersionUID = 1L;

    private LongFilter id;

    private StringFilter title;

    private OpportunityStatusFilter status;

    private IntegerFilter value;

    private IntegerFilter complexity;

    private IntegerFilter sortOrder;

    private InstantFilter createdDate;

    private InstantFilter lastModifiedDate;

    private LongFilter outcomeId;

    private LongFilter parentId;

    private StringFilter ownerId;

    private LongFilter interviewId;

    private LongFilter tagId;

    private Boolean distinct;

    public OpportunityCriteria() {}

    public OpportunityCriteria(OpportunityCriteria other) {
        this.id = other.optionalId().map(LongFilter::copy).orElse(null);
        this.title = other.optionalTitle().map(StringFilter::copy).orElse(null);
        this.status = other.optionalStatus().map(OpportunityStatusFilter::copy).orElse(null);
        this.value = other.optionalValue().map(IntegerFilter::copy).orElse(null);
        this.complexity = other.optionalComplexity().map(IntegerFilter::copy).orElse(null);
        this.sortOrder = other.optionalSortOrder().map(IntegerFilter::copy).orElse(null);
        this.createdDate = other.optionalCreatedDate().map(InstantFilter::copy).orElse(null);
        this.lastModifiedDate = other.optionalLastModifiedDate().map(InstantFilter::copy).orElse(null);
        this.outcomeId = other.optionalOutcomeId().map(LongFilter::copy).orElse(null);
        this.parentId = other.optionalParentId().map(LongFilter::copy).orElse(null);
        this.ownerId = other.optionalOwnerId().map(StringFilter::copy).orElse(null);
        this.interviewId = other.optionalInterviewId().map(LongFilter::copy).orElse(null);
        this.tagId = other.optionalTagId().map(LongFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public OpportunityCriteria copy() {
        return new OpportunityCriteria(this);
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

    public OpportunityStatusFilter getStatus() {
        return status;
    }

    public Optional<OpportunityStatusFilter> optionalStatus() {
        return Optional.ofNullable(status);
    }

    public OpportunityStatusFilter status() {
        if (status == null) {
            setStatus(new OpportunityStatusFilter());
        }
        return status;
    }

    public void setStatus(OpportunityStatusFilter status) {
        this.status = status;
    }

    public IntegerFilter getValue() {
        return value;
    }

    public Optional<IntegerFilter> optionalValue() {
        return Optional.ofNullable(value);
    }

    public IntegerFilter value() {
        if (value == null) {
            setValue(new IntegerFilter());
        }
        return value;
    }

    public void setValue(IntegerFilter value) {
        this.value = value;
    }

    public IntegerFilter getComplexity() {
        return complexity;
    }

    public Optional<IntegerFilter> optionalComplexity() {
        return Optional.ofNullable(complexity);
    }

    public IntegerFilter complexity() {
        if (complexity == null) {
            setComplexity(new IntegerFilter());
        }
        return complexity;
    }

    public void setComplexity(IntegerFilter complexity) {
        this.complexity = complexity;
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

    public LongFilter getOutcomeId() {
        return outcomeId;
    }

    public Optional<LongFilter> optionalOutcomeId() {
        return Optional.ofNullable(outcomeId);
    }

    public LongFilter outcomeId() {
        if (outcomeId == null) {
            setOutcomeId(new LongFilter());
        }
        return outcomeId;
    }

    public void setOutcomeId(LongFilter outcomeId) {
        this.outcomeId = outcomeId;
    }

    public LongFilter getParentId() {
        return parentId;
    }

    public Optional<LongFilter> optionalParentId() {
        return Optional.ofNullable(parentId);
    }

    public LongFilter parentId() {
        if (parentId == null) {
            setParentId(new LongFilter());
        }
        return parentId;
    }

    public void setParentId(LongFilter parentId) {
        this.parentId = parentId;
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

    public LongFilter getInterviewId() {
        return interviewId;
    }

    public Optional<LongFilter> optionalInterviewId() {
        return Optional.ofNullable(interviewId);
    }

    public LongFilter interviewId() {
        if (interviewId == null) {
            setInterviewId(new LongFilter());
        }
        return interviewId;
    }

    public void setInterviewId(LongFilter interviewId) {
        this.interviewId = interviewId;
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
        final OpportunityCriteria that = (OpportunityCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(title, that.title) &&
            Objects.equals(status, that.status) &&
            Objects.equals(value, that.value) &&
            Objects.equals(complexity, that.complexity) &&
            Objects.equals(sortOrder, that.sortOrder) &&
            Objects.equals(createdDate, that.createdDate) &&
            Objects.equals(lastModifiedDate, that.lastModifiedDate) &&
            Objects.equals(outcomeId, that.outcomeId) &&
            Objects.equals(parentId, that.parentId) &&
            Objects.equals(ownerId, that.ownerId) &&
            Objects.equals(interviewId, that.interviewId) &&
            Objects.equals(tagId, that.tagId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            title,
            status,
            value,
            complexity,
            sortOrder,
            createdDate,
            lastModifiedDate,
            outcomeId,
            parentId,
            ownerId,
            interviewId,
            tagId,
            distinct
        );
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "OpportunityCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalTitle().map(f -> "title=" + f + ", ").orElse("") +
            optionalStatus().map(f -> "status=" + f + ", ").orElse("") +
            optionalValue().map(f -> "value=" + f + ", ").orElse("") +
            optionalComplexity().map(f -> "complexity=" + f + ", ").orElse("") +
            optionalSortOrder().map(f -> "sortOrder=" + f + ", ").orElse("") +
            optionalCreatedDate().map(f -> "createdDate=" + f + ", ").orElse("") +
            optionalLastModifiedDate().map(f -> "lastModifiedDate=" + f + ", ").orElse("") +
            optionalOutcomeId().map(f -> "outcomeId=" + f + ", ").orElse("") +
            optionalParentId().map(f -> "parentId=" + f + ", ").orElse("") +
            optionalOwnerId().map(f -> "ownerId=" + f + ", ").orElse("") +
            optionalInterviewId().map(f -> "interviewId=" + f + ", ").orElse("") +
            optionalTagId().map(f -> "tagId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
