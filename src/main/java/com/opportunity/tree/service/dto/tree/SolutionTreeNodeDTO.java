package com.opportunity.tree.service.dto.tree;

import com.opportunity.tree.domain.enumeration.SolutionStatus;
import java.io.Serializable;

/**
 * Solution node in the team tree response (TREE-001). Carries the fields the
 * canvas needs to render and identify a solution.
 */
public class SolutionTreeNodeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String title;
    private String description;
    private SolutionStatus status;
    private Integer sortOrder;

    public SolutionTreeNodeDTO() {}

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

    public SolutionStatus getStatus() {
        return status;
    }

    public void setStatus(SolutionStatus status) {
        this.status = status;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
