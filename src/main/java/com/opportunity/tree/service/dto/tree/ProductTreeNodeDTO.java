package com.opportunity.tree.service.dto.tree;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Product node in the team tree response (TREE-001). A product forms a
 * top-level branch of the team's tree.
 */
public class ProductTreeNodeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String description;
    private String vision;
    private Boolean archived;
    private Instant createdDate;
    private List<OutcomeTreeNodeDTO> outcomes = new ArrayList<>();

    public ProductTreeNodeDTO() {}

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVision() {
        return vision;
    }

    public void setVision(String vision) {
        this.vision = vision;
    }

    public Boolean getArchived() {
        return archived;
    }

    public void setArchived(Boolean archived) {
        this.archived = archived;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    public List<OutcomeTreeNodeDTO> getOutcomes() {
        return outcomes;
    }

    public void setOutcomes(List<OutcomeTreeNodeDTO> outcomes) {
        this.outcomes = outcomes;
    }
}
