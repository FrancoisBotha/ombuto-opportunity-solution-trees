<template>
  <div class="ost-toolbar" data-cy="ost-canvas-toolbar">
    <CanvasProductCombo :products="tree.products" :product-id="ui.productId" :counts="productCounts" @select="emit('product', $event)" />

    <label class="ost-toolbar__search">
      <span class="ost-sr-only">Search nodes</span>
      <PhMagnifyingGlass :size="14" class="ost-toolbar__search-icon" aria-hidden="true" />
      <input
        class="ost-input ost-toolbar__search-input"
        type="search"
        placeholder="Search nodes"
        autocomplete="off"
        :value="ui.query"
        :aria-describedby="jumpHintId"
        data-cy="ost-search"
        @input="ui.setQuery(($event.target as HTMLInputElement).value)"
        @keydown.esc="ui.setQuery('')"
        @keydown.enter="onEnter"
      />
    </label>
    <!-- Outside the label: a description (aria-describedby), not part of the field's name. -->
    <span :id="jumpHintId" class="ost-sr-only">Enter selects the next match on the canvas, Shift+Enter the previous one.</span>
    <span class="ost-sr-only" role="status" data-cy="ost-search-status">{{ jumpStatus }}</span>

    <div class="ost-toolbar__branches" role="group" aria-label="Expand or collapse branches">
      <button
        type="button"
        class="ost-btn ost-toolbar__branch-btn"
        data-cy="ost-collapse-all"
        title="Collapse all branches in the current product view"
        :disabled="!branchKeys.some(key => !ui.collapsed[key])"
        @click="setAllCollapsed(true)"
      >
        Collapse all
      </button>
      <button
        type="button"
        class="ost-btn ost-toolbar__branch-btn"
        data-cy="ost-expand-all"
        title="Expand all branches in the current product view"
        :disabled="!branchKeys.some(key => ui.collapsed[key])"
        @click="setAllCollapsed(false)"
      >
        Expand all
      </button>
    </div>

    <div class="ost-toolbar__chips" role="group" aria-label="Show node types">
      <button
        v-for="chip in chips"
        :key="chip.type"
        type="button"
        class="ost-toolbar__chip ost-hit"
        :class="{ 'is-on': chip.on }"
        :aria-pressed="chip.on"
        :data-cy="`ost-filter-${chip.type}`"
        @click="ui.toggleType(chip.type)"
      >
        {{ chip.label }}<span class="ost-toolbar__chip-count">{{ chip.count }}</span>
      </button>
    </div>

    <div class="ost-toolbar__zoom">
      <ConnectionIndicator />
      <button
        type="button"
        class="ost-btn ost-toolbar__zoom-btn"
        title="Zoom out"
        aria-label="Zoom out"
        data-cy="ost-zoom-out"
        @click="emit('zoomOut')"
      >
        −
      </button>
      <div class="ost-toolbar__zoom-level" data-cy="ost-zoom-level">{{ Math.round(zoom * 100) }}%</div>
      <button
        type="button"
        class="ost-btn ost-toolbar__zoom-btn"
        title="Zoom in"
        aria-label="Zoom in"
        data-cy="ost-zoom-in"
        @click="emit('zoomIn')"
      >
        +
      </button>
      <button type="button" class="ost-btn ost-toolbar__fit" title="Fit the branch to the canvas" data-cy="ost-fit" @click="emit('fit')">
        Fit
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * Canvas toolbar row: product scope · search · type-filter chips · connection state · zoom −/%/+ · Fit.
 *
 * The search box is also the keyboard way to any node (NFR-3): the canvas only renders the nodes in
 * view, so Tab cannot reach the rest. Enter emits `jump` with the next match (Shift+Enter the
 * previous; it wraps) — in tree order, among the types shown and the product(s) in scope — and the
 * page selects and centres it. A status line announces "Match 2 of 5: <title>".
 */
import { PhMagnifyingGlass } from '@phosphor-icons/vue';
import { computed, nextTick, ref, watch } from 'vue';

import { countByType } from '../domain/derive';
import { TYPE_BOX } from '../domain/rules';
import type { NodeType } from '../domain/types';
import { useOstTreeStore } from '../stores/ost-tree.store';
import { useOstUiStore } from '../stores/ost-ui.store';

import CanvasProductCombo from './CanvasProductCombo.vue';
import ConnectionIndicator from './ConnectionIndicator.vue';
import { searchMatches } from './canvas-model';

defineProps<{ zoom: number }>();
const emit = defineEmits<{ product: [productId: string | 'all']; zoomIn: []; zoomOut: []; fit: []; jump: [key: string] }>();

const tree = useOstTreeStore();
const ui = useOstUiStore();

const CHIP_TYPES: NodeType[] = ['outcome', 'opportunity', 'solution', 'assumption', 'evidence'];

const productCounts = computed(() => Object.fromEntries(tree.products.map(p => [p.id, tree.descendantCount(p.id)])));

/** Include hidden descendants, but leave products outside the current scope untouched. */
const branchKeys = computed(() => {
  const roots = new Set(tree.roots.map(root => root.id));
  const parents = new Set(tree.nodes.map(node => node.parent).filter(Boolean));
  return tree.nodes.filter(node => parents.has(node.id) && roots.has(tree.ancestors(node.id)[0]?.id ?? node.id)).map(node => node.id);
});

async function setAllCollapsed(collapsed: boolean) {
  ui.setCollapsedMany(branchKeys.value, collapsed);
  await nextTick();
  emit('fit');
}

// ---- keyboard jump to a match -------------------------------------------------------------------
const jumpHintId = `ost-search-hint-${Math.random().toString(36).slice(2, 9)}`;
const cursor = ref(-1);
const jumpStatus = ref('');

watch(
  () => [ui.query, ui.productId, ui.hiddenTypes] as const,
  () => {
    cursor.value = -1;
    jumpStatus.value = '';
  },
);

/** Enter / Shift+Enter jump — but not the Enter that confirms an IME composition. */
function onEnter(event: KeyboardEvent) {
  if (event.isComposing) return;
  event.preventDefault();
  jump(event.shiftKey ? -1 : 1);
}

function jump(step: 1 | -1) {
  const inScope = new Set(tree.roots.map(r => r.id));
  const rootOf = (key: string) => tree.ancestors(key)[0]?.id ?? key;
  const matches = searchMatches(tree.nodes, ui.query, ui.hiddenTypes, n => inScope.has(rootOf(n.id)));
  if (!matches.length) {
    cursor.value = -1;
    jumpStatus.value = ui.query.trim() ? 'No matching nodes on the canvas' : '';
    return;
  }
  // Continue from the selected node when it is a match (the user may have clicked another match
  // since the last jump), else from the last match jumped to.
  const selected = matches.indexOf(ui.selectedId ?? '');
  const from = selected >= 0 ? selected : cursor.value;
  const next = from < 0 ? (step > 0 ? 0 : matches.length - 1) : (from + step + matches.length) % matches.length;
  cursor.value = next;
  const key = matches[next];
  jumpStatus.value = `Match ${next + 1} of ${matches.length}: ${tree.byId(key)?.title ?? ''}`;
  emit('jump', key);
}

/**
 * The chips dim what is on the canvas, so their counts describe the canvas: the product in scope,
 * not the whole team tree (FR-C8 with FR-N2). The same scope the search jump uses.
 */
const chips = computed(() => {
  const inScope = new Set(tree.roots.map(r => r.id));
  const rootOf = (key: string) => tree.ancestors(key)[0]?.id ?? key;
  const counts = countByType(tree.nodes.filter(n => inScope.has(rootOf(n.id))));
  return CHIP_TYPES.map(type => ({ type, label: TYPE_BOX[type].label, count: counts[type], on: !ui.hiddenTypes[type] }));
});
</script>

<style scoped>
.ost-toolbar {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 9px 16px;
  flex: none;
  flex-wrap: wrap;
  border-bottom: 1px solid var(--color-divider);
}

.ost-toolbar__search {
  position: relative;
  display: flex;
  align-items: center;
  margin: 0;
}

.ost-toolbar__search-icon {
  position: absolute;
  left: 9px;
  opacity: 0.55;
  pointer-events: none;
}

.ost-root .ost-toolbar__search-input {
  width: 208px;
  min-height: 32px;
  padding: 4px 10px 4px 28px;
  font-size: 13px;
  background: transparent;
}

.ost-toolbar__branches {
  display: flex;
  gap: 4px;
}

.ost-root .ost-toolbar__branch-btn {
  min-height: 32px;
  padding: 4px 8px;
  font-size: 12px;
  white-space: nowrap;
}

.ost-toolbar__chips {
  display: flex;
  gap: 5px;
  flex-wrap: wrap;
}

.ost-toolbar__chip {
  font: inherit;
  font-family: var(--font-heading);
  font-size: 11px;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  padding: 5px 9px;
  cursor: pointer;
  border-radius: var(--radius-sm);
  border: 1px solid var(--color-neutral-800);
  background: transparent;
  color: var(--color-neutral-400);
}
.ost-toolbar__chip.is-on {
  border-color: var(--color-accent-600);
  background: var(--color-accent-900);
  color: var(--color-accent-200);
}

.ost-toolbar__chip-count {
  opacity: 0.55;
  margin-left: 6px;
}

.ost-toolbar__zoom {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 6px;
}

/* The connection state leads the zoom group (epic §7: "next to the zoom controls"). */
.ost-toolbar__zoom > .ost-connection {
  margin-right: 6px;
}

.ost-root .ost-toolbar__zoom-btn {
  width: 30px;
  height: 30px;
  padding: 0;
  font-size: 15px;
}

.ost-toolbar__zoom-level {
  font-size: 12px;
  width: 44px;
  text-align: center;
  letter-spacing: 0.04em;
}

.ost-root .ost-toolbar__fit {
  height: 30px;
  font-size: 12px;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}
</style>
