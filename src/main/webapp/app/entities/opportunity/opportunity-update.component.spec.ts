import { beforeEach, describe, expect, it, vitest } from 'vitest';
import { type RouteLocation } from 'vue-router';

import { type MountingOptions, shallowMount } from '@vue/test-utils';
import dayjs from 'dayjs';
import sinon, { type SinonStubbedInstance } from 'sinon';

import InterviewService from '@/entities/interview/interview.service';
import OutcomeService from '@/entities/outcome/outcome.service';
import TagService from '@/entities/tag/tag.service';
import UserService from '@/entities/user/user.service';
import AlertService from '@/shared/alert/alert.service';
import { DATE_TIME_LONG_FORMAT } from '@/shared/composables/date-format';

import OpportunityUpdate from './opportunity-update.vue';
import OpportunityService from './opportunity.service';

type OpportunityUpdateComponentType = InstanceType<typeof OpportunityUpdate>;

let route: Partial<RouteLocation>;
const routerGoMock = vitest.fn();

vitest.mock('vue-router', () => ({
  useRoute: () => route,
  useRouter: () => ({ go: routerGoMock }),
}));

const opportunitySample = { id: 123 };

describe('Component Tests', () => {
  let mountOptions: MountingOptions<OpportunityUpdateComponentType>['global'];
  let alertService: AlertService;

  describe('Opportunity Management Update Component', () => {
    let comp: OpportunityUpdateComponentType;
    let opportunityServiceStub: SinonStubbedInstance<OpportunityService>;

    beforeEach(() => {
      route = {};
      opportunityServiceStub = sinon.createStubInstance<OpportunityService>(OpportunityService);
      opportunityServiceStub.retrieve.onFirstCall().resolves(Promise.resolve([]));

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
          opportunityService: () => opportunityServiceStub,
          outcomeService: () =>
            sinon.createStubInstance<OutcomeService>(OutcomeService, {
              retrieve: sinon.stub().resolves({}),
            } as any),

          userService: () =>
            sinon.createStubInstance<UserService>(UserService, {
              retrieve: sinon.stub().resolves({}),
            } as any),
          interviewService: () =>
            sinon.createStubInstance<InterviewService>(InterviewService, {
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
        const wrapper = shallowMount(OpportunityUpdate, { global: mountOptions });
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
        const wrapper = shallowMount(OpportunityUpdate, { global: mountOptions });
        comp = wrapper.vm;
        comp.opportunity = opportunitySample;
        opportunityServiceStub.update.resolves(opportunitySample);

        // WHEN
        comp.save();
        await comp.$nextTick();

        // THEN
        expect(opportunityServiceStub.update.calledWith(opportunitySample)).toBeTruthy();
        expect(comp.isSaving).toEqual(false);
      });

      it('Should call create service on save for new entity', async () => {
        // GIVEN
        const entity = {};
        opportunityServiceStub.create.resolves(entity);
        const wrapper = shallowMount(OpportunityUpdate, { global: mountOptions });
        comp = wrapper.vm;
        comp.opportunity = entity;

        // WHEN
        comp.save();
        await comp.$nextTick();

        // THEN
        expect(opportunityServiceStub.create.calledWith(entity)).toBeTruthy();
        expect(comp.isSaving).toEqual(false);
      });
    });

    describe('Before route enter', () => {
      it('Should retrieve data', async () => {
        // GIVEN
        opportunityServiceStub.find.resolves(opportunitySample);
        opportunityServiceStub.retrieve.resolves([opportunitySample]);

        // WHEN
        route = {
          params: {
            opportunityId: `${opportunitySample.id}`,
          },
        };
        const wrapper = shallowMount(OpportunityUpdate, { global: mountOptions });
        comp = wrapper.vm;
        await comp.$nextTick();

        // THEN
        expect(comp.opportunity).toMatchObject(opportunitySample);
      });
    });

    describe('Previous state', () => {
      it('Should go previous state', async () => {
        opportunityServiceStub.find.resolves(opportunitySample);
        const wrapper = shallowMount(OpportunityUpdate, { global: mountOptions });
        comp = wrapper.vm;
        await comp.$nextTick();

        comp.previousState();
        await comp.$nextTick();

        expect(routerGoMock).toHaveBeenCalledWith(-1);
      });
    });
  });
});
