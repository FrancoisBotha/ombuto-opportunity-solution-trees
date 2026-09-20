import { beforeEach, describe, expect, it } from 'vitest';

import { createPinia, setActivePinia } from 'pinia';
import { mount } from '@vue/test-utils';

import { THEME_STORAGE_KEY, useThemeStore } from '@/shared/config/store/theme-store';

import ThemeToggle from './theme-toggle.vue';

const mountToggle = () => mount(ThemeToggle);

describe('ThemeToggle', () => {
  beforeEach(() => {
    localStorage.clear();
    document.documentElement.removeAttribute('data-theme');
    document.documentElement.removeAttribute('data-bs-theme');
    setActivePinia(createPinia());
  });

  it('offers light while dark is on: sun icon, name states the target', () => {
    const wrapper = mountToggle();
    const button = wrapper.get('[data-cy="themeToggle"]');

    expect(button.attributes('aria-label')).toBe('Switch to light theme');
    expect(button.attributes('aria-pressed')).toBe('false');
    expect(wrapper.find('[data-cy="themeToggleIconSun"]').exists()).toBe(true);
    expect(wrapper.find('[data-cy="themeToggleIconMoon"]').exists()).toBe(false);
  });

  it('swaps the icon and the accessible name when clicked', async () => {
    const wrapper = mountToggle();
    const button = wrapper.get('[data-cy="themeToggle"]');

    await button.trigger('click');

    expect(useThemeStore().theme).toBe('light');
    expect(button.attributes('aria-label')).toBe('Switch to dark theme');
    expect(button.attributes('aria-pressed')).toBe('true');
    expect(wrapper.find('[data-cy="themeToggleIconMoon"]').exists()).toBe(true);
    expect(wrapper.find('[data-cy="themeToggleIconSun"]').exists()).toBe(false);
    expect(document.documentElement.getAttribute('data-theme')).toBe('light');
    expect(localStorage.getItem(THEME_STORAGE_KEY)).toBe('light');

    await button.trigger('click');

    expect(useThemeStore().theme).toBe('dark');
    expect(button.attributes('aria-label')).toBe('Switch to light theme');
    expect(wrapper.find('[data-cy="themeToggleIconSun"]').exists()).toBe(true);
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark');
  });

  it('starts from the remembered theme', () => {
    localStorage.setItem(THEME_STORAGE_KEY, 'light');
    const wrapper = mountToggle();

    expect(wrapper.get('[data-cy="themeToggle"]').attributes('aria-label')).toBe('Switch to dark theme');
    expect(wrapper.find('[data-cy="themeToggleIconMoon"]').exists()).toBe(true);
  });

  it('is a real button with a title matching its accessible name', () => {
    const wrapper = mountToggle();
    const button = wrapper.get('[data-cy="themeToggle"]');
    expect(button.element.tagName).toBe('BUTTON');
    expect(button.attributes('type')).toBe('button');
    expect(button.attributes('title')).toBe(button.attributes('aria-label'));
  });
});
