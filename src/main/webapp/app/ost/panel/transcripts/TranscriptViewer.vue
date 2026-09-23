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
        <div v-if="transcript && tree.canEdit" class="ost-transcript-viewer__actions">
          <button type="button" class="ost-btn" data-cy="ost-transcript-viewer-edit" :disabled="deleting" @click="emitEdit">Edit</button>
          <template v-if="!confirmingDelete">
            <button
              type="button"
              class="ost-btn ost-btn--destructive"
              data-cy="ost-transcript-viewer-delete"
              :disabled="deleting"
              @click="confirmingDelete = true"
            >
              Delete
            </button>
          </template>
          <template v-else>
            <span class="ost-transcript-viewer__confirm" data-cy="ost-transcript-viewer-confirm"> Delete this transcript? </span>
            <button
              type="button"
              class="ost-btn"
              data-cy="ost-transcript-viewer-cancel-delete"
              :disabled="deleting"
              @click="confirmingDelete = false"
            >
              Cancel
            </button>
            <button
              type="button"
              class="ost-btn ost-btn--destructive"
              data-cy="ost-transcript-viewer-confirm-delete"
              :disabled="deleting"
              @click="doDelete"
            >
              {{ deleting ? 'Deleting…' : 'Delete' }}
            </button>
          </template>
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
 * MTRANS-005 / MTRANS-006 — Transcript viewer dialog.
 *
 * Fetches the transcript body on open (lazy — the list never carries bodies, NFR-024). Renders it
 * in a scrollable {@code <pre>} that preserves line breaks. The body is text-interpolated
 * ({@code \{\{ body \}\}}), NEVER via {@code v-html}, so HTML inside a pasted transcript cannot
 * execute (NFR-023).
 *
 * MTRANS-006 adds Edit and Delete affordances for editors / owners: {@code canEdit} gates them,
 * so viewers see the same read-only viewer they saw before. Delete requires an inline confirm
 * (criterion 4).
 */
import { onMounted, ref, watch } from 'vue';

import type { TranscriptDTO } from '../../ost.model';
import { useOstTreeStore } from '../../stores/ost-tree.store';
import OstDialog from '../../overlays/OstDialog.vue';

const props = defineProps<{ transcriptId: number }>();
const emit = defineEmits<{ close: []; edit: [TranscriptDTO]; deleted: [number] }>();

const tree = useOstTreeStore();
const transcript = ref<TranscriptDTO | null>(null);
const loading = ref(false);
const error = ref<string | null>(null);
const confirmingDelete = ref(false);
const deleting = ref(false);

async function load() {
  loading.value = true;
  error.value = null;
  transcript.value = null;
  confirmingDelete.value = false;
  try {
    const dto = await tree.loadTranscript(props.transcriptId);
    if (!dto) error.value = 'The transcript could not be loaded.';
    else transcript.value = dto;
  } finally {
    loading.value = false;
  }
}

function emitEdit() {
  if (transcript.value) emit('edit', transcript.value);
}

async function doDelete() {
  if (!transcript.value || deleting.value) return;
  deleting.value = true;
  try {
    const ok = await tree.deleteTranscript(transcript.value.id);
    if (ok) {
      emit('deleted', transcript.value.id);
      emit('close');
    }
  } finally {
    deleting.value = false;
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

.ost-transcript-viewer__actions {
  display: flex;
  gap: 6px;
  align-items: center;
  margin-top: 6px;
  flex-wrap: wrap;
}

.ost-transcript-viewer__confirm {
  font-size: 12px;
  color: var(--color-neutral-300);
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
