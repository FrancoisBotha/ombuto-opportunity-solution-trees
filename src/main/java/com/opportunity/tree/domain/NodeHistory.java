package com.opportunity.tree.domain;

import com.opportunity.tree.domain.enumeration.HistoryEventType;
import com.opportunity.tree.domain.enumeration.TreeNodeType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Append-only per-node changelog, written server-side only.
 */
@Entity
@Table(name = "node_history")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class NodeHistory implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "node_type", nullable = false)
    private TreeNodeType nodeType;

    @NotNull
    @Column(name = "node_id", nullable = false)
    private Long nodeId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private HistoryEventType eventType;

    @NotNull
    @Size(max = 500)
    @Column(name = "summary", length = 500, nullable = false)
    private String summary;

    @NotNull
    @Column(name = "created_date", nullable = false)
    private Instant createdDate;

    @ManyToOne(fetch = FetchType.LAZY)
    private User author;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public NodeHistory id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TreeNodeType getNodeType() {
        return this.nodeType;
    }

    public NodeHistory nodeType(TreeNodeType nodeType) {
        this.setNodeType(nodeType);
        return this;
    }

    public void setNodeType(TreeNodeType nodeType) {
        this.nodeType = nodeType;
    }

    public Long getNodeId() {
        return this.nodeId;
    }

    public NodeHistory nodeId(Long nodeId) {
        this.setNodeId(nodeId);
        return this;
    }

    public void setNodeId(Long nodeId) {
        this.nodeId = nodeId;
    }

    public HistoryEventType getEventType() {
        return this.eventType;
    }

    public NodeHistory eventType(HistoryEventType eventType) {
        this.setEventType(eventType);
        return this;
    }

    public void setEventType(HistoryEventType eventType) {
        this.eventType = eventType;
    }

    public String getSummary() {
        return this.summary;
    }

    public NodeHistory summary(String summary) {
        this.setSummary(summary);
        return this;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public Instant getCreatedDate() {
        return this.createdDate;
    }

    public NodeHistory createdDate(Instant createdDate) {
        this.setCreatedDate(createdDate);
        return this;
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    public User getAuthor() {
        return this.author;
    }

    public void setAuthor(User user) {
        this.author = user;
    }

    public NodeHistory author(User user) {
        this.setAuthor(user);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NodeHistory)) {
            return false;
        }
        return getId() != null && getId().equals(((NodeHistory) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "NodeHistory{" +
            "id=" + getId() +
            ", nodeType='" + getNodeType() + "'" +
            ", nodeId=" + getNodeId() +
            ", eventType='" + getEventType() + "'" +
            ", summary='" + getSummary() + "'" +
            ", createdDate='" + getCreatedDate() + "'" +
            "}";
    }
}
