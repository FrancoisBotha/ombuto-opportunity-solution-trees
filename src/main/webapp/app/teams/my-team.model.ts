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

export interface ITeamMember {
  id: number;
  userId: string;
  login: string;
  firstName?: string | null;
  lastName?: string | null;
  role: TeamRole;
  joinedDate?: string | null;
}

export interface IUserSearchResult {
  id: string;
  login: string;
  name: string;
}

export interface IAddTeamMemberRequest {
  userId: string;
  role: TeamRole;
}

export interface IChangeTeamMemberRoleRequest {
  role: TeamRole;
}

export interface ITeamProduct {
  id: number;
  name: string;
  description?: string | null;
  archived: boolean;
  createdDate?: string | null;
  teamId: number;
}

export interface ICreateTeamProductRequest {
  name: string;
  description?: string | null;
}

export interface IUpdateTeamProductRequest {
  name: string;
  description?: string | null;
}
