import { beforeEach, describe, expect, it, vitest } from 'vitest';

import { type MountingOptions, shallowMount } from '@vue/test-utils';
import sinon, { type SinonStubbedInstance } from 'sinon';

import AlertService from '@/shared/alert/alert.service';

import OutcomeService from './outcome.service';
import Outcome from './outcome.vue';

type OutcomeComponentType = InstanceType<typeof Outcome>;

const bModalStub = {
  render: () => {},
  methods: {
    hide: () => {},
    show: () => {},
  },
};

describe('Component Tests', () => {
  let alertService: AlertService;

  describe('Outcome Management Component', () => {
    let outcomeServiceStub: SinonStubbedInstance<OutcomeService>;
    let mountOptions: MountingOptions<OutcomeComponentType>['global'];

    beforeEach(() => {
      outcomeServiceStub = sinon.createStubInstance<OutcomeService>(OutcomeService);
      outcomeServiceStub.retrieve.resolves({ headers: {} });

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
          outcomeService: () => outcomeServiceStub,
        },
      };
    });

    describe('Mount', () => {
      it('Should call load all on init', async () => {
        // GIVEN
        outcomeServiceStub.retrieve.resolves({ headers: {}, data: [{ id: 123 }] });

        // WHEN
        const wrapper = shallowMount(Outcome, { global: mountOptions });
        const comp = wrapper.vm;
        await comp.$nextTick();

        // THEN
        expect(outcomeServiceStub.retrieve.calledOnce).toBeTruthy();
        expect(comp.outcomes[0]).toEqual(expect.objectContaining({ id: 123 }));
      });
    });
    describe('Handles', () => {
      let comp: OutcomeComponentType;

      beforeEach(async () => {
        const wrapper = shallowMount(Outcome, { global: mountOptions });
        comp = wrapper.vm;
        await comp.$nextTick();
        outcomeServiceStub.retrieve.reset();
        outcomeServiceStub.retrieve.resolves({ headers: {}, data: [] });
      });

      it('Should call delete service on confirmDelete', async () => {
        // GIVEN
        outcomeServiceStub.delete.resolves({});

        // WHEN
        comp.prepareRemove({ id: 123 });

        comp.removeOutcome();
        await comp.$nextTick(); // clear components

        // THEN
        expect(outcomeServiceStub.delete.called).toBeTruthy();

        // THEN
        await comp.$nextTick(); // handle component clear watch
        expect(outcomeServiceStub.retrieve.callCount).toEqual(1);
      });
    });
  });
});
