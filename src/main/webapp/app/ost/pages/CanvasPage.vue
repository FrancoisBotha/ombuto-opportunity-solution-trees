<template>
  <section class="ost-canvas-page" data-cy="ostCanvasPage">
    <!-- STUB (step 6): step 7 builds CanvasToolbar, NodePalette, TreeCanvas (Vue Flow) and the legend. -->
    <div class="ost-canvas-page__body">
      <div class="ost-canvas-page__canvas" data-cy="ostCanvasStub">
        <span class="ost-label">{{ tree.roots.length }} product branch{{ tree.roots.length === 1 ? '' : 'es' }} in scope</span>
      </div>
      <DetailPanel v-if="ui.selectedId && ui.rightOpen" />
    </div>
  </section>
</template>

<script setup lang="ts">
/**
 * Tree canvas (/trees/:teamId/canvas?product=<key>&node=<key>). Owned by step 7.
 * The query is the deep link: `product` scopes the canvas, `node` selects a node.
 */
import { watch } from 'vue';
import { useRoute } from 'vue-router';

import DetailPanel from '../panel/DetailPanel.vue';
import { useOstTreeStore } from '../stores/ost-tree.store';
import { useOstUiStore } from '../stores/ost-ui.store';

const route = useRoute();
const tree = useOstTreeStore();
const ui = useOstUiStore();

watch(
  () => [route.query.product, route.query.node, tree.team?.id] as const,
  ([product, node]) => {
    const productKey = typeof product === 'string' && tree.byId(product)?.type === 'product' ? product : 'all';
    ui.setProduct(productKey);
    if (typeof node === 'string' && tree.byId(node)) ui.select(node);
  },
  { immediate: true },
);
</script>

<style scoped>
.ost-canvas-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.ost-canvas-page__body {
  flex: 1;
  display: flex;
  min-height: 0;
}

.ost-canvas-page__canvas {
  flex: 1;
  display: grid;
  place-items: center;
  background-image: radial-gradient(circle, color-mix(in srgb, var(--color-text) 9%, transparent) 1px, transparent 1px);
  background-size: 28px 28px;
}
</style>
