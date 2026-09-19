<template>
  <div v-if="node" class="ost-tab ost-questions" data-cy="ostTab-questions">
    <p class="ost-questions__hint" data-cy="ost-questions-hint">
      What do we still need to learn about this opportunity? Delivery work belongs in Jira, under Links.
    </p>

    <ul v-if="node.questions.length" class="ost-questions__list" aria-label="Open questions">
      <li
        v-for="q in node.questions"
        :key="q.id ?? q.text"
        class="ost-questions__item"
        :data-cy="`ost-question-${q.id}`"
        :data-done="q.done"
      >
        <button
          type="button"
          role="checkbox"
          class="ost-questions__box ost-tap"
          :class="{ 'is-done': q.done }"
          :aria-checked="q.done"
          :aria-label="q.text"
          :disabled="readonly"
          :data-cy="`ost-question-toggle-${q.id}`"
          @click="toggle(q)"
        >
          <PhCheck v-if="q.done" :size="10" aria-hidden="true" />
        </button>
        <span class="ost-questions__text" :class="{ 'is-done': q.done }">{{ q.text }}</span>
        <button
          v-if="!readonly"
          type="button"
          class="ost-questions__remove ost-tap"
          title="Remove"
          :aria-label="`Remove question: ${q.text}`"
          :data-cy="`ost-question-remove-${q.id}`"
          @click="remove(q)"
        >
          <PhX :size="11" aria-hidden="true" />
        </button>
      </li>
    </ul>
    <p v-else class="ost-questions__empty" data-cy="ost-questions-empty">No open questions yet.</p>

    <input
      v-if="!readonly"
      v-model="draft"
      class="ost-input ost-questions__input"
      aria-label="New open question"
      placeholder="Add an open question, then Enter"
      :maxlength="TEXT_MAX"
      :disabled="adding"
      data-cy="ost-question-add"
      @keydown.enter.prevent="add"
      @keydown.esc.prevent="draft = ''"
    />

    <div class="ost-questions__summary" data-cy="ost-questions-summary">{{ openCount }} open · {{ answeredCount }} answered</div>
  </div>
</template>

<script setup lang="ts">
/**
 * Open Qs tab (opportunities only, FR-Q1/Q2): a checklist of discovery questions — add (Enter),
 * tick / untick, remove — with the "n open · n answered" summary. The tab badge (PanelTabs) counts
 * the open ones. The copy steers delivery work to Jira. Viewers see the list read-only.
 */
import { computed, nextTick, ref } from 'vue';

import { PhCheck, PhX } from '@phosphor-icons/vue';

import type { Question } from '../../domain/types';
import { useOstTreeStore } from '../../stores/ost-tree.store';
import { usePanelAction } from '../panel-action';

const props = defineProps<{ nodeKey: string }>();
const tree = useOstTreeStore();
const { run } = usePanelAction();

/** Server limit (TreeOpenQuestionService.TEXT_MAX). */
const TEXT_MAX = 500;

const node = computed(() => {
  const n = tree.byId(props.nodeKey);
  return n?.type === 'opportunity' ? n : undefined;
});
const readonly = computed(() => !tree.canEdit);
const openCount = computed(() => node.value?.questions.filter(q => !q.done).length ?? 0);
const answeredCount = computed(() => node.value?.questions.filter(q => q.done).length ?? 0);

const draft = ref('');
const adding = ref(false);

async function add(event: KeyboardEvent) {
  const text = draft.value.trim();
  if (readonly.value || adding.value || !text || event.isComposing) return;
  adding.value = true;
  const ok = await run(() => tree.addQuestion(props.nodeKey, text), 'The question could not be added.');
  adding.value = false;
  if (ok) draft.value = '';
  // re-enabled input: give focus back so questions can be added one after another
  await nextTick();
  (event.target as HTMLInputElement | null)?.focus();
}

function toggle(q: Question) {
  if (readonly.value || q.id === undefined) return;
  const id = q.id;
  void run(() => tree.updateQuestion(props.nodeKey, id, { done: !q.done }), 'The question could not be saved.');
}

function remove(q: Question) {
  if (readonly.value || q.id === undefined) return;
  const id = q.id;
  void run(() => tree.removeQuestion(props.nodeKey, id), 'The question could not be removed.');
}
</script>

<style scoped>
.ost-questions {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.ost-questions__hint {
  margin: 0;
  font-size: 10.5px;
  line-height: 1.4;
  color: var(--color-neutral-400);
}

.ost-questions__list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.ost-questions__item {
  display: flex;
  align-items: flex-start;
  gap: 9px;
}

.ost-questions__box {
  width: 16px;
  height: 16px;
  flex: none;
  margin-top: 1px;
  display: grid;
  place-items: center;
  padding: 0;
  font: inherit;
  line-height: 1;
  border-radius: 4px;
  cursor: pointer;
  background: transparent;
  border: 1px solid var(--color-neutral-600);
  color: var(--color-neutral-100);
}

.ost-questions__box.is-done {
  background: var(--color-accent-600);
  border-color: var(--color-accent-500);
}

.ost-questions__box:disabled {
  cursor: default;
}

.ost-questions__text {
  font-size: 13px;
  line-height: 1.35;
  text-wrap: pretty;
  overflow-wrap: anywhere;
  color: var(--color-text);
}

.ost-questions__text.is-done {
  color: var(--color-neutral-500);
  text-decoration: line-through;
}

.ost-questions__remove {
  margin-left: auto;
  flex: none;
  width: 20px;
  height: 20px;
  display: grid;
  place-items: center;
  padding: 0;
  font: inherit;
  background: none;
  border: 0;
  cursor: pointer;
  color: var(--color-neutral-600);
}

.ost-questions__remove:hover {
  color: var(--color-accent-300);
}

.ost-questions__empty {
  margin: 0;
  font-size: 12px;
  color: var(--color-neutral-500);
}

.ost-questions .ost-questions__input {
  font-size: 13px;
  background: transparent;
}

.ost-questions__summary {
  font-size: 10.5px;
  color: var(--color-neutral-500);
}
</style>
