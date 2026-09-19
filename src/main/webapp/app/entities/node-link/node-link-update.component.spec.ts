import { beforeEach, describe, expect, it, vitest } from 'vitest';
import { type RouteLocation } from 'vue-router';

import { type MountingOptions, shallowMount } from '@vue/test-utils';
import dayjs from 'dayjs';
import sinon, { type SinonStubbedInstance } from 'sinon';

import AssumptionService from '@/entities/assumption/assumption.service';
import EvidenceService from '@/entities/evidence/evidence.service';
import OpportunityService from '@/entities/opportunity/opportunity.service';
import OutcomeService from '@/entities/outcome/outcome.service';
import ProductService from '@/entities/product/product.service';
import SolutionService from '@/entities/solution/solution.service';
import AlertService from '@/shared/alert/alert.service';
import { DATE_TIME_LONG_FORMAT } from '@/shared/composables/date-format';

import NodeLinkUpdate from './node-link-update.vue';
import NodeLinkService from './node-link.service';

type NodeLinkUpdateComponentType = InstanceType<typeof NodeLinkUpdate>;

let route: Partial<RouteLocation>;
const routerGoMock = vitest.fn();

vitest.mock('vue-router', () => ({
  useRoute: () => route,
  useRouter: () => ({ go: routerGoMock }),
}));

const nodeLinkSample = { id: 123 };

describe('Component Tests', () => {
  let mountOptions: MountingOptions<NodeLinkUpdateComponentType>['global'];
  let alertService: AlertService;

  describe('NodeLink Management Update Component', () => {
    let comp: NodeLinkUpdateComponentType;
    let nodeLinkServiceStub: SinonStubbedInstance<NodeLinkService>;

    beforeEach(() => {
      route = {};
      nodeLinkServiceStub = sinon.createStubInstance<NodeLinkService>(NodeLinkService);
      nodeLinkServiceStub.retrieve.onFirstCall().resolves(Promise.resolve([]));

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
          nodeLinkService: () => nodeLinkServiceStub,
          productService: () =>
            sinon.createStubInstance<ProductService>(ProductService, {
              retrieve: sinon.stub().resolves({}),
            } as any),
          outcomeService: () =>
            sinon.createStubInstance<OutcomeService>(OutcomeService, {
              retrieve: sinon.stub().resolves({}),
            } as any),
          opportunityService: () =>
            sinon.createStubInstance<OpportunityService>(OpportunityService, {
              retrieve: sinon.stub().resolves({}),
            } as any),
          solutionService: () =>
            sinon.createStubInstance<SolutionService>(SolutionService, {
              retrieve: sinon.stub().resolves({}),
            } as any),
          assumptionService: () =>
            sinon.createStubInstance<AssumptionService>(AssumptionService, {
              retrieve: sinon.stub().resolves({}),
            } as any),
          evidenceService: () =>
            sinon.createStubInstance<EvidenceService>(EvidenceService, {
              retrieve: sinon.stub().resolves({}),
            } as any),
        },
      };
    });

    afterEach(() => {
      vitest.resetAllMocks();
    });

    describe('load', () => {
      beforeEach(() => {
        const wrapper = shallowMount(NodeLinkUpdate, { global: mountOptions });
        comp = wrapper.vm;
      });
      it('Should convert date from string', () => {
        // GIVEN
        const date = new Date('2019-10-15T11:42:02Z');

        // WHEN
        const convertedDate = comp.convertDateTimeFromServer(date);

        // THEN
        expect(convertedDate).toEqual(dayjs(date).format(DATE_TIME_LONG_FORMAT));
      });

      it('Should not convert date if date is not present', () => {
        expect(comp.convertDateTimeFromServer(null)).toBeNull();
      });
    });

    describe('save', () => {
      it('Should call update service on save for existing entity', async () => {
        // GIVEN
        const wrapper = shallowMount(NodeLinkUpdate, { global: mountOptions });
        comp = wrapper.vm;
        comp.nodeLink = nodeLinkSample;
        nodeLinkServiceStub.update.resolves(nodeLinkSample);

        // WHEN
        comp.save();
        await comp.$nextTick();

        // THEN
        expect(nodeLinkServiceStub.update.calledWith(nodeLinkSample)).toBeTruthy();
        expect(comp.isSaving).toEqual(false);
      });

      it('Should call create service on save for new entity', async () => {
        // GIVEN
        const entity = {};
        nodeLinkServiceStub.create.resolves(entity);
        const wrapper = shallowMount(NodeLinkUpdate, { global: mountOptions });
        comp = wrapper.vm;
        comp.nodeLink = entity;

        // WHEN
        comp.save();
        await comp.$nextTick();

        // THEN
        expect(nodeLinkServiceStub.create.calledWith(entity)).toBeTruthy();
        expect(comp.isSaving).toEqual(false);
      });
    });

    describe('Before route enter', () => {
      it('Should retrieve data', async () => {
        // GIVEN
        nodeLinkServiceStub.find.resolves(nodeLinkSample);
        nodeLinkServiceStub.retrieve.resolves([nodeLinkSample]);

        // WHEN
        route = {
          params: {
            nodeLinkId: `${nodeLinkSample.id}`,
          },
        };
        const wrapper = shallowMount(NodeLinkUpdate, { global: mountOptions });
        comp = wrapper.vm;
        await comp.$nextTick();

        // THEN
        expect(comp.nodeLink).toMatchObject(nodeLinkSample);
      });
    });

    describe('Previous state', () => {
      it('Should go previous state', async () => {
        nodeLinkServiceStub.find.resolves(nodeLinkSample);
        const wrapper = shallowMount(NodeLinkUpdate, { global: mountOptions });
        comp = wrapper.vm;
        await comp.$nextTick();

        comp.previousState();
        await comp.$nextTick();

        expect(routerGoMock).toHaveBeenCalledWith(-1);
      });
    });
  });
});
