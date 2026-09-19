/*
 * Pure rules behind canvas editing (step 9): which child types a node offers, where a dragged node
 * or palette type may be dropped, pointer hit testing against the derived layout, and title
 * validation for inline rename. No Vue, no DOM — unit tested on their own (edit-rules.spec.ts).
 *
 * Hit testing follows the prototype (Ombuto OST.dc.html nodeDown / paletteDown): the POINTER, in
 * flow coordinates, must be inside a laid-out box [x - w/2, x + w/2] × [y, y + h], where h is the
 * larger of the layout height and the node's rendered height (a clamped title renders taller).
 */
import type { Placed } from '../domain/layout';
import { ALLOWED } from '../domain/rules';
import type { NodeType, OstNode } from '../domain/types';

import type { Point, Viewport } from './useViewport';

/** Client point → the legal node a new `type` would attach under there, or null. */
export type ResolveTarget = (...args: [type: NodeType, clientX: number, clientY: number]) => string | null;

/** Types the palette offers, in prototype order (products are created from the Teams page). */
export const CREATABLE_TYPES: NodeType[] = ['outcome', 'opportunity', 'solution', 'assumption', 'evidence'];

/** The + menu of a node: exactly its permitted child types. */
export const childTypesFor = (parentType: NodeType): NodeType[] => [...ALLOWED[parentType]];

/** May a new node of `type` be attached under a node of `parentType`? */
export const canAttach = (type: NodeType, parentType: NodeType): boolean => ALLOWED[parentType].includes(type);

/** Screen (client) point → flow point, given the canvas element's rect and the viewport. */
export function clientToFlow(client: Point, rect: { left: number; top: number }, viewport: Viewport): Point {
  return { x: (client.x - rect.left - viewport.x) / viewport.zoom, y: (client.y - rect.top - viewport.y) / viewport.zoom };
}

/** Is the client point inside the rect (exclusive edges, as in the prototype)? */
export const insideRect = (client: Point, r: { left: number; top: number; right: number; bottom: number }): boolean =>
  client.x > r.left && client.x < r.right && client.y > r.top && client.y < r.bottom;

/** Rendered height of a node in flow units, when known (measured by Vue Flow). */
export type HeightOf = (key: string) => number | undefined;

/**
 * Key of the laid-out box under the flow point, skipping `exclude` (the node being dragged, which
 * always sits under the pointer). A box is max(layout height, rendered height) tall. The tidy
 * layout never overlaps boxes; if it ever did, the last in paint order wins.
 */
export function boxAt(point: Point, placed: Record<string, Placed>, exclude?: string | null, heightOf?: HeightOf): string | null {
  let hit: string | null = null;
  for (const [key, p] of Object.entries(placed)) {
    if (key === exclude) continue;
    if (point.x <= p.x - p.w / 2 || point.x >= p.x + p.w / 2 || point.y <= p.y) continue;
    const h = Math.max(p.h, heightOf?.(key) ?? 0);
    if (point.y < p.y + h) hit = key;
  }
  return hit;
}

/**
 * Every node `dragKey` may be re-parented under, in one pass (same rule as rules.canReparent):
 * never itself, its current parent or one of its descendants, and only a parent type that permits
 * the dragged type. Products have no legal parent.
 */
export function legalParents(dragKey: string, nodes: OstNode[]): Set<string> {
  const legal = new Set<string>();
  const drag = nodes.find(n => n.id === dragKey);
  if (!drag) return legal;
  const kids = new Map<string, string[]>();
  for (const n of nodes) {
    if (!n.parent) continue;
    const bucket = kids.get(n.parent);
    if (bucket) bucket.push(n.id);
    else kids.set(n.parent, [n.id]);
  }
  const subtree = new Set<string>([dragKey]);
  const stack = [dragKey];
  while (stack.length) {
    for (const child of kids.get(stack.pop()!) ?? []) {
      if (!subtree.has(child)) {
        subtree.add(child);
        stack.push(child);
      }
    }
  }
  for (const n of nodes) {
    if (subtree.has(n.id) || n.id === drag.parent) continue;
    if (canAttach(drag.type, n.type)) legal.add(n.id);
  }
  return legal;
}

/** Every node a new `type` may be attached under (palette drag / armed tool highlighting). */
export function attachTargets(type: NodeType, nodes: OstNode[]): Set<string> {
  return new Set(nodes.filter(n => canAttach(type, n.type)).map(n => n.id));
}

/** The legal target under the flow point, or null (the drop is rejected). */
export function dropTargetAt(
  point: Point,
  placed: Record<string, Placed>,
  legal: ReadonlySet<string>,
  exclude?: string | null,
  heightOf?: HeightOf,
): string | null {
  const key = boxAt(point, placed, exclude, heightOf);
  return key && legal.has(key) ? key : null;
}

// ---- inline rename ------------------------------------------------------------------------------

export const TITLE_MIN = 2;

/** Server limits: products 100 (name), assumptions + evidence 500, everything else 200. */
export function titleMax(type: NodeType): number {
  if (type === 'product') return 100;
  return type === 'assumption' || type === 'evidence' ? 500 : 200;
}

export type TitleCheck = { ok: true; title: string } | { ok: false; message: string };

/** Trims and checks a title against the server's limits; the message is shown inline. */
export function validateTitle(type: NodeType, raw: string): TitleCheck {
  const title = raw.trim();
  if (!title) return { ok: false, message: 'A title is required.' };
  if (title.length < TITLE_MIN) return { ok: false, message: `Titles need at least ${TITLE_MIN} characters.` };
  const max = titleMax(type);
  if (title.length > max) return { ok: false, message: `Titles can be at most ${max} characters (${title.length} now).` };
  return { ok: true, title };
}
