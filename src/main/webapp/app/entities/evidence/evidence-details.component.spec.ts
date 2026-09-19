import { beforeEach, describe, expect, it, vitest } from 'vitest';
import { type RouteLocation } from 'vue-router';

import { type MountingOptions, shallowMount } from '@vue/test-utils';
import sinon, { type SinonStubbedInstance } from 'sinon';

import AlertService from '@/shared/alert/alert.service';

import EvidenceDetails from './evidence-details.vue';
import EvidenceService from './evidence.service';

type EvidenceDetailsComponentType = InstanceType<typeof EvidenceDetails>;

let route: Partial<RouteLocation>;
const routerGoMock = vitest.fn();

vitest.mock('vue-router', () => ({
  useRoute: () => route,
  useRouter: () => ({ go: routerGoMock }),
}));

const evidenceSample = { id: 123 };

describe('Component Tests', () => {
  let alertService: AlertService;

  afterEach(() => {
    vitest.resetAllMocks();
  });

  describe('Evidence Management Detail Component', () => {
    let evidenceServiceStub: SinonStubbedInstance<EvidenceService>;
    let mountOptions: MountingOptions<EvidenceDetailsComponentType>['global'];

    beforeEach(() => {
      route = {};
      evidenceServiceStub = sinon.createStubInstance<EvidenceService>(EvidenceService);

      alertService = new AlertService({
        toast: {
          show: vitest.fn(),
        } as any,
      });

      mountOptions = {
        stubs: {
          'font-awesome-icon': true,
          'router-link': true,
        },
        provide: {
          alertService,
          evidenceService: () => evidenceServiceStub,
        },
      };
    });

    describe('Navigate to details', () => {
      it('Should call load all on init', async () => {
        // GIVEN
        evidenceServiceStub.find.resolves(evidenceSample);
        route = {
          params: {
            evidenceId: `${123}`,
          },
        };
        const wrapper = shallowMount(EvidenceDetails, { global: mountOptions });
        const comp = wrapper.vm;
        // WHEN
        await comp.$nextTick();

        // THEN
        expect(comp.evidence).toMatchObject(evidenceSample);
      });
    });

    describe('Previous state', () => {
      it('Should go previous state', async () => {
        evidenceServiceStub.find.resolves(evidenceSample);
        const wrapper = shallowMount(EvidenceDetails, { global: mountOptions });
        const comp = wrapper.vm;
        await comp.$nextTick();

        comp.previousState();
        await comp.$nextTick();

        expect(routerGoMock).toHaveBeenCalledWith(-1);
      });
    });
  });
});
