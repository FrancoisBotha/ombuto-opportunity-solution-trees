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

export interface CreateProductInput {
  name: string;
  description?: string | null;
  vision?: string | null;
}

export interface CreateChildInput {
  title: string;
  description?: string | null;
}

export interface UpdateProductInput {
  name?: string;
  description?: string | null;
  vision?: string | null;
  archived?: boolean;
}

export interface UpdateOutcomeInput {
  title?: string;
  description?: string | null;
  status?: OutcomeStatus;
}

export interface UpdateOpportunityInput {
  title?: string;
  description?: string | null;
  status?: OpportunityStatus;
  valuerating?: number;
  complexity?: number;
}

export interface UpdateSolutionInput {
  title?: string;
  description?: string | null;
  status?: SolutionStatus;
  effort?: number | null;
}

const patchHeaders = { headers: { 'Content-Type': 'application/merge-patch+json' } } as const;

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

  updateProduct(id: number, patch: UpdateProductInput): Promise<IProductTreeNode> {
    const body = { id, ...patch };
    return axios.patch<IProductTreeNode>(`${productsApiUrl}/${id}`, body, patchHeaders).then(res => this.normaliseProduct(res.data));
  }

  updateOutcome(id: number, patch: UpdateOutcomeInput): Promise<IOutcomeTreeNode> {
    const body = { id, ...patch };
    return axios.patch<IOutcomeTreeNode>(`${outcomesApiUrl}/${id}`, body, patchHeaders).then(res => this.normaliseOutcome(res.data));
  }

  updateOpportunity(id: number, patch: UpdateOpportunityInput): Promise<IOpportunityTreeNode> {
    const body = { id, ...patch };
    return axios
      .patch<IOpportunityTreeNode>(`${opportunitiesApiUrl}/${id}`, body, patchHeaders)
      .then(res => this.normaliseOpportunity(res.data));
  }

  updateSolution(id: number, patch: UpdateSolutionInput): Promise<ISolutionTreeNode> {
    const body = { id, ...patch };
    return axios.patch<ISolutionTreeNode>(`${solutionsApiUrl}/${id}`, body, patchHeaders).then(res => res.data);
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
