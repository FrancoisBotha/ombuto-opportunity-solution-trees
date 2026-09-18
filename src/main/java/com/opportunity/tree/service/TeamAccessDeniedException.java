package com.opportunity.tree.service;

import java.io.Serial;
import org.springframework.security.access.AccessDeniedException;

/**
 * Thrown by {@link TeamAccessService} enforcement methods when the current user
 * either has no membership in the requested team or the referenced resource does
 * not exist. Both cases raise the same exception so callers cannot infer resource
 * existence from the error response.
 *
 * <p>Extending Spring Security's {@link AccessDeniedException} causes the default
 * exception translation filter to render an HTTP 403 response, without leaking
 * whether the resource is missing or merely inaccessible.
 */
public class TeamAccessDeniedException extends AccessDeniedException {

    @Serial
    private static final long serialVersionUID = 1L;

    public TeamAccessDeniedException() {
        super("Access denied");
    }

    public TeamAccessDeniedException(String message) {
        super(message);
    }
}
