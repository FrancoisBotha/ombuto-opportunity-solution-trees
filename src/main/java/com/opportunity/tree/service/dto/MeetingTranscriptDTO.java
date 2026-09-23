package com.opportunity.tree.service.dto;

import com.opportunity.tree.domain.enumeration.MeetingTranscriptSource;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * A DTO for the {@link com.opportunity.tree.domain.MeetingTranscript} entity.
 */
@Schema(description = "Transcript of a meeting attached to exactly one tree node (Epic 12).")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class MeetingTranscriptDTO implements Serializable {

    private Long id;

    @NotNull
    @Size(min = 2, max = 200)
    private String title;

    @NotNull
    private LocalDate meetingDate;

    @Size(max = 500)
    private String attendees;

    @Lob
    private String body;

    @NotNull
    private MeetingTranscriptSource source;

    @NotNull
    private Instant createdDate;

    private Instant editedDate;

    private UserDTO author;

    private ProductDTO product;

    private OutcomeDTO outcome;

    private OpportunityDTO opportunity;

    private SolutionDTO solution;

    private AssumptionDTO assumption;

    private EvidenceDTO evidence;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDate getMeetingDate() {
        return meetingDate;
    }

    public void setMeetingDate(LocalDate meetingDate) {
        this.meetingDate = meetingDate;
    }

    public String getAttendees() {
        return attendees;
    }

    public void setAttendees(String attendees) {
        this.attendees = attendees;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public MeetingTranscriptSource getSource() {
        return source;
    }

    public void setSource(MeetingTranscriptSource source) {
        this.source = source;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    public Instant getEditedDate() {
        return editedDate;
    }

    public void setEditedDate(Instant editedDate) {
        this.editedDate = editedDate;
    }

    public UserDTO getAuthor() {
        return author;
    }

    public void setAuthor(UserDTO author) {
        this.author = author;
    }

    public ProductDTO getProduct() {
        return product;
    }

    public void setProduct(ProductDTO product) {
        this.product = product;
    }

    public OutcomeDTO getOutcome() {
        return outcome;
    }

    public void setOutcome(OutcomeDTO outcome) {
        this.outcome = outcome;
    }

    public OpportunityDTO getOpportunity() {
        return opportunity;
    }

    public void setOpportunity(OpportunityDTO opportunity) {
        this.opportunity = opportunity;
    }

    public SolutionDTO getSolution() {
        return solution;
    }

    public void setSolution(SolutionDTO solution) {
        this.solution = solution;
    }

    public AssumptionDTO getAssumption() {
        return assumption;
    }

    public void setAssumption(AssumptionDTO assumption) {
        this.assumption = assumption;
    }

    public EvidenceDTO getEvidence() {
        return evidence;
    }

    public void setEvidence(EvidenceDTO evidence) {
        this.evidence = evidence;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MeetingTranscriptDTO)) {
            return false;
        }

        MeetingTranscriptDTO meetingTranscriptDTO = (MeetingTranscriptDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, meetingTranscriptDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "MeetingTranscriptDTO{" +
            "id=" + getId() +
            ", title='" + getTitle() + "'" +
            ", meetingDate='" + getMeetingDate() + "'" +
            ", attendees='" + getAttendees() + "'" +
            ", body='" + getBody() + "'" +
            ", source='" + getSource() + "'" +
            ", createdDate='" + getCreatedDate() + "'" +
            ", editedDate='" + getEditedDate() + "'" +
            ", author=" + getAuthor() +
            ", product=" + getProduct() +
            ", outcome=" + getOutcome() +
            ", opportunity=" + getOpportunity() +
            ", solution=" + getSolution() +
            ", assumption=" + getAssumption() +
            ", evidence=" + getEvidence() +
            "}";
    }
}
