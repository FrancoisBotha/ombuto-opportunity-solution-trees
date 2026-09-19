package com.opportunity.tree.web.rest;

import com.opportunity.tree.domain.enumeration.TreeNodeType;
import com.opportunity.tree.service.NodeWriteRuleException;
import java.util.Locale;

/**
 * Parses the {@code type} segment of {@code /api/tree/nodes/{type}/{id}/...}
 * case-insensitively (e.g. {@code opportunity} or {@code OPPORTUNITY}). An
 * unknown value is a 400 with error key {@code nodetypeinvalid}.
 */
final class TreeNodeTypePath {

    private TreeNodeTypePath() {}

    static TreeNodeType parse(String raw) {
        if (raw != null) {
            try {
                return TreeNodeType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                // fall through to the 400 below
            }
        }
        throw new NodeWriteRuleException("Unknown node type: " + raw, "treeNode", "nodetypeinvalid");
    }
}
