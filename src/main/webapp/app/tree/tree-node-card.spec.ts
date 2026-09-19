import { describe, expect, it } from 'vitest';

import { mount } from '@vue/test-utils';

import { OpportunityStatus } from '@/shared/model/enumerations/opportunity-status.model';

import type { IOpportunityTreeNode } from './tree.model';
import TreeNodeCard from './tree-node-card.vue';

const opp = (): IOpportunityTreeNode => ({
  id: 1,
  title: 'Opp',
  description: null,
  status: OpportunityStatus.IDENTIFIED,
  valuerating: 3,
  complexity: 3,
  sortOrder: 0,
  parentId: null,
  children: [],
  solutions: [],
});

describe('TreeNodeCard menu actions', () => {
  it('does not render any action buttons when canEdit is false (viewer)', () => {
    const wrapper = mount(TreeNodeCard, {
      props: { type: 'opportunity', node: opp() as any, canEdit: false, canMoveUp: true, canMoveDown: true, canMoveTo: true },
    });
    expect(wrapper.find('[data-cy="treeNodeMoveTo-opportunity-1"]').exists()).toBe(false);
    expect(wrapper.find('[data-cy="treeNodeMoveUp-opportunity-1"]').exists()).toBe(false);
    expect(wrapper.find('[data-cy="treeNodeMoveDown-opportunity-1"]').exists()).toBe(false);
    expect(wrapper.find('[data-cy="treeNodeDelete-opportunity-1"]').exists()).toBe(false);
  });

  it('renders and emits Move to… when canEdit and canMoveTo are true', async () => {
    const wrapper = mount(TreeNodeCard, {
      props: { type: 'opportunity', node: opp() as any, canEdit: true, canMoveUp: true, canMoveDown: false, canMoveTo: true },
    });
    const btn = wrapper.find('[data-cy="treeNodeMoveTo-opportunity-1"]');
    expect(btn.exists()).toBe(true);
    await btn.trigger('click');
    expect(wrapper.emitted('move-to')).toEqual([[{ type: 'opportunity', id: 1 }]]);
  });

  it('hides Move to… when canMoveTo is false', () => {
    const wrapper = mount(TreeNodeCard, {
      props: { type: 'opportunity', node: opp() as any, canEdit: true, canMoveTo: false },
    });
    expect(wrapper.find('[data-cy="treeNodeMoveTo-opportunity-1"]').exists()).toBe(false);
  });

  it('disables Move up at the first position and Move down at the last', () => {
    const wrapper = mount(TreeNodeCard, {
      props: { type: 'opportunity', node: opp() as any, canEdit: true, canMoveUp: false, canMoveDown: false },
    });
    const up = wrapper.find('[data-cy="treeNodeMoveUp-opportunity-1"]');
    const down = wrapper.find('[data-cy="treeNodeMoveDown-opportunity-1"]');
    expect(up.attributes('disabled')).toBeDefined();
    expect(down.attributes('disabled')).toBeDefined();
  });

  it('emits move-up / move-down when reorder actions are clicked', async () => {
    const wrapper = mount(TreeNodeCard, {
      props: { type: 'opportunity', node: opp() as any, canEdit: true, canMoveUp: true, canMoveDown: true },
    });
    await wrapper.find('[data-cy="treeNodeMoveUp-opportunity-1"]').trigger('click');
    await wrapper.find('[data-cy="treeNodeMoveDown-opportunity-1"]').trigger('click');
    expect(wrapper.emitted('move-up')).toEqual([[{ type: 'opportunity', id: 1 }]]);
    expect(wrapper.emitted('move-down')).toEqual([[{ type: 'opportunity', id: 1 }]]);
  });

  it('keyboard activation of a reorder button does not fall through to card selection (card keydown is scoped .self)', async () => {
    const wrapper = mount(TreeNodeCard, {
      props: { type: 'opportunity', node: opp() as any, canEdit: true, canMoveUp: true, canMoveDown: true, canMoveTo: true },
    });
    // Pressing Enter while focus is on Move up must not bubble up and select the card
    // (the parent card's Enter handler previously ran preventDefault, cancelling the button's own click).
    await wrapper.find('[data-cy="treeNodeMoveUp-opportunity-1"]').trigger('keydown.enter');
    await wrapper.find('[data-cy="treeNodeMoveDown-opportunity-1"]').trigger('keydown.enter');
    await wrapper.find('[data-cy="treeNodeMoveTo-opportunity-1"]').trigger('keydown.enter');
    // Card 'select' must NOT have been emitted from those keydowns.
    expect(wrapper.emitted('select')).toBeUndefined();
    // The card's own Enter (fired on the card itself) still emits select.
    await wrapper.find('[data-cy="treeNode-opportunity-1"]').trigger('keydown.enter');
    expect(wrapper.emitted('select')).toBeTruthy();
  });

  it('labels reorder actions as Move left / Move right for products', () => {
    const product = { id: 5, name: 'Prod', outcomes: [] } as any;
    const wrapper = mount(TreeNodeCard, {
      props: { type: 'product', node: product, canEdit: true, canMoveUp: true, canMoveDown: true, canMoveTo: false },
    });
    const up = wrapper.find('[data-cy="treeNodeMoveUp-product-5"]');
    expect(up.text()).toContain('Move left');
    const down = wrapper.find('[data-cy="treeNodeMoveDown-product-5"]');
    expect(down.text()).toContain('Move right');
  });
});
