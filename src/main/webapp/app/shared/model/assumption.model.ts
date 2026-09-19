import { type AssumptionStatus } from '@/shared/model/enumerations/assumption-status.model';
import { type ISolution } from '@/shared/model/solution.model';
import { type IUser } from '@/shared/model/user.model';

export interface IAssumption {
  id?: number;
  statement?: string;
  description?: string | null;
  status?: keyof typeof AssumptionStatus;
  confidence?: number;
  sortOrder?: number;
  createdDate?: Date;
  lastModifiedDate?: Date | null;
  solution?: ISolution;
  owner?: IUser | null;
}

export class Assumption implements IAssumption {
  constructor(
    public id?: number,
    public statement?: string,
    public description?: string | null,
    public status?: keyof typeof AssumptionStatus,
    public confidence?: number,
    public sortOrder?: number,
    public createdDate?: Date,
    public lastModifiedDate?: Date | null,
    public solution?: ISolution,
    public owner?: IUser | null,
  ) {}
}
