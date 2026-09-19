import { type Ref, defineComponent, inject, onMounted, ref } from 'vue';

import { useAlertService } from '@/shared/alert/alert.service';
import { useDateFormat } from '@/shared/composables';
import { type IOpenQuestion } from '@/shared/model/open-question.model';

import OpenQuestionService from './open-question.service';

export default defineComponent({
  name: 'OpenQuestion',
  setup() {
    const dateFormat = useDateFormat();
    const openQuestionService = inject('openQuestionService', () => new OpenQuestionService());
    const alertService = inject('alertService', () => useAlertService(), true);

    const openQuestions: Ref<IOpenQuestion[]> = ref([]);

    const isFetching = ref(false);

    const clear = () => {};

    const retrieveOpenQuestions = async () => {
      isFetching.value = true;
      try {
        const res = await openQuestionService().retrieve();
        openQuestions.value = res.data;
      } catch (err) {
        alertService.showHttpError(err.response);
      } finally {
        isFetching.value = false;
      }
    };

    const handleSyncList = () => {
      retrieveOpenQuestions();
    };

    onMounted(async () => {
      await retrieveOpenQuestions();
    });

    const removeId: Ref<number> = ref(null);
    const removeEntity = ref<any>(null);
    const prepareRemove = (instance: IOpenQuestion) => {
      removeId.value = instance.id;
      removeEntity.value.show();
    };
    const closeDialog = () => {
      removeEntity.value.hide();
    };
    const removeOpenQuestion = async () => {
      try {
        await openQuestionService().delete(removeId.value);
        const message = `A OpenQuestion is deleted with identifier ${removeId.value}`;
        alertService.showInfo(message, { variant: 'danger' });
        removeId.value = null;
        retrieveOpenQuestions();
        closeDialog();
      } catch (error) {
        alertService.showHttpError(error.response);
      }
    };

    return {
      openQuestions,
      handleSyncList,
      isFetching,
      retrieveOpenQuestions,
      clear,
      ...dateFormat,
      removeId,
      removeEntity,
      prepareRemove,
      closeDialog,
      removeOpenQuestion,
    };
  },
});
