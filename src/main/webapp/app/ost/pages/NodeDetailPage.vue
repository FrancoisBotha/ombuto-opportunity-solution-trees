<template>
  <div ref="pageEl" class="ost-page ost-nd" data-cy="ostNodeDetailPage">
    <article v-if="node" class="ost-nd__inner" data-cy="ost-node-detail" :data-node-key="node.id" :data-type="node.type">
      <div class="ost-nd__bar">
        <router-link
          class="ost-btn ost-btn--ghost ost-nd__bar-btn"
          :to="canvasRoute"
          data-cy="ost-node-detail-open-canvas"
          :data-from-canvas="ui.detailFromCanvas ? 'true' : 'false'"
        >
          <template v-if="ui.detailFromCanvas">← Back to canvas</template>
          <template v-else>
            Open on canvas
            <PhArrowUpRight :size="13" aria-hidden="true" />
          </template>
        </router-link>
        <button
          v-if="!readonly"
          type="button"
          class="ost-btn ost-nd__bar-btn ost-nd__delete"
          data-cy="ost-node-detail-delete"
          @click="askDelete"
        >
          <PhTrash :size="13" aria-hidden="true" />
          Delete
        </button>
      </div>

      <header class="ost-nd__head">
        <div class="ost-nd__heading">
          <div class="ost-nd__kicker" data-cy="ost-node-detail-kicker">{{ typeLabel }}</div>
          <NodeDetailTitle :key="node.id" ref="titleRef" :node="node" :readonly="readonly" />
          <nav class="ost-nd__crumbs" aria-label="Ancestors" data-cy="ost-node-detail-breadcrumb">
            <template v-for="(crumb, i) in crumbs" :key="crumb.id">
              <span v-if="i > 0" class="ost-nd__sep" aria-hidden="true">›</span>
              <router-link class="ost-nd__crumb ost-hit" :to="detailRoute(crumb.id)" :data-cy="`ost-node-detail-crumb-${crumb.id}`">
                {{ crumb.title }}
              </router-link>
            </template>
            <span v-if="!crumbs.length">Top of the tree</span>
          </nav>
        </div>
        <div v-if="node.status || tags.length" class="ost-nd__tags">
          <span
            v-if="node.status"
            class="ost-badge ost-nd__status"
            :class="`ost-badge--${statusTone(node.status)}`"
            data-cy="ost-node-detail-status"
            >{{ node.status }}</span
          >
          <span v-for="tag in tags" :key="tag.id" class="ost-nd__tag" :title="tag.title" :data-cy="`ost-node-detail-metric-${tag.id}`">
            <i v-if="tag.dot" class="ost-nd__dot" :style="{ background: tag.dot }" aria-hidden="true"></i>{{ tag.label }}
          </span>
        </div>
      </header>

      <div v-if="shownError" class="ost-nd__error" role="alert" data-cy="ost-node-detail-error">
        <span>{{ shownError }}</span>
        <button type="button" class="ost-nd__error-close ost-tap" aria-label="Dismiss" @click="errors.report(null)">
          <PhX :size="11" aria-hidden="true" />
        </button>
      </div>

      <div class="ost-nd__grid" :class="{ 'is-single': !hasChat }">
        <div class="ost-nd__main">
          <section v-if="hasSignals" class="ost-nd__signals" aria-label="Status and signals">
            <NodeFields :key="`signals:${node.id}`" :node-key="node.id" :readonly="readonly" variant="page" />
          </section>

          <div class="ost-nd__notes" data-cy="ost-node-detail-notes">
            <NotesField :key="`notes:${node.id}`" :value="node.note" :readonly="readonly" @change="save({ note: $event })" />
          </div>

          <NodeDetailChildren :node-key="node.id" :team-id="teamId" :readonly="readonly" @created="onCreated" />

          <section v-if="node.type === 'opportunity'" aria-labelledby="ost-nd-questions-label" data-cy="ost-node-detail-questions">
            <h2 id="ost-nd-questions-label" class="ost-nd-section">Open questions</h2>
            <OpenQuestionsTab :key="`questions:${node.id}`" :node-key="node.id" />
          </section>

          <section v-if="!hasChat" aria-labelledby="ost-nd-links-label" data-cy="ost-node-detail-links">
            <h2 id="ost-nd-links-label" class="ost-nd-section">Links</h2>
            <LinksTab :key="`links:${node.id}`" :node-key="node.id" />
          </section>
        </div>

        <aside v-if="hasChat" class="ost-nd__side">
          <section class="ost-nd__discussion" aria-labelledby="ost-nd-chat-label" data-cy="ost-node-detail-chat">
            <h2 id="ost-nd-chat-label" class="ost-nd-section">Discussion</h2>
            <div class="ost-nd__chat">
              <ChatThread :node-key="node.id" variant="page" />
            </div>
          </section>
          <section aria-labelledby="ost-nd-links-label" data-cy="ost-node-detail-links">
            <h2 id="ost-nd-links-label" class="ost-nd-section">Links</h2>
            <LinksTab :key="`links:${node.id}`" :node-key="node.id" />
          </section>
        </aside>
      </div>
    </article>

    <div v-else-if="deleting" class="ost-nd__leaving" aria-live="polite">Deleted. Leaving this page…</div>

    <div v-else class="ost-state" data-cy="ost-node-detail-missing">
      <h1 class="ost-state__title">Node not found</h1>
      <p class="ost-state__text">It may have been deleted, or it belongs to another team.</p>
      <router-link
        class="ost-btn ost-btn--primary"
        :to="{ name: 'OstCanvas', params: { teamId } }"
        data-cy="ost-node-detail-missing-canvas"
      >
        Open the tree canvas
      </router-link>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * Full-page node detail (/trees/:teamId/nodes/:nodeKey) — the full-width version of the panel
 * (prototype view "detail"): open-on-canvas / delete bar; type kicker, editable title, ancestor
 * breadcrumb (each crumb opens that node's page), status + metric tags; the type's fields, notes,
 * children as cards with quick-add, open questions (opportunity); a discussion column with the
 * node's chat and its links. Products have no chat. Viewers get everything read-only.
 *
 * The node on the page is also the selected node (ui.selectedId), so "Open on canvas" and the
 * browser's back button land on it, and panel-style errors (panel-action.ts) are shown here.
 * Selecting it never opens the canvas's detail panel (the panel keeps whatever state it had).
 * The bar's canvas link reads "← Back to canvas" when the page was opened from the canvas
 * (prototype), otherwise "Open on canvas" ↗.
 */
import { computed, nextTick, ref, watch } from 'vue';
import { onBeforeRouteLeave, onBeforeRouteUpdate, useRoute, useRouter } from 'vue-router';

import { PhArrowUpRight, PhTrash, PhX } from '@phosphor-icons/vue';

import ChatThread from '../chat/ChatThread.vue';
import { statusTone } from '../domain/derive';
import type { NodePatch } from '../domain/mapping';
import { TYPE_BOX } from '../domain/rules';
import NodeDetailChildren from '../node-detail/NodeDetailChildren.vue';
import NodeDetailTitle from '../node-detail/NodeDetailTitle.vue';
import { metricTags } from '../node-detail/node-detail-format';
import NodeFields from '../panel/fields/NodeFields.vue';
import NotesField from '../panel/fields/NotesField.vue';
import { providePanelErrors, usePanelAction } from '../panel/panel-action';
import LinksTab from '../panel/tabs/LinksTab.vue';
import OpenQuestionsTab from '../panel/tabs/OpenQuestionsTab.vue';
import { useOstTreeStore } from '../stores/ost-tree.store';
import { useOstUiStore } from '../stores/ost-ui.store';

const route = useRoute();
const router = useRouter();
const tree = useOstTreeStore();
const ui = useOstUiStore();
const errors = providePanelErrors();
const { run } = usePanelAction(errors);

const pageEl = ref<HTMLElement | null>(null);
const titleRef = ref<InstanceType<typeof NodeDetailTitle> | null>(null);

const teamId = computed(() => String(route.params.teamId ?? ''));
const nodeKey = computed(() => String(route.params.nodeKey ?? ''));
const node = computed(() => tree.byId(nodeKey.value));
const readonly = computed(() => !tree.canEdit);
const typeLabel = computed(() => (node.value ? TYPE_BOX[node.value.type].label : ''));
const crumbs = computed(() => (node.value ? tree.ancestors(node.value.id) : []));
const tags = computed(() => (node.value ? metricTags(node.value, tree.nodes) : []));
const hasSignals = computed(() => !!node.value && ['opportunity', 'solution', 'assumption'].includes(node.value.type));
/** Every node except a product has a thread (FR-M1). */
const hasChat = computed(() => !!node.value && node.value.type !== 'product');
const shownError = computed(() => {
  const key = errors.nodeKey.value;
  return errors.message.value && (key === null || key === node.value?.id) ? errors.message.value : null;
});

const detailRoute = (key: string) => ({ name: 'OstNodeDetail', params: { teamId: teamId.value, nodeKey: key } });
/**
 * Back to the canvas, on the node AND in the product scope the user left it in: the canvas reads
 * its scope from `?product=` and falls back to "All products" when it is absent (FR-N2 / US-13).
 */
const canvasRoute = computed(() => ({
  name: 'OstCanvas',
  params: { teamId: teamId.value },
  query: { node: nodeKey.value, ...(ui.productId !== 'all' ? { product: ui.productId } : {}) },
}));

// ---- the page's node is the selection -----------------------------------------------------------
watch(
  nodeKey,
  key => {
    errors.report(null);
    if (tree.byId(key) && ui.selectedId !== key) ui.select(key, { openPanel: false });
  },
  { immediate: true },
);

/**
 * Leaving the node (another node's page, another view): blur the focused field first, so a title
 * or note being typed commits to the node it was typed for — before the route (and so `node`)
 * changes underneath it.
 */
function commitFocused() {
  const active = document.activeElement;
  if (active instanceof HTMLElement && pageEl.value?.contains(active)) active.blur();
}
onBeforeRouteUpdate(commitFocused);
onBeforeRouteLeave(to => {
  commitFocused();
  // Another node's page keeps the way back to the canvas; anywhere else forgets it.
  if (to.name !== 'OstNodeDetail') ui.setDetailFromCanvas(false);
});

// ---- edits --------------------------------------------------------------------------------------
function save(patch: NodePatch) {
  const current = node.value;
  if (readonly.value || !current) return;
  void run(() => tree.patchNode(current.id, patch));
}

/** Quick-add: the new child's page opens with its title selected for renaming. */
const focusTitleFor = ref<string | null>(null);

async function onCreated(key: string) {
  ui.stopEditing();
  focusTitleFor.value = key;
  await router.push(detailRoute(key));
}

watch(
  () => node.value?.id,
  async key => {
    if (!key || key !== focusTitleFor.value) return;
    focusTitleFor.value = null;
    await nextTick();
    titleRef.value?.focusAndSelect();
  },
  { flush: 'post' },
);

// ---- delete: confirm, then go to the parent's page (or the canvas for a product) ---------------
const deleting = ref<{ key: string; parent: string | null } | null>(null);

function askDelete() {
  const current = node.value;
  if (readonly.value || !current) return;
  deleting.value = { key: current.id, parent: current.parent };
  ui.askDelete(current.id);
}

watch(
  () => [ui.confirmId, node.value] as const,
  () => {
    const pending = deleting.value;
    if (!pending) return;
    if (!tree.byId(pending.key)) {
      const parent = pending.parent ? tree.byId(pending.parent) : undefined;
      void router.replace(parent ? detailRoute(parent.id) : { name: 'OstCanvas', params: { teamId: teamId.value } }).finally(() => {
        deleting.value = null;
      });
    } else if (ui.confirmId !== pending.key) {
      deleting.value = null; // cancelled or failed
    }
  },
);
</script>

<style scoped>
/*
 * Responsive to the page's own width (container queries), like the dashboard: with the app sidebar
 * expanded a 390px phone leaves the page ~160px wide.
 */
.ost-page {
  height: 100%;
  overflow: auto;
  container: ost-node-detail / inline-size;
}

.ost-nd__inner {
  max-width: 1000px;
  margin: 0 auto;
  padding: 28px 40px 60px;
}

.ost-nd__bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.ost-root .ost-nd__bar-btn {
  height: 28px;
  font-size: 12px;
  letter-spacing: 0.06em;
}

.ost-root .ost-nd__delete {
  margin-left: auto;
}

.ost-nd__head {
  display: flex;
  align-items: flex-start;
  gap: 18px;
  margin-top: 6px;
  padding: 12px 0 16px;
  border-bottom: 1px solid var(--color-divider);
}

.ost-nd__heading {
  flex: 1;
  min-width: 0;
}

.ost-nd__kicker {
  font-family: var(--font-heading);
  font-size: 10px;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--color-accent-300);
}

.ost-nd__crumbs {
  margin-top: 8px;
  font-size: 12px;
  color: color-mix(in srgb, var(--color-text) 60%, transparent);
  text-wrap: pretty;
  overflow-wrap: anywhere;
}

.ost-root .ost-nd__crumb {
  color: inherit;
}

.ost-root .ost-nd__crumb:hover {
  color: var(--color-accent-300);
  text-decoration: underline;
}

.ost-nd__sep {
  margin: 0 6px;
}

/* Touch: each crumb has a 48px hit area (.ost-hit). Crumbs wrap whole with rows 48px apart, clear
   of the title above, so no two targets overlap. */
@media (pointer: coarse) {
  .ost-nd__crumbs {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    row-gap: 32px;
    margin-top: 18px;
  }

  .ost-root .ost-nd__crumb {
    min-width: 0;
  }
}

.ost-nd__tags {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 6px;
  align-items: center;
  flex: none;
  max-width: 45%;
}

.ost-root .ost-nd__status {
  font-family: var(--font-heading);
  font-size: 12px;
  padding: 4px 10px;
}

.ost-nd__tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 3px 10px;
  font-size: 11px;
  letter-spacing: 0.02em;
  white-space: nowrap;
  background: var(--color-neutral-800);
  color: var(--color-neutral-100);
  border-radius: calc(var(--radius-md) * 0.75);
}

.ost-nd__dot {
  width: 7px;
  height: 7px;
  flex: none;
  border-radius: 50%;
}

.ost-nd__error {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-top: 14px;
  padding: 7px 8px 7px 10px;
  font-size: 12px;
  line-height: 1.4;
  color: var(--color-neutral-200);
  background: var(--color-neutral-900);
  border: 1px solid var(--color-neutral-600);
  border-radius: var(--radius-md);
}

.ost-nd__error span {
  flex: 1;
  min-width: 0;
}

.ost-nd__error-close {
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

.ost-nd__grid {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) minmax(0, 1fr);
  gap: 34px;
  margin-top: 24px;
}

.ost-nd__grid.is-single {
  grid-template-columns: minmax(0, 1fr);
}

.ost-nd__main,
.ost-nd__side {
  display: flex;
  flex-direction: column;
  gap: 26px;
  min-width: 0;
}

.ost-nd__side {
  gap: 24px;
}

/* Notes: the prototype's page sizes (11px label, 14px text, 110px tall). */
.ost-nd__notes :deep(.ost-field__label) {
  font-size: 11px;
  margin-bottom: 8px;
}

.ost-nd__notes :deep(.ost-notes) {
  font-size: 14px;
  min-height: 110px;
}

.ost-nd__chat {
  display: flex;
  flex-direction: column;
  height: clamp(300px, 60vh, 600px);
  min-height: 0;
}

.ost-nd__leaving {
  padding: 32px;
  color: var(--color-neutral-400);
}

/* One column: the discussion goes below the main column. */
@container ost-node-detail (max-width: 760px) {
  .ost-nd__inner {
    padding: 24px 16px 48px;
  }

  .ost-nd__head {
    flex-wrap: wrap;
    gap: 10px;
  }

  .ost-nd__tags {
    flex: 1 1 100%;
    min-width: 0;
    max-width: none;
    justify-content: flex-start;
  }

  .ost-nd__grid {
    grid-template-columns: minmax(0, 1fr);
    gap: 28px;
  }

  .ost-nd__chat {
    height: clamp(280px, 55vh, 480px);
  }
}

/* Phone with the app sidebar open (~160px): smaller type, tags and buttons may wrap. */
@container ost-node-detail (max-width: 360px) {
  .ost-nd__inner {
    padding: 18px 10px 40px;
  }

  .ost-nd__head :deep(.ost-nd-title) {
    font-size: 22px;
  }

  .ost-nd__tag,
  .ost-root .ost-nd__status {
    white-space: normal;
    overflow-wrap: anywhere;
  }

  .ost-root .ost-nd__bar-btn {
    padding: 0 6px;
  }

  .ost-root .ost-nd__delete {
    margin-left: 0;
  }

  /* The panel's $-scale needs ~200px in one row; here its steps wrap instead. */
  .ost-nd__signals :deep(.ost-value) {
    flex-wrap: wrap;
  }

  .ost-nd__signals :deep(.ost-value__step) {
    flex: 1 0 auto;
    padding: 0 6px;
  }
}
</style>
