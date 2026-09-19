<template>
  <div
    class="ost-minimap"
    role="group"
    aria-roledescription="overview map"
    aria-label="Overview map: click or drag to recentre the canvas, scroll to zoom"
    data-cy="ost-minimap"
    @pointerdown="onDown"
    @wheel.prevent.stop="onWheel"
  >
    <div class="ost-minimap__label">Overview</div>
    <i
      v-for="box in boxes"
      :key="box.id"
      class="ost-minimap__node"
      :class="[`ost-minimap__node--${box.type}`, { 'is-selected': box.id === selectedId }]"
      :style="box.style"
      :data-cy="`ost-minimap-node-${box.id}`"
    ></i>
    <i v-if="viewRect" class="ost-minimap__view" :style="viewRect" data-cy="ost-minimap-view"></i>
  </div>
</template>

<script setup lang="ts">
/**
 * Overview map (bottom-right, 198×134) — every laid-out node plus the current viewport rectangle.
 * Click or drag in it to recentre the canvas on that point (pannable); the wheel zooms the canvas
 * about its centre (zoomable). Drawn from the derived layout, so it never depends on Vue Flow
 * having measured off-screen nodes (only-render-visible-elements is on).
 */
import { computed, onBeforeUnmount } from 'vue';

import type { Placed } from '../domain/layout';
import type { OstNode } from '../domain/types';

import { minimapToFlow, minimapTransform } from './canvas-model';
import type { Point, Size, Viewport } from './useViewport';

const props = defineProps<{
  nodes: OstNode[];
  placed: Record<string, Placed>;
  selectedId: string | null;
  viewport: Viewport;
  size: Size;
}>();
const emit = defineEmits<{ centre: [point: Point]; zoom: [deltaY: number] }>();

const transform = computed(() => minimapTransform(props.placed));

const boxes = computed(() => {
  const t = transform.value;
  if (!t) return [];
  return props.nodes
    .filter(n => props.placed[n.id])
    .map(n => {
      const p = props.placed[n.id];
      return {
        id: n.id,
        type: n.type,
        style: {
          left: `${t.ox + (p.x - p.w / 2) * t.s}px`,
          top: `${t.oy + p.y * t.s}px`,
          width: `${Math.max(3, p.w * t.s)}px`,
          height: `${Math.max(2, p.h * t.s)}px`,
        },
      };
    });
});

const viewRect = computed(() => {
  const t = transform.value;
  const { viewport: v, size } = props;
  if (!t || !size.width || !v.zoom) return null;
  return {
    left: `${t.ox + (-v.x / v.zoom) * t.s}px`,
    top: `${t.oy + (-v.y / v.zoom) * t.s}px`,
    width: `${(size.width / v.zoom) * t.s}px`,
    height: `${(size.height / v.zoom) * t.s}px`,
  };
});

let dragBox: DOMRect | null = null;

function centreAt(event: PointerEvent) {
  const t = transform.value;
  if (!t || !dragBox) return;
  emit('centre', minimapToFlow(t, event.clientX - dragBox.left, event.clientY - dragBox.top));
}

function stop() {
  window.removeEventListener('pointermove', centreAt);
  window.removeEventListener('pointerup', stop);
  window.removeEventListener('pointercancel', stop);
  dragBox = null;
}

function onDown(event: PointerEvent) {
  if (event.button !== 0) return;
  event.stopPropagation();
  event.preventDefault();
  dragBox = (event.currentTarget as HTMLElement).getBoundingClientRect();
  centreAt(event);
  window.addEventListener('pointermove', centreAt);
  window.addEventListener('pointerup', stop);
  window.addEventListener('pointercancel', stop);
}

function onWheel(event: WheelEvent) {
  emit('zoom', event.deltaY);
}

onBeforeUnmount(stop);
</script>

<style scoped>
.ost-minimap {
  position: absolute;
  right: 12px;
  bottom: 12px;
  z-index: 5;
  width: 198px;
  height: 134px;
  background: color-mix(in srgb, var(--color-bg) 88%, transparent);
  border: 1px solid var(--color-neutral-800);
  border-radius: var(--radius-md);
  overflow: hidden;
  box-shadow: var(--shadow-sm);
  cursor: crosshair;
  touch-action: none;
}

.ost-minimap__label {
  position: absolute;
  left: 8px;
  top: 6px;
  font-size: 9px;
  letter-spacing: 0.09em;
  text-transform: uppercase;
  color: var(--color-neutral-500);
  pointer-events: none;
}

.ost-minimap__node {
  position: absolute;
  border-radius: 1px;
  opacity: 0.8;
  pointer-events: none;
}
.ost-minimap__node--product,
.ost-minimap__node--outcome {
  background: var(--color-accent-500);
}
.ost-minimap__node--opportunity {
  background: var(--color-accent-400);
}
.ost-minimap__node--solution {
  background: var(--color-neutral-400);
}
.ost-minimap__node--assumption {
  background: var(--color-neutral-600);
}
.ost-minimap__node--evidence {
  background: var(--color-neutral-700);
}
.ost-minimap__node.is-selected {
  background: var(--color-accent-200);
  opacity: 1;
}

.ost-minimap__view {
  position: absolute;
  pointer-events: none;
  border-radius: 2px;
  border: 1px solid var(--color-accent-300);
  background: color-mix(in srgb, var(--color-accent) 12%, transparent);
}
</style>
