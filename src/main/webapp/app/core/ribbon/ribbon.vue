<template>
  <span
    v-if="ribbonEnabled"
    class="ribbon"
    data-cy="ribbon"
    :title="`Running on the ${ribbonEnv} profile — not production`"
    :aria-label="`Environment: ${ribbonEnv}`"
    >{{ ribbonEnv }}</span
  >
</template>

<script lang="ts" src="./ribbon.component.ts"></script>

<style scoped>
/* ==========================================================================
Non-production environment marker.

It used to be a red diagonal banner pinned to the top-left of the viewport, where it covered the
sidebar's first rows (and the new NAVIGATION label). It is now a pill in the navbar, beside the
version badge: still unmissable, readable in both themes, and never over the navigation.
Colours come from the shared palette (content/css/theme.css).
========================================================================== */
.ribbon {
  display: inline-flex;
  align-items: center;
  height: 1.15rem;
  padding: 0 0.45rem;
  margin-left: 0.4rem;
  border: 1px solid var(--ost-warn-border);
  border-radius: 999px;
  background-color: var(--ost-warn-bg);
  color: var(--ost-warn-fg);
  font-size: 0.62rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  white-space: nowrap;
  cursor: default;
}
</style>
