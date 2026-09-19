import { describe, expect, it } from 'vitest';

import { node, sampleNodes } from '../domain/fixtures.test-util';
import type { Placed } from '../domain/layout';
import { ALLOWED, canReparent } from '../domain/rules';
import { NODE_TYPES, type NodeType } from '../domain/types';

import {
  CREATABLE_TYPES,
  attachTargets,
  boxAt,
  canAttach,
  childTypesFor,
  clientToFlow,
  dropTargetAt,
  insideRect,
  legalParents,
  titleMax,
  validateTitle,
} from './edit-rules';

const box = (x: number, y: number, w = 200, h = 90): Placed => ({ x, y, w, h }) as Placed;

describe('edit rules', () => {
  describe('+ menu options', () => {
    const cases: [NodeType, NodeType[]][] = [
      ['product', ['outcome']],
      ['outcome', ['opportunity']],
      ['opportunity', ['opportunity', 'solution', 'evidence']],
      ['solution', ['assumption']],
      ['assumption', ['evidence']],
      ['evidence', []],
    ];
    it.each(cases)('%s offers exactly %j', (type, expected) => {
      expect(childTypesFor(type)).toEqual(expected);
    });

    it('evidence may only go under an opportunity or an assumption', () => {
      expect(NODE_TYPES.filter(parent => canAttach('evidence', parent))).toEqual(['opportunity', 'assumption']);
    });

    it('returns a copy (callers cannot mutate ALLOWED)', () => {
      childTypesFor('opportunity').push('product');
      expect(ALLOWED.opportunity).toEqual(['opportunity', 'solution', 'evidence']);
    });

    it('the palette offers every type but products', () => {
      expect(CREATABLE_TYPES).toEqual(['outcome', 'opportunity', 'solution', 'assumption', 'evidence']);
    });
  });

  describe('hit testing', () => {
    const placed = { a: box(100, 0), b: box(400, 0), c: box(100, 200) };

    it('finds the box under a flow point (centre x, top y), edges exclusive', () => {
      expect(boxAt({ x: 100, y: 45 }, placed)).toBe('a');
      expect(boxAt({ x: 1, y: 1 }, placed)).toBe('a');
      expect(boxAt({ x: 0, y: 45 }, placed)).toBeNull(); // left edge
      expect(boxAt({ x: 100, y: 90 }, placed)).toBeNull(); // bottom edge
      expect(boxAt({ x: 450, y: 10 }, placed)).toBe('b');
      expect(boxAt({ x: 100, y: 150 }, placed)).toBeNull(); // the gap between levels
      expect(boxAt({ x: 100, y: 250 }, placed)).toBe('c');
    });

    it('skips the dragged node, which always sits under the pointer', () => {
      expect(boxAt({ x: 100, y: 45 }, placed, 'a')).toBeNull();
      expect(boxAt({ x: 100, y: 45 }, { ...placed, z: box(100, 0) }, 'z')).toBe('a');
    });

    it('drop target = the legal box under the point, else null', () => {
      const legal = new Set(['b']);
      expect(dropTargetAt({ x: 400, y: 30 }, placed, legal)).toBe('b');
      expect(dropTargetAt({ x: 100, y: 30 }, placed, legal)).toBeNull();
      expect(dropTargetAt({ x: 900, y: 30 }, placed, legal)).toBeNull();
    });

    it('converts client points to flow points through the viewport', () => {
      const rect = { left: 200, top: 100 };
      expect(clientToFlow({ x: 200, y: 100 }, rect, { x: 0, y: 0, zoom: 1 })).toEqual({ x: 0, y: 0 });
      expect(clientToFlow({ x: 400, y: 300 }, rect, { x: 50, y: 20, zoom: 0.5 })).toEqual({ x: 300, y: 360 });
    });

    it('insideRect excludes the edges', () => {
      const r = { left: 0, top: 0, right: 10, bottom: 10 };
      expect(insideRect({ x: 5, y: 5 }, r)).toBe(true);
      expect(insideRect({ x: 0, y: 5 }, r)).toBe(false);
      expect(insideRect({ x: 5, y: 10 }, r)).toBe(false);
    });
  });

  describe('re-parent validation', () => {
    const nodes = sampleNodes();

    it('never the node itself, its current parent or a descendant; only a permitting type', () => {
      // opportunity-2 sits under opportunity-1
      const legal = legalParents('opportunity-2', nodes);
      expect([...legal].sort()).toEqual(['outcome-1']);
      // opportunity-1 (under outcome-1): not itself, not outcome-1, not its child opportunity-2
      expect([...legalParents('opportunity-1', nodes)]).toEqual([]);
      // solution-1 can go under any opportunity except its parent
      expect([...legalParents('solution-1', nodes)]).toEqual(['opportunity-2']);
      // evidence-1 (under opportunity-1): the other opportunity and every assumption
      expect([...legalParents('evidence-1', nodes)].sort()).toEqual(['assumption-1', 'assumption-2', 'assumption-3', 'opportunity-2']);
    });

    it('products have no legal parent', () => {
      expect(legalParents('product-1', nodes).size).toBe(0);
    });

    it('agrees with rules.canReparent for every pair', () => {
      for (const drag of nodes) {
        const legal = legalParents(drag.id, nodes);
        for (const target of nodes) expect(legal.has(target.id), `${drag.id} → ${target.id}`).toBe(canReparent(drag.id, target.id, nodes));
      }
    });

    it('unknown keys give nothing', () => {
      expect(legalParents('opportunity-99', nodes).size).toBe(0);
    });

    it('never loops on cyclic data', () => {
      const cyclic = [node('opportunity-1', 'opportunity-2'), node('opportunity-2', 'opportunity-1'), node('outcome-1', 'product-1')];
      expect([...legalParents('opportunity-1', cyclic)]).toEqual(['outcome-1']);
    });

    it('attach targets for a palette type are the nodes that permit it', () => {
      expect([...attachTargets('solution', nodes)].sort()).toEqual(['opportunity-1', 'opportunity-2']);
      expect([...attachTargets('outcome', nodes)].sort()).toEqual(['product-1', 'product-2']);
      expect([...attachTargets('evidence', nodes)].sort()).toEqual([
        'assumption-1',
        'assumption-2',
        'assumption-3',
        'opportunity-1',
        'opportunity-2',
      ]);
    });
  });

  describe('titles', () => {
    it('trims and accepts 2..max characters', () => {
      expect(validateTitle('opportunity', '  Faster onboarding ')).toEqual({ ok: true, title: 'Faster onboarding' });
      expect(validateTitle('outcome', 'ab')).toEqual({ ok: true, title: 'ab' });
    });

    it('refuses empty, too short and too long titles with a message', () => {
      expect(validateTitle('solution', '   ')).toEqual({ ok: false, message: 'A title is required.' });
      expect(validateTitle('solution', 'a')).toEqual({ ok: false, message: 'Titles need at least 2 characters.' });
      const long = validateTitle('solution', 'x'.repeat(201));
      expect(long.ok).toBe(false);
      expect(!long.ok && long.message).toContain('at most 200 characters');
    });

    it('uses the server limits per type', () => {
      expect(titleMax('product')).toBe(100);
      expect(titleMax('outcome')).toBe(200);
      expect(titleMax('opportunity')).toBe(200);
      expect(titleMax('solution')).toBe(200);
      expect(titleMax('assumption')).toBe(500);
      expect(titleMax('evidence')).toBe(500);
      expect(validateTitle('evidence', 'x'.repeat(500)).ok).toBe(true);
      expect(validateTitle('product', 'x'.repeat(101)).ok).toBe(false);
    });
  });
});
