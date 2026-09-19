import { beforeEach, describe, expect, it, vitest } from 'vitest';
import { type RouteLocation } from 'vue-router';

import { type MountingOptions, shallowMount } from '@vue/test-utils';
import dayjs from 'dayjs';
import sinon, { type SinonStubbedInstance } from 'sinon';

import SolutionService from '@/entities/solution/solution.service';
import UserService from '@/entities/user/user.service';
import AlertService from '@/shared/alert/alert.service';
import { DATE_TIME_LONG_FORMAT } from '@/shared/composables/date-format';

import AssumptionUpdate from './assumption-update.vue';
import AssumptionService from './assumption.service';

type AssumptionUpdateComponentType = InstanceType<typeof AssumptionUpdate>;

let route: Partial<RouteLocation>;
const routerGoMock = vitest.fn();

vitest.mock('vue-router', () => ({
  useRoute: () => route,
  useRouter: () => ({ go: routerGoMock }),
}));

const assumptionSample = { id: 123 };

describe('Component Tests', () => {
  let mountOptions: MountingOptions<AssumptionUpdateComponentType>['global'];
  let alertService: AlertService;

  describe('Assumption Management Update Component', () => {
    let comp: AssumptionUpdateComponentType;
    let assumptionServiceStub: SinonStubbedInstance<AssumptionService>;

    beforeEach(() => {
      route = {};
      assumptionServiceStub = sinon.createStubInstance<AssumptionService>(AssumptionService);
      assumptionServiceStub.retrieve.onFirstCall().resolves(Promise.resolve([]));

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
          assumptionService: () => assumptionServiceStub,
          solutionService: () =>
            sinon.createStubInstance<SolutionService>(SolutionService, {
              retrieve: sinon.stub().resolves({}),
            } as any),

          userService: () =>
            sinon.createStubInstance<UserService>(UserService, {
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
        const wrapper = shallowMount(AssumptionUpdate, { global: mountOptions });
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
        const wrapper = shallowMount(AssumptionUpdate, { global: mountOptions });
        comp = wrapper.vm;
        comp.assumption = assumptionSample;
        assumptionServiceStub.update.resolves(assumptionSample);

        // WHEN
        comp.save();
        await comp.$nextTick();

        // THEN
        expect(assumptionServiceStub.update.calledWith(assumptionSample)).toBeTruthy();
        expect(comp.isSaving).toEqual(false);
      });

      it('Should call create service on save for new entity', async () => {
        // GIVEN
        const entity = {};
        assumptionServiceStub.create.resolves(entity);
        const wrapper = shallowMount(AssumptionUpdate, { global: mountOptions });
        comp = wrapper.vm;
        comp.assumption = entity;

        // WHEN
        comp.save();
        await comp.$nextTick();

        // THEN
        expect(assumptionServiceStub.create.calledWith(entity)).toBeTruthy();
        expect(comp.isSaving).toEqual(false);
      });
    });

    describe('Before route enter', () => {
      it('Should retrieve data', async () => {
        // GIVEN
        assumptionServiceStub.find.resolves(assumptionSample);
        assumptionServiceStub.retrieve.resolves([assumptionSample]);

        // WHEN
        route = {
          params: {
            assumptionId: `${assumptionSample.id}`,
          },
        };
        const wrapper = shallowMount(AssumptionUpdate, { global: mountOptions });
        comp = wrapper.vm;
        await comp.$nextTick();

        // THEN
        expect(comp.assumption).toMatchObject(assumptionSample);
      });
    });

    describe('Previous state', () => {
      it('Should go previous state', async () => {
        assumptionServiceStub.find.resolves(assumptionSample);
        const wrapper = shallowMount(AssumptionUpdate, { global: mountOptions });
        comp = wrapper.vm;
        await comp.$nextTick();

        comp.previousState();
        await comp.$nextTick();

        expect(routerGoMock).toHaveBeenCalledWith(-1);
      });
    });
  });
});
