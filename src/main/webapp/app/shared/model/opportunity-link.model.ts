import { type LinkType } from '@/shared/model/enumerations/link-type.model';
import { type IOpportunity } from '@/shared/model/opportunity.model';

export interface IOpportunityLink {
  id?: number;
  name?: string;
  url?: string;
  type?: keyof typeof LinkType;
  sortOrder?: number;
  opportunity?: IOpportunity;
}

export class OpportunityLink implements IOpportunityLink {
  constructor(
    public id?: number,
    public name?: string,
    public url?: string,
    public type?: keyof typeof LinkType,
    public sortOrder?: number,
    public opportunity?: IOpportunity,
  ) {}
}
