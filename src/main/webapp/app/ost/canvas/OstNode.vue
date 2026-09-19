<template>
  <div
    class="ost-node"
    :class="[
      `ost-node--${node.type}`,
      { 'is-selected': selected, 'is-match': match, 'is-dimmed': dimmed, 'is-target': dropTarget, 'has-priority': isOpportunity },
    ]"
    :style="{ width: `${box.w}px`, minHeight: `${box.h}px` }"
    :data-cy="`ost-node-${node.id}`"
    :data-node-key="node.id"
    :data-node-type="node.type"
    :aria-label="`${box.label}: ${node.title}`"
  >
    <Handle type="target" :position="Position.Top" class="ost-node__handle" :connectable="false" />

    <div class="ost-node__kicker">{{ box.label }}</div>
    <div class="ost-node__title" data-cy="ost-node-title">{{ node.title }}</div>

    <div v-if="node.type !== 'product'" class="ost-node__meta">
      <span v-if="node.status" class="ost-node__badge" :class="`ost-node__badge--${tone}`" :data-tone="tone" data-cy="ost-node-status">{{
        node.status
      }}</span>
      <span v-if="node.type === 'assumption'" class="ost-node__metric" data-cy="ost-node-metric">{{ node.conf }}% confidence</span>
      <span v-else-if="node.type === 'solution' && evidence && evidence.score !== null" class="ost-node__metric" data-cy="ost-node-metric">
        {{ evidence.score }}% evidence
      </span>
      <span v-else-if="isOpportunity" class="ost-node__money" title="Opportunity value" data-cy="ost-node-value"
        ><span class="ost-node__money-on">{{ '$'.repeat(value) }}</span
        ><span class="ost-node__money-off">{{ '$'.repeat(5 - value) }}</span></span
      >
      <button
        type="button"
        class="ost-node__chat nopan"
        :class="{ 'is-empty': !node.commentCount }"
        title="Open thread"
        :aria-label="`Open thread (${node.commentCount} message${node.commentCount === 1 ? '' : 's'})`"
        :data-cy="`ost-node-chat-${node.id}`"
        @click.stop="emit('chat')"
      >
        <PhChat :size="11" aria-hidden="true" />{{ node.commentCount }}
      </button>
    </div>

    <div
      v-if="isOpportunity"
      class="ost-node__priority"
      :title="`Priority ${node.priority} · ${priorityLabel(node.priority)}`"
      data-cy="ost-node-priority"
    >
      <i v-for="dot in priorityDots" :key="dot.step" :data-filled="dot.fill > 0" :data-color="dot.color" :style="dot.style" />
    </div>

    <button
      v-if="canAdd"
      type="button"
      class="ost-node__add nopan"
      title="Add child"
      :aria-label="`Add a child under ${node.title}`"
      :data-cy="`ost-node-add-${node.id}`"
      @click.stop="emit('add')"
    >
      +
    </button>
    <button
      v-if="childCount > 0"
      type="button"
      class="ost-node__toggle nopan"
      :class="{ 'is-collapsed': collapsed }"
      :title="collapsed ? `Expand (${childCount} hidden)` : 'Collapse'"
      :aria-expanded="!collapsed"
      :data-cy="`ost-collapse-${node.id}`"
      @click.stop="emit('toggle')"
    >
      {{ collapsed ? `+${childCount}` : '–' }}
    </button>

    <Handle type="source" :position="Position.Bottom" class="ost-node__handle" :connectable="false" />
  </div>
</template>

<script setup lang="ts">
/**
 * One tree node on the canvas (Vue Flow custom node body). Type is carried by frame + fill weight,
 * never hue — see the frame table in the design handoff README. Purely presentational: the canvas
 * computes every flag from the stores and handles the emitted events.
 *
 * Adapted from design_handoff_ombuto_ost/vue-reference/src/components/OstNode.vue: styles moved to
 * classes, Phosphor chat icon, the `+` only for editors on types that can have children, and an
 * evidence metric on solutions (the prototype computes it; one metric per type).
 */
import { PhChat } from '@phosphor-icons/vue';
import { computed } from 'vue';

import { Handle, Position } from '@vue-flow/core';

import { statusTone } from '../domain/derive';
import { TYPE_BOX, priorityColor, priorityLabel } from '../domain/rules';
import type { OstNode } from '../domain/types';

const props = withDefaults(
  defineProps<{
    node: OstNode;
    selected?: boolean;
    dimmed?: boolean;
    match?: boolean;
    dropTarget?: boolean;
    /** direct children (shown as +n when collapsed) */
    childCount?: number;
    collapsed?: boolean;
    /** editors only, and only for types that permit children */
    canAdd?: boolean;
    /** solutions: derived evidence strength */
    evidence?: { tests: number; score: number | null } | null;
  }>(),
  { selected: false, dimmed: false, match: false, dropTarget: false, childCount: 0, collapsed: false, canAdd: false, evidence: null },
);

const emit = defineEmits<{ add: []; toggle: []; chat: [] }>();

const box = computed(() => TYPE_BOX[props.node.type]);
const isOpportunity = computed(() => props.node.type === 'opportunity');
const tone = computed(() => statusTone(props.node.status));
const value = computed(() => Math.min(5, Math.max(0, Math.round(props.node.value || 0))));

/** Five dots, top = highest band; each fills over its 20-point band and grows 4.5 → 7px. */
const priorityDots = computed(() =>
  [5, 4, 3, 2, 1].map(step => {
    const p = props.node.priority;
    const fill = Math.max(0, Math.min(1, (p - (step - 1) * 20) / 20));
    const size = (4.5 + fill * 2.5).toFixed(1);
    const color = fill > 0 ? priorityColor(p) : 'var(--color-neutral-800)';
    return {
      step,
      fill,
      color,
      style: {
        width: `${size}px`,
        height: `${size}px`,
        background: color,
        opacity: fill > 0 ? String(0.35 + fill * 0.65) : '1',
      },
    };
  }),
);
</script>

<style scoped>
.ost-node {
  position: relative;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
  padding: 9px 11px 8px;
  border-radius: var(--radius-md);
  border: 1px solid var(--color-neutral-800);
  background: var(--color-surface);
  color: var(--color-text);
  font-family: var(--font-body);
  user-select: none;
  cursor: pointer;
  transition: opacity 0.18s ease;
}

/* ---- frames: type is carried by frame, never hue ------------------------------------------- */
.ost-node--product {
  background: var(--color-accent-900);
  border-color: var(--color-accent-700);
  justify-content: center;
}
.ost-node--outcome {
  background: var(--color-accent-900);
  border-color: var(--color-accent-600);
}
.ost-node--opportunity {
  background: var(--color-surface);
  border-color: var(--color-accent-500);
  padding-right: 20px;
}
.ost-node--solution {
  background: var(--color-neutral-900);
  border-color: var(--color-neutral-700);
}
.ost-node--assumption {
  background: transparent;
  border: 1px dashed var(--color-accent-500);
}
.ost-node--evidence {
  background: transparent;
  border-color: var(--color-neutral-800);
  font-style: italic;
}

/* ---- states ---------------------------------------------------------------------------------- */
.ost-node.is-match {
  border-style: solid;
  border-color: var(--color-accent);
  box-shadow: 0 0 0 3px var(--color-accent-800);
}
.ost-node.is-target {
  outline: 1px dashed var(--color-accent-400);
  outline-offset: 3px;
}
.ost-node.is-selected {
  outline: 2px solid var(--color-accent);
  outline-offset: 2px;
}
.ost-node.is-dimmed {
  opacity: 0.24;
}

/* ---- content --------------------------------------------------------------------------------- */
.ost-node__kicker {
  font-family: var(--font-heading);
  font-size: 9px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  font-style: normal;
  opacity: 0.7;
}

.ost-node__title {
  margin-top: 6px;
  font-family: var(--font-heading);
  font-weight: 600;
  font-size: 15px;
  line-height: 1.18;
  text-wrap: pretty;
  overflow: hidden;
  overflow-wrap: anywhere;
}

.ost-node__meta {
  margin-top: auto;
  padding-top: 6px;
  display: flex;
  align-items: center;
  gap: 5px;
  font-style: normal;
}

.ost-node__badge {
  display: inline-flex;
  align-items: center;
  font-family: var(--font-heading);
  font-size: 10px;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  padding: 2px 7px;
  border-radius: var(--radius-sm);
  border: 1px solid transparent;
  white-space: nowrap;
}
.ost-node__badge--good {
  background: var(--color-accent-800);
  color: var(--color-accent-200);
}
.ost-node__badge--flight {
  background: transparent;
  border-color: var(--color-accent-600);
  color: var(--color-accent-300);
}
.ost-node__badge--bad {
  background: var(--color-neutral-900);
  color: var(--color-neutral-300);
}

.ost-node__metric {
  font-size: 10px;
  letter-spacing: 0.08em;
  opacity: 0.7;
  min-width: 0; /* wraps like the prototype ("20% / confidence") so the thread chip never overflows */
}

.ost-node__money {
  font-size: 11px;
  letter-spacing: -0.04em;
  white-space: nowrap;
}
.ost-node__money-on {
  color: var(--color-accent-300);
}
.ost-node__money-off {
  color: var(--color-neutral-700);
}

.ost-node__chat {
  margin-left: auto;
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font: inherit;
  font-size: 10px;
  padding: 2px 6px;
  cursor: pointer;
  border-radius: 999px;
  border: 1px solid var(--color-neutral-700);
  background: transparent;
  color: var(--color-accent-300);
}
.ost-node__chat.is-empty {
  border-color: transparent;
  color: var(--color-neutral-500);
}
.ost-node__chat:hover {
  border-color: var(--color-accent-600);
}

.ost-node__priority {
  position: absolute;
  right: 6px;
  top: 24px;
  bottom: 9px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 4px;
}
.ost-node__priority i {
  display: block;
  border-radius: 50%;
}

.ost-node__add {
  position: absolute;
  right: -1px;
  top: -1px;
  width: 19px;
  height: 19px;
  display: grid;
  place-items: center;
  padding: 0;
  font: inherit;
  font-style: normal;
  font-size: 13px;
  line-height: 1;
  border-radius: var(--radius-sm);
  background: var(--color-surface);
  border: 1px solid var(--color-neutral-700);
  color: var(--color-accent-300);
  cursor: pointer;
}
.ost-node__add:hover {
  background: var(--color-accent-800);
  border-color: var(--color-accent-600);
  color: var(--color-accent-200);
}

.ost-node__toggle {
  position: absolute;
  left: 50%;
  bottom: -11px;
  transform: translateX(-50%);
  min-width: 24px;
  height: 20px;
  padding: 0 6px;
  font: inherit;
  font-style: normal;
  font-family: var(--font-heading);
  font-size: 11px;
  letter-spacing: 0.04em;
  line-height: 1;
  border-radius: var(--radius-sm);
  background: var(--color-surface);
  color: var(--color-text);
  border: 1px solid var(--color-neutral-800);
  cursor: pointer;
  z-index: 20;
}
.ost-node__toggle.is-collapsed {
  background: var(--color-accent-800);
  color: var(--color-accent-200);
  border-color: var(--color-accent-600);
}

.ost-node__handle {
  opacity: 0;
  pointer-events: none;
}
</style>
