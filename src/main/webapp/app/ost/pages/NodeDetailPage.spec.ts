import { afterEach, describe, expect, it } from 'vitest';

import { type VueWrapper, enableAutoUnmount, flushPromises, mount } from '@vue/test-utils';
import { type Router, createMemoryHistory, createRouter } from 'vue-router';

import { comment } from '../chat/chat.test-util';
import { dto } from '../domain/fixtures.test-util';
import { metricTags } from '../node-detail/node-detail-format';
import { PANEL_TREE, apiError, setupStores } from '../panel/panel.test-util';

import NodeDetailPage from './NodeDetailPage.vue';

enableAutoUnmount(afterEach);

const Stub = (cy: string) => ({ template: `<div data-cy="${cy}"></div>` });

async function mountPage(key: string, extra: Parameters<typeof setupStores>[1] = {}) {
  const ctx = await setupStores(PANEL_TREE, extra);
  ctx.service.listComments.resolves([comment(1, 'admin', new Date().toISOString(), { body: 'Hello from Ana' })]);
  const router: Router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/trees/:teamId/canvas', name: 'OstCanvas', component: Stub('canvas') },
      { path: '/trees/:teamId/nodes/:nodeKey', name: 'OstNodeDetail', component: NodeDetailPage },
    ],
  });
  await router.push(`/trees/7/nodes/${key}`);
  await router.isReady();
  const wrapper: VueWrapper = mount({ template: '<router-view />' }, { attachTo: document.body, global: { plugins: [router, ctx.pinia] } });
  await flushPromises();
  return { ...ctx, router, wrapper };
}

const has = (wrapper: VueWrapper, cy: string) => wrapper.find(`[data-cy="${cy}"]`).exists();
const tagIds = (wrapper: VueWrapper) =>
  wrapper.findAll('[data-cy^="ost-node-detail-metric-"]').map(t => t.attributes('data-cy')!.replace('ost-node-detail-metric-', ''));

describe('NodeDetailPage', () => {
  describe('sections per type', () => {
    it('opportunity: status, priority + value tags, fields, notes, children, open questions, chat and links', async () => {
      const { wrapper, ui } = await mountPage('opportunity-1');
      expect(ui.selectedId).toBe('opportunity-1');
      expect(wrapper.get('[data-cy="ost-node-detail-kicker"]').text()).toBe('Opportunity');
      expect((wrapper.get('[data-cy="ost-node-detail-title"]').element as HTMLTextAreaElement).value).toBe('Hard to find people');
      expect(wrapper.get('[data-cy="ost-node-detail-status"]').text()).toBe('exploring');
      expect(wrapper.get('[data-cy="ost-node-detail-status"]').classes()).toContain('ost-badge--flight');
      expect(tagIds(wrapper)).toEqual(['priority', 'value']);
      expect(wrapper.get('[data-cy="ost-node-detail-metric-priority"]').text()).toBe('Medium priority');
      expect(wrapper.get('[data-cy="ost-node-detail-metric-value"]').text()).toBe('$$$ Solid');
      expect(has(wrapper, 'ost-status-validated')).toBe(true);
      expect(has(wrapper, 'ost-notes')).toBe(true);
      expect(has(wrapper, 'ost-node-detail-questions')).toBe(true);
      expect(has(wrapper, 'ostTab-questions')).toBe(true);
      expect(has(wrapper, 'ost-node-detail-chat')).toBe(true);
      expect(wrapper.get('[data-cy="ost-node-detail-chat"]').text()).toContain('Hello from Ana');
      expect(has(wrapper, 'ostTab-links')).toBe(true);
      expect(has(wrapper, 'ost-node-detail-add-opportunity')).toBe(true);
      expect(has(wrapper, 'ost-node-detail-add-solution')).toBe(true);
      expect(has(wrapper, 'ost-node-detail-add-evidence')).toBe(true);
    });

    it('breadcrumb crumbs link to each ancestor’s detail page', async () => {
      const { wrapper, router } = await mountPage('assumption-2');
      const crumbs = wrapper.findAll('[data-cy^="ost-node-detail-crumb-"]');
      expect(crumbs.map(c => c.attributes('data-cy'))).toEqual([
        'ost-node-detail-crumb-product-1',
        'ost-node-detail-crumb-outcome-1',
        'ost-node-detail-crumb-opportunity-1',
        'ost-node-detail-crumb-solution-1',
      ]);
      expect(crumbs[2].attributes('href')).toBe('/trees/7/nodes/opportunity-1');
      await crumbs[3].trigger('click');
      await flushPromises();
      expect(router.currentRoute.value.params.nodeKey).toBe('solution-1');
      expect(wrapper.get('[data-cy="ost-node-detail-kicker"]').text()).toBe('Solution');
    });

    it('solution: evidence-strength tag derived from its assumptions', async () => {
      const { wrapper } = await mountPage('solution-1');
      expect(tagIds(wrapper)).toEqual(['evidence']);
      // supported → 100, testing at 60 → (100 + 60) / 2
      expect(wrapper.get('[data-cy="ost-node-detail-metric-evidence"]').text()).toBe('80% evidence');
      expect(has(wrapper, 'ost-evidence-bar')).toBe(true);
      expect(has(wrapper, 'ost-node-detail-questions')).toBe(false);
    });

    it('assumption: confidence tag; evidence: no status or tags', async () => {
      const first = await mountPage('assumption-2');
      expect(tagIds(first.wrapper)).toEqual(['confidence']);
      expect(first.wrapper.get('[data-cy="ost-node-detail-metric-confidence"]').text()).toBe('Med-high · 60%');
      first.wrapper.unmount();
      const second = await mountPage('evidence-1');
      expect(has(second.wrapper, 'ost-node-detail-status')).toBe(false);
      expect(tagIds(second.wrapper)).toEqual([]);
      expect(has(second.wrapper, 'ost-node-detail-chat')).toBe(true);
      expect(second.wrapper.get('[data-cy="ost-node-detail-no-children"]').text()).toContain('Evidence nodes have no children');
    });

    it('product: top of the tree, links, no chat', async () => {
      const { wrapper, service } = await mountPage('product-1');
      expect(wrapper.get('[data-cy="ost-node-detail-breadcrumb"]').text()).toBe('Top of the tree');
      expect(has(wrapper, 'ost-node-detail-chat')).toBe(false);
      expect(has(wrapper, 'ost-chat-thread')).toBe(false);
      expect(service.listComments.called).toBe(false);
      expect(has(wrapper, 'ost-node-detail-links')).toBe(true);
      expect(has(wrapper, 'ost-node-detail-status')).toBe(false);
    });
  });

  describe('children as cards', () => {
    it('one card per child, linking to its detail page, with its status', async () => {
      const { wrapper, router } = await mountPage('opportunity-1');
      const cards = wrapper.findAll('[data-cy^="ost-node-detail-child-"]');
      expect(cards.map(c => c.attributes('data-cy'))).toEqual(['ost-node-detail-child-solution-1', 'ost-node-detail-child-solution-2']);
      expect(cards[0].text()).toContain('In-app invite');
      expect(cards[0].text()).toContain('candidate');
      expect(cards[0].text()).toContain('3 beneath');
      expect(cards[0].attributes('href')).toBe('/trees/7/nodes/solution-1');
      await cards[1].trigger('click');
      await flushPromises();
      expect(router.currentRoute.value.fullPath).toBe('/trees/7/nodes/solution-2');
      expect((wrapper.get('[data-cy="ost-node-detail-title"]').element as HTMLTextAreaElement).value).toBe('Lonely solution');
    });

    it('quick-add creates the child and opens its page with the title selected', async () => {
      const { wrapper, router, service, ui } = await mountPage('solution-2');
      service.createNode.resolves(dto('assumption-9', 'solution-2', { title: 'New assumption', status: 'UNTESTED', confidence: 40 }));
      await wrapper.get('[data-cy="ost-node-detail-add-assumption"]').trigger('click');
      await flushPromises();
      expect(service.createNode.firstCall.args[0]).toMatchObject({ type: 'ASSUMPTION', parentType: 'SOLUTION', parentId: 2 });
      expect(router.currentRoute.value.params.nodeKey).toBe('assumption-9');
      expect(ui.editingId).toBeNull();
      const title = wrapper.get('[data-cy="ost-node-detail-title"]').element as HTMLTextAreaElement;
      expect(title.value).toBe('New assumption');
      expect(document.activeElement).toBe(title);
    });
  });

  describe('editing', () => {
    it('title: Enter commits, Escape cancels, too short is refused inline', async () => {
      const { wrapper, service } = await mountPage('solution-1');
      service.patchNode.callsFake(async (_t, _id, body) => dto('solution-1', 'opportunity-1', { title: body.title, status: 'CANDIDATE' }));
      const title = wrapper.get('[data-cy="ost-node-detail-title"]');
      await title.trigger('focus');
      await title.setValue('Renamed');
      await title.trigger('keydown', { key: 'Escape' });
      await title.trigger('blur');
      expect(service.patchNode.called).toBe(false);
      expect((title.element as HTMLTextAreaElement).value).toBe('In-app invite');

      await title.trigger('focus');
      await title.setValue('x');
      await title.trigger('blur');
      await flushPromises();
      expect(service.patchNode.called).toBe(false);
      expect(wrapper.get('[data-cy="ost-node-detail-error"]').text()).toContain('at least 2');

      await title.trigger('focus');
      await title.setValue('Renamed invite');
      await title.trigger('blur');
      await flushPromises();
      expect(service.patchNode.firstCall.args).toEqual(['solution', 1, { title: 'Renamed invite' }]);
    });

    it('status and notes save through the store; a refusal shows on the page', async () => {
      const { wrapper, service, tree } = await mountPage('opportunity-1');
      service.patchNode.onFirstCall().resolves(dto('opportunity-1', 'outcome-1', { status: 'VALIDATED', priority: 50, valueRating: 3 }));
      await wrapper.get('[data-cy="ost-status-validated"]').trigger('click');
      await flushPromises();
      expect(service.patchNode.firstCall.args[2]).toEqual({ status: 'VALIDATED' });
      expect(wrapper.get('[data-cy="ost-node-detail-status"]').text()).toBe('validated');

      service.patchNode.onSecondCall().rejects(apiError(400, 'error.invalidnotes'));
      const notes = wrapper.get('[data-cy="ost-notes"]');
      await notes.trigger('focus');
      await notes.setValue('We learned things');
      await notes.trigger('blur');
      await flushPromises();
      expect(service.patchNode.secondCall.args[2]).toEqual({ notes: 'We learned things' });
      expect(has(wrapper, 'ost-node-detail-error')).toBe(true);
      expect(tree.error).toBeNull();
      expect(tree.byId('opportunity-1')!.note).toBe('');
    });

    it('leaving for another node commits the note being typed to the node it was typed for', async () => {
      const { wrapper, router, service } = await mountPage('solution-1');
      service.patchNode.resolves(dto('solution-1', 'opportunity-1', { notes: 'Half typed', status: 'CANDIDATE' }));
      const notes = wrapper.get('[data-cy="ost-notes"]');
      (notes.element as HTMLTextAreaElement).focus();
      await notes.setValue('Half typed');
      await router.push('/trees/7/nodes/solution-2');
      await flushPromises();
      expect(service.patchNode.callCount).toBe(1);
      expect(service.patchNode.firstCall.args).toEqual(['solution', 1, { notes: 'Half typed' }]);
      expect((wrapper.get('[data-cy="ost-notes"]').element as HTMLTextAreaElement).value).toBe('');
    });
  });

  describe('delete', () => {
    it('asks for confirmation, then goes to the parent’s page', async () => {
      const { wrapper, router, service, tree, ui } = await mountPage('solution-1');
      service.deleteNode.resolves();
      await wrapper.get('[data-cy="ost-node-detail-delete"]').trigger('click');
      expect(ui.confirmId).toBe('solution-1');
      // what ConfirmDeleteDialog does on confirm
      await tree.deleteNode('solution-1');
      ui.cancelDelete();
      await flushPromises();
      expect(router.currentRoute.value.params.nodeKey).toBe('opportunity-1');
      expect(tree.byId('assumption-1')).toBeUndefined();
    });

    it('a product goes back to the canvas; cancelling stays', async () => {
      const { wrapper, router, service, tree, ui } = await mountPage('product-1');
      await wrapper.get('[data-cy="ost-node-detail-delete"]').trigger('click');
      ui.cancelDelete();
      await flushPromises();
      expect(router.currentRoute.value.name).toBe('OstNodeDetail');
      service.deleteNode.resolves();
      await wrapper.get('[data-cy="ost-node-detail-delete"]').trigger('click');
      await tree.deleteNode('product-1');
      ui.cancelDelete();
      await flushPromises();
      expect(router.currentRoute.value.name).toBe('OstCanvas');
      expect(router.currentRoute.value.params.teamId).toBe('7');
    });
  });

  it('viewers get everything read-only: no delete, quick-add, editable title or posting', async () => {
    const { wrapper } = await mountPage('opportunity-1', { canEdit: false, currentUserRole: 'VIEWER' });
    const title = wrapper.get('[data-cy="ost-node-detail-title"]');
    expect(title.element.tagName).toBe('H1');
    expect(title.text()).toBe('Hard to find people');
    expect(has(wrapper, 'ost-node-detail-delete')).toBe(false);
    expect(wrapper.findAll('[data-cy^="ost-node-detail-add-"]')).toHaveLength(0);
    expect(wrapper.get('[data-cy="ost-status-validated"]').attributes('disabled')).toBeDefined();
    expect(wrapper.get('[data-cy="ost-notes"]').attributes('readonly')).toBeDefined();
    expect(wrapper.get('[data-cy="ost-chat-input"]').attributes('disabled')).toBeDefined();
    expect(has(wrapper, 'ost-chat-readonly')).toBe(true);
    expect(has(wrapper, 'ost-node-detail-open-canvas')).toBe(true);
  });

  it('an unknown or foreign node key shows the not-found state', async () => {
    const { wrapper } = await mountPage('opportunity-999');
    expect(has(wrapper, 'ost-node-detail')).toBe(false);
    expect(wrapper.get('[data-cy="ost-node-detail-missing"]').text()).toContain('Node not found');
    expect(wrapper.get('[data-cy="ost-node-detail-missing-canvas"]').attributes('href')).toBe('/trees/7/canvas');
  });

  it('"Open on canvas" deep-links the node', async () => {
    const { wrapper } = await mountPage('assumption-1');
    expect(wrapper.get('[data-cy="ost-node-detail-open-canvas"]').attributes('href')).toBe('/trees/7/canvas?node=assumption-1');
  });
});

describe('metricTags', () => {
  it('lists nothing for products, outcomes and evidence', async () => {
    const { tree } = await setupStores();
    for (const key of ['product-1', 'outcome-1', 'evidence-1']) expect(metricTags(tree.byId(key)!, tree.nodes)).toEqual([]);
  });

  it('marks a solution without assumptions untested', async () => {
    const { tree } = await setupStores();
    expect(metricTags(tree.byId('solution-2')!, tree.nodes).map(t => t.label)).toEqual(['untested']);
  });
});
