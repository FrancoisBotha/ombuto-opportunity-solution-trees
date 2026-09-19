<template>
  <nav class="ost-tabs" role="tablist" aria-label="Node details">
    <button
      v-for="tab in tabs"
      :key="tab.id"
      type="button"
      role="tab"
      class="ost-tabs__tab"
      :class="{ 'is-active': tab.id === active }"
      :aria-selected="tab.id === active"
      :data-cy="`ost-tab-${tab.id}`"
      @click="emit('select', tab.id)"
    >
      {{ tab.label }}
      <span v-if="tab.badge > 0" class="ost-tabs__badge" :data-cy="`ost-tab-badge-${tab.id}`">{{ tab.badge }}</span>
    </button>
  </nav>
</template>

<script setup lang="ts">
/**
 * Detail-panel tab bar. The set adapts to the node type (Product: Detail + Links; Open Qs on
 * opportunities only); badges are derived: link count, message count, open-question count.
 */
import { computed } from 'vue';

import { type PanelTab, panelTabsFor } from '../domain/derive';
import type { OstNode } from '../domain/types';

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
