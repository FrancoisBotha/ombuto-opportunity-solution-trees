<template>
  <div
    ref="wrap"
    class="ost-canvas"
    data-cy="ost-canvas"
    :data-zoom="zoom.toFixed(3)"
    :style="{ '--ost-zoom': zoom.toFixed(3) }"
    tabindex="-1"
    @focusin="onFocusIn"
  >
    <VueFlow
      :id="FLOW_ID"
      class="ost-canvas__flow"
      :nodes="flowNodes"
      :edges="flowEdges"
      :edge-types="EDGE_TYPES"
      :min-zoom="ZOOM_MIN"
      :max-zoom="ZOOM_MAX"
      :default-viewport="{ x: 0, y: 0, zoom: 0.82 }"
      :nodes-draggable="tree.canEdit"
      :node-drag-threshold="4"
      :select-nodes-on-drag="false"
      :nodes-connectable="false"
      :elements-selectable="false"
      :delete-key-code="null"
      :zoom-on-scroll="false"
      :pan-on-scroll="false"
      :zoom-on-double-click="false"
      :pan-on-drag="true"
      :only-render-visible-elements="true"
      @node-click="onNodeClick"
      @node-drag-start="onDragStart"
      @node-drag="onDrag"
      @node-drag-stop="onDragStop"
      @pane-ready="onReady"
    >
      <template #node-ost="{ id }">
        <OstNode
          v-if="tree.byId(id)"
          :node="tree.byId(id)!"
          :selected="ui.selectedId === id"
          :match="matchesQuery(tree.byId(id)!, ui.query)"
          :dimmed="isDimmed(tree.byId(id)!, ui.query, ui.hiddenTypes)"
          :legal-target="legal.has(id)"
          :drop-target="ui.dropTargetId === id"
          :child-count="kidCounts.get(id) ?? 0"
          :collapsed="!!ui.collapsed[id]"
          :can-add="tree.canEdit && ALLOWED[tree.byId(id)!.type].length > 0"
          :add-menu-open="ui.addMenuId === id"
          :can-rename="tree.canEdit"
          :editing="tree.canEdit && ui.editingId === id"
          :edit-draft="retry?.key === id ? retry.draft : null"
          :edit-error="retry?.key === id ? retry.error : null"
          :pulse="tree.pulseFor(id)"
          @add="toggleAddMenu(id)"
          @add-choose="createChild(id, $event)"
          @add-close="closeAddMenu(id)"
          @toggle="toggleCollapse(id)"
          @chat="openChat(id)"
          @activate="activate(id)"
          @rename="startRename(id)"
          @rename-commit="commitRename(id, $event)"
          @rename-cancel="cancelRename(id)"
          @delete="askDelete(id)"
        />
      </template>
    </VueFlow>

    <div v-if="!tree.loading && !tree.products.length" class="ost-canvas__empty">
      <div class="ost-state" data-cy="ost-canvas-empty">
        <div class="ost-state__title">No products in this tree yet</div>
        <p class="ost-state__text">
          Products are created on the team’s page. Each product becomes a branch of the tree with its own outcomes, opportunities, solutions
          and assumptions.
        </p>
        <router-link
          v-if="tree.canEdit && tree.team"
          class="ost-btn ost-btn--primary"
          :to="`/teams/${tree.team.id}`"
          data-cy="ost-canvas-go-to-team"
          >Go to the team</router-link
        >
      </div>
    </div>

    <CanvasLegend :can-edit="tree.canEdit" />
    <CanvasMinimap
      :nodes="visible"
      :placed="tree.placed"
      :selected-id="ui.selectedId"
      :viewport="flow.viewport.value"
      :size="flow.dimensions.value"
      @centre="view.centreOnPoint"
      @zoom="onMinimapWheel"
    />
  </div>
</template>

<script setup lang="ts">
/**
 * The tree canvas: Vue Flow with custom nodes/edges. Positions ALWAYS come from the store's derived
 * tidy layout (tree.placed) — users never place a node. Selection lives in the ui store, not in
 * Vue Flow.
 *
 * Wiring follows design_handoff_ombuto_ost/vue-reference/src/components/TreeCanvas.vue, with the
 * handoff's viewport rules: custom cursor-anchored wheel zoom (Vue Flow's zoom-on-scroll off), Fit
 * with a 0.68 floor on first mount and on product switch, only-render-visible-elements, and a
 * 180ms ease on node moves.
 *
 * Editing (step 9, editors only):
 * - drag a node onto another to re-parent: legal targets are highlighted, the one under the
 *   pointer is the drop target (edit-rules.ts); a drop calls tree.moveNode (append). Vue Flow keeps
 *   a dragged node where it was dropped, so after EVERY drag the positions are explicitly reset to
 *   the layout (snapToLayout) — a rejected or failed drop animates back. Escape, a window blur or
 *   leaving the canvas mid-drag cancel it (the node snaps back); a release anywhere outside the
 *   canvas is a rejected drop;
 * - the node `+` menu and the palette (drag or armed click) create via tree.createNode; the new
 *   node is selected, centred when off-screen and in rename mode;
 * - double-click (or F2) renames inline; Delete asks for confirmation (ConfirmDeleteDialog).
 *
 * While a node is being renamed or has its + menu open, that node stays rendered: panning or
 * wheel-zooming it out of view must not unmount its rename field (losing the draft) or its menu.
 * Culling stays on for every other node (pinRendered in canvas-model.ts; turning culling off for the
 * whole tree cost ~250ms per rename / + menu at 300 nodes — step 13b perf smoke).
 */
import { computed, markRaw, nextTick, onUnmounted, ref, shallowRef, watch } from 'vue';

import { type EdgeTypesObject, type NodeDragEvent, type NodeMouseEvent, VueFlow, useVueFlow } from '@vue-flow/core';

import type { Placed } from '../domain/layout';
import { ALLOWED } from '../domain/rules';
import type { NodeType } from '../domain/types';
import { useOstTreeStore } from '../stores/ost-tree.store';
import { useOstUiStore } from '../stores/ost-ui.store';

import CanvasLegend from './CanvasLegend.vue';
import CanvasMinimap from './CanvasMinimap.vue';
import OstEdge from './OstEdge.vue';
import OstNode from './OstNode.vue';
import { childCounts, isDimmed, isDraggable, laidOutNodes, matchesQuery, pinRendered, toFlowEdges, toFlowNodes } from './canvas-model';
import { attachTargets, canAttach, clientToFlow, dropTargetAt, insideRect, legalParents } from './edit-rules';
import { focusIsLost, restoreFocusAfterRename, typingElsewhere } from './rename-focus';
import { ZOOM_MAX, ZOOM_MIN, type Point, useViewport, wheelZoom } from './useViewport';

const emit = defineEmits<{ select: [key: string]; zoom: [zoom: number] }>();

const FLOW_ID = 'ost-canvas';
// OstEdge declares only the props it uses; Vue Flow's EdgeProps typing wants them all.
const EDGE_TYPES = { ost: markRaw(OstEdge) } as unknown as EdgeTypesObject;
const NONE: ReadonlySet<string> = new Set();

const tree = useOstTreeStore();
const ui = useOstUiStore();
const flow = useVueFlow(FLOW_ID);
const wrap = ref<HTMLElement | null>(null);
const view = useViewport(flow, wrap);
const zoom = view.zoom;

const visible = computed(() => laidOutNodes(tree.nodes, tree.placed));
/** Nodes with a rename field or + menu open: kept rendered however far out of view (see the header). */
const keepRendered = computed<ReadonlySet<string>>(() => {
  if (!tree.canEdit) return NONE;
  const keys = [ui.editingId, ui.addMenuId].filter((key): key is string => !!key);
  return keys.length ? new Set(keys) : NONE;
});
const flowNodes = computed(() => toFlowNodes(visible.value, tree.placed, n => isDraggable(n, tree.canEdit) && ui.editingId !== n.id));
const flowEdges = computed(() => toFlowEdges(visible.value, tree.placed));
// One pass over the nodes, shared by every node body (no per-node scans).
const kidCounts = computed(() => childCounts(tree.nodes));

watch(zoom, z => emit('zoom', z), { immediate: true });

function onMinimapWheel(deltaY: number) {
  view.userMoved();
  const s = view.size();
  flow.setViewport(wheelZoom({ ...flow.viewport.value }, { x: s.width / 2, y: s.height / 2 }, deltaY));
}

// ---- helpers ------------------------------------------------------------------------------------
const nodeEl = (key: string) => wrap.value?.querySelector<HTMLElement>(`.ost-node[data-node-key="${CSS.escape(key)}"]`) ?? null;

/** Focus a node body (or the canvas when it is not rendered) — never scrolls the canvas. */
function focusNode(key: string | null | undefined) {
  const el = key ? nodeEl(key) : null;
  (el ?? wrap.value)?.focus({ preventScroll: true });
}

/** Is the laid-out box entirely inside the canvas at the current viewport? */
function boxInView(p: Placed): boolean {
  const { x, y, zoom: z } = flow.viewport.value;
  const s = view.size();
  const left = (p.x - p.w / 2) * z + x;
  const top = p.y * z + y;
  return left >= 0 && top >= 0 && left + p.w * z <= s.width && top + p.h * z <= s.height;
}

/** The client point → flow point conversion for hit testing. */
function flowPoint(client: Point): Point | null {
  const r = wrap.value?.getBoundingClientRect();
  if (!r || !insideRect(client, r)) return null;
  return clientToFlow(client, r, flow.viewport.value);
}

/**
 * Put every node back where the layout says (Vue Flow keeps dragged positions). computedPosition
 * too: Vue Flow only recomputes it inside a MOUNTED node, while visibility culling decides on it — a
 * node dropped outside the pane (over the app sidebar, outside the window) is culled at once and
 * would stay invisible at its stale spot, Fit or not. Same for a culled node the layout moved.
 * A node being dragged is left alone unless `all`.
 */
function snapToLayout(all = true) {
  for (const n of flowNodes.value) {
    const live = flow.findNode(n.id);
    if (!live || (!all && live.dragging && !keepRendered.value.has(n.id))) continue;
    const { x, y } = n.position;
    const cp = live.computedPosition;
    if (live.position.x !== x || live.position.y !== y || cp.x !== x || cp.y !== y) {
      flow.updateNode(n.id, { position: { x, y }, computedPosition: { ...cp, x, y } });
    }
  }
}

// ---- legal targets (node drag, palette drag, armed tool) ------------------------------------------
const dragKey = ref<string | null>(null);

/** The nodes currently flagged by pinRendered (see keepRendered). */
let pinned: ReadonlySet<string> = NONE;
function applyPins() {
  const now = keepRendered.value;
  pinRendered(id => flow.findNode(id), pinned, now, dragKey.value);
  pinned = now;
}
watch(keepRendered, applyPins, { flush: 'post' });

// Layout changes (collapse, create, move, remote edits) move culled nodes too; a node created with
// its rename field open reaches Vue Flow only now, so it is pinned here.
watch(
  flowNodes,
  () =>
    nextTick(() => {
      snapToLayout(false);
      applyPins();
    }),
  { flush: 'post' },
);
const legal = computed<ReadonlySet<string>>(() => {
  if (!tree.canEdit) return NONE;
  if (dragKey.value) return legalParents(dragKey.value, tree.nodes);
  const type = ui.paletteDrag?.type ?? ui.tool;
  return type ? attachTargets(type, tree.nodes) : NONE;
});

/**
 * Rendered height (flow units) of a node Vue Flow has measured: a clamped three-line title renders
 * taller than its layout box, and a drop on that extra strip must still hit the node.
 */
const renderedHeight = (key: string): number | undefined => flow.findNode(key)?.dimensions.height || undefined;

/** For the palette: the legal node under a client point for a new `type`, or null. */
function resolveAttachTarget(type: NodeType, clientX: number, clientY: number): string | null {
  if (!tree.canEdit) return null;
  const point = flowPoint({ x: clientX, y: clientY });
  return point ? dropTargetAt(point, tree.placed, attachTargets(type, tree.nodes), null, renderedHeight) : null;
}

// ---- selection / activation -------------------------------------------------------------------
function onNodeClick({ node }: NodeMouseEvent) {
  activate(node.id);
}

/** Click or Enter on a node: attach the armed palette type when it fits, otherwise select. */
function activate(key: string) {
  const armed = ui.tool;
  if (armed && tree.canEdit) {
    const target = tree.byId(key);
    // An armed click on a node that cannot take the type does nothing (prototype nodeDown).
    if (target && canAttach(armed, target.type)) {
      ui.armTool(null);
      void createChild(key, armed);
    }
    return;
  }
  emit('select', key);
}

/** The node's chat chip: select the node (prototype openChat) and open its thread in the modal. */
function openChat(key: string) {
  ui.select(key, { openPanel: false });
  ui.openChat(key);
}

// ---- + menu / create -----------------------------------------------------------------------------
function toggleAddMenu(key: string) {
  if (!tree.canEdit) return;
  if (ui.addMenuId === key) {
    ui.openAddMenu(null);
    return;
  }
  ui.select(key);
  ui.openAddMenu(key);
}

/** A menu reports close when dismissed or unmounted; only the menu that is open may close it. */
function closeAddMenu(key: string) {
  if (ui.addMenuId === key) ui.openAddMenu(null);
}

const creating = ref(false);

/** Creates a child; the store selects it and starts rename. Centres it when it lands off-screen. */
async function createChild(parentKey: string, type: NodeType) {
  if (creating.value || !tree.canEdit) return;
  creating.value = true;
  view.userMoved(); // editing is the user taking over the view: no automatic re-fit from here on
  ui.openAddMenu(null);
  try {
    const key = await tree.createNode(parentKey, type);
    if (key) await reveal(key);
  } finally {
    creating.value = false;
  }
}

async function reveal(key: string) {
  await nextTick();
  const p = tree.placed[key];
  if (p && !boxInView(p)) view.centreOnBox(p);
}

// ---- collapse -----------------------------------------------------------------------------------
/**
 * Collapse / expand re-lays out the branch; keep the toggled node where it was on screen (eased
 * with the node transition) so its chip never slides away from the pointer or under the sidebar.
 */
async function toggleCollapse(key: string) {
  const before = tree.placed[key];
  ui.toggleCollapse(key);
  await nextTick();
  const after = tree.placed[key];
  if (!before || !after) return;
  const z = flow.viewport.value.zoom;
  const dx = (before.x - after.x) * z;
  const dy = (before.y - after.y) * z;
  if (Math.abs(dx) > 0.5 || Math.abs(dy) > 0.5) view.panBy(dx, dy);
}

// ---- rename -------------------------------------------------------------------------------------
/**
 * After a refused save the field reopens with the rejected draft and the server's message — unless
 * the user is typing in another field by then (the rename ended with a click there): reopening would
 * take their focus, so the message stays on the toast instead.
 */
const retry = shallowRef<{ key: string; draft: string; error: string } | null>(null);

function startRename(key: string) {
  if (!tree.canEdit || !tree.byId(key)) return;
  retry.value = null;
  ui.select(key);
  ui.startEditing(key);
}

/** Closes the rename field. */
function closeRename(key: string) {
  if (ui.editingId === key) ui.stopEditing();
  if (retry.value?.key === key) retry.value = null;
}

/** Closes the rename field; focus goes back to the node only if nothing else took it (rename-focus.ts). */
async function endRename(key: string) {
  closeRename(key);
  await restoreFocusAfterRename(() => focusNode(key));
}

async function commitRename(key: string, title: string) {
  const node = tree.byId(key);
  if (ui.editingId !== key) return; // a stale editor (e.g. unmounted while another node took over)
  closeRename(key);
  // The optimistic title lands in the same tick the field closes (no frame showing the old title);
  // focus is restored once it has settled.
  const saving = node && title !== node.title ? tree.patchNode(key, { title }) : null;
  await restoreFocusAfterRename(() => focusNode(key));
  if (!saving) return;
  const ok = await saving;
  // No retry once the user may no longer edit (demoted meanwhile): the toast says why.
  if (!ok && tree.canEdit && tree.byId(key) && !ui.editingId && !typingElsewhere(wrap.value)) {
    retry.value = { key, draft: title, error: tree.error ?? 'The title could not be saved.' };
    tree.clearError();
    ui.startEditing(key);
  }
}

function cancelRename(key: string) {
  if (ui.editingId === key) void endRename(key);
}

// ---- delete -------------------------------------------------------------------------------------
function askDelete(key: string) {
  if (!tree.canEdit || !tree.byId(key)) return;
  ui.select(key);
  ui.askDelete(key);
}

/** After the dialog closes: back to the node (cancelled) or to its parent (deleted). */
let deleting: { key: string; parent: string | null } | null = null;
watch(
  () => ui.confirmId,
  async key => {
    if (key) {
      deleting = { key, parent: tree.byId(key)?.parent ?? null };
      return;
    }
    const was = deleting;
    deleting = null;
    if (!was) return;
    await nextTick();
    if (focusIsLost()) focusNode(tree.byId(was.key) ? was.key : was.parent);
  },
);

// ---- drag to re-parent --------------------------------------------------------------------------
function clientOf(event: MouseEvent | TouchEvent): Point | null {
  if ('touches' in event) {
    const t = event.touches[0] ?? event.changedTouches[0];
    return t ? { x: t.clientX, y: t.clientY } : null;
  }
  return { x: event.clientX, y: event.clientY };
}

/** Last pointer position of the node drag (to end Vue Flow's gesture where it is). */
let dragClient: Point | null = null;

function listenNodeDrag(on: boolean) {
  if (on) {
    window.addEventListener('keydown', onNodeDragKeydown, true);
    window.addEventListener('blur', cancelNodeDrag);
  } else {
    window.removeEventListener('keydown', onNodeDragKeydown, true);
    window.removeEventListener('blur', cancelNodeDrag);
  }
}

function onDragStart({ node, event }: NodeDragEvent) {
  if (!tree.canEdit) return;
  ui.openAddMenu(null);
  dragKey.value = node.id;
  dragClient = clientOf(event);
  listenNodeDrag(true);
}

function onDrag({ event, node }: NodeDragEvent) {
  if (dragKey.value !== node.id) return;
  const client = clientOf(event);
  dragClient = client ?? dragClient;
  const point = client ? flowPoint(client) : null;
  ui.setDropTarget(point ? dropTargetAt(point, tree.placed, legal.value, node.id, renderedHeight) : null);
}

function onNodeDragKeydown(event: KeyboardEvent) {
  if (event.key !== 'Escape' || !dragKey.value) return;
  event.preventDefault();
  event.stopPropagation();
  cancelNodeDrag();
}

/**
 * Escape / window blur mid-drag: no drop. Clears the drop state, then ends Vue Flow's own gesture
 * (d3-drag listens for mouseup on the window) so the node stops following the pointer; its
 * drag-stop snaps it back. A touch drag ends with its touchend and is ignored as a drop meanwhile.
 */
function cancelNodeDrag() {
  if (!dragKey.value) return;
  dragKey.value = null;
  ui.setDropTarget(null);
  listenNodeDrag(false);
  const at = dragClient ?? { x: 0, y: 0 };
  window.dispatchEvent(new MouseEvent('mouseup', { bubbles: true, cancelable: true, view: window, clientX: at.x, clientY: at.y }));
  void nextTick(() => snapToLayout());
}

async function onDragStop({ node }: NodeDragEvent) {
  const key = node.id;
  const target = dragKey.value === key ? ui.dropTargetId : null;
  dragKey.value = null;
  dragClient = null;
  listenNodeDrag(false);
  ui.setDropTarget(null);
  const moving = target ? tree.moveNode(key, target) : null;
  // Rejected: straight back. Accepted: the optimistic move re-lays out; a failed move rolls back.
  await nextTick();
  snapToLayout();
  if (moving) {
    await moving;
    await nextTick();
    snapToLayout();
  }
}

// ---- keyboard -----------------------------------------------------------------------------------
/** Keyboard focus on a node partly outside the canvas brings it into view (and never scrolls). */
function onFocusIn(event: FocusEvent) {
  const el = event.target as HTMLElement | null;
  for (const scroller of [wrap.value, wrap.value?.querySelector<HTMLElement>('.vue-flow')]) {
    if (scroller && (scroller.scrollTop || scroller.scrollLeft)) scroller.scrollTo(0, 0);
  }
  const key = el?.classList.contains('ost-node') ? el.dataset.nodeKey : undefined;
  if (key && el?.matches(':focus-visible')) {
    const p = tree.placed[key];
    if (p && !boxInView(p)) view.centreOnBox(p);
  }
}

// ---- fit + centring -------------------------------------------------------------------------------
const ready = ref(false);
let pendingCentre: string | null = null;

/** Centre the pending deep-link node; drop it when it is not laid out (e.g. outside the product scope). */
function applyPendingCentre() {
  if (!pendingCentre || !ready.value) return;
  const p = tree.placed[pendingCentre];
  if (!p) pendingCentre = null;
  else if (view.centreOnBox(p)) pendingCentre = null;
}

/** Fit the branch in scope, then honour a pending deep-link centre. */
function fit() {
  if (!ready.value) return;
  view.fit(
    () => tree.placed,
    () => tree.roots.map(r => r.id),
  );
  applyPendingCentre();
}

function expandAncestors(key: string) {
  for (const ancestor of tree.ancestors(key)) if (ui.collapsed[ancestor.id]) ui.setCollapsed(ancestor.id, false);
}

/** Centre `key` on the canvas, expanding collapsed ancestors so it is laid out. */
async function centreOn(key: string) {
  pendingCentre = key;
  // Before the pane is ready the layout change is deferred to onReady (see there).
  if (!ready.value) return;
  expandAncestors(key);
  await nextTick();
  applyPendingCentre();
}

async function onReady() {
  ready.value = true;
  // Structural changes (expanding a deep link's ancestors) are applied only once Vue Flow is
  // ready; nodes added before that were not picked up by its node sync.
  if (pendingCentre) expandAncestors(pendingCentre);
  await nextTick();
  fit();
}

watch(
  () => ui.productId,
  async () => {
    await nextTick();
    fit();
  },
  { flush: 'post' },
);

/**
 * The detail panel opening (a click with nothing selected, the "Details" edge tab) narrows the
 * canvas by 346px from the right: when that clips the selected node, ease the view just far enough
 * to show it whole. Runs after the panel has mounted (flush post), so the canvas is measured at its
 * new width. A deep link centres the node itself (pendingCentre).
 */
watch(
  () => !!(ui.rightOpen && ui.selectedId),
  open => {
    const key = ui.selectedId;
    const p = key ? tree.placed[key] : undefined;
    if (!open || !key || !p || !ready.value || pendingCentre) return;
    view.reveal({ ...p, h: Math.max(p.h, renderedHeight(key) ?? 0) });
  },
  { flush: 'post' },
);

/**
 * Leaving the canvas (route change): the nodes' rename fields have settled by now (children unmount
 * before this hook), so drop whatever transient edit state is left — nothing may re-open or stay
 * armed on the next visit.
 */
onUnmounted(() => {
  listenNodeDrag(false);
  pinRendered(id => flow.findNode(id), pinned, NONE, null);
  pinned = NONE;
  if (ui.editingId) ui.stopEditing();
  if (ui.addMenuId) ui.openAddMenu(null);
  if (ui.dropTargetId) ui.setDropTarget(null);
  dragKey.value = null;
});

defineExpose({ zoomIn: view.zoomIn, zoomOut: view.zoomOut, fit, centreOn, resolveAttachTarget, createChild, focusNode });
</script>

<style scoped>
.ost-canvas {
  position: relative;
  flex: 1;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
  outline: none;
  background-image: radial-gradient(circle, color-mix(in srgb, var(--color-text) 9%, transparent) 1px, transparent 1px);
  background-size: 28px 28px;
}

.ost-canvas__empty {
  position: absolute;
  inset: 0;
  z-index: 4;
  display: grid;
  place-items: center;
  pointer-events: none;
}
.ost-canvas__empty .ost-state {
  margin: 0 16px;
  pointer-events: auto;
}

.ost-canvas__flow {
  width: 100%;
  height: 100%;
}

/* Re-layout eases nodes to their new place over 180ms (off while a node is being dragged). */
.ost-canvas :deep(.vue-flow__node) {
  transition: transform 0.18s ease;
  cursor: pointer;
}
.ost-canvas :deep(.vue-flow__node.draggable) {
  cursor: grab;
}
.ost-canvas :deep(.vue-flow__node.dragging) {
  transition: none;
  cursor: grabbing;
  z-index: 1000 !important;
}
.ost-canvas :deep(.vue-flow__node.dragging .ost-node) {
  box-shadow: var(--shadow-lg);
}
/* A node with its + menu or rename field open sits above its neighbours. */
.ost-canvas :deep(.vue-flow__node:has(.ost-add-menu)),
.ost-canvas :deep(.vue-flow__node:has(.ost-rename)) {
  z-index: 900 !important;
}
.ost-canvas :deep(.vue-flow__pane) {
  cursor: grab;
}
.ost-canvas :deep(.vue-flow__pane.dragging) {
  cursor: grabbing;
}
</style>
