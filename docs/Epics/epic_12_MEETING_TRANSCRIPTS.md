# Epic 12: Meeting Transcripts

Status: NEW
Owner: human
Created: 2026-09-19
Last Updated: 2026-09-21
Depends On: epic_02_TREE_EDITOR_CORE, epic_10_MCP_SERVER

---

## 1. Purpose

Discovery conversations are the raw material behind every node in the tree, but
today the words themselves live in Teams recordings and personal notes. Nothing
in the tree carries what was actually said. This epic lets a team attach the
transcript of a meeting to any node, so the evidence behind an outcome,
opportunity or solution is one click away — and readable by an agent over MCP.

**Working increment:** A team member pastes a meeting transcript onto a node,
sees it listed in the node's detail panel, opens it in a dialog to read it, and
their LLM agent reads the same transcript through the MCP server.

## 2. User Story

As a member of a product trio, I want to attach meeting transcripts to the nodes
they informed, So that anyone looking at a node can read what was actually said
instead of taking the node title on trust.

## 3. Scope

- **In Scope:** a `MeetingTranscript` entity attached to exactly one tree node;
  capture by pasting text into a dialog; upload of `.txt`, `.vtt` and `.srt`
  files parsed server-side to text; a transcript list in the node detail panel;
  a read-only viewer dialog for the full text; edit and delete; a transcript
  count badge on the canvas; MCP tools `list_transcripts` and `get_transcript`.
- **Out of Scope:** AI summarisation, extraction or suggested opportunities;
  `.docx` upload (needs a new backend parsing dependency — see §10); creating
  opportunities from highlighted transcript text; audio or video recordings;
  Teams / Zoom / Granola integrations; real-time transcript events over
  WebSocket; a standalone team-wide transcript browse-and-search page; and the
  `Interview` entity of Epic 7, which is untouched by this epic.

A transcript belongs to **exactly one node**. Pasting the same meeting onto a
second node creates a second, independent copy; editing one does not change the
other. Sharing one transcript across several nodes was considered and
deliberately rejected as unnecessary complexity for the value it adds.

## 4. Functional Requirements

1. FR-060 — A team member with edit rights can add a meeting transcript to any tree node from the node's detail panel by pasting text into a dialog, with title, meeting date, attendees and body.
2. FR-061 — A team member with edit rights can add a transcript by uploading a `.txt`, `.vtt` or `.srt` file, which the server parses to plain text, keeping VTT/SRT speaker labels and discarding timestamps and cue numbering.
3. FR-062 — A node's detail panel lists that node's transcripts newest first, showing title, meeting date and attendees; viewers can read them but not add, edit or delete.
4. FR-063 — Opening a transcript from the list shows its full text in a scrollable dialog, rendered as plain text.
5. FR-064 — A user with edit rights can edit a transcript's title, date, attendees and body, and delete it; deleting affects only that transcript and never the node it hangs from.
6. FR-065 — Nodes with transcripts show a transcript count badge on the canvas, alongside the comment and link indicators.
7. FR-066 — MCP exposes `list_transcripts` (transcripts for a team or a single node, metadata only, no body) and `get_transcript` (one transcript with its full body), both scoped through `TeamAccessService` like every other tool.

## 5. Non-Functional Requirements

1. NFR-022 — Privacy: transcript bodies may contain customer personal data — they are only returned to members of the owning team, never written to application logs, and never included in WebSocket events.
2. NFR-023 — Security: uploads are validated server-side by extension and size limit before parsing, parsed as text only, and transcript bodies are rendered as plain text and never as HTML; all reads and writes are scoped through `TeamAccessService`.
3. NFR-024 — Performance: transcript bodies are never included in the tree payload or in any list response — those carry counts and metadata only — and the body is fetched only when the viewer dialog opens, so NFR-004's 2-second tree render budget holds unchanged.

## 6. UI/UX Notes

- Node detail panel gains a "Transcripts" section beneath comments: a compact
  list of title · date · attendees, newest first, with an "Add transcript"
  button for editors.
- Add / edit dialog: title, meeting date, attendees, and a large monospace
  textarea for the body, with a file-drop area above it that fills the textarea
  from a parsed upload so the user can review the text before saving.
- Viewer dialog: title and metadata in the header, body in a scrollable
  monospace pane preserving line breaks, with edit and delete for editors.
- Canvas badge: a small document icon with the count, styled to match the
  comment (FR-038) and link (FR-049) indicators; absent when the count is zero.

## 7. Data Model Impact

New `MeetingTranscript` entity in `ombuto.jdl`, following the `NodeLink`
pattern exactly:

- `title` String required minlength(2) maxlength(200)
- `meetingDate` LocalDate required
- `attendees` String maxlength(500)
- `body` TextBlob required
- `source` MeetingTranscriptSource required — new enum `PASTED`, `UPLOADED`
- `createdDate` Instant required, server-set
- `editedDate` Instant
- ManyToOne `author(login)` to User
- Six optional ManyToOne node relationships — `product`, `outcome`,
  `opportunity`, `solution`, `assumption`, `evidence` — of which **exactly one
  is set**, enforced server-side as `NodeLink` does.

Needs an index on each node foreign key for the badge count query. Paginated and
filtered like `Comment`.

## 8. Integration Impact

- Team-scoped transcript service and endpoints via `TeamAccessService`, locked
  down like the other team-owned entities (Epic 1 pattern) — the generated
  `MeetingTranscriptResource` gets the class-level admin `@PreAuthorize` and an
  entry in `GeneratedEndpointsSecurityIT`, with the team-scoped resource written
  by hand.
- Tree payload gains a per-node transcript count.
- `TreeNodeCascadeService` must delete a node's transcripts when the node is
  deleted.
- MCP server gains two tools; the "Connect an agent" page's tool list grows by
  two entries.
- `NodeHistory` gains `TRANSCRIPT_ADDED` and `TRANSCRIPT_DELETED` event types if
  Epic 4's history work is in place; otherwise this is a no-op.

## 9. Acceptance Criteria

Epic is complete when:

- [ ] The application builds and runs, and a team member can paste a meeting transcript onto a tree node, see it in the node's detail panel, and open and read it in a dialog, end to end without any other epic being complete
- [ ] A `.txt`, `.vtt` and `.srt` file each upload and parse to readable text, with VTT/SRT speaker labels kept and timestamps removed
- [ ] A transcript is attached to exactly one node; the server rejects a write that sets zero or more than one node relationship
- [ ] Transcripts of other teams are never listed or readable, and a transcript cannot be attached to another team's node (server-enforced)
- [ ] Viewers can read transcripts but not add, edit, upload or delete
- [ ] Transcript bodies never appear in application logs, WebSocket payloads, the tree payload or any list response
- [ ] A node's canvas badge shows the correct transcript count and disappears at zero
- [ ] Deleting a node deletes its transcripts; deleting a transcript leaves the node intact
- [ ] `list_transcripts` returns metadata only and `get_transcript` returns the body, both refusing data the caller may not see
- [ ] An oversized or wrong-extension upload is rejected with a clear message and nothing is stored
- [ ] JUnit, Vitest and a Playwright test for paste → see-in-panel → open-in-dialog pass

## 10. Risks & Unknowns

- **Copies drift.** One node per transcript means a meeting that genuinely
  informed four opportunities is four pastes, and editing one copy leaves the
  other three stale. This is an accepted trade for a simpler model. If the
  friction shows up in real use, a many-to-many join is a later epic and the
  entity migrates cleanly.
- **Transcript size.** A one-hour transcript is roughly 60–100 KB of text. The
  upload size limit and the `TextBlob` column need a deliberate ceiling, and the
  viewer dialog should stay responsive at the top of that range.
- **`.docx` will be asked for.** It is the format most meeting tools export, and
  it is out of scope here only because it needs Apache POI or similar — a new
  backend dependency, which the engineering guide requires approval for. Worth
  raising as its own decision rather than smuggling into a ticket.
- **VTT dialects vary.** Teams, Zoom and Granola each write speaker labels
  differently. The parser should degrade to "keep the text, drop the timing"
  rather than fail on an unrecognised variant.
- **No real-time.** Transcript changes are deliberately not published over
  WebSocket (NFR-022), so a collaborator's canvas badge can be stale until they
  reload. Accepted; revisit if it confuses people in practice.
- **Overlap with Epic 7.** A customer interview can now be recorded two ways: an
  `Interview` linked to opportunities (Epic 7) or a transcript on a node (this
  epic). Both are intentional and the epics stay separate, but the UI copy
  should make the distinction obvious — the evidence *record* versus the *words*.

## 11. Dependencies

Epic 2 (the tree editor, its node detail panel and canvas nodes) — required for
anything in this epic to be visible. Epic 10 (the MCP server) for FR-066 only;
the rest of the epic ships without it. Transitively Epic 1 for team scoping.

## 12. References

- prd: docs/Product Requirements Document/PRD.md
- architecture: docs/Architecture/Architecture.md
- data_model: docs/Data Model/Schema.ddl
- epic: epic_12_MEETING_TRANSCRIPTS.md

## 13. Implementation Notes (For Planning Agent)

Before modifying a module, use `docs/Code Map/codemap.json` to answer three questions:

1. What calls it?
2. What does it affect?
3. Which tests cover it?

Do NOT regenerate the code map inside a feature ticket. Mid-epic the map is expected to lag the code, and that drift is normal. If the map is missing, stale, or cannot answer the three questions, read the affected code directly and record in the ticket notes which questions it could not answer. The epic's final closeout ticket regenerates `codemap.html`, `codemap.json`, and `codemap.lock` together.

Suggested ticket breakdown (complexity: medium):

1. Backend: `MeetingTranscript` entity and enum in `ombuto.jdl`, Liquibase changelog with node indexes, team-scoped service and endpoints, the exactly-one-node rule, generated-resource lockdown, log hygiene (FR-060, FR-064, NFR-022, NFR-023).
2. Backend: upload endpoint with extension and size validation, and the `.txt` / `.vtt` / `.srt` parsers (FR-061, NFR-023).
3. Backend: per-node transcript counts in the tree payload and `TreeNodeCascadeService` deletion (FR-065, NFR-024).
4. Frontend: transcripts section in the node detail panel, list and viewer dialog (FR-062, FR-063).
5. Frontend: add / edit dialog with paste textarea and file-drop upload (FR-060, FR-061, FR-064).
6. Frontend: canvas transcript count badge (FR-065).
7. MCP: `list_transcripts` and `get_transcript` tools plus the "Connect an agent" page entries (FR-066, NFR-022).
8. Playwright e2e: paste → see in panel → open in dialog → edit → delete.
9. Closeout: regenerate the code map.

Expected complexity: medium
Estimated total effort: 9 tickets
