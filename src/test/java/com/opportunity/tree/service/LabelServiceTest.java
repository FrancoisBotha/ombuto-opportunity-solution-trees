package com.opportunity.tree.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.opportunity.tree.domain.Opportunity;
import com.opportunity.tree.domain.Solution;
import com.opportunity.tree.domain.Tag;
import com.opportunity.tree.domain.Team;
import com.opportunity.tree.domain.enumeration.HistoryEventType;
import com.opportunity.tree.domain.enumeration.TreeNodeType;
import com.opportunity.tree.repository.TagRepository;
import com.opportunity.tree.service.broadcast.TreeChangePublisher;
import com.opportunity.tree.service.dto.tree.TreeNodeDTO;
import jakarta.persistence.EntityManager;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * LABEL-001 unit tests for {@link LabelService}. Covers acceptance criterion 6 (cross-team
 * rejection), criterion 10 (deletion cascade), and criterion 4 (suggestion order: current team
 * first, then the caller's other member teams).
 */
@ExtendWith(MockitoExtension.class)
class LabelServiceTest {

    private static final Long TEAM_A = 1L;
    private static final Long TEAM_B = 2L;
    private static final Long OPPORTUNITY_ID = 100L;

    @Mock
    private TeamAccessService teamAccessService;

    @Mock
    private TagRepository tagRepository;

    @Mock
    private NodeHistoryRecorder historyRecorder;

    @Mock
    private TreeNodeDtoAssembler dtoAssembler;

    @Mock
    private TreeChangePublisher changePublisher;

    @Mock
    private TreeStructureLock structureLock;

    @Mock
    private EntityManager em;

    private LabelService service;
    private Team teamA;
    private Team teamB;

    @BeforeEach
    void setUp() throws Exception {
        service = new LabelService(teamAccessService, tagRepository, historyRecorder, dtoAssembler, changePublisher, structureLock);
        Field emField = LabelService.class.getDeclaredField("em");
        emField.setAccessible(true);
        emField.set(service, em);
        teamA = new Team();
        teamA.setId(TEAM_A);
        teamA.setName("Team A");
        teamB = new Team();
        teamB.setId(TEAM_B);
        teamB.setName("Team B");
    }

    // -----------------------------------------------------------------------
    // Criterion 6: cross-team tag application rejected server-side
    // -----------------------------------------------------------------------

    @Test
    void rejectsTagFromDifferentTeam() {
        Tag foreign = new Tag();
        foreign.setId(500L);
        foreign.setName("Foreign");
        foreign.setTeam(teamB);
        Opportunity opp = new Opportunity();
        opp.setId(OPPORTUNITY_ID);
        opp.setTags(new HashSet<>());
        when(teamAccessService.requireEditNode(TreeNodeType.OPPORTUNITY, OPPORTUNITY_ID)).thenReturn(TEAM_A);
        when(tagRepository.findAllById(Set.of(500L))).thenReturn(List.of(foreign));
        lenient().when(em.find(Opportunity.class, OPPORTUNITY_ID)).thenReturn(opp);

        assertThatThrownBy(() -> service.applyTags(TreeNodeType.OPPORTUNITY, OPPORTUNITY_ID, Set.of(500L)))
            .isInstanceOf(NodeWriteRuleException.class)
            .hasMessageContaining("different team");
        verify(historyRecorder, never()).record(any(), any(), any(), any());
        verify(changePublisher, never()).publish(any(), any(), any());
    }

    @Test
    void rejectsAssumptionAsUnsupportedType() {
        assertThatThrownBy(() -> service.applyTags(TreeNodeType.ASSUMPTION, 1L, Set.of())).isInstanceOf(NodeWriteRuleException.class);
    }

    // -----------------------------------------------------------------------
    // Criterion 10: deleting a tag removes it from every node
    // -----------------------------------------------------------------------

    @Test
    void deleteClearsAssociationsAndReturnsCount() {
        Tag tag = new Tag();
        tag.setId(700L);
        tag.setName("Retention");
        tag.setTeam(teamA);
        Opportunity o1 = new Opportunity();
        o1.setId(1001L);
        o1.setTags(new HashSet<>(Set.of(tag)));
        tag.setOpportunities(new HashSet<>(Set.of(o1)));
        Solution s1 = new Solution();
        s1.setId(2001L);
        s1.setTags(new HashSet<>(Set.of(tag)));
        tag.setSolutions(new HashSet<>(Set.of(s1)));
        when(tagRepository.findById(700L)).thenReturn(Optional.of(tag));
        when(tagRepository.countAssociations(700L)).thenReturn(2L);

        long removed = service.deleteTag(700L);

        assertThat(removed).isEqualTo(2L);
        assertThat(o1.getTags()).isEmpty();
        assertThat(s1.getTags()).isEmpty();
        verify(tagRepository).delete(tag);
    }

    // -----------------------------------------------------------------------
    // Criterion 4: suggestion order — current team first, then other teams
    // -----------------------------------------------------------------------

    @Test
    void suggestionsListCurrentTeamFirstThenOtherTeams() {
        Tag inA = new Tag();
        inA.setId(10L);
        inA.setName("Mobile Value Stream");
        inA.setTeam(teamA);
        Tag alsoInA = new Tag();
        alsoInA.setId(11L);
        alsoInA.setName("Onboarding");
        alsoInA.setTeam(teamA);
        Tag inB = new Tag();
        inB.setId(20L);
        inB.setName("Enterprise");
        inB.setTeam(teamB);
        Tag dupInB = new Tag();
        dupInB.setId(21L);
        dupInB.setName("mobile value stream"); // duplicates A's normalized name — should be filtered
        dupInB.setTeam(teamB);
        when(tagRepository.findAllByTeamId(TEAM_A)).thenReturn(List.of(inA, alsoInA));
        when(tagRepository.findAllByTeamIdIn(Set.of(TEAM_B))).thenReturn(List.of(inB, dupInB));
        when(teamAccessService.getCurrentUserTeamIds()).thenReturn(Set.of(TEAM_A, TEAM_B));

        var suggestions = service.listSuggestions(TEAM_A);

        assertThat(suggestions)
            .extracting(t -> t.name())
            .containsExactly("Mobile Value Stream", "Onboarding", "Enterprise");
    }

    // -----------------------------------------------------------------------
    // Criterion 2/11: applying a NEW tag records history + publishes
    // -----------------------------------------------------------------------

    @Test
    void applyingTagsPublishesChangeAndRecordsHistory() {
        Tag mine = new Tag();
        mine.setId(300L);
        mine.setName("Retention");
        mine.setTeam(teamA);
        Opportunity opp = new Opportunity();
        opp.setId(OPPORTUNITY_ID);
        opp.setTags(new HashSet<>());
        TreeNodeDTO dto = new TreeNodeDTO();
        dto.setType(TreeNodeType.OPPORTUNITY);
        dto.setId(OPPORTUNITY_ID);
        when(teamAccessService.requireEditNode(TreeNodeType.OPPORTUNITY, OPPORTUNITY_ID)).thenReturn(TEAM_A);
        when(tagRepository.findAllById(Set.of(300L))).thenReturn(List.of(mine));
        when(em.find(Opportunity.class, OPPORTUNITY_ID)).thenReturn(opp);
        when(dtoAssembler.toDto(TreeNodeType.OPPORTUNITY, OPPORTUNITY_ID)).thenReturn(dto);

        TreeNodeDTO result = service.applyTags(TreeNodeType.OPPORTUNITY, OPPORTUNITY_ID, Set.of(300L));

        assertThat(result).isSameAs(dto);
        verify(historyRecorder).record(eq(TreeNodeType.OPPORTUNITY), eq(OPPORTUNITY_ID), eq(HistoryEventType.TAGS_CHANGED), any());
        verify(changePublisher).publish(any(), eq(TEAM_A), eq(dto));
    }
}
