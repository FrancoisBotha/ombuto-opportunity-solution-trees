import { computed, defineComponent, ref } from 'vue';

export interface McpTool {
  name: string;
  description: string;
}

const TOOLS: McpTool[] = [
  { name: 'list_products', description: 'Lists the products (with team) the caller may read.' },
  { name: 'get_tree', description: "Returns a team's whole opportunity solution tree, optionally limited to one product." },
  { name: 'get_node', description: 'Returns one node (outcome, opportunity or solution) by type and id with its details.' },
  { name: 'list_interviews', description: 'Returns interviews for a product or team with their linked opportunities.' },
];

const VERIFIED_CLIENT_NAME = 'Claude Code';
const VERIFIED_CLIENT_VERSION = '2.1.278';

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
    const keycloakOrigin = computed(() => deriveKeycloakOrigin(origin.value));
    const tokenUrl = computed(() => `${keycloakOrigin.value}/realms/jhipster/protocol/openid-connect/token`);
    const copyState = ref<{ endpoint: boolean; config: boolean }>({ endpoint: false, config: false });

    const clientConfigSnippet = computed(() => {
      const cfg = {
        mcpServers: {
          'ombuto-ost': {
            type: 'sse',
            url: endpointUrl.value,
            headers: {
              Authorization: 'Bearer <paste-access-token-here>',
            },
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
      keycloakOrigin,
      tokenUrl,
      clientConfigSnippet,
      copyState,
      copy,
      verifiedClientName: VERIFIED_CLIENT_NAME,
      verifiedClientVersion: VERIFIED_CLIENT_VERSION,
    };
  },
});
