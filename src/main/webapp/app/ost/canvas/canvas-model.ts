/*
 * Pure mapping from the derived layout to Vue Flow elements. Positions ALWAYS come from the tidy
 * layout (layoutTree): users never place a node. Collapsed subtrees and out-of-scope products are
 * simply absent from `placed`, so they are absent here too.
 */
import type { Edge, Node } from '@vue-flow/core';

import { type Placed, edgePath } from '../domain/layout';
import type { NodeType, OstNode } from '../domain/types';

export type OstEdgeKind = 'spine' | 'solution' | 'test' | 'evidence';

export interface OstEdgeData {
  d: string;
  kind: OstEdgeKind;
}

export interface OstFlowNodeData {
  key: string;
}

/** The edge style is chosen by the CHILD's type (prototype: Ombuto OST.dc.html edges). */
export function edgeKind(childType: NodeType): OstEdgeKind {
  if (childType === 'assumption') return 'test';
  if (childType === 'evidence') return 'evidence';
  if (childType === 'solution') return 'solution';
  return 'spine';
}

/** Nodes that are laid out (in scope, not under a collapsed ancestor), in tree order. */
export const laidOutNodes = (nodes: OstNode[], placed: Record<string, Placed>): OstNode[] => nodes.filter(n => placed[n.id]);

/**
 * `draggable` decides per node whether it can be dragged onto another to re-parent (editors only,
 * never products). Dragging never places a node: the canvas resets positions after every drag.
 * Vue Flow's own wrapper stays unfocusable — the node body (OstNode) is the focus target.
 */
export function toFlowNodes(
  visible: OstNode[],
  placed: Record<string, Placed>,
  draggable: (n: OstNode) => boolean = () => false,
): Node<OstFlowNodeData>[] {
  return visible.map(n => {
    const p = placed[n.id];
    return {
      id: n.id,
      type: 'ost',
      position: flowPosition(p),
      data: { key: n.id },
      draggable: draggable(n),
      connectable: false,
      selectable: false,
      focusable: false,
    };
  });
}

/** layoutTree gives the CENTRE x and the TOP y; Vue Flow wants the top-left corner. */
export const flowPosition = (p: Placed) => ({ x: p.x - p.w / 2, y: p.y });

/** May this node be dragged to re-parent it? Editors only; products have no parent to change. */
export const isDraggable = (n: Pick<OstNode, 'type'>, canEdit: boolean): boolean => canEdit && n.type !== 'product';

export function toFlowEdges(visible: OstNode[], placed: Record<string, Placed>): Edge<OstEdgeData>[] {
  const edges: Edge<OstEdgeData>[] = [];
  for (const n of visible) {
    if (!n.parent || !placed[n.parent]) continue;
    edges.push({
      id: `e-${n.id}`,
      type: 'ost',
      source: n.parent,
      target: n.id,
      selectable: false,
      focusable: false,
      data: { d: edgePath(placed[n.parent], placed[n.id]), kind: edgeKind(n.type) },
    });
  }
  return edges;
}

/** Direct child count per node key, in one pass (the collapse chip's +n). */
export function childCounts(nodes: OstNode[]): Map<string, number> {
  const counts = new Map<string, number>();
  for (const n of nodes) if (n.parent) counts.set(n.parent, (counts.get(n.parent) ?? 0) + 1);
  return counts;
}

/**
 * Evidence strength of every tested solution in one pass (same rule as rules.evidenceStrength:
 * supported → 100, refuted → 0, otherwise the assumption's confidence; mean, rounded).
 * Solutions without assumptions are absent.
 */
export function evidenceBySolution(nodes: OstNode[]): Map<string, { tests: number; score: number }> {
  const acc = new Map<string, { tests: number; sum: number }>();
  for (const n of nodes) {
    if (n.type !== 'assumption' || !n.parent) continue;
    const a = acc.get(n.parent) ?? { tests: 0, sum: 0 };
    a.tests += 1;
    a.sum += n.status === 'supported' ? 100 : n.status === 'refuted' ? 0 : n.conf;
    acc.set(n.parent, a);
  }
  return new Map([...acc].map(([key, a]) => [key, { tests: a.tests, score: Math.round(a.sum / a.tests) }]));
}

/** Evidence score per tested solution as a primitive, so an unchanged score never re-renders a node. */
export function evidenceScores(nodes: OstNode[]): Map<string, number> {
  return new Map([...evidenceBySolution(nodes)].map(([key, e]) => [key, e.score]));
}

// ---- overview map (prototype: Ombuto OST.dc.html "mini") ------------------------------------------

export const MINIMAP_W = 198;
export const MINIMAP_H = 134;

/** Flow → minimap transform: minimap = o + flow * s. Null when nothing is laid out. */
export interface MinimapTransform {
  s: number;
  ox: number;
  oy: number;
}

export function minimapTransform(placed: Record<string, Placed>): MinimapTransform | null {
  const boxes = Object.values(placed);
  if (!boxes.length) return null;
  let x0 = Infinity;
  let x1 = -Infinity;
  let y1 = -Infinity;
  for (const p of boxes) {
    x0 = Math.min(x0, p.x - p.w / 2);
    x1 = Math.max(x1, p.x + p.w / 2);
    y1 = Math.max(y1, p.y + p.h);
  }
  // 9px side gutters, 20px top band for the "Overview" label, 10px bottom.
  const s = Math.min((MINIMAP_W - 18) / Math.max(1, x1 - x0), (MINIMAP_H - 30) / Math.max(1, y1));
  return { s, ox: 9 - x0 * s, oy: 20 };
}

/** Minimap point (relative to the map's top-left) → flow point. */
export const minimapToFlow = (t: MinimapTransform, x: number, y: number) => ({ x: (x - t.ox) / t.s, y: (y - t.oy) / t.s });

/** Search: title + notes, case-insensitive. An empty query matches nothing (and dims nothing). */
export function matchesQuery(node: Pick<OstNode, 'title' | 'note'>, query: string): boolean {
  const q = query.trim().toLowerCase();
  return !!q && `${node.title} ${node.note ?? ''}`.toLowerCase().includes(q);
}

/** Dimmed = a search is active and this is not a match, or its type is filtered out. */
export function isDimmed(node: OstNode, query: string, hiddenTypes: Partial<Record<NodeType, boolean>>): boolean {
  return (!!query.trim() && !matchesQuery(node, query)) || !!hiddenTypes[node.type];
}
