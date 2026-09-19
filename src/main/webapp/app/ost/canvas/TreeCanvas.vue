<template>
  <div ref="wrap" class="ost-canvas" data-cy="ost-canvas" :data-zoom="zoom.toFixed(3)">
    <VueFlow
      :id="FLOW_ID"
      class="ost-canvas__flow"
      :nodes="flowNodes"
      :edges="flowEdges"
      :edge-types="EDGE_TYPES"
      :min-zoom="ZOOM_MIN"
      :max-zoom="ZOOM_MAX"
      :default-viewport="{ x: 0, y: 0, zoom: 0.82 }"
      :nodes-draggable="false"
      :nodes-connectable="false"
      :elements-selectable="false"
      :zoom-on-scroll="false"
      :pan-on-scroll="false"
      :zoom-on-double-click="false"
      :pan-on-drag="true"
      :only-render-visible-elements="true"
      @node-click="onNodeClick"
      @pane-ready="onReady"
    >
      <template #node-ost="{ id }">
        <OstNode
          v-if="tree.byId(id)"
          :node="tree.byId(id)!"
          :selected="ui.selectedId === id"
          :match="matchesQuery(tree.byId(id)!, ui.query)"
          :dimmed="isDimmed(tree.byId(id)!, ui.query, ui.hiddenTypes)"
          :drop-target="ui.dropTargetId === id"
          :child-count="childCounts.get(id) ?? 0"
          :collapsed="!!ui.collapsed[id]"
          :can-add="tree.canEdit && ALLOWED[tree.byId(id)!.type].length > 0"
          :evidence="tree.byId(id)!.type === 'solution' ? evidenceStrength(id, tree.nodes) : null"
          @add="emit('add', id)"
          @toggle="ui.toggleCollapse(id)"
          @chat="ui.openChat(id)"
        />
      </template>
    </VueFlow>

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
 * tidy layout (tree.placed) — users never place a node, and dragging is off in this step (drag to
 * re-parent arrives with canvas editing). Selection lives in the ui store, not in Vue Flow.
 *
 * Wiring follows design_handoff_ombuto_ost/vue-reference/src/components/TreeCanvas.vue, with the
 * handoff's viewport rules: custom cursor-anchored wheel zoom (Vue Flow's zoom-on-scroll off), Fit
 * with a 0.68 floor on first mount and on product switch, only-render-visible-elements, and a
 * 180ms ease on node moves.
 */
import { computed, markRaw, nextTick, ref, watch } from 'vue';

import { type EdgeTypesObject, type NodeMouseEvent, VueFlow, useVueFlow } from '@vue-flow/core';

import { evidenceStrength } from '../domain/derive';
import { ALLOWED } from '../domain/rules';
import { useOstTreeStore } from '../stores/ost-tree.store';
import { useOstUiStore } from '../stores/ost-ui.store';

import CanvasLegend from './CanvasLegend.vue';
import CanvasMinimap from './CanvasMinimap.vue';
import OstEdge from './OstEdge.vue';
import OstNode from './OstNode.vue';
import { isDimmed, laidOutNodes, matchesQuery, toFlowEdges, toFlowNodes } from './canvas-model';
import { ZOOM_MAX, ZOOM_MIN, useViewport, wheelZoom } from './useViewport';

const emit = defineEmits<{ select: [key: string]; add: [key: string]; zoom: [zoom: number] }>();

const FLOW_ID = 'ost-canvas';
// OstEdge declares only the props it uses; Vue Flow's EdgeProps typing wants them all.
const EDGE_TYPES = { ost: markRaw(OstEdge) } as unknown as EdgeTypesObject;

const tree = useOstTreeStore();
const ui = useOstUiStore();
const flow = useVueFlow(FLOW_ID);
const wrap = ref<HTMLElement | null>(null);
const view = useViewport(flow, wrap);
const zoom = view.zoom;

const visible = computed(() => laidOutNodes(tree.nodes, tree.placed));
const flowNodes = computed(() => toFlowNodes(visible.value, tree.placed));
const flowEdges = computed(() => toFlowEdges(visible.value, tree.placed));
/** Direct children per node (the collapse chip's +n). */
const childCounts = computed(() => {
  const counts = new Map<string, number>();
  for (const n of tree.nodes) if (n.parent) counts.set(n.parent, (counts.get(n.parent) ?? 0) + 1);
  return counts;
});

watch(zoom, z => emit('zoom', z), { immediate: true });

function onNodeClick({ node }: NodeMouseEvent) {
  emit('select', node.id);
}

function onMinimapWheel(deltaY: number) {
  const s = view.size();
  flow.setViewport(wheelZoom({ ...flow.viewport.value }, { x: s.width / 2, y: s.height / 2 }, deltaY));
}

// ---- fit + centring -------------------------------------------------------------------------------
const ready = ref(false);
let pendingCentre: string | null = null;

function applyPendingCentre() {
  if (!pendingCentre || !ready.value) return;
  const p = tree.placed[pendingCentre];
  if (p && view.centreOnBox(p)) pendingCentre = null;
}

/** Fit the branch in scope, then honour a pending deep-link centre. */
function fit() {
  if (!ready.value) return;
  view.fit(
    tree.placed,
    tree.roots.map(r => r.id),
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

defineExpose({ zoomIn: view.zoomIn, zoomOut: view.zoomOut, fit, centreOn });
</script>

<style scoped>
.ost-canvas {
  position: relative;
  flex: 1;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
  background-image: radial-gradient(circle, color-mix(in srgb, var(--color-text) 9%, transparent) 1px, transparent 1px);
  background-size: 28px 28px;
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
.ost-canvas :deep(.vue-flow__node.dragging) {
  transition: none;
}
.ost-canvas :deep(.vue-flow__pane) {
  cursor: grab;
}
.ost-canvas :deep(.vue-flow__pane.dragging) {
  cursor: grabbing;
}
</style>
