import { beforeEach, describe, expect, it, vitest } from 'vitest';

import { type MountingOptions, shallowMount } from '@vue/test-utils';
import sinon, { type SinonStubbedInstance } from 'sinon';

import AlertService from '@/shared/alert/alert.service';

import EvidenceService from './evidence.service';
import Evidence from './evidence.vue';

type EvidenceComponentType = InstanceType<typeof Evidence>;

const bModalStub = {
  render: () => {},
  methods: {
    hide: () => {},
    show: () => {},
  },
};

describe('Component Tests', () => {
  let alertService: AlertService;

  describe('Evidence Management Component', () => {
    let evidenceServiceStub: SinonStubbedInstance<EvidenceService>;
    let mountOptions: MountingOptions<EvidenceComponentType>['global'];

    beforeEach(() => {
      evidenceServiceStub = sinon.createStubInstance<EvidenceService>(EvidenceService);
      evidenceServiceStub.retrieve.resolves({ headers: {} });

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
          evidenceService: () => evidenceServiceStub,
        },
      };
    });

    describe('Mount', () => {
      it('Should call load all on init', async () => {
        // GIVEN
        evidenceServiceStub.retrieve.resolves({ headers: {}, data: [{ id: 123 }] });

        // WHEN
        const wrapper = shallowMount(Evidence, { global: mountOptions });
        const comp = wrapper.vm;
        await comp.$nextTick();

        // THEN
        expect(evidenceServiceStub.retrieve.calledOnce).toBeTruthy();
        expect(comp.evidences[0]).toEqual(expect.objectContaining({ id: 123 }));
      });
    });
    describe('Handles', () => {
      let comp: EvidenceComponentType;

      beforeEach(async () => {
        const wrapper = shallowMount(Evidence, { global: mountOptions });
        comp = wrapper.vm;
        await comp.$nextTick();
        evidenceServiceStub.retrieve.reset();
        evidenceServiceStub.retrieve.resolves({ headers: {}, data: [] });
      });

      it('Should call delete service on confirmDelete', async () => {
        // GIVEN
        evidenceServiceStub.delete.resolves({});

        // WHEN
        comp.prepareRemove({ id: 123 });

        comp.removeEvidence();
        await comp.$nextTick(); // clear components

        // THEN
        expect(evidenceServiceStub.delete.called).toBeTruthy();

        // THEN
        await comp.$nextTick(); // handle component clear watch
        expect(evidenceServiceStub.retrieve.callCount).toEqual(1);
      });
    });
  });
});
