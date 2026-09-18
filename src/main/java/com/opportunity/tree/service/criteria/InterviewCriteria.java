package com.opportunity.tree.service.criteria;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link com.opportunity.tree.domain.Interview} entity. This class is used
 * in {@link com.opportunity.tree.web.rest.InterviewResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /interviews?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class InterviewCriteria implements Serializable, Criteria {

    @Serial
    private static final long serialVersionUID = 1L;

    private LongFilter id;

    private StringFilter title;

    private StringFilter participant;

    private LocalDateFilter interviewDate;

    private StringFilter recordingUrl;

    private InstantFilter createdDate;

    private LongFilter productId;

    private StringFilter interviewerId;

    private LongFilter opportunityId;

    private Boolean distinct;

    public InterviewCriteria() {}

    public InterviewCriteria(InterviewCriteria other) {
        this.id = other.optionalId().map(LongFilter::copy).orElse(null);
        this.title = other.optionalTitle().map(StringFilter::copy).orElse(null);
        this.participant = other.optionalParticipant().map(StringFilter::copy).orElse(null);
        this.interviewDate = other.optionalInterviewDate().map(LocalDateFilter::copy).orElse(null);
        this.recordingUrl = other.optionalRecordingUrl().map(StringFilter::copy).orElse(null);
        this.createdDate = other.optionalCreatedDate().map(InstantFilter::copy).orElse(null);
        this.productId = other.optionalProductId().map(LongFilter::copy).orElse(null);
        this.interviewerId = other.optionalInterviewerId().map(StringFilter::copy).orElse(null);
        this.opportunityId = other.optionalOpportunityId().map(LongFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public InterviewCriteria copy() {
        return new InterviewCriteria(this);
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

    public StringFilter getParticipant() {
        return participant;
    }

    public Optional<StringFilter> optionalParticipant() {
        return Optional.ofNullable(participant);
    }

    public StringFilter participant() {
        if (participant == null) {
            setParticipant(new StringFilter());
        }
        return participant;
    }

    public void setParticipant(StringFilter participant) {
        this.participant = participant;
    }

    public LocalDateFilter getInterviewDate() {
        return interviewDate;
    }

    public Optional<LocalDateFilter> optionalInterviewDate() {
        return Optional.ofNullable(interviewDate);
    }

    public LocalDateFilter interviewDate() {
        if (interviewDate == null) {
            setInterviewDate(new LocalDateFilter());
        }
        return interviewDate;
    }

    public void setInterviewDate(LocalDateFilter interviewDate) {
        this.interviewDate = interviewDate;
    }

    public StringFilter getRecordingUrl() {
        return recordingUrl;
    }

    public Optional<StringFilter> optionalRecordingUrl() {
        return Optional.ofNullable(recordingUrl);
    }

    public StringFilter recordingUrl() {
        if (recordingUrl == null) {
            setRecordingUrl(new StringFilter());
        }
        return recordingUrl;
    }

    public void setRecordingUrl(StringFilter recordingUrl) {
        this.recordingUrl = recordingUrl;
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

    public StringFilter getInterviewerId() {
        return interviewerId;
    }

    public Optional<StringFilter> optionalInterviewerId() {
        return Optional.ofNullable(interviewerId);
    }

    public StringFilter interviewerId() {
        if (interviewerId == null) {
            setInterviewerId(new StringFilter());
        }
        return interviewerId;
    }

    public void setInterviewerId(StringFilter interviewerId) {
        this.interviewerId = interviewerId;
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
        final InterviewCriteria that = (InterviewCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(title, that.title) &&
            Objects.equals(participant, that.participant) &&
            Objects.equals(interviewDate, that.interviewDate) &&
            Objects.equals(recordingUrl, that.recordingUrl) &&
            Objects.equals(createdDate, that.createdDate) &&
            Objects.equals(productId, that.productId) &&
            Objects.equals(interviewerId, that.interviewerId) &&
            Objects.equals(opportunityId, that.opportunityId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            title,
            participant,
            interviewDate,
            recordingUrl,
            createdDate,
            productId,
            interviewerId,
            opportunityId,
            distinct
        );
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "InterviewCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalTitle().map(f -> "title=" + f + ", ").orElse("") +
            optionalParticipant().map(f -> "participant=" + f + ", ").orElse("") +
            optionalInterviewDate().map(f -> "interviewDate=" + f + ", ").orElse("") +
            optionalRecordingUrl().map(f -> "recordingUrl=" + f + ", ").orElse("") +
            optionalCreatedDate().map(f -> "createdDate=" + f + ", ").orElse("") +
            optionalProductId().map(f -> "productId=" + f + ", ").orElse("") +
            optionalInterviewerId().map(f -> "interviewerId=" + f + ", ").orElse("") +
            optionalOpportunityId().map(f -> "opportunityId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
