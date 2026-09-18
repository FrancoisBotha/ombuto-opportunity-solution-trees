import { beforeEach, describe, expect, it, vitest } from 'vitest';

import { type MountingOptions, shallowMount } from '@vue/test-utils';
import sinon, { type SinonStubbedInstance } from 'sinon';

import AlertService from '@/shared/alert/alert.service';

import SolutionLinkService from './solution-link.service';
import SolutionLink from './solution-link.vue';

type SolutionLinkComponentType = InstanceType<typeof SolutionLink>;

const bModalStub = {
  render: () => {},
  methods: {
    hide: () => {},
    show: () => {},
  },
};

describe('Component Tests', () => {
  let alertService: AlertService;

  describe('SolutionLink Management Component', () => {
    let solutionLinkServiceStub: SinonStubbedInstance<SolutionLinkService>;
    let mountOptions: MountingOptions<SolutionLinkComponentType>['global'];

    beforeEach(() => {
      solutionLinkServiceStub = sinon.createStubInstance<SolutionLinkService>(SolutionLinkService);
      solutionLinkServiceStub.retrieve.resolves({ headers: {} });

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
          solutionLinkService: () => solutionLinkServiceStub,
        },
      };
    });

    describe('Mount', () => {
      it('Should call load all on init', async () => {
        // GIVEN
        solutionLinkServiceStub.retrieve.resolves({ headers: {}, data: [{ id: 123 }] });

        // WHEN
        const wrapper = shallowMount(SolutionLink, { global: mountOptions });
        const comp = wrapper.vm;
        await comp.$nextTick();

        // THEN
        expect(solutionLinkServiceStub.retrieve.calledOnce).toBeTruthy();
        expect(comp.solutionLinks[0]).toEqual(expect.objectContaining({ id: 123 }));
      });
    });
    describe('Handles', () => {
      let comp: SolutionLinkComponentType;

      beforeEach(async () => {
        const wrapper = shallowMount(SolutionLink, { global: mountOptions });
        comp = wrapper.vm;
        await comp.$nextTick();
        solutionLinkServiceStub.retrieve.reset();
        solutionLinkServiceStub.retrieve.resolves({ headers: {}, data: [] });
      });

      it('Should call delete service on confirmDelete', async () => {
        // GIVEN
        solutionLinkServiceStub.delete.resolves({});

        // WHEN
        comp.prepareRemove({ id: 123 });

        comp.removeSolutionLink();
        await comp.$nextTick(); // clear components

        // THEN
        expect(solutionLinkServiceStub.delete.called).toBeTruthy();

        // THEN
        await comp.$nextTick(); // handle component clear watch
        expect(solutionLinkServiceStub.retrieve.callCount).toEqual(1);
      });
    });
  });
});
