<template>
  <div class="ost-placeholder" data-cy="ostPlaceholder">
    <h2 id="page-heading" data-cy="OstPlaceholderHeading">
      <span>Tree builder</span>
    </h2>
    <p class="text-muted">Tree builder coming soon.</p>
    <router-link to="/trees" class="btn btn-secondary" data-cy="ostPlaceholderBack">
      <span>Back to Trees</span>
    </router-link>
  </div>
</template>

<script lang="ts">
import { defineComponent } from 'vue';

/** Temporary stand-in for /trees/:teamId until the OST tree builder shell lands. */
export default defineComponent({
  name: 'OstPlaceholder',
});
</script>
