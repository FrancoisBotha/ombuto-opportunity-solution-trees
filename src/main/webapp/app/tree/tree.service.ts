import axios from 'axios';

import { OpportunityStatus } from '@/shared/model/enumerations/opportunity-status.model';
import { OutcomeStatus } from '@/shared/model/enumerations/outcome-status.model';
import { SolutionStatus } from '@/shared/model/enumerations/solution-status.model';

import type { IOpportunityTreeNode, IOutcomeTreeNode, IProductTreeNode, ISolutionTreeNode, ITeamTree, TreeNodeType } from './tree.model';

const teamsApiUrl = 'api/teams';
const productsApiUrl = 'api/products';
const outcomesApiUrl = 'api/outcomes';
const opportunitiesApiUrl = 'api/opportunities';
const solutionsApiUrl = 'api/solutions';
const treeProductsApiUrl = 'api/tree/products';
const treeOutcomesApiUrl = 'api/tree/outcomes';
const treeOpportunitiesApiUrl = 'api/tree/opportunities';
const treeSolutionsApiUrl = 'api/tree/solutions';
const treeNodesMoveApiUrl = 'api/tree/nodes/move';

/**
 * Store-facing move payload. `parentType`/`parentId` are BOTH null when
 * repositioning a product along the top row; for every other node kind both
 * are required. `position` is zero-based and always sent (child count means
 * "last").
 */
export interface MoveNodeRequest {
  nodeType: TreeNodeType;
  nodeId: number;
  parentType: TreeNodeType | null;
  parentId: number | null;
  position: number;
}

export interface MoveSiblingOrder {
  nodeType: TreeNodeType;
  id: number;
  sortOrder: number;
}

export interface OpportunityOutcomeUpdate {
  opportunityId: number;
  outcomeId: number;
}

export interface MoveNodeResponse {
  nodeType: TreeNodeType;
  nodeId: number;
  parentType: TreeNodeType | null;
  parentId: number | null;
  outcomeId: number | null;
  sortOrder: number | null;
  oldSiblings: MoveSiblingOrder[];
  newSiblings: MoveSiblingOrder[];
  outcomeUpdates: OpportunityOutcomeUpdate[];
}

const toWireType = (t: TreeNodeType | null): string | null => (t == null ? null : t.toUpperCase());
const fromWireType = (t: string | null | undefined): TreeNodeType | null => (t == null ? null : (t.toLowerCase() as TreeNodeType));

interface WireSibling {
  nodeType: string;
  id: number;
  sortOrder: number;
}

interface WireMoveResponse {
  nodeType: string;
  nodeId: number;
  parentType: string | null;
  parentId: number | null;
  outcomeId: number | null;
  sortOrder: number | null;
  oldSiblings?: WireSibling[] | null;
  newSiblings?: WireSibling[] | null;
  outcomeUpdates?: OpportunityOutcomeUpdate[] | null;
}

const decodeSiblings = (s: WireSibling[] | null | undefined): MoveSiblingOrder[] =>
  (s ?? []).map(x => ({ nodeType: fromWireType(x.nodeType) as TreeNodeType, id: x.id, sortOrder: x.sortOrder }));

export interface CreateProductInput {
  name: string;
  description?: string | null;
  vision?: string | null;
}

export interface CreateChildInput {
  title: string;
  description?: string | null;
}

const nowIso = (): string => new Date().toISOString();

export default class TreeService {
  getTeamTree(teamId: number): Promise<ITeamTree> {
    return axios.get<ITeamTree>(`${teamsApiUrl}/${teamId}/tree`).then(res => res.data);
  }

  createProduct(teamId: number, input: CreateProductInput): Promise<IProductTreeNode> {
    const body = {
      name: input.name,
      description: input.description ?? null,
      vision: input.vision ?? null,
      archived: false,
      createdDate: nowIso(),
      team: { id: teamId },
    };
    return axios.post<IProductTreeNode>(productsApiUrl, body).then(res => this.normaliseProduct(res.data));
  }

  createOutcome(productId: number, input: CreateChildInput): Promise<IOutcomeTreeNode> {
    const body = {
      title: input.title,
      description: input.description ?? null,
      status: OutcomeStatus.DRAFT,
      sortOrder: 0,
      createdDate: nowIso(),
      product: { id: productId },
    };
    return axios.post<IOutcomeTreeNode>(outcomesApiUrl, body).then(res => this.normaliseOutcome(res.data));
  }

  createOpportunityUnderOutcome(outcomeId: number, input: CreateChildInput): Promise<IOpportunityTreeNode> {
    const body = {
      title: input.title,
      description: input.description ?? null,
      status: OpportunityStatus.IDENTIFIED,
      valuerating: 3,
      complexity: 3,
      sortOrder: 0,
      createdDate: nowIso(),
      outcome: { id: outcomeId },
      parent: null,
    };
    return axios.post<IOpportunityTreeNode>(opportunitiesApiUrl, body).then(res => this.normaliseOpportunity(res.data));
  }

  createOpportunityUnderOpportunity(parentId: number, outcomeId: number, input: CreateChildInput): Promise<IOpportunityTreeNode> {
    const body = {
      title: input.title,
      description: input.description ?? null,
      status: OpportunityStatus.IDENTIFIED,
      valuerating: 3,
      complexity: 3,
      sortOrder: 0,
      createdDate: nowIso(),
      outcome: { id: outcomeId },
      parent: { id: parentId },
    };
    return axios.post<IOpportunityTreeNode>(opportunitiesApiUrl, body).then(res => this.normaliseOpportunity(res.data));
  }

  createSolution(opportunityId: number, input: CreateChildInput): Promise<ISolutionTreeNode> {
    const body = {
      title: input.title,
      description: input.description ?? null,
      status: SolutionStatus.IDEA,
      effort: 3,
      sortOrder: 0,
      createdDate: nowIso(),
      opportunity: { id: opportunityId },
    };
    return axios.post<ISolutionTreeNode>(solutionsApiUrl, body).then(res => res.data);
  }

  moveNode(request: MoveNodeRequest): Promise<MoveNodeResponse> {
    const wire = {
      nodeType: toWireType(request.nodeType),
      nodeId: request.nodeId,
      parentType: toWireType(request.parentType),
      parentId: request.parentId,
      position: request.position,
    };
    return axios.post<WireMoveResponse>(treeNodesMoveApiUrl, wire).then(res => {
      const data = res.data;
      return {
        nodeType: fromWireType(data.nodeType) as TreeNodeType,
        nodeId: data.nodeId,
        parentType: fromWireType(data.parentType),
        parentId: data.parentId,
        outcomeId: data.outcomeId,
        sortOrder: data.sortOrder,
        oldSiblings: decodeSiblings(data.oldSiblings),
        newSiblings: decodeSiblings(data.newSiblings),
        outcomeUpdates: data.outcomeUpdates ?? [],
      };
    });
  }

  deleteNode(type: TreeNodeType, id: number): Promise<void> {
    let url = '';
    if (type === 'product') url = `${treeProductsApiUrl}/${id}`;
    else if (type === 'outcome') url = `${treeOutcomesApiUrl}/${id}`;
    else if (type === 'opportunity') url = `${treeOpportunitiesApiUrl}/${id}`;
    else if (type === 'solution') url = `${treeSolutionsApiUrl}/${id}`;
    return axios.delete(url).then(() => undefined);
  }

  private normaliseProduct(p: any): IProductTreeNode {
    return { ...p, outcomes: p.outcomes ?? [] } as IProductTreeNode;
  }
  private normaliseOutcome(o: any): IOutcomeTreeNode {
    return { ...o, opportunities: o.opportunities ?? [] } as IOutcomeTreeNode;
  }
  private normaliseOpportunity(o: any): IOpportunityTreeNode {
    return { ...o, children: o.children ?? [], solutions: o.solutions ?? [] } as IOpportunityTreeNode;
  }
}
