# Architecture: Ombuto Opportunity Solution Tree

## 1. What We're Building

Ombuto OST is a JHipster 9 monolith: a Spring Boot backend (Spring MVC + JPA)
serving a Vue 3 SPA from one deployable jar, with PostgreSQL as the only
datastore. See the PRD at `docs/Product Requirements Document/PRD.md`. It runs
as a single application instance per organisation. The server is authoritative
for every tree and persists each edit. The target architecture also pushes each
edit over WebSockets to everyone else viewing that tree; that broadcast is not
built yet (see "Built so far" in section 3).

## 2. Tech Stack

- Scaffold: JHipster 9 monolith, Maven — entities generated from `ombuto.jdl`,
  with MapStruct DTOs and a service layer.
- Backend: Spring Boot with Spring MVC and JPA/Hibernate (non-reactive) — the
  better-supported JHipster path, and required by its WebSocket option.
- Database: PostgreSQL in prod, H2 on disk in dev, Liquibase migrations —
  JHipster defaults.
- Frontend: Vue 3, TypeScript, Pinia, Vue Router, bootstrap-vue-next, Vite.
- Real-time: JHipster's `spring-websocket` option — STOMP over WebSocket using
  Spring's in-memory simple broker. The client is the generated STOMP client,
  wrapped in a Pinia store.
- Auth: OAuth2/OIDC via Spring Security, with Keycloak as the identity provider.
- MCP server: Spring AI's MCP server starter (WebMVC variant), inside the same
  app.
- Tests: JUnit and Cucumber for the backend, Vitest for the frontend, Playwright
  for e2e.
- Packaging: Docker image built with Jib; docker-compose for Postgres and
  Keycloak.

The repository was first generated as a reactive (WebFlux + R2DBC) app. It is
regenerated from `ombuto.jdl` with `reactive: false` and
`websocket: spring-websocket` in `.yo-rc.json` to match this document.

## 3. How It's Put Together

**Built so far vs. planned.** Teams and scoped access (Epic 1) and the Tree
Builder (Epic 11, section 6) are built. The real-time broadcast, `ROLE_OVERVIEW`
and the MCP server described below are the target design of Epics 5, 9 and 10
and do not exist in the code yet: other people's edits appear on reload.

One Spring Boot process serves everything: the built Vue SPA as static files,
the REST API under `/api`, the STOMP endpoint under `/websocket`, and the MCP
endpoint. PostgreSQL is the single source of truth. There is no cache and no
external broker.

**Backend** follows the JHipster layering generated from `ombuto.jdl`: REST
resource → service → JPA repository, with MapStruct DTOs at the boundary. Three
hand-written pieces sit on top of the generated code:

- **Team access.** A `TeamAccessService` resolves the current user's
  `TeamMember` rows and answers "can this user read or edit this team's tree?"
  (a product, and every node beneath it, resolves to its team). Every
  service method that touches a tree goes through it, and list queries are
  filtered by the user's team ids. Viewers are read-only. A global
  `ROLE_OVERVIEW` authority, assigned in Keycloak, grants read access across all
  teams for heads of product.
- **Tree collaboration.** All writes go through the REST API, which validates,
  authorises and persists them. After commit, the service publishes a small
  change event (`nodeType`, `id`, `action`, DTO, `user`) to
  `/topic/teams/{teamId}/tree`. Clients never write over the socket: a client
  `SEND` to any `/topic/**` destination is rejected by
  `WebsocketSecurityConfiguration` (server-to-client only), and
  `TreeTopicChannelInterceptor` additionally refuses any `SEND` that targets the
  `/topic` namespace. SUBSCRIBE frames are authorised against `TeamAccessService`
  in the same channel interceptor (`TreeTopicChannelInterceptor`), which is
  **default-deny for the whole `/topic` namespace**: a SUBSCRIBE is admitted only
  when its destination is literally `/topic/teams/{teamId}/tree` (canonical
  decimal id, no sign, no leading zeros, no trailing segment) _and_
  `TeamAccessService.canReadTeam` grants the principal read access. This matters
  because Spring's simple broker resolves subscription destinations with an
  `AntPathMatcher`, so a _pattern_ destination such as `/topic/teams/*/tree` or
  `/topic/**` matches every team's real destination; authenticating the
  subscriber is therefore not enough, and anything that is not the exact tree
  topic — wildcards, path traversal (`/topic/teams/1/tree/../2/tree`),
  percent-encoded forms, zero-padded ids, unknown `/topic/...` destinations — is
  refused. **A refusal is an explicit STOMP `ERROR` frame, not a silent drop:**
  the interceptor throws `AccessDeniedException`, which Spring's
  `StompSubProtocolHandler` renders as an `ERROR` frame, so a refused client
  learns it is not subscribed instead of waiting forever for events that will
  never come. Viewers subscribe and receive events like any other member
  (FR-034). The WebSocket handshake reuses the authenticated HTTP session and
  unauthenticated handshakes are refused (NFR-011). Comments use the same
  mechanism.
- **MCP server.** Read-only tools (list products, get tree, get node, list
  interviews) call the same services, so they are scoped the same way. Callers
  present a Keycloak bearer token. The starter is
  `org.springframework.ai:spring-ai-starter-mcp-server-webmvc`, pinned via the
  `spring-ai-bom` (property `spring-ai.version` in `pom.xml`) at **2.0.1** — a
  Spring Boot 4-compatible line. The transport is Server-Sent Events over HTTP
  (Spring AI's WebMVC SSE) served on `/mcp` (message channel `/mcp/message`),
  configured in `application.yml` under `spring.ai.mcp.server`. Capabilities are
  set to tools only: `resource`, `prompt` and `completion` are all disabled.
  MCPSRV-001 wires the starter, an isolated permit-all `SecurityFilterChain`
  for `/mcp/**` (in `McpSecurityConfiguration`) and a single `ping` probe tool
  so the transport can be verified end-to-end; MCPSRV-002 replaces the
  permit-all chain with a stateless Keycloak resource-server chain.

**Frontend** loads a team's whole tree (all of the team's products as top-level
branches) in one request into a Pinia store and renders it on the Tree Builder
canvas, optionally scoped to one product (section 6). Planned for Epic 5: it
subscribes to that team's topic, an incoming event patches the store, the user's
own echoed event is ignored, and on reconnect the client reloads the tree rather
than replaying missed events.

Concurrent edits are last-write-wins per node. Edits are small (one node's
fields, or a move), so true conflicts are rare and the losing user sees the
winning value immediately.

## 4. Key Decisions

- Non-reactive Spring MVC + JPA, not WebFlux: JHipster's WebSocket support only
  exists on the MVC stack, and nothing here needs reactive throughput.
- JHipster's STOMP WebSocket with the in-memory simple broker, not RabbitMQ or
  Redis pub/sub: the app runs as a single instance, so there is nothing to fan
  out across. A second instance would require an external broker. This is the
  single-instance / in-memory-broker limitation called out by **NFR-012**:
  every tree change event is fanned out from one JVM's `SimpMessagingTemplate`
  to that same JVM's connected STOMP sessions. Horizontal scaling (a second
  application instance behind a load balancer, or blue/green with connected
  clients) is out of scope; it would need an external broker (RabbitMQ or Redis
  STOMP relay) and per-team `seq`/`epoch` state moved out of process, both of
  which are a separate architecture decision.
- Writes over REST, WebSocket for broadcast only: one write path to validate and
  secure. Considered sending edits as STOMP messages and rejected it because it
  duplicates the generated REST layer.
- Last-write-wins per node, not CRDT/OT: edits are small and structured, not
  free-text co-editing. Considered Yjs and rejected it as far more machinery
  than a product trio needs.
- Reload the tree on reconnect, not event replay: no event log to keep, and
  trees are small enough to fetch whole.
- Node discussion is a flat, chat-style thread, not threaded comments: a product
  trio talking about one node does not need reply trees, and a flat thread reads
  and renders the same everywhere (panel tab, chat modal, detail page). The
  `Comment.parent` self-relationship that the original model carried was removed
  from `ombuto.jdl` and the schema on 2026-09-20; this is a closed decision, not
  a deferral, and epic 6 is closed against it. Live delivery of messages is the
  only chat work left, and it belongs to epic 5.
- JHipster's `tracker` WebSocket sample (`web/websocket/ActivityService`,
  `app/admin/tracker/*`) was deleted on 2026-09-20 and stays deleted (RTC-003
  confirmed the decision): it tracked page views per session, had nothing to do
  with the tree, and would have shared the `/topic` prefix with the team topic
  — a footgun the interceptor should not have to work around. The STOMP
  infrastructure it came with — `WebsocketConfiguration` (SockJS endpoint,
  simple `/topic` broker) and `WebsocketSecurityConfiguration` (authentication
  required on `/topic/**`, client SEND denied, everything else denied) — is
  kept for the team topic in epic 5, with `TreeTopicChannelInterceptor` layered
  on top for the per-team check.
- Keycloak for all sign-in, local accounts included: Keycloak brokers the
  company IdP and holds local username/password users, so the app stores no
  passwords and sees one OIDC provider. Considered JHipster JWT auth with its
  own user table and rejected it because it means owning password reset and
  storage.
- Team scoping enforced in the service layer via `TeamAccessService`, not in
  controllers or the client: REST, WebSocket subscriptions and MCP all pass
  through the same check.
- MCP server inside the monolith, not a separate process: it reuses the services
  and authorisation, and there is one thing to deploy.
- One deployment per organisation, no tenant layer: `Team` is the top-level
  scope.
- One tree per team, with products as top-level branches, not one tree per
  product: members working on different products share one canvas and one
  real-time topic. No schema change — `Product → Team` and `Outcome → Product`
  already express it; only the tree load and the topic are team-scoped.
  Considered a separate `Tree` entity and rejected it as an extra level nobody
  asked for.

## 5. Security & Data

The trust boundary is the application server; the browser and MCP clients are
untrusted. Authentication is OIDC against Keycloak: the SPA uses the
session-cookie login flow JHipster generates (with CSRF protection), MCP clients
send a Keycloak bearer token, and the WebSocket handshake rides on the same
session. The app stores no passwords — local accounts live in Keycloak.
Authorisation is team membership plus role (owner / editor / viewer), checked
server-side on every read, write and topic subscription.

Secrets (database password, OIDC client secret) are supplied as environment
variables, never committed; the `secret-samples` profile is for dev and e2e
only. Interview notes may contain customer personal data, so they sit behind the
same team scoping as the tree, and `ROLE_OVERVIEW` users see interview titles
only, not notes. The database is not encrypted at the application level; disk
encryption and TLS termination are left to the hosting environment.

## 6. Tree Builder (OST)

The Tree Builder (Epic 11,
[`epic_11_OST_TREE_BUILDER.md`](../Epics/epic_11_OST_TREE_BUILDER.md)) replaced the
first custom SVG tree editor (`app/tree`, Epics 2 and 3) and the `Experiment`,
`OpportunityLink` and `SolutionLink` entities. The node hierarchy is Product →
Outcome → Opportunity (nestable) → Solution → Assumption, with Evidence under an
Opportunity or an Assumption. `NodeLink`, `OpenQuestion` and `NodeHistory` hang
off the nodes. The model lives in `ombuto.jdl` like every other entity.

**Canvas and layout.** The canvas is Vue Flow (`@vue-flow/core`) with custom
node and edge components. Positions are never stored: `domain/layout.ts` derives
a tidy top-down layout (O(n)) from the parent links and `sortOrder` on every
change. The store exposes it as `placed`, and a drag only re-parents, it never
places a node. Wheel zoom, Fit and the overview map are OST code. Vue Flow's own
zoom-on-scroll and minimap are not used. Only nodes in view are rendered
(`only-render-visible-elements`). A node with an open rename field or `+` menu is
pinned so it stays mounted when it leaves the view (`pinRendered`).

**Module layout** (`src/main/webapp/app/ost/`, lazy-loaded):

| Folder / file                                | Contents                                                                                                                                                                                               |
| -------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `domain/`                                    | Pure logic: `layout.ts` (tidy layout, edge paths), `rules.ts` (allowed children, defaults, default links), `mapping.ts` (DTO ↔ node), `derive.ts` (breadcrumbs, counts, dashboard and tracker figures) |
| `stores/`                                    | `ost-tree.store.ts` (team, flat nodes, `placed`, all API actions), `ost-ui.store.ts` (selection, panel, filters, palette, collapse, dialogs)                                                           |
| `ost.service.ts`, `ost.routes.ts`            | REST client for the tree APIs, and the `/trees` routes                                                                                                                                                 |
| `shell/`, `pages/`                           | `OstShell` (team combo, view tabs, members) and the dashboard, canvas, Experiments and full-page node detail pages                                                                                     |
| `canvas/`, `panel/`, `chat/`, `node-detail/` | Canvas (nodes, edges, toolbar, palette, `+` menu, minimap), detail panel and its tabs, the shared chat thread, detail-page parts                                                                       |
| `overlays/`, `styles/`                       | Dialogs and toasts, scoped design tokens and base styles                                                                                                                                               |

Routes: `/trees` opens the last-used (else first) team. `/trees/:teamId` is the
shell (`meta.fullBleed`, which drops the app's card chrome in `app.vue`) with
the children dashboard, `canvas?product=&node=`, `experiments` and
`nodes/:nodeKey`.

**Stores.** Edits are optimistic. `patchNode` applies a field change at once and
sequences in-flight patches per node and per field: a response or rollback only
touches a field that has no newer pending patch or newer applied response. A
failed patch rolls back to the newest value still pending, else to the last
confirmed one. Moves are optimistic with rollback. Creates wait for the server,
which assigns ids, then select the new node in rename mode. When a write gets a
403 or a `409 error.concurrencyFailure`, the store re-reads the tree. A node that
has gone is removed locally with "This item was deleted by someone else". Collapsed
branches and the last-used team are kept in `localStorage` per user (and team).

**Scoped styling.** The design tokens (`styles/ost-tokens.css`) are declared on
`.ost-root`, the shell's root element, not on `:root`. The OST styles, Inter and
Vue Flow's base CSS are imported only by the lazy OST chunk and use no global
selectors, because that CSS stays loaded after the user navigates away. Vue Flow's
`theme-default.css` is not imported, and OST screens use no Bootstrap buttons or
cards. The palette the tokens draw from is app-wide and lives in §7.

**Backend: custom tree APIs beside the generated CRUD.** The tree rules live in
hand-written classes, never in generated ones (engineering guide §4b):

- Read: `TeamTreeResource` `GET /api/teams/{teamId}/tree` → `TeamTreeService`
  returns one flat, pre-ordered `TeamTreeDTO` (members, `evidenceThisMonth`,
  nodes with links, questions, comment counts and product last activity) in a
  fixed number of queries (`TeamTreeRepository`).
- Writes under `/api/tree/**`: `TreeNodeResource`/`TreeNodeWriteService`
  (create, merge-patch, cascade delete through `TreeNodeCascadeService`),
  `TreeNodeMoveResource`/`TreeNodeMoveService` (re-parent, product reorder,
  cycle and cross-team checks, dense renumbering), `TreeNodeLinkResource`,
  `TreeOpenQuestionResource`, `TreeCommentResource` and
  `TreeNodeHistoryResource` (read). `TreeNodeRules` holds the server copy of the
  allowed-children matrix. `DefaultNodeLinks` adds each type's default links on
  create.
- Access: `TeamAccessService.requireReadNode` / `requireEditNode` resolve any
  node, link, question or comment to its team (`TreeAccessLookupRepository`).
  Any member may read. OWNER and EDITOR may write, and that includes chat.
  Editing or deleting a comment is limited to its author while they are still an
  editor. A non-member, or an unknown id, gets 403 with no existence leak.
  `ROLE_ADMIN` gives no implicit tree access.
- The generated CRUD resources (`/api/outcomes`, `/api/evidences`,
  `/api/node-links`, …) are admin-only via class-level
  `@PreAuthorize(ROLE_ADMIN)`, and their Vue screens sit under the admin-only
  "Static Data" menu. `ProductResource` is the exception: it is team-scoped, and
  its delete cascades like a tree delete.

**Team lock and 409s.** `TreeStructureLock` takes a pessimistic write lock on the
owning `Team` row before any write that appends to or renumbers sibling lists, or
that could race a cascade delete. That covers node create, move, patch and delete,
product create, reorder and team change (both teams, lower id first), link and
open-question add, and chat post, edit and delete. The wait is bounded to 5 s. After
the wait, the service re-checks that the node and parent still exist. Conflicts
return 409 with a generic `detail` and never any SQL:

- `error.concurrencyFailure`: the lock timed out, or the request it queued behind
  deleted the target. Clients re-read the tree.
- `error.dataintegrity`: a foreign-key or constraint violation (SQLState class 23),
  for example an admin CRUD delete of a node that has children.
- `error.duplicate`: a unique-constraint violation.

**History.** `NodeHistoryRecorder.record(type, id, event, summary)` writes in the
same transaction as the change, called only from the tree services, with the
current user as author. It records: created, status, confidence, priority (only
when `round(priority / 10)` changes), value, a move to a new parent, link
added/removed, open question added, and comment added/deleted. It never records
title, notes, owner or archived edits, or comment edits. Summaries use the
prototype's wording. Products have no history and no chat (400).
`GET …/history` lists newest first. A cascade delete removes the history rows of
every node it deletes (`node_history` is indexed on `(node_type, node_id)` by a
custom changelog).

**Dev seed.** `config/seed/DevDataSeeder` (`@Profile("dev")`, gated by
`application.seed.enabled`, which is true in `application-dev.yml`) seeds the
`admin` and `user` accounts with their Keycloak ids and four teams. Team Jupiter
and Team Venus have `user` as OWNER and `admin` as VIEWER; Best Team and Team
Mars have `admin` as OWNER. Team Jupiter gets the full prototype tree. Seeding
is idempotent per team name. It retries only while the schema is not ready yet,
and its timestamps fall inside the current month. The JHipster `faker` Liquibase
context is no longer loaded in dev.

**Existing dev databases must be wiped.** Step 1 of the Tree Builder regenerated
the entity changelogs in place (the project is pre-production, see guide §4b). A
dev database created before that fails Liquibase checksum validation: stop the
app and delete `target/h2db/` (H2), or recreate the PostgreSQL container.

**Error detail in dev.** In the `dev` profile, some non-database exceptions
(Jackson parse errors, type mismatches) still echo Java class names in the
problem `detail`. This is the generator's default. The `prod` profile masks
them (`ExceptionTranslator.getCustomizedErrorDetails`). Database errors never
carry SQL in any profile.

**Known limits.** There is no real-time sync: other people's edits appear on
reload (Epic 5). Only the nodes in view are in the DOM, so Tab cannot reach
off-screen nodes. Search + Enter jumps to any match, and the panel breadcrumb
and child list move through the tree.

## 7. Theming (light / dark)

`src/main/webapp/content/css/theme.css` is the **single source of the palette**
("Ombuto OST · Nocturne", `docs/Mockups/ColorPalette.png`). It has two halves:

1. **Fixed palette** — the accent and neutral ramps, the four Nocturne core
   colours (`--ost-nocturne-ground/surface/text/accent`), the status tones and
   the priority spectrum. A ramp step is a pigment; it never varies by theme.
2. **Roles** — `--ost-bg`, `--ost-surface`, `--ost-text`, `--ost-border`,
   `--ost-accent`, the chrome roles, … defined on `:root` for **dark** and
   re-pointed at other steps of the **same ramps** under
   `:root[data-theme='light']`. No new hues are invented for light.

CSS custom properties, not Sass variables: Sass cannot be re-pointed at runtime,
so a toggle would mean shipping two stylesheets. Bootstrap is followed rather
than forked — `global.scss` maps `--ost-*` onto Bootstrap's `--bs-*` under
`html[data-theme]`, and `data-bs-theme` rides along on `<html>`. Every pair the
app ships meets WCAG 2.1 AA (4.5:1 body text, 3:1 UI borders); the ratios are
recorded beside each role in `theme.css`.

**Dark is the default, and the OS preference is never consulted.** The choice is
the user's: a navbar toggle (`data-cy="themeToggle"`) writes `dark` or `light` to
`localStorage` under `ombuto-theme` and sets `data-theme` on `<html>`
(`app/shared/config/store/theme-store.ts`). `index.html` carries a tiny inline
copy of the same read so the loading splash is already themed; `main.ts` calls
`initTheme()` before mounting. Every storage access is wrapped in `try`/`catch`
and falls back to dark.

Two deliberate exceptions stay **Nocturne dark in the light theme**:

- **The tree canvas.** OST is a design-tool artboard, not a document. It reads
  only the fixed palette, never the roles, so the light theme cannot reach it.
- **The sidebar rail.** The rail is the app's spine and shares the canvas's
  ground, so navigation reads the same wherever you are. Its `--ost-sidebar-*`
  roles are built from the fixed palette and are **not** repeated in the light
  block. This is a product decision, not an oversight — do not "fix" it by adding
  light overrides. The navbar and all page content do follow the theme.

`src/test/javascript/playwright/theme.spec.ts` guards all of it, including the
rail's computed background being the dark token in both themes.

The non-production marker (`app/core/ribbon/`, driven by
`display-ribbon-on-profiles`) is a pill in the navbar beside the version badge,
tinted with the warm end of the priority spectrum. It used to be a diagonal
banner pinned to the viewport's top-left corner, where it covered the sidebar's
first rows.
