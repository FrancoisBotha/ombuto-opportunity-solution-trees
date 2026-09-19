import type { IOpportunityTreeNode, IOutcomeTreeNode, IProductTreeNode, ITeamTree, TreeNode, TreeNodeType } from './tree.model';

export type MovableNodeType = 'outcome' | 'opportunity' | 'solution';

export interface MoveTarget {
  parentType: TreeNodeType;
  parentId: number;
  label: string;
  productId: number;
  productName: string;
  depth: number;
}

const validParentTypes: Record<MovableNodeType, TreeNodeType[]> = {
  outcome: ['product'],
  opportunity: ['outcome', 'opportunity'],
  solution: ['opportunity'],
};

export const isMovableType = (type: TreeNodeType): type is MovableNodeType =>
  type === 'outcome' || type === 'opportunity' || type === 'solution';

export const validParentTypesFor = (type: TreeNodeType): TreeNodeType[] => {
  if (!isMovableType(type)) return [];
  return validParentTypes[type];
};

const collectDescendantKeys = (type: TreeNodeType, node: TreeNode, into: Set<string>): void => {
  const k = (t: TreeNodeType, id: number) => `${t}:${id}`;
  if (type === 'product') {
    for (const o of (node as IProductTreeNode).outcomes ?? []) {
      into.add(k('outcome', o.id));
      collectDescendantKeys('outcome', o, into);
    }
  } else if (type === 'outcome') {
    for (const o of (node as IOutcomeTreeNode).opportunities ?? []) {
      into.add(k('opportunity', o.id));
      collectDescendantKeys('opportunity', o, into);
    }
  } else if (type === 'opportunity') {
    for (const c of (node as IOpportunityTreeNode).children ?? []) {
      into.add(k('opportunity', c.id));
      collectDescendantKeys('opportunity', c, into);
    }
    for (const s of (node as IOpportunityTreeNode).solutions ?? []) {
      into.add(k('solution', s.id));
    }
  }
};

export interface CanMoveArgs {
  nodeType: TreeNodeType;
  nodeId: number;
  node: TreeNode;
  currentParentType: TreeNodeType | 'team';
  currentParentId: number | null;
  targetParentType: TreeNodeType;
  targetParentId: number;
}

/**
 * Answers whether a node can be moved (re-parented) under the given target parent.
 * Not intended for sibling reordering — reorder never goes through canMove.
 */
export const canMove = (args: CanMoveArgs): boolean => {
  const { nodeType, nodeId, node, currentParentType, currentParentId, targetParentType, targetParentId } = args;
  if (!isMovableType(nodeType)) return false;
  if (!validParentTypesFor(nodeType).includes(targetParentType)) return false;
  // Never onto itself
  if (targetParentType === nodeType && targetParentId === nodeId) return false;
  // Never a no-op re-parent onto the current parent
  if (currentParentType === targetParentType && currentParentId === targetParentId) return false;
  // Never into own descendants
  const descendants = new Set<string>();
  collectDescendantKeys(nodeType, node, descendants);
  if (descendants.has(`${targetParentType}:${targetParentId}`)) return false;
  return true;
};

export interface ListValidTargetsArgs {
  tree: ITeamTree | null;
  nodeType: TreeNodeType;
  nodeId: number;
  node: TreeNode;
  currentParentType: TreeNodeType | 'team';
  currentParentId: number | null;
}

/**
 * List every valid target parent for the node, grouped/indented by product for readability.
 * Deep-tree friendly: an opportunity may be re-parented onto any outcome or any opportunity in the tree
 * that isn't the node itself, a descendant, or its current parent.
 */
export const listValidTargets = (args: ListValidTargetsArgs): MoveTarget[] => {
  const { tree, nodeType, nodeId, node, currentParentType, currentParentId } = args;
  if (!tree || !isMovableType(nodeType)) return [];
  const allowedParentTypes = validParentTypesFor(nodeType);
  const results: MoveTarget[] = [];
  const check = (targetParentType: TreeNodeType, targetParentId: number): boolean =>
    canMove({ nodeType, nodeId, node, currentParentType, currentParentId, targetParentType, targetParentId });

  for (const product of tree.products ?? []) {
    if (allowedParentTypes.includes('product') && check('product', product.id)) {
      results.push({
        parentType: 'product',
        parentId: product.id,
        label: product.name,
        productId: product.id,
        productName: product.name,
        depth: 0,
      });
    }
    for (const outcome of product.outcomes ?? []) {
      if (allowedParentTypes.includes('outcome') && check('outcome', outcome.id)) {
        results.push({
          parentType: 'outcome',
          parentId: outcome.id,
          label: outcome.title,
          productId: product.id,
          productName: product.name,
          depth: 1,
        });
      }
      const walkOpps = (opps: IOpportunityTreeNode[], depth: number) => {
        for (const opp of opps) {
          if (allowedParentTypes.includes('opportunity') && check('opportunity', opp.id)) {
            results.push({
              parentType: 'opportunity',
              parentId: opp.id,
              label: opp.title,
              productId: product.id,
              productName: product.name,
              depth,
            });
          }
          if (opp.children && opp.children.length > 0) {
            walkOpps(opp.children, depth + 1);
          }
        }
      };
      walkOpps(outcome.opportunities ?? [], 2);
    }
  }
  return results;
};
