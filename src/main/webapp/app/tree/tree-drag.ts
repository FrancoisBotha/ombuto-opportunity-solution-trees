import type { LayoutNode } from './tree-layout';
import { canMove } from './tree-move';
import type { IOpportunityTreeNode, IOutcomeTreeNode, IProductTreeNode, ITeamTree, TreeNode, TreeNodeType } from './tree.model';

/**
 * A "reparent" drop — pointer landed on the interior of a valid new parent card.
 * Dropping here calls the store's moveNode action and the moved node is appended
 * as the last child of the new parent.
 */
export interface ReparentDropTarget {
  kind: 'reparent';
  parentType: TreeNodeType;
  parentId: number;
  /** The layout node that highlighted as the drop target. */
  targetKey: string;
}

/**
 * An "insertion" drop — pointer landed between two siblings (or before the first /
 * after the last). Dropping here reorders (and re-parents if the gap sits under a
 * different parent). Products get an 'insert' with parentType 'team'.
 */
export interface InsertDropTarget {
  kind: 'insert';
  parentType: TreeNodeType | 'team';
  parentId: number | null;
  /** Index inside the destination parent's children list. */
  position: number;
  /** Where to draw the insertion marker, in canvas node-space coordinates. */
  markerX: number;
  markerY: number;
  markerHeight: number;
}

export type DropTarget = ReparentDropTarget | InsertDropTarget;

export interface ParentInfo {
  parentType: TreeNodeType | 'team';
  parentId: number | null;
  siblings: Array<{ type: TreeNodeType; id: number }>;
  index: number;
}

/**
 * Build a map from `${type}:${id}` → ParentInfo for every node in the tree,
 * including products (parent = 'team'). Used by drop-target resolution to
 * find sibling groups without another tree walk.
 */
export const buildParentInfo = (tree: ITeamTree | null): Map<string, ParentInfo> => {
  const map = new Map<string, ParentInfo>();
  if (!tree) return map;
  const k = (t: TreeNodeType, id: number) => `${t}:${id}`;

  const productSibs = (tree.products ?? []).map(p => ({ type: 'product' as TreeNodeType, id: p.id }));
  (tree.products ?? []).forEach((p, i) => {
    map.set(k('product', p.id), { parentType: 'team', parentId: null, siblings: productSibs, index: i });
    const outcomeSibs = (p.outcomes ?? []).map(o => ({ type: 'outcome' as TreeNodeType, id: o.id }));
    (p.outcomes ?? []).forEach((o, oi) => {
      map.set(k('outcome', o.id), { parentType: 'product', parentId: p.id, siblings: outcomeSibs, index: oi });
      const oppSibs = (o.opportunities ?? []).map(op => ({ type: 'opportunity' as TreeNodeType, id: op.id }));
      (o.opportunities ?? []).forEach((op, opi) => {
        map.set(k('opportunity', op.id), { parentType: 'outcome', parentId: o.id, siblings: oppSibs, index: opi });
        walkOpp(op, map, k);
      });
    });
  });
  return map;
};

const walkOpp = (parent: IOpportunityTreeNode, map: Map<string, ParentInfo>, k: (t: TreeNodeType, id: number) => string): void => {
  // Under an opportunity, children (opportunities) and solutions each form
  // their own sibling group.
  const childSibs = (parent.children ?? []).map(c => ({ type: 'opportunity' as TreeNodeType, id: c.id }));
  (parent.children ?? []).forEach((c, i) => {
    map.set(k('opportunity', c.id), { parentType: 'opportunity', parentId: parent.id, siblings: childSibs, index: i });
    walkOpp(c, map, k);
  });
  const solSibs = (parent.solutions ?? []).map(s => ({ type: 'solution' as TreeNodeType, id: s.id }));
  (parent.solutions ?? []).forEach((s, i) => {
    map.set(k('solution', s.id), { parentType: 'opportunity', parentId: parent.id, siblings: solSibs, index: i });
  });
};

export interface ResolveArgs {
  pointer: { x: number; y: number };
  layoutNodes: LayoutNode[];
  tree: ITeamTree | null;
  movingType: TreeNodeType;
  movingId: number;
  movingNode: TreeNode;
  currentParentType: TreeNodeType | 'team';
  currentParentId: number | null;
  /**
   * Fraction of a node's width considered an "edge" (insertion) zone at each
   * side. Middle (1 - 2*edgeFraction) is a "reparent" zone. Default 0.25.
   */
  edgeFraction?: number;
}

const isInside = (n: LayoutNode, x: number, y: number): boolean => x >= n.x && x < n.x + n.width && y >= n.y && y < n.y + n.height;

/**
 * Resolve which drop target — if any — the pointer is over.
 *
 * Rules:
 *  - If the pointer is over a card and near its left/right edge AND the card
 *    is a sibling of the moving node under a valid parent → an insert marker
 *    between siblings.
 *  - If the pointer is over a card's middle AND canMove permits it as the
 *    new parent → a reparent target.
 *  - Otherwise null (nothing to drop on).
 *
 * A card that is the moving node itself, or one of its descendants, never
 * highlights. Products form their own sibling group under parent 'team';
 * only products themselves may insert there.
 */
export const resolveDropTarget = (args: ResolveArgs): DropTarget | null => {
  const { pointer, layoutNodes, tree, movingType, movingId, movingNode, currentParentType, currentParentId, edgeFraction = 0.25 } = args;
  if (!tree) return null;
  // Find the hovered node (if any).
  const hovered = layoutNodes.find(n => isInside(n, pointer.x, pointer.y));
  const parentInfo = buildParentInfo(tree);
  const k = (t: TreeNodeType, id: number) => `${t}:${id}`;

  const isValidReparent = (targetType: TreeNodeType, targetId: number): boolean =>
    canMove({
      nodeType: movingType,
      nodeId: movingId,
      node: movingNode,
      currentParentType,
      currentParentId,
      targetParentType: targetType,
      targetParentId: targetId,
    });

  const insertUnderInfo = (
    info: ParentInfo,
    hoveredIndex: number,
    side: 'left' | 'right',
    hoveredNode: LayoutNode,
  ): InsertDropTarget | null => {
    // Position is the array index at which the moved node lands, expressed in
    // the destination array AFTER the moved node has been removed from its
    // current position (matching moveNodeToPosition's contract).
    let position = side === 'left' ? hoveredIndex : hoveredIndex + 1;
    // Re-parent + insertion validity: the parent must accept movingType, and
    // must NOT be the moving node itself or its descendant, and NOT a no-op.
    if (info.parentType === 'team') {
      // Top-row sibling group is products only.
      if (movingType !== 'product') return null;
      if (currentParentType !== 'team') return null; // products can't reparent
      // Same-parent (top row) reorder — apply the same no-op / shift correction.
      const currentIndex = info.siblings.findIndex(s => s.type === movingType && s.id === movingId);
      if (currentIndex >= 0) {
        if (position === currentIndex || position === currentIndex + 1) return null;
        if (position > currentIndex) position -= 1;
      }
    } else {
      const okAsParent = isValidReparent(info.parentType, info.parentId as number);
      const sameParent = currentParentType === info.parentType && currentParentId === (info.parentId as number);
      if (!okAsParent && !sameParent) return null;
      // Also disallow inserting adjacent to oneself in the same parent (no-op),
      // and shift positions after the moving node down by one to account for
      // the fact that the node is removed before it is re-inserted.
      if (sameParent) {
        const currentIndex = info.siblings.findIndex(s => s.type === movingType && s.id === movingId);
        if (currentIndex >= 0) {
          if (position === currentIndex || position === currentIndex + 1) return null;
          if (position > currentIndex) position -= 1;
        }
      }
    }
    // Marker sits between the hovered node and its neighbour on `side`.
    let markerX: number;
    if (side === 'left') {
      const prevKey = hoveredIndex > 0 ? k(info.siblings[hoveredIndex - 1].type, info.siblings[hoveredIndex - 1].id) : null;
      const prev = prevKey ? layoutNodes.find(n => n.key === prevKey) : null;
      if (prev) markerX = (prev.x + prev.width + hoveredNode.x) / 2;
      else markerX = hoveredNode.x - 8;
    } else {
      const nextKey =
        hoveredIndex < info.siblings.length - 1 ? k(info.siblings[hoveredIndex + 1].type, info.siblings[hoveredIndex + 1].id) : null;
      const next = nextKey ? layoutNodes.find(n => n.key === nextKey) : null;
      if (next) markerX = (hoveredNode.x + hoveredNode.width + next.x) / 2;
      else markerX = hoveredNode.x + hoveredNode.width + 8;
    }
    return {
      kind: 'insert',
      parentType: info.parentType,
      parentId: info.parentId,
      position,
      markerX,
      markerY: hoveredNode.y,
      markerHeight: hoveredNode.height,
    };
  };

  if (!hovered) return null;

  // Never highlight the moving card itself.
  if (hovered.type === movingType && hovered.id === movingId) return null;

  // Never highlight a descendant of the moving node — check via canMove semantics
  // implicitly through the reparent test; but for insertion siblings we also need
  // a plain descendant check for the hovered card (which is a *sibling*, not a
  // parent). Compute descendants ad hoc.
  const descendants = new Set<string>();
  collectDescendantKeys(movingType, movingNode, descendants);
  if (descendants.has(k(hovered.type, hovered.id))) return null;

  const relX = pointer.x - hovered.x;
  const w = hovered.width;
  const leftEdge = w * edgeFraction;
  const rightEdge = w * (1 - edgeFraction);

  // Insertion zones — hovered card is a sibling under some parent.
  const info = parentInfo.get(k(hovered.type, hovered.id));
  if (info && hovered.type === movingType) {
    if (relX < leftEdge) {
      const t = insertUnderInfo(info, info.index, 'left', hovered);
      if (t) return t;
    } else if (relX >= rightEdge) {
      const t = insertUnderInfo(info, info.index, 'right', hovered);
      if (t) return t;
    }
  }

  // Reparent zone — hovered card is the candidate new parent.
  if (isValidReparent(hovered.type, hovered.id)) {
    return { kind: 'reparent', parentType: hovered.type, parentId: hovered.id, targetKey: hovered.key };
  }

  return null;
};

const collectDescendantKeys = (type: TreeNodeType, node: TreeNode, into: Set<string>): void => {
  const k = (t: TreeNodeType, id: number) => `${t}:${id}`;
  if (type === 'product') {
    for (const o of (node as IProductTreeNode).outcomes ?? []) {
      into.add(k('outcome', o.id));
      collectDescendantKeys('outcome', o, into);
    }
  } else if (type === 'outcome') {
    for (const o of (node as IOutcomeTreeNode).opportunities ?? []) {
      into.add(k('opportunity', o.id));
      collectDescendantKeys('opportunity', o, into);
    }
  } else if (type === 'opportunity') {
    for (const c of (node as IOpportunityTreeNode).children ?? []) {
      into.add(k('opportunity', c.id));
      collectDescendantKeys('opportunity', c, into);
    }
    for (const s of (node as IOpportunityTreeNode).solutions ?? []) {
      into.add(k('solution', s.id));
    }
  }
};

/**
 * Drag lifecycle — a plain state machine that owns the "did we cross the
 * click-vs-drag threshold?" decision. Extracted so it can be unit-tested
 * without mounting the whole editor.
 */
export const DRAG_THRESHOLD_PX = 5;

export interface DragCandidate {
  type: TreeNodeType;
  id: number;
  /** Client-space (screen) coordinates where the mousedown happened. */
  startClientX: number;
  startClientY: number;
}

export type DragPhase = 'idle' | 'candidate' | 'dragging' | 'cancelled';

export interface DragLifecycleCallbacks {
  /** Convert client (screen) coordinates to canvas node-space coordinates. */
  toCanvas: (clientX: number, clientY: number) => { x: number; y: number };
  /** Given a canvas-space pointer, resolve the current drop target (or null). */
  resolveTarget: (pointer: { x: number; y: number }) => DropTarget | null;
  /** Called with a resolved drop target when the user releases the pointer. */
  onDrop: (target: DropTarget) => void;
  /** Called when the drag is cancelled (escape, or dropped on nothing). */
  onCancel: () => void;
  /** Called when a candidate crosses the threshold and becomes an active drag. */
  onActivate?: () => void;
  /** Called when pointer moves during an active drag. */
  onMove?: (pointer: { x: number; y: number }, clientX: number, clientY: number, target: DropTarget | null) => void;
  /**
   * Called on mousedown to decide whether to start a candidate. Returning
   * false makes the mousedown a no-op — used for read-only trees.
   */
  canStart: () => boolean;
}

export interface DragLifecycle {
  phase(): DragPhase;
  onMouseDown(type: TreeNodeType, id: number, clientX: number, clientY: number): void;
  onMouseMove(clientX: number, clientY: number): void;
  onMouseUp(): void;
  onKeyDown(key: string): void;
  reset(): void;
}

export const createDragLifecycle = (cbs: DragLifecycleCallbacks): DragLifecycle => {
  let phase: DragPhase = 'idle';
  let candidate: DragCandidate | null = null;
  let currentTarget: DropTarget | null = null;

  return {
    phase: () => phase,
    onMouseDown(type, id, clientX, clientY) {
      if (!cbs.canStart()) return;
      candidate = { type, id, startClientX: clientX, startClientY: clientY };
      phase = 'candidate';
      currentTarget = null;
    },
    onMouseMove(clientX, clientY) {
      if (phase === 'idle' || phase === 'cancelled') return;
      if (!candidate) return;
      const dx = clientX - candidate.startClientX;
      const dy = clientY - candidate.startClientY;
      if (phase === 'candidate') {
        if (Math.hypot(dx, dy) < DRAG_THRESHOLD_PX) return;
        phase = 'dragging';
        cbs.onActivate?.();
      }
      const pointer = cbs.toCanvas(clientX, clientY);
      currentTarget = cbs.resolveTarget(pointer);
      cbs.onMove?.(pointer, clientX, clientY, currentTarget);
    },
    onMouseUp() {
      const wasDragging = phase === 'dragging';
      const target = currentTarget;
      phase = 'idle';
      candidate = null;
      currentTarget = null;
      if (!wasDragging) return; // plain click — leave selection to the card handler
      if (target) cbs.onDrop(target);
      else cbs.onCancel();
    },
    onKeyDown(key) {
      if (key !== 'Escape') return;
      if (phase === 'candidate' || phase === 'dragging') {
        phase = 'cancelled';
        candidate = null;
        currentTarget = null;
        cbs.onCancel();
        phase = 'idle';
      }
    },
    reset() {
      phase = 'idle';
      candidate = null;
      currentTarget = null;
    },
  };
};
