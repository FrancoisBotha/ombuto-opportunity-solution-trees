package com.opportunity.tree.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.security.web.util.matcher.RequestMatcher;

/** Selects browser document navigations that are safe to restore after authentication. */
final class NavigationRequestMatcher implements RequestMatcher {

    private static final String FETCH_MODE = "Sec-Fetch-Mode";

    @Override
    public boolean matches(HttpServletRequest request) {
        if (!HttpMethod.GET.matches(request.getMethod())) {
            return false;
        }

        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (isPathOrChild(path, "/api") || isPathOrChild(path, "/websocket")) {
            return false;
        }

        return "navigate".equalsIgnoreCase(request.getHeader(FETCH_MODE)) || acceptsHtml(request.getHeader(HttpHeaders.ACCEPT));
    }

    private boolean acceptsHtml(String accept) {
        if (accept == null) {
            return false;
        }
        try {
            return MediaType.parseMediaTypes(accept)
                .stream()
                .anyMatch(
                    mediaType ->
                        MediaType.TEXT_HTML.getType().equals(mediaType.getType()) &&
                        MediaType.TEXT_HTML.getSubtype().equals(mediaType.getSubtype())
                );
        } catch (InvalidMediaTypeException ignored) {
            return false;
        }
    }

    private boolean isPathOrChild(String path, String prefix) {
        return path.equals(prefix) || path.startsWith(prefix + "/");
    }
}
