# Ombuto OST — Functional Requirements & User Stories

Source of truth: the interactive prototype `Ombuto OST.dc.html`. This document describes
intended behaviour for implementation (Vue 3 + Vue Flow assumed). Where the prototype and
continuous-discovery theory (Teresa Torres) differ, the theory note is called out.

---

## 1. Product summary

Ombuto OST is a web app for product orgs practising continuous discovery. A **team** keeps
**one** Opportunity Solution Tree covering all of its products. Each product is a top-level
branch; beneath it sit desired outcomes, customer opportunities, candidate solutions, and the
assumptions/experiments that test them. Primary users are a product trio (PM, design, eng)
working on the tree together.

## 2. Domain model

```
Team
└── Product            (tree root; one branch per product)
    └── Outcome
        └── Opportunity            (nestable: opportunity → opportunity)
            ├── Solution
            │   └── Assumption     (assumption test)
            └── Evidence           (interview snippet)
```

**Allowed parent → child**

| Parent | Permitted children |
|---|---|
| Product | Outcome |
| Outcome | Opportunity |
| Opportunity | Opportunity, Solution, Evidence |
| Solution | Assumption, Evidence\* |
| Assumption | — |
| Evidence | — |

\* Theory note: customer evidence belongs under **opportunities** (it is what the opportunity is
derived from) and test results under **assumptions**. Evidence under a solution is a known smell
(collecting proof for a favoured idea). Recommend restricting Evidence to Opportunity + Assumption.

**Node attributes by type** — a field is present only where marked.

| Field | Product | Outcome | Opportunity | Solution | Assumption | Evidence |
|---|---|---|---|---|---|---|
| Title (inline editable) | ● | ● | ● | ● | ● | ● |
| Notes (free text) | ● | ● | ● | ● | ● | ● |
| Status | – | – | unexplored / exploring / validated / parked | candidate / building / shipped / dropped | untested / testing / supported / refuted | – |
| Confidence (0–100) | – | – | – | – | ● | – |
| Evidence strength (derived, read-only) | – | – | – | ● | – | – |
| Priority (1–100 continuous) | – | – | ● | – | – | – |
| Value ($–$$$$$) | – | – | ● | – | – | – |
| Open questions (checklist) | – | – | ● | – | – | – |
| Links | 1 default (Product space) | Confluence | Confluence, Jira Initiative, Jira Epic | Confluence, Jira Initiative, Jira Epic | Confluence | Confluence, Jira Ticket |
| Comments / chat | – | ● | ● | ● | ● | ● |
| History | – | ● | ● | ● | ● | ● |
| Owner | – | – | – | – | ● | – |

Rationale (theory): confidence is a property of an **assumption** (strength of evidence that it
holds), not of outcomes or opportunities. Opportunities are **prioritised and sized** instead.
A solution's credibility is derived from its assumption tests, never set by hand.

**Derived value — evidence strength (solution):**
`score = mean over child assumptions of (supported → 100, refuted → 0, otherwise its confidence)`;
`null` when the solution has no assumptions. Also expose test count, supported count, refuted count.

---

## 3. Functional requirements

### 3.1 Canvas
- **FR-C1** Render the tree top-down with automatic tidy layout: leaves laid left→right, each
  parent centred over its children; fixed row pitch per depth. Node positions are never
  hand-placed — layout recomputes on every structural change, animated over ~180 ms.
- **FR-C2** Pan by dragging empty canvas; zoom by wheel (cursor-anchored, clamp 0.35–1.6);
  zoom in/out buttons and a **Fit** action. Fit runs once on first mount and on product switch.
- **FR-C3** Collapse/expand any node with children; collapsed nodes show a `+n` chip with the
  hidden child count; collapsed subtrees are excluded from layout.
- **FR-C4** Drag a node onto another to **re-parent**. Valid drop targets are highlighted; a drop
  is rejected if the target is the node itself, a descendant of it, or does not permit that child
  type. Rejected drops animate back with no change.
- **FR-C5** Double-click a node title to rename inline (Enter commits, Escape cancels).
- **FR-C6** Each node exposes a `+` that opens a menu of the **valid child types for that node**;
  choosing one creates the child and puts it straight into rename mode.
- **FR-C7** Search dims non-matching nodes and outlines matches (title + notes, case-insensitive).
- **FR-C8** Type filter chips dim (not remove) nodes of the toggled-off types, preserving layout.
- **FR-C9** Minimap (bottom-right) shows all visible nodes and the current viewport rectangle;
  click or drag in it to recentre the canvas.
- **FR-C10** Legend explains node shapes; hint text explains drag-to-reparent and rename.

### 3.2 Node palette (left panel)
- **FR-P1** Lists creatable node types with a one-line hint of where each may attach.
- **FR-P2** **Drag** a type onto a node to attach it there; valid targets highlight during the
  drag and a ghost chip follows the cursor showing "Attach here" / "Drop on a highlighted node".
- **FR-P3** **Click** a type to arm it, then click a valid node to attach; clicking the armed type
  again disarms. A drag must not leave the palette in a stuck state.
- **FR-P4** Palette collapses to a narrow rail and reopens.

### 3.3 Detail panel (right)
- **FR-D1** Opens on node selection; header shows type, breadcrumb of ancestors and an editable
  title. Closing hides the panel but keeps the selection; a "Details" tab on the canvas edge and
  any node click reopen it.
- **FR-D2** Tabs: **Detail · Links · Chat · Open Qs · History**. Tab set adapts to node type —
  Product shows Detail + Links only; Open Qs appears for Opportunity only.
- **FR-D3** *Detail*: status chips, confidence (assumption), evidence-strength bar (solution),
  value `$`-scale and priority slider (opportunity), notes, child list with navigation, and
  quick-add buttons for permitted child types.
- **FR-D4** *Links*: name + URL rows, editable, removable, each openable in a new tab; "Add link";
  and one-click **restore** buttons for that type's default links, disabled when already present.
- **FR-D5** *Chat*: see 3.5. *Open Qs*: see 3.6. *History*: see 3.7.
- **FR-D6** Delete asks for confirmation, naming the node and its descendant count, and cascades
  to the whole subtree.

### 3.4 Priority & value (opportunity)
- **FR-V1** Priority is a continuous 1–100 slider with a colour ramp from cold (low) to warm
  (high); the node shows it as a five-dot vertical bar on its right edge, dots filling and
  growing with the value and taking the same colour.
- **FR-V2** Value is a five-step `$ … $$$$$` scale with a word label (Marginal → Outsized);
  the node shows filled/unfilled `$` glyphs.

### 3.5 Chat
- **FR-M1** Every node except Product has a thread. The node shows a comment count chip that
  opens the thread in a modal; the same thread is available in the panel's Chat tab.
- **FR-M2** Messages are bubbles (own messages right-aligned, others left), grouped under a
  date-time stamp, with the author's initials on the first message of a run.
- **FR-M3** Compose and send (Enter); **edit** and **delete** own messages; edited messages are
  marked. The thread auto-scrolls to the latest message; a jump-to-latest button appears when
  scrolled away from the bottom.

### 3.6 Open questions (opportunity)
- **FR-Q1** A checklist of discovery questions: add, tick/untick, remove. The tab badge counts
  open items; a summary reads "n open · n answered".
- **FR-Q2** Copy must steer users away from delivery tasks ("Delivery work belongs in Jira").

### 3.7 History
- **FR-H1** Append-only per-node changelog, newest first, each entry = author · timestamp · what.
- **FR-H2** Recorded events: status change, confidence change, priority change (≥1 band),
  value change, re-parent, comment added/deleted, link added/removed, open question added.
  Title and notes typing must **not** generate entries.

### 3.8 Navigation & scope
- **FR-N1** Top-level views: **Trees** (dashboard), **Tree canvas**, **Experiments**, plus a
  full-page node detail.
- **FR-N2** Product combo-box scopes the canvas to one product branch or **All products**
  (checkbox row); switching re-fits the view.
- **FR-N3** Team switcher (combo-box) changes team context, member avatars and dashboard kicker.
- **FR-N4** Trees dashboard: portfolio counters (opportunities, solutions, tests running,
  interviews this month) and one card per product with its outcome, node counts and an open action.
- **FR-N5** Experiments tracker: table of every assumption across the tree — assumption, the
  solution it tests, owner, status, confidence — each row opening that node on the canvas.

---

## 4. User stories

Format: *As a … I want … so that …* + acceptance criteria.

**US-1 — Map an opportunity space**
As a PM I want to add outcomes, opportunities, solutions and assumptions under a product so that
my team's discovery is visible in one tree.
- Only valid child types are offered at each node (`+` menu and palette hints agree).
- A new node appears in the right place, is selected, and is in rename mode.

**US-2 — Restructure as understanding changes**
As a PM I want to drag a node onto a different parent so that the tree reflects what we now
believe.
- Invalid targets (self, descendant, disallowed type) never accept the drop.
- Children travel with the node; layout re-tidies automatically.

**US-3 — Work a large tree**
As a designer I want to pan, zoom, fit, collapse branches and use a minimap so that I can work on
one branch without losing the whole.
- Wheel zoom is cursor-anchored; Fit frames the current product branch.
- Collapsed branches show their hidden child count.

**US-4 — Find things**
As an engineer I want to search and filter by node type so that I can find the assumption we're
currently testing.
- Matches are outlined, non-matches dimmed; filters dim rather than reflow.

**US-5 — Size and prioritise opportunities**
As a PM I want to set an opportunity's value and priority so that the trio agrees what to pursue
next.
- Value is `$`–`$$$$$`; priority is a continuous slider with cold→warm feedback.
- Both are visible on the node without opening the panel.

**US-6 — Record what we still need to learn**
As a PM I want an Open Questions checklist on an opportunity so that discovery next-steps live
next to the need, not in a separate doc.
- Items can be added, ticked and removed; the tab badge shows the open count.

**US-7 — Test assumptions honestly**
As a trio member I want to set an assumption's status and confidence so that the evidence behind a
solution is explicit.
- Confidence exists only on assumptions.
- The parent solution's evidence strength recomputes automatically and cannot be edited by hand.

**US-8 — Track experiments across the tree**
As a PM I want a tracker listing every assumption with its solution, owner, status and confidence
so that I can run the weekly discovery review from one screen.
- Rows open the corresponding node on the canvas.

**US-9 — Discuss in context**
As a trio member I want a chat thread on any node so that the conversation stays attached to the
work, not in Slack.
- Messages carry author and timestamp; I can edit and delete my own.
- The node shows an unread-agnostic message count; the thread opens from the node or the panel.

**US-10 — Link out to delivery**
As an engineer I want Confluence/Jira links on a node so that I can jump to the spec or ticket.
- Defaults differ by node type; deleted defaults can be restored in one click.

**US-11 — Understand how we got here**
As a stakeholder I want a per-node change log so that I can see when an opportunity was validated
or a solution dropped.
- Entries are append-only, newest first, and exclude noisy title/notes keystrokes.

**US-12 — Delete safely**
As a PM I want a confirmation before deleting so that I don't lose a subtree by accident.
- The dialog names the node and how many descendants go with it.

**US-13 — Switch context**
As a PM in two teams I want to switch team and scope the canvas to one product (or all) so that I
can focus.
- Switching product re-fits the canvas; "All products" shows every branch.

---

## 5. Non-functional

- **NFR-1** Canvas remains responsive at 300+ nodes (virtualise node rendering if needed; layout
  is O(n) per change).
- **NFR-2** All editing is optimistic and immediate; no modal "save" step except deletion.
- **NFR-3** Keyboard: Enter/Escape in rename and composer; Escape closes dialogs;
  all interactive elements focusable with a visible focus ring.
- **NFR-4** Multi-user is expected (trio) — model comments, history and edits as append-friendly
  so a realtime/CRDT layer can be added without schema change.
- **NFR-5** Accessibility: ≥4.5:1 text contrast, 44px minimum hit targets on touch, dialogs trap
  focus and are dismissible by backdrop click and Escape.

## 6. Out of scope (this phase)

Bets/commitment nodes (deliberately removed — commitment lives in Jira), version history/undo of
the whole tree, permissions and sharing, export, interview scheduling, and automatic snippet
tagging.
