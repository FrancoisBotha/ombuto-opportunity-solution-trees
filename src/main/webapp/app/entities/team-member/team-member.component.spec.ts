import { beforeEach, describe, expect, it, vitest } from 'vitest';

import { type MountingOptions, shallowMount } from '@vue/test-utils';
import sinon, { type SinonStubbedInstance } from 'sinon';

import AlertService from '@/shared/alert/alert.service';

import TeamMemberService from './team-member.service';
import TeamMember from './team-member.vue';

type TeamMemberComponentType = InstanceType<typeof TeamMember>;

const bModalStub = {
  render: () => {},
  methods: {
    hide: () => {},
    show: () => {},
  },
};

describe('Component Tests', () => {
  let alertService: AlertService;

  describe('TeamMember Management Component', () => {
    let teamMemberServiceStub: SinonStubbedInstance<TeamMemberService>;
    let mountOptions: MountingOptions<TeamMemberComponentType>['global'];

    beforeEach(() => {
      teamMemberServiceStub = sinon.createStubInstance<TeamMemberService>(TeamMemberService);
      teamMemberServiceStub.retrieve.resolves({ headers: {} });

      alertService = new AlertService({
        toast: {
          show: vitest.fn(),
        } as any,
      });

      mountOptions = {
        stubs: {
          bModal: bModalStub as any,
          'font-awesome-icon': true,
          'b-badge': true,
          'b-button': true,
          'router-link': true,
        },
        directives: {
          'b-modal': {},
        },
        provide: {
          alertService,
          teamMemberService: () => teamMemberServiceStub,
        },
      };
    });

    describe('Mount', () => {
      it('Should call load all on init', async () => {
        // GIVEN
        teamMemberServiceStub.retrieve.resolves({ headers: {}, data: [{ id: 123 }] });

        // WHEN
        const wrapper = shallowMount(TeamMember, { global: mountOptions });
        const comp = wrapper.vm;
        await comp.$nextTick();

        // THEN
        expect(teamMemberServiceStub.retrieve.calledOnce).toBeTruthy();
        expect(comp.teamMembers[0]).toEqual(expect.objectContaining({ id: 123 }));
      });
    });
    describe('Handles', () => {
      let comp: TeamMemberComponentType;

      beforeEach(async () => {
        const wrapper = shallowMount(TeamMember, { global: mountOptions });
        comp = wrapper.vm;
        await comp.$nextTick();
        teamMemberServiceStub.retrieve.reset();
        teamMemberServiceStub.retrieve.resolves({ headers: {}, data: [] });
      });

      it('Should call delete service on confirmDelete', async () => {
        // GIVEN
        teamMemberServiceStub.delete.resolves({});

        // WHEN
        comp.prepareRemove({ id: 123 });

        comp.removeTeamMember();
        await comp.$nextTick(); // clear components

        // THEN
        expect(teamMemberServiceStub.delete.called).toBeTruthy();

        // THEN
        await comp.$nextTick(); // handle component clear watch
        expect(teamMemberServiceStub.retrieve.callCount).toEqual(1);
      });
    });
  });
});
