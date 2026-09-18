import { beforeEach, describe, expect, it } from 'vitest';

import { createPinia, setActivePinia } from 'pinia';

import { TeamRole } from '@/shared/model/enumerations/team-role.model';

import type { IMyTeam } from './my-team.model';
import { useTeamsStore } from './teams.store';

const team = (id: number, role: TeamRole, name = `Team ${id}`): IMyTeam => ({
  id,
  name,
  description: null,
  createdDate: null,
  role,
  memberCount: 1,
  productCount: 0,
});

describe('teams.store', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
  });

  it('starts empty and unloaded', () => {
    const store = useTeamsStore();
    expect(store.teams).toEqual([]);
    expect(store.loaded).toBe(false);
  });

  it('setTeams marks the store loaded and stores the list', () => {
    const store = useTeamsStore();
    store.setTeams([team(1, TeamRole.OWNER), team(2, TeamRole.VIEWER)]);
    expect(store.loaded).toBe(true);
    expect(store.teams).toHaveLength(2);
  });

  it('roleFor returns the caller role or null when unknown', () => {
    const store = useTeamsStore();
    store.setTeams([team(1, TeamRole.EDITOR)]);
    expect(store.roleFor(1)).toBe(TeamRole.EDITOR);
    expect(store.roleFor(999)).toBeNull();
    expect(store.roleFor(null)).toBeNull();
  });

  it('isOwner is true only for OWNER', () => {
    const store = useTeamsStore();
    store.setTeams([team(1, TeamRole.OWNER), team(2, TeamRole.EDITOR), team(3, TeamRole.VIEWER)]);
    expect(store.isOwner(1)).toBe(true);
    expect(store.isOwner(2)).toBe(false);
    expect(store.isOwner(3)).toBe(false);
    expect(store.isOwner(999)).toBe(false);
  });

  it('canEdit is true for OWNER or EDITOR, false otherwise', () => {
    const store = useTeamsStore();
    store.setTeams([team(1, TeamRole.OWNER), team(2, TeamRole.EDITOR), team(3, TeamRole.VIEWER)]);
    expect(store.canEdit(1)).toBe(true);
    expect(store.canEdit(2)).toBe(true);
    expect(store.canEdit(3)).toBe(false);
    expect(store.canEdit(999)).toBe(false);
  });

  it('upsertTeam adds a new team and replaces an existing one', () => {
    const store = useTeamsStore();
    store.setTeams([team(1, TeamRole.OWNER)]);
    store.upsertTeam(team(2, TeamRole.VIEWER));
    expect(store.teams).toHaveLength(2);
    store.upsertTeam(team(1, TeamRole.OWNER, 'Renamed'));
    expect(store.teams).toHaveLength(2);
    expect(store.teamById(1)?.name).toBe('Renamed');
  });
});
