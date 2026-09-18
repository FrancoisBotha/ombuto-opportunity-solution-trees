import { type OpportunityStatus } from '@/shared/model/enumerations/opportunity-status.model';
import { type IInterview } from '@/shared/model/interview.model';
import { type IOutcome } from '@/shared/model/outcome.model';
import { type ITag } from '@/shared/model/tag.model';
import { type IUser } from '@/shared/model/user.model';
export interface IOpportunity {
  id?: number;
  title?: string;
  description?: string | null;
  status?: keyof typeof OpportunityStatus;
  valuerating?: number;
  complexity?: number;
  sortOrder?: number;
  createdDate?: Date;
  lastModifiedDate?: Date | null;
  outcome?: IOutcome;
  parent?: IOpportunity | null;
  owner?: IUser | null;
  interviews?: IInterview[] | null;
  tags?: ITag[] | null;
}

export class Opportunity implements IOpportunity {
  constructor(
    public id?: number,
    public title?: string,
    public description?: string | null,
    public status?: keyof typeof OpportunityStatus,
    public valuerating?: number,
    public complexity?: number,
    public sortOrder?: number,
    public createdDate?: Date,
    public lastModifiedDate?: Date | null,
    public outcome?: IOutcome,
    public parent?: IOpportunity | null,
    public owner?: IUser | null,
    public interviews?: IInterview[] | null,
    public tags?: ITag[] | null,
  ) {}
}
