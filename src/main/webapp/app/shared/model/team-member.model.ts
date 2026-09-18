import { type TeamRole } from '@/shared/model/enumerations/team-role.model';
import { type ITeam } from '@/shared/model/team.model';
import { type IUser } from '@/shared/model/user.model';

export interface ITeamMember {
  id?: number;
  role?: keyof typeof TeamRole;
  joinedDate?: Date;
  team?: ITeam;
  user?: IUser;
}

export class TeamMember implements ITeamMember {
  constructor(
    public id?: number,
    public role?: keyof typeof TeamRole,
    public joinedDate?: Date,
    public team?: ITeam,
    public user?: IUser,
  ) {}
}
