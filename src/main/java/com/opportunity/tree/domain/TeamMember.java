package com.opportunity.tree.domain;

import com.opportunity.tree.domain.enumeration.TeamRole;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Join entity between JHipster User and Team, carrying a role.
 */
@Table("team_member")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class TeamMember implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @Column("id")
    private Long id;

    @NotNull(message = "must not be null")
    @Column("role")
    private TeamRole role;

    @NotNull(message = "must not be null")
    @Column("joined_date")
    private Instant joinedDate;

    @org.springframework.data.annotation.Transient
    private Team team;

    @org.springframework.data.annotation.Transient
    private User user;

    @Column("team_id")
    private Long teamId;

    @Column("user_id")
    private String userId;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public TeamMember id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TeamRole getRole() {
        return this.role;
    }

    public TeamMember role(TeamRole role) {
        this.setRole(role);
        return this;
    }

    public void setRole(TeamRole role) {
        this.role = role;
    }

    public Instant getJoinedDate() {
        return this.joinedDate;
    }

    public TeamMember joinedDate(Instant joinedDate) {
        this.setJoinedDate(joinedDate);
        return this;
    }

    public void setJoinedDate(Instant joinedDate) {
        this.joinedDate = joinedDate;
    }

    public Team getTeam() {
        return this.team;
    }

    public void setTeam(Team team) {
        this.team = team;
        this.teamId = team != null ? team.getId() : null;
    }

    public TeamMember team(Team team) {
        this.setTeam(team);
        return this;
    }

    public User getUser() {
        return this.user;
    }

    public void setUser(User user) {
        this.user = user;
        this.userId = user != null ? user.getId() : null;
    }

    public TeamMember user(User user) {
        this.setUser(user);
        return this;
    }

    public Long getTeamId() {
        return this.teamId;
    }

    public void setTeamId(Long team) {
        this.teamId = team;
    }

    public String getUserId() {
        return this.userId;
    }

    public void setUserId(String user) {
        this.userId = user;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TeamMember)) {
            return false;
        }
        return getId() != null && getId().equals(((TeamMember) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "TeamMember{" +
            "id=" + getId() +
            ", role='" + getRole() + "'" +
            ", joinedDate='" + getJoinedDate() + "'" +
            "}";
    }
}
