package com.opportunity.tree.service;

import java.io.Serial;

/**
 * Signals a tree node write-rule violation (TREE-002) that the caller can act on: missing or
 * invalid parent, parent in another team, opportunity cycle. The controller layer converts it
 * into a 400 with the error key intact (never a 500).
 */
public class NodeWriteRuleException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String entityName;

    private final String errorKey;

    public NodeWriteRuleException(String message, String entityName, String errorKey) {
        super(message);
        this.entityName = entityName;
        this.errorKey = errorKey;
    }

    public String getEntityName() {
        return entityName;
    }

    public String getErrorKey() {
        return errorKey;
    }
}
