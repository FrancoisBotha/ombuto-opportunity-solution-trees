import { type AssumptionCategory } from '@/shared/model/enumerations/assumption-category.model';
import { type IExperiment } from '@/shared/model/experiment.model';
import { type ISolution } from '@/shared/model/solution.model';

export interface IAssumption {
  id?: number;
  statement?: string;
  category?: keyof typeof AssumptionCategory;
  importance?: number;
  evidence?: number;
  validated?: boolean | null;
  createdDate?: Date;
  solution?: ISolution;
  experiments?: IExperiment[] | null;
}

export class Assumption implements IAssumption {
  constructor(
    public id?: number,
    public statement?: string,
    public category?: keyof typeof AssumptionCategory,
    public importance?: number,
    public evidence?: number,
    public validated?: boolean | null,
    public createdDate?: Date,
    public solution?: ISolution,
    public experiments?: IExperiment[] | null,
  ) {
    this.validated = this.validated ?? false;
  }
}
