import { afterEach, describe, expect, it } from 'vitest';

import { enableAutoUnmount, flushPromises } from '@vue/test-utils';

import { dto } from '../domain/fixtures.test-util';

import DetailPanel from './DetailPanel.vue';
import { PANEL_TREE, apiError, mountWith, setupStores } from './panel.test-util';

enableAutoUnmount(afterEach);

async function mountPanel(select: string | null, extra: Parameters<typeof setupStores>[1] = {}) {
  const ctx = await setupStores(PANEL_TREE, extra);
  ctx.ui.select(select);
  const wrapper = await mountWith(DetailPanel, ctx.pinia);
  return { ...ctx, wrapper };
}

const tabIds = (wrapper: Awaited<ReturnType<typeof mountPanel>>['wrapper']) =>
  wrapper.findAll('[data-cy^="ost-tab-"]:not([data-cy^="ost-tab-badge"])').map(t => t.attributes('data-cy')!.replace('ost-tab-', ''));

describe('DetailPanel', () => {
  describe('tab set per type', () => {
    it.each([
      ['product-1', ['detail', 'links']],
      ['outcome-1', ['detail', 'links', 'chat', 'history']],
      ['opportunity-1', ['detail', 'links', 'chat', 'questions', 'history']],
      ['solution-1', ['detail', 'links', 'chat', 'history']],
      ['assumption-1', ['detail', 'links', 'chat', 'history']],
      ['evidence-1', ['detail', 'links', 'chat', 'history']],
    ])('%s → %j', async (key, tabs) => {
      const { wrapper } = await mountPanel(key);
      expect(tabIds(wrapper)).toEqual(tabs);
    });

    it('labels the tabs and badges links, chat and open questions (not history)', async () => {
      const { wrapper } = await mountPanel('opportunity-1');
      expect(wrapper.get('[data-cy="ost-tab-questions"]').text()).toContain('Open Qs');
      expect(wrapper.get('[data-cy="ost-tab-badge-links"]').text()).toBe('2');
      expect(wrapper.get('[data-cy="ost-tab-badge-chat"]').text()).toBe('2');
      expect(wrapper.get('[data-cy="ost-tab-badge-questions"]').text()).toBe('2');
      expect(wrapper.find('[data-cy="ost-tab-badge-history"]').exists()).toBe(false);
    });

    it('hides zero badges', async () => {
      const { wrapper } = await mountPanel('solution-1');
      expect(wrapper.find('[data-cy="ost-tab-badge-links"]').exists()).toBe(false);
      expect(wrapper.find('[data-cy="ost-tab-badge-chat"]').exists()).toBe(false);
    });

    it('switches tabs and falls back to Detail when the type has no such tab', async () => {
      const { wrapper, ui } = await mountPanel('opportunity-1');
      await wrapper.get('[data-cy="ost-tab-questions"]').trigger('click');
      expect(ui.panelTab).toBe('questions');
      expect(wrapper.find('[data-cy="ostTab-questions"]').exists()).toBe(true);
      ui.select('product-1');
      await flushPromises();
      expect(wrapper.get('[data-cy="ost-tab-detail"]').classes()).toContain('is-active');
      expect(wrapper.find('[data-cy="ostTab-detail"]').exists()).toBe(true);
    });
  });

  describe('header', () => {
    it('shows the type kicker and the ancestor breadcrumb', async () => {
      const { wrapper } = await mountPanel('assumption-1');
      expect(wrapper.get('[data-cy="ost-panel-kicker"]').text()).toBe('Assumption');
      const crumbs = wrapper.findAll('[data-cy^="ost-breadcrumb-"]').map(c => c.attributes('data-cy'));
      expect(crumbs).toEqual([
        'ost-breadcrumb-product-1',
        'ost-breadcrumb-outcome-1',
        'ost-breadcrumb-opportunity-1',
        'ost-breadcrumb-solution-1',
      ]);
      expect(wrapper.get('[data-cy="ost-breadcrumb"]').text()).toContain('Discovery Hub');
    });

    it('products are the top of the tree', async () => {
      const { wrapper } = await mountPanel('product-1');
      expect(wrapper.get('[data-cy="ost-breadcrumb"]').text()).toBe('Top of the tree');
    });

    it('a breadcrumb click selects the ancestor and asks the canvas to centre it', async () => {
      const { wrapper, ui } = await mountPanel('assumption-1');
      await wrapper.get('[data-cy="ost-breadcrumb-outcome-1"]').trigger('click');
      expect(ui.selectedId).toBe('outcome-1');
      expect(ui.centreRequest?.key).toBe('outcome-1');
      expect((wrapper.get('[data-cy="ost-panel-title"]').element as HTMLInputElement).value).toBe('Weekly interviews');
    });

    it('commits the title on Enter', async () => {
      const { wrapper, service, tree } = await mountPanel('outcome-1');
      service.patchNode.resolves(dto('outcome-1', 'product-1', { title: 'Renamed outcome' }));
      const input = wrapper.get('[data-cy="ost-panel-title"]');
      await input.trigger('focus');
      await input.setValue('Renamed outcome');
      await input.trigger('keydown', { key: 'Enter' });
      await input.trigger('blur');
      await flushPromises();
      expect(service.patchNode.calledOnceWith('outcome', 1, { title: 'Renamed outcome' })).toBe(true);
      expect(tree.byId('outcome-1')?.title).toBe('Renamed outcome');
    });

    it('Escape cancels the title edit', async () => {
      const { wrapper, service } = await mountPanel('outcome-1');
      const input = wrapper.get('[data-cy="ost-panel-title"]');
      (input.element as HTMLInputElement).focus();
      await input.setValue('Something else');
      await input.trigger('keydown', { key: 'Escape' });
      await flushPromises();
      expect(service.patchNode.called).toBe(false);
      expect((input.element as HTMLInputElement).value).toBe('Weekly interviews');
    });

    it('shows a refused title inline and rolls it back', async () => {
      const { wrapper, service, tree } = await mountPanel('outcome-1');
      service.patchNode.rejects(apiError(400, 'error.invalidtitle'));
      const input = wrapper.get('[data-cy="ost-panel-title"]');
      await input.setValue('No good');
      await input.trigger('blur');
      await flushPromises();
      expect(wrapper.get('[data-cy="ost-panel-error"]').text()).toContain('Titles need 2 to 200 characters');
      expect(tree.error).toBeNull(); // moved from the toast into the panel
      expect(tree.byId('outcome-1')?.title).toBe('Weekly interviews');
      expect((input.element as HTMLInputElement).value).toBe('Weekly interviews');
    });

    it('hiding keeps the selection and leaves a Details tab that reopens the panel', async () => {
      const { wrapper, ui } = await mountPanel('solution-1');
      await wrapper.get('[data-cy="ost-panel-hide"]').trigger('click');
      expect(ui.rightOpen).toBe(false);
      expect(ui.selectedId).toBe('solution-1');
      expect(wrapper.find('[data-cy="ost-panel"]').exists()).toBe(false);
      await wrapper.get('[data-cy="ost-panel-reopen"]').trigger('click');
      expect(wrapper.find('[data-cy="ost-panel"]').exists()).toBe(true);
    });

    it('a node selection reopens a hidden panel', async () => {
      const { wrapper, ui } = await mountPanel('solution-1');
      ui.setRightOpen(false);
      await flushPromises();
      ui.select('solution-2');
      await flushPromises();
      expect(wrapper.find('[data-cy="ost-panel"]').exists()).toBe(true);
    });
  });

  describe('footer', () => {
    it('links to the full-page detail and asks to delete', async () => {
      const { wrapper, ui } = await mountPanel('solution-1');
      const to = JSON.parse(wrapper.get('[data-cy="ost-open-detail"]').attributes('data-to')!);
      expect(to).toEqual({ name: 'OstNodeDetail', params: { teamId: '7', nodeKey: 'solution-1' } });
      await wrapper.get('[data-cy="ost-delete"]').trigger('click');
      expect(ui.confirmId).toBe('solution-1');
    });
  });

  describe('viewer', () => {
    it('is read-only: no delete, read-only title, no edits sent', async () => {
      const { wrapper, service } = await mountPanel('opportunity-1', { canEdit: false, currentUserRole: 'VIEWER' });
      expect(wrapper.find('[data-cy="ost-delete"]').exists()).toBe(false);
      expect(wrapper.find('[data-cy="ost-open-detail"]').exists()).toBe(true);
      const input = wrapper.get('[data-cy="ost-panel-title"]');
      expect(input.attributes('readonly')).toBeDefined();
      await input.setValue('Nope');
      await input.trigger('blur');
      await flushPromises();
      expect(service.patchNode.called).toBe(false);
    });
  });
});
