# Handoff: Ombuto OST — opportunity solution tree app

## Overview

Ombuto OST is a web app for product organisations practising continuous discovery
(Teresa Torres). A **team** keeps **one** opportunity solution tree covering all of its
products: each product is a top-level branch, beneath it the desired outcomes, the customer
opportunities, the solutions under consideration, and the assumptions/experiments that test
them. Primary users are a product trio (PM, design, engineering) working the tree together.

Target implementation: **Vue 3 + Vue Flow** (the user's stated stack). A reference
implementation of the non-obvious parts ships in `vue-reference/` — see *Files* below.

## About the design files

The `.dc.html` files in this bundle are **design references created in HTML** — prototypes
showing intended look and behaviour. They are **not** production code to port line by line.
The task is to recreate them in the target codebase using its established patterns
(Vue 3 SFCs, its router, its state layer). `vue-reference/` is the exception: it is real
Vue 3/TypeScript source for the pieces worth carrying over verbatim (layout algorithm,
domain rules, derived values, the custom Vue Flow node).

## Fidelity

**High fidelity.** Colours, type, spacing, radii, states and interactions are final. Recreate
the UI faithfully using the tokens in `vue-reference/src/styles/tokens.css`. Every value in the
prototype comes from those tokens; the only deliberate exception is the priority spectrum
(documented below).

---

## Screens / views

### 1. Tree canvas (primary)
**Purpose:** build and work the tree.
**Layout:** full-height column. Top nav 54px (brand · view tabs · team combo · avatars).
Toolbar row ~48px (product combo · search · type-filter chips · zoom −/%/+ · Fit).
Body is a 3-column flex row: left palette 182px (collapses to a 34px rail), canvas flex:1,
right detail panel 346px (hideable, selection preserved).
**Canvas furniture:** dot-grid background (text colour at 9%, 28px pitch); legend bottom-left
(bounded `right:224px`, wraps); overview map bottom-right 198×134.
**Node geometry (width × min-height):** product 244×60, outcome 238×92, opportunity 218×100,
solution 206×88, assumption 198×84, evidence 206×92. Padding 9px 11px 8px — opportunities get
20px right padding to clear the priority rail. Radius 8px (`--radius-md`).
**Node contents:** kicker 9px/.08em uppercase at 70% opacity · title 15px/600 line-height 1.18 ·
meta row (status badge, one metric, thread-count chip) · `+` button top-right 19×19 ·
collapse chip bottom-centre when it has children.
**Node frames (type is carried by frame, never hue):**
| Type | Background | Border |
|---|---|---|
| Product | `--color-accent-900` | 1px solid `--color-accent-700` |
| Outcome | `--color-accent-900` | 1px solid `--color-accent-600` |
| Opportunity | `--color-surface` | 1px solid `--color-accent-500` |
| Solution | `--color-neutral-900` | 1px solid `--color-neutral-700` |
| Assumption | transparent | 1px **dashed** `--color-accent-500` |
| Evidence | transparent | 1px solid `--color-neutral-800`, italic body |

**Edges:** orthogonal, parent bottom-centre → child top-centre. Spine 1.6px
`--color-accent-600`; assumption/evidence 1.1px dashed `4 4`; evidence stroke
`--color-neutral-700`.
**States:** selected = 2px solid `--color-accent` outline, offset 2px. Legal drop target =
1–2px dashed `--color-accent-400` outline, offset 3–4px, background `--color-accent-800`.
Search match = 1px `--color-accent` border + `0 0 0 3px --color-accent-800`. Dimmed = opacity .24.

### 2. Trees dashboard
Portfolio view: four counters (opportunities, solutions, tests running, interviews this month)
in a 4-up grid, then one card per product (kicker, name, its outcome, four mini counts, last
edited, "Open branch").

### 3. Experiments tracker
Table of every assumption in the tree: assumption · solution it tests · owner · status ·
confidence. Rows are clickable and open that node on the canvas.

### 4. Node detail page
Full-width version of the panel: breadcrumb, title, status + metric tags, notes, children as
cards, discussion column.

### 5. Detail panel (right, 346px)
Header: type kicker, ancestor breadcrumb, editable title, hide button.
Tabs: **Detail · Links · Chat · Open Qs · History** (12px, 7px 6px 8px padding, 2px accent
underline on the active tab, count badge pill). Tab set adapts by type — Product gets
Detail + Links only; Open Qs is opportunity-only.

---

## Interactions & behaviour

- **Layout is derived.** Positions always come from the tidy layout (`layoutTree`); users never
  free-place a node. Re-layout animates over ~180ms ease on left/top.
- **Pan:** drag empty canvas. **Zoom:** cursor-anchored wheel, ×1.08 per notch, clamped
  0.35–1.6. **Fit:** frames the current branch, floor 0.68 so titles stay legible.
- **Re-parent:** drag a node onto another. Rejected when target is self, a descendant, or does
  not permit that child type; rejected drops snap back.
- **Create:** the node `+` opens a menu of that node's valid child types; the left palette
  supports drag-to-attach (ghost chip follows the cursor) and click-to-arm-then-click.
  New nodes land selected and in rename mode.
- **Rename:** double-click the title (Enter commits, Escape cancels).
- **Collapse:** chip under a parent; collapsed nodes show `+n`.
- **Search/filter:** dim rather than remove, so layout never jumps.
- **Overview map:** click or drag to recentre.
- **Chat:** bubbles — own right-aligned `--color-accent-600` with 17/17/5/17 radius, others left
  `--color-neutral-900` with 17/17/17/5; date-time stamp above each run; author initials on the
  first of a run; edit/delete own messages; auto-scroll to latest with a jump-to-latest button.
- **Delete:** confirmation dialog naming the node and its descendant count; cascades.
- **Dismissal:** overlays close on backdrop click and Escape. Hiding the detail panel keeps the
  selection; the "Details" tab on the canvas edge or any node click reopens it.

## State management

```
nodes[]                     the tree (flat list, parent pointers)
selectedId, panelTab        selection + right-panel tab
productId | 'all', teamId   scope
collapsed{}, query, hiddenTypes{}
tx, ty, zoom                viewport (Vue Flow owns this)
tool | paletteDrag, addMenuId, dropTargetId, editingId
chatId, chatDraft, chatEdit confirmId
leftOpen, rightOpen
```

Derived, never stored: node positions, edge paths, solution evidence strength, tab badge counts.
All edits are optimistic and immediate — no save step except deletion. Model comments and
history as append-only so a realtime layer can be added later without a schema change.

## Design tokens

Full set in `vue-reference/src/styles/tokens.css`. Key values:

- Ground `#161826` · surface `#232532` · text `#e9e9ed` · accent `#9184d9`
- Accent ramp 100→900: `#f5f4ff #e7e5fe #d2cefd #b5abfc #968ae0 #796cbf #5d5294 #423a6a #2b2741`
- Neutral ramp 100→900: `#f3f5fe #e4e7f5 #cfd3e5 #b2b6ca #9397ab #75798c #595d6c #3f424d #292b31`
- Type: Inter 400/500 (node titles 600). Sizes 36 / 16 / 15 / 13 / 12 / 10 / 9px.
  Labels 10px uppercase, letter-spacing .09em; node kickers 9px/.08em.
- Radius 4 / 8 / 14px. Spacing 2.8 / 5.6 / 8.4 / 11.2 / 16.8 / 22.4px.
- Shadows: sm `0 0 0 1px #3f424d`; md `0 0 0 1px #595d6c, 0 6px 18px rgba(0,0,0,.55)`;
  lg `0 0 0 1px #9397ab, 0 16px 40px rgba(0,0,0,.65)`.
- **Priority spectrum** (the one sanctioned non-token colour): fixed lightness 0.74, hue
  250°→25°, chroma 0.075→0.16 — `oklch(0.74 C H)`. Cold = low priority, warm = high.
- **Status tones** — three voices only, no traffic lights: settled-good (validated / supported /
  shipped) `--color-accent-800` bg + `--color-accent-200` text; in-flight (exploring / testing /
  candidate / building / unexplored / untested) transparent + 1px `--color-accent-600` +
  `--color-accent-300` text; settled-negative (refuted / dropped / parked)
  `--color-neutral-900` + `--color-neutral-300`.
- Buttons: primary is an **accent outline**, never a fill. Destructive actions use the neutral
  ramp — the system has no danger hue and one must not be invented.

## Domain rules the UI must enforce

| Parent | Permitted children |
|---|---|
| Product | Outcome |
| Outcome | Opportunity |
| Opportunity | Opportunity, Solution, Evidence |
| Solution | Assumption, Evidence\* |
| Assumption / Evidence | — |

\* Theory note: customer evidence belongs under **opportunities**, test results under
**assumptions**. Evidence under a solution is a known smell — consider removing it.

Per-type fields: status (opportunity / solution / assumption only) · **confidence on assumptions
only** · **priority and $-value on opportunities only** · evidence strength on solutions
(derived: supported → 100, refuted → 0, else the assumption's confidence, averaged) ·
open questions on opportunities · chat + history on everything except products.

## Assets

No images or icon files. Icons are inline SVG in the Phosphor style (chevron, link-out, send,
chat, check) — swap for `@phosphor-icons/vue` in the real app. Fonts: Inter via Google Fonts.

## Files

| File | What it is |
|---|---|
| `Ombuto OST.dc.html` | The interactive prototype — the behavioural source of truth. Open in a browser. |
| `Ombuto OST — Style Guide.dc.html` | Visual spec: tokens, node taxonomy, badges, controls, canvas chrome, do/don't. |
| `REQUIREMENTS.md` | Functional requirements (FR-*) and user stories (US-*) with acceptance criteria. |
| `support.js`, `_ds/` | Runtime + design-system stylesheet the two HTML files need to render. Not for production. |
| `vue-reference/src/domain/rules.ts` | Allowed parent→child, status vocabularies, default links, evidence rollup, priority colour, re-parent guard. **Port as-is.** |
| `vue-reference/src/domain/layout.ts` | The tidy tree layout + orthogonal edge path. **Port as-is.** |
| `vue-reference/src/stores/tree.ts` | Pinia store: single `patch()` mutation that writes the change log, add/re-parent/cascade-delete. |
| `vue-reference/src/components/OstNode.vue` | Vue Flow custom node with all six frames, badges, priority rail. |
| `vue-reference/src/components/TreeCanvas.vue` | Vue Flow wiring: derived positions, edges, drag-to-re-parent hit testing, minimap. |
| `vue-reference/src/styles/tokens.css` | The token sheet. |
| `vue-reference/src/data/seed.ts` | Trimmed demo data mirroring the prototype. |

To run the reference: `cd vue-reference && npm install && npm run dev` (add your own
`index.html`, `main.ts` and router — only the non-obvious modules are provided).

## Out of scope this phase

Bets/commitment nodes (deliberately removed — commitment lives in Jira), tree-wide undo/version
history, permissions and sharing, export, interview scheduling, automatic snippet tagging.
