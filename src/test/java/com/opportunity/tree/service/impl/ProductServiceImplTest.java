package com.opportunity.tree.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.opportunity.tree.domain.Product;
import com.opportunity.tree.domain.Team;
import com.opportunity.tree.repository.ProductRepository;
import com.opportunity.tree.service.DefaultNodeLinks;
import com.opportunity.tree.service.NodeWriteRuleException;
import com.opportunity.tree.service.TeamAccessDeniedException;
import com.opportunity.tree.service.TeamAccessService;
import com.opportunity.tree.service.dto.ProductDTO;
import com.opportunity.tree.service.dto.TeamDTO;
import com.opportunity.tree.service.mapper.ProductMapper;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for TREE-002 write rules on {@link ProductServiceImpl}: a Product
 * requires a team parent, {@code createdDate} is server-set on create and
 * preserved on update, and the archived flag can be flipped through update.
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    private static final Long TEAM_ID = 1L;
    private static final Long PRODUCT_ID = 10L;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private TeamAccessService teamAccessService;

    private ProductMapper productMapper;

    private ProductServiceImpl service;

    private Team team;

    @BeforeEach
    void setUp() {
        productMapper = mock(ProductMapper.class);
        service = new ProductServiceImpl(productRepository, productMapper, teamAccessService, mock(DefaultNodeLinks.class));

        team = new Team();
        team.setId(TEAM_ID);

        lenient()
            .when(productMapper.toEntity(any(ProductDTO.class)))
            .thenAnswer(inv -> {
                ProductDTO dto = inv.getArgument(0);
                Product p = new Product();
                p.setId(dto.getId());
                p.setName(dto.getName());
                p.setArchived(dto.getArchived());
                p.setCreatedDate(dto.getCreatedDate());
                p.setTeam(team);
                return p;
            });
        lenient()
            .when(productRepository.save(any(Product.class)))
            .thenAnswer(inv -> inv.getArgument(0));
        lenient()
            .when(productMapper.toDto(any(Product.class)))
            .thenAnswer(inv -> {
                Product p = inv.getArgument(0);
                ProductDTO dto = new ProductDTO();
                dto.setId(p.getId());
                dto.setName(p.getName());
                dto.setArchived(p.getArchived());
                dto.setCreatedDate(p.getCreatedDate());
                return dto;
            });
    }

    @Test
    void createRequiresATeamParent() {
        ProductDTO input = createDto();
        input.setTeam(null);

        assertThatThrownBy(() -> service.save(input)).isInstanceOf(NodeWriteRuleException.class);
    }

    @Test
    void createSetsCreatedDateServerSideIgnoringClientValue() {
        Instant clientDate = Instant.parse("1999-01-01T00:00:00Z");
        ProductDTO input = createDto();
        input.setCreatedDate(clientDate);
        Instant before = Instant.now();

        service.save(input);

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        org.mockito.Mockito.verify(productRepository).save(captor.capture());
        assertThat(captor.getValue().getCreatedDate()).isAfterOrEqualTo(before).isNotEqualTo(clientDate);
    }

    @Test
    void createDeniedForViewer() {
        doThrow(new TeamAccessDeniedException()).when(teamAccessService).requireEditTeam(TEAM_ID);
        ProductDTO input = createDto();

        assertThatThrownBy(() -> service.save(input)).isInstanceOf(TeamAccessDeniedException.class);
    }

    @Test
    void updatePreservesCreatedDateAndAllowsArchivedFlagChange() {
        Instant originalCreated = Instant.parse("2020-01-01T00:00:00Z");
        Product existing = new Product();
        existing.setId(PRODUCT_ID);
        existing.setName("Original");
        existing.setArchived(Boolean.FALSE);
        existing.setCreatedDate(originalCreated);
        existing.setTeam(team);
        when(productRepository.findById(eq(PRODUCT_ID))).thenReturn(Optional.of(existing));

        ProductDTO input = createDto();
        input.setId(PRODUCT_ID);
        input.setArchived(Boolean.TRUE);
        input.setCreatedDate(Instant.parse("1999-06-01T00:00:00Z"));

        service.update(input);

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        org.mockito.Mockito.verify(productRepository).save(captor.capture());
        assertThat(captor.getValue().getCreatedDate()).isEqualTo(originalCreated);
        assertThat(captor.getValue().getArchived()).isTrue();
    }

    private ProductDTO createDto() {
        ProductDTO dto = new ProductDTO();
        dto.setName("A product");
        dto.setArchived(Boolean.FALSE);
        TeamDTO t = new TeamDTO();
        t.setId(TEAM_ID);
        dto.setTeam(t);
        return dto;
    }
}
