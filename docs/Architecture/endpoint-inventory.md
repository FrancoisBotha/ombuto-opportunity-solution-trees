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

| Controller                 | Verb and path                                 | Who                          | Notes                                                                                               |
| -------------------------- | --------------------------------------------- | ---------------------------- | --------------------------------------------------------------------------------------------------- |
| `TeamTreeResource`         | GET `/api/teams/{teamId}/tree`                | member                       | Flat, pre-ordered `TeamTreeDTO`: nodes, members, `canEdit`, `evidenceThisMonth`                     |
| `TreeNodeResource`         | POST `/api/tree/nodes`                        | OWNER / EDITOR               | Create under an allowed parent (server-side matrix), with defaults and default links. Lock.         |
| `TreeNodeResource`         | PATCH `/api/tree/nodes/{type}/{id}`           | OWNER / EDITOR               | Merge-patch of title, notes, status, confidence, priority, valueRating, ownerLogin, archived. Lock. |
| `TreeNodeResource`         | DELETE `/api/tree/nodes/{type}/{id}`          | OWNER / EDITOR               | Cascades the subtree with its links, questions, comments and history. Lock.                         |
| `TreeNodeMoveResource`     | POST `/api/tree/nodes/move`                   | OWNER / EDITOR               | Re-parent (cycle / type / same-team checks), product reorder. Lock.                                 |
| `TreeNodeLinkResource`     | POST `/api/tree/nodes/{type}/{id}/links`      | OWNER / EDITOR               | Add a link (`^https?://.+`). Lock.                                                                  |
| `TreeNodeLinkResource`     | PATCH / DELETE `/api/tree/links/{id}`         | OWNER / EDITOR               | Edit / remove a link                                                                                |
| `TreeOpenQuestionResource` | POST `/api/tree/opportunities/{id}/questions` | OWNER / EDITOR               | Add an open question (opportunities only). Lock.                                                    |
| `TreeOpenQuestionResource` | PATCH / DELETE `/api/tree/questions/{id}`     | OWNER / EDITOR               | Edit text, tick / untick, remove                                                                    |
| `TreeCommentResource`      | GET `/api/tree/nodes/{type}/{id}/comments`    | member                       | Oldest first. Products → 400 `chatnotsupported`                                                     |
| `TreeCommentResource`      | POST `/api/tree/nodes/{type}/{id}/comments`   | OWNER / EDITOR               | Post. Lock.                                                                                         |
| `TreeCommentResource`      | PATCH / DELETE `/api/tree/comments/{id}`      | author, while OWNER / EDITOR | Edit (sets `editedDate`) / delete own message. Lock.                                                |
| `TreeNodeHistoryResource`  | GET `/api/tree/nodes/{type}/{id}/history`     | member                       | Newest first. Products → 400 `historynotsupported`                                                  |

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

## MCP endpoint

MCPSRV-001 wires the Spring AI MCP server starter (WebMVC / SSE transport,
`org.springframework.ai:spring-ai-starter-mcp-server-webmvc`, pinned to Spring AI **2.0.1**
via the `spring-ai-bom` in `pom.xml`) into the same Spring Boot process. Endpoint path is
`/mcp` (SSE) with message posts on `/mcp/message`, configured in `application.yml` under
`spring.ai.mcp.server`. Capabilities are locked down to **tools only** — resources, prompts and
completions are all disabled. The only tool registered in this ticket is `ping`, a trivial
read-only probe that exposes no application data; it exists to prove tool discovery and
invocation over the transport. A dedicated, stateless, CSRF-exempt `SecurityFilterChain` in
`McpSecurityConfiguration` isolates `/mcp/**` from the session-based `SecurityConfiguration` so
the existing web login, CSRF and REST API rules are untouched. Bearer-token authentication and
the business tools are added by later MCPSRV tickets.

| Endpoint       | Verb | Transport                | Protection                                                                                            |
| -------------- | ---- | ------------------------ | ----------------------------------------------------------------------------------------------------- |
| `/mcp`         | GET  | SSE stream (MCP)         | Permit-all in this ticket (isolated chain); replaced by Keycloak bearer-token auth in MCPSRV-002.     |
| `/mcp/message` | POST | JSON-RPC message channel | Same as above. CSRF is disabled on this chain only; the session-based chain keeps CSRF for `/api/**`. |

## Platform endpoints (not team-owned)

| Controller           | Path(s)                                     | Verbs             | Protection                                                                                                                                                                                                                                                                             |
| -------------------- | ------------------------------------------- | ----------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `AccountResource`    | `/api/account`, `/api/authenticate`         | GET               | `authenticated` (SecurityConfiguration `authz` rules; `/api/authenticate` and `/api/auth-info` are `permitAll`)                                                                                                                                                                        |
| `AuthInfoResource`   | `/api/auth-info`                            | GET               | `permitAll`                                                                                                                                                                                                                                                                            |
| `AuthorityResource`  | `/api/authorities`, `/api/authorities/{id}` | GET, POST, DELETE | Restricted by SecurityConfiguration — falls under `/api/admin/**`? No, `/api/authorities/**` matches `/api/**` (`authenticated`). The controller itself does not add `@PreAuthorize`; the authorities catalogue is inherently a platform concern and is treated as authenticated-only. |
| `PublicUserResource` | `/api/users`                                | GET               | `authenticated` — exposes the user directory the team-member picker needs.                                                                                                                                                                                                             |
| `LogoutResource`     | `/api/logout`                               | POST              | `authenticated`                                                                                                                                                                                                                                                                        |

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
