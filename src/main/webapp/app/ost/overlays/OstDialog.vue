<template>
  <div class="ost-dialog-backdrop" data-cy="ostDialogBackdrop" @click.self="emit('close')">
    <div
      ref="panel"
      class="ost-dialog"
      role="dialog"
      aria-modal="true"
      :aria-labelledby="titleId"
      :class="{ 'ost-dialog--flush': flush }"
      :style="{ width: width }"
      :data-cy="dataCy"
      tabindex="-1"
      @keydown="onKeydown"
    >
      <slot name="header" :title-id="titleId">
        <h2 :id="titleId" class="ost-dialog__title">{{ title }}</h2>
      </slot>
      <div class="ost-dialog__body">
        <slot></slot>
      </div>
      <div v-if="$slots.actions" class="ost-dialog__actions">
        <slot name="actions"></slot>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * Modal shell for every OST overlay: focus moves in and is trapped, Escape and backdrop click
 * close it, and focus returns to whatever had it before the dialog opened. `flush` drops the
 * padding and lets the body fill (the chat modal); the `header` slot replaces the title
 * (it receives `titleId`, which must label the dialog).
 */
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue';

withDefaults(defineProps<{ title: string; width?: string; dataCy?: string; flush?: boolean }>(), {
  width: '340px',
  dataCy: 'ostDialog',
  flush: false,
});
const emit = defineEmits<{ close: [] }>();

const titleId = `ost-dialog-title-${Math.random().toString(36).slice(2, 10)}`;
const panel = ref<HTMLElement | null>(null);
let returnFocusTo: HTMLElement | null = null;

const FOCUSABLE =
  'a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])';

const focusables = (): HTMLElement[] =>
  panel.value ? Array.from(panel.value.querySelectorAll<HTMLElement>(FOCUSABLE)).filter(el => !el.hasAttribute('hidden')) : [];

function onKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') {
    event.stopPropagation();
    event.preventDefault();
    emit('close');
    return;
  }
  if (event.key !== 'Tab') return;
  const items = focusables();
  if (!items.length) {
    event.preventDefault();
    panel.value?.focus();
    return;
  }
  const first = items[0];
  const last = items[items.length - 1];
  const active = document.activeElement as HTMLElement | null;
  if (event.shiftKey && (active === first || active === panel.value)) {
    event.preventDefault();
    last.focus();
  } else if (!event.shiftKey && active === last) {
    event.preventDefault();
    first.focus();
  }
}

/** Escape pressed while focus is outside the panel (e.g. on the backdrop) still closes. */
function onDocumentKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape' && panel.value && !panel.value.contains(event.target as Node)) {
    event.preventDefault();
    emit('close');
  }
}

onMounted(async () => {
  returnFocusTo = document.activeElement instanceof HTMLElement ? document.activeElement : null;
  document.addEventListener('keydown', onDocumentKeydown);
  await nextTick();
  const preferred = panel.value?.querySelector<HTMLElement>('[autofocus], [data-autofocus]');
  (preferred ?? focusables()[0] ?? panel.value)?.focus();
});

onBeforeUnmount(() => {
  document.removeEventListener('keydown', onDocumentKeydown);
  if (returnFocusTo && document.contains(returnFocusTo)) returnFocusTo.focus();
});
</script>

<style scoped>
/*
 * Above the app chrome — navbar 1030 (va-navbar.scss), sidebar 1020 (va-sidemenu.scss) — so the
 * modal dims and blocks everything and centres on the whole viewport. Toasts sit above at 1060.
 */
.ost-dialog-backdrop {
  position: fixed;
  inset: 0;
  z-index: 1050;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: color-mix(in srgb, #05060c 62%, transparent);
}

.ost-dialog {
  max-width: 100%;
  max-height: calc(100vh - 48px);
  overflow: auto;
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  background: var(--color-surface);
  color: var(--color-text);
  border: 1px solid var(--color-neutral-800);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
}

.ost-dialog--flush {
  max-height: 82vh;
  padding: 0;
  gap: 0;
  overflow: hidden;
}

.ost-dialog--flush .ost-dialog__body {
  display: flex;
  flex: 1 1 auto;
  min-height: 0;
  color: var(--color-text);
}

.ost-dialog:focus {
  outline: none;
}

.ost-dialog__title {
  font-size: 16px;
  font-weight: 500;
}

.ost-dialog__body {
  font-size: 13px;
  line-height: 1.45;
  color: var(--color-neutral-300);
}

.ost-dialog__actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
  margin-top: 4px;
}
</style>
