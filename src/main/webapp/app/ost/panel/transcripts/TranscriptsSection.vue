<template>
  <section v-if="node" class="ost-transcripts" data-cy="ost-transcripts" :data-node-key="nodeKey">
    <h3 class="ost-transcripts__title">
      Transcripts<span v-if="items.length" class="ost-transcripts__count">({{ items.length }})</span>
    </h3>
    <p v-if="loading && !items.length" class="ost-transcripts__empty">Loading transcripts…</p>
    <p v-else-if="!items.length" class="ost-transcripts__empty" data-cy="ost-transcripts-empty">No transcripts on this node yet.</p>
    <ol v-else class="ost-transcripts__list" aria-label="Transcripts, newest first">
      <li v-for="t in items" :key="t.id" class="ost-transcripts__item">
        <button
          type="button"
          class="ost-transcripts__row"
          :data-cy="`ost-transcript-item-${t.id}`"
          :data-transcript-id="t.id"
          @click="open(t.id)"
        >
          <span class="ost-transcripts__row-title">{{ t.title }}</span>
          <span class="ost-transcripts__row-meta">
            <time :datetime="t.meetingDate">{{ t.meetingDate }}</time>
            <span v-if="t.attendees" class="ost-transcripts__row-attendees" :title="t.attendees">· {{ t.attendees }}</span>
          </span>
        </button>
      </li>
    </ol>
    <TranscriptViewer v-if="openId !== null" :transcript-id="openId" @close="openId = null" />
  </section>
</template>

<script setup lang="ts">
/**
 * MTRANS-005 — Transcripts section in the node detail panel, beneath the chat.
 *
 * Lists a node's transcripts newest first, showing title, meeting date and attendees only —
 * bodies are NEVER fetched for the list (NFR-024). Clicking a row opens the viewer, which pulls
 * that transcript's body on demand. This ticket ships the read side only: viewers and editors
 * see the same list and viewer, and no mutation controls are rendered here (add / edit / delete
 * belong to a later frontend ticket in the epic breakdown).
 */
import { computed, onMounted, ref, watch } from 'vue';

import type { TranscriptMetaDTO } from '../../ost.model';
import { useOstTreeStore } from '../../stores/ost-tree.store';

import TranscriptViewer from './TranscriptViewer.vue';

const props = defineProps<{ nodeKey: string }>();
const tree = useOstTreeStore();

const node = computed(() => tree.byId(props.nodeKey));
const items = computed<TranscriptMetaDTO[]>(() => tree.transcripts[props.nodeKey] ?? []);
const loading = ref(false);
const openId = ref<number | null>(null);
let loadingKey: string | null = null;

async function load() {
  const key = props.nodeKey;
  if (!node.value || loadingKey === key) return;
  loadingKey = key;
  loading.value = true;
  try {
    await tree.loadTranscripts(key);
  } finally {
    if (loadingKey === key) {
      loadingKey = null;
      loading.value = false;
    }
  }
}

function open(id: number) {
  openId.value = id;
}

onMounted(load);
watch(
  () => props.nodeKey,
  () => {
    openId.value = null;
    void load();
  },
);
</script>

<style scoped>
.ost-transcripts {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--color-divider);
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.ost-transcripts__title {
  margin: 0;
  font-family: var(--font-heading);
  font-size: 11px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--color-neutral-400);
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.ost-transcripts__count {
  font-weight: 400;
  color: var(--color-neutral-500);
}

.ost-transcripts__empty {
  margin: 0;
  font-size: 12px;
  color: var(--color-neutral-500);
}

.ost-transcripts__list {
  margin: 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.ost-transcripts__row {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
  width: 100%;
  background: transparent;
  border: 1px solid var(--color-divider);
  border-radius: 0;
  padding: 6px 8px;
  font: inherit;
  color: inherit;
  text-align: left;
  cursor: pointer;
}

.ost-transcripts__row:hover {
  background: var(--color-accent-900);
  border-color: var(--color-accent-600);
}

.ost-transcripts__row-title {
  font-size: 12.5px;
  line-height: 1.3;
  text-wrap: pretty;
}

.ost-transcripts__row-meta {
  font-size: 10.5px;
  color: var(--color-neutral-500);
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}

.ost-transcripts__row-attendees {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 100%;
}
</style>
