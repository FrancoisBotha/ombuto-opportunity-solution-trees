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
        data-cy="ost-search"
        @input="ui.setQuery(($event.target as HTMLInputElement).value)"
        @keydown.esc="ui.setQuery('')"
      />
    </label>

    <div class="ost-toolbar__chips" role="group" aria-label="Show node types">
      <button
        v-for="chip in chips"
        :key="chip.type"
        type="button"
        class="ost-toolbar__chip"
        :class="{ 'is-on': chip.on }"
        :aria-pressed="chip.on"
        :data-cy="`ost-filter-${chip.type}`"
        @click="ui.toggleType(chip.type)"
      >
        {{ chip.label }}<span class="ost-toolbar__chip-count">{{ chip.count }}</span>
      </button>
    </div>

    <div class="ost-toolbar__zoom">
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
/** Canvas toolbar row: product scope · search · type-filter chips · zoom −/%/+ · Fit. */
import { PhMagnifyingGlass } from '@phosphor-icons/vue';
import { computed } from 'vue';

import { countByType } from '../domain/derive';
import { TYPE_BOX } from '../domain/rules';
import type { NodeType } from '../domain/types';
import { useOstTreeStore } from '../stores/ost-tree.store';
import { useOstUiStore } from '../stores/ost-ui.store';

import CanvasProductCombo from './CanvasProductCombo.vue';

defineProps<{ zoom: number }>();
const emit = defineEmits<{ product: [productId: string | 'all']; zoomIn: []; zoomOut: []; fit: [] }>();

const tree = useOstTreeStore();
const ui = useOstUiStore();

const CHIP_TYPES: NodeType[] = ['outcome', 'opportunity', 'solution', 'assumption', 'evidence'];

const productCounts = computed(() => Object.fromEntries(tree.products.map(p => [p.id, tree.descendantCount(p.id)])));

const chips = computed(() => {
  const counts = countByType(tree.nodes);
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
