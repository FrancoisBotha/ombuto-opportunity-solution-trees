import { computed, ref, shallowRef } from 'vue';

import { defineStore } from 'pinia';

import { breadcrumb, descendantIds, orderTree } from '../domain/derive';
import { layoutTree } from '../domain/layout';
import { type NodePatch, fromDto, parseKey, pickPatchFields, toApiType, toPatchBody } from '../domain/mapping';
import { ALLOWED, canReparent, defaultLinks } from '../domain/rules';
import type { NodeType, OstNode } from '../domain/types';
import { type LoadFailure, describeError, loadFailure } from '../ost-errors';
import type { CommentDTO, HistoryEntryDTO, MyTeamDTO, TeamMemberDTO, TeamTreeDTO, TreeNodeDTO } from '../ost.model';
import OstService from '../ost.service';

import { useOstUiStore, writeLastTeam } from './ost-ui.store';

export type OstServiceFactory = () => OstService;

export interface TeamMeta {
  id: number;
  name: string;
  description: string | null;
  currentUserLogin: string;
  currentUserRole: TeamTreeDTO['currentUserRole'];
  canEdit: boolean;
  evidenceThisMonth: number;
  members: TeamMemberDTO[];
}

const toTeamMeta = (dto: TeamTreeDTO): TeamMeta => ({
  id: dto.id,
  name: dto.name,
  description: dto.description ?? null,
  currentUserLogin: dto.currentUserLogin,
  currentUserRole: dto.currentUserRole,
  canEdit: !!dto.canEdit,
  evidenceThisMonth: dto.evidenceThisMonth ?? 0,
  members: dto.members ?? [],
});

/**
 * The team's tree: flat nodes (pre-order, parent pointers) + team meta, and every API action.
 * Edits are optimistic with rollback; creates and deletes wait for the server.
 * View state (selection, collapse, scope, ...) lives in ost-ui.store.ts.
 */
export const useOstTreeStore = defineStore('ostTree', () => {
  let serviceFactory: OstServiceFactory = () => new OstService();
  const api = () => serviceFactory();

  // ---- state -----------------------------------------------------------------------------------
  const teams = ref<MyTeamDTO[]>([]);
  const teamsLoaded = ref(false);
  const teamId = ref<number | null>(null);
  const team = ref<TeamMeta | null>(null);
  const nodes = ref<OstNode[]>([]);
  const loading = ref(false);
  const loadError = ref<LoadFailure | null>(null);
  /** Last failed action, surfaced to the user; cleared by clearError(). */
  const error = ref<string | null>(null);
  /** Chat threads by node key, oldest first (loaded on demand). */
  const comments = ref<Record<string, CommentDTO[]>>({});
  /** History by node key, newest first (loaded on demand, invalidated by writes). */
  const history = ref<Record<string, HistoryEntryDTO[]>>({});
  /** Guards against an older tree response overwriting a newer team switch. */
  const loadSeq = shallowRef(0);

  // ---- getters -----------------------------------------------------------------------------------
  const index = computed(() => new Map(nodes.value.map(n => [n.id, n])));
  const byId = (key: string | null | undefined): OstNode | undefined => (key ? index.value.get(key) : undefined);
  const childrenOf = (key: string) => nodes.value.filter(n => n.parent === key);
  const canEdit = computed(() => !!team.value?.canEdit);
  const products = computed(() => nodes.value.filter(n => n.type === 'product'));
  const selected = computed(() => byId(useOstUiStore().selectedId));
  /** Products in scope: all of them or the one chosen in the product combo. */
  const roots = computed(() => {
    const ui = useOstUiStore();
    return products.value.filter(p => ui.productId === 'all' || p.id === ui.productId);
  });
  /** Derived positions (tidy layout) — never stored, never user-set. */
  const placed = computed(() =>
    layoutTree(nodes.value, {
      roots: roots.value.map(r => r.id),
      collapsed: useOstUiStore().collapsed,
    }),
  );
  const descendantCount = (key: string) => descendantIds(key, nodes.value).length;
  const ancestors = (key: string) => breadcrumb(key, nodes.value);
  const memberByLogin = (login: string | null | undefined) => (login ? team.value?.members.find(m => m.login === login) : undefined);

  // ---- helpers -----------------------------------------------------------------------------------
  const fail = (err: unknown, fallback?: string) => {
    error.value = describeError(err, fallback);
  };
  const invalidateHistory = (key: string) => {
    if (history.value[key]) {
      const next = { ...history.value };
      delete next[key];
      history.value = next;
    }
  };
  const replaceNode = (next: OstNode) => {
    const i = nodes.value.findIndex(n => n.id === next.id);
    if (i >= 0) nodes.value.splice(i, 1, next);
    else nodes.value.push(next);
  };
  const applyServerNode = (dto: TreeNodeDTO) => {
    const fresh = fromDto(dto);
    const current = byId(fresh.id);
    // Products carry lastActivity only on the tree read; keep ours when the write returns null.
    if (current && !fresh.lastActivity) fresh.lastActivity = current.lastActivity;
    replaceNode(fresh);
    return fresh;
  };

  // ---- actions: configuration ----------------------------------------------------------------
  function setServiceFactory(factory: OstServiceFactory) {
    serviceFactory = factory;
  }
  function clearError() {
    error.value = null;
  }
  function reset() {
    teamId.value = null;
    team.value = null;
    nodes.value = [];
    loading.value = false;
    loadError.value = null;
    error.value = null;
    comments.value = {};
    history.value = {};
  }

  // ---- actions: read -------------------------------------------------------------------------
  async function loadTeams() {
    try {
      teams.value = await api().listMyTeams();
    } catch (err) {
      fail(err, 'Your teams could not be loaded.');
      teams.value = [];
    } finally {
      teamsLoaded.value = true;
    }
    return teams.value;
  }

  async function loadTree(id: number) {
    const seq = ++loadSeq.value;
    const ui = useOstUiStore();
    if (teamId.value !== id) {
      reset();
      ui.reset();
    }
    teamId.value = id;
    loading.value = true;
    loadError.value = null;
    try {
      const dto = await api().getTree(id);
      if (seq !== loadSeq.value) return false;
      team.value = toTeamMeta(dto);
      nodes.value = orderTree((dto.nodes ?? []).map(fromDto));
      ui.restoreCollapsed(dto.currentUserLogin, dto.id);
      ui.pruneCollapsed(new Set(nodes.value.map(n => n.id)));
      writeLastTeam(dto.currentUserLogin, dto.id);
      return true;
    } catch (err) {
      if (seq !== loadSeq.value) return false;
      team.value = null;
      nodes.value = [];
      loadError.value = loadFailure(err);
      return false;
    } finally {
      if (seq === loadSeq.value) loading.value = false;
    }
  }

  // ---- actions: nodes ------------------------------------------------------------------------
  /** Optimistic field patch; rolls back and surfaces an error when the server refuses. */
  async function patchNode(key: string, patch: NodePatch): Promise<boolean> {
    const node = byId(key);
    const parsed = parseKey(key);
    if (!node || !parsed || !Object.keys(patch).length) return false;
    const snapshot = pickPatchFields(node, patch);
    Object.assign(node, patch);
    try {
      const dto = await api().patchNode(parsed.type, parsed.id, toPatchBody(patch));
      applyServerNode(dto);
      invalidateHistory(key);
      return true;
    } catch (err) {
      const current = byId(key);
      if (current) Object.assign(current, snapshot);
      fail(err);
      return false;
    }
  }

  /**
   * Creates a child (first allowed type when none given). Waits for the server, then selects the
   * new node and puts it in rename mode. Returns the new key, or null on failure.
   */
  async function createNode(parentKey: string, type?: NodeType, title?: string): Promise<string | null> {
    const parent = byId(parentKey);
    const parsed = parseKey(parentKey);
    if (!parent || !parsed) return null;
    const childType = type ?? ALLOWED[parent.type][0];
    if (!childType || !ALLOWED[parent.type].includes(childType)) {
      error.value = 'That node cannot go there.';
      return null;
    }
    try {
      const dto = await api().createNode({
        type: toApiType(childType),
        parentType: toApiType(parent.type),
        parentId: parsed.id,
        ...(title ? { title } : {}),
      });
      const created = fromDto(dto);
      nodes.value = orderTree([...nodes.value.filter(n => n.id !== created.id), created]);
      const ui = useOstUiStore();
      if (ui.collapsed[parentKey]) ui.setCollapsed(parentKey, false);
      ui.select(created.id);
      ui.startEditing(created.id);
      invalidateHistory(parentKey);
      return created.id;
    } catch (err) {
      fail(err, 'The node could not be created.');
      return null;
    }
  }

  /**
   * Re-parents `key` under `targetKey` (or reorders a product when targetKey is null).
   * Optimistic; applies the server's sibling order on success and rolls back on failure.
   */
  async function moveNode(key: string, targetKey: string | null, position?: number): Promise<boolean> {
    const node = byId(key);
    const parsed = parseKey(key);
    if (!node || !parsed) return false;
    let target: OstNode | undefined;
    if (targetKey === null) {
      if (node.type !== 'product') return false;
    } else {
      target = byId(targetKey);
      if (!target) return false;
      const reorderOnly = node.parent === targetKey && position !== undefined;
      if (!reorderOnly && !canReparent(key, targetKey, nodes.value)) {
        error.value = 'That node cannot go there.';
        return false;
      }
    }
    const before = nodes.value.map(n => ({ id: n.id, parent: n.parent, sortOrder: n.sortOrder }));
    const beforeOrder = nodes.value.slice();
    // Optimistic placement: `position` is a zero-based index among the new parent's same-type children.
    const newParent = target ? target.id : null;
    const siblings = nodes.value
      .filter(n => n.parent === newParent && n.type === node.type && n.id !== node.id)
      .sort((a, b) => a.sortOrder - b.sortOrder || a.dbId - b.dbId);
    node.parent = newParent;
    if (position !== undefined && position < siblings.length) {
      node.sortOrder = siblings[Math.max(0, position)].sortOrder - 0.5;
    } else {
      node.sortOrder = (siblings.at(-1)?.sortOrder ?? 0) + 1;
    }
    nodes.value = orderTree(nodes.value);
    try {
      const res = await api().moveNode({
        nodeType: toApiType(node.type),
        nodeId: parsed.id,
        parentType: target ? toApiType(target.type) : null,
        parentId: target ? target.dbId : null,
        ...(position !== undefined ? { position } : {}),
      });
      applyServerNode(res.node);
      for (const s of res.siblings ?? []) {
        const sib = byId(s.key);
        if (sib) sib.sortOrder = s.sortOrder;
      }
      nodes.value = orderTree(nodes.value);
      if (target) {
        const ui = useOstUiStore();
        if (ui.collapsed[target.id]) ui.setCollapsed(target.id, false);
      }
      invalidateHistory(key);
      return true;
    } catch (err) {
      const byKey = new Map(before.map(b => [b.id, b]));
      for (const n of beforeOrder) {
        const b = byKey.get(n.id);
        if (b) {
          n.parent = b.parent;
          n.sortOrder = b.sortOrder;
        }
      }
      nodes.value = beforeOrder;
      fail(err, 'The node could not be moved.');
      return false;
    }
  }

  /** Deletes a node and (server-side) its subtree; removes the subtree locally after the 204. */
  async function deleteNode(key: string): Promise<boolean> {
    const node = byId(key);
    const parsed = parseKey(key);
    if (!node || !parsed) return false;
    try {
      await api().deleteNode(parsed.type, parsed.id);
    } catch (err) {
      fail(err, 'The node could not be deleted.');
      return false;
    }
    const doomed = new Set([key, ...descendantIds(key, nodes.value)]);
    nodes.value = nodes.value.filter(n => !doomed.has(n.id));
    const nextComments = { ...comments.value };
    const nextHistory = { ...history.value };
    for (const k of doomed) {
      delete nextComments[k];
      delete nextHistory[k];
    }
    comments.value = nextComments;
    history.value = nextHistory;
    useOstUiStore().forgetNodes(doomed);
    return true;
  }

  // ---- actions: links --------------------------------------------------------------------------
  async function addLink(key: string, link: { name: string; url: string }): Promise<boolean> {
    const node = byId(key);
    const parsed = parseKey(key);
    if (!node || !parsed) return false;
    try {
      const dto = await api().addLink(parsed.type, parsed.id, link);
      byId(key)?.links.push({ id: dto.id, name: dto.name, url: dto.url });
      invalidateHistory(key);
      return true;
    } catch (err) {
      fail(err, 'The link could not be added.');
      return false;
    }
  }

  async function updateLink(key: string, linkId: number, patch: { name?: string; url?: string }): Promise<boolean> {
    const link = byId(key)?.links.find(l => l.id === linkId);
    if (!link) return false;
    const snapshot = { name: link.name, url: link.url };
    Object.assign(link, patch);
    try {
      const dto = await api().updateLink(linkId, patch);
      Object.assign(link, { name: dto.name, url: dto.url });
      return true;
    } catch (err) {
      Object.assign(link, snapshot);
      fail(err, 'The link could not be saved.');
      return false;
    }
  }

  async function removeLink(key: string, linkId: number): Promise<boolean> {
    const node = byId(key);
    const i = node?.links.findIndex(l => l.id === linkId) ?? -1;
    if (!node || i < 0) return false;
    const [removed] = node.links.splice(i, 1);
    try {
      await api().deleteLink(linkId);
      invalidateHistory(key);
      return true;
    } catch (err) {
      node.links.splice(i, 0, removed);
      fail(err, 'The link could not be removed.');
      return false;
    }
  }

  /** Re-adds any default link (by name) the node is missing. */
  async function restoreDefaultLinks(key: string): Promise<boolean> {
    const node = byId(key);
    if (!node) return false;
    const have = new Set(node.links.map(l => l.name));
    let ok = true;
    for (const link of defaultLinks({ id: node.id, type: node.type })) {
      if (!have.has(link.name)) ok = (await addLink(key, link)) && ok;
    }
    return ok;
  }

  // ---- actions: open questions -------------------------------------------------------------------
  async function addQuestion(key: string, text: string): Promise<boolean> {
    const node = byId(key);
    if (!node || node.type !== 'opportunity') return false;
    try {
      const dto = await api().addQuestion(node.dbId, text);
      byId(key)?.questions.push({ id: dto.id, text: dto.text, done: !!dto.done });
      invalidateHistory(key);
      return true;
    } catch (err) {
      fail(err, 'The question could not be added.');
      return false;
    }
  }

  async function updateQuestion(key: string, questionId: number, patch: { text?: string; done?: boolean }): Promise<boolean> {
    const question = byId(key)?.questions.find(q => q.id === questionId);
    if (!question) return false;
    const snapshot = { text: question.text, done: question.done };
    Object.assign(question, patch);
    try {
      const dto = await api().updateQuestion(questionId, patch);
      Object.assign(question, { text: dto.text, done: !!dto.done });
      return true;
    } catch (err) {
      Object.assign(question, snapshot);
      fail(err, 'The question could not be saved.');
      return false;
    }
  }

  async function removeQuestion(key: string, questionId: number): Promise<boolean> {
    const node = byId(key);
    const i = node?.questions.findIndex(q => q.id === questionId) ?? -1;
    if (!node || i < 0) return false;
    const [removed] = node.questions.splice(i, 1);
    try {
      await api().deleteQuestion(questionId);
      return true;
    } catch (err) {
      node.questions.splice(i, 0, removed);
      fail(err, 'The question could not be removed.');
      return false;
    }
  }

  // ---- actions: chat -------------------------------------------------------------------------
  async function loadComments(key: string): Promise<CommentDTO[]> {
    const parsed = parseKey(key);
    if (!parsed || parsed.type === 'product') return [];
    try {
      const list = await api().listComments(parsed.type, parsed.id);
      comments.value = { ...comments.value, [key]: list };
      const node = byId(key);
      if (node) node.commentCount = list.length;
      return list;
    } catch (err) {
      fail(err, 'The conversation could not be loaded.');
      return comments.value[key] ?? [];
    }
  }

  async function addComment(key: string, body: string): Promise<CommentDTO | null> {
    const parsed = parseKey(key);
    if (!parsed) return null;
    try {
      const dto = await api().addComment(parsed.type, parsed.id, body);
      comments.value = { ...comments.value, [key]: [...(comments.value[key] ?? []), dto] };
      const node = byId(key);
      if (node) node.commentCount += 1;
      invalidateHistory(key);
      return dto;
    } catch (err) {
      fail(err, 'The message could not be sent.');
      return null;
    }
  }

  async function editComment(key: string, commentId: number, body: string): Promise<boolean> {
    const list = comments.value[key] ?? [];
    const i = list.findIndex(c => c.id === commentId);
    if (i < 0) return false;
    const snapshot = list[i];
    list.splice(i, 1, { ...snapshot, body });
    try {
      const dto = await api().updateComment(commentId, body);
      const j = (comments.value[key] ?? []).findIndex(c => c.id === commentId);
      if (j >= 0) comments.value[key].splice(j, 1, dto);
      return true;
    } catch (err) {
      const j = (comments.value[key] ?? []).findIndex(c => c.id === commentId);
      if (j >= 0) comments.value[key].splice(j, 1, snapshot);
      fail(err, 'The message could not be edited.');
      return false;
    }
  }

  async function deleteComment(key: string, commentId: number): Promise<boolean> {
    const list = comments.value[key] ?? [];
    const i = list.findIndex(c => c.id === commentId);
    if (i < 0) return false;
    const [removed] = list.splice(i, 1);
    const node = byId(key);
    if (node) node.commentCount = Math.max(0, node.commentCount - 1);
    try {
      await api().deleteComment(commentId);
      invalidateHistory(key);
      return true;
    } catch (err) {
      (comments.value[key] ?? []).splice(i, 0, removed);
      if (node) node.commentCount += 1;
      fail(err, 'The message could not be deleted.');
      return false;
    }
  }

  // ---- actions: history ------------------------------------------------------------------------
  async function loadHistory(key: string): Promise<HistoryEntryDTO[]> {
    const parsed = parseKey(key);
    if (!parsed || parsed.type === 'product') return [];
    try {
      const list = await api().listHistory(parsed.type, parsed.id);
      history.value = { ...history.value, [key]: list };
      return list;
    } catch (err) {
      fail(err, 'The history could not be loaded.');
      return history.value[key] ?? [];
    }
  }

  return {
    // state
    teams,
    teamsLoaded,
    teamId,
    team,
    nodes,
    loading,
    loadError,
    error,
    comments,
    history,
    loadSeq,
    // getters
    byId,
    childrenOf,
    canEdit,
    products,
    selected,
    roots,
    placed,
    descendantCount,
    ancestors,
    memberByLogin,
    // actions
    setServiceFactory,
    clearError,
    reset,
    loadTeams,
    loadTree,
    patchNode,
    createNode,
    moveNode,
    deleteNode,
    addLink,
    updateLink,
    removeLink,
    restoreDefaultLinks,
    addQuestion,
    updateQuestion,
    removeQuestion,
    loadComments,
    addComment,
    editComment,
    deleteComment,
    loadHistory,
  };
});
