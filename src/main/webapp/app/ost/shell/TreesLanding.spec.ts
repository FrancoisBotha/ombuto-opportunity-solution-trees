import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { type VueWrapper, flushPromises, mount } from '@vue/test-utils';
import sinon, { type SinonStubbedInstance } from 'sinon';
import { computed } from 'vue';
import { type Router, createMemoryHistory, createRouter } from 'vue-router';

import OstService from '../ost.service';
import { writeLastTeam } from '../stores/ost-ui.store';

import TreesLanding from './TreesLanding.vue';

const TEAMS = [
  { id: 7, name: 'Team Jupiter', role: 'OWNER' as const, memberCount: 2, productCount: 1 },
  { id: 8, name: 'Team Venus', role: 'EDITOR' as const, memberCount: 2, productCount: 0 },
];

describe('TreesLanding', () => {
  let service: SinonStubbedInstance<OstService>;
  let router: Router;
  let dashboard: () => Promise<unknown>;
  let wrapper: VueWrapper | null = null;

  beforeEach(() => {
    localStorage.clear();
    service = sinon.createStubInstance(OstService);
    dashboard = async () => ({ template: '<div data-cy="dash"></div>' });
    router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/trees', component: TreesLanding },
        { path: '/trees/:teamId', name: 'OstDashboard', component: () => dashboard() as any },
      ],
    });
  });

  afterEach(() => {
    wrapper?.unmount();
    wrapper = null;
    vi.unstubAllGlobals();
  });

  async function mountLanding() {
    await router.push('/trees');
    await router.isReady();
    wrapper = mount(TreesLanding, {
      global: { plugins: [router], provide: { ostService: () => service, currentUsername: computed(() => 'user') } },
    });
    await flushPromises();
    return wrapper;
  }

  it('sends the user to the last team used', async () => {
    writeLastTeam('user', 8);
    service.listMyTeams.resolves(TEAMS);
    await mountLanding();
    expect(router.currentRoute.value.fullPath).toBe('/trees/8');
  });

  it('shows the empty state when the user is in no team', async () => {
    service.listMyTeams.resolves([]);
    const w = await mountLanding();
    expect(w.find('[data-cy="ostTreesEmpty"]').exists()).toBe(true);
  });

  it('offers a retry when the teams cannot be loaded', async () => {
    service.listMyTeams.rejects(new Error('offline'));
    const w = await mountLanding();
    expect(w.find('[data-cy="ostTreesError"]').text()).toContain('Your teams could not be loaded');
    expect(w.find('[data-cy="ostTreesReload"]').exists()).toBe(false);

    service.listMyTeams.resolves(TEAMS);
    await w.find('[data-cy="ostTreesRetry"]').trigger('click');
    await flushPromises();
    expect(router.currentRoute.value.fullPath).toBe('/trees/7');
  });

  it('asks for a reload, not a retry, when the page chunk fails to load', async () => {
    service.listMyTeams.resolves(TEAMS);
    dashboard = () => Promise.reject(new TypeError('Failed to fetch dynamically imported module'));
    const w = await mountLanding();
    expect(w.find('[data-cy="ostTreesError"]').exists()).toBe(false);
    expect(w.find('[data-cy="ostTreesReload"]').text()).toContain('Reload the page');

    const reload = vi.fn();
    vi.stubGlobal('location', { ...window.location, reload });
    await w.find('[data-cy="ostTreesReloadButton"]').trigger('click');
    expect(reload).toHaveBeenCalled();
  });
});
