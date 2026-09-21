package com.opportunity.tree.service.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.opportunity.tree.repository.McpNodeReadRepository;
import com.opportunity.tree.service.TeamAccessDeniedException;
import com.opportunity.tree.service.TeamAccessService;
import com.opportunity.tree.service.mcp.dto.SearchHit;
import com.opportunity.tree.service.mcp.dto.SearchPage;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link SearchTool} that lock in the ticket's acceptance criteria without a
 * database — team scoping, cross-team refusal, pagination, empty-result behaviour, minimum
 * query length and read-only guarantees. Integration coverage (real JPQL, cross-team search
 * that must return nothing) lives in {@code SearchToolIT}.
 */
@ExtendWith(MockitoExtension.class)
class SearchToolTest {

    private static final Long TEAM_A = 100L;
    private static final Long TEAM_B = 200L;

    @Mock
    private TeamAccessService teamAccessService;

    @Mock
    private McpNodeReadRepository readRepository;

    private SearchTool tool;

    @BeforeEach
    void setUp() {
        tool = new SearchTool(teamAccessService, readRepository);
        // Default: empty results across every type; individual tests override what matters.
        lenient().when(readRepository.searchProducts(any(), anyString())).thenReturn(List.of());
        lenient().when(readRepository.searchOutcomes(any(), anyString())).thenReturn(List.of());
        lenient().when(readRepository.searchOpportunities(any(), anyString())).thenReturn(List.of());
        lenient().when(readRepository.searchSolutions(any(), anyString())).thenReturn(List.of());
        lenient().when(readRepository.searchAssumptions(any(), anyString())).thenReturn(List.of());
        lenient().when(readRepository.searchEvidence(any(), anyString())).thenReturn(List.of());
    }

    @Test
    void searchNodes_returnsHitsFromCallerTeamsMerged() {
        when(teamAccessService.getCurrentUserTeamIds()).thenReturn(Set.of(TEAM_A));
        when(readRepository.searchOpportunities(Set.of(TEAM_A), "%signup%")).thenReturn(
            oneRow("OPPORTUNITY", 40L, "Signup friction", "New users bounce at signup", "OPEN", TEAM_A, "Alpha")
        );

        SearchPage page = tool.searchNodes("Signup", null, null, null);

        assertThat(page.total()).isEqualTo(1);
        assertThat(page.hits())
            .singleElement()
            .satisfies(hit -> {
                assertThat(hit.type()).isEqualTo("OPPORTUNITY");
                assertThat(hit.id()).isEqualTo(40L);
                assertThat(hit.title()).isEqualTo("Signup friction");
                assertThat(hit.description()).isEqualTo("New users bounce at signup");
                assertThat(hit.teamId()).isEqualTo(TEAM_A);
            });
        assertThat(page.hasMore()).isFalse();
    }

    @Test
    void searchNodes_withTeamIdScopesToThatTeam() {
        when(teamAccessService.getCurrentUserTeamIds()).thenReturn(Set.of(TEAM_A, TEAM_B));
        when(readRepository.searchProducts(Set.of(TEAM_A), "%check%")).thenReturn(
            oneRow("PRODUCT", 10L, "Checkout", "d", null, TEAM_A, "Alpha")
        );

        SearchPage page = tool.searchNodes("check", TEAM_A, null, null);

        assertThat(page.hits()).extracting(SearchHit::teamId).containsOnly(TEAM_A);
    }

    @Test
    void searchNodes_withTeamIdNotAMember_isRefusedWithTeamAccessDenied() {
        when(teamAccessService.getCurrentUserTeamIds()).thenReturn(Set.of(TEAM_A));

        assertThatThrownBy(() -> tool.searchNodes("check", TEAM_B, null, null)).isInstanceOf(TeamAccessDeniedException.class);
    }

    @Test
    void searchNodes_withNoQuery_isRefused() {
        assertThatThrownBy(() -> tool.searchNodes(null, null, null, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tool.searchNodes("", null, null, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tool.searchNodes(" a ", null, null, null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void searchNodes_forCallerInNoTeamAndNoTeamId_returnsEmptyPage() {
        when(teamAccessService.getCurrentUserTeamIds()).thenReturn(Set.of());

        SearchPage page = tool.searchNodes("anything", null, null, null);

        assertThat(page.total()).isZero();
        assertThat(page.hits()).isEmpty();
        assertThat(page.hasMore()).isFalse();
    }

    @Test
    void searchNodes_paginatesLargeResultSetAndReportsHasMore() {
        when(teamAccessService.getCurrentUserTeamIds()).thenReturn(Set.of(TEAM_A));
        // 45 opportunities all matching the same term.
        List<Object[]> many = new java.util.ArrayList<>(45);
        for (int i = 0; i < 45; i++) {
            many.add(row("OPPORTUNITY", (long) (1000 + i), String.format("Signup friction %03d", i), "d", "OPEN", TEAM_A, "Alpha"));
        }
        when(readRepository.searchOpportunities(Set.of(TEAM_A), "%signup%")).thenReturn(many);

        SearchPage first = tool.searchNodes("signup", null, 0, 20);
        SearchPage second = tool.searchNodes("signup", null, 20, 20);
        SearchPage third = tool.searchNodes("signup", null, 40, 20);

        assertThat(first.total()).isEqualTo(45);
        assertThat(first.hits()).hasSize(20);
        assertThat(first.hasMore()).isTrue();
        assertThat(second.hits()).hasSize(20);
        assertThat(second.hasMore()).isTrue();
        assertThat(third.hits()).hasSize(5);
        assertThat(third.hasMore()).isFalse();
    }

    @Test
    void searchNodes_capsLimitAtMaxAndDefaultsWhenMissing() {
        when(teamAccessService.getCurrentUserTeamIds()).thenReturn(Set.of(TEAM_A));

        // Enormous requested limit is capped at MAX_LIMIT.
        SearchPage capped = tool.searchNodes("term", null, null, 10_000);
        assertThat(capped.limit()).isEqualTo(SearchTool.MAX_LIMIT);

        // Missing limit falls back to DEFAULT_LIMIT.
        SearchPage def = tool.searchNodes("term", null, null, null);
        assertThat(def.limit()).isEqualTo(SearchTool.DEFAULT_LIMIT);
    }

    @Test
    void searchNodes_lowercasesQueryForCaseInsensitiveMatch() {
        when(teamAccessService.getCurrentUserTeamIds()).thenReturn(Set.of(TEAM_A));
        when(readRepository.searchOpportunities(Set.of(TEAM_A), "%signup%")).thenReturn(
            oneRow("OPPORTUNITY", 40L, "Signup friction", "d", "OPEN", TEAM_A, "Alpha")
        );

        SearchPage page = tool.searchNodes("SiGnUp", null, null, null);

        assertThat(page.hits()).hasSize(1);
    }

    @Test
    void toolDescription_documentsScopingAndPagination() {
        for (java.lang.reflect.Method m : SearchTool.class.getDeclaredMethods()) {
            org.springframework.ai.tool.annotation.Tool ann = m.getAnnotation(org.springframework.ai.tool.annotation.Tool.class);
            if (ann != null) {
                assertThat(ann.name()).isEqualTo("search_nodes");
                assertThat(ann.description()).contains("team").contains("paginat").containsIgnoringCase("read-only");
                return;
            }
        }
        throw new AssertionError("No @Tool annotation on SearchTool");
    }

    private static Object[] row(String type, Long id, String title, String desc, String status, Long teamId, String teamName) {
        return new Object[] { type, id, title, desc, status, teamId, teamName };
    }

    private static List<Object[]> oneRow(String type, Long id, String title, String desc, String status, Long teamId, String teamName) {
        List<Object[]> rows = new java.util.ArrayList<>();
        rows.add(row(type, id, title, desc, status, teamId, teamName));
        return rows;
    }
}
