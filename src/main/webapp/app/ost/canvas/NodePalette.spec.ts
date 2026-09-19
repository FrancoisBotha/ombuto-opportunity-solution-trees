import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { type VueWrapper, mount } from '@vue/test-utils';
import { createPinia, setActivePinia } from 'pinia';

import type { NodeType } from '../domain/types';
import { useOstUiStore } from '../stores/ost-ui.store';

import NodePalette from './NodePalette.vue';

/** happy-dom has no PointerEvent constructor with coordinates everywhere; build one that has them. */
function pointer(type: string, x: number, y: number, extra: Record<string, unknown> = {}) {
  const event = new MouseEvent(type, { bubbles: true, cancelable: true, clientX: x, clientY: y, button: 0 });
  Object.defineProperty(event, 'pointerId', { value: 1 });
  for (const [k, v] of Object.entries(extra)) Object.defineProperty(event, k, { value: v });
  return event;
}

describe('NodePalette', () => {
  let wrapper: VueWrapper;
  let ui: ReturnType<typeof useOstUiStore>;
  let resolveTarget: ReturnType<typeof vi.fn<(type: NodeType, x: number, y: number) => string | null>>;

  const tool = (type: NodeType) => wrapper.get(`[data-cy="ost-palette-${type}"]`);

  /** Press on a type at (10,10), then move/release on window. */
  async function press(type: NodeType) {
    tool(type).element.dispatchEvent(pointer('pointerdown', 10, 10));
    await wrapper.vm.$nextTick();
  }
  async function moveTo(x: number, y: number) {
    window.dispatchEvent(pointer('pointermove', x, y));
    await wrapper.vm.$nextTick();
  }
  async function release(x: number, y: number) {
    window.dispatchEvent(pointer('pointerup', x, y));
    await wrapper.vm.$nextTick();
  }

  beforeEach(() => {
    setActivePinia(createPinia());
    ui = useOstUiStore();
    resolveTarget = vi.fn((type: NodeType, x: number) => (x > 300 && type !== 'outcome' ? 'opportunity-1' : null));
    wrapper = mount(NodePalette, { props: { resolveTarget }, attachTo: document.body });
  });

  afterEach(() => {
    wrapper.unmount();
    vi.useRealTimers();
  });

  it('lists every creatable type with its hint', () => {
    const labels = wrapper.findAll('.ost-palette__label').map(l => l.text());
    expect(labels).toEqual(['Outcome', 'Opportunity', 'Solution', 'Assumption', 'Evidence']);
    expect(tool('evidence').text()).toContain('Customer evidence goes under an opportunity; test results under an assumption.');
    expect(wrapper.find('[data-cy="ost-palette-product"]').exists()).toBe(false);
  });

  describe('click to arm', () => {
    it('a click arms the type; clicking it again disarms; another type switches', async () => {
      await tool('solution').trigger('click');
      expect(ui.tool).toBe('solution');
      expect(tool('solution').attributes('aria-pressed')).toBe('true');
      expect(wrapper.get('[data-cy="ost-palette-hint"]').text()).toBe('Click a highlighted node to attach the new solution.');
      await tool('evidence').trigger('click');
      expect(ui.tool).toBe('evidence');
      await tool('evidence').trigger('click');
      expect(ui.tool).toBeNull();
      expect(wrapper.get('[data-cy="ost-palette-hint"]').text()).toContain('Drag a type onto the node it belongs under');
    });

    it('Escape disarms', async () => {
      await tool('assumption').trigger('click');
      window.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
      expect(ui.tool).toBeNull();
    });

    it('an Escape something else already handled (e.g. a dialog) does not disarm', async () => {
      await tool('assumption').trigger('click');
      const event = new KeyboardEvent('keydown', { key: 'Escape', cancelable: true });
      event.preventDefault();
      window.dispatchEvent(event);
      expect(ui.tool).toBe('assumption');
    });

    it('a press without movement is a click, not a drag', async () => {
      await press('outcome');
      await moveTo(12, 11); // under the 5px threshold
      expect(ui.paletteDrag).toBeNull();
      await release(12, 11);
      await tool('outcome').trigger('click');
      expect(ui.tool).toBe('outcome');
      expect(wrapper.emitted('attach')).toBeUndefined();
    });
  });

  describe('drag to attach', () => {
    it('shows the ghost chip, tracks the legal target and attaches on drop', async () => {
      await press('solution');
      await moveTo(60, 40);
      expect(ui.paletteDrag).toEqual({ type: 'solution', x: 60, y: 40 });
      const ghost = wrapper.get('[data-cy="ost-palette-ghost"]');
      expect(ghost.text()).toContain('Solution');
      expect(ghost.text()).toContain('Drop on a highlighted node');
      expect(ghost.attributes('style')).toContain('left: 74px');
      expect(ghost.attributes('style')).toContain('top: 52px');

      await moveTo(400, 200);
      expect(ui.dropTargetId).toBe('opportunity-1');
      expect(wrapper.get('[data-cy="ost-palette-ghost"]').text()).toContain('Attach here');

      await release(400, 200);
      expect(wrapper.emitted('attach')).toEqual([['opportunity-1', 'solution']]);
      expect(ui.paletteDrag).toBeNull();
      expect(ui.dropTargetId).toBeNull();
      expect(wrapper.find('[data-cy="ost-palette-ghost"]').exists()).toBe(false);
    });

    it('a drop away from a legal node does nothing', async () => {
      await press('outcome');
      await moveTo(400, 200); // resolveTarget refuses outcomes here
      expect(ui.dropTargetId).toBeNull();
      await release(400, 200);
      expect(wrapper.emitted('attach')).toBeUndefined();
      expect(ui.paletteDrag).toBeNull();
    });

    it('the click that ends a drag does not arm the type, and a drag disarms an armed one', async () => {
      await tool('evidence').trigger('click');
      expect(ui.tool).toBe('evidence');
      await press('solution');
      await moveTo(80, 80);
      await release(80, 80);
      await tool('solution').trigger('click');
      expect(ui.tool).toBeNull();
    });

    it.each([
      ['pointercancel', () => window.dispatchEvent(pointer('pointercancel', 0, 0))],
      ['window blur', () => window.dispatchEvent(new Event('blur'))],
      ['Escape', () => window.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }))],
    ])('never gets stuck: %s ends the drag without attaching', async (_label, end) => {
      await press('solution');
      await moveTo(400, 200);
      expect(ui.paletteDrag).not.toBeNull();
      end();
      await wrapper.vm.$nextTick();
      expect(ui.paletteDrag).toBeNull();
      expect(ui.dropTargetId).toBeNull();
      // Later pointer events are ignored: nothing is attached.
      await moveTo(420, 220);
      await release(420, 220);
      expect(ui.paletteDrag).toBeNull();
      expect(wrapper.emitted('attach')).toBeUndefined();
    });

    it('unmounting mid-drag clears the drag state', async () => {
      await press('solution');
      await moveTo(400, 200);
      wrapper.unmount();
      expect(ui.paletteDrag).toBeNull();
      expect(ui.dropTargetId).toBeNull();
      wrapper = mount(NodePalette, { props: { resolveTarget }, attachTo: document.body });
    });
  });

  describe('collapse', () => {
    it('collapses to the rail and reopens', async () => {
      expect(wrapper.get('[data-cy="ost-palette"]').attributes('data-state')).toBe('open');
      await tool('solution').trigger('click');
      await wrapper.get('[data-cy="ost-palette-toggle"]').trigger('click');
      expect(ui.leftOpen).toBe(false);
      expect(ui.tool).toBeNull(); // hiding the palette disarms
      expect(wrapper.get('[data-cy="ost-palette"]').attributes('data-state')).toBe('closed');
      expect(wrapper.find('[data-cy="ost-palette-solution"]').exists()).toBe(false);
      expect(wrapper.text()).toContain('Palette');
      await wrapper.get('[data-cy="ost-palette-toggle"]').trigger('click');
      expect(ui.leftOpen).toBe(true);
      expect(wrapper.find('[data-cy="ost-palette-solution"]').exists()).toBe(true);
    });
  });

  describe('leaving the canvas', () => {
    it('unmounting disarms an armed type', async () => {
      await tool('assumption').trigger('click');
      expect(ui.tool).toBe('assumption');
      wrapper.unmount();
      expect(ui.tool).toBeNull();
      wrapper = mount(NodePalette, { props: { resolveTarget }, attachTo: document.body });
    });

    it('unmounting mid-drag clears the drag and its drop target', async () => {
      await press('solution');
      await moveTo(400, 200);
      expect(ui.paletteDrag).not.toBeNull();
      expect(ui.dropTargetId).toBe('opportunity-1');
      wrapper.unmount();
      expect(ui.paletteDrag).toBeNull();
      expect(ui.dropTargetId).toBeNull();
      wrapper = mount(NodePalette, { props: { resolveTarget }, attachTo: document.body });
    });
  });

  describe('icons', () => {
    /** Phosphor regular carets (as the prototype paths): 8-unit arcs; bold would be 12. */
    const caretPath = () => wrapper.get('[data-cy="ost-palette-toggle"] svg path').attributes('d') ?? '';

    it('the hide and rail carets use the regular weight', async () => {
      expect(caretPath()).toMatch(/a8,8/);
      await wrapper.get('[data-cy="ost-palette-toggle"]').trigger('click');
      expect(caretPath()).toMatch(/a8,8/);
    });
  });
});
