import { computed, defineComponent } from 'vue';

import { PhMoon, PhSun } from '@phosphor-icons/vue';

import { useThemeStore } from '@/shared/config/store/theme-store';

export default defineComponent({
  name: 'ThemeToggle',
  components: { PhMoon, PhSun },
  setup() {
    const themeStore = useThemeStore();

    const isDark = computed(() => themeStore.isDark);
    // States the TARGET, so a screen reader announces what pressing the button will do.
    const label = computed(() => `Switch to ${themeStore.otherTheme} theme`);

    return {
      isDark,
      label,
      toggleTheme: () => themeStore.toggleTheme(),
    };
  },
});
