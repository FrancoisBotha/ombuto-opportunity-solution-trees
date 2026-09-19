import { type IOpportunity } from '@/shared/model/opportunity.model';

export interface IOpenQuestion {
  id?: number;
  questionText?: string;
  done?: boolean;
  sortOrder?: number;
  createdDate?: Date;
  opportunity?: IOpportunity;
}

export class OpenQuestion implements IOpenQuestion {
  constructor(
    public id?: number,
    public questionText?: string,
    public done?: boolean,
    public sortOrder?: number,
    public createdDate?: Date,
    public opportunity?: IOpportunity,
  ) {
    this.done = this.done ?? false;
  }
}
