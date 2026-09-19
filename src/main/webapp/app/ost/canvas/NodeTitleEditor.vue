<template>
  <div class="ost-rename nodrag nopan">
    <input
      ref="input"
      v-model="draft"
      class="ost-input ost-rename__input"
      type="text"
      :aria-label="`Rename ${label}`"
      :aria-invalid="!!message"
      :aria-describedby="message ? messageId : undefined"
      :maxlength="max + 50"
      data-cy="ost-rename-input"
      @keydown="onKeydown"
      @blur="onBlur"
      @input="message = null"
      @pointerdown.stop
      @dblclick.stop
      @click.stop
    />
    <div v-if="message" :id="messageId" class="ost-rename__message" role="alert" data-cy="ost-rename-error">{{ message }}</div>
  </div>
</template>

<script setup lang="ts">
/**
 * Inline title editor on a canvas node (prototype: the `.input` that replaces the title while
 * `editing`). Enter commits, Escape cancels, blur commits — a blur with an invalid title cancels
 * instead (there is nowhere left to show the message). Invalid titles on Enter stay in the field
 * with inline feedback. Emits once: commit(title) or cancel.
 */
import { onBeforeUnmount, onMounted, ref } from 'vue';

import type { NodeType } from '../domain/types';

import { titleMax, validateTitle } from './edit-rules';

const props = withDefaults(defineProps<{ type: NodeType; label: string; value: string; error?: string | null }>(), { error: null });
const emit = defineEmits<{ commit: [title: string]; cancel: [] }>();

const input = ref<HTMLInputElement | null>(null);
const draft = ref(props.value);
const message = ref<string | null>(props.error);
const max = titleMax(props.type);
const messageId = `ost-rename-msg-${Math.random().toString(36).slice(2, 9)}`;
let done = false;
let frame = 0;

function finish(kind: 'commit' | 'cancel') {
  if (done) return;
  if (kind === 'cancel') {
    done = true;
    emit('cancel');
    return;
  }
  const check = validateTitle(props.type, draft.value);
  if (!check.ok) {
    message.value = check.message;
    return;
  }
  done = true;
  emit('commit', check.title);
}

function onKeydown(event: KeyboardEvent) {
  // Keep the canvas, palette and page shortcuts (Delete, Escape, ...) out of the field.
  event.stopPropagation();
  if (event.key === 'Enter') {
    event.preventDefault();
    finish('commit');
  } else if (event.key === 'Escape') {
    event.preventDefault();
    finish('cancel');
  }
}

function onBlur() {
  if (done) return;
  if (validateTitle(props.type, draft.value).ok) finish('commit');
  else finish('cancel');
}

/**
 * Focus + select on mount. A node Vue Flow has only just added is `visibility: hidden` until it is
 * measured, and a hidden input cannot take focus — retry for a few frames.
 */
function focusSoon(tries = 12) {
  const el = input.value;
  if (!el || done) return;
  el.focus({ preventScroll: true });
  if (document.activeElement === el) {
    el.select();
    return;
  }
  if (tries > 0) frame = requestAnimationFrame(() => focusSoon(tries - 1));
}

onMounted(() => focusSoon());
onBeforeUnmount(() => cancelAnimationFrame(frame));

defineExpose({ finish });
</script>

<style scoped>
.ost-rename {
  margin-top: 6px;
  font-style: normal;
}

.ost-rename__input {
  min-height: 30px;
  padding: 4px 8px;
  font-size: 13px;
  background: var(--color-bg);
  user-select: text;
}

.ost-rename__message {
  margin-top: 4px;
  font-size: 10.5px;
  line-height: 1.3;
  color: var(--color-accent-200);
}
</style>
