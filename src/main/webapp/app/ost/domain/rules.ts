/*
 * OST domain rules — ported from the design handoff:
 *   docs/References/Vue Flow Opportunity Solution Tree/design_handoff_ombuto_ost/vue-reference/src/domain/rules.ts
 *
 * Ported as-is except for these deliberate edits (see the implementation plan §4):
 * - ALLOWED: Evidence only under Opportunity | Assumption (the server enforces the same matrix):
 *   solution: ['assumption'] (was ['assumption', 'evidence']), assumption: ['evidence'] (was []).
 * - PALETTE_HINT added: the evidence palette hint explains where evidence may be attached.
 * - defaultLinks (LINK-001): returns slot NAMES only, no URLs. The panel renders each slot as an
 *   add-button that opens the add-link form prefilled with the slot's name. Nothing is persisted
 *   at node creation; a NodeLink row means a human attached it.
 * - Formatting follows the repo's Prettier config.
 */
import type { NodeType, OstNode } from './types';

/** Node box geometry used by the tidy layout. Keep in sync with OstNode.vue. */
export const TYPE_BOX: Record<NodeType, { label: string; w: number; h: number }> = {
  product: { label: 'Product', w: 244, h: 60 },
  outcome: { label: 'Outcome', w: 238, h: 92 },
  opportunity: { label: 'Opportunity', w: 218, h: 100 },
  solution: { label: 'Solution', w: 206, h: 88 },
  assumption: { label: 'Assumption', w: 198, h: 84 },
  evidence: { label: 'Evidence', w: 206, h: 92 },
};

/** The only legal parent -> child relationships. Enforce on create AND on re-parent. */
export const ALLOWED: Record<NodeType, NodeType[]> = {
  product: ['outcome'],
  outcome: ['opportunity'],
  opportunity: ['opportunity', 'solution', 'evidence'],
  solution: ['assumption'],
  assumption: ['evidence'],
  evidence: [],
};

/** Palette hints: where each type may be attached. */
export const PALETTE_HINT: Record<NodeType, string> = {
  product: 'Products are created from the Teams page.',
  outcome: 'Attach under a product.',
  opportunity: 'Attach under an outcome or another opportunity.',
  solution: 'Attach under an opportunity.',
  assumption: 'Attach under a solution.',
  evidence: 'Customer evidence goes under an opportunity; test results under an assumption.',
};

/** Status vocabularies. Outcomes and products deliberately have none. */
export const STATUS: Record<NodeType, string[]> = {
  product: [],
  outcome: [],
  opportunity: ['unexplored', 'exploring', 'validated', 'parked'],
  solution: ['candidate', 'exploring', 'devready', 'building', 'shipped', 'dropped'],
  assumption: ['untested', 'testing', 'supported', 'refuted'],
  evidence: [],
};

export const GOOD_STATUS = ['validated', 'supported', 'shipped'];
export const BAD_STATUS = ['refuted', 'dropped', 'parked'];

/**
 * Default link slots per node type — the panel renders each as an add-button that opens the
 * add-link form prefilled with the slot's name (LINK-001). No URLs: the previous placeholder
 * URLs (with a hardcoded Atlassian tenant) were never real links, and the server no longer
 * persists them. A NodeLink row now means a human attached it.
 */
export function defaultLinks(type: NodeType): { name: string }[] {
  switch (type) {
    case 'product':
      return [{ name: 'Product space' }];
    case 'outcome':
    case 'assumption':
      return [{ name: 'Confluence' }];
    case 'evidence':
      return [{ name: 'Confluence' }, { name: 'Jira Ticket' }];
    default:
      return [{ name: 'Confluence' }, { name: 'Jira Initiative' }, { name: 'Jira Epic' }];
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
export const priorityLabel = (p: number) => (p >= 85 ? 'Critical' : p >= 65 ? 'High' : p >= 40 ? 'Medium' : p >= 20 ? 'Low' : 'Lowest');
export const valueLabel = (v: number) => ['—', 'Marginal', 'Modest', 'Solid', 'Strong', 'Outsized'][v] ?? '';

export function isDescendant(id: string, ofId: string, nodes: OstNode[]): boolean {
  let n = nodes.find(x => x.id === id);
  while (n?.parent) {
    if (n.parent === ofId) return true;
    n = nodes.find(x => x.id === n!.parent);
  }
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
