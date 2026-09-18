# Epic 8: Jira & Confluence Links

Status: NEW
Owner: human
Created: 2026-09-18
Last Updated: 2026-09-18
Depends On: epic_02_TREE_EDITOR_CORE

---

## 1. Purpose
Connect the tree to where delivery work happens. A user pastes a Jira or
Confluence URL (or any other URL) onto an opportunity or solution and it becomes
a named, typed link on the node.

**Working increment:** A team member can paste a URL onto an opportunity or
solution and see it as a named, typed link on that node that opens the ticket or
page.

## 2. User Story
As a member of a product trio, I want to attach the Jira tickets and Confluence
pages related to an opportunity or solution, So that anyone reading the tree can
jump straight to the work.

## 3. Scope
- **In Scope:** paste-to-add links on opportunities and solutions; automatic
  type and name suggestion from the URL (Jira issue → `TICKET` named by issue
  key; Confluence page → `DOCUMENT` named from the page title in the URL; other
  URLs → `OTHER` named by host); manual edit of name and type; remove and
  reorder; link indicator on the node.
- **Out of Scope:** calling the Jira or Confluence APIs (live titles, statuses,
  OAuth to Atlassian); server-side fetching of pasted URLs; links on other node
  types; creating Jira tickets from the tree.

## 4. Functional Requirements
1. FR-045 — A user with edit rights can paste a URL into an opportunity's or solution's detail panel to add a link to it.
2. FR-046 — The app suggests the link type and name from the URL pattern: Jira issue URLs become `TICKET` named by issue key, Confluence page URLs become `DOCUMENT` named from the URL's page title, anything else `OTHER` named by host.
3. FR-047 — A user can change a link's name and type, remove it, and reorder a node's links.
4. FR-048 — Links are shown as typed chips (icon per type) in the detail panel and open in a new tab.
5. FR-049 — Nodes with links show a link indicator with the count on the canvas.

## 5. Non-Functional Requirements
1. NFR-015 — Security: only `http`/`https` URLs are accepted (validated server-side), links open with `rel="noopener noreferrer"`, and the server never fetches a pasted URL.
2. NFR-016 — Security: link reads and writes are scoped through `TeamAccessService` like the node they belong to.

## 6. UI/UX Notes
- "Links" section in the detail panel with a single "Paste a URL…" input; on
  paste/enter the link appears immediately with the suggested name and type,
  both editable inline.
- Distinct icons for ticket, document, prototype, design, analytics, interview,
  other.

## 7. Data Model Impact
Uses the existing `OpportunityLink` and `SolutionLink` entities (`name`, `url`,
`type`, `sortOrder`). Links are deleted with their node. No schema change.

## 8. Integration Impact
Team-scoped link endpoints; tree payload gains per-node link counts (links
themselves load with the node detail). URL parsing is a pure client-side utility
mirrored by server validation. If Epic 5 is done, link changes publish a node
update event. No network integration with Atlassian.

## 9. Acceptance Criteria
- [ ] The application builds and runs, and a team member can paste a URL onto an opportunity or solution and see it as a named, typed link on that node that opens the ticket or page, end to end without any other epic being complete
- [ ] `https://acme.atlassian.net/browse/ABC-123` is suggested as a `TICKET` named `ABC-123`; a Confluence `/wiki/spaces/…/pages/…/Page+Title` URL as a `DOCUMENT` named `Page Title`; self-hosted Jira `/browse/KEY-1` URLs are recognised too
- [ ] `javascript:` and other non-http(s) URLs are rejected by client and server
- [ ] Viewers see links but cannot add, edit or remove them
- [ ] Vitest covers the URL parser; JUnit covers validation and scoping; one Playwright test covers paste-to-add

## 10. Risks & Unknowns
- Atlassian URL formats vary (cloud vs. data center, short links); the parser
  should fall back gracefully to `OTHER` rather than guess wrongly.
- The `LinkType` enum has no Jira/Confluence-specific values; `TICKET` and
  `DOCUMENT` are used. Add enum values only through a JDL change ticket.

## 11. Dependencies
Epic 2 (opportunity and solution nodes, detail panel); transitively Epic 1.

## 12. References
- prd: docs/Product Requirements Document/PRD.md
- architecture: docs/Architecture/Architecture.md
- data_model: docs/Data Model/Schema.ddl
- epic: epic_08_JIRA_AND_CONFLUENCE_LINKS.md

## 13. Implementation Notes
Before modifying a module, use `docs/Code Map/codemap.json` to answer three questions:

1. What calls it?
2. What does it affect?
3. Which tests cover it?

Do NOT regenerate the code map inside a feature ticket. Mid-epic the map is expected to lag the code, and that drift is normal. If the map is missing, stale, or cannot answer the three questions, read the affected code directly and record in the ticket notes which questions it could not answer. The epic's final closeout ticket regenerates `codemap.html`, `codemap.json`, and `codemap.lock` together.

Suggested ticket breakdown (complexity: low-medium):
1. Backend: team-scoped link endpoints, URL validation, link counts in the tree payload (FR-045, FR-047, NFR-015, NFR-016).
2. Frontend: URL parser utility with tests (FR-046).
3. Frontend: links section — paste-to-add, edit, remove, reorder, chips (FR-045, FR-047, FR-048).
4. Frontend: node link indicator + Playwright test (FR-049).
5. Closeout: regenerate the code map.
