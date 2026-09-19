<template>
  <div ref="list" class="ost-tabs" role="tablist" aria-label="Node details" @keydown="onKey">
    <button
      v-for="tab in tabs"
      :id="tabDomId(tab.id)"
      :key="tab.id"
      type="button"
      role="tab"
      class="ost-tabs__tab"
      :class="{ 'is-active': tab.id === active }"
      :aria-selected="tab.id === active"
      :aria-controls="tab.id === active ? tabPanelDomId(tab.id) : undefined"
      :tabindex="tab.id === active ? 0 : -1"
      :data-tab="tab.id"
      :data-cy="`ost-tab-${tab.id}`"
      @click="emit('select', tab.id)"
    >
      {{ tab.label }}
      <span v-if="tab.badge > 0" class="ost-tabs__badge" :data-cy="`ost-tab-badge-${tab.id}`">{{ tab.badge }}</span>
    </button>
  </div>
</template>

<script setup lang="ts">
/**
 * Detail-panel tab bar. The set adapts to the node type (Product: Detail + Links; Open Qs on
 * opportunities only); badges are derived: link count, message count, open-question count.
 * WAI-ARIA tabs: roving tabindex, Left/Right (wrapping), Home/End move focus and select;
 * the active tab controls the panel's tabpanel (DetailPanel).
 */
import { computed, nextTick, ref } from 'vue';

import { type PanelTab, panelTabsFor } from '../domain/derive';
import type { OstNode } from '../domain/types';

import { tabDomId, tabPanelDomId } from './panel-format';

const props = defineProps<{ node: OstNode; active: PanelTab }>();
const emit = defineEmits<{ select: [tab: PanelTab] }>();

const LABELS: Record<PanelTab, string> = { detail: 'Detail', links: 'Links', chat: 'Chat', questions: 'Open Qs', history: 'History' };

const tabs = computed(() =>
  panelTabsFor(props.node.type).map(id => ({
    id,
    label: LABELS[id],
    badge:
      id === 'links'
        ? props.node.links.length
        : id === 'chat'
          ? props.node.commentCount
          : id === 'questions'
            ? props.node.questions.filter(q => !q.done).length
            : 0,
  })),
);

const list = ref<HTMLElement | null>(null);

async function onKey(event: KeyboardEvent) {
  const ids = tabs.value.map(t => t.id);
  const current = ids.indexOf(props.active);
  let next: number | null = null;
  if (event.key === 'ArrowRight') next = (current + 1) % ids.length;
  else if (event.key === 'ArrowLeft') next = (current - 1 + ids.length) % ids.length;
  else if (event.key === 'Home') next = 0;
  else if (event.key === 'End') next = ids.length - 1;
  if (next === null || !ids.length) return;
  event.preventDefault();
  emit('select', ids[next]);
  await nextTick();
  list.value?.querySelector<HTMLElement>(`[data-tab="${ids[next]}"]`)?.focus();
}
</script>

<style scoped>
.ost-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 2px 1px;
  padding: 6px 8px 0;
  border-bottom: 1px solid var(--color-divider);
}

.ost-tabs__tab {
  position: relative;
  display: flex;
  align-items: center;
  gap: 5px;
  font: inherit;
  font-size: 12px;
  padding: 7px 6px 8px;
  cursor: pointer;
  white-space: nowrap;
  background: transparent;
  border: 0;
  border-bottom: 2px solid transparent;
  color: var(--color-neutral-400);
  font-weight: 400;
}

.ost-tabs__tab.is-active {
  border-bottom-color: var(--color-accent);
  color: var(--color-text);
  font-weight: 500;
}

.ost-tabs__badge {
  font-size: 9.5px;
  padding: 1px 5px;
  border-radius: 999px;
  background: var(--color-neutral-900);
  color: var(--color-neutral-400);
  font-weight: 400;
}

.ost-tabs__tab.is-active .ost-tabs__badge {
  background: var(--color-accent-800);
  color: var(--color-accent-200);
}
</style>
