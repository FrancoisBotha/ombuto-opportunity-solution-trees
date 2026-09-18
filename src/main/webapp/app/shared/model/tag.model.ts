import { type IOpportunity } from '@/shared/model/opportunity.model';
import { type ISolution } from '@/shared/model/solution.model';
import { type ITeam } from '@/shared/model/team.model';

export interface ITag {
  id?: number;
  name?: string;
  colour?: string | null;
  team?: ITeam;
  opportunities?: IOpportunity[] | null;
  solutions?: ISolution[] | null;
}

export class Tag implements ITag {
  constructor(
    public id?: number,
    public name?: string,
    public colour?: string | null,
    public team?: ITeam,
    public opportunities?: IOpportunity[] | null,
    public solutions?: ISolution[] | null,
  ) {}
}
