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
        Verified against <strong data-cy="verifiedClient">{{ verifiedClientName }} {{ verifiedClientVersion }}</strong> over the Streamable
        HTTP transport. Save this as <code>.mcp.json</code> in your project, replace the token placeholder, then start Claude Code and
        approve the project server when prompted.
      </p>
      <div class="d-flex align-items-start gap-2">
        <pre class="p-2 bg-light border rounded flex-grow-1 mb-0" data-cy="clientConfigSnippet">{{ clientConfigSnippet }}</pre>
        <button class="btn btn-outline-secondary" type="button" data-cy="copyConfigButton" @click="copy('config', clientConfigSnippet)">
          <font-awesome-icon icon="copy"></font-awesome-icon>
          <span> {{ copyState.config ? 'Copied' : 'Copy' }}</span>
        </button>
      </div>
    </section>

    <section class="card p-3 mb-3" data-cy="tokenInstructions">
      <h4>Obtain a bearer token</h4>
      <p>
        The MCP endpoint accepts a Keycloak-issued OAuth2 access token as an <code>Authorization: Bearer</code> header. Use the dev-realm
        public client <code>mcp_client</code> (added in ticket MCPSRV-002) — it has direct-access grants enabled and PKCE, and matches the
        audience the endpoint validates.
      </p>
      <p>
        Keycloak runs on its own host and port (in dev,
        <code data-cy="keycloakOrigin">{{ keycloakOrigin }}</code
        >). Quickest path for a desktop client that cannot yet do interactive OAuth is a one-shot direct-access-grant call against the
        realm's token endpoint:
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
        Copy the <code>access_token</code> field from the response into the <code>Authorization</code> header of your MCP client
        configuration. Tokens expire; refresh when your client reports a 401.
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
