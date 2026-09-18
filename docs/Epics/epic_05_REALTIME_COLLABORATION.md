# Epic 5: Real-Time Collaboration

Status: NEW
Owner: human
Created: 2026-09-18
Last Updated: 2026-09-18
Depends On: epic_03_TREE_REARRANGING, epic_04_ASSUMPTIONS_AND_EXPERIMENTS

---

## 1. Purpose
The defining property of Ombuto OST: a trio — or several members working on
different products — edits the same team tree at once and everyone sees every
change immediately. The server stays authoritative: writes go over REST, and
after commit a change event is broadcast over STOMP to everyone viewing that
team's tree.

**Working increment:** Two or more team members with the same tree open see each
other's additions, edits, moves and deletions appear on their canvas
immediately, without refreshing.

## 2. User Story
As a member of a product trio, I want to see my colleagues' changes to the tree
as they make them, So that we can work in the same tree at the same time without
overwriting or missing each other's work.

## 3. Scope
- **In Scope:** change events (`nodeType`, `id`, `action`, DTO, `user`) published
  after commit to `/topic/teams/{teamId}/tree` for every node type and action
  that exists (create, update, delete, move; products through experiments);
  STOMP client wrapped in a Pinia store; patching the tree store from events;
  ignoring the user's own echo; subscription authorisation in a STOMP channel
  interceptor; reload on reconnect; connection status indicator; a brief
  highlight showing who changed a node.
- **Out of Scope:** presence / live cursors; field-level locking; CRDT/OT
  merging (last-write-wins per node is the decision); event replay; an external
  broker or multi-instance deployment; comments (Epic 6).

## 4. Functional Requirements
1. FR-029 — After a tree write commits, the server publishes a change event (node type, id, action, DTO, acting user) to `/topic/teams/{teamId}/tree`; nothing is published for a rolled-back write.
2. FR-030 — A client with a team's tree open subscribes to that team's topic and applies incoming create, update, delete and move events to its tree store so the canvas updates without a reload.
3. FR-031 — The client applies its own edits when the REST call returns and ignores the echoed event for them.
4. FR-032 — Topic subscriptions are authorised against `TeamAccessService` in a STOMP channel interceptor; a user cannot subscribe to a team they do not belong to, and clients cannot send to tree topics.
5. FR-033 — When the WebSocket connection is lost the UI shows it; on reconnect the client reloads the whole tree rather than replaying events.
6. FR-034 — A node changed by someone else is briefly highlighted with that user's name; if the node is open in the detail panel, the panel shows the new values (last write wins).

## 5. Non-Functional Requirements
1. NFR-010 — Performance: a committed change is visible to other connected clients within 1 second on a local network.
2. NFR-011 — Security: the WebSocket handshake uses the authenticated session; unauthenticated connections and unauthorised subscriptions are refused.
3. NFR-012 — Architecture: uses Spring's in-memory simple broker in a single application instance; no external broker is introduced.

## 6. UI/UX Notes
- Small connection indicator in the editor toolbar (live / reconnecting).
- Remote change: 1–2 s coloured pulse on the node plus the editor's name.
- Remote delete of the node the user is editing: panel closes with a toast
  explaining who deleted it.
- Remote edits must not steal focus, move the viewport or discard the user's
  unsaved typing in other fields without telling them.

## 7. Data Model Impact
None. No event log is stored.

## 8. Integration Impact
- Backend: after-commit event publishing in the tree services (create, update,
  delete, move); channel interceptor in `WebsocketSecurityConfiguration`.
- Frontend: generated STOMP client wrapped in a Pinia store; tree store gains
  event-patching.
- Replaces or removes JHipster's sample "tracker" WebSocket usage if it conflicts.

## 9. Acceptance Criteria
- [ ] The application builds and runs, and two or more team members with the same tree open see each other's additions, edits, moves and deletions appear on their canvas immediately, without refreshing, end to end without any other epic being complete
- [ ] Events cover every node type (product through experiment) and every action including moves
- [ ] A non-member's subscription to a team topic is rejected; sending to a tree topic from a client is rejected
- [ ] A user's own edit is not applied twice
- [ ] Killing and restoring the connection shows the indicator and results in a full tree reload
- [ ] Two users editing the same node concurrently both end up showing the last saved value
- [ ] Integration test for publish-after-commit and the interceptor; a two-browser Playwright test for live updates

## 10. Risks & Unknowns
- Publishing strictly after commit (e.g. `TransactionSynchronization` /
  `@TransactionalEventListener`) — easy to get subtly wrong.
- Move events carry a subtree change; the event must let clients re-parent
  without a reload, or fall back to reloading the tree for moves.
- Unsaved local edits vs. incoming remote edits on the same node need a simple,
  explicit rule.

## 11. Dependencies
Epics 3 and 4, so that moves and assumption/experiment changes are broadcast from
the start rather than retrofitted (and transitively Epics 1 and 2 for
`TeamAccessService` and the tree store).

## 12. References
- prd: docs/Product Requirements Document/PRD.md
- architecture: docs/Architecture/Architecture.md
- epic: epic_05_REALTIME_COLLABORATION.md

## 13. Implementation Notes
Before modifying a module, use `docs/Code Map/codemap.json` to answer three questions:

1. What calls it?
2. What does it affect?
3. Which tests cover it?

Do NOT regenerate the code map inside a feature ticket. Mid-epic the map is expected to lag the code, and that drift is normal. If the map is missing, stale, or cannot answer the three questions, read the affected code directly and record in the ticket notes which questions it could not answer. The epic's final closeout ticket regenerates `codemap.html`, `codemap.json`, and `codemap.lock` together.

Suggested ticket breakdown (complexity: high):
1. Backend: change-event model and after-commit publisher wired into tree writes (FR-029).
2. Backend: STOMP channel interceptor for subscription authorisation (FR-032, NFR-011).
3. Frontend: STOMP Pinia store with connect / subscribe / reconnect (FR-033).
4. Frontend: tree store event patching and echo suppression (FR-030, FR-031).
5. Frontend: connection indicator, remote-change highlight, detail-panel conflict rule (FR-033, FR-034).
6. Two-browser Playwright test and latency check (NFR-010).
7. Closeout: regenerate the code map.
