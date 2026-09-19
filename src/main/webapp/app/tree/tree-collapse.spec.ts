import { beforeEach, describe, expect, it, vi } from 'vitest';

import { OpportunityStatus } from '@/shared/model/enumerations/opportunity-status.model';
import { OutcomeStatus } from '@/shared/model/enumerations/outcome-status.model';
import { TeamRole } from '@/shared/model/enumerations/team-role.model';

import type { IOpportunityTreeNode, IOutcomeTreeNode, IProductTreeNode, ITeamTree } from './tree.model';
import { CollapseState, type CollapseStorage, collapseKey, pruneKeys, storageKeyFor } from './tree-collapse';

const opp = (id: number, children: IOpportunityTreeNode[] = [], solutions: any[] = []): IOpportunityTreeNode => ({
  id,
  title: `Opp ${id}`,
  status: OpportunityStatus.IDENTIFIED,
  valuerating: 3,
  complexity: 3,
  sortOrder: 0,
  parentId: null,
  children,
  solutions,
});

const outcome = (id: number, opps: IOpportunityTreeNode[] = []): IOutcomeTreeNode => ({
  id,
  title: `Outcome ${id}`,
  status: OutcomeStatus.ACTIVE,
  sortOrder: 0,
  opportunities: opps,
});

const product = (id: number, outcomes: IOutcomeTreeNode[] = []): IProductTreeNode => ({
  id,
  name: `Product ${id}`,
  archived: false,
  outcomes,
});

const tree = (products: IProductTreeNode[] = []): ITeamTree => ({
  id: 1,
  name: 'T',
  currentUserRole: TeamRole.EDITOR,
  canEdit: true,
  products,
});

class MemoryStorage implements CollapseStorage {
  data = new Map<string, string>();
  getItem(k: string) {
    return this.data.has(k) ? (this.data.get(k) as string) : null;
  }
  setItem(k: string, v: string) {
    this.data.set(k, v);
  }
  removeItem(k: string) {
    this.data.delete(k);
  }
}

describe('storageKeyFor', () => {
  it('scopes the key by user login and team id', () => {
    expect(storageKeyFor('alice', 7)).toBe('ombuto.tree.collapse.alice.7');
    expect(storageKeyFor('bob', 7)).not.toBe(storageKeyFor('alice', 7));
    expect(storageKeyFor('alice', 8)).not.toBe(storageKeyFor('alice', 7));
  });
  it('falls back for anonymous / no-team', () => {
    expect(storageKeyFor(null, null)).toBe('ombuto.tree.collapse.anonymous.none');
  });
});

describe('pruneKeys', () => {
  it('drops ids of nodes that no longer exist', () => {
    const t = tree([product(1, [outcome(10, [opp(100)])])]);
    const stored = [collapseKey('opportunity', 100), collapseKey('opportunity', 999)];
    const kept = pruneKeys(stored, t);
    expect(kept.has(collapseKey('opportunity', 100))).toBe(true);
    expect(kept.has(collapseKey('opportunity', 999))).toBe(false);
  });
});

describe('CollapseState — persistence & pruning', () => {
  let storage: MemoryStorage;
  beforeEach(() => {
    storage = new MemoryStorage();
  });

  it('collapse writes through to storage under the correct key', () => {
    const t = tree([product(1, [outcome(10, [opp(100)])])]);
    const s = new CollapseState({ userLogin: 'alice', teamId: 7, tree: t, storage });
    s.collapse('opportunity', 100);
    const raw = storage.getItem('ombuto.tree.collapse.alice.7');
    expect(raw).not.toBeNull();
    expect(JSON.parse(raw as string)).toContain('opportunity:100');
  });

  it('restore on construction rebuilds state from storage', () => {
    storage.setItem('ombuto.tree.collapse.alice.7', JSON.stringify(['opportunity:100']));
    const t = tree([product(1, [outcome(10, [opp(100)])])]);
    const s = new CollapseState({ userLogin: 'alice', teamId: 7, tree: t, storage });
    expect(s.isCollapsed('opportunity', 100)).toBe(true);
    expect(s.isCollapsed('opportunity', 200)).toBe(false);
  });

  it('prunes ids of deleted nodes on construction and persists the pruned set', () => {
    storage.setItem('ombuto.tree.collapse.alice.7', JSON.stringify(['opportunity:100', 'opportunity:999']));
    const t = tree([product(1, [outcome(10, [opp(100)])])]);
    const s = new CollapseState({ userLogin: 'alice', teamId: 7, tree: t, storage });
    expect(s.isCollapsed('opportunity', 100)).toBe(true);
    expect(s.isCollapsed('opportunity', 999)).toBe(false);
    // Persisted pruned form has only the surviving id.
    const persisted = JSON.parse(storage.getItem('ombuto.tree.collapse.alice.7') as string);
    expect(persisted).toEqual(['opportunity:100']);
  });

  it('reconcile prunes ids that disappear from a freshly loaded tree', () => {
    storage.setItem('ombuto.tree.collapse.alice.7', JSON.stringify(['opportunity:100']));
    const t1 = tree([product(1, [outcome(10, [opp(100)])])]);
    const s = new CollapseState({ userLogin: 'alice', teamId: 7, tree: t1, storage });
    expect(s.isCollapsed('opportunity', 100)).toBe(true);
    const t2 = tree([product(1, [outcome(10, [])])]);
    s.reconcile(t2);
    expect(s.isCollapsed('opportunity', 100)).toBe(false);
    const persisted = JSON.parse(storage.getItem('ombuto.tree.collapse.alice.7') as string);
    expect(persisted).toEqual([]);
  });

  it('degrades to in-memory when storage is null', () => {
    const t = tree([product(1, [outcome(10, [opp(100)])])]);
    const s = new CollapseState({ userLogin: 'alice', teamId: 7, tree: t, storage: null });
    expect(s.persistent).toBe(false);
    s.collapse('opportunity', 100);
    expect(s.isCollapsed('opportunity', 100)).toBe(true);
  });

  it('degrades to in-memory when storage throws on setItem', () => {
    const throwing: CollapseStorage = {
      getItem: () => null,
      setItem: vi.fn(() => {
        throw new Error('QuotaExceeded');
      }),
    };
    const t = tree([product(1, [outcome(10, [opp(100)])])]);
    const s = new CollapseState({ userLogin: 'alice', teamId: 7, tree: t, storage: throwing });
    // Should not throw.
    expect(() => s.collapse('opportunity', 100)).not.toThrow();
    expect(s.isCollapsed('opportunity', 100)).toBe(true);
  });

  it('degrades to in-memory when stored JSON is malformed', () => {
    storage.setItem('ombuto.tree.collapse.alice.7', '{not-json');
    const t = tree([product(1, [outcome(10, [opp(100)])])]);
    const s = new CollapseState({ userLogin: 'alice', teamId: 7, tree: t, storage });
    expect(s.isCollapsed('opportunity', 100)).toBe(false);
    s.collapse('opportunity', 100);
    expect(s.isCollapsed('opportunity', 100)).toBe(true);
  });

  it('toggle flips the state and returns the new collapsed flag', () => {
    const t = tree([product(1, [outcome(10, [opp(100)])])]);
    const s = new CollapseState({ userLogin: 'alice', teamId: 7, tree: t, storage });
    expect(s.toggle('opportunity', 100)).toBe(true);
    expect(s.isCollapsed('opportunity', 100)).toBe(true);
    expect(s.toggle('opportunity', 100)).toBe(false);
    expect(s.isCollapsed('opportunity', 100)).toBe(false);
  });
});
