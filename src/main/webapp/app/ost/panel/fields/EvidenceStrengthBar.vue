<template>
  <div class="ost-field">
    <div class="ost-field__label ost-field__label--split">
      <span>Evidence strength</span
      ><span data-cy="ost-evidence-score">{{ strength.score === null ? 'untested' : `${strength.score}%` }}</span>
    </div>
    <div
      class="ost-evidence"
      role="meter"
      aria-label="Evidence strength"
      aria-valuemin="0"
      aria-valuemax="100"
      :aria-valuenow="strength.score ?? 0"
      data-cy="ost-evidence-bar"
      :data-score="strength.score ?? ''"
    >
      <i class="ost-evidence__fill" :style="{ width: `${strength.score ?? 0}%` }"></i>
    </div>
    <div class="ost-field__hint" data-cy="ost-evidence-note">{{ evidenceNote(strength) }}</div>
  </div>
</template>

<script setup lang="ts">
/** Solution evidence strength: derived from the assumptions below it, read-only (US-7). */
import { computed } from 'vue';

import { evidenceStrength } from '../../domain/derive';
import { useOstTreeStore } from '../../stores/ost-tree.store';
import { evidenceNote } from '../panel-format';

const props = defineProps<{ nodeKey: string }>();
const tree = useOstTreeStore();

const strength = computed(() => evidenceStrength(props.nodeKey, tree.nodes));
</script>

<style scoped>
.ost-evidence {
  position: relative;
  height: 6px;
  border-radius: 999px;
  background: var(--color-neutral-900);
  overflow: hidden;
}

.ost-evidence__fill {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  border-radius: 999px;
  background: var(--color-accent-500);
}
</style>
