import { type Ref, defineComponent, inject, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { useAlertService } from '@/shared/alert/alert.service';
import { useDateFormat } from '@/shared/composables';
import useDataUtils from '@/shared/data/data-utils.service';
import { type IInterview } from '@/shared/model/interview.model';

import InterviewService from './interview.service';

export default defineComponent({
  name: 'InterviewDetails',
  setup() {
    const dateFormat = useDateFormat();
    const interviewService = inject('interviewService', () => new InterviewService());
    const alertService = inject('alertService', () => useAlertService(), true);

    const dataUtils = useDataUtils();

    const route = useRoute();
    const router = useRouter();

    const previousState = () => router.go(-1);
    const interview: Ref<IInterview> = ref({});

    const retrieveInterview = async interviewId => {
      try {
        const res = await interviewService().find(interviewId);
        interview.value = res;
      } catch (error) {
        alertService.showHttpError(error.response);
      }
    };

    if (route.params?.interviewId) {
      retrieveInterview(route.params.interviewId);
    }

    return {
      ...dateFormat,
      alertService,
      interview,

      ...dataUtils,

      previousState,
    };
  },
});
