<template>
  <div
    class="ost-palette-ghost"
    :class="{ 'is-over': over }"
    :style="{ left: `${x + 14}px`, top: `${y + 12}px` }"
    aria-hidden="true"
    data-cy="ost-palette-ghost"
    :data-over="over ? 'true' : 'false'"
  >
    {{ label }}<span class="ost-palette-ghost__note">{{ over ? 'Attach here' : 'Drop on a highlighted node' }}</span>
  </div>
</template>

<script setup lang="ts">
/** The chip that follows the cursor while a palette type is dragged (prototype: ghostStyle). */
defineProps<{ label: string; x: number; y: number; over: boolean }>();
</script>

<style scoped>
.ost-palette-ghost {
  position: fixed;
  z-index: 200;
  pointer-events: none;
  padding: 6px 10px;
  font-size: 12px;
  font-weight: 500;
  white-space: nowrap;
  border-radius: var(--radius-md);
  background: var(--color-surface);
  color: var(--color-text);
  border: 1px solid var(--color-neutral-700);
  box-shadow: var(--shadow-md);
  opacity: 0.96;
}
.ost-palette-ghost.is-over {
  border-color: var(--color-accent-400);
}

.ost-palette-ghost__note {
  margin-left: 7px;
  color: var(--color-neutral-400);
}
</style>
