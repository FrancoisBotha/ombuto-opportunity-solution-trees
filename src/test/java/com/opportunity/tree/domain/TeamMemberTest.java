package com.opportunity.tree.domain;

import static com.opportunity.tree.domain.TeamMemberTestSamples.*;
import static com.opportunity.tree.domain.TeamTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.opportunity.tree.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class TeamMemberTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(TeamMember.class);
        TeamMember teamMember1 = getTeamMemberSample1();
        TeamMember teamMember2 = new TeamMember();
        assertThat(teamMember1).isNotEqualTo(teamMember2);

        teamMember2.setId(teamMember1.getId());
        assertThat(teamMember1).isEqualTo(teamMember2);

        teamMember2 = getTeamMemberSample2();
        assertThat(teamMember1).isNotEqualTo(teamMember2);
    }

    @Test
    void teamTest() {
        TeamMember teamMember = getTeamMemberRandomSampleGenerator();
        Team teamBack = getTeamRandomSampleGenerator();

        teamMember.setTeam(teamBack);
        assertThat(teamMember.getTeam()).isEqualTo(teamBack);

        teamMember.team(null);
        assertThat(teamMember.getTeam()).isNull();
    }
}
