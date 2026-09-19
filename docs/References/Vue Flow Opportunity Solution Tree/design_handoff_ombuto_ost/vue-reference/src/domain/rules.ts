import type { NodeType, OstNode } from './types';

/** Node box geometry used by the tidy layout. Keep in sync with OstNode.vue. */
export const TYPE_BOX: Record<NodeType, { label: string; w: number; h: number }> = {
  product:     { label: 'Product',     w: 244, h: 60 },
  outcome:     { label: 'Outcome',     w: 238, h: 92 },
  opportunity: { label: 'Opportunity', w: 218, h: 100 },
  solution:    { label: 'Solution',    w: 206, h: 88 },
  assumption:  { label: 'Assumption',  w: 198, h: 84 },
  evidence:    { label: 'Evidence',    w: 206, h: 92 },
};

/** The only legal parent -> child relationships. Enforce on create AND on re-parent. */
export const ALLOWED: Record<NodeType, NodeType[]> = {
  product: ['outcome'],
  outcome: ['opportunity'],
  opportunity: ['opportunity', 'solution', 'evidence'],
  solution: ['assumption', 'evidence'],
  assumption: [],
  evidence: [],
};

/** Status vocabularies. Outcomes and products deliberately have none. */
export const STATUS: Record<NodeType, string[]> = {
  product: [],
  outcome: [],
  opportunity: ['unexplored', 'exploring', 'validated', 'parked'],
  solution: ['candidate', 'building', 'shipped', 'dropped'],
  assumption: ['untested', 'testing', 'supported', 'refuted'],
  evidence: [],
};

export const GOOD_STATUS = ['validated', 'supported', 'shipped'];
export const BAD_STATUS = ['refuted', 'dropped', 'parked'];

/** Default links seeded per type (restorable in the Links tab). */
export function defaultLinks(node: Pick<OstNode, 'id' | 'type'>): { name: string; url: string }[] {
  const base = 'https://ombuto.atlassian.net';
  switch (node.type) {
    case 'product':    return [{ name: 'Product space', url: `${base}/wiki/spaces/${node.id}` }];
    case 'outcome':
    case 'assumption': return [{ name: 'Confluence', url: `${base}/wiki/discovery/${node.id}` }];
    case 'evidence':   return [
      { name: 'Confluence', url: `${base}/wiki/discovery/${node.id}` },
      { name: 'Jira Ticket', url: `${base}/browse/DISC-000` },
    ];
    default:           return [
      { name: 'Confluence', url: `${base}/wiki/discovery/${node.id}` },
      { name: 'Jira Initiative', url: `${base}/browse/INIT-000` },
      { name: 'Jira Epic', url: `${base}/browse/DISC-000` },
    ];
  }
}

/**
 * Evidence strength for a solution — DERIVED, never editable.
 * supported -> 100, refuted -> 0, otherwise the assumption's own confidence.
 */
export function evidenceStrength(solutionId: string, nodes: OstNode[]) {
  const tests = nodes.filter(n => n.parent === solutionId && n.type === 'assumption');
  const supported = tests.filter(t => t.status === 'supported').length;
  const refuted = tests.filter(t => t.status === 'refuted').length;
  const score = tests.length
    ? Math.round(tests.reduce((a, t) => a + (t.status === 'supported' ? 100 : t.status === 'refuted' ? 0 : t.conf), 0) / tests.length)
    : null;
  return { tests: tests.length, supported, refuted, score };
}

/** Priority spectrum: fixed lightness, hue 250deg -> 25deg, chroma 0.075 -> 0.16. */
export function priorityColor(p: number, l = 0.74) {
  const t = Math.min(100, Math.max(0, p)) / 100;
  return `oklch(${l} ${(0.075 + t * 0.085).toFixed(3)} ${((250 + t * 135) % 360).toFixed(1)})`;
}
export const priorityLabel = (p: number) =>
  p >= 85 ? 'Critical' : p >= 65 ? 'High' : p >= 40 ? 'Medium' : p >= 20 ? 'Low' : 'Lowest';
export const valueLabel = (v: number) => ['—', 'Marginal', 'Modest', 'Solid', 'Strong', 'Outsized'][v] ?? '';

export function isDescendant(id: string, ofId: string, nodes: OstNode[]): boolean {
  let n = nodes.find(x => x.id === id);
  while (n?.parent) { if (n.parent === ofId) return true; n = nodes.find(x => x.id === n!.parent); }
  return false;
}

/** Full re-parent guard: no self, no descendant, type must be permitted. */
export function canReparent(dragId: string, targetId: string, nodes: OstNode[]) {
  if (dragId === targetId) return false;
  const drag = nodes.find(n => n.id === dragId);
  const target = nodes.find(n => n.id === targetId);
  if (!drag || !target || drag.parent === targetId) return false;
  if (isDescendant(targetId, dragId, nodes)) return false;
  return ALLOWED[target.type].includes(drag.type);
}
