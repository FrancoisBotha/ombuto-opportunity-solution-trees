import { beforeEach, describe, expect, it, vi } from 'vitest';

import { createPinia, setActivePinia } from 'pinia';

import { OpportunityStatus } from '@/shared/model/enumerations/opportunity-status.model';
import { TeamRole } from '@/shared/model/enumerations/team-role.model';

import type { IOpportunityTreeNode, ISolutionTreeNode, ITeamTree } from './tree.model';
import { validChildTypes } from './tree.model';
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

  describe('validChildTypes', () => {
    it('offers only outcome under product', () => {
      expect(validChildTypes('product')).toEqual(['outcome']);
    });
    it('offers only opportunity under outcome', () => {
      expect(validChildTypes('outcome')).toEqual(['opportunity']);
    });
    it('offers opportunity or solution under opportunity', () => {
      expect(validChildTypes('opportunity').sort()).toEqual(['opportunity', 'solution'].sort());
    });
    it('offers nothing under solution', () => {
      expect(validChildTypes('solution')).toEqual([]);
    });
  });

  describe('addProduct', () => {
    it('POSTs a product and appends it to the tree without a reload', async () => {
      const store = useTreeStore();
      store.teamId = 10;
      store.setTree(sampleTree());
      const created = { id: 555, name: 'New product', outcomes: [] };
      const svc = { createProduct: vi.fn().mockResolvedValue(created) } as any;
      const result = await store.addProduct({ name: 'New product', description: 'd', vision: 'v' }, svc);
      expect(result).toEqual(created);
      expect(svc.createProduct).toHaveBeenCalledWith(10, { name: 'New product', description: 'd', vision: 'v' });
      expect(store.products.map(p => p.id)).toEqual([100, 555]);
      expect(store.writeError).toBeNull();
    });
    it('records a 403 forbidden write error and does not mutate the tree', async () => {
      const store = useTreeStore();
      store.teamId = 10;
      store.setTree(sampleTree());
      const svc = { createProduct: vi.fn().mockRejectedValue({ response: { status: 403 } }) } as any;
      const result = await store.addProduct({ name: 'x' }, svc);
      expect(result).toBeNull();
      expect(store.writeError).toBe('forbidden');
      expect(store.writeErrorMessage).toMatch(/permission/i);
      expect(store.products.map(p => p.id)).toEqual([100]);
    });
    it('records a 400 validation write error', async () => {
      const store = useTreeStore();
      store.teamId = 10;
      store.setTree(sampleTree());
      const svc = {
        createProduct: vi.fn().mockRejectedValue({ response: { status: 400, data: { title: 'name is required' } } }),
      } as any;
      const result = await store.addProduct({ name: '' }, svc);
      expect(result).toBeNull();
      expect(store.writeError).toBe('validation');
      expect(store.writeErrorMessage).toMatch(/name is required/i);
      expect(store.products.map(p => p.id)).toEqual([100]);
    });
  });

  describe('addChild', () => {
    it('adds an outcome under a product', async () => {
      const store = useTreeStore();
      store.setTree(sampleTree());
      const svc = { createOutcome: vi.fn().mockResolvedValue({ id: 210, title: 'O new', opportunities: [] }) } as any;
      const created = await store.addChild('product', 100, 'outcome', { title: 'O new' }, svc);
      expect(created?.id).toBe(210);
      expect(svc.createOutcome).toHaveBeenCalledWith(100, { title: 'O new' });
      expect(store.childrenOf('product', 100).map(n => (n as any).id)).toEqual([200, 210]);
    });
    it('adds an opportunity under an outcome, appending last', async () => {
      const store = useTreeStore();
      store.setTree(sampleTree());
      const svc = {
        createOpportunityUnderOutcome: vi.fn().mockResolvedValue({ id: 310, title: 'Opp new', children: [], solutions: [] }),
      } as any;
      const created = await store.addChild('outcome', 200, 'opportunity', { title: 'Opp new' }, svc);
      expect(created?.id).toBe(310);
      expect(svc.createOpportunityUnderOutcome).toHaveBeenCalledWith(200, { title: 'Opp new' });
      expect(store.childrenOf('outcome', 200).map(n => (n as any).id)).toEqual([300, 310]);
    });
    it('adds a nested opportunity under another opportunity, three levels deep', async () => {
      const store = useTreeStore();
      store.setTree(sampleTree());
      // Level 3: parent is 301 (already nested under 300, which is under outcome 200).
      const svc = {
        createOpportunityUnderOpportunity: vi.fn().mockResolvedValue({ id: 320, title: 'Opp L3', children: [], solutions: [] }),
      } as any;
      const created = await store.addChild('opportunity', 301, 'opportunity', { title: 'Opp L3' }, svc);
      expect(created?.id).toBe(320);
      // The service must be given the enclosing outcome id (200) alongside the parent id (301).
      expect(svc.createOpportunityUnderOpportunity).toHaveBeenCalledWith(301, 200, { title: 'Opp L3' });
      expect(store.childrenOf('opportunity', 301).map(n => (n as any).id)).toContain(320);
    });
    it('adds a solution under an opportunity', async () => {
      const store = useTreeStore();
      store.setTree(sampleTree());
      const svc = { createSolution: vi.fn().mockResolvedValue({ id: 410, title: 'Sol new' } as ISolutionTreeNode) } as any;
      const created = await store.addChild('opportunity', 300, 'solution', { title: 'Sol new' }, svc);
      expect(created?.id).toBe(410);
      const sols = (store.findNode('opportunity', 300) as IOpportunityTreeNode).solutions.map(s => s.id);
      expect(sols).toEqual([400, 410]);
    });
    it('rejects an invalid child type combination', async () => {
      const store = useTreeStore();
      store.setTree(sampleTree());
      const svc = { createSolution: vi.fn() } as any;
      const result = await store.addChild('outcome', 200, 'solution', { title: 'nope' }, svc);
      expect(result).toBeNull();
      expect(store.writeError).toBe('validation');
      expect(svc.createSolution).not.toHaveBeenCalled();
    });
    it('records a 403 forbidden error on addChild', async () => {
      const store = useTreeStore();
      store.setTree(sampleTree());
      const svc = { createOutcome: vi.fn().mockRejectedValue({ response: { status: 403 } }) } as any;
      const result = await store.addChild('product', 100, 'outcome', { title: 'x' }, svc);
      expect(result).toBeNull();
      expect(store.writeError).toBe('forbidden');
      expect(store.childrenOf('product', 100).map(n => (n as any).id)).toEqual([200]);
    });
  });

  describe('updateNode', () => {
    it('patches title and status on the local node when the service succeeds', async () => {
      const store = useTreeStore();
      store.setTree(sampleTree());
      const svc = {
        updateNode: vi.fn().mockResolvedValue({ id: 300, title: 'Opp A edited', status: OpportunityStatus.PRIORITISED }),
      } as any;
      const result = await store.updateNode('opportunity', 300, { title: 'Opp A edited', status: OpportunityStatus.PRIORITISED }, svc);
      expect(result).not.toBeNull();
      expect(svc.updateNode).toHaveBeenCalledWith('opportunity', 300, { title: 'Opp A edited', status: OpportunityStatus.PRIORITISED });
      const node = store.findNode('opportunity', 300) as IOpportunityTreeNode;
      expect(node.title).toBe('Opp A edited');
      expect(node.status).toBe(OpportunityStatus.PRIORITISED);
      // Descendants are preserved.
      expect(node.children.map(c => c.id)).toEqual([301]);
    });
    it('returns null and records a write error when the service rejects', async () => {
      const store = useTreeStore();
      store.setTree(sampleTree());
      const svc = { updateNode: vi.fn().mockRejectedValue({ response: { status: 403 } }) } as any;
      const result = await store.updateNode('opportunity', 300, { title: 'x' }, svc);
      expect(result).toBeNull();
      expect(store.writeError).toBe('forbidden');
      const node = store.findNode('opportunity', 300) as IOpportunityTreeNode;
      expect(node.title).toBe('Opp A');
    });
  });

  describe('deleteNode', () => {
    it('deletes the node via the API and removes it and its descendants from the store', async () => {
      const store = useTreeStore();
      store.setTree(sampleTree());
      const svc = { deleteNode: vi.fn().mockResolvedValue(undefined) } as any;
      const ok = await store.deleteNode('opportunity', 300, svc);
      expect(ok).toBe(true);
      expect(svc.deleteNode).toHaveBeenCalledWith('opportunity', 300);
      expect(store.findNode('opportunity', 300)).toBeNull();
      expect(store.findNode('opportunity', 301)).toBeNull();
      expect(store.findNode('solution', 400)).toBeNull();
      expect(store.findNode('solution', 401)).toBeNull();
    });
    it('clears selection when the deleted node was selected', async () => {
      const store = useTreeStore();
      store.setTree(sampleTree());
      store.selectNode('opportunity', 300);
      const svc = { deleteNode: vi.fn().mockResolvedValue(undefined) } as any;
      await store.deleteNode('opportunity', 300, svc);
      expect(store.selectedNodeType).toBeNull();
      expect(store.selectedNodeId).toBeNull();
    });
    it('clears selection when the deleted node contained the selected node', async () => {
      const store = useTreeStore();
      store.setTree(sampleTree());
      store.selectNode('solution', 401);
      const svc = { deleteNode: vi.fn().mockResolvedValue(undefined) } as any;
      await store.deleteNode('outcome', 200, svc);
      expect(store.selectedNodeType).toBeNull();
      expect(store.selectedNodeId).toBeNull();
    });
    it('clears the focused product when the deleted node was the focused product', async () => {
      const store = useTreeStore();
      store.setTree(sampleTree());
      store.focusProduct(100);
      const svc = { deleteNode: vi.fn().mockResolvedValue(undefined) } as any;
      await store.deleteNode('product', 100, svc);
      expect(store.focusedProductId).toBeNull();
    });
    it('leaves the store unchanged on a 403', async () => {
      const store = useTreeStore();
      store.setTree(sampleTree());
      const svc = { deleteNode: vi.fn().mockRejectedValue({ response: { status: 403 } }) } as any;
      const ok = await store.deleteNode('opportunity', 300, svc);
      expect(ok).toBe(false);
      expect(store.writeError).toBe('forbidden');
      expect(store.findNode('opportunity', 300)).not.toBeNull();
    });
  });
});
