import type { OpportunityStatus } from '@/shared/model/enumerations/opportunity-status.model';
import type { OutcomeStatus } from '@/shared/model/enumerations/outcome-status.model';
import type { SolutionStatus } from '@/shared/model/enumerations/solution-status.model';
import type { TeamRole } from '@/shared/model/enumerations/team-role.model';

export type TreeNodeType = 'product' | 'outcome' | 'opportunity' | 'solution';

export interface ISolutionTreeNode {
  id: number;
  title: string;
  description?: string | null;
  status?: SolutionStatus | null;
  effort?: number | null;
  sortOrder?: number | null;
}

export interface IOpportunityTreeNode {
  id: number;
  title: string;
  description?: string | null;
  status?: OpportunityStatus | null;
  valuerating?: number | null;
  complexity?: number | null;
  sortOrder?: number | null;
  parentId?: number | null;
  children: IOpportunityTreeNode[];
  solutions: ISolutionTreeNode[];
}

export interface IOutcomeTreeNode {
  id: number;
  title: string;
  description?: string | null;
  metric?: string | null;
  targetValue?: string | null;
  currentValue?: string | null;
  status?: OutcomeStatus | null;
  startDate?: string | null;
  targetDate?: string | null;
  sortOrder?: number | null;
  opportunities: IOpportunityTreeNode[];
}

export interface IProductTreeNode {
  id: number;
  name: string;
  description?: string | null;
  vision?: string | null;
  archived?: boolean | null;
  createdDate?: string | null;
  outcomes: IOutcomeTreeNode[];
}

export interface ITeamTree {
  id: number;
  name: string;
  description?: string | null;
  createdDate?: string | null;
  currentUserRole?: TeamRole | null;
  canEdit: boolean;
  products: IProductTreeNode[];
}

export type TreeNode = IProductTreeNode | IOutcomeTreeNode | IOpportunityTreeNode | ISolutionTreeNode;
