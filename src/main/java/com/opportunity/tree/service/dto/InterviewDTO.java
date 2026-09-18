package com.opportunity.tree.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A DTO for the {@link com.opportunity.tree.domain.Interview} entity.
 */
@Schema(description = "Weekly customer interview or other evidence source; opportunities are mined from these.")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class InterviewDTO implements Serializable {

    private Long id;

    @NotNull(message = "must not be null")
    @Size(min = 2, max = 200)
    private String title;

    @Size(max = 200)
    private String participant;

    @NotNull(message = "must not be null")
    private LocalDate interviewDate;

    @Lob
    private String notes;

    @Size(max = 2000)
    private String recordingUrl;

    @NotNull(message = "must not be null")
    private Instant createdDate;

    @NotNull
    private ProductDTO product;

    private UserDTO interviewer;

    private Set<OpportunityDTO> opportunities = new HashSet<>();

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

    public String getParticipant() {
        return participant;
    }

    public void setParticipant(String participant) {
        this.participant = participant;
    }

    public LocalDate getInterviewDate() {
        return interviewDate;
    }

    public void setInterviewDate(LocalDate interviewDate) {
        this.interviewDate = interviewDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getRecordingUrl() {
        return recordingUrl;
    }

    public void setRecordingUrl(String recordingUrl) {
        this.recordingUrl = recordingUrl;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    public ProductDTO getProduct() {
        return product;
    }

    public void setProduct(ProductDTO product) {
        this.product = product;
    }

    public UserDTO getInterviewer() {
        return interviewer;
    }

    public void setInterviewer(UserDTO interviewer) {
        this.interviewer = interviewer;
    }

    public Set<OpportunityDTO> getOpportunities() {
        return opportunities;
    }

    public void setOpportunities(Set<OpportunityDTO> opportunities) {
        this.opportunities = opportunities;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof InterviewDTO)) {
            return false;
        }

        InterviewDTO interviewDTO = (InterviewDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, interviewDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "InterviewDTO{" +
            "id=" + getId() +
            ", title='" + getTitle() + "'" +
            ", participant='" + getParticipant() + "'" +
            ", interviewDate='" + getInterviewDate() + "'" +
            ", notes='" + getNotes() + "'" +
            ", recordingUrl='" + getRecordingUrl() + "'" +
            ", createdDate='" + getCreatedDate() + "'" +
            ", product=" + getProduct() +
            ", interviewer=" + getInterviewer() +
            ", opportunities=" + getOpportunities() +
            "}";
    }
}
