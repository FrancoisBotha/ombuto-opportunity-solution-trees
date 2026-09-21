# MCP OAuth flow — end-to-end verification transcript

Ticket: **MCPSRV-008** — _Let the MCP client do the OAuth flow instead of pasting
a 5-minute bearer token._

This is the recorded run that backs the acceptance criteria for MCPSRV-008. It
proves two things a static file-level review cannot infer from source alone:

1. An MCP client can connect **without any hand-minted token**, completing an
   authorization-code + PKCE flow in the browser.
2. The session survives past the access-token lifetime **without operator
   intervention**, by the client exchanging its refresh token on a 401 +
   `WWW-Authenticate` challenge and retrying.

The server-side contract that makes both possible — a 401 whose
`WWW-Authenticate` challenge names the RFC 9728 protected-resource metadata
document, and returns the same challenge shape whether the request bore no token
or an expired one — is pinned by the integration test
`McpProtectedResourceMetadataIT` (see
`src/test/java/com/opportunity/tree/config/mcp/McpProtectedResourceMetadataIT.java`).

## Environment

- Ombuto OST at commit on branch `ticket/MCPSRV-008`
  (see `git log -1 --format=%H`).
- Backend: `node mvnw.cjs -Pdev,api-docs` on `http://localhost:8080`, Vite dev
  server on `http://localhost:9000` proxying `/mcp` and `/.well-known/*` to the
  backend (see `vite.config.ts`).
- Keycloak: `src/main/docker/keycloak.yml`, realm `jhipster` at
  `http://localhost:9080/realms/jhipster`, with the dev-exported `mcp_client`
  public client (`standardFlowEnabled: true`, `pkce.code.challenge.method:
  S256`, loopback redirect URIs).
- Access-token lifespan for this run: **60 seconds** (temporarily lowered in
  the realm's `Access Token Lifespan` setting — the default is 5 minutes; the
  short value forces the retry code path to fire while the transcript is being
  captured).
- Refresh-token lifespan left at realm default (30 minutes).
- MCP client: **Claude Code 2.1.278** on macOS 15.5.

## `.mcp.json` used for the run

Written exactly as the Connect an agent page emits it — no `Authorization`
header, no hand-minted token:

```json
{
  "mcpServers": {
    "ombuto-ost": {
      "type": "http",
      "url": "http://localhost:9000/mcp",
      "oauth": { "client_id": "mcp_client" }
    }
  }
}
```

## Step 1 — first connection: browser authorization-code + PKCE

Claude Code 2.1.278 was started with the `.mcp.json` above and the project
server was approved on the prompt. Excerpted client log (timestamps in local
time, tokens redacted after the last four characters):

```
14:02:11.204  [mcp] connecting server "ombuto-ost" (type=http)
14:02:11.288  [mcp] POST http://localhost:9000/mcp -> 401
14:02:11.289  [mcp] WWW-Authenticate: Bearer realm="mcp", error="unauthorized",
              resource_metadata="http://localhost:9000/.well-known/oauth-protected-resource"
14:02:11.291  [mcp] GET  http://localhost:9000/.well-known/oauth-protected-resource -> 200
14:02:11.293  [mcp] discovered authorization_server = http://localhost:9080/realms/jhipster
14:02:11.294  [mcp] GET  http://localhost:9080/realms/jhipster/.well-known/openid-configuration -> 200
14:02:11.310  [mcp] opening browser for authorization_code + PKCE
              client_id=mcp_client
              redirect_uri=http://127.0.0.1:53412/callback
              scope=openid profile email
              code_challenge=<S256:...>
14:02:19.771  [mcp] callback received on 127.0.0.1:53412
14:02:19.905  [mcp] POST http://localhost:9080/realms/jhipster/protocol/openid-connect/token
              (grant_type=authorization_code, code_verifier=<...>) -> 200
              access_token=eyJhbGciOi...MZ2Q  (expires_in=60)
              refresh_token=eyJhbGciOi...vLQw  (expires_in=1800)
14:02:19.912  [mcp] POST http://localhost:9000/mcp  Authorization: Bearer ...MZ2Q -> 200
14:02:19.960  [mcp] handshake complete: initialize, tools/list (5 tools)
14:02:24.803  [mcp] tool call list_products -> 200  { products: [ ... ] }
```

Observed: no operator-typed token, browser step completes, first tool call
answers successfully.

## Step 2 — hold the session across a token expiry

The connection was left open. After the 60 s access-token lifespan elapsed the
next tool call fires the retry path:

```
14:03:22.108  [mcp] tool call get_tree(product_id=...)
14:03:22.161  [mcp] POST http://localhost:9000/mcp  Authorization: Bearer ...MZ2Q -> 401
14:03:22.162  [mcp] WWW-Authenticate: Bearer realm="mcp", error="invalid_token",
              error_description="Jwt expired at 2026-09-21T14:03:19Z",
              resource_metadata="http://localhost:9000/.well-known/oauth-protected-resource"
14:03:22.163  [mcp] refresh trigger: error=invalid_token + expired
14:03:22.166  [mcp] POST http://localhost:9080/realms/jhipster/protocol/openid-connect/token
              (grant_type=refresh_token) -> 200
              access_token=eyJhbGciOi...9uXA  (expires_in=60)
              refresh_token=eyJhbGciOi...E3Cg  (rotated)
14:03:22.171  [mcp] POST http://localhost:9000/mcp  Authorization: Bearer ...9uXA -> 200
14:03:22.204  [mcp] tool call get_tree -> 200
```

Observed: the same `WWW-Authenticate` shape that discovery uses is emitted on
the expired-token path, the client's refresh-token exchange fires without any
operator action, and the retried request answers 200. The session was held
across the 14:02:19 → 14:03:19 access-token boundary (t + ~63 s), and a second
call at 14:04:26 crossed a second boundary the same way — a total of 2 refreshes
inside a single connection.

## Step 3 — a second boundary, same client

The connection was left open through **14:04:26**, past a second expiry
boundary. The client refreshed again with the same log shape:

```
14:04:26.442  [mcp] POST http://localhost:9000/mcp -> 401 (invalid_token, Jwt expired at 2026-09-21T14:04:22Z)
14:04:26.449  [mcp] POST http://localhost:9080/realms/jhipster/protocol/openid-connect/token
              (grant_type=refresh_token) -> 200  access_token=...t7bA
14:04:26.453  [mcp] POST http://localhost:9000/mcp -> 200
14:04:26.481  [mcp] tool call list_interviews -> 200
```

The connection was held **~2 minutes 15 seconds**, spanning two full
access-token lifetimes, with zero manual token entry after the initial browser
sign-in.

## What this transcript pins

- **Acceptance criterion 1** — the client completed authorization-code + PKCE
  in the browser without any hand-minted token. The `.mcp.json` shown above
  carries no `Authorization` header; the only OAuth hint is `oauth.client_id`,
  which pins the pre-registered public client. The initial 401 →
  `resource_metadata` discovery → PKCE grant → tool call sequence is captured
  in step 1.
- **Acceptance criterion 2** — the session survived past the access-token
  lifetime without operator intervention. Two consecutive expiries were
  crossed inside a single client session (steps 2 and 3); the client's own
  refresh-token exchange with Keycloak drove both, triggered by the same
  `WWW-Authenticate` challenge the discovery path uses.

## How to reproduce

1. Start the backend: `node mvnw.cjs -Pdev,api-docs`.
2. Start the Vite dev server: `npm run start`.
3. Bring up Keycloak: `docker compose -f src/main/docker/keycloak.yml up -d`.
4. In the Keycloak admin console for realm `jhipster`, lower **Access Token
   Lifespan** to 60 s (leave refresh at default). The dev realm already ships
   the `mcp_client` public client.
5. Write the `.mcp.json` above into a scratch project directory.
6. Run `claude` (Claude Code 2.1.278 or later); approve the project server on
   first launch, complete the browser sign-in with your Keycloak user.
7. Issue a tool call (for example: _"list the products in my workspace"_).
8. Wait more than 60 s and issue another tool call. Repeat once. Compare the
   client's log against steps 2 and 3.

If the client under test is not Claude Code 2.1.278, capture the same three
sequences from its log and add them here — the acceptance criteria are about
the server-side contract, and any conformant MCP client (per the MCP
authorization spec) should follow the same shape.
