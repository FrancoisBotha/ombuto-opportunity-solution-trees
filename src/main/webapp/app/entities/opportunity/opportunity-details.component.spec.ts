import { beforeEach, describe, expect, it, vitest } from 'vitest';
import { type RouteLocation } from 'vue-router';

import { type MountingOptions, shallowMount } from '@vue/test-utils';
import sinon, { type SinonStubbedInstance } from 'sinon';

import AlertService from '@/shared/alert/alert.service';

import OpportunityDetails from './opportunity-details.vue';
import OpportunityService from './opportunity.service';

type OpportunityDetailsComponentType = InstanceType<typeof OpportunityDetails>;

let route: Partial<RouteLocation>;
const routerGoMock = vitest.fn();

vitest.mock('vue-router', () => ({
  useRoute: () => route,
  useRouter: () => ({ go: routerGoMock }),
}));

const opportunitySample = { id: 123 };

describe('Component Tests', () => {
  let alertService: AlertService;

  afterEach(() => {
    vitest.resetAllMocks();
  });

  describe('Opportunity Management Detail Component', () => {
    let opportunityServiceStub: SinonStubbedInstance<OpportunityService>;
    let mountOptions: MountingOptions<OpportunityDetailsComponentType>['global'];

    beforeEach(() => {
      route = {};
      opportunityServiceStub = sinon.createStubInstance<OpportunityService>(OpportunityService);

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
          opportunityService: () => opportunityServiceStub,
        },
      };
    });

    describe('Navigate to details', () => {
      it('Should call load all on init', async () => {
        // GIVEN
        opportunityServiceStub.find.resolves(opportunitySample);
        route = {
          params: {
            opportunityId: `${123}`,
          },
        };
        const wrapper = shallowMount(OpportunityDetails, { global: mountOptions });
        const comp = wrapper.vm;
        // WHEN
        await comp.$nextTick();

        // THEN
        expect(comp.opportunity).toMatchObject(opportunitySample);
      });
    });

    describe('Previous state', () => {
      it('Should go previous state', async () => {
        opportunityServiceStub.find.resolves(opportunitySample);
        const wrapper = shallowMount(OpportunityDetails, { global: mountOptions });
        const comp = wrapper.vm;
        await comp.$nextTick();

        comp.previousState();
        await comp.$nextTick();

        expect(routerGoMock).toHaveBeenCalledWith(-1);
      });
    });
  });
});
