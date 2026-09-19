import { describe, expect, it } from 'vitest';

import { OpportunityStatus } from '@/shared/model/enumerations/opportunity-status.model';
import { OutcomeStatus } from '@/shared/model/enumerations/outcome-status.model';
import { SolutionStatus } from '@/shared/model/enumerations/solution-status.model';
import { TeamRole } from '@/shared/model/enumerations/team-role.model';

import type { IOpportunityTreeNode, IOutcomeTreeNode, IProductTreeNode, ITeamTree } from './tree.model';
import { layoutTree, nodeKey } from './tree-layout';

const emptyTeam = (): ITeamTree => ({
  id: 1,
  name: 'T',
  description: null,
  createdDate: null,
  currentUserRole: TeamRole.EDITOR,
  canEdit: true,
  products: [],
});

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

describe('layoutTree', () => {
  it('returns empty result for null tree', () => {
    const r = layoutTree(null);
    expect(r.nodes).toEqual([]);
    expect(r.edges).toEqual([]);
    expect(r.width).toBe(0);
    expect(r.height).toBe(0);
  });

  it('returns empty result for a team with no products', () => {
    const r = layoutTree(emptyTeam());
    expect(r.nodes).toEqual([]);
  });

  it('places a single product at depth 0 and its outcome below it', () => {
    const t = emptyTeam();
    t.products = [product(1, [outcome(10)])];
    const r = layoutTree(t, { nodeHeight: 80, vGap: 60 });
    const p = r.nodes.find(n => n.key === nodeKey('product', 1))!;
    const o = r.nodes.find(n => n.key === nodeKey('outcome', 10))!;
    expect(p.y).toBe(0);
    expect(o.y).toBe(80 + 60);
    // outcome is centered under product
    expect(p.x + p.width / 2).toBeCloseTo(o.x + o.width / 2);
    // edge from product to outcome
    const edge = r.edges.find(e => e.fromKey === p.key && e.toKey === o.key);
    expect(edge).toBeDefined();
  });

  it('lays out three levels of nested opportunities without overlapping siblings', () => {
    const grand1 = opp(310);
    const grand2 = opp(311);
    const grand3 = opp(320);
    const parent1 = opp(300, [grand1, grand2]);
    const parent2 = opp(301, [grand3]);
    const t = emptyTeam();
    t.products = [product(1, [outcome(10, [parent1, parent2])])];
    const r = layoutTree(t, { nodeWidth: 100, hGap: 20 });
    // grand1, grand2, grand3 are at level 4 (product=0, outcome=1, parent=2, grand=3)
    const g1 = r.nodes.find(n => n.id === 310)!;
    const g2 = r.nodes.find(n => n.id === 311)!;
    const g3 = r.nodes.find(n => n.id === 320)!;
    for (const g of [g1, g2, g3]) expect(g.y).toBe(3 * (80 + 60));
    // Siblings do not overlap
    const xs = [g1, g2, g3].map(g => ({ left: g.x, right: g.x + g.width })).sort((a, b) => a.left - b.left);
    for (let i = 1; i < xs.length; i++) {
      expect(xs[i].left).toBeGreaterThanOrEqual(xs[i - 1].right);
    }
    // Different parent branches (parent1 vs parent2 grand) do not overlap either
    const p300 = r.nodes.find(n => n.id === 300)!;
    const p301 = r.nodes.find(n => n.id === 301)!;
    expect(p301.x).toBeGreaterThanOrEqual(p300.x + p300.width);
  });

  it('positions multiple products in the top row without overlap', () => {
    const t = emptyTeam();
    t.products = [product(1, [outcome(10)]), product(2, [outcome(20)])];
    const r = layoutTree(t);
    const p1 = r.nodes.find(n => n.id === 1)!;
    const p2 = r.nodes.find(n => n.id === 2)!;
    expect(p1.y).toBe(0);
    expect(p2.y).toBe(0);
    expect(p2.x).toBeGreaterThanOrEqual(p1.x + p1.width);
  });

  it('honours focusedProductId by dropping other product branches', () => {
    const t = emptyTeam();
    t.products = [product(1, [outcome(10)]), product(2, [outcome(20)])];
    const r = layoutTree(t, { focusedProductId: 2 });
    expect(r.nodes.find(n => n.id === 1)).toBeUndefined();
    expect(r.nodes.find(n => n.id === 10)).toBeUndefined();
    expect(r.nodes.find(n => n.id === 2)).toBeDefined();
    expect(r.nodes.find(n => n.id === 20)).toBeDefined();
  });

  it('lays out a solution node underneath an opportunity', () => {
    const solution = { id: 401, title: 'Sol', status: SolutionStatus.IDEA, sortOrder: 0 };
    const t = emptyTeam();
    t.products = [product(1, [outcome(10, [opp(300, [], [solution])])])];
    const r = layoutTree(t);
    const s = r.nodes.find(n => n.key === nodeKey('solution', 401))!;
    expect(s).toBeDefined();
    expect(s.type).toBe('solution');
  });

  it('skips descendants and connectors under a collapsed subtree, and reports hidden-descendant count', () => {
    const grand = opp(310);
    const parent = opp(300, [grand]);
    const t = emptyTeam();
    t.products = [product(1, [outcome(10, [parent])])];
    const collapsedKeys = new Set<string>([nodeKey('outcome', 10)]);
    const r = layoutTree(t, { collapsedKeys });
    // The collapsed outcome is still laid out.
    const o = r.nodes.find(n => n.key === nodeKey('outcome', 10))!;
    expect(o).toBeDefined();
    expect(o.collapsed).toBe(true);
    expect(o.hasChildren).toBe(true);
    expect(o.hiddenDescendantCount).toBe(2);
    // Descendants and their connectors disappear.
    expect(r.nodes.find(n => n.id === 300)).toBeUndefined();
    expect(r.nodes.find(n => n.id === 310)).toBeUndefined();
    expect(r.edges.find(e => e.toKey === nodeKey('opportunity', 300))).toBeUndefined();
  });

  it('a nested collapsed branch stays collapsed when an ancestor is expanded', () => {
    const grand = opp(310);
    const parent = opp(300, [grand]);
    const t = emptyTeam();
    t.products = [product(1, [outcome(10, [parent])])];
    const collapsedKeys = new Set<string>([nodeKey('opportunity', 300)]);
    const r = layoutTree(t, { collapsedKeys });
    // Outcome is expanded and visible.
    expect(r.nodes.find(n => n.id === 10)).toBeDefined();
    // Opportunity 300 is laid out, but its child is hidden.
    const p = r.nodes.find(n => n.key === nodeKey('opportunity', 300))!;
    expect(p.collapsed).toBe(true);
    expect(p.hiddenDescendantCount).toBe(1);
    expect(r.nodes.find(n => n.id === 310)).toBeUndefined();
  });

  it('a leaf node has hasChildren=false and no chevron info', () => {
    const t = emptyTeam();
    t.products = [product(1, [outcome(10, [opp(300)])])];
    const r = layoutTree(t);
    const leaf = r.nodes.find(n => n.id === 300)!;
    expect(leaf.hasChildren).toBe(false);
    expect(leaf.collapsed).toBe(false);
    expect(leaf.hiddenDescendantCount).toBe(0);
  });

  it('lays out a 500-node tree in well under 2 seconds', () => {
    // Build a tree with 1 product, 5 outcomes, each with a chain of nested opportunities + solutions.
    const t = emptyTeam();
    const outcomes: IOutcomeTreeNode[] = [];
    let created = 1; // product
    let nextId = 1000;
    for (let o = 0; o < 5; o++) {
      const opps: IOpportunityTreeNode[] = [];
      for (let i = 0; i < 20; i++) {
        const child = opp(nextId++, [], [{ id: nextId++, title: 's', status: SolutionStatus.IDEA, sortOrder: 0 } as any]);
        created += 2;
        const parent = opp(nextId++, [child]);
        created += 1;
        opps.push(parent);
      }
      outcomes.push(outcome(nextId++, opps));
      created += 1;
    }
    t.products = [product(1, outcomes)];
    // Pad to ~500 nodes with more solutions.
    while (created < 500) {
      outcomes[0].opportunities[0].solutions!.push({ id: nextId++, title: 's', status: SolutionStatus.IDEA, sortOrder: 0 } as any);
      created += 1;
    }
    const started = performance.now();
    const r = layoutTree(t);
    const elapsed = performance.now() - started;
    expect(r.nodes.length).toBeGreaterThanOrEqual(500);
    expect(elapsed).toBeLessThan(2000);
  });
});
