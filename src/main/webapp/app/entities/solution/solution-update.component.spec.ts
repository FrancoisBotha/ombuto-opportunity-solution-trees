import { beforeEach, describe, expect, it, vitest } from 'vitest';
import { type RouteLocation } from 'vue-router';

import { type MountingOptions, shallowMount } from '@vue/test-utils';
import dayjs from 'dayjs';
import sinon, { type SinonStubbedInstance } from 'sinon';

import OpportunityService from '@/entities/opportunity/opportunity.service';
import TagService from '@/entities/tag/tag.service';
import UserService from '@/entities/user/user.service';
import AlertService from '@/shared/alert/alert.service';
import { DATE_TIME_LONG_FORMAT } from '@/shared/composables/date-format';

import SolutionUpdate from './solution-update.vue';
import SolutionService from './solution.service';

type SolutionUpdateComponentType = InstanceType<typeof SolutionUpdate>;

let route: Partial<RouteLocation>;
const routerGoMock = vitest.fn();

vitest.mock('vue-router', () => ({
  useRoute: () => route,
  useRouter: () => ({ go: routerGoMock }),
}));

const solutionSample = { id: 123 };

describe('Component Tests', () => {
  let mountOptions: MountingOptions<SolutionUpdateComponentType>['global'];
  let alertService: AlertService;

  describe('Solution Management Update Component', () => {
    let comp: SolutionUpdateComponentType;
    let solutionServiceStub: SinonStubbedInstance<SolutionService>;

    beforeEach(() => {
      route = {};
      solutionServiceStub = sinon.createStubInstance<SolutionService>(SolutionService);
      solutionServiceStub.retrieve.onFirstCall().resolves(Promise.resolve([]));

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
          solutionService: () => solutionServiceStub,
          opportunityService: () =>
            sinon.createStubInstance<OpportunityService>(OpportunityService, {
              retrieve: sinon.stub().resolves({}),
            } as any),

          userService: () =>
            sinon.createStubInstance<UserService>(UserService, {
              retrieve: sinon.stub().resolves({}),
            } as any),
          tagService: () =>
            sinon.createStubInstance<TagService>(TagService, {
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
        const wrapper = shallowMount(SolutionUpdate, { global: mountOptions });
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
        const wrapper = shallowMount(SolutionUpdate, { global: mountOptions });
        comp = wrapper.vm;
        comp.solution = solutionSample;
        solutionServiceStub.update.resolves(solutionSample);

        // WHEN
        comp.save();
        await comp.$nextTick();

        // THEN
        expect(solutionServiceStub.update.calledWith(solutionSample)).toBeTruthy();
        expect(comp.isSaving).toEqual(false);
      });

      it('Should call create service on save for new entity', async () => {
        // GIVEN
        const entity = {};
        solutionServiceStub.create.resolves(entity);
        const wrapper = shallowMount(SolutionUpdate, { global: mountOptions });
        comp = wrapper.vm;
        comp.solution = entity;

        // WHEN
        comp.save();
        await comp.$nextTick();

        // THEN
        expect(solutionServiceStub.create.calledWith(entity)).toBeTruthy();
        expect(comp.isSaving).toEqual(false);
      });
    });

    describe('Before route enter', () => {
      it('Should retrieve data', async () => {
        // GIVEN
        solutionServiceStub.find.resolves(solutionSample);
        solutionServiceStub.retrieve.resolves([solutionSample]);

        // WHEN
        route = {
          params: {
            solutionId: `${solutionSample.id}`,
          },
        };
        const wrapper = shallowMount(SolutionUpdate, { global: mountOptions });
        comp = wrapper.vm;
        await comp.$nextTick();

        // THEN
        expect(comp.solution).toMatchObject(solutionSample);
      });
    });

    describe('Previous state', () => {
      it('Should go previous state', async () => {
        solutionServiceStub.find.resolves(solutionSample);
        const wrapper = shallowMount(SolutionUpdate, { global: mountOptions });
        comp = wrapper.vm;
        await comp.$nextTick();

        comp.previousState();
        await comp.$nextTick();

        expect(routerGoMock).toHaveBeenCalledWith(-1);
      });
    });
  });
});
