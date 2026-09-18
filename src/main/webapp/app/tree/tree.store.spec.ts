import { beforeEach, describe, expect, it, vi } from 'vitest';

import { createPinia, setActivePinia } from 'pinia';

import { OpportunityStatus } from '@/shared/model/enumerations/opportunity-status.model';
import { TeamRole } from '@/shared/model/enumerations/team-role.model';

import type { IOpportunityTreeNode, ISolutionTreeNode, ITeamTree } from './tree.model';
import { useTreeStore } from './tree.store';

const sampleTree = (): ITeamTree => ({
  id: 10,
  name: 'Team Alpha',
  description: null,
  createdDate: null,
  currentUserRole: TeamRole.EDITOR,
  canEdit: true,
  products: [
    {
      id: 100,
      name: 'Product 1',
      description: null,
      vision: null,
      archived: false,
      createdDate: null,
      outcomes: [
        {
          id: 200,
          title: 'Outcome A',
          description: null,
          metric: null,
          targetValue: null,
          currentValue: null,
          status: null,
          startDate: null,
          targetDate: null,
          sortOrder: 0,
          opportunities: [
            {
              id: 300,
              title: 'Opp A',
              description: null,
              status: OpportunityStatus.IDENTIFIED,
              valuerating: 3,
              complexity: 3,
              sortOrder: 0,
              parentId: null,
              children: [
                {
                  id: 301,
                  title: 'Opp A.1',
                  description: null,
                  status: OpportunityStatus.IDENTIFIED,
                  valuerating: 3,
                  complexity: 3,
                  sortOrder: 0,
                  parentId: 300,
                  children: [],
                  solutions: [
                    {
                      id: 401,
                      title: 'Sol nested',
                      description: null,
                      status: null,
                      effort: 3,
                      sortOrder: 0,
                    },
                  ],
                },
              ],
              solutions: [
                {
                  id: 400,
                  title: 'Sol 1',
                  description: null,
                  status: null,
                  effort: 3,
                  sortOrder: 0,
                },
              ],
            },
          ],
        },
      ],
    },
  ],
});

describe('tree.store', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
  });

  it('starts empty', () => {
    const store = useTreeStore();
    expect(store.tree).toBeNull();
    expect(store.loaded).toBe(false);
    expect(store.canEdit).toBe(false);
  });

  it('load calls the tree service exactly once and stores the tree', async () => {
    const store = useTreeStore();
    const svc = { getTeamTree: vi.fn().mockResolvedValue(sampleTree()) };
    await store.load(10, svc as any);
    expect(svc.getTeamTree).toHaveBeenCalledTimes(1);
    expect(svc.getTeamTree).toHaveBeenCalledWith(10);
    expect(store.loaded).toBe(true);
    expect(store.canEdit).toBe(true);
    expect(store.products).toHaveLength(1);
    expect(store.error).toBeNull();
  });

  it('load records forbidden on 403', async () => {
    const store = useTreeStore();
    const svc = { getTeamTree: vi.fn().mockRejectedValue({ response: { status: 403 } }) };
    await store.load(10, svc as any);
    expect(store.error).toBe('forbidden');
    expect(store.tree).toBeNull();
    expect(store.errorMessage).toMatch(/access/i);
  });

  it('load records not-found on 404', async () => {
    const store = useTreeStore();
    const svc = { getTeamTree: vi.fn().mockRejectedValue({ response: { status: 404 } }) };
    await store.load(10, svc as any);
    expect(store.error).toBe('not-found');
    expect(store.tree).toBeNull();
  });

  it('findNode looks up products, outcomes, opportunities and solutions by id', async () => {
    const store = useTreeStore();
    store.setTree(sampleTree());
    expect(store.findNode('product', 100)?.id).toBe(100);
    expect(store.findNode('outcome', 200)?.id).toBe(200);
    expect(store.findNode('opportunity', 301)?.id).toBe(301);
    expect(store.findNode('solution', 401)?.id).toBe(401);
    expect(store.findNode('solution', 9999)).toBeNull();
  });

  it('childrenOf returns direct children of a node', () => {
    const store = useTreeStore();
    store.setTree(sampleTree());
    expect(store.childrenOf('product', 100).map(n => (n as any).id)).toEqual([200]);
    expect(store.childrenOf('outcome', 200).map(n => (n as any).id)).toEqual([300]);
    // opportunity's children include nested opportunities and solutions
    const oppChildren = store
      .childrenOf('opportunity', 300)
      .map(n => (n as any).id)
      .sort();
    expect(oppChildren).toEqual([301, 400]);
    expect(store.childrenOf('solution', 400)).toEqual([]);
  });

  it('descendantCountOf counts all descendants', () => {
    const store = useTreeStore();
    store.setTree(sampleTree());
    // outcome 200: opp 300, opp 301, sol 400, sol 401 = 4
    expect(store.descendantCountOf('outcome', 200)).toBe(4);
    // opp 300: opp 301 + sol 400 + sol 401 = 3
    expect(store.descendantCountOf('opportunity', 300)).toBe(3);
    expect(store.descendantCountOf('solution', 400)).toBe(0);
  });

  it('insertNode appends a new outcome under a product without a refetch', () => {
    const store = useTreeStore();
    store.setTree(sampleTree());
    const newOutcome = {
      id: 999,
      title: 'New outcome',
      opportunities: [],
    } as any;
    const ok = store.insertNode('product', 100, 'outcome', newOutcome);
    expect(ok).toBe(true);
    expect(store.childrenOf('product', 100).map(n => (n as any).id)).toEqual([200, 999]);
  });

  it('insertNode appends a product under the team root', () => {
    const store = useTreeStore();
    store.setTree(sampleTree());
    const ok = store.insertNode('team', null, 'product', { id: 101, name: 'P2', outcomes: [] } as any);
    expect(ok).toBe(true);
    expect(store.products.map(p => p.id)).toEqual([100, 101]);
  });

  it('replaceNode swaps the node in place', () => {
    const store = useTreeStore();
    store.setTree(sampleTree());
    const updated = { ...(store.findNode('opportunity', 300) as IOpportunityTreeNode), title: 'Opp A renamed' };
    const ok = store.replaceNode('opportunity', 300, updated);
    expect(ok).toBe(true);
    expect((store.findNode('opportunity', 300) as IOpportunityTreeNode).title).toBe('Opp A renamed');
  });

  it('removeNode removes the node and its descendants', () => {
    const store = useTreeStore();
    store.setTree(sampleTree());
    const ok = store.removeNode('opportunity', 300);
    expect(ok).toBe(true);
    expect(store.findNode('opportunity', 300)).toBeNull();
    expect(store.findNode('opportunity', 301)).toBeNull();
    expect(store.findNode('solution', 400)).toBeNull();
    expect(store.findNode('solution', 401)).toBeNull();
  });

  it('removeNode of a solution only removes that solution', () => {
    const store = useTreeStore();
    store.setTree(sampleTree());
    const ok = store.removeNode('solution', 400);
    expect(ok).toBe(true);
    expect(store.findNode('solution', 400)).toBeNull();
    expect(store.findNode('opportunity', 300)).not.toBeNull();
  });

  it('canEdit reflects the tree flag', () => {
    const store = useTreeStore();
    const readOnly = sampleTree();
    readOnly.canEdit = false;
    store.setTree(readOnly);
    expect(store.canEdit).toBe(false);
  });

  it('reset clears state', () => {
    const store = useTreeStore();
    store.setTree(sampleTree());
    store.reset();
    expect(store.tree).toBeNull();
    expect(store.loaded).toBe(false);
  });
});
