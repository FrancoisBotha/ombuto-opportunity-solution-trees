<template>
  <div class="connect-agent" data-cy="connectAgentPage">
    <h2 id="page-heading">
      <span>Connect an agent</span>
    </h2>

    <p class="lead">
      Point your LLM agent tool at this workspace so it can read your teams' products, trees, nodes and interviews — using your Keycloak
      identity, with the same team scoping as the web UI.
    </p>

    <section class="card p-3 mb-3">
      <h4>MCP endpoint</h4>
      <p class="text-muted small mb-2">
        The server exposes an MCP endpoint over the Streamable HTTP transport. The URL below is derived from the current origin, so it works
        whether you are running on localhost, a preview environment or production.
      </p>
      <div class="d-flex align-items-center gap-2">
        <code class="p-2 bg-light border rounded flex-grow-1" data-cy="mcpEndpointUrl">{{ endpointUrl }}</code>
        <button class="btn btn-outline-secondary" type="button" data-cy="copyEndpointButton" @click="copy('endpoint', endpointUrl)">
          <font-awesome-icon icon="copy"></font-awesome-icon>
          <span> {{ copyState.endpoint ? 'Copied' : 'Copy' }}</span>
        </button>
      </div>
    </section>

    <section class="card p-3 mb-3">
      <h4>Client configuration</h4>
      <p class="text-muted small mb-2">
        Configuration for <strong data-cy="verifiedClient">{{ verifiedClientName }} {{ verifiedClientVersion }}+</strong> over the
        Streamable HTTP transport. Save this as <code>.mcp.json</code> in your project, then start Claude Code and approve the project
        server when prompted. The client opens your browser to sign in with Keycloak the first time and refreshes tokens on its own — no
        bearer token to paste and no expiry to manage.
      </p>
      <div class="d-flex align-items-start gap-2">
        <pre class="p-2 bg-light border rounded flex-grow-1 mb-0" data-cy="clientConfigSnippet">{{ clientConfigSnippet }}</pre>
        <button class="btn btn-outline-secondary" type="button" data-cy="copyConfigButton" @click="copy('config', clientConfigSnippet)">
          <font-awesome-icon icon="copy"></font-awesome-icon>
          <span> {{ copyState.config ? 'Copied' : 'Copy' }}</span>
        </button>
      </div>
    </section>

    <section class="card p-3 mb-3" data-cy="authFlowInstructions">
      <h4>How authorization works</h4>
      <p>
        The MCP client discovers the authorization server automatically, following the MCP authorization specification. On its first
        request, the server answers <code>401</code> with a <code>WWW-Authenticate</code> challenge that names the protected-resource
        metadata document at <code data-cy="metadataUrl">{{ metadataUrl }}</code
        >. The client fetches that document, learns which Keycloak realm to talk to, and runs an
        <strong>authorization-code + PKCE</strong> flow in your browser. Access tokens are refreshed by the client — the connection survives
        past a token's lifetime without your intervention: when the current access token expires, the server returns the same
        <code>401</code> + <code>WWW-Authenticate</code>
        challenge (see the metadata + expired-token integration test), which triggers the client's refresh-token exchange with Keycloak and
        the request is retried with a fresh token.
      </p>
      <p>
        The Keycloak realm ships a public client called
        <code data-cy="mcpClientId">{{ mcpClientId }}</code> for MCP callers, with PKCE enforced (<code>S256</code>) and loopback redirect
        URIs (<code>http://127.0.0.1:*</code>, <code>http://localhost:*</code>, and their HTTPS variants) so any desktop MCP client can
        complete the browser step. Direct-access (password) grants are <strong>not required</strong> and are turned off in production.
      </p>
      <p class="mb-0" data-cy="clientIdInstructions">
        The <code>.mcp.json</code> snippet above carries only the transport type and the server URL — no bearer token, no static
        <code>Authorization</code> header, no client secret. The MCP client learns everything else (authorization server, scopes, and the
        pre-registered public client id <code>mcp_client</code>) from the <code>WWW-Authenticate</code> challenge and the protected-resource
        metadata document; the browser sign-in is opened against a loopback redirect URI Keycloak already accepts for
        <code>mcp_client</code>. Claude Code stores the resulting tokens and refreshes them automatically, so the connection survives past
        an access token's lifetime with no manual intervention.
      </p>
    </section>

    <section class="card p-3 mb-3" data-cy="tokenInstructions">
      <h4>Local-development fallback: mint a token by hand</h4>
      <p class="text-warning small mb-2">
        <strong>Local development only.</strong> Use this only if your MCP client cannot yet run the authorization-code flow. It relies on
        the direct-access-grant capability of the dev realm, which is disabled in production; the pasted token expires in five minutes and
        the client will not refresh it.
      </p>
      <p>
        Keycloak runs on its own host and port (in dev,
        <code data-cy="keycloakOrigin">{{ keycloakOrigin }}</code
        >). Run the one-shot direct-access-grant curl against the realm's token endpoint:
      </p>
      <pre class="p-2 bg-light border rounded" data-cy="tokenCurl">
curl -s -X POST \
  '{{ tokenUrl }}' \
  -d 'client_id=mcp_client' \
  -d 'grant_type=password' \
  -d 'scope=openid profile email roles' \
  -d 'username=&lt;your keycloak username&gt;' \
  -d 'password=&lt;your keycloak password&gt;'</pre
      >
      <p class="small text-muted mb-0">
        Copy the <code>access_token</code> field from the response into an <code>Authorization: Bearer</code> header in your MCP client
        config. This is a temporary workaround; prefer the OAuth flow above.
      </p>
    </section>

    <section class="card p-3 mb-3">
      <h4>Available tools</h4>
      <p class="text-muted small mb-2" data-cy="accessScopingNote">
        All tools are read-only. Access mirrors your team memberships — an agent sees exactly what you see in the web UI, and nothing else.
      </p>
      <ul class="list-unstyled mb-0">
        <li v-for="tool in tools" :key="tool.name" class="mb-2" :data-cy="`mcpTool-${tool.name}`">
          <code :data-cy="`mcpToolName-${tool.name}`">{{ tool.name }}</code>
          <span class="text-muted"> — </span>
          <span :data-cy="`mcpToolDescription-${tool.name}`">{{ tool.description }}</span>
        </li>
      </ul>
    </section>
  </div>
</template>

<script lang="ts" src="./connect-agent.component.ts"></script>

<style scoped>
.connect-agent pre {
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 0.85em;
}
.connect-agent code {
  font-size: 0.95em;
}
</style>
