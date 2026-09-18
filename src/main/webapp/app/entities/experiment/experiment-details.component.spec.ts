import { beforeEach, describe, expect, it, vitest } from 'vitest';
import { type RouteLocation } from 'vue-router';

import { type MountingOptions, shallowMount } from '@vue/test-utils';
import sinon, { type SinonStubbedInstance } from 'sinon';

import AlertService from '@/shared/alert/alert.service';

import ExperimentDetails from './experiment-details.vue';
import ExperimentService from './experiment.service';

type ExperimentDetailsComponentType = InstanceType<typeof ExperimentDetails>;

let route: Partial<RouteLocation>;
const routerGoMock = vitest.fn();

vitest.mock('vue-router', () => ({
  useRoute: () => route,
  useRouter: () => ({ go: routerGoMock }),
}));

const experimentSample = { id: 123 };

describe('Component Tests', () => {
  let alertService: AlertService;

  afterEach(() => {
    vitest.resetAllMocks();
  });

  describe('Experiment Management Detail Component', () => {
    let experimentServiceStub: SinonStubbedInstance<ExperimentService>;
    let mountOptions: MountingOptions<ExperimentDetailsComponentType>['global'];

    beforeEach(() => {
      route = {};
      experimentServiceStub = sinon.createStubInstance<ExperimentService>(ExperimentService);

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
          experimentService: () => experimentServiceStub,
        },
      };
    });

    describe('Navigate to details', () => {
      it('Should call load all on init', async () => {
        // GIVEN
        experimentServiceStub.find.resolves(experimentSample);
        route = {
          params: {
            experimentId: `${123}`,
          },
        };
        const wrapper = shallowMount(ExperimentDetails, { global: mountOptions });
        const comp = wrapper.vm;
        // WHEN
        await comp.$nextTick();

        // THEN
        expect(comp.experiment).toMatchObject(experimentSample);
      });
    });

    describe('Previous state', () => {
      it('Should go previous state', async () => {
        experimentServiceStub.find.resolves(experimentSample);
        const wrapper = shallowMount(ExperimentDetails, { global: mountOptions });
        const comp = wrapper.vm;
        await comp.$nextTick();

        comp.previousState();
        await comp.$nextTick();

        expect(routerGoMock).toHaveBeenCalledWith(-1);
      });
    });
  });
});
