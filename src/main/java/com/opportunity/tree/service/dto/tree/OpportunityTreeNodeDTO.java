package com.opportunity.tree.service.dto.tree;

import com.opportunity.tree.domain.enumeration.OpportunityStatus;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Opportunity node in the team tree response (TREE-001). Opportunities nest
 * arbitrarily via {@link #children} and carry their {@link #solutions} inline.
 */
public class OpportunityTreeNodeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String title;
    private String description;
    private OpportunityStatus status;
    private Integer valuerating;
    private Integer sortOrder;
    private Long parentId;
    private List<OpportunityTreeNodeDTO> children = new ArrayList<>();
    private List<SolutionTreeNodeDTO> solutions = new ArrayList<>();

    public OpportunityTreeNodeDTO() {}

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

    public Integer getValuerating() {
        return valuerating;
    }

    public void setValuerating(Integer valuerating) {
        this.valuerating = valuerating;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public List<OpportunityTreeNodeDTO> getChildren() {
        return children;
    }

    public void setChildren(List<OpportunityTreeNodeDTO> children) {
        this.children = children;
    }

    public List<SolutionTreeNodeDTO> getSolutions() {
        return solutions;
    }

    public void setSolutions(List<SolutionTreeNodeDTO> solutions) {
        this.solutions = solutions;
    }
}
