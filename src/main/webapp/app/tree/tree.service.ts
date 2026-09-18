import axios from 'axios';

import type { ITeamTree } from './tree.model';

const teamsApiUrl = 'api/teams';

export default class TreeService {
  getTeamTree(teamId: number): Promise<ITeamTree> {
    return axios.get<ITeamTree>(`${teamsApiUrl}/${teamId}/tree`).then(res => res.data);
  }
}
