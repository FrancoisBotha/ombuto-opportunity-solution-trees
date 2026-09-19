<template>
  <div class="ost-field">
    <label class="ost-field__label" :for="id">Notes</label>
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
/** Free-text notes; committed on blur (no history entry — see the API contract). */
import { ref, watch } from 'vue';

const props = defineProps<{ value: string; readonly?: boolean }>();
const emit = defineEmits<{ change: [value: string] }>();

const id = `ost-notes-${Math.random().toString(36).slice(2, 9)}`;
const draft = ref(props.value);
const focused = ref(false);

// Follow outside changes (server response, rollback) unless the user is typing.
watch(
  () => props.value,
  v => {
    if (!focused.value) draft.value = v;
  },
);

function commit() {
  focused.value = false;
  if (props.readonly) return;
  if (draft.value !== props.value) emit('change', draft.value);
}
</script>

<style scoped>
.ost-field .ost-notes {
  font-size: 13px;
  min-height: 78px;
  background: transparent;
}
</style>
