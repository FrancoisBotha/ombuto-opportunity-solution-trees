import { afterEach, describe, expect, it, vi } from 'vitest';

import { type VueWrapper, mount } from '@vue/test-utils';
import { nextTick } from 'vue';

import type { MyTeamDTO } from '../ost.model';

import OstTeamCombo from './OstTeamCombo.vue';

const TEAMS: MyTeamDTO[] = Array.from({ length: 28 }, (_, i) => ({
  id: i + 1,
  name: `Team ${i + 1}`,
  role: 'EDITOR',
  memberCount: 2,
  productCount: i % 3,
}));

describe('OstTeamCombo', () => {
  let wrapper: VueWrapper | null = null;
  let outside: HTMLButtonElement | null = null;

  afterEach(() => {
    wrapper?.unmount();
    wrapper = null;
    outside?.remove();
    outside = null;
    vi.restoreAllMocks();
  });

  async function mountCombo() {
    outside = document.createElement('button');
    document.body.appendChild(outside);
    wrapper = mount(OstTeamCombo, { props: { teams: TEAMS, currentId: 1 }, attachTo: document.body });
    return wrapper;
  }

  const menu = (w: VueWrapper) => w.find('[data-cy="ostTeamComboMenu"]');
  const trigger = (w: VueWrapper) => w.find('[data-cy="ostTeamComboButton"]');

  /** Gives every option a 40px row in a 200px-high menu (happy-dom has no layout). */
  function fakeLayout(w: VueWrapper) {
    const list = menu(w).element as HTMLElement;
    Object.defineProperty(list, 'clientHeight', { configurable: true, value: 200 });
    w.findAll('[role="option"]').forEach((option, i) => {
      Object.defineProperty(option.element, 'offsetTop', { configurable: true, value: 5 + i * 40 });
      Object.defineProperty(option.element, 'offsetHeight', { configurable: true, value: 40 });
    });
    return list;
  }

  it('walks to the last of many teams with ArrowDown, scrolling only the menu', async () => {
    const w = await mountCombo();
    const focus = vi.spyOn(HTMLElement.prototype, 'focus');
    await trigger(w).trigger('keydown', { key: 'ArrowDown' });
    await nextTick();
    const list = fakeLayout(w);
    const options = w.findAll('[role="option"]');
    expect(options).toHaveLength(28);
    expect(document.activeElement).toBe(options[0].element);

    for (let i = 1; i < options.length; i++) await menu(w).trigger('keydown', { key: 'ArrowDown' });
    expect(document.activeElement).toBe(options[27].element);
    // Every focus call opts out of the browser's scroll-into-view (which would scroll the shell).
    expect(focus.mock.calls.every(([opts]) => (opts as FocusOptions | undefined)?.preventScroll === true)).toBe(true);
    // The last option (top 5 + 27 * 40, 40 high) sits at the bottom of the 200px menu viewport.
    expect(list.scrollTop).toBe(5 + 27 * 40 + 40 - 200);

    await menu(w).trigger('keydown', { key: 'Home' });
    expect(document.activeElement).toBe(options[0].element);
    expect(list.scrollTop).toBe(5);
    await menu(w).trigger('keydown', { key: 'End' });
    expect(document.activeElement).toBe(options[27].element);
  });

  it('closes on Escape from the menu (focus back on the trigger) and from the trigger itself', async () => {
    const w = await mountCombo();
    await trigger(w).trigger('keydown', { key: 'ArrowDown' });
    await nextTick();
    await menu(w).trigger('keydown', { key: 'Escape' });
    expect(menu(w).exists()).toBe(false);
    expect(document.activeElement).toBe(trigger(w).element);

    await trigger(w).trigger('click');
    expect(menu(w).exists()).toBe(true);
    await trigger(w).trigger('keydown', { key: 'Escape' });
    expect(menu(w).exists()).toBe(false);
  });

  it('closes when focus leaves the combo, not when it moves inside it', async () => {
    const w = await mountCombo();
    await trigger(w).trigger('click');
    const first = w.find('[role="option"]');
    await trigger(w).trigger('focusout', { relatedTarget: first.element });
    expect(menu(w).exists()).toBe(true);

    await first.trigger('focusout', { relatedTarget: outside });
    expect(menu(w).exists()).toBe(false);

    await trigger(w).trigger('click');
    await trigger(w).trigger('focusout', { relatedTarget: null });
    expect(menu(w).exists()).toBe(false);
  });

  it('stays open while an option is being clicked in a browser that does not focus buttons', async () => {
    const w = await mountCombo();
    await trigger(w).trigger('click');
    const option = w.find('[data-cy="ostTeamOption-5"]');
    await option.trigger('pointerdown');
    await trigger(w).trigger('focusout', { relatedTarget: null });
    expect(menu(w).exists()).toBe(true);
    document.dispatchEvent(new Event('pointerup'));
    await option.trigger('click');
    expect(w.emitted('select')).toEqual([[5]]);
    expect(menu(w).exists()).toBe(false);
  });

  it('closes on a pointer press outside', async () => {
    const w = await mountCombo();
    await trigger(w).trigger('click');
    outside!.dispatchEvent(new Event('pointerdown', { bubbles: true }));
    await nextTick();
    expect(menu(w).exists()).toBe(false);
  });
});
