/*
 * TreeNodeDTO <-> OstNode mapping. The server speaks upper-case enums (OPPORTUNITY, EXPLORING)
 * and entity ids; the domain layer (rules.ts, layout.ts) speaks the handoff's lower-case
 * vocabulary and uses the node key ("opportunity-12") as id and parentKey as parent.
 */
import type { ApiNodeType, PatchNodeRequest, TreeNodeDTO } from '../ost.model';

import { NODE_TYPES, type NodeType, type OstNode } from './types';

export type NodePatch = Partial<Pick<OstNode, 'title' | 'note' | 'status' | 'conf' | 'priority' | 'value' | 'owner' | 'archived'>>;

export const toNodeType = (type: string): NodeType => {
  const t = type.toLowerCase() as NodeType;
  if (!NODE_TYPES.includes(t)) throw new Error(`Unknown node type: ${type}`);
  return t;
};

export const toApiType = (type: NodeType): ApiNodeType => type.toUpperCase() as ApiNodeType;

export const nodeKey = (type: NodeType, id: number): string => `${type}-${id}`;

/** "opportunity-12" -> { type: 'opportunity', id: 12 }; null when the key is malformed. */
export function parseKey(key: string): { type: NodeType; id: number } | null {
  const match = /^([a-z]+)-(\d+)$/i.exec(key ?? '');
  if (!match) return null;
  const type = match[1].toLowerCase() as NodeType;
  if (!NODE_TYPES.includes(type)) return null;
  return { type, id: Number(match[2]) };
}

export function fromDto(dto: TreeNodeDTO): OstNode {
  return {
    id: dto.key,
    dbId: dto.id,
    type: toNodeType(dto.type),
    parent: dto.parentKey ?? null,
    title: dto.title ?? '',
    note: dto.notes ?? '',
    status: dto.status ? dto.status.toLowerCase() : '',
    conf: dto.confidence ?? 0,
    priority: dto.priority ?? 0,
    value: dto.valueRating ?? 0,
    owner: dto.ownerLogin ?? '',
    sortOrder: dto.sortOrder ?? 0,
    archived: !!dto.archived,
    createdDate: dto.createdDate ?? null,
    lastModifiedDate: dto.lastModifiedDate ?? null,
    commentCount: dto.commentCount ?? 0,
    links: (dto.links ?? []).map(l => ({ id: l.id, name: l.name, url: l.url })),
    questions: (dto.questions ?? []).map(q => ({ id: q.id, text: q.text, done: !!q.done })),
    lastActivity: dto.lastActivity ? { at: dto.lastActivity.at, byLogin: dto.lastActivity.byLogin ?? null } : null,
  };
}

/** Domain patch -> PATCH body. Only the keys present in the patch are sent. */
export function toPatchBody(patch: NodePatch): PatchNodeRequest {
  const body: PatchNodeRequest = {};
  if (patch.title !== undefined) body.title = patch.title;
  if (patch.note !== undefined) body.notes = patch.note === '' ? null : patch.note;
  if (patch.status !== undefined) body.status = patch.status.toUpperCase();
  if (patch.conf !== undefined) body.confidence = patch.conf;
  if (patch.priority !== undefined) body.priority = patch.priority;
  if (patch.value !== undefined) body.valueRating = patch.value;
  if (patch.owner !== undefined) body.ownerLogin = patch.owner === '' ? null : patch.owner;
  if (patch.archived !== undefined) body.archived = patch.archived;
  return body;
}

/** Inverse of fromDto for the editable fields (used by tests and optimistic snapshots). */
export function pickPatchFields(node: OstNode, patch: NodePatch): NodePatch {
  const snapshot: NodePatch = {};
  for (const k of Object.keys(patch) as (keyof NodePatch)[]) {
    (snapshot as any)[k] = node[k];
  }
  return snapshot;
}
