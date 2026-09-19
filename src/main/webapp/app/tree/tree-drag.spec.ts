import { describe, expect, it, vi } from 'vitest';

import { OpportunityStatus } from '@/shared/model/enumerations/opportunity-status.model';
import { OutcomeStatus } from '@/shared/model/enumerations/outcome-status.model';
import { TeamRole } from '@/shared/model/enumerations/team-role.model';

import { layoutTree } from './tree-layout';
import type { LayoutNode } from './tree-layout';
import type { ITeamTree, TreeNode } from './tree.model';
import { createDragLifecycle, DRAG_THRESHOLD_PX, resolveDropTarget } from './tree-drag';

// A small tree with two products, one outcome per product, one opportunity under
// each outcome. Enough to exercise reparent, insert-before/after, and rejection
// of the moving-node / descendant / wrong-parent-type cases.
const makeTree = (): ITeamTree => ({
  id: 1,
  name: 'T',
  description: null,
  createdDate: null,
  currentUserRole: TeamRole.EDITOR,
  canEdit: true,
  products: [
    {
      id: 100,
      name: 'P1',
      archived: false,
      outcomes: [
        {
          id: 200,
          title: 'O1',
          status: OutcomeStatus.ACTIVE,
          sortOrder: 0,
          opportunities: [
            {
              id: 300,
              title: 'Opp1',
              status: OpportunityStatus.IDENTIFIED,
              sortOrder: 0,
              parentId: null,
              children: [
                {
                  id: 301,
                  title: 'OppChild',
                  status: OpportunityStatus.IDENTIFIED,
                  sortOrder: 0,
                  parentId: 300,
                  children: [],
                  solutions: [],
                },
              ],
              solutions: [],
            },
          ],
        },
      ],
    },
    {
      id: 101,
      name: 'P2',
      archived: false,
      outcomes: [
        {
          id: 201,
          title: 'O2',
          status: OutcomeStatus.ACTIVE,
          sortOrder: 0,
          opportunities: [
            {
              id: 310,
              title: 'Opp2',
              status: OpportunityStatus.IDENTIFIED,
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
});

const centre = (n: LayoutNode) => ({ x: n.x + n.width / 2, y: n.y + n.height / 2 });
const leftEdge = (n: LayoutNode) => ({ x: n.x + n.width * 0.1, y: n.y + n.height / 2 });
const rightEdge = (n: LayoutNode) => ({ x: n.x + n.width * 0.9, y: n.y + n.height / 2 });

describe('resolveDropTarget', () => {
  const tree = makeTree();
  const layout = layoutTree(tree);
  const nodeByKey = (k: string) => layout.nodes.find(n => n.key === k)!;

  it('resolves a valid reparent when hovering the centre of a permitted parent', () => {
    // Move outcome 200 (currently under product 100) onto product 101.
    const outcome200 = tree.products[0].outcomes[0] as unknown as TreeNode;
    const target = resolveDropTarget({
      pointer: centre(nodeByKey('product:101')),
      layoutNodes: layout.nodes,
      tree,
      movingType: 'outcome',
      movingId: 200,
      movingNode: outcome200,
      currentParentType: 'product',
      currentParentId: 100,
    });
    expect(target).toEqual({
      kind: 'reparent',
      parentType: 'product',
      parentId: 101,
      targetKey: 'product:101',
    });
  });

  it('returns null over the moving node itself', () => {
    const opp = tree.products[0].outcomes[0].opportunities[0] as unknown as TreeNode;
    const target = resolveDropTarget({
      pointer: centre(nodeByKey('opportunity:300')),
      layoutNodes: layout.nodes,
      tree,
      movingType: 'opportunity',
      movingId: 300,
      movingNode: opp,
      currentParentType: 'outcome',
      currentParentId: 200,
    });
    expect(target).toBeNull();
  });

  it('returns null over one of the moving node’s own descendants', () => {
    const opp = tree.products[0].outcomes[0].opportunities[0] as unknown as TreeNode;
    // Hovering the child 301 while dragging its parent 300 must not highlight.
    const target = resolveDropTarget({
      pointer: centre(nodeByKey('opportunity:301')),
      layoutNodes: layout.nodes,
      tree,
      movingType: 'opportunity',
      movingId: 300,
      movingNode: opp,
      currentParentType: 'outcome',
      currentParentId: 200,
    });
    expect(target).toBeNull();
  });

  it('returns null over an invalid parent type', () => {
    // Solutions cannot become children of an outcome (only opportunity).
    const solution: TreeNode = { id: 999, title: 'S', sortOrder: 0 } as unknown as TreeNode;
    const target = resolveDropTarget({
      pointer: centre(nodeByKey('outcome:201')),
      layoutNodes: layout.nodes,
      tree,
      movingType: 'solution',
      movingId: 999,
      movingNode: solution,
      currentParentType: 'opportunity',
      currentParentId: 310,
    });
    expect(target).toBeNull();
  });

  it('returns an insertion target on the left edge of a sibling', () => {
    // Move outcome 200 to insert BEFORE outcome 201 (i.e. under product 101 at pos 0).
    // But 201 is under product 101 not 100, so this is a re-parent + position=0.
    const outcome200 = tree.products[0].outcomes[0] as unknown as TreeNode;
    const target = resolveDropTarget({
      pointer: leftEdge(nodeByKey('outcome:201')),
      layoutNodes: layout.nodes,
      tree,
      movingType: 'outcome',
      movingId: 200,
      movingNode: outcome200,
      currentParentType: 'product',
      currentParentId: 100,
    });
    expect(target).not.toBeNull();
    expect(target!.kind).toBe('insert');
    if (target!.kind === 'insert') {
      expect(target.parentType).toBe('product');
      expect(target.parentId).toBe(101);
      expect(target.position).toBe(0);
    }
  });

  it('returns an insertion target on the right edge of a sibling', () => {
    const outcome200 = tree.products[0].outcomes[0] as unknown as TreeNode;
    const target = resolveDropTarget({
      pointer: rightEdge(nodeByKey('outcome:201')),
      layoutNodes: layout.nodes,
      tree,
      movingType: 'outcome',
      movingId: 200,
      movingNode: outcome200,
      currentParentType: 'product',
      currentParentId: 100,
    });
    expect(target).not.toBeNull();
    expect(target!.kind).toBe('insert');
    if (target!.kind === 'insert') {
      expect(target.parentType).toBe('product');
      expect(target.parentId).toBe(101);
      expect(target.position).toBe(1);
    }
  });

  it('shifts insertion position down by one when reordering later within the same sibling group', () => {
    // Tree with three sibling opportunities under one outcome, and reorder the
    // first (Opp A) to a later slot. Since the moving node is removed before
    // insertion, hovering the right edge of Opp C (index 2 in the WITH-moving
    // list) must resolve to position 2 in the AFTER-removal list (not 3).
    const localTree: ITeamTree = {
      id: 1,
      name: 'T',
      description: null,
      createdDate: null,
      currentUserRole: TeamRole.EDITOR,
      canEdit: true,
      products: [
        {
          id: 500,
          name: 'P',
          archived: false,
          outcomes: [
            {
              id: 600,
              title: 'O',
              status: OutcomeStatus.ACTIVE,
              sortOrder: 0,
              opportunities: [
                { id: 700, title: 'A', status: OpportunityStatus.IDENTIFIED, sortOrder: 0, parentId: null, children: [], solutions: [] },
                { id: 701, title: 'B', status: OpportunityStatus.IDENTIFIED, sortOrder: 1, parentId: null, children: [], solutions: [] },
                { id: 702, title: 'C', status: OpportunityStatus.IDENTIFIED, sortOrder: 2, parentId: null, children: [], solutions: [] },
              ],
            },
          ],
        },
      ],
    };
    const localLayout = layoutTree(localTree);
    const nodeC = localLayout.nodes.find(n => n.key === 'opportunity:702')!;
    const opp700 = localTree.products[0].outcomes[0].opportunities[0] as unknown as TreeNode;
    const target = resolveDropTarget({
      pointer: rightEdge(nodeC),
      layoutNodes: localLayout.nodes,
      tree: localTree,
      movingType: 'opportunity',
      movingId: 700,
      movingNode: opp700,
      currentParentType: 'outcome',
      currentParentId: 600,
    });
    expect(target).not.toBeNull();
    expect(target!.kind).toBe('insert');
    if (target!.kind === 'insert') {
      // AFTER removal the list is [B, C]; drop-after-C is position 2.
      expect(target.position).toBe(2);
    }
  });

  it('allows inserting products among the top row (parentType team)', () => {
    // Move product 101 to insert BEFORE product 100 — top row reorder.
    const product101 = tree.products[1] as unknown as TreeNode;
    const target = resolveDropTarget({
      pointer: leftEdge(nodeByKey('product:100')),
      layoutNodes: layout.nodes,
      tree,
      movingType: 'product',
      movingId: 101,
      movingNode: product101,
      currentParentType: 'team',
      currentParentId: null,
    });
    expect(target).not.toBeNull();
    expect(target!.kind).toBe('insert');
    if (target!.kind === 'insert') {
      expect(target.parentType).toBe('team');
      expect(target.parentId).toBeNull();
      expect(target.position).toBe(0);
    }
  });
});

describe('createDragLifecycle', () => {
  const makeCbs = (opts: { canStart?: boolean; target?: any } = {}) => ({
    canStart: vi.fn(() => opts.canStart !== false),
    toCanvas: vi.fn((cx: number, cy: number) => ({ x: cx, y: cy })),
    resolveTarget: vi.fn(() => opts.target ?? null),
    onDrop: vi.fn(),
    onCancel: vi.fn(),
    onActivate: vi.fn(),
    onMove: vi.fn(),
  });

  it('does not activate a drag until the movement threshold is crossed', () => {
    const cbs = makeCbs({ target: { kind: 'reparent', parentType: 'product', parentId: 1, targetKey: 'p:1' } });
    const l = createDragLifecycle(cbs);
    l.onMouseDown('outcome', 200, 100, 100);
    expect(l.phase()).toBe('candidate');
    // A tiny move under threshold — still a candidate, no activation.
    l.onMouseMove(100 + DRAG_THRESHOLD_PX - 1, 100);
    expect(l.phase()).toBe('candidate');
    expect(cbs.onActivate).not.toHaveBeenCalled();
    // Mouseup here is a plain click — no drop, no cancel.
    l.onMouseUp();
    expect(cbs.onDrop).not.toHaveBeenCalled();
    expect(cbs.onCancel).not.toHaveBeenCalled();
    expect(l.phase()).toBe('idle');
  });

  it('activates and calls onDrop when released over a valid target', () => {
    const target = { kind: 'reparent' as const, parentType: 'product' as const, parentId: 1, targetKey: 'p:1' };
    const cbs = makeCbs({ target });
    const l = createDragLifecycle(cbs);
    l.onMouseDown('outcome', 200, 0, 0);
    l.onMouseMove(20, 20); // past threshold
    expect(l.phase()).toBe('dragging');
    expect(cbs.onActivate).toHaveBeenCalledOnce();
    l.onMouseUp();
    expect(cbs.onDrop).toHaveBeenCalledWith(target);
    expect(cbs.onCancel).not.toHaveBeenCalled();
    expect(l.phase()).toBe('idle');
  });

  it('cancels (snap back) when dropped over no valid target', () => {
    const cbs = makeCbs({ target: null });
    const l = createDragLifecycle(cbs);
    l.onMouseDown('outcome', 200, 0, 0);
    l.onMouseMove(20, 20);
    l.onMouseUp();
    expect(cbs.onDrop).not.toHaveBeenCalled();
    expect(cbs.onCancel).toHaveBeenCalledOnce();
  });

  it('cancels on Escape mid-drag', () => {
    const cbs = makeCbs({ target: { kind: 'reparent', parentType: 'product', parentId: 1, targetKey: 'p:1' } });
    const l = createDragLifecycle(cbs);
    l.onMouseDown('outcome', 200, 0, 0);
    l.onMouseMove(20, 20);
    expect(l.phase()).toBe('dragging');
    l.onKeyDown('Escape');
    expect(cbs.onCancel).toHaveBeenCalledOnce();
    expect(l.phase()).toBe('idle');
    // A subsequent mouseup does nothing.
    l.onMouseUp();
    expect(cbs.onDrop).not.toHaveBeenCalled();
  });

  it('refuses to start a drag when canStart returns false (viewer)', () => {
    const cbs = makeCbs({ canStart: false });
    const l = createDragLifecycle(cbs);
    l.onMouseDown('outcome', 200, 0, 0);
    expect(l.phase()).toBe('idle');
    l.onMouseMove(20, 20);
    l.onMouseUp();
    expect(cbs.onActivate).not.toHaveBeenCalled();
    expect(cbs.onDrop).not.toHaveBeenCalled();
    expect(cbs.onCancel).not.toHaveBeenCalled();
  });
});
