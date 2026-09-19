package com.opportunity.tree.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * A DTO for the {@link com.opportunity.tree.domain.Comment} entity.
 */
@Schema(description = "Flat, chat-style comment on a tree node: no replies, no threading. Exactly one of the node relationships is set.")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class CommentDTO implements Serializable {

    private Long id;

    @Lob
    private String body;

    @NotNull
    private Instant createdDate;

    private Instant editedDate;

    @NotNull
    private UserDTO author;

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

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
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
        if (!(o instanceof CommentDTO)) {
            return false;
        }

        CommentDTO commentDTO = (CommentDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, commentDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "CommentDTO{" +
            "id=" + getId() +
            ", body='" + getBody() + "'" +
            ", createdDate='" + getCreatedDate() + "'" +
            ", editedDate='" + getEditedDate() + "'" +
            ", author=" + getAuthor() +
            ", outcome=" + getOutcome() +
            ", opportunity=" + getOpportunity() +
            ", solution=" + getSolution() +
            ", assumption=" + getAssumption() +
            ", evidence=" + getEvidence() +
            "}";
    }
}
