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
import com.opportunity.tree.domain.Solution;
import com.opportunity.tree.domain.Team;
import com.opportunity.tree.domain.enumeration.SolutionStatus;
import com.opportunity.tree.repository.OpportunityRepository;
import com.opportunity.tree.repository.SolutionRepository;
import com.opportunity.tree.service.NodeWriteRuleException;
import com.opportunity.tree.service.TeamAccessDeniedException;
import com.opportunity.tree.service.TeamAccessService;
import com.opportunity.tree.service.dto.OpportunityDTO;
import com.opportunity.tree.service.dto.SolutionDTO;
import com.opportunity.tree.service.mapper.SolutionMapper;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for TREE-002 node write rules on {@link SolutionServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class SolutionServiceImplTest {

    private static final Long OPPORTUNITY_ID = 200L;
    private static final Long SOLUTION_ID = 300L;

    @Mock
    private SolutionRepository solutionRepository;

    @Mock
    private OpportunityRepository opportunityRepository;

    @Mock
    private TeamAccessService teamAccessService;

    private SolutionMapper solutionMapper;

    private SolutionServiceImpl service;

    private Opportunity opportunity;

    @BeforeEach
    void setUp() {
        solutionMapper = mock(SolutionMapper.class);
        service = new SolutionServiceImpl(solutionRepository, opportunityRepository, solutionMapper, teamAccessService);

        opportunity = new Opportunity().id(OPPORTUNITY_ID);

        lenient().when(opportunityRepository.findById(OPPORTUNITY_ID)).thenReturn(Optional.of(opportunity));
        lenient()
            .when(solutionMapper.toEntity(any(SolutionDTO.class)))
            .thenAnswer(inv -> {
                SolutionDTO dto = inv.getArgument(0);
                Solution s = new Solution();
                s.setId(dto.getId());
                s.setTitle(dto.getTitle());
                s.setStatus(dto.getStatus());
                s.setEffort(dto.getEffort());
                s.setSortOrder(dto.getSortOrder());
                s.setCreatedDate(dto.getCreatedDate());
                s.setLastModifiedDate(dto.getLastModifiedDate());
                return s;
            });
        lenient()
            .when(solutionRepository.save(any(Solution.class)))
            .thenAnswer(inv -> inv.getArgument(0));
        lenient()
            .when(solutionMapper.toDto(any(Solution.class)))
            .thenAnswer(inv -> {
                Solution s = inv.getArgument(0);
                SolutionDTO dto = new SolutionDTO();
                dto.setId(s.getId());
                dto.setTitle(s.getTitle());
                dto.setStatus(s.getStatus());
                dto.setSortOrder(s.getSortOrder());
                dto.setCreatedDate(s.getCreatedDate());
                dto.setLastModifiedDate(s.getLastModifiedDate());
                return dto;
            });
    }

    @Test
    void createSetsServerFieldsAndAppendsSortOrder() {
        Instant clientDate = Instant.parse("1999-01-01T00:00:00Z");
        SolutionDTO input = createDto();
        input.setCreatedDate(clientDate);
        input.setLastModifiedDate(clientDate);
        input.setSortOrder(999);
        when(solutionRepository.findMaxSortOrderByOpportunityId(OPPORTUNITY_ID)).thenReturn(2);
        Instant before = Instant.now();

        service.save(input);

        Solution saved = captureSaved();
        assertThat(saved.getCreatedDate()).isAfterOrEqualTo(before).isNotEqualTo(clientDate);
        assertThat(saved.getLastModifiedDate()).isEqualTo(saved.getCreatedDate());
        assertThat(saved.getSortOrder()).isEqualTo(3);
    }

    @Test
    void createRejectsWhenOpportunityMissing() {
        SolutionDTO input = createDto();
        input.setOpportunity(null);

        assertThatThrownBy(() -> service.save(input)).isInstanceOf(NodeWriteRuleException.class);
    }

    @Test
    void createRejectsWhenOpportunityDoesNotExist() {
        SolutionDTO input = createDto();
        OpportunityDTO missing = new OpportunityDTO();
        missing.setId(9999L);
        input.setOpportunity(missing);
        when(opportunityRepository.findById(9999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.save(input)).isInstanceOf(NodeWriteRuleException.class);
    }

    @Test
    void createDeniedForViewer() {
        doThrow(new TeamAccessDeniedException()).when(teamAccessService).requireEditOpportunity(OPPORTUNITY_ID);
        SolutionDTO input = createDto();

        assertThatThrownBy(() -> service.save(input)).isInstanceOf(TeamAccessDeniedException.class);
    }

    @Test
    void updatePreservesCreatedDateAndSetsLastModifiedDate() {
        Instant originalCreated = Instant.parse("2020-01-01T00:00:00Z");
        Solution existing = new Solution();
        existing.setId(SOLUTION_ID);
        existing.setOpportunity(opportunity);
        existing.setStatus(SolutionStatus.IDEA);
        existing.setSortOrder(0);
        existing.setCreatedDate(originalCreated);
        when(solutionRepository.findById(SOLUTION_ID)).thenReturn(Optional.of(existing));

        SolutionDTO input = createDto();
        input.setId(SOLUTION_ID);
        input.setCreatedDate(Instant.parse("1999-06-01T00:00:00Z"));
        Instant before = Instant.now();

        service.update(input);

        Solution saved = captureSaved();
        assertThat(saved.getCreatedDate()).isEqualTo(originalCreated);
        assertThat(saved.getLastModifiedDate()).isAfterOrEqualTo(before);
    }

    @Test
    void updateRejectsWhenTargetParentOpportunityIsInAnotherTeam() {
        Team teamA = new Team();
        teamA.setId(10L);
        Team teamB = new Team();
        teamB.setId(20L);

        Product productA = new Product();
        productA.setId(11L);
        productA.setTeam(teamA);
        Outcome outcomeA = new Outcome();
        outcomeA.setId(12L);
        outcomeA.setProduct(productA);
        Opportunity existingParent = new Opportunity().id(OPPORTUNITY_ID);
        existingParent.setOutcome(outcomeA);

        Product productB = new Product();
        productB.setId(21L);
        productB.setTeam(teamB);
        Outcome outcomeB = new Outcome();
        outcomeB.setId(22L);
        outcomeB.setProduct(productB);
        Long otherOppId = 999L;
        Opportunity otherTeamParent = new Opportunity().id(otherOppId);
        otherTeamParent.setOutcome(outcomeB);
        when(opportunityRepository.findById(otherOppId)).thenReturn(Optional.of(otherTeamParent));

        Solution existing = new Solution();
        existing.setId(SOLUTION_ID);
        existing.setOpportunity(existingParent);
        existing.setStatus(SolutionStatus.IDEA);
        existing.setSortOrder(0);
        existing.setCreatedDate(Instant.parse("2020-01-01T00:00:00Z"));
        when(solutionRepository.findById(SOLUTION_ID)).thenReturn(Optional.of(existing));

        SolutionDTO input = createDto();
        input.setId(SOLUTION_ID);
        OpportunityDTO newParent = new OpportunityDTO();
        newParent.setId(otherOppId);
        input.setOpportunity(newParent);

        assertThatThrownBy(() -> service.update(input))
            .isInstanceOf(NodeWriteRuleException.class)
            .hasMessageContaining("same team");
    }

    @Test
    void updateStatusIsAllowed() {
        Solution existing = new Solution();
        existing.setId(SOLUTION_ID);
        existing.setOpportunity(opportunity);
        existing.setStatus(SolutionStatus.IDEA);
        existing.setSortOrder(0);
        existing.setCreatedDate(Instant.parse("2020-01-01T00:00:00Z"));
        when(solutionRepository.findById(SOLUTION_ID)).thenReturn(Optional.of(existing));

        SolutionDTO input = createDto();
        input.setId(SOLUTION_ID);
        input.setStatus(SolutionStatus.SHIPPED);

        service.update(input);

        Solution saved = captureSaved();
        assertThat(saved.getStatus()).isEqualTo(SolutionStatus.SHIPPED);
    }

    private SolutionDTO createDto() {
        SolutionDTO dto = new SolutionDTO();
        dto.setTitle("A solution");
        dto.setStatus(SolutionStatus.IDEA);
        dto.setSortOrder(0);
        OpportunityDTO op = new OpportunityDTO();
        op.setId(OPPORTUNITY_ID);
        dto.setOpportunity(op);
        return dto;
    }

    private Solution captureSaved() {
        ArgumentCaptor<Solution> captor = ArgumentCaptor.forClass(Solution.class);
        org.mockito.Mockito.verify(solutionRepository).save(captor.capture());
        return captor.getValue();
    }
}
