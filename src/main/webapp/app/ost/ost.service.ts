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
  TranscriptDTO,
  TranscriptMetaDTO,
  TranscriptParseResultDTO,
  TranscriptWriteRequest,
  TreeNodeDTO,
  TreeNodeTagDTO,
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

  // ---- MTRANS-005: transcripts (metadata list + lazy body fetch) -----------------------------

  listTranscriptsByNode(type: string, id: number): Promise<TranscriptMetaDTO[]> {
    return axios.get<TranscriptMetaDTO[]>(`${treeApi}/nodes/${seg(type)}/${id}/transcripts`).then(res => res.data);
  }

  getTranscript(id: number): Promise<TranscriptDTO> {
    return axios.get<TranscriptDTO>(`${treeApi}/transcripts/${id}`).then(res => res.data);
  }

  // ---- MTRANS-006: transcript create / edit / delete + parse upload -------------------------

  createTranscript(request: TranscriptWriteRequest): Promise<TranscriptDTO> {
    return axios.post<TranscriptDTO>(`${treeApi}/transcripts`, request).then(res => res.data);
  }

  updateTranscript(id: number, request: TranscriptWriteRequest): Promise<TranscriptDTO> {
    return axios.patch<TranscriptDTO>(`${treeApi}/transcripts/${id}`, request).then(res => res.data);
  }

  deleteTranscript(id: number): Promise<void> {
    return axios.delete(`${treeApi}/transcripts/${id}`).then(() => undefined);
  }

  parseTranscriptUpload(teamId: number, file: File): Promise<TranscriptParseResultDTO> {
    const form = new FormData();
    form.append('file', file);
    return axios
      .post<TranscriptParseResultDTO>(`${treeApi}/teams/${teamId}/transcripts/parse`, form, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
      .then(res => res.data);
  }

  // ---- LABEL-001: team-scoped labels --------------------------------------------------------

  listLabelSuggestions(teamId: number): Promise<TreeNodeTagDTO[]> {
    return axios.get<TreeNodeTagDTO[]>(`${treeApi}/labels/suggestions/${teamId}`).then(res => res.data);
  }

  createLabel(teamId: number, name: string): Promise<TreeNodeTagDTO> {
    return axios.post<TreeNodeTagDTO>(`${treeApi}/labels/team/${teamId}`, { name }).then(res => res.data);
  }

  applyLabels(type: string, id: number, tagIds: number[], requestId?: string): Promise<TreeNodeDTO> {
    return axios.put<TreeNodeDTO>(`${treeApi}/labels/node/${seg(type)}/${id}`, { tagIds }, withRequestId(requestId)).then(res => res.data);
  }
}
