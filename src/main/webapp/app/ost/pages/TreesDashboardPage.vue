<template>
  <section class="ost-page" data-cy="ostDashboardPage">
    <!-- STUB (step 6): step 8 builds the four counters and one card per product. -->
    <div class="ost-label" data-cy="ostDashboardKicker">{{ tree.team?.name }} · continuous discovery</div>
    <h1 class="ost-page__title">Trees</h1>
  </section>
</template>

<script setup lang="ts">
/** Trees dashboard (/trees/:teamId). Owned by step 8 — use dashboardStats/productCards from domain/derive.ts. */
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
