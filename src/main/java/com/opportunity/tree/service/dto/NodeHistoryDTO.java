package com.opportunity.tree.service.dto;

import com.opportunity.tree.domain.enumeration.HistoryEventType;
import com.opportunity.tree.domain.enumeration.TreeNodeType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * A DTO for the {@link com.opportunity.tree.domain.NodeHistory} entity.
 */
@Schema(description = "Append-only per-node changelog, written server-side only.")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class NodeHistoryDTO implements Serializable {

    private Long id;

    @NotNull
    private TreeNodeType nodeType;

    @NotNull
    private Long nodeId;

    @NotNull
    private HistoryEventType eventType;

    @NotNull
    @Size(max = 500)
    private String summary;

    @NotNull
    private Instant createdDate;

    private UserDTO author;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TreeNodeType getNodeType() {
        return nodeType;
    }

    public void setNodeType(TreeNodeType nodeType) {
        this.nodeType = nodeType;
    }

    public Long getNodeId() {
        return nodeId;
    }

    public void setNodeId(Long nodeId) {
        this.nodeId = nodeId;
    }

    public HistoryEventType getEventType() {
        return eventType;
    }

    public void setEventType(HistoryEventType eventType) {
        this.eventType = eventType;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    public UserDTO getAuthor() {
        return author;
    }

    public void setAuthor(UserDTO author) {
        this.author = author;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NodeHistoryDTO)) {
            return false;
        }

        NodeHistoryDTO nodeHistoryDTO = (NodeHistoryDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, nodeHistoryDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "NodeHistoryDTO{" +
            "id=" + getId() +
            ", nodeType='" + getNodeType() + "'" +
            ", nodeId=" + getNodeId() +
            ", eventType='" + getEventType() + "'" +
            ", summary='" + getSummary() + "'" +
            ", createdDate='" + getCreatedDate() + "'" +
            ", author=" + getAuthor() +
            "}";
    }
}
