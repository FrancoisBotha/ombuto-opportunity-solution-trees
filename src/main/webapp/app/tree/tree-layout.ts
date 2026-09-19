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
  /**
   * Keys (`<type>:<id>`) whose subtree should be hidden. The collapsed node itself
   * is still laid out — only its descendants and their connectors are skipped.
   */
  collapsedKeys?: ReadonlySet<string> | null;
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
  /** True when this node has children but they are hidden by the collapse state. */
  collapsed: boolean;
  /** True when this node has any children at all (regardless of collapse). */
  hasChildren: boolean;
  /** Total descendants across all levels — meaningful only when `collapsed` is true. */
  hiddenDescendantCount: number;
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
  /** Children rendered by the layout (empty when the node is collapsed, even though `originalChildCount > 0`). */
  children: AbstractNode[];
  /** True when the original tree node has any children — even if `children` is empty because we collapsed it. */
  hasChildren: boolean;
  /** Total descendants in the ORIGINAL tree (not the pruned/collapsed one). */
  totalDescendants: number;
  /** True when this node was collapsed by the layout. */
  collapsed: boolean;
}

const DEFAULTS = {
  nodeWidth: 200,
  nodeHeight: 80,
  hGap: 30,
  vGap: 60,
  productGap: 60,
};

export const nodeKey = (type: TreeNodeType, id: number): string => `${type}:${id}`;

const isCollapsed = (collapsedKeys: ReadonlySet<string> | null | undefined, type: TreeNodeType, id: number): boolean =>
  collapsedKeys != null && collapsedKeys.has(nodeKey(type, id));

const solutionToAbstract = (s: ISolutionTreeNode): AbstractNode => ({
  type: 'solution',
  id: s.id,
  node: s,
  children: [],
  hasChildren: false,
  totalDescendants: 0,
  collapsed: false,
});

const opportunityToAbstract = (op: IOpportunityTreeNode, collapsedKeys: ReadonlySet<string> | null | undefined): AbstractNode => {
  const rawChildren = [
    ...(op.children ?? []).map(c => opportunityToAbstract(c, collapsedKeys)),
    ...(op.solutions ?? []).map(solutionToAbstract),
  ];
  const hasChildren = rawChildren.length > 0;
  const collapsed = hasChildren && isCollapsed(collapsedKeys, 'opportunity', op.id);
  const totalDescendants = rawChildren.reduce((sum, c) => sum + 1 + c.totalDescendants, 0);
  return {
    type: 'opportunity',
    id: op.id,
    node: op,
    children: collapsed ? [] : rawChildren,
    hasChildren,
    totalDescendants,
    collapsed,
  };
};

const outcomeToAbstract = (o: IOutcomeTreeNode, collapsedKeys: ReadonlySet<string> | null | undefined): AbstractNode => {
  const rawChildren = (o.opportunities ?? []).map(op => opportunityToAbstract(op, collapsedKeys));
  const hasChildren = rawChildren.length > 0;
  const collapsed = hasChildren && isCollapsed(collapsedKeys, 'outcome', o.id);
  const totalDescendants = rawChildren.reduce((sum, c) => sum + 1 + c.totalDescendants, 0);
  return {
    type: 'outcome',
    id: o.id,
    node: o,
    children: collapsed ? [] : rawChildren,
    hasChildren,
    totalDescendants,
    collapsed,
  };
};

const productToAbstract = (p: IProductTreeNode, collapsedKeys: ReadonlySet<string> | null | undefined): AbstractNode => {
  const rawChildren = (p.outcomes ?? []).map(o => outcomeToAbstract(o, collapsedKeys));
  const hasChildren = rawChildren.length > 0;
  const collapsed = hasChildren && isCollapsed(collapsedKeys, 'product', p.id);
  const totalDescendants = rawChildren.reduce((sum, c) => sum + 1 + c.totalDescendants, 0);
  return {
    type: 'product',
    id: p.id,
    node: p,
    children: collapsed ? [] : rawChildren,
    hasChildren,
    totalDescendants,
    collapsed,
  };
};

const buildRoots = (
  tree: ITeamTree,
  focusedProductId: number | null,
  collapsedKeys: ReadonlySet<string> | null | undefined,
): AbstractNode[] => {
  const list = focusedProductId != null ? tree.products.filter(p => p.id === focusedProductId) : tree.products;
  return list.map(p => productToAbstract(p, collapsedKeys));
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
    collapsed: n.collapsed,
    hasChildren: n.hasChildren,
    hiddenDescendantCount: n.collapsed ? n.totalDescendants : 0,
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
  const roots = buildRoots(tree, options.focusedProductId ?? null, options.collapsedKeys ?? null);
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
