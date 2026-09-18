package com.opportunity.tree.service.dto;

import com.opportunity.tree.domain.enumeration.LinkType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.util.Objects;

/**
 * A DTO for the {@link com.opportunity.tree.domain.SolutionLink} entity.
 */
@Schema(description = "Named external URL attached to a solution (prototype, ticket, design).")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class SolutionLinkDTO implements Serializable {

    private Long id;

    @NotNull(message = "must not be null")
    @Size(min = 1, max = 100)
    private String name;

    @NotNull(message = "must not be null")
    @Size(max = 2000)
    @Pattern(regexp = "^https?:\\/\\/.+")
    private String url;

    @NotNull(message = "must not be null")
    private LinkType type;

    @NotNull(message = "must not be null")
    private Integer sortOrder;

    @NotNull
    private SolutionDTO solution;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public LinkType getType() {
        return type;
    }

    public void setType(LinkType type) {
        this.type = type;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public SolutionDTO getSolution() {
        return solution;
    }

    public void setSolution(SolutionDTO solution) {
        this.solution = solution;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SolutionLinkDTO)) {
            return false;
        }

        SolutionLinkDTO solutionLinkDTO = (SolutionLinkDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, solutionLinkDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "SolutionLinkDTO{" +
            "id=" + getId() +
            ", name='" + getName() + "'" +
            ", url='" + getUrl() + "'" +
            ", type='" + getType() + "'" +
            ", sortOrder=" + getSortOrder() +
            ", solution=" + getSolution() +
            "}";
    }
}
