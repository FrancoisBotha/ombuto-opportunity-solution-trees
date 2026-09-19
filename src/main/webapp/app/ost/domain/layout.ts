/*
 * Tidy tree layout — ported AS-IS from the design handoff:
 *   docs/References/Vue Flow Opportunity Solution Tree/design_handoff_ombuto_ost/vue-reference/src/domain/layout.ts
 *
 * Deliberate edits: none to the algorithm. Formatting follows the repo's Prettier config.
 */
import { TYPE_BOX } from './rules';
import type { OstNode } from './types';

export const GAP_X = 26;
export const ROW_PITCH = 156;

export interface Placed {
  x: number;
  y: number;
  w: number;
  h: number;
  depth: number;
}

/**
 * Tidy top-down tree layout. Leaves are laid left to right; a parent is centred
 * over the span of its children. Node positions are NEVER user-set — recompute
 * on every structural change and animate the delta (~180ms ease).
 *
 * x is the node CENTRE; y is its TOP. Vue Flow wants top-left, so pass
 * { x: x - w / 2, y }.
 */
export function layoutTree(nodes: OstNode[], opts: { roots: string[]; collapsed: Record<string, boolean> }): Record<string, Placed> {
  const pos: Record<string, Placed> = {};
  const childrenOf = (id: string) => nodes.filter(n => n.parent === id);
  let cursor = 0;

  const walk = (node: OstNode, depth: number): number => {
    const box = TYPE_BOX[node.type];
    const kids = opts.collapsed[node.id] ? [] : childrenOf(node.id);
    let cx: number;
    if (!kids.length) {
      cx = cursor + box.w / 2;
      cursor += box.w + GAP_X;
    } else {
      const spans = kids.map(k => walk(k, depth + 1));
      cx = (spans[0] + spans[spans.length - 1]) / 2;
    }
    pos[node.id] = { x: cx, y: depth * ROW_PITCH, w: box.w, h: box.h, depth };
    return cx;
  };

  for (const rootId of opts.roots) {
    const root = nodes.find(n => n.id === rootId);
    if (root) {
      walk(root, 0);
      cursor += 80;
    }
  }
  return pos;
}

/** Orthogonal parent -> child edge path (three segments). */
export function edgePath(parent: Placed, child: Placed) {
  const y0 = parent.y + parent.h;
  const midY = y0 + (child.y - y0) / 2;
  return `M ${parent.x} ${y0} V ${midY} H ${child.x} V ${child.y}`;
}
