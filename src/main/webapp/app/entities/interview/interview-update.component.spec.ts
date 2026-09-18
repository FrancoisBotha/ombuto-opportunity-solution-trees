import { beforeEach, describe, expect, it, vitest } from 'vitest';
import { type RouteLocation } from 'vue-router';

import { type MountingOptions, shallowMount } from '@vue/test-utils';
import dayjs from 'dayjs';
import sinon, { type SinonStubbedInstance } from 'sinon';

import OpportunityService from '@/entities/opportunity/opportunity.service';
import ProductService from '@/entities/product/product.service';
import UserService from '@/entities/user/user.service';
import AlertService from '@/shared/alert/alert.service';
import { DATE_TIME_LONG_FORMAT } from '@/shared/composables/date-format';

import InterviewUpdate from './interview-update.vue';
import InterviewService from './interview.service';

type InterviewUpdateComponentType = InstanceType<typeof InterviewUpdate>;

let route: Partial<RouteLocation>;
const routerGoMock = vitest.fn();

vitest.mock('vue-router', () => ({
  useRoute: () => route,
  useRouter: () => ({ go: routerGoMock }),
}));

const interviewSample = { id: 123 };

describe('Component Tests', () => {
  let mountOptions: MountingOptions<InterviewUpdateComponentType>['global'];
  let alertService: AlertService;

  describe('Interview Management Update Component', () => {
    let comp: InterviewUpdateComponentType;
    let interviewServiceStub: SinonStubbedInstance<InterviewService>;

    beforeEach(() => {
      route = {};
      interviewServiceStub = sinon.createStubInstance<InterviewService>(InterviewService);
      interviewServiceStub.retrieve.onFirstCall().resolves(Promise.resolve([]));

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
          interviewService: () => interviewServiceStub,
          productService: () =>
            sinon.createStubInstance<ProductService>(ProductService, {
              retrieve: sinon.stub().resolves({}),
            } as any),

          userService: () =>
            sinon.createStubInstance<UserService>(UserService, {
              retrieve: sinon.stub().resolves({}),
            } as any),
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

    describe('load', () => {
      beforeEach(() => {
        const wrapper = shallowMount(InterviewUpdate, { global: mountOptions });
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
        const wrapper = shallowMount(InterviewUpdate, { global: mountOptions });
        comp = wrapper.vm;
        comp.interview = interviewSample;
        interviewServiceStub.update.resolves(interviewSample);

        // WHEN
        comp.save();
        await comp.$nextTick();

        // THEN
        expect(interviewServiceStub.update.calledWith(interviewSample)).toBeTruthy();
        expect(comp.isSaving).toEqual(false);
      });

      it('Should call create service on save for new entity', async () => {
        // GIVEN
        const entity = {};
        interviewServiceStub.create.resolves(entity);
        const wrapper = shallowMount(InterviewUpdate, { global: mountOptions });
        comp = wrapper.vm;
        comp.interview = entity;

        // WHEN
        comp.save();
        await comp.$nextTick();

        // THEN
        expect(interviewServiceStub.create.calledWith(entity)).toBeTruthy();
        expect(comp.isSaving).toEqual(false);
      });
    });

    describe('Before route enter', () => {
      it('Should retrieve data', async () => {
        // GIVEN
        interviewServiceStub.find.resolves(interviewSample);
        interviewServiceStub.retrieve.resolves([interviewSample]);

        // WHEN
        route = {
          params: {
            interviewId: `${interviewSample.id}`,
          },
        };
        const wrapper = shallowMount(InterviewUpdate, { global: mountOptions });
        comp = wrapper.vm;
        await comp.$nextTick();

        // THEN
        expect(comp.interview).toMatchObject(interviewSample);
      });
    });

    describe('Previous state', () => {
      it('Should go previous state', async () => {
        interviewServiceStub.find.resolves(interviewSample);
        const wrapper = shallowMount(InterviewUpdate, { global: mountOptions });
        comp = wrapper.vm;
        await comp.$nextTick();

        comp.previousState();
        await comp.$nextTick();

        expect(routerGoMock).toHaveBeenCalledWith(-1);
      });
    });
  });
});
