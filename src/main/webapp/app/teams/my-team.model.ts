import type { TeamRole } from '@/shared/model/enumerations/team-role.model';

export interface IMyTeam {
  id: number;
  name: string;
  description?: string | null;
  createdDate?: string | null;
  role: TeamRole;
  memberCount: number;
  productCount: number;
}

export interface ICreateTeamRequest {
  name: string;
  description?: string | null;
}

export interface IUpdateTeamRequest {
  name: string;
  description?: string | null;
}
