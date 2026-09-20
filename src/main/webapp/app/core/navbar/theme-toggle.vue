<template>
  <button
    type="button"
    class="theme-toggle"
    data-cy="themeToggle"
    :aria-label="label"
    :title="label"
    :aria-pressed="isDark ? 'false' : 'true'"
    @click="toggleTheme()"
  >
    <!-- Dark now: offer the sun. Light now: offer the moon. -->
    <ph-sun v-if="isDark" :size="17" weight="bold" data-cy="themeToggleIconSun" aria-hidden="true" />
    <ph-moon v-else :size="17" weight="bold" data-cy="themeToggleIconMoon" aria-hidden="true" />
  </button>
</template>

<script lang="ts" src="./theme-toggle.component.ts"></script>
