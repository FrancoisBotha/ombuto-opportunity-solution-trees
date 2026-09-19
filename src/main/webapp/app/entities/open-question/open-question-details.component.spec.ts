import { beforeEach, describe, expect, it, vitest } from 'vitest';
import { type RouteLocation } from 'vue-router';

import { type MountingOptions, shallowMount } from '@vue/test-utils';
import sinon, { type SinonStubbedInstance } from 'sinon';

import AlertService from '@/shared/alert/alert.service';

import OpenQuestionDetails from './open-question-details.vue';
import OpenQuestionService from './open-question.service';

type OpenQuestionDetailsComponentType = InstanceType<typeof OpenQuestionDetails>;

let route: Partial<RouteLocation>;
const routerGoMock = vitest.fn();

vitest.mock('vue-router', () => ({
  useRoute: () => route,
  useRouter: () => ({ go: routerGoMock }),
}));

const openQuestionSample = { id: 123 };

describe('Component Tests', () => {
  let alertService: AlertService;

  afterEach(() => {
    vitest.resetAllMocks();
  });

  describe('OpenQuestion Management Detail Component', () => {
    let openQuestionServiceStub: SinonStubbedInstance<OpenQuestionService>;
    let mountOptions: MountingOptions<OpenQuestionDetailsComponentType>['global'];

    beforeEach(() => {
      route = {};
      openQuestionServiceStub = sinon.createStubInstance<OpenQuestionService>(OpenQuestionService);

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
          openQuestionService: () => openQuestionServiceStub,
        },
      };
    });

    describe('Navigate to details', () => {
      it('Should call load all on init', async () => {
        // GIVEN
        openQuestionServiceStub.find.resolves(openQuestionSample);
        route = {
          params: {
            openQuestionId: `${123}`,
          },
        };
        const wrapper = shallowMount(OpenQuestionDetails, { global: mountOptions });
        const comp = wrapper.vm;
        // WHEN
        await comp.$nextTick();

        // THEN
        expect(comp.openQuestion).toMatchObject(openQuestionSample);
      });
    });

    describe('Previous state', () => {
      it('Should go previous state', async () => {
        openQuestionServiceStub.find.resolves(openQuestionSample);
        const wrapper = shallowMount(OpenQuestionDetails, { global: mountOptions });
        const comp = wrapper.vm;
        await comp.$nextTick();

        comp.previousState();
        await comp.$nextTick();

        expect(routerGoMock).toHaveBeenCalledWith(-1);
      });
    });
  });
});
