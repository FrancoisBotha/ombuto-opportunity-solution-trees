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
