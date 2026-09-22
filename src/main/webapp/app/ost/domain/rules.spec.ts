import { describe, expect, it } from 'vitest';

import { node, sampleNodes } from './fixtures.test-util';
import {
  ALLOWED,
  BAD_STATUS,
  GOOD_STATUS,
  STATUS,
  TYPE_BOX,
  canReparent,
  defaultLinks,
  evidenceStrength,
  isDescendant,
  priorityColor,
  priorityLabel,
  valueLabel,
} from './rules';
import { NODE_TYPES } from './types';

describe('OST rules', () => {
  it('permits exactly the parent -> child matrix the server enforces', () => {
    expect(ALLOWED).toEqual({
      product: ['outcome'],
      outcome: ['opportunity'],
      opportunity: ['opportunity', 'solution', 'evidence'],
      solution: ['assumption'],
      assumption: ['evidence'],
      evidence: [],
    });
  });

  it('has box geometry for every type from the handoff', () => {
    expect(NODE_TYPES.map(t => [TYPE_BOX[t].w, TYPE_BOX[t].h])).toEqual([
      [244, 60],
      [238, 92],
      [218, 100],
      [206, 88],
      [198, 84],
      [206, 92],
    ]);
  });

  it('gives statuses only to opportunities, solutions and assumptions, split into three voices', () => {
    expect(STATUS.product).toEqual([]);
    expect(STATUS.outcome).toEqual([]);
    expect(STATUS.evidence).toEqual([]);
    expect(STATUS.opportunity[0]).toBe('unexplored');
    expect(STATUS.solution[0]).toBe('candidate');
    expect(STATUS.assumption[0]).toBe('untested');
    const all = [...STATUS.opportunity, ...STATUS.solution, ...STATUS.assumption];
    expect(GOOD_STATUS.every(s => all.includes(s))).toBe(true);
    expect(BAD_STATUS.every(s => all.includes(s))).toBe(true);
  });

  it('lists the default link slot names per type — UI affordance only, no URLs baked in', () => {
    // LINK-001: defaults are add-buttons in the panel, never persisted. defaultLinks
    // returns only names — the Atlassian base URL used to build placeholder URLs is gone.
    expect(defaultLinks('product').map(l => l.name)).toEqual(['Product space']);
    expect(defaultLinks('opportunity').map(l => l.name)).toEqual(['Confluence', 'Jira Initiative', 'Jira Epic']);
    expect(defaultLinks('solution').map(l => l.name)).toEqual(['Confluence', 'Jira Initiative', 'Jira Epic']);
    expect(defaultLinks('outcome').map(l => l.name)).toEqual(['Confluence']);
    expect(defaultLinks('assumption').map(l => l.name)).toEqual(['Confluence']);
    expect(defaultLinks('evidence').map(l => l.name)).toEqual(['Confluence', 'Jira Ticket']);
    expect(defaultLinks('opportunity')[0]).not.toHaveProperty('url');
  });

  it('derives evidence strength: supported 100, refuted 0, otherwise confidence, averaged', () => {
    // supported(30 -> 100), refuted(90 -> 0), testing(60) => 160 / 3 = 53
    expect(evidenceStrength('solution-1', sampleNodes())).toEqual({ tests: 3, supported: 1, refuted: 1, score: 53 });
    expect(evidenceStrength('solution-9', sampleNodes())).toEqual({ tests: 0, supported: 0, refuted: 0, score: null });
  });

  it('maps priority onto the cold -> warm spectrum and labels', () => {
    expect(priorityColor(0)).toBe('oklch(0.74 0.075 250.0)');
    expect(priorityColor(100)).toBe('oklch(0.74 0.160 25.0)');
    expect(priorityColor(250)).toBe(priorityColor(100));
    expect([90, 70, 50, 25, 5].map(priorityLabel)).toEqual(['Critical', 'High', 'Medium', 'Low', 'Lowest']);
    expect(valueLabel(1)).toBe('Marginal');
    expect(valueLabel(5)).toBe('Outsized');
    expect(valueLabel(9)).toBe('');
  });

  it('detects descendants', () => {
    const nodes = sampleNodes();
    expect(isDescendant('evidence-2', 'opportunity-1', nodes)).toBe(true);
    expect(isDescendant('opportunity-1', 'evidence-2', nodes)).toBe(false);
  });

  it('guards re-parenting: no self, no descendant, no current parent, type must be permitted', () => {
    const nodes = sampleNodes();
    expect(canReparent('solution-1', 'solution-1', nodes)).toBe(false);
    expect(canReparent('opportunity-1', 'opportunity-2', nodes)).toBe(false); // own descendant
    expect(canReparent('solution-1', 'opportunity-1', nodes)).toBe(false); // already there
    expect(canReparent('solution-1', 'opportunity-2', nodes)).toBe(true);
    expect(canReparent('evidence-1', 'assumption-1', nodes)).toBe(true);
    expect(canReparent('evidence-1', 'solution-1', nodes)).toBe(false); // evidence not under solutions
    expect(canReparent('outcome-1', 'product-2', nodes)).toBe(true);
    expect(canReparent('opportunity-2', 'product-2', nodes)).toBe(false);
    expect(canReparent('missing', 'product-2', [...nodes, node('product-9', null)])).toBe(false);
  });
});
