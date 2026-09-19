import { describe, expect, it } from 'vitest';

import {
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
  });

  it('counts evidence from the 1st of the month, 00:00 UTC — the server boundary', () => {
    const now = new Date('2026-10-01T00:30:00Z');
    const list = [
      node('evidence-7', 'assumption-1', { createdDate: '2026-09-30T23:59:59Z' }),
      node('evidence-8', 'assumption-1', { createdDate: '2026-10-01T00:00:00Z' }),
      node('evidence-9', 'assumption-1', { createdDate: '2026-10-01T00:10:00.123456+00:00' }),
      node('evidence-10', 'assumption-1', { createdDate: null }),
      node('assumption-1', 'solution-1', { createdDate: '2026-10-01T00:10:00Z' }),
    ];
    expect(evidenceThisMonth(list, now)).toBe(2);
    expect(dashboardStats(list, { now }).evidenceThisMonth).toBe(2);
  });

  it('builds one card per product', () => {
    const cards = productCards(nodes);
    expect(cards.map(c => c.product.id)).toEqual(['product-1', 'product-2']);
    expect(cards[0].outcomes).toEqual([{ key: 'outcome-1', title: 'Title outcome-1' }]);
    expect(cards[0].counts).toEqual({ opportunities: 2, solutions: 1, assumptions: 3, evidence: 2 });
    expect(cards[1].outcomes).toEqual([]);
  });

  it('lists every outcome of a product in tree order', () => {
    const list = [
      ...nodes,
      node('outcome-5', 'product-1', { title: 'Second', sortOrder: 2 }),
      node('outcome-4', 'product-1', { title: 'First', sortOrder: -1 }),
    ];
    expect(productCards(list)[0].outcomes.map(o => o.title)).toEqual(['First', 'Title outcome-1', 'Second']);
  });

  it('takes the newest of the server last activity and local edits in the branch', () => {
    const server = { at: '2026-09-18T10:00:00Z', byLogin: 'admin' };
    const base = () => [
      node('product-1', null, { lastActivity: server, createdDate: '2026-01-01T00:00:00Z' }),
      node('outcome-1', 'product-1', { createdDate: '2026-09-01T00:00:00Z' }),
      node('opportunity-1', 'outcome-1', { createdDate: '2026-09-02T00:00:00Z', lastModifiedDate: '2026-09-10T00:00:00Z' }),
    ];
    const [card] = productCards(base());
    expect(card.lastActivity).toBe(server.at);
    expect(card.lastEditedBy).toBe('admin');

    const edited = base();
    edited[2].lastModifiedDate = '2026-09-19T08:00:00.5Z';
    const [newer] = productCards(edited);
    expect(newer.lastActivity).toBe('2026-09-19T08:00:00.5Z');
    expect(newer.lastEditedBy).toBeNull();

    const noServer = base();
    noServer[0].lastActivity = null;
    expect(productCards(noServer)[0]).toMatchObject({ lastActivity: '2026-09-10T00:00:00Z', lastEditedBy: null });
    expect(productCards([node('product-3', null)])[0]).toMatchObject({ lastActivity: null, lastEditedBy: null });
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
