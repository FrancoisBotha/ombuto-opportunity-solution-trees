import { type IAssumption } from '@/shared/model/assumption.model';
import { type IEvidence } from '@/shared/model/evidence.model';
import { type IOpportunity } from '@/shared/model/opportunity.model';
import { type IOutcome } from '@/shared/model/outcome.model';
import { type ISolution } from '@/shared/model/solution.model';
import { type IUser } from '@/shared/model/user.model';

export interface IComment {
  id?: number;
  body?: string;
  createdDate?: Date;
  editedDate?: Date | null;
  author?: IUser;
  outcome?: IOutcome | null;
  opportunity?: IOpportunity | null;
  solution?: ISolution | null;
  assumption?: IAssumption | null;
  evidence?: IEvidence | null;
}

export class Comment implements IComment {
  constructor(
    public id?: number,
    public body?: string,
    public createdDate?: Date,
    public editedDate?: Date | null,
    public author?: IUser,
    public outcome?: IOutcome | null,
    public opportunity?: IOpportunity | null,
    public solution?: ISolution | null,
    public assumption?: IAssumption | null,
    public evidence?: IEvidence | null,
  ) {}
}
