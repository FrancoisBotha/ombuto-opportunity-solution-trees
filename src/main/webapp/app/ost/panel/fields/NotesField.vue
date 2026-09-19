<template>
  <div class="ost-field">
    <label class="ost-field__label ost-field__label--6" :for="id">Notes</label>
    <textarea
      :id="id"
      v-model="draft"
      class="ost-input ost-notes"
      placeholder="What did we learn?"
      :readonly="readonly"
      data-cy="ost-notes"
      @focus="focused = true"
      @blur="commit"
    ></textarea>
  </div>
</template>

<script setup lang="ts">
/**
 * Free-text notes; committed on blur (no history entry — see the API contract).
 *
 * `change` carries a `settled(ok)` callback: when the save fails (the store rolls the note back),
 * the typed text is put back in the field and kept there — never wiped — so the user can copy it
 * or blur again to retry.
 */
import { ref, watch } from 'vue';

import type { Settled } from '../panel-action';

const props = defineProps<{ value: string; readonly?: boolean }>();
const emit = defineEmits<{ change: [value: string, settled: Settled] }>();

const id = `ost-notes-${Math.random().toString(36).slice(2, 9)}`;
const draft = ref(props.value);
const focused = ref(false);
/** A draft whose save failed: held in the field until the user commits again. */
const unsaved = ref<string | null>(null);

// Follow outside changes (server response, rollback) unless the user is typing or a failed draft is held.
watch(
  () => props.value,
  v => {
    if (!focused.value && unsaved.value === null) draft.value = v;
  },
);

function commit() {
  focused.value = false;
  if (props.readonly) return;
  unsaved.value = null;
  if (draft.value === props.value) return;
  const sent = draft.value;
  emit('change', sent, ok => {
    if (ok) return;
    unsaved.value = sent;
    if (!focused.value) draft.value = sent;
  });
}
</script>

<style scoped>
.ost-field .ost-notes {
  font-size: 13px;
  min-height: 78px;
  background: transparent;
}
</style>
