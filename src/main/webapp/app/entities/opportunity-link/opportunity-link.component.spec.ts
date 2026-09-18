import { beforeEach, describe, expect, it, vitest } from 'vitest';

import { type MountingOptions, shallowMount } from '@vue/test-utils';
import sinon, { type SinonStubbedInstance } from 'sinon';

import AlertService from '@/shared/alert/alert.service';

import OpportunityLinkService from './opportunity-link.service';
import OpportunityLink from './opportunity-link.vue';

type OpportunityLinkComponentType = InstanceType<typeof OpportunityLink>;

const bModalStub = {
  render: () => {},
  methods: {
    hide: () => {},
    show: () => {},
  },
};

describe('Component Tests', () => {
  let alertService: AlertService;

  describe('OpportunityLink Management Component', () => {
    let opportunityLinkServiceStub: SinonStubbedInstance<OpportunityLinkService>;
    let mountOptions: MountingOptions<OpportunityLinkComponentType>['global'];

    beforeEach(() => {
      opportunityLinkServiceStub = sinon.createStubInstance<OpportunityLinkService>(OpportunityLinkService);
      opportunityLinkServiceStub.retrieve.resolves({ headers: {} });

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
          opportunityLinkService: () => opportunityLinkServiceStub,
        },
      };
    });

    describe('Mount', () => {
      it('Should call load all on init', async () => {
        // GIVEN
        opportunityLinkServiceStub.retrieve.resolves({ headers: {}, data: [{ id: 123 }] });

        // WHEN
        const wrapper = shallowMount(OpportunityLink, { global: mountOptions });
        const comp = wrapper.vm;
        await comp.$nextTick();

        // THEN
        expect(opportunityLinkServiceStub.retrieve.calledOnce).toBeTruthy();
        expect(comp.opportunityLinks[0]).toEqual(expect.objectContaining({ id: 123 }));
      });
    });
    describe('Handles', () => {
      let comp: OpportunityLinkComponentType;

      beforeEach(async () => {
        const wrapper = shallowMount(OpportunityLink, { global: mountOptions });
        comp = wrapper.vm;
        await comp.$nextTick();
        opportunityLinkServiceStub.retrieve.reset();
        opportunityLinkServiceStub.retrieve.resolves({ headers: {}, data: [] });
      });

      it('Should call delete service on confirmDelete', async () => {
        // GIVEN
        opportunityLinkServiceStub.delete.resolves({});

        // WHEN
        comp.prepareRemove({ id: 123 });

        comp.removeOpportunityLink();
        await comp.$nextTick(); // clear components

        // THEN
        expect(opportunityLinkServiceStub.delete.called).toBeTruthy();

        // THEN
        await comp.$nextTick(); // handle component clear watch
        expect(opportunityLinkServiceStub.retrieve.callCount).toEqual(1);
      });
    });
  });
});
