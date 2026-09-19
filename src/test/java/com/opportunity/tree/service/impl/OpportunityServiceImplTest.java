package com.opportunity.tree.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.opportunity.tree.domain.Opportunity;
import com.opportunity.tree.domain.Outcome;
import com.opportunity.tree.domain.Product;
import com.opportunity.tree.domain.Team;
import com.opportunity.tree.domain.enumeration.OpportunityStatus;
import com.opportunity.tree.repository.OpportunityRepository;
import com.opportunity.tree.repository.OutcomeRepository;
import com.opportunity.tree.service.NodeWriteRuleException;
import com.opportunity.tree.service.TeamAccessDeniedException;
import com.opportunity.tree.service.TeamAccessService;
import com.opportunity.tree.service.dto.OpportunityDTO;
import com.opportunity.tree.service.dto.OutcomeDTO;
import com.opportunity.tree.service.mapper.OpportunityMapper;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for TREE-002 node write rules on {@link OpportunityServiceImpl}:
 * server-set fields, append-last ordering, defaults, parent validation, cycle
 * detection, and the role matrix through {@link TeamAccessService}.
 */
@ExtendWith(MockitoExtension.class)
class OpportunityServiceImplTest {

    private static final Long TEAM_ID = 1L;
    private static final Long OTHER_TEAM_ID = 2L;
    private static final Long PRODUCT_ID = 10L;
    private static final Long OUTCOME_ID = 100L;
    private static final Long OTHER_TEAM_OUTCOME_ID = 101L;
    private static final Long PARENT_OPP_ID = 200L;
    private static final Long SELF_ID = 300L;

    @Mock
    private OpportunityRepository opportunityRepository;

    @Mock
    private OutcomeRepository outcomeRepository;

    @Mock
    private TeamAccessService teamAccessService;

    private OpportunityMapper opportunityMapper;

    private OpportunityServiceImpl service;

    private Outcome outcome;
    private Outcome otherTeamOutcome;

    @BeforeEach
    void setUp() {
        opportunityMapper = mock(OpportunityMapper.class);
        service = new OpportunityServiceImpl(opportunityRepository, outcomeRepository, opportunityMapper, teamAccessService);

        Team team = new Team();
        team.setId(TEAM_ID);
        Product product = new Product().id(PRODUCT_ID);
        product.setTeam(team);
        outcome = new Outcome().id(OUTCOME_ID);
        outcome.setProduct(product);

        Team otherTeam = new Team();
        otherTeam.setId(OTHER_TEAM_ID);
        Product otherProduct = new Product().id(11L);
        otherProduct.setTeam(otherTeam);
        otherTeamOutcome = new Outcome().id(OTHER_TEAM_OUTCOME_ID);
        otherTeamOutcome.setProduct(otherProduct);

        lenient().when(outcomeRepository.findById(OUTCOME_ID)).thenReturn(Optional.of(outcome));
        lenient().when(outcomeRepository.findById(OTHER_TEAM_OUTCOME_ID)).thenReturn(Optional.of(otherTeamOutcome));
        lenient()
            .when(opportunityMapper.toEntity(any(OpportunityDTO.class)))
            .thenAnswer(inv -> mapDtoToEntity(inv.getArgument(0)));
        lenient()
            .when(opportunityRepository.save(any(Opportunity.class)))
            .thenAnswer(inv -> inv.getArgument(0));
        lenient()
            .when(opportunityMapper.toDto(any(Opportunity.class)))
            .thenAnswer(inv -> {
                Opportunity o = inv.getArgument(0);
                OpportunityDTO dto = new OpportunityDTO();
                dto.setId(o.getId());
                dto.setTitle(o.getTitle());
                dto.setStatus(o.getStatus());
                dto.setValuerating(o.getValuerating());
                dto.setComplexity(o.getComplexity());
                dto.setSortOrder(o.getSortOrder());
                dto.setCreatedDate(o.getCreatedDate());
                dto.setLastModifiedDate(o.getLastModifiedDate());
                return dto;
            });
    }

    // ---- create: server-set fields (AC1) ----

    @Test
    void createSetsCreatedAndLastModifiedDateIgnoringClientValues() {
        Instant clientDate = Instant.parse("1999-01-01T00:00:00Z");
        OpportunityDTO input = newCreateDto();
        input.setCreatedDate(clientDate);
        input.setLastModifiedDate(clientDate);
        stubSortOrderMax(-1);
        Instant before = Instant.now();

        service.save(input);

        Opportunity saved = captureSaved();
        assertThat(saved.getCreatedDate()).isNotEqualTo(clientDate).isAfterOrEqualTo(before);
        assertThat(saved.getLastModifiedDate()).isEqualTo(saved.getCreatedDate());
    }

    // ---- create: append-last sortOrder ignoring client value (AC2) ----

    @Test
    void createAppendsLastSortOrderIgnoringClientValue() {
        OpportunityDTO input = newCreateDto();
        input.setSortOrder(999);
        stubSortOrderMax(4);

        service.save(input);

        Opportunity saved = captureSaved();
        assertThat(saved.getSortOrder()).isEqualTo(5);
    }

    @Test
    void createStartsSortOrderAtZeroForFirstSibling() {
        OpportunityDTO input = newCreateDto();
        stubSortOrderMax(-1);

        service.save(input);

        Opportunity saved = captureSaved();
        assertThat(saved.getSortOrder()).isZero();
    }

    // ---- create: defaults valuerating/complexity = 3 (AC3) ----

    @Test
    void createDefaultsValueratingAndComplexityToThree() {
        OpportunityDTO input = newCreateDto();
        input.setValuerating(null);
        input.setComplexity(null);
        stubSortOrderMax(-1);

        service.save(input);

        Opportunity saved = captureSaved();
        assertThat(saved.getValuerating()).isEqualTo(3);
        assertThat(saved.getComplexity()).isEqualTo(3);
    }

    // ---- parent validation (AC4) ----

    @Test
    void createRejectsWhenNoOutcomeOrParent() {
        OpportunityDTO input = newCreateDto();
        input.setOutcome(null);
        input.setParent(null);

        assertThatThrownBy(() -> service.save(input)).isInstanceOf(NodeWriteRuleException.class);
    }

    @Test
    void createRejectsWhenOutcomeDoesNotExist() {
        OpportunityDTO input = newCreateDto();
        OutcomeDTO missing = new OutcomeDTO();
        missing.setId(9999L);
        input.setOutcome(missing);
        when(outcomeRepository.findById(9999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.save(input)).isInstanceOf(NodeWriteRuleException.class);
    }

    @Test
    void createUnderParentOpportunityInheritsOutcome() {
        Opportunity parent = new Opportunity().id(PARENT_OPP_ID);
        parent.setOutcome(outcome);
        when(opportunityRepository.findById(PARENT_OPP_ID)).thenReturn(Optional.of(parent));

        OpportunityDTO input = newCreateDto();
        input.setOutcome(null);
        OpportunityDTO parentDto = new OpportunityDTO();
        parentDto.setId(PARENT_OPP_ID);
        input.setParent(parentDto);
        when(opportunityRepository.findMaxSortOrderByParentId(PARENT_OPP_ID)).thenReturn(2);

        service.save(input);

        Opportunity saved = captureSaved();
        assertThat(saved.getOutcome()).isEqualTo(outcome);
        assertThat(saved.getParent()).isEqualTo(parent);
        assertThat(saved.getSortOrder()).isEqualTo(3);
    }

    // ---- nesting three levels (AC6) ----

    @Test
    void supportsNestingThreeLevelsDeep() {
        Opportunity level1 = new Opportunity().id(400L);
        level1.setOutcome(outcome);
        Opportunity level2 = new Opportunity().id(401L);
        level2.setOutcome(outcome);
        level2.setParent(level1);
        when(opportunityRepository.findById(401L)).thenReturn(Optional.of(level2));
        when(opportunityRepository.findMaxSortOrderByParentId(401L)).thenReturn(-1);

        OpportunityDTO level3 = newCreateDto();
        level3.setOutcome(null);
        OpportunityDTO parentDto = new OpportunityDTO();
        parentDto.setId(401L);
        level3.setParent(parentDto);

        service.save(level3);

        Opportunity saved = captureSaved();
        assertThat(saved.getParent()).isEqualTo(level2);
        assertThat(saved.getOutcome()).isEqualTo(outcome);
    }

    // ---- same-team parent (AC5) ----

    @Test
    void updateRejectsReparentingToDifferentTeam() {
        Opportunity existing = existingOpportunity();
        when(opportunityRepository.findById(SELF_ID)).thenReturn(Optional.of(existing));
        Opportunity foreignParent = new Opportunity().id(500L);
        foreignParent.setOutcome(otherTeamOutcome);
        when(opportunityRepository.findById(500L)).thenReturn(Optional.of(foreignParent));

        OpportunityDTO input = updateDto();
        OpportunityDTO parentDto = new OpportunityDTO();
        parentDto.setId(500L);
        input.setParent(parentDto);

        assertThatThrownBy(() -> service.update(input)).isInstanceOf(NodeWriteRuleException.class);
    }

    @Test
    void updateRejectsSelfAsParent() {
        Opportunity existing = existingOpportunity();
        when(opportunityRepository.findById(SELF_ID)).thenReturn(Optional.of(existing));

        OpportunityDTO input = updateDto();
        OpportunityDTO parentDto = new OpportunityDTO();
        parentDto.setId(SELF_ID);
        input.setParent(parentDto);

        assertThatThrownBy(() -> service.update(input)).isInstanceOf(NodeWriteRuleException.class);
    }

    @Test
    void updateRejectsCycleWhereParentIsDescendant() {
        Opportunity existing = existingOpportunity();
        Opportunity descendant = new Opportunity().id(700L);
        descendant.setOutcome(outcome);
        descendant.setParent(existing);
        when(opportunityRepository.findById(SELF_ID)).thenReturn(Optional.of(existing));
        when(opportunityRepository.findById(700L)).thenReturn(Optional.of(descendant));

        OpportunityDTO input = updateDto();
        OpportunityDTO parentDto = new OpportunityDTO();
        parentDto.setId(700L);
        input.setParent(parentDto);

        assertThatThrownBy(() -> service.update(input)).isInstanceOf(NodeWriteRuleException.class);
    }

    // ---- role matrix (AC7) ----

    @Test
    void createDeniedForViewer() {
        doThrow(new TeamAccessDeniedException()).when(teamAccessService).requireEditOutcome(OUTCOME_ID);

        OpportunityDTO input = newCreateDto();

        assertThatThrownBy(() -> service.save(input)).isInstanceOf(TeamAccessDeniedException.class);
    }

    @Test
    void updateDeniedForViewer() {
        Opportunity existing = existingOpportunity();
        when(opportunityRepository.findById(SELF_ID)).thenReturn(Optional.of(existing));
        doThrow(new TeamAccessDeniedException()).when(teamAccessService).requireEditOpportunity(SELF_ID);

        assertThatThrownBy(() -> service.update(updateDto())).isInstanceOf(TeamAccessDeniedException.class);
    }

    // ---- update: lastModifiedDate updated, createdDate preserved (AC1) ----

    @Test
    void updatePreservesCreatedDateAndSetsLastModifiedDate() {
        Instant originalCreated = Instant.parse("2020-01-01T00:00:00Z");
        Opportunity existing = existingOpportunity();
        existing.setCreatedDate(originalCreated);
        when(opportunityRepository.findById(SELF_ID)).thenReturn(Optional.of(existing));

        OpportunityDTO input = updateDto();
        input.setCreatedDate(Instant.parse("1999-06-01T00:00:00Z"));
        Instant before = Instant.now();

        service.update(input);

        Opportunity saved = captureSaved();
        assertThat(saved.getCreatedDate()).isEqualTo(originalCreated);
        assertThat(saved.getLastModifiedDate()).isAfterOrEqualTo(before);
    }

    // ---- status is updateable (AC9) ----

    @Test
    void updateChangesStatus() {
        Opportunity existing = existingOpportunity();
        existing.setStatus(OpportunityStatus.IDENTIFIED);
        when(opportunityRepository.findById(SELF_ID)).thenReturn(Optional.of(existing));

        OpportunityDTO input = updateDto();
        input.setStatus(OpportunityStatus.PRIORITISED);

        service.update(input);

        Opportunity saved = captureSaved();
        assertThat(saved.getStatus()).isEqualTo(OpportunityStatus.PRIORITISED);
    }

    // ---- helpers ----

    private void stubSortOrderMax(int maxRoot) {
        when(opportunityRepository.findMaxSortOrderByOutcomeIdRoot(OUTCOME_ID)).thenReturn(maxRoot);
    }

    private OpportunityDTO newCreateDto() {
        OpportunityDTO dto = new OpportunityDTO();
        dto.setTitle("New opp");
        dto.setStatus(OpportunityStatus.IDENTIFIED);
        dto.setValuerating(3);
        dto.setComplexity(3);
        OutcomeDTO out = new OutcomeDTO();
        out.setId(OUTCOME_ID);
        dto.setOutcome(out);
        return dto;
    }

    private OpportunityDTO updateDto() {
        OpportunityDTO dto = new OpportunityDTO();
        dto.setId(SELF_ID);
        dto.setTitle("Existing");
        dto.setStatus(OpportunityStatus.IDENTIFIED);
        dto.setValuerating(3);
        dto.setComplexity(3);
        OutcomeDTO out = new OutcomeDTO();
        out.setId(OUTCOME_ID);
        dto.setOutcome(out);
        return dto;
    }

    private Opportunity existingOpportunity() {
        Opportunity existing = new Opportunity().id(SELF_ID);
        existing.setOutcome(outcome);
        existing.setStatus(OpportunityStatus.IDENTIFIED);
        existing.setValuerating(3);
        existing.setComplexity(3);
        existing.setSortOrder(0);
        existing.setCreatedDate(Instant.parse("2020-01-01T00:00:00Z"));
        return existing;
    }

    private Opportunity captureSaved() {
        ArgumentCaptor<Opportunity> captor = ArgumentCaptor.forClass(Opportunity.class);
        org.mockito.Mockito.verify(opportunityRepository).save(captor.capture());
        return captor.getValue();
    }

    private static Opportunity mapDtoToEntity(OpportunityDTO dto) {
        Opportunity o = new Opportunity();
        o.setId(dto.getId());
        o.setTitle(dto.getTitle());
        o.setDescription(dto.getDescription());
        o.setStatus(dto.getStatus());
        o.setValuerating(dto.getValuerating());
        o.setComplexity(dto.getComplexity());
        o.setSortOrder(dto.getSortOrder());
        o.setCreatedDate(dto.getCreatedDate());
        o.setLastModifiedDate(dto.getLastModifiedDate());
        return o;
    }
}
