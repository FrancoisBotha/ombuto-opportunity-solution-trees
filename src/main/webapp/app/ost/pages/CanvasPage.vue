<template>
  <section class="ost-canvas-page" data-cy="ostCanvasPage">
    <CanvasToolbar :zoom="zoom" @product="chooseProduct" @zoom-in="canvas?.zoomIn()" @zoom-out="canvas?.zoomOut()" @fit="canvas?.fit()" />
    <div class="ost-canvas-page__body">
      <!-- step 9 adds the node palette on the left -->
      <TreeCanvas ref="canvas" @select="selectFromCanvas" @add="ui.openAddMenu" @zoom="zoom = $event" />
      <DetailPanel v-if="ui.selectedId" />
    </div>
  </section>
</template>

<script setup lang="ts">
/**
 * Tree canvas (/trees/:teamId/canvas?product=<key>&node=<key>).
 * The query is the deep link: `product` scopes the canvas, `node` selects a node and centres it.
 * Clicking a node selects it and writes ?node= back (without re-centring); the product combo
 * writes ?product= and the canvas re-fits.
 */
import { onMounted, ref, watch } from 'vue';
import { type LocationQuery, useRoute, useRouter } from 'vue-router';

import CanvasToolbar from '../canvas/CanvasToolbar.vue';
import TreeCanvas from '../canvas/TreeCanvas.vue';
import DetailPanel from '../panel/DetailPanel.vue';
import { useOstTreeStore } from '../stores/ost-tree.store';
import { useOstUiStore } from '../stores/ost-ui.store';

const route = useRoute();
const router = useRouter();
const tree = useOstTreeStore();
const ui = useOstUiStore();

const canvas = ref<InstanceType<typeof TreeCanvas> | null>(null);
const zoom = ref(1);

const queryKey = (value: LocationQuery[string]) => (typeof value === 'string' ? value : null);

watch(
  () => [route.query.product, route.query.node, tree.team?.id] as const,
  ([product, node]) => {
    const productKey = queryKey(product);
    ui.setProduct(productKey && tree.byId(productKey)?.type === 'product' ? productKey : 'all');
    const nodeKey = queryKey(node);
    // A ?node= that is not the current selection came from outside (deep link, other page): centre it.
    if (nodeKey && tree.byId(nodeKey) && nodeKey !== ui.selectedId) {
      ui.select(nodeKey);
      canvas.value?.centreOn(nodeKey);
    }
  },
  { immediate: true },
);

// Keep ?node= in step with the selection, whoever changed it (canvas, panel, ...).
watch(
  () => ui.selectedId,
  key => {
    if ((key ?? null) !== queryKey(route.query.node)) replaceQuery({ node: key ?? undefined });
  },
);

// The panel (breadcrumb, child list, quick-add) asks for a node to be centred.
watch(
  () => ui.centreRequest,
  req => req && canvas.value?.centreOn(req.key),
);

onMounted(() => {
  const nodeKey = queryKey(route.query.node);
  if (nodeKey && tree.byId(nodeKey)) canvas.value?.centreOn(nodeKey);
});

function replaceQuery(patch: Record<string, string | undefined>) {
  const query: LocationQuery = { ...route.query };
  for (const [k, v] of Object.entries(patch)) {
    if (v === undefined) delete query[k];
    else query[k] = v;
  }
  router.replace({ query });
}

function selectFromCanvas(key: string) {
  ui.select(key);
}

function chooseProduct(productId: string | 'all') {
  // Set the scope now (the canvas re-fits on change); the query watcher confirms it.
  ui.setProduct(productId);
  replaceQuery({ product: productId === 'all' ? undefined : productId });
}
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
</style>
