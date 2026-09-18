package com.opportunity.tree.service;

import java.io.Serial;

/**
 * Signals a rule violation from {@link TeamManagementService} that the caller can act on:
 * duplicate member, last-owner protection, unknown user, and so on. The controller layer
 * converts it into a 400 with the error key intact (never a 500). See TEAMS-002 AC 5/6.
 */
public class TeamManagementException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String errorKey;

    public TeamManagementException(String errorKey) {
        super(errorKey);
        this.errorKey = errorKey;
    }

    public String getErrorKey() {
        return errorKey;
    }
}
