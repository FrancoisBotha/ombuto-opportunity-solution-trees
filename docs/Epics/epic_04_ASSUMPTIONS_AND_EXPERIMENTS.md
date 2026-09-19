# Epic 4: Assumptions & Experiments

> **Largely superseded by [Epic 11: OST Tree Builder](epic_11_OST_TREE_BUILDER.md) (2026-09-20).**
> Assumptions ship as tree nodes under a solution with status, confidence, owner and notes, and
> Evidence replaced the Experiment entity, which was removed from `ombuto.jdl`. A solution's
> credibility is now derived from its assumptions (evidence strength), never hand-set, and the
> Experiments tracker lists assumptions. Only the unbuilt extras remain open: an
> importance/evidence 2x2 mapping view and experiment-style planning fields. Do not plan work
> from this epic without re-reading epic 11 first.

Status: NEW
Owner: human
Created: 2026-09-18
Last Updated: 2026-09-18
Depends On: epic_02_TREE_EDITOR_CORE

---

## 1. Purpose

Complete the depth of the tree. A solution is only as good as the assumptions
behind it; trios map those assumptions and run small experiments to test them.
This epic adds both levels beneath Solution.

**Working increment:** A user can add assumptions to a solution, plan and run
experiments against those assumptions, record the result and learnings, and see
which assumptions are tested and validated directly in the tree.

## 2. User Story

As a member of a product trio, I want to record the assumptions behind each
solution and the experiments that test them, So that we can see at a glance
which solutions rest on evidence and which on hope.

## 3. Scope

- **In Scope:** Assumption nodes under a solution (statement, category,
  importance, evidence, validated); Experiment nodes under a solution linked to
  one or more of that solution's assumptions; experiment status, result and
  learnings; both included in the team tree payload; edit and delete.
- **Out of Scope:** an importance/evidence 2×2 mapping view; experiments spanning
  multiple solutions; moving assumptions/experiments between solutions unless
  Epic 3 is already done (then the move rules are extended in that epic's
  pattern via a follow-up ticket).

## 4. Functional Requirements

1. FR-023 — A user can add, edit and delete assumptions under a solution with statement, category, importance (1–5) and evidence (1–5).
2. FR-024 — A user can add, edit and delete experiments under a solution with title, hypothesis, method, success criteria and dates.
3. FR-025 — A user can link an experiment to one or more assumptions of the same solution; the link is shown in the tree and in both detail panels.
4. FR-026 — A user can set an experiment's status and, when completed, its result (supported / refuted / inconclusive) and learnings.
5. FR-027 — A user can mark an assumption validated or invalidated; the assumption node shows its state and the results of its linked experiments.
6. FR-028 — The team tree payload and canvas include assumptions and experiments beneath their solution.

## 5. Non-Functional Requirements

1. NFR-009 — Performance: the tree is still fetched in a single request and NFR-004's render budget holds with assumptions and experiments included.

## 6. UI/UX Notes

- Assumption and experiment cards are smaller than opportunity/solution cards;
  assumptions show a category tag and a validated / invalidated / untested mark;
  experiments show status and a result icon.
- Experiment ↔ assumption links drawn as thin connectors or listed as chips on
  the experiment card (choose whichever stays legible — record the choice).
- Solution "add child" menu offers Assumption and Experiment.

## 7. Data Model Impact

No new entities — Assumption, Experiment and their many-to-many join exist in
`ombuto.jdl`. Server enforces that an experiment's assumptions belong to the same
solution. Cascade delete from Solution extends to both.

## 8. Integration Impact

Extends the tree service/DTO, tree store, canvas and detail panel from Epic 2.
No external systems.

## 9. Acceptance Criteria

- [ ] The application builds and runs, and a user can add assumptions to a solution, plan and run experiments against those assumptions, record the result and learnings, and see which assumptions are tested and validated directly in the tree, end to end without any other epic being complete
- [ ] An experiment cannot be linked to an assumption of a different solution (server-enforced)
- [ ] Result can only be set when the experiment is completed
- [ ] Deleting a solution removes its assumptions and experiments
- [ ] Whole tree still loads in one request
- [ ] JUnit, Vitest and a Playwright test for the assumption → experiment → result flow pass

## 10. Risks & Unknowns

- Visual clutter: two extra levels can overwhelm the canvas; collapse (Epic 3)
  mitigates it but may not be built yet.
- Shares files with Epic 3 (tree store, canvas) if built concurrently.

## 11. Dependencies

Epic 2.

## 12. References

- prd: docs/Product Requirements Document/PRD.md
- architecture: docs/Architecture/Architecture.md
- data_model: docs/Data Model/Schema.ddl
- epic: epic_04_ASSUMPTIONS_AND_EXPERIMENTS.md

## 13. Implementation Notes

Before modifying a module, use `docs/Code Map/codemap.json` to answer three questions:

1. What calls it?
2. What does it affect?
3. Which tests cover it?

Do NOT regenerate the code map inside a feature ticket. Mid-epic the map is expected to lag the code, and that drift is normal. If the map is missing, stale, or cannot answer the three questions, read the affected code directly and record in the ticket notes which questions it could not answer. The epic's final closeout ticket regenerates `codemap.html`, `codemap.json`, and `codemap.lock` together.

Suggested ticket breakdown (complexity: medium):

1. Backend: include assumptions/experiments in the tree payload; same-solution rule; cascade (FR-028, NFR-009).
2. Frontend: assumption nodes and detail panel (FR-023, FR-027).
3. Frontend: experiment nodes and detail panel with status/result/learnings (FR-024, FR-026).
4. Frontend: experiment ↔ assumption linking (FR-025).
5. Playwright e2e.
6. Closeout: regenerate the code map.
