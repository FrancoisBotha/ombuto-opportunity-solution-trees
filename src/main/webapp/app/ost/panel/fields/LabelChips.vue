<template>
  <div class="ost-labels" data-cy="ost-labels">
    <div class="ost-field__label ost-field__label--7">Labels ({{ tags.length }})</div>

    <ul v-if="tags.length" class="ost-labels__list" data-cy="ost-labels-list">
      <li v-for="tag in tags" :key="tag.id" class="ost-labels__chip" :data-cy="`ost-label-chip-${tag.id}`">
        <span class="ost-labels__chip-name">{{ tag.name }}</span>
        <button
          v-if="!readonly"
          type="button"
          class="ost-labels__chip-remove"
          :data-cy="`ost-label-remove-${tag.id}`"
          :aria-label="`Remove label ${tag.name}`"
          @click="removeTag(tag)"
        >
          ×
        </button>
      </li>
    </ul>

    <div v-if="!readonly" class="ost-labels__input-row">
      <input
        v-model="draft"
        type="text"
        class="ost-labels__input"
        data-cy="ost-label-input"
        placeholder="Add label…"
        maxlength="50"
        @keydown.enter.prevent="commitDraft"
        @focus="loadSuggestions"
      />
      <button type="button" class="ost-btn ost-btn--quick" data-cy="ost-label-add" :disabled="!draft.trim() || busy" @click="commitDraft">
        Add
      </button>
    </div>

    <ul v-if="!readonly && filteredSuggestions.length" class="ost-labels__suggest" data-cy="ost-label-suggest">
      <li
        v-for="s in filteredSuggestions"
        :key="`sug-${s.id}`"
        class="ost-labels__suggest-item"
        :data-cy="`ost-label-suggest-${s.id}`"
        @click="applySuggestion(s)"
      >
        {{ s.name }}
      </li>
    </ul>
  </div>
</template>

<script setup lang="ts">
/**
 * LABEL-001: inline label editor for OPPORTUNITY and SOLUTION nodes.
 *
 * - Renders every applied label as a chip, with a per-chip remove button for editors.
 * - The input creates a label inline on Enter, or picks one from the suggestions dropdown.
 * - Suggestions are loaded lazily from the team-scoped label API and ordered current-team first,
 *   then the caller's other teams (server-side ordering — this component preserves it).
 *
 * Every write goes through the tree store's {@code applyLabels} action, so the panel-action
 * error handling and realtime echo work the same way as they do for other node field changes.
 */
import { computed, ref } from 'vue';

import type { TagRef } from '../../domain/types';
import { useOstTreeStore } from '../../stores/ost-tree.store';
import { usePanelAction } from '../panel-action';

const props = defineProps<{ nodeKey: string; tags: TagRef[]; readonly: boolean; teamId: number | null }>();

const tree = useOstTreeStore();
const { run } = usePanelAction();

const draft = ref('');
const suggestions = ref<TagRef[]>([]);
const busy = ref(false);

const filteredSuggestions = computed(() => {
  const applied = new Set(props.tags.map(t => t.id));
  const q = draft.value.trim().toLowerCase();
  return suggestions.value.filter(s => !applied.has(s.id) && (q === '' || s.name.toLowerCase().includes(q))).slice(0, 8);
});

async function loadSuggestions() {
  if (props.teamId == null || suggestions.value.length) return;
  suggestions.value = await tree.listLabelSuggestions(props.teamId);
}

async function commitDraft() {
  const name = draft.value.trim();
  if (!name || busy.value || props.teamId == null) return;
  busy.value = true;
  try {
    // Prefer an existing suggestion with the same normalized name — path of least resistance.
    const existing = suggestions.value.find(s => s.name.trim().toLowerCase() === name.toLowerCase());
    const tag = existing ?? (await tree.createLabel(props.teamId, name));
    if (!tag) return;
    if (!suggestions.value.some(s => s.id === tag.id)) suggestions.value = [tag, ...suggestions.value];
    await addToNode(tag);
    draft.value = '';
  } finally {
    busy.value = false;
  }
}

async function applySuggestion(tag: TagRef) {
  if (props.readonly) return;
  await addToNode(tag);
}

async function addToNode(tag: TagRef) {
  if (props.tags.some(t => t.id === tag.id)) return;
  const next = [...props.tags, tag];
  await run(() => tree.applyLabels(props.nodeKey, next));
}

async function removeTag(tag: TagRef) {
  const next = props.tags.filter(t => t.id !== tag.id);
  await run(() => tree.applyLabels(props.nodeKey, next));
}
</script>

<style scoped>
.ost-labels {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.ost-labels__list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-wrap: wrap;
  gap: 4px 6px;
}

.ost-labels__chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  padding: 2px 6px;
  border: 1px solid var(--color-divider);
  border-radius: 12px;
  background: var(--color-accent-900);
}

.ost-labels__chip-remove {
  background: transparent;
  border: 0;
  padding: 0 2px;
  cursor: pointer;
  font-size: 14px;
  line-height: 1;
  color: inherit;
}

.ost-labels__input-row {
  display: flex;
  gap: 6px;
  align-items: center;
}

.ost-labels__input {
  flex: 1;
  min-width: 0;
  font: inherit;
  padding: 4px 6px;
  border: 1px solid var(--color-divider);
  background: transparent;
  color: inherit;
}

.ost-labels__suggest {
  list-style: none;
  padding: 0;
  margin: 0;
  border: 1px solid var(--color-divider);
  max-height: 160px;
  overflow-y: auto;
}

.ost-labels__suggest-item {
  padding: 4px 6px;
  cursor: pointer;
  font-size: 12px;
}

.ost-labels__suggest-item:hover {
  background: var(--color-accent-900);
}
</style>
