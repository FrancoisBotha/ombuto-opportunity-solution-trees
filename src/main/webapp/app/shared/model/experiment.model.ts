import { type IAssumption } from '@/shared/model/assumption.model';
import { type ExperimentResult } from '@/shared/model/enumerations/experiment-result.model';
import { type ExperimentStatus } from '@/shared/model/enumerations/experiment-status.model';
import { type ISolution } from '@/shared/model/solution.model';
export interface IExperiment {
  id?: number;
  title?: string;
  hypothesis?: string | null;
  method?: string | null;
  successCriteria?: string | null;
  status?: keyof typeof ExperimentStatus;
  result?: keyof typeof ExperimentResult | null;
  learnings?: string | null;
  startDate?: Date | null;
  endDate?: Date | null;
  createdDate?: Date;
  solution?: ISolution;
  assumptions?: IAssumption[] | null;
}

export class Experiment implements IExperiment {
  constructor(
    public id?: number,
    public title?: string,
    public hypothesis?: string | null,
    public method?: string | null,
    public successCriteria?: string | null,
    public status?: keyof typeof ExperimentStatus,
    public result?: keyof typeof ExperimentResult | null,
    public learnings?: string | null,
    public startDate?: Date | null,
    public endDate?: Date | null,
    public createdDate?: Date,
    public solution?: ISolution,
    public assumptions?: IAssumption[] | null,
  ) {}
}
