import { beforeEach, describe, expect, it, vi } from 'vitest';

import { createTestingPinia } from '@pinia/testing';
import { mount as fullMount } from '@vue/test-utils';
import sinon, { type SinonStubbedInstance } from 'sinon';

let currentRouteParams: Record<string, any> = { teamId: '10' };
vi.mock('vue-router', () => ({
  useRoute: () => ({ params: currentRouteParams }),
  useRouter: () => ({ push: vi.fn() }),
}));

import { OpportunityStatus } from '@/shared/model/enumerations/opportunity-status.model';
import { OutcomeStatus } from '@/shared/model/enumerations/outcome-status.model';
import { TeamRole } from '@/shared/model/enumerations/team-role.model';

import type { ITeamTree } from './tree.model';
import TreeEditor from './tree-editor.vue';
import TreeService from './tree.service';

const buildTree = (): ITeamTree => ({
  id: 10,
  name: 'Alpha',
  currentUserRole: TeamRole.EDITOR,
  canEdit: true,
  products: [
    {
      id: 1,
      name: 'P1',
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
              title: 'Opp 100',
              status: OpportunityStatus.IDENTIFIED,
              valuerating: 3,
              complexity: 3,
              sortOrder: 0,
              parentId: null,
              children: [],
              solutions: [],
            },
            {
              id: 200,
              title: 'Opp 200',
              status: OpportunityStatus.IDENTIFIED,
              valuerating: 3,
              complexity: 3,
              sortOrder: 1,
              parentId: null,
              children: [],
              solutions: [],
            },
          ],
        },
      ],
    },
  ],
});

const flush = async (wrapper: { vm: { $nextTick: () => Promise<void> } }) => {
  for (let i = 0; i < 4; i++) await wrapper.vm.$nextTick();
};

const mountEditor = (svcStub: SinonStubbedInstance<TreeService>, pinia?: ReturnType<typeof createTestingPinia>) =>
  fullMount(TreeEditor, {
    global: {
      plugins: [pinia ?? createTestingPinia({ stubActions: false })],
      stubs: { 'font-awesome-icon': true, 'router-link': true },
      provide: { treeService: () => svcStub },
    },
  });

const installFakeLocalStorage = () => {
  const store = new Map<string, string>();
  const fake = {
    getItem: (k: string) => (store.has(k) ? (store.get(k) as string) : null),
    setItem: (k: string, v: string) => {
      store.set(k, String(v));
    },
    removeItem: (k: string) => {
      store.delete(k);
    },
    clear: () => store.clear(),
    key: (i: number) => Array.from(store.keys())[i] ?? null,
    get length() {
      return store.size;
    },
  };
  Object.defineProperty(window, 'localStorage', { configurable: true, value: fake });
  return fake;
};

describe('TreeEditor — collapse / expand', () => {
  let svc: SinonStubbedInstance<TreeService>;
  beforeEach(() => {
    svc = sinon.createStubInstance<TreeService>(TreeService);
    currentRouteParams = { teamId: '10' };
    installFakeLocalStorage();
  });

  it('renders a chevron with aria-expanded on nodes that have children', async () => {
    svc.getTeamTree.resolves(buildTree());
    const wrapper = mountEditor(svc);
    await flush(wrapper);
    const outcomeChevron = wrapper.find('[data-cy="treeNodeCollapse-outcome-10"]');
    expect(outcomeChevron.exists()).toBe(true);
    expect(outcomeChevron.attributes('aria-expanded')).toBe('true');
    // Opportunity 100 has no children — no chevron.
    expect(wrapper.find('[data-cy="treeNodeCollapse-opportunity-100"]').exists()).toBe(false);
  });

  it('collapses a branch on chevron click and hides the descendant cards; shows hidden count', async () => {
    svc.getTeamTree.resolves(buildTree());
    const wrapper = mountEditor(svc);
    await flush(wrapper);
    // Descendants are visible before.
    expect(wrapper.find('[data-cy="treeNode-opportunity-100"]').exists()).toBe(true);
    expect(wrapper.find('[data-cy="treeNode-opportunity-200"]').exists()).toBe(true);
    await wrapper.find('[data-cy="treeNodeCollapse-outcome-10"]').trigger('click');
    await flush(wrapper);
    expect(wrapper.find('[data-cy="treeNode-opportunity-100"]').exists()).toBe(false);
    expect(wrapper.find('[data-cy="treeNode-opportunity-200"]').exists()).toBe(false);
    const chev = wrapper.find('[data-cy="treeNodeCollapse-outcome-10"]');
    expect(chev.attributes('aria-expanded')).toBe('false');
    const count = wrapper.find('[data-cy="treeNodeHiddenCount-outcome-10"]');
    expect(count.exists()).toBe(true);
    expect(count.text()).toBe('2');
  });

  it("preserves each team's collapse state across a team switch and back", async () => {
    // Team 10 tree per buildTree(); team 20 has different node ids.
    const team20: ITeamTree = {
      id: 20,
      name: 'Beta',
      currentUserRole: TeamRole.EDITOR,
      canEdit: true,
      products: [
        {
          id: 2,
          name: 'P2',
          archived: false,
          outcomes: [
            {
              id: 30,
              title: 'Outcome B',
              status: OutcomeStatus.ACTIVE,
              sortOrder: 0,
              opportunities: [
                {
                  id: 300,
                  title: 'Opp 300',
                  status: OpportunityStatus.IDENTIFIED,
                  valuerating: 3,
                  complexity: 3,
                  sortOrder: 0,
                  parentId: null,
                  children: [],
                  solutions: [],
                },
              ],
            },
          ],
        },
      ],
    };
    svc.getTeamTree.callsFake(async (id: number) => (id === 10 ? buildTree() : team20));

    // Share ONE pinia across mounts to reproduce the real singleton store — the
    // bug was that leftover `loaded=true` + previous team's tree in the store
    // caused the new team's collapse watcher to prune stored ids against the
    // wrong tree.
    const pinia = createTestingPinia({ stubActions: false });

    currentRouteParams = { teamId: '10' };
    const w1 = mountEditor(svc, pinia);
    await flush(w1);
    await w1.find('[data-cy="treeNodeCollapse-outcome-10"]').trigger('click');
    await flush(w1);
    expect(w1.find('[data-cy="treeNode-opportunity-100"]').exists()).toBe(false);
    w1.unmount();

    // Mount for team 20 next — the pinia store is shared between mounts.
    currentRouteParams = { teamId: '20' };
    const w2 = mountEditor(svc, pinia);
    await flush(w2);
    // Team 20's collapse state is independent; nothing collapsed here yet.
    const chev20 = w2.find('[data-cy="treeNodeCollapse-outcome-30"]');
    expect(chev20.exists()).toBe(true);
    expect(chev20.attributes('aria-expanded')).toBe('true');
    w2.unmount();

    // Back to team 10 — outcome 10 must still be collapsed from the localStorage entry.
    currentRouteParams = { teamId: '10' };
    const w3 = mountEditor(svc, pinia);
    await flush(w3);
    const chev10 = w3.find('[data-cy="treeNodeCollapse-outcome-10"]');
    expect(chev10.exists()).toBe(true);
    expect(chev10.attributes('aria-expanded')).toBe('false');
    expect(w3.find('[data-cy="treeNode-opportunity-100"]').exists()).toBe(false);
  });

  it('moving a node onto a collapsed parent expands that parent so it stays visible', async () => {
    svc.getTeamTree.resolves(buildTree());
    // moveNode returns a MoveNodeResponse; use a minimal one — the store's _applyMoveResponse is defensive against empty arrays.
    (svc.moveNode as unknown as sinon.SinonStub).resolves({
      nodeType: 'opportunity',
      nodeId: 200,
      oldSiblings: [],
      newSiblings: [],
      outcomeUpdates: [],
    });
    const wrapper = mountEditor(svc);
    await flush(wrapper);

    // Collapse Outcome A (has children 100 and 200).
    await wrapper.find('[data-cy="treeNodeCollapse-outcome-10"]').trigger('click');
    await flush(wrapper);
    expect(wrapper.find('[data-cy="treeNode-opportunity-100"]').exists()).toBe(false);

    // Move opp 200 onto outcome 10 via the editor's onDrop-equivalent path — invoke exposed move to onto collapsed parent.
    // The Move-to dialog isn't easy to wire up here; instead trigger the store move directly via the exposed component API.
    const vm = wrapper.vm as any;
    // Simulate opening move dialog + confirming via the exposed methods.
    vm.openMoveModal('opportunity', 200);
    await flush(wrapper);
    vm.selectMoveTarget({ parentType: 'outcome', parentId: 10, label: 'Outcome A', productId: 1, productName: 'P1', depth: 1 });
    await vm.confirmMove();
    await flush(wrapper);

    // After move, the previously collapsed outcome should have expanded and its descendants visible again.
    const chev = wrapper.find('[data-cy="treeNodeCollapse-outcome-10"]');
    expect(chev.attributes('aria-expanded')).toBe('true');
    expect(wrapper.find('[data-cy="treeNode-opportunity-100"]').exists()).toBe(true);
  });
});
