<template>
  <div v-if="node" class="ost-tab ost-history" data-cy="ostTab-history">
    <p v-if="loading && !entries.length" class="ost-history__empty">Loading the history…</p>
    <p v-else-if="!entries.length" class="ost-history__empty" data-cy="ost-history-empty">No changes recorded yet.</p>
    <ol v-else class="ost-history__list" aria-label="Change history, newest first">
      <li v-for="(h, i) in entries" :key="h.id" class="ost-history__item" :data-cy="`ost-history-${h.id}`" :data-event="h.eventType">
        <div class="ost-history__rail" aria-hidden="true">
          <i class="ost-history__dot" :class="{ 'is-latest': i === 0 }"></i>
          <i v-if="i < entries.length - 1" class="ost-history__line"></i>
        </div>
        <div class="ost-history__body">
          <div class="ost-history__what" data-cy="ost-history-what">{{ h.summary }}</div>
          <div class="ost-history__meta" data-cy="ost-history-meta">
            <span :title="h.authorLogin ?? ''">{{ h.authorInitials || h.authorLogin || 'System' }}</span> ·
            <time :datetime="h.createdDate" :title="exactTime(h.createdDate)">{{ formatStamp(h.createdDate) }}</time>
          </div>
        </div>
      </li>
    </ol>
  </div>
</template>

<script setup lang="ts">
/**
 * History tab (FR-H1/H2): the node's append-only changelog, newest first — what · author ·
 * timestamp — as the prototype's timeline. The server writes the entries; the tree store drops the
 * cached list after every write that records one, and this tab reloads when that happens.
 */
import { computed, onMounted, ref, watch } from 'vue';

import { formatStamp } from '../../chat/chat-format';
import type { HistoryEntryDTO } from '../../ost.model';
import { useOstTreeStore } from '../../stores/ost-tree.store';

const props = defineProps<{ nodeKey: string }>();
const tree = useOstTreeStore();

const node = computed(() => {
  const n = tree.byId(props.nodeKey);
  return n && n.type !== 'product' ? n : undefined;
});
const entries = computed<HistoryEntryDTO[]>(() => tree.history[props.nodeKey] ?? []);
const loading = ref(false);

const exactTime = (iso: string) => new Date(iso).toLocaleString();

async function load() {
  if (!node.value || loading.value) return;
  loading.value = true;
  try {
    await tree.loadHistory(props.nodeKey);
  } finally {
    loading.value = false;
  }
}

// Always refresh on open (other people's changes), then whenever a write invalidates the cache.
onMounted(load);
watch(
  () => tree.history[props.nodeKey],
  cached => {
    if (cached === undefined) void load();
  },
);
</script>

<style scoped>
.ost-history__list {
  display: flex;
  flex-direction: column;
  margin: 0;
  padding: 0;
  list-style: none;
}

.ost-history__item {
  display: flex;
  gap: 10px;
  padding: 0 0 14px;
}

.ost-history__rail {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex: none;
  width: 16px;
}

.ost-history__dot {
  width: 7px;
  height: 7px;
  flex: none;
  margin-top: 5px;
  border-radius: 50%;
  background: var(--color-neutral-600);
}

.ost-history__dot.is-latest {
  background: var(--color-accent-400);
}

.ost-history__line {
  flex: 1;
  width: 1px;
  margin-top: 3px;
  margin-bottom: -14px;
  background: var(--color-neutral-800);
}

.ost-history__body {
  min-width: 0;
  padding-bottom: 2px;
}

.ost-history__what {
  font-size: 12.5px;
  line-height: 1.3;
  text-wrap: pretty;
}

.ost-history__meta {
  margin-top: 2px;
  font-size: 10.5px;
  color: var(--color-neutral-500);
}

.ost-history__empty {
  margin: 0;
  font-size: 12px;
  color: var(--color-neutral-500);
}
</style>
