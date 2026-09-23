<template>
  <OstDialog :title="dialogTitle" width="720px" data-cy="ost-transcript-form" @close="close">
    <form class="ost-transcript-form" @submit.prevent="save">
      <div class="ost-transcript-form__row">
        <label for="ost-transcript-title" class="ost-transcript-form__label">Title</label>
        <input
          id="ost-transcript-title"
          v-model="title"
          type="text"
          class="ost-transcript-form__input"
          data-cy="ost-transcript-form-title"
          :maxlength="TITLE_MAX"
          autocomplete="off"
          autofocus
          required
        />
      </div>
      <div class="ost-transcript-form__grid">
        <div class="ost-transcript-form__row">
          <label for="ost-transcript-date" class="ost-transcript-form__label">Meeting date</label>
          <input
            id="ost-transcript-date"
            v-model="meetingDate"
            type="date"
            class="ost-transcript-form__input"
            data-cy="ost-transcript-form-date"
            required
          />
        </div>
        <div class="ost-transcript-form__row">
          <label for="ost-transcript-attendees" class="ost-transcript-form__label">Attendees</label>
          <input
            id="ost-transcript-attendees"
            v-model="attendees"
            type="text"
            class="ost-transcript-form__input"
            data-cy="ost-transcript-form-attendees"
            :maxlength="ATTENDEES_MAX"
            placeholder="e.g. Kira, Ana, Sam"
          />
        </div>
      </div>
      <div
        v-if="!isEdit"
        class="ost-transcript-form__drop"
        :class="{ 'ost-transcript-form__drop--over': dragOver, 'ost-transcript-form__drop--busy': parsing }"
        data-cy="ost-transcript-form-drop"
        @dragover.prevent="dragOver = true"
        @dragenter.prevent="dragOver = true"
        @dragleave.prevent="dragOver = false"
        @drop.prevent="onDrop"
      >
        <p class="ost-transcript-form__drop-hint">
          Drop a .txt, .vtt or .srt file here, or
          <button
            type="button"
            class="ost-transcript-form__drop-link"
            data-cy="ost-transcript-form-file-btn"
            :disabled="parsing"
            @click="pickFile"
          >
            choose a file
          </button>
          to fill the body below. Parsing removes cue timestamps and keeps speaker labels.
        </p>
        <p v-if="parsing" class="ost-transcript-form__drop-status" data-cy="ost-transcript-form-parsing">Parsing…</p>
        <p v-else-if="uploadedFilename" class="ost-transcript-form__drop-status" data-cy="ost-transcript-form-uploaded">
          Loaded <code>{{ uploadedFilename }}</code> — review the body below and click Save to store as an uploaded transcript.
        </p>
        <input
          ref="fileInput"
          type="file"
          class="ost-transcript-form__file-input"
          data-cy="ost-transcript-form-file"
          accept=".txt,.vtt,.srt"
          @change="onFileChosen"
        />
      </div>
      <div class="ost-transcript-form__row">
        <label for="ost-transcript-body" class="ost-transcript-form__label">Transcript</label>
        <textarea
          id="ost-transcript-body"
          v-model="body"
          rows="16"
          class="ost-transcript-form__body"
          data-cy="ost-transcript-form-body"
          placeholder="Paste the meeting transcript here…"
          spellcheck="false"
          required
        ></textarea>
        <p class="ost-transcript-form__hint">
          {{ body.length }} character{{ body.length === 1 ? '' : 's' }} · up to {{ MAX_BODY_BYTES.toLocaleString() }} bytes.
        </p>
      </div>
      <p v-if="error" class="ost-transcript-form__error" data-cy="ost-transcript-form-error" role="alert">{{ error }}</p>
    </form>
    <template #actions>
      <button type="button" class="ost-btn" data-cy="ost-transcript-form-cancel" :disabled="saving" @click="close">Cancel</button>
      <button
        type="button"
        class="ost-btn ost-btn--primary"
        data-cy="ost-transcript-form-save"
        :disabled="saving || parsing || !canSave"
        @click="save"
      >
        {{ saving ? 'Saving…' : 'Save' }}
      </button>
    </template>
  </OstDialog>
</template>

<script setup lang="ts">
/**
 * MTRANS-006 — add / edit transcript dialog.
 *
 * A form with title, meeting date, attendees and a large monospace textarea for the body, plus a
 * file-drop area (on create only) that sends the file to the parse endpoint and fills the textarea
 * so the user can review the text before saving. Nothing is stored until the user clicks Save:
 *
 * - Manual paste → save with source=PASTED.
 * - File selection or drop → parsed by the server, textarea filled for review, save with
 *   source=UPLOADED once the user confirms.
 *
 * On save the store refreshes the node's transcript list and count, so the panel and the canvas
 * badge follow at once without WebSocket publication (NFR-022).
 *
 * Invalid or oversized uploads surface a clear message and the current draft (title, date,
 * attendees, whatever body was there) is left untouched — no transcript is created (criterion 3).
 */
import { computed, ref } from 'vue';

import type { TranscriptDTO } from '../../ost.model';
import { describeError } from '../../ost-errors';
import { useOstTreeStore } from '../../stores/ost-tree.store';
import OstDialog from '../../overlays/OstDialog.vue';

const TITLE_MIN = 2;
const TITLE_MAX = 200;
const ATTENDEES_MAX = 500;
/**
 * Client-side ceiling for the pasted body and the uploaded file (bytes of UTF-8). Matches the
 * server's {@code TreeMeetingTranscriptService.BODY_MAX_BYTES} and
 * {@code TranscriptUploadParser.MAX_UPLOAD_BYTES}. Kept as a UI hint and pre-flight guard: the
 * server remains the final gate.
 */
const MAX_BODY_BYTES = 1024 * 1024;
const ALLOWED_EXT = ['txt', 'vtt', 'srt'] as const;

const props = defineProps<{ nodeKey: string; transcript?: TranscriptDTO | null }>();
const emit = defineEmits<{ close: []; saved: [TranscriptDTO] }>();

const tree = useOstTreeStore();
const isEdit = computed(() => !!props.transcript);
const dialogTitle = computed(() => (isEdit.value ? 'Edit transcript' : 'Add transcript'));

const title = ref(props.transcript?.title ?? '');
const meetingDate = ref(props.transcript?.meetingDate ?? isoToday());
const attendees = ref(props.transcript?.attendees ?? '');
const body = ref(props.transcript?.body ?? '');
const uploadedFilename = ref<string | null>(null);
const source = ref<'PASTED' | 'UPLOADED'>(props.transcript?.source ?? 'PASTED');
const parsing = ref(false);
const saving = ref(false);
const error = ref<string | null>(null);
const dragOver = ref(false);
const fileInput = ref<HTMLInputElement | null>(null);

const canSave = computed(
  () =>
    title.value.trim().length >= TITLE_MIN &&
    title.value.trim().length <= TITLE_MAX &&
    !!meetingDate.value &&
    body.value.trim().length > 0 &&
    utf8Bytes(body.value) <= MAX_BODY_BYTES,
);

function isoToday(): string {
  const d = new Date();
  const pad = (n: number) => n.toString().padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

function utf8Bytes(text: string): number {
  return new Blob([text]).size;
}

function extOf(name: string): string {
  const dot = name.lastIndexOf('.');
  return dot < 0 ? '' : name.slice(dot + 1).toLowerCase();
}

function pickFile() {
  fileInput.value?.click();
}

function onFileChosen(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  if (file) void parseFile(file);
  input.value = '';
}

function onDrop(event: DragEvent) {
  dragOver.value = false;
  const file = event.dataTransfer?.files?.[0];
  if (file) void parseFile(file);
}

async function parseFile(file: File) {
  error.value = null;
  const ext = extOf(file.name);
  if (!ALLOWED_EXT.includes(ext as (typeof ALLOWED_EXT)[number])) {
    error.value = 'Uploaded file must be .txt, .vtt or .srt.';
    return;
  }
  if (file.size > MAX_BODY_BYTES) {
    error.value = `Uploaded file exceeds the ${MAX_BODY_BYTES.toLocaleString()} byte limit.`;
    return;
  }
  parsing.value = true;
  try {
    const parsed = await tree.parseTranscriptUpload(file);
    if (parsed === null) {
      error.value = tree.error ?? 'The uploaded file could not be parsed.';
      tree.clearError();
      return;
    }
    body.value = parsed;
    uploadedFilename.value = file.name;
    source.value = 'UPLOADED';
  } catch (err) {
    error.value = describeError(err, 'The uploaded file could not be parsed.');
  } finally {
    parsing.value = false;
  }
}

async function save() {
  if (!canSave.value || saving.value || parsing.value) return;
  saving.value = true;
  error.value = null;
  try {
    const request = {
      title: title.value.trim(),
      meetingDate: meetingDate.value,
      attendees: attendees.value.trim() || null,
      body: body.value,
      source: source.value,
    };
    const dto = isEdit.value
      ? await tree.updateTranscript(props.transcript!.id, request)
      : await tree.createTranscript(props.nodeKey, request);
    if (!dto) {
      error.value = tree.error ?? 'The transcript could not be saved.';
      tree.clearError();
      return;
    }
    emit('saved', dto);
    emit('close');
  } finally {
    saving.value = false;
  }
}

function close() {
  if (!saving.value && !parsing.value) emit('close');
}
</script>

<style scoped>
.ost-transcript-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.ost-transcript-form__row {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.ost-transcript-form__grid {
  display: grid;
  grid-template-columns: minmax(160px, 1fr) minmax(220px, 2fr);
  gap: 12px;
}

.ost-transcript-form__label {
  font-family: var(--font-heading);
  font-size: 11px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--color-neutral-400);
}

.ost-transcript-form__input {
  padding: 6px 8px;
  background: var(--color-surface);
  color: var(--color-text);
  border: 1px solid var(--color-neutral-700);
  border-radius: 0;
  font: inherit;
}

.ost-transcript-form__body {
  padding: 8px 10px;
  background: var(--color-surface);
  color: var(--color-text);
  border: 1px solid var(--color-neutral-700);
  border-radius: 0;
  font-family: var(--font-mono, ui-monospace, SFMono-Regular, Menlo, monospace);
  font-size: 12.5px;
  line-height: 1.45;
  resize: vertical;
  min-height: 220px;
}

.ost-transcript-form__hint {
  margin: 0;
  font-size: 11px;
  color: var(--color-neutral-500);
}

.ost-transcript-form__drop {
  padding: 12px;
  border: 1px dashed var(--color-neutral-700);
  background: color-mix(in srgb, var(--color-surface) 92%, var(--color-neutral-800));
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.ost-transcript-form__drop--over {
  border-color: var(--color-accent-600);
  background: var(--color-accent-900);
}

.ost-transcript-form__drop--busy {
  opacity: 0.7;
}

.ost-transcript-form__drop-hint {
  margin: 0;
  font-size: 12px;
  color: var(--color-neutral-400);
}

.ost-transcript-form__drop-status {
  margin: 0;
  font-size: 11.5px;
  color: var(--color-neutral-300);
}

.ost-transcript-form__drop-link {
  background: transparent;
  border: 0;
  padding: 0;
  color: var(--color-accent-400);
  cursor: pointer;
  text-decoration: underline;
  font: inherit;
}

.ost-transcript-form__file-input {
  display: none;
}

.ost-transcript-form__error {
  margin: 0;
  padding: 8px 10px;
  border: 1px solid var(--color-negative-600, #b3261e);
  color: var(--color-negative-100, #f8d7d5);
  background: color-mix(in srgb, var(--color-negative-600, #b3261e) 20%, transparent);
  font-size: 12px;
}
</style>
