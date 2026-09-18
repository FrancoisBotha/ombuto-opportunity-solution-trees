import { beforeEach, describe, expect, it, vitest } from 'vitest';
import { type RouteLocation } from 'vue-router';

import { type MountingOptions, shallowMount } from '@vue/test-utils';
import sinon, { type SinonStubbedInstance } from 'sinon';

import OpportunityService from '@/entities/opportunity/opportunity.service';
import AlertService from '@/shared/alert/alert.service';

import OpportunityLinkUpdate from './opportunity-link-update.vue';
import OpportunityLinkService from './opportunity-link.service';

type OpportunityLinkUpdateComponentType = InstanceType<typeof OpportunityLinkUpdate>;

let route: Partial<RouteLocation>;
const routerGoMock = vitest.fn();

vitest.mock('vue-router', () => ({
  useRoute: () => route,
  useRouter: () => ({ go: routerGoMock }),
}));

const opportunityLinkSample = { id: 123 };

describe('Component Tests', () => {
  let mountOptions: MountingOptions<OpportunityLinkUpdateComponentType>['global'];
  let alertService: AlertService;

  describe('OpportunityLink Management Update Component', () => {
    let comp: OpportunityLinkUpdateComponentType;
    let opportunityLinkServiceStub: SinonStubbedInstance<OpportunityLinkService>;

    beforeEach(() => {
      route = {};
      opportunityLinkServiceStub = sinon.createStubInstance<OpportunityLinkService>(OpportunityLinkService);
      opportunityLinkServiceStub.retrieve.onFirstCall().resolves(Promise.resolve([]));

      alertService = new AlertService({
        toast: {
          show: vitest.fn(),
        } as any,
      });

      mountOptions = {
        stubs: {
          'font-awesome-icon': true,
          'b-input-group': true,
          'b-input-group-prepend': true,
          'b-form-datepicker': true,
          'b-form-input': true,
        },
        provide: {
          alertService,
          opportunityLinkService: () => opportunityLinkServiceStub,
          opportunityService: () =>
            sinon.createStubInstance<OpportunityService>(OpportunityService, {
              retrieve: sinon.stub().resolves({}),
            } as any),
        },
      };
    });

    afterEach(() => {
      vitest.resetAllMocks();
    });

    describe('save', () => {
      it('Should call update service on save for existing entity', async () => {
        // GIVEN
        const wrapper = shallowMount(OpportunityLinkUpdate, { global: mountOptions });
        comp = wrapper.vm;
        comp.opportunityLink = opportunityLinkSample;
        opportunityLinkServiceStub.update.resolves(opportunityLinkSample);

        // WHEN
        comp.save();
        await comp.$nextTick();

        // THEN
        expect(opportunityLinkServiceStub.update.calledWith(opportunityLinkSample)).toBeTruthy();
        expect(comp.isSaving).toEqual(false);
      });

      it('Should call create service on save for new entity', async () => {
        // GIVEN
        const entity = {};
        opportunityLinkServiceStub.create.resolves(entity);
        const wrapper = shallowMount(OpportunityLinkUpdate, { global: mountOptions });
        comp = wrapper.vm;
        comp.opportunityLink = entity;

        // WHEN
        comp.save();
        await comp.$nextTick();

        // THEN
        expect(opportunityLinkServiceStub.create.calledWith(entity)).toBeTruthy();
        expect(comp.isSaving).toEqual(false);
      });
    });

    describe('Before route enter', () => {
      it('Should retrieve data', async () => {
        // GIVEN
        opportunityLinkServiceStub.find.resolves(opportunityLinkSample);
        opportunityLinkServiceStub.retrieve.resolves([opportunityLinkSample]);

        // WHEN
        route = {
          params: {
            opportunityLinkId: `${opportunityLinkSample.id}`,
          },
        };
        const wrapper = shallowMount(OpportunityLinkUpdate, { global: mountOptions });
        comp = wrapper.vm;
        await comp.$nextTick();

        // THEN
        expect(comp.opportunityLink).toMatchObject(opportunityLinkSample);
      });
    });

    describe('Previous state', () => {
      it('Should go previous state', async () => {
        opportunityLinkServiceStub.find.resolves(opportunityLinkSample);
        const wrapper = shallowMount(OpportunityLinkUpdate, { global: mountOptions });
        comp = wrapper.vm;
        await comp.$nextTick();

        comp.previousState();
        await comp.$nextTick();

        expect(routerGoMock).toHaveBeenCalledWith(-1);
      });
    });
  });
});
