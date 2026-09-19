<template>
  <div class="ost-field">
    <div class="ost-field__label ost-field__label--split">
      <span :id="labelId">Priority</span><span data-cy="ost-priority-label">{{ priorityLabel(shown) }} · {{ Math.round(shown) }}</span>
    </div>
    <div
      ref="track"
      class="ost-priority"
      :class="{ 'is-readonly': readonly, 'is-dragging': draft !== null }"
      role="slider"
      :tabindex="readonly ? -1 : 0"
      aria-valuemin="1"
      aria-valuemax="100"
      :aria-valuenow="Math.round(shown)"
      :aria-valuetext="`${priorityLabel(shown)} (${Math.round(shown)})`"
      :aria-labelledby="labelId"
      :aria-readonly="readonly ? 'true' : undefined"
      data-cy="ost-priority"
      :data-value="Math.round(shown)"
      @pointerdown="onDown"
      @pointermove="onMove"
      @pointerup="onUp"
      @pointercancel="onCancel"
      @lostpointercapture="onUp"
      @keydown="onKey"
    >
      <i class="ost-priority__track"></i>
      <i class="ost-priority__fill" :style="fillStyle"></i>
      <i class="ost-priority__handle" :style="handleStyle" :data-color="priorityColor(shown)" data-cy="ost-priority-handle"></i>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * Opportunity priority: continuous 1–100 slider with the cold → warm oklch ramp (FR-V1).
 * Dragging only moves a local draft; the value is committed once, on release. Arrow keys
 * (±1), Page Up/Down (±10), Home/End commit immediately.
 */
import { computed, ref } from 'vue';

import { priorityColor, priorityLabel } from '../../domain/rules';

const props = defineProps<{ value: number; readonly?: boolean }>();
const emit = defineEmits<{ change: [value: number] }>();

const labelId = `ost-pri-${Math.random().toString(36).slice(2, 9)}`;
const track = ref<HTMLElement | null>(null);
/** Value under the pointer while dragging; null when idle. */
const draft = ref<number | null>(null);
let rect: DOMRect | null = null;

const shown = computed(() => draft.value ?? props.value);

const clamp = (v: number) => Math.min(100, Math.max(1, Math.round(v)));

const fillStyle = computed(() => {
  const p = shown.value;
  return {
    width: `calc(${p}% - ${(p / 100) * 4}px)`,
    background: `linear-gradient(90deg, ${priorityColor(Math.max(6, p * 0.15))}, ${priorityColor(p)})`,
  };
});

const handleStyle = computed(() => {
  const color = priorityColor(shown.value);
  return { left: `${shown.value}%`, background: color, boxShadow: `0 0 0 4px color-mix(in srgb, ${color} 22%, transparent)` };
});

function valueAt(clientX: number) {
  const r = rect ?? track.value?.getBoundingClientRect();
  if (!r || !r.width) return props.value;
  return clamp(((clientX - r.left) / r.width) * 100);
}

function onDown(event: PointerEvent) {
  if (props.readonly || event.button > 0) return;
  event.preventDefault();
  rect = track.value?.getBoundingClientRect() ?? null;
  try {
    track.value?.setPointerCapture?.(event.pointerId);
  } catch {
    // capture is a nicety (keeps the drag when the pointer leaves the track)
  }
  track.value?.focus();
  draft.value = valueAt(event.clientX);
}

function onMove(event: PointerEvent) {
  if (draft.value !== null) draft.value = valueAt(event.clientX);
}

function onUp() {
  if (draft.value === null) return;
  const value = draft.value;
  draft.value = null;
  rect = null;
  if (value !== props.value) emit('change', value);
}

function onCancel() {
  draft.value = null;
  rect = null;
}

function onKey(event: KeyboardEvent) {
  if (props.readonly) return;
  const steps: Record<string, number> = { ArrowLeft: -1, ArrowDown: -1, ArrowRight: 1, ArrowUp: 1, PageDown: -10, PageUp: 10 };
  let next: number | null = null;
  if (event.key in steps) next = clamp(props.value + steps[event.key]);
  else if (event.key === 'Home') next = 1;
  else if (event.key === 'End') next = 100;
  if (next === null) return;
  event.preventDefault();
  if (next !== props.value) emit('change', next);
}
</script>

<style scoped>
.ost-priority {
  position: relative;
  height: 26px;
  cursor: ew-resize;
  touch-action: none;
  border-radius: var(--radius-sm);
}

.ost-priority.is-readonly {
  cursor: default;
}

.ost-priority i {
  position: absolute;
  top: 50%;
  pointer-events: none;
}

.ost-priority__track {
  left: 0;
  right: 0;
  height: 4px;
  transform: translateY(-50%);
  border-radius: 999px;
  background: var(--color-neutral-900);
}

.ost-priority__fill {
  left: 2px;
  height: 4px;
  transform: translateY(-50%);
  border-radius: 999px;
}

.ost-priority__handle {
  width: 15px;
  height: 15px;
  transform: translate(-50%, -50%);
  border-radius: 50%;
}
</style>
