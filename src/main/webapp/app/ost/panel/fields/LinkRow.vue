<template>
  <div class="ost-link" :data-cy="`ost-link-row-${link.id}`">
    <div class="ost-link__head">
      <span class="ost-link__dot" :class="{ 'is-wiki': isWikiLink(link.url) }"></span>
      <input
        ref="nameInput"
        v-model="name"
        class="ost-input ost-link__name"
        aria-label="Link name"
        maxlength="100"
        :readonly="readonly"
        data-cy="ost-link-name"
        @keydown.enter.prevent="blurTarget"
        @keydown.esc.prevent="cancel($event)"
        @blur="commitName"
      />
      <a
        class="ost-link__open ost-tap"
        :href="link.url"
        target="_blank"
        rel="noopener noreferrer"
        :title="`Open ${link.name}`"
        :aria-label="`Open ${link.name} in a new tab`"
        data-cy="ost-link-open"
      >
        <PhArrowSquareOut :size="13" aria-hidden="true" />
      </a>
      <button
        v-if="!readonly"
        type="button"
        class="ost-link__remove ost-tap"
        title="Remove"
        :aria-label="`Remove ${link.name}`"
        data-cy="ost-link-remove"
        @click="emit('remove')"
      >
        <PhX :size="12" aria-hidden="true" />
      </button>
    </div>
    <input
      ref="urlInput"
      v-model="url"
      class="ost-input ost-link__url"
      aria-label="Link URL"
      placeholder="https://"
      maxlength="2000"
      :readonly="readonly"
      data-cy="ost-link-url"
      @keydown.enter.prevent="blurTarget"
      @keydown.esc.prevent="cancel($event)"
      @blur="commitUrl"
    />
    <div v-if="invalid" class="ost-link__error" role="alert" data-cy="ost-link-error">{{ invalid }}</div>
  </div>
</template>

<script setup lang="ts">
/** One link: name + URL, edited inline (commit on Enter / blur, Escape reverts), openable, removable. */
import { ref, watch } from 'vue';

import { PhArrowSquareOut, PhX } from '@phosphor-icons/vue';

import type { LinkRef } from '../../domain/types';
import { isValidLinkUrl, isWikiLink } from '../panel-format';

const props = defineProps<{ link: LinkRef; readonly?: boolean }>();
const emit = defineEmits<{ save: [patch: { name?: string; url?: string }]; remove: [] }>();

const name = ref(props.link.name);
const url = ref(props.link.url);
const invalid = ref<string | null>(null);
const nameInput = ref<HTMLInputElement | null>(null);
const urlInput = ref<HTMLInputElement | null>(null);
let cancelling = false;

const focused = (el: HTMLInputElement | null) => !!el && document.activeElement === el;

// Follow the stored link (server response, rollback): each draft resets on its own field's
// change only, and never while the user is typing in it (a save of the other field must not
// wipe what is being typed here).
watch(
  () => props.link.name,
  n => {
    if (!focused(nameInput.value)) name.value = n;
  },
);
watch(
  () => props.link.url,
  u => {
    if (!focused(urlInput.value)) url.value = u;
  },
);

const blurTarget = (event: Event) => (event.target as HTMLElement).blur();

function cancel(event: KeyboardEvent) {
  cancelling = true;
  name.value = props.link.name;
  url.value = props.link.url;
  invalid.value = null;
  (event.target as HTMLInputElement).blur();
  cancelling = false;
}

function commitName() {
  if (cancelling || props.readonly) return;
  const next = name.value.trim();
  if (!next) {
    invalid.value = 'Link names cannot be empty.';
    name.value = props.link.name;
    return;
  }
  invalid.value = null;
  if (next !== props.link.name) emit('save', { name: next });
  else name.value = props.link.name;
}

function commitUrl() {
  if (cancelling || props.readonly) return;
  const next = url.value.trim();
  if (!isValidLinkUrl(next)) {
    invalid.value = 'Links must start with http:// or https://.';
    url.value = props.link.url;
    return;
  }
  invalid.value = null;
  if (next !== props.link.url) emit('save', { url: next });
  else url.value = props.link.url;
}
</script>

<style scoped>
.ost-link {
  display: flex;
  flex-direction: column;
  gap: 5px;
  padding: 9px 10px;
  border: 1px solid var(--color-neutral-800);
  border-radius: var(--radius-md);
}

.ost-link__head {
  display: flex;
  align-items: center;
  gap: 7px;
}

.ost-link__dot {
  width: 7px;
  height: 7px;
  flex: none;
  border-radius: 50%;
  background: var(--color-neutral-500);
}

.ost-link__dot.is-wiki {
  background: var(--color-accent-400);
}

.ost-link .ost-link__name {
  flex: 1;
  min-width: 0;
  min-height: 26px;
  padding: 2px 6px;
  font-size: 12.5px;
  font-weight: 500;
  background: transparent;
  border-color: transparent;
}

.ost-link .ost-link__url {
  min-height: 28px;
  padding: 4px 8px;
  font-size: 12px;
  color: var(--color-neutral-300);
  background: var(--color-surface);
}

.ost-link__open,
.ost-link__remove {
  display: grid;
  place-items: center;
  width: 24px;
  height: 24px;
  flex: none;
  border-radius: var(--radius-sm);
}

.ost-link__open {
  color: var(--color-accent-300);
}

.ost-link__remove {
  font: inherit;
  background: none;
  border: 0;
  cursor: pointer;
  color: var(--color-neutral-500);
}

.ost-link__remove:hover {
  color: var(--color-accent-300);
}

.ost-link__error {
  font-size: 11px;
  color: var(--color-accent-300);
}
</style>
