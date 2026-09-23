package com.opportunity.tree.web.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.web.filter.OncePerRequestFilter;

public class SpaWebFilter extends OncePerRequestFilter {

    private final RequestCache requestCache;

    public SpaWebFilter(RequestCache requestCache) {
        this.requestCache = requestCache;
    }

    /**
     * Forwards any unmapped paths (except those containing a period) to the client {@code index.html}.
     *
     * <p>Because the forward hands the request off to the SPA before Spring Security's
     * {@code ExceptionTranslationFilter} can see it, a deep link such as {@code /trees/42/canvas}
     * would otherwise never be saved as the post-login destination. To keep deep links working,
     * this filter asks the injected {@link RequestCache} to save the incoming request before it
     * forwards to {@code /index.html}. The cache's matcher (see
     * {@code NavigationRequestMatcher}) already excludes {@code /api} and {@code /websocket} and
     * requires a real navigation, so background XHR and SockJS requests are unaffected.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        // Request URI includes the contextPath if any, removed it.
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (
            !path.startsWith("/api") &&
            !path.startsWith("/management") &&
            !path.startsWith("/v3/api-docs") &&
            !path.startsWith("/h2-console") &&
            !path.startsWith("/login") &&
            !path.startsWith("/oauth2") &&
            !path.startsWith("/websocket") &&
            !path.contains(".") &&
            path.matches("/(.*)")
        ) {
            requestCache.saveRequest(request, response);
            request.getRequestDispatcher("/index.html").forward(request, response);
            return;
        }

        filterChain.doFilter(request, response);
    }
}
