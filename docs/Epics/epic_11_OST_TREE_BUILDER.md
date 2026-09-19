# Epic 11: OST Tree Builder

Status: DONE
Owner: human
Created: 2026-09-19
Last Updated: 2026-09-20
Depends On: epic_01_TEAMS_AND_SCOPED_ACCESS
Supersedes: epic_02_TREE_EDITOR_CORE, epic_03_TREE_REARRANGING

---

## 1. Purpose

This epic delivers the Ombuto OST design handoff
(`docs/References/Vue Flow Opportunity Solution Tree/design_handoff_ombuto_ost/`,
its `README.md`, `REQUIREMENTS.md` and prototype `Ombuto OST.dc.html`) as a
working feature. A team's opportunity solution tree is built on a Vue Flow
canvas, with a detail panel, chat, open questions, per-node history, a portfolio
dashboard, an experiments tracker and a full-page node detail. It replaced the
earlier custom SVG tree editor (Epics 2 and 3) and the `Experiment` model.

**Working increment:** a team member opens `/trees` and builds the team's tree
(Product → Outcome → Opportunity → Solution → Assumption, with Evidence).
Members size and prioritise opportunities, test assumptions, discuss any node,
link out to Confluence and Jira, and see how the tree evolved. Viewers see
everything and change nothing. Everything persists.

## 2. Scope delivered

Built in steps OST-1 to OST-13b, with fix passes 5b, 5c, 6b, 8b, 9b and 13a (see
`git log --grep '\[OST-'`):

- **Data model** (step 1, JDL + generator): see section 5.
- **Dev seed** (step 2): `DevDataSeeder` seeds four teams and the full prototype
  tree in Team Jupiter (Architecture section 6).
- **Backend** (steps 3–5): the flat tree read, node create / patch / move /
  delete, links, open questions, chat and history. Team-scoped access, the team
  structure lock, and 409 semantics. All of it lives in custom classes beside the
  generated CRUD, which is admin-only.
- **Frontend** (steps 6–12): the `app/ost` module, with shell, dashboard, canvas,
  palette, `+` menu, inline rename, drag to re-parent, detail panel (Detail,
  Links, Chat, Open Qs and History tabs), chat modal, Experiments tracker and
  full-page node detail. It uses scoped dark tokens under `.ost-root`.
- **Hardening** (13a/13b): keyboard jump to a node (continuing from a match the
  user clicked), 44px touch targets (compact controls get an invisible hit area),
  dialogs above the app chrome, and error-detail hygiene. Also: a canvas rename
  that ends with a click into the panel title is kept, and a user demoted to
  viewer is told so on their next write. Finally, a performance fix for rename
  and the `+` menu at 300+ nodes, and this documentation.

Architecture: [`Architecture.md`](../Architecture/Architecture.md) section 6.
Endpoints: [`endpoint-inventory.md`](../Architecture/endpoint-inventory.md).
Tests: [`test-strategy.md`](../Test%20Strategy/test-strategy.md) section 9a.

## 3. Requirement coverage (handoff `REQUIREMENTS.md`)

| Requirement                                  | Delivered                                                                                                                                                                                                 | Main tests                                                                  |
| -------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------- |
| FR-C1 tidy layout, never hand-placed, 180 ms | `domain/layout.ts` derives positions on every change and does not store them; nodes ease over 180 ms                                                                                                      | `layout.spec.ts`, `ost-canvas-view`                                         |
| FR-C2 pan, cursor-anchored wheel zoom, Fit   | `useViewport.ts` (0.35–1.6, Fit floor 0.68, fits on first mount and on product switch)                                                                                                                    | `useViewport.spec.ts`, `ost-canvas-view`                                    |
| FR-C3 collapse with `+n`                     | Collapse chip. The toggled node stays in place on screen, and collapsed state is kept per user and team                                                                                                   | `ost-canvas-view`, journey                                                  |
| FR-C4 drag to re-parent                      | Legal targets highlighted; self, descendant and wrong-type drops snap back; a failed server move rolls back                                                                                               | `edit-rules.spec.ts`, `ost-canvas-edit`, journey                            |
| FR-C5 inline rename                          | Double-click or F2; Enter / blur commit, Escape cancels                                                                                                                                                   | `NodeTitleEditor.spec.ts`, `ost-canvas-edit`, journey                       |
| FR-C6 `+` menu of valid children             | `AddChildMenu`. The new node is selected and in rename mode                                                                                                                                               | `AddChildMenu.spec.ts`, `ost-canvas-edit`, journey                          |
| FR-C7 / FR-C8 search, type filters           | Search dims non-matches and outlines matches; filters dim without reflowing the layout                                                                                                                    | `ost-canvas-view`, journey                                                  |
| FR-C9 minimap                                | Custom overview map, pannable and zoomable                                                                                                                                                                | `CanvasMinimap.spec.ts`                                                     |
| FR-C10 legend and hints                      | `CanvasLegend`; "View only" hint for viewers                                                                                                                                                              | `ost-canvas-view`                                                           |
| FR-P1–P4 palette                             | Drag with ghost chip, click-to-arm, collapse to a 34px rail; disarmed on Escape, blur and leaving the page                                                                                                | `NodePalette.spec.ts`, `ost-canvas-edit`, journey                           |
| FR-D1–D6 detail panel                        | Header, breadcrumb, editable title, hide and reopen, tabs by type, Detail / Links tabs, delete with descendant count                                                                                      | `DetailPanel` / `DetailTab` / `LinksTab` specs, `ost-detail-panel`, journey |
| FR-V1 / FR-V2 priority and value             | `PrioritySlider` (one PATCH on release), `ValueScale`; priority rail and `$` glyphs on the node                                                                                                           | `ost-detail-panel`, journey                                                 |
| FR-M1–M3 chat                                | `ChatThread` shared by the tab, the modal (from the node chip) and the detail page; edit and delete own messages; jump-to-latest                                                                          | `ChatThread.spec.ts`, `ost-collaboration-tabs`, journey                     |
| FR-Q1 / FR-Q2 open questions                 | Checklist with badge and summary, and the "Delivery work belongs in Jira" copy                                                                                                                            | `OpenQuestionsTab.spec.ts`, `ost-collaboration-tabs`, journey               |
| FR-H1 / FR-H2 history                        | Server-side `NodeHistoryRecorder`; newest first; nothing recorded for title or notes                                                                                                                      | `TreeNodeHistoryResourceIT`, `TreeNodeWriteResourceIT`, journey             |
| FR-N1 views                                  | Trees dashboard, canvas, Experiments, full-page node detail (`/trees/:teamId/...`)                                                                                                                        | `ost-shell`, `ost-node-detail-page`                                         |
| FR-N2 / FR-N3 product combo, team switcher   | `CanvasProductCombo` (`?product=`, re-fits); `OstTeamCombo` with member avatars                                                                                                                           | `ost-shell`, `ost-canvas-view`, journey                                     |
| FR-N4 dashboard                              | Four counters (the fourth is Evidence this month, see section 4); product cards with Open branch                                                                                                          | `derive.spec.ts`, `ost-dashboard-experiments`                               |
| FR-N5 Experiments tracker                    | Every assumption, with its solution, owner, status and confidence; a row opens the node on the canvas                                                                                                     | `dashboard-experiments.spec.ts`, `ost-dashboard-experiments`, journey       |
| US-1 to US-13                                | Covered by the rows above; `ost-journey.spec.ts` walks all of them in one flow                                                                                                                            | journey                                                                     |
| NFR-1 300+ nodes                             | O(n) layout; only nodes in view rendered; rename and `+` menu no longer mount the whole tree (section 7)                                                                                                  | `ost-perf.spec.ts`                                                          |
| NFR-2 optimistic edits                       | Per-node, per-field sequenced patches with rollback; confirmation only for delete                                                                                                                         | `ost-tree.store.spec.ts`                                                    |
| NFR-3 keyboard                               | Focusable nodes (Enter / Space select, F2 rename, Delete), focus ring, tab arrow keys, dialog focus trap, search + Enter jump                                                                             | `OstNode.spec.ts`, `CanvasToolbar.spec.ts`, `ost-canvas-edit`               |
| NFR-4 append-friendly                        | Comments and history are append-only rows; edits are field patches                                                                                                                                        | backend ITs                                                                 |
| NFR-5 accessibility                          | 44px targets on coarse pointers (`.ost-tap` sizes small buttons; `.ost-hit` adds an invisible 48px hit area to compact chips, tabs and node controls); dialogs trap focus and close on backdrop or Escape | `touch-targets.spec.ts`, journey (touch check)                              |

## 4. Deliberate deviations from the prototype

- **Evidence only under an Opportunity or an Assumption**, never under a
  Solution. `REQUIREMENTS.md` §2 recommends this: evidence under a solution is
  proof collected for a favoured idea. The rule is enforced on the server
  (`TreeNodeRules`) and in the client (`rules.ts`).
- **Products are created on the team page**, not on the canvas. A product is a
  team-level object, so the canvas empty state links to the team page. Products
  cannot be dragged. The move API can reorder them, but no gesture uses it yet.
- **Custom overview map** instead of `@vue-flow/minimap`. It is drawn from the
  derived layout, so it shows every laid-out node even though Vue Flow only
  measures nodes in view. It also matches the prototype look.
- **Keyboard node actions**: Enter / Space select, F2 renames, Delete asks to
  delete, and search + Enter / Shift+Enter jumps to the next or previous match.
  These cover NFR-3, and the jump reaches nodes that are not rendered.
- **Title clamping**: titles clamp at 3 lines, or 2 for products, solutions and
  assumptions, with the full title as a tooltip. Long titles would otherwise run
  into the row below the node's edge elbow.
- **Add-link form**: "Add link" opens a name + URL form validated as
  `http(s)://…`. The prototype appended a "New link / https://" row at once, but
  the server rejects URLs that are not valid.
- **Dashboard counter**: the fourth counter is _Evidence this month_, not
  _Interviews_. Interviews are not part of the tree.
- **Viewers are read-only everywhere, chat included.**
- **Model trimmed**: Outcome metric, target and dates, Opportunity complexity,
  Solution effort, and Assumption category and importance were dropped. They are
  not in the handoff.
- **Full-page detail**: "← Back to canvas" when opened from the canvas,
  "Open on canvas" when loaded directly. It selects the node without opening the
  canvas panel.
- **Deep links**: `?product=` and `?node=` live in the canvas URL. A node outside
  the chosen product switches the scope to that node's product.
- **Automatic fit** keeps re-fitting on resize until the user moves the view.

## 5. Data model changes (`ombuto.jdl`)

- **Removed**: the entities `Experiment`, `OpportunityLink` and `SolutionLink`,
  and the enums `OutcomeStatus`, `ExperimentStatus`, `ExperimentResult`,
  `LinkType` and `AssumptionCategory`. Also removed the fields listed under
  "Model trimmed" in section 4. Outcome has no status any more.
- **Changed**: each status now has four values: Opportunity
  UNEXPLORED/EXPLORING/VALIDATED/PARKED, Solution
  CANDIDATE/BUILDING/SHIPPED/DROPPED, Assumption
  UNTESTED/TESTING/SUPPORTED/REFUTED. New fields: `Product.sortOrder` and
  `Opportunity.priority` (1–100). Assumption gains `status`, `confidence`
  (0–100), `owner` (User) and `sortOrder`, and loses its `evidence` count and
  `validated` flag. Comment can now belong to an Assumption or to Evidence.
- **Added**: `Evidence` (exactly one parent, an Opportunity or an Assumption),
  `NodeLink` (one nullable foreign key per node type), `OpenQuestion` (on an
  Opportunity) and `NodeHistory` (`nodeType` + `nodeId`, no foreign keys,
  append-only, written only on the server). A custom changelog indexes
  `(node_type, node_id)`.
- Existing dev databases must be wiped (`target/h2db/`): the entity changelogs
  were regenerated in place.

## 6. Known limitations

- **No real-time sync.** Other people's edits appear on reload. Epic 5 adds the
  WebSocket broadcast, and the handoff lists realtime as out of scope for this
  phase.
- **Virtualised nodes and Tab order.** Only nodes in view are in the DOM, so Tab
  cannot reach off-screen nodes. Search + Enter jumps to any match, and the panel
  breadcrumb and child list move through the tree.
- Adding a comment or an open question waits for the server, which assigns the
  id. There is no pending bubble.
- On a touch screen, the canvas node controls (`+`, collapse and chat chips) get
  a hit area of 48 screen px down to about 75% zoom. Below that it stops growing
  at 64 canvas px, so it cannot swallow neighbouring nodes. Zoom in to work by
  touch.
- `ROLE_ADMIN` has no implicit access to a team's tree. Admins use the
  admin-only Static Data screens, or join the team.
- In the `dev` profile, some non-database errors still show Java class names in
  the problem `detail` (the generator default, masked in `prod`).
- `@vue-flow/minimap` is still a dependency, used only for its CSS import
  (carry-over C13).

## 7. Performance (C21, `ost-perf.spec.ts`)

Measured with a throwaway tree of 306 nodes (1 product, 5 outcomes, 30
opportunities, 90 solutions, 180 assumptions), in Chromium at 1600×1000 against
the Vite dev server and the H2 dev backend. The spec was run on its own on
2026-09-20. Each run writes its numbers to `target/playwright/ost-perf.json`.

| Measure                                                     | Result                                                                                                           |
| ----------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------- |
| First canvas render (navigation → first node)               | 0.5–0.8 s; tree read done after 0.2–0.4 s; settled after 0.7–1.0 s; 16 of 306 nodes in the DOM at the fitted 68% |
| Wheel zoom, one notch per frame (30 out to 35%, 30 back in) | p50 16.7 ms, p95 16.8–33 ms, max 33 ms per frame; at most 28 nodes rendered                                      |
| Open inline rename, before the fix                          | 310 ms, with a 234 ms long task; all 306 nodes mounted                                                           |
| Open inline rename, after the fix                           | 33–60 ms, no long task; 11–13 nodes rendered                                                                     |
| Open `+` menu, before the fix → after                       | 296 ms (235 ms long task) → 21–47 ms                                                                             |
| Close rename / menu (after)                                 | 32–44 ms                                                                                                         |

**Change made.** Opening a rename field or a `+` menu used to switch visibility
culling off for the whole tree, so the edited node could not be unmounted. At
306 nodes that cost about 300 ms each time. Culling now stays on. Only the edited
node (and its menu) is pinned through Vue Flow's own culling exemption
(`pinRendered` in `canvas/canvas-model.ts`, which sets the node's `dragging`
flag). The rename draft still survives the node being zoomed out of view
(`ost-canvas-edit`, `ost-perf`). `NodeTitleEditor` still commits on unmount as a
safety net.

## 8. Tests

The Vitest specs sit next to the code under `app/ost/**`. The backend ITs are
listed in test-strategy §9a. The Playwright specs are `ost-*.spec.ts`, with
`ost-journey.spec.ts` as the end-to-end flow: editor, touch-target check, then a
viewer in a second session.
