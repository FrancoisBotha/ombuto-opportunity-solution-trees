import { beforeEach, describe, expect, it, vitest } from 'vitest';
import { type RouteLocation } from 'vue-router';

import { type MountingOptions, shallowMount } from '@vue/test-utils';
import dayjs from 'dayjs';
import sinon, { type SinonStubbedInstance } from 'sinon';

import OpportunityService from '@/entities/opportunity/opportunity.service';
import AlertService from '@/shared/alert/alert.service';
import { DATE_TIME_LONG_FORMAT } from '@/shared/composables/date-format';

import OpenQuestionUpdate from './open-question-update.vue';
import OpenQuestionService from './open-question.service';

type OpenQuestionUpdateComponentType = InstanceType<typeof OpenQuestionUpdate>;

let route: Partial<RouteLocation>;
const routerGoMock = vitest.fn();

vitest.mock('vue-router', () => ({
  useRoute: () => route,
  useRouter: () => ({ go: routerGoMock }),
}));

const openQuestionSample = { id: 123 };

describe('Component Tests', () => {
  let mountOptions: MountingOptions<OpenQuestionUpdateComponentType>['global'];
  let alertService: AlertService;

  describe('OpenQuestion Management Update Component', () => {
    let comp: OpenQuestionUpdateComponentType;
    let openQuestionServiceStub: SinonStubbedInstance<OpenQuestionService>;

    beforeEach(() => {
      route = {};
      openQuestionServiceStub = sinon.createStubInstance<OpenQuestionService>(OpenQuestionService);
      openQuestionServiceStub.retrieve.onFirstCall().resolves(Promise.resolve([]));

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
          openQuestionService: () => openQuestionServiceStub,
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
        const wrapper = shallowMount(OpenQuestionUpdate, { global: mountOptions });
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
        const wrapper = shallowMount(OpenQuestionUpdate, { global: mountOptions });
        comp = wrapper.vm;
        comp.openQuestion = openQuestionSample;
        openQuestionServiceStub.update.resolves(openQuestionSample);

        // WHEN
        comp.save();
        await comp.$nextTick();

        // THEN
        expect(openQuestionServiceStub.update.calledWith(openQuestionSample)).toBeTruthy();
        expect(comp.isSaving).toEqual(false);
      });

      it('Should call create service on save for new entity', async () => {
        // GIVEN
        const entity = {};
        openQuestionServiceStub.create.resolves(entity);
        const wrapper = shallowMount(OpenQuestionUpdate, { global: mountOptions });
        comp = wrapper.vm;
        comp.openQuestion = entity;

        // WHEN
        comp.save();
        await comp.$nextTick();

        // THEN
        expect(openQuestionServiceStub.create.calledWith(entity)).toBeTruthy();
        expect(comp.isSaving).toEqual(false);
      });
    });

    describe('Before route enter', () => {
      it('Should retrieve data', async () => {
        // GIVEN
        openQuestionServiceStub.find.resolves(openQuestionSample);
        openQuestionServiceStub.retrieve.resolves([openQuestionSample]);

        // WHEN
        route = {
          params: {
            openQuestionId: `${openQuestionSample.id}`,
          },
        };
        const wrapper = shallowMount(OpenQuestionUpdate, { global: mountOptions });
        comp = wrapper.vm;
        await comp.$nextTick();

        // THEN
        expect(comp.openQuestion).toMatchObject(openQuestionSample);
      });
    });

    describe('Previous state', () => {
      it('Should go previous state', async () => {
        openQuestionServiceStub.find.resolves(openQuestionSample);
        const wrapper = shallowMount(OpenQuestionUpdate, { global: mountOptions });
        comp = wrapper.vm;
        await comp.$nextTick();

        comp.previousState();
        await comp.$nextTick();

        expect(routerGoMock).toHaveBeenCalledWith(-1);
      });
    });
  });
});
