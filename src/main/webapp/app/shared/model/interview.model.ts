import { type IOpportunity } from '@/shared/model/opportunity.model';
import { type IProduct } from '@/shared/model/product.model';
import { type IUser } from '@/shared/model/user.model';

export interface IInterview {
  id?: number;
  title?: string;
  participant?: string | null;
  interviewDate?: Date;
  notes?: string | null;
  recordingUrl?: string | null;
  createdDate?: Date;
  product?: IProduct;
  interviewer?: IUser | null;
  opportunities?: IOpportunity[] | null;
}

export class Interview implements IInterview {
  constructor(
    public id?: number,
    public title?: string,
    public participant?: string | null,
    public interviewDate?: Date,
    public notes?: string | null,
    public recordingUrl?: string | null,
    public createdDate?: Date,
    public product?: IProduct,
    public interviewer?: IUser | null,
    public opportunities?: IOpportunity[] | null,
  ) {}
}
