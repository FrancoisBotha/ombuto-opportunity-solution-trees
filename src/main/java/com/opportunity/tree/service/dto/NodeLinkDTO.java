package com.opportunity.tree.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * A DTO for the {@link com.opportunity.tree.domain.NodeLink} entity.
 */
@Schema(description = "Name + URL link on any tree node. Exactly one node relationship is set.")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class NodeLinkDTO implements Serializable {

    private Long id;

    @NotNull
    @Size(min = 1, max = 100)
    private String name;

    @NotNull
    @Size(max = 2000)
    @Pattern(regexp = "^https?:\\/\\/.+")
    private String url;

    @NotNull
    private Integer sortOrder;

    @NotNull
    private Instant createdDate;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
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
        if (!(o instanceof NodeLinkDTO)) {
            return false;
        }

        NodeLinkDTO nodeLinkDTO = (NodeLinkDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, nodeLinkDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "NodeLinkDTO{" +
            "id=" + getId() +
            ", name='" + getName() + "'" +
            ", url='" + getUrl() + "'" +
            ", sortOrder=" + getSortOrder() +
            ", createdDate='" + getCreatedDate() + "'" +
            ", product=" + getProduct() +
            ", outcome=" + getOutcome() +
            ", opportunity=" + getOpportunity() +
            ", solution=" + getSolution() +
            ", assumption=" + getAssumption() +
            ", evidence=" + getEvidence() +
            "}";
    }
}
