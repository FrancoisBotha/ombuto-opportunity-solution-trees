import { beforeEach, describe, expect, it, vitest } from 'vitest';
import { type RouteLocation } from 'vue-router';

import { type MountingOptions, shallowMount } from '@vue/test-utils';
import sinon, { type SinonStubbedInstance } from 'sinon';

import AlertService from '@/shared/alert/alert.service';

import MeetingTranscriptDetails from './meeting-transcript-details.vue';
import MeetingTranscriptService from './meeting-transcript.service';

type MeetingTranscriptDetailsComponentType = InstanceType<typeof MeetingTranscriptDetails>;

let route: Partial<RouteLocation>;
const routerGoMock = vitest.fn();

vitest.mock('vue-router', () => ({
  useRoute: () => route,
  useRouter: () => ({ go: routerGoMock }),
}));

const meetingTranscriptSample = { id: 123 };

describe('Component Tests', () => {
  let alertService: AlertService;

  afterEach(() => {
    vitest.resetAllMocks();
  });

  describe('MeetingTranscript Management Detail Component', () => {
    let meetingTranscriptServiceStub: SinonStubbedInstance<MeetingTranscriptService>;
    let mountOptions: MountingOptions<MeetingTranscriptDetailsComponentType>['global'];

    beforeEach(() => {
      route = {};
      meetingTranscriptServiceStub = sinon.createStubInstance<MeetingTranscriptService>(MeetingTranscriptService);

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
          meetingTranscriptService: () => meetingTranscriptServiceStub,
        },
      };
    });

    describe('Navigate to details', () => {
      it('Should call load all on init', async () => {
        // GIVEN
        meetingTranscriptServiceStub.find.resolves(meetingTranscriptSample);
        route = {
          params: {
            meetingTranscriptId: `${123}`,
          },
        };
        const wrapper = shallowMount(MeetingTranscriptDetails, { global: mountOptions });
        const comp = wrapper.vm;
        // WHEN
        await comp.$nextTick();

        // THEN
        expect(comp.meetingTranscript).toMatchObject(meetingTranscriptSample);
      });
    });

    describe('Previous state', () => {
      it('Should go previous state', async () => {
        meetingTranscriptServiceStub.find.resolves(meetingTranscriptSample);
        const wrapper = shallowMount(MeetingTranscriptDetails, { global: mountOptions });
        const comp = wrapper.vm;
        await comp.$nextTick();

        comp.previousState();
        await comp.$nextTick();

        expect(routerGoMock).toHaveBeenCalledWith(-1);
      });
    });
  });
});
