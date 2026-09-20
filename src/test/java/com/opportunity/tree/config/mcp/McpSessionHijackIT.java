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
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.mcp.server.webmvc.transport.WebMvcSseServerTransportProvider;
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
 * MCPSRV-002 — MCP session hijacking.
 *
 * <p>The SSE transport routes a JSON-RPC message by its {@code sessionId} alone. Before
 * {@link McpSessionPrincipalFilter}, <em>any</em> authenticated user could POST a {@code tools/call}
 * to {@code /mcp/message?sessionId=<someone else's session>}: it answered 200, the tool ran under
 * the poster's identity, and the result — data from a team the stream's owner is denied — was
 * delivered into the victim's SSE stream.
 *
 * <p>This test drives the transport over real HTTP with two identities:
 * <ul>
 *   <li>the victim opens the SSE stream, completes the MCP handshake and keeps reading;</li>
 *   <li>the attacker, with their own perfectly valid bearer token, posts a {@code tools/call} to the
 *   victim's session id.</li>
 * </ul>
 * The post must be refused (404, the same answer as an unknown session) <b>and</b> nothing may
 * reach the victim's stream. Reverting the {@code addFilterAfter(...)} line in
 * {@link McpSecurityConfiguration} makes both assertions fail: the post answers 200 and the
 * attacker's team data appears in the victim's stream.
 *
 * <p>{@code sessionOwnerCanStillCallToolsOnTheirOwnSession} is the positive control: the binding
 * must not break the legitimate client.
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
        "spring.ai.mcp.server.sse-endpoint=/mcp",
        "spring.ai.mcp.server.sse-message-endpoint=/mcp/message",
        "spring.ai.mcp.server.capabilities.tool=true",
        "spring.ai.mcp.server.capabilities.resource=false",
        "spring.ai.mcp.server.capabilities.prompt=false",
        "spring.ai.mcp.server.capabilities.completion=false",
    }
)
class McpSessionHijackIT {

    private static final String TOKEN_PREFIX = "valid.mcpsrv002.";
    private static final Pattern SESSION_ID = Pattern.compile("sessionId=([0-9a-fA-F-]{36})");
    private static final Duration WAIT = Duration.ofSeconds(15);
    /** How long we give the (refused) hijack to show up in the victim's stream before declaring it absent. */
    private static final Duration SILENCE_WINDOW = Duration.ofSeconds(3);

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

    @Autowired
    private WebMvcSseServerTransportProvider transportProvider;

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
    void crossPrincipalMessage_isRefused_andNothingReachesTheVictimStream() throws Exception {
        try (SseSession victim = openSse(victimLogin)) {
            victim.handshake();

            // The attacker's own token is perfectly valid — only the session is not theirs.
            HttpResponse<String> hijack = postMessage(
                attackerLogin,
                victim.sessionId,
                "{\"jsonrpc\":\"2.0\",\"id\":4242,\"method\":\"tools/call\"," + "\"params\":{\"name\":\"list_products\",\"arguments\":{}}}"
            );

            assertThat(hijack.statusCode())
                .as("a message posted to a session the caller did not open must be refused, " + "indistinguishably from an unknown session")
                .isEqualTo(404);

            List<String> delivered = victim.drainFor(SILENCE_WINDOW);
            String stream = String.join("\n", delivered);
            assertThat(stream).as("no response to the hijacked request may reach the victim's stream").doesNotContain("4242");
            assertThat(stream).as("no data from the attacker's team may reach the victim's stream").doesNotContain(attackerSecretMarker);
            assertThat(stream).doesNotContain(String.valueOf(attackerTeamId));
        }
    }

    @Test
    void crossPrincipalMessage_withPercentEncodedPath_isRefused_andNothingReachesTheVictimStream() throws Exception {
        try (SseSession victim = openSse(victimLogin)) {
            victim.handshake();

            // The dispatcher resolves /mcp/messag%65 to the same handler as /mcp/message, so the
            // transport would route this to the victim's session. The owner check must still fire.
            // Reverting the filter's sessionId-based detection (matching the raw path instead) lets
            // this answer 200 and leaks the poster's result into the victim's stream.
            HttpResponse<String> hijack = postMessageToPath(
                attackerLogin,
                "/mcp/messag%65?sessionId=" + victim.sessionId,
                "{\"jsonrpc\":\"2.0\",\"id\":4343,\"method\":\"tools/call\"," + "\"params\":{\"name\":\"list_products\",\"arguments\":{}}}"
            );

            assertThat(hijack.statusCode())
                .as("a percent-encoded message path that resolves to /mcp/message must not bypass the owner check")
                .isEqualTo(404);

            List<String> delivered = victim.drainFor(SILENCE_WINDOW);
            String stream = String.join("\n", delivered);
            assertThat(stream).as("no response to the encoded-path hijack may reach the victim's stream").doesNotContain("4343");
            assertThat(stream).as("no data from the attacker's team may reach the victim's stream").doesNotContain(attackerSecretMarker);
        }
    }

    @Test
    void unknownSessionId_isRefusedTheSameWay() throws Exception {
        HttpResponse<String> response = postMessage(
            attackerLogin,
            UUID.randomUUID().toString(),
            "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/call\",\"params\":{\"name\":\"ping\",\"arguments\":{}}}"
        );
        assertThat(response.statusCode()).isEqualTo(404);
    }

    @Test
    void outOfRangeNumericIdDoesNotLeakParserInternals() throws Exception {
        try (SseSession victim = openSse(victimLogin)) {
            victim.handshake();

            // A JSON integer literal one beyond Long.MAX_VALUE fails Spring AI argument binding.
            // On the old code the raw Jackson message ("out of range of `long`",
            // "StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION") is copied straight into the tool
            // result; McpToolErrorSanitizer rewrites it to a generic hint.
            HttpResponse<String> response = postMessage(
                victimLogin,
                victim.sessionId,
                "{\"jsonrpc\":\"2.0\",\"id\":515,\"method\":\"tools/call\"," +
                    "\"params\":{\"name\":\"get_node\",\"arguments\":{\"type\":\"PRODUCT\",\"id\":9223372036854776000}}}"
            );
            assertThat(response.statusCode()).isEqualTo(200);

            String result = victim.awaitLineContaining("\"id\":515");
            assertThat(result).as("the out-of-range id is reported as a tool error").contains("isError");
            assertThat(result)
                .as("no parser internals may leak in the tool error")
                .doesNotContain("StreamReadFeature")
                .doesNotContain("byte offset")
                .doesNotContainIgnoringCase("jackson")
                .doesNotContain("`long`");
        }
    }

    @Test
    void sessionOwnerCanStillCallToolsOnTheirOwnSession() throws Exception {
        try (SseSession victim = openSse(victimLogin)) {
            victim.handshake();

            HttpResponse<String> own = postMessage(
                victimLogin,
                victim.sessionId,
                "{\"jsonrpc\":\"2.0\",\"id\":7,\"method\":\"tools/call\"," + "\"params\":{\"name\":\"list_products\",\"arguments\":{}}}"
            );
            assertThat(own.statusCode()).as("the session's own owner must still be served").isEqualTo(200);

            String result = victim.awaitLineContaining("\"id\":7");
            assertThat(result).contains("VictimVisibleProduct");
            assertThat(result).doesNotContain(attackerSecretMarker);
        }
    }

    @Test
    void openSessionIsBoundToTheUserThatOpenedIt() throws Exception {
        try (SseSession victim = openSse(victimLogin)) {
            victim.handshake();
            assertThat(sessionRegistry.isOwnedBy(victim.sessionId, victimLogin)).isTrue();
            assertThat(sessionRegistry.isOwnedBy(victim.sessionId, attackerLogin)).isFalse();
        }
        // Release happens on the container's async-completion callback, which fires only once the
        // dropped connection is noticed; it is covered by McpSessionRegistryTest, not timed here.
    }

    // ---------------------------------------------------------------------
    // Raw SSE + JSON-RPC harness (the MCP SDK client does not expose the session id)
    // ---------------------------------------------------------------------

    private SseSession openSse(String login) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/mcp"))
            .header("Authorization", "Bearer " + TOKEN_PREFIX + login)
            .header("Accept", "text/event-stream")
            .timeout(WAIT)
            .GET()
            .build();
        HttpResponse<Stream<String>> response = http.send(request, HttpResponse.BodyHandlers.ofLines());
        assertThat(response.statusCode()).as("SSE handshake for %s", login).isEqualTo(200);
        SseSession session = new SseSession(login, response);
        session.start();
        return session;
    }

    private HttpResponse<String> postMessage(String login, String sessionId, String jsonRpc) throws Exception {
        return postMessageToPath(login, "/mcp/message?sessionId=" + sessionId, jsonRpc);
    }

    private HttpResponse<String> postMessageToPath(String login, String pathAndQuery, String jsonRpc) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + pathAndQuery))
            .header("Authorization", "Bearer " + TOKEN_PREFIX + login)
            .header("Content-Type", "application/json")
            .timeout(WAIT)
            .POST(HttpRequest.BodyPublishers.ofString(jsonRpc))
            .build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private final class SseSession implements AutoCloseable {

        private final String login;
        private final HttpResponse<Stream<String>> response;
        private final BlockingQueue<String> lines = new LinkedBlockingQueue<>();
        private Thread reader;
        private String sessionId;

        private SseSession(String login, HttpResponse<Stream<String>> response) {
            this.login = login;
            this.response = response;
        }

        void start() throws Exception {
            reader = new Thread(() -> {
                try {
                    response.body().forEach(lines::add);
                } catch (RuntimeException ignored) {
                    // stream closed
                }
            });
            reader.setDaemon(true);
            reader.start();
            String endpoint = awaitLineContaining("sessionId=");
            Matcher matcher = SESSION_ID.matcher(endpoint);
            assertThat(matcher.find()).as("endpoint event announces a session id: %s", endpoint).isTrue();
            sessionId = matcher.group(1);
        }

        /** Completes the MCP handshake as this session's own principal. */
        void handshake() throws Exception {
            String version = transportProvider.protocolVersions().get(0);
            HttpResponse<String> init = postMessage(
                login,
                sessionId,
                "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{\"protocolVersion\":\"" +
                    version +
                    "\",\"capabilities\":{},\"clientInfo\":{\"name\":\"hijack-it\",\"version\":\"1.0\"}}}"
            );
            assertThat(init.statusCode()).as("initialize for the session's own owner").isEqualTo(200);
            String initResult = awaitLineContaining("\"id\":1");
            assertThat(initResult).contains("protocolVersion");
            HttpResponse<String> initialized = postMessage(
                login,
                sessionId,
                "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}"
            );
            assertThat(initialized.statusCode()).isEqualTo(200);
        }

        String awaitLineContaining(String needle) throws InterruptedException {
            long deadline = System.nanoTime() + WAIT.toNanos();
            while (System.nanoTime() < deadline) {
                String line = lines.poll(500, TimeUnit.MILLISECONDS);
                if (line != null && line.contains(needle)) {
                    return line;
                }
            }
            throw new AssertionError("No SSE line containing '" + needle + "' arrived within " + WAIT);
        }

        /** Everything that arrives in the next {@code window} — expected to be empty (or keep-alives). */
        List<String> drainFor(Duration window) throws InterruptedException {
            List<String> collected = new ArrayList<>();
            long deadline = System.nanoTime() + window.toNanos();
            while (System.nanoTime() < deadline) {
                String line = lines.poll(200, TimeUnit.MILLISECONDS);
                if (line != null) {
                    collected.add(line);
                }
            }
            return collected;
        }

        @Override
        public void close() throws IOException {
            if (reader != null) {
                reader.interrupt();
            }
            response.body().close();
        }
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
