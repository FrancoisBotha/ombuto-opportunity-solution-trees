import { beforeEach, describe, expect, it, vitest } from 'vitest';
import { type RouteLocation } from 'vue-router';

import { type MountingOptions, shallowMount } from '@vue/test-utils';
import dayjs from 'dayjs';
import sinon, { type SinonStubbedInstance } from 'sinon';

import AssumptionService from '@/entities/assumption/assumption.service';
import SolutionService from '@/entities/solution/solution.service';
import AlertService from '@/shared/alert/alert.service';
import { DATE_TIME_LONG_FORMAT } from '@/shared/composables/date-format';

import ExperimentUpdate from './experiment-update.vue';
import ExperimentService from './experiment.service';

type ExperimentUpdateComponentType = InstanceType<typeof ExperimentUpdate>;

let route: Partial<RouteLocation>;
const routerGoMock = vitest.fn();

vitest.mock('vue-router', () => ({
  useRoute: () => route,
  useRouter: () => ({ go: routerGoMock }),
}));

const experimentSample = { id: 123 };

describe('Component Tests', () => {
  let mountOptions: MountingOptions<ExperimentUpdateComponentType>['global'];
  let alertService: AlertService;

  describe('Experiment Management Update Component', () => {
    let comp: ExperimentUpdateComponentType;
    let experimentServiceStub: SinonStubbedInstance<ExperimentService>;

    beforeEach(() => {
      route = {};
      experimentServiceStub = sinon.createStubInstance<ExperimentService>(ExperimentService);
      experimentServiceStub.retrieve.onFirstCall().resolves(Promise.resolve([]));

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
          experimentService: () => experimentServiceStub,
          solutionService: () =>
            sinon.createStubInstance<SolutionService>(SolutionService, {
              retrieve: sinon.stub().resolves({}),
            } as any),
          assumptionService: () =>
            sinon.createStubInstance<AssumptionService>(AssumptionService, {
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
        const wrapper = shallowMount(ExperimentUpdate, { global: mountOptions });
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
        const wrapper = shallowMount(ExperimentUpdate, { global: mountOptions });
        comp = wrapper.vm;
        comp.experiment = experimentSample;
        experimentServiceStub.update.resolves(experimentSample);

        // WHEN
        comp.save();
        await comp.$nextTick();

        // THEN
        expect(experimentServiceStub.update.calledWith(experimentSample)).toBeTruthy();
        expect(comp.isSaving).toEqual(false);
      });

      it('Should call create service on save for new entity', async () => {
        // GIVEN
        const entity = {};
        experimentServiceStub.create.resolves(entity);
        const wrapper = shallowMount(ExperimentUpdate, { global: mountOptions });
        comp = wrapper.vm;
        comp.experiment = entity;

        // WHEN
        comp.save();
        await comp.$nextTick();

        // THEN
        expect(experimentServiceStub.create.calledWith(entity)).toBeTruthy();
        expect(comp.isSaving).toEqual(false);
      });
    });

    describe('Before route enter', () => {
      it('Should retrieve data', async () => {
        // GIVEN
        experimentServiceStub.find.resolves(experimentSample);
        experimentServiceStub.retrieve.resolves([experimentSample]);

        // WHEN
        route = {
          params: {
            experimentId: `${experimentSample.id}`,
          },
        };
        const wrapper = shallowMount(ExperimentUpdate, { global: mountOptions });
        comp = wrapper.vm;
        await comp.$nextTick();

        // THEN
        expect(comp.experiment).toMatchObject(experimentSample);
      });
    });

    describe('Previous state', () => {
      it('Should go previous state', async () => {
        experimentServiceStub.find.resolves(experimentSample);
        const wrapper = shallowMount(ExperimentUpdate, { global: mountOptions });
        comp = wrapper.vm;
        await comp.$nextTick();

        comp.previousState();
        await comp.$nextTick();

        expect(routerGoMock).toHaveBeenCalledWith(-1);
      });
    });
  });
});
