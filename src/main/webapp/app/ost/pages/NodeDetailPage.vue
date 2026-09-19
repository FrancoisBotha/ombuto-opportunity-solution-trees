<template>
  <section class="ost-page" data-cy="ostNodeDetailPage">
    <!-- STUB (step 6): step 12 builds breadcrumb, tags, notes, child cards and the discussion column. -->
    <template v-if="node">
      <div class="ost-label">{{ typeLabel }}</div>
      <h1 class="ost-page__title" data-cy="ostNodeDetailTitle">{{ node.title }}</h1>
    </template>
    <div v-else class="ost-state" data-cy="ostNodeMissing">
      <div class="ost-state__title">Node not found</div>
      <p class="ost-state__text">It may have been deleted, or it belongs to another team.</p>
      <router-link class="ost-btn ost-btn--primary" :to="{ name: 'OstCanvas', params: { teamId: route.params.teamId } }">
        Open the tree canvas
      </router-link>
    </div>
  </section>
</template>

<script setup lang="ts">
/** Full-page node detail (/trees/:teamId/nodes/:nodeKey). Owned by step 12. */
import { computed } from 'vue';
import { useRoute } from 'vue-router';

import { TYPE_BOX } from '../domain/rules';
import { useOstTreeStore } from '../stores/ost-tree.store';

const route = useRoute();
const tree = useOstTreeStore();
const node = computed(() => tree.byId(String(route.params.nodeKey ?? '')));
const typeLabel = computed(() => (node.value ? TYPE_BOX[node.value.type].label : ''));
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
