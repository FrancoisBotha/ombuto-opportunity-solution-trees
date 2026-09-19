import { afterEach, beforeEach, describe, expect, it } from 'vitest';

import { type VueWrapper, flushPromises, mount } from '@vue/test-utils';
import { createPinia } from 'pinia';
import sinon, { type SinonStubbedInstance } from 'sinon';
import { type Router, createMemoryHistory, createRouter } from 'vue-router';

import { dto, treeDto } from '../domain/fixtures.test-util';
import type { TeamTreeDTO } from '../ost.model';
import OstService from '../ost.service';
import { useOstTreeStore } from '../stores/ost-tree.store';

import ExperimentsPage from './ExperimentsPage.vue';
import TreesDashboardPage from './TreesDashboardPage.vue';

const Stub = (cy: string) => ({ template: `<div data-cy="${cy}"></div>` });

/** Two products; branch 1 has two outcomes, nested opportunities, solutions, assumptions and evidence. */
function fixtureTree(): TeamTreeDTO {
  return treeDto(
    [
      dto('product-1', null, {
        title: 'Discovery Hub',
        sortOrder: 1,
        lastActivity: { at: new Date(Date.now() - 5 * 60_000).toISOString(), byLogin: 'user' },
      }),
      dto('outcome-1', 'product-1', { title: 'Teams interview weekly: 34% → 60%' }),
      dto('outcome-2', 'product-1', { title: 'Second outcome', sortOrder: 1 }),
      dto('opportunity-1', 'outcome-1', { status: 'EXPLORING', priority: 80, valueRating: 4 }),
      dto('opportunity-2', 'opportunity-1', { status: 'UNEXPLORED' }),
      dto('solution-1', 'opportunity-1', { title: 'In-app invite', status: 'CANDIDATE' }),
      dto('assumption-1', 'solution-1', { title: 'Users accept invites', status: 'TESTING', confidence: 40, ownerLogin: 'user' }),
      dto('assumption-2', 'solution-1', { title: 'Sales won’t block', status: 'UNTESTED', confidence: 20, sortOrder: 1 }),
      dto('evidence-1', 'opportunity-1'),
      dto('evidence-2', 'assumption-1'),
      dto('product-2', null, { title: 'Insights', sortOrder: 2, archived: true }),
      dto('outcome-3', 'product-2', { title: 'Insight outcome' }),
      dto('opportunity-3', 'outcome-3', { status: 'VALIDATED' }),
      dto('solution-2', 'opportunity-3', { title: 'Auto-tagging', status: 'BUILDING' }),
      dto('assumption-3', 'solution-2', { title: 'Tags are accurate', status: 'SUPPORTED', confidence: 80, ownerLogin: 'admin' }),
      dto('assumption-4', 'solution-2', { title: 'Volume correlates', status: 'REFUTED', confidence: 10, ownerLogin: 'ghost' }),
      dto('assumption-5', 'solution-2', { title: 'Another test', status: 'TESTING', confidence: 55 }),
    ],
    { evidenceThisMonth: 5 },
  );
}

describe('Trees dashboard and Experiments tracker', () => {
  let service: SinonStubbedInstance<OstService>;
  let router: Router;
  let pinia: ReturnType<typeof createPinia>;
  let wrapper: VueWrapper | null = null;

  beforeEach(() => {
    localStorage.clear();
    service = sinon.createStubInstance(OstService);
    pinia = createPinia();
    router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/teams/:id', component: Stub('team') },
        {
          path: '/trees/:teamId',
          component: { template: '<router-view />' },
          children: [
            { path: '', name: 'OstDashboard', component: TreesDashboardPage },
            { path: 'canvas', name: 'OstCanvas', component: Stub('canvas') },
            { path: 'experiments', name: 'OstExperiments', component: ExperimentsPage },
          ],
        },
      ],
    });
  });

  afterEach(() => {
    wrapper?.unmount();
    wrapper = null;
  });

  async function mountAt(path: string, tree: TeamTreeDTO = fixtureTree()) {
    service.getTree.resolves(tree);
    const store = useOstTreeStore(pinia);
    store.setServiceFactory(() => service);
    await store.loadTree(tree.id);
    await router.push(path);
    await router.isReady();
    wrapper = mount({ template: '<router-view />' }, { attachTo: document.body, global: { plugins: [router, pinia] } });
    await flushPromises();
    return wrapper;
  }

  const text = (w: VueWrapper, cy: string) => w.find(`[data-cy="${cy}"]`).text();

  describe('dashboard', () => {
    it('shows the team kicker and the four counters from the tree (A5: evidence this month from the server)', async () => {
      const w = await mountAt('/trees/7');
      expect(w.find('[data-cy="ost-dashboard"]').exists()).toBe(true);
      expect(text(w, 'ostDashboardKicker')).toBe('Team Jupiter · continuous discovery');
      const value = (key: string) => w.find(`[data-cy="ost-stat-${key}"] [data-cy="ost-stat-value"]`).text();
      expect(value('opportunities')).toBe('3');
      expect(value('solutions')).toBe('2');
      expect(value('tests')).toBe('2');
      expect(value('evidence')).toBe('5');
      expect(text(w, 'ost-stat-evidence')).toContain('Evidence this month');
    });

    it('renders one card per product with kicker, outcomes, mini counts and last edit', async () => {
      const w = await mountAt('/trees/7');
      const cards = w.findAll('[data-cy^="ost-product-card-"]');
      expect(cards.map(c => c.attributes('data-cy'))).toEqual(['ost-product-card-product-1', 'ost-product-card-product-2']);

      const first = w.find('[data-cy="ost-product-card-product-1"]');
      expect(first.text()).toContain('Product branch 01');
      expect(first.text()).toContain('Discovery Hub');
      expect(first.text()).toContain('Teams interview weekly: 34% → 60%');
      expect(first.text()).toContain('Second outcome');
      const count = (card: typeof first, key: string) => card.find(`[data-cy="ost-product-count-${key}"]`).text();
      expect(count(first, 'opportunities')).toBe('2Opps');
      expect(count(first, 'solutions')).toBe('1Sols');
      expect(count(first, 'assumptions')).toBe('2Tests');
      expect(count(first, 'evidence')).toBe('2Evid');
      expect(first.find('[data-cy="ost-product-updated"]').text()).toBe('Last edited 5m ago by Kira P.');
      expect(first.find('[data-cy="ost-product-archived"]').exists()).toBe(false);

      const second = w.find('[data-cy="ost-product-card-product-2"]');
      expect(second.text()).toContain('Product branch 02');
      expect(second.find('[data-cy="ost-product-archived"]').text()).toBe('Archived');
      expect(count(second, 'assumptions')).toBe('3Tests');
    });

    it('"Open branch" links to the canvas scoped to that product', async () => {
      const w = await mountAt('/trees/7');
      expect(w.find('[data-cy="ost-open-branch-product-2"]').attributes('href')).toBe('/trees/7/canvas?product=product-2');
      await w.find('[data-cy="ost-open-branch-product-1"]').trigger('click');
      await flushPromises();
      expect(router.currentRoute.value.fullPath).toBe('/trees/7/canvas?product=product-1');
    });

    it('shows zero counters and an empty state for a team without products', async () => {
      const w = await mountAt('/trees/7', treeDto([], { evidenceThisMonth: 0 }));
      for (const key of ['opportunities', 'solutions', 'tests', 'evidence']) {
        expect(w.find(`[data-cy="ost-stat-${key}"] [data-cy="ost-stat-value"]`).text()).toBe('0');
      }
      expect(w.findAll('[data-cy^="ost-product-card-"]')).toHaveLength(0);
      expect(w.find('[data-cy="ost-dashboard-empty"]').exists()).toBe(true);
      expect(w.find('[data-cy="ost-dashboard-go-to-team"]').attributes('href')).toBe('/teams/7');
    });

    it('is the same read-only page for a viewer', async () => {
      const w = await mountAt('/trees/7', { ...fixtureTree(), canEdit: false, currentUserRole: 'VIEWER' });
      expect(w.findAll('[data-cy^="ost-product-card-"]')).toHaveLength(2);
      expect(w.findAll('input, textarea, select')).toHaveLength(0);
    });
  });

  describe('experiments', () => {
    it('lists every assumption in tree order with solution, owner, status and confidence', async () => {
      const w = await mountAt('/trees/7/experiments');
      expect(w.find('[data-cy="ost-experiments"]').exists()).toBe(true);
      const rows = w.findAll('[data-cy^="ost-experiment-row-"]');
      expect(rows.map(r => r.attributes('data-cy'))).toEqual([
        'ost-experiment-row-assumption-1',
        'ost-experiment-row-assumption-2',
        'ost-experiment-row-assumption-3',
        'ost-experiment-row-assumption-4',
        'ost-experiment-row-assumption-5',
      ]);
      const cells = rows[0].findAll('td').map(td => td.text());
      expect(cells).toEqual(['Users accept invites', 'In-app invite', 'Kira P.', 'testing', '40%']);
      const owner = (i: number) => rows[i].find('[data-cy="ost-experiment-owner"]').text();
      expect(owner(1)).toBe('Unassigned');
      expect(owner(2)).toBe('Ana R.');
      expect(owner(3)).toBe('ghost');
    });

    it('uses the three status tones', async () => {
      const w = await mountAt('/trees/7/experiments');
      const badge = (key: string) => w.find(`[data-cy="ost-experiment-row-${key}"] [data-cy="ost-experiment-status"]`).classes();
      expect(badge('assumption-1')).toContain('ost-badge--flight');
      expect(badge('assumption-2')).toContain('ost-badge--flight');
      expect(badge('assumption-3')).toContain('ost-badge--good');
      expect(badge('assumption-4')).toContain('ost-badge--bad');
    });

    it('summarises running, queued and settled tests', async () => {
      const w = await mountAt('/trees/7/experiments');
      expect(text(w, 'ost-experiments-summary')).toBe('2 running · 1 queued · 2 settled');
    });

    it('opens the node on the canvas, scoped to its product, on click, Enter and Space', async () => {
      const w = await mountAt('/trees/7/experiments');
      await w.find('[data-cy="ost-experiment-row-assumption-3"]').trigger('click');
      await flushPromises();
      expect(router.currentRoute.value.fullPath).toBe('/trees/7/canvas?node=assumption-3&product=product-2');

      await router.push('/trees/7/experiments');
      await flushPromises();
      const row = w.find('[data-cy="ost-experiment-row-assumption-1"]');
      expect(row.attributes('tabindex')).toBe('0');
      await row.trigger('keydown', { key: 'Enter' });
      await flushPromises();
      expect(router.currentRoute.value.fullPath).toBe('/trees/7/canvas?node=assumption-1&product=product-1');

      await router.push('/trees/7/experiments');
      await flushPromises();
      await w.find('[data-cy="ost-experiment-row-assumption-2"]').trigger('keydown', { key: ' ' });
      await flushPromises();
      expect(router.currentRoute.value.fullPath).toBe('/trees/7/canvas?node=assumption-2&product=product-1');
    });

    it('shows an empty state when the tree has no assumptions', async () => {
      const w = await mountAt('/trees/7/experiments', treeDto([dto('product-1', null), dto('outcome-1', 'product-1')]));
      expect(w.findAll('[data-cy^="ost-experiment-row-"]')).toHaveLength(0);
      expect(w.find('table').exists()).toBe(false);
      expect(w.find('[data-cy="ost-experiments-empty"]').exists()).toBe(true);
      expect(text(w, 'ost-experiments-summary')).toBe('0 running · 0 queued · 0 settled');
      expect(w.find('[data-cy="ost-experiments-open-canvas"]').attributes('href')).toBe('/trees/7/canvas');
    });
  });
});
