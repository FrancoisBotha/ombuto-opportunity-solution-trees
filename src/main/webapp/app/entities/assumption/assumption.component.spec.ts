import { beforeEach, describe, expect, it, vitest } from 'vitest';

import { type MountingOptions, shallowMount } from '@vue/test-utils';
import sinon, { type SinonStubbedInstance } from 'sinon';

import AlertService from '@/shared/alert/alert.service';

import AssumptionService from './assumption.service';
import Assumption from './assumption.vue';

type AssumptionComponentType = InstanceType<typeof Assumption>;

const bModalStub = {
  render: () => {},
  methods: {
    hide: () => {},
    show: () => {},
  },
};

describe('Component Tests', () => {
  let alertService: AlertService;

  describe('Assumption Management Component', () => {
    let assumptionServiceStub: SinonStubbedInstance<AssumptionService>;
    let mountOptions: MountingOptions<AssumptionComponentType>['global'];

    beforeEach(() => {
      assumptionServiceStub = sinon.createStubInstance<AssumptionService>(AssumptionService);
      assumptionServiceStub.retrieve.resolves({ headers: {} });

      alertService = new AlertService({
        toast: {
          show: vitest.fn(),
        } as any,
      });

      mountOptions = {
        stubs: {
          bModal: bModalStub as any,
          'font-awesome-icon': true,
          'b-badge': true,
          'b-button': true,
          'router-link': true,
        },
        directives: {
          'b-modal': {},
        },
        provide: {
          alertService,
          assumptionService: () => assumptionServiceStub,
        },
      };
    });

    describe('Mount', () => {
      it('Should call load all on init', async () => {
        // GIVEN
        assumptionServiceStub.retrieve.resolves({ headers: {}, data: [{ id: 123 }] });

        // WHEN
        const wrapper = shallowMount(Assumption, { global: mountOptions });
        const comp = wrapper.vm;
        await comp.$nextTick();

        // THEN
        expect(assumptionServiceStub.retrieve.calledOnce).toBeTruthy();
        expect(comp.assumptions[0]).toEqual(expect.objectContaining({ id: 123 }));
      });
    });
    describe('Handles', () => {
      let comp: AssumptionComponentType;

      beforeEach(async () => {
        const wrapper = shallowMount(Assumption, { global: mountOptions });
        comp = wrapper.vm;
        await comp.$nextTick();
        assumptionServiceStub.retrieve.reset();
        assumptionServiceStub.retrieve.resolves({ headers: {}, data: [] });
      });

      it('Should call delete service on confirmDelete', async () => {
        // GIVEN
        assumptionServiceStub.delete.resolves({});

        // WHEN
        comp.prepareRemove({ id: 123 });

        comp.removeAssumption();
        await comp.$nextTick(); // clear components

        // THEN
        expect(assumptionServiceStub.delete.called).toBeTruthy();

        // THEN
        await comp.$nextTick(); // handle component clear watch
        expect(assumptionServiceStub.retrieve.callCount).toEqual(1);
      });
    });
  });
});
