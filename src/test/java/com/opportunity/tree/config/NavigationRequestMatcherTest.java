package com.opportunity.tree.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;

class NavigationRequestMatcherTest {

    private final NavigationRequestMatcher matcher = new NavigationRequestMatcher();

    @Test
    void matchesTopLevelDocumentRequests() {
        assertThat(matches("/trees/42/canvas", "text/html,application/xhtml+xml", null)).isTrue();
        assertThat(matches("/trees/42/canvas", "*/*", "navigate")).isTrue();
    }

    @Test
    void rejectsApiWebsocketAndBackgroundRequests() {
        assertThat(matches("/websocket/tracker/info", "text/html", "navigate")).isFalse();
        assertThat(matches("/api/teams/42/tree", "text/html", "navigate")).isFalse();
        assertThat(matches("/trees/42/canvas", "application/json", "cors")).isFalse();
        assertThat(matches("/trees/42/canvas", "*/*", "cors")).isFalse();

        MockHttpServletRequest post = request("/trees/42/canvas", "text/html", "navigate");
        post.setMethod("POST");
        assertThat(matcher.matches(post)).isFalse();
    }

    private boolean matches(String path, String accept, String fetchMode) {
        return matcher.matches(request(path, accept, fetchMode));
    }

    private MockHttpServletRequest request(String path, String accept, String fetchMode) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.addHeader(HttpHeaders.ACCEPT, accept);
        if (fetchMode != null) {
            request.addHeader("Sec-Fetch-Mode", fetchMode);
        }
        return request;
    }
}
