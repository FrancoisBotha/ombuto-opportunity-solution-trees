import { defineStore } from 'pinia';

import type { IOpportunityTreeNode, IOutcomeTreeNode, IProductTreeNode, ITeamTree, TreeNode, TreeNodeType } from './tree.model';
import TreeService, { type CreateChildInput, type CreateProductInput, type MoveNodeResponse } from './tree.service';

export interface MoveToast {
  showError: (message: string) => void;
}

export type TreeLoadError = 'forbidden' | 'not-found' | 'unknown';

interface NodeIndexEntry {
  type: TreeNodeType;
  node: TreeNode;
  parent: TreeNode | ITeamTree | null;
}

export type TreeWriteError = 'forbidden' | 'validation' | 'unknown';

export interface TreeState {
  teamId: number | null;
  tree: ITeamTree | null;
  loading: boolean;
  loaded: boolean;
  error: TreeLoadError | null;
  errorMessage: string | null;
  writeError: TreeWriteError | null;
  writeErrorMessage: string | null;
  selectedNodeType: TreeNodeType | null;
  selectedNodeId: number | null;
  focusedProductId: number | null;
}

const walkOpportunities = (opps: IOpportunityTreeNode[], parent: TreeNode | ITeamTree, index: Map<string, NodeIndexEntry>): void => {
  for (const opp of opps) {
    index.set(key('opportunity', opp.id), { type: 'opportunity', node: opp, parent });
    for (const sol of opp.solutions ?? []) {
      index.set(key('solution', sol.id), { type: 'solution', node: sol, parent: opp });
    }
    if (opp.children && opp.children.length > 0) {
      walkOpportunities(opp.children, opp, index);
    }
  }
};

const buildIndex = (tree: ITeamTree | null): Map<string, NodeIndexEntry> => {
  const index = new Map<string, NodeIndexEntry>();
  if (!tree) return index;
  for (const product of tree.products ?? []) {
    index.set(key('product', product.id), { type: 'product', node: product, parent: tree });
    for (const outcome of product.outcomes ?? []) {
      index.set(key('outcome', outcome.id), { type: 'outcome', node: outcome, parent: product });
      walkOpportunities(outcome.opportunities ?? [], outcome, index);
    }
  }
  return index;
};

const key = (type: TreeNodeType, id: number): string => `${type}:${id}`;

const childrenArrayFor = (parent: TreeNode | ITeamTree, childType: TreeNodeType): TreeNode[] | null => {
  if (childType === 'product' && parent && (parent as ITeamTree).products) {
    return (parent as ITeamTree).products as unknown as TreeNode[];
  }
  if (childType === 'outcome' && (parent as IProductTreeNode).outcomes) {
    return (parent as IProductTreeNode).outcomes as unknown as TreeNode[];
  }
  if (childType === 'opportunity') {
    if ((parent as IOutcomeTreeNode).opportunities) {
      return (parent as IOutcomeTreeNode).opportunities as unknown as TreeNode[];
    }
    if ((parent as IOpportunityTreeNode).children) {
      return (parent as IOpportunityTreeNode).children as unknown as TreeNode[];
    }
  }
  if (childType === 'solution' && (parent as IOpportunityTreeNode).solutions) {
    return (parent as IOpportunityTreeNode).solutions as unknown as TreeNode[];
  }
  return null;
};

const countDescendants = (type: TreeNodeType, node: TreeNode): number => {
  let count = 0;
  if (type === 'product') {
    for (const o of (node as IProductTreeNode).outcomes ?? []) {
      count += 1 + countDescendants('outcome', o);
    }
  } else if (type === 'outcome') {
    for (const o of (node as IOutcomeTreeNode).opportunities ?? []) {
      count += 1 + countDescendants('opportunity', o);
    }
  } else if (type === 'opportunity') {
    for (const c of (node as IOpportunityTreeNode).children ?? []) {
      count += 1 + countDescendants('opportunity', c);
    }
    for (const s of (node as IOpportunityTreeNode).solutions ?? []) {
      count += 1 + countDescendants('solution', s);
    }
  }
  return count;
};

export const useTreeStore = defineStore('tree', {
  state: (): TreeState => ({
    teamId: null,
    tree: null,
    loading: false,
    loaded: false,
    error: null,
    errorMessage: null,
    writeError: null,
    writeErrorMessage: null,
    selectedNodeType: null,
    selectedNodeId: null,
    focusedProductId: null,
  }),
  getters: {
    canEdit: state => (state.tree ? !!state.tree.canEdit : false),
    products: state => (state.tree ? state.tree.products : []),
    nodeIndex(state): Map<string, NodeIndexEntry> {
      return buildIndex(state.tree);
    },
    findNode() {
      return (type: TreeNodeType, id: number): TreeNode | null => {
        const entry = this.nodeIndex.get(key(type, id));
        return entry ? entry.node : null;
      };
    },
    childrenOf() {
      return (type: TreeNodeType, id: number): TreeNode[] => {
        const node = this.findNode(type, id);
        if (!node) return [];
        if (type === 'product') return [...((node as IProductTreeNode).outcomes ?? [])] as TreeNode[];
        if (type === 'outcome') return [...((node as IOutcomeTreeNode).opportunities ?? [])] as TreeNode[];
        if (type === 'opportunity') {
          return [...((node as IOpportunityTreeNode).children ?? []), ...((node as IOpportunityTreeNode).solutions ?? [])] as TreeNode[];
        }
        return [];
      };
    },
    descendantCountOf() {
      return (type: TreeNodeType, id: number): number => {
        const node = this.findNode(type, id);
        if (!node) return 0;
        return countDescendants(type, node);
      };
    },
  },
  actions: {
    async load(teamId: number, service?: TreeService): Promise<void> {
      const svc = service ?? new TreeService();
      this.teamId = teamId;
      this.loading = true;
      this.loaded = false;
      this.error = null;
      this.errorMessage = null;
      try {
        const tree = await svc.getTeamTree(teamId);
        this.tree = tree;
        this.loaded = true;
      } catch (err: any) {
        const status = err?.response?.status;
        this.tree = null;
        if (status === 403) {
          this.error = 'forbidden';
          this.errorMessage = 'You do not have access to this team.';
        } else if (status === 404) {
          this.error = 'not-found';
          this.errorMessage = 'This team does not exist.';
        } else {
          this.error = 'unknown';
          this.errorMessage = 'Failed to load the team tree.';
        }
      } finally {
        this.loading = false;
      }
    },
    setTree(tree: ITeamTree | null) {
      this.tree = tree;
      this.loaded = tree !== null;
      this.error = null;
      this.errorMessage = null;
    },
    reset() {
      this.teamId = null;
      this.tree = null;
      this.loading = false;
      this.loaded = false;
      this.error = null;
      this.errorMessage = null;
      this.writeError = null;
      this.writeErrorMessage = null;
      this.selectedNodeType = null;
      this.selectedNodeId = null;
      this.focusedProductId = null;
    },
    clearWriteError() {
      this.writeError = null;
      this.writeErrorMessage = null;
    },
    selectNode(type: TreeNodeType, id: number) {
      this.selectedNodeType = type;
      this.selectedNodeId = id;
    },
    clearSelection() {
      this.selectedNodeType = null;
      this.selectedNodeId = null;
    },
    focusProduct(productId: number) {
      this.focusedProductId = productId;
      if (this.selectedNodeType && this.selectedNodeId != null) {
        // Clear a selection that no longer sits under the focused product.
        const idx = buildIndex(this.tree);
        if (!idx.get(key(this.selectedNodeType, this.selectedNodeId))) {
          this.clearSelection();
        }
      }
    },
    clearFocus() {
      this.focusedProductId = null;
    },
    /** Insert a node under the given parent (append last). */
    insertNode(parentType: TreeNodeType | 'team', parentId: number | null, childType: TreeNodeType, child: TreeNode): boolean {
      if (!this.tree) return false;
      let parent: TreeNode | ITeamTree | null;
      if (parentType === 'team') {
        parent = this.tree;
      } else {
        const entry = buildIndex(this.tree).get(key(parentType, parentId as number));
        parent = entry ? entry.node : null;
      }
      if (!parent) return false;
      const arr = childrenArrayFor(parent, childType);
      if (!arr) return false;
      arr.push(child);
      return true;
    },
    /** Replace a node in place with an updated version (must be same type and id). */
    replaceNode(type: TreeNodeType, id: number, updated: TreeNode): boolean {
      if (!this.tree) return false;
      const entry = buildIndex(this.tree).get(key(type, id));
      if (!entry || !entry.parent) return false;
      const arr = childrenArrayFor(entry.parent, type);
      if (!arr) return false;
      const idx = arr.findIndex(n => (n as { id: number }).id === id);
      if (idx < 0) return false;
      arr.splice(idx, 1, updated);
      return true;
    },
    /** Remove a node (and its descendants) from the tree. */
    removeNode(type: TreeNodeType, id: number): boolean {
      if (!this.tree) return false;
      const entry = buildIndex(this.tree).get(key(type, id));
      if (!entry || !entry.parent) return false;
      const arr = childrenArrayFor(entry.parent, type);
      if (!arr) return false;
      const idx = arr.findIndex(n => (n as { id: number }).id === id);
      if (idx < 0) return false;
      arr.splice(idx, 1);
      return true;
    },
    /** Create a new top-level product via the API and append it to the tree. */
    async addProduct(input: CreateProductInput, service?: TreeService): Promise<IProductTreeNode | null> {
      if (!this.tree || this.teamId == null) return null;
      const svc = service ?? new TreeService();
      this.clearWriteError();
      try {
        const created = await svc.createProduct(this.teamId, input);
        this.insertNode('team', null, 'product', created);
        return created;
      } catch (err: any) {
        this.recordWriteError(err);
        return null;
      }
    },
    /** Create a child node under the given parent via the API and append it in the store. */
    async addChild(
      parentType: TreeNodeType,
      parentId: number,
      childType: TreeNodeType,
      input: CreateChildInput,
      service?: TreeService,
    ): Promise<TreeNode | null> {
      if (!this.tree) return null;
      const svc = service ?? new TreeService();
      this.clearWriteError();
      try {
        let created: TreeNode | null = null;
        if (parentType === 'product' && childType === 'outcome') {
          created = await svc.createOutcome(parentId, input);
        } else if (parentType === 'outcome' && childType === 'opportunity') {
          created = await svc.createOpportunityUnderOutcome(parentId, input);
        } else if (parentType === 'opportunity' && childType === 'opportunity') {
          const outcomeId = this.enclosingOutcomeId(parentId);
          if (outcomeId == null) return null;
          created = await svc.createOpportunityUnderOpportunity(parentId, outcomeId, input);
        } else if (parentType === 'opportunity' && childType === 'solution') {
          created = await svc.createSolution(parentId, input);
        } else {
          this.writeError = 'validation';
          this.writeErrorMessage = `Cannot add a ${childType} under a ${parentType}.`;
          return null;
        }
        if (created) {
          this.insertNode(parentType, parentId, childType, created);
        }
        return created;
      } catch (err: any) {
        this.recordWriteError(err);
        return null;
      }
    },
    /**
     * Delete a node via the API and remove it (with descendants) from the store.
     * If the node was selected or was the focused product, clears that state.
     */
    async deleteNode(type: TreeNodeType, id: number, service?: TreeService): Promise<boolean> {
      if (!this.tree) return false;
      const svc = service ?? new TreeService();
      this.clearWriteError();
      try {
        await svc.deleteNode(type, id);
      } catch (err: any) {
        this.recordWriteError(err);
        return false;
      }
      // Collect descendant keys BEFORE mutating so we can clear a selection sitting under this subtree.
      const doomed = new Set<string>();
      doomed.add(key(type, id));
      const node = this.findNode(type, id);
      if (node) {
        const collect = (t: TreeNodeType, n: TreeNode) => {
          if (t === 'product') {
            for (const o of (n as IProductTreeNode).outcomes ?? []) {
              doomed.add(key('outcome', o.id));
              collect('outcome', o);
            }
          } else if (t === 'outcome') {
            for (const o of (n as IOutcomeTreeNode).opportunities ?? []) {
              doomed.add(key('opportunity', o.id));
              collect('opportunity', o);
            }
          } else if (t === 'opportunity') {
            for (const c of (n as IOpportunityTreeNode).children ?? []) {
              doomed.add(key('opportunity', c.id));
              collect('opportunity', c);
            }
            for (const s of (n as IOpportunityTreeNode).solutions ?? []) {
              doomed.add(key('solution', s.id));
            }
          }
        };
        collect(type, node);
      }
      this.removeNode(type, id);
      if (type === 'product' && this.focusedProductId === id) {
        this.focusedProductId = null;
      }
      if (this.selectedNodeType && this.selectedNodeId != null) {
        if (doomed.has(key(this.selectedNodeType, this.selectedNodeId))) {
          this.clearSelection();
        }
      }
      return true;
    },
    /**
     * Move a node to a new parent (re-parent). Applies the move optimistically,
     * calls the server, and rolls back the tree on failure. On error, sets writeError
     * and (if provided) surfaces the server's message via the toast callback.
     */
    async moveNode(
      type: TreeNodeType,
      id: number,
      newParentType: TreeNodeType,
      newParentId: number,
      options: { service?: TreeService; toast?: MoveToast } = {},
    ): Promise<boolean> {
      if (!this.tree) return false;
      const svc = options.service ?? new TreeService();
      this.clearWriteError();
      const snapshot: ITeamTree = JSON.parse(JSON.stringify(this.tree));
      const entry = buildIndex(this.tree).get(key(type, id));
      if (!entry || !entry.parent) return false;
      const targetEntry = buildIndex(this.tree).get(key(newParentType, newParentId));
      if (!targetEntry) return false;
      // Detach from current parent
      const fromArr = childrenArrayFor(entry.parent, type);
      if (!fromArr) return false;
      const idx = fromArr.findIndex(n => (n as { id: number }).id === id);
      if (idx < 0) return false;
      const [moved] = fromArr.splice(idx, 1);
      // Attach to new parent
      const toArr = childrenArrayFor(targetEntry.node, type);
      if (!toArr) {
        // Restore before returning false
        fromArr.splice(idx, 0, moved);
        return false;
      }
      // Update parentId / outcome reference bookkeeping for opportunities.
      if (type === 'opportunity') {
        if (newParentType === 'outcome') {
          (moved as IOpportunityTreeNode).parentId = null;
        } else if (newParentType === 'opportunity') {
          (moved as IOpportunityTreeNode).parentId = newParentId;
        }
      }
      const appendPosition = toArr.length;
      (moved as { sortOrder?: number | null }).sortOrder = appendPosition;
      toArr.push(moved);
      // Renumber siblings in the destination.
      toArr.forEach((n, i) => {
        (n as { sortOrder?: number | null }).sortOrder = i;
      });
      // Renumber siblings in the source (only if different parent).
      fromArr.forEach((n, i) => {
        (n as { sortOrder?: number | null }).sortOrder = i;
      });
      try {
        const response = await svc.moveNode({
          nodeType: type,
          nodeId: id,
          parentType: newParentType,
          parentId: newParentId,
          position: appendPosition,
        });
        this._applyMoveResponse(response);
        return true;
      } catch (err: any) {
        // Roll back to the snapshot.
        this.tree = snapshot;
        const status = err?.response?.status;
        const body = err?.response?.data;
        const serverMessage =
          (body && (body.detail || body.title || body.message)) ||
          (status === 403 ? 'You do not have permission to modify this tree.' : 'The move was rejected by the server.');
        if (status === 403) this.writeError = 'forbidden';
        else if (status === 400) this.writeError = 'validation';
        else this.writeError = 'unknown';
        this.writeErrorMessage = String(serverMessage);
        if (options.toast) options.toast.showError(String(serverMessage));
        return false;
      }
    },
    /**
     * Move a node up or down among its siblings (products move left/right along the top row).
     * Persists the new order via the same move endpoint. No-op at the ends.
     */
    async reorderSibling(
      type: TreeNodeType,
      id: number,
      direction: -1 | 1,
      options: { service?: TreeService; toast?: MoveToast } = {},
    ): Promise<boolean> {
      if (!this.tree) return false;
      const svc = options.service ?? new TreeService();
      this.clearWriteError();
      const snapshot: ITeamTree = JSON.parse(JSON.stringify(this.tree));
      const entry = buildIndex(this.tree).get(key(type, id));
      if (!entry || !entry.parent) return false;
      const arr = childrenArrayFor(entry.parent, type);
      if (!arr) return false;
      const idx = arr.findIndex(n => (n as { id: number }).id === id);
      if (idx < 0) return false;
      const newIdx = idx + direction;
      if (newIdx < 0 || newIdx >= arr.length) return false;
      const [node] = arr.splice(idx, 1);
      arr.splice(newIdx, 0, node);
      arr.forEach((n, i) => {
        (n as { sortOrder?: number | null }).sortOrder = i;
      });
      // Determine the parent identity to send. Products moving on the top row
      // have NO parent — the backend rejects a parent for products.
      let parentType: TreeNodeType | null = null;
      let parentId: number | null = null;
      const parent = entry.parent;
      if (type === 'product') {
        parentType = null;
        parentId = null;
      } else if ((parent as IProductTreeNode).outcomes && !(parent as any).opportunities) {
        parentType = 'product';
        parentId = (parent as IProductTreeNode).id;
      } else if ((parent as IOutcomeTreeNode).opportunities && !(parent as any).children) {
        parentType = 'outcome';
        parentId = (parent as IOutcomeTreeNode).id;
      } else if ((parent as IOpportunityTreeNode).children || (parent as IOpportunityTreeNode).solutions) {
        parentType = 'opportunity';
        parentId = (parent as IOpportunityTreeNode).id;
      }
      try {
        const response = await svc.moveNode({ nodeType: type, nodeId: id, parentType, parentId, position: newIdx });
        this._applyMoveResponse(response);
        return true;
      } catch (err: any) {
        this.tree = snapshot;
        const status = err?.response?.status;
        const body = err?.response?.data;
        const serverMessage =
          (body && (body.detail || body.title || body.message)) ||
          (status === 403 ? 'You do not have permission to modify this tree.' : 'The move was rejected by the server.');
        if (status === 403) this.writeError = 'forbidden';
        else if (status === 400) this.writeError = 'validation';
        else this.writeError = 'unknown';
        this.writeErrorMessage = String(serverMessage);
        if (options.toast) options.toast.showError(String(serverMessage));
        return false;
      }
    },
    /** True if the node has a previous sibling (or previous product on the top row). */
    canMoveUp(type: TreeNodeType, id: number): boolean {
      return this._siblingIndex(type, id).idx > 0;
    },
    /** True if the node has a next sibling (or next product on the top row). */
    canMoveDown(type: TreeNodeType, id: number): boolean {
      const info = this._siblingIndex(type, id);
      return info.idx >= 0 && info.idx < info.length - 1;
    },
    /**
     * Reconcile the in-memory tree with the server's authoritative response —
     * apply the new sortOrder to every sibling under both the old and the new
     * parent, and rewrite each nested opportunity's outcomeId that the server
     * carried across as part of the move. Never refetches the whole tree.
     */
    _applyMoveResponse(response: MoveNodeResponse) {
      if (!this.tree) return;
      const applySibs = (sibs: { nodeType: TreeNodeType; id: number; sortOrder: number }[]) => {
        if (!sibs || sibs.length === 0) return;
        const idx = buildIndex(this.tree);
        for (const s of sibs) {
          const target = idx.get(key(s.nodeType, s.id));
          if (target) (target.node as { sortOrder?: number | null }).sortOrder = s.sortOrder;
        }
      };
      applySibs(response.oldSiblings ?? []);
      applySibs(response.newSiblings ?? []);
      for (const upd of response.outcomeUpdates ?? []) {
        const entry = buildIndex(this.tree).get(key('opportunity', upd.opportunityId));
        if (entry) (entry.node as any).outcomeId = upd.outcomeId;
      }
      if (response.sortOrder != null) {
        const moved = buildIndex(this.tree).get(key(response.nodeType, response.nodeId));
        if (moved) (moved.node as { sortOrder?: number | null }).sortOrder = response.sortOrder;
      }
    },
    _siblingIndex(type: TreeNodeType, id: number): { idx: number; length: number } {
      if (!this.tree) return { idx: -1, length: 0 };
      const entry = buildIndex(this.tree).get(key(type, id));
      if (!entry || !entry.parent) return { idx: -1, length: 0 };
      const arr = childrenArrayFor(entry.parent, type);
      if (!arr) return { idx: -1, length: 0 };
      const idx = arr.findIndex(n => (n as { id: number }).id === id);
      return { idx, length: arr.length };
    },
    parentOf(type: TreeNodeType, id: number): { type: TreeNodeType | 'team'; id: number | null } | null {
      if (!this.tree) return null;
      const entry = buildIndex(this.tree).get(key(type, id));
      if (!entry || !entry.parent) return null;
      const parent = entry.parent;
      // Team root
      if ((parent as ITeamTree).products && !(parent as any).outcomes) {
        return { type: 'team', id: null };
      }
      if ((parent as IProductTreeNode).outcomes && !(parent as any).opportunities) {
        return { type: 'product', id: (parent as IProductTreeNode).id };
      }
      if ((parent as IOutcomeTreeNode).opportunities && !(parent as any).children) {
        return { type: 'outcome', id: (parent as IOutcomeTreeNode).id };
      }
      return { type: 'opportunity', id: (parent as IOpportunityTreeNode).id };
    },
    enclosingOutcomeId(opportunityId: number): number | null {
      if (!this.tree) return null;
      const index = buildIndex(this.tree);
      let currentType: TreeNodeType = 'opportunity';
      let currentId = opportunityId;
      // Walk parents until we hit an outcome parent.
      for (let hop = 0; hop < 1000; hop++) {
        const entry = index.get(key(currentType, currentId));
        if (!entry) return null;
        const parent = entry.parent;
        if (!parent) return null;
        // If parent is an outcome (has 'opportunities' array but no 'children'), we found it.
        if ((parent as IOutcomeTreeNode).opportunities && !(parent as IOpportunityTreeNode).children) {
          return (parent as IOutcomeTreeNode).id;
        }
        // parent must be another opportunity
        currentType = 'opportunity';
        currentId = (parent as IOpportunityTreeNode).id;
      }
      return null;
    },
    recordWriteError(err: any) {
      const status = err?.response?.status;
      if (status === 403) {
        this.writeError = 'forbidden';
        this.writeErrorMessage = 'You do not have permission to modify this tree.';
      } else if (status === 400) {
        this.writeError = 'validation';
        const body = err?.response?.data;
        const detail = (body && (body.title || body.detail)) || 'The server rejected the request.';
        this.writeErrorMessage = String(detail);
      } else {
        this.writeError = 'unknown';
        this.writeErrorMessage = 'Something went wrong. Please try again.';
      }
    },
  },
});
