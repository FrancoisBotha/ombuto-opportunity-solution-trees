import { type SolutionStatus } from '@/shared/model/enumerations/solution-status.model';
import { type IOpportunity } from '@/shared/model/opportunity.model';
import { type ITag } from '@/shared/model/tag.model';
import { type IUser } from '@/shared/model/user.model';

export interface ISolution {
  id?: number;
  title?: string;
  description?: string | null;
  status?: keyof typeof SolutionStatus;
  sortOrder?: number;
  createdDate?: Date;
  lastModifiedDate?: Date | null;
  opportunity?: IOpportunity;
  owner?: IUser | null;
  tags?: ITag[] | null;
}

export class Solution implements ISolution {
  constructor(
    public id?: number,
    public title?: string,
    public description?: string | null,
    public status?: keyof typeof SolutionStatus,
    public sortOrder?: number,
    public createdDate?: Date,
    public lastModifiedDate?: Date | null,
    public opportunity?: IOpportunity,
    public owner?: IUser | null,
    public tags?: ITag[] | null,
  ) {}
}
