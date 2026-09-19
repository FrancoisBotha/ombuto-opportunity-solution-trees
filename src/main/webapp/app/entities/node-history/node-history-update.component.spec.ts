import { beforeEach, describe, expect, it, vitest } from 'vitest';
import { type RouteLocation } from 'vue-router';

import { type MountingOptions, shallowMount } from '@vue/test-utils';
import dayjs from 'dayjs';
import sinon, { type SinonStubbedInstance } from 'sinon';

import UserService from '@/entities/user/user.service';
import AlertService from '@/shared/alert/alert.service';
import { DATE_TIME_LONG_FORMAT } from '@/shared/composables/date-format';

import NodeHistoryUpdate from './node-history-update.vue';
import NodeHistoryService from './node-history.service';

type NodeHistoryUpdateComponentType = InstanceType<typeof NodeHistoryUpdate>;

let route: Partial<RouteLocation>;
const routerGoMock = vitest.fn();

vitest.mock('vue-router', () => ({
  useRoute: () => route,
  useRouter: () => ({ go: routerGoMock }),
}));

const nodeHistorySample = { id: 123 };

describe('Component Tests', () => {
  let mountOptions: MountingOptions<NodeHistoryUpdateComponentType>['global'];
  let alertService: AlertService;

  describe('NodeHistory Management Update Component', () => {
    let comp: NodeHistoryUpdateComponentType;
    let nodeHistoryServiceStub: SinonStubbedInstance<NodeHistoryService>;

    beforeEach(() => {
      route = {};
      nodeHistoryServiceStub = sinon.createStubInstance<NodeHistoryService>(NodeHistoryService);
      nodeHistoryServiceStub.retrieve.onFirstCall().resolves(Promise.resolve([]));

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
          nodeHistoryService: () => nodeHistoryServiceStub,

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
        const wrapper = shallowMount(NodeHistoryUpdate, { global: mountOptions });
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
        const wrapper = shallowMount(NodeHistoryUpdate, { global: mountOptions });
        comp = wrapper.vm;
        comp.nodeHistory = nodeHistorySample;
        nodeHistoryServiceStub.update.resolves(nodeHistorySample);

        // WHEN
        comp.save();
        await comp.$nextTick();

        // THEN
        expect(nodeHistoryServiceStub.update.calledWith(nodeHistorySample)).toBeTruthy();
        expect(comp.isSaving).toEqual(false);
      });

      it('Should call create service on save for new entity', async () => {
        // GIVEN
        const entity = {};
        nodeHistoryServiceStub.create.resolves(entity);
        const wrapper = shallowMount(NodeHistoryUpdate, { global: mountOptions });
        comp = wrapper.vm;
        comp.nodeHistory = entity;

        // WHEN
        comp.save();
        await comp.$nextTick();

        // THEN
        expect(nodeHistoryServiceStub.create.calledWith(entity)).toBeTruthy();
        expect(comp.isSaving).toEqual(false);
      });
    });

    describe('Before route enter', () => {
      it('Should retrieve data', async () => {
        // GIVEN
        nodeHistoryServiceStub.find.resolves(nodeHistorySample);
        nodeHistoryServiceStub.retrieve.resolves([nodeHistorySample]);

        // WHEN
        route = {
          params: {
            nodeHistoryId: `${nodeHistorySample.id}`,
          },
        };
        const wrapper = shallowMount(NodeHistoryUpdate, { global: mountOptions });
        comp = wrapper.vm;
        await comp.$nextTick();

        // THEN
        expect(comp.nodeHistory).toMatchObject(nodeHistorySample);
      });
    });

    describe('Previous state', () => {
      it('Should go previous state', async () => {
        nodeHistoryServiceStub.find.resolves(nodeHistorySample);
        const wrapper = shallowMount(NodeHistoryUpdate, { global: mountOptions });
        comp = wrapper.vm;
        await comp.$nextTick();

        comp.previousState();
        await comp.$nextTick();

        expect(routerGoMock).toHaveBeenCalledWith(-1);
      });
    });
  });
});
