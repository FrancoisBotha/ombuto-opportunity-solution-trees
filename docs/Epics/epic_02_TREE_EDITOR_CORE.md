# Epic 2: Tree Editor Core

Status: TICKETS
Owner: human
Created: 2026-09-18
Last Updated: 2026-09-18
Depends On: epic_01_TEAMS_AND_SCOPED_ACCESS

---

## 1. Purpose
Deliver the thinnest usable Opportunity Solution Tree: a visual editor in which a
signed-in user opens a team's tree and builds it from Products through Outcomes
and nested Opportunities down to Solutions, with a status on each node. A team
has **one tree**; each of the team's products is a top-level branch of it, so
different members can work on different products on the same canvas.

The JHipster scaffold (Keycloak sign-in, app shell, generated entities and
Liquibase schema) already exists and is treated as stack bootstrap, not as part
of this epic. Teams, membership, roles and `TeamAccessService` come from Epic 1;
this epic builds the tree on top of them.

**Working increment:** A team member can open their team's tree, add several
products to it, and build each product's branch — outcomes, nested
opportunities and solutions — adding, editing, deleting and setting the status
of nodes in a visual editor, with everything persisted across reloads.

## 2. User Story
As a member of a product trio, I want to build my team's opportunity solution
tree in a structured visual editor, with all of our products on one tree, So
that our discovery work lives somewhere structured and current instead of a
whiteboard drawing.

## 3. Scope
- **In Scope:**
  - "Trees" landing page listing the user's teams, each opening that team's tree
    (also reachable from the team page of Epic 1).
  - Whole-tree read endpoint per team (products, outcomes, opportunities,
    solutions) consumed in one request into a Pinia store.
  - Visual tree editor rendering Team → Product → Outcome → Opportunity
    (nested) → Solution.
  - Add, edit and delete for Product, Outcome, Opportunity and Solution nodes
    from the editor.
  - Status on each node (Outcome / Opportunity / Solution status enums; Product
    archived flag).
  - Focus the canvas on a single product.
  - Sidebar navigation entry for the tree editor.
- **Out of Scope:**
  - Moving / reordering nodes and collapse state (Epic 3).
  - Assumptions and Experiments (Epic 4).
  - Team creation, membership and roles (already delivered by Epic 1).
  - Real-time broadcast (Epic 5), comments (Epic 6), interviews (Epic 7),
    links (Epic 8), tags.

## 4. Functional Requirements
1. FR-009 — A signed-in user sees a "Trees" page listing the teams they belong to and can open a team's tree from it.
2. FR-010 — The server returns a team's whole tree (products, outcomes, nested opportunities, solutions) in a single request, ordered by `sortOrder`.
3. FR-011 — The tree editor renders the team's products as top-level branches with Outcome → nested Opportunity → Solution beneath each, visually distinguishing node types.
4. FR-012 — A user can add a product to the team's tree from the editor (name, description, vision).
5. FR-013 — A user can add an outcome under a product, an opportunity under an outcome or another opportunity, and a solution under an opportunity; new nodes are appended last among their siblings.
6. FR-014 — A user can edit a selected node's fields in a detail panel and the node updates in place when the save returns.
7. FR-015 — A user can set the status of an outcome, opportunity or solution (and archive a product); the status is shown on the node.
8. FR-016 — A user can delete a node after confirmation; deleting a node deletes its descendants.
9. FR-017 — A user can focus the canvas on a single product and return to the whole-team view.

## 5. Non-Functional Requirements
1. NFR-004 — Performance: a tree of 500 nodes loads and renders in under 2 seconds on a developer laptop against the dev database.
2. NFR-005 — Security: every tree read and write is authorised through `TeamAccessService` (members read, owners and editors write, viewers are read-only) and keeps JHipster's CSRF protection.
3. NFR-006 — Integrity: the server validates every write against the constraints in `ombuto.jdl` (required fields, lengths, rating ranges) and rejects structurally invalid parents.

## 6. UI/UX Notes
- Top-down tree layout on a pannable / zoomable canvas; product nodes form the
  top row.
- Node cards show type, title and a status badge; node types are colour-coded
  using the existing Pulse theme palette.
- Hover / select a node to reveal an "add child" affordance offering only the
  valid child types.
- Right-hand detail panel for editing the selected node; destructive actions
  need confirmation and state how many descendants will be removed.
- Product focus selector in the editor toolbar.
- Empty state for a team with no products: a single "Add your first product"
  call to action.

## 7. Data Model Impact
No new entities — Team, Product, Outcome, Opportunity and Solution already exist
from `ombuto.jdl`. `createdDate` / `lastModifiedDate` / `sortOrder` are set
server-side. Deletes must cascade to descendants (service-level or FK cascade
via a Liquibase changeset). Defaults: `valuerating` and `complexity` default to
3 when a node is created from the canvas.

## 8. Integration Impact
- New read endpoint `GET /api/teams/{teamId}/tree` and a tree service assembling
  the DTO graph without N+1 queries.
- Reuses generated REST resources / services for node writes, extended where
  needed (cascade delete, server-set fields).
- Frontend: new tree Pinia store, tree editor route and components, sidebar entry.
- Keycloak: unchanged (generated OIDC login).

## 9. Acceptance Criteria
- [ ] The application builds and runs, and a user can open a team's tree, add several products to it, and build each product's branch — outcomes, nested opportunities and solutions — adding, editing, deleting and setting the status of nodes in a visual editor, with everything persisted across reloads, end to end without any other epic being complete
- [ ] A team's tree shows all of that team's products as top-level branches and no other team's nodes
- [ ] A non-member cannot load or modify the tree (server-enforced); a viewer sees the tree with all editing affordances disabled and writes rejected
- [ ] Opportunities can be nested at least three levels deep
- [ ] Only valid child types can be added under each node type; the server rejects invalid parents
- [ ] Deleting a node removes its descendants after a confirmation naming the count
- [ ] Focusing on one product hides the other products' branches; clearing the focus restores them
- [ ] The whole tree is fetched in one request
- [ ] Backend (JUnit), frontend (Vitest) and one Playwright e2e test for the build-a-tree flow pass

## 10. Risks & Unknowns
- Tree rendering approach: hand-rolled SVG/CSS layout vs. a layout library. A new
  library needs approval per the engineering guide — the first ticket should
  propose one and get sign-off.
- The working tree currently holds a large uncommitted JHipster regeneration;
  it must be committed before agents run in worktrees.
- The code map does not exist yet; tickets must read code directly until the
  closeout ticket generates it.

## 11. Dependencies
Epic 1 (teams, membership, `TeamAccessService`, product list on the team page).
Relies on the existing JHipster scaffold and the dev Keycloak from
`src/main/docker/keycloak.yml`.

## 12. References
- prd: docs/Product Requirements Document/PRD.md
- architecture: docs/Architecture/Architecture.md
- data_model: docs/Data Model/Schema.ddl
- jdl: ombuto.jdl
- test_strategy: docs/Test Strategy/test-strategy.md
- epic: epic_02_TREE_EDITOR_CORE.md

## 13. Implementation Notes
Before modifying a module, use `docs/Code Map/codemap.json` to answer three questions:

1. What calls it?
2. What does it affect?
3. Which tests cover it?

Do NOT regenerate the code map inside a feature ticket. Mid-epic the map is expected to lag the code, and that drift is normal. If the map is missing, stale, or cannot answer the three questions, read the affected code directly and record in the ticket notes which questions it could not answer. The epic's final closeout ticket regenerates `codemap.html`, `codemap.json`, and `codemap.lock` together.

Suggested ticket breakdown (complexity: high — carries the tree plumbing every later epic extends):
1. Backend: team tree endpoint + tree DTO assembly (FR-010).
2. Backend: node write rules — server-set fields, parent validation, cascade delete (FR-013, FR-016, NFR-006).
3. Frontend: Trees page, route, sidebar entry, tree Pinia store (FR-009).
4. Frontend: tree canvas rendering and product focus (FR-011, FR-017).
5. Frontend: add-child and delete interactions (FR-012, FR-013, FR-016).
6. Frontend: node detail panel with editing and status (FR-014, FR-015).
7. Playwright e2e for the build-a-tree flow.
8. Closeout: regenerate the code map (`codemap.html`, `codemap.json`, `codemap.lock`).
