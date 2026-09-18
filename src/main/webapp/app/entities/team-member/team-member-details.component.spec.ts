import { beforeEach, describe, expect, it, vitest } from 'vitest';
import { type RouteLocation } from 'vue-router';

import { type MountingOptions, shallowMount } from '@vue/test-utils';
import sinon, { type SinonStubbedInstance } from 'sinon';

import AlertService from '@/shared/alert/alert.service';

import TeamMemberDetails from './team-member-details.vue';
import TeamMemberService from './team-member.service';

type TeamMemberDetailsComponentType = InstanceType<typeof TeamMemberDetails>;

let route: Partial<RouteLocation>;
const routerGoMock = vitest.fn();

vitest.mock('vue-router', () => ({
  useRoute: () => route,
  useRouter: () => ({ go: routerGoMock }),
}));

const teamMemberSample = { id: 123 };

describe('Component Tests', () => {
  let alertService: AlertService;

  afterEach(() => {
    vitest.resetAllMocks();
  });

  describe('TeamMember Management Detail Component', () => {
    let teamMemberServiceStub: SinonStubbedInstance<TeamMemberService>;
    let mountOptions: MountingOptions<TeamMemberDetailsComponentType>['global'];

    beforeEach(() => {
      route = {};
      teamMemberServiceStub = sinon.createStubInstance<TeamMemberService>(TeamMemberService);

      alertService = new AlertService({
        toast: {
          show: vitest.fn(),
        } as any,
      });

      mountOptions = {
        stubs: {
          'font-awesome-icon': true,
          'router-link': true,
        },
        provide: {
          alertService,
          teamMemberService: () => teamMemberServiceStub,
        },
      };
    });

    describe('Navigate to details', () => {
      it('Should call load all on init', async () => {
        // GIVEN
        teamMemberServiceStub.find.resolves(teamMemberSample);
        route = {
          params: {
            teamMemberId: `${123}`,
          },
        };
        const wrapper = shallowMount(TeamMemberDetails, { global: mountOptions });
        const comp = wrapper.vm;
        // WHEN
        await comp.$nextTick();

        // THEN
        expect(comp.teamMember).toMatchObject(teamMemberSample);
      });
    });

    describe('Previous state', () => {
      it('Should go previous state', async () => {
        teamMemberServiceStub.find.resolves(teamMemberSample);
        const wrapper = shallowMount(TeamMemberDetails, { global: mountOptions });
        const comp = wrapper.vm;
        await comp.$nextTick();

        comp.previousState();
        await comp.$nextTick();

        expect(routerGoMock).toHaveBeenCalledWith(-1);
      });
    });
  });
});
