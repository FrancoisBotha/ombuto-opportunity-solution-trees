<template>
  <aside v-if="node && ui.rightOpen" class="ost-panel" data-cy="ost-panel" :aria-label="`${typeLabel} details`">
    <header class="ost-panel__header">
      <div class="ost-panel__top">
        <div class="ost-panel__where">
          <div class="ost-panel__kicker" data-cy="ost-panel-kicker">{{ typeLabel }}</div>
          <nav class="ost-panel__crumbs" aria-label="Ancestors" data-cy="ost-breadcrumb">
            <template v-for="(crumb, i) in crumbs" :key="crumb.id">
              <span v-if="i > 0" class="ost-panel__sep" aria-hidden="true">›</span>
              <button type="button" class="ost-panel__crumb ost-hit" :data-cy="`ost-breadcrumb-${crumb.id}`" @click="go(crumb.id)">
                {{ crumb.title }}
              </button>
            </template>
            <span v-if="!crumbs.length">Top of the tree</span>
          </nav>
        </div>
        <button
          ref="hideButton"
          type="button"
          class="ost-panel__hide ost-tap"
          title="Hide panel"
          aria-label="Hide panel"
          data-cy="ost-panel-hide"
          @click="hide"
        >
          <PhX :size="14" aria-hidden="true" />
        </button>
      </div>
      <input
        v-model="title"
        class="ost-input ost-panel__title"
        :aria-label="`${typeLabel} title`"
        :readonly="!tree.canEdit"
        :maxlength="titleMax"
        data-cy="ost-panel-title"
        @focus="onTitleFocus"
        @input="titleDirty = true"
        @keydown.enter.prevent="blurTarget"
        @keydown.esc.prevent="cancelTitle"
        @blur="commitTitle"
      />
    </header>

    <PanelTabs :node="node" :active="activeTab" @select="ui.setPanelTab($event)" />

    <div class="ost-panel__body">
      <div v-if="shownError" class="ost-panel__error" role="alert" data-cy="ost-panel-error">
        <span>{{ shownError }}</span>
        <button type="button" class="ost-panel__error-close ost-tap" aria-label="Dismiss" @click="errors.report(null)">
          <PhX :size="11" aria-hidden="true" />
        </button>
      </div>

      <div :id="tabPanelDomId(activeTab)" class="ost-panel__tabpanel" role="tabpanel" :aria-labelledby="tabDomId(activeTab)">
        <component :is="TAB_COMPONENTS[activeTab]" :key="`${activeTab}:${node.id}`" :node-key="node.id" />
      </div>

      <div class="ost-panel__footer">
        <router-link
          class="ost-btn ost-btn--primary ost-panel__action ost-panel__open"
          :to="{ name: 'OstNodeDetail', params: { teamId: String(tree.teamId ?? ''), nodeKey: node.id } }"
          data-cy="ost-open-detail"
        >
          Open detail
        </router-link>
        <button v-if="tree.canEdit" type="button" class="ost-btn ost-panel__action" data-cy="ost-delete" @click="ui.askDelete(node.id)">
          Delete
        </button>
      </div>
    </div>
  </aside>

  <div v-else-if="node" class="ost-panel-reopen">
    <button
      ref="reopenButton"
      type="button"
      class="ost-panel-reopen__btn ost-tap"
      title="Show details"
      data-cy="ost-panel-reopen"
      @click="reopen"
    >
      <PhCaretLeft :size="13" aria-hidden="true" />
      Details
    </button>
  </div>
</template>

<script setup lang="ts">
/**
 * Right-hand detail panel (346px): type kicker, clickable ancestor breadcrumb, editable title,
 * hide button; tabs by type (PanelTabs); Open detail + Delete. Hiding keeps the selection and
 * leaves a "Details" tab on the canvas edge; a node click reopens it too (ui.select).
 * Viewers (canEdit false) get everything read-only.
 */
import { computed, nextTick, ref, watch } from 'vue';

import { PhCaretLeft, PhX } from '@phosphor-icons/vue';

import { titleMax as titleMaxFor } from '../canvas/edit-rules';
import { type PanelTab, panelTabsFor } from '../domain/derive';
import { TYPE_BOX } from '../domain/rules';
import { useOstTreeStore } from '../stores/ost-tree.store';
import { useOstUiStore } from '../stores/ost-ui.store';

import PanelTabs from './PanelTabs.vue';
import { providePanelErrors, usePanelAction } from './panel-action';
import { tabDomId, tabPanelDomId } from './panel-format';
import ChatTab from './tabs/ChatTab.vue';
import DetailTab from './tabs/DetailTab.vue';
import HistoryTab from './tabs/HistoryTab.vue';
import LinksTab from './tabs/LinksTab.vue';
import OpenQuestionsTab from './tabs/OpenQuestionsTab.vue';

const TAB_COMPONENTS = { detail: DetailTab, links: LinksTab, chat: ChatTab, questions: OpenQuestionsTab, history: HistoryTab };

const tree = useOstTreeStore();
const ui = useOstUiStore();
const errors = providePanelErrors();
const { run } = usePanelAction(errors);

const node = computed(() => tree.selected);
const typeLabel = computed(() => (node.value ? TYPE_BOX[node.value.type].label : ''));
const crumbs = computed(() => (node.value ? tree.ancestors(node.value.id) : []));
/** A tab the node's type does not have falls back to Detail (the stored tab is kept). */
const activeTab = computed<PanelTab>(() => {
  const tabs = node.value ? panelTabsFor(node.value.type) : [];
  return tabs.includes(ui.panelTab) ? ui.panelTab : 'detail';
});
/** Server limits per type: products 100, assumptions + evidence 500, the rest 200. */
const titleMax = computed(() => (node.value ? titleMaxFor(node.value.type) : 200));
/** The error slot only shows messages about the node on screen. */
const shownError = computed(() => {
  const key = errors.nodeKey.value;
  return errors.message.value && (key === null || key === node.value?.id) ? errors.message.value : null;
});
const hideButton = ref<HTMLButtonElement | null>(null);
const reopenButton = ref<HTMLButtonElement | null>(null);

// ---- title: commit on Enter / blur, Escape cancels -----------------------------------------------
const title = ref(node.value?.title ?? '');
/** The user has typed in the field since it was focused (a focused but untouched field follows the store). */
const titleDirty = ref(false);

watch(
  () => [node.value?.id, node.value?.title] as const,
  ([id, value], old) => {
    // A new node always resets the draft; the same node unless the user has typed an edit of their own
    // (e.g. a canvas rename committed while focus was moving into this field must show up here).
    if (id !== old?.[0]) titleDirty.value = false;
    if (!titleDirty.value) title.value = value ?? '';
  },
);

/** Focus starts from the stored title unless an edit is already typed. */
function onTitleFocus() {
  if (!titleDirty.value) title.value = node.value?.title ?? '';
}

// A new selection starts with a clean error slot.
watch(
  () => node.value?.id,
  () => errors.report(null),
);

const blurTarget = (event: Event) => (event.target as HTMLElement).blur();

/** Escape reverts the typed title; focus stays in the field (a later blur has nothing to commit). */
function cancelTitle(event: KeyboardEvent) {
  titleDirty.value = false;
  title.value = node.value?.title ?? '';
  (event.target as HTMLInputElement).select();
}

function commitTitle() {
  titleDirty.value = false;
  const current = node.value;
  if (!current || !tree.canEdit) return;
  const next = title.value.trim();
  if (next.length < 2) {
    title.value = current.title;
    if (next.length) errors.report('Titles need at least 2 characters.', current.id);
    return;
  }
  if (next === current.title) {
    title.value = current.title;
    return;
  }
  const key = current.id;
  void run(() => tree.patchNode(key, { title: next }));
}

// ---- navigation / visibility ------------------------------------------------------------------
function go(key: string) {
  ui.select(key);
  ui.requestCentre(key);
}

/** Hiding moves focus to the "Details" edge tab, reopening from it back to the hide button. */
async function hide() {
  ui.setRightOpen(false);
  await nextTick();
  reopenButton.value?.focus();
}

async function reopen() {
  ui.setRightOpen(true);
  await nextTick();
  hideButton.value?.focus();
}
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
  padding: 14px 16px 12px;
  border-bottom: 1px solid var(--color-divider);
}

.ost-panel__top {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.ost-panel__where {
  flex: 1;
  min-width: 0;
}

.ost-panel__kicker {
  font-family: var(--font-heading);
  font-size: 10px;
  letter-spacing: 0.09em;
  text-transform: uppercase;
  color: var(--color-accent-300);
}

.ost-panel__crumbs {
  margin-top: 3px;
  font-size: 11px;
  opacity: 0.6;
  text-wrap: pretty;
}

.ost-panel__crumb {
  display: inline;
  padding: 0;
  font: inherit;
  text-align: left;
  color: inherit;
  background: none;
  border: 0;
  cursor: pointer;
}

.ost-panel__crumb:hover {
  color: var(--color-accent-300);
  text-decoration: underline;
}

.ost-panel__sep {
  margin: 0 6px;
}

/* Touch: each crumb has a 48px hit area (.ost-hit). Crumbs wrap whole, rows 48px apart, and the
   title field starts below the last row's hit area, so no two targets overlap. */
@media (pointer: coarse) {
  .ost-panel__crumbs {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    row-gap: 34px;
    margin-bottom: 8px;
  }

  .ost-panel__crumb {
    min-width: 0;
    overflow-wrap: anywhere;
  }
}

.ost-panel__hide {
  display: grid;
  place-items: center;
  width: 26px;
  height: 26px;
  flex: none;
  padding: 0;
  font: inherit;
  color: var(--color-accent);
  background: transparent;
  border: 1px solid transparent;
  border-radius: var(--radius-md);
  cursor: pointer;
}

.ost-panel__hide:hover {
  background: color-mix(in srgb, var(--color-text) 7%, transparent);
}

.ost-panel__header .ost-panel__title {
  margin-top: 10px;
  min-height: 38px;
  font-family: var(--font-heading);
  font-weight: 600;
  font-size: 17px;
  background: transparent;
}

.ost-panel__body {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 14px 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.ost-panel__error {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 7px 8px 7px 10px;
  font-size: 12px;
  line-height: 1.4;
  color: var(--color-neutral-200);
  background: var(--color-neutral-900);
  border: 1px solid var(--color-neutral-600);
  border-radius: var(--radius-md);
}

.ost-panel__error span {
  flex: 1;
}

.ost-panel__error-close {
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

.ost-panel__footer {
  display: flex;
  gap: 6px;
  margin-top: auto;
  padding-top: 12px;
  border-top: 1px solid var(--color-divider);
}

.ost-panel__footer .ost-panel__action {
  height: 32px;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.ost-panel__footer .ost-panel__open {
  flex: 1;
}

.ost-panel-reopen {
  position: relative;
  width: 0;
  flex: none;
}

.ost-panel-reopen__btn {
  position: absolute;
  right: 0;
  top: 14px;
  z-index: 20;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 7px 10px;
  font: inherit;
  font-size: 11.5px;
  color: var(--color-accent-300);
  background: var(--color-surface);
  border: 1px solid var(--color-neutral-800);
  border-right: 0;
  border-radius: var(--radius-md) 0 0 var(--radius-md);
  box-shadow: var(--shadow-sm);
  cursor: pointer;
  white-space: nowrap;
}

.ost-panel-reopen__btn:hover {
  border-color: var(--color-accent-600);
}
</style>
