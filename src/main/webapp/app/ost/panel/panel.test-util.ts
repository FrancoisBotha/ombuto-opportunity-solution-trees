/** Test-only: mounts panel components against a real tree store backed by a stubbed OstService. */
import { flushPromises, mount } from '@vue/test-utils';
import { createPinia, setActivePinia } from 'pinia';
import sinon, { type SinonStubbedInstance } from 'sinon';

import { dto, treeDto } from '../domain/fixtures.test-util';
import type { TreeNodeDTO } from '../ost.model';
import OstService from '../ost.service';
import { useOstTreeStore } from '../stores/ost-tree.store';
import { useOstUiStore } from '../stores/ost-ui.store';

export const apiError = (status: number, message?: string) => Object.assign(new Error('http'), { response: { status, data: { message } } });

export const PANEL_TREE: TreeNodeDTO[] = [
  dto('product-1', null, {
    title: 'Discovery Hub',
    links: [{ id: 1, name: 'Product space', url: 'https://ombuto.atlassian.net/wiki/spaces/product-1' }],
  }),
  dto('outcome-1', 'product-1', { title: 'Weekly interviews' }),
  dto('opportunity-1', 'outcome-1', {
    title: 'Hard to find people',
    status: 'EXPLORING',
    priority: 50,
    valueRating: 3,
    commentCount: 2,
    links: [
      { id: 11, name: 'Confluence', url: 'https://ombuto.atlassian.net/wiki/discovery/opportunity-1' },
      { id: 12, name: 'Jira Epic', url: 'https://ombuto.atlassian.net/browse/DISC-000' },
    ],
    questions: [
      { id: 1, text: 'Who?', done: false },
      { id: 2, text: 'When?', done: true },
      { id: 3, text: 'Why?', done: false },
    ],
    tags: [{ id: 100, name: 'Mobile Value Stream' }],
  }),
  dto('solution-1', 'opportunity-1', { title: 'In-app invite', status: 'CANDIDATE' }),
  dto('assumption-1', 'solution-1', { title: 'Users accept', status: 'SUPPORTED', confidence: 30, ownerLogin: 'user' }),
  dto('assumption-2', 'solution-1', { title: 'Sales agree', status: 'TESTING', confidence: 60 }),
  dto('evidence-1', 'assumption-2', { title: 'Test result' }),
  dto('solution-2', 'opportunity-1', { title: 'Lonely solution', status: 'BUILDING' }),
];

export async function setupStores(nodes: TreeNodeDTO[] = PANEL_TREE, extra: Parameters<typeof treeDto>[1] = {}) {
  localStorage.clear();
  const pinia = createPinia();
  setActivePinia(pinia);
  const service: SinonStubbedInstance<OstService> = sinon.createStubInstance(OstService);
  service.getTree.resolves(treeDto(nodes, extra));
  const tree = useOstTreeStore();
  const ui = useOstUiStore();
  tree.setServiceFactory(() => service);
  await tree.loadTree(7);
  return { pinia, service, tree, ui };
}

const RouterLinkStub = {
  name: 'RouterLink',
  props: ['to'],
  template: '<a :data-to="JSON.stringify(to)" v-bind="$attrs"><slot /></a>',
};

export async function mountWith<T>(component: T, pinia: ReturnType<typeof createPinia>, props: Record<string, unknown> = {}) {
  const wrapper = mount(component as any, {
    props,
    attachTo: document.body,
    global: { plugins: [pinia], stubs: { RouterLink: RouterLinkStub, 'router-link': RouterLinkStub } },
  });
  await flushPromises();
  return wrapper;
}
