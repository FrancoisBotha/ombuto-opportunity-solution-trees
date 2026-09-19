<script setup lang="ts">
/**
 * Vue Flow wiring. Positions come from layoutTree() — users never free-place a
 * node; dragging a node onto another RE-PARENTS it.
 */
import { computed, markRaw, ref } from 'vue';
import { VueFlow, type NodeDragEvent } from '@vue-flow/core';
import { Background } from '@vue-flow/background';
import { MiniMap } from '@vue-flow/minimap';
import { Controls } from '@vue-flow/controls';
import OstNodeView from './OstNode.vue';
import { useTree } from '../stores/tree';
import { canReparent, TYPE_BOX } from '../domain/rules';

const tree = useTree();
const dropTargetId = ref<string | null>(null);

const visible = computed(() => {
  const hidden = (id: string | null): boolean => {
    let p = id;
    while (p) { if (tree.collapsed[p]) return true; p = tree.byId(p)?.parent ?? null; }
    return false;
  };
  return tree.nodes.filter(n => tree.placed[n.id] && !hidden(n.parent));
});

const flowNodes = computed(() => visible.value.map(n => {
  const p = tree.placed[n.id];
  const q = tree.query.trim().toLowerCase();
  const match = !!q && (n.title + ' ' + n.note).toLowerCase().includes(q);
  return {
    id: n.id,
    type: 'ost',
    position: { x: p.x - p.w / 2, y: p.y },
    data: {
      node: n,
      selected: tree.selectedId === n.id,
      dimmed: (!!q && !match) || !!tree.hiddenTypes[n.type],
      match,
      dropTarget: dropTargetId.value === n.id,
      childCount: tree.children(n.id).length,
      collapsed: !!tree.collapsed[n.id],
    },
  };
}));

const flowEdges = computed(() => visible.value.filter(n => n.parent).map(n => ({
  id: 'e-' + n.id,
  source: n.parent!,
  target: n.id,
  type: 'smoothstep',
  style: {
    stroke: n.type === 'evidence' ? 'var(--color-neutral-700)' : 'var(--color-accent-600)',
    strokeWidth: n.type === 'opportunity' || n.type === 'outcome' ? 1.6 : 1.1,
    strokeDasharray: n.type === 'assumption' || n.type === 'evidence' ? '4 4' : undefined,
  },
})));

/** Hit-test the dragged node against every other box to find a legal parent. */
function hitTest(e: NodeDragEvent) {
  const { x, y } = e.node.position;
  const cx = x + TYPE_BOX[tree.byId(e.node.id)!.type].w / 2;
  const hit = Object.entries(tree.placed).find(([id, p]) =>
    id !== e.node.id && cx > p.x - p.w / 2 && cx < p.x + p.w / 2 && y > p.y - 40 && y < p.y + p.h);
  dropTargetId.value = hit && canReparent(e.node.id, hit[0], tree.nodes) ? hit[0] : null;
}
function onDragStop(e: NodeDragEvent) {
  if (dropTargetId.value) tree.reparent(e.node.id, dropTargetId.value);
  dropTargetId.value = null; // rejected drops snap back: positions are derived
}
</script>

<template>
  <VueFlow
    :nodes="flowNodes" :edges="flowEdges"
    :node-types="{ ost: markRaw(OstNodeView) }"
    :min-zoom="0.35" :max-zoom="1.6" :nodes-draggable="true" :fit-view-on-init="true"
    @node-click="tree.selectedId = $event.node.id"
    @node-drag="hitTest" @node-drag-stop="onDragStop"
  >
    <Background :gap="28" :size="1" pattern-color="color-mix(in srgb, var(--color-text) 9%, transparent)" />
    <MiniMap pannable zoomable />
    <Controls />
  </VueFlow>
</template>
