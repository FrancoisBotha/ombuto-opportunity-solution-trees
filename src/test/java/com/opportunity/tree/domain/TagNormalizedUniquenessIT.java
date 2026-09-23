package com.opportunity.tree.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.opportunity.tree.IntegrationTest;
import com.opportunity.tree.repository.TagRepository;
import com.opportunity.tree.repository.TeamRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

/**
 * LABEL-001 criterion 5: the database enforces case- and whitespace-insensitive uniqueness of tag
 * names within a single team. Two tags whose names normalize to the same value are rejected by
 * the unique constraint on (team_id, normalized_name).
 */
@IntegrationTest
@Transactional
class TagNormalizedUniquenessIT {

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private TeamRepository teamRepository;

    @PersistenceContext
    private EntityManager em;

    @Test
    void secondTagWithSameNormalizedNameInSameTeamIsRejected() {
        Team team = new Team();
        team.setName("Team " + System.nanoTime());
        team.setCreatedDate(java.time.Instant.now());
        team = teamRepository.saveAndFlush(team);

        Tag first = new Tag();
        first.setTeam(team);
        first.setName("Mobile Value Stream");
        tagRepository.saveAndFlush(first);

        Tag duplicateByCase = new Tag();
        duplicateByCase.setTeam(team);
        duplicateByCase.setName("  mobile VALUE stream  ");
        assertThat(duplicateByCase.getNormalizedName()).isEqualTo("mobile value stream");

        Team finalTeam = team;
        assertThatThrownBy(() -> {
            tagRepository.saveAndFlush(duplicateByCase);
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void identicalNormalizedNameInDifferentTeamsIsAllowed() {
        Team teamA = new Team();
        teamA.setName("Team A " + System.nanoTime());
        teamA.setCreatedDate(java.time.Instant.now());
        teamRepository.saveAndFlush(teamA);
        Team teamB = new Team();
        teamB.setName("Team B " + System.nanoTime());
        teamB.setCreatedDate(java.time.Instant.now());
        teamRepository.saveAndFlush(teamB);

        Tag inA = new Tag();
        inA.setTeam(teamA);
        inA.setName("Onboarding");
        tagRepository.saveAndFlush(inA);

        Tag inB = new Tag();
        inB.setTeam(teamB);
        inB.setName("Onboarding");
        tagRepository.saveAndFlush(inB);

        assertThat(inA.getId()).isNotNull();
        assertThat(inB.getId()).isNotNull();
        assertThat(inA.getId()).isNotEqualTo(inB.getId());
    }
}
