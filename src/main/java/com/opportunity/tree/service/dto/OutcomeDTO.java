package com.opportunity.tree.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * A DTO for the {@link com.opportunity.tree.domain.Outcome} entity.
 */
@Schema(description = "Desired outcome at the top of the tree, e.g. \"Increase weekly active users\".")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class OutcomeDTO implements Serializable {

    private Long id;

    @NotNull
    @Size(min = 2, max = 200)
    private String title;

    @Lob
    private String description;

    @NotNull
    private Integer sortOrder;

    @NotNull
    private Instant createdDate;

    private Instant lastModifiedDate;

    @NotNull
    private ProductDTO product;

    private UserDTO owner;

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

    public ProductDTO getProduct() {
        return product;
    }

    public void setProduct(ProductDTO product) {
        this.product = product;
    }

    public UserDTO getOwner() {
        return owner;
    }

    public void setOwner(UserDTO owner) {
        this.owner = owner;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OutcomeDTO)) {
            return false;
        }

        OutcomeDTO outcomeDTO = (OutcomeDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, outcomeDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "OutcomeDTO{" +
            "id=" + getId() +
            ", title='" + getTitle() + "'" +
            ", description='" + getDescription() + "'" +
            ", sortOrder=" + getSortOrder() +
            ", createdDate='" + getCreatedDate() + "'" +
            ", lastModifiedDate='" + getLastModifiedDate() + "'" +
            ", product=" + getProduct() +
            ", owner=" + getOwner() +
            "}";
    }
}
