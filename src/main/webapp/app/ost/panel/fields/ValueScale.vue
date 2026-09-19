<template>
  <div class="ost-field">
    <div class="ost-field__label ost-field__label--split ost-field__label--7">
      <span :id="labelId">Opportunity value</span><span data-cy="ost-value-label">{{ valueLabel(value || 3) }}</span>
    </div>
    <div class="ost-value" role="group" :aria-labelledby="labelId">
      <button
        v-for="step in STEPS"
        :key="step"
        type="button"
        class="ost-value__step ost-hit"
        :class="{ 'is-on': step <= value }"
        :title="valueLabel(step)"
        :aria-label="`${valueLabel(step)} (${'$'.repeat(step)})`"
        :aria-pressed="step === value"
        :disabled="readonly"
        :data-cy="`ost-value-${step}`"
        @click="pick(step)"
      >
        {{ '$'.repeat(step) }}
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
/** Opportunity value: five-step $ … $$$$$ scale with a word label (Marginal → Outsized). Commits on click. */
import { valueLabel } from '../../domain/rules';

const props = defineProps<{ value: number; readonly?: boolean }>();
const emit = defineEmits<{ change: [value: number] }>();

const STEPS = [1, 2, 3, 4, 5];
const labelId = `ost-value-${Math.random().toString(36).slice(2, 9)}`;

function pick(step: number) {
  if (!props.readonly && step !== props.value) emit('change', step);
}
</script>

<style scoped>
.ost-value {
  display: flex;
  gap: 4px;
}

.ost-value__step {
  flex: 1;
  height: 30px;
  font: inherit;
  font-size: 13px;
  letter-spacing: -0.04em;
  cursor: pointer;
  border-radius: var(--radius-sm);
  border: 1px solid var(--color-neutral-800);
  background: transparent;
  color: var(--color-neutral-500);
}

.ost-value__step.is-on {
  border-color: var(--color-accent-600);
  background: var(--color-accent-900);
  color: var(--color-accent-200);
}

.ost-value__step:disabled {
  cursor: default;
}
</style>
