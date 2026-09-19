import { type IAssumption } from '@/shared/model/assumption.model';
import { type IOpportunity } from '@/shared/model/opportunity.model';

export interface IEvidence {
  id?: number;
  title?: string;
  description?: string | null;
  sortOrder?: number;
  createdDate?: Date;
  lastModifiedDate?: Date | null;
  opportunity?: IOpportunity | null;
  assumption?: IAssumption | null;
}

export class Evidence implements IEvidence {
  constructor(
    public id?: number,
    public title?: string,
    public description?: string | null,
    public sortOrder?: number,
    public createdDate?: Date,
    public lastModifiedDate?: Date | null,
    public opportunity?: IOpportunity | null,
    public assumption?: IAssumption | null,
  ) {}
}
