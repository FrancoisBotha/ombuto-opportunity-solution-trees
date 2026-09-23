package com.opportunity.tree.service.dto.tree;

import com.opportunity.tree.domain.enumeration.TreeNodeType;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * One node of the flat tree returned by {@code GET /api/teams/{teamId}/tree}.
 *
 * <p>Fields that do not apply to a node's type are {@code null}: {@code status}
 * (opportunity, solution, assumption), {@code confidence} (assumption),
 * {@code priority} and {@code valueRating} (opportunity), {@code ownerLogin}
 * (outcome, opportunity, solution, assumption), {@code archived} and
 * {@code lastActivity} (product), {@code lastModifiedDate} (not product).
 * {@code questions} is only filled for opportunities. {@code title} is the
 * product name, the assumption statement, or the node title otherwise.
 */
public class TreeNodeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String key;
    private TreeNodeType type;
    private Long id;
    private String parentKey;
    private String title;
    private String notes;
    private String status;
    private Integer confidence;
    private Integer priority;
    private Integer valueRating;
    private String ownerLogin;
    private Integer sortOrder;
    private Boolean archived;
    private Instant createdDate;
    private Instant lastModifiedDate;
    private long commentCount;
    private List<TreeNodeLinkDTO> links = new ArrayList<>();
    private List<TreeOpenQuestionDTO> questions = new ArrayList<>();
    /** LABEL-001: only populated for OPPORTUNITY and SOLUTION; empty (never null) for other types. */
    private List<TreeNodeTagDTO> tags = new ArrayList<>();
    private TreeActivityDTO lastActivity;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public TreeNodeType getType() {
        return type;
    }

    public void setType(TreeNodeType type) {
        this.type = type;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getParentKey() {
        return parentKey;
    }

    public void setParentKey(String parentKey) {
        this.parentKey = parentKey;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getConfidence() {
        return confidence;
    }

    public void setConfidence(Integer confidence) {
        this.confidence = confidence;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Integer getValueRating() {
        return valueRating;
    }

    public void setValueRating(Integer valueRating) {
        this.valueRating = valueRating;
    }

    public String getOwnerLogin() {
        return ownerLogin;
    }

    public void setOwnerLogin(String ownerLogin) {
        this.ownerLogin = ownerLogin;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
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

    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(Instant lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public long getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(long commentCount) {
        this.commentCount = commentCount;
    }

    public List<TreeNodeLinkDTO> getLinks() {
        return links;
    }

    public void setLinks(List<TreeNodeLinkDTO> links) {
        this.links = links;
    }

    public List<TreeOpenQuestionDTO> getQuestions() {
        return questions;
    }

    public void setQuestions(List<TreeOpenQuestionDTO> questions) {
        this.questions = questions;
    }

    public List<TreeNodeTagDTO> getTags() {
        return tags;
    }

    public void setTags(List<TreeNodeTagDTO> tags) {
        this.tags = tags == null ? new ArrayList<>() : tags;
    }

    public TreeActivityDTO getLastActivity() {
        return lastActivity;
    }

    public void setLastActivity(TreeActivityDTO lastActivity) {
        this.lastActivity = lastActivity;
    }

    @Override
    public String toString() {
        return "TreeNodeDTO{key='" + key + "', parentKey='" + parentKey + "', title='" + title + "', sortOrder=" + sortOrder + "}";
    }
}
