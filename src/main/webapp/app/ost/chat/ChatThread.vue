<template>
  <section class="ost-chat" :class="`ost-chat--${variant}`" data-cy="ost-chat-thread" :data-node-key="nodeKey" aria-label="Conversation">
    <div class="ost-chat__viewport">
      <div
        ref="scroller"
        class="ost-chat__scroll"
        role="log"
        aria-label="Messages"
        aria-live="polite"
        tabindex="0"
        data-cy="ost-chat-scroll"
        @scroll="onScroll"
      >
        <p v-if="loading && !items.length" class="ost-chat__empty">Loading the conversation…</p>
        <p v-else-if="!items.length" class="ost-chat__empty" data-cy="ost-chat-empty">No messages yet. Start the conversation.</p>
        <div
          v-for="item in items"
          :key="item.comment.id"
          class="ost-chat__item"
          :class="{ 'is-mine': item.comment.mine, 'is-run-start': item.runStart }"
        >
          <div v-if="item.stamp" class="ost-chat__stamp" data-cy="ost-chat-stamp">{{ item.stamp }}</div>
          <div
            v-if="item.who"
            class="ost-chat__who"
            :title="item.comment.authorName ?? item.comment.authorLogin ?? ''"
            data-cy="ost-chat-who"
          >
            {{ item.who }}
          </div>
          <div
            class="ost-chat__bubble"
            :class="{ 'is-editing': editingId === item.comment.id }"
            :data-cy="`ost-chat-msg-${item.comment.id}`"
            :data-mine="item.comment.mine ? 'true' : 'false'"
            :title="exactTime(item.comment.createdDate)"
          >
            <span class="ost-chat__text">{{ item.comment.body }}</span
            ><span
              v-if="item.comment.editedDate"
              class="ost-chat__edited"
              data-cy="ost-chat-edited"
              :title="exactTime(item.comment.editedDate)"
            >
              (edited)</span
            >
          </div>
          <div v-if="item.comment.mine && canEdit" class="ost-chat__own">
            <button
              type="button"
              class="ost-chat__own-btn ost-tap"
              :aria-label="`Edit your message: ${item.comment.body.slice(0, 40)}`"
              :disabled="busy"
              :data-cy="`ost-chat-edit-${item.comment.id}`"
              @click="startEdit(item.comment)"
            >
              Edit
            </button>
            <button
              type="button"
              class="ost-chat__own-btn ost-tap"
              :aria-label="`Delete your message: ${item.comment.body.slice(0, 40)}`"
              :disabled="busy"
              :data-cy="`ost-chat-delete-${item.comment.id}`"
              @click="remove(item.comment.id)"
            >
              Delete
            </button>
          </div>
        </div>
      </div>
      <button
        v-if="!atBottom && items.length"
        type="button"
        class="ost-chat__jump ost-tap"
        title="Jump to latest"
        aria-label="Jump to the latest message"
        data-cy="ost-chat-jump"
        @click="jumpToLatest"
      >
        <PhCaretDown :size="14" aria-hidden="true" />
      </button>
    </div>

    <div v-if="error" class="ost-chat__error" role="alert" data-cy="ost-chat-error">
      <span>{{ error }}</span>
      <button type="button" class="ost-chat__error-close ost-tap" aria-label="Dismiss" @click="error = null">
        <PhX :size="11" aria-hidden="true" />
      </button>
    </div>

    <footer class="ost-chat__composer">
      <div v-if="editingId !== null" class="ost-chat__editing" data-cy="ost-chat-editing">
        <span class="ost-chat__editing-pill">Editing message</span>
        <button type="button" class="ost-chat__own-btn ost-tap" data-cy="ost-chat-edit-cancel" @click="cancelEdit(true)">Cancel</button>
      </div>
      <div class="ost-chat__row">
        <textarea
          ref="input"
          v-model="draft"
          class="ost-input ost-chat__input"
          rows="1"
          :maxlength="BODY_MAX"
          :placeholder="canEdit ? 'Message the trio' : 'Read only'"
          :aria-label="editingId !== null ? 'Edit your message' : 'Message'"
          :aria-describedby="canEdit ? undefined : readonlyId"
          :disabled="!canEdit"
          :data-autofocus="autofocus && canEdit ? '' : undefined"
          data-cy="ost-chat-input"
          @keydown="onKey"
          @input="autosize"
        ></textarea>
        <button
          type="button"
          class="ost-chat__send"
          :class="{ 'is-ready': ready }"
          :disabled="!ready"
          :title="editingId !== null ? 'Save' : 'Send'"
          :aria-label="editingId !== null ? 'Save the edited message' : 'Send message'"
          data-cy="ost-chat-send"
          @click="send"
        >
          <PhPaperPlaneRight :size="15" weight="fill" aria-hidden="true" />
        </button>
      </div>
      <p v-if="!canEdit" :id="readonlyId" class="ost-chat__readonly" data-cy="ost-chat-readonly">
        You have view access: you can read this thread, but only owners and editors can post.
      </p>
    </footer>
  </section>
</template>

<script setup lang="ts">
/**
 * A node's chat thread — shared by the panel's Chat tab, the chat modal (node chip) and the
 * full-page node detail. Bubbles are grouped into runs (chat-format.ts): own messages right in
 * accent, others left in neutral, a date-time stamp above a run and the author's initials on its
 * first bubble. Own messages can be edited (in the composer: Enter saves, Escape cancels, marked
 * "(edited)") and deleted. Enter sends, Shift+Enter adds a line. The thread follows the latest
 * message; a jump-to-latest button appears when scrolled away. Viewers read only (A4).
 *
 * Messages and the node's comment count go through the tree store, so the node chip and the tab
 * badge follow every add / delete.
 */
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';

import { PhCaretDown, PhPaperPlaneRight, PhX } from '@phosphor-icons/vue';

import type { CommentDTO } from '../ost.model';
import { useOstTreeStore } from '../stores/ost-tree.store';

import { groupThread, isNearBottom } from './chat-format';

const props = withDefaults(defineProps<{ nodeKey: string; variant?: 'panel' | 'modal' | 'page'; autofocus?: boolean }>(), {
  variant: 'panel',
  autofocus: false,
});

/** Server limit (TreeCommentService.BODY_MAX). */
const BODY_MAX = 10_000;

const tree = useOstTreeStore();

const scroller = ref<HTMLElement | null>(null);
const input = ref<HTMLTextAreaElement | null>(null);
const loading = ref(false);
const busy = ref(false);
const draft = ref('');
const editingId = ref<number | null>(null);
const error = ref<string | null>(null);
/** The view shows the latest message; false once the user scrolls up (shows the jump button). */
const atBottom = ref(true);
/** While a smooth jump runs, intermediate scroll events must not bring the button back. */
let jumping: ReturnType<typeof setTimeout> | null = null;
let loadSeq = 0;

const readonlyId = `ost-chat-ro-${Math.random().toString(36).slice(2, 9)}`;

const canEdit = computed(() => tree.canEdit);
const comments = computed<CommentDTO[]>(() => tree.comments[props.nodeKey] ?? []);
const items = computed(() => groupThread(comments.value));
const ready = computed(() => canEdit.value && !busy.value && draft.value.trim().length > 0);

const exactTime = (iso: string | null | undefined) => (iso ? new Date(iso).toLocaleString() : '');

// ---- scrolling -----------------------------------------------------------------------------------
function scrollToEnd(smooth = false) {
  const el = scroller.value;
  if (!el) return;
  if (smooth && typeof el.scrollTo === 'function') el.scrollTo({ top: el.scrollHeight, behavior: 'smooth' });
  else el.scrollTop = el.scrollHeight;
  atBottom.value = true;
}

function onScroll() {
  const el = scroller.value;
  if (!el) return;
  const bottom = isNearBottom(el);
  if (jumping) {
    if (!bottom) return;
    clearTimeout(jumping);
    jumping = null;
  }
  atBottom.value = bottom;
}

function jumpToLatest() {
  if (jumping) clearTimeout(jumping);
  jumping = setTimeout(() => {
    jumping = null;
    if (scroller.value) atBottom.value = isNearBottom(scroller.value);
  }, 800);
  scrollToEnd(true);
}

// New messages: follow them when the view is at the bottom or the message is our own.
watch(
  () => comments.value.length,
  async (now, before) => {
    if (now <= before) return;
    const last = comments.value.at(-1);
    if (atBottom.value || last?.mine) {
      await nextTick();
      scrollToEnd(false);
    }
  },
);

// ---- loading -------------------------------------------------------------------------------------
async function load() {
  const seq = ++loadSeq;
  loading.value = true;
  const before = tree.error;
  await tree.loadComments(props.nodeKey);
  if (seq !== loadSeq) return;
  loading.value = false;
  if (tree.error && tree.error !== before) takeError('The conversation could not be loaded.');
  await nextTick();
  scrollToEnd(false);
}

watch(
  () => props.nodeKey,
  () => {
    cancelEdit(false);
    draft.value = '';
    error.value = null;
    atBottom.value = true;
    void load();
  },
);

// The modal focuses [data-autofocus] itself; panel and page threads leave focus alone.
onMounted(() => void load());

onBeforeUnmount(() => {
  if (jumping) clearTimeout(jumping);
});

// ---- composing / editing -------------------------------------------------------------------------
function takeError(fallback: string) {
  error.value = tree.error ?? fallback;
  tree.clearError();
}

function autosize() {
  const el = input.value;
  if (!el) return;
  el.style.height = 'auto';
  el.style.height = `${Math.min(el.scrollHeight + 2, 132)}px`;
}

async function focusInput() {
  await nextTick();
  autosize();
  input.value?.focus();
}

function startEdit(comment: CommentDTO) {
  // Not while a send / save / delete is in flight: its completion would clear the new edit's draft.
  if (!canEdit.value || !comment.mine || busy.value) return;
  editingId.value = comment.id;
  draft.value = comment.body;
  error.value = null;
  void focusInput();
}

function cancelEdit(refocus: boolean) {
  if (editingId.value === null) return;
  editingId.value = null;
  draft.value = '';
  if (refocus) void focusInput();
  else void nextTick(autosize);
}

async function send() {
  const text = draft.value.trim();
  if (!canEdit.value || busy.value || !text) return;
  error.value = null;
  busy.value = true;
  try {
    const id = editingId.value;
    if (id !== null) {
      const original = comments.value.find(c => c.id === id);
      if (!original) {
        cancelEdit(true);
        return;
      }
      if (original.body === text) {
        cancelEdit(true);
        return;
      }
      if (await tree.editComment(props.nodeKey, id, text)) {
        // Leave the composer alone if the user moved on (cancelled, or typed more) meanwhile.
        if (editingId.value === id) {
          editingId.value = null;
          if (draft.value.trim() === text) draft.value = '';
        }
      } else {
        takeError('The message could not be edited.');
      }
    } else {
      atBottom.value = true;
      // Clear the composer only if it still holds what was sent (the user may have typed on) and
      // has not switched to editing a message meanwhile.
      if (await tree.addComment(props.nodeKey, text)) {
        if (editingId.value === null && draft.value.trim() === text) draft.value = '';
      } else takeError('The message could not be sent.');
    }
  } finally {
    busy.value = false;
    void focusInput();
  }
}

async function remove(id: number) {
  if (!canEdit.value || busy.value) return;
  if (editingId.value === id) cancelEdit(false);
  error.value = null;
  busy.value = true;
  try {
    if (!(await tree.deleteComment(props.nodeKey, id))) takeError('The message could not be deleted.');
  } finally {
    busy.value = false;
  }
}

function onKey(event: KeyboardEvent) {
  if (event.key === 'Enter' && !event.shiftKey && !event.isComposing) {
    event.preventDefault();
    void send();
  } else if (event.key === 'Escape' && editingId.value !== null) {
    // Escape leaves edit mode first; a second Escape reaches the dialog (closes the modal).
    event.preventDefault();
    event.stopPropagation();
    cancelEdit(true);
  }
}

defineExpose({ scrollToEnd, focusInput });
</script>

<style scoped>
.ost-chat {
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.ost-chat--modal,
.ost-chat--page {
  flex: 1 1 auto;
}

.ost-chat__viewport {
  position: relative;
  display: flex;
  flex: 1 1 auto;
  min-height: 0;
}

.ost-chat__scroll {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.ost-chat--panel .ost-chat__scroll {
  max-height: max(220px, calc(100vh - 430px));
  padding: 0 2px 4px;
}

.ost-chat--modal .ost-chat__scroll {
  padding: 14px 14px 18px;
}

.ost-chat--page .ost-chat__scroll {
  padding: 4px 2px 8px;
}

.ost-chat__scroll:focus-visible {
  outline: 1px solid var(--color-accent-600);
  outline-offset: 2px;
}

.ost-chat__empty {
  margin: 8px 0;
  font-size: 12px;
  color: var(--color-neutral-500);
}

.ost-chat__item {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  max-width: 100%;
}

.ost-chat__item.is-mine {
  align-items: flex-end;
}

.ost-chat__stamp {
  align-self: center;
  font-size: 10px;
  color: var(--color-neutral-500);
  padding: 9px 0 5px;
}

.ost-chat--modal .ost-chat__stamp {
  padding: 10px 0 6px;
}

.ost-chat__who {
  font-size: 10px;
  color: var(--color-neutral-500);
  padding: 0 10px 3px;
}

.ost-chat__bubble {
  max-width: 78%;
  padding: 8px 12px;
  font-size: 13px;
  line-height: 1.35;
  text-wrap: pretty;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  border-radius: 17px 17px 17px 5px;
  background: var(--color-neutral-900);
  color: var(--color-text);
  border: 1px solid var(--color-neutral-800);
}

.ost-chat__item.is-mine .ost-chat__bubble {
  border-radius: 17px 17px 5px 17px;
  background: var(--color-accent-600);
  color: var(--color-neutral-100);
  border-color: var(--color-accent-500);
}

.ost-chat__bubble.is-editing {
  outline: 1px dashed var(--color-accent-300);
  outline-offset: 2px;
}

.ost-chat__edited {
  opacity: 0.75;
}

.ost-chat__own {
  display: flex;
  gap: 10px;
  padding: 3px 6px 0;
}

.ost-chat__own-btn {
  font: inherit;
  font-size: 10.5px;
  background: none;
  border: 0;
  padding: 0;
  cursor: pointer;
  color: var(--color-neutral-400);
}

.ost-chat__own-btn:hover {
  color: var(--color-accent-300);
}

.ost-chat__own-btn:disabled {
  cursor: default;
  opacity: 0.5;
}

.ost-chat__jump {
  position: absolute;
  right: 12px;
  bottom: 10px;
  width: 30px;
  height: 30px;
  display: grid;
  place-items: center;
  padding: 0;
  border-radius: 50%;
  background: var(--color-neutral-900);
  border: 1px solid var(--color-neutral-700);
  color: var(--color-accent-300);
  cursor: pointer;
  box-shadow: var(--shadow-md);
}

.ost-chat__jump:hover {
  border-color: var(--color-accent-600);
}

.ost-chat__error {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-top: 8px;
  padding: 7px 8px 7px 10px;
  font-size: 12px;
  line-height: 1.4;
  color: var(--color-neutral-200);
  background: var(--color-neutral-900);
  border: 1px solid var(--color-neutral-600);
  border-radius: var(--radius-md);
}

.ost-chat--modal .ost-chat__error {
  margin: 8px 12px 0;
}

.ost-chat__error span {
  flex: 1;
}

.ost-chat__error-close {
  display: grid;
  place-items: center;
  width: 18px;
  height: 18px;
  padding: 0;
  background: none;
  border: 0;
  color: var(--color-neutral-400);
  cursor: pointer;
}

.ost-chat__composer {
  display: flex;
  flex-direction: column;
  gap: 7px;
  margin-top: 10px;
}

.ost-chat--modal .ost-chat__composer {
  margin-top: 0;
  padding: 10px 12px 12px;
  border-top: 1px solid var(--color-neutral-800);
}

.ost-chat__editing {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 11px;
  color: var(--color-neutral-400);
}

.ost-chat__editing .ost-chat__own-btn {
  font-size: 11px;
}

.ost-chat__editing-pill {
  padding: 2px 8px;
  border-radius: 999px;
  background: var(--color-accent-900);
  color: var(--color-accent-200);
}

.ost-chat__row {
  display: flex;
  align-items: flex-end;
  gap: 7px;
}

.ost-chat--modal .ost-chat__row {
  gap: 8px;
}

.ost-chat .ost-chat__input {
  flex: 1;
  min-height: 34px;
  max-height: 132px;
  padding: 7px 13px;
  font-size: 13px;
  line-height: 1.35;
  border-radius: 17px;
  background: transparent;
  resize: none;
  overflow-y: auto;
}

.ost-chat--modal .ost-chat__input {
  padding: 8px 14px;
  background: var(--color-bg);
}

.ost-chat .ost-chat__input:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.ost-chat__send {
  width: 32px;
  height: 32px;
  flex: none;
  display: grid;
  place-items: center;
  padding: 0;
  border-radius: 50%;
  cursor: not-allowed;
  border: 1px solid var(--color-neutral-800);
  background: transparent;
  color: var(--color-neutral-500);
}

.ost-chat__send.is-ready {
  cursor: pointer;
  border-color: var(--color-accent-500);
  background: var(--color-accent-600);
  color: var(--color-neutral-100);
}

.ost-chat__readonly {
  margin: 0;
  font-size: 10.5px;
  line-height: 1.4;
  color: var(--color-neutral-400);
}
</style>
