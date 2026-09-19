import { describe, expect, it } from 'vitest';

import { mount } from '@vue/test-utils';

import { node } from '../domain/fixtures.test-util';
import { priorityColor } from '../domain/rules';
import type { NodeType, OstNode } from '../domain/types';

import OstNodeView from './OstNode.vue';

const HandleStub = { name: 'Handle', template: '<i class="handle-stub"></i>' };

function mountNode(n: OstNode, props: Record<string, unknown> = {}) {
  return mount(OstNodeView, {
    props: { node: n, ...props },
    global: { stubs: { Handle: HandleStub } },
  });
}

/** The dot colour (happy-dom drops oklch() from inline styles, so the node mirrors it in data-color). */
const bg = (el: Element) => el.getAttribute('data-color');

describe('OstNode', () => {
  describe('frames', () => {
    const cases: [NodeType, string, number][] = [
      ['product', 'Product', 244],
      ['outcome', 'Outcome', 238],
      ['opportunity', 'Opportunity', 218],
      ['solution', 'Solution', 206],
      ['assumption', 'Assumption', 198],
      ['evidence', 'Evidence', 206],
    ];

    it.each(cases)('%s gets its own frame class, kicker and box width', (type, label, width) => {
      const wrapper = mountNode(node(`${type}-1`, type === 'product' ? null : 'x-1'));
      const root = wrapper.get('.ost-node');
      expect(root.classes()).toContain(`ost-node--${type}`);
      // exactly one frame class
      expect(root.classes().filter(c => /^ost-node--/.test(c))).toEqual([`ost-node--${type}`]);
      expect(wrapper.get('.ost-node__kicker').text()).toBe(label);
      expect(root.attributes('style')).toContain(`width: ${width}px`);
      expect(root.attributes('data-cy')).toBe(`ost-node-${type}-1`);
    });

    it('shows the title', () => {
      const wrapper = mountNode(node('outcome-1', 'product-1', { title: 'Teams interview weekly' }));
      expect(wrapper.get('[data-cy="ost-node-title"]').text()).toBe('Teams interview weekly');
    });

    it('products have no meta row (no status, no chat)', () => {
      const wrapper = mountNode(node('product-1', null, { commentCount: 3 }));
      expect(wrapper.find('.ost-node__meta').exists()).toBe(false);
      expect(wrapper.find('.ost-node__chat').exists()).toBe(false);
    });
  });

  describe('status badge tones', () => {
    it.each([
      ['validated', 'good'],
      ['supported', 'good'],
      ['shipped', 'good'],
      ['exploring', 'flight'],
      ['unexplored', 'flight'],
      ['testing', 'flight'],
      ['untested', 'flight'],
      ['candidate', 'flight'],
      ['building', 'flight'],
      ['refuted', 'bad'],
      ['dropped', 'bad'],
      ['parked', 'bad'],
    ])('%s → %s', (status, tone) => {
      const type: NodeType = ['validated', 'exploring', 'unexplored', 'parked'].includes(status)
        ? 'opportunity'
        : ['shipped', 'candidate', 'building', 'dropped'].includes(status)
          ? 'solution'
          : 'assumption';
      const wrapper = mountNode(node(`${type}-1`, 'x-1', { status }));
      const badge = wrapper.get('[data-cy="ost-node-status"]');
      expect(badge.text()).toBe(status);
      expect(badge.classes()).toContain(`ost-node__badge--${tone}`);
      expect(badge.attributes('data-tone')).toBe(tone);
    });

    it('no badge without a status', () => {
      expect(mountNode(node('outcome-1', 'product-1')).find('[data-cy="ost-node-status"]').exists()).toBe(false);
    });
  });

  describe('one metric per type', () => {
    it('assumptions show confidence', () => {
      const wrapper = mountNode(node('assumption-1', 'solution-1', { status: 'testing', conf: 65 }));
      expect(wrapper.get('[data-cy="ost-node-metric"]').text()).toBe('65% confidence');
      expect(wrapper.find('[data-cy="ost-node-value"]').exists()).toBe(false);
    });

    it('solutions show their derived evidence strength when tested', () => {
      const solution = node('solution-1', 'opportunity-1', { status: 'building' });
      expect(
        mountNode(solution, { evidence: { tests: 2, score: 55 } })
          .get('[data-cy="ost-node-metric"]')
          .text(),
      ).toBe('55% evidence');
      expect(
        mountNode(solution, { evidence: { tests: 0, score: null } })
          .find('[data-cy="ost-node-metric"]')
          .exists(),
      ).toBe(false);
    });

    it('outcomes and evidence show no metric', () => {
      expect(mountNode(node('outcome-1', 'product-1')).find('[data-cy="ost-node-metric"]').exists()).toBe(false);
      expect(mountNode(node('evidence-1', 'opportunity-1')).find('[data-cy="ost-node-metric"]').exists()).toBe(false);
    });
  });

  describe('opportunity value ($ glyphs)', () => {
    it.each([1, 3, 5])('value %i → %i filled of 5', value => {
      const wrapper = mountNode(node('opportunity-1', 'outcome-1', { status: 'exploring', value }));
      expect(wrapper.get('.ost-node__money-on').text()).toBe('$'.repeat(value));
      expect(wrapper.get('.ost-node__money-off').text()).toBe('$'.repeat(5 - value));
    });

    it('only opportunities show $', () => {
      expect(
        mountNode(node('solution-1', 'opportunity-1', { value: 4 }))
          .find('[data-cy="ost-node-value"]')
          .exists(),
      ).toBe(false);
    });
  });

  describe('priority rail', () => {
    const dots = (priority: number) =>
      mountNode(node('opportunity-1', 'outcome-1', { status: 'exploring', priority }))
        .get('[data-cy="ost-node-priority"]')
        .findAll('i');

    it('always has five dots, top = highest band', () => {
      expect(dots(50)).toHaveLength(5);
    });

    it.each([
      [1, 1],
      [20, 1],
      [21, 2],
      [50, 3],
      [80, 4],
      [92, 5],
      [100, 5],
    ])('priority %i fills %i dots', (priority, filled) => {
      const list = dots(priority);
      expect(list.filter(d => d.attributes('data-filled') === 'true')).toHaveLength(filled);
      // filled dots are the BOTTOM ones (rail reads bottom-up)
      expect(list.map(d => d.attributes('data-filled'))).toEqual([...Array(5 - filled).fill('false'), ...Array(filled).fill('true')]);
    });

    it('filled dots take the oklch priority colour; empty ones the neutral token', () => {
      const list = dots(92);
      const colour = priorityColor(92);
      expect(colour).toMatch(/^oklch\(0\.74 /);
      for (const d of list) expect(bg(d.element)).toBe(colour);
      const low = dots(10);
      expect(bg(low[4].element)).toBe(priorityColor(10));
      expect(bg(low[0].element)).toBe('var(--color-neutral-800)');
    });

    it('a partly filled dot is smaller and fainter than a full one', () => {
      const list = dots(50); // bands: 5 empty, 4 empty, 3 half, 2 full, 1 full
      const full = list[4].element as HTMLElement;
      const half = list[2].element as HTMLElement;
      expect(parseFloat(full.style.width)).toBe(7);
      expect(parseFloat(half.style.width)).toBeCloseTo(5.8);
      expect(Number(half.style.opacity)).toBeLessThan(Number(full.style.opacity));
    });

    it('is on opportunities only', () => {
      expect(mountNode(node('solution-1', 'opportunity-1')).find('[data-cy="ost-node-priority"]').exists()).toBe(false);
    });
  });

  describe('thread chip', () => {
    it('shows the comment count and emits chat without bubbling', async () => {
      const wrapper = mountNode(node('opportunity-3', 'outcome-1', { commentCount: 5 }));
      const chip = wrapper.get('[data-cy="ost-node-chat-opportunity-3"]');
      expect(chip.text()).toBe('5');
      expect(chip.classes()).not.toContain('is-empty');
      await chip.trigger('click');
      expect(wrapper.emitted('chat')).toHaveLength(1);
    });

    it('is quiet when there are no messages', () => {
      const chip = mountNode(node('evidence-1', 'opportunity-1')).get('.ost-node__chat');
      expect(chip.text()).toBe('0');
      expect(chip.classes()).toContain('is-empty');
    });
  });

  describe('+ button', () => {
    it('renders only when canAdd and emits add', async () => {
      expect(mountNode(node('outcome-1', 'product-1')).find('[data-cy="ost-node-add-outcome-1"]').exists()).toBe(false);
      const wrapper = mountNode(node('outcome-1', 'product-1'), { canAdd: true });
      await wrapper.get('[data-cy="ost-node-add-outcome-1"]').trigger('click');
      expect(wrapper.emitted('add')).toHaveLength(1);
    });
  });

  describe('collapse chip', () => {
    it('is absent without children', () => {
      expect(mountNode(node('outcome-1', 'product-1')).find('[data-cy="ost-collapse-outcome-1"]').exists()).toBe(false);
    });

    it('shows – when expanded and +n when collapsed, and emits toggle', async () => {
      const expanded = mountNode(node('outcome-1', 'product-1'), { childCount: 3 });
      const chip = expanded.get('[data-cy="ost-collapse-outcome-1"]');
      expect(chip.text()).toBe('–');
      expect(chip.attributes('aria-expanded')).toBe('true');
      await chip.trigger('click');
      expect(expanded.emitted('toggle')).toHaveLength(1);

      const collapsed = mountNode(node('outcome-1', 'product-1'), { childCount: 3, collapsed: true });
      const c = collapsed.get('[data-cy="ost-collapse-outcome-1"]');
      expect(c.text()).toBe('+3');
      expect(c.classes()).toContain('is-collapsed');
      expect(c.attributes('aria-expanded')).toBe('false');
    });
  });

  describe('states', () => {
    it('maps selected / match / dimmed / drop target to classes', () => {
      const wrapper = mountNode(node('solution-1', 'opportunity-1'), { selected: true, match: true, dimmed: true, dropTarget: true });
      expect(wrapper.get('.ost-node').classes()).toEqual(expect.arrayContaining(['is-selected', 'is-match', 'is-dimmed', 'is-target']));
      const plain = mountNode(node('solution-1', 'opportunity-1'));
      expect(plain.get('.ost-node').classes()).not.toEqual(expect.arrayContaining(['is-selected']));
      expect(plain.get('.ost-node').classes()).not.toContain('is-dimmed');
    });
  });
});
