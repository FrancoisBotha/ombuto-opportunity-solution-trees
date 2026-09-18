package com.opportunity.tree.service.criteria;

import com.opportunity.tree.domain.enumeration.OutcomeStatus;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link com.opportunity.tree.domain.Outcome} entity. This class is used
 * in {@link com.opportunity.tree.web.rest.OutcomeResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /outcomes?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class OutcomeCriteria implements Serializable, Criteria {

    /**
     * Class for filtering OutcomeStatus
     */
    public static class OutcomeStatusFilter extends Filter<OutcomeStatus> {

        public OutcomeStatusFilter() {}

        public OutcomeStatusFilter(OutcomeStatusFilter filter) {
            super(filter);
        }

        @Override
        public OutcomeStatusFilter copy() {
            return new OutcomeStatusFilter(this);
        }
    }

    @Serial
    private static final long serialVersionUID = 1L;

    private LongFilter id;

    private StringFilter title;

    private StringFilter metric;

    private StringFilter targetValue;

    private StringFilter currentValue;

    private OutcomeStatusFilter status;

    private LocalDateFilter startDate;

    private LocalDateFilter targetDate;

    private IntegerFilter sortOrder;

    private InstantFilter createdDate;

    private InstantFilter lastModifiedDate;

    private LongFilter productId;

    private StringFilter ownerId;

    private Boolean distinct;

    public OutcomeCriteria() {}

    public OutcomeCriteria(OutcomeCriteria other) {
        this.id = other.optionalId().map(LongFilter::copy).orElse(null);
        this.title = other.optionalTitle().map(StringFilter::copy).orElse(null);
        this.metric = other.optionalMetric().map(StringFilter::copy).orElse(null);
        this.targetValue = other.optionalTargetValue().map(StringFilter::copy).orElse(null);
        this.currentValue = other.optionalCurrentValue().map(StringFilter::copy).orElse(null);
        this.status = other.optionalStatus().map(OutcomeStatusFilter::copy).orElse(null);
        this.startDate = other.optionalStartDate().map(LocalDateFilter::copy).orElse(null);
        this.targetDate = other.optionalTargetDate().map(LocalDateFilter::copy).orElse(null);
        this.sortOrder = other.optionalSortOrder().map(IntegerFilter::copy).orElse(null);
        this.createdDate = other.optionalCreatedDate().map(InstantFilter::copy).orElse(null);
        this.lastModifiedDate = other.optionalLastModifiedDate().map(InstantFilter::copy).orElse(null);
        this.productId = other.optionalProductId().map(LongFilter::copy).orElse(null);
        this.ownerId = other.optionalOwnerId().map(StringFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public OutcomeCriteria copy() {
        return new OutcomeCriteria(this);
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

    public StringFilter getMetric() {
        return metric;
    }

    public Optional<StringFilter> optionalMetric() {
        return Optional.ofNullable(metric);
    }

    public StringFilter metric() {
        if (metric == null) {
            setMetric(new StringFilter());
        }
        return metric;
    }

    public void setMetric(StringFilter metric) {
        this.metric = metric;
    }

    public StringFilter getTargetValue() {
        return targetValue;
    }

    public Optional<StringFilter> optionalTargetValue() {
        return Optional.ofNullable(targetValue);
    }

    public StringFilter targetValue() {
        if (targetValue == null) {
            setTargetValue(new StringFilter());
        }
        return targetValue;
    }

    public void setTargetValue(StringFilter targetValue) {
        this.targetValue = targetValue;
    }

    public StringFilter getCurrentValue() {
        return currentValue;
    }

    public Optional<StringFilter> optionalCurrentValue() {
        return Optional.ofNullable(currentValue);
    }

    public StringFilter currentValue() {
        if (currentValue == null) {
            setCurrentValue(new StringFilter());
        }
        return currentValue;
    }

    public void setCurrentValue(StringFilter currentValue) {
        this.currentValue = currentValue;
    }

    public OutcomeStatusFilter getStatus() {
        return status;
    }

    public Optional<OutcomeStatusFilter> optionalStatus() {
        return Optional.ofNullable(status);
    }

    public OutcomeStatusFilter status() {
        if (status == null) {
            setStatus(new OutcomeStatusFilter());
        }
        return status;
    }

    public void setStatus(OutcomeStatusFilter status) {
        this.status = status;
    }

    public LocalDateFilter getStartDate() {
        return startDate;
    }

    public Optional<LocalDateFilter> optionalStartDate() {
        return Optional.ofNullable(startDate);
    }

    public LocalDateFilter startDate() {
        if (startDate == null) {
            setStartDate(new LocalDateFilter());
        }
        return startDate;
    }

    public void setStartDate(LocalDateFilter startDate) {
        this.startDate = startDate;
    }

    public LocalDateFilter getTargetDate() {
        return targetDate;
    }

    public Optional<LocalDateFilter> optionalTargetDate() {
        return Optional.ofNullable(targetDate);
    }

    public LocalDateFilter targetDate() {
        if (targetDate == null) {
            setTargetDate(new LocalDateFilter());
        }
        return targetDate;
    }

    public void setTargetDate(LocalDateFilter targetDate) {
        this.targetDate = targetDate;
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

    public LongFilter getProductId() {
        return productId;
    }

    public Optional<LongFilter> optionalProductId() {
        return Optional.ofNullable(productId);
    }

    public LongFilter productId() {
        if (productId == null) {
            setProductId(new LongFilter());
        }
        return productId;
    }

    public void setProductId(LongFilter productId) {
        this.productId = productId;
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
        final OutcomeCriteria that = (OutcomeCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(title, that.title) &&
            Objects.equals(metric, that.metric) &&
            Objects.equals(targetValue, that.targetValue) &&
            Objects.equals(currentValue, that.currentValue) &&
            Objects.equals(status, that.status) &&
            Objects.equals(startDate, that.startDate) &&
            Objects.equals(targetDate, that.targetDate) &&
            Objects.equals(sortOrder, that.sortOrder) &&
            Objects.equals(createdDate, that.createdDate) &&
            Objects.equals(lastModifiedDate, that.lastModifiedDate) &&
            Objects.equals(productId, that.productId) &&
            Objects.equals(ownerId, that.ownerId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            title,
            metric,
            targetValue,
            currentValue,
            status,
            startDate,
            targetDate,
            sortOrder,
            createdDate,
            lastModifiedDate,
            productId,
            ownerId,
            distinct
        );
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "OutcomeCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalTitle().map(f -> "title=" + f + ", ").orElse("") +
            optionalMetric().map(f -> "metric=" + f + ", ").orElse("") +
            optionalTargetValue().map(f -> "targetValue=" + f + ", ").orElse("") +
            optionalCurrentValue().map(f -> "currentValue=" + f + ", ").orElse("") +
            optionalStatus().map(f -> "status=" + f + ", ").orElse("") +
            optionalStartDate().map(f -> "startDate=" + f + ", ").orElse("") +
            optionalTargetDate().map(f -> "targetDate=" + f + ", ").orElse("") +
            optionalSortOrder().map(f -> "sortOrder=" + f + ", ").orElse("") +
            optionalCreatedDate().map(f -> "createdDate=" + f + ", ").orElse("") +
            optionalLastModifiedDate().map(f -> "lastModifiedDate=" + f + ", ").orElse("") +
            optionalProductId().map(f -> "productId=" + f + ", ").orElse("") +
            optionalOwnerId().map(f -> "ownerId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
