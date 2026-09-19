<template>
  <div ref="root" class="ost-product-combo">
    <button
      ref="trigger"
      type="button"
      class="ost-product-combo__button ost-hit"
      :class="{ 'is-open': open }"
      aria-haspopup="listbox"
      :aria-expanded="open"
      data-cy="ost-product-combo"
      @click="open = !open"
      @keydown.down.prevent="openAndFocus(0)"
    >
      <span class="ost-product-combo__label" data-cy="ost-product-combo-label">{{ current ? current.title : 'All products' }}</span>
      <span class="ost-product-combo__meta">{{ current ? nodeCount(current.id) : branchCount }}</span>
      <PhCaretDown :size="12" class="ost-product-combo__caret" aria-hidden="true" />
    </button>
    <div v-if="open" class="ost-product-combo__menu" role="listbox" aria-label="Scope the canvas" @keydown="onMenuKey">
      <button
        ref="options"
        type="button"
        role="option"
        class="ost-product-combo__option"
        :class="{ 'is-on': productId === 'all' }"
        :aria-selected="productId === 'all'"
        data-cy="ost-product-option-all"
        @click="choose('all')"
      >
        <span class="ost-product-combo__box" :class="{ 'is-on': productId === 'all' }">
          <PhCheck v-if="productId === 'all'" :size="11" aria-hidden="true" />
        </span>
        <span class="ost-product-combo__name">All products</span>
        <span class="ost-product-combo__aside">{{ branchCount }}</span>
      </button>
      <i class="ost-product-combo__rule" aria-hidden="true"></i>
      <button
        v-for="product in products"
        :key="product.id"
        ref="options"
        type="button"
        role="option"
        class="ost-product-combo__option"
        :class="{ 'is-on': productId === product.id }"
        :aria-selected="productId === product.id"
        :data-cy="`ost-product-option-${product.id}`"
        @click="choose(product.id)"
      >
        <span class="ost-product-combo__radio" :class="{ 'is-on': productId === product.id }" aria-hidden="true"></span>
        <span class="ost-product-combo__text">
          <span class="ost-product-combo__name">{{ product.title }}</span>
          <span class="ost-product-combo__sub">{{ nodeCount(product.id) }}</span>
        </span>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
/** Product scope combo: "All products" (checkbox row) or one product branch (radio rows). */
import { PhCaretDown, PhCheck } from '@phosphor-icons/vue';
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue';

import type { OstNode } from '../domain/types';

const props = defineProps<{ products: OstNode[]; productId: string | 'all'; counts: Record<string, number> }>();
const emit = defineEmits<{ select: [productId: string | 'all'] }>();

const open = ref(false);
const root = ref<HTMLElement | null>(null);
const trigger = ref<HTMLButtonElement | null>(null);
const options = ref<HTMLButtonElement[]>([]);

const current = computed(() => (props.productId === 'all' ? null : (props.products.find(p => p.id === props.productId) ?? null)));
const branchCount = computed(() => `${props.products.length} branch${props.products.length === 1 ? '' : 'es'}`);
const nodeCount = (key: string) => {
  const n = props.counts[key] ?? 0;
  return `${n} node${n === 1 ? '' : 's'}`;
};

async function openAndFocus(index: number) {
  open.value = true;
  await nextTick();
  options.value[index]?.focus();
}

function close(returnFocus = false) {
  open.value = false;
  if (returnFocus) trigger.value?.focus();
}

function choose(productId: string | 'all') {
  close(true);
  emit('select', productId);
}

function onMenuKey(event: KeyboardEvent) {
  const i = options.value.findIndex(el => el === document.activeElement);
  if (event.key === 'Escape') {
    event.preventDefault();
    close(true);
  } else if (event.key === 'ArrowDown') {
    event.preventDefault();
    options.value[Math.min(options.value.length - 1, i + 1)]?.focus();
  } else if (event.key === 'ArrowUp') {
    event.preventDefault();
    if (i <= 0) trigger.value?.focus();
    else options.value[i - 1]?.focus();
  }
}

function onDocumentPointer(event: Event) {
  if (open.value && root.value && !root.value.contains(event.target as Node)) close();
}

onMounted(() => document.addEventListener('pointerdown', onDocumentPointer));
onBeforeUnmount(() => document.removeEventListener('pointerdown', onDocumentPointer));
</script>

<style scoped>
.ost-product-combo {
  position: relative;
}

.ost-product-combo__button {
  display: flex;
  align-items: center;
  gap: 8px;
  font: inherit;
  cursor: pointer;
  padding: 6px 10px;
  border-radius: var(--radius-md);
  color: var(--color-text);
  border: 1px solid var(--color-neutral-800);
  background: transparent;
}
.ost-product-combo__button.is-open {
  border-color: var(--color-accent-600);
  background: var(--color-accent-900);
}

.ost-product-combo__label {
  font-size: 13px;
  font-weight: 500;
  white-space: nowrap;
}

.ost-product-combo__meta,
.ost-product-combo__aside {
  font-size: 11px;
  color: var(--color-neutral-400);
  white-space: nowrap;
}

.ost-product-combo__aside {
  margin-left: auto;
}

.ost-product-combo__caret {
  opacity: 0.7;
  margin-left: 2px;
}

.ost-product-combo__menu {
  position: absolute;
  left: 0;
  top: calc(100% + 6px);
  min-width: 232px;
  z-index: 70;
  padding: 5px;
  background: var(--color-surface);
  border: 1px solid var(--color-neutral-800);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-md);
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.ost-product-combo__option {
  display: flex;
  align-items: center;
  gap: 9px;
  width: 100%;
  padding: 7px 9px;
  font: inherit;
  color: var(--color-text);
  cursor: pointer;
  text-align: left;
  border-radius: var(--radius-sm);
  border: 1px solid transparent;
  background: transparent;
}
.ost-product-combo__option:hover {
  background: color-mix(in srgb, var(--color-text) 7%, transparent);
}
.ost-product-combo__option.is-on {
  background: var(--color-accent-900);
}

.ost-product-combo__box {
  width: 15px;
  height: 15px;
  flex: none;
  display: grid;
  place-items: center;
  border-radius: 4px;
  border: 1px solid var(--color-neutral-600);
  background: transparent;
  color: var(--color-bg);
}
.ost-product-combo__box.is-on {
  border-color: var(--color-accent-400);
  background: var(--color-accent-400);
}

.ost-product-combo__radio {
  width: 15px;
  height: 15px;
  flex: none;
  border-radius: 50%;
  border: 1px solid var(--color-neutral-600);
}
.ost-product-combo__radio.is-on {
  border-color: var(--color-accent-400);
  box-shadow:
    inset 0 0 0 3px var(--color-surface),
    inset 0 0 0 9px var(--color-accent-400);
}

.ost-product-combo__rule {
  height: 1px;
  background: var(--color-neutral-800);
  margin: 3px 8px;
}

.ost-product-combo__text {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.ost-product-combo__name {
  font-size: 13px;
  font-weight: 500;
}

.ost-product-combo__sub {
  font-size: 10.5px;
  color: var(--color-neutral-400);
}
</style>
