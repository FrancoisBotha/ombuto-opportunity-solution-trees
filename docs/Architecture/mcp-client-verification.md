# MCP client verification note

Companion note for MCPSRV-006. This records an end to end verification against the locally running Ombuto OST application on 20 September 2026.

## Environment and clients

- Ombuto OST backend: `http://localhost:8080`
- Keycloak realm: `http://localhost:9080/realms/jhipster`
- MCP transport: legacy HTTP Server-Sent Events (SSE), endpoint `/mcp`, message endpoint `/mcp/message`
- Client used for all four calls: official MCP Java SDK `io.modelcontextprotocol.sdk:mcp:2.0.0`
- Page configuration checked with: Claude Code `2.1.278`
- Authenticated identity: seeded `admin`, a member of `Team Jupiter`

The SDK initialization response negotiated protocol `2024-11-05` and identified the server as `opportunity-solution-tree-mcp` version `0.0.1`.

## Token acquisition

The bearer token came from the public Keycloak client added by MCPSRV-002:

```bash
curl -s -X POST http://localhost:9080/realms/jhipster/protocol/openid-connect/token \
  -d client_id=mcp_client \
  -d grant_type=password \
  -d 'scope=openid profile email roles' \
  -d username=admin \
  -d password=admin
```

The `access_token` from that response was used as `Bearer <access-token>` below. The token value is omitted from this note.

## Exact configuration

Claude Code generated and accepted this `.mcp.json` shape when invoked with `claude mcp add --transport sse`:

```json
{
  "mcpServers": {
    "ombuto-ost": {
      "type": "sse",
      "url": "http://localhost:8080/mcp",
      "headers": {
        "Authorization": "Bearer <access-token>"
      }
    }
  }
}
```

The four calls were made with the official Java client using the equivalent configuration:

```java
HttpClientSseClientTransport transport = HttpClientSseClientTransport.builder("http://localhost:8080")
    .sseEndpoint("/mcp")
    .requestBuilder(HttpRequest.newBuilder().header("Authorization", "Bearer " + token))
    .build();
McpSyncClient client = McpClient.sync(transport)
    .requestTimeout(Duration.ofSeconds(30))
    .initializationTimeout(Duration.ofSeconds(30))
    .build();
client.initialize();
```

`tools/list` returned five registered tools:

```text
[get_node, get_tree, ping, list_interviews, list_products]
```

`ping` is the transport probe registered by MCPSRV-001. The product feature consists of the other four read only tools, and all four were called successfully below.

## Observed calls

### `list_products`

Arguments: `{}`

```json
{
  "total": 2,
  "products": [
    { "id": 1101, "title": "Discovery Canvas", "teamId": 1001, "teamName": "Team Jupiter", "archived": false },
    { "id": 1102, "title": "Insight Library", "teamId": 1001, "teamName": "Team Jupiter", "archived": false }
  ]
}
```

### `get_tree`

Arguments: `{ "teamId": 1001, "productId": 1101 }`

The response reported `nodeCount: 14`, `nodeCeiling: 1000`, `overflow: false`, and `message: null`. Its first three nodes were:

```json
{
  "teamId": 1001,
  "teamName": "Team Jupiter",
  "nodeCount": 14,
  "nodeCeiling": 1000,
  "overflow": false,
  "message": null,
  "nodes": [
    { "id": 1101, "type": "PRODUCT", "title": "Discovery Canvas", "status": null, "parentType": null, "parentId": null },
    {
      "id": 1201,
      "type": "OUTCOME",
      "title": "Teams running ≥1 interview a week: 34% → 60%",
      "status": null,
      "parentType": "PRODUCT",
      "parentId": 1101
    },
    {
      "id": 1301,
      "type": "OPPORTUNITY",
      "title": "I can never find time to schedule interviews",
      "status": "EXPLORING",
      "parentType": "OUTCOME",
      "parentId": 1201
    }
  ]
}
```

Eleven more nodes followed in the same flat preorder response.

### `get_node`

Arguments: `{ "type": "PRODUCT", "id": 1101 }`

```json
{
  "type": "PRODUCT",
  "id": 1101,
  "title": "Discovery Canvas",
  "description": null,
  "status": null,
  "parent": null,
  "children": [{ "type": "OUTCOME", "id": 1201, "title": "Teams running ≥1 interview a week: 34% → 60%" }],
  "linkCount": 1,
  "evidenceCount": null,
  "commentCount": null
}
```

### `list_interviews`

Arguments: `{ "productId": 1101 }`

```json
{
  "teamId": 1001,
  "productId": 1101,
  "page": 0,
  "size": 20,
  "limit": 50,
  "totalMatching": 0,
  "notesIncluded": true,
  "interviews": []
}
```

An empty list is the observed seeded data result; the call completed successfully and retained the caller's team and notes visibility metadata.

## Removed token verification

The same SDK client was created without its `Authorization` request header. `client.initialize()` failed with:

```text
java.lang.RuntimeException: Client failed to initialize by explicit API call
```

The literal HTTP response observed for `GET /mcp` without the token was:

```http
HTTP/1.1 401
WWW-Authenticate: Bearer resource_metadata="http://127.0.0.1:8080/.well-known/oauth-protected-resource"
Content-Length: 0
```

The body was empty (zero bytes), matching Spring Security's `BearerTokenAuthenticationEntryPoint`. Restoring a fresh `mcp_client` token allowed initialization and all four calls above.

## Ticket notes summary

Verified the official MCP Java SDK 2.0.0 over SSE against the local app with a bearer token from Keycloak's `mcp_client`. Discovery returned `ping` plus the four product tools. `list_products`, `get_tree`, `get_node`, and `list_interviews` all returned the caller's Team Jupiter data. Removing the token made SDK initialization fail and produced HTTP 401 with an empty body and a Bearer challenge. The page's `.mcp.json` shape was generated and accepted by Claude Code 2.1.278.
