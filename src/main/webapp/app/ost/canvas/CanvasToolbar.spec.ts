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
