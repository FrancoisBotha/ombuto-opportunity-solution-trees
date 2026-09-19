<template>
  <div
    ref="root"
    class="ost-add-menu nodrag nopan"
    role="menu"
    :aria-label="`Add a child under ${parentTitle}`"
    data-cy="ost-add-menu"
    @keydown="onKeydown"
    @pointerdown.stop
    @click.stop
    @dblclick.stop
  >
    <div class="ost-add-menu__title" aria-hidden="true">Add child</div>
    <button
      v-for="option in options"
      :key="option.type"
      type="button"
      role="menuitem"
      class="ost-add-menu__item"
      :data-cy="`ost-add-menu-${option.type}`"
      @click="emit('choose', option.type)"
    >
      <span class="ost-swatch" :class="`ost-swatch--${option.type}`" aria-hidden="true"></span>{{ option.label }}
    </button>
  </div>
</template>

<script setup lang="ts">
/**
 * The node `+` menu: exactly the node's valid child types (prototype: `addOptions`, "Add child").
 * Keyboard: focus lands on the first item, ↑/↓/Home/End move, Enter/Space choose, Escape or Tab
 * closes (focus goes back to the `+`). A pointer press anywhere outside the menu and its `+` closes it.
 */
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue';

import { TYPE_BOX } from '../domain/rules';
import type { NodeType } from '../domain/types';

import { childTypesFor } from './edit-rules';

const props = defineProps<{ parentType: NodeType; parentTitle: string; trigger?: HTMLElement | null }>();
const emit = defineEmits<{ choose: [type: NodeType]; close: [refocus: boolean] }>();

const root = ref<HTMLElement | null>(null);
const options = computed(() => childTypesFor(props.parentType).map(type => ({ type, label: TYPE_BOX[type].label })));

const items = () => Array.from(root.value?.querySelectorAll<HTMLButtonElement>('[role="menuitem"]') ?? []);

function move(delta: number | 'first' | 'last') {
  const list = items();
  if (!list.length) return;
  const i = list.indexOf(document.activeElement as HTMLButtonElement);
  const next = delta === 'first' ? 0 : delta === 'last' ? list.length - 1 : (i + delta + list.length) % list.length;
  list[next].focus({ preventScroll: true });
}

function onKeydown(event: KeyboardEvent) {
  event.stopPropagation();
  if (event.key === 'ArrowDown') move(1);
  else if (event.key === 'ArrowUp') move(-1);
  else if (event.key === 'Home') move('first');
  else if (event.key === 'End') move('last');
  else if (event.key === 'Escape') emit('close', true);
  else if (event.key === 'Tab') emit('close', false);
  else return;
  if (event.key !== 'Tab') event.preventDefault();
}

function onDocumentPointerDown(event: PointerEvent) {
  const target = event.target as Node | null;
  if (!target || root.value?.contains(target) || props.trigger?.contains(target)) return;
  emit('close', false);
}

onMounted(async () => {
  document.addEventListener('pointerdown', onDocumentPointerDown, true);
  await nextTick();
  items()[0]?.focus({ preventScroll: true });
});
onBeforeUnmount(() => document.removeEventListener('pointerdown', onDocumentPointerDown, true));
</script>

<style scoped>
.ost-add-menu {
  position: absolute;
  right: -2px;
  top: 20px;
  z-index: 90;
  min-width: 126px;
  padding: 4px;
  display: flex;
  flex-direction: column;
  gap: 1px;
  background: var(--color-surface);
  border: 1px solid var(--color-neutral-700);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-md);
  font-style: normal;
  cursor: default;
}

.ost-add-menu__title {
  font-size: 9px;
  letter-spacing: 0.09em;
  text-transform: uppercase;
  color: var(--color-neutral-500);
  padding: 3px 6px 4px;
}

.ost-add-menu__item {
  display: flex;
  align-items: center;
  gap: 7px;
  width: 100%;
  padding: 6px 7px;
  font: inherit;
  font-size: 12.5px;
  text-align: left;
  color: var(--color-text);
  background: transparent;
  border: 0;
  border-radius: var(--radius-sm);
  cursor: pointer;
}
.ost-add-menu__item:hover,
.ost-add-menu__item:focus-visible {
  background: var(--color-accent-900);
}

.ost-swatch {
  width: 10px;
  height: 10px;
  flex: none;
  border-radius: 2px;
  background: transparent;
  border: 1px solid var(--color-accent-500);
}
.ost-swatch--outcome {
  background: var(--color-accent-900);
}
.ost-swatch--solution {
  background: var(--color-neutral-900);
  border-color: var(--color-neutral-600);
}
.ost-swatch--assumption {
  border-style: dashed;
}
.ost-swatch--evidence {
  border-color: var(--color-neutral-600);
}
</style>
