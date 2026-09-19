import { type ITeam } from '@/shared/model/team.model';

export interface IProduct {
  id?: number;
  name?: string;
  description?: string | null;
  vision?: string | null;
  archived?: boolean;
  sortOrder?: number;
  createdDate?: Date;
  team?: ITeam;
}

export class Product implements IProduct {
  constructor(
    public id?: number,
    public name?: string,
    public description?: string | null,
    public vision?: string | null,
    public archived?: boolean,
    public sortOrder?: number,
    public createdDate?: Date,
    public team?: ITeam,
  ) {
    this.archived = this.archived ?? false;
  }
}
