import { beforeEach, describe, expect, it, vitest } from 'vitest';
import { type RouteLocation } from 'vue-router';

import { type MountingOptions, shallowMount } from '@vue/test-utils';
import sinon, { type SinonStubbedInstance } from 'sinon';

import AlertService from '@/shared/alert/alert.service';

import SolutionDetails from './solution-details.vue';
import SolutionService from './solution.service';

type SolutionDetailsComponentType = InstanceType<typeof SolutionDetails>;

let route: Partial<RouteLocation>;
const routerGoMock = vitest.fn();

vitest.mock('vue-router', () => ({
  useRoute: () => route,
  useRouter: () => ({ go: routerGoMock }),
}));

const solutionSample = { id: 123 };

describe('Component Tests', () => {
  let alertService: AlertService;

  afterEach(() => {
    vitest.resetAllMocks();
  });

  describe('Solution Management Detail Component', () => {
    let solutionServiceStub: SinonStubbedInstance<SolutionService>;
    let mountOptions: MountingOptions<SolutionDetailsComponentType>['global'];

    beforeEach(() => {
      route = {};
      solutionServiceStub = sinon.createStubInstance<SolutionService>(SolutionService);

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
          solutionService: () => solutionServiceStub,
        },
      };
    });

    describe('Navigate to details', () => {
      it('Should call load all on init', async () => {
        // GIVEN
        solutionServiceStub.find.resolves(solutionSample);
        route = {
          params: {
            solutionId: `${123}`,
          },
        };
        const wrapper = shallowMount(SolutionDetails, { global: mountOptions });
        const comp = wrapper.vm;
        // WHEN
        await comp.$nextTick();

        // THEN
        expect(comp.solution).toMatchObject(solutionSample);
      });
    });

    describe('Previous state', () => {
      it('Should go previous state', async () => {
        solutionServiceStub.find.resolves(solutionSample);
        const wrapper = shallowMount(SolutionDetails, { global: mountOptions });
        const comp = wrapper.vm;
        await comp.$nextTick();

        comp.previousState();
        await comp.$nextTick();

        expect(routerGoMock).toHaveBeenCalledWith(-1);
      });
    });
  });
});
