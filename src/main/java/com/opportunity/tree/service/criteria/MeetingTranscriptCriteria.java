package com.opportunity.tree.service.criteria;

import com.opportunity.tree.domain.enumeration.MeetingTranscriptSource;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link com.opportunity.tree.domain.MeetingTranscript} entity. This class is used
 * in {@link com.opportunity.tree.web.rest.MeetingTranscriptResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /meeting-transcripts?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class MeetingTranscriptCriteria implements Serializable, Criteria {

    /**
     * Class for filtering MeetingTranscriptSource
     */
    public static class MeetingTranscriptSourceFilter extends Filter<MeetingTranscriptSource> {

        public MeetingTranscriptSourceFilter() {}

        public MeetingTranscriptSourceFilter(MeetingTranscriptSourceFilter filter) {
            super(filter);
        }

        @Override
        public MeetingTranscriptSourceFilter copy() {
            return new MeetingTranscriptSourceFilter(this);
        }
    }

    @Serial
    private static final long serialVersionUID = 1L;

    private LongFilter id;

    private StringFilter title;

    private LocalDateFilter meetingDate;

    private StringFilter attendees;

    private MeetingTranscriptSourceFilter source;

    private InstantFilter createdDate;

    private InstantFilter editedDate;

    private StringFilter authorId;

    private LongFilter productId;

    private LongFilter outcomeId;

    private LongFilter opportunityId;

    private LongFilter solutionId;

    private LongFilter assumptionId;

    private LongFilter evidenceId;

    private Boolean distinct;

    public MeetingTranscriptCriteria() {}

    public MeetingTranscriptCriteria(MeetingTranscriptCriteria other) {
        this.id = other.optionalId().map(LongFilter::copy).orElse(null);
        this.title = other.optionalTitle().map(StringFilter::copy).orElse(null);
        this.meetingDate = other.optionalMeetingDate().map(LocalDateFilter::copy).orElse(null);
        this.attendees = other.optionalAttendees().map(StringFilter::copy).orElse(null);
        this.source = other.optionalSource().map(MeetingTranscriptSourceFilter::copy).orElse(null);
        this.createdDate = other.optionalCreatedDate().map(InstantFilter::copy).orElse(null);
        this.editedDate = other.optionalEditedDate().map(InstantFilter::copy).orElse(null);
        this.authorId = other.optionalAuthorId().map(StringFilter::copy).orElse(null);
        this.productId = other.optionalProductId().map(LongFilter::copy).orElse(null);
        this.outcomeId = other.optionalOutcomeId().map(LongFilter::copy).orElse(null);
        this.opportunityId = other.optionalOpportunityId().map(LongFilter::copy).orElse(null);
        this.solutionId = other.optionalSolutionId().map(LongFilter::copy).orElse(null);
        this.assumptionId = other.optionalAssumptionId().map(LongFilter::copy).orElse(null);
        this.evidenceId = other.optionalEvidenceId().map(LongFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public MeetingTranscriptCriteria copy() {
        return new MeetingTranscriptCriteria(this);
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

    public LocalDateFilter getMeetingDate() {
        return meetingDate;
    }

    public Optional<LocalDateFilter> optionalMeetingDate() {
        return Optional.ofNullable(meetingDate);
    }

    public LocalDateFilter meetingDate() {
        if (meetingDate == null) {
            setMeetingDate(new LocalDateFilter());
        }
        return meetingDate;
    }

    public void setMeetingDate(LocalDateFilter meetingDate) {
        this.meetingDate = meetingDate;
    }

    public StringFilter getAttendees() {
        return attendees;
    }

    public Optional<StringFilter> optionalAttendees() {
        return Optional.ofNullable(attendees);
    }

    public StringFilter attendees() {
        if (attendees == null) {
            setAttendees(new StringFilter());
        }
        return attendees;
    }

    public void setAttendees(StringFilter attendees) {
        this.attendees = attendees;
    }

    public MeetingTranscriptSourceFilter getSource() {
        return source;
    }

    public Optional<MeetingTranscriptSourceFilter> optionalSource() {
        return Optional.ofNullable(source);
    }

    public MeetingTranscriptSourceFilter source() {
        if (source == null) {
            setSource(new MeetingTranscriptSourceFilter());
        }
        return source;
    }

    public void setSource(MeetingTranscriptSourceFilter source) {
        this.source = source;
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

    public InstantFilter getEditedDate() {
        return editedDate;
    }

    public Optional<InstantFilter> optionalEditedDate() {
        return Optional.ofNullable(editedDate);
    }

    public InstantFilter editedDate() {
        if (editedDate == null) {
            setEditedDate(new InstantFilter());
        }
        return editedDate;
    }

    public void setEditedDate(InstantFilter editedDate) {
        this.editedDate = editedDate;
    }

    public StringFilter getAuthorId() {
        return authorId;
    }

    public Optional<StringFilter> optionalAuthorId() {
        return Optional.ofNullable(authorId);
    }

    public StringFilter authorId() {
        if (authorId == null) {
            setAuthorId(new StringFilter());
        }
        return authorId;
    }

    public void setAuthorId(StringFilter authorId) {
        this.authorId = authorId;
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

    public LongFilter getSolutionId() {
        return solutionId;
    }

    public Optional<LongFilter> optionalSolutionId() {
        return Optional.ofNullable(solutionId);
    }

    public LongFilter solutionId() {
        if (solutionId == null) {
            setSolutionId(new LongFilter());
        }
        return solutionId;
    }

    public void setSolutionId(LongFilter solutionId) {
        this.solutionId = solutionId;
    }

    public LongFilter getAssumptionId() {
        return assumptionId;
    }

    public Optional<LongFilter> optionalAssumptionId() {
        return Optional.ofNullable(assumptionId);
    }

    public LongFilter assumptionId() {
        if (assumptionId == null) {
            setAssumptionId(new LongFilter());
        }
        return assumptionId;
    }

    public void setAssumptionId(LongFilter assumptionId) {
        this.assumptionId = assumptionId;
    }

    public LongFilter getEvidenceId() {
        return evidenceId;
    }

    public Optional<LongFilter> optionalEvidenceId() {
        return Optional.ofNullable(evidenceId);
    }

    public LongFilter evidenceId() {
        if (evidenceId == null) {
            setEvidenceId(new LongFilter());
        }
        return evidenceId;
    }

    public void setEvidenceId(LongFilter evidenceId) {
        this.evidenceId = evidenceId;
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
        final MeetingTranscriptCriteria that = (MeetingTranscriptCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(title, that.title) &&
            Objects.equals(meetingDate, that.meetingDate) &&
            Objects.equals(attendees, that.attendees) &&
            Objects.equals(source, that.source) &&
            Objects.equals(createdDate, that.createdDate) &&
            Objects.equals(editedDate, that.editedDate) &&
            Objects.equals(authorId, that.authorId) &&
            Objects.equals(productId, that.productId) &&
            Objects.equals(outcomeId, that.outcomeId) &&
            Objects.equals(opportunityId, that.opportunityId) &&
            Objects.equals(solutionId, that.solutionId) &&
            Objects.equals(assumptionId, that.assumptionId) &&
            Objects.equals(evidenceId, that.evidenceId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            title,
            meetingDate,
            attendees,
            source,
            createdDate,
            editedDate,
            authorId,
            productId,
            outcomeId,
            opportunityId,
            solutionId,
            assumptionId,
            evidenceId,
            distinct
        );
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "MeetingTranscriptCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalTitle().map(f -> "title=" + f + ", ").orElse("") +
            optionalMeetingDate().map(f -> "meetingDate=" + f + ", ").orElse("") +
            optionalAttendees().map(f -> "attendees=" + f + ", ").orElse("") +
            optionalSource().map(f -> "source=" + f + ", ").orElse("") +
            optionalCreatedDate().map(f -> "createdDate=" + f + ", ").orElse("") +
            optionalEditedDate().map(f -> "editedDate=" + f + ", ").orElse("") +
            optionalAuthorId().map(f -> "authorId=" + f + ", ").orElse("") +
            optionalProductId().map(f -> "productId=" + f + ", ").orElse("") +
            optionalOutcomeId().map(f -> "outcomeId=" + f + ", ").orElse("") +
            optionalOpportunityId().map(f -> "opportunityId=" + f + ", ").orElse("") +
            optionalSolutionId().map(f -> "solutionId=" + f + ", ").orElse("") +
            optionalAssumptionId().map(f -> "assumptionId=" + f + ", ").orElse("") +
            optionalEvidenceId().map(f -> "evidenceId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
