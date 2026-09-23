import { type Ref, defineComponent, inject, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { useAlertService } from '@/shared/alert/alert.service';
import { useDateFormat } from '@/shared/composables';
import useDataUtils from '@/shared/data/data-utils.service';
import { type IMeetingTranscript } from '@/shared/model/meeting-transcript.model';

import MeetingTranscriptService from './meeting-transcript.service';

export default defineComponent({
  name: 'MeetingTranscriptDetails',
  setup() {
    const dateFormat = useDateFormat();
    const meetingTranscriptService = inject('meetingTranscriptService', () => new MeetingTranscriptService());
    const alertService = inject('alertService', () => useAlertService(), true);

    const dataUtils = useDataUtils();

    const route = useRoute();
    const router = useRouter();

    const previousState = () => router.go(-1);
    const meetingTranscript: Ref<IMeetingTranscript> = ref({});

    const retrieveMeetingTranscript = async meetingTranscriptId => {
      try {
        const res = await meetingTranscriptService().find(meetingTranscriptId);
        meetingTranscript.value = res;
      } catch (error) {
        alertService.showHttpError(error.response);
      }
    };

    if (route.params?.meetingTranscriptId) {
      retrieveMeetingTranscript(route.params.meetingTranscriptId);
    }

    return {
      ...dateFormat,
      alertService,
      meetingTranscript,

      ...dataUtils,

      previousState,
    };
  },
});
