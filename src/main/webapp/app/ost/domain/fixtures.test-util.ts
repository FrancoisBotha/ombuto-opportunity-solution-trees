/** Test-only builders for OST nodes and DTOs (not imported by application code). */
import type { TeamTreeDTO, TreeNodeDTO } from '../ost.model';

import type { NodeType, OstNode } from './types';

let nextId = 1000;

export function node(key: string, parent: string | null, extra: Partial<OstNode> = {}): OstNode {
  const [type, id] = key.split('-');
  return {
    id: key,
    dbId: Number(id) || nextId++,
    type: type as NodeType,
    parent,
    title: `Title ${key}`,
    note: '',
    status: '',
    conf: 40,
    priority: 50,
    value: 3,
    owner: '',
    sortOrder: 0,
    archived: false,
    createdDate: null,
    lastModifiedDate: null,
    commentCount: 0,
    links: [],
    questions: [],
    lastActivity: null,
    ...extra,
  };
}

export function dto(key: string, parentKey: string | null, extra: Partial<TreeNodeDTO> = {}): TreeNodeDTO {
  const [type, id] = key.split('-');
  return {
    key,
    type: type.toUpperCase() as TreeNodeDTO['type'],
    id: Number(id),
    parentKey,
    title: `Title ${key}`,
    notes: null,
    status: null,
    confidence: null,
    priority: null,
    valueRating: null,
    ownerLogin: null,
    sortOrder: 0,
    archived: false,
    createdDate: '2026-09-01T10:00:00Z',
    lastModifiedDate: null,
    commentCount: 0,
    links: [],
    questions: [],
    lastActivity: null,
    ...extra,
  };
}

/** A small tree: two products, outcome, nested opportunities, solution with assumptions and evidence. */
export function sampleNodes(): OstNode[] {
  return [
    node('product-1', null, { sortOrder: 1 }),
    node('outcome-1', 'product-1'),
    node('opportunity-1', 'outcome-1', { status: 'exploring', priority: 80, value: 4 }),
    node('opportunity-2', 'opportunity-1', { status: 'unexplored', sortOrder: 0 }),
    node('solution-1', 'opportunity-1', { status: 'candidate' }),
    node('assumption-1', 'solution-1', { status: 'supported', conf: 30, owner: 'user' }),
    node('assumption-2', 'solution-1', { status: 'refuted', conf: 90, sortOrder: 1 }),
    node('assumption-3', 'solution-1', { status: 'testing', conf: 60, sortOrder: 2 }),
    node('evidence-1', 'opportunity-1', { createdDate: '2026-09-10T08:00:00Z' }),
    node('evidence-2', 'assumption-3', { createdDate: '2026-08-10T08:00:00Z' }),
    node('product-2', null, { sortOrder: 2 }),
  ];
}

export function treeDto(nodes: TreeNodeDTO[] = [], extra: Partial<TeamTreeDTO> = {}): TeamTreeDTO {
  return {
    id: 7,
    name: 'Team Jupiter',
    description: null,
    currentUserLogin: 'user',
    currentUserRole: 'OWNER',
    canEdit: true,
    evidenceThisMonth: 1,
    members: [
      { login: 'user', firstName: 'Kira', lastName: 'P', initials: 'KP', role: 'OWNER' },
      { login: 'admin', firstName: 'Ana', lastName: 'R', initials: 'AR', role: 'VIEWER' },
    ],
    nodes,
    ...extra,
  };
}

/**
 * A large, realistic tree for performance smoke tests: `products` products, each with one outcome,
 * `opportunities` opportunities, 2 solutions per opportunity, 2 assumptions per solution and one
 * evidence per assumption (1 + 1 + o * (1 + 2 * (1 + 2 * 2)) nodes per product).
 */
export function bigTreeDtos(products = 2, opportunities = 15): TreeNodeDTO[] {
  const out: TreeNodeDTO[] = [];
  let id = 1;
  for (let p = 0; p < products; p++) {
    const product = `product-${id++}`;
    out.push(dto(product, null, { sortOrder: p }));
    const outcome = `outcome-${id++}`;
    out.push(dto(outcome, product));
    for (let o = 0; o < opportunities; o++) {
      const opportunity = `opportunity-${id++}`;
      out.push(dto(opportunity, outcome, { status: 'EXPLORING', priority: 50, valueRating: 3, sortOrder: o }));
      for (let s = 0; s < 2; s++) {
        const solution = `solution-${id++}`;
        out.push(dto(solution, opportunity, { status: 'CANDIDATE', sortOrder: s }));
        for (let a = 0; a < 2; a++) {
          const assumption = `assumption-${id++}`;
          out.push(dto(assumption, solution, { status: 'TESTING', confidence: 40, sortOrder: a }));
          out.push(dto(`evidence-${id++}`, assumption));
        }
      }
    }
  }
  return out;
}
