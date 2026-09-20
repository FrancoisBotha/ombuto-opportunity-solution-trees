import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { createPinia, setActivePinia } from 'pinia';

import { DEFAULT_THEME, THEME_STORAGE_KEY, type Theme, applyTheme, initTheme, readStoredTheme, useThemeStore } from './theme-store';

const attributes = () => ({
  theme: document.documentElement.getAttribute('data-theme'),
  bootstrap: document.documentElement.getAttribute('data-bs-theme'),
});

describe('theme store', () => {
  beforeEach(() => {
    localStorage.clear();
    document.documentElement.removeAttribute('data-theme');
    document.documentElement.removeAttribute('data-bs-theme');
    setActivePinia(createPinia());
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('defaults to dark when nothing is stored', () => {
    expect(DEFAULT_THEME).toBe('dark');
    expect(readStoredTheme()).toBe('dark');
    expect(useThemeStore().theme).toBe('dark');
    expect(useThemeStore().isDark).toBe(true);
  });

  it('never follows the OS preference', () => {
    // A light-preferring OS must not win over the app default.
    vi.spyOn(globalThis, 'matchMedia').mockReturnValue({ matches: true } as MediaQueryList);
    expect(readStoredTheme()).toBe('dark');
  });

  it('reads a remembered light theme', () => {
    localStorage.setItem(THEME_STORAGE_KEY, 'light');
    expect(readStoredTheme()).toBe('light');
    expect(useThemeStore().theme).toBe('light');
    expect(useThemeStore().isDark).toBe(false);
  });

  it('falls back to dark on a corrupt stored value', () => {
    for (const corrupt of ['', 'DARK', 'nocturne', '{"theme":"light"}', 'null']) {
      localStorage.setItem(THEME_STORAGE_KEY, corrupt);
      expect(readStoredTheme()).toBe(DEFAULT_THEME);
    }
  });

  it('falls back to dark when localStorage throws on read', () => {
    localStorage.setItem(THEME_STORAGE_KEY, 'light');
    vi.spyOn(globalThis.localStorage, 'getItem').mockImplementation(() => {
      throw new DOMException('blocked');
    });
    expect(readStoredTheme()).toBe('dark');
  });

  it('toggles between dark and light, and back', () => {
    const store = useThemeStore();
    expect(store.otherTheme).toBe('light');

    store.toggleTheme();
    expect(store.theme).toBe('light');
    expect(store.isDark).toBe(false);
    expect(store.otherTheme).toBe('dark');

    store.toggleTheme();
    expect(store.theme).toBe('dark');
  });

  it('persists the choice under a stable key', () => {
    const store = useThemeStore();
    store.toggleTheme();
    expect(localStorage.getItem(THEME_STORAGE_KEY)).toBe('light');
    store.setTheme('dark');
    expect(localStorage.getItem(THEME_STORAGE_KEY)).toBe('dark');
  });

  it('still applies the theme when localStorage refuses the write', () => {
    const store = useThemeStore();
    vi.spyOn(globalThis.localStorage, 'setItem').mockImplementation(() => {
      throw new DOMException('quota');
    });
    expect(() => store.toggleTheme()).not.toThrow();
    expect(store.theme).toBe('light');
    expect(attributes()).toEqual({ theme: 'light', bootstrap: 'light' });
  });

  it('applies the theme to <html> as data-theme and data-bs-theme', () => {
    applyTheme('light');
    expect(attributes()).toEqual({ theme: 'light', bootstrap: 'light' });
    applyTheme('dark');
    expect(attributes()).toEqual({ theme: 'dark', bootstrap: 'dark' });
  });

  it('initTheme applies dark on a first visit and the stored theme afterwards', () => {
    expect(initTheme()).toBe('dark');
    expect(attributes()).toEqual({ theme: 'dark', bootstrap: 'dark' });

    localStorage.setItem(THEME_STORAGE_KEY, 'light' satisfies Theme);
    expect(initTheme()).toBe('light');
    expect(attributes()).toEqual({ theme: 'light', bootstrap: 'light' });
  });

  it('setTheme moves the <html> attributes with the state', () => {
    const store = useThemeStore();
    store.setTheme('light');
    expect(attributes()).toEqual({ theme: 'light', bootstrap: 'light' });
    store.setTheme('dark');
    expect(attributes()).toEqual({ theme: 'dark', bootstrap: 'dark' });
  });
});
