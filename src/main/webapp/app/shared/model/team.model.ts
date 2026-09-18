export interface ITeam {
  id?: number;
  name?: string;
  description?: string | null;
  createdDate?: Date;
}

export class Team implements ITeam {
  constructor(
    public id?: number,
    public name?: string,
    public description?: string | null,
    public createdDate?: Date,
  ) {}
}
