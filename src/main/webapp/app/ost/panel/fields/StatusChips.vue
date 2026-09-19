<template>
  <div class="ost-field">
    <div :id="labelId" class="ost-field__label ost-field__label--6">Status</div>
    <div class="ost-chips" role="group" :aria-labelledby="labelId">
      <button
        v-for="option in options"
        :key="option"
        type="button"
        class="ost-chip ost-hit"
        :class="{ 'is-on': option === status }"
        :aria-pressed="option === status"
        :disabled="readonly"
        :data-cy="`ost-status-${option}`"
        @click="pick(option)"
      >
        {{ option }}
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
/** Status chips for opportunities, solutions and assumptions (vocabulary from rules.STATUS). */
const props = defineProps<{ status: string; options: string[]; readonly?: boolean }>();
const emit = defineEmits<{ change: [status: string] }>();

const labelId = `ost-status-${Math.random().toString(36).slice(2, 9)}`;

function pick(option: string) {
  if (!props.readonly && option !== props.status) emit('change', option);
}
</script>

<style scoped>
.ost-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.ost-chip {
  font: inherit;
  font-family: var(--font-heading);
  font-size: 11px;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  padding: 5px 9px;
  cursor: pointer;
  border-radius: var(--radius-sm);
  border: 1px solid var(--color-neutral-800);
  background: transparent;
  color: var(--color-neutral-400);
}

.ost-chip:hover:not(:disabled) {
  border-color: var(--color-accent-600);
}

.ost-chip.is-on {
  border-color: var(--color-accent-600);
  background: var(--color-accent-900);
  color: var(--color-accent-200);
}

.ost-chip:disabled {
  cursor: default;
}
</style>
