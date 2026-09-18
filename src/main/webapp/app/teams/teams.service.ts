import axios from 'axios';

import type {
  IAddTeamMemberRequest,
  IChangeTeamMemberRoleRequest,
  ICreateTeamProductRequest,
  ICreateTeamRequest,
  IMyTeam,
  ITeamMember,
  ITeamProduct,
  IUpdateTeamProductRequest,
  IUpdateTeamRequest,
  IUserSearchResult,
} from './my-team.model';

const baseApiUrl = 'api/team-management';
const productsApiUrl = 'api/products';
const teamsApiUrl = 'api/teams';

interface IServerProductDTO {
  id: number;
  name: string;
  description?: string | null;
  archived: boolean;
  createdDate?: string | null;
  team?: { id: number } | null;
}

const toTeamProduct = (dto: IServerProductDTO): ITeamProduct => ({
  id: dto.id,
  name: dto.name,
  description: dto.description ?? null,
  archived: !!dto.archived,
  createdDate: dto.createdDate ?? null,
  teamId: dto.team?.id ?? 0,
});

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

  listTeamProducts(teamId: number): Promise<ITeamProduct[]> {
    return axios.get<IServerProductDTO[]>(`${teamsApiUrl}/${teamId}/products`).then(res => res.data.map(toTeamProduct));
  }

  createTeamProduct(teamId: number, request: ICreateTeamProductRequest): Promise<ITeamProduct> {
    const body = {
      name: request.name,
      description: request.description ?? null,
      archived: false,
      createdDate: new Date().toISOString(),
      team: { id: teamId },
    };
    return axios.post<IServerProductDTO>(productsApiUrl, body).then(res => toTeamProduct(res.data));
  }

  updateTeamProduct(product: ITeamProduct, request: IUpdateTeamProductRequest): Promise<ITeamProduct> {
    const body = {
      id: product.id,
      name: request.name,
      description: request.description ?? null,
      archived: product.archived,
      createdDate: product.createdDate ?? new Date().toISOString(),
      team: { id: product.teamId },
    };
    return axios.put<IServerProductDTO>(`${productsApiUrl}/${product.id}`, body).then(res => toTeamProduct(res.data));
  }

  setProductArchived(productId: number, archived: boolean): Promise<ITeamProduct> {
    const body = { id: productId, archived };
    return axios
      .patch<IServerProductDTO>(`${productsApiUrl}/${productId}`, body, {
        headers: { 'Content-Type': 'application/merge-patch+json' },
      })
      .then(res => toTeamProduct(res.data));
  }
}
