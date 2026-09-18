import { beforeEach, describe, expect, it, vi } from 'vitest';

import { createTestingPinia } from '@pinia/testing';
import { type MountingOptions, mount as fullMount, shallowMount } from '@vue/test-utils';
import sinon, { type SinonStubbedInstance } from 'sinon';

let currentRouteParams: Record<string, any> = { teamId: '10' };
vi.mock('vue-router', () => ({
  useRoute: () => ({ params: currentRouteParams }),
  useRouter: () => ({ push: vi.fn() }),
}));

import { OpportunityStatus } from '@/shared/model/enumerations/opportunity-status.model';
import { OutcomeStatus } from '@/shared/model/enumerations/outcome-status.model';
import { SolutionStatus } from '@/shared/model/enumerations/solution-status.model';
import { TeamRole } from '@/shared/model/enumerations/team-role.model';

import type { ITeamTree } from './tree.model';
import TreeEditor from './tree-editor.vue';
import TreeService from './tree.service';
import { useTreeStore } from './tree.store';

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
      archived: false,
      outcomes: [
        {
          id: 10,
          title: 'Outcome A',
          status: OutcomeStatus.ACTIVE,
          sortOrder: 0,
          opportunities: [
            {
              id: 100,
              title: 'Opp',
              status: OpportunityStatus.IDENTIFIED,
              valuerating: 3,
              complexity: 3,
              sortOrder: 0,
              parentId: null,
              children: [],
              solutions: [{ id: 1000, title: 'Sol', status: SolutionStatus.IDEA, sortOrder: 0 } as any],
            },
          ],
        },
      ],
    },
    {
      id: 2,
      name: 'Product Two',
      archived: true,
      outcomes: [
        {
          id: 20,
          title: 'Outcome B',
          status: OutcomeStatus.ACTIVE,
          sortOrder: 0,
          opportunities: [],
        },
      ],
    },
  ],
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

const mountFull = (treeServiceStub: SinonStubbedInstance<TreeService>, routeParams: Record<string, any> = { teamId: '10' }) => {
  currentRouteParams = routeParams;
  return fullMount(TreeEditor, {
    global: {
      plugins: [createTestingPinia({ stubActions: false })],
      stubs: {
        'font-awesome-icon': true,
        'router-link': true,
      },
      provide: {
        treeService: () => treeServiceStub,
      },
    },
  });
};

const flush = async (wrapper: { vm: { $nextTick: () => Promise<void> } }) => {
  await wrapper.vm.$nextTick();
  await wrapper.vm.$nextTick();
  await wrapper.vm.$nextTick();
};

describe('TreeEditor Component', () => {
  let treeServiceStub: SinonStubbedInstance<TreeService>;

  beforeEach(() => {
    treeServiceStub = sinon.createStubInstance<TreeService>(TreeService);
  });

  it('renders the shell after a successful load', async () => {
    treeServiceStub.getTeamTree.resolves(sampleTree());
    const wrapper = mount(treeServiceStub);
    await flush(wrapper);
    expect(treeServiceStub.getTeamTree.calledOnce).toBe(true);
    expect(wrapper.find('[data-cy="treeEditorShell"]').exists()).toBe(true);
    expect(wrapper.find('[data-cy="treeEditorTeamName"]').text()).toContain('Alpha');
  });

  it('shows an access-denied message on 403', async () => {
    treeServiceStub.getTeamTree.rejects({ response: { status: 403 } });
    const wrapper = mount(treeServiceStub);
    await flush(wrapper);
    expect(wrapper.find('[data-cy="treeEditorAccessDenied"]').exists()).toBe(true);
    expect(wrapper.find('[data-cy="treeEditorShell"]').exists()).toBe(false);
  });

  it('shows a not-found message on 404', async () => {
    treeServiceStub.getTeamTree.rejects({ response: { status: 404 } });
    const wrapper = mount(treeServiceStub);
    await flush(wrapper);
    expect(wrapper.find('[data-cy="treeEditorNotFound"]').exists()).toBe(true);
  });

  it('marks the shell as read-only when canEdit is false', async () => {
    const readOnly = sampleTree();
    readOnly.canEdit = false;
    treeServiceStub.getTeamTree.resolves(readOnly);
    const wrapper = mount(treeServiceStub);
    await flush(wrapper);
    expect(wrapper.find('[data-cy="treeEditorReadOnly"]').exists()).toBe(true);
  });

  it('shows the empty state with an "Add your first product" call to action', async () => {
    treeServiceStub.getTeamTree.resolves(sampleTree());
    const wrapper = mount(treeServiceStub);
    await flush(wrapper);
    expect(wrapper.find('[data-cy="treeEditorEmpty"]').exists()).toBe(true);
    expect(wrapper.find('[data-cy="treeEditorAddFirstProduct"]').text()).toContain('Add your first product');
  });

  it('renders one card per node in the loaded tree', async () => {
    treeServiceStub.getTeamTree.resolves(populatedTree());
    const wrapper = mountFull(treeServiceStub);
    await flush(wrapper);
    expect(wrapper.find('[data-cy="treeNode-product-1"]').exists()).toBe(true);
    expect(wrapper.find('[data-cy="treeNode-product-2"]').exists()).toBe(true);
    expect(wrapper.find('[data-cy="treeNode-outcome-10"]').exists()).toBe(true);
    expect(wrapper.find('[data-cy="treeNode-opportunity-100"]').exists()).toBe(true);
    expect(wrapper.find('[data-cy="treeNode-solution-1000"]').exists()).toBe(true);
  });

  it('shows a status badge on each non-product node and an ARCHIVED badge on archived products', async () => {
    treeServiceStub.getTeamTree.resolves(populatedTree());
    const wrapper = mountFull(treeServiceStub);
    await flush(wrapper);
    const outcomeCard = wrapper.find('[data-cy="treeNode-outcome-10"]');
    expect(outcomeCard.find('[data-cy="treeNodeStatus"]').text()).toContain('ACTIVE');
    const archivedProduct = wrapper.find('[data-cy="treeNode-product-2"]');
    expect(archivedProduct.find('[data-cy="treeNodeStatus"]').text()).toContain('ARCHIVED');
    const activeProduct = wrapper.find('[data-cy="treeNode-product-1"]');
    expect(activeProduct.find('[data-cy="treeNodeStatus"]').exists()).toBe(false);
  });

  it('records the clicked node as selected in the store', async () => {
    treeServiceStub.getTeamTree.resolves(populatedTree());
    const wrapper = mountFull(treeServiceStub);
    await flush(wrapper);
    const store = useTreeStore();
    await wrapper.find('[data-cy="treeNode-outcome-10"]').trigger('click');
    expect(store.selectedNodeType).toBe('outcome');
    expect(store.selectedNodeId).toBe(10);
  });

  it('clears selection when clicking the empty canvas', async () => {
    treeServiceStub.getTeamTree.resolves(populatedTree());
    const wrapper = mountFull(treeServiceStub);
    await flush(wrapper);
    const store = useTreeStore();
    store.selectNode('outcome', 10);
    await wrapper.find('[data-cy="treeEditorCanvas"]').trigger('click');
    expect(store.selectedNodeType).toBeNull();
    expect(store.selectedNodeId).toBeNull();
  });

  it('hides other product branches when a product is focused via the toolbar', async () => {
    treeServiceStub.getTeamTree.resolves(populatedTree());
    const wrapper = mountFull(treeServiceStub);
    await flush(wrapper);
    const select = wrapper.find('[data-cy="treeEditorFocusSelect"]');
    await select.setValue('1');
    await wrapper.vm.$nextTick();
    expect(wrapper.find('[data-cy="treeNode-product-1"]').exists()).toBe(true);
    expect(wrapper.find('[data-cy="treeNode-product-2"]').exists()).toBe(false);
    expect(wrapper.find('[data-cy="treeNode-outcome-20"]').exists()).toBe(false);
  });

  it('restores the whole-team view when the focus is cleared', async () => {
    treeServiceStub.getTeamTree.resolves(populatedTree());
    const wrapper = mountFull(treeServiceStub);
    await flush(wrapper);
    const store = useTreeStore();
    store.focusProduct(1);
    await wrapper.vm.$nextTick();
    expect(wrapper.find('[data-cy="treeEditorClearFocus"]').exists()).toBe(true);
    await wrapper.find('[data-cy="treeEditorClearFocus"]').trigger('click');
    await wrapper.vm.$nextTick();
    expect(store.focusedProductId).toBeNull();
    expect(wrapper.find('[data-cy="treeNode-product-1"]').exists()).toBe(true);
    expect(wrapper.find('[data-cy="treeNode-product-2"]').exists()).toBe(true);
  });

  it('supports panning by mouse-drag and resetting the view', async () => {
    treeServiceStub.getTeamTree.resolves(populatedTree());
    const wrapper = mountFull(treeServiceStub);
    await flush(wrapper);
    const canvas = wrapper.find('[data-cy="treeEditorCanvas"]');
    await canvas.trigger('mousedown', { clientX: 100, clientY: 100 });
    await canvas.trigger('mousemove', { clientX: 150, clientY: 130 });
    await canvas.trigger('mouseup');
    const viewport = wrapper.find('.tree-canvas__viewport').element as HTMLElement;
    expect(viewport.style.transform).toContain('translate(50px, 30px)');
    await wrapper.find('[data-cy="treeEditorResetView"]').trigger('click');
    expect((wrapper.find('.tree-canvas__viewport').element as HTMLElement).style.transform).toContain('translate(0px, 0px)');
  });

  it('lays out and renders a 500-node tree in well under 2 seconds', async () => {
    const big: ITeamTree = {
      id: 10,
      name: 'Perf',
      description: null,
      createdDate: null,
      currentUserRole: TeamRole.EDITOR,
      canEdit: true,
      products: [],
    };
    let nextId = 1000;
    let created = 1;
    const outcomes: any[] = [];
    for (let o = 0; o < 5; o++) {
      const opps: any[] = [];
      for (let i = 0; i < 20; i++) {
        const solution = { id: nextId++, title: 's', status: SolutionStatus.IDEA, sortOrder: 0 };
        const child = {
          id: nextId++,
          title: 'child',
          status: OpportunityStatus.IDENTIFIED,
          valuerating: 3,
          complexity: 3,
          sortOrder: 0,
          parentId: null,
          children: [],
          solutions: [solution],
        };
        const parent = {
          id: nextId++,
          title: 'parent',
          status: OpportunityStatus.IDENTIFIED,
          valuerating: 3,
          complexity: 3,
          sortOrder: 0,
          parentId: null,
          children: [child],
          solutions: [],
        };
        opps.push(parent);
        created += 3;
      }
      outcomes.push({ id: nextId++, title: `O${o}`, status: OutcomeStatus.ACTIVE, sortOrder: 0, opportunities: opps });
      created += 1;
    }
    while (created < 500) {
      outcomes[0].opportunities[0].solutions.push({ id: nextId++, title: 's', status: SolutionStatus.IDEA, sortOrder: 0 });
      created += 1;
    }
    big.products = [{ id: 1, name: 'P', archived: false, outcomes }];
    treeServiceStub.getTeamTree.resolves(big);
    const started = performance.now();
    const wrapper = mountFull(treeServiceStub);
    await flush(wrapper);
    const elapsed = performance.now() - started;
    // Sanity: 500+ node cards actually rendered.
    const cards = wrapper.findAll('[data-cy^="treeNode-"]');
    expect(cards.length).toBeGreaterThanOrEqual(500);
    expect(elapsed).toBeLessThan(2000);
    // Log the measured render time so a run of this spec doubles as the
    // "manual check" performance recording for TREE-005.
    // eslint-disable-next-line no-console
    console.log(`[TREE-005 render-perf] nodes=${cards.length} elapsed_ms=${elapsed.toFixed(1)}`);
  });

  it('opens the add-product modal from the toolbar and creates a product on submit', async () => {
    treeServiceStub.getTeamTree.resolves(populatedTree());
    (treeServiceStub as any).createProduct = sinon.stub().resolves({ id: 999, name: 'Brand new', outcomes: [] });
    const wrapper = mountFull(treeServiceStub);
    await flush(wrapper);
    expect(wrapper.find('[data-cy="treeEditorAddProduct"]').exists()).toBe(true);
    await wrapper.find('[data-cy="treeEditorAddProduct"]').trigger('click');
    expect(wrapper.find('[data-cy="treeEditorAddProductModal"]').exists()).toBe(true);
    await wrapper.find('[data-cy="addProductName"]').setValue('Brand new');
    await wrapper.find('[data-cy="addProductVision"]').setValue('a vision');
    await wrapper.find('[data-cy="treeEditorAddProductModal"] form').trigger('submit.prevent');
    await flush(wrapper);
    expect((treeServiceStub as any).createProduct.calledOnce).toBe(true);
    const store = useTreeStore();
    expect(store.products.map(p => p.id)).toContain(999);
    // New product renders as a top-level branch without a reload.
    expect(wrapper.find('[data-cy="treeNode-product-999"]').exists()).toBe(true);
    expect(treeServiceStub.getTeamTree.callCount).toBe(1);
  });

  it('opens the add-product modal from the empty state "Add your first product" call to action', async () => {
    treeServiceStub.getTeamTree.resolves(sampleTree());
    const wrapper = mountFull(treeServiceStub);
    await flush(wrapper);
    await wrapper.find('[data-cy="treeEditorAddFirstProduct"]').trigger('click');
    expect(wrapper.find('[data-cy="treeEditorAddProductModal"]').exists()).toBe(true);
  });

  it('hides the add-product, add-child and delete affordances when canEdit is false', async () => {
    const readOnly = populatedTree();
    readOnly.canEdit = false;
    treeServiceStub.getTeamTree.resolves(readOnly);
    const wrapper = mountFull(treeServiceStub);
    await flush(wrapper);
    expect(wrapper.find('[data-cy="treeEditorAddProduct"]').exists()).toBe(false);
    expect(wrapper.find('[data-cy="treeNodeAddChild-product-1-outcome"]').exists()).toBe(false);
    expect(wrapper.find('[data-cy="treeNodeDelete-product-1"]').exists()).toBe(false);
    // Empty-state call-to-action stays disabled when read-only.
    const empty = sampleTree();
    empty.canEdit = false;
    treeServiceStub.getTeamTree.resetHistory();
    treeServiceStub.getTeamTree.resolves(empty);
    const wrapper2 = mountFull(treeServiceStub);
    await flush(wrapper2);
    const btn = wrapper2.find('[data-cy="treeEditorAddFirstProduct"]');
    expect(btn.exists()).toBe(true);
    expect((btn.element as HTMLButtonElement).disabled).toBe(true);
  });

  it('only offers valid child types on the "add child" affordance for each node type', async () => {
    treeServiceStub.getTeamTree.resolves(populatedTree());
    const wrapper = mountFull(treeServiceStub);
    await flush(wrapper);
    // Product -> Outcome only.
    expect(wrapper.find('[data-cy="treeNodeAddChild-product-1-outcome"]').exists()).toBe(true);
    expect(wrapper.find('[data-cy="treeNodeAddChild-product-1-opportunity"]').exists()).toBe(false);
    expect(wrapper.find('[data-cy="treeNodeAddChild-product-1-solution"]').exists()).toBe(false);
    // Outcome -> Opportunity only.
    expect(wrapper.find('[data-cy="treeNodeAddChild-outcome-10-opportunity"]').exists()).toBe(true);
    expect(wrapper.find('[data-cy="treeNodeAddChild-outcome-10-outcome"]').exists()).toBe(false);
    expect(wrapper.find('[data-cy="treeNodeAddChild-outcome-10-solution"]').exists()).toBe(false);
    // Opportunity -> Opportunity + Solution.
    expect(wrapper.find('[data-cy="treeNodeAddChild-opportunity-100-opportunity"]').exists()).toBe(true);
    expect(wrapper.find('[data-cy="treeNodeAddChild-opportunity-100-solution"]').exists()).toBe(true);
    // Solution -> nothing.
    expect(wrapper.find('[data-cy="treeNodeAddChild-solution-1000-opportunity"]').exists()).toBe(false);
    expect(wrapper.find('[data-cy="treeNodeAddChild-solution-1000-solution"]').exists()).toBe(false);
  });

  it('adds a child node under the selected parent via the API and appends it last on the canvas', async () => {
    treeServiceStub.getTeamTree.resolves(populatedTree());
    (treeServiceStub as any).createOutcome = sinon.stub().resolves({ id: 55, title: 'Added', opportunities: [] });
    const wrapper = mountFull(treeServiceStub);
    await flush(wrapper);
    await wrapper.find('[data-cy="treeNodeAddChild-product-1-outcome"]').trigger('click');
    expect(wrapper.find('[data-cy="treeEditorAddChildModal"]').exists()).toBe(true);
    await wrapper.find('[data-cy="addChildTitle"]').setValue('Added');
    await wrapper.find('[data-cy="treeEditorAddChildModal"] form').trigger('submit.prevent');
    await flush(wrapper);
    expect((treeServiceStub as any).createOutcome.calledOnce).toBe(true);
    const store = useTreeStore();
    expect(store.childrenOf('product', 1).map(n => (n as any).id)).toEqual([10, 55]);
    expect(wrapper.find('[data-cy="treeNode-outcome-55"]').exists()).toBe(true);
  });

  it('confirms deletion in a dialog that states how many descendants will be removed', async () => {
    treeServiceStub.getTeamTree.resolves(populatedTree());
    (treeServiceStub as any).deleteNode = sinon.stub().resolves(undefined);
    const wrapper = mountFull(treeServiceStub);
    await flush(wrapper);
    // Product 1 owns outcome 10 + opportunity 100 + solution 1000 = 3 descendants.
    await wrapper.find('[data-cy="treeNodeDelete-product-1"]').trigger('click');
    const modal = wrapper.find('[data-cy="treeEditorDeleteModal"]');
    expect(modal.exists()).toBe(true);
    expect(modal.find('[data-cy="deleteConfirmCount"]').text()).toBe('3');
    await modal.find('[data-cy="deleteConfirm"]').trigger('click');
    await flush(wrapper);
    expect((treeServiceStub as any).deleteNode.calledOnceWith('product', 1)).toBe(true);
    const store = useTreeStore();
    expect(store.findNode('product', 1)).toBeNull();
    expect(wrapper.find('[data-cy="treeNode-product-1"]').exists()).toBe(false);
  });

  it('cancelling the delete confirmation leaves the tree unchanged', async () => {
    treeServiceStub.getTeamTree.resolves(populatedTree());
    (treeServiceStub as any).deleteNode = sinon.stub().resolves(undefined);
    const wrapper = mountFull(treeServiceStub);
    await flush(wrapper);
    await wrapper.find('[data-cy="treeNodeDelete-product-1"]').trigger('click');
    await wrapper.find('[data-cy="deleteCancel"]').trigger('click');
    await flush(wrapper);
    expect((treeServiceStub as any).deleteNode.called).toBe(false);
    const store = useTreeStore();
    expect(store.findNode('product', 1)).not.toBeNull();
  });

  it('shows a server validation error and leaves the store unchanged', async () => {
    treeServiceStub.getTeamTree.resolves(populatedTree());
    (treeServiceStub as any).createOutcome = sinon
      .stub()
      .rejects({ response: { status: 400, data: { title: 'title must be at least 2 chars' } } });
    const wrapper = mountFull(treeServiceStub);
    await flush(wrapper);
    await wrapper.find('[data-cy="treeNodeAddChild-product-1-outcome"]').trigger('click');
    await wrapper.find('[data-cy="addChildTitle"]').setValue('Bad');
    await wrapper.find('[data-cy="treeEditorAddChildModal"] form').trigger('submit.prevent');
    await flush(wrapper);
    expect(wrapper.find('[data-cy="treeEditorWriteError"]').text()).toMatch(/title must be at least/i);
    const store = useTreeStore();
    expect(store.childrenOf('product', 1).map(n => (n as any).id)).toEqual([10]);
  });

  it('shows a server authorisation (403) error on delete and leaves the store unchanged', async () => {
    treeServiceStub.getTeamTree.resolves(populatedTree());
    (treeServiceStub as any).deleteNode = sinon.stub().rejects({ response: { status: 403 } });
    const wrapper = mountFull(treeServiceStub);
    await flush(wrapper);
    await wrapper.find('[data-cy="treeNodeDelete-outcome-10"]').trigger('click');
    await wrapper.find('[data-cy="deleteConfirm"]').trigger('click');
    await flush(wrapper);
    expect(wrapper.find('[data-cy="treeEditorWriteError"]').text()).toMatch(/permission/i);
    const store = useTreeStore();
    expect(store.findNode('outcome', 10)).not.toBeNull();
  });

  it('clears the selection and focus when the deleted node was selected / focused', async () => {
    treeServiceStub.getTeamTree.resolves(populatedTree());
    (treeServiceStub as any).deleteNode = sinon.stub().resolves(undefined);
    const wrapper = mountFull(treeServiceStub);
    await flush(wrapper);
    const store = useTreeStore();
    store.selectNode('product', 1);
    store.focusProduct(1);
    await wrapper.find('[data-cy="treeNodeDelete-product-1"]').trigger('click');
    await wrapper.find('[data-cy="deleteConfirm"]').trigger('click');
    await flush(wrapper);
    expect(store.selectedNodeId).toBeNull();
    expect(store.focusedProductId).toBeNull();
  });

  it('zoom-in / zoom-out toolbar buttons change the zoom level', async () => {
    treeServiceStub.getTeamTree.resolves(populatedTree());
    const wrapper = mountFull(treeServiceStub);
    await flush(wrapper);
    const initial = wrapper.find('[data-cy="treeEditorZoomLevel"]').text();
    expect(initial).toBe('100%');
    await wrapper.find('[data-cy="treeEditorZoomIn"]').trigger('click');
    expect(wrapper.find('[data-cy="treeEditorZoomLevel"]').text()).not.toBe(initial);
    await wrapper.find('[data-cy="treeEditorResetView"]').trigger('click');
    expect(wrapper.find('[data-cy="treeEditorZoomLevel"]').text()).toBe('100%');
  });
});
