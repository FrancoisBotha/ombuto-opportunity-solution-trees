<template>
  <div class="ost-field">
    <div class="ost-field__label ost-field__label--split ost-field__label--6">
      <span :id="labelId">Confidence in this assumption</span><span data-cy="ost-confidence-label">{{ confidenceLabel(value) }}</span>
    </div>
    <div class="ost-confidence" role="group" :aria-labelledby="labelId" data-cy="ost-confidence" :data-value="value">
      <button
        v-for="step in CONFIDENCE_STEPS"
        :key="step"
        type="button"
        class="ost-confidence__step"
        :class="{ 'is-on': value >= step }"
        :title="`${step}%`"
        :aria-label="`${step}%`"
        :aria-pressed="value === step"
        :disabled="readonly"
        :data-cy="`ost-confidence-${step}`"
        @click="pick(step)"
      ></button>
    </div>
    <div class="ost-field__hint ost-field__hint--tight">
      How strong is the evidence that this assumption holds? Move it as test results come back.
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * Assumption confidence (0–100) as the prototype's five-step bar: 20 · 40 · 60 · 80 · 100.
 * Steps up to the value are filled; only the step equal to the value is announced as pressed.
 */
import { CONFIDENCE_STEPS, confidenceLabel } from '../panel-format';

const props = defineProps<{ value: number; readonly?: boolean }>();
const emit = defineEmits<{ change: [value: number] }>();

const labelId = `ost-conf-${Math.random().toString(36).slice(2, 9)}`;

function pick(step: number) {
  if (!props.readonly && step !== props.value) emit('change', step);
}
</script>

<style scoped>
.ost-confidence {
  display: flex;
  gap: 3px;
}

.ost-confidence__step {
  flex: 1;
  height: 22px;
  padding: 0;
  cursor: pointer;
  border-radius: var(--radius-sm);
  border: 1px solid var(--color-neutral-800);
  background: transparent;
}

.ost-confidence__step.is-on {
  background: var(--color-accent-600);
}

.ost-confidence__step:disabled {
  cursor: default;
}
</style>
