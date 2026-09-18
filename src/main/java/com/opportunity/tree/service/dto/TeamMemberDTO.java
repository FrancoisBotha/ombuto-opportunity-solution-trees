package com.opportunity.tree.service.dto;

import com.opportunity.tree.domain.enumeration.TeamRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * A DTO for the {@link com.opportunity.tree.domain.TeamMember} entity.
 */
@Schema(description = "Join entity between JHipster User and Team, carrying a role.")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class TeamMemberDTO implements Serializable {

    private Long id;

    @NotNull
    private TeamRole role;

    @NotNull
    private Instant joinedDate;

    @NotNull
    private TeamDTO team;

    @NotNull
    private UserDTO user;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public TeamDTO getTeam() {
        return team;
    }

    public void setTeam(TeamDTO team) {
        this.team = team;
    }

    public UserDTO getUser() {
        return user;
    }

    public void setUser(UserDTO user) {
        this.user = user;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TeamMemberDTO)) {
            return false;
        }

        TeamMemberDTO teamMemberDTO = (TeamMemberDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, teamMemberDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "TeamMemberDTO{" +
            "id=" + getId() +
            ", role='" + getRole() + "'" +
            ", joinedDate='" + getJoinedDate() + "'" +
            ", team=" + getTeam() +
            ", user=" + getUser() +
            "}";
    }
}
