import { beforeEach, describe, expect, it, vitest } from 'vitest';
import { type RouteLocation } from 'vue-router';

import { type MountingOptions, shallowMount } from '@vue/test-utils';
import sinon, { type SinonStubbedInstance } from 'sinon';

import AlertService from '@/shared/alert/alert.service';

import InterviewDetails from './interview-details.vue';
import InterviewService from './interview.service';

type InterviewDetailsComponentType = InstanceType<typeof InterviewDetails>;

let route: Partial<RouteLocation>;
const routerGoMock = vitest.fn();

vitest.mock('vue-router', () => ({
  useRoute: () => route,
  useRouter: () => ({ go: routerGoMock }),
}));

const interviewSample = { id: 123 };

describe('Component Tests', () => {
  let alertService: AlertService;

  afterEach(() => {
    vitest.resetAllMocks();
  });

  describe('Interview Management Detail Component', () => {
    let interviewServiceStub: SinonStubbedInstance<InterviewService>;
    let mountOptions: MountingOptions<InterviewDetailsComponentType>['global'];

    beforeEach(() => {
      route = {};
      interviewServiceStub = sinon.createStubInstance<InterviewService>(InterviewService);

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
          interviewService: () => interviewServiceStub,
        },
      };
    });

    describe('Navigate to details', () => {
      it('Should call load all on init', async () => {
        // GIVEN
        interviewServiceStub.find.resolves(interviewSample);
        route = {
          params: {
            interviewId: `${123}`,
          },
        };
        const wrapper = shallowMount(InterviewDetails, { global: mountOptions });
        const comp = wrapper.vm;
        // WHEN
        await comp.$nextTick();

        // THEN
        expect(comp.interview).toMatchObject(interviewSample);
      });
    });

    describe('Previous state', () => {
      it('Should go previous state', async () => {
        interviewServiceStub.find.resolves(interviewSample);
        const wrapper = shallowMount(InterviewDetails, { global: mountOptions });
        const comp = wrapper.vm;
        await comp.$nextTick();

        comp.previousState();
        await comp.$nextTick();

        expect(routerGoMock).toHaveBeenCalledWith(-1);
      });
    });
  });
});
