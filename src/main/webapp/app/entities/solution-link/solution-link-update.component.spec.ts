import { beforeEach, describe, expect, it, vitest } from 'vitest';
import { type RouteLocation } from 'vue-router';

import { type MountingOptions, shallowMount } from '@vue/test-utils';
import sinon, { type SinonStubbedInstance } from 'sinon';

import SolutionService from '@/entities/solution/solution.service';
import AlertService from '@/shared/alert/alert.service';

import SolutionLinkUpdate from './solution-link-update.vue';
import SolutionLinkService from './solution-link.service';

type SolutionLinkUpdateComponentType = InstanceType<typeof SolutionLinkUpdate>;

let route: Partial<RouteLocation>;
const routerGoMock = vitest.fn();

vitest.mock('vue-router', () => ({
  useRoute: () => route,
  useRouter: () => ({ go: routerGoMock }),
}));

const solutionLinkSample = { id: 123 };

describe('Component Tests', () => {
  let mountOptions: MountingOptions<SolutionLinkUpdateComponentType>['global'];
  let alertService: AlertService;

  describe('SolutionLink Management Update Component', () => {
    let comp: SolutionLinkUpdateComponentType;
    let solutionLinkServiceStub: SinonStubbedInstance<SolutionLinkService>;

    beforeEach(() => {
      route = {};
      solutionLinkServiceStub = sinon.createStubInstance<SolutionLinkService>(SolutionLinkService);
      solutionLinkServiceStub.retrieve.onFirstCall().resolves(Promise.resolve([]));

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
          solutionLinkService: () => solutionLinkServiceStub,
          solutionService: () =>
            sinon.createStubInstance<SolutionService>(SolutionService, {
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
        const wrapper = shallowMount(SolutionLinkUpdate, { global: mountOptions });
        comp = wrapper.vm;
        comp.solutionLink = solutionLinkSample;
        solutionLinkServiceStub.update.resolves(solutionLinkSample);

        // WHEN
        comp.save();
        await comp.$nextTick();

        // THEN
        expect(solutionLinkServiceStub.update.calledWith(solutionLinkSample)).toBeTruthy();
        expect(comp.isSaving).toEqual(false);
      });

      it('Should call create service on save for new entity', async () => {
        // GIVEN
        const entity = {};
        solutionLinkServiceStub.create.resolves(entity);
        const wrapper = shallowMount(SolutionLinkUpdate, { global: mountOptions });
        comp = wrapper.vm;
        comp.solutionLink = entity;

        // WHEN
        comp.save();
        await comp.$nextTick();

        // THEN
        expect(solutionLinkServiceStub.create.calledWith(entity)).toBeTruthy();
        expect(comp.isSaving).toEqual(false);
      });
    });

    describe('Before route enter', () => {
      it('Should retrieve data', async () => {
        // GIVEN
        solutionLinkServiceStub.find.resolves(solutionLinkSample);
        solutionLinkServiceStub.retrieve.resolves([solutionLinkSample]);

        // WHEN
        route = {
          params: {
            solutionLinkId: `${solutionLinkSample.id}`,
          },
        };
        const wrapper = shallowMount(SolutionLinkUpdate, { global: mountOptions });
        comp = wrapper.vm;
        await comp.$nextTick();

        // THEN
        expect(comp.solutionLink).toMatchObject(solutionLinkSample);
      });
    });

    describe('Previous state', () => {
      it('Should go previous state', async () => {
        solutionLinkServiceStub.find.resolves(solutionLinkSample);
        const wrapper = shallowMount(SolutionLinkUpdate, { global: mountOptions });
        comp = wrapper.vm;
        await comp.$nextTick();

        comp.previousState();
        await comp.$nextTick();

        expect(routerGoMock).toHaveBeenCalledWith(-1);
      });
    });
  });
});
