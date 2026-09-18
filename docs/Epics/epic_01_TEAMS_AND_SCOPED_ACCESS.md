# Epic 1: Teams & Scoped Access

Status: NEW
Owner: human
Created: 2026-09-18
Last Updated: 2026-09-18

---

## 1. Purpose
Everything in Ombuto OST belongs to a team: a team owns one tree, and the tree
holds the team's products. This epic delivers teams, membership with owner /
editor / viewer roles, the team's product list, and the server-side
`TeamAccessService` that every later epic goes through — so that no tree data is
ever built without access control around it.

The JHipster scaffold (Keycloak OIDC sign-in, app shell, generated entities and
Liquibase schema) already exists and is treated as stack bootstrap, not as part
of this epic.

**Working increment:** A signed-in user can create a team, add colleagues as
owner, editor or viewer, and set up the team's products, while every user sees
only the teams they belong to and viewers cannot change anything.

## 2. User Story
As a product manager, I want to set up my team, its members and its products,
So that our discovery work has a home that only the right people can see and
edit.

## 3. Scope
- **In Scope:**
  - "My teams" page and team detail page (members, products).
  - Create / rename / describe a team; the creator becomes its owner.
  - Owners add and remove members and change roles; a user may belong to several
    teams with a different role in each.
  - Owners and editors create, edit and archive the team's products (list form;
    products become tree branches in Epic 2).
  - `TeamAccessService` (can read / can edit / is owner, by team, product or any
    node beneath it) and team-filtered list queries.
  - Locking down the generated entity REST endpoints for team-owned entities so
    they cannot bypass team scoping.
  - Keycloak set-up guide for brokering the company identity provider, with
    local Keycloak accounts still supported.
- **Out of Scope:** the tree editor (Epic 2); `ROLE_OVERVIEW` cross-team read
  access (Epic 9); email invitations; self-service join requests; deleting a team
  that has tree content.

## 4. Functional Requirements
1. FR-001 — A signed-in user can create a team (name, description) and automatically becomes its owner.
2. FR-002 — A user sees a "My teams" page listing only the teams they belong to, with their role in each.
3. FR-003 — An owner can add an existing user to the team as owner, editor or viewer, change a member's role, and remove a member.
4. FR-004 — A team always keeps at least one owner: the last owner cannot be removed or demoted.
5. FR-005 — A user can belong to several teams with a different role in each.
6. FR-006 — Owners and editors can create, edit and archive the team's products from the team page; viewers see them read-only.
7. FR-007 — A `TeamAccessService` answers read / edit / owner checks for a team, a product or any node beneath it, and every service method touching team-owned data goes through it; list queries are filtered by the caller's team ids.
8. FR-008 — Users can sign in through a company identity provider brokered by Keycloak or a local Keycloak account; the set-up is documented for deployers.

## 5. Non-Functional Requirements
1. NFR-001 — Security: authorisation is enforced in the service layer on every read and write, never only in the client or controller; generated endpoints for team-owned entities cannot bypass it.
2. NFR-002 — Security: requests for another team's data return 403/404 without revealing whether the resource exists.
3. NFR-003 — Security: the OIDC client secret and database password are supplied by environment variables; nothing beyond the dev `secret-samples` profile is committed.

## 6. UI/UX Notes
- Sidebar entry "Teams". "My teams" as cards or a table: name, role badge,
  member and product counts; "New team" button.
- Team page with two sections: Members (user picker by login / name, role
  dropdown, remove) and Products (name, description, archived toggle).
- Controls a user's role does not permit are hidden or disabled, not merely
  failing on click. Empty state invites the user to create their first team.

## 7. Data Model Impact
No new entities — Team, TeamMember and Product exist in `ombuto.jdl`. Add a unique
constraint on (`team`, `user`) in TeamMember via a Liquibase changeset.
`createdDate` / `joinedDate` are set server-side. Users exist in the app only
after their first Keycloak login (JHipster syncs them), so only users who have
signed in once can be added.

## 8. Integration Impact
- New `TeamAccessService`; team and membership REST endpoints (or hardened
  generated ones); product endpoints scoped by team.
- Generated CRUD resources for team-owned entities restricted (admin-only or
  routed through the access service).
- Keycloak: documentation for identity-provider brokering in
  `src/main/docker/realm-config/`-based dev set-up; no app code change for SSO.
- Frontend: teams Pinia store, routes, sidebar entry.

## 9. Acceptance Criteria
- [ ] The application builds and runs, and a user can create a team, add colleagues as owner, editor or viewer, and set up the team's products, while every user sees only the teams they belong to and viewers cannot change anything, end to end without any other epic being complete
- [ ] A user who is not a member cannot list, read or modify a team, its members or its products through any endpoint, including the generated ones
- [ ] A viewer receives 403 on every write and sees no edit controls
- [ ] The last owner cannot be removed or demoted
- [ ] The same user can be owner in one team and viewer in another, and sees the correct controls in each
- [ ] Signing in with a local Keycloak account works in dev, and the IdP-brokering guide exists
- [ ] JUnit tests cover `TeamAccessService` for each role and for non-members; a Playwright test covers create team → add member → add product with two users

## 10. Risks & Unknowns
- Adding members depends on the user having logged in once; if that proves too
  limiting, pre-provisioning from Keycloak is a separate ticket.
- The generated CRUD screens and endpoints expose everything by default; missing
  one is a data leak — the lock-down ticket needs an explicit endpoint inventory.
- The working tree currently holds a large uncommitted JHipster regeneration; it
  must be committed before agents run in worktrees.
- The code map does not exist yet; tickets read the code directly and this
  epic's closeout ticket generates the first map.

## 11. Dependencies
None. Relies on the existing JHipster scaffold and the dev Keycloak from
`src/main/docker/keycloak.yml`.

## 12. References
- prd: docs/Product Requirements Document/PRD.md
- architecture: docs/Architecture/Architecture.md
- data_model: docs/Data Model/Schema.ddl
- jdl: ombuto.jdl
- test_strategy: docs/Test Strategy/test-strategy.md
- epic: epic_01_TEAMS_AND_SCOPED_ACCESS.md

## 13. Implementation Notes
Before modifying a module, use `docs/Code Map/codemap.json` to answer three questions:

1. What calls it?
2. What does it affect?
3. Which tests cover it?

Do NOT regenerate the code map inside a feature ticket. Mid-epic the map is expected to lag the code, and that drift is normal. If the map is missing, stale, or cannot answer the three questions, read the affected code directly and record in the ticket notes which questions it could not answer. The epic's final closeout ticket regenerates `codemap.html`, `codemap.json`, and `codemap.lock` together.

Suggested ticket breakdown (complexity: medium-high — security-critical):
1. Backend: `TeamAccessService` + unit tests (FR-007, NFR-001, NFR-002).
2. Backend: team and membership endpoints with owner rules (FR-001, FR-003–FR-005).
3. Backend: team-scoped product endpoints; lock down generated endpoints for team-owned entities (FR-006, NFR-001).
4. Frontend: "My teams" page, create team, sidebar entry, teams store (FR-001, FR-002).
5. Frontend: team page — members management (FR-003, FR-004).
6. Frontend: team page — products management with role-aware controls (FR-006).
7. Docs + e2e: Keycloak IdP brokering guide; two-user Playwright test (FR-008, NFR-003).
8. Closeout: generate the code map (`codemap.html`, `codemap.json`, `codemap.lock`).
