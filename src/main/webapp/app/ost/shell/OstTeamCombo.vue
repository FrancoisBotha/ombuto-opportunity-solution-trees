<template>
  <div ref="root" class="ost-team-combo" data-cy="ostTeamCombo" @focusout="onFocusOut" @pointerdown="pointerInside = true">
    <button
      ref="trigger"
      type="button"
      class="ost-team-combo__button"
      :class="{ 'is-open': open }"
      aria-haspopup="listbox"
      :aria-expanded="open"
      data-cy="ostTeamComboButton"
      @click="toggle"
      @keydown.down.prevent="openAndFocus(0)"
      @keydown.esc="onTriggerEscape"
    >
      <span class="ost-team-combo__label" data-cy="ostTeamComboLabel">{{ label }}</span>
      <PhCaretDown :size="12" weight="bold" class="ost-team-combo__caret" aria-hidden="true" />
    </button>
    <div
      v-if="open"
      ref="menu"
      class="ost-team-combo__menu"
      role="listbox"
      aria-label="Switch team"
      data-cy="ostTeamComboMenu"
      @keydown="onMenuKey"
    >
      <p v-if="!teams.length" class="ost-team-combo__empty">You are not in any team yet.</p>
      <button
        v-for="team in teams"
        :key="team.id"
        ref="options"
        type="button"
        role="option"
        class="ost-team-combo__option"
        :class="{ 'is-current': team.id === currentId }"
        :aria-selected="team.id === currentId"
        :data-cy="`ostTeamOption-${team.id}`"
        @click="choose(team.id)"
      >
        <span class="ost-team-combo__text">
          <span class="ost-team-combo__name">{{ team.name }}</span>
          <span class="ost-team-combo__meta">{{ meta(team) }}</span>
        </span>
        <PhCheck v-if="team.id === currentId" :size="13" weight="bold" class="ost-team-combo__check" aria-hidden="true" />
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
/** Team switcher in the OST top nav (combo button + listbox). */
import { PhCaretDown, PhCheck } from '@phosphor-icons/vue';
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue';

import type { MyTeamDTO } from '../ost.model';

const props = defineProps<{ teams: MyTeamDTO[]; currentId: number | null; currentName?: string | null }>();
const emit = defineEmits<{ select: [teamId: number] }>();

const open = ref(false);
const root = ref<HTMLElement | null>(null);
const trigger = ref<HTMLButtonElement | null>(null);
const menu = ref<HTMLElement | null>(null);
const options = ref<HTMLButtonElement[]>([]);
/** A press inside the combo is in progress (some browsers do not focus buttons on click). */
const pointerInside = ref(false);

const label = computed(() => props.currentName ?? props.teams.find(t => t.id === props.currentId)?.name ?? 'Choose a team');

const ROLE_LABEL: Record<string, string> = { OWNER: 'Owner', EDITOR: 'Editor', VIEWER: 'Viewer' };
const meta = (team: MyTeamDTO) =>
  `${ROLE_LABEL[team.role] ?? team.role} · ${team.productCount} product${team.productCount === 1 ? '' : 's'}`;

function toggle() {
  open.value = !open.value;
}

async function openAndFocus(index: number) {
  open.value = true;
  await nextTick();
  focusOption(index);
}

/**
 * Focuses an option without letting the browser scroll any ancestor into view (the OST shell and
 * page must never scroll), then scrolls only the menu so the option is visible.
 */
function focusOption(index: number) {
  const el = options.value[index];
  if (!el) return;
  el.focus({ preventScroll: true });
  const list = menu.value;
  if (!list) return;
  const top = el.offsetTop;
  const bottom = top + el.offsetHeight;
  if (top < list.scrollTop) list.scrollTop = top;
  else if (bottom > list.scrollTop + list.clientHeight) list.scrollTop = bottom - list.clientHeight;
}

function close(returnFocus = false) {
  open.value = false;
  if (returnFocus) trigger.value?.focus();
}

function choose(teamId: number) {
  close(true);
  if (teamId !== props.currentId) emit('select', teamId);
}

function onTriggerEscape(event: KeyboardEvent) {
  if (!open.value) return;
  event.preventDefault();
  close();
}

function onMenuKey(event: KeyboardEvent) {
  const i = options.value.findIndex(el => el === document.activeElement);
  const last = options.value.length - 1;
  if (event.key === 'Escape') {
    event.preventDefault();
    close(true);
  } else if (event.key === 'ArrowDown') {
    event.preventDefault();
    focusOption(Math.min(last, i + 1));
  } else if (event.key === 'ArrowUp') {
    event.preventDefault();
    if (i <= 0) trigger.value?.focus({ preventScroll: true });
    else focusOption(i - 1);
  } else if (event.key === 'Home') {
    event.preventDefault();
    focusOption(0);
  } else if (event.key === 'End') {
    event.preventDefault();
    focusOption(last);
  }
}

/** Focus leaving the combo (Tab, a click elsewhere) closes the menu. */
function onFocusOut(event: FocusEvent) {
  const next = event.relatedTarget as Node | null;
  if (!open.value || (next && root.value?.contains(next))) return;
  if (!next && pointerInside.value) return;
  close();
}

function onDocumentPointer(event: Event) {
  if (open.value && root.value && !root.value.contains(event.target as Node)) close();
}

function onDocumentPointerUp() {
  pointerInside.value = false;
}

onMounted(() => {
  document.addEventListener('pointerdown', onDocumentPointer);
  document.addEventListener('pointerup', onDocumentPointerUp);
  document.addEventListener('pointercancel', onDocumentPointerUp);
});
onBeforeUnmount(() => {
  document.removeEventListener('pointerdown', onDocumentPointer);
  document.removeEventListener('pointerup', onDocumentPointerUp);
  document.removeEventListener('pointercancel', onDocumentPointerUp);
});
</script>

<style scoped>
.ost-team-combo {
  position: relative;
}

.ost-team-combo__button {
  display: flex;
  align-items: center;
  gap: 7px;
  font: inherit;
  cursor: pointer;
  padding: 5px 9px;
  border-radius: var(--radius-md);
  border: 1px solid var(--color-neutral-800);
  background: transparent;
  color: var(--color-neutral-300);
}

.ost-team-combo__button.is-open {
  border-color: var(--color-accent-600);
  background: var(--color-accent-900);
  color: var(--color-accent-200);
}

.ost-team-combo__label {
  font-size: 11px;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  white-space: nowrap;
}

.ost-team-combo__caret {
  opacity: 0.7;
}

.ost-team-combo__menu {
  position: absolute;
  right: 0;
  top: calc(100% + 6px);
  min-width: 196px;
  /* A long team list scrolls inside the menu; the shell (overflow: clip) never scrolls. */
  max-height: min(420px, calc(100vh - 80px));
  overflow-y: auto;
  overscroll-behavior: contain;
  z-index: 80;
  padding: 5px;
  background: var(--color-surface);
  border: 1px solid var(--color-neutral-800);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-md);
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.ost-team-combo__empty {
  margin: 0;
  padding: 7px 9px;
  color: var(--color-neutral-400);
}

.ost-team-combo__option {
  flex: none;
  display: flex;
  align-items: center;
  gap: 10px;
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

.ost-team-combo__option:hover {
  background: color-mix(in srgb, var(--color-text) 7%, transparent);
}

.ost-team-combo__option.is-current {
  background: var(--color-accent-900);
}

.ost-team-combo__text {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.ost-team-combo__name {
  font-size: 13px;
  font-weight: 500;
}

.ost-team-combo__meta {
  font-size: 10.5px;
  color: var(--color-neutral-400);
}

.ost-team-combo__check {
  margin-left: auto;
  flex: none;
  color: var(--color-accent-300);
}
</style>
