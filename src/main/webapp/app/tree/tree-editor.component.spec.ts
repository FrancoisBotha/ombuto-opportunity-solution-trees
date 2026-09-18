import { beforeEach, describe, expect, it, vi } from 'vitest';

import { createTestingPinia } from '@pinia/testing';
import { type MountingOptions, shallowMount } from '@vue/test-utils';
import sinon, { type SinonStubbedInstance } from 'sinon';

let currentRouteParams: Record<string, any> = { teamId: '10' };
vi.mock('vue-router', () => ({
  useRoute: () => ({ params: currentRouteParams }),
  useRouter: () => ({ push: vi.fn() }),
}));

import { TeamRole } from '@/shared/model/enumerations/team-role.model';

import type { ITeamTree } from './tree.model';
import TreeEditor from './tree-editor.vue';
import TreeService from './tree.service';

type TreeEditorComponentType = InstanceType<typeof TreeEditor>;

const sampleTree = (): ITeamTree => ({
  id: 10,
  name: 'Alpha',
  description: 'desc',
  createdDate: null,
  currentUserRole: TeamRole.EDITOR,
  canEdit: true,
  products: [],
});

const mount = (treeServiceStub: SinonStubbedInstance<TreeService>, routeParams: Record<string, any> = { teamId: '10' }) => {
  currentRouteParams = routeParams;
  const options: MountingOptions<TreeEditorComponentType>['global'] = {
    plugins: [createTestingPinia({ stubActions: false })],
    stubs: {
      'font-awesome-icon': true,
      'router-link': true,
    },
    provide: {
      treeService: () => treeServiceStub,
    },
  };
  return shallowMount(TreeEditor, { global: options });
};

describe('TreeEditor Component', () => {
  let treeServiceStub: SinonStubbedInstance<TreeService>;

  beforeEach(() => {
    treeServiceStub = sinon.createStubInstance<TreeService>(TreeService);
  });

  it('renders the shell after a successful load', async () => {
    treeServiceStub.getTeamTree.resolves(sampleTree());
    const wrapper = mount(treeServiceStub);
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();
    expect(treeServiceStub.getTeamTree.calledOnce).toBe(true);
    expect(wrapper.find('[data-cy="treeEditorShell"]').exists()).toBe(true);
    expect(wrapper.find('[data-cy="treeEditorTeamName"]').text()).toContain('Alpha');
  });

  it('shows an access-denied message on 403', async () => {
    treeServiceStub.getTeamTree.rejects({ response: { status: 403 } });
    const wrapper = mount(treeServiceStub);
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();
    expect(wrapper.find('[data-cy="treeEditorAccessDenied"]').exists()).toBe(true);
    expect(wrapper.find('[data-cy="treeEditorShell"]').exists()).toBe(false);
  });

  it('shows a not-found message on 404', async () => {
    treeServiceStub.getTeamTree.rejects({ response: { status: 404 } });
    const wrapper = mount(treeServiceStub);
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();
    expect(wrapper.find('[data-cy="treeEditorNotFound"]').exists()).toBe(true);
  });

  it('marks the shell as read-only when canEdit is false', async () => {
    const readOnly = sampleTree();
    readOnly.canEdit = false;
    treeServiceStub.getTeamTree.resolves(readOnly);
    const wrapper = mount(treeServiceStub);
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();
    expect(wrapper.find('[data-cy="treeEditorReadOnly"]').exists()).toBe(true);
  });
});
