<template>
  <OstDialog title="Transcript" width="640px" data-cy="ost-transcript-viewer" flush @close="emit('close')">
    <template #header="{ titleId }">
      <div class="ost-transcript-viewer__header">
        <h2 :id="titleId" class="ost-transcript-viewer__title" data-cy="ost-transcript-viewer-title">
          {{ transcript?.title ?? (loading ? 'Loading transcript…' : 'Transcript') }}
        </h2>
        <div v-if="transcript" class="ost-transcript-viewer__meta" data-cy="ost-transcript-viewer-meta">
          <time :datetime="transcript.meetingDate">{{ transcript.meetingDate }}</time>
          <span v-if="transcript.attendees">· {{ transcript.attendees }}</span>
        </div>
      </div>
    </template>
    <p v-if="loading" class="ost-transcript-viewer__empty">Loading transcript…</p>
    <p v-else-if="error" class="ost-transcript-viewer__empty" data-cy="ost-transcript-viewer-error">{{ error }}</p>
    <pre v-else-if="transcript" class="ost-transcript-viewer__body" data-cy="ost-transcript-viewer-body">{{ transcript.body }}</pre>
  </OstDialog>
</template>

<script setup lang="ts">
/**
 * MTRANS-005 — Transcript viewer dialog.
 *
 * Fetches the transcript body on open (lazy — the list never carries bodies, NFR-024), and shows
 * it in a scrollable `<pre>` that preserves line breaks. The body is rendered as text through
 * Vue's normal interpolation (`{{ body }}`), NEVER via `v-html`, so an attacker's HTML inside a
 * pasted transcript can never execute (NFR-023). Team viewers open the same viewer editors do,
 * without any mutation controls.
 */
import { onMounted, ref, watch } from 'vue';

import type { TranscriptDTO } from '../../ost.model';
import { useOstTreeStore } from '../../stores/ost-tree.store';
import OstDialog from '../../overlays/OstDialog.vue';

const props = defineProps<{ transcriptId: number }>();
const emit = defineEmits<{ close: [] }>();

const tree = useOstTreeStore();
const transcript = ref<TranscriptDTO | null>(null);
const loading = ref(false);
const error = ref<string | null>(null);

async function load() {
  loading.value = true;
  error.value = null;
  transcript.value = null;
  try {
    const dto = await tree.loadTranscript(props.transcriptId);
    if (!dto) error.value = 'The transcript could not be loaded.';
    else transcript.value = dto;
  } finally {
    loading.value = false;
  }
}

onMounted(load);
watch(
  () => props.transcriptId,
  () => void load(),
);
</script>

<style scoped>
.ost-transcript-viewer__header {
  padding: 16px 20px 8px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  border-bottom: 1px solid var(--color-divider);
}

.ost-transcript-viewer__title {
  margin: 0;
  font-family: var(--font-heading);
  font-size: 16px;
  font-weight: 600;
}

.ost-transcript-viewer__meta {
  font-size: 11.5px;
  color: var(--color-neutral-500);
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.ost-transcript-viewer__empty {
  margin: 0;
  padding: 20px;
  font-size: 12.5px;
  color: var(--color-neutral-500);
}

.ost-transcript-viewer__body {
  margin: 0;
  padding: 16px 20px;
  max-height: 60vh;
  overflow: auto;
  font-family: var(--font-mono, ui-monospace, SFMono-Regular, Menlo, monospace);
  font-size: 12.5px;
  line-height: 1.45;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
