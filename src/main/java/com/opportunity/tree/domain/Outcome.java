package com.opportunity.tree.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.opportunity.tree.domain.enumeration.OutcomeStatus;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Desired outcome at the top of the tree, e.g. \"Increase weekly active users\".
 */
@Table("outcome")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Outcome implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @Column("id")
    private Long id;

    @NotNull(message = "must not be null")
    @Size(min = 2, max = 200)
    @Column("title")
    private String title;

    @Column("description")
    private String description;

    @Size(max = 200)
    @Column("metric")
    private String metric;

    @Size(max = 100)
    @Column("target_value")
    private String targetValue;

    @Size(max = 100)
    @Column("current_value")
    private String currentValue;

    @NotNull(message = "must not be null")
    @Column("status")
    private OutcomeStatus status;

    @Column("start_date")
    private LocalDate startDate;

    @Column("target_date")
    private LocalDate targetDate;

    @NotNull(message = "must not be null")
    @Column("sort_order")
    private Integer sortOrder;

    @NotNull(message = "must not be null")
    @Column("created_date")
    private Instant createdDate;

    @Column("last_modified_date")
    private Instant lastModifiedDate;

    @org.springframework.data.annotation.Transient
    @JsonIgnoreProperties(value = { "team" }, allowSetters = true)
    private Product product;

    @org.springframework.data.annotation.Transient
    private User owner;

    @Column("product_id")
    private Long productId;

    @Column("owner_id")
    private String ownerId;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public Outcome id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return this.title;
    }

    public Outcome title(String title) {
        this.setTitle(title);
        return this;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return this.description;
    }

    public Outcome description(String description) {
        this.setDescription(description);
        return this;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMetric() {
        return this.metric;
    }

    public Outcome metric(String metric) {
        this.setMetric(metric);
        return this;
    }

    public void setMetric(String metric) {
        this.metric = metric;
    }

    public String getTargetValue() {
        return this.targetValue;
    }

    public Outcome targetValue(String targetValue) {
        this.setTargetValue(targetValue);
        return this;
    }

    public void setTargetValue(String targetValue) {
        this.targetValue = targetValue;
    }

    public String getCurrentValue() {
        return this.currentValue;
    }

    public Outcome currentValue(String currentValue) {
        this.setCurrentValue(currentValue);
        return this;
    }

    public void setCurrentValue(String currentValue) {
        this.currentValue = currentValue;
    }

    public OutcomeStatus getStatus() {
        return this.status;
    }

    public Outcome status(OutcomeStatus status) {
        this.setStatus(status);
        return this;
    }

    public void setStatus(OutcomeStatus status) {
        this.status = status;
    }

    public LocalDate getStartDate() {
        return this.startDate;
    }

    public Outcome startDate(LocalDate startDate) {
        this.setStartDate(startDate);
        return this;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getTargetDate() {
        return this.targetDate;
    }

    public Outcome targetDate(LocalDate targetDate) {
        this.setTargetDate(targetDate);
        return this;
    }

    public void setTargetDate(LocalDate targetDate) {
        this.targetDate = targetDate;
    }

    public Integer getSortOrder() {
        return this.sortOrder;
    }

    public Outcome sortOrder(Integer sortOrder) {
        this.setSortOrder(sortOrder);
        return this;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Instant getCreatedDate() {
        return this.createdDate;
    }

    public Outcome createdDate(Instant createdDate) {
        this.setCreatedDate(createdDate);
        return this;
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    public Instant getLastModifiedDate() {
        return this.lastModifiedDate;
    }

    public Outcome lastModifiedDate(Instant lastModifiedDate) {
        this.setLastModifiedDate(lastModifiedDate);
        return this;
    }

    public void setLastModifiedDate(Instant lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public Product getProduct() {
        return this.product;
    }

    public void setProduct(Product product) {
        this.product = product;
        this.productId = product != null ? product.getId() : null;
    }

    public Outcome product(Product product) {
        this.setProduct(product);
        return this;
    }

    public User getOwner() {
        return this.owner;
    }

    public void setOwner(User user) {
        this.owner = user;
        this.ownerId = user != null ? user.getId() : null;
    }

    public Outcome owner(User user) {
        this.setOwner(user);
        return this;
    }

    public Long getProductId() {
        return this.productId;
    }

    public void setProductId(Long product) {
        this.productId = product;
    }

    public String getOwnerId() {
        return this.ownerId;
    }

    public void setOwnerId(String user) {
        this.ownerId = user;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Outcome)) {
            return false;
        }
        return getId() != null && getId().equals(((Outcome) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Outcome{" +
            "id=" + getId() +
            ", title='" + getTitle() + "'" +
            ", description='" + getDescription() + "'" +
            ", metric='" + getMetric() + "'" +
            ", targetValue='" + getTargetValue() + "'" +
            ", currentValue='" + getCurrentValue() + "'" +
            ", status='" + getStatus() + "'" +
            ", startDate='" + getStartDate() + "'" +
            ", targetDate='" + getTargetDate() + "'" +
            ", sortOrder=" + getSortOrder() +
            ", createdDate='" + getCreatedDate() + "'" +
            ", lastModifiedDate='" + getLastModifiedDate() + "'" +
            "}";
    }
}
