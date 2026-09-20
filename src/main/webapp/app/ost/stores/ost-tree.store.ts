import { computed, ref, shallowRef, toRaw } from 'vue';

import { defineStore } from 'pinia';

import { breadcrumb, descendantIds, evidenceThisMonth as countEvidenceThisMonth, orderTree } from '../domain/derive';
import { layoutTree } from '../domain/layout';
import { type NodePatch, fromDto, parseKey, toApiType, toPatchBody } from '../domain/mapping';
import { ALLOWED, canReparent, defaultLinks } from '../domain/rules';
import type { NodeType, OstNode } from '../domain/types';
import { DELETED_ELSEWHERE, DEMOTED_TO_VIEWER, type LoadFailure, describeError, httpStatus, loadFailure, messageKey } from '../ost-errors';
import type {
  CommentDTO,
  HistoryEntryDTO,
  MyTeamDTO,
  NodeLinkDTO,
  OpenQuestionDTO,
  TeamMemberDTO,
  TeamTreeDTO,
  TreeNodeDTO,
} from '../ost.model';
import OstService from '../ost.service';

import { useOstUiStore, writeLastTeam } from './ost-ui.store';

export type OstServiceFactory = () => OstService;

type PatchField = keyof NodePatch;
const PATCH_FIELDS: PatchField[] = ['title', 'note', 'status', 'conf', 'priority', 'value', 'owner', 'archived'];

/** Realtime event body applied to the flat store (RTC-005). See docs/Epics/epic_05_REALTIME_COLLABORATION.md §5/FR-030. */
export type OstTreeEvent =
  | { type: 'NODE_CREATED'; actingUserLogin?: string | null; requestId?: string; node: TreeNodeDTO }
  | { type: 'NODE_UPDATED'; actingUserLogin?: string | null; requestId?: string; node: TreeNodeDTO }
  | {
      type: 'NODE_MOVED';
      actingUserLogin?: string | null;
      requestId?: string;
      node: TreeNodeDTO;
      siblings?: { key: string; sortOrder: number }[];
    }
  | { type: 'NODE_DELETED'; actingUserLogin?: string | null; requestId?: string; key: string; descendantKeys?: string[] }
  | { type: 'LINK_ADDED' | 'LINK_UPDATED'; actingUserLogin?: string | null; requestId?: string; key: string; link: NodeLinkDTO }
  | { type: 'LINK_REMOVED'; actingUserLogin?: string | null; requestId?: string; key: string; linkId: number }
  | {
      type: 'QUESTION_ADDED' | 'QUESTION_UPDATED';
      actingUserLogin?: string | null;
      requestId?: string;
      key: string;
      question: OpenQuestionDTO;
    }
  | { type: 'QUESTION_REMOVED'; actingUserLogin?: string | null; requestId?: string; key: string; questionId: number }
  | {
      type: 'COMMENT_ADDED' | 'COMMENT_UPDATED';
      actingUserLogin?: string | null;
      requestId?: string;
      key: string;
      comment: CommentDTO;
      commentCount?: number;
    }
  | {
      type: 'COMMENT_DELETED';
      actingUserLogin?: string | null;
      requestId?: string;
      key: string;
      commentId: number;
      commentCount?: number;
    };

/** Wire-shape event as broadcast by TreeChangePublisher / TreeChangeBroadcaster. */
interface WireTreeEvent {
  type: string;
  actingUserLogin?: string | null;
  requestId?: string;
  payload?: unknown;
  // The flat variant (still accepted for internal test/utility callers) uses these directly:
  node?: TreeNodeDTO;
  key?: string;
  link?: NodeLinkDTO;
  linkId?: number;
  question?: OpenQuestionDTO;
  questionId?: number;
  comment?: CommentDTO;
  commentId?: number;
  commentCount?: number;
  siblings?: { key: string; sortOrder: number }[];
  descendantKeys?: string[];
}

/**
 * Turns a wire {@code TreeChangeEvent} (whose body lives under {@code payload}, with the server's
 * field names) into the flat {@link OstTreeEvent} shape the store applies. If the incoming record
 * already has the flat fields at the top level, it is returned unchanged.
 */
function normalizeEvent(raw: WireTreeEvent): OstTreeEvent | null {
  const meta = { actingUserLogin: raw.actingUserLogin, requestId: raw.requestId };
  const p: any = raw.payload ?? {};
  switch (raw.type) {
    case 'NODE_CREATED':
    case 'NODE_UPDATED': {
      const node = raw.node ?? (p as TreeNodeDTO);
      return node ? ({ type: raw.type, ...meta, node } as OstTreeEvent) : null;
    }
    case 'NODE_MOVED': {
      const node = raw.node ?? (p.node as TreeNodeDTO);
      const siblings = (raw.siblings ?? p.siblings ?? []) as { key: string; sortOrder: number }[];
      return node ? ({ type: 'NODE_MOVED', ...meta, node, siblings } as OstTreeEvent) : null;
    }
    case 'NODE_DELETED': {
      const key = raw.key ?? (p.key as string);
      const descendantKeys = (raw.descendantKeys ?? p.cascadedKeys ?? []) as string[];
      return key ? ({ type: 'NODE_DELETED', ...meta, key, descendantKeys } as OstTreeEvent) : null;
    }
    case 'LINK_ADDED':
    case 'LINK_UPDATED': {
      const key = raw.key ?? (p.nodeKey as string);
      const link = raw.link ?? (p.link as NodeLinkDTO);
      return key && link ? ({ type: raw.type, ...meta, key, link } as OstTreeEvent) : null;
    }
    case 'LINK_REMOVED': {
      const key = raw.key ?? (p.nodeKey as string);
      const linkId = raw.linkId ?? (p.linkId as number);
      return key && linkId != null ? ({ type: 'LINK_REMOVED', ...meta, key, linkId } as OstTreeEvent) : null;
    }
    case 'QUESTION_ADDED':
    case 'QUESTION_UPDATED': {
      const key = raw.key ?? (p.nodeKey as string);
      const question = raw.question ?? (p.question as OpenQuestionDTO);
      return key && question ? ({ type: raw.type, ...meta, key, question } as OstTreeEvent) : null;
    }
    case 'QUESTION_REMOVED': {
      const key = raw.key ?? (p.nodeKey as string);
      const questionId = raw.questionId ?? (p.questionId as number);
      return key && questionId != null ? ({ type: 'QUESTION_REMOVED', ...meta, key, questionId } as OstTreeEvent) : null;
    }
    case 'COMMENT_ADDED':
    case 'COMMENT_UPDATED': {
      const key = raw.key ?? (p.nodeKey as string);
      const comment = raw.comment ?? (p.comment as CommentDTO);
      const commentCount = raw.commentCount ?? (typeof p.commentCount === 'number' ? p.commentCount : undefined);
      return key && comment ? ({ type: raw.type, ...meta, key, comment, commentCount } as OstTreeEvent) : null;
    }
    case 'COMMENT_DELETED': {
      const key = raw.key ?? (p.nodeKey as string);
      const commentId = raw.commentId ?? (p.commentId as number);
      const commentCount = raw.commentCount ?? (typeof p.commentCount === 'number' ? p.commentCount : undefined);
      return key && commentId != null ? ({ type: 'COMMENT_DELETED', ...meta, key, commentId, commentCount } as OstTreeEvent) : null;
    }
    default:
      return null;
  }
}

const RECENT_REQUEST_ID_CAP = 200;
let requestIdCounter = 0;
const generateRequestId = (): string =>
  `rtc-${Date.now().toString(36)}-${(++requestIdCounter).toString(36)}-${Math.random().toString(36).slice(2, 8)}`;

/** In-flight bookkeeping for one editable field of one node (see patchNode). */
interface FieldTrack {
  /** optimistic value of every patch of this field still waiting for the server, by write seq */
  pending: Map<number, unknown>;
  /** newest value the server has confirmed (the rollback target) and the seq that confirmed it */
  confirmed: unknown;
  confirmedSeq: number;
  /** seq of the newest response whose value for this field is on screen */
  appliedSeq: number;
}

interface NodeTrack {
  fields: Partial<Record<PatchField, FieldTrack>>;
  /** seq of the newest patch response applied to this node */
  appliedSeq: number;
  inflight: number;
}

export interface TeamMeta {
  id: number;
  name: string;
  description: string | null;
  currentUserLogin: string;
  currentUserRole: TeamTreeDTO['currentUserRole'];
  canEdit: boolean;
  members: TeamMemberDTO[];
}

const toTeamMeta = (dto: TeamTreeDTO): TeamMeta => ({
  id: dto.id,
  name: dto.name,
  description: dto.description ?? null,
  currentUserLogin: dto.currentUserLogin,
  currentUserRole: dto.currentUserRole,
  canEdit: !!dto.canEdit,
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
  /** Orders optimistic writes (patches and moves) so late responses never undo newer edits. */
  let writeSeq = 0;
  const patchTracks = new Map<string, NodeTrack>();
  /** Newest move seq per node key, while that move is in flight. */
  const moveSeqs = new Map<string, number>();
  /** Bumped by every history-writing write per node key; a history read that raced one re-reads. */
  const historyGen = new Map<string, number>();
  /** Request ids this client sent recently, so it can drop the echo of its own event (FR-032). */
  const recentRequestIds: string[] = [];
  const recentRequestIdSet = new Set<string>();
  /** Fields the user is currently typing in (per node), so remote events do not overwrite them (FR-033). */
  const typingFields = new Map<string, Set<PatchField>>();
  /**
   * Fields whose latest remote value was dropped because a local edit was in flight or the user
   * was typing; consumers surface them via the panel's dirty-field mechanism (FR-033).
   */
  const droppedRemote = ref<Record<string, Partial<Record<PatchField, unknown>>>>({});

  function rememberRequestId(id: string) {
    if (recentRequestIdSet.has(id)) return;
    recentRequestIdSet.add(id);
    recentRequestIds.push(id);
    if (recentRequestIds.length > RECENT_REQUEST_ID_CAP) {
      const evicted = recentRequestIds.shift();
      if (evicted) recentRequestIdSet.delete(evicted);
    }
  }
  function isOwnEcho(event: { actingUserLogin?: string | null; requestId?: string }): boolean {
    const me = team.value?.currentUserLogin;
    if (!me || !event.actingUserLogin || event.actingUserLogin !== me) return false;
    return !!event.requestId && recentRequestIdSet.has(event.requestId);
  }
  function markTyping(key: string, field: PatchField) {
    let set = typingFields.get(key);
    if (!set) {
      set = new Set();
      typingFields.set(key, set);
    }
    set.add(field);
  }
  function clearTyping(key: string, field?: PatchField) {
    const set = typingFields.get(key);
    if (!set) return;
    if (field === undefined) typingFields.delete(key);
    else {
      set.delete(field);
      if (!set.size) typingFields.delete(key);
    }
  }
  function isTyping(key: string, field: PatchField): boolean {
    return !!typingFields.get(key)?.has(field);
  }
  function hasPendingPatch(key: string, field: PatchField): boolean {
    const track = patchTracks.get(key);
    return !!track?.fields[field]?.pending.size;
  }
  function noteDroppedRemote(key: string, field: PatchField, value: unknown) {
    const current = droppedRemote.value[key] ?? {};
    droppedRemote.value = { ...droppedRemote.value, [key]: { ...current, [field]: value } };
  }
  function acknowledgeDroppedRemote(key: string, field?: PatchField) {
    const forNode = droppedRemote.value[key];
    if (!forNode) return;
    if (field === undefined) {
      const next = { ...droppedRemote.value };
      delete next[key];
      droppedRemote.value = next;
      return;
    }
    if (!(field in forNode)) return;
    const nextForNode = { ...forNode };
    delete nextForNode[field];
    const next = { ...droppedRemote.value };
    if (Object.keys(nextForNode).length) next[key] = nextForNode;
    else delete next[key];
    droppedRemote.value = next;
  }
  const droppedRemoteFor = (key: string): Partial<Record<PatchField, unknown>> => droppedRemote.value[key] ?? {};

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
  /**
   * Derived positions (tidy layout) — never stored, never user-set. Depends only on membership,
   * order and parent pointers: those are read through the reactive list (so they are tracked),
   * then the layout runs over the raw objects to keep proxy overhead out of its inner loops.
   */
  const placed = computed(() => {
    const list = nodes.value;
    for (const n of list) void n.parent;
    return layoutTree(toRaw(list), {
      roots: roots.value.map(r => r.id),
      collapsed: useOstUiStore().collapsed,
    });
  });
  /** Descendant count of every node, computed once per structural change (O(n)). */
  const descendantCounts = computed(() => {
    const list = nodes.value;
    const kids = new Map<string, string[]>();
    for (const n of list) {
      if (!n.parent) continue;
      const bucket = kids.get(n.parent);
      if (bucket) bucket.push(n.id);
      else kids.set(n.parent, [n.id]);
    }
    const counts = new Map<string, number>();
    const visiting = new Set<string>();
    const count = (key: string): number => {
      const known = counts.get(key);
      if (known !== undefined) return known;
      if (visiting.has(key)) return 0; // corrupt (cyclic) data: never loop
      visiting.add(key);
      let total = 0;
      for (const child of kids.get(key) ?? []) total += 1 + count(child);
      visiting.delete(key);
      counts.set(key, total);
      return total;
    };
    for (const n of list) count(n.id);
    return counts;
  });
  const descendantCount = (key: string) => descendantCounts.value.get(key) ?? 0;
  const ancestors = (key: string) => breadcrumb(key, nodes.value);
  /**
   * A5 counter, derived from the nodes' createdDate (UTC month, as the server computes it) so it
   * follows creates, deletes and moves; the tree read's snapshot would go stale.
   */
  const evidenceThisMonth = computed(() => countEvidenceThisMonth(nodes.value));
  const memberByLogin = (login: string | null | undefined) => (login ? team.value?.members.find(m => m.login === login) : undefined);

  // ---- helpers -----------------------------------------------------------------------------------
  const fail = (err: unknown, fallback?: string, type?: NodeType) => {
    error.value = describeError(err, fallback, type);
  };
  const invalidateHistory = (key: string) => {
    historyGen.set(key, (historyGen.get(key) ?? 0) + 1);
    if (history.value[key]) {
      const next = { ...history.value };
      delete next[key];
      history.value = next;
    }
  };
  /**
   * Records a successful local write in the branch's product.lastActivity (what the server's next
   * tree read would report), so the dashboard's "Last edited … by …" follows in-session edits.
   */
  const touchBranch = (key: string, at?: string | null) => {
    const node = byId(key);
    const product = node?.type === 'product' ? node : breadcrumb(key, nodes.value)[0];
    if (product?.type !== 'product') return;
    const stamp = at ?? new Date().toISOString();
    const previous = product.lastActivity?.at ? Date.parse(product.lastActivity.at) : NaN;
    if (!Number.isNaN(previous) && previous > Date.parse(stamp)) return;
    product.lastActivity = { at: stamp, byLogin: team.value?.currentUserLogin ?? null };
  };
  /** A write the server recorded in the node's history: drop the cached history, touch the branch. */
  const recordWrite = (key: string, at?: string | null) => {
    invalidateHistory(key);
    touchBranch(key, at);
  };

  /** Drops a node and its subtree locally, with everything that refers to them (chat, history, view state). */
  function removeSubtree(key: string) {
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
  }

  /**
   * Surfaces a failed write about `keys` (the node, and for creates/moves the parent or target).
   * The server answers 403 for an unknown id (no existence leak), and a 409 concurrencyFailure when
   * a concurrent delete won; both can mean "someone else deleted it". Then the tree is re-read:
   * a node that is gone is removed here (deselected, panel closed) and the message says so;
   * otherwise the permission / conflict message stands. The re-read's permissions are applied too:
   * a user demoted to viewer meanwhile loses the edit controls at once, and is told why.
   */
  async function failOnNodes(err: unknown, keys: (string | null | undefined)[], fallback?: string, type?: NodeType) {
    fail(err, fallback, type);
    const status = httpStatus(err);
    if (status !== 403 && !(status === 409 && messageKey(err) === 'concurrencyFailure')) return;
    const id = teamId.value;
    if (id === null) return;
    const seq = loadSeq.value;
    let fresh: TeamTreeDTO;
    try {
      fresh = await api().getTree(id);
    } catch {
      return; // the team itself is out of reach (access revoked): the permission message stands
    }
    if (seq !== loadSeq.value || teamId.value !== id) return;
    let demoted = false;
    if (team.value) {
      const meta = toTeamMeta(fresh);
      demoted = team.value.canEdit && !meta.canEdit;
      team.value = { ...team.value, currentUserRole: meta.currentUserRole, canEdit: meta.canEdit, members: meta.members };
    }
    const alive = new Set((fresh.nodes ?? []).map(n => fromDto(n).id));
    const gone = keys.filter((k): k is string => !!k && !!byId(k) && !alive.has(k));
    for (const k of gone) if (byId(k)) removeSubtree(k);
    // Say why the edit controls just went away; otherwise, why the node did.
    if (demoted) error.value = DEMOTED_TO_VIEWER;
    else if (gone.length) error.value = DELETED_ELSEWHERE;
  }

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
    patchTracks.clear();
    moveSeqs.clear();
    historyGen.clear();
    typingFields.clear();
    droppedRemote.value = {};
    recentRequestIds.length = 0;
    recentRequestIdSet.clear();
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
  /**
   * Optimistic field patch; rolls back and surfaces an error when the server refuses.
   *
   * Patches are sequenced per node and field, so quick successive edits may resolve in any order:
   * a response or rollback only touches a field when no newer patch of that field is pending and
   * no newer response for it has been applied; a failed patch rolls back to the newest value still
   * pending, else to the last server-confirmed value.
   */
  async function patchNode(key: string, patch: NodePatch): Promise<boolean> {
    const node = byId(key);
    const parsed = parseKey(key);
    const fields = (Object.keys(patch) as PatchField[]).filter(f => PATCH_FIELDS.includes(f));
    if (!node || !parsed || !fields.length) return false;
    const seq = ++writeSeq;
    let track = patchTracks.get(key);
    if (!track) {
      track = { fields: {}, appliedSeq: 0, inflight: 0 };
      patchTracks.set(key, track);
    }
    track.inflight += 1;
    for (const f of fields) {
      let ft = track.fields[f];
      if (!ft?.pending.size) {
        // nothing of this field is in flight, so what is on screen is what the server has
        ft = { pending: new Map(), confirmed: node[f], confirmedSeq: ft?.confirmedSeq ?? 0, appliedSeq: ft?.appliedSeq ?? 0 };
        track.fields[f] = ft;
      }
      ft.pending.set(seq, patch[f]);
    }
    Object.assign(node, patch);
    const ownTrack = track;
    const live = () => patchTracks.get(key) === ownTrack;
    const requestId = generateRequestId();
    rememberRequestId(requestId);
    try {
      const dto = await api().patchNode(parsed.type, parsed.id, toPatchBody(patch), requestId);
      if (live()) settlePatch(key, ownTrack, seq, fields, fromDto(dto));
      recordWrite(key, dto.lastModifiedDate);
      return true;
    } catch (err) {
      if (live()) rollbackPatch(key, ownTrack, seq, fields);
      await failOnNodes(err, [key], undefined, node.type);
      return false;
    } finally {
      if (live() && --ownTrack.inflight === 0) patchTracks.delete(key);
    }
  }

  const newerPending = (ft: FieldTrack, seq: number) => [...ft.pending.keys()].some(s => s > seq);

  function settlePatch(key: string, track: NodeTrack, seq: number, fields: PatchField[], fresh: OstNode) {
    const newest = seq > track.appliedSeq;
    const changes: Partial<OstNode> = {};
    for (const f of PATCH_FIELDS) {
      const ft = track.fields[f];
      if (ft && fields.includes(f)) {
        ft.pending.delete(seq);
        if (seq > ft.confirmedSeq) {
          ft.confirmed = fresh[f];
          ft.confirmedSeq = seq;
        }
        if (!newerPending(ft, seq) && seq > ft.appliedSeq) {
          (changes as any)[f] = fresh[f];
          ft.appliedSeq = seq;
        }
      } else if (newest && (!ft || (!ft.pending.size && seq > ft.appliedSeq))) {
        // a field this patch did not send, with nothing of it in flight: the server copy wins
        (changes as any)[f] = fresh[f];
      }
    }
    if (newest) {
      track.appliedSeq = seq;
      changes.lastModifiedDate = fresh.lastModifiedDate;
      // Products carry lastActivity only on the tree read; keep ours when the write returns null.
      if (fresh.lastActivity) changes.lastActivity = fresh.lastActivity;
    }
    // parent/sortOrder (moveNode), links, questions and the comment count belong to their own
    // actions; a field patch does not change them, so they are left alone.
    const current = byId(key);
    if (current) Object.assign(current, changes);
  }

  function rollbackPatch(key: string, track: NodeTrack, seq: number, fields: PatchField[]) {
    const current = byId(key);
    for (const f of fields) {
      const ft = track.fields[f];
      if (!ft) continue;
      ft.pending.delete(seq);
      if (newerPending(ft, seq) || seq < ft.appliedSeq) continue;
      const older = [...ft.pending.keys()].sort((a, b) => b - a)[0];
      if (current) (current as any)[f] = older !== undefined ? ft.pending.get(older) : ft.confirmed;
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
    const requestId = generateRequestId();
    rememberRequestId(requestId);
    try {
      const dto = await api().createNode(
        {
          type: toApiType(childType),
          parentType: toApiType(parent.type),
          parentId: parsed.id,
          ...(title ? { title } : {}),
        },
        requestId,
      );
      const created = fromDto(dto);
      nodes.value = orderTree([...nodes.value.filter(n => n.id !== created.id), created]);
      const ui = useOstUiStore();
      if (ui.collapsed[parentKey]) ui.setCollapsed(parentKey, false);
      ui.select(created.id);
      ui.startEditing(created.id);
      // The server writes history for the new node only (the parent's history is unchanged).
      touchBranch(created.id, created.createdDate);
      return created.id;
    } catch (err) {
      await failOnNodes(err, [parentKey], 'The node could not be created.');
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
    const before = { parent: node.parent, sortOrder: node.sortOrder };
    const seq = ++writeSeq;
    moveSeqs.set(key, seq);
    const latest = () => moveSeqs.get(key) === seq;
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
    const requestId = generateRequestId();
    rememberRequestId(requestId);
    try {
      const res = await api().moveNode(
        {
          nodeType: toApiType(node.type),
          nodeId: parsed.id,
          parentType: target ? toApiType(target.type) : null,
          parentId: target ? target.dbId : null,
          ...(position !== undefined ? { position } : {}),
        },
        requestId,
      );
      const fresh = fromDto(res.node);
      const current = byId(key);
      if (current && latest()) {
        // Only the placement: a concurrent patch owns the editable fields.
        current.parent = fresh.parent;
        current.sortOrder = fresh.sortOrder;
        current.lastModifiedDate = fresh.lastModifiedDate;
      }
      for (const s of res.siblings ?? []) {
        const sib = byId(s.key);
        if (sib) sib.sortOrder = s.sortOrder;
      }
      nodes.value = orderTree(nodes.value);
      if (target) {
        const ui = useOstUiStore();
        if (ui.collapsed[target.id]) ui.setCollapsed(target.id, false);
      }
      recordWrite(key, fresh.lastModifiedDate);
      return true;
    } catch (err) {
      // Undo only this move's placement, on the current node objects (never a stale snapshot,
      // which could undo a patch that succeeded meanwhile), unless a newer move superseded it.
      const current = byId(key);
      if (current && latest()) {
        current.parent = before.parent;
        current.sortOrder = before.sortOrder;
        nodes.value = orderTree(nodes.value);
      }
      await failOnNodes(err, [key, targetKey], 'The node could not be moved.');
      return false;
    } finally {
      if (latest()) moveSeqs.delete(key);
    }
  }

  /** Deletes a node and (server-side) its subtree; removes the subtree locally after the 204. */
  async function deleteNode(key: string): Promise<boolean> {
    const node = byId(key);
    const parsed = parseKey(key);
    if (!node || !parsed) return false;
    const requestId = generateRequestId();
    rememberRequestId(requestId);
    try {
      await api().deleteNode(parsed.type, parsed.id, requestId);
    } catch (err) {
      await failOnNodes(err, [key], 'The node could not be deleted.');
      return false;
    }
    if (node.type !== 'product') touchBranch(key);
    removeSubtree(key);
    return true;
  }

  // ---- actions: links --------------------------------------------------------------------------
  async function addLink(key: string, link: { name: string; url: string }): Promise<boolean> {
    const node = byId(key);
    const parsed = parseKey(key);
    if (!node || !parsed) return false;
    const requestId = generateRequestId();
    rememberRequestId(requestId);
    try {
      const dto = await api().addLink(parsed.type, parsed.id, link, requestId);
      byId(key)?.links.push({ id: dto.id, name: dto.name, url: dto.url });
      recordWrite(key);
      return true;
    } catch (err) {
      await failOnNodes(err, [key], 'The link could not be added.');
      return false;
    }
  }

  async function updateLink(key: string, linkId: number, patch: { name?: string; url?: string }): Promise<boolean> {
    const link = byId(key)?.links.find(l => l.id === linkId);
    if (!link) return false;
    const snapshot = { name: link.name, url: link.url };
    Object.assign(link, patch);
    const requestId = generateRequestId();
    rememberRequestId(requestId);
    try {
      const dto = await api().updateLink(linkId, patch, requestId);
      Object.assign(link, { name: dto.name, url: dto.url });
      return true;
    } catch (err) {
      Object.assign(link, snapshot);
      await failOnNodes(err, [key], 'The link could not be saved.');
      return false;
    }
  }

  async function removeLink(key: string, linkId: number): Promise<boolean> {
    const node = byId(key);
    const i = node?.links.findIndex(l => l.id === linkId) ?? -1;
    if (!node || i < 0) return false;
    const [removed] = node.links.splice(i, 1);
    const requestId = generateRequestId();
    rememberRequestId(requestId);
    try {
      await api().deleteLink(linkId, requestId);
      recordWrite(key);
      return true;
    } catch (err) {
      node.links.splice(i, 0, removed);
      await failOnNodes(err, [key], 'The link could not be removed.');
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
    const requestId = generateRequestId();
    rememberRequestId(requestId);
    try {
      const dto = await api().addQuestion(node.dbId, text, requestId);
      byId(key)?.questions.push({ id: dto.id, text: dto.text, done: !!dto.done });
      recordWrite(key);
      return true;
    } catch (err) {
      await failOnNodes(err, [key], 'The question could not be added.');
      return false;
    }
  }

  async function updateQuestion(key: string, questionId: number, patch: { text?: string; done?: boolean }): Promise<boolean> {
    const question = byId(key)?.questions.find(q => q.id === questionId);
    if (!question) return false;
    const snapshot = { text: question.text, done: question.done };
    Object.assign(question, patch);
    const requestId = generateRequestId();
    rememberRequestId(requestId);
    try {
      const dto = await api().updateQuestion(questionId, patch, requestId);
      Object.assign(question, { text: dto.text, done: !!dto.done });
      return true;
    } catch (err) {
      Object.assign(question, snapshot);
      await failOnNodes(err, [key], 'The question could not be saved.');
      return false;
    }
  }

  async function removeQuestion(key: string, questionId: number): Promise<boolean> {
    const node = byId(key);
    const i = node?.questions.findIndex(q => q.id === questionId) ?? -1;
    if (!node || i < 0) return false;
    const [removed] = node.questions.splice(i, 1);
    const requestId = generateRequestId();
    rememberRequestId(requestId);
    try {
      await api().deleteQuestion(questionId, requestId);
      return true;
    } catch (err) {
      node.questions.splice(i, 0, removed);
      await failOnNodes(err, [key], 'The question could not be removed.');
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
    const requestId = generateRequestId();
    rememberRequestId(requestId);
    try {
      const dto = await api().addComment(parsed.type, parsed.id, body, requestId);
      comments.value = { ...comments.value, [key]: [...(comments.value[key] ?? []), dto] };
      const node = byId(key);
      if (node) node.commentCount += 1;
      recordWrite(key);
      return dto;
    } catch (err) {
      await failOnNodes(err, [key], 'The message could not be sent.');
      return null;
    }
  }

  async function editComment(key: string, commentId: number, body: string): Promise<boolean> {
    const list = comments.value[key] ?? [];
    const i = list.findIndex(c => c.id === commentId);
    if (i < 0) return false;
    const snapshot = list[i];
    list.splice(i, 1, { ...snapshot, body });
    const requestId = generateRequestId();
    rememberRequestId(requestId);
    try {
      const dto = await api().updateComment(commentId, body, requestId);
      const j = (comments.value[key] ?? []).findIndex(c => c.id === commentId);
      if (j >= 0) comments.value[key].splice(j, 1, dto);
      return true;
    } catch (err) {
      const j = (comments.value[key] ?? []).findIndex(c => c.id === commentId);
      if (j >= 0) comments.value[key].splice(j, 1, snapshot);
      await failOnNodes(err, [key], 'The message could not be edited.');
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
    const requestId = generateRequestId();
    rememberRequestId(requestId);
    try {
      await api().deleteComment(commentId, requestId);
      recordWrite(key);
      return true;
    } catch (err) {
      // Re-insert only when the list does not have it (a reload meanwhile may already show it again).
      const current = comments.value[key];
      if (current && !current.some(c => c.id === commentId)) {
        current.splice(Math.min(i, current.length), 0, removed);
        const still = byId(key);
        if (still) still.commentCount += 1;
      }
      await failOnNodes(err, [key], 'The message could not be deleted.');
      return false;
    }
  }

  // ---- actions: history ------------------------------------------------------------------------
  /**
   * Reads a node's history (newest first) into the cache. When a write of this node lands while
   * the read is in flight, the read may predate it, so it is repeated (a few times at most).
   */
  async function loadHistory(key: string): Promise<HistoryEntryDTO[]> {
    const parsed = parseKey(key);
    if (!parsed || parsed.type === 'product') return [];
    try {
      let list: HistoryEntryDTO[] = [];
      for (let attempt = 0; attempt < 3; attempt++) {
        const gen = historyGen.get(key) ?? 0;
        list = await api().listHistory(parsed.type, parsed.id);
        if ((historyGen.get(key) ?? 0) === gen) break;
      }
      if (!byId(key)) return list;
      history.value = { ...history.value, [key]: list };
      return list;
    } catch (err) {
      fail(err, 'The history could not be loaded.');
      return history.value[key] ?? [];
    }
  }

  // ---- actions: realtime -------------------------------------------------------------------

  /**
   * Applies a batch of server change events to the flat store (RTC-005). Every event that carries
   * this client's requestId is dropped (echo suppression, FR-032). NODE_UPDATED keeps any field a
   * local patch is in flight for, or the user is currently typing in, and records the dropped
   * remote value so the panel can surface it via the existing dirty-field mechanism (FR-033).
   * A remote delete of a node with a local edit in flight reuses the "deleted by someone else"
   * message that the store already shows after a rejected write.
   */
  function applyEvents(events: readonly (OstTreeEvent | WireTreeEvent)[]): void {
    for (const raw of events) {
      const event = normalizeEvent(raw as WireTreeEvent);
      if (!event) continue;
      if (isOwnEcho(event)) continue;
      switch (event.type) {
        case 'NODE_CREATED':
          applyNodeUpsert(event.node, true);
          break;
        case 'NODE_UPDATED':
          applyNodeUpsert(event.node, false);
          break;
        case 'NODE_MOVED':
          applyNodeMoved(event.node, event.siblings ?? []);
          break;
        case 'NODE_DELETED':
          applyNodeDeleted(event.key);
          break;
        case 'LINK_ADDED':
          applyLinkUpsert(event.key, event.link, true);
          break;
        case 'LINK_UPDATED':
          applyLinkUpsert(event.key, event.link, false);
          break;
        case 'LINK_REMOVED':
          applyLinkRemoved(event.key, event.linkId);
          break;
        case 'QUESTION_ADDED':
          applyQuestionUpsert(event.key, event.question, true);
          break;
        case 'QUESTION_UPDATED':
          applyQuestionUpsert(event.key, event.question, false);
          break;
        case 'QUESTION_REMOVED':
          applyQuestionRemoved(event.key, event.questionId);
          break;
        case 'COMMENT_ADDED':
          applyCommentUpsert(event.key, event.comment, event.commentCount, true);
          break;
        case 'COMMENT_UPDATED':
          applyCommentUpsert(event.key, event.comment, event.commentCount, false);
          break;
        case 'COMMENT_DELETED':
          applyCommentDeleted(event.key, event.commentId, event.commentCount);
          break;
      }
    }
  }

  function applyNodeUpsert(dtoNode: TreeNodeDTO, isCreate: boolean) {
    const fresh = fromDto(dtoNode);
    const current = byId(fresh.id);
    if (!current) {
      if (isCreate) {
        nodes.value = orderTree([...nodes.value, fresh]);
      }
      return;
    }
    // Fields the user has a patch pending for, or is currently typing in, are held locally.
    for (const f of PATCH_FIELDS) {
      const held = hasPendingPatch(fresh.id, f) || isTyping(fresh.id, f);
      const remote = fresh[f];
      if (held) {
        if ((current as any)[f] !== remote) noteDroppedRemote(fresh.id, f, remote);
        continue;
      }
      if ((current as any)[f] !== remote) (current as any)[f] = remote;
    }
    // Structural bookkeeping: parent/sortOrder belong to NODE_MOVED, not NODE_UPDATED.
    current.commentCount = fresh.commentCount;
    current.links = fresh.links;
    current.questions = fresh.questions;
    current.lastModifiedDate = fresh.lastModifiedDate;
    if (fresh.lastActivity) current.lastActivity = fresh.lastActivity;
    if (fresh.createdDate && !current.createdDate) current.createdDate = fresh.createdDate;
  }

  function applyNodeMoved(dtoNode: TreeNodeDTO, siblings: { key: string; sortOrder: number }[]) {
    const fresh = fromDto(dtoNode);
    const current = byId(fresh.id);
    if (!current) {
      nodes.value = orderTree([...nodes.value, fresh]);
    } else {
      current.parent = fresh.parent;
      current.sortOrder = fresh.sortOrder;
      current.lastModifiedDate = fresh.lastModifiedDate;
    }
    for (const s of siblings) {
      const sib = byId(s.key);
      if (sib) sib.sortOrder = s.sortOrder;
    }
    nodes.value = orderTree(nodes.value);
  }

  function applyNodeDeleted(key: string) {
    if (!byId(key)) return;
    const hadLocalEdit = patchTracks.has(key) || moveSeqs.has(key) || (typingFields.get(key)?.size ?? 0) > 0;
    removeSubtree(key);
    if (hadLocalEdit) error.value = DELETED_ELSEWHERE;
  }

  function applyLinkUpsert(key: string, link: NodeLinkDTO, isAdd: boolean) {
    const node = byId(key);
    if (!node) return;
    const i = node.links.findIndex(l => l.id === link.id);
    const value = { id: link.id, name: link.name, url: link.url };
    if (i >= 0) node.links.splice(i, 1, value);
    else if (isAdd) node.links.push(value);
    else node.links.push(value);
  }

  function applyLinkRemoved(key: string, linkId: number) {
    const node = byId(key);
    if (!node) return;
    const i = node.links.findIndex(l => l.id === linkId);
    if (i >= 0) node.links.splice(i, 1);
  }

  function applyQuestionUpsert(key: string, question: OpenQuestionDTO, isAdd: boolean) {
    const node = byId(key);
    if (!node) return;
    const i = node.questions.findIndex(q => q.id === question.id);
    const value = { id: question.id, text: question.text, done: !!question.done };
    if (i >= 0) node.questions.splice(i, 1, value);
    else if (isAdd) node.questions.push(value);
    else node.questions.push(value);
  }

  function applyQuestionRemoved(key: string, questionId: number) {
    const node = byId(key);
    if (!node) return;
    const i = node.questions.findIndex(q => q.id === questionId);
    if (i >= 0) node.questions.splice(i, 1);
  }

  function applyCommentUpsert(key: string, dto: CommentDTO, count: number | undefined, isAdd: boolean) {
    const node = byId(key);
    if (!node) return;
    const list = comments.value[key];
    if (list) {
      const i = list.findIndex(c => c.id === dto.id);
      const next = list.slice();
      if (i >= 0) next.splice(i, 1, dto);
      else if (isAdd) next.push(dto);
      else next.push(dto);
      comments.value = { ...comments.value, [key]: next };
    }
    node.commentCount = count !== undefined ? count : list ? list.length : node.commentCount + (isAdd ? 1 : 0);
  }

  function applyCommentDeleted(key: string, commentId: number, count: number | undefined) {
    const node = byId(key);
    if (!node) return;
    const list = comments.value[key];
    if (list) {
      const i = list.findIndex(c => c.id === commentId);
      if (i >= 0) {
        const next = list.slice();
        next.splice(i, 1);
        comments.value = { ...comments.value, [key]: next };
      }
    }
    node.commentCount = count !== undefined ? count : Math.max(0, node.commentCount - 1);
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
    droppedRemote,
    // getters
    droppedRemoteFor,
    byId,
    childrenOf,
    canEdit,
    products,
    selected,
    roots,
    placed,
    descendantCount,
    ancestors,
    evidenceThisMonth,
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
    applyEvents,
    markTyping,
    clearTyping,
    acknowledgeDroppedRemote,
  };
});
