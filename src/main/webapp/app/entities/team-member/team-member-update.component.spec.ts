import { beforeEach, describe, expect, it, vitest } from 'vitest';
import { type RouteLocation } from 'vue-router';

import { type MountingOptions, shallowMount } from '@vue/test-utils';
import dayjs from 'dayjs';
import sinon, { type SinonStubbedInstance } from 'sinon';

import TeamService from '@/entities/team/team.service';
import UserService from '@/entities/user/user.service';
import AlertService from '@/shared/alert/alert.service';
import { DATE_TIME_LONG_FORMAT } from '@/shared/composables/date-format';

import TeamMemberUpdate from './team-member-update.vue';
import TeamMemberService from './team-member.service';

type TeamMemberUpdateComponentType = InstanceType<typeof TeamMemberUpdate>;

let route: Partial<RouteLocation>;
const routerGoMock = vitest.fn();

vitest.mock('vue-router', () => ({
  useRoute: () => route,
  useRouter: () => ({ go: routerGoMock }),
}));

const teamMemberSample = { id: 123 };

describe('Component Tests', () => {
  let mountOptions: MountingOptions<TeamMemberUpdateComponentType>['global'];
  let alertService: AlertService;

  describe('TeamMember Management Update Component', () => {
    let comp: TeamMemberUpdateComponentType;
    let teamMemberServiceStub: SinonStubbedInstance<TeamMemberService>;

    beforeEach(() => {
      route = {};
      teamMemberServiceStub = sinon.createStubInstance<TeamMemberService>(TeamMemberService);
      teamMemberServiceStub.retrieve.onFirstCall().resolves(Promise.resolve([]));

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
          teamMemberService: () => teamMemberServiceStub,
          teamService: () =>
            sinon.createStubInstance<TeamService>(TeamService, {
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
        const wrapper = shallowMount(TeamMemberUpdate, { global: mountOptions });
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
        const wrapper = shallowMount(TeamMemberUpdate, { global: mountOptions });
        comp = wrapper.vm;
        comp.teamMember = teamMemberSample;
        teamMemberServiceStub.update.resolves(teamMemberSample);

        // WHEN
        comp.save();
        await comp.$nextTick();

        // THEN
        expect(teamMemberServiceStub.update.calledWith(teamMemberSample)).toBeTruthy();
        expect(comp.isSaving).toEqual(false);
      });

      it('Should call create service on save for new entity', async () => {
        // GIVEN
        const entity = {};
        teamMemberServiceStub.create.resolves(entity);
        const wrapper = shallowMount(TeamMemberUpdate, { global: mountOptions });
        comp = wrapper.vm;
        comp.teamMember = entity;

        // WHEN
        comp.save();
        await comp.$nextTick();

        // THEN
        expect(teamMemberServiceStub.create.calledWith(entity)).toBeTruthy();
        expect(comp.isSaving).toEqual(false);
      });
    });

    describe('Before route enter', () => {
      it('Should retrieve data', async () => {
        // GIVEN
        teamMemberServiceStub.find.resolves(teamMemberSample);
        teamMemberServiceStub.retrieve.resolves([teamMemberSample]);

        // WHEN
        route = {
          params: {
            teamMemberId: `${teamMemberSample.id}`,
          },
        };
        const wrapper = shallowMount(TeamMemberUpdate, { global: mountOptions });
        comp = wrapper.vm;
        await comp.$nextTick();

        // THEN
        expect(comp.teamMember).toMatchObject(teamMemberSample);
      });
    });

    describe('Previous state', () => {
      it('Should go previous state', async () => {
        teamMemberServiceStub.find.resolves(teamMemberSample);
        const wrapper = shallowMount(TeamMemberUpdate, { global: mountOptions });
        comp = wrapper.vm;
        await comp.$nextTick();

        comp.previousState();
        await comp.$nextTick();

        expect(routerGoMock).toHaveBeenCalledWith(-1);
      });
    });
  });
});
