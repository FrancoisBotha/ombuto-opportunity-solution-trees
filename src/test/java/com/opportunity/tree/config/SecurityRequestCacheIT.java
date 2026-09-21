package com.opportunity.tree.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.opportunity.tree.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@AutoConfigureMockMvc
@Transactional
class SecurityRequestCacheIT {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private RequestCache requestCache;

    @Test
    void sockJsInfoRequestIsNotThePostLoginDestination() throws Exception {
        MockHttpSession session = new MockHttpSession();

        mvc.perform(get("/websocket/tracker/info?t=1234").accept("*/*").session(session)).andExpect(status().isUnauthorized());

        assertThat(successfulLoginRedirect(session)).isEqualTo("/");
    }

    @Test
    void reconnectDoesNotReplaceAnExistingTreeDestination() throws Exception {
        MockHttpSession session = new MockHttpSession();
        saveNavigation(session, "/trees/42/canvas");

        mvc.perform(get("/websocket/tracker/info?t=5678").accept("*/*").session(session)).andExpect(status().isUnauthorized());

        assertThat(successfulLoginRedirect(session)).isEqualTo("http://localhost/trees/42/canvas?continue");
    }

    @Test
    void loginWithoutAPriorDestinationUsesTheApplicationLandingPage() throws Exception {
        assertThat(successfulLoginRedirect(new MockHttpSession())).isEqualTo("/");
    }

    private void saveNavigation(MockHttpSession session, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(80);
        request.setSession(session);
        request.addHeader(HttpHeaders.ACCEPT, "text/html");
        request.addHeader("Sec-Fetch-Mode", "navigate");
        requestCache.saveRequest(request, new MockHttpServletResponse());
    }

    private String successfulLoginRedirect(MockHttpSession session) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/login/success");
        request.setSession(session);
        MockHttpServletResponse response = new MockHttpServletResponse();

        SavedRequestAwareAuthenticationSuccessHandler successHandler = new SavedRequestAwareAuthenticationSuccessHandler();
        successHandler.setRequestCache(requestCache);
        successHandler.onAuthenticationSuccess(request, response, new TestingAuthenticationToken("user", "password", "ROLE_USER"));
        return response.getRedirectedUrl();
    }
}
