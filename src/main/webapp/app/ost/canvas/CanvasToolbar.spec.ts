import { beforeEach, describe, expect, it } from 'vitest';

import { mount } from '@vue/test-utils';
import { createPinia, setActivePinia } from 'pinia';

import CanvasToolbar from './CanvasToolbar.vue';

/** Live-region semantics on the element or any ancestor. */
function liveAncestor(el: Element | null): string | null {
  for (let at = el; at; at = at.parentElement) {
    const live = at.getAttribute('aria-live');
    const role = at.getAttribute('role');
    if ((live && live !== 'off') || ['status', 'alert', 'log', 'timer', 'marquee'].includes(role ?? '')) {
      return at.outerHTML.slice(0, 80);
    }
  }
  return null;
}

describe('CanvasToolbar', () => {
  beforeEach(() => setActivePinia(createPinia()));

  const mountToolbar = (zoom = 0.82) => mount(CanvasToolbar, { props: { zoom }, attachTo: document.body });

  it('shows the zoom level, which is not a live region (every wheel notch would be announced)', () => {
    const w = mountToolbar();
    const level = w.get('[data-cy="ost-zoom-level"]');
    expect(level.text()).toBe('82%');
    expect(liveAncestor(level.element)).toBeNull();
    w.unmount();
  });

  it('the product combo carets and check use the Phosphor regular weight (prototype paths)', async () => {
    const w = mountToolbar();
    const caret = w.get('[data-cy="ost-product-combo"] svg path').attributes('d') ?? '';
    expect(caret).toMatch(/a8,8/); // regular: 8-unit arcs (bold: 12)
    await w.get('[data-cy="ost-product-combo"]').trigger('click');
    const check = w.get('[data-cy="ost-product-option-all"] svg path').attributes('d') ?? '';
    expect(check).toMatch(/a8,8/);
    w.unmount();
  });
});

describe('CanvasToolbar keyboard jump to a node (C17)', () => {
  it('Enter / Shift+Enter in the search box step through the matches (types shown, in scope) and announce them', async () => {
    const { setupStores } = await import('../panel/panel.test-util');
    const ctx = await setupStores();
    const w = mount(CanvasToolbar, { props: { zoom: 1 }, attachTo: document.body, global: { plugins: [ctx.pinia] } });
    const search = w.get('[data-cy="ost-search"]');
    await search.setValue('in');
    expect(search.attributes('aria-describedby')).toBeTruthy();

    await search.trigger('keydown', { key: 'Enter' });
    await search.trigger('keydown', { key: 'Enter' });
    await search.trigger('keydown', { key: 'Enter' });
    await search.trigger('keydown', { key: 'Enter', shiftKey: true });
    const order = ['outcome-1', 'opportunity-1', 'solution-1'];
    expect(w.emitted('jump')).toEqual([[order[0]], [order[1]], [order[2]], [order[1]]]);
    expect(w.get('[data-cy="ost-search-status"]').text()).toBe('Match 2 of 3: Hard to find people');

    // Hidden types are skipped; a new query starts over.
    ctx.ui.toggleType('outcome');
    await search.trigger('keydown', { key: 'Enter' });
    expect(w.emitted('jump')!.at(-1)).toEqual(['opportunity-1']);

    await search.setValue('nothing like this');
    await search.trigger('keydown', { key: 'Enter' });
    expect(w.emitted('jump')).toHaveLength(5);
    expect(w.get('[data-cy="ost-search-status"]').text()).toBe('No matching nodes on the canvas');
    w.unmount();
  });
});
