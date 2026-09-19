import { describe, expect, it } from 'vitest';

import { layoutTree } from '../domain/layout';
import { evidenceStrength } from '../domain/rules';
import { node, sampleNodes } from '../domain/fixtures.test-util';

import {
  childCounts,
  edgeKind,
  evidenceBySolution,
  isDimmed,
  isDraggable,
  laidOutNodes,
  matchesQuery,
  minimapToFlow,
  minimapTransform,
  toFlowEdges,
  toFlowNodes,
} from './canvas-model';
import {
  FIT_FLOOR,
  FIT_MAX,
  REVEAL_MARGIN,
  WHEEL_IN,
  ZOOM_MAX,
  ZOOM_MIN,
  centreOn,
  clampZoom,
  fitViewport,
  revealDelta,
  stepZoom,
  wheelZoom,
  zoomAt,
} from './useViewport';

/** Flow point under a screen point for a viewport. */
const flowAt = (v: { x: number; y: number; zoom: number }, sx: number, sy: number) => ({ x: (sx - v.x) / v.zoom, y: (sy - v.y) / v.zoom });

describe('useViewport maths', () => {
  it('clamps zoom to 0.35–1.6', () => {
    expect(clampZoom(0.1)).toBe(ZOOM_MIN);
    expect(clampZoom(5)).toBe(ZOOM_MAX);
    expect(clampZoom(1)).toBe(1);
    expect([ZOOM_MIN, ZOOM_MAX]).toEqual([0.35, 1.6]);
  });

  it('wheel zooms ×1.08 per notch in and ≈÷1.08 out', () => {
    const v = { x: 0, y: 0, zoom: 1 };
    expect(wheelZoom(v, { x: 0, y: 0 }, -100).zoom).toBeCloseTo(WHEEL_IN);
    expect(wheelZoom(v, { x: 0, y: 0 }, 100).zoom).toBeCloseTo(0.926);
  });

  it('keeps the flow point under the cursor fixed (cursor anchoring)', () => {
    const v = { x: 120, y: 40, zoom: 0.82 };
    const cursor = { x: 640, y: 380 };
    const before = flowAt(v, cursor.x, cursor.y);
    let next = v;
    for (let i = 0; i < 5; i++) next = wheelZoom(next, cursor, -1);
    const after = flowAt(next, cursor.x, cursor.y);
    expect(next.zoom).toBeGreaterThan(v.zoom);
    expect(after.x).toBeCloseTo(before.x, 6);
    expect(after.y).toBeCloseTo(before.y, 6);
  });

  it('stops at the clamp and does not drift the anchor there', () => {
    let v = { x: 10, y: 20, zoom: 1.55 };
    for (let i = 0; i < 20; i++) v = wheelZoom(v, { x: 300, y: 200 }, -1);
    expect(v.zoom).toBe(ZOOM_MAX);
    const again = wheelZoom(v, { x: 300, y: 200 }, -1);
    expect(again).toEqual(v);
    let w = { x: 0, y: 0, zoom: 0.4 };
    for (let i = 0; i < 20; i++) w = wheelZoom(w, { x: 300, y: 200 }, 1);
    expect(w.zoom).toBe(ZOOM_MIN);
  });

  it("zoomAt uses the prototype formula t' = m − (m − t)·k", () => {
    expect(zoomAt({ x: 100, y: 50, zoom: 1 }, { x: 300, y: 250 }, 1.5)).toEqual({ zoom: 1.5, x: 300 - 200 * 1.5, y: 250 - 200 * 1.5 });
  });

  it('toolbar steps zoom about the canvas centre', () => {
    const size = { width: 1000, height: 600 };
    const v = { x: 37, y: -12, zoom: 0.8 };
    const next = stepZoom(v, size, 1);
    expect(next.zoom).toBeCloseTo(0.92);
    const c0 = flowAt(v, 500, 300);
    const c1 = flowAt(next, 500, 300);
    expect(c1.x).toBeCloseTo(c0.x, 6);
    expect(c1.y).toBeCloseTo(c0.y, 6);
    expect(stepZoom({ x: 0, y: 0, zoom: 0.36 }, size, -1).zoom).toBe(ZOOM_MIN);
  });

  describe('revealDelta (a node the opening panel would clip)', () => {
    const box = { x: 900, y: 100, w: 206, h: 80, depth: 3 };
    const size = { width: 800, height: 600 };

    it('does not move a node that is fully visible', () => {
      expect(revealDelta(box, { x: -400, y: 0, zoom: 1 }, size)).toEqual({ x: 0, y: 0 });
    });

    it('pans just far enough to leave the margin at the edge it crossed, respecting the zoom', () => {
      // At zoom 1 the node spans 797..1003 on screen: 1003 - (800 - 24) = 227 to the left.
      expect(revealDelta(box, { x: 0, y: 0, zoom: 1 }, size)).toEqual({ x: 800 - REVEAL_MARGIN - 1003, y: 0 });
      // At zoom 0.5 it spans 398.5..501.5 + x; with x = 350 its right edge is 851.5.
      const d = revealDelta(box, { x: 350, y: 0, zoom: 0.5 }, size);
      expect(d.x).toBeCloseTo(800 - REVEAL_MARGIN - 851.5, 6);
      // Above the top and left of the left edge.
      expect(revealDelta(box, { x: -850, y: -120, zoom: 1 }, size)).toEqual({
        x: REVEAL_MARGIN - (797 - 850),
        y: REVEAL_MARGIN - (100 - 120),
      });
      // Below the bottom.
      expect(revealDelta(box, { x: -400, y: 500, zoom: 1 }, size).y).toBe(600 - REVEAL_MARGIN - 680);
    });

    it('a node wider than the canvas keeps its left edge in view', () => {
      expect(revealDelta(box, { x: 0, y: 0, zoom: 1 }, { width: 200, height: 600 }).x).toBe(REVEAL_MARGIN - 797);
    });
  });

  describe('fitViewport', () => {
    const size = { width: 1200, height: 800 };

    it('returns null when nothing is laid out or the canvas has no size', () => {
      expect(fitViewport({}, [], size)).toBeNull();
      const placed = layoutTree(sampleNodes(), { roots: ['product-1'], collapsed: {} });
      expect(fitViewport(placed, ['product-1'], { width: 0, height: 0 })).toBeNull();
    });

    it('centres a small branch horizontally, 28px from the top, never above 1.1', () => {
      const placed = layoutTree([node('product-1', null)], { roots: ['product-1'], collapsed: {} });
      const v = fitViewport(placed, ['product-1'], size)!;
      expect(v.zoom).toBe(FIT_MAX);
      expect(v.y).toBe(28);
      const p = placed['product-1'];
      // the product's centre sits in the middle of the canvas
      expect(p.x * v.zoom + v.x).toBeCloseTo(size.width / 2, 6);
    });

    it('never zooms below the 0.68 floor; a wide branch centres on its roots', () => {
      const wide = [node('product-1', null), ...Array.from({ length: 30 }, (_, i) => node(`outcome-${i + 10}`, 'product-1'))];
      const placed = layoutTree(wide, { roots: ['product-1'], collapsed: {} });
      const v = fitViewport(placed, ['product-1'], size)!;
      expect(v.zoom).toBe(FIT_FLOOR);
      expect(placed['product-1'].x * v.zoom + v.x).toBeCloseTo(size.width / 2, 6);
    });

    it('frames every laid-out box when it fits', () => {
      const placed = layoutTree(sampleNodes(), { roots: ['product-1'], collapsed: {} });
      const v = fitViewport(placed, ['product-1'], { width: 3000, height: 1400 })!;
      for (const p of Object.values(placed)) {
        expect((p.x - p.w / 2) * v.zoom + v.x).toBeGreaterThanOrEqual(0);
        expect((p.x + p.w / 2) * v.zoom + v.x).toBeLessThanOrEqual(3000);
        expect((p.y + p.h) * v.zoom + v.y).toBeLessThanOrEqual(1400);
      }
    });
  });

  it('centreOn puts the point in the middle of the canvas', () => {
    const v = centreOn({ x: 400, y: 300 }, { width: 1000, height: 600 }, 1.2);
    expect(400 * v.zoom + v.x).toBeCloseTo(500);
    expect(300 * v.zoom + v.y).toBeCloseTo(300);
    expect(centreOn({ x: 0, y: 0 }, { width: 10, height: 10 }, 9).zoom).toBe(ZOOM_MAX);
  });
});

describe('canvas-model', () => {
  const nodes = sampleNodes();

  it('positions come from the layout (centre x → top-left) and collapsed subtrees vanish', () => {
    const placed = layoutTree(nodes, { roots: ['product-1'], collapsed: { 'solution-1': true } });
    const visible = laidOutNodes(nodes, placed);
    expect(visible.map(n => n.id)).not.toContain('assumption-1');
    expect(visible.map(n => n.id)).not.toContain('product-2');
    const flow = toFlowNodes(visible, placed);
    const opp = flow.find(f => f.id === 'opportunity-1')!;
    expect(opp.position).toEqual({ x: placed['opportunity-1'].x - 109, y: placed['opportunity-1'].y });
    expect(flow.every(f => f.draggable === false && f.type === 'ost')).toBe(true);
  });

  it('only editors may drag, and never a product', () => {
    const placed = layoutTree(nodes, { roots: ['product-1'], collapsed: {} });
    const flow = toFlowNodes(laidOutNodes(nodes, placed), placed, n => isDraggable(n, true));
    expect(flow.find(f => f.id === 'product-1')!.draggable).toBe(false);
    expect(flow.filter(f => f.id !== 'product-1').every(f => f.draggable)).toBe(true);
    expect(isDraggable({ type: 'opportunity' }, false)).toBe(false);
    // Vue Flow's wrapper never takes focus: the node body does.
    expect(flow.every(f => f.focusable === false)).toBe(true);
  });

  it('builds one orthogonal edge per laid-out child with the child-type style', () => {
    const placed = layoutTree(nodes, { roots: ['product-1'], collapsed: {} });
    const edges = toFlowEdges(laidOutNodes(nodes, placed), placed);
    expect(edges).toHaveLength(Object.keys(placed).length - 1);
    const e = edges.find(x => x.target === 'solution-1')!;
    expect(e.source).toBe('opportunity-1');
    expect(e.data!.d).toMatch(/^M [\d.-]+ [\d.-]+ V [\d.-]+ H [\d.-]+ V [\d.-]+$/);
    expect(e.data!.kind).toBe('solution');
    expect(edgeKind('outcome')).toBe('spine');
    expect(edgeKind('opportunity')).toBe('spine');
    expect(edgeKind('assumption')).toBe('test');
    expect(edgeKind('evidence')).toBe('evidence');
  });

  it('counts direct children and derives evidence strength in one pass, matching the domain rule', () => {
    const counts = childCounts(nodes);
    expect(counts.get('opportunity-1')).toBe(3);
    expect(counts.get('solution-1')).toBe(3);
    expect(counts.get('evidence-1')).toBeUndefined();
    const strength = evidenceBySolution(nodes);
    const rule = evidenceStrength('solution-1', nodes);
    expect(strength.get('solution-1')).toEqual({ tests: rule.tests, score: rule.score });
    expect(strength.has('opportunity-1')).toBe(false);
  });

  it('search matches title + notes case-insensitively; empty query dims nothing', () => {
    const n = node('opportunity-9', 'outcome-1', { title: 'Scheduling pain', note: 'Mentioned by ACME' });
    expect(matchesQuery(n, 'sched')).toBe(true);
    expect(matchesQuery(n, 'acme')).toBe(true);
    expect(matchesQuery(n, 'zzz')).toBe(false);
    expect(matchesQuery(n, '  ')).toBe(false);
    expect(isDimmed(n, '', {})).toBe(false);
    expect(isDimmed(n, 'zzz', {})).toBe(true);
    expect(isDimmed(n, 'sched', {})).toBe(false);
    expect(isDimmed(n, '', { opportunity: true })).toBe(true);
  });

  it('minimap maps flow ↔ map coordinates consistently', () => {
    const placed = layoutTree(nodes, { roots: ['product-1', 'product-2'], collapsed: {} });
    const t = minimapTransform(placed)!;
    expect(minimapTransform({})).toBeNull();
    const p = placed['opportunity-1'];
    const mx = t.ox + p.x * t.s;
    const my = t.oy + p.y * t.s;
    const back = minimapToFlow(t, mx, my);
    expect(back.x).toBeCloseTo(p.x);
    expect(back.y).toBeCloseTo(p.y);
    // everything fits inside the 198×134 map
    for (const b of Object.values(placed)) {
      expect(t.ox + (b.x + b.w / 2) * t.s).toBeLessThanOrEqual(198);
      expect(t.oy + (b.y + b.h) * t.s).toBeLessThanOrEqual(134);
    }
  });
});
