package com.opportunity.tree.service.dto.tree;

import com.opportunity.tree.domain.enumeration.OutcomeStatus;
import java.io.Serializable;
import java.time.LocalDate;
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
    private String metric;
    private String targetValue;
    private String currentValue;
    private OutcomeStatus status;
    private LocalDate startDate;
    private LocalDate targetDate;
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

    public String getMetric() {
        return metric;
    }

    public void setMetric(String metric) {
        this.metric = metric;
    }

    public String getTargetValue() {
        return targetValue;
    }

    public void setTargetValue(String targetValue) {
        this.targetValue = targetValue;
    }

    public String getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(String currentValue) {
        this.currentValue = currentValue;
    }

    public OutcomeStatus getStatus() {
        return status;
    }

    public void setStatus(OutcomeStatus status) {
        this.status = status;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getTargetDate() {
        return targetDate;
    }

    public void setTargetDate(LocalDate targetDate) {
        this.targetDate = targetDate;
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
