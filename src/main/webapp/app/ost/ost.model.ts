/** Wire types for the OST backend API (see TeamTreeResource and the Tree*Resource classes). */

export type ApiNodeType = 'PRODUCT' | 'OUTCOME' | 'OPPORTUNITY' | 'SOLUTION' | 'ASSUMPTION' | 'EVIDENCE';
export type ApiTeamRole = 'OWNER' | 'EDITOR' | 'VIEWER';

export interface TeamMemberDTO {
  login: string;
  firstName: string | null;
  lastName: string | null;
  initials: string;
  role: ApiTeamRole;
}

export interface NodeLinkDTO {
  id: number;
  name: string;
  url: string;
  sortOrder?: number;
}

export interface OpenQuestionDTO {
  id: number;
  text: string;
  done: boolean;
  sortOrder?: number;
}

/** LABEL-001: a tag attached to an opportunity or a solution. */
export interface TreeNodeTagDTO {
  id: number;
  name: string;
}

export interface TreeNodeDTO {
  key: string;
  type: ApiNodeType;
  id: number;
  parentKey: string | null;
  title: string;
  notes: string | null;
  status: string | null;
  confidence: number | null;
  priority: number | null;
  valueRating: number | null;
  ownerLogin: string | null;
  sortOrder: number | null;
  archived: boolean | null;
  createdDate: string | null;
  lastModifiedDate: string | null;
  commentCount: number | null;
  /** MTRANS-004/005: number of transcripts on this node; body is never included in the tree payload. */
  transcriptCount: number | null;
  links: NodeLinkDTO[] | null;
  questions: OpenQuestionDTO[] | null;
  /** LABEL-001: empty (never null) for non-taggable types. */
  tags: TreeNodeTagDTO[] | null;
  lastActivity: { at: string; byLogin: string | null } | null;
}

export interface TeamTreeDTO {
  id: number;
  name: string;
  description: string | null;
  currentUserLogin: string;
  currentUserRole: ApiTeamRole;
  canEdit: boolean;
  evidenceThisMonth: number;
  members: TeamMemberDTO[];
  nodes: TreeNodeDTO[];
}

/** Entry of GET api/team-management/my-teams (the team switcher's source). */
export interface MyTeamDTO {
  id: number;
  name: string;
  description?: string | null;
  role: ApiTeamRole;
  memberCount: number;
  productCount: number;
}

export interface CreateNodeRequest {
  type: ApiNodeType;
  parentType: ApiNodeType;
  parentId: number;
  title?: string;
}

/** PATCH body: absent = unchanged; null clears notes / ownerLogin. */
export interface PatchNodeRequest {
  title?: string;
  notes?: string | null;
  status?: string;
  confidence?: number;
  priority?: number;
  valueRating?: number;
  ownerLogin?: string | null;
  archived?: boolean;
}

export interface MoveNodeRequest {
  nodeType: ApiNodeType;
  nodeId: number;
  parentType: ApiNodeType | null;
  parentId: number | null;
  position?: number;
}

export interface MoveNodeResponse {
  node: TreeNodeDTO;
  siblings: { key: string; sortOrder: number }[];
}

/**
 * A chat message on a tree node. Ownership is NOT on the DTO: it is derived per viewer by
 * comparing {@link authorLogin} to the store's {@code team.currentUserLogin}. A per-viewer flag on
 * a payload broadcast to a team topic would attribute someone else's message to every recipient
 * (CHAT-001).
 */
export interface CommentDTO {
  id: number;
  body: string;
  authorLogin: string | null;
  authorInitials: string | null;
  authorName: string | null;
  createdDate: string;
  editedDate: string | null;
}

/**
 * MTRANS-005: metadata view of a meeting transcript. The list endpoint returns this — never the
 * body — so a scrollable list can load without pulling every transcript's text (NFR-024).
 */
export interface TranscriptMetaDTO {
  id: number;
  title: string;
  meetingDate: string;
  attendees: string | null;
  source: 'PASTED' | 'UPLOADED';
  nodeType: string;
  nodeId: number;
  nodeKey: string;
  authorLogin: string | null;
  authorInitials: string | null;
  authorName: string | null;
  createdDate: string;
  editedDate: string | null;
}

/** MTRANS-005: a transcript with its body. Only returned by the single-transcript read endpoint. */
export interface TranscriptDTO extends TranscriptMetaDTO {
  body: string;
}

/**
 * MTRANS-006: request body for creating or editing a transcript. On create, exactly one node id
 * must be set (server-enforced); on edit, the node ids are ignored — a transcript is bound to its
 * node for life. {@code source} defaults to PASTED on create; it is ignored on update.
 */
export interface TranscriptWriteRequest {
  title: string;
  meetingDate: string;
  attendees: string | null;
  body: string;
  source?: 'PASTED' | 'UPLOADED';
  productId?: number | null;
  outcomeId?: number | null;
  opportunityId?: number | null;
  solutionId?: number | null;
  assumptionId?: number | null;
  evidenceId?: number | null;
}

/** MTRANS-006: response of the parse-only upload endpoint — parsed body text, ready for review. */
export interface TranscriptParseResultDTO {
  body: string;
}

export interface HistoryEntryDTO {
  id: number;
  eventType: string;
  summary: string;
  authorLogin: string | null;
  authorInitials: string | null;
  createdDate: string;
}
