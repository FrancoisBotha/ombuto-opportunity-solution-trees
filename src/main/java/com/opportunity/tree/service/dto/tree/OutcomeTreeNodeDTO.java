package com.opportunity.tree.service.dto.tree;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Outcome node in the team tree response (TREE-001).
 */
public class OutcomeTreeNodeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String title;
    private String description;
    private Integer sortOrder;
    private List<OpportunityTreeNodeDTO> opportunities = new ArrayList<>();

    public OutcomeTreeNodeDTO() {}

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

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public List<OpportunityTreeNodeDTO> getOpportunities() {
        return opportunities;
    }

    public void setOpportunities(List<OpportunityTreeNodeDTO> opportunities) {
        this.opportunities = opportunities;
    }
}
