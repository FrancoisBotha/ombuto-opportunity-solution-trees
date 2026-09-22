import { existsSync, readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';

import { beforeEach, describe, expect, it } from 'vitest';

import { findIconDefinition } from '@fortawesome/fontawesome-svg-core';
import { shallowMount } from '@vue/test-utils';

import { initFortAwesome } from '@/shared/config/config';
import ConnectAgent from './connect-agent.vue';

function projectRoot(): string {
  let dir = process.cwd();
  while (!existsSync(join(dir, 'vite.config.ts')) && dirname(dir) !== dir) dir = dirname(dir);
  return dir;
}

function viteProxyPaths(): string[] {
  const config = readFileSync(join(projectRoot(), 'vite.config.ts'), 'utf8');
  const match = /proxy:[\s\S]*?\[([^\]]*)\]\.map/.exec(config);
  return match ? [...match[1].matchAll(/'([^']+)'/g)].map(m => m[1]) : [];
}

describe('ConnectAgent Component', () => {
  beforeEach(() => {
    Object.defineProperty(window, 'location', {
      writable: true,
      value: { origin: 'https://ost.example.com' } as Location,
    });
  });

  it('renders the page heading', () => {
    const wrapper = shallowMount(ConnectAgent, {
      global: { stubs: { 'font-awesome-icon': true } },
    });
    expect(wrapper.find('[data-cy="connectAgentPage"]').exists()).toBe(true);
    expect(wrapper.text()).toContain('Connect an agent');
  });

  it('shows the MCP endpoint derived from the current origin (not hard-coded)', () => {
    const wrapper = shallowMount(ConnectAgent, {
      global: { stubs: { 'font-awesome-icon': true } },
    });
    const endpoint = wrapper.find('[data-cy="mcpEndpointUrl"]').text();
    expect(endpoint).toBe('https://ost.example.com/mcp');
    expect(endpoint).not.toBe('http://localhost:8080/mcp');
  });

  it('recomputes the endpoint when running under a different origin', () => {
    Object.defineProperty(window, 'location', {
      writable: true,
      value: { origin: 'http://localhost:9000' } as Location,
    });
    const wrapper = shallowMount(ConnectAgent, {
      global: { stubs: { 'font-awesome-icon': true } },
    });
    expect(wrapper.find('[data-cy="mcpEndpointUrl"]').text()).toBe('http://localhost:9000/mcp');
  });

  it('lists the MCP tools with a one-line description each', () => {
    const wrapper = shallowMount(ConnectAgent, {
      global: { stubs: { 'font-awesome-icon': true } },
    });
    const rows = wrapper.findAll('[data-cy^="mcpTool-"]');
    expect(rows).toHaveLength(5);
    const names = rows.map(r => r.find('[data-cy^="mcpToolName-"]').text());
    expect(names).toEqual(expect.arrayContaining(['list_products', 'get_tree', 'get_node', 'list_interviews', 'list_node_comments']));
    for (const row of rows) {
      const desc = row.find('[data-cy^="mcpToolDescription-"]').text();
      expect(desc.length).toBeGreaterThan(0);
    }
  });

  it('names the verified MCP client and version', () => {
    const wrapper = shallowMount(ConnectAgent, {
      global: { stubs: { 'font-awesome-icon': true } },
    });
    expect(wrapper.find('[data-cy="verifiedClient"]').exists()).toBe(true);
    expect(wrapper.find('[data-cy="verifiedClient"]').text().length).toBeGreaterThan(0);
  });

  it('shows a valid Claude Code Streamable HTTP configuration with no bearer token to paste', () => {
    const wrapper = shallowMount(ConnectAgent, {
      global: { stubs: { 'font-awesome-icon': true } },
    });
    const config = JSON.parse(wrapper.find('[data-cy="clientConfigSnippet"]').text());
    expect(wrapper.find('[data-cy="verifiedClient"]').text()).toContain('Claude Code 2.1.278');
    expect(config.mcpServers['ombuto-ost']).toMatchObject({
      type: 'http',
      url: 'https://ost.example.com/mcp',
      oauth: {
        clientId: 'mcp_client',
        callbackPort: 3334,
      },
    });
    // The Streamable HTTP transport is expressed via `type: "http"`; the deprecated `type: "sse"`
    // must not creep back in — MCP clients treat it as the HTTP+SSE transport instead.
    expect(config.mcpServers['ombuto-ost'].type).not.toBe('sse');
    expect(config.mcpServers['ombuto-ost']).not.toHaveProperty('transport');
    // MCPSRV-008: OAuth uses a public client id and PKCE; no static Authorization header is stored.
    expect(config.mcpServers['ombuto-ost']).not.toHaveProperty('headers');
  });

  it('uses Claude Codes documented fields for the pre-registered public client', () => {
    const wrapper = shallowMount(ConnectAgent, {
      global: { stubs: { 'font-awesome-icon': true } },
    });
    expect(wrapper.find('[data-cy="mcpClientId"]').text()).toBe('mcp_client');
    const instr = wrapper.find('[data-cy="clientIdInstructions"]').text();
    expect(instr).toContain('mcp_client');
    expect(instr).toContain('oauth.clientId');
    expect(instr).toContain('oauth.callbackPort');
  });

  it('registers the icons used by the page and its account-menu entry', () => {
    initFortAwesome({ component: () => undefined } as any);
    expect(findIconDefinition({ prefix: 'fas', iconName: 'copy' })).toBeDefined();
    expect(findIconDefinition({ prefix: 'fas', iconName: 'plug' })).toBeDefined();
  });

  it('documents the OAuth authorization-code + PKCE flow discovered from the protected-resource metadata', () => {
    const wrapper = shallowMount(ConnectAgent, {
      global: { stubs: { 'font-awesome-icon': true } },
    });
    const text = wrapper.find('[data-cy="authFlowInstructions"]').text();
    expect(text.toLowerCase()).toContain('pkce');
    expect(text.toLowerCase()).toContain('authorization-code');
    expect(text).toContain('mcp_client');
    expect(wrapper.find('[data-cy="metadataUrl"]').text()).toBe('https://ost.example.com/.well-known/oauth-protected-resource');
  });

  it('marks the direct-access-grant curl as a local-development fallback only', () => {
    const wrapper = shallowMount(ConnectAgent, {
      global: { stubs: { 'font-awesome-icon': true } },
    });
    const text = wrapper.find('[data-cy="tokenInstructions"]').text();
    expect(text.toLowerCase()).toContain('local development');
    expect(text).toContain('mcp_client');
  });

  it('renders the Keycloak token endpoint on port 9080 without /auth prefix and includes the scope', () => {
    Object.defineProperty(window, 'location', {
      writable: true,
      value: { origin: 'http://localhost:9000' } as Location,
    });
    const wrapper = shallowMount(ConnectAgent, {
      global: { stubs: { 'font-awesome-icon': true } },
    });
    const curl = wrapper.find('[data-cy="tokenCurl"]').text();
    expect(curl).toContain('http://localhost:9080/realms/jhipster/protocol/openid-connect/token');
    expect(curl).not.toContain('/auth/realms/');
    expect(curl).toContain('scope=openid profile email roles');
  });

  it('routes the displayed dev endpoint to the backend via the Vite proxy', () => {
    Object.defineProperty(window, 'location', {
      writable: true,
      value: { origin: 'http://localhost:9000' } as Location,
    });
    const wrapper = shallowMount(ConnectAgent, {
      global: { stubs: { 'font-awesome-icon': true } },
    });
    const endpoint = wrapper.find('[data-cy="mcpEndpointUrl"]').text();
    const path = new URL(endpoint).pathname;
    expect(viteProxyPaths()).toContain(path);
  });

  it("documents get_node's new open-questions and links fields (MCPSRV-012)", () => {
    const wrapper = shallowMount(ConnectAgent, {
      global: { stubs: { 'font-awesome-icon': true } },
    });
    const desc = wrapper.find('[data-cy="mcpToolDescription-get_node"]').text().toLowerCase();
    expect(desc).toContain('open question');
    expect(desc).toContain('link');
    expect(desc).toContain('target');
    expect(desc).toContain('resolved');
  });

  it('states that access mirrors the caller team memberships', () => {
    const wrapper = shallowMount(ConnectAgent, {
      global: { stubs: { 'font-awesome-icon': true } },
    });
    expect(wrapper.find('[data-cy="accessScopingNote"]').text().toLowerCase()).toContain('team');
  });
});
