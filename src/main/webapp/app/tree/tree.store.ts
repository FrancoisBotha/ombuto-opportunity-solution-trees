import { defineStore } from 'pinia';

import type { IOpportunityTreeNode, IOutcomeTreeNode, IProductTreeNode, ITeamTree, TreeNode, TreeNodeType } from './tree.model';
import TreeService, { type CreateChildInput, type CreateProductInput } from './tree.service';

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
