# REST endpoint inventory

Source of truth: every `@RestController` under `com.opportunity.tree.web.rest`.
This inventory backs TEAMS-004 (NFR-001, NFR-002): every generated CRUD endpoint
whose entity is team-owned must either be restricted to `ROLE_ADMIN` at the
controller or route through `TeamAccessService` in the service layer.

## Legend

- **Owner scope** — how the entity is owned:
  - `team-owned` — belongs to a team (directly or transitively via Product /
    Outcome / Opportunity / Solution).
  - `platform` — global (users, authorities, auth/session plumbing).
- **Protection** — what enforces authorisation:
  - `ROLE_ADMIN` — locked at the controller with `@PreAuthorize("hasAuthority('ROLE_ADMIN')")`.
  - `TeamAccessService` — every read/write in the service goes through
    `TeamAccessService` (non-members get 403 without existence disclosure —
    NFR-002).
  - `authenticated` — any signed-in user; behaviour is intentionally public to
    all authenticated users.
  - `permitAll` / role from `SecurityConfiguration` — filter chain rule.

## Team-owned entities (generated CRUD)

Every generated CRUD resource except `ProductResource` carries a class-level
`@PreAuthorize(ROLE_ADMIN)`: they are the admin "Static Data" screens, and team
members work through the tree APIs below instead. All support GET, GET/{id},
POST, PUT/{id}, PATCH/{id} and DELETE/{id}. Outcome, Opportunity, Solution and
Interview also support GET /count.

| Controller             | Base path             | Owner scope                               | Protection                                                                                                                                                                                                                                        |
| ---------------------- | --------------------- | ----------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `TeamResource`         | `/api/teams`          | team-owned                                | `ROLE_ADMIN`                                                                                                                                                                                                                                      |
| `TeamMemberResource`   | `/api/team-members`   | team-owned                                | `ROLE_ADMIN`                                                                                                                                                                                                                                      |
| `ProductResource`      | `/api/products`       | team-owned                                | `TeamAccessService` (via `ProductServiceImpl`): the list is filtered to the caller's teams, and create / update / delete need OWNER or EDITOR. `sortOrder` is server-owned. DELETE cascades the whole product subtree (`TreeNodeCascadeService`). |
| `OutcomeResource`      | `/api/outcomes`       | team-owned (via Product)                  | `ROLE_ADMIN`                                                                                                                                                                                                                                      |
| `OpportunityResource`  | `/api/opportunities`  | team-owned (via Outcome / Opportunity)    | `ROLE_ADMIN`                                                                                                                                                                                                                                      |
| `SolutionResource`     | `/api/solutions`      | team-owned (via Opportunity)              | `ROLE_ADMIN`                                                                                                                                                                                                                                      |
| `AssumptionResource`   | `/api/assumptions`    | team-owned (via Solution)                 | `ROLE_ADMIN`                                                                                                                                                                                                                                      |
| `EvidenceResource`     | `/api/evidences`      | team-owned (via Opportunity / Assumption) | `ROLE_ADMIN`                                                                                                                                                                                                                                      |
| `NodeLinkResource`     | `/api/node-links`     | team-owned (via any node)                 | `ROLE_ADMIN`                                                                                                                                                                                                                                      |
| `OpenQuestionResource` | `/api/open-questions` | team-owned (via Opportunity)              | `ROLE_ADMIN`                                                                                                                                                                                                                                      |
| `NodeHistoryResource`  | `/api/node-histories` | team-owned (via node type + id)           | `ROLE_ADMIN`                                                                                                                                                                                                                                      |
| `CommentResource`      | `/api/comments`       | team-owned (via node)                     | `ROLE_ADMIN`                                                                                                                                                                                                                                      |
| `InterviewResource`    | `/api/interviews`     | team-owned (via Product)                  | `ROLE_ADMIN`                                                                                                                                                                                                                                      |
| `TagResource`          | `/api/tags`           | team-owned (via Solution / Assumption)    | `ROLE_ADMIN`                                                                                                                                                                                                                                      |

A generated DELETE that a database reference blocks (for example, an opportunity
that still has children) returns `409 error.dataintegrity` without any SQL.
`GeneratedEndpointsSecurityIT` asserts that every verb above is admin-only.

**Authorisation runs before body validation.** Method security (`@PreAuthorize`)
only fires once Spring MVC has deserialised and validated the handler arguments,
so an admin-only resource answered an unauthorised caller's incomplete body with
a 400 naming the controller, its package and its DTO fields. The admin-only
paths are therefore also matched in the filter chain
(`SecurityConfiguration.GENERATED_ADMIN_ONLY_API_PATHS`), which makes the answer
a plain 403 whatever the body is; the annotations stay as defence in depth.
`/api/teams` is a special case: the prefix is shared with the member-facing
`/api/teams/{teamId}/tree` and `/api/teams/{teamId}/products`, so
`GENERATED_ADMIN_ONLY_TEAM_PATHS` matches only the two shapes `TeamResource`
owns — `/api/teams` and `/api/teams/*` (one segment). `/api/products/**` is not
matched at all: `ProductResource` is member-facing.

## Tree Builder APIs (built on `TeamAccessService`)

These are hand-written resources beside the generated code (Epic 11), open to
authenticated users. Authorisation is checked per call:

- **Read**: any member of the node's team.
- **Write**: OWNER or EDITOR. Chat is a write, so viewers are read-only there too.
- **Non-members, unknown ids and `ROLE_ADMIN` without membership**: 403, with no
  existence leak.
- **Anonymous**: 401.

`{type}` is one of `product`, `outcome`, `opportunity`, `solution`, `assumption`,
`evidence` (case-insensitive). The writes marked "Lock" take the team's
`TreeStructureLock`. They answer `409 error.concurrencyFailure` when the wait
passes 5 s, or when the node was deleted by the request they queued behind.

| Controller                 | Verb and path                                 | Who                          | Notes                                                                                                                                                       |
| -------------------------- | --------------------------------------------- | ---------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `TeamTreeResource`         | GET `/api/teams/{teamId}/tree`                | member                       | Flat, pre-ordered `TeamTreeDTO`: nodes, members, `canEdit`, `evidenceThisMonth`                                                                             |
| `TreeNodeResource`         | POST `/api/tree/nodes`                        | OWNER / EDITOR               | Create under an allowed parent (server-side matrix), with defaults and default links. Lock.                                                                 |
| `TreeNodeResource`         | PATCH `/api/tree/nodes/{type}/{id}`           | OWNER / EDITOR               | Merge-patch of title, notes, status, confidence, priority, valueRating, ownerLogin, archived. Lock.                                                         |
| `TreeNodeResource`         | DELETE `/api/tree/nodes/{type}/{id}`          | OWNER / EDITOR               | Cascades the subtree with its links, questions, comments and history. Lock.                                                                                 |
| `TreeNodeMoveResource`     | POST `/api/tree/nodes/move`                   | OWNER / EDITOR               | Re-parent (cycle / type / same-team checks), product reorder. Lock.                                                                                         |
| `TreeNodeLinkResource`     | POST `/api/tree/nodes/{type}/{id}/links`      | OWNER / EDITOR               | Add a link (`^https?://.+`). Lock.                                                                                                                          |
| `TreeNodeLinkResource`     | PATCH / DELETE `/api/tree/links/{id}`         | OWNER / EDITOR               | Edit / remove a link                                                                                                                                        |
| `TreeOpenQuestionResource` | POST `/api/tree/opportunities/{id}/questions` | OWNER / EDITOR               | Add an open question (opportunities only). Lock.                                                                                                            |
| `TreeOpenQuestionResource` | PATCH / DELETE `/api/tree/questions/{id}`     | OWNER / EDITOR               | Edit text, tick / untick, remove                                                                                                                            |
| `TreeCommentResource`      | GET `/api/tree/nodes/{type}/{id}/comments`    | member                       | Oldest first. Products → 400 `chatnotsupported`. `TreeCommentDTO` carries `authorLogin` — ownership is derived per viewer, never on the payload (CHAT-001). |
| `TreeCommentResource`      | POST `/api/tree/nodes/{type}/{id}/comments`   | OWNER / EDITOR               | Post. Lock. Response and broadcast payload both use the viewer-agnostic `TreeCommentDTO`.                                                                   |
| `TreeCommentResource`      | PATCH / DELETE `/api/tree/comments/{id}`      | author, while OWNER / EDITOR | Edit (sets `editedDate`) / delete own message. Lock. Author-only enforced by `requireOwnComment` (403).                                                     |
| `TreeNodeHistoryResource`  | GET `/api/tree/nodes/{type}/{id}/history`     | member                       | Newest first. Products → 400 `historynotsupported`                                                                                                          |

The rules behind these endpoints (defaults, history, locking) are in
[`Architecture.md`](Architecture.md) section 6 and the resources' Javadoc.

## Team-scoped façades (built on `TeamAccessService`)

Introduced by TEAMS-002 / TEAMS-003. These remain open to authenticated users;
authorisation is enforced per call inside the service.

| Controller               | Path                           | Notes                                                                                                                                    |
| ------------------------ | ------------------------------ | ---------------------------------------------------------------------------------------------------------------------------------------- |
| `TeamManagementResource` | `/api/team-management/**`      | Create team / list-my-teams / member add-remove-role / user-search. All flows delegate to `TeamManagementService` → `TeamAccessService`. |
| `TeamProductResource`    | `/api/teams/{teamId}/products` | Lists a team's products (including archived); non-members get 403 via `teamAccessService.requireReadTeam`.                               |
| `AdminTeamResource`      | `/api/admin/teams/**`          | Admin team list / create / delete and member management. `ROLE_ADMIN` (class-level and the `/api/admin/**` filter rule).                 |

## STOMP / WebSocket destinations

Not an HTTP resource, but the same access-control rules apply and they are easy
to miss when auditing controllers.

| Destination                        | Frame     | Who                                                                                                                                     |
| ---------------------------------- | --------- | --------------------------------------------------------------------------------------------------------------------------------------- |
| `/websocket/**` (SockJS handshake) | —         | `authenticated` (`SecurityConfiguration`)                                                                                               |
| `/topic/teams/{teamId}/tree`       | SUBSCRIBE | Any member of that team (`TeamAccessService.canReadTeam`), viewers included. Enforced by `TreeTopicChannelInterceptor`.                 |
| anything else under `/topic`       | SUBSCRIBE | Refused (default-deny, STOMP `ERROR` frame) — wildcards, traversal, encoded and zero-padded ids all fail the exact-match rule (FR-034). |
| any `/topic` destination           | SEND      | Refused — `/topic` is server-to-client only.                                                                                            |

**Known gap.** Authorisation is decided once, when the SUBSCRIBE frame arrives.
`TreeChangeBroadcaster` then fans every event out to the topic, so a member
removed afterwards keeps receiving that team's tree events until its own client
acts on the `MEMBERSHIP_CHANGED` event and unsubscribes. That is a client-side
courtesy, not an enforcement: the server neither re-checks membership on the
outbound channel nor force-unsubscribes. Closing it needs a `clientOutboundChannel`
interceptor that re-resolves the subscriber's membership per message (or a
registry of subscriptions to revoke on removal) — see epic 5 FR-034 / FR-037.

## MCP endpoint

MCPSRV-001 wires the Spring AI MCP server starter (WebMVC,
`org.springframework.ai:spring-ai-starter-mcp-server-webmvc`, pinned to Spring AI **2.0.1**
via the `spring-ai-bom` in `pom.xml`) into the same Spring Boot process. MCPSRV-009 switched
the transport from the deprecated HTTP+SSE (which needed a companion `/mcp/message` endpoint)
to the current **Streamable HTTP** transport, configured under `spring.ai.mcp.server` with
`protocol: STREAMABLE` and `streamable-http.mcp-endpoint: /mcp`. Every JSON-RPC message travels
as a `POST /mcp`; the optional server-to-client listening stream is served on `GET /mcp` and
per-session identity is carried in the `Mcp-Session-Id` HTTP header (both request and response),
not in a URL segment. Capabilities are locked down to **tools only** — resources, prompts and
completions are all disabled. The only tool registered in MCPSRV-001 is `ping`, a trivial
read-only probe that exposes no application data; it exists to prove tool discovery and
invocation over the transport. A dedicated, stateless, CSRF-exempt `SecurityFilterChain` in
`McpSecurityConfiguration` isolates `/mcp` from the session-based `SecurityConfiguration` so
the existing web login, CSRF and REST API rules are untouched.

MCPSRV-002 replaces the placeholder permit-all rule on that chain with an OAuth2 resource-server
JWT authentication step. The chain is `SessionCreationPolicy.STATELESS`, has CSRF disabled for
`/mcp` only, and reuses the shared `JwtDecoder` bean (Keycloak JWK set, `JwtValidators`
issuer + expiry, `AudienceValidator`) and the shared
`Converter<Jwt, AbstractAuthenticationToken>` (`JwtAuthenticationConverter` +
`SecurityUtils.extractAuthorityFromClaims`) so the caller resolves to the same application user
and authorities the session login produces. `TeamAccessService` and any other REST code that
reads the `SecurityContext` work unchanged from an MCP tool. `McpSessionPrincipalFilter` binds
every session id the transport mints (announced in the `Mcp-Session-Id` response header of the
initialize call) to the caller that opened it, and refuses any subsequent request that carries
a session id belonging to somebody else — indistinguishably from an unknown session. Because the
check is header-driven, no path-encoding variant (e.g. `/mc%70`) can bypass it.

| Endpoint | Verb            | Transport                       | Protection                                                                                                                                                                                                                                                                                                                                                                                |
| -------- | --------------- | ------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `/mcp`   | POST/GET/DELETE | Streamable HTTP (MCP, JSON-RPC) | Keycloak bearer JWT (`Authorization: Bearer …`); signature, issuer, audience, expiry validated. No token / invalid token → 401. `Mcp-Session-Id` header on any request other than `initialize` must belong to the caller (404 otherwise, same shape as an unknown session). CSRF is disabled on this chain only; the session-based chain keeps `CookieCsrfTokenRepository` for `/api/**`. |

### Token acquisition for MCP callers (dev realm)

The dev Keycloak realm (`src/main/docker/realm-config/jhipster-realm.json`) ships a dedicated
public client `mcp_client` for MCP callers. It is separate from the web UI's `web_app` client so
that a token issued for an MCP agent tool is scoped to that use, and its audience is set
explicitly (via the `audience-account` protocol mapper) to `account`, which the existing
`AudienceValidator` accepts.

Two token-acquisition paths are supported out of the box:

- **Authorization Code + PKCE** (recommended for desktop MCP clients that support OAuth): open
  `http://localhost:9080/realms/jhipster/protocol/openid-connect/auth?client_id=mcp_client&response_type=code&scope=openid%20profile%20email%20roles&redirect_uri=http://localhost:8080/&code_challenge=<S256>&code_challenge_method=S256`
  in a browser, complete the Keycloak login, then exchange the returned `code` at
  `http://localhost:9080/realms/jhipster/protocol/openid-connect/token` with
  `grant_type=authorization_code&client_id=mcp_client&code=<code>&redirect_uri=http://localhost:8080/&code_verifier=<verifier>`.
- **Resource Owner Password (dev-only)** for smoke-testing from the terminal:

  ```bash
  curl -s -X POST http://localhost:9080/realms/jhipster/protocol/openid-connect/token \
    -d client_id=mcp_client \
    -d grant_type=password \
    -d scope="openid profile email roles" \
    -d username=admin -d password=admin
  ```

  This grant is enabled on `mcp_client` for the dev realm only (`directAccessGrantsEnabled`);
  production Keycloak realms should not enable it.

Use the resulting `access_token` as `Authorization: Bearer <token>` when connecting an MCP
client. Tokens without a valid signature, with the wrong issuer, without `account` in the `aud`
claim, or past their `exp` are rejected with HTTP 401. On every rejection the MCP chain's
`McpAuthenticationEntryPoint` logs a WARN line with the `OAuth2Error` code and description
(and, for `JwtValidationException`, the per-error details) without echoing the bearer token or
the `Authorization` header.

## Platform endpoints (not team-owned)

| Controller           | Path(s)                                          | Verbs             | Protection                                                                                                                                                                                                                                                           |
| -------------------- | ------------------------------------------------ | ----------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `AccountResource`    | `/api/account`, `/api/authenticate`              | GET               | `authenticated` (SecurityConfiguration `authz` rules; `/api/authenticate` and `/api/auth-info` are `permitAll`)                                                                                                                                                      |
| `AuthInfoResource`   | `/api/auth-info`                                 | GET               | `permitAll`                                                                                                                                                                                                                                                          |
| `AuthorityResource`  | `/api/authorities`, `/api/authorities/{id}`      | GET, POST, DELETE | `ROLE_ADMIN`. Every handler carries `@PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")` (per method, not per class), and `/api/authorities/**` is matched in `GENERATED_ADMIN_ONLY_API_PATHS` so the refusal happens in the filter chain, before the body is validated. |
| `BackupResource`     | `/api/admin/backup`, `/api/admin/backup/restore` | GET, POST         | `ROLE_ADMIN` (class-level and the `/api/admin/**` filter rule). Restore is multipart; over the configured `spring.servlet.multipart` limits it answers 413, not 403.                                                                                                 |
| `PublicUserResource` | `/api/users`                                     | GET               | `authenticated` — exposes the user directory the team-member picker needs.                                                                                                                                                                                           |
| `LogoutResource`     | `/api/logout`                                    | POST              | `authenticated`                                                                                                                                                                                                                                                      |

## Front-end alignment

The generated JHipster CRUD screens for team-owned entities (Team, TeamMember,
Product, Outcome, Opportunity, Solution, Assumption, Evidence, NodeLink,
OpenQuestion, NodeHistory, Interview, Comment, Tag) are gated on `ROLE_ADMIN` in
the router (`src/main/webapp/app/router/entities.ts`). They are linked only from
the admin-only "Static Data" sidebar group, so a non-admin user is never led to
a URL that returns 403.

Team members reach their teams and products through the team-scoped façades
(TEAMS-002 / TEAMS-003), and their trees through the Tree Builder (`/trees`)
and the APIs above.
