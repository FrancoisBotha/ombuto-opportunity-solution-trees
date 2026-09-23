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
import UserService from '@/entities/user/user.service';
import AlertService from '@/shared/alert/alert.service';
import { DATE_TIME_LONG_FORMAT } from '@/shared/composables/date-format';

import MeetingTranscriptUpdate from './meeting-transcript-update.vue';
import MeetingTranscriptService from './meeting-transcript.service';

type MeetingTranscriptUpdateComponentType = InstanceType<typeof MeetingTranscriptUpdate>;

let route: Partial<RouteLocation>;
const routerGoMock = vitest.fn();

vitest.mock('vue-router', () => ({
  useRoute: () => route,
  useRouter: () => ({ go: routerGoMock }),
}));

const meetingTranscriptSample = { id: 123 };

describe('Component Tests', () => {
  let mountOptions: MountingOptions<MeetingTranscriptUpdateComponentType>['global'];
  let alertService: AlertService;

  describe('MeetingTranscript Management Update Component', () => {
    let comp: MeetingTranscriptUpdateComponentType;
    let meetingTranscriptServiceStub: SinonStubbedInstance<MeetingTranscriptService>;

    beforeEach(() => {
      route = {};
      meetingTranscriptServiceStub = sinon.createStubInstance<MeetingTranscriptService>(MeetingTranscriptService);
      meetingTranscriptServiceStub.retrieve.onFirstCall().resolves(Promise.resolve([]));

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
          meetingTranscriptService: () => meetingTranscriptServiceStub,

          userService: () =>
            sinon.createStubInstance<UserService>(UserService, {
              retrieve: sinon.stub().resolves({}),
            } as any),
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
        const wrapper = shallowMount(MeetingTranscriptUpdate, { global: mountOptions });
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
        const wrapper = shallowMount(MeetingTranscriptUpdate, { global: mountOptions });
        comp = wrapper.vm;
        comp.meetingTranscript = meetingTranscriptSample;
        meetingTranscriptServiceStub.update.resolves(meetingTranscriptSample);

        // WHEN
        comp.save();
        await comp.$nextTick();

        // THEN
        expect(meetingTranscriptServiceStub.update.calledWith(meetingTranscriptSample)).toBeTruthy();
        expect(comp.isSaving).toEqual(false);
      });

      it('Should call create service on save for new entity', async () => {
        // GIVEN
        const entity = {};
        meetingTranscriptServiceStub.create.resolves(entity);
        const wrapper = shallowMount(MeetingTranscriptUpdate, { global: mountOptions });
        comp = wrapper.vm;
        comp.meetingTranscript = entity;

        // WHEN
        comp.save();
        await comp.$nextTick();

        // THEN
        expect(meetingTranscriptServiceStub.create.calledWith(entity)).toBeTruthy();
        expect(comp.isSaving).toEqual(false);
      });
    });

    describe('Before route enter', () => {
      it('Should retrieve data', async () => {
        // GIVEN
        meetingTranscriptServiceStub.find.resolves(meetingTranscriptSample);
        meetingTranscriptServiceStub.retrieve.resolves([meetingTranscriptSample]);

        // WHEN
        route = {
          params: {
            meetingTranscriptId: `${meetingTranscriptSample.id}`,
          },
        };
        const wrapper = shallowMount(MeetingTranscriptUpdate, { global: mountOptions });
        comp = wrapper.vm;
        await comp.$nextTick();

        // THEN
        expect(comp.meetingTranscript).toMatchObject(meetingTranscriptSample);
      });
    });

    describe('Previous state', () => {
      it('Should go previous state', async () => {
        meetingTranscriptServiceStub.find.resolves(meetingTranscriptSample);
        const wrapper = shallowMount(MeetingTranscriptUpdate, { global: mountOptions });
        comp = wrapper.vm;
        await comp.$nextTick();

        comp.previousState();
        await comp.$nextTick();

        expect(routerGoMock).toHaveBeenCalledWith(-1);
      });
    });
  });
});
