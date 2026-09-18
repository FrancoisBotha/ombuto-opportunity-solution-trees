package com.opportunity.tree.service.dto;

import com.opportunity.tree.domain.enumeration.TeamRole;
import java.io.Serializable;
import java.time.Instant;

/**
 * Row of the "my teams" list: a team the caller belongs to, plus the caller's role in it and the
 * member/product counts used by the sidebar and cards (TEAMS-002 AC 2).
 */
public class MyTeamDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String description;
    private Instant createdDate;
    private TeamRole role;
    private long memberCount;
    private long productCount;

    public MyTeamDTO() {}

    public MyTeamDTO(Long id, String name, String description, Instant createdDate, TeamRole role, long memberCount, long productCount) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.createdDate = createdDate;
        this.role = role;
        this.memberCount = memberCount;
        this.productCount = productCount;
    }

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

    public TeamRole getRole() {
        return role;
    }

    public void setRole(TeamRole role) {
        this.role = role;
    }

    public long getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(long memberCount) {
        this.memberCount = memberCount;
    }

    public long getProductCount() {
        return productCount;
    }

    public void setProductCount(long productCount) {
        this.productCount = productCount;
    }
}
