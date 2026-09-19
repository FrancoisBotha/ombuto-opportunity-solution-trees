import { describe, expect, it } from 'vitest';

import { bigTreeDtos, node, sampleNodes } from './fixtures.test-util';
import { GAP_X, type Placed, ROW_PITCH, edgePath, layoutTree } from './layout';
import { fromDto } from './mapping';
import { TYPE_BOX } from './rules';
import type { OstNode } from './types';

describe('OST tidy layout', () => {
  const nodes = sampleNodes();
  const placed = layoutTree(nodes, { roots: ['product-1', 'product-2'], collapsed: {} });

  it('places every node reachable from the roots, one row per depth', () => {
    expect(Object.keys(placed).sort()).toEqual(nodes.map(n => n.id).sort());
    expect(placed['product-1'].y).toBe(0);
    expect(placed['outcome-1'].y).toBe(ROW_PITCH);
    expect(placed['evidence-2'].y).toBe(5 * ROW_PITCH);
    expect(placed['evidence-2'].depth).toBe(5);
  });

  it('uses the type box for width and height', () => {
    for (const n of nodes) {
      expect(placed[n.id].w).toBe(TYPE_BOX[n.type].w);
      expect(placed[n.id].h).toBe(TYPE_BOX[n.type].h);
    }
  });

  it('centres each parent over the span of its first and last child', () => {
    const kids = (id: string) => nodes.filter(n => n.parent === id).map(n => placed[n.id].x);
    for (const n of nodes) {
      const xs = kids(n.id);
      if (xs.length) expect(placed[n.id].x).toBeCloseTo((xs[0] + xs[xs.length - 1]) / 2);
    }
  });

  it('lays leaves left to right in array order without overlap', () => {
    const leaves = ['opportunity-2', 'assumption-1', 'assumption-2', 'evidence-2', 'evidence-1', 'product-2'];
    const xs = leaves.map(id => placed[id]);
    for (let i = 1; i < xs.length; i++) {
      const gap = xs[i].x - xs[i].w / 2 - (xs[i - 1].x + xs[i - 1].w / 2);
      expect(gap).toBeGreaterThanOrEqual(GAP_X);
    }
    // First leaf starts at the origin.
    expect(placed['opportunity-2'].x - placed['opportunity-2'].w / 2).toBe(0);
  });

  it('keeps nodes on one row from overlapping', () => {
    const byRow = new Map<number, { l: number; r: number }[]>();
    for (const p of Object.values(placed)) {
      const row = byRow.get(p.y) ?? [];
      row.push({ l: p.x - p.w / 2, r: p.x + p.w / 2 });
      byRow.set(p.y, row);
    }
    for (const row of byRow.values()) {
      row.sort((a, b) => a.l - b.l);
      for (let i = 1; i < row.length; i++) expect(row[i].l).toBeGreaterThanOrEqual(row[i - 1].r);
    }
  });

  it('leaves an 80px gutter between root branches', () => {
    const lastLeaf = placed['evidence-1'];
    const product2 = placed['product-2'];
    expect(product2.x - product2.w / 2).toBe(lastLeaf.x + lastLeaf.w / 2 + GAP_X + 80);
  });

  it('hides the children of collapsed nodes and treats them as leaves', () => {
    const collapsed = layoutTree(nodes, { roots: ['product-1'], collapsed: { 'solution-1': true } });
    expect(collapsed['solution-1']).toBeDefined();
    expect(collapsed['assumption-1']).toBeUndefined();
    expect(collapsed['evidence-2']).toBeUndefined();
    expect(collapsed['product-2']).toBeUndefined();
  });

  it('ignores unknown roots and lays out only the scoped product', () => {
    const scoped = layoutTree([...nodes, node('outcome-9', 'product-2')], { roots: ['product-2', 'product-404'], collapsed: {} });
    expect(Object.keys(scoped).sort()).toEqual(['outcome-9', 'product-2']);
    expect(scoped['product-2'].x).toBe(scoped['outcome-9'].x);
  });

  it('draws an orthogonal three-segment edge from parent bottom-centre to child top-centre', () => {
    const parent = { x: 100, y: 0, w: 200, h: 60, depth: 0 };
    const child = { x: 40, y: 156, w: 200, h: 92, depth: 1 };
    expect(edgePath(parent, child)).toBe('M 100 60 V 108 H 40 V 156');
  });

  describe('large trees', () => {
    /** The handoff's original O(n^2) algorithm, kept as the reference the memoised port must match. */
    function referenceLayout(list: OstNode[], opts: { roots: string[]; collapsed: Record<string, boolean> }) {
      const pos: Record<string, Placed> = {};
      const childrenOf = (id: string) => list.filter(n => n.parent === id);
      let cursor = 0;
      const walk = (n: OstNode, depth: number): number => {
        const box = TYPE_BOX[n.type];
        const kids = opts.collapsed[n.id] ? [] : childrenOf(n.id);
        let cx: number;
        if (!kids.length) {
          cx = cursor + box.w / 2;
          cursor += box.w + GAP_X;
        } else {
          const spans = kids.map(k => walk(k, depth + 1));
          cx = (spans[0] + spans[spans.length - 1]) / 2;
        }
        pos[n.id] = { x: cx, y: depth * ROW_PITCH, w: box.w, h: box.h, depth };
        return cx;
      };
      for (const rootId of opts.roots) {
        const root = list.find(n => n.id === rootId);
        if (root) {
          walk(root, 0);
          cursor += 80;
        }
      }
      return pos;
    }

    const big = bigTreeDtos(3, 25).map(fromDto);
    const roots = big.filter(n => n.type === 'product').map(n => n.id);

    it('matches the reference layout exactly', () => {
      expect(big.length).toBeGreaterThan(300);
      const collapsed = { [big.find(n => n.type === 'solution')!.id]: true };
      expect(layoutTree(big, { roots, collapsed: {} })).toEqual(referenceLayout(big, { roots, collapsed: {} }));
      expect(layoutTree(big, { roots, collapsed })).toEqual(referenceLayout(big, { roots, collapsed }));
    });

    it('lays out 300+ nodes quickly', () => {
      const started = performance.now();
      for (let i = 0; i < 20; i++) layoutTree(big, { roots, collapsed: {} });
      expect((performance.now() - started) / 20).toBeLessThan(25);
    });
  });
});
