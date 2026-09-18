# Architecture: Ombuto Opportunity Solution Tree

## 1. What We're Building

Ombuto OST is a JHipster 9 monolith: a Spring Boot backend (Spring MVC + JPA)
serving a Vue 3 SPA from one deployable jar, with PostgreSQL as the only
datastore. See the PRD at `docs/Product Requirements Document/PRD.md`. It runs
as a single application instance per organisation. The defining architectural
property is real-time collaboration: the server is authoritative for every
tree, persists each edit, and pushes it over WebSockets to everyone else viewing
that tree.

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
  `/topic/teams/{teamId}/tree`. Clients never write over the socket.
  Subscriptions are checked against `TeamAccessService` in a STOMP channel
  interceptor, so a user can only listen to trees they are allowed to see.
  Comments use the same mechanism.
- **MCP server.** Read-only tools (list products, get tree, get node, list
  interviews) call the same services, so they are scoped the same way. Callers
  present a Keycloak bearer token.

**Frontend** loads a team's whole tree (all of the team's products as top-level
branches) in one request into a Pinia store, renders it in the tree editor with
an optional focus on one product, and subscribes to that team's topic. An
incoming event patches the store. The user's own edits are applied when the REST
call returns, and their echoed event is ignored. On reconnect, the client
reloads the tree rather than replaying missed events.

Concurrent edits are last-write-wins per node. Edits are small (one node's
fields, or a move), so true conflicts are rare and the losing user sees the
winning value immediately.

## 4. Key Decisions

- Non-reactive Spring MVC + JPA, not WebFlux: JHipster's WebSocket support only
  exists on the MVC stack, and nothing here needs reactive throughput.
- JHipster's STOMP WebSocket with the in-memory simple broker, not RabbitMQ or
  Redis pub/sub: the app runs as a single instance, so there is nothing to fan
  out across. A second instance would require an external broker.
- Writes over REST, WebSocket for broadcast only: one write path to validate and
  secure. Considered sending edits as STOMP messages and rejected it because it
  duplicates the generated REST layer.
- Last-write-wins per node, not CRDT/OT: edits are small and structured, not
  free-text co-editing. Considered Yjs and rejected it as far more machinery
  than a product trio needs.
- Reload the tree on reconnect, not event replay: no event log to keep, and
  trees are small enough to fetch whole.
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
