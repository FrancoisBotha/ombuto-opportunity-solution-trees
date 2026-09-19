# Epic 6: Threaded Comments

> **Superseded by [Epic 11: OST Tree Builder](epic_11_OST_TREE_BUILDER.md) (2026-09-20). Closed;
> do not plan work from it.** Node discussion shipped in Epic 11 as a **flat, chat-style thread**
> on every node type except Product: post, edit and delete your own message, comment counts on
> nodes and the panel badge, and a chat modal from the node chip.
>
> **Threading is a rejected decision, not deferred work.** Chat stays flat — the
> `Comment.parent` self-relationship was removed from `ombuto.jdl` and the schema on 2026-09-20,
> so replies are not "not yet built", they are out of the product. Everything below that talks
> about threads, replies or reply depth is kept for history only.
>
> The one genuinely open item — live delivery of messages over the team topic — belongs to
> [Epic 5: Realtime Collaboration](epic_05_REALTIME_COLLABORATION.md).

Status: SUPERSEDED
Owner: human
Created: 2026-09-18
Last Updated: 2026-09-20
Depends On: epic_05_REALTIME_COLLABORATION

---

## 1. Purpose

Discussion about an opportunity or solution should live on the node, not in a
chat tool. This epic adds threaded comments on tree nodes, delivered live through
the same mechanism as tree changes.

**Working increment:** A team member can comment on an outcome, opportunity or
solution, reply in a thread, and see colleagues' comments arrive live.

## 2. User Story

As a member of a product trio, I want to discuss a node in a thread attached to
it, So that the reasoning behind our decisions stays with the tree.

## 3. Scope

- **In Scope:** comments on outcomes, opportunities and solutions (the node
  types the `Comment` entity supports); replies; edit and delete of one's own
  comments; comment count badge on nodes; live delivery over the team topic.
- **Out of Scope:** comments on products, assumptions and experiments (needs a
  JDL change — separate epic if wanted); @mentions and notifications; rich text;
  reactions; resolving threads.

## 4. Functional Requirements

1. FR-035 — A team member with edit rights can add a comment to an outcome, opportunity or solution from the node's detail panel; viewers can read comments.
2. FR-036 — A user can reply to a comment, forming a thread shown in chronological order under its parent.
3. FR-037 — A user can edit or delete their own comments; edited comments are marked as edited, and deleting a comment with replies leaves a "deleted" placeholder.
4. FR-038 — Nodes with comments show a comment count badge on the canvas.
5. FR-039 — New, edited and deleted comments are delivered live to everyone viewing the team's tree, updating open threads and badges.

## 5. Non-Functional Requirements

1. NFR-013 — Security: comment reads, writes and events are scoped through `TeamAccessService` like the node they belong to, and comment bodies are rendered as plain text (no HTML injection).

## 6. UI/UX Notes

- "Comments" tab/section in the node detail panel: thread list, reply inline,
  composer at the bottom, author name and relative time.
- Badge on the node card; unseen-since-open comments get a subtle highlight.

## 7. Data Model Impact

Uses the existing `Comment` entity. Server enforces that exactly one of
`outcome` / `opportunity` / `solution` is set, that a reply shares its parent's
node, and sets `author`, `createdDate`, `editedDate`. Comments are deleted with
their node.

## 8. Integration Impact

Comment service and endpoints scoped by team; comment events published on
`/topic/teams/{teamId}/tree` using Epic 5's publisher; tree payload gains
per-node comment counts; threads are loaded on demand per node.

## 9. Acceptance Criteria

- [ ] The application builds and runs, and a team member can comment on an outcome, opportunity or solution, reply in a thread, and see colleagues' comments arrive live, end to end without any other epic being complete
- [ ] A user cannot edit or delete someone else's comment (server-enforced)
- [ ] Viewers can read but not write comments; non-members can do neither
- [ ] Comment badges update live for other users
- [ ] Script tags in a comment body are displayed as text
- [ ] JUnit, Vitest and a two-browser Playwright test pass

## 10. Risks & Unknowns

- Whether viewers should be allowed to comment is a product decision; this epic
  assumes not. Changing it is a one-line rule in the comment service.

## 11. Dependencies

Epic 5 (event publisher, STOMP store).

## 12. References

- prd: docs/Product Requirements Document/PRD.md
- architecture: docs/Architecture/Architecture.md
- data_model: docs/Data Model/Schema.ddl
- epic: epic_06_THREADED_COMMENTS.md

## 13. Implementation Notes

Before modifying a module, use `docs/Code Map/codemap.json` to answer three questions:

1. What calls it?
2. What does it affect?
3. Which tests cover it?

Do NOT regenerate the code map inside a feature ticket. Mid-epic the map is expected to lag the code, and that drift is normal. If the map is missing, stale, or cannot answer the three questions, read the affected code directly and record in the ticket notes which questions it could not answer. The epic's final closeout ticket regenerates `codemap.html`, `codemap.json`, and `codemap.lock` together.

Suggested ticket breakdown (complexity: medium):

1. Backend: team-scoped comment service/endpoints with ownership rules and comment counts in the tree payload (FR-035–FR-037, NFR-013).
2. Backend: comment events on the team topic (FR-039).
3. Frontend: comments section with threads, reply, edit, delete (FR-035–FR-037).
4. Frontend: node badges and live updates (FR-038, FR-039).
5. Two-browser Playwright test.
6. Closeout: regenerate the code map.
