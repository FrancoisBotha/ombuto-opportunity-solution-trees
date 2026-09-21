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
  links: NodeLinkDTO[] | null;
  questions: OpenQuestionDTO[] | null;
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

export interface HistoryEntryDTO {
  id: number;
  eventType: string;
  summary: string;
  authorLogin: string | null;
  authorInitials: string | null;
  createdDate: string;
}
