import { defineStore } from 'pinia';

import { TeamRole } from '@/shared/model/enumerations/team-role.model';

import type { IMyTeam } from './my-team.model';

export interface TeamsState {
  teams: IMyTeam[];
  loaded: boolean;
  loading: boolean;
  error: string | null;
}

export const useTeamsStore = defineStore('teams', {
  state: (): TeamsState => ({
    teams: [],
    loaded: false,
    loading: false,
    error: null,
  }),
  getters: {
    /** Role the caller has in the given team, or null when they are not a member. */
    roleFor:
      state =>
      (teamId: number | null | undefined): TeamRole | null => {
        if (teamId == null) return null;
        const found = state.teams.find(t => t.id === teamId);
        return found ? found.role : null;
      },
    isOwner() {
      return (teamId: number | null | undefined): boolean => this.roleFor(teamId) === TeamRole.OWNER;
    },
    canEdit() {
      return (teamId: number | null | undefined): boolean => {
        const role = this.roleFor(teamId);
        return role === TeamRole.OWNER || role === TeamRole.EDITOR;
      };
    },
    teamById:
      state =>
      (teamId: number | null | undefined): IMyTeam | null => {
        if (teamId == null) return null;
        return state.teams.find(t => t.id === teamId) ?? null;
      },
  },
  actions: {
    setTeams(teams: IMyTeam[]) {
      this.teams = teams;
      this.loaded = true;
      this.error = null;
    },
    upsertTeam(team: IMyTeam) {
      const idx = this.teams.findIndex(t => t.id === team.id);
      if (idx >= 0) {
        this.teams.splice(idx, 1, team);
      } else {
        this.teams.push(team);
      }
    },
    setLoading(loading: boolean) {
      this.loading = loading;
    },
    setError(error: string | null) {
      this.error = error;
    },
    reset() {
      this.teams = [];
      this.loaded = false;
      this.loading = false;
      this.error = null;
    },
  },
});
