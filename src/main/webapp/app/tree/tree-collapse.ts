import type { IOpportunityTreeNode, IOutcomeTreeNode, IProductTreeNode, ITeamTree, TreeNodeType } from './tree.model';

/** A node key `<type>:<id>` — same format as `tree-layout.nodeKey`. */
export type CollapseKey = string;

export const collapseKey = (type: TreeNodeType, id: number): CollapseKey => `${type}:${id}`;

const STORAGE_PREFIX = 'ombuto.tree.collapse';

/** Compute the localStorage key for a given (userLogin, teamId) pair. */
export const storageKeyFor = (userLogin: string | null | undefined, teamId: number | null | undefined): string => {
  const u = userLogin && userLogin.length > 0 ? userLogin : 'anonymous';
  const t = teamId != null ? String(teamId) : 'none';
  return `${STORAGE_PREFIX}.${u}.${t}`;
};

/** Enumerate every node key present in the tree. Used to prune stored ids that no longer exist. */
export const collectAllKeys = (tree: ITeamTree | null): Set<CollapseKey> => {
  const out = new Set<CollapseKey>();
  if (!tree) return out;
  const walkOpp = (opp: IOpportunityTreeNode) => {
    out.add(collapseKey('opportunity', opp.id));
    for (const c of opp.children ?? []) walkOpp(c);
    for (const s of opp.solutions ?? []) out.add(collapseKey('solution', s.id));
  };
  for (const p of tree.products ?? []) {
    out.add(collapseKey('product', p.id));
    for (const o of p.outcomes ?? []) {
      out.add(collapseKey('outcome', o.id));
      for (const op of o.opportunities ?? []) walkOpp(op);
    }
  }
  return out;
};

/** Return a new Set containing only keys that exist in the tree. */
export const pruneKeys = (stored: Iterable<CollapseKey>, tree: ITeamTree | null): Set<CollapseKey> => {
  const valid = collectAllKeys(tree);
  const out = new Set<CollapseKey>();
  for (const k of stored) if (valid.has(k)) out.add(k);
  return out;
};

/** Minimal Storage interface — matches window.localStorage. */
export interface CollapseStorage {
  getItem(key: string): string | null;
  setItem(key: string, value: string): void;
  removeItem?(key: string): void;
}

/**
 * Return `window.localStorage` if it's usable, otherwise null. "Usable" means
 * getItem + setItem don't throw when exercised. Any DOMException (SecurityError,
 * QuotaExceededError, private-mode restrictions) drops us back to in-memory.
 */
export const detectStorage = (): CollapseStorage | null => {
  try {
    if (typeof window === 'undefined' || !window.localStorage) return null;
    const probeKey = '__ombuto_collapse_probe__';
    window.localStorage.setItem(probeKey, '1');
    window.localStorage.removeItem(probeKey);
    return window.localStorage;
  } catch {
    return null;
  }
};

const readRaw = (storage: CollapseStorage | null, key: string): CollapseKey[] => {
  if (!storage) return [];
  try {
    const raw = storage.getItem(key);
    if (!raw) return [];
    const parsed = JSON.parse(raw);
    if (!Array.isArray(parsed)) return [];
    return parsed.filter((v): v is string => typeof v === 'string');
  } catch {
    return [];
  }
};

const writeRaw = (storage: CollapseStorage | null, key: string, keys: Iterable<CollapseKey>): boolean => {
  if (!storage) return false;
  try {
    storage.setItem(key, JSON.stringify(Array.from(keys)));
    return true;
  } catch {
    return false;
  }
};

/**
 * Per (user, team) collapse state. Loads from localStorage on construction (pruning
 * dead ids), writes back on every mutation. If localStorage is unavailable OR any
 * read/write throws, everything degrades silently to in-memory only.
 */
export class CollapseState {
  private readonly storage: CollapseStorage | null;
  private readonly key: string;
  private readonly collapsed: Set<CollapseKey>;

  constructor(opts: {
    userLogin: string | null | undefined;
    teamId: number | null;
    tree: ITeamTree | null;
    storage?: CollapseStorage | null;
  }) {
    this.storage = opts.storage !== undefined ? opts.storage : detectStorage();
    this.key = storageKeyFor(opts.userLogin, opts.teamId);
    const stored = readRaw(this.storage, this.key);
    this.collapsed = pruneKeys(stored, opts.tree);
    // Persist the pruned result so dead ids don't linger.
    if (this.collapsed.size !== stored.length) writeRaw(this.storage, this.key, this.collapsed);
  }

  keys(): Set<CollapseKey> {
    return new Set(this.collapsed);
  }

  isCollapsed(type: TreeNodeType, id: number): boolean {
    return this.collapsed.has(collapseKey(type, id));
  }

  collapse(type: TreeNodeType, id: number): void {
    this.collapsed.add(collapseKey(type, id));
    writeRaw(this.storage, this.key, this.collapsed);
  }

  expand(type: TreeNodeType, id: number): void {
    if (this.collapsed.delete(collapseKey(type, id))) {
      writeRaw(this.storage, this.key, this.collapsed);
    }
  }

  toggle(type: TreeNodeType, id: number): boolean {
    if (this.isCollapsed(type, id)) {
      this.expand(type, id);
      return false;
    }
    this.collapse(type, id);
    return true;
  }

  /** Re-prune against a (possibly changed) tree; persists if anything was removed. */
  reconcile(tree: ITeamTree | null): void {
    const valid = collectAllKeys(tree);
    let changed = false;
    for (const k of Array.from(this.collapsed)) {
      if (!valid.has(k)) {
        this.collapsed.delete(k);
        changed = true;
      }
    }
    if (changed) writeRaw(this.storage, this.key, this.collapsed);
  }

  /** True when this state is backed by real storage (false = in-memory fallback). */
  get persistent(): boolean {
    return this.storage !== null;
  }

  /** For tests / debugging. */
  get storageKey(): string {
    return this.key;
  }
}

/** Descendant count helper — used by the layout to surface the hidden count on a collapsed node. */
export const countDescendantsInTree = (type: TreeNodeType, node: unknown): number => {
  let n = 0;
  if (type === 'product') {
    for (const o of (node as IProductTreeNode).outcomes ?? []) n += 1 + countDescendantsInTree('outcome', o);
  } else if (type === 'outcome') {
    for (const o of (node as IOutcomeTreeNode).opportunities ?? []) n += 1 + countDescendantsInTree('opportunity', o);
  } else if (type === 'opportunity') {
    for (const c of (node as IOpportunityTreeNode).children ?? []) n += 1 + countDescendantsInTree('opportunity', c);
    for (const s of (node as IOpportunityTreeNode).solutions ?? []) n += 1 + countDescendantsInTree('solution', s);
  }
  return n;
};
