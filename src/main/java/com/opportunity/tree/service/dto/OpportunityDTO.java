package com.opportunity.tree.service.dto;

import com.opportunity.tree.domain.enumeration.OpportunityStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A DTO for the {@link com.opportunity.tree.domain.Opportunity} entity.
 */
@Schema(description = "Customer need, pain point or desire. Nested via parent to form sub-opportunities.")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class OpportunityDTO implements Serializable {

    private Long id;

    @NotNull
    @Size(min = 2, max = 200)
    private String title;

    @Lob
    private String description;

    @NotNull
    private OpportunityStatus status;

    @NotNull
    @Min(value = 1)
    @Max(value = 5)
    private Integer value;

    @NotNull
    @Min(value = 1)
    @Max(value = 5)
    private Integer complexity;

    @NotNull
    private Integer sortOrder;

    @NotNull
    private Instant createdDate;

    private Instant lastModifiedDate;

    @NotNull
    private OutcomeDTO outcome;

    private OpportunityDTO parent;

    private UserDTO owner;

    private Set<InterviewDTO> interviews = new HashSet<>();

    private Set<TagDTO> tags = new HashSet<>();

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public OpportunityStatus getStatus() {
        return status;
    }

    public void setStatus(OpportunityStatus status) {
        this.status = status;
    }

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }

    public Integer getComplexity() {
        return complexity;
    }

    public void setComplexity(Integer complexity) {
        this.complexity = complexity;
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

    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(Instant lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public OutcomeDTO getOutcome() {
        return outcome;
    }

    public void setOutcome(OutcomeDTO outcome) {
        this.outcome = outcome;
    }

    public OpportunityDTO getParent() {
        return parent;
    }

    public void setParent(OpportunityDTO parent) {
        this.parent = parent;
    }

    public UserDTO getOwner() {
        return owner;
    }

    public void setOwner(UserDTO owner) {
        this.owner = owner;
    }

    public Set<InterviewDTO> getInterviews() {
        return interviews;
    }

    public void setInterviews(Set<InterviewDTO> interviews) {
        this.interviews = interviews;
    }

    public Set<TagDTO> getTags() {
        return tags;
    }

    public void setTags(Set<TagDTO> tags) {
        this.tags = tags;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OpportunityDTO)) {
            return false;
        }

        OpportunityDTO opportunityDTO = (OpportunityDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, opportunityDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "OpportunityDTO{" +
            "id=" + getId() +
            ", title='" + getTitle() + "'" +
            ", description='" + getDescription() + "'" +
            ", status='" + getStatus() + "'" +
            ", value=" + getValue() +
            ", complexity=" + getComplexity() +
            ", sortOrder=" + getSortOrder() +
            ", createdDate='" + getCreatedDate() + "'" +
            ", lastModifiedDate='" + getLastModifiedDate() + "'" +
            ", outcome=" + getOutcome() +
            ", parent=" + getParent() +
            ", owner=" + getOwner() +
            ", interviews=" + getInterviews() +
            ", tags=" + getTags() +
            "}";
    }
}
