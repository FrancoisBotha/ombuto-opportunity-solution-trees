/*
 * OST domain types.
 *
 * Adapted from the design handoff: docs/References/Vue Flow Opportunity Solution Tree/
 * design_handoff_ombuto_ost/vue-reference/src/domain/types.ts
 *
 * Deliberate edits against the reference:
 * - `id` is the server node key ("opportunity-12"); `dbId` carries the numeric entity id.
 * - comments and history are NOT embedded in the node — they are fetched per node through the
 *   API (see stores/ost-tree.store.ts); the node only carries `commentCount`.
 * - links and questions carry their server ids so they can be edited/removed.
 * - server bookkeeping fields (sortOrder, archived, created/modified dates, lastActivity) added.
 */
export type NodeType = 'product' | 'outcome' | 'opportunity' | 'solution' | 'assumption' | 'evidence';

export const NODE_TYPES: NodeType[] = ['product', 'outcome', 'opportunity', 'solution', 'assumption', 'evidence'];

export interface LinkRef {
  id?: number;
  name: string;
  url: string;
}

export interface Question {
  id?: number;
  text: string;
  done: boolean;
}

export interface LastActivity {
  at: string;
  byLogin: string | null;
}

export interface OstNode {
  /** Server node key, e.g. "opportunity-12". Used as the Vue Flow node id. */
  id: string;
  /** Numeric entity id on the server. */
  dbId: number;
  type: NodeType;
  /** Parent node key; null for products. */
  parent: string | null;
  title: string;
  note: string;
  /** assumption + solution + opportunity only; see STATUS in rules.ts. '' otherwise. */
  status: string;
  /** assumptions only, 0-100 */
  conf: number;
  /** opportunities only, 1-100 continuous */
  priority: number;
  /** opportunities only, 1-5 */
  value: number;
  /** assumptions only: owner login ('' when unassigned) */
  owner: string;
  sortOrder: number;
  archived: boolean;
  createdDate: string | null;
  lastModifiedDate: string | null;
  commentCount: number;
  links: LinkRef[];
  questions: Question[];
  /** products only: newest history entry in the branch */
  lastActivity: LastActivity | null;
}
