package com.opportunity.tree.service.mcp;

import com.opportunity.tree.domain.enumeration.TreeNodeType;
import com.opportunity.tree.service.ProductService;
import com.opportunity.tree.service.TeamAccessDeniedException;
import com.opportunity.tree.service.TeamTreeService;
import com.opportunity.tree.service.TreeNodeRef;
import com.opportunity.tree.service.dto.ProductDTO;
import com.opportunity.tree.service.dto.tree.TeamTreeDTO;
import com.opportunity.tree.service.dto.tree.TreeNodeDTO;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only MCP tools that expose the opportunity solution tree to an authenticated agent.
 *
 * <p>Both tools resolve the caller from the {@code SecurityContext} populated by the MCP
 * bearer-token filter chain (see {@code McpSecurityConfiguration}) and go through
 * {@code TeamAccessService} for every team they touch (the checks live inside the two services
 * this tool delegates to), so an agent only ever sees data of teams the caller belongs to — a
 * team the caller does not belong to yields a {@link TeamAccessDeniedException}, not partial
 * data, and unknown ids are refused with the same shape so ids cannot be probed for existence
 * (NFR-002).
 *
 * <p>The tools reuse the existing {@link ProductService#findAllForCurrentUser()} and
 * {@link TeamTreeService#getTreeForTeam(Long)} assemblers rather than issuing their own
 * queries; neither performs any create, update or delete (NFR-019).
 */
@Service
@Transactional(readOnly = true)
public class TreeTool {

    /**
     * Maximum number of nodes returned in a single {@code get_tree} response. Larger trees
     * must be requested one product at a time via the {@code productId} parameter — this
     * bounds the payload so an agent's context does not blow up on a big organisation's
     * tree (NFR-021).
     */
    static final int NODE_CEILING = 1000;

    private final ProductService productService;
    private final TeamTreeService teamTreeService;
    private final McpProductTreeReader productTreeReader;

    @Autowired
    public TreeTool(ProductService productService, TeamTreeService teamTreeService, McpProductTreeReader productTreeReader) {
        this.productService = productService;
        this.teamTreeService = teamTreeService;
        this.productTreeReader = productTreeReader;
    }

    public TreeTool(ProductService productService, TeamTreeService teamTreeService) {
        this(productService, teamTreeService, null);
    }

    @Tool(
        name = "list_products",
        description = "List every product the caller may read across all of their teams, each with its " +
            "id, title and owning team's id and name. Prefer this tool first to discover the ids you need " +
            "before calling get_tree. Read-only; performs no writes."
    )
    public ProductListResponse listProducts() {
        List<ProductDTO> products = productService.findAllForCurrentUser();
        List<ProductSummary> summaries = new ArrayList<>(products.size());
        for (ProductDTO p : products) {
            Long teamId = p.getTeam() == null ? null : p.getTeam().getId();
            String teamName = p.getTeam() == null ? null : p.getTeam().getName();
            summaries.add(new ProductSummary(p.getId(), p.getName(), teamId, teamName, Boolean.TRUE.equals(p.getArchived())));
        }
        summaries.sort(
            Comparator.<ProductSummary, String>comparing(
                s -> s.teamName() == null ? "" : s.teamName(),
                String.CASE_INSENSITIVE_ORDER
            ).thenComparing(s -> s.title() == null ? "" : s.title(), String.CASE_INSENSITIVE_ORDER)
        );
        return new ProductListResponse(summaries.size(), summaries);
    }

    @Tool(
        name = "get_tree",
        description = "Return a team's opportunity solution tree — products, outcomes, opportunities, " +
            "solutions, assumptions and evidence — as structured JSON. Each node carries its type, title, " +
            "description, status, parent id/type, priority (opportunity 1-100, higher first), valueRating " +
            "(opportunity 1-5), confidence (assumption 0-100%, lower is less certain), ownerLogin and dates. " +
            "Non-applicable fields are null. Rank all opportunities, including nested ones, by descending " +
            "priority and include every tie. Product pages have up to 1000 nodes in stable type, " +
            "sortOrder, id order; use offset + nodeCount while hasMore is true. Parent references preserve " +
            "hierarchy across pages. For large team trees, overflow requires scoping by product. Use " +
            "search_nodes to find a node when you do not already know its id or team. Call list_products " +
            "first to discover the ids. Read-only; performs no writes."
    )
    public TreeResponse getTree(
        @ToolParam(description = "The id of the team whose tree to return. The caller must be a member of this team.") Long teamId,
        @ToolParam(
            required = false,
            description = "Optional product id. When set, only that product's branch is returned. Use this to scope large trees."
        ) Long productId,
        @ToolParam(
            required = false,
            description = "Zero-based offset for product-scoped pages of up to 1000 nodes. Follow hasMore by adding nodeCount to offset."
        ) Integer offset
    ) {
        if (teamId == null) {
            throw new TeamAccessDeniedException();
        }
        int effectiveOffset = offset == null || offset < 0 ? 0 : offset;
        if (productTreeReader != null) {
            String teamName = productTreeReader.requireTeamName(teamId);
            if (productId != null) {
                productTreeReader.requireProductInTeam(productId, teamId);
                McpProductTreeReader.ProductPage page = productTreeReader.readProduct(productId, effectiveOffset, NODE_CEILING);
                return new TreeResponse(
                    teamId,
                    teamName,
                    page.nodes().size(),
                    NODE_CEILING,
                    false,
                    null,
                    page.nodes(),
                    page.total(),
                    effectiveOffset,
                    (long) effectiveOffset + page.nodes().size() < page.total()
                );
            }
            if (effectiveOffset != 0) {
                throw new IllegalArgumentException("offset requires productId");
            }
            long total = productTreeReader.countTeamNodes(teamId);
            if (total > NODE_CEILING) {
                return new TreeResponse(
                    teamId,
                    teamName,
                    0,
                    NODE_CEILING,
                    true,
                    "Team tree exceeds 1000 nodes. Use list_products and page each product with get_tree(productId, offset).",
                    List.of(),
                    total,
                    0,
                    false
                );
            }
        }
        // Enforces membership through TeamAccessService inside getTreeForTeam: throws
        // TeamAccessDeniedException whether the team exists or not.
        TeamTreeDTO tree = teamTreeService.getTreeForTeam(teamId);

        List<TreeNodeDTO> nodes = tree.getNodes();

        if (productId != null) {
            // Cross-team probe defence: if the product does not belong to this team (whether
            // it exists in another team or nowhere at all) we throw the same
            // TeamAccessDeniedException the team check would.
            String productKey = TreeNodeRef.key(TreeNodeType.PRODUCT, productId);
            boolean productBelongsToTeam = nodes.stream().anyMatch(n -> productKey.equals(n.getKey()));
            if (!productBelongsToTeam) {
                throw new TeamAccessDeniedException();
            }
            nodes = descendantsIncluding(nodes, productKey);
        }

        if (nodes.size() > NODE_CEILING) {
            String message =
                "This team's tree has " +
                nodes.size() +
                " nodes, which exceeds the get_tree ceiling of " +
                NODE_CEILING +
                " nodes. Call get_tree with a productId to scope the response to one product. " +
                "Use list_products to discover the ids.";
            return new TreeResponse(tree.getId(), tree.getName(), 0, NODE_CEILING, true, message, List.of());
        }

        List<TreeNode> mapped = new ArrayList<>(nodes.size());
        for (TreeNodeDTO n : nodes) {
            mapped.add(project(n));
        }
        return new TreeResponse(tree.getId(), tree.getName(), mapped.size(), NODE_CEILING, false, null, mapped);
    }

    /** Compatibility for in-process callers; the MCP schema exposes the offset parameter. */
    public TreeResponse getTree(Long teamId, Long productId) {
        return getTree(teamId, productId, null);
    }

    /**
     * Returns the sub-tree rooted at {@code rootKey} in pre-order. The input list is already
     * in pre-order (see {@code TeamTreeService#preOrder}), so we walk it once, adding a node
     * whenever its parent has been retained.
     */
    private static List<TreeNodeDTO> descendantsIncluding(List<TreeNodeDTO> preOrder, String rootKey) {
        Set<String> retained = new HashSet<>();
        List<TreeNodeDTO> out = new ArrayList<>();
        for (TreeNodeDTO n : preOrder) {
            if (rootKey.equals(n.getKey())) {
                retained.add(n.getKey());
                out.add(n);
                continue;
            }
            if (n.getParentKey() != null && retained.contains(n.getParentKey())) {
                retained.add(n.getKey());
                out.add(n);
            }
        }
        return out;
    }

    private static TreeNode project(TreeNodeDTO n) {
        String parentType = null;
        Long parentId = null;
        if (n.getParentKey() != null) {
            int dash = n.getParentKey().lastIndexOf('-');
            if (dash > 0 && dash < n.getParentKey().length() - 1) {
                try {
                    parentType = n.getParentKey().substring(0, dash).toUpperCase(Locale.ROOT);
                    parentId = Long.parseLong(n.getParentKey().substring(dash + 1));
                } catch (NumberFormatException ignored) {
                    parentType = null;
                    parentId = null;
                }
            }
        }
        List<String> labels = null;
        if (
            n.getType() == com.opportunity.tree.domain.enumeration.TreeNodeType.OPPORTUNITY ||
            n.getType() == com.opportunity.tree.domain.enumeration.TreeNodeType.SOLUTION
        ) {
            labels =
                n.getTags() == null
                    ? List.of()
                    : n
                          .getTags()
                          .stream()
                          .map(t -> t.name())
                          .toList();
        }
        return new TreeNode(
            n.getId(),
            n.getType() == null ? null : n.getType().name(),
            n.getTitle(),
            n.getNotes(),
            n.getStatus(),
            parentType,
            parentId,
            n.getPriority(),
            n.getValueRating(),
            n.getConfidence(),
            n.getOwnerLogin(),
            n.getCreatedDate(),
            n.getLastModifiedDate(),
            labels
        );
    }

    // ---------------------------------------------------------------------
    // Response records — small, human-readable projections that give the agent
    // ids and titles side by side so it can answer in the user's vocabulary.
    // ---------------------------------------------------------------------

    public record ProductListResponse(int total, List<ProductSummary> products) {}

    public record ProductSummary(Long id, String title, Long teamId, String teamName, boolean archived) {}

    public record TreeResponse(
        Long teamId,
        String teamName,
        int nodeCount,
        int nodeCeiling,
        boolean overflow,
        String message,
        List<TreeNode> nodes,
        long totalNodes,
        int offset,
        boolean hasMore
    ) {
        public TreeResponse(
            Long teamId,
            String teamName,
            int nodeCount,
            int nodeCeiling,
            boolean overflow,
            String message,
            List<TreeNode> nodes
        ) {
            this(teamId, teamName, nodeCount, nodeCeiling, overflow, message, nodes, nodeCount, 0, false);
        }
    }

    public record TreeNode(
        Long id,
        String type,
        String title,
        String description,
        String status,
        String parentType,
        Long parentId,
        Integer priority,
        Integer valueRating,
        Integer confidence,
        String ownerLogin,
        Instant createdDate,
        Instant lastModifiedDate,
        /** LABEL-001: tag names for OPPORTUNITY / SOLUTION; null for other node types. */
        List<String> labels
    ) {
        public TreeNode(
            Long id,
            String type,
            String title,
            String description,
            String status,
            String parentType,
            Long parentId,
            Integer priority,
            Integer valueRating,
            Integer confidence,
            String ownerLogin,
            Instant createdDate,
            Instant lastModifiedDate
        ) {
            this(
                id,
                type,
                title,
                description,
                status,
                parentType,
                parentId,
                priority,
                valueRating,
                confidence,
                ownerLogin,
                createdDate,
                lastModifiedDate,
                null
            );
        }
    }
}
