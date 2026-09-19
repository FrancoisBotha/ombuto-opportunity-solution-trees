package com.opportunity.tree.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.opportunity.tree.domain.Outcome;
import com.opportunity.tree.domain.Product;
import com.opportunity.tree.domain.Team;
import com.opportunity.tree.domain.enumeration.OutcomeStatus;
import com.opportunity.tree.repository.OutcomeRepository;
import com.opportunity.tree.repository.ProductRepository;
import com.opportunity.tree.service.NodeWriteRuleException;
import com.opportunity.tree.service.TeamAccessDeniedException;
import com.opportunity.tree.service.TeamAccessService;
import com.opportunity.tree.service.dto.OutcomeDTO;
import com.opportunity.tree.service.dto.ProductDTO;
import com.opportunity.tree.service.mapper.OutcomeMapper;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for TREE-002 node write rules on {@link OutcomeServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class OutcomeServiceImplTest {

    private static final Long PRODUCT_ID = 10L;
    private static final Long OUTCOME_ID = 100L;

    @Mock
    private OutcomeRepository outcomeRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private TeamAccessService teamAccessService;

    private OutcomeMapper outcomeMapper;

    private OutcomeServiceImpl service;

    private Product product;

    @BeforeEach
    void setUp() {
        outcomeMapper = mock(OutcomeMapper.class);
        service = new OutcomeServiceImpl(outcomeRepository, productRepository, outcomeMapper, teamAccessService);

        Team team = new Team();
        team.setId(1L);
        product = new Product().id(PRODUCT_ID);
        product.setTeam(team);

        lenient().when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        lenient()
            .when(outcomeMapper.toEntity(any(OutcomeDTO.class)))
            .thenAnswer(inv -> {
                OutcomeDTO dto = inv.getArgument(0);
                Outcome o = new Outcome();
                o.setId(dto.getId());
                o.setTitle(dto.getTitle());
                o.setStatus(dto.getStatus());
                o.setSortOrder(dto.getSortOrder());
                o.setCreatedDate(dto.getCreatedDate());
                o.setLastModifiedDate(dto.getLastModifiedDate());
                return o;
            });
        lenient()
            .when(outcomeRepository.save(any(Outcome.class)))
            .thenAnswer(inv -> inv.getArgument(0));
        lenient()
            .when(outcomeMapper.toDto(any(Outcome.class)))
            .thenAnswer(inv -> {
                Outcome o = inv.getArgument(0);
                OutcomeDTO dto = new OutcomeDTO();
                dto.setId(o.getId());
                dto.setTitle(o.getTitle());
                dto.setStatus(o.getStatus());
                dto.setSortOrder(o.getSortOrder());
                dto.setCreatedDate(o.getCreatedDate());
                dto.setLastModifiedDate(o.getLastModifiedDate());
                return dto;
            });
    }

    @Test
    void createSetsServerFieldsIgnoringClientValues() {
        Instant clientDate = Instant.parse("1999-01-01T00:00:00Z");
        OutcomeDTO input = createDto();
        input.setCreatedDate(clientDate);
        input.setLastModifiedDate(clientDate);
        input.setSortOrder(999);
        when(outcomeRepository.findMaxSortOrderByProductId(PRODUCT_ID)).thenReturn(1);
        Instant before = Instant.now();

        service.save(input);

        Outcome saved = captureSaved();
        assertThat(saved.getCreatedDate()).isAfterOrEqualTo(before).isNotEqualTo(clientDate);
        assertThat(saved.getLastModifiedDate()).isEqualTo(saved.getCreatedDate());
        assertThat(saved.getSortOrder()).isEqualTo(2);
    }

    @Test
    void createRejectsWhenProductMissing() {
        OutcomeDTO input = createDto();
        input.setProduct(null);

        assertThatThrownBy(() -> service.save(input)).isInstanceOf(NodeWriteRuleException.class);
    }

    @Test
    void createRejectsWhenProductDoesNotExist() {
        OutcomeDTO input = createDto();
        ProductDTO missing = new ProductDTO();
        missing.setId(9999L);
        input.setProduct(missing);
        when(productRepository.findById(9999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.save(input)).isInstanceOf(NodeWriteRuleException.class);
    }

    @Test
    void createDeniedForViewer() {
        doThrow(new TeamAccessDeniedException()).when(teamAccessService).requireEditProduct(PRODUCT_ID);
        OutcomeDTO input = createDto();

        assertThatThrownBy(() -> service.save(input)).isInstanceOf(TeamAccessDeniedException.class);
    }

    @Test
    void updatePreservesCreatedDateAndSetsLastModifiedDate() {
        Instant originalCreated = Instant.parse("2020-01-01T00:00:00Z");
        Outcome existing = new Outcome().id(OUTCOME_ID);
        existing.setProduct(product);
        existing.setStatus(OutcomeStatus.DRAFT);
        existing.setSortOrder(0);
        existing.setCreatedDate(originalCreated);
        when(outcomeRepository.findById(OUTCOME_ID)).thenReturn(Optional.of(existing));

        OutcomeDTO input = createDto();
        input.setId(OUTCOME_ID);
        input.setCreatedDate(Instant.parse("1999-06-01T00:00:00Z"));
        Instant before = Instant.now();

        service.update(input);

        Outcome saved = captureSaved();
        assertThat(saved.getCreatedDate()).isEqualTo(originalCreated);
        assertThat(saved.getLastModifiedDate()).isAfterOrEqualTo(before);
    }

    @Test
    void updateStatusIsAllowed() {
        Outcome existing = new Outcome().id(OUTCOME_ID);
        existing.setProduct(product);
        existing.setStatus(OutcomeStatus.DRAFT);
        existing.setSortOrder(0);
        existing.setCreatedDate(Instant.parse("2020-01-01T00:00:00Z"));
        when(outcomeRepository.findById(OUTCOME_ID)).thenReturn(Optional.of(existing));

        OutcomeDTO input = createDto();
        input.setId(OUTCOME_ID);
        input.setStatus(OutcomeStatus.ACHIEVED);

        service.update(input);

        Outcome saved = captureSaved();
        assertThat(saved.getStatus()).isEqualTo(OutcomeStatus.ACHIEVED);
    }

    private OutcomeDTO createDto() {
        OutcomeDTO dto = new OutcomeDTO();
        dto.setTitle("An outcome");
        dto.setStatus(OutcomeStatus.DRAFT);
        dto.setSortOrder(0);
        ProductDTO p = new ProductDTO();
        p.setId(PRODUCT_ID);
        dto.setProduct(p);
        return dto;
    }

    private Outcome captureSaved() {
        ArgumentCaptor<Outcome> captor = ArgumentCaptor.forClass(Outcome.class);
        org.mockito.Mockito.verify(outcomeRepository).save(captor.capture());
        return captor.getValue();
    }
}
