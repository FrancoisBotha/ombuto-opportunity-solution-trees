<template>
  <section class="ost-page" data-cy="ostExperimentsPage">
    <!-- STUB (step 6): step 8 builds the tracker table + summary (rows open the node on the canvas). -->
    <div class="ost-label">{{ tree.team?.name }} · experiments</div>
    <h1 class="ost-page__title">Experiments</h1>
  </section>
</template>

<script setup lang="ts">
/** Experiments tracker (/trees/:teamId/experiments). Owned by step 8 — use experimentRows from domain/derive.ts. */
import { useOstTreeStore } from '../stores/ost-tree.store';

const tree = useOstTreeStore();
</script>

<style scoped>
.ost-page {
  padding: 26px 32px;
  overflow: auto;
  height: 100%;
}

.ost-page__title {
  font-size: 36px;
  font-weight: 400;
  margin-top: 6px;
}
</style>
