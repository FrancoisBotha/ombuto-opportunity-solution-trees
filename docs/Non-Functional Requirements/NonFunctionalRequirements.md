# Non-Functional Requirements

| ID | Sub-System | Description | Status | Epic |
|----|-----------|-------------|--------|------|
| NFR-001 | Teams & Access | Security: authorisation is enforced in the service layer on every read and write, never only in the client or controller; generated endpoints for team-owned entities cannot bypass it. | NEW | epic_01_TEAMS_AND_SCOPED_ACCESS |
| NFR-002 | Teams & Access | Security: requests for another team's data return 403/404 without revealing whether the resource exists. | NEW | epic_01_TEAMS_AND_SCOPED_ACCESS |
| NFR-003 | Teams & Access | Security: the OIDC client secret and database password are supplied by environment variables; nothing beyond the dev `secret-samples` profile is committed. | NEW | epic_01_TEAMS_AND_SCOPED_ACCESS |
| NFR-004 | Tree Editor | Performance: a tree of 500 nodes loads and renders in under 2 seconds on a developer laptop against the dev database. | NEW | epic_02_TREE_EDITOR_CORE |
| NFR-005 | Tree Editor | Security: every tree read and write is authorised through `TeamAccessService` (members read, owners and editors write, viewers are read-only) and keeps JHipster's CSRF protection. | NEW | epic_02_TREE_EDITOR_CORE |
| NFR-006 | Tree Editor | Integrity: the server validates every write against the constraints in `ombuto.jdl` (required fields, lengths, rating ranges) and rejects structurally invalid parents. | NEW | epic_02_TREE_EDITOR_CORE |
| NFR-007 | Tree Editor | Integrity: a move (re-parent plus sibling renumbering) is applied in a single transaction; a failed move leaves the tree unchanged. | NEW | epic_03_TREE_REARRANGING |
| NFR-008 | Tree Editor | Accessibility: every rearranging action is reachable by keyboard via the "Move to…" and reorder menu actions. | NEW | epic_03_TREE_REARRANGING |
| NFR-009 | Tree Editor | Performance: the tree is still fetched in a single request and NFR-004's render budget holds with assumptions and experiments included. | NEW | epic_04_ASSUMPTIONS_AND_EXPERIMENTS |
| NFR-010 | Real-Time | Performance: a committed change is visible to other connected clients within 1 second on a local network. | NEW | epic_05_REALTIME_COLLABORATION |
| NFR-011 | Real-Time | Security: the WebSocket handshake uses the authenticated session; unauthenticated connections and unauthorised subscriptions are refused. | NEW | epic_05_REALTIME_COLLABORATION |
| NFR-012 | Real-Time | Architecture: uses Spring's in-memory simple broker in a single application instance; no external broker is introduced. | NEW | epic_05_REALTIME_COLLABORATION |
| NFR-013 | Comments | Security: comment reads, writes and events are scoped through `TeamAccessService` like the node they belong to, and comment bodies are rendered as plain text (no HTML injection). | NEW | epic_06_THREADED_COMMENTS |
| NFR-014 | Interviews | Privacy: interview notes may contain customer personal data — they are only returned to members of the owning team, never written to application logs, and never included in WebSocket events. | NEW | epic_07_INTERVIEWS_AS_EVIDENCE |
| NFR-015 | Links | Security: only `http`/`https` URLs are accepted (validated server-side), links open with `rel="noopener noreferrer"`, and the server never fetches a pasted URL. | NEW | epic_08_JIRA_AND_CONFLUENCE_LINKS |
| NFR-016 | Links | Security: link reads and writes are scoped through `TeamAccessService` like the node they belong to. | NEW | epic_08_JIRA_AND_CONFLUENCE_LINKS |
| NFR-017 | Overview | Privacy: interview notes are withheld server-side at DTO level for overview access, not hidden in the client. | NEW | epic_09_LEADERSHIP_OVERVIEW |
| NFR-018 | Overview | Performance: the overview loads with a bounded number of queries (no per-team or per-outcome N+1) and renders within 2 seconds for 20 teams. | NEW | epic_09_LEADERSHIP_OVERVIEW |
| NFR-019 | MCP Server | Security: the MCP server exposes no tool that modifies data, and every tool call goes through `TeamAccessService`. | NEW | epic_10_MCP_SERVER |
| NFR-020 | MCP Server | Security: bearer tokens are validated for signature, issuer, audience and expiry; the MCP endpoint is stateless and exempt from CSRF without weakening CSRF for the session-based API. | NEW | epic_10_MCP_SERVER |
| NFR-021 | MCP Server | Robustness: tool responses are structured JSON with bounded size (large trees can be requested per product) and tool descriptions are clear enough for an agent to choose correctly. | NEW | epic_10_MCP_SERVER |
