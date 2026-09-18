package com.opportunity.tree.service.dto.tree;

import com.opportunity.tree.domain.enumeration.TeamRole;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Root document returned by {@code GET /api/teams/{teamId}/tree} (TREE-001).
 *
 * <p>Wraps the team's own fields together with its full tree of products,
 * outcomes, opportunities (nested to arbitrary depth) and solutions. Also
 * carries the caller's effective role and a {@link #canEdit} flag so the
 * frontend can disable editing for viewers without a second request.
 */
public class TeamTreeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String description;
    private Instant createdDate;
    private TeamRole currentUserRole;
    private boolean canEdit;
    private List<ProductTreeNodeDTO> products = new ArrayList<>();

    public TeamTreeDTO() {}

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    public TeamRole getCurrentUserRole() {
        return currentUserRole;
    }

    public void setCurrentUserRole(TeamRole currentUserRole) {
        this.currentUserRole = currentUserRole;
    }

    public boolean isCanEdit() {
        return canEdit;
    }

    public void setCanEdit(boolean canEdit) {
        this.canEdit = canEdit;
    }

    public List<ProductTreeNodeDTO> getProducts() {
        return products;
    }

    public void setProducts(List<ProductTreeNodeDTO> products) {
        this.products = products;
    }
}
