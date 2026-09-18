import { beforeEach, describe, expect, it, vitest } from 'vitest';
import { type RouteLocation } from 'vue-router';

import { type MountingOptions, shallowMount } from '@vue/test-utils';
import sinon, { type SinonStubbedInstance } from 'sinon';

import AlertService from '@/shared/alert/alert.service';

import SolutionLinkDetails from './solution-link-details.vue';
import SolutionLinkService from './solution-link.service';

type SolutionLinkDetailsComponentType = InstanceType<typeof SolutionLinkDetails>;

let route: Partial<RouteLocation>;
const routerGoMock = vitest.fn();

vitest.mock('vue-router', () => ({
  useRoute: () => route,
  useRouter: () => ({ go: routerGoMock }),
}));

const solutionLinkSample = { id: 123 };

describe('Component Tests', () => {
  let alertService: AlertService;

  afterEach(() => {
    vitest.resetAllMocks();
  });

  describe('SolutionLink Management Detail Component', () => {
    let solutionLinkServiceStub: SinonStubbedInstance<SolutionLinkService>;
    let mountOptions: MountingOptions<SolutionLinkDetailsComponentType>['global'];

    beforeEach(() => {
      route = {};
      solutionLinkServiceStub = sinon.createStubInstance<SolutionLinkService>(SolutionLinkService);

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
          solutionLinkService: () => solutionLinkServiceStub,
        },
      };
    });

    describe('Navigate to details', () => {
      it('Should call load all on init', async () => {
        // GIVEN
        solutionLinkServiceStub.find.resolves(solutionLinkSample);
        route = {
          params: {
            solutionLinkId: `${123}`,
          },
        };
        const wrapper = shallowMount(SolutionLinkDetails, { global: mountOptions });
        const comp = wrapper.vm;
        // WHEN
        await comp.$nextTick();

        // THEN
        expect(comp.solutionLink).toMatchObject(solutionLinkSample);
      });
    });

    describe('Previous state', () => {
      it('Should go previous state', async () => {
        solutionLinkServiceStub.find.resolves(solutionLinkSample);
        const wrapper = shallowMount(SolutionLinkDetails, { global: mountOptions });
        const comp = wrapper.vm;
        await comp.$nextTick();

        comp.previousState();
        await comp.$nextTick();

        expect(routerGoMock).toHaveBeenCalledWith(-1);
      });
    });
  });
});
