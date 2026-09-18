# Epic 9: Leadership Overview

Status: NEW
Owner: human
Created: 2026-09-18
Last Updated: 2026-09-18
Depends On: epic_07_INTERVIEWS_AS_EVIDENCE

---

## 1. Purpose
Whiteboard trees are invisible outside the team that drew them. Heads of product
and similar leaders get a read-only overview across every team's tree, without
being added to each team, and without access to raw interview notes.

**Working increment:** A head of product with the `ROLE_OVERVIEW` authority can
open an overview of all teams' products, outcomes and opportunities and drill
into any team's tree read-only, seeing interview titles but never the notes.

## 2. User Story
As a head of product, I want a read-only overview of every team's outcomes and
opportunities, So that I can see what all teams are pursuing without asking each
one for a screenshot.

## 3. Scope
- **In Scope:** `ROLE_OVERVIEW` honoured by `TeamAccessService` as global read
  access; overview page (teams → products → outcomes with status, metric,
  current vs. target, and opportunity counts by status); drill-down into any
  team's tree in read-only mode; interview titles only for overview users;
  documentation for assigning the role in Keycloak.
- **Out of Scope:** editing of any kind by overview users; cross-team reporting,
  charts or exports; managing the role from inside the app; commenting by
  overview users.

## 4. Functional Requirements
1. FR-050 — A user with the `ROLE_OVERVIEW` authority (assigned in Keycloak) has read access to every team's tree through `TeamAccessService` without being a team member, and no write access from that authority.
2. FR-051 — An overview page lists all teams with their products and outcomes (status, metric, current and target value) and the number of opportunities per status under each outcome.
3. FR-052 — From the overview a user can open any team's tree in read-only mode, including live updates if real-time collaboration is in place.
4. FR-053 — Overview users see interview titles, dates and linked opportunities but never interview notes, participants or recording URLs unless they are also a member of that team.

## 5. Non-Functional Requirements
1. NFR-017 — Privacy: interview notes are withheld server-side at DTO level for overview access, not hidden in the client.
2. NFR-018 — Performance: the overview loads with a bounded number of queries (no per-team or per-outcome N+1) and renders within 2 seconds for 20 teams.

## 6. UI/UX Notes
- Sidebar entry "Overview", visible only with the authority.
- Collapsible team sections; outcome rows with status badge and a compact
  stacked bar of opportunity statuses; click-through to the tree.
- A persistent "Read-only overview" banner inside a tree opened this way.

## 7. Data Model Impact
None. `ROLE_OVERVIEW` is added to the Authority seed data and to the dev Keycloak
realm config (`src/main/docker/realm-config/jhipster-realm.json`) with a sample
user.

## 8. Integration Impact
`TeamAccessService` read checks and list filters; STOMP subscription interceptor
(if Epic 5 is done) accepts overview readers; interview DTO mapping gains a
redacted form; new overview endpoint and page; Keycloak role-mapping docs.

## 9. Acceptance Criteria
- [ ] The application builds and runs, and a head of product with the `ROLE_OVERVIEW` authority can open an overview of all teams' products, outcomes and opportunities and drill into any team's tree read-only, seeing interview titles but never the notes, end to end without any other epic being complete
- [ ] Every write attempted with only `ROLE_OVERVIEW` returns 403
- [ ] Interview notes, participant and recording URL are absent from API responses for overview access
- [ ] A user who is both overview and a team editor keeps full rights in their own team
- [ ] Users without the authority cannot reach the overview page or endpoint
- [ ] JUnit covers the access matrix (member roles × overview); a Playwright test covers the overview drill-down

## 10. Risks & Unknowns
- A global read bypass is the riskiest change to `TeamAccessService`; it must be
  read-only by construction, not by convention.
- Which summary numbers leaders actually want is unvalidated — keep the page
  simple and iterate.

## 11. Dependencies
Epic 7 (interviews exist so the title-only rule is real and testable);
transitively Epics 1 and 2.

## 12. References
- prd: docs/Product Requirements Document/PRD.md
- architecture: docs/Architecture/Architecture.md
- epic: epic_09_LEADERSHIP_OVERVIEW.md

## 13. Implementation Notes
Before modifying a module, use `docs/Code Map/codemap.json` to answer three questions:

1. What calls it?
2. What does it affect?
3. Which tests cover it?

Do NOT regenerate the code map inside a feature ticket. Mid-epic the map is expected to lag the code, and that drift is normal. If the map is missing, stale, or cannot answer the three questions, read the affected code directly and record in the ticket notes which questions it could not answer. The epic's final closeout ticket regenerates `codemap.html`, `codemap.json`, and `codemap.lock` together.

Suggested ticket breakdown (complexity: medium):
1. Backend: `ROLE_OVERVIEW` authority, realm config, read-only path in `TeamAccessService` and subscription interceptor (FR-050).
2. Backend: interview redaction for overview access (FR-053, NFR-017).
3. Backend: overview summary endpoint (FR-051, NFR-018).
4. Frontend: overview page and guarded route/sidebar entry (FR-051).
5. Frontend: read-only tree mode with banner (FR-052).
6. Playwright e2e + Keycloak role docs.
7. Closeout: regenerate the code map.
