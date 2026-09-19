import { beforeEach, describe, expect, it, vitest } from 'vitest';

import { createTestingPinia } from '@pinia/testing';
import { type MountingOptions, shallowMount } from '@vue/test-utils';
import sinon, { type SinonStubbedInstance } from 'sinon';

import AlertService from '@/shared/alert/alert.service';
import { TeamRole } from '@/shared/model/enumerations/team-role.model';
import type { IMyTeam } from '@/teams/my-team.model';
import TeamsService from '@/teams/teams.service';

import Trees from './trees.vue';

type TreesComponentType = InstanceType<typeof Trees>;

const sampleTeam = (id = 1, role: TeamRole = TeamRole.OWNER, name = `Team ${id}`): IMyTeam => ({
  id,
  name,
  description: 'desc',
  createdDate: null,
  role,
  memberCount: 3,
  productCount: 2,
});

describe('Trees Component', () => {
  let teamsServiceStub: SinonStubbedInstance<TeamsService>;
  let alertService: AlertService;
  let mountOptions: MountingOptions<TreesComponentType>['global'];

  beforeEach(() => {
    teamsServiceStub = sinon.createStubInstance<TeamsService>(TeamsService);
    alertService = new AlertService({ toast: { show: vitest.fn() } as any });
    mountOptions = {
      plugins: [createTestingPinia({ stubActions: false })],
      stubs: {
        'font-awesome-icon': true,
        'router-link': true,
      },
      provide: {
        alertService,
        teamsService: () => teamsServiceStub,
      },
    };
  });

  it('renders one card per team the user belongs to', async () => {
    teamsServiceStub.listMyTeams.resolves([sampleTeam(1, TeamRole.OWNER, 'Alpha'), sampleTeam(2, TeamRole.VIEWER, 'Beta')]);

    const wrapper = shallowMount(Trees, { global: mountOptions });
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();

    expect(teamsServiceStub.listMyTeams.calledOnce).toBe(true);
    expect(wrapper.find('[data-cy="treesList"]').exists()).toBe(true);
    expect(wrapper.find('[data-cy="treeCard-1"]').exists()).toBe(true);
    expect(wrapper.find('[data-cy="treeCard-2"]').exists()).toBe(true);
    expect(wrapper.find('[data-cy="treesEmptyState"]').exists()).toBe(false);
  });

  it('shows the empty state when the user belongs to no team', async () => {
    teamsServiceStub.listMyTeams.resolves([]);

    const wrapper = shallowMount(Trees, { global: mountOptions });
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();

    expect(wrapper.find('[data-cy="treesEmptyState"]').exists()).toBe(true);
    expect(wrapper.find('[data-cy="treesList"]').exists()).toBe(false);
  });
});
