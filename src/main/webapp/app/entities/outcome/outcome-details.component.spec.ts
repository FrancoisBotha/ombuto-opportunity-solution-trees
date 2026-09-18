import { beforeEach, describe, expect, it, vitest } from 'vitest';
import { type RouteLocation } from 'vue-router';

import { type MountingOptions, shallowMount } from '@vue/test-utils';
import sinon, { type SinonStubbedInstance } from 'sinon';

import AlertService from '@/shared/alert/alert.service';

import OutcomeDetails from './outcome-details.vue';
import OutcomeService from './outcome.service';

type OutcomeDetailsComponentType = InstanceType<typeof OutcomeDetails>;

let route: Partial<RouteLocation>;
const routerGoMock = vitest.fn();

vitest.mock('vue-router', () => ({
  useRoute: () => route,
  useRouter: () => ({ go: routerGoMock }),
}));

const outcomeSample = { id: 123 };

describe('Component Tests', () => {
  let alertService: AlertService;

  afterEach(() => {
    vitest.resetAllMocks();
  });

  describe('Outcome Management Detail Component', () => {
    let outcomeServiceStub: SinonStubbedInstance<OutcomeService>;
    let mountOptions: MountingOptions<OutcomeDetailsComponentType>['global'];

    beforeEach(() => {
      route = {};
      outcomeServiceStub = sinon.createStubInstance<OutcomeService>(OutcomeService);

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
          outcomeService: () => outcomeServiceStub,
        },
      };
    });

    describe('Navigate to details', () => {
      it('Should call load all on init', async () => {
        // GIVEN
        outcomeServiceStub.find.resolves(outcomeSample);
        route = {
          params: {
            outcomeId: `${123}`,
          },
        };
        const wrapper = shallowMount(OutcomeDetails, { global: mountOptions });
        const comp = wrapper.vm;
        // WHEN
        await comp.$nextTick();

        // THEN
        expect(comp.outcome).toMatchObject(outcomeSample);
      });
    });

    describe('Previous state', () => {
      it('Should go previous state', async () => {
        outcomeServiceStub.find.resolves(outcomeSample);
        const wrapper = shallowMount(OutcomeDetails, { global: mountOptions });
        const comp = wrapper.vm;
        await comp.$nextTick();

        comp.previousState();
        await comp.$nextTick();

        expect(routerGoMock).toHaveBeenCalledWith(-1);
      });
    });
  });
});
