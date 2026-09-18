import axios from 'axios';

import type { ICreateTeamRequest, IMyTeam, IUpdateTeamRequest } from './my-team.model';

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
}
