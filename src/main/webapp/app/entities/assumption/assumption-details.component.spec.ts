import { beforeEach, describe, expect, it, vitest } from 'vitest';
import { type RouteLocation } from 'vue-router';

import { type MountingOptions, shallowMount } from '@vue/test-utils';
import sinon, { type SinonStubbedInstance } from 'sinon';

import AlertService from '@/shared/alert/alert.service';

import AssumptionDetails from './assumption-details.vue';
import AssumptionService from './assumption.service';

type AssumptionDetailsComponentType = InstanceType<typeof AssumptionDetails>;

let route: Partial<RouteLocation>;
const routerGoMock = vitest.fn();

vitest.mock('vue-router', () => ({
  useRoute: () => route,
  useRouter: () => ({ go: routerGoMock }),
}));

const assumptionSample = { id: 123 };

describe('Component Tests', () => {
  let alertService: AlertService;

  afterEach(() => {
    vitest.resetAllMocks();
  });

  describe('Assumption Management Detail Component', () => {
    let assumptionServiceStub: SinonStubbedInstance<AssumptionService>;
    let mountOptions: MountingOptions<AssumptionDetailsComponentType>['global'];

    beforeEach(() => {
      route = {};
      assumptionServiceStub = sinon.createStubInstance<AssumptionService>(AssumptionService);

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
          assumptionService: () => assumptionServiceStub,
        },
      };
    });

    describe('Navigate to details', () => {
      it('Should call load all on init', async () => {
        // GIVEN
        assumptionServiceStub.find.resolves(assumptionSample);
        route = {
          params: {
            assumptionId: `${123}`,
          },
        };
        const wrapper = shallowMount(AssumptionDetails, { global: mountOptions });
        const comp = wrapper.vm;
        // WHEN
        await comp.$nextTick();

        // THEN
        expect(comp.assumption).toMatchObject(assumptionSample);
      });
    });

    describe('Previous state', () => {
      it('Should go previous state', async () => {
        assumptionServiceStub.find.resolves(assumptionSample);
        const wrapper = shallowMount(AssumptionDetails, { global: mountOptions });
        const comp = wrapper.vm;
        await comp.$nextTick();

        comp.previousState();
        await comp.$nextTick();

        expect(routerGoMock).toHaveBeenCalledWith(-1);
      });
    });
  });
});
