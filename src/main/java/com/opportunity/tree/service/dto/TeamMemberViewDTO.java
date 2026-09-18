package com.opportunity.tree.service.dto;

import com.opportunity.tree.domain.enumeration.TeamRole;
import java.io.Serializable;
import java.time.Instant;

public class TeamMemberViewDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String userId;
    private String login;
    private String firstName;
    private String lastName;
    private TeamRole role;
    private Instant joinedDate;

    public TeamMemberViewDTO() {}

    public TeamMemberViewDTO(Long id, String userId, String login, String firstName, String lastName, TeamRole role, Instant joinedDate) {
        this.id = id;
        this.userId = userId;
        this.login = login;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
        this.joinedDate = joinedDate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public TeamRole getRole() {
        return role;
    }

    public void setRole(TeamRole role) {
        this.role = role;
    }

    public Instant getJoinedDate() {
        return joinedDate;
    }

    public void setJoinedDate(Instant joinedDate) {
        this.joinedDate = joinedDate;
    }
}
