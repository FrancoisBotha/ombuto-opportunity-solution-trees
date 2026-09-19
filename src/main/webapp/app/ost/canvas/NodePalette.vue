<template>
  <div v-if="!ui.leftOpen" class="ost-palette-rail" data-cy="ost-palette" data-state="closed">
    <button
      type="button"
      class="ost-palette-rail__open"
      title="Show node palette"
      aria-label="Show node palette"
      :aria-expanded="false"
      data-cy="ost-palette-toggle"
      @click="ui.setLeftOpen(true)"
    >
      <PhCaretRight :size="13" weight="bold" aria-hidden="true" />
    </button>
    <span class="ost-palette-rail__label" aria-hidden="true">Palette</span>
  </div>

  <aside v-else class="ost-palette" aria-label="Node palette" data-cy="ost-palette" data-state="open">
    <div class="ost-palette__head">
      <span class="ost-palette__title">Place a node</span>
      <button
        type="button"
        class="ost-palette__hide"
        title="Hide palette"
        aria-label="Hide palette"
        :aria-expanded="true"
        data-cy="ost-palette-toggle"
        @click="hide"
      >
        <PhCaretLeft :size="13" weight="bold" aria-hidden="true" />
      </button>
    </div>

    <button
      v-for="tool in tools"
      :key="tool.type"
      type="button"
      class="ost-palette__tool"
      :class="{ 'is-armed': ui.tool === tool.type }"
      :aria-pressed="ui.tool === tool.type"
      :aria-describedby="`ost-palette-hint-${tool.type}`"
      :data-cy="`ost-palette-${tool.type}`"
      @pointerdown="onPointerDown($event, tool.type)"
      @click="onClick(tool.type)"
    >
      <span class="ost-palette__swatch" :class="`ost-palette__swatch--${tool.type}`" aria-hidden="true"></span>
      <span class="ost-palette__text">
        <span class="ost-palette__label">{{ tool.label }}</span>
        <span :id="`ost-palette-hint-${tool.type}`" class="ost-palette__hint">{{ tool.hint }}</span>
      </span>
    </button>

    <div class="ost-palette__footer" aria-live="polite" data-cy="ost-palette-hint">{{ footer }}</div>

    <PaletteGhost
      v-if="ui.paletteDrag"
      :label="TYPE_BOX[ui.paletteDrag.type].label"
      :x="ui.paletteDrag.x"
      :y="ui.paletteDrag.y"
      :over="!!ui.dropTargetId"
    />
  </aside>
</template>

<script setup lang="ts">
/**
 * Left node palette (182px, collapses to a 34px rail) — prototype: `tools`, paletteDown, toolHint.
 *
 * Two ways to attach a new node:
 * - drag a type (5px threshold) onto a node: the ghost chip follows the cursor, legal targets are
 *   highlighted by the canvas, dropping on a legal node attaches, anywhere else does nothing;
 * - click a type to arm it (click it again, or Escape, to disarm), then click or press Enter on a
 *   highlighted node.
 * A drag always ends cleanly: pointerup, pointercancel, window blur, Escape and unmount all clear it.
 * `resolveTarget` (from the canvas) maps a client point to the legal node under it, or null.
 */
import { PhCaretLeft, PhCaretRight } from '@phosphor-icons/vue';
import { computed, onBeforeUnmount, watch } from 'vue';

import { PALETTE_HINT, TYPE_BOX } from '../domain/rules';
import type { NodeType } from '../domain/types';
import { useOstUiStore } from '../stores/ost-ui.store';

import PaletteGhost from './PaletteGhost.vue';
import { CREATABLE_TYPES, type ResolveTarget } from './edit-rules';

const props = defineProps<{ resolveTarget: ResolveTarget }>();
const emit = defineEmits<{ attach: [parentKey: string, type: NodeType] }>();

const ui = useOstUiStore();

/** Pointer travel (px, Manhattan) before a press on a type becomes a drag — prototype value. */
const DRAG_THRESHOLD = 5;
/** A click right after a drag ends is the drag's own click, not an arm. */
const CLICK_AFTER_DRAG_MS = 200;

const tools = CREATABLE_TYPES.map(type => ({ type, label: TYPE_BOX[type].label, hint: PALETTE_HINT[type] }));

const footer = computed(() =>
  ui.tool
    ? `Click a highlighted node to attach the new ${TYPE_BOX[ui.tool].label.toLowerCase()}.`
    : 'Drag a type onto the node it belongs under — or click to arm it, then click a node. Drag a node onto another to re-parent; double-click to rename.',
);

// ---- drag ------------------------------------------------------------------------------------------
let press: { type: NodeType; x: number; y: number; pointerId: number; moved: boolean } | null = null;
let dragEndedAt = 0;

function listen(on: boolean) {
  if (on) {
    window.addEventListener('pointermove', onPointerMove);
    window.addEventListener('pointerup', onPointerUp);
    window.addEventListener('pointercancel', abortDrag);
    window.addEventListener('blur', abortDrag);
    window.addEventListener('keydown', onDragKeydown, true);
  } else {
    window.removeEventListener('pointermove', onPointerMove);
    window.removeEventListener('pointerup', onPointerUp);
    window.removeEventListener('pointercancel', abortDrag);
    window.removeEventListener('blur', abortDrag);
    window.removeEventListener('keydown', onDragKeydown, true);
  }
}

function onPointerDown(event: PointerEvent, type: NodeType) {
  if (event.button !== 0) return;
  if (press) abortDrag(); // a press that never saw its pointerup must not wedge the palette
  // No text selection / native drag while dragging a type.
  event.preventDefault();
  press = { type, x: event.clientX, y: event.clientY, pointerId: event.pointerId, moved: false };
  listen(true);
}

function onPointerMove(event: PointerEvent) {
  if (!press || event.pointerId !== press.pointerId) return;
  if (!press.moved && Math.abs(event.clientX - press.x) + Math.abs(event.clientY - press.y) < DRAG_THRESHOLD) return;
  if (!press.moved) {
    press.moved = true;
    ui.startPaletteDrag(press.type, event.clientX, event.clientY);
  } else {
    ui.movePaletteDrag(event.clientX, event.clientY);
  }
  ui.setDropTarget(props.resolveTarget(press.type, event.clientX, event.clientY));
}

function onPointerUp(event: PointerEvent) {
  if (!press || event.pointerId !== press.pointerId) return;
  const { type, moved } = press;
  const target = moved ? props.resolveTarget(type, event.clientX, event.clientY) : null;
  finishDrag();
  if (!moved) return; // a plain click: the click handler arms the type
  dragEndedAt = Date.now();
  if (ui.tool) ui.armTool(null);
  if (target) emit('attach', target, type);
}

function onDragKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape' && press) {
    event.stopPropagation();
    abortDrag();
  }
}

function abortDrag() {
  if (press?.moved) dragEndedAt = Date.now();
  finishDrag();
}

function finishDrag() {
  press = null;
  listen(false);
  if (ui.paletteDrag || ui.dropTargetId) ui.endPaletteDrag();
}

// ---- click to arm ------------------------------------------------------------------------------
function onClick(type: NodeType) {
  if (Date.now() - dragEndedAt < CLICK_AFTER_DRAG_MS) return;
  ui.armTool(type);
}

function onArmedKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape' && ui.tool && !event.defaultPrevented) ui.armTool(null);
}
watch(
  () => !!ui.tool,
  armed => (armed ? window.addEventListener('keydown', onArmedKeydown) : window.removeEventListener('keydown', onArmedKeydown)),
  { immediate: true },
);

function hide() {
  abortDrag();
  if (ui.tool) ui.armTool(null);
  ui.setLeftOpen(false);
}

onBeforeUnmount(() => {
  abortDrag();
  window.removeEventListener('keydown', onArmedKeydown);
});
</script>

<style scoped>
.ost-palette-rail {
  width: 34px;
  flex: none;
  border-right: 1px solid var(--color-divider);
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 10px 0;
  gap: 10px;
}

.ost-palette-rail__open {
  width: 24px;
  height: 24px;
  display: grid;
  place-items: center;
  padding: 0;
  border-radius: var(--radius-sm);
  background: transparent;
  border: 1px solid var(--color-neutral-800);
  color: var(--color-accent-300);
  cursor: pointer;
}
.ost-palette-rail__open:hover {
  border-color: var(--color-accent-600);
}

.ost-palette-rail__label {
  writing-mode: vertical-rl;
  font-size: 10px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--color-neutral-500);
}

.ost-palette {
  width: 182px;
  flex: none;
  border-right: 1px solid var(--color-divider);
  padding: 12px 10px 10px;
  display: flex;
  flex-direction: column;
  gap: 5px;
  overflow: auto;
}

.ost-palette__head {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 0 2px 3px;
}

.ost-palette__title {
  font-size: 10px;
  letter-spacing: 0.09em;
  text-transform: uppercase;
  color: var(--color-neutral-400);
}

.ost-palette__hide {
  margin-left: auto;
  width: 20px;
  height: 20px;
  display: grid;
  place-items: center;
  padding: 0;
  border-radius: var(--radius-sm);
  background: none;
  border: 0;
  color: var(--color-neutral-500);
  cursor: pointer;
}
.ost-palette__hide:hover {
  color: var(--color-accent-300);
}

.ost-palette__tool {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  width: 100%;
  padding: 7px 8px;
  font: inherit;
  color: var(--color-text);
  cursor: pointer;
  text-align: left;
  border-radius: var(--radius-md);
  border: 1px solid var(--color-neutral-800);
  background: transparent;
  touch-action: none;
  user-select: none;
}
.ost-palette__tool:hover {
  border-color: var(--color-neutral-700);
}
.ost-palette__tool.is-armed {
  border-color: var(--color-accent-600);
  background: var(--color-accent-900);
}

.ost-palette__swatch {
  width: 13px;
  height: 13px;
  flex: none;
  margin-top: 2px;
  border-radius: 3px;
  background: transparent;
  border: 1px solid var(--color-accent-500);
}
.ost-palette__swatch--outcome {
  background: var(--color-accent-900);
}
.ost-palette__swatch--solution {
  background: var(--color-neutral-900);
  border-color: var(--color-neutral-700);
}
.ost-palette__swatch--assumption {
  border-style: dashed;
}
.ost-palette__swatch--evidence {
  border-color: var(--color-neutral-700);
}

.ost-palette__text {
  display: flex;
  flex-direction: column;
  gap: 1px;
  min-width: 0;
  text-align: left;
}

.ost-palette__label {
  font-size: 13px;
  font-weight: 500;
}

.ost-palette__hint {
  font-size: 10.5px;
  color: var(--color-neutral-400);
  line-height: 1.25;
}

.ost-palette__footer {
  margin-top: auto;
  font-size: 10.5px;
  color: var(--color-neutral-400);
  line-height: 1.4;
  padding: 10px 2px 0;
}
</style>
