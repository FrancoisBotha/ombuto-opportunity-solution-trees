import { beforeEach, describe, expect, it, vi } from 'vitest';

import { createTestingPinia } from '@pinia/testing';
import { mount } from '@vue/test-utils';
import sinon, { type SinonStubbedInstance } from 'sinon';

vi.mock('vue-router', () => ({
  useRoute: () => ({ params: { teamId: '10' } }),
  useRouter: () => ({ push: vi.fn() }),
}));

import { OpportunityStatus } from '@/shared/model/enumerations/opportunity-status.model';
import { OutcomeStatus } from '@/shared/model/enumerations/outcome-status.model';
import { SolutionStatus } from '@/shared/model/enumerations/solution-status.model';
import { TeamRole } from '@/shared/model/enumerations/team-role.model';

import type { ITeamTree } from './tree.model';
import TreeDetailPanel from './tree-detail-panel.vue';
import TreeService from './tree.service';
import { useTreeStore } from './tree.store';

const populatedTree = (): ITeamTree => ({
  id: 10,
  name: 'Alpha',
  description: null,
  createdDate: null,
  currentUserRole: TeamRole.EDITOR,
  canEdit: true,
  products: [
    {
      id: 1,
      name: 'Product One',
      description: 'p-desc',
      vision: 'p-vision',
      archived: false,
      outcomes: [
        {
          id: 10,
          title: 'Outcome A',
          description: 'o-desc',
          status: OutcomeStatus.ACTIVE,
          sortOrder: 0,
          opportunities: [
            {
              id: 100,
              title: 'Opp',
              description: null,
              status: OpportunityStatus.IDENTIFIED,
              valuerating: 3,
              complexity: 3,
              sortOrder: 0,
              parentId: null,
              children: [],
              solutions: [{ id: 1000, title: 'Sol', description: null, status: SolutionStatus.IDEA, effort: 3, sortOrder: 0 } as any],
            },
          ],
        },
      ],
    },
  ],
});

const mountPanel = (svc: SinonStubbedInstance<TreeService>) => {
  const pinia = createTestingPinia({ stubActions: false });
  const wrapper = mount(TreeDetailPanel, {
    global: {
      plugins: [pinia],
      stubs: { 'font-awesome-icon': true, 'router-link': true },
      provide: {
        treeService: () => svc,
      },
    },
  });
  const store = useTreeStore();
  store.setTree(populatedTree());
  return { wrapper, store };
};

const flush = async (wrapper: { vm: { $nextTick: () => Promise<void> } }) => {
  await wrapper.vm.$nextTick();
  await wrapper.vm.$nextTick();
  await wrapper.vm.$nextTick();
};

describe('TreeDetailPanel', () => {
  let svc: SinonStubbedInstance<TreeService>;

  beforeEach(() => {
    svc = sinon.createStubInstance<TreeService>(TreeService);
  });

  it('is not rendered when nothing is selected', async () => {
    const { wrapper, store } = mountPanel(svc);
    await flush(wrapper);
    store.clearSelection();
    await flush(wrapper);
    expect(wrapper.find('[data-cy="treeDetailPanel"]').exists()).toBe(false);
  });

  it('opens with product fields when a product is selected and closes when cleared', async () => {
    const { wrapper, store } = mountPanel(svc);
    store.selectNode('product', 1);
    await flush(wrapper);
    expect(wrapper.find('[data-cy="treeDetailPanel"]').exists()).toBe(true);
    expect((wrapper.find('[data-cy="detailProductName"]').element as HTMLInputElement).value).toBe('Product One');
    expect((wrapper.find('[data-cy="detailProductDescription"]').element as HTMLTextAreaElement).value).toBe('p-desc');
    expect((wrapper.find('[data-cy="detailProductVision"]').element as HTMLTextAreaElement).value).toBe('p-vision');
    expect(wrapper.find('[data-cy="detailProductArchived"]').exists()).toBe(true);
    store.clearSelection();
    await flush(wrapper);
    expect(wrapper.find('[data-cy="treeDetailPanel"]').exists()).toBe(false);
  });

  it('opens with outcome fields (title, description, status) when an outcome is selected', async () => {
    const { wrapper, store } = mountPanel(svc);
    store.selectNode('outcome', 10);
    await flush(wrapper);
    expect(wrapper.find('[data-cy="treeDetailPanel"]').exists()).toBe(true);
    expect((wrapper.find('[data-cy="detailTitle"]').element as HTMLInputElement).value).toBe('Outcome A');
    const status = wrapper.find('[data-cy="detailStatus"]');
    expect(status.exists()).toBe(true);
    const options = status.findAll('option').map(o => o.attributes('value'));
    expect(options).toEqual(expect.arrayContaining(['DRAFT', 'ACTIVE', 'ACHIEVED', 'ABANDONED']));
  });

  it('opens with opportunity fields (title, description, status, ratings) when an opportunity is selected', async () => {
    const { wrapper, store } = mountPanel(svc);
    store.selectNode('opportunity', 100);
    await flush(wrapper);
    expect((wrapper.find('[data-cy="detailTitle"]').element as HTMLInputElement).value).toBe('Opp');
    const status = wrapper.find('[data-cy="detailStatus"]');
    const options = status.findAll('option').map(o => o.attributes('value'));
    expect(options).toEqual(
      expect.arrayContaining(['IDENTIFIED', 'EXPLORING', 'PRIORITISED', 'IN_PROGRESS', 'ADDRESSED', 'PARKED', 'DISCARDED']),
    );
    expect(wrapper.find('[data-cy="detailValuerating"]').exists()).toBe(true);
    expect(wrapper.find('[data-cy="detailComplexity"]').exists()).toBe(true);
  });

  it('opens with solution fields (title, description, status, effort) when a solution is selected', async () => {
    const { wrapper, store } = mountPanel(svc);
    store.selectNode('solution', 1000);
    await flush(wrapper);
    const status = wrapper.find('[data-cy="detailStatus"]');
    const options = status.findAll('option').map(o => o.attributes('value'));
    expect(options).toEqual(
      expect.arrayContaining(['IDEA', 'ASSUMPTION_MAPPING', 'TESTING', 'VALIDATED', 'INVALIDATED', 'SHIPPED', 'DROPPED']),
    );
    expect(wrapper.find('[data-cy="detailEffort"]').exists()).toBe(true);
  });

  it('saves an outcome via PATCH and updates the node in place with the new status', async () => {
    (svc as any).updateOutcome = sinon.stub().resolves({
      id: 10,
      title: 'Outcome A renamed',
      description: 'o-desc',
      status: OutcomeStatus.ACHIEVED,
      sortOrder: 0,
    });
    const { wrapper, store } = mountPanel(svc);
    store.selectNode('outcome', 10);
    await flush(wrapper);
    await wrapper.find('[data-cy="detailTitle"]').setValue('Outcome A renamed');
    await wrapper.find('[data-cy="detailStatus"]').setValue(OutcomeStatus.ACHIEVED);
    await wrapper.find('[data-cy="detailSave"]').trigger('click');
    await flush(wrapper);
    expect((svc as any).updateOutcome.calledOnce).toBe(true);
    const args = (svc as any).updateOutcome.firstCall.args;
    expect(args[0]).toBe(10);
    expect(args[1].title).toBe('Outcome A renamed');
    expect(args[1].status).toBe(OutcomeStatus.ACHIEVED);
    const updated = store.findNode('outcome', 10) as any;
    expect(updated.title).toBe('Outcome A renamed');
    expect(updated.status).toBe(OutcomeStatus.ACHIEVED);
    // Descendants preserved.
    expect(updated.opportunities.length).toBe(1);
    // Only one call to getTeamTree ever (never during save).
    expect((svc as any).getTeamTree.callCount).toBe(0);
  });

  it('saves through a default TreeService when the app provides none', async () => {
    const updateOutcome = sinon.stub(TreeService.prototype, 'updateOutcome').resolves({
      id: 10,
      title: 'Outcome A renamed',
      description: 'o-desc',
      status: OutcomeStatus.ACTIVE,
      sortOrder: 0,
    } as any);
    try {
      const wrapper = mount(TreeDetailPanel, {
        global: { plugins: [createTestingPinia({ stubActions: false })], stubs: { 'font-awesome-icon': true, 'router-link': true } },
      });
      const store = useTreeStore();
      store.setTree(populatedTree());
      store.selectNode('outcome', 10);
      await flush(wrapper);
      await wrapper.find('[data-cy="detailTitle"]').setValue('Outcome A renamed');
      await wrapper.find('[data-cy="detailSave"]').trigger('click');
      await flush(wrapper);
      expect(updateOutcome.calledOnce).toBe(true);
      expect((store.findNode('outcome', 10) as any).title).toBe('Outcome A renamed');
    } finally {
      updateOutcome.restore();
    }
  });

  it('toggling archived on a product and saving sends archived and updates the node in place', async () => {
    (svc as any).updateProduct = sinon.stub().resolves({
      id: 1,
      name: 'Product One',
      description: 'p-desc',
      vision: 'p-vision',
      archived: true,
    });
    const { wrapper, store } = mountPanel(svc);
    store.selectNode('product', 1);
    await flush(wrapper);
    await wrapper.find('[data-cy="detailProductArchived"]').setValue(true);
    await wrapper.find('[data-cy="detailSave"]').trigger('click');
    await flush(wrapper);
    expect((svc as any).updateProduct.calledOnce).toBe(true);
    expect((svc as any).updateProduct.firstCall.args[1].archived).toBe(true);
    expect((store.findNode('product', 1) as any).archived).toBe(true);
    expect((store.findNode('product', 1) as any).outcomes.length).toBe(1);
  });

  it('blocks saving when required title is empty and shows an inline validation message', async () => {
    (svc as any).updateOutcome = sinon.stub().resolves({});
    const { wrapper, store } = mountPanel(svc);
    store.selectNode('outcome', 10);
    await flush(wrapper);
    await wrapper.find('[data-cy="detailTitle"]').setValue('');
    await wrapper.find('[data-cy="detailSave"]').trigger('click');
    await flush(wrapper);
    expect((svc as any).updateOutcome.called).toBe(false);
    expect(wrapper.find('[data-cy="detailValidationError"]').text()).toMatch(/title/i);
  });

  it('blocks saving when opportunity valuerating is out of 1..5 range', async () => {
    (svc as any).updateOpportunity = sinon.stub().resolves({});
    const { wrapper, store } = mountPanel(svc);
    store.selectNode('opportunity', 100);
    await flush(wrapper);
    await wrapper.find('[data-cy="detailValuerating"]').setValue('9');
    await wrapper.find('[data-cy="detailSave"]').trigger('click');
    await flush(wrapper);
    expect((svc as any).updateOpportunity.called).toBe(false);
    expect(wrapper.find('[data-cy="detailValidationError"]').text()).toMatch(/1.*5/);
  });

  it('shows a server 400 error in the panel and keeps the previous node values', async () => {
    (svc as any).updateOutcome = sinon.stub().rejects({
      response: { status: 400, data: { title: 'title must be at most 200 chars' } },
    });
    const { wrapper, store } = mountPanel(svc);
    store.selectNode('outcome', 10);
    await flush(wrapper);
    await wrapper.find('[data-cy="detailTitle"]').setValue('Something else');
    await wrapper.find('[data-cy="detailSave"]').trigger('click');
    await flush(wrapper);
    expect(wrapper.find('[data-cy="detailServerError"]').text()).toMatch(/title must be at most/);
    // Node keeps previous title.
    expect((store.findNode('outcome', 10) as any).title).toBe('Outcome A');
  });

  it('shows a server 403 error in the panel and keeps the previous node values', async () => {
    (svc as any).updateOutcome = sinon.stub().rejects({ response: { status: 403 } });
    const { wrapper, store } = mountPanel(svc);
    store.selectNode('outcome', 10);
    await flush(wrapper);
    await wrapper.find('[data-cy="detailTitle"]').setValue('Rename');
    await wrapper.find('[data-cy="detailSave"]').trigger('click');
    await flush(wrapper);
    expect(wrapper.find('[data-cy="detailServerError"]').text()).toMatch(/permission/i);
    expect((store.findNode('outcome', 10) as any).title).toBe('Outcome A');
  });

  it('prompts before switching selection when there are unsaved edits', async () => {
    const { wrapper, store } = mountPanel(svc);
    store.selectNode('outcome', 10);
    await flush(wrapper);
    await wrapper.find('[data-cy="detailTitle"]').setValue('Dirty edit');
    // Attempt to switch selection: intercepted by prompt.
    store.selectNode('opportunity', 100);
    await flush(wrapper);
    expect(wrapper.find('[data-cy="detailUnsavedPrompt"]').exists()).toBe(true);
    // Discard advances to the new selection.
    await wrapper.find('[data-cy="detailUnsavedDiscard"]').trigger('click');
    await flush(wrapper);
    expect(wrapper.find('[data-cy="detailUnsavedPrompt"]').exists()).toBe(false);
    expect(store.selectedNodeType).toBe('opportunity');
    expect(store.selectedNodeId).toBe(100);
  });

  it('keeps unsaved edits when "Keep editing" is chosen on the unsaved prompt', async () => {
    const { wrapper, store } = mountPanel(svc);
    store.selectNode('outcome', 10);
    await flush(wrapper);
    await wrapper.find('[data-cy="detailTitle"]').setValue('Dirty edit');
    // Attempt to switch selection: intercepted by the prompt.
    store.selectNode('opportunity', 100);
    await flush(wrapper);
    expect(wrapper.find('[data-cy="detailUnsavedPrompt"]').exists()).toBe(true);
    // Choose "Keep editing" — should revert store selection and preserve the edit.
    await wrapper.find('[data-cy="detailUnsavedKeep"]').trigger('click');
    await flush(wrapper);
    expect(wrapper.find('[data-cy="detailUnsavedPrompt"]').exists()).toBe(false);
    expect(store.selectedNodeType).toBe('outcome');
    expect(store.selectedNodeId).toBe(10);
    expect((wrapper.find('[data-cy="detailTitle"]').element as HTMLInputElement).value).toBe('Dirty edit');
  });

  it('renders read-only fields with no save, status or delete controls when canEdit is false', async () => {
    const { wrapper, store } = mountPanel(svc);
    const t = populatedTree();
    t.canEdit = false;
    store.setTree(t);
    store.selectNode('outcome', 10);
    await flush(wrapper);
    expect(wrapper.find('[data-cy="treeDetailPanel"]').exists()).toBe(true);
    expect(wrapper.find('[data-cy="detailSave"]').exists()).toBe(false);
    expect(wrapper.find('[data-cy="detailDelete"]').exists()).toBe(false);
    expect(wrapper.find('[data-cy="detailStatus"]').exists()).toBe(false);
    // Title displayed as read-only text.
    expect(wrapper.find('[data-cy="detailTitleReadonly"]').text()).toContain('Outcome A');
  });

  it('exposes a delete action that opens the shared delete confirmation flow', async () => {
    const { wrapper, store } = mountPanel(svc);
    store.selectNode('outcome', 10);
    await flush(wrapper);
    const btn = wrapper.find('[data-cy="detailDelete"]');
    expect(btn.exists()).toBe(true);
    const events: any[] = [];
    wrapper.vm.$.appContext.config.globalProperties.$emitDelete = (payload: any) => events.push(payload);
    await btn.trigger('click');
    // Component emits a 'delete' event that the editor wires to its existing delete modal.
    expect(wrapper.emitted().delete).toBeTruthy();
    expect(wrapper.emitted().delete![0][0]).toEqual({ type: 'outcome', id: 10 });
  });
});
