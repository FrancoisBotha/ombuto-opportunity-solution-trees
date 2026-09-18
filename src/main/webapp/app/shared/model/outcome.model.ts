import { type OutcomeStatus } from '@/shared/model/enumerations/outcome-status.model';
import { type IProduct } from '@/shared/model/product.model';
import { type IUser } from '@/shared/model/user.model';

export interface IOutcome {
  id?: number;
  title?: string;
  description?: string | null;
  metric?: string | null;
  targetValue?: string | null;
  currentValue?: string | null;
  status?: keyof typeof OutcomeStatus;
  startDate?: Date | null;
  targetDate?: Date | null;
  sortOrder?: number;
  createdDate?: Date;
  lastModifiedDate?: Date | null;
  product?: IProduct;
  owner?: IUser | null;
}

export class Outcome implements IOutcome {
  constructor(
    public id?: number,
    public title?: string,
    public description?: string | null,
    public metric?: string | null,
    public targetValue?: string | null,
    public currentValue?: string | null,
    public status?: keyof typeof OutcomeStatus,
    public startDate?: Date | null,
    public targetDate?: Date | null,
    public sortOrder?: number,
    public createdDate?: Date,
    public lastModifiedDate?: Date | null,
    public product?: IProduct,
    public owner?: IUser | null,
  ) {}
}
