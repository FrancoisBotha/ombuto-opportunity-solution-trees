package com.opportunity.tree.service;

import com.opportunity.tree.config.ApplicationProperties;
import com.opportunity.tree.domain.enumeration.TreeNodeType;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * The suggested default link slots per node type (LINK-001).
 *
 * <p>Historically this component wrote placeholder {@link com.opportunity.tree.domain.NodeLink}
 * rows on every new node (Jira {@code DISC-000}/{@code INIT-000} keys, Confluence pages that
 * did not exist). Those rows were indistinguishable from links a human had attached, so
 * {@code linkCount} meant "this node was created" rather than "this node is linked to
 * anything", and any agent that followed a key traced to a placeholder.
 *
 * <p>The scaffold itself is deliberate: the panel offers these names as add-buttons that open
 * the add-link form prefilled. Persistence is gone; a {@link com.opportunity.tree.domain.NodeLink}
 * row now means a human attached it.
 *
 * <p>The Atlassian base URL that used to build placeholder URLs is kept here as an instance
 * field read from {@link ApplicationProperties.DefaultLinks#getBaseUrl()} rather than a
 * hardcoded constant, so the tenant is no longer baked into code.
 */
@Component
public class DefaultNodeLinks {

    /** One default link slot the panel can offer as an add-button. */
    public record LinkSpec(String name, String url) {}

    private final String baseUrl;

    public DefaultNodeLinks(ApplicationProperties properties) {
        this.baseUrl = properties.getDefaultLinks().getBaseUrl();
    }

    /**
     * The default link slots for a node of the given type and id, in display order. Not
     * persisted at node creation (LINK-001); available for callers that need to compose a
     * placeholder URL suggestion.
     */
    public List<LinkSpec> forNode(TreeNodeType type, Long id) {
        String key = TreeNodeRef.key(type, id);
        String confluence = baseUrl + "/wiki/discovery/" + key;
        return switch (type) {
            case PRODUCT -> List.of(new LinkSpec("Product space", baseUrl + "/wiki/spaces/" + key));
            case OUTCOME, ASSUMPTION -> List.of(new LinkSpec("Confluence", confluence));
            case EVIDENCE -> List.of(new LinkSpec("Confluence", confluence), new LinkSpec("Jira Ticket", baseUrl + "/browse/DISC-000"));
            case OPPORTUNITY, SOLUTION -> List.of(
                new LinkSpec("Confluence", confluence),
                new LinkSpec("Jira Initiative", baseUrl + "/browse/INIT-000"),
                new LinkSpec("Jira Epic", baseUrl + "/browse/DISC-000")
            );
        };
    }
}
