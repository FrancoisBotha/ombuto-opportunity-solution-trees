import { type IAssumption } from '@/shared/model/assumption.model';
import { type IEvidence } from '@/shared/model/evidence.model';
import { type IOpportunity } from '@/shared/model/opportunity.model';
import { type IOutcome } from '@/shared/model/outcome.model';
import { type IProduct } from '@/shared/model/product.model';
import { type ISolution } from '@/shared/model/solution.model';

export interface INodeLink {
  id?: number;
  name?: string;
  url?: string;
  sortOrder?: number;
  createdDate?: Date;
  product?: IProduct | null;
  outcome?: IOutcome | null;
  opportunity?: IOpportunity | null;
  solution?: ISolution | null;
  assumption?: IAssumption | null;
  evidence?: IEvidence | null;
}

export class NodeLink implements INodeLink {
  constructor(
    public id?: number,
    public name?: string,
    public url?: string,
    public sortOrder?: number,
    public createdDate?: Date,
    public product?: IProduct | null,
    public outcome?: IOutcome | null,
    public opportunity?: IOpportunity | null,
    public solution?: ISolution | null,
    public assumption?: IAssumption | null,
    public evidence?: IEvidence | null,
  ) {}
}
