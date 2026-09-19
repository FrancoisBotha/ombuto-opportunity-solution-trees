<template>
  <h1 v-if="readonly" class="ost-nd-title" data-cy="ost-node-detail-title">{{ node.title }}</h1>
  <h1 v-else class="ost-nd-title ost-nd-title--edit">
    <textarea
      ref="field"
      v-model="draft"
      class="ost-nd-title__input"
      rows="1"
      :aria-label="`${typeLabel} title`"
      :maxlength="max"
      data-cy="ost-node-detail-title"
      @focus="focused = true"
      @keydown.enter.prevent="blurField"
      @keydown.esc.prevent="cancel"
      @blur="commit"
      @input="autosize"
    ></textarea>
  </h1>
</template>

<script setup lang="ts">
/**
 * The page's title: plain heading for viewers; for editors a wrapping one-line field (Enter or
 * blur commits, Escape cancels). Mounted per node (keyed by the page), so a commit always goes to
 * the node the text was typed for. Failures and validation messages go to the page's error slot.
 */
import { computed, nextTick, onMounted, ref, watch } from 'vue';

import { titleMax, validateTitle } from '../canvas/edit-rules';
import { TYPE_BOX } from '../domain/rules';
import type { OstNode } from '../domain/types';
import { usePanelAction } from '../panel/panel-action';
import { useOstTreeStore } from '../stores/ost-tree.store';

const props = defineProps<{ node: OstNode; readonly: boolean }>();

const tree = useOstTreeStore();
const { run, errors } = usePanelAction();

const field = ref<HTMLTextAreaElement | null>(null);
const draft = ref(props.node.title);
const focused = ref(false);
let cancelling = false;

const typeLabel = computed(() => TYPE_BOX[props.node.type].label);
const max = computed(() => titleMax(props.node.type));

// Follow outside changes (server response, rollback) unless the user is typing.
watch(
  () => props.node.title,
  value => {
    if (!focused.value) draft.value = value;
  },
);
watch(draft, () => nextTick(autosize));

function autosize() {
  const el = field.value;
  if (!el) return;
  el.style.height = 'auto';
  el.style.height = `${el.scrollHeight}px`;
}

onMounted(autosize);

const blurField = () => field.value?.blur();

function cancel() {
  cancelling = true;
  draft.value = props.node.title;
  field.value?.blur();
  cancelling = false;
}

function commit() {
  focused.value = false;
  const current = props.node;
  if (cancelling || props.readonly || !tree.byId(current.id)) return;
  const next = draft.value.replace(/\s*\n\s*/g, ' ');
  if (next.trim() === current.title) {
    draft.value = current.title;
    return;
  }
  const check = validateTitle(current.type, next);
  if (!check.ok) {
    draft.value = current.title;
    errors.report(check.message, current.id);
    return;
  }
  void run(() => tree.patchNode(current.id, { title: check.title }));
}

/** Focuses the field with its text selected (a quick-added child lands here in rename mode). */
function focusAndSelect() {
  field.value?.focus();
  field.value?.select();
}

defineExpose({ focusAndSelect });
</script>

<style scoped>
.ost-root .ost-nd-title {
  margin: 7px 0 0;
  font-size: 32px;
  line-height: 1.1;
  font-weight: 500;
  text-wrap: pretty;
  overflow-wrap: anywhere;
}

/*
 * The field's padding + border (3px / 7px) sit outside the text column: the typed title lines up
 * with the kicker and breadcrumb exactly like the read-only heading does.
 */
.ost-nd-title__input {
  display: block;
  width: calc(100% + 14px);
  margin: -3px -7px;
  padding: 2px 6px;
  font: inherit;
  line-height: inherit;
  letter-spacing: inherit;
  color: var(--color-text);
  caret-color: var(--color-accent);
  background: transparent;
  border: 1px solid transparent;
  border-radius: var(--radius-md);
  resize: none;
  overflow: hidden;
  field-sizing: content;
  overflow-wrap: anywhere;
}

.ost-nd-title__input:hover {
  border-color: var(--color-divider);
}

.ost-nd-title__input:focus-visible,
.ost-nd-title__input:focus {
  border-color: var(--color-accent);
  outline: none;
}
</style>
