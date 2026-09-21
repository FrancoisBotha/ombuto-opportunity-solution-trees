package com.opportunity.tree.config.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.opportunity.tree.OpportunitySolutionTreeApp;
import com.opportunity.tree.config.AsyncSyncConfiguration;
import com.opportunity.tree.config.EmbeddedSQL;
import com.opportunity.tree.config.JacksonConfiguration;
import com.opportunity.tree.config.JacksonHibernateConfiguration;
import com.opportunity.tree.config.TestSecurityConfiguration;
import com.opportunity.tree.domain.Product;
import com.opportunity.tree.domain.Team;
import com.opportunity.tree.domain.TeamMember;
import com.opportunity.tree.domain.User;
import com.opportunity.tree.domain.enumeration.TeamRole;
import com.opportunity.tree.security.AuthoritiesConstants;
import com.opportunity.tree.web.rest.OstTreeTestCleanup;
import jakarta.persistence.EntityManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * MCPSRV-002 (Streamable HTTP form) — MCP session hijacking.
 *
 * <p>The Streamable HTTP transport routes a JSON-RPC request by its {@code Mcp-Session-Id}
 * header alone. Before {@link McpSessionPrincipalFilter}, any authenticated user could POST a
 * {@code tools/call} carrying <em>somebody else's</em> session id and have the tool run under
 * the poster's identity while the result was written to the victim's response stream. This test
 * drives the transport over real HTTP with two identities and proves the owner check refuses
 * every cross-principal request that carries a session id — including a percent-encoded path
 * variant that used to bypass an earlier fix in the SSE transport.
 *
 * <p>{@code sessionOwnerCanStillPostToTheirOwnSession} is the positive control: the binding must
 * not break the legitimate client.
 */
@SpringBootTest(
    classes = {
        OpportunitySolutionTreeApp.class,
        JacksonConfiguration.class,
        AsyncSyncConfiguration.class,
        TestSecurityConfiguration.class,
        JacksonHibernateConfiguration.class,
    },
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@EmbeddedSQL
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestPropertySource(
    properties = {
        "spring.ai.mcp.server.enabled=true",
        "spring.ai.mcp.server.name=opportunity-solution-tree-mcp",
        "spring.ai.mcp.server.version=0.0.1",
        "spring.ai.mcp.server.type=SYNC",
        "spring.ai.mcp.server.stdio=false",
        "spring.ai.mcp.server.protocol=STREAMABLE",
        "spring.ai.mcp.server.streamable-http.mcp-endpoint=/mcp",
        "spring.ai.mcp.server.capabilities.tool=true",
        "spring.ai.mcp.server.capabilities.resource=false",
        "spring.ai.mcp.server.capabilities.prompt=false",
        "spring.ai.mcp.server.capabilities.completion=false",
    }
)
class McpSessionHijackIT {

    private static final String TOKEN_PREFIX = "valid.mcpsrv002.";
    private static final String SESSION_ID_HEADER = McpSessionPrincipalFilter.SESSION_ID_HEADER;
    private static final Duration WAIT = Duration.ofSeconds(15);

    @LocalServerPort
    private int port;

    @Autowired
    private EntityManager em;

    @Autowired
    private PlatformTransactionManager txManager;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    private McpSessionRegistry sessionRegistry;

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    private TransactionTemplate tx;
    private String victimLogin;
    private String attackerLogin;
    private Long victimTeamId;
    private Long attackerTeamId;
    private String attackerSecretMarker;

    @BeforeEach
    void seed() {
        reset(jwtDecoder);
        when(jwtDecoder.decode(anyString())).thenAnswer(inv -> {
            String tokenValue = inv.getArgument(0);
            if (tokenValue == null || !tokenValue.startsWith(TOKEN_PREFIX)) {
                throw new BadJwtException("Not a recognised test token: " + tokenValue);
            }
            return jwtFor(tokenValue.substring(TOKEN_PREFIX.length()));
        });

        tx = new TransactionTemplate(txManager);
        String suffix = "-" + UUID.randomUUID().toString().substring(0, 8);
        victimLogin = "victim" + suffix;
        attackerLogin = "attacker" + suffix;
        attackerSecretMarker = "AttackerSECRET" + suffix.replace("-", "");

        tx.executeWithoutResult(status -> {
            Team victimTeam = persistTeam("victim-team" + suffix);
            Team attackerTeam = persistTeam("attacker-team" + suffix);
            persistMembership(victimTeam, persistUser(victimLogin), TeamRole.OWNER);
            persistMembership(attackerTeam, persistUser(attackerLogin), TeamRole.OWNER);
            persistProduct(victimTeam, "VictimVisibleProduct" + suffix);
            persistProduct(attackerTeam, attackerSecretMarker + "Product");
            em.flush();
            victimTeamId = victimTeam.getId();
            attackerTeamId = attackerTeam.getId();
        });
    }

    @AfterEach
    void cleanUp() {
        OstTreeTestCleanup.removeTeamsAndUsers(
            txManager,
            em,
            Arrays.asList(victimTeamId, attackerTeamId),
            Arrays.asList(victimLogin, attackerLogin)
        );
    }

    @Test
    void crossPrincipalRequest_carryingVictimSessionId_isRefused() throws Exception {
        String victimSession = initializeSessionFor(victimLogin);

        // The attacker's own token is perfectly valid — only the session is not theirs.
        HttpResponse<String> hijack = postTo(
            "/mcp",
            attackerLogin,
            Optional.of(victimSession),
            "{\"jsonrpc\":\"2.0\",\"id\":4242,\"method\":\"tools/call\"," + "\"params\":{\"name\":\"list_products\",\"arguments\":{}}}"
        );

        assertThat(hijack.statusCode())
            .as("a request carrying a session id the caller did not open must be refused, " + "indistinguishably from an unknown session")
            .isEqualTo(404);
        assertThat(hijack.body()).as("no data from the attacker's team may leak in the refusal body").doesNotContain(attackerSecretMarker);
    }

    @Test
    void crossPrincipalRequest_percentEncodedPath_stillCannotBypassTheOwnerCheck() throws Exception {
        String victimSession = initializeSessionFor(victimLogin);

        // The dispatcher resolves /mc%70 to the same handler as /mcp. In the old SSE transport
        // the owner check keyed off the raw request URI and a percent-encoded path let the check
        // be skipped. The Streamable HTTP filter keys off the Mcp-Session-Id header, not the
        // path, so no path spelling can bypass it.
        HttpResponse<String> hijack = postTo(
            "/mc%70",
            attackerLogin,
            Optional.of(victimSession),
            "{\"jsonrpc\":\"2.0\",\"id\":4343,\"method\":\"tools/call\"," + "\"params\":{\"name\":\"list_products\",\"arguments\":{}}}"
        );

        assertThat(hijack.statusCode()).as("a percent-encoded path that resolves to /mcp must not bypass the owner check").isEqualTo(404);
        assertThat(hijack.body()).doesNotContain(attackerSecretMarker);
    }

    @Test
    void unknownSessionId_isRefusedTheSameWay() throws Exception {
        HttpResponse<String> response = postTo(
            "/mcp",
            attackerLogin,
            Optional.of(UUID.randomUUID().toString()),
            "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/call\",\"params\":{\"name\":\"ping\",\"arguments\":{}}}"
        );
        assertThat(response.statusCode())
            .as("an unknown session id and a hijacked session id must be indistinguishable to the caller")
            .isEqualTo(404);
    }

    @Test
    void sessionOwnerCanStillPostToTheirOwnSession() throws Exception {
        String victimSession = initializeSessionFor(victimLogin);

        HttpResponse<String> own = postTo(
            "/mcp",
            victimLogin,
            Optional.of(victimSession),
            "{\"jsonrpc\":\"2.0\",\"id\":7,\"method\":\"tools/call\"," + "\"params\":{\"name\":\"list_products\",\"arguments\":{}}}"
        );
        assertThat(own.statusCode()).as("the session's own owner must still be served").isEqualTo(200);
        assertThat(own.body()).contains("VictimVisibleProduct");
        assertThat(own.body()).doesNotContain(attackerSecretMarker);
    }

    @Test
    void initializeBindsTheSessionToTheCallerThatOpenedIt() throws Exception {
        String session = initializeSessionFor(victimLogin);

        assertThat(sessionRegistry.isOwnedBy(session, victimLogin)).isTrue();
        assertThat(sessionRegistry.isOwnedBy(session, attackerLogin)).isFalse();
    }

    @Test
    void deleteMcpTerminatesTheSessionAndReleasesTheBinding() throws Exception {
        int sizeBefore = sessionRegistry.size();
        String session = initializeSessionFor(victimLogin);
        assertThat(sessionRegistry.size()).as("initialize adds one binding").isEqualTo(sizeBefore + 1);
        assertThat(sessionRegistry.isOwnedBy(session, victimLogin)).isTrue();

        HttpResponse<String> terminate = deleteTo("/mcp", victimLogin, Optional.of(session));
        assertThat(terminate.statusCode()).as("DELETE /mcp is the transport's session-termination request").isLessThan(500);

        assertThat(sessionRegistry.size()).as("DELETE /mcp must release the session-to-principal binding").isEqualTo(sizeBefore);
        assertThat(sessionRegistry.isOwnedBy(session, victimLogin)).isFalse();

        // A follow-up request under the terminated id must be refused as an unknown session.
        HttpResponse<String> afterDelete = postTo(
            "/mcp",
            victimLogin,
            Optional.of(session),
            "{\"jsonrpc\":\"2.0\",\"id\":9,\"method\":\"tools/call\"," + "\"params\":{\"name\":\"list_products\",\"arguments\":{}}}"
        );
        assertThat(afterDelete.statusCode()).as("a terminated session id must not be routable").isEqualTo(404);
    }

    private HttpResponse<String> deleteTo(String pathAndQuery, String login, Optional<String> sessionId) throws Exception {
        HttpRequest.Builder rb = HttpRequest.newBuilder(URI.create("http://localhost:" + port + pathAndQuery))
            .header("Authorization", "Bearer " + TOKEN_PREFIX + login)
            .header("Accept", "application/json, text/event-stream")
            .timeout(WAIT)
            .DELETE();
        sessionId.ifPresent(id -> rb.header(SESSION_ID_HEADER, id));
        return http.send(rb.build(), HttpResponse.BodyHandlers.ofString());
    }

    // ---------------------------------------------------------------------
    // Streamable HTTP helpers (initialize -> Mcp-Session-Id header)
    // ---------------------------------------------------------------------

    /** Performs a JSON-RPC initialize as {@code login} and returns the minted session id. */
    private String initializeSessionFor(String login) throws Exception {
        HttpResponse<String> init = postTo(
            "/mcp",
            login,
            Optional.empty(),
            "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\"," +
                "\"params\":{\"protocolVersion\":\"2025-06-18\"," +
                "\"capabilities\":{},\"clientInfo\":{\"name\":\"hijack-it\",\"version\":\"1.0\"}}}"
        );
        assertThat(init.statusCode()).as("initialize response for %s", login).isEqualTo(200);
        String sessionId = init.headers().firstValue(SESSION_ID_HEADER).orElse(null);
        assertThat(sessionId).as("initialize response carries the %s header", SESSION_ID_HEADER).isNotBlank();
        // Send the notifications/initialized so the server considers the session ready.
        postTo("/mcp", login, Optional.of(sessionId), "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}");
        return sessionId;
    }

    private HttpResponse<String> postTo(String pathAndQuery, String login, Optional<String> sessionId, String jsonRpc) throws Exception {
        HttpRequest.Builder rb = HttpRequest.newBuilder(URI.create("http://localhost:" + port + pathAndQuery))
            .header("Authorization", "Bearer " + TOKEN_PREFIX + login)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json, text/event-stream")
            .timeout(WAIT)
            .POST(HttpRequest.BodyPublishers.ofString(jsonRpc));
        sessionId.ifPresent(id -> rb.header(SESSION_ID_HEADER, id));
        return http.send(rb.build(), HttpResponse.BodyHandlers.ofString());
    }

    // ---------------------------------------------------------------------
    // Fixtures
    // ---------------------------------------------------------------------

    private Jwt jwtFor(String login) {
        return Jwt.withTokenValue(TOKEN_PREFIX + login)
            .header("alg", "RS256")
            .claim("sub", "user-" + login)
            .claim("preferred_username", login)
            .claim("roles", List.of(AuthoritiesConstants.USER))
            // MCPSRV-007: the MCP filter chain refuses tokens without the `mcp-server` audience.
            .audience(List.of("mcp-server"))
            .issuedAt(Instant.now().minusSeconds(60))
            .expiresAt(Instant.now().plusSeconds(300))
            .build();
    }

    private Team persistTeam(String name) {
        Team t = new Team().name(name).description("d").createdDate(Instant.now());
        em.persist(t);
        return t;
    }

    private User persistUser(String login) {
        User u = new User();
        u.setId(UUID.randomUUID().toString());
        u.setLogin(login);
        u.setActivated(true);
        u.setEmail(login + "@example.com");
        u.setFirstName(login);
        u.setLastName("test");
        u.setLangKey("en");
        em.persist(u);
        return u;
    }

    private void persistMembership(Team t, User u, TeamRole role) {
        TeamMember tm = new TeamMember().role(role).joinedDate(Instant.now());
        tm.setTeam(t);
        tm.setUser(u);
        em.persist(tm);
    }

    private void persistProduct(Team team, String name) {
        Product p = new Product().name(name).description("d").archived(false).sortOrder(0).createdDate(Instant.now()).team(team);
        em.persist(p);
    }
}
