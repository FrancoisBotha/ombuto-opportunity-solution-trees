package com.opportunity.tree.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * A DTO for the {@link com.opportunity.tree.domain.OpenQuestion} entity.
 */
@Schema(description = "Checklist item on an opportunity.")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class OpenQuestionDTO implements Serializable {

    private Long id;

    @NotNull
    @Size(min = 1, max = 500)
    private String questionText;

    @NotNull
    private Boolean done;

    @NotNull
    private Integer sortOrder;

    @NotNull
    private Instant createdDate;

    @NotNull
    private OpportunityDTO opportunity;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public Boolean getDone() {
        return done;
    }

    public void setDone(Boolean done) {
        this.done = done;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    public OpportunityDTO getOpportunity() {
        return opportunity;
    }

    public void setOpportunity(OpportunityDTO opportunity) {
        this.opportunity = opportunity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OpenQuestionDTO)) {
            return false;
        }

        OpenQuestionDTO openQuestionDTO = (OpenQuestionDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, openQuestionDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "OpenQuestionDTO{" +
            "id=" + getId() +
            ", questionText='" + getQuestionText() + "'" +
            ", done='" + getDone() + "'" +
            ", sortOrder=" + getSortOrder() +
            ", createdDate='" + getCreatedDate() + "'" +
            ", opportunity=" + getOpportunity() +
            "}";
    }
}
