# Epic 10: MCP Server

Status: TICKETS
Owner: human
Created: 2026-09-18
Last Updated: 2026-09-20
Depends On: epic_07_INTERVIEWS_AS_EVIDENCE

---

## 1. Purpose
Make the tree available to LLM agent tools. A read-only MCP server inside the
monolith exposes trees and interviews through the same services — and therefore
the same team scoping — as the web UI.

For this epic the "real UI" is the user's MCP client (e.g. Claude Code or Claude
Desktop): the end-to-end task is a user asking their agent about their tree.

**Working increment:** A user can connect their LLM agent tool to Ombuto OST with
their Keycloak identity and have it read their teams' products, trees, nodes and
interviews — and nothing they are not allowed to see.

## 2. User Story
As a member of a product trio, I want my AI assistant to read our opportunity
solution tree, So that I can ask it questions and draft work grounded in our
actual discovery data.

## 3. Scope
- **In Scope:** Spring AI MCP server starter (WebMVC) in the same application;
  read-only tools `list_products`, `get_tree`, `get_node`, `list_interviews`;
  Keycloak bearer-token authentication on the MCP endpoint; scoping through
  `TeamAccessService` (including `ROLE_OVERVIEW` rules if Epic 9 is done); an
  in-app "Connect an agent" help page with the endpoint URL and example client
  configuration.
- **Out of Scope:** any write tool; MCP resources/prompts beyond the four tools;
  per-tool permissions or API keys managed in the app; a separate MCP process.

## 4. Functional Requirements
1. FR-054 — The application exposes an MCP endpoint served by the same Spring Boot process using Spring AI's MCP server starter (WebMVC).
2. FR-055 — The MCP endpoint authenticates callers by Keycloak bearer token and resolves them to the same application user as the web UI; calls without a valid token are refused.
3. FR-056 — `list_products` returns the products (with team) the caller may read; `get_tree` returns a team's whole tree, optionally limited to one product.
4. FR-057 — `get_node` returns one node by type and id with its details (children summary, links, evidence and comment counts where those features exist).
5. FR-058 — `list_interviews` returns interviews for a product or team with their linked opportunities, applying the same notes-visibility rules as the web UI.
6. FR-059 — A "Connect an agent" page in the app shows the MCP endpoint URL and copy-paste client configuration, including how to obtain a token.

## 5. Non-Functional Requirements
1. NFR-019 — Security: the MCP server exposes no tool that modifies data, and every tool call goes through `TeamAccessService`.
2. NFR-020 — Security: bearer tokens are validated for signature, issuer, audience and expiry; the MCP endpoint is stateless and exempt from CSRF without weakening CSRF for the session-based API.
3. NFR-021 — Robustness: tool responses are structured JSON with bounded size (large trees can be requested per product) and tool descriptions are clear enough for an agent to choose correctly.

## 6. UI/UX Notes
- "Connect an agent" page under the user menu: endpoint URL, example
  configuration snippets, the list of tools with one-line descriptions, and a
  note that access mirrors the user's team memberships.
- Tool output uses titles and statuses, not internal ids alone, so agents can
  answer in the user's vocabulary.

## 7. Data Model Impact
None.

## 8. Integration Impact
- New dependency: Spring AI MCP server starter (approved by the Architecture
  document).
- `SecurityConfiguration`: a second, stateless resource-server filter chain for
  the MCP path alongside the existing session login; Keycloak dev realm gains a
  client / audience suitable for MCP callers.
- Tools call the existing tree and interview services.

## 9. Acceptance Criteria
- [ ] The application builds and runs, and a user can connect their LLM agent tool to Ombuto OST with their Keycloak identity and have it read their teams' products, trees, nodes and interviews — and nothing they are not allowed to see, end to end without any other epic being complete
- [ ] A real MCP client (documented in the ticket notes) connects and successfully calls all four tools
- [ ] Calls without a token, or with an expired or wrong-audience token, are refused
- [ ] A caller never receives data of a team they do not belong to, by any tool or any id guess
- [ ] No tool can create, change or delete data
- [ ] The session-based web login and CSRF protection still work unchanged
- [ ] Integration tests cover each tool's scoping; the "Connect an agent" page exists

## 10. Risks & Unknowns
- Spring AI MCP starter and transport (streamable HTTP vs. SSE) versions move
  quickly; pin a version compatible with the JHipster 9 Spring Boot line.
- How desktop MCP clients obtain a Keycloak token (OAuth flow support vs.
  pasting a token) varies by client; the first ticket should settle and document
  one working path.
- Two security filter chains in one app is a common source of accidental
  exposure.

## 11. Dependencies
Epic 7 (interviews for `list_interviews`); transitively Epics 1 and 2. Richer
output appears automatically as Epics 4, 6, 8 and 9 land, but none is required.

## 12. References
- prd: docs/Product Requirements Document/PRD.md
- architecture: docs/Architecture/Architecture.md
- epic: epic_10_MCP_SERVER.md

## 13. Implementation Notes
Before modifying a module, use `docs/Code Map/codemap.json` to answer three questions:

1. What calls it?
2. What does it affect?
3. Which tests cover it?

Do NOT regenerate the code map inside a feature ticket. Mid-epic the map is expected to lag the code, and that drift is normal. If the map is missing, stale, or cannot answer the three questions, read the affected code directly and record in the ticket notes which questions it could not answer. The epic's final closeout ticket regenerates `codemap.html`, `codemap.json`, and `codemap.lock` together.

Suggested ticket breakdown (complexity: medium-high):
1. Backend: add the MCP starter, endpoint up with a trivial tool, version pinned (FR-054).
2. Backend: bearer-token filter chain and Keycloak realm client; user resolution (FR-055, NFR-020).
3. Backend: `list_products` and `get_tree` tools (FR-056, NFR-019, NFR-021).
4. Backend: `get_node` and `list_interviews` tools (FR-057, FR-058).
5. Frontend + docs: "Connect an agent" page; verified connection from a real MCP client (FR-059).
6. Closeout: regenerate the code map.
