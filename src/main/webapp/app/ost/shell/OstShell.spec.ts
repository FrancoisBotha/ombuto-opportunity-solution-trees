import { beforeEach, describe, expect, it, vi } from 'vitest';

import { flushPromises, mount } from '@vue/test-utils';
import { createPinia } from 'pinia';
import { Subject } from 'rxjs';
import sinon, { type SinonStubbedInstance } from 'sinon';
import { type Router, createMemoryHistory, createRouter } from 'vue-router';

import { dto, treeDto } from '../domain/fixtures.test-util';
import OstService from '../ost.service';
import { useOstRealtimeStore } from '../stores/ost-realtime.store';
import { useOstTreeStore } from '../stores/ost-tree.store';
import { useOstUiStore } from '../stores/ost-ui.store';

import OstShell from './OstShell.vue';

const Page = (cy: string) => ({ template: `<div data-cy="${cy}"></div>` });

const apiError = (status: number) => Object.assign(new Error('http'), { response: { status, data: {} } });

describe('OstShell', () => {
  let service: SinonStubbedInstance<OstService>;
  let router: Router;
  let pinia: ReturnType<typeof createPinia>;
  let stomp: {
    connectionState$: Subject<number>;
    configure: ReturnType<typeof vi.fn>;
    activate: ReturnType<typeof vi.fn>;
    deactivate: ReturnType<typeof vi.fn>;
    watch: ReturnType<typeof vi.fn>;
  };

  beforeEach(() => {
    localStorage.clear();
    service = sinon.createStubInstance(OstService);
    service.listMyTeams.resolves([
      { id: 7, name: 'Team Jupiter', role: 'OWNER', memberCount: 2, productCount: 1 },
      { id: 8, name: 'Team Venus', role: 'OWNER', memberCount: 2, productCount: 0 },
    ]);
    service.getTree.callsFake(async (id: number) =>
      id === 7 ? treeDto([dto('product-1', null), dto('outcome-1', 'product-1')]) : treeDto([], { id, name: 'Team Venus' }),
    );
    pinia = createPinia();
    stomp = {
      connectionState$: new Subject(),
      configure: vi.fn(),
      activate: vi.fn(),
      deactivate: vi.fn(async () => undefined),
      watch: vi.fn(() => new Subject()),
    };
    useOstRealtimeStore(pinia).setClientFactory(() => stomp as any);
    router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/trees', component: Page('landing') },
        {
          path: '/trees/:teamId',
          component: OstShell,
          children: [
            { path: '', name: 'OstDashboard', component: Page('dash') },
            { path: 'canvas', name: 'OstCanvas', component: Page('canvas') },
            { path: 'experiments', name: 'OstExperiments', component: Page('experiments') },
            { path: 'nodes/:nodeKey', name: 'OstNodeDetail', component: Page('node') },
          ],
        },
      ],
    });
  });

  async function mountAt(path: string) {
    await router.push(path);
    await router.isReady();
    const wrapper = mount(
      { template: '<router-view />' },
      { attachTo: document.body, global: { plugins: [router, pinia], provide: { ostService: () => service } } },
    );
    await flushPromises();
    return wrapper;
  }

  it('loads the tree and renders the nav: brand, tabs, team combo and member avatars', async () => {
    const wrapper = await mountAt('/trees/7');
    expect(service.getTree.calledWith(7)).toBe(true);
    expect(wrapper.find('.ost-root').exists()).toBe(true);
    expect(wrapper.find('[data-cy="dash"]').exists()).toBe(true);
    expect(wrapper.find('[data-cy="ostTeamComboLabel"]').text()).toBe('Team Jupiter');
    expect(wrapper.findAll('[data-cy^="ostMember-"]').map(a => a.text())).toEqual(['KP', 'AR']);
    expect(wrapper.find('[data-cy="ostMember-admin"]').attributes('title')).toBe('Ana R (viewer)');
    expect(wrapper.find('[data-cy="ostTabTrees"]').classes()).toContain('is-active');
    wrapper.unmount();
  });

  it('routes between views with the tabs', async () => {
    const wrapper = await mountAt('/trees/7');
    await wrapper.find('[data-cy="ostTabCanvas"]').trigger('click');
    await flushPromises();
    expect(router.currentRoute.value.fullPath).toBe('/trees/7/canvas');
    expect(wrapper.find('[data-cy="canvas"]').exists()).toBe(true);
    expect(service.getTree.callCount).toBe(1);
    wrapper.unmount();
  });

  it('switches team from the combo, keeping the current view, and reloads the tree', async () => {
    const wrapper = await mountAt('/trees/7/experiments');
    await wrapper.find('[data-cy="ostTeamComboButton"]').trigger('click');
    expect(wrapper.findAll('[role="option"]')).toHaveLength(2);
    await wrapper.find('[data-cy="ostTeamOption-8"]').trigger('click');
    await flushPromises();
    expect(router.currentRoute.value.fullPath).toBe('/trees/8/experiments');
    expect(service.getTree.calledWith(8)).toBe(true);
    expect(wrapper.find('[data-cy="ostTeamComboLabel"]').text()).toBe('Team Venus');
    wrapper.unmount();
  });

  it('hands received events to the tree store so a remote change reaches the flat node list', async () => {
    // Guards the applyEvents wiring: the transport used to validate and batch every event and then
    // drop it on the floor, which made live collaboration silently a no-op.
    const realtime = useOstRealtimeStore(pinia);
    const frames: FrameRequestCallback[] = [];
    realtime.setFrameScheduler(callback => frames.push(callback), vi.fn());
    const messages = new Subject<{ body: string }>();
    stomp.watch.mockReturnValue(messages);

    const wrapper = await mountAt('/trees/7');
    const tree = useOstTreeStore(pinia);
    expect(tree.byId('outcome-1')?.title).not.toBe('Renamed remotely');

    messages.next({
      body: JSON.stringify({
        type: 'NODE_UPDATED',
        teamId: 7,
        seq: 1,
        epoch: 'e1',
        actingUserLogin: 'someone-else',
        at: '2026-09-20T00:00:00Z',
        payload: dto('outcome-1', 'product-1', { title: 'Renamed remotely' }),
      }),
    });
    frames.shift()?.(performance.now());
    await flushPromises();

    expect(tree.byId('outcome-1')?.title).toBe('Renamed remotely');
    wrapper.unmount();
  });

  /**
   * S2 / FR-034 — a SUBSCRIBE is authorised once, when it is made, and being removed from a team
   * does not end the STOMP session. The shell has to drop the subscription itself the moment the
   * removal event arrives; otherwise a former member's socket keeps carrying the team's tree.
   */
  it('drops the realtime subscription when this user is removed from the open team', async () => {
    const realtime = useOstRealtimeStore(pinia);
    const frames: FrameRequestCallback[] = [];
    realtime.setFrameScheduler(callback => frames.push(callback), vi.fn());
    const messages = new Subject<{ body: string }>();
    stomp.watch.mockReturnValue(messages);

    const wrapper = await mountAt('/trees/7');
    expect(stomp.deactivate).not.toHaveBeenCalled();

    messages.next({
      body: JSON.stringify({
        type: 'MEMBERSHIP_CHANGED',
        teamId: 7,
        seq: 1,
        epoch: 'e1',
        actingUserLogin: 'admin',
        at: '2026-09-20T00:00:00Z',
        payload: { login: useOstTreeStore(pinia).team?.currentUserLogin, role: null, removed: true },
      }),
    });
    frames.shift()?.(performance.now());
    await flushPromises();

    expect(useOstTreeStore(pinia).removedFromCurrentTeam).toBe(true);
    expect(stomp.deactivate).toHaveBeenCalled();
    expect(realtime.activeTeamId).toBeNull();
    wrapper.unmount();
  });

  it('clears team-scoped UI state when switching team', async () => {
    const wrapper = await mountAt('/trees/7');
    useOstUiStore(pinia).select('product-1');
    await router.push('/trees/8');
    await flushPromises();
    expect(useOstUiStore(pinia).selectedId).toBeNull();
    wrapper.unmount();
  });

  it('shows not-found for a non-numeric team id without calling the API', async () => {
    const wrapper = await mountAt('/trees/abc');
    expect(wrapper.find('[data-cy="ostNotFound"]').exists()).toBe(true);
    expect(wrapper.find('[data-cy="dash"]').exists()).toBe(false);
    expect(service.getTree.called).toBe(false);
    wrapper.unmount();
  });

  it('shows forbidden when the tree read is refused', async () => {
    service.getTree.rejects(apiError(403));
    const wrapper = await mountAt('/trees/404');
    expect(wrapper.find('[data-cy="ostForbidden"]').exists()).toBe(true);
    expect(wrapper.find('[data-cy="dash"]').exists()).toBe(false);
    wrapper.unmount();
  });

  it('offers a retry when loading fails for another reason', async () => {
    service.getTree.onFirstCall().rejects(apiError(500));
    const wrapper = await mountAt('/trees/7');
    expect(wrapper.find('[data-cy="ostLoadError"]').exists()).toBe(true);
    await wrapper.find('[data-cy="ostRetry"]').trigger('click');
    await flushPromises();
    expect(wrapper.find('[data-cy="dash"]').exists()).toBe(true);
    wrapper.unmount();
  });

  it('asks before deleting, naming the node and its descendants, and cascades on confirm', async () => {
    service.deleteNode.resolves();
    const wrapper = await mountAt('/trees/7');
    const ui = useOstUiStore(pinia);
    ui.askDelete('product-1');
    await flushPromises();
    const dialog = wrapper.find('[data-cy="ostConfirmDelete"]');
    expect(dialog.attributes('role')).toBe('dialog');
    expect(dialog.text()).toContain('Delete this product?');
    expect(wrapper.find('[data-cy="ostConfirmDeleteText"]').text()).toContain('“Title product-1” and its 1 descendant will be removed');
    expect(document.activeElement?.getAttribute('data-cy')).toBe('ostConfirmDeleteCancel');
    await wrapper.find('[data-cy="ostConfirmDeleteConfirm"]').trigger('click');
    await flushPromises();
    expect(service.deleteNode.calledWith('product', 1)).toBe(true);
    expect(wrapper.find('[data-cy="ostConfirmDelete"]').exists()).toBe(false);
    expect(ui.confirmId).toBeNull();
    wrapper.unmount();
  });

  it('closes dialogs on Escape and on backdrop click without deleting', async () => {
    const wrapper = await mountAt('/trees/7');
    const ui = useOstUiStore(pinia);
    ui.askDelete('outcome-1');
    await flushPromises();
    await wrapper.find('[data-cy="ostConfirmDelete"]').trigger('keydown', { key: 'Escape' });
    expect(ui.confirmId).toBeNull();
    ui.askDelete('outcome-1');
    await flushPromises();
    await wrapper.find('[data-cy="ostDialogBackdrop"]').trigger('click');
    expect(ui.confirmId).toBeNull();
    expect(service.deleteNode.called).toBe(false);
    wrapper.unmount();
  });

  it('traps Tab focus inside the dialog', async () => {
    const wrapper = await mountAt('/trees/7');
    useOstUiStore(pinia).askDelete('outcome-1');
    await flushPromises();
    const confirm = wrapper.find('[data-cy="ostConfirmDeleteConfirm"]');
    (confirm.element as HTMLElement).focus();
    await wrapper.find('[data-cy="ostConfirmDelete"]').trigger('keydown', { key: 'Tab' });
    expect(document.activeElement?.getAttribute('data-cy')).toBe('ostConfirmDeleteCancel');
    await wrapper.find('[data-cy="ostConfirmDelete"]').trigger('keydown', { key: 'Tab', shiftKey: true });
    expect(document.activeElement?.getAttribute('data-cy')).toBe('ostConfirmDeleteConfirm');
    wrapper.unmount();
  });
});
