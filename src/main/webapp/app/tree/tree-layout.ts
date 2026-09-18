import type {
  IOpportunityTreeNode,
  IOutcomeTreeNode,
  IProductTreeNode,
  ISolutionTreeNode,
  ITeamTree,
  TreeNode,
  TreeNodeType,
} from './tree.model';

export interface LayoutOptions {
  nodeWidth?: number;
  nodeHeight?: number;
  hGap?: number;
  vGap?: number;
  productGap?: number;
  focusedProductId?: number | null;
}

export interface LayoutNode {
  key: string;
  type: TreeNodeType;
  id: number;
  x: number;
  y: number;
  width: number;
  height: number;
  data: TreeNode;
}

export interface LayoutEdge {
  fromKey: string;
  toKey: string;
  fromX: number;
  fromY: number;
  toX: number;
  toY: number;
}

export interface LayoutResult {
  nodes: LayoutNode[];
  edges: LayoutEdge[];
  width: number;
  height: number;
}

interface AbstractNode {
  type: TreeNodeType;
  id: number;
  node: TreeNode;
  children: AbstractNode[];
}

const DEFAULTS = {
  nodeWidth: 200,
  nodeHeight: 80,
  hGap: 30,
  vGap: 60,
  productGap: 60,
};

export const nodeKey = (type: TreeNodeType, id: number): string => `${type}:${id}`;

const solutionToAbstract = (s: ISolutionTreeNode): AbstractNode => ({
  type: 'solution',
  id: s.id,
  node: s,
  children: [],
});

const opportunityToAbstract = (op: IOpportunityTreeNode): AbstractNode => ({
  type: 'opportunity',
  id: op.id,
  node: op,
  children: [...(op.children ?? []).map(opportunityToAbstract), ...(op.solutions ?? []).map(solutionToAbstract)],
});

const outcomeToAbstract = (o: IOutcomeTreeNode): AbstractNode => ({
  type: 'outcome',
  id: o.id,
  node: o,
  children: (o.opportunities ?? []).map(opportunityToAbstract),
});

const productToAbstract = (p: IProductTreeNode): AbstractNode => ({
  type: 'product',
  id: p.id,
  node: p,
  children: (p.outcomes ?? []).map(outcomeToAbstract),
});

const buildRoots = (tree: ITeamTree, focusedProductId: number | null): AbstractNode[] => {
  const list = focusedProductId != null ? tree.products.filter(p => p.id === focusedProductId) : tree.products;
  return list.map(productToAbstract);
};

const computeSubtreeWidth = (
  n: AbstractNode,
  opts: Required<Omit<LayoutOptions, 'focusedProductId'>>,
  cache: Map<AbstractNode, number>,
): number => {
  const cached = cache.get(n);
  if (cached !== undefined) return cached;
  if (n.children.length === 0) {
    cache.set(n, opts.nodeWidth);
    return opts.nodeWidth;
  }
  let sum = 0;
  for (const c of n.children) sum += computeSubtreeWidth(c, opts, cache);
  sum += opts.hGap * (n.children.length - 1);
  const w = Math.max(opts.nodeWidth, sum);
  cache.set(n, w);
  return w;
};

const place = (
  n: AbstractNode,
  xOffset: number,
  depth: number,
  opts: Required<Omit<LayoutOptions, 'focusedProductId'>>,
  cache: Map<AbstractNode, number>,
  out: LayoutResult,
): number => {
  const width = computeSubtreeWidth(n, opts, cache);
  const y = depth * (opts.nodeHeight + opts.vGap);
  let centerX: number;
  if (n.children.length === 0) {
    centerX = xOffset + width / 2;
  } else {
    let cursor = xOffset;
    const centers: number[] = [];
    for (const c of n.children) {
      const cw = computeSubtreeWidth(c, opts, cache);
      centers.push(place(c, cursor, depth + 1, opts, cache, out));
      cursor += cw + opts.hGap;
    }
    centerX = (centers[0] + centers[centers.length - 1]) / 2;
  }
  const key = nodeKey(n.type, n.id);
  out.nodes.push({
    key,
    type: n.type,
    id: n.id,
    x: centerX - opts.nodeWidth / 2,
    y,
    width: opts.nodeWidth,
    height: opts.nodeHeight,
    data: n.node,
  });
  for (const c of n.children) {
    const ck = nodeKey(c.type, c.id);
    out.edges.push({
      fromKey: key,
      toKey: ck,
      fromX: centerX,
      fromY: y + opts.nodeHeight,
      toX: 0,
      toY: (depth + 1) * (opts.nodeHeight + opts.vGap),
    });
  }
  return centerX;
};

export const layoutTree = (tree: ITeamTree | null, options: LayoutOptions = {}): LayoutResult => {
  const opts = {
    nodeWidth: options.nodeWidth ?? DEFAULTS.nodeWidth,
    nodeHeight: options.nodeHeight ?? DEFAULTS.nodeHeight,
    hGap: options.hGap ?? DEFAULTS.hGap,
    vGap: options.vGap ?? DEFAULTS.vGap,
    productGap: options.productGap ?? DEFAULTS.productGap,
  };
  const out: LayoutResult = { nodes: [], edges: [], width: 0, height: 0 };
  if (!tree || !tree.products || tree.products.length === 0) return out;
  const roots = buildRoots(tree, options.focusedProductId ?? null);
  if (roots.length === 0) return out;
  const cache = new Map<AbstractNode, number>();
  let x = 0;
  let maxY = 0;
  for (const root of roots) {
    const w = computeSubtreeWidth(root, opts, cache);
    place(root, x, 0, opts, cache, out);
    x += w + opts.productGap;
  }
  out.width = Math.max(0, x - opts.productGap);
  const centerByKey = new Map<string, LayoutNode>();
  for (const n of out.nodes) {
    centerByKey.set(n.key, n);
    if (n.y + n.height > maxY) maxY = n.y + n.height;
  }
  for (const e of out.edges) {
    const to = centerByKey.get(e.toKey);
    if (to) {
      e.toX = to.x + to.width / 2;
      e.toY = to.y;
    }
  }
  out.height = maxY;
  return out;
};
