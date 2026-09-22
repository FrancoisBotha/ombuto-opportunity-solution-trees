# Verifying the MCP OAuth flow

This runbook captures the exact Claude Code configuration and the evidence an
operator records while exercising a live app and Keycloak. The build pins the
server contracts and validates that the generated `.mcp.json` contains the
vendor-supported pre-registered client fields. A release check completes the
browser sign-in and expiry hold against the deployed identity provider.

## What this proves

Two acceptance criteria of MCPSRV-008 are only meaningful end-to-end:

- An MCP client can connect without any hand-minted token, completing an
  authorization-code + PKCE flow in the browser.
- The session survives past the access token lifetime without manual
  intervention — proven by holding a connection across at least one expiry.

Both are operator-owned checks against a live app + Keycloak. Do them before
declaring MCPSRV-008 done in a real environment.

## Local end-to-end evidence (2026-09-22)

The flow was exercised against the app built from `5c37f8c6f589d1578741e59b9f20b102e3653e21`
on `http://127.0.0.1:8080/mcp`, with the local Keycloak realm at
`http://localhost:9080/realms/jhipster` and Claude Code 2.1.278. The CLI was
configured with `--transport http --client-id mcp_client --callback-port 3334`.

Claude Code's `mcp login --no-browser` generated an authorization-code request
with an `S256` PKCE challenge. A headless browser signed in to the dev realm as
its seeded admin user, followed the redirect to `http://localhost:3334/callback`,
and received Claude Code's "Authentication successful" confirmation. The CLI
then printed `Authenticated with "ombuto-doctor"`; `mcp get` reported
`Status: Connected`. No bearer token was minted or pasted by the operator.

To check a tool call without an Anthropic account in the isolated CLI profile,
the local verification probe used the access token Claude Code had stored and
opened a Streamable HTTP MCP session. It called `list_products` in that session
at **11:34:28 UTC**: success, 4 products. Claude Code's stored credential gave
the original access token an expiry of **11:38:03 UTC**. At **11:38:04 UTC**, a
second `mcp get` reported `Status: Connected` without another browser sign-in;
the stored credential now expired at **11:43:11 UTC**, showing that the client
had refreshed it. At **11:38:14 UTC**, the probe called `list_products` again
with the **same MCP session ID** and Claude Code's refreshed access token:
success, 4 products. The probe and CLI credential files were kept under the
ignored `target/` directory, and no token or authorization code is recorded
here.

This check uses Claude Code for the browser authorization and refresh, and a
small local probe for the two tool calls on one MCP session. A production
release should still repeat Steps 2 and 3 against its own Keycloak realm and
deployment, especially if its token lifetime or redirect URI differs.

## Preconditions

- The app is running (dev: `npm run app:start`; production: use the deployment
  compose file) and reachable at `${APP_ORIGIN}`
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

## Configuration contract verified for this change

At `2026-09-22 09:42:03 UTC`, commit `4f0b6fe592587cfc4e15f8c6ae3b3da0a48aa55a`
was checked with Claude Code `2.1.278` using:

    claude mcp add --transport http --client-id mcp_client \
      --callback-port 3334 ombuto-ost https://ost.example.com/mcp
    claude mcp get ombuto-ost

The real client reported `OAuth: client_id configured, callback_port 3334`.
The example host was deliberately unreachable; this check records that the
client accepts and retains the static public-client configuration. It is not a
substitute for the live browser and expiry evidence recorded in Steps 2 and 3.

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
    #      "scopes_supported": [ "openid", "profile", "email", "roles", "offline_access" ] }

If the challenge or metadata shape differs from that, stop here and fix the
server — the client will not be able to discover the authorization server.

## Step 2 — Connect from an MCP client and complete the browser flow

1. Save the `.mcp.json` snippet the in-app **Connect an agent** page prints
   into the client's project directory. In addition to `type: "http"` and
   `url: "${APP_ORIGIN}/mcp"`, it carries the documented Claude Code settings
   `oauth.clientId: "mcp_client"` and `oauth.callbackPort: 3334`. This is the
   JSON form of `claude mcp add --client-id mcp_client --callback-port 3334`.
   It contains no token or client secret.
2. Start Claude Code, approve the project's `ombuto-ost` server when prompted,
   then choose it in `/mcp` (or run `claude mcp login ombuto-ost`).
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
  the challenge shape at `T2` — the server must return the _same_
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
- `POST /mcp` with an _expired_ bearer token returns 401 with the same
  `resource_metadata` challenge plus `error="invalid_token"` and an
  expired-token description — the shape the client keys off to trigger its
  refresh-token exchange.

Those tests prove the server keeps its side of the bargain on every commit.
They do not, and cannot, prove that a specific desktop MCP client actually
runs the browser flow and refreshes — that is what Steps 2 and 3 above are
for.
