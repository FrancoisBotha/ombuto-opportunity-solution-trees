<template>
  <div class="ost-legend" data-cy="ost-legend">
    <span v-for="item in ITEMS" :key="item.type" class="ost-legend__item">
      <i class="ost-legend__swatch" :class="`ost-legend__swatch--${item.type}`" aria-hidden="true"></i>{{ item.label }}
    </span>
    <span class="ost-legend__hint" data-cy="ost-legend-hint">{{ hint }}</span>
  </div>
</template>

<script setup lang="ts">
/** Canvas legend (bottom-left): node frames by type, plus the interaction hint. */
import { computed } from 'vue';

import { TYPE_BOX } from '../domain/rules';
import type { NodeType } from '../domain/types';

const props = defineProps<{ canEdit: boolean }>();

const ITEMS = (['outcome', 'opportunity', 'solution', 'assumption', 'evidence'] as NodeType[]).map(type => ({
  type,
  label: TYPE_BOX[type].label,
}));

/**
 * Only the nodes in view are rendered (and so reachable with Tab); the search box's Enter is the
 * keyboard way to every other node, so the hint names it for editors and viewers alike.
 */
const hint = computed(() =>
  props.canEdit
    ? 'Drag a node onto another to re-parent · double-click a title to rename · search + Enter jumps to a node'
    : 'View only · drag the canvas to pan, scroll to zoom · search + Enter jumps to a node',
);
</script>

<style scoped>
.ost-legend {
  position: absolute;
  left: 14px;
  bottom: 12px;
  right: 224px;
  z-index: 5;
  display: flex;
  flex-wrap: wrap;
  gap: 6px 14px;
  align-items: center;
  padding: 7px 11px;
  background: color-mix(in srgb, var(--color-bg) 86%, transparent);
  border-radius: var(--radius-md);
  border: 1px solid var(--color-divider);
  font-family: var(--font-heading);
  font-size: 10px;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  pointer-events: auto;
}

.ost-legend__item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.ost-legend__swatch {
  width: 14px;
  height: 10px;
  border-radius: 3px;
  border: 1px solid var(--color-neutral-800);
}
.ost-legend__swatch--outcome {
  background: var(--color-accent-900);
  border-color: var(--color-accent-600);
}
.ost-legend__swatch--opportunity {
  background: var(--color-surface);
  border-color: var(--color-accent-500);
}
.ost-legend__swatch--solution {
  background: var(--color-neutral-900);
  border-color: var(--color-neutral-700);
}
.ost-legend__swatch--assumption {
  border: 1px dashed var(--color-accent-500);
}
.ost-legend__swatch--evidence {
  border-color: var(--color-neutral-700);
}

.ost-legend__hint {
  font-family: var(--font-body);
  font-size: 10.5px;
  letter-spacing: 0;
  text-transform: none;
  color: var(--color-neutral-400);
}
</style>
