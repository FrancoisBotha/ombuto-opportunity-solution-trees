package com.opportunity.tree.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.opportunity.tree.domain.enumeration.LinkType;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Named external URL attached to a solution (prototype, ticket, design).
 */
@Table("solution_link")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class SolutionLink implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @Column("id")
    private Long id;

    @NotNull(message = "must not be null")
    @Size(min = 1, max = 100)
    @Column("name")
    private String name;

    @NotNull(message = "must not be null")
    @Size(max = 2000)
    @Pattern(regexp = "^https?:\\/\\/.+")
    @Column("url")
    private String url;

    @NotNull(message = "must not be null")
    @Column("type")
    private LinkType type;

    @NotNull(message = "must not be null")
    @Column("sort_order")
    private Integer sortOrder;

    @org.springframework.data.annotation.Transient
    @JsonIgnoreProperties(value = { "opportunity", "owner", "tags" }, allowSetters = true)
    private Solution solution;

    @Column("solution_id")
    private Long solutionId;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public SolutionLink id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public SolutionLink name(String name) {
        this.setName(name);
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return this.url;
    }

    public SolutionLink url(String url) {
        this.setUrl(url);
        return this;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public LinkType getType() {
        return this.type;
    }

    public SolutionLink type(LinkType type) {
        this.setType(type);
        return this;
    }

    public void setType(LinkType type) {
        this.type = type;
    }

    public Integer getSortOrder() {
        return this.sortOrder;
    }

    public SolutionLink sortOrder(Integer sortOrder) {
        this.setSortOrder(sortOrder);
        return this;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Solution getSolution() {
        return this.solution;
    }

    public void setSolution(Solution solution) {
        this.solution = solution;
        this.solutionId = solution != null ? solution.getId() : null;
    }

    public SolutionLink solution(Solution solution) {
        this.setSolution(solution);
        return this;
    }

    public Long getSolutionId() {
        return this.solutionId;
    }

    public void setSolutionId(Long solution) {
        this.solutionId = solution;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SolutionLink)) {
            return false;
        }
        return getId() != null && getId().equals(((SolutionLink) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "SolutionLink{" +
            "id=" + getId() +
            ", name='" + getName() + "'" +
            ", url='" + getUrl() + "'" +
            ", type='" + getType() + "'" +
            ", sortOrder=" + getSortOrder() +
            "}";
    }
}
