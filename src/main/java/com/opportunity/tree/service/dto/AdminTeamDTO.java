package com.opportunity.tree.service.dto;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Row of the admin-only team list (ADHOC-001). Unlike {@link MyTeamDTO} this does not depend on the
 * calling admin's membership: the counts and owner logins describe the team itself.
 */
public class AdminTeamDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String description;
    private Instant createdDate;
    private long memberCount;
    private long productCount;
    private List<String> ownerLogins = new ArrayList<>();

    public AdminTeamDTO() {}

    public AdminTeamDTO(
        Long id,
        String name,
        String description,
        Instant createdDate,
        long memberCount,
        long productCount,
        List<String> ownerLogins
    ) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.createdDate = createdDate;
        this.memberCount = memberCount;
        this.productCount = productCount;
        this.ownerLogins = ownerLogins == null ? new ArrayList<>() : ownerLogins;
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

    public List<String> getOwnerLogins() {
        return ownerLogins;
    }

    public void setOwnerLogins(List<String> ownerLogins) {
        this.ownerLogins = ownerLogins == null ? new ArrayList<>() : ownerLogins;
    }
}
