import { defineStore } from 'pinia';

/**
 * Light / dark theme.
 *
 * The palette lives in content/css/theme.css: `:root` holds the DARK roles and
 * `:root[data-theme='light']` re-points them at other steps of the same ramps. Switching the theme
 * is therefore a single attribute flip on <html> — no stylesheet swap, no reload.
 *
 * Dark is the default. The OS `prefers-color-scheme` is deliberately NOT consulted; the choice is
 * the user's and is remembered per browser.
 *
 * The OST tree canvas is an artboard and stays dark in both themes, so it reads only the fixed
 * palette (`--ost-nocturne-*` and the ramps), never the roles this store flips.
 */
export type Theme = 'dark' | 'light';

export const THEME_STORAGE_KEY = 'ombuto-theme';
export const DEFAULT_THEME: Theme = 'dark';

const isTheme = (value: unknown): value is Theme => value === 'dark' || value === 'light';

/** The remembered theme, or dark when nothing valid is stored (or storage is blocked). */
export function readStoredTheme(): Theme {
  try {
    const stored = localStorage.getItem(THEME_STORAGE_KEY);
    return isTheme(stored) ? stored : DEFAULT_THEME;
  } catch {
    return DEFAULT_THEME;
  }
}

function storeTheme(theme: Theme): void {
  try {
    localStorage.setItem(THEME_STORAGE_KEY, theme);
  } catch {
    /* storage unavailable (private mode, blocked cookies) — the theme still applies for this page */
  }
}

/** Puts the theme on <html>. `data-bs-theme` lets Bootstrap 5.3 flip its own component surfaces. */
export function applyTheme(theme: Theme): void {
  const root = globalThis.document?.documentElement;
  if (!root) return;
  root.setAttribute('data-theme', theme);
  root.setAttribute('data-bs-theme', theme);
}

/** Called from main.ts before the app mounts, so the first paint is already themed. */
export function initTheme(): Theme {
  const theme = readStoredTheme();
  applyTheme(theme);
  return theme;
}

export const useThemeStore = defineStore('theme', {
  state: (): { theme: Theme } => ({ theme: readStoredTheme() }),
  getters: {
    isDark: state => state.theme === 'dark',
    /** The theme the toggle switches to — what the button's accessible name announces. */
    otherTheme: (state): Theme => (state.theme === 'dark' ? 'light' : 'dark'),
  },
  actions: {
    setTheme(theme: Theme) {
      this.theme = theme;
      storeTheme(theme);
      applyTheme(theme);
    },
    toggleTheme() {
      this.setTheme(this.otherTheme);
    },
  },
});

export type ThemeStore = ReturnType<typeof useThemeStore>;
