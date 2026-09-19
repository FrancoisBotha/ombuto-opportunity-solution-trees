import { beforeEach, describe, expect, it, vitest } from 'vitest';
import { type RouteLocation } from 'vue-router';

import { type MountingOptions, shallowMount } from '@vue/test-utils';
import sinon, { type SinonStubbedInstance } from 'sinon';

import AlertService from '@/shared/alert/alert.service';

import NodeHistoryDetails from './node-history-details.vue';
import NodeHistoryService from './node-history.service';

type NodeHistoryDetailsComponentType = InstanceType<typeof NodeHistoryDetails>;

let route: Partial<RouteLocation>;
const routerGoMock = vitest.fn();

vitest.mock('vue-router', () => ({
  useRoute: () => route,
  useRouter: () => ({ go: routerGoMock }),
}));

const nodeHistorySample = { id: 123 };

describe('Component Tests', () => {
  let alertService: AlertService;

  afterEach(() => {
    vitest.resetAllMocks();
  });

  describe('NodeHistory Management Detail Component', () => {
    let nodeHistoryServiceStub: SinonStubbedInstance<NodeHistoryService>;
    let mountOptions: MountingOptions<NodeHistoryDetailsComponentType>['global'];

    beforeEach(() => {
      route = {};
      nodeHistoryServiceStub = sinon.createStubInstance<NodeHistoryService>(NodeHistoryService);

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
          nodeHistoryService: () => nodeHistoryServiceStub,
        },
      };
    });

    describe('Navigate to details', () => {
      it('Should call load all on init', async () => {
        // GIVEN
        nodeHistoryServiceStub.find.resolves(nodeHistorySample);
        route = {
          params: {
            nodeHistoryId: `${123}`,
          },
        };
        const wrapper = shallowMount(NodeHistoryDetails, { global: mountOptions });
        const comp = wrapper.vm;
        // WHEN
        await comp.$nextTick();

        // THEN
        expect(comp.nodeHistory).toMatchObject(nodeHistorySample);
      });
    });

    describe('Previous state', () => {
      it('Should go previous state', async () => {
        nodeHistoryServiceStub.find.resolves(nodeHistorySample);
        const wrapper = shallowMount(NodeHistoryDetails, { global: mountOptions });
        const comp = wrapper.vm;
        await comp.$nextTick();

        comp.previousState();
        await comp.$nextTick();

        expect(routerGoMock).toHaveBeenCalledWith(-1);
      });
    });
  });
});
