# Epic 7: Interviews as Evidence

Status: NEW
Owner: human
Created: 2026-09-18
Last Updated: 2026-09-18
Depends On: epic_02_TREE_EDITOR_CORE

---

## 1. Purpose
Opportunities come from customer interviews. This epic lets a trio log their
weekly interviews and link each one to the opportunities it supports, so every
opportunity shows the evidence behind it.

**Working increment:** A team member can log a customer interview for a product,
link it to the opportunities it supports, and see and open that evidence from
the opportunity in the tree.

## 2. User Story
As a member of a product trio, I want to record customer interviews and link
them to opportunities, So that the tree shows which opportunities are backed by
what we actually heard.

## 3. Scope
- **In Scope:** interview list per team filterable by product; create / edit /
  delete interviews (title, participant, date, notes, recording URL,
  interviewer); link and unlink interviews to opportunities from either side;
  evidence count on opportunity nodes; evidence list in the opportunity detail
  panel.
- **Out of Scope:** uploading recordings or transcripts; AI summarisation;
  creating opportunities directly from highlighted notes; `ROLE_OVERVIEW`
  title-only access (Epic 9).

## 4. Functional Requirements
1. FR-040 — A team member with edit rights can log an interview against one of the team's products with title, participant, date, notes, recording URL and interviewer.
2. FR-041 — A team member can browse the team's interviews, filter them by product and search by title or participant, and open one to read it.
3. FR-042 — A user can link an interview to one or more opportunities of the same team, and unlink it, from both the interview page and the opportunity detail panel.
4. FR-043 — An opportunity node shows the number of supporting interviews, and its detail panel lists them with links to open each.
5. FR-044 — A user with edit rights can edit and delete an interview; deleting removes its links but not the opportunities.

## 5. Non-Functional Requirements
1. NFR-014 — Privacy: interview notes may contain customer personal data — they are only returned to members of the owning team, never written to application logs, and never included in WebSocket events.

## 6. UI/UX Notes
- Sidebar entry "Interviews"; table sorted by date, product filter, search box.
- Interview page: form on the left, linked opportunities (searchable picker
  grouped by product → outcome) on the right.
- Opportunity card: small evidence badge with the count; zero is shown subtly so
  unsupported opportunities stand out.

## 7. Data Model Impact
Uses the existing `Interview` entity and the Opportunity ↔ Interview
many-to-many. Server enforces that linked opportunities belong to the same team
as the interview's product. `createdDate` set server-side.

## 8. Integration Impact
Team-scoped interview service/endpoints via `TeamAccessService`; tree payload
gains per-opportunity evidence counts. If Epic 5 is already done, link/unlink
publishes an opportunity update event (count only, no notes).

## 9. Acceptance Criteria
- [ ] The application builds and runs, and a team member can log a customer interview for a product, link it to the opportunities it supports, and see and open that evidence from the opportunity in the tree, end to end without any other epic being complete
- [ ] Interviews of other teams are never listed or readable
- [ ] An interview cannot be linked to another team's opportunity (server-enforced)
- [ ] Viewers can read interviews but not create, edit, link or delete
- [ ] Notes never appear in logs or WebSocket payloads
- [ ] JUnit, Vitest and a Playwright test for log → link → see-on-tree pass

## 10. Risks & Unknowns
- Whether viewers should see full notes is a privacy call; this epic assumes team
  members of any role may.
- The generated Interview CRUD must be locked down like the other team-owned
  entities (Epic 1 pattern).

## 11. Dependencies
Epic 2 (opportunities and the tree editor); transitively Epic 1 for team scoping.

## 12. References
- prd: docs/Product Requirements Document/PRD.md
- architecture: docs/Architecture/Architecture.md
- data_model: docs/Data Model/Schema.ddl
- epic: epic_07_INTERVIEWS_AS_EVIDENCE.md

## 13. Implementation Notes
Before modifying a module, use `docs/Code Map/codemap.json` to answer three questions:

1. What calls it?
2. What does it affect?
3. Which tests cover it?

Do NOT regenerate the code map inside a feature ticket. Mid-epic the map is expected to lag the code, and that drift is normal. If the map is missing, stale, or cannot answer the three questions, read the affected code directly and record in the ticket notes which questions it could not answer. The epic's final closeout ticket regenerates `codemap.html`, `codemap.json`, and `codemap.lock` together.

Suggested ticket breakdown (complexity: medium):
1. Backend: team-scoped interview service/endpoints, same-team link rule, log hygiene (FR-040, FR-041, FR-044, NFR-014).
2. Backend: link/unlink endpoints and evidence counts in the tree payload (FR-042, FR-043).
3. Frontend: interviews list, filter, search, sidebar entry (FR-041).
4. Frontend: interview form page with opportunity picker (FR-040, FR-042, FR-044).
5. Frontend: evidence badge and detail-panel evidence list (FR-043).
6. Playwright e2e.
7. Closeout: regenerate the code map.
