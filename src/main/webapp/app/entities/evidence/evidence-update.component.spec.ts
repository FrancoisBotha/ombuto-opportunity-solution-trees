import { beforeEach, describe, expect, it, vitest } from 'vitest';
import { type RouteLocation } from 'vue-router';

import { type MountingOptions, shallowMount } from '@vue/test-utils';
import dayjs from 'dayjs';
import sinon, { type SinonStubbedInstance } from 'sinon';

import AssumptionService from '@/entities/assumption/assumption.service';
import OpportunityService from '@/entities/opportunity/opportunity.service';
import AlertService from '@/shared/alert/alert.service';
import { DATE_TIME_LONG_FORMAT } from '@/shared/composables/date-format';

import EvidenceUpdate from './evidence-update.vue';
import EvidenceService from './evidence.service';

type EvidenceUpdateComponentType = InstanceType<typeof EvidenceUpdate>;

let route: Partial<RouteLocation>;
const routerGoMock = vitest.fn();

vitest.mock('vue-router', () => ({
  useRoute: () => route,
  useRouter: () => ({ go: routerGoMock }),
}));

const evidenceSample = { id: 123 };

describe('Component Tests', () => {
  let mountOptions: MountingOptions<EvidenceUpdateComponentType>['global'];
  let alertService: AlertService;

  describe('Evidence Management Update Component', () => {
    let comp: EvidenceUpdateComponentType;
    let evidenceServiceStub: SinonStubbedInstance<EvidenceService>;

    beforeEach(() => {
      route = {};
      evidenceServiceStub = sinon.createStubInstance<EvidenceService>(EvidenceService);
      evidenceServiceStub.retrieve.onFirstCall().resolves(Promise.resolve([]));

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
          evidenceService: () => evidenceServiceStub,
          opportunityService: () =>
            sinon.createStubInstance<OpportunityService>(OpportunityService, {
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
        const wrapper = shallowMount(EvidenceUpdate, { global: mountOptions });
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
        const wrapper = shallowMount(EvidenceUpdate, { global: mountOptions });
        comp = wrapper.vm;
        comp.evidence = evidenceSample;
        evidenceServiceStub.update.resolves(evidenceSample);

        // WHEN
        comp.save();
        await comp.$nextTick();

        // THEN
        expect(evidenceServiceStub.update.calledWith(evidenceSample)).toBeTruthy();
        expect(comp.isSaving).toEqual(false);
      });

      it('Should call create service on save for new entity', async () => {
        // GIVEN
        const entity = {};
        evidenceServiceStub.create.resolves(entity);
        const wrapper = shallowMount(EvidenceUpdate, { global: mountOptions });
        comp = wrapper.vm;
        comp.evidence = entity;

        // WHEN
        comp.save();
        await comp.$nextTick();

        // THEN
        expect(evidenceServiceStub.create.calledWith(entity)).toBeTruthy();
        expect(comp.isSaving).toEqual(false);
      });
    });

    describe('Before route enter', () => {
      it('Should retrieve data', async () => {
        // GIVEN
        evidenceServiceStub.find.resolves(evidenceSample);
        evidenceServiceStub.retrieve.resolves([evidenceSample]);

        // WHEN
        route = {
          params: {
            evidenceId: `${evidenceSample.id}`,
          },
        };
        const wrapper = shallowMount(EvidenceUpdate, { global: mountOptions });
        comp = wrapper.vm;
        await comp.$nextTick();

        // THEN
        expect(comp.evidence).toMatchObject(evidenceSample);
      });
    });

    describe('Previous state', () => {
      it('Should go previous state', async () => {
        evidenceServiceStub.find.resolves(evidenceSample);
        const wrapper = shallowMount(EvidenceUpdate, { global: mountOptions });
        comp = wrapper.vm;
        await comp.$nextTick();

        comp.previousState();
        await comp.$nextTick();

        expect(routerGoMock).toHaveBeenCalledWith(-1);
      });
    });
  });
});
