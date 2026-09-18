import { beforeEach, describe, expect, it, vitest } from 'vitest';
import { type RouteLocation } from 'vue-router';

import { type MountingOptions, shallowMount } from '@vue/test-utils';
import sinon, { type SinonStubbedInstance } from 'sinon';

import AlertService from '@/shared/alert/alert.service';

import OpportunityLinkDetails from './opportunity-link-details.vue';
import OpportunityLinkService from './opportunity-link.service';

type OpportunityLinkDetailsComponentType = InstanceType<typeof OpportunityLinkDetails>;

let route: Partial<RouteLocation>;
const routerGoMock = vitest.fn();

vitest.mock('vue-router', () => ({
  useRoute: () => route,
  useRouter: () => ({ go: routerGoMock }),
}));

const opportunityLinkSample = { id: 123 };

describe('Component Tests', () => {
  let alertService: AlertService;

  afterEach(() => {
    vitest.resetAllMocks();
  });

  describe('OpportunityLink Management Detail Component', () => {
    let opportunityLinkServiceStub: SinonStubbedInstance<OpportunityLinkService>;
    let mountOptions: MountingOptions<OpportunityLinkDetailsComponentType>['global'];

    beforeEach(() => {
      route = {};
      opportunityLinkServiceStub = sinon.createStubInstance<OpportunityLinkService>(OpportunityLinkService);

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
          opportunityLinkService: () => opportunityLinkServiceStub,
        },
      };
    });

    describe('Navigate to details', () => {
      it('Should call load all on init', async () => {
        // GIVEN
        opportunityLinkServiceStub.find.resolves(opportunityLinkSample);
        route = {
          params: {
            opportunityLinkId: `${123}`,
          },
        };
        const wrapper = shallowMount(OpportunityLinkDetails, { global: mountOptions });
        const comp = wrapper.vm;
        // WHEN
        await comp.$nextTick();

        // THEN
        expect(comp.opportunityLink).toMatchObject(opportunityLinkSample);
      });
    });

    describe('Previous state', () => {
      it('Should go previous state', async () => {
        opportunityLinkServiceStub.find.resolves(opportunityLinkSample);
        const wrapper = shallowMount(OpportunityLinkDetails, { global: mountOptions });
        const comp = wrapper.vm;
        await comp.$nextTick();

        comp.previousState();
        await comp.$nextTick();

        expect(routerGoMock).toHaveBeenCalledWith(-1);
      });
    });
  });
});
