package com.opportunity.tree.config.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.opportunity.tree.OpportunitySolutionTreeApp;
import com.opportunity.tree.config.AsyncSyncConfiguration;
import com.opportunity.tree.config.EmbeddedSQL;
import com.opportunity.tree.config.JacksonConfiguration;
import com.opportunity.tree.config.JacksonHibernateConfiguration;
import com.opportunity.tree.config.TestSecurityConfiguration;
import com.opportunity.tree.domain.Assumption;
import com.opportunity.tree.domain.Evidence;
import com.opportunity.tree.domain.Interview;
import com.opportunity.tree.domain.Opportunity;
import com.opportunity.tree.domain.Outcome;
import com.opportunity.tree.domain.Product;
import com.opportunity.tree.domain.Solution;
import com.opportunity.tree.domain.Team;
import com.opportunity.tree.domain.TeamMember;
import com.opportunity.tree.domain.User;
import com.opportunity.tree.domain.enumeration.AssumptionStatus;
import com.opportunity.tree.domain.enumeration.OpportunityStatus;
import com.opportunity.tree.domain.enumeration.SolutionStatus;
import com.opportunity.tree.domain.enumeration.TeamRole;
import com.opportunity.tree.security.AuthoritiesConstants;
import com.opportunity.tree.service.mcp.GetNodeTool;
import com.opportunity.tree.service.mcp.ListInterviewsTool;
import com.opportunity.tree.service.mcp.ProbeTool;
import com.opportunity.tree.service.mcp.TreeTool;
import com.opportunity.tree.web.rest.OstTreeTestCleanup;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.persistence.EntityManager;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * MCPSRV-005: one cross-cutting integration test class, in the spirit of
 * {@code GeneratedEndpointsSecurityIT}, that drives the real MCP endpoint over the JSON-RPC
 * transport and proves the epic's security acceptance criteria hold for every tool together.
 *
 * <p>Runs the application on a real {@code RANDOM_PORT} so the tests can hit the actual MCP
 * transport (SSE handshake on {@code /mcp}, JSON-RPC messages on {@code /mcp/message}) through
 * the {@link io.modelcontextprotocol.client.transport.HttpClientSseClientTransport} the MCP SDK
 * ships. Each of alice (team A), bob (team B) and carol (no team) opens their own bearer-token
 * session and every cross-team assertion is made against the JSON-RPC response body returned by
 * the server &mdash; that is where "no field of B's data appears in the response" and
 * "refusal-for-foreign-id is indistinguishable from refusal-for-missing-id" become meaningful
 * at protocol level.
 *
 * <ul>
 *   <li>AC 1 &mdash; the cross-team tests drive {@code /mcp} over HTTP with three real users
 *       (alice team A, bob team B, carol no team) using per-user JWTs the mocked
 *       {@link JwtDecoder} resolves through the actual bearer-token filter chain.</li>
 *   <li>AC 2 &mdash; the MCP tool registry contains exactly the four tools plus the {@code ping}
 *       probe; the test fails if a new tool is added without updating the expected set.</li>
 *   <li>AC 3 &mdash; every registered MCP tool is read-only: row counts of every affected table
 *       are identical before and after invoking each tool.</li>
 *   <li>AC 4 &mdash; token-refusal cases (missing / malformed / expired / wrong-issuer /
 *       wrong-audience) hit {@code /mcp} over HTTP and are all refused with 401 with no tool
 *       invocation.</li>
 *   <li>AC 5 &amp; 6 &mdash; user A against user B's team, product, node and interview ids
 *       returns no field of B's data through the JSON-RPC transport, and the refusal shape is
 *       indistinguishable from a refusal for a non-existent id.</li>
 *   <li>AC 7 &mdash; the session-based OAuth2 login entrypoint, the CSRF token flow (both the
 *       positive-with-token case and the negative-without-token case) and a representative
 *       {@code /api} call all still work alongside the MCP chain.</li>
 *   <li>AC 8 &mdash; runs under the standard backend test command ({@code node mvnw.cjs
 *       verify}); introduces no new test framework (JUnit 5, AssertJ, Mockito, MockMvc and the
 *       MCP SDK client are already on the classpath).</li>
 * </ul>
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
@AutoConfigureMockMvc
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
class McpEndpointSecurityScopingIT {

    private static final Long NON_EXISTENT_ID = 987_654_321L;
    private static final String TOKEN_PREFIX = "valid.mcpsrv005.";

    @LocalServerPort
    private int port;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private EntityManager em;

    @Autowired
    private PlatformTransactionManager txManager;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    @Qualifier("syncTools")
    private List<McpServerFeatures.SyncToolSpecification> syncToolSpecifications;

    // Tool beans are spied so the token-refusal tests can assert that a rejected request
    // never reaches any tool. The tools themselves remain fully functional in every
    // authenticated test below; the spies just record interactions.
    @MockitoSpyBean
    private ProbeTool probeTool;

    @MockitoSpyBean
    private TreeTool treeTool;

    @MockitoSpyBean
    private GetNodeTool getNodeTool;

    @MockitoSpyBean
    private ListInterviewsTool listInterviewsTool;

    private TransactionTemplate tx;

    private Long teamAId;
    private Long teamBId;
    private String aliceLogin;
    private String bobLogin;
    private String carolLogin;
    private Long productAId;
    private Long productBId;
    private Long opportunityAId;
    private Long opportunityBId;
    private Long interviewAId;
    private Long interviewBId;
    private String bravoSecretMarker;

    @BeforeEach
    void resetMocksAndSeed() {
        reset(jwtDecoder, probeTool, treeTool, getNodeTool, listInterviewsTool);

        // The mocked JwtDecoder resolves per-token-value: `valid.mcpsrv005.<login>` -> JWT whose
        // `preferred_username` claim is `<login>`. The real JwtAuthenticationConverter maps that
        // claim to the security principal, so the same code path as the web login populates the
        // caller's identity.
        when(jwtDecoder.decode(anyString())).thenAnswer(inv -> {
            String tokenValue = inv.getArgument(0);
            if (tokenValue == null || !tokenValue.startsWith(TOKEN_PREFIX)) {
                throw new BadJwtException("Not a recognised test token: " + tokenValue);
            }
            String login = tokenValue.substring(TOKEN_PREFIX.length());
            return jwtFor(login);
        });

        tx = new TransactionTemplate(txManager);
        seed();
    }

    @AfterEach
    void cleanUp() {
        // Unique per-run suffixes stop this class colliding with itself, but they do not stop the
        // seeded rows leaking: this IT is not @Transactional, @DirtiesContext refreshes the Spring
        // context but NOT the Testcontainers Postgres instance, which is shared by every IT in the
        // JVM. The committed users, teams, products, outcomes and opportunities were therefore
        // visible to later tests and broke them — UserResourceIT and AccountResourceIT with
        // "fk_team_member__user_id" when they deleted their users, and the generated
        // Outcome/Opportunity/Solution/Interview resource ITs with wrong row counts. Remove exactly
        // what was seeded, in FK-safe order.
        OstTreeTestCleanup.removeTeamsAndUsers(
            txManager,
            em,
            Arrays.asList(teamAId, teamBId),
            Arrays.asList(aliceLogin, bobLogin, carolLogin)
        );
    }

    private void seed() {
        String suffix = "-" + UUID.randomUUID().toString().substring(0, 8);
        aliceLogin = "alice" + suffix;
        bobLogin = "bob" + suffix;
        carolLogin = "carol" + suffix;
        bravoSecretMarker = "BravoSECRET" + suffix.replace("-", "");
        String secretParticipant = bravoSecretMarker + "Participant";
        String secretNotes = bravoSecretMarker + "Notes";
        String secretInterviewTitle = bravoSecretMarker + "Interview";
        String secretOpportunityTitle = bravoSecretMarker + "Opportunity";
        String secretOutcomeTitle = bravoSecretMarker + "Outcome";
        String secretProductName = bravoSecretMarker + "Product";

        tx.executeWithoutResult(status -> {
            Team teamA = persistTeam("teamA" + suffix);
            Team teamB = persistTeam("teamB" + suffix);
            User alice = persistUser(aliceLogin);
            User bob = persistUser(bobLogin);
            User carol = persistUser(carolLogin);
            persistMembership(teamA, alice, TeamRole.OWNER);
            persistMembership(teamB, bob, TeamRole.OWNER);
            // carol deliberately belongs to no team — AC 1's third user.

            Product productA = persistProduct(teamA, "Alpha" + suffix, 0);
            Product productB = persistProduct(teamB, secretProductName, 0);

            Outcome outcomeA = persistOutcome(productA, "Grow WAU" + suffix);
            Opportunity opportunityA = persistOpportunity(outcomeA, "Onboarding is confusing" + suffix);
            Solution solutionA = persistSolution(opportunityA, "Interactive tour" + suffix);
            persistAssumption(solutionA, "Users will complete the tour");
            persistEvidence(opportunityA, "Interview snippet");

            Outcome outcomeB = persistOutcome(productB, secretOutcomeTitle);
            Opportunity opportunityB = persistOpportunity(outcomeB, secretOpportunityTitle);

            Interview interviewA = persistInterview(
                productA,
                "Alpha kickoff" + suffix,
                "Ava" + suffix,
                LocalDate.of(2026, 1, 15),
                "alpha notes",
                alice,
                Set.of(opportunityA)
            );
            Interview interviewB = persistInterview(
                productB,
                secretInterviewTitle,
                secretParticipant,
                LocalDate.of(2026, 2, 20),
                secretNotes,
                bob,
                Set.of(opportunityB)
            );
            em.flush();

            teamAId = teamA.getId();
            teamBId = teamB.getId();
            productAId = productA.getId();
            productBId = productB.getId();
            opportunityAId = opportunityA.getId();
            opportunityBId = opportunityB.getId();
            interviewAId = interviewA.getId();
            interviewBId = interviewB.getId();
        });
    }

    // ---------------------------------------------------------------------
    // AC 2 — tool registry contains exactly the four read-only tools + probe
    // ---------------------------------------------------------------------

    @Test
    void mcpToolRegistry_containsExactlyTheFourReadOnlyToolsPlusTheProbe() {
        List<String> registeredNames = syncToolSpecifications
            .stream()
            .map(s -> s.tool().name())
            .sorted()
            .toList();
        assertThat(registeredNames)
            .as("MCP tool registry — a new tool here must be added to the expected set on purpose")
            .containsExactlyInAnyOrder("ping", "list_products", "get_tree", "get_node", "list_interviews");
    }

    // ---------------------------------------------------------------------
    // AC 3 — every registered MCP tool is read-only
    // ---------------------------------------------------------------------

    @Test
    void everyRegisteredMcpTool_isReadOnly_databaseRowCountsUnchangedAfterInvocation() throws Exception {
        Map<String, Long> before = snapshotCounts();

        List<String> registered = syncToolSpecifications
            .stream()
            .map(s -> s.tool().name())
            .toList();
        assertThat(registered)
            .as("registered tool set must match AC 2 — update this test in tandem if a tool is added")
            .containsExactlyInAnyOrder("ping", "list_products", "get_tree", "get_node", "list_interviews");

        // Drive every registered tool through the real JSON-RPC transport as alice — this
        // guarantees the read-only assertion covers the full HTTP + principal resolution +
        // serialisation path, not just the bean layer.
        try (McpSyncClient client = openClientAs(aliceLogin)) {
            client.callTool(new McpSchema.CallToolRequest("ping", Map.of()));
            client.callTool(new McpSchema.CallToolRequest("list_products", Map.of()));
            client.callTool(new McpSchema.CallToolRequest("get_tree", Map.of("teamId", teamAId)));
            client.callTool(new McpSchema.CallToolRequest("get_node", Map.of("type", "PRODUCT", "id", productAId)));
            client.callTool(new McpSchema.CallToolRequest("list_interviews", Map.of("productId", productAId)));
        }

        Map<String, Long> after = snapshotCounts();
        assertThat(after).as("row counts unchanged after read-only tools").isEqualTo(before);
    }

    private Map<String, Long> snapshotCounts() {
        Map<String, Long> counts = new HashMap<>();
        for (String entity : List.of(
            "Team",
            "TeamMember",
            "Product",
            "Outcome",
            "Opportunity",
            "Solution",
            "Assumption",
            "Evidence",
            "Interview",
            "Comment",
            "NodeLink",
            "OpenQuestion",
            "User"
        )) {
            counts.put(entity, countOf(entity));
        }
        return counts;
    }

    // ---------------------------------------------------------------------
    // AC 4 — token failure modes are refused with 401 and no tool is invoked
    // ---------------------------------------------------------------------

    @Test
    void noAuthorizationHeader_isRefusedWith401_andNoToolIsInvoked() throws Exception {
        MvcResult result = mvc.perform(get("/mcp")).andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(401);
        assertNoToolWasInvoked();
    }

    @Test
    void malformedToken_isRefusedWith401_andNoToolIsInvoked() throws Exception {
        // Override the per-test stub with a specific decoder failure.
        reset(jwtDecoder);
        when(jwtDecoder.decode(anyString())).thenThrow(new BadJwtException("Malformed token"));

        MvcResult result = mvc.perform(get("/mcp").header("Authorization", "Bearer this-is-not-a-jwt")).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(401);
        assertNoToolWasInvoked();
    }

    @Test
    void expiredToken_isRefusedWith401_andNoToolIsInvoked() throws Exception {
        reset(jwtDecoder);
        when(jwtDecoder.decode(anyString())).thenThrow(
            new JwtValidationException("Jwt expired", List.of(new OAuth2Error("invalid_token", "Jwt expired at ...", null)))
        );

        MvcResult result = mvc.perform(get("/mcp").header("Authorization", "Bearer expired.token")).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(401);
        assertNoToolWasInvoked();
    }

    @Test
    void wrongIssuerToken_isRefusedWith401_andNoToolIsInvoked() throws Exception {
        reset(jwtDecoder);
        when(jwtDecoder.decode(anyString())).thenThrow(
            new JwtValidationException("Invalid issuer", List.of(new OAuth2Error("invalid_token", "The iss claim is not valid", null)))
        );

        MvcResult result = mvc.perform(get("/mcp").header("Authorization", "Bearer wrong.iss.token")).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(401);
        assertNoToolWasInvoked();
    }

    @Test
    void wrongAudienceToken_isRefusedWith401_andNoToolIsInvoked() throws Exception {
        reset(jwtDecoder);
        when(jwtDecoder.decode(anyString())).thenThrow(
            new JwtValidationException(
                "Invalid audience",
                List.of(new OAuth2Error("invalid_token", "The required audience is missing", null))
            )
        );

        MvcResult result = mvc.perform(get("/mcp").header("Authorization", "Bearer wrong.aud.token")).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(401);
        assertNoToolWasInvoked();
    }

    // ---------------------------------------------------------------------
    // AC 1, 5 & 6 — cross-team scoping via the real JSON-RPC transport
    // ---------------------------------------------------------------------

    @Test
    void tools_list_overHttp_asAlice_returnsExactlyTheFourToolsPlusProbe() {
        try (McpSyncClient client = openClientAs(aliceLogin)) {
            McpSchema.ListToolsResult result = client.listTools();
            assertThat(result.tools())
                .extracting(McpSchema.Tool::name)
                .as("tools/list served over HTTP JSON-RPC")
                .containsExactlyInAnyOrder("ping", "list_products", "get_tree", "get_node", "list_interviews");
        }
    }

    @Test
    void listProducts_asAliceOverHttp_returnsOnlyTeamAProducts_andLeaksNoTeamBField() {
        try (McpSyncClient client = openClientAs(aliceLogin)) {
            McpSchema.CallToolResult result = client.callTool(new McpSchema.CallToolRequest("list_products", Map.of()));
            String body = renderContent(result);
            assertThat(Boolean.TRUE.equals(result.isError())).as("list_products for a valid team member").isFalse();
            assertThat(body).as("alice's list_products response mentions team A product").contains("Alpha");
            assertThat(body).as("no field of team B data leaks to alice").doesNotContain(bravoSecretMarker);
            assertThat(body).doesNotContain(String.valueOf(productBId));
        }
    }

    @Test
    void listProducts_asBobOverHttp_returnsOnlyTeamBProducts_andLeaksNoTeamAField() {
        try (McpSyncClient client = openClientAs(bobLogin)) {
            McpSchema.CallToolResult result = client.callTool(new McpSchema.CallToolRequest("list_products", Map.of()));
            String body = renderContent(result);
            assertThat(Boolean.TRUE.equals(result.isError())).isFalse();
            // Bob sees his own product but not alice's team.
            assertThat(body).contains(bravoSecretMarker);
            assertThat(body).doesNotContain("Alpha");
            assertThat(body).doesNotContain(String.valueOf(productAId));
        }
    }

    @Test
    void listProducts_asCarolInNoTeamOverHttp_returnsAnEmptyList_leaksNoData() {
        try (McpSyncClient client = openClientAs(carolLogin)) {
            McpSchema.CallToolResult result = client.callTool(new McpSchema.CallToolRequest("list_products", Map.of()));
            String body = renderContent(result);
            assertThat(Boolean.TRUE.equals(result.isError())).isFalse();
            assertThat(body).doesNotContain("Alpha");
            assertThat(body).doesNotContain(bravoSecretMarker);
        }
    }

    @Test
    void getTree_asAliceForTeamBIdOverHttp_isRefused_andLeaksNoTeamBData() {
        try (McpSyncClient client = openClientAs(aliceLogin)) {
            String foreign = errorBody(client, "get_tree", Map.of("teamId", teamBId));
            String missing = errorBody(client, "get_tree", Map.of("teamId", NON_EXISTENT_ID));

            assertThat(foreign).doesNotContain(bravoSecretMarker);
            assertThat(foreign).doesNotContain(String.valueOf(productBId));
            // AC 6: refusal for foreign id is indistinguishable from refusal for missing id.
            assertThat(foreign).isEqualTo(missing);
        }
    }

    @Test
    void getTree_asBobForTeamAIdOverHttp_isRefused_andLeaksNoTeamAData() {
        try (McpSyncClient client = openClientAs(bobLogin)) {
            String foreign = errorBody(client, "get_tree", Map.of("teamId", teamAId));
            String missing = errorBody(client, "get_tree", Map.of("teamId", NON_EXISTENT_ID));

            assertThat(foreign).doesNotContain("Alpha");
            assertThat(foreign).doesNotContain(String.valueOf(teamAId));
            assertThat(foreign).isEqualTo(missing);
        }
    }

    @Test
    void getTree_asCarolInNoTeamOverHttp_isRefusedForBothTeams_withIndistinguishableMessages() {
        try (McpSyncClient client = openClientAs(carolLogin)) {
            String foreignA = errorBody(client, "get_tree", Map.of("teamId", teamAId));
            String foreignB = errorBody(client, "get_tree", Map.of("teamId", teamBId));
            String missing = errorBody(client, "get_tree", Map.of("teamId", NON_EXISTENT_ID));

            assertThat(foreignA).isEqualTo(missing);
            assertThat(foreignB).isEqualTo(missing);
            assertThat(foreignB).doesNotContain(bravoSecretMarker);
        }
    }

    @Test
    void getNode_asAliceForTeamBProductIdOverHttp_isRefused_andLeaksNoTeamBData() {
        try (McpSyncClient client = openClientAs(aliceLogin)) {
            String foreign = errorBody(client, "get_node", Map.of("type", "PRODUCT", "id", productBId));
            String missing = errorBody(client, "get_node", Map.of("type", "PRODUCT", "id", NON_EXISTENT_ID));

            assertThat(foreign).doesNotContain(bravoSecretMarker);
            assertThat(foreign).isEqualTo(missing);
        }
    }

    @Test
    void getNode_asAliceForTeamBOpportunityIdOverHttp_isRefused_andLeaksNoTeamBData() {
        try (McpSyncClient client = openClientAs(aliceLogin)) {
            String foreign = errorBody(client, "get_node", Map.of("type", "OPPORTUNITY", "id", opportunityBId));
            String missing = errorBody(client, "get_node", Map.of("type", "OPPORTUNITY", "id", NON_EXISTENT_ID));

            assertThat(foreign).doesNotContain(bravoSecretMarker);
            assertThat(foreign).isEqualTo(missing);
        }
    }

    @Test
    void getNode_asBobForTeamAProductIdOverHttp_isRefused_andLeaksNoTeamAData() {
        try (McpSyncClient client = openClientAs(bobLogin)) {
            String foreign = errorBody(client, "get_node", Map.of("type", "PRODUCT", "id", productAId));
            String missing = errorBody(client, "get_node", Map.of("type", "PRODUCT", "id", NON_EXISTENT_ID));

            assertThat(foreign).doesNotContain("Alpha");
            assertThat(foreign).isEqualTo(missing);
        }
    }

    @Test
    void getNode_asCarolInNoTeamOverHttp_isRefusedIdenticallyForRealAndFakeIds() {
        try (McpSyncClient client = openClientAs(carolLogin)) {
            String realA = errorBody(client, "get_node", Map.of("type", "PRODUCT", "id", productAId));
            String realB = errorBody(client, "get_node", Map.of("type", "PRODUCT", "id", productBId));
            String missing = errorBody(client, "get_node", Map.of("type", "PRODUCT", "id", NON_EXISTENT_ID));

            assertThat(realA).isEqualTo(missing);
            assertThat(realB).isEqualTo(missing);
            assertThat(realB).doesNotContain(bravoSecretMarker);
        }
    }

    @Test
    void listInterviews_asAliceForTeamBProductIdOverHttp_isRefused_andLeaksNoTeamBData() {
        try (McpSyncClient client = openClientAs(aliceLogin)) {
            String foreign = errorBody(client, "list_interviews", Map.of("productId", productBId));
            String missing = errorBody(client, "list_interviews", Map.of("productId", NON_EXISTENT_ID));

            assertThat(foreign).doesNotContain(bravoSecretMarker);
            assertThat(foreign).doesNotContain(String.valueOf(interviewBId));
            assertThat(foreign).isEqualTo(missing);
        }
    }

    @Test
    void listInterviews_asAliceForTeamBTeamIdOverHttp_isRefused() {
        try (McpSyncClient client = openClientAs(aliceLogin)) {
            String foreign = errorBody(client, "list_interviews", Map.of("teamId", teamBId));
            String missing = errorBody(client, "list_interviews", Map.of("teamId", NON_EXISTENT_ID));

            assertThat(foreign).doesNotContain(bravoSecretMarker);
            assertThat(foreign).isEqualTo(missing);
        }
    }

    @Test
    void listInterviews_asBobForTeamAProductIdOverHttp_isRefused() {
        try (McpSyncClient client = openClientAs(bobLogin)) {
            String foreign = errorBody(client, "list_interviews", Map.of("productId", productAId));
            String missing = errorBody(client, "list_interviews", Map.of("productId", NON_EXISTENT_ID));

            assertThat(foreign).doesNotContain("Alpha");
            assertThat(foreign).doesNotContain(String.valueOf(interviewAId));
            assertThat(foreign).isEqualTo(missing);
        }
    }

    @Test
    void listInterviews_asCarolInNoTeamOverHttp_isRefused() {
        try (McpSyncClient client = openClientAs(carolLogin)) {
            String realA = errorBody(client, "list_interviews", Map.of("productId", productAId));
            String realB = errorBody(client, "list_interviews", Map.of("productId", productBId));
            String missing = errorBody(client, "list_interviews", Map.of("productId", NON_EXISTENT_ID));

            assertThat(realA).isEqualTo(missing);
            assertThat(realB).isEqualTo(missing);
            assertThat(realB).doesNotContain(bravoSecretMarker);
        }
    }

    @Test
    void positiveControl_listInterviews_asAliceForHerOwnProductOverHttp_returnsHerOwnData() {
        // A positive control that keeps the negative assertions above meaningful — if the
        // tool always errored, every "no field of B" assertion would trivially pass.
        try (McpSyncClient client = openClientAs(aliceLogin)) {
            McpSchema.CallToolResult result = client.callTool(
                new McpSchema.CallToolRequest("list_interviews", Map.of("productId", productAId))
            );
            String body = renderContent(result);
            assertThat(Boolean.TRUE.equals(result.isError())).isFalse();
            assertThat(body).contains("Alpha kickoff");
            assertThat(body).doesNotContain(bravoSecretMarker);
        }
    }

    // ---------------------------------------------------------------------
    // AC 7 — session-based web login, CSRF token flow and a representative
    //        /api call still work while the MCP chain is active
    // ---------------------------------------------------------------------

    @Test
    void sessionBasedLogin_oauth2AuthorizationEndpoint_isNotInterceptedByMcpChain() throws Exception {
        // The session-based chain is what serves the OAuth2 login redirect
        // (/oauth2/authorization/oidc). If the MCP chain's @Order or securityMatcher were
        // misconfigured, this path would 401 out of the bearer-token chain. Instead it must
        // 3xx-redirect the browser at the IdP, proving the login flow is still reachable.
        MvcResult login = mvc.perform(get("/oauth2/authorization/oidc")).andReturn();
        int status = login.getResponse().getStatus();
        assertThat(status)
            .as("OAuth2 login redirect status — the MCP chain must not have intercepted /oauth2/authorization/**")
            .isBetween(300, 399);
        assertThat(login.getResponse().getRedirectedUrl()).as("redirect URL points to the IdP authorization endpoint").isNotBlank();
    }

    @Test
    @WithMockUser(username = "session-user-mcpsrv005", authorities = { AuthoritiesConstants.USER })
    void sessionBasedApi_authenticate_stillWorksAlongsideTheMcpChain() throws Exception {
        mvc.perform(get("/api/authenticate").accept(MediaType.APPLICATION_JSON)).andExpect(status().is(204));
    }

    @Test
    void sessionBasedApi_unauthenticated_isRefusedAsBefore_notLeakedByMcpChain() throws Exception {
        mvc.perform(get("/api/authenticate")).andExpect(status().is(401));
    }

    @Test
    @WithMockUser(username = "session-user-csrf-neg-mcpsrv005", authorities = { AuthoritiesConstants.ADMIN })
    void sessionBasedApi_stateChangingPost_withoutCsrfToken_isRefusedBySessionChain() throws Exception {
        // Negative CSRF leg: a state-changing POST without an X-XSRF-TOKEN header is refused with
        // 403 — proving the MCP chain's csrf(disable) did not weaken the session-based CSRF.
        mvc.perform(post("/api/account").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().is(403));
    }

    @Test
    @WithMockUser(username = "session-user-csrf-pos-mcpsrv005", authorities = { AuthoritiesConstants.ADMIN })
    void sessionBasedApi_stateChangingPost_withValidCsrfToken_reachesControllerAlongsideTheMcpChain() throws Exception {
        // Positive CSRF leg (AC 7): with a valid CSRF token, the same POST is admitted through
        // the CsrfFilter. `.with(csrf())` matches the CookieCsrfTokenRepository token the
        // SpaCsrfTokenRequestHandler expects. A configuration that rejected every POST would
        // fail this test — proving the CSRF flow "still works".
        MvcResult result = mvc.perform(post("/api/account").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}")).andReturn();

        assertThat(result.getResponse().getStatus())
            .as("with a valid CSRF token the request is not refused by CsrfFilter")
            .isNotEqualTo(403);
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private McpSyncClient openClientAs(String login) {
        String bearer = TOKEN_PREFIX + login;
        HttpClientStreamableHttpTransport transport = HttpClientStreamableHttpTransport.builder("http://localhost:" + port)
            .endpoint("/mcp")
            .requestBuilder(HttpRequest.newBuilder().header("Authorization", "Bearer " + bearer))
            .build();
        McpSyncClient client = McpClient.sync(transport)
            .requestTimeout(Duration.ofSeconds(30))
            .initializationTimeout(Duration.ofSeconds(30))
            .build();
        client.initialize();
        return client;
    }

    private String errorBody(McpSyncClient client, String tool, Map<String, Object> args) {
        try {
            McpSchema.CallToolResult result = client.callTool(new McpSchema.CallToolRequest(tool, args));
            assertThat(Boolean.TRUE.equals(result.isError()))
                .as("call to %s with %s must have failed for cross-team / missing id", tool, args)
                .isTrue();
            return renderContent(result);
        } catch (McpError e) {
            // JSON-RPC protocol-level error — normalise to the message so refusal shape is comparable.
            return "McpError:" + String.valueOf(e.getMessage());
        }
    }

    private static String renderContent(McpSchema.CallToolResult result) {
        StringBuilder sb = new StringBuilder();
        if (result.content() != null) {
            for (McpSchema.Content c : result.content()) {
                if (c instanceof McpSchema.TextContent tc) {
                    sb.append(tc.text());
                } else {
                    sb.append(c.toString());
                }
                sb.append('\n');
            }
        }
        if (result.structuredContent() != null) {
            sb.append(result.structuredContent().toString());
        }
        return sb.toString();
    }

    private void assertNoToolWasInvoked() {
        verifyNoInteractions(probeTool, treeTool, getNodeTool, listInterviewsTool);
    }

    private long countOf(String entityName) {
        Long r = tx.execute(status -> ((Number) em.createQuery("SELECT COUNT(x) FROM " + entityName + " x").getSingleResult()).longValue());
        return r == null ? 0L : r;
    }

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

    private Product persistProduct(Team team, String name, int sortOrder) {
        Product p = new Product().name(name).description("d").archived(false).sortOrder(sortOrder).createdDate(Instant.now()).team(team);
        em.persist(p);
        return p;
    }

    private Outcome persistOutcome(Product p, String title) {
        Outcome o = new Outcome().title(title).description("d").sortOrder(0).createdDate(Instant.now()).product(p);
        em.persist(o);
        return o;
    }

    private Opportunity persistOpportunity(Outcome o, String title) {
        Opportunity op = new Opportunity()
            .title(title)
            .description("d")
            .status(OpportunityStatus.UNEXPLORED)
            .valuerating(3)
            .priority(50)
            .sortOrder(0)
            .createdDate(Instant.now())
            .outcome(o);
        em.persist(op);
        return op;
    }

    private Solution persistSolution(Opportunity op, String title) {
        Solution s = new Solution()
            .title(title)
            .description("d")
            .status(SolutionStatus.CANDIDATE)
            .sortOrder(0)
            .createdDate(Instant.now())
            .opportunity(op);
        em.persist(s);
        return s;
    }

    private Assumption persistAssumption(Solution s, String statement) {
        Assumption a = new Assumption()
            .statement(statement)
            .description("d")
            .status(AssumptionStatus.UNTESTED)
            .confidence(40)
            .sortOrder(0)
            .createdDate(Instant.now())
            .solution(s);
        em.persist(a);
        return a;
    }

    private Evidence persistEvidence(Opportunity op, String title) {
        Evidence e = new Evidence().title(title).sortOrder(0).createdDate(Instant.now()).opportunity(op);
        em.persist(e);
        return e;
    }

    private Interview persistInterview(
        Product product,
        String title,
        String participant,
        LocalDate date,
        String notes,
        User interviewer,
        Set<Opportunity> opportunities
    ) {
        Interview i = new Interview()
            .title(title)
            .participant(participant)
            .interviewDate(date)
            .notes(notes)
            .createdDate(Instant.now())
            .product(product)
            .interviewer(interviewer);
        em.persist(i);
        for (Opportunity op : opportunities) {
            op.getInterviews().add(i);
            i.getOpportunities().add(op);
        }
        return i;
    }
}
