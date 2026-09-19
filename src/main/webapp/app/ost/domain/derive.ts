/*
 * Derived values — never stored. Pure functions over the flat node list so pages, the panel
 * and the store can share them (and they can be unit tested without a component).
 */
import { BAD_STATUS, GOOD_STATUS, evidenceStrength as ruleEvidenceStrength } from './rules';
import type { NodeType, OstNode } from './types';

/** Sibling group order used by the server's tree read (and so by the tidy layout). */
export const CHILD_TYPE_ORDER: NodeType[] = ['outcome', 'opportunity', 'solution', 'assumption', 'evidence'];

export type PanelTab = 'detail' | 'links' | 'chat' | 'questions' | 'history';

export type StatusTone = 'good' | 'flight' | 'bad';

/** The three status voices: settled-good, in-flight, settled-negative (see .ost-badge--*). */
export const statusTone = (status: string): StatusTone =>
  GOOD_STATUS.includes(status) ? 'good' : BAD_STATUS.includes(status) ? 'bad' : 'flight';

export const byKey = (nodes: OstNode[]): Map<string, OstNode> => new Map(nodes.map(n => [n.id, n]));

export function childrenOf(key: string, nodes: OstNode[]): OstNode[] {
  return nodes.filter(n => n.parent === key);
}

/** Ancestors from the root (product) down to the direct parent. Excludes the node itself. */
export function breadcrumb(key: string, nodes: OstNode[]): OstNode[] {
  const index = byKey(nodes);
  const trail: OstNode[] = [];
  const seen = new Set<string>([key]);
  let current = index.get(key);
  while (current?.parent) {
    const parent = index.get(current.parent);
    if (!parent || seen.has(parent.id)) break;
    seen.add(parent.id);
    trail.unshift(parent);
    current = parent;
  }
  return trail;
}

/** Keys of every node below `key` (not including it). */
export function descendantIds(key: string, nodes: OstNode[]): string[] {
  const kids = new Map<string, string[]>();
  for (const n of nodes) {
    if (!n.parent) continue;
    const list = kids.get(n.parent);
    if (list) list.push(n.id);
    else kids.set(n.parent, [n.id]);
  }
  const out: string[] = [];
  const stack = [...(kids.get(key) ?? [])];
  const seen = new Set<string>([key]);
  while (stack.length) {
    const id = stack.pop()!;
    if (seen.has(id)) continue;
    seen.add(id);
    out.push(id);
    stack.push(...(kids.get(id) ?? []));
  }
  return out;
}

export const descendantCount = (key: string, nodes: OstNode[]): number => descendantIds(key, nodes).length;

/** Nearest ancestor of the given type (not the node itself). */
export function ancestorOfType(key: string, type: NodeType, nodes: OstNode[]): OstNode | null {
  return (
    breadcrumb(key, nodes)
      .reverse()
      .find(n => n.type === type) ?? null
  );
}

/** The product a node belongs to (the node itself when it is a product). */
export function productOf(key: string, nodes: OstNode[]): OstNode | null {
  const node = nodes.find(n => n.id === key);
  if (!node) return null;
  if (node.type === 'product') return node;
  return breadcrumb(key, nodes)[0] ?? null;
}

/** Evidence strength for a solution (see rules.ts); null score when it has no assumptions. */
export function evidenceStrength(solutionKey: string, nodes: OstNode[]) {
  return ruleEvidenceStrength(solutionKey, nodes);
}

export function countByType(nodes: OstNode[]): Record<NodeType, number> {
  const counts: Record<NodeType, number> = { product: 0, outcome: 0, opportunity: 0, solution: 0, assumption: 0, evidence: 0 };
  for (const n of nodes) counts[n.type]++;
  return counts;
}

const compareSiblings = (a: OstNode, b: OstNode) =>
  CHILD_TYPE_ORDER.indexOf(a.type) - CHILD_TYPE_ORDER.indexOf(b.type) || a.sortOrder - b.sortOrder || a.dbId - b.dbId;

/**
 * Re-orders a flat list into the server's pre-order: products by sortOrder, then each node's
 * children grouped outcome -> opportunity -> solution -> assumption -> evidence, each by
 * sortOrder, id. Orphans (parent missing) are kept at the end so nothing is silently lost.
 */
export function orderTree(nodes: OstNode[]): OstNode[] {
  const kids = new Map<string, OstNode[]>();
  const roots: OstNode[] = [];
  const keys = new Set(nodes.map(n => n.id));
  for (const n of nodes) {
    if (!n.parent) roots.push(n);
    else if (keys.has(n.parent)) {
      const list = kids.get(n.parent);
      if (list) list.push(n);
      else kids.set(n.parent, [n]);
    }
  }
  roots.sort((a, b) => a.sortOrder - b.sortOrder || a.dbId - b.dbId);
  const out: OstNode[] = [];
  const seen = new Set<string>();
  const walk = (n: OstNode) => {
    if (seen.has(n.id)) return;
    seen.add(n.id);
    out.push(n);
    for (const k of (kids.get(n.id) ?? []).sort(compareSiblings)) walk(k);
  };
  roots.forEach(walk);
  for (const n of nodes) if (!seen.has(n.id)) out.push(n);
  return out;
}

/** Which detail-panel tabs a node type gets (Product: Detail + Links; Open Qs: opportunities only). */
export function panelTabsFor(type: NodeType): PanelTab[] {
  if (type === 'product') return ['detail', 'links'];
  if (type === 'opportunity') return ['detail', 'links', 'chat', 'questions', 'history'];
  return ['detail', 'links', 'chat', 'history'];
}

/** Start of the current calendar month in UTC — the boundary the server's TeamTreeService uses. */
const utcMonthStart = (now: Date) => Date.UTC(now.getUTCFullYear(), now.getUTCMonth(), 1);

const time = (iso: string | null | undefined) => {
  const t = iso ? Date.parse(iso) : NaN;
  return Number.isNaN(t) ? null : t;
};

/**
 * Evidence nodes created in the current UTC calendar month (A5 counter). Derived from the nodes'
 * createdDate exactly as the server derives TeamTreeDTO.evidenceThisMonth (created on or after the
 * 1st of the month, 00:00 UTC), so it follows creates, deletes and moves made in this session.
 */
export function evidenceThisMonth(nodes: OstNode[], now = new Date()): number {
  const from = utcMonthStart(now);
  return nodes.filter(n => {
    if (n.type !== 'evidence') return false;
    const created = time(n.createdDate);
    return created !== null && created >= from;
  }).length;
}

export interface DashboardStats {
  opportunities: number;
  solutions: number;
  testsRunning: number;
  evidenceThisMonth: number;
}

/** The four dashboard counters, all derived from the current nodes. */
export function dashboardStats(nodes: OstNode[], opts: { now?: Date } = {}): DashboardStats {
  const counts = countByType(nodes);
  return {
    opportunities: counts.opportunity,
    solutions: counts.solution,
    testsRunning: nodes.filter(n => n.type === 'assumption' && n.status === 'testing').length,
    evidenceThisMonth: evidenceThisMonth(nodes, opts.now),
  };
}

export interface ProductCard {
  product: OstNode;
  /** The product's outcomes (node key + title), in tree order. */
  outcomes: { key: string; title: string }[];
  counts: { opportunities: number; solutions: number; assumptions: number; evidence: number };
  /** Newest edit in the branch: the server's last activity or a newer local change. */
  lastActivity: string | null;
  /** Who made that edit, when known (the product's lastActivity author). */
  lastEditedBy: string | null;
}

/**
 * One card per product: its outcomes, four mini counts and last edit. The last edit is the newest
 * of the server's product.lastActivity (history, tree read) and the local created/modified dates
 * of every node in the branch, so edits made in this session move it forward.
 */
export function productCards(nodes: OstNode[]): ProductCard[] {
  const ordered = orderTree(nodes);
  return nodes
    .filter(n => n.type === 'product')
    .sort((a, b) => a.sortOrder - b.sortOrder || a.dbId - b.dbId)
    .map(product => {
      const ids = new Set(descendantIds(product.id, nodes));
      const branch = nodes.filter(n => ids.has(n.id));
      const c = countByType(branch);
      const outcomes = ordered.filter(n => n.parent === product.id && n.type === 'outcome').map(n => ({ key: n.id, title: n.title }));
      let newestLocal: string | null = null;
      for (const n of [product, ...branch]) {
        for (const iso of [n.lastModifiedDate, n.createdDate, n.lastActivity?.at]) {
          const t = time(iso);
          if (t !== null && (newestLocal === null || t > time(newestLocal)!)) newestLocal = iso!;
        }
      }
      const server = product.lastActivity;
      const serverTime = time(server?.at);
      const localTime = time(newestLocal);
      const serverWins = serverTime !== null && (localTime === null || serverTime >= localTime);
      return {
        product,
        outcomes,
        counts: { opportunities: c.opportunity, solutions: c.solution, assumptions: c.assumption, evidence: c.evidence },
        lastActivity: serverWins ? server!.at : newestLocal,
        lastEditedBy: serverWins ? (server!.byLogin ?? null) : null,
      };
    });
}

export interface ExperimentRow {
  key: string;
  statement: string;
  solutionKey: string | null;
  solutionTitle: string | null;
  productKey: string | null;
  productTitle: string | null;
  owner: string;
  status: string;
  confidence: number;
}

/** Every assumption in the tree (optionally one product), in tree order. */
export function experimentRows(nodes: OstNode[], productKey: string | 'all' = 'all'): ExperimentRow[] {
  return orderTree(nodes)
    .filter(n => n.type === 'assumption')
    .map(a => {
      const trail = breadcrumb(a.id, nodes);
      const solution = [...trail].reverse().find(n => n.type === 'solution') ?? null;
      const product = trail[0]?.type === 'product' ? trail[0] : null;
      return {
        key: a.id,
        statement: a.title,
        solutionKey: solution?.id ?? null,
        solutionTitle: solution?.title ?? null,
        productKey: product?.id ?? null,
        productTitle: product?.title ?? null,
        owner: a.owner,
        status: a.status,
        confidence: a.conf,
      };
    })
    .filter(r => productKey === 'all' || r.productKey === productKey);
}

/** Tracker summary: how many assumptions sit in each status. */
export function experimentSummary(rows: ExperimentRow[]): Record<string, number> {
  const out: Record<string, number> = { untested: 0, testing: 0, supported: 0, refuted: 0 };
  for (const r of rows) out[r.status] = (out[r.status] ?? 0) + 1;
  return out;
}
