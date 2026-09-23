/**
 * LABEL-001 component test for LabelChips: covers criteria 2 (apply, remove, create inline),
 * 3 (unlimited labels), 4 (suggestion order), 8 (viewer read-only), 17 (component coverage).
 */
import { afterEach, describe, expect, it } from 'vitest';

import { enableAutoUnmount, flushPromises } from '@vue/test-utils';

import { dto } from '../../domain/fixtures.test-util';
import { PANEL_TREE, mountWith, setupStores } from '../panel.test-util';

import LabelChips from './LabelChips.vue';

enableAutoUnmount(afterEach);

async function mount(nodeKey: string, extra: Parameters<typeof setupStores>[1] = {}) {
  const ctx = await setupStores(PANEL_TREE, extra);
  ctx.ui.select(nodeKey);
  const node = ctx.tree.byId(nodeKey)!;
  const wrapper = await mountWith(LabelChips, ctx.pinia, {
    nodeKey,
    tags: node.tags,
    readonly: false,
    teamId: 7,
  });
  return { ...ctx, wrapper };
}

describe('LabelChips', () => {
  it('renders one chip per applied label', async () => {
    const { wrapper } = await mount('opportunity-1');
    const chips = wrapper.findAll('[data-cy^="ost-label-chip-"]');
    expect(chips).toHaveLength(1);
    expect(chips[0].text()).toContain('Mobile Value Stream');
  });

  it('creates a label inline on Enter and adds it to the node', async () => {
    const { wrapper, service, tree } = await mount('opportunity-1');
    service.createLabel.resolves({ id: 200, name: 'Retention' });
    service.applyLabels.resolves(
      dto('opportunity-1', 'outcome-1', {
        status: 'EXPLORING',
        priority: 50,
        valueRating: 3,
        tags: [
          { id: 100, name: 'Mobile Value Stream' },
          { id: 200, name: 'Retention' },
        ],
      }),
    );
    const input = wrapper.get('[data-cy="ost-label-input"]');
    await input.setValue('Retention');
    await input.trigger('keydown.enter');
    await flushPromises();
    expect(service.createLabel.calledOnceWith(7, 'Retention')).toBe(true);
    expect(service.applyLabels.calledOnce).toBe(true);
    expect(service.applyLabels.firstCall.args[2]).toEqual([100, 200]);
    expect(tree.byId('opportunity-1')?.tags.map(t => t.id)).toEqual([100, 200]);
  });

  it('removes a label when its remove button is clicked', async () => {
    const { wrapper, service } = await mount('opportunity-1');
    service.applyLabels.resolves(dto('opportunity-1', 'outcome-1', { status: 'EXPLORING', priority: 50, valueRating: 3, tags: [] }));
    await wrapper.get('[data-cy="ost-label-remove-100"]').trigger('click');
    await flushPromises();
    expect(service.applyLabels.firstCall.args[2]).toEqual([]);
  });

  it('lists suggestions on focus in the server-supplied order (current team first)', async () => {
    const { wrapper, service } = await mount('opportunity-1');
    service.listLabelSuggestions.resolves([
      { id: 100, name: 'Mobile Value Stream' },
      { id: 101, name: 'Onboarding' },
      { id: 999, name: 'Other-team Only' },
    ]);
    await wrapper.get('[data-cy="ost-label-input"]').trigger('focus');
    await flushPromises();
    // Applied label (id 100) is filtered out; server ordering is preserved for the rest.
    const items = wrapper.findAll('[data-cy^="ost-label-suggest-"]');
    expect(items.map(i => i.text())).toEqual(['Onboarding', 'Other-team Only']);
  });

  it('supports many labels — the panel stays usable with several applied (no cap)', async () => {
    const many = Array.from({ length: 12 }, (_, i) => ({ id: 500 + i, name: `Label ${i}` }));
    const ctx = await setupStores(PANEL_TREE);
    const wrapper = await mountWith(LabelChips, ctx.pinia, {
      nodeKey: 'opportunity-1',
      tags: many,
      readonly: false,
      teamId: 7,
    });
    expect(wrapper.findAll('[data-cy^="ost-label-chip-"]')).toHaveLength(12);
  });

  it('viewers see labels but cannot edit them', async () => {
    const ctx = await setupStores(PANEL_TREE);
    const wrapper = await mountWith(LabelChips, ctx.pinia, {
      nodeKey: 'opportunity-1',
      tags: [{ id: 100, name: 'Mobile Value Stream' }],
      readonly: true,
      teamId: 7,
    });
    expect(wrapper.find('[data-cy="ost-label-input"]').exists()).toBe(false);
    expect(wrapper.find('[data-cy="ost-label-remove-100"]').exists()).toBe(false);
    expect(wrapper.text()).toContain('Mobile Value Stream');
  });
});
