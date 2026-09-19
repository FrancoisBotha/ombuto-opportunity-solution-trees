<template>
  <aside v-if="node" class="ost-panel" data-cy="ostDetailPanel" :aria-label="`${typeLabel} details`">
    <!-- STUB (step 6): step 10 builds the header (kicker, breadcrumb, editable title, hide) and tab bar. -->
    <header class="ost-panel__header">
      <div class="ost-kicker">{{ typeLabel }}</div>
      <h2 class="ost-panel__title" data-cy="ostDetailPanelTitle">{{ node.title }}</h2>
    </header>
    <nav class="ost-panel__tabs" role="tablist">
      <button
        v-for="tab in tabs"
        :key="tab"
        type="button"
        role="tab"
        class="ost-panel__tab"
        :class="{ 'is-active': tab === activeTab }"
        :aria-selected="tab === activeTab"
        :data-cy="`ostPanelTab-${tab}`"
        @click="ui.setPanelTab(tab)"
      >
        {{ TAB_LABELS[tab] }}
      </button>
    </nav>
    <component :is="TAB_COMPONENTS[activeTab]" :node-key="node.id" />
  </aside>
</template>

<script setup lang="ts">
/** Right-hand detail panel (346px). Owned by step 10 (tabs: step 10 Detail/Links, step 11 Chat/Open Qs/History). */
import { computed } from 'vue';

import { type PanelTab, panelTabsFor } from '../domain/derive';
import { TYPE_BOX } from '../domain/rules';
import { useOstTreeStore } from '../stores/ost-tree.store';
import { useOstUiStore } from '../stores/ost-ui.store';

import ChatTab from './tabs/ChatTab.vue';
import DetailTab from './tabs/DetailTab.vue';
import HistoryTab from './tabs/HistoryTab.vue';
import LinksTab from './tabs/LinksTab.vue';
import OpenQuestionsTab from './tabs/OpenQuestionsTab.vue';

const TAB_LABELS: Record<PanelTab, string> = { detail: 'Detail', links: 'Links', chat: 'Chat', questions: 'Open Qs', history: 'History' };
const TAB_COMPONENTS = { detail: DetailTab, links: LinksTab, chat: ChatTab, questions: OpenQuestionsTab, history: HistoryTab };

const tree = useOstTreeStore();
const ui = useOstUiStore();

const node = computed(() => tree.selected);
const typeLabel = computed(() => (node.value ? TYPE_BOX[node.value.type].label : ''));
const tabs = computed(() => (node.value ? panelTabsFor(node.value.type) : []));
/** A tab the node's type does not have falls back to Detail (the stored tab is kept). */
const activeTab = computed<PanelTab>(() => (tabs.value.includes(ui.panelTab) ? ui.panelTab : 'detail'));
</script>

<style scoped>
.ost-panel {
  width: 346px;
  flex: none;
  display: flex;
  flex-direction: column;
  min-height: 0;
  border-left: 1px solid var(--color-divider);
  background: var(--color-bg);
}

.ost-panel__header {
  padding: 14px 16px 10px;
}

.ost-panel__title {
  font-size: 16px;
  font-weight: 500;
  margin-top: 4px;
}

.ost-panel__tabs {
  display: flex;
  gap: 1px;
  padding: 0 10px;
  border-bottom: 1px solid var(--color-divider);
}

.ost-panel__tab {
  font: inherit;
  font-size: 12px;
  padding: 7px 6px 8px;
  background: none;
  border: 0;
  border-bottom: 2px solid transparent;
  color: var(--color-neutral-400);
  cursor: pointer;
}

.ost-panel__tab.is-active {
  color: var(--color-text);
  font-weight: 500;
  border-bottom-color: var(--color-accent);
}
</style>
