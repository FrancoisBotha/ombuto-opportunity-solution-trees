package com.opportunity.tree.service;

import com.opportunity.tree.domain.enumeration.TreeNodeType;
import java.util.Locale;

/**
 * Identifies one tree node by type and id — e.g. the node a link, open
 * question or comment hangs off. {@link #key()} is the client-side node key
 * used by the tree read ({@code "opportunity-12"}).
 */
public record TreeNodeRef(TreeNodeType type, Long id) {
    public String key() {
        return key(type, id);
    }

    public static String key(TreeNodeType type, Long id) {
        return type.name().toLowerCase(Locale.ROOT) + "-" + id;
    }
}
