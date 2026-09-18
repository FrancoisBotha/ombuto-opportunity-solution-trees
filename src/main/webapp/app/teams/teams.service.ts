import axios from 'axios';

import type {
  IAddTeamMemberRequest,
  IChangeTeamMemberRoleRequest,
  ICreateTeamRequest,
  IMyTeam,
  ITeamMember,
  IUpdateTeamRequest,
  IUserSearchResult,
} from './my-team.model';

const baseApiUrl = 'api/team-management';

export default class TeamsService {
  listMyTeams(): Promise<IMyTeam[]> {
    return axios.get<IMyTeam[]>(`${baseApiUrl}/my-teams`).then(res => res.data);
  }

  getTeam(id: number): Promise<IMyTeam> {
    return axios.get<IMyTeam>(`${baseApiUrl}/teams/${id}`).then(res => res.data);
  }

  createTeam(request: ICreateTeamRequest): Promise<IMyTeam> {
    return axios.post<IMyTeam>(`${baseApiUrl}/teams`, request).then(res => res.data);
  }

  updateTeam(id: number, request: IUpdateTeamRequest): Promise<IMyTeam> {
    return axios.put<IMyTeam>(`${baseApiUrl}/teams/${id}`, request).then(res => res.data);
  }

  listMembers(teamId: number): Promise<ITeamMember[]> {
    return axios.get<ITeamMember[]>(`${baseApiUrl}/teams/${teamId}/members`).then(res => res.data);
  }

  addMember(teamId: number, request: IAddTeamMemberRequest): Promise<ITeamMember> {
    return axios.post<ITeamMember>(`${baseApiUrl}/teams/${teamId}/members`, request).then(res => res.data);
  }

  changeMemberRole(teamId: number, userId: string, request: IChangeTeamMemberRoleRequest): Promise<ITeamMember> {
    return axios.put<ITeamMember>(`${baseApiUrl}/teams/${teamId}/members/${userId}`, request).then(res => res.data);
  }

  removeMember(teamId: number, userId: string): Promise<void> {
    return axios.delete<void>(`${baseApiUrl}/teams/${teamId}/members/${userId}`).then(() => undefined);
  }

  searchUsers(teamId: number, query: string, limit = 20): Promise<IUserSearchResult[]> {
    return axios
      .get<IUserSearchResult[]>(`${baseApiUrl}/teams/${teamId}/user-search`, { params: { q: query, limit } })
      .then(res => res.data);
  }
}
