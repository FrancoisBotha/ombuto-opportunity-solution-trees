import axios, { type AxiosRequestConfig } from 'axios';

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

/** Header the server echoes on the resulting change event so a client can drop its own echo (RTC-005). */
export const REQUEST_ID_HEADER = 'X-Client-Request-Id';

const withRequestId = (requestId: string | undefined): AxiosRequestConfig | undefined =>
  requestId ? { headers: { [REQUEST_ID_HEADER]: requestId } } : undefined;

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

  createNode(request: CreateNodeRequest, requestId?: string): Promise<TreeNodeDTO> {
    return axios.post<TreeNodeDTO>(`${treeApi}/nodes`, request, withRequestId(requestId)).then(res => res.data);
  }

  patchNode(type: string, id: number, body: PatchNodeRequest, requestId?: string): Promise<TreeNodeDTO> {
    return axios.patch<TreeNodeDTO>(`${treeApi}/nodes/${seg(type)}/${id}`, body, withRequestId(requestId)).then(res => res.data);
  }

  moveNode(request: MoveNodeRequest, requestId?: string): Promise<MoveNodeResponse> {
    return axios.post<MoveNodeResponse>(`${treeApi}/nodes/move`, request, withRequestId(requestId)).then(res => res.data);
  }

  deleteNode(type: string, id: number, requestId?: string): Promise<void> {
    return axios.delete(`${treeApi}/nodes/${seg(type)}/${id}`, withRequestId(requestId)).then(() => undefined);
  }

  // ---- links ---------------------------------------------------------------------------------

  addLink(type: string, id: number, link: { name: string; url: string }, requestId?: string): Promise<NodeLinkDTO> {
    return axios.post<NodeLinkDTO>(`${treeApi}/nodes/${seg(type)}/${id}/links`, link, withRequestId(requestId)).then(res => res.data);
  }

  updateLink(linkId: number, patch: { name?: string; url?: string }, requestId?: string): Promise<NodeLinkDTO> {
    return axios.patch<NodeLinkDTO>(`${treeApi}/links/${linkId}`, patch, withRequestId(requestId)).then(res => res.data);
  }

  deleteLink(linkId: number, requestId?: string): Promise<void> {
    return axios.delete(`${treeApi}/links/${linkId}`, withRequestId(requestId)).then(() => undefined);
  }

  // ---- open questions (opportunities) -------------------------------------------------------

  addQuestion(opportunityId: number, text: string, requestId?: string): Promise<OpenQuestionDTO> {
    return axios
      .post<OpenQuestionDTO>(`${treeApi}/opportunities/${opportunityId}/questions`, { text }, withRequestId(requestId))
      .then(res => res.data);
  }

  updateQuestion(questionId: number, patch: { text?: string; done?: boolean }, requestId?: string): Promise<OpenQuestionDTO> {
    return axios.patch<OpenQuestionDTO>(`${treeApi}/questions/${questionId}`, patch, withRequestId(requestId)).then(res => res.data);
  }

  deleteQuestion(questionId: number, requestId?: string): Promise<void> {
    return axios.delete(`${treeApi}/questions/${questionId}`, withRequestId(requestId)).then(() => undefined);
  }

  // ---- chat ----------------------------------------------------------------------------------

  listComments(type: string, id: number): Promise<CommentDTO[]> {
    return axios.get<CommentDTO[]>(`${treeApi}/nodes/${seg(type)}/${id}/comments`).then(res => res.data);
  }

  addComment(type: string, id: number, body: string, requestId?: string): Promise<CommentDTO> {
    return axios.post<CommentDTO>(`${treeApi}/nodes/${seg(type)}/${id}/comments`, { body }, withRequestId(requestId)).then(res => res.data);
  }

  updateComment(commentId: number, body: string, requestId?: string): Promise<CommentDTO> {
    return axios.patch<CommentDTO>(`${treeApi}/comments/${commentId}`, { body }, withRequestId(requestId)).then(res => res.data);
  }

  deleteComment(commentId: number, requestId?: string): Promise<void> {
    return axios.delete(`${treeApi}/comments/${commentId}`, withRequestId(requestId)).then(() => undefined);
  }

  // ---- history -------------------------------------------------------------------------------

  listHistory(type: string, id: number): Promise<HistoryEntryDTO[]> {
    return axios.get<HistoryEntryDTO[]>(`${treeApi}/nodes/${seg(type)}/${id}/history`).then(res => res.data);
  }
}
