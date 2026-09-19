<script setup lang="ts">
/**
 * Vue Flow custom node. Type is carried by frame + fill weight, never hue.
 * Register as: :node-types="{ ost: markRaw(OstNode) }"
 */
import { computed } from 'vue';
import { Handle, Position } from '@vue-flow/core';
import { TYPE_BOX, GOOD_STATUS, BAD_STATUS, priorityColor } from '../domain/rules';
import type { OstNode } from '../domain/types';

const props = defineProps<{ data: { node: OstNode; selected: boolean; dimmed: boolean; match: boolean; dropTarget: boolean; childCount: number; collapsed: boolean } }>();
const emit = defineEmits<{ (e: 'add'): void; (e: 'toggle'): void; (e: 'chat'): void }>();

const n = computed(() => props.data.node);
const box = computed(() => TYPE_BOX[n.value.type]);

const frame = computed(() => {
  switch (n.value.type) {
    case 'product':     return { background: 'var(--color-accent-900)', border: '1px solid var(--color-accent-700)' };
    case 'outcome':     return { background: 'var(--color-accent-900)', border: '1px solid var(--color-accent-600)' };
    case 'opportunity': return { background: 'var(--color-surface)',    border: '1px solid var(--color-accent-500)' };
    case 'solution':    return { background: 'var(--color-neutral-900)', border: '1px solid var(--color-neutral-700)' };
    case 'assumption':  return { background: 'transparent',             border: '1px dashed var(--color-accent-500)' };
    default:            return { background: 'transparent',             border: '1px solid var(--color-neutral-800)', fontStyle: 'italic' };
  }
});

const statusTone = computed(() => {
  if (GOOD_STATUS.includes(n.value.status)) return { background: 'var(--color-accent-800)', color: 'var(--color-accent-200)', border: '1px solid transparent' };
  if (BAD_STATUS.includes(n.value.status)) return { background: 'var(--color-neutral-900)', color: 'var(--color-neutral-300)', border: '1px solid transparent' };
  return { background: 'transparent', color: 'var(--color-accent-300)', border: '1px solid var(--color-accent-600)' };
});

const isOpportunity = computed(() => n.value.type === 'opportunity');
const priDots = computed(() => [5, 4, 3, 2, 1].map(step => {
  const fill = Math.max(0, Math.min(1, (n.value.priority - (step - 1) * 20) / 20));
  return { size: 4.5 + fill * 2.5, color: fill > 0 ? priorityColor(n.value.priority) : 'var(--color-neutral-800)', opacity: fill > 0 ? 0.35 + fill * 0.65 : 1 };
}));
</script>

<template>
  <div
    class="ost-node"
    :class="{ 'is-selected': data.selected, 'is-target': data.dropTarget, 'is-match': data.match }"
    :style="{ width: box.w + 'px', minHeight: box.h + 'px', opacity: data.dimmed ? 0.24 : 1,
              padding: isOpportunity ? '9px 20px 8px 11px' : '9px 11px 8px', ...frame }"
  >
    <Handle type="target" :position="Position.Top" style="opacity:0" />
    <div class="kicker">{{ box.label }}</div>
    <div class="title">{{ n.title }}</div>

    <div class="meta">
      <span v-if="n.status" class="badge" :style="statusTone">{{ n.status }}</span>
      <span v-if="n.type === 'assumption'" class="muted">{{ n.conf }}% confidence</span>
      <span v-if="isOpportunity" class="money">
        {{ '$'.repeat(n.value) }}<span class="money-rest">{{ '$'.repeat(5 - n.value) }}</span>
      </span>
      <button v-if="n.type !== 'product'" class="chat" @click.stop="emit('chat')">💬 {{ n.comments.length }}</button>
    </div>

    <div v-if="isOpportunity" class="pri" title="Priority">
      <i v-for="(d, i) in priDots" :key="i"
         :style="{ width: d.size + 'px', height: d.size + 'px', background: d.color, opacity: d.opacity }" />
    </div>

    <button class="plus" @click.stop="emit('add')">+</button>
    <button v-if="data.childCount" class="toggle" @click.stop="emit('toggle')">
      {{ data.collapsed ? '+' + data.childCount : '–' }}
    </button>
    <Handle type="source" :position="Position.Bottom" style="opacity:0" />
  </div>
</template>

<style scoped>
.ost-node { position: relative; display: flex; flex-direction: column; border-radius: var(--radius-md);
  color: var(--color-text); user-select: none; cursor: grab; transition: transform .18s ease; }
.ost-node.is-selected { outline: 2px solid var(--color-accent); outline-offset: 2px; }
.ost-node.is-target   { outline: 1px dashed var(--color-accent-400); outline-offset: 3px; cursor: copy; }
.ost-node.is-match    { box-shadow: 0 0 0 3px var(--color-accent-800); }
.kicker { font-size: 9px; letter-spacing: .08em; text-transform: uppercase; opacity: .7; }
.title  { margin-top: 6px; font-size: 15px; font-weight: 600; line-height: 1.18; text-wrap: pretty; }
.meta   { margin-top: auto; padding-top: 6px; display: flex; align-items: center; gap: 5px; }
.badge  { font-size: 10px; letter-spacing: .06em; text-transform: uppercase; padding: 2px 7px; border-radius: var(--radius-sm); }
.muted  { font-size: 10px; opacity: .7; }
.money  { font-size: 11px; letter-spacing: -.04em; color: var(--color-accent-300); }
.money-rest { color: var(--color-neutral-700); }
.chat   { margin-left: auto; font: inherit; font-size: 10px; padding: 2px 6px; border-radius: 999px;
  background: transparent; border: 1px solid var(--color-neutral-700); color: var(--color-accent-300); cursor: pointer; }
.pri    { position: absolute; right: 6px; top: 24px; bottom: 9px; display: flex; flex-direction: column;
  justify-content: center; align-items: center; gap: 4px; }
.pri i  { border-radius: 50%; }
.plus   { position: absolute; right: -1px; top: -1px; width: 19px; height: 19px; display: grid; place-items: center;
  font-size: 13px; line-height: 1; border-radius: var(--radius-sm); background: var(--color-surface);
  border: 1px solid var(--color-neutral-700); color: var(--color-accent-300); cursor: pointer; }
.plus:hover { background: var(--color-accent-800); border-color: var(--color-accent-600); }
.toggle { position: absolute; left: 50%; bottom: -11px; transform: translateX(-50%); min-width: 24px; height: 20px;
  font-size: 11px; border-radius: var(--radius-sm); background: var(--color-surface);
  border: 1px solid var(--color-neutral-800); color: var(--color-text); cursor: pointer; }
</style>
