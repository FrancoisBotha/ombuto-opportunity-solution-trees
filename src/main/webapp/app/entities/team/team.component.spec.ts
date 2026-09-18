import { beforeEach, describe, expect, it, vitest } from 'vitest';

import { type MountingOptions, shallowMount } from '@vue/test-utils';
import sinon, { type SinonStubbedInstance } from 'sinon';

import AlertService from '@/shared/alert/alert.service';

import TeamService from './team.service';
import Team from './team.vue';

type TeamComponentType = InstanceType<typeof Team>;

const bModalStub = {
  render: () => {},
  methods: {
    hide: () => {},
    show: () => {},
  },
};

describe('Component Tests', () => {
  let alertService: AlertService;

  describe('Team Management Component', () => {
    let teamServiceStub: SinonStubbedInstance<TeamService>;
    let mountOptions: MountingOptions<TeamComponentType>['global'];

    beforeEach(() => {
      teamServiceStub = sinon.createStubInstance<TeamService>(TeamService);
      teamServiceStub.retrieve.resolves({ headers: {} });

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
          teamService: () => teamServiceStub,
        },
      };
    });

    describe('Mount', () => {
      it('Should call load all on init', async () => {
        // GIVEN
        teamServiceStub.retrieve.resolves({ headers: {}, data: [{ id: 123 }] });

        // WHEN
        const wrapper = shallowMount(Team, { global: mountOptions });
        const comp = wrapper.vm;
        await comp.$nextTick();

        // THEN
        expect(teamServiceStub.retrieve.calledOnce).toBeTruthy();
        expect(comp.teams[0]).toEqual(expect.objectContaining({ id: 123 }));
      });
    });
    describe('Handles', () => {
      let comp: TeamComponentType;

      beforeEach(async () => {
        const wrapper = shallowMount(Team, { global: mountOptions });
        comp = wrapper.vm;
        await comp.$nextTick();
        teamServiceStub.retrieve.reset();
        teamServiceStub.retrieve.resolves({ headers: {}, data: [] });
      });

      it('Should call delete service on confirmDelete', async () => {
        // GIVEN
        teamServiceStub.delete.resolves({});

        // WHEN
        comp.prepareRemove({ id: 123 });

        comp.removeTeam();
        await comp.$nextTick(); // clear components

        // THEN
        expect(teamServiceStub.delete.called).toBeTruthy();

        // THEN
        await comp.$nextTick(); // handle component clear watch
        expect(teamServiceStub.retrieve.callCount).toEqual(1);
      });
    });
  });
});
