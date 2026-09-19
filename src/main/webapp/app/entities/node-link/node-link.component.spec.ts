import { beforeEach, describe, expect, it, vitest } from 'vitest';

import { type MountingOptions, shallowMount } from '@vue/test-utils';
import sinon, { type SinonStubbedInstance } from 'sinon';

import AlertService from '@/shared/alert/alert.service';

import NodeLinkService from './node-link.service';
import NodeLink from './node-link.vue';

type NodeLinkComponentType = InstanceType<typeof NodeLink>;

const bModalStub = {
  render: () => {},
  methods: {
    hide: () => {},
    show: () => {},
  },
};

describe('Component Tests', () => {
  let alertService: AlertService;

  describe('NodeLink Management Component', () => {
    let nodeLinkServiceStub: SinonStubbedInstance<NodeLinkService>;
    let mountOptions: MountingOptions<NodeLinkComponentType>['global'];

    beforeEach(() => {
      nodeLinkServiceStub = sinon.createStubInstance<NodeLinkService>(NodeLinkService);
      nodeLinkServiceStub.retrieve.resolves({ headers: {} });

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
          nodeLinkService: () => nodeLinkServiceStub,
        },
      };
    });

    describe('Mount', () => {
      it('Should call load all on init', async () => {
        // GIVEN
        nodeLinkServiceStub.retrieve.resolves({ headers: {}, data: [{ id: 123 }] });

        // WHEN
        const wrapper = shallowMount(NodeLink, { global: mountOptions });
        const comp = wrapper.vm;
        await comp.$nextTick();

        // THEN
        expect(nodeLinkServiceStub.retrieve.calledOnce).toBeTruthy();
        expect(comp.nodeLinks[0]).toEqual(expect.objectContaining({ id: 123 }));
      });
    });
    describe('Handles', () => {
      let comp: NodeLinkComponentType;

      beforeEach(async () => {
        const wrapper = shallowMount(NodeLink, { global: mountOptions });
        comp = wrapper.vm;
        await comp.$nextTick();
        nodeLinkServiceStub.retrieve.reset();
        nodeLinkServiceStub.retrieve.resolves({ headers: {}, data: [] });
      });

      it('Should call delete service on confirmDelete', async () => {
        // GIVEN
        nodeLinkServiceStub.delete.resolves({});

        // WHEN
        comp.prepareRemove({ id: 123 });

        comp.removeNodeLink();
        await comp.$nextTick(); // clear components

        // THEN
        expect(nodeLinkServiceStub.delete.called).toBeTruthy();

        // THEN
        await comp.$nextTick(); // handle component clear watch
        expect(nodeLinkServiceStub.retrieve.callCount).toEqual(1);
      });
    });
  });
});
