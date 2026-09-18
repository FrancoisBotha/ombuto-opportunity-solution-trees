import { type LinkType } from '@/shared/model/enumerations/link-type.model';
import { type ISolution } from '@/shared/model/solution.model';

export interface ISolutionLink {
  id?: number;
  name?: string;
  url?: string;
  type?: keyof typeof LinkType;
  sortOrder?: number;
  solution?: ISolution;
}

export class SolutionLink implements ISolutionLink {
  constructor(
    public id?: number,
    public name?: string,
    public url?: string,
    public type?: keyof typeof LinkType,
    public sortOrder?: number,
    public solution?: ISolution,
  ) {}
}
