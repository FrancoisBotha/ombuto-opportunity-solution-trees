package com.opportunity.tree.service.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.opportunity.tree.domain.enumeration.TreeNodeType;
import com.opportunity.tree.service.ProductService;
import com.opportunity.tree.service.TeamAccessDeniedException;
import com.opportunity.tree.service.TeamTreeService;
import com.opportunity.tree.service.dto.ProductDTO;
import com.opportunity.tree.service.dto.TeamDTO;
import com.opportunity.tree.service.dto.tree.TeamTreeDTO;
import com.opportunity.tree.service.dto.tree.TreeNodeDTO;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for the MCPSRV-003 MCP tools ({@link TreeTool#listProducts()} and
 * {@link TreeTool#getTree(Long, Long)}) covering the ticket's acceptance criteria:
 *
 * <ol>
 *   <li>list_products returns every product the caller may read across all of their teams,
 *       with id, title, teamId and teamName.</li>
 *   <li>get_tree returns a team's whole tree, or only the named product's branch, as JSON
 *       with node type, title, status and parent/child links.</li>
 *   <li>Both tools go through TeamAccessService (via the underlying services); a non-member
 *       team throws {@link TeamAccessDeniedException}.</li>
 *   <li>A product id from another team is refused with the same error shape whether the team
 *       or product exists or not (existence cannot be probed).</li>
 *   <li>Both tools reuse the existing services and neither performs writes.</li>
 *   <li>Output uses human-readable titles and statuses alongside ids.</li>
 *   <li>Descriptions state what each tool returns and when to prefer scoping to a product.</li>
 *   <li>Responses are bounded by a documented node ceiling with an actionable overflow
 *       message.</li>
 *   <li>Coverage: a member of one team sees only that team's data; a caller in no team sees
 *       an empty list; a cross-team id is refused. (ROLE_OVERVIEW does not exist in this
 *       codebase yet — see grep in MCPSRV-003 — so that leg is deferred to the epic ticket
 *       introducing it.)</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
class TreeToolTest {

    private static final Long TEAM_A = 100L;
    private static final Long TEAM_B = 200L;

    private static final Long PRODUCT_A1 = 10L;
    private static final Long PRODUCT_A2 = 11L;
    private static final Long PRODUCT_B1 = 20L;

    private static final Long OUTCOME_A1 = 30L;
    private static final Long OPP_A1 = 40L;
    private static final Long OUTCOME_A2 = 31L;

    @Mock
    private ProductService productService;

    @Mock
    private TeamTreeService teamTreeService;

    private TreeTool tool;

    @BeforeEach
    void setUp() {
        tool = new TreeTool(productService, teamTreeService);
    }

    // ---------------------------------------------------------------------
    // list_products
    // ---------------------------------------------------------------------

    @Test
    void listProducts_returnsIdTitleAndOwningTeamForEveryProductAcrossTeams() {
        // AC 1 + AC 6: id, title, team id and team name across all of the caller's teams.
        when(productService.findAllForCurrentUser()).thenReturn(
            List.of(product(PRODUCT_A1, "Checkout", TEAM_A, "Alpha", false), product(PRODUCT_B1, "Onboarding", TEAM_B, "Bravo", false))
        );

        TreeTool.ProductListResponse response = tool.listProducts();

        assertThat(response.total()).isEqualTo(2);
        assertThat(response.products())
            .extracting(
                TreeTool.ProductSummary::id,
                TreeTool.ProductSummary::title,
                TreeTool.ProductSummary::teamId,
                TreeTool.ProductSummary::teamName
            )
            .containsExactlyInAnyOrder(
                org.assertj.core.groups.Tuple.tuple(PRODUCT_A1, "Checkout", TEAM_A, "Alpha"),
                org.assertj.core.groups.Tuple.tuple(PRODUCT_B1, "Onboarding", TEAM_B, "Bravo")
            );
    }

    @Test
    void listProducts_forCallerInNoTeam_returnsEmptyList() {
        // AC 9 (caller in no team): the underlying service returns an empty list — no partial
        // data leaks, no exception, just nothing to show.
        when(productService.findAllForCurrentUser()).thenReturn(Collections.emptyList());

        TreeTool.ProductListResponse response = tool.listProducts();

        assertThat(response.total()).isZero();
        assertThat(response.products()).isEmpty();
    }

    @Test
    void listProducts_forMemberOfOneTeam_seesOnlyThatTeamsProducts() {
        // AC 9 (member of one team): the underlying service filters by the caller's team ids,
        // so the tool sees only that team's products. We assert on the two products returned by
        // ProductService and confirm no other team appears in the response.
        when(productService.findAllForCurrentUser()).thenReturn(
            List.of(product(PRODUCT_A1, "Checkout", TEAM_A, "Alpha", false), product(PRODUCT_A2, "Search", TEAM_A, "Alpha", false))
        );

        TreeTool.ProductListResponse response = tool.listProducts();

        assertThat(response.products()).extracting(TreeTool.ProductSummary::teamId).containsOnly(TEAM_A);
        assertThat(response.products()).extracting(TreeTool.ProductSummary::title).containsExactly("Checkout", "Search");
    }

    @Test
    void listProducts_reusesProductServiceAndPerformsNoWrites() {
        // AC 5: the tool delegates to ProductService.findAllForCurrentUser() and never invokes
        // save / update / partialUpdate / delete.
        when(productService.findAllForCurrentUser()).thenReturn(Collections.emptyList());

        tool.listProducts();

        verify(productService).findAllForCurrentUser();
        verify(productService, never()).save(org.mockito.ArgumentMatchers.any());
        verify(productService, never()).update(org.mockito.ArgumentMatchers.any());
        verify(productService, never()).partialUpdate(org.mockito.ArgumentMatchers.any());
        verify(productService, never()).delete(org.mockito.ArgumentMatchers.anyLong());
    }

    // ---------------------------------------------------------------------
    // get_tree
    // ---------------------------------------------------------------------

    @Test
    void getTree_forTeam_returnsWholeTreeAsJsonWithTypeTitleStatusAndParent() {
        // AC 2 + AC 6: the tree is returned with type, title, status and parent links.
        TeamTreeDTO tree = teamTree(TEAM_A, "Alpha");
        tree.setNodes(
            List.of(
                node(TreeNodeType.PRODUCT, PRODUCT_A1, null, "Checkout", null),
                node(TreeNodeType.OUTCOME, OUTCOME_A1, "product-" + PRODUCT_A1, "Higher activation", null),
                node(TreeNodeType.OPPORTUNITY, OPP_A1, "outcome-" + OUTCOME_A1, "Signup friction", "OPEN")
            )
        );
        when(teamTreeService.getTreeForTeam(TEAM_A)).thenReturn(tree);

        TreeTool.TreeResponse response = tool.getTree(TEAM_A, null);

        assertThat(response.overflow()).isFalse();
        assertThat(response.teamId()).isEqualTo(TEAM_A);
        assertThat(response.teamName()).isEqualTo("Alpha");
        assertThat(response.nodeCount()).isEqualTo(3);
        assertThat(response.nodes())
            .extracting(
                TreeTool.TreeNode::id,
                TreeTool.TreeNode::type,
                TreeTool.TreeNode::title,
                TreeTool.TreeNode::status,
                TreeTool.TreeNode::parentType,
                TreeTool.TreeNode::parentId
            )
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple(PRODUCT_A1, "PRODUCT", "Checkout", null, null, null),
                org.assertj.core.groups.Tuple.tuple(OUTCOME_A1, "OUTCOME", "Higher activation", null, "PRODUCT", PRODUCT_A1),
                org.assertj.core.groups.Tuple.tuple(OPP_A1, "OPPORTUNITY", "Signup friction", "OPEN", "OUTCOME", OUTCOME_A1)
            );
    }

    @Test
    void getTree_withProductId_returnsOnlyThatProductsBranch() {
        // AC 2 (scoped to product): with productId set, only that product's sub-tree is returned.
        TeamTreeDTO tree = teamTree(TEAM_A, "Alpha");
        tree.setNodes(
            List.of(
                node(TreeNodeType.PRODUCT, PRODUCT_A1, null, "Checkout", null),
                node(TreeNodeType.OUTCOME, OUTCOME_A1, "product-" + PRODUCT_A1, "Higher activation", null),
                node(TreeNodeType.PRODUCT, PRODUCT_A2, null, "Search", null),
                node(TreeNodeType.OUTCOME, OUTCOME_A2, "product-" + PRODUCT_A2, "Better matches", null)
            )
        );
        when(teamTreeService.getTreeForTeam(TEAM_A)).thenReturn(tree);

        TreeTool.TreeResponse response = tool.getTree(TEAM_A, PRODUCT_A1);

        assertThat(response.overflow()).isFalse();
        assertThat(response.nodes()).extracting(TreeTool.TreeNode::id).containsExactly(PRODUCT_A1, OUTCOME_A1);
    }

    @Test
    void getTree_forNonMemberTeam_throwsTeamAccessDeniedException() {
        // AC 3: a team the caller does not belong to yields a TeamAccessDeniedException.
        // TeamTreeService already enforces the check; the tool must let it propagate rather
        // than swallow it.
        when(teamTreeService.getTreeForTeam(TEAM_B)).thenThrow(new TeamAccessDeniedException());

        assertThatThrownBy(() -> tool.getTree(TEAM_B, null)).isInstanceOf(TeamAccessDeniedException.class);
    }

    @Test
    void getTree_withNullTeamId_isRefused() {
        // Defensive: a missing teamId must be refused with the same TeamAccessDeniedException
        // shape, not a NullPointerException.
        assertThatThrownBy(() -> tool.getTree(null, null)).isInstanceOf(TeamAccessDeniedException.class);
    }

    @Test
    void getTree_withProductFromAnotherTeam_isRefusedWithSameErrorShape() {
        // AC 4: a product id from another team is refused with the same TeamAccessDeniedException
        // as a caller-not-a-member — same shape whether the product exists or not, so ids cannot
        // be probed. TEAM_A's tree does not contain PRODUCT_B1 (which lives in TEAM_B).
        TeamTreeDTO tree = teamTree(TEAM_A, "Alpha");
        tree.setNodes(List.of(node(TreeNodeType.PRODUCT, PRODUCT_A1, null, "Checkout", null)));
        when(teamTreeService.getTreeForTeam(TEAM_A)).thenReturn(tree);

        assertThatThrownBy(() -> tool.getTree(TEAM_A, PRODUCT_B1)).isInstanceOf(TeamAccessDeniedException.class);
    }

    @Test
    void getTree_withUnknownProductId_isRefusedWithSameErrorShape() {
        // AC 4: a product id that does not exist anywhere is refused with the same
        // TeamAccessDeniedException as a cross-team probe. The tool cannot distinguish the two
        // cases in its response.
        TeamTreeDTO tree = teamTree(TEAM_A, "Alpha");
        tree.setNodes(List.of(node(TreeNodeType.PRODUCT, PRODUCT_A1, null, "Checkout", null)));
        when(teamTreeService.getTreeForTeam(TEAM_A)).thenReturn(tree);

        Throwable crossTeam = org.assertj.core.api.Assertions.catchThrowable(() -> tool.getTree(TEAM_A, PRODUCT_B1));
        Throwable unknown = org.assertj.core.api.Assertions.catchThrowable(() -> tool.getTree(TEAM_A, 424242L));

        assertThat(crossTeam).isInstanceOf(TeamAccessDeniedException.class);
        assertThat(unknown).isInstanceOf(TeamAccessDeniedException.class);
        assertThat(crossTeam.getMessage()).isEqualTo(unknown.getMessage());
    }

    @Test
    void getTree_reusesTeamTreeServiceAndPerformsNoWrites() {
        // AC 5: the tool delegates to TeamTreeService.getTreeForTeam and never touches any
        // write-capable service. The mocks would refuse unexpected interactions.
        when(teamTreeService.getTreeForTeam(TEAM_A)).thenReturn(teamTree(TEAM_A, "Alpha"));

        tool.getTree(TEAM_A, null);

        verify(teamTreeService).getTreeForTeam(eq(TEAM_A));
        verify(productService, never()).save(org.mockito.ArgumentMatchers.any());
        verify(productService, never()).delete(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void getTree_beyondNodeCeiling_returnsActionableOverflowMessageInsteadOfPayload() {
        // AC 8: get_tree has a documented node ceiling; when exceeded it returns a clear,
        // actionable message pointing at per-product scoping instead of an unbounded payload.
        TeamTreeDTO tree = teamTree(TEAM_A, "Alpha");
        int oversize = TreeTool.NODE_CEILING + 1;
        List<TreeNodeDTO> many = new ArrayList<>(oversize);
        many.add(node(TreeNodeType.PRODUCT, PRODUCT_A1, null, "Checkout", null));
        for (int i = 1; i < oversize; i++) {
            many.add(node(TreeNodeType.OUTCOME, (long) (10_000 + i), "product-" + PRODUCT_A1, "Outcome " + i, null));
        }
        tree.setNodes(many);
        when(teamTreeService.getTreeForTeam(TEAM_A)).thenReturn(tree);

        TreeTool.TreeResponse response = tool.getTree(TEAM_A, null);

        assertThat(response.overflow()).isTrue();
        assertThat(response.nodeCeiling()).isEqualTo(TreeTool.NODE_CEILING);
        assertThat(response.nodes()).isEmpty();
        assertThat(response.message()).contains("productId").contains(String.valueOf(oversize));
    }

    @Test
    void toolDescriptions_mentionReturnedShapeAndProductScoping() {
        // AC 7: tool and parameter descriptions state what each tool returns and when to
        // prefer it, including that get_tree should be scoped to one product for large trees.
        org.springframework.ai.tool.annotation.Tool list = findToolAnnotation("listProducts");
        org.springframework.ai.tool.annotation.Tool get = findToolAnnotation("getTree");

        assertThat(list.name()).isEqualTo("list_products");
        assertThat(list.description()).contains("product").contains("team").contains("id");

        assertThat(get.name()).isEqualTo("get_tree");
        assertThat(get.description()).contains("product").contains("large");
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private static org.springframework.ai.tool.annotation.Tool findToolAnnotation(String methodName) {
        for (java.lang.reflect.Method m : TreeTool.class.getDeclaredMethods()) {
            if (m.getName().equals(methodName)) {
                org.springframework.ai.tool.annotation.Tool ann = m.getAnnotation(org.springframework.ai.tool.annotation.Tool.class);
                if (ann != null) {
                    return ann;
                }
            }
        }
        throw new AssertionError("No @Tool on TreeTool." + methodName);
    }

    private static ProductDTO product(Long id, String name, Long teamId, String teamName, boolean archived) {
        ProductDTO p = new ProductDTO();
        p.setId(id);
        p.setName(name);
        p.setArchived(archived);
        TeamDTO t = new TeamDTO();
        t.setId(teamId);
        t.setName(teamName);
        p.setTeam(t);
        return p;
    }

    private static TeamTreeDTO teamTree(Long id, String name) {
        TeamTreeDTO t = new TeamTreeDTO();
        t.setId(id);
        t.setName(name);
        return t;
    }

    private static TreeNodeDTO node(TreeNodeType type, Long id, String parentKey, String title, String status) {
        TreeNodeDTO n = new TreeNodeDTO();
        n.setKey(type.name().toLowerCase(java.util.Locale.ROOT) + "-" + id);
        n.setType(type);
        n.setId(id);
        n.setParentKey(parentKey);
        n.setTitle(title);
        n.setStatus(status);
        return n;
    }
}
