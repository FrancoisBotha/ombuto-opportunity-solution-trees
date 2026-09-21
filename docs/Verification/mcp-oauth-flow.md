# Verifying the MCP OAuth flow

This is a **manual runbook**, not a captured transcript. The MCP authorization
flow ends with a desktop browser sign-in that an automated build environment
cannot drive; the code here has an operator perform the run against a live
deployment and observe the outcome. What the automated build **can** and does
pin is the server-side contract that the flow relies on — see the "What the
integration tests pin" section below.

## What this proves

Two acceptance criteria of MCPSRV-008 are only meaningful end-to-end:

- An MCP client can connect without any hand-minted token, completing an
  authorization-code + PKCE flow in the browser.
- The session survives past the access token lifetime without manual
  intervention — proven by holding a connection across at least one expiry.

Both are operator-owned checks against a live app + Keycloak. Do them before
declaring MCPSRV-008 done in a real environment.

## Preconditions

- The app is running (dev: `npm run app:start`; prod: `docker compose -f
  deploy/docker-compose.prod.yml up -d`) and reachable at `${APP_ORIGIN}`
  (dev: `http://localhost:9000`; prod: `https://<host>`).
- Keycloak is running and has the `mcp_client` client from
  `src/main/docker/realm-config/jhipster-realm.json` (dev) or the equivalent
  documented in
  [`docs/Deployment/aws-internal-single-instance.md`](../Deployment/aws-internal-single-instance.md)
  (prod). PKCE is enforced (`S256`); direct-access grants are off in prod.
- An MCP client that supports the MCP authorization spec (RFC 9728 protected
  resource metadata + `WWW-Authenticate` challenge discovery). Claude Code
  ≥ 2.1 is the reference client.
- A Keycloak user with a team membership so tool calls return something
  non-empty.

## Step 1 — Confirm the 401 challenge and metadata (no browser needed)

Run these against a live app. This does not prove the browser flow, but it
does prove the two server-side contracts the client depends on.

    curl -i -X POST "${APP_ORIGIN}/mcp"
    #  → HTTP/1.1 401
    #  → WWW-Authenticate: Bearer resource_metadata="${APP_ORIGIN}/.well-known/oauth-protected-resource"

    curl -s "${APP_ORIGIN}/.well-known/oauth-protected-resource" | jq .
    #  → { "resource": ".../mcp",
    #      "authorization_servers": [ "http(s)://.../realms/jhipster" ],
    #      "bearer_methods_supported": [ "header" ],
    #      "scopes_supported": [ "openid", "profile", "email", "roles" ] }

If the challenge or metadata shape differs from that, stop here and fix the
server — the client will not be able to discover the authorization server.

## Step 2 — Connect from an MCP client and complete the browser flow

1. Save the `.mcp.json` snippet the in-app **Connect an agent** page prints
   into the client's project directory. It carries only `type: "http"` and
   `url: "${APP_ORIGIN}/mcp"`, with no bearer token and no `oauth.*` keys.
2. Start the MCP client and approve the project's `ombuto-ost` server when
   prompted.
3. The client discovers the authorization server from the challenge, opens a
   browser to Keycloak, and asks the user to sign in and consent. Complete
   the sign-in.
4. Verify a tool call round-trips (for example, `list_products`).

**Record the following on the ticket**: the client name and version, the app
build's git commit (`/management/info`), and the timestamps of sign-in and
first successful tool call. That triple is the honest end-to-end evidence for
the first acceptance criterion.

If the client cannot complete the sign-in, capture its log and the exact
`WWW-Authenticate` header the server emitted (`curl -i -X POST`) — the
combination is what distinguishes a client bug from a server-side spec
deviation.

## Step 3 — Hold a connection past the access token lifetime

1. In the Keycloak admin console, shorten **Realm settings → Tokens → Access
   Token Lifespan** to a value shorter than you are prepared to wait — 60 s
   is comfortable.
2. Reconnect the MCP client (so it picks up a token minted under the new
   lifespan). Note the wall-clock time; call it `T0`.
3. Make one tool call and confirm it succeeds. Call this time `T1`.
4. Wait for `T0 + lifespan + 30 s` without touching the client.
5. Make another tool call in the same session. Call this time `T2`.

**Pass**: the call at `T2` succeeds without any new browser sign-in and
without the operator pasting or refreshing anything. That proves the
refresh-token exchange happened silently, satisfying the "survives past
expiry" criterion. Record `T0`, `T1`, `T2`, the configured lifespan, and the
client's log line acknowledging the refresh on the ticket as evidence.

**Fail modes to look for**:

- The call at `T2` returns 401 to the user: the client did not refresh. Check
  the challenge shape at `T2` — the server must return the *same*
  `Bearer resource_metadata="…"` challenge with `error="invalid_token"` for
  the client to know to refresh (pinned by
  `McpProtectedResourceMetadataIT#mcpEndpoint_afterAccessTokenExpiry_returns401WithSameResourceMetadataChallenge_soClientRefreshes`).
- The browser opens again: the refresh token was rejected. Check the realm's
  refresh-token lifespan and whether the client's stored refresh token is
  still valid.

## What the integration tests pin

`src/test/java/com/opportunity/tree/config/mcp/McpProtectedResourceMetadataIT.java`
pins the server-side pieces of the contract the client depends on:

- `GET /.well-known/oauth-protected-resource` returns 200 with `resource`,
  `authorization_servers`, `bearer_methods_supported` and `scopes_supported`.
- `GET /.well-known/oauth-protected-resource/mcp` (Claude Code's
  path-derived variant) returns the same document.
- `POST /mcp` with no `Authorization` header returns 401 with
  `WWW-Authenticate: Bearer resource_metadata="${baseUrl}/.well-known/oauth-protected-resource"`.
- `POST /mcp` with an *expired* bearer token returns 401 with the same
  `resource_metadata` challenge plus `error="invalid_token"` and an
  expired-token description — the shape the client keys off to trigger its
  refresh-token exchange.

Those tests prove the server keeps its side of the bargain on every commit.
They do not, and cannot, prove that a specific desktop MCP client actually
runs the browser flow and refreshes — that is what Steps 2 and 3 above are
for.
