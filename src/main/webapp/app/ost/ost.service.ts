import axios from 'axios';

import type {
  CommentDTO,
  CreateNodeRequest,
  HistoryEntryDTO,
  MoveNodeRequest,
  MoveNodeResponse,
  MyTeamDTO,
  NodeLinkDTO,
  OpenQuestionDTO,
  PatchNodeRequest,
  TeamTreeDTO,
  TreeNodeDTO,
} from './ost.model';

const treeApi = 'api/tree';

/** Node type as it appears in an URL path segment (the server accepts any case). */
const seg = (type: string) => type.toLowerCase();

/**
 * Every OST endpoint the frontend uses. Inject in components with
 * `inject('ostService', () => new OstService())` — no third argument — and call it as `ostService()`.
 */
export default class OstService {
  // ---- teams / tree read -------------------------------------------------------------------

  listMyTeams(): Promise<MyTeamDTO[]> {
    return axios.get<MyTeamDTO[]>('api/team-management/my-teams').then(res => res.data);
  }

  getTree(teamId: number): Promise<TeamTreeDTO> {
    return axios.get<TeamTreeDTO>(`api/teams/${teamId}/tree`).then(res => res.data);
  }

  // ---- nodes ---------------------------------------------------------------------------------

  createNode(request: CreateNodeRequest): Promise<TreeNodeDTO> {
    return axios.post<TreeNodeDTO>(`${treeApi}/nodes`, request).then(res => res.data);
  }

  patchNode(type: string, id: number, body: PatchNodeRequest): Promise<TreeNodeDTO> {
    return axios.patch<TreeNodeDTO>(`${treeApi}/nodes/${seg(type)}/${id}`, body).then(res => res.data);
  }

  moveNode(request: MoveNodeRequest): Promise<MoveNodeResponse> {
    return axios.post<MoveNodeResponse>(`${treeApi}/nodes/move`, request).then(res => res.data);
  }

  deleteNode(type: string, id: number): Promise<void> {
    return axios.delete(`${treeApi}/nodes/${seg(type)}/${id}`).then(() => undefined);
  }

  // ---- links ---------------------------------------------------------------------------------

  addLink(type: string, id: number, link: { name: string; url: string }): Promise<NodeLinkDTO> {
    return axios.post<NodeLinkDTO>(`${treeApi}/nodes/${seg(type)}/${id}/links`, link).then(res => res.data);
  }

  updateLink(linkId: number, patch: { name?: string; url?: string }): Promise<NodeLinkDTO> {
    return axios.patch<NodeLinkDTO>(`${treeApi}/links/${linkId}`, patch).then(res => res.data);
  }

  deleteLink(linkId: number): Promise<void> {
    return axios.delete(`${treeApi}/links/${linkId}`).then(() => undefined);
  }

  // ---- open questions (opportunities) -------------------------------------------------------

  addQuestion(opportunityId: number, text: string): Promise<OpenQuestionDTO> {
    return axios.post<OpenQuestionDTO>(`${treeApi}/opportunities/${opportunityId}/questions`, { text }).then(res => res.data);
  }

  updateQuestion(questionId: number, patch: { text?: string; done?: boolean }): Promise<OpenQuestionDTO> {
    return axios.patch<OpenQuestionDTO>(`${treeApi}/questions/${questionId}`, patch).then(res => res.data);
  }

  deleteQuestion(questionId: number): Promise<void> {
    return axios.delete(`${treeApi}/questions/${questionId}`).then(() => undefined);
  }

  // ---- chat ----------------------------------------------------------------------------------

  listComments(type: string, id: number): Promise<CommentDTO[]> {
    return axios.get<CommentDTO[]>(`${treeApi}/nodes/${seg(type)}/${id}/comments`).then(res => res.data);
  }

  addComment(type: string, id: number, body: string): Promise<CommentDTO> {
    return axios.post<CommentDTO>(`${treeApi}/nodes/${seg(type)}/${id}/comments`, { body }).then(res => res.data);
  }

  updateComment(commentId: number, body: string): Promise<CommentDTO> {
    return axios.patch<CommentDTO>(`${treeApi}/comments/${commentId}`, { body }).then(res => res.data);
  }

  deleteComment(commentId: number): Promise<void> {
    return axios.delete(`${treeApi}/comments/${commentId}`).then(() => undefined);
  }

  // ---- history -------------------------------------------------------------------------------

  listHistory(type: string, id: number): Promise<HistoryEntryDTO[]> {
    return axios.get<HistoryEntryDTO[]>(`${treeApi}/nodes/${seg(type)}/${id}/history`).then(res => res.data);
  }
}
