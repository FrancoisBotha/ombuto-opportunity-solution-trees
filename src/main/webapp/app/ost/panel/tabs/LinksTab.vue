<template>
  <div v-if="node" class="ost-tab ost-links" data-cy="ostTab-links">
    <LinkRow
      v-for="link in node.links"
      :key="link.id ?? link.name"
      :link="link"
      :readonly="readonly"
      @save="save(link.id, $event)"
      @remove="remove(link.id)"
    />
    <p v-if="!node.links.length" class="ost-links__empty" data-cy="ost-links-empty">No links yet.</p>

    <div v-if="!readonly && restore.length" class="ost-links__restore">
      <span class="ost-links__restore-label">Restore a default link</span>
      <button
        v-for="option in restore"
        :key="option.name"
        type="button"
        class="ost-links__restore-btn"
        :disabled="option.present || busy"
        :title="option.present ? `${option.name} already linked` : `Add a ${option.name} link`"
        :data-cy="`ost-link-restore-${slug(option.name)}`"
        @click="restoreOne(option)"
      >
        <component :is="restoreIcon(option.name)" :size="14" aria-hidden="true" />
        <span>{{ option.label }}</span>
      </button>
    </div>

    <form v-if="adding" class="ost-links__form" data-cy="ost-link-form" novalidate @submit.prevent="submit">
      <input
        ref="nameInput"
        v-model="draft.name"
        class="ost-input ost-links__input"
        aria-label="New link name"
        placeholder="Name"
        maxlength="100"
        data-cy="ost-link-new-name"
        @keydown.esc.prevent="closeForm"
      />
      <input
        v-model="draft.url"
        class="ost-input ost-links__input"
        aria-label="New link URL"
        placeholder="https://"
        maxlength="2000"
        data-cy="ost-link-new-url"
        @keydown.esc.prevent="closeForm"
      />
      <div v-if="formError" class="ost-links__error" role="alert" data-cy="ost-link-error">{{ formError }}</div>
      <div class="ost-links__form-actions">
        <button type="button" class="ost-btn ost-links__small" data-cy="ost-link-new-cancel" @click="closeForm">Cancel</button>
        <button type="submit" class="ost-btn ost-btn--primary ost-links__small" :disabled="busy" data-cy="ost-link-new-save">Add</button>
      </div>
    </form>
    <button v-else-if="!readonly" type="button" class="ost-btn ost-links__add" data-cy="ost-link-add" @click="openForm">+ Add link</button>
  </div>
</template>

<script setup lang="ts">
/**
 * Links tab: name + URL rows (inline edit, remove, open in a new tab), "Add link" with URL
 * validation, and one-click restore buttons for the type's default links (disabled when present).
 */
import { computed, nextTick, reactive, ref } from 'vue';

import { PhFileText, PhFlag, PhLightning, PhLink } from '@phosphor-icons/vue';

import { useOstTreeStore } from '../../stores/ost-tree.store';
import LinkRow from '../fields/LinkRow.vue';
import { usePanelAction } from '../panel-action';
import { type RestoreOption, isValidLinkUrl, restoreOptions, slug } from '../panel-format';

const props = defineProps<{ nodeKey: string }>();
const tree = useOstTreeStore();
const { run } = usePanelAction();

const node = computed(() => tree.byId(props.nodeKey));
const readonly = computed(() => !tree.canEdit);
const restore = computed(() => (node.value ? restoreOptions(node.value) : []));

const adding = ref(false);
const busy = ref(false);
const draft = reactive({ name: '', url: 'https://' });
const formError = ref<string | null>(null);
const nameInput = ref<HTMLInputElement | null>(null);

function restoreIcon(name: string) {
  if (/confluence|space/i.test(name)) return PhFileText;
  if (/initiative/i.test(name)) return PhFlag;
  if (/epic|ticket/i.test(name)) return PhLightning;
  return PhLink;
}

function save(linkId: number | undefined, patch: { name?: string; url?: string }) {
  if (readonly.value || linkId === undefined) return;
  void run(() => tree.updateLink(props.nodeKey, linkId, patch));
}

function remove(linkId: number | undefined) {
  if (readonly.value || linkId === undefined) return;
  void run(() => tree.removeLink(props.nodeKey, linkId));
}

async function restoreOne(option: RestoreOption) {
  if (readonly.value || option.present || busy.value) return;
  busy.value = true;
  try {
    await run(() => tree.addLink(props.nodeKey, { name: option.name, url: option.url }));
  } finally {
    busy.value = false;
  }
}

async function openForm() {
  draft.name = '';
  draft.url = 'https://';
  formError.value = null;
  adding.value = true;
  await nextTick();
  nameInput.value?.focus();
}

function closeForm() {
  adding.value = false;
  formError.value = null;
}

async function submit() {
  if (readonly.value || busy.value) return;
  const name = draft.name.trim();
  const url = draft.url.trim();
  if (!name) {
    formError.value = 'Give the link a name.';
    return;
  }
  if (!isValidLinkUrl(url)) {
    formError.value = 'Links must start with http:// or https://.';
    return;
  }
  formError.value = null;
  busy.value = true;
  try {
    const ok = await run(() => tree.addLink(props.nodeKey, { name, url }));
    if (ok) adding.value = false;
  } finally {
    busy.value = false;
  }
}
</script>

<style scoped>
.ost-links {
  display: flex;
  flex-direction: column;
  gap: 9px;
}

.ost-links__empty {
  margin: 0;
  font-size: 12px;
  color: var(--color-neutral-500);
}

.ost-links__restore {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  margin-top: 2px;
}

.ost-links__restore-label {
  width: 100%;
  margin-bottom: 1px;
  font-size: 10px;
  letter-spacing: 0.09em;
  text-transform: uppercase;
  color: var(--color-neutral-500);
}

.ost-links__restore-btn {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 5px 9px;
  font: inherit;
  font-size: 11.5px;
  border-radius: var(--radius-md);
  cursor: pointer;
  background: transparent;
  border: 1px solid var(--color-accent-600);
  color: var(--color-accent-300);
}

.ost-links__restore-btn:disabled {
  cursor: default;
  opacity: 0.45;
  border-color: var(--color-neutral-800);
  color: var(--color-neutral-500);
}

.ost-links__form {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 9px 10px;
  border: 1px dashed var(--color-accent-600);
  border-radius: var(--radius-md);
}

.ost-links__form .ost-links__input {
  min-height: 28px;
  padding: 4px 8px;
  font-size: 12.5px;
}

.ost-links__form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 6px;
}

.ost-links .ost-links__small {
  height: 28px;
  font-size: 12px;
}

.ost-links .ost-links__add {
  height: 30px;
  font-size: 12px;
}

.ost-links__error {
  font-size: 11px;
  color: var(--color-accent-300);
}
</style>
