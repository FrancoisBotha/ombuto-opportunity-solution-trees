import { computed, defineComponent, ref } from 'vue';

export interface McpTool {
  name: string;
  description: string;
}

const TOOLS: McpTool[] = [
  { name: 'list_products', description: 'Lists the products (with team) the caller may read.' },
  { name: 'get_tree', description: "Returns a team's whole opportunity solution tree, optionally limited to one product." },
  {
    name: 'get_node',
    description:
      'Returns one node by type and id with its details — parent, direct children, links (target, type and title), open questions (text and resolved state, for opportunities), and evidence / comment counts where they apply.',
  },
  { name: 'list_interviews', description: 'Returns interviews for a product or team with their linked opportunities.' },
  { name: 'list_node_comments', description: 'Returns the discussion thread on one node — body, author and times, oldest first.' },
];

const VERIFIED_CLIENT_NAME = 'Claude Code';
const VERIFIED_CLIENT_VERSION = '2.1.278';
const TRANSPORT_TYPE = 'http';
const MCP_CLIENT_ID = 'mcp_client';

const DEV_KEYCLOAK_ORIGIN = 'http://localhost:9080';

function deriveKeycloakOrigin(appOrigin: string): string {
  try {
    const url = new URL(appOrigin);
    if (url.hostname === 'localhost' || url.hostname === '127.0.0.1') {
      return DEV_KEYCLOAK_ORIGIN;
    }
    return `${url.protocol}//${url.hostname.replace(/^app\./, 'auth.')}`;
  } catch {
    return DEV_KEYCLOAK_ORIGIN;
  }
}

export default defineComponent({
  name: 'ConnectAgent',
  setup() {
    const origin = ref(typeof window !== 'undefined' ? window.location.origin : '');
    const endpointUrl = computed(() => `${origin.value}/mcp`);
    const metadataUrl = computed(() => `${origin.value}/.well-known/oauth-protected-resource`);
    const keycloakOrigin = computed(() => deriveKeycloakOrigin(origin.value));
    const tokenUrl = computed(() => `${keycloakOrigin.value}/realms/jhipster/protocol/openid-connect/token`);
    const copyState = ref<{ endpoint: boolean; config: boolean }>({ endpoint: false, config: false });

    const clientConfigSnippet = computed(() => {
      const cfg = {
        mcpServers: {
          'ombuto-ost': {
            type: TRANSPORT_TYPE,
            url: endpointUrl.value,
          },
        },
      };
      return JSON.stringify(cfg, null, 2);
    });

    const copy = async (key: 'endpoint' | 'config', text: string) => {
      try {
        await navigator.clipboard.writeText(text);
        copyState.value[key] = true;
        setTimeout(() => {
          copyState.value[key] = false;
        }, 1500);
      } catch {
        // clipboard not available — user can still select and copy manually
      }
    };

    return {
      tools: TOOLS,
      origin,
      endpointUrl,
      metadataUrl,
      keycloakOrigin,
      tokenUrl,
      clientConfigSnippet,
      copyState,
      copy,
      verifiedClientName: VERIFIED_CLIENT_NAME,
      verifiedClientVersion: VERIFIED_CLIENT_VERSION,
      mcpClientId: MCP_CLIENT_ID,
    };
  },
});
