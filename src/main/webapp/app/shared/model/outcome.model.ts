import { type IProduct } from '@/shared/model/product.model';
import { type IUser } from '@/shared/model/user.model';

export interface IOutcome {
  id?: number;
  title?: string;
  description?: string | null;
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
    public sortOrder?: number,
    public createdDate?: Date,
    public lastModifiedDate?: Date | null,
    public product?: IProduct,
    public owner?: IUser | null,
  ) {}
}
