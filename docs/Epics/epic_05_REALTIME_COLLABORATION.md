# Epic 5: Real-Time Collaboration

Status: TICKETS
Owner: human
Created: 2026-09-18
Last Updated: 2026-09-20
Depends On: epic_11_OST_TREE_BUILDER

> Rewritten 2026-09-20 against the delivered Tree Builder (epic 11). It previously
> depended on epics 3 and 4 and assumed the old nested tree payload, the pre-OST
> editor and an `Experiment` entity. None of those exist now.

---

## 1. Purpose

The defining property of Ombuto OST: a trio — or several members working on
different products — edits the same team tree at once and everyone sees every
change immediately. The server stays authoritative: writes go over REST, and
after commit a change event is broadcast over STOMP to everyone viewing that
team's tree.

**Working increment:** Two or more team members with the same tree open see each
other's additions, edits, moves, deletions, links, open questions and chat
messages appear immediately, without refreshing.

## 2. User Story

As a member of a product trio, I want to see my colleagues' changes to the tree
as they make them, So that we can work in the same tree at the same time without
overwriting or missing each other's work.

## 3. What already exists (build on it, don't re-invent)

- **Node types:** product, outcome, opportunity, solution, assumption, evidence.
  There is no `Experiment` entity.
- **Read model:** `GET /api/teams/{teamId}/tree` returns a _flat_ `TeamTreeDTO`
  (`nodes: TreeNodeDTO[]`, each with `key` = `"type-id"`, `parentKey`, fields,
  `links`, `questions`, `commentCount`, products' `lastActivity`), plus
  `members`, `currentUserRole`, `canEdit`, `evidenceThisMonth`.
- **Write APIs:** `POST /api/tree/nodes`, `PATCH /api/tree/nodes/{type}/{id}`,
  `POST /api/tree/nodes/move` (returns `{node, siblings[{key,sortOrder}]}`),
  `DELETE /api/tree/nodes/{type}/{id}`, plus links, open questions, comments and
  history reads. All are team scoped through `TeamAccessService`.
- **Serialisation:** every structural write takes `TreeStructureLock` (a
  per-team row lock) and lock/constraint failures surface as 409. Writes to one
  team therefore already commit in a defined order.
- **History:** `NodeHistory` rows are appended server-side by
  `NodeHistoryRecorder` for status, confidence, priority band, value, move, link
  add/remove, question add and comment add/delete — never for title or notes.
- **Client store:** `ost-tree.store.ts` holds the flat node list, derives layout
  positions, and tracks **in-flight patches per node and per field** with
  rollback. `failOnNodes` already re-reads the tree on 403/409, removes a node
  deleted by someone else ("This item was deleted by someone else."), and applies
  a role demotion.
- **Chat:** flat comments on every type except product, with counts on nodes and
  the panel badge (epic 6's scope, minus threading and live delivery).

## 4. Scope

- **In Scope:** after-commit change events for every tree write (node create,
  patch, move, delete; link add/update/remove; open question add/update/remove;
  comment add/edit/delete) published to `/topic/teams/{teamId}/tree`; a STOMP
  client wrapped in a Pinia store; applying events to the flat tree store
  without clobbering in-flight local edits; echo suppression; subscription
  authorisation in a STOMP channel interceptor; connection indicator; full
  reload on reconnect or on a detected gap; a brief highlight naming who changed
  a node; live `canEdit` when a member's role changes.
- **Out of Scope:** presence and live cursors; field-level locking; CRDT/OT
  merging (last write wins per field remains the decision); event replay or a
  durable event log; an external broker or multi-instance deployment;
  notifications outside the open page; threaded replies (epic 6).

## 5. Functional Requirements

1. **FR-029** — After a tree write commits, the server publishes a change event
   to `/topic/teams/{teamId}/tree`. Nothing is published for a rolled-back or
   rejected write. Publication happens strictly after commit
   (`@TransactionalEventListener(AFTER_COMMIT)` or an equivalent
   `TransactionSynchronization`), never inside the transaction.
2. **FR-030** — Event payloads reuse the existing DTOs so a client can apply them
   without a reload:
   - `NODE_CREATED` / `NODE_UPDATED`: the full `TreeNodeDTO`.
   - `NODE_MOVED`: the same body as `MoveTreeNodeResponse` (`node` +
     `siblings[{key, sortOrder}]` for both old and new parent).
   - `NODE_DELETED`: `{key}` plus the keys of every cascaded descendant, so the
     client can drop the subtree, its links, questions and comments.
   - `LINK_*`, `QUESTION_*`: the owning node `key` plus the link/question DTO or id.
   - `COMMENT_*`: the node `key`, the comment DTO or id, and the node's new
     `commentCount`.

   Every event carries `teamId`, `actingUserLogin`, `at`, a per-team monotonic
   `seq` and a server `epoch`.

3. **FR-031** — A client with a team's tree open subscribes to that team's topic
   and applies events to the flat store: nodes are added, patched, re-parented
   (using `siblings` for sort order) or removed, and derived values
   (`commentCount`, product `lastActivity`, evidence-this-month, dashboard and
   experiment-tracker rows) follow automatically.
4. **FR-032** — A client ignores the echo of its own writes: it applies its own
   change when the REST call returns, and an incoming event is dropped when it
   matches a write this client made (`actingUserLogin` plus a client-supplied
   request id echoed in the event).
5. **FR-033** — An incoming event must never overwrite an in-flight local edit:
   a field with a pending patch, or an input the user is currently typing in,
   keeps its local value; all other fields update. Where a remote value is
   dropped this way, the UI says so unobtrusively (the existing dirty-field
   mechanism in the panel is the model).
6. **FR-034** — Topic subscriptions are authorised in a STOMP channel
   interceptor against `TeamAccessService`: a non-member cannot subscribe to a
   team topic, and clients cannot SEND to tree topics (server → client only).
   Viewers subscribe and receive events like anyone else.
7. **FR-035** — The UI shows connection state (live / reconnecting / offline).
   On reconnect, on a `seq` gap, or on an `epoch` change (server restart), the
   client reloads the whole tree instead of replaying events.
8. **FR-036** — A node changed by someone else pulses briefly with that person's
   name. If it is open in the detail panel, the panel shows the new values
   (subject to FR-033). If it was deleted, the existing "deleted by someone
   else" handling runs. Remote changes must not steal focus, move the viewport
   (no auto-pan on remote events) or discard unsaved typing without saying so.
9. **FR-037** — When a member's role changes (including removal from the team),
   the affected client updates `canEdit` / `currentUserRole` live: edit
   affordances appear or disappear without a reload, reusing the demotion
   message already shown after a rejected write.

## 6. Non-Functional Requirements

1. **NFR-010** — A committed change is visible to other connected clients within
   1 second on a local network.
2. **NFR-011** — The WebSocket handshake uses the authenticated session;
   unauthenticated connections and unauthorised subscriptions are refused.
3. **NFR-012** — Spring's in-memory simple broker, single application instance.
   No external broker. The single-instance limit is documented in
   `docs/Architecture/Architecture.md`; horizontal scaling needs a separate
   architecture decision.
4. **NFR-013** — Event publishing must not slow writes measurably and must never
   fail a write: a broadcast failure is logged, not propagated.
5. **NFR-014** — A tree with 300+ nodes receiving a burst of remote events stays
   responsive (no full re-layout per event beyond the existing derived layout).

## 7. UI/UX Notes

- Connection indicator in the canvas toolbar, next to the zoom controls.
- Remote change: a 1–2 s pulse on the node using the accent token, with the
  person's name or initials; no layout jump.
- Remote delete of the node being edited: panel closes with the existing toast.
- Dark tokens only, scoped under `.ost-root`, like the rest of the OST module.

## 8. Data Model Impact

**None.** No event log is stored; `seq` and `epoch` are in-memory per instance.
If durable events or multi-instance delivery are ever needed, that is a new
epic and a JDL change.

## 9. Integration Impact

- **Backend:** an event type and publisher; after-commit hooks in
  `TreeNodeWriteService`, `TreeNodeMoveService`, `TreeNodeCascadeService`,
  `TreeNodeLinkService`, `TreeOpenQuestionService` and `TreeCommentService`;
  a channel interceptor in `WebsocketSecurityConfiguration`; team membership
  changes published from `TeamManagementService` (FR-037).
- **Frontend:** a STOMP Pinia store (connect, subscribe per team, reconnect with
  backoff); event application in `ost-tree.store.ts` alongside the existing
  in-flight patch sequencing; indicator and pulse in the canvas.
- **Existing sample code:** JHipster's `tracker` sample
  (`web/websocket/ActivityService.java`, `app/admin/tracker/*`) is unrelated to
  the tree. Decide explicitly: keep it as admin-only, or remove it. Do not let
  it share the tree topic.

## 10. Acceptance Criteria

Epic is complete when:

- [ ] Two members with the same tree open see each other's creates, patches, moves, deletes, links, questions and chat messages within a second, with no reload
- [ ] Events cover all six node types and every write API; nothing is published for a rejected or rolled-back write
- [ ] A non-member cannot subscribe to a team topic; a client SEND to a tree topic is rejected; viewers receive events
- [ ] A user's own edit is never applied twice and never flickers
- [ ] An incoming event does not overwrite a field the user is editing or has a patch in flight
- [ ] Dropping and restoring the connection shows the indicator and triggers one full tree reload; a `seq` gap or server restart does the same
- [ ] Two users editing the same node concurrently converge on the last committed value per field
- [ ] Changing a member's role updates their edit affordances live
- [ ] Integration tests for publish-after-commit (including no publish on rollback) and the interceptor; a two-session Playwright test for live updates and a latency check

## 11. Risks & Unknowns

- **Ordering vs. the team lock:** structural writes serialise on
  `TreeStructureLock`, but PATCH and comment writes take it too — confirm every
  publisher assigns `seq` under the lock so events cannot be numbered out of order.
- **Move events:** carry the subtree implicitly; clients must re-parent from
  `siblings` without reloading. Falling back to a reload for moves is acceptable
  only if the event proves unreliable.
- **In-flight edits vs. remote events:** the rule is per field (FR-033) and must
  be tested explicitly, including a remote delete during a local edit.
- **Echo suppression** needs a client request id threaded through the REST call
  and back into the event, or duplicate application is likely.
- **Burst behaviour:** a subtree delete or a bulk move produces many events;
  consider coalescing per animation frame on the client.

## 12. Dependencies

Epic 11 (the Tree Builder: flat tree read, write APIs, team lock, OST client
stores). Epic 6 (threaded comments) now depends on this epic only for live
delivery; the rest of its scope already shipped in epic 11.

## 13. References

- prd: docs/Product Requirements Document/PRD.md
- architecture: docs/Architecture/Architecture.md (Tree Builder section)
- epic: docs/Epics/epic_11_OST_TREE_BUILDER.md
- api contract: docs/Architecture/endpoint-inventory.md
- test strategy: docs/Test Strategy/test-strategy.md (§9a OST tests, two-session
  Playwright patterns, `@ConcurrentConnections`)

## 14. Implementation Notes (For Planning Agent)

Suggested ticket breakdown (complexity: high):

1. Backend: event model + after-commit publisher with per-team `seq`/`epoch`, wired into node create/patch/move/delete (FR-029, FR-030).
2. Backend: publishers for links, open questions and comments; membership/role events (FR-030, FR-037).
3. Backend: STOMP channel interceptor authorising subscriptions via `TeamAccessService`; reject client SENDs (FR-034, NFR-011).
4. Frontend: STOMP Pinia store — connect, subscribe per team, backoff reconnect, epoch/gap detection, connection state (FR-035).
5. Frontend: apply events to `ost-tree.store.ts` with echo suppression and the in-flight/dirty-field rule (FR-031, FR-032, FR-033).
6. Frontend: remote-change pulse with author name, panel behaviour on remote update/delete, live `canEdit` (FR-036, FR-037).
7. Two-session Playwright test (live updates, conflict, reconnect) and a latency check (NFR-010, NFR-014).
8. Closeout: regenerate the code map; update Architecture and test-strategy docs.
