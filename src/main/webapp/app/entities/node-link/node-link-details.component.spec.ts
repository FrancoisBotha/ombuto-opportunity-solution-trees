import { beforeEach, describe, expect, it, vitest } from 'vitest';
import { type RouteLocation } from 'vue-router';

import { type MountingOptions, shallowMount } from '@vue/test-utils';
import sinon, { type SinonStubbedInstance } from 'sinon';

import AlertService from '@/shared/alert/alert.service';

import NodeLinkDetails from './node-link-details.vue';
import NodeLinkService from './node-link.service';

type NodeLinkDetailsComponentType = InstanceType<typeof NodeLinkDetails>;

let route: Partial<RouteLocation>;
const routerGoMock = vitest.fn();

vitest.mock('vue-router', () => ({
  useRoute: () => route,
  useRouter: () => ({ go: routerGoMock }),
}));

const nodeLinkSample = { id: 123 };

describe('Component Tests', () => {
  let alertService: AlertService;

  afterEach(() => {
    vitest.resetAllMocks();
  });

  describe('NodeLink Management Detail Component', () => {
    let nodeLinkServiceStub: SinonStubbedInstance<NodeLinkService>;
    let mountOptions: MountingOptions<NodeLinkDetailsComponentType>['global'];

    beforeEach(() => {
      route = {};
      nodeLinkServiceStub = sinon.createStubInstance<NodeLinkService>(NodeLinkService);

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
          nodeLinkService: () => nodeLinkServiceStub,
        },
      };
    });

    describe('Navigate to details', () => {
      it('Should call load all on init', async () => {
        // GIVEN
        nodeLinkServiceStub.find.resolves(nodeLinkSample);
        route = {
          params: {
            nodeLinkId: `${123}`,
          },
        };
        const wrapper = shallowMount(NodeLinkDetails, { global: mountOptions });
        const comp = wrapper.vm;
        // WHEN
        await comp.$nextTick();

        // THEN
        expect(comp.nodeLink).toMatchObject(nodeLinkSample);
      });
    });

    describe('Previous state', () => {
      it('Should go previous state', async () => {
        nodeLinkServiceStub.find.resolves(nodeLinkSample);
        const wrapper = shallowMount(NodeLinkDetails, { global: mountOptions });
        const comp = wrapper.vm;
        await comp.$nextTick();

        comp.previousState();
        await comp.$nextTick();

        expect(routerGoMock).toHaveBeenCalledWith(-1);
      });
    });
  });
});
