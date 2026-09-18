import { beforeEach, describe, expect, it, vitest } from 'vitest';

import { createTestingPinia } from '@pinia/testing';
import { type MountingOptions, shallowMount } from '@vue/test-utils';
import sinon, { type SinonStubbedInstance } from 'sinon';

import AlertService from '@/shared/alert/alert.service';
import { TeamRole } from '@/shared/model/enumerations/team-role.model';

import type { IMyTeam } from './my-team.model';
import MyTeams from './my-teams.vue';
import TeamsService from './teams.service';
import { useTeamsStore } from './teams.store';

type MyTeamsComponentType = InstanceType<typeof MyTeams>;

const sampleTeam = (id = 1, role: TeamRole = TeamRole.OWNER, name = `Team ${id}`): IMyTeam => ({
  id,
  name,
  description: 'desc',
  createdDate: null,
  role,
  memberCount: 3,
  productCount: 2,
});

describe('MyTeams Component', () => {
  let teamsServiceStub: SinonStubbedInstance<TeamsService>;
  let alertService: AlertService;
  let mountOptions: MountingOptions<MyTeamsComponentType>['global'];

  beforeEach(() => {
    teamsServiceStub = sinon.createStubInstance<TeamsService>(TeamsService);

    alertService = new AlertService({
      toast: {
        show: vitest.fn(),
      } as any,
    });

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

  it('renders the list of teams returned by the API', async () => {
    teamsServiceStub.listMyTeams.resolves([sampleTeam(1, TeamRole.OWNER, 'Alpha'), sampleTeam(2, TeamRole.VIEWER, 'Beta')]);

    const wrapper = shallowMount(MyTeams, { global: mountOptions });
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();

    expect(teamsServiceStub.listMyTeams.calledOnce).toBe(true);
    const store = useTeamsStore();
    expect(store.teams).toHaveLength(2);
    expect(wrapper.find('[data-cy="teamsList"]').exists()).toBe(true);
    expect(wrapper.find('[data-cy="teamsEmptyState"]').exists()).toBe(false);
  });

  it('shows the empty state when the caller has no teams', async () => {
    teamsServiceStub.listMyTeams.resolves([]);

    const wrapper = shallowMount(MyTeams, { global: mountOptions });
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();

    expect(wrapper.find('[data-cy="teamsEmptyState"]').exists()).toBe(true);
    expect(wrapper.find('[data-cy="teamsList"]').exists()).toBe(false);
  });

  it('creates a team via the API and adds it to the store', async () => {
    teamsServiceStub.listMyTeams.resolves([]);
    const created = sampleTeam(42, TeamRole.OWNER, 'Brand new');
    teamsServiceStub.createTeam.resolves(created);

    const wrapper = shallowMount(MyTeams, { global: mountOptions });
    const comp = wrapper.vm as any;
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();

    comp.openCreateForm();
    comp.newTeamName = 'Brand new';
    comp.newTeamDescription = 'hello';
    await comp.submitCreate();

    expect(teamsServiceStub.createTeam.calledOnce).toBe(true);
    expect(teamsServiceStub.createTeam.firstCall.args[0]).toEqual({ name: 'Brand new', description: 'hello' });
    const store = useTeamsStore();
    expect(store.teamById(42)?.name).toBe('Brand new');
    expect(comp.showCreateForm).toBe(false);
  });

  it('shows a validation error when the name is blank', async () => {
    teamsServiceStub.listMyTeams.resolves([]);

    const wrapper = shallowMount(MyTeams, { global: mountOptions });
    const comp = wrapper.vm as any;
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();

    comp.openCreateForm();
    comp.newTeamName = '   ';
    await comp.submitCreate();

    expect(teamsServiceStub.createTeam.called).toBe(false);
    expect(comp.formError).toBe('Name is required');
  });

  it('surfaces API validation errors from create-team', async () => {
    teamsServiceStub.listMyTeams.resolves([]);
    teamsServiceStub.createTeam.rejects({ response: { status: 400, data: { detail: 'Name already used' } } });

    const wrapper = shallowMount(MyTeams, { global: mountOptions });
    const comp = wrapper.vm as any;
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();

    comp.openCreateForm();
    comp.newTeamName = 'Dup';
    await comp.submitCreate();

    expect(comp.formError).toBe('Name already used');
    expect(comp.showCreateForm).toBe(true);
  });
});
