import { describe, expect, it } from 'vitest';

import {
  ancestorOfType,
  breadcrumb,
  childrenOf,
  countByType,
  dashboardStats,
  descendantCount,
  descendantIds,
  evidenceStrength,
  evidenceThisMonth,
  experimentRows,
  experimentSummary,
  orderTree,
  panelTabsFor,
  productCards,
  productOf,
  statusTone,
} from './derive';
import { node, sampleNodes } from './fixtures.test-util';

describe('OST derived values', () => {
  const nodes = sampleNodes();

  it('builds the ancestor breadcrumb from the product down, excluding the node', () => {
    expect(breadcrumb('evidence-2', nodes).map(n => n.id)).toEqual([
      'product-1',
      'outcome-1',
      'opportunity-1',
      'solution-1',
      'assumption-3',
    ]);
    expect(breadcrumb('product-1', nodes)).toEqual([]);
    expect(breadcrumb('nope', nodes)).toEqual([]);
  });

  it('counts descendants and lists children', () => {
    expect(descendantCount('opportunity-1', nodes)).toBe(7);
    expect(descendantCount('product-2', nodes)).toBe(0);
    expect(descendantIds('solution-1', nodes).sort()).toEqual(['assumption-1', 'assumption-2', 'assumption-3', 'evidence-2']);
    expect(childrenOf('solution-1', nodes).map(n => n.id)).toEqual(['assumption-1', 'assumption-2', 'assumption-3']);
  });

  it('survives a parent cycle', () => {
    const cyclic = [node('opportunity-1', 'opportunity-2'), node('opportunity-2', 'opportunity-1')];
    expect(breadcrumb('opportunity-1', cyclic).map(n => n.id)).toEqual(['opportunity-2']);
    expect(descendantIds('opportunity-1', cyclic)).toEqual(['opportunity-2']);
  });

  it('finds the product and nearest ancestors of a type', () => {
    expect(productOf('evidence-2', nodes)?.id).toBe('product-1');
    expect(productOf('product-2', nodes)?.id).toBe('product-2');
    expect(ancestorOfType('opportunity-2', 'opportunity', nodes)?.id).toBe('opportunity-1');
    expect(ancestorOfType('outcome-1', 'solution', nodes)).toBeNull();
  });

  it('derives solution evidence strength through the rules', () => {
    expect(evidenceStrength('solution-1', nodes).score).toBe(53);
  });

  it('orders a shuffled list into server pre-order', () => {
    const shuffled = [...nodes].reverse();
    const ordered = orderTree(shuffled).map(n => n.id);
    expect(ordered[0]).toBe('product-1');
    expect(ordered.at(-1)).toBe('product-2');
    // opportunity-1's children grouped: opportunity, solution, evidence
    const kids = ordered.filter(id => nodes.find(n => n.id === id)?.parent === 'opportunity-1');
    expect(kids).toEqual(['opportunity-2', 'solution-1', 'evidence-1']);
    // each node comes after its parent
    for (const n of nodes) if (n.parent) expect(ordered.indexOf(n.id)).toBeGreaterThan(ordered.indexOf(n.parent));
  });

  it('computes the dashboard counters', () => {
    const now = new Date('2026-09-19T12:00:00Z');
    expect(countByType(nodes).assumption).toBe(3);
    expect(evidenceThisMonth(nodes, now)).toBe(1);
    expect(dashboardStats(nodes, { now })).toEqual({ opportunities: 2, solutions: 1, testsRunning: 1, evidenceThisMonth: 1 });
    expect(dashboardStats(nodes, { evidenceThisMonth: 9 }).evidenceThisMonth).toBe(9);
  });

  it('builds one card per product', () => {
    const cards = productCards(nodes);
    expect(cards.map(c => c.product.id)).toEqual(['product-1', 'product-2']);
    expect(cards[0].outcomeTitle).toBe('Title outcome-1');
    expect(cards[0].counts).toEqual({ opportunities: 2, solutions: 1, assumptions: 3, evidence: 2 });
    expect(cards[1].outcomeTitle).toBeNull();
  });

  it('lists experiment rows with their solution and product, and summarises statuses', () => {
    const rows = experimentRows(nodes);
    expect(rows.map(r => r.key)).toEqual(['assumption-1', 'assumption-2', 'assumption-3']);
    expect(rows[0]).toMatchObject({
      solutionKey: 'solution-1',
      productKey: 'product-1',
      owner: 'user',
      status: 'supported',
      confidence: 30,
    });
    expect(experimentRows(nodes, 'product-2')).toEqual([]);
    expect(experimentSummary(rows)).toEqual({ untested: 0, testing: 1, supported: 1, refuted: 1 });
  });

  it('adapts panel tabs by type and maps statuses to three tones', () => {
    expect(panelTabsFor('product')).toEqual(['detail', 'links']);
    expect(panelTabsFor('opportunity')).toContain('questions');
    expect(panelTabsFor('solution')).not.toContain('questions');
    expect(['validated', 'testing', 'parked'].map(statusTone)).toEqual(['good', 'flight', 'bad']);
  });
});
