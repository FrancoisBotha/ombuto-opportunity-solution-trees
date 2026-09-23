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
      ['outcome-1', ['detail', 'links', 'transcripts', 'history']],
      ['opportunity-1', ['detail', 'links', 'transcripts', 'questions', 'history']],
      ['solution-1', ['detail', 'links', 'transcripts', 'history']],
      ['assumption-1', ['detail', 'links', 'transcripts', 'history']],
      ['evidence-1', ['detail', 'links', 'transcripts', 'history']],
    ])('%s → %j', async (key, tabs) => {
      const { wrapper } = await mountPanel(key);
      expect(tabIds(wrapper)).toEqual(tabs);
    });

    it('labels the tabs and badges links, transcripts and open questions (not history)', async () => {
      const { wrapper, tree } = await mountPanel('opportunity-1');
      tree.byId('opportunity-1')!.transcriptCount = 2;
      await flushPromises();
      expect(wrapper.get('[data-cy="ost-tab-questions"]').text()).toContain('Open Qs');
      expect(wrapper.get('[data-cy="ost-tab-badge-links"]').text()).toBe('2');
      expect(wrapper.get('[data-cy="ost-tab-badge-transcripts"]').text()).toBe('2');
      expect(wrapper.get('[data-cy="ost-tab-badge-questions"]').text()).toBe('2');
      expect(wrapper.find('[data-cy="ost-tab-badge-history"]').exists()).toBe(false);
    });

    it('hides zero badges', async () => {
      const { wrapper } = await mountPanel('solution-1');
      expect(wrapper.find('[data-cy="ost-tab-badge-links"]').exists()).toBe(false);
      expect(wrapper.find('[data-cy="ost-tab-badge-transcripts"]').exists()).toBe(false);
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

    it('Escape cancels the title edit and keeps focus in the field', async () => {
      const { wrapper, service } = await mountPanel('outcome-1');
      const input = wrapper.get('[data-cy="ost-panel-title"]');
      (input.element as HTMLInputElement).focus();
      await input.setValue('Something else');
      await input.trigger('keydown', { key: 'Escape' });
      await flushPromises();
      expect(service.patchNode.called).toBe(false);
      expect((input.element as HTMLInputElement).value).toBe('Weekly interviews');
      expect(document.activeElement).toBe(input.element);
      // Leaving the field afterwards commits nothing.
      await input.trigger('blur');
      await flushPromises();
      expect(service.patchNode.called).toBe(false);
    });

    it('a focused but untouched title field follows a rename made elsewhere; a typed edit is kept', async () => {
      const { wrapper, service, tree } = await mountPanel('outcome-1');
      const input = wrapper.get('[data-cy="ost-panel-title"]');
      const field = input.element as HTMLInputElement;
      // Focus lands in the field before the canvas rename's title reaches the store.
      field.focus();
      await input.trigger('focus');
      tree.byId('outcome-1')!.title = 'Renamed on the canvas';
      await flushPromises();
      expect(field.value).toBe('Renamed on the canvas');
      // Typing from there edits the NEW title, and Enter saves exactly that.
      service.patchNode.resolves(dto('outcome-1', 'product-1', { title: 'Renamed on the canvas yz' }));
      await input.setValue('Renamed on the canvas yz');
      tree.byId('outcome-1')!.title = 'Someone else';
      await flushPromises();
      expect(field.value).toBe('Renamed on the canvas yz'); // the user's own typing is never overwritten
      await input.trigger('blur');
      await flushPromises();
      expect(service.patchNode.lastCall.args.slice(0, 3)).toEqual(['outcome', 1, { title: 'Renamed on the canvas yz' }]);
      // Afterwards the field follows the store again.
      tree.byId('outcome-1')!.title = 'Later remote title';
      await flushPromises();
      expect(field.value).toBe('Later remote title');
    });

    it('focusing the field picks up a title changed while it was not being edited', async () => {
      const { wrapper, tree } = await mountPanel('outcome-1');
      const input = wrapper.get('[data-cy="ost-panel-title"]');
      tree.byId('outcome-1')!.title = 'Changed meanwhile';
      await input.trigger('focus');
      expect((input.element as HTMLInputElement).value).toBe('Changed meanwhile');
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

    it('says why an emptied title snapped back instead of reverting in silence', async () => {
      const { wrapper, service, tree } = await mountPanel('outcome-1');
      const input = wrapper.get('[data-cy="ost-panel-title"]');
      await input.setValue('');
      await input.trigger('blur');
      await flushPromises();
      expect(wrapper.get('[data-cy="ost-panel-error"]').text()).toBe('A title is required.');
      expect(tree.byId('outcome-1')?.title).toBe('Weekly interviews');
      expect((input.element as HTMLInputElement).value).toBe('Weekly interviews');
      expect(service.patchNode.called).toBe(false);
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

  describe('step-10 review follow-ups (C15)', () => {
    it.each([
      ['product-1', '100'],
      ['outcome-1', '200'],
      ['opportunity-1', '200'],
      ['assumption-1', '500'],
      ['evidence-1', '500'],
    ])('%s: title maxlength %s', async (key, max) => {
      const { wrapper } = await mountPanel(key);
      expect(wrapper.get('[data-cy="ost-panel-title"]').attributes('maxlength')).toBe(max);
    });

    it('explains a refused title with the rule of the node type', async () => {
      const { wrapper, service } = await mountPanel('product-1');
      service.patchNode.rejects(apiError(400, 'error.invalidtitle'));
      const input = wrapper.get('[data-cy="ost-panel-title"]');
      await input.setValue('Product that is fine locally');
      await input.trigger('blur');
      await flushPromises();
      expect(wrapper.get('[data-cy="ost-panel-error"]').text()).toBe('Product names need 2 to 100 characters.');
    });

    it('a failure that returns after the selection changed is not shown against the new node', async () => {
      const { wrapper, service, tree, ui } = await mountPanel('outcome-1');
      let reject!: (e: unknown) => void;
      service.patchNode.returns(new Promise((_, r) => (reject = r)) as any);
      const input = wrapper.get('[data-cy="ost-panel-title"]');
      await input.setValue('Renamed outcome');
      await input.trigger('blur');
      ui.select('solution-1');
      await flushPromises();
      reject(apiError(500));
      await flushPromises();
      expect(wrapper.find('[data-cy="ost-panel-error"]').exists()).toBe(false);
      // left on the toast, labelled with the node it was about
      expect(tree.error).toBe('“Weekly interviews”: Something went wrong. Your change was not saved.');
    });

    it('tabs: aria-controls / tabpanel wiring and a roving tabindex', async () => {
      const { wrapper } = await mountPanel('opportunity-1');
      const active = wrapper.get('[data-cy="ost-tab-detail"]');
      const panel = wrapper.get('[role="tabpanel"]');
      expect(active.attributes('aria-controls')).toBe(panel.attributes('id'));
      expect(panel.attributes('aria-labelledby')).toBe(active.attributes('id'));
      expect(active.attributes('tabindex')).toBe('0');
      expect(wrapper.get('[data-cy="ost-tab-links"]').attributes('tabindex')).toBe('-1');
      expect(wrapper.get('[data-cy="ost-tab-links"]').attributes('aria-controls')).toBeUndefined();
    });

    it('tabs: arrow keys (wrapping), Home and End select and focus', async () => {
      const { wrapper, ui } = await mountPanel('opportunity-1');
      const list = wrapper.get('[role="tablist"]');
      await list.trigger('keydown', { key: 'ArrowRight' });
      await flushPromises();
      expect(ui.panelTab).toBe('links');
      expect(document.activeElement?.getAttribute('data-cy')).toBe('ost-tab-links');
      await list.trigger('keydown', { key: 'End' });
      await flushPromises();
      expect(ui.panelTab).toBe('history');
      await list.trigger('keydown', { key: 'ArrowRight' });
      await flushPromises();
      expect(ui.panelTab).toBe('detail');
      await list.trigger('keydown', { key: 'ArrowLeft' });
      await flushPromises();
      expect(ui.panelTab).toBe('history');
      await list.trigger('keydown', { key: 'Home' });
      await flushPromises();
      expect(ui.panelTab).toBe('detail');
      expect(document.activeElement?.getAttribute('data-cy')).toBe('ost-tab-detail');
    });

    it('focusing the title marks it as being typed and a suppressed remote value shows as a dismissible hint', async () => {
      const { wrapper, tree } = await mountPanel('outcome-1');
      const input = wrapper.get('[data-cy="ost-panel-title"]');
      await input.trigger('focus');
      // A remote NODE_UPDATED for a field the user is typing in must be suppressed and recorded.
      tree.applyEvents([
        {
          type: 'NODE_UPDATED',
          node: dto('outcome-1', 'product-1', { title: 'Remote rename' }),
          actingUserLogin: 'someone-else',
        },
      ]);
      await flushPromises();
      expect(tree.droppedRemoteFor('outcome-1').title).toBe('Remote rename');
      const hint = wrapper.get('[data-cy="ost-panel-title-dropped"]');
      expect(hint.text()).toContain('Remote rename');
      await wrapper.get('[data-cy="ost-panel-title-dropped-dismiss"]').trigger('click');
      await flushPromises();
      expect(wrapper.find('[data-cy="ost-panel-title-dropped"]').exists()).toBe(false);
      expect(tree.droppedRemoteFor('outcome-1').title).toBeUndefined();
    });

    it('hide moves focus to the Details tab, reopening moves it back to the hide button', async () => {
      const { wrapper } = await mountPanel('solution-1');
      await wrapper.get('[data-cy="ost-panel-hide"]').trigger('click');
      await flushPromises();
      expect(document.activeElement?.getAttribute('data-cy')).toBe('ost-panel-reopen');
      await wrapper.get('[data-cy="ost-panel-reopen"]').trigger('click');
      await flushPromises();
      expect(document.activeElement?.getAttribute('data-cy')).toBe('ost-panel-hide');
    });
  });
});
