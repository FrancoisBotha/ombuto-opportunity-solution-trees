# Epic 3: Tree Rearranging

Status: NEW
Owner: human
Created: 2026-09-18
Last Updated: 2026-09-18
Depends On: epic_02_TREE_EDITOR_CORE

---

## 1. Purpose
Discovery trees are reshaped constantly: opportunities are regrouped, solutions
move to a better-fitting opportunity, siblings are re-prioritised. This epic
makes the tree rearrangeable instead of delete-and-recreate.

**Working increment:** A user can drag a node to a new parent, reorder siblings,
and collapse or expand branches in the tree editor, and the new structure
survives a reload.

## 2. User Story
As a member of a product trio, I want to rearrange the tree by dragging nodes,
So that the tree keeps matching our current understanding without retyping it.

## 3. Scope
- **In Scope:** drag-and-drop re-parenting; sibling reordering; a menu-based
  "Move to…" alternative; collapse / expand branches; moving a node with its
  whole subtree, including an outcome to another product.
- **Out of Scope:** moving nodes between teams; multi-select moves; undo/redo;
  broadcasting moves to other users (Epic 5).

## 4. Functional Requirements
1. FR-018 — A user can drag a node onto a valid new parent and the node moves with its whole subtree (outcome → product; opportunity → outcome or opportunity; solution → opportunity).
2. FR-019 — A user can reorder a node among its siblings, and products along the top row; order is persisted via `sortOrder`.
3. FR-020 — Invalid drops (wrong parent type, a node into its own subtree, another team's node) are visibly refused in the UI and rejected by the server.
4. FR-021 — A user can collapse and expand any branch; collapse state is remembered per user in the browser.
5. FR-022 — A user can move a node through a "Move to…" menu action as a non-drag alternative.

## 5. Non-Functional Requirements
1. NFR-007 — Integrity: a move (re-parent plus sibling renumbering) is applied in a single transaction; a failed move leaves the tree unchanged.
2. NFR-008 — Accessibility: every rearranging action is reachable by keyboard via the "Move to…" and reorder menu actions.

## 6. UI/UX Notes
- Drag ghost of the node; valid drop targets highlight, invalid ones show a
  not-allowed cursor; insertion marker between siblings for reordering.
- Collapse chevron on every node with children, showing the hidden descendant
  count when collapsed.
- On a rejected move the node snaps back and a toast explains why.

## 7. Data Model Impact
No schema change. Uses existing parent relationships and `sortOrder`. When an
opportunity moves to a different outcome, its nested opportunities' `outcome`
reference must be updated with it.

## 8. Integration Impact
- New move endpoint (e.g. `POST /api/tree/nodes/move` with node type, id, new
  parent, position) in the tree service from Epic 2.
- Frontend tree store gains a move action; collapse state kept in
  `localStorage`.

## 9. Acceptance Criteria
- [ ] The application builds and runs, and a user can drag a node to a new parent, reorder siblings, and collapse or expand branches in the tree editor, and the new structure survives a reload, end to end without any other epic being complete
- [ ] Moving an opportunity with nested children and solutions keeps the whole subtree intact
- [ ] Dropping a node into its own subtree or onto an invalid parent type is refused client-side and returns an error server-side
- [ ] Sibling order after a reorder is identical after reload
- [ ] Every move can be performed without a mouse
- [ ] Backend tests cover cycle prevention and sibling renumbering; a Playwright test covers a drag move

## 10. Risks & Unknowns
- Drag-and-drop on a pan/zoom canvas is fiddly; if a DnD library is wanted it
  needs approval per the engineering guide.
- Epics 3 and 4 both extend the tree store and canvas from Epic 2 — expect
  overlapping files if they are built concurrently; chain tickets touching the
  store.

## 11. Dependencies
Epic 2 (tree endpoint, store and canvas).

## 12. References
- prd: docs/Product Requirements Document/PRD.md
- architecture: docs/Architecture/Architecture.md
- data_model: docs/Data Model/Schema.ddl
- epic: epic_03_TREE_REARRANGING.md

## 13. Implementation Notes
Before modifying a module, use `docs/Code Map/codemap.json` to answer three questions:

1. What calls it?
2. What does it affect?
3. Which tests cover it?

Do NOT regenerate the code map inside a feature ticket. Mid-epic the map is expected to lag the code, and that drift is normal. If the map is missing, stale, or cannot answer the three questions, read the affected code directly and record in the ticket notes which questions it could not answer. The epic's final closeout ticket regenerates `codemap.html`, `codemap.json`, and `codemap.lock` together.

Suggested ticket breakdown (complexity: medium):
1. Backend: move endpoint with validation, cycle prevention, transactional renumbering (FR-018–FR-020, NFR-007).
2. Frontend: store move action + "Move to…" / reorder menu actions (FR-022, NFR-008).
3. Frontend: drag-and-drop re-parenting and sibling reordering (FR-018, FR-019, FR-020).
4. Frontend: collapse / expand with remembered state (FR-021).
5. Playwright e2e for move and reorder.
6. Closeout: regenerate the code map.
