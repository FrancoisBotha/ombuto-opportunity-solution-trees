import { beforeEach, describe, expect, it, vitest } from 'vitest';

import { createTestingPinia } from '@pinia/testing';
import { type MountingOptions, shallowMount } from '@vue/test-utils';
import sinon, { type SinonStubbedInstance } from 'sinon';

import AlertService from '@/shared/alert/alert.service';
import { TeamRole } from '@/shared/model/enumerations/team-role.model';

import type { IMyTeam, ITeamMember } from './my-team.model';
import TeamMembers from './team-members.vue';
import TeamsService from './teams.service';
import { useTeamsStore } from './teams.store';

type TeamMembersComponentType = InstanceType<typeof TeamMembers>;

const sampleTeam = (id: number, role: TeamRole): IMyTeam => ({
  id,
  name: `Team ${id}`,
  description: null,
  createdDate: null,
  role,
  memberCount: 3,
  productCount: 0,
});

const member = (userId: string, role: TeamRole, login = `${userId}-login`): ITeamMember => ({
  id: Number.parseInt(userId.replace(/\D/g, ''), 10) || 1,
  userId,
  login,
  firstName: 'Given',
  lastName: 'Family',
  role,
  joinedDate: '2026-09-01T00:00:00Z',
});

describe('TeamMembers Component', () => {
  let teamsServiceStub: SinonStubbedInstance<TeamsService>;
  let alertService: AlertService;
  let mountOptions: MountingOptions<TeamMembersComponentType>['global'];

  beforeEach(() => {
    teamsServiceStub = sinon.createStubInstance<TeamsService>(TeamsService);

    alertService = new AlertService({
      toast: { show: vitest.fn() } as any,
    });

    mountOptions = {
      plugins: [createTestingPinia({ stubActions: false })],
      stubs: {
        'font-awesome-icon': true,
      },
      provide: {
        alertService,
        teamsService: () => teamsServiceStub,
      },
    };
  });

  const mountAs = async (role: TeamRole, members: ITeamMember[]) => {
    teamsServiceStub.listMembers.resolves(members);
    const wrapper = shallowMount(TeamMembers, {
      global: mountOptions,
      props: { teamId: 7 },
    });
    const store = useTeamsStore();
    store.upsertTeam(sampleTeam(7, role));
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();
    return wrapper;
  };

  it('renders each member with login, role and joined date', async () => {
    const wrapper = await mountAs(TeamRole.OWNER, [member('u1', TeamRole.OWNER), member('u2', TeamRole.VIEWER)]);
    expect(teamsServiceStub.listMembers.calledOnceWith(7)).toBe(true);
    const rows = wrapper.findAll('[data-cy-shared="memberRow"]');
    expect(rows).toHaveLength(2);
    expect(wrapper.find('[data-cy="memberRow-u1"]').text()).toContain('u1-login');
    expect(wrapper.find('[data-cy="memberRow-u1"]').text()).toContain('OWNER');
    expect(wrapper.find('[data-cy="memberRow-u1"]').text()).toContain('2026');
  });

  it('shows the add-member button and role controls to owners', async () => {
    const wrapper = await mountAs(TeamRole.OWNER, [member('u1', TeamRole.OWNER), member('u2', TeamRole.VIEWER)]);
    expect(wrapper.find('[data-cy="addMemberButton"]').exists()).toBe(true);
    // Editable role dropdown for the non-last-owner viewer
    expect(wrapper.find('[data-cy="roleSelect-u2"]').exists()).toBe(true);
    expect(wrapper.find('[data-cy="removeMemberButton-u2"]').exists()).toBe(true);
  });

  it('hides the add, role dropdown and remove controls for viewers', async () => {
    const wrapper = await mountAs(TeamRole.VIEWER, [member('u1', TeamRole.OWNER), member('u2', TeamRole.VIEWER)]);
    expect(wrapper.find('[data-cy="addMemberButton"]').exists()).toBe(false);
    expect(wrapper.find('[data-cy="roleSelect-u1"]').exists()).toBe(false);
    expect(wrapper.find('[data-cy="roleSelect-u2"]').exists()).toBe(false);
    expect(wrapper.find('[data-cy="removeMemberButton-u1"]').exists()).toBe(false);
    expect(wrapper.find('[data-cy="removeMemberButton-u2"]').exists()).toBe(false);
    // Static role text is shown instead
    expect(wrapper.find('[data-cy="memberRole-u1"]').text()).toContain('OWNER');
  });

  it('hides the add, role dropdown and remove controls for editors', async () => {
    const wrapper = await mountAs(TeamRole.EDITOR, [member('u1', TeamRole.OWNER), member('u2', TeamRole.EDITOR)]);
    expect(wrapper.find('[data-cy="addMemberButton"]').exists()).toBe(false);
    expect(wrapper.find('[data-cy="roleSelect-u1"]').exists()).toBe(false);
    expect(wrapper.find('[data-cy="removeMemberButton-u2"]').exists()).toBe(false);
  });

  it('disables the last owner remove and role controls with an explanatory hint', async () => {
    const wrapper = await mountAs(TeamRole.OWNER, [member('u1', TeamRole.OWNER), member('u2', TeamRole.VIEWER)]);
    // u1 is the only OWNER
    const roleSelect = wrapper.find('[data-cy="roleSelect-u1"]');
    expect(roleSelect.exists()).toBe(true);
    expect(roleSelect.attributes('disabled')).toBeDefined();
    const removeButton = wrapper.find('[data-cy="removeMemberButton-u1"]');
    expect(removeButton.exists()).toBe(true);
    expect(removeButton.attributes('disabled')).toBeDefined();
    expect(wrapper.find('[data-cy="lastOwnerHint-u1"]').exists()).toBe(true);
  });

  it('surfaces a server-side lastowner rejection as a clear error message', async () => {
    const wrapper = await mountAs(TeamRole.OWNER, [
      member('u1', TeamRole.OWNER),
      member('u2', TeamRole.OWNER),
      member('u3', TeamRole.VIEWER),
    ]);
    teamsServiceStub.changeMemberRole.rejects({
      response: { status: 400, data: { errorKey: 'lastowner' } },
    });
    const comp = wrapper.vm as any;
    await comp.changeRole(member('u1', TeamRole.OWNER), TeamRole.VIEWER);
    await wrapper.vm.$nextTick();
    expect(comp.actionError).toContain('at least one owner');
  });

  it('adds a member and reloads the member list', async () => {
    const wrapper = await mountAs(TeamRole.OWNER, [member('u1', TeamRole.OWNER)]);
    const comp = wrapper.vm as any;
    teamsServiceStub.searchUsers.resolves([{ id: 'u9', login: 'new', name: 'New Person' }]);
    const newMember = member('u9', TeamRole.EDITOR, 'new');
    teamsServiceStub.addMember.resolves(newMember);
    teamsServiceStub.listMembers.resolves([member('u1', TeamRole.OWNER), newMember]);

    comp.openAddForm();
    comp.searchQuery = 'new';
    await comp.doSearch();
    comp.selectUser({ id: 'u9', login: 'new', name: 'New Person' });
    comp.selectedRole = TeamRole.EDITOR;
    await comp.submitAdd();

    expect(teamsServiceStub.addMember.calledOnce).toBe(true);
    expect(teamsServiceStub.addMember.firstCall.args[0]).toBe(7);
    expect(teamsServiceStub.addMember.firstCall.args[1]).toEqual({ userId: 'u9', role: TeamRole.EDITOR });
    expect(comp.showAddForm).toBe(false);
  });

  it('shows a friendly message when the selected user is already a member', async () => {
    const wrapper = await mountAs(TeamRole.OWNER, [member('u1', TeamRole.OWNER)]);
    const comp = wrapper.vm as any;
    // Shape of the real BadRequestAlertException response
    teamsServiceStub.addMember.rejects({
      response: {
        status: 400,
        data: {
          title: 'Bad Request',
          status: 400,
          detail: "400 BAD_REQUEST, ProblemDetailWithCause[title='Bad Request', properties='{message=error.memberexists}']",
          message: 'error.memberexists',
          params: 'teamMember',
        },
      },
    });

    comp.openAddForm();
    comp.selectUser({ id: 'u1', login: 'admin', name: 'Admin Administrator' });
    await comp.submitAdd();

    expect(comp.addError).toBe('That user is already a member of this team.');
    expect(comp.showAddForm).toBe(true);
  });

  it('never shows a raw ProblemDetail dump for an unrecognised error', async () => {
    const wrapper = await mountAs(TeamRole.OWNER, [member('u1', TeamRole.OWNER)]);
    const comp = wrapper.vm as any;
    teamsServiceStub.addMember.rejects({
      response: {
        status: 400,
        data: { detail: "400 BAD_REQUEST, ProblemDetailWithCause[title='Bad Request']", message: 'error.somethingnew' },
      },
    });

    comp.openAddForm();
    comp.selectUser({ id: 'u9', login: 'new', name: 'New Person' });
    await comp.submitAdd();

    expect(comp.addError).toBe('Could not add member');
  });

  it('the picker mentions that users must have signed in at least once', async () => {
    const wrapper = await mountAs(TeamRole.OWNER, [member('u1', TeamRole.OWNER)]);
    const comp = wrapper.vm as any;
    comp.openAddForm();
    await wrapper.vm.$nextTick();
    expect(wrapper.find('[data-cy="addMemberHint"]').text().toLowerCase()).toContain('signed in');
  });
});
