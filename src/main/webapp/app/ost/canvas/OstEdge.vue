<template>
  <path
    class="ost-edge"
    :class="`ost-edge--${data.kind}`"
    :d="data.d"
    fill="none"
    :data-cy="`ost-edge-${target}`"
    :data-edge-kind="data.kind"
  />
</template>

<script setup lang="ts">
/**
 * Parent → child edge. The path is NOT computed from Vue Flow's handle positions: it is the
 * orthogonal three-segment path from the tidy layout (domain/layout.ts edgePath), parent
 * bottom-centre → child top-centre, so it always matches the derived layout.
 * As in the prototype: the spine (into outcomes and opportunities) is 1.6px accent-600, edges into
 * solutions 1.1px solid, into assumptions and evidence 1.1px dashed 4 4 (evidence in neutral-700).
 */
import type { OstEdgeData } from './canvas-model';

// Vue Flow passes every edge prop (positions, node objects, …); only these are used — keep the
// rest off the <path>.
defineOptions({ inheritAttrs: false });
defineProps<{ id: string; source: string; target: string; data: OstEdgeData }>();
</script>

<style scoped>
.ost-edge {
  stroke: var(--color-accent-600);
  stroke-width: 1.6px;
  transition: d 0.18s ease;
}
.ost-edge--solution {
  stroke-width: 1.1px;
}
.ost-edge--test {
  stroke-width: 1.1px;
  stroke-dasharray: 4 4;
}
.ost-edge--evidence {
  stroke: var(--color-neutral-700);
  stroke-width: 1.1px;
  stroke-dasharray: 4 4;
}
</style>
