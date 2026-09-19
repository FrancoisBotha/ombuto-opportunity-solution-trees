package com.opportunity.tree.service;

import com.opportunity.tree.domain.Assumption;
import com.opportunity.tree.domain.Evidence;
import com.opportunity.tree.domain.NodeLink;
import com.opportunity.tree.domain.Opportunity;
import com.opportunity.tree.domain.Outcome;
import com.opportunity.tree.domain.Product;
import com.opportunity.tree.domain.Solution;
import com.opportunity.tree.domain.enumeration.TreeNodeType;
import com.opportunity.tree.repository.NodeLinkRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * The default links every new node gets (handoff REQUIREMENTS §2 / {@code rules.ts defaultLinks}):
 *
 * <ul>
 *   <li>Product — Product space</li>
 *   <li>Outcome, Assumption — Confluence</li>
 *   <li>Opportunity, Solution — Confluence, Jira Initiative, Jira Epic</li>
 *   <li>Evidence — Confluence, Jira Ticket</li>
 * </ul>
 *
 * URLs use the node key ({@code opportunity-12}) as the page id. Adding default links writes no
 * history. Used by node creation ({@link TreeNodeWriteService}) and product creation
 * ({@code ProductServiceImpl}).
 */
@Component
public class DefaultNodeLinks {

    public static final String BASE_URL = "https://ombuto.atlassian.net";

    /** One default link. */
    public record LinkSpec(String name, String url) {}

    private final NodeLinkRepository nodeLinkRepository;

    public DefaultNodeLinks(NodeLinkRepository nodeLinkRepository) {
        this.nodeLinkRepository = nodeLinkRepository;
    }

    /** The default links for a node of the given type and id, in display order. */
    public static List<LinkSpec> forNode(TreeNodeType type, Long id) {
        String key = TreeNodeRef.key(type, id);
        String confluence = BASE_URL + "/wiki/discovery/" + key;
        return switch (type) {
            case PRODUCT -> List.of(new LinkSpec("Product space", BASE_URL + "/wiki/spaces/" + key));
            case OUTCOME, ASSUMPTION -> List.of(new LinkSpec("Confluence", confluence));
            case EVIDENCE -> List.of(new LinkSpec("Confluence", confluence), new LinkSpec("Jira Ticket", BASE_URL + "/browse/DISC-000"));
            case OPPORTUNITY, SOLUTION -> List.of(
                new LinkSpec("Confluence", confluence),
                new LinkSpec("Jira Initiative", BASE_URL + "/browse/INIT-000"),
                new LinkSpec("Jira Epic", BASE_URL + "/browse/DISC-000")
            );
        };
    }

    /**
     * Persists the default links for a saved node entity (Product, Outcome, Opportunity, Solution,
     * Assumption or Evidence — it must already have an id).
     */
    public List<NodeLink> addDefaults(Object node) {
        TreeNodeType type = typeOf(node);
        Long id = idOf(node);
        Instant now = Instant.now();
        List<LinkSpec> specs = forNode(type, id);
        List<NodeLink> links = new ArrayList<>(specs.size());
        for (int i = 0; i < specs.size(); i++) {
            NodeLink link = new NodeLink().name(specs.get(i).name()).url(specs.get(i).url()).sortOrder(i).createdDate(now);
            switch (node) {
                case Product p -> link.setProduct(p);
                case Outcome o -> link.setOutcome(o);
                case Opportunity o -> link.setOpportunity(o);
                case Solution s -> link.setSolution(s);
                case Assumption a -> link.setAssumption(a);
                case Evidence e -> link.setEvidence(e);
                default -> throw new IllegalArgumentException("Not a tree node: " + node);
            }
            links.add(link);
        }
        return nodeLinkRepository.saveAll(links);
    }

    private static TreeNodeType typeOf(Object node) {
        return switch (node) {
            case Product p -> TreeNodeType.PRODUCT;
            case Outcome o -> TreeNodeType.OUTCOME;
            case Opportunity o -> TreeNodeType.OPPORTUNITY;
            case Solution s -> TreeNodeType.SOLUTION;
            case Assumption a -> TreeNodeType.ASSUMPTION;
            case Evidence e -> TreeNodeType.EVIDENCE;
            default -> throw new IllegalArgumentException("Not a tree node: " + node);
        };
    }

    private static Long idOf(Object node) {
        return switch (node) {
            case Product p -> p.getId();
            case Outcome o -> o.getId();
            case Opportunity o -> o.getId();
            case Solution s -> s.getId();
            case Assumption a -> a.getId();
            case Evidence e -> e.getId();
            default -> throw new IllegalArgumentException("Not a tree node: " + node);
        };
    }
}
