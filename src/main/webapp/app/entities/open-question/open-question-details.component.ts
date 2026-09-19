import { type Ref, defineComponent, inject, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { useAlertService } from '@/shared/alert/alert.service';
import { useDateFormat } from '@/shared/composables';
import { type IOpenQuestion } from '@/shared/model/open-question.model';

import OpenQuestionService from './open-question.service';

export default defineComponent({
  name: 'OpenQuestionDetails',
  setup() {
    const dateFormat = useDateFormat();
    const openQuestionService = inject('openQuestionService', () => new OpenQuestionService());
    const alertService = inject('alertService', () => useAlertService(), true);

    const route = useRoute();
    const router = useRouter();

    const previousState = () => router.go(-1);
    const openQuestion: Ref<IOpenQuestion> = ref({});

    const retrieveOpenQuestion = async openQuestionId => {
      try {
        const res = await openQuestionService().find(openQuestionId);
        openQuestion.value = res;
      } catch (error) {
        alertService.showHttpError(error.response);
      }
    };

    if (route.params?.openQuestionId) {
      retrieveOpenQuestion(route.params.openQuestionId);
    }

    return {
      ...dateFormat,
      alertService,
      openQuestion,

      previousState,
    };
  },
});
