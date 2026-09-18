import { type Ref, defineComponent, inject, onMounted, ref } from 'vue';

import { useAlertService } from '@/shared/alert/alert.service';
import { useDateFormat } from '@/shared/composables';
import useDataUtils from '@/shared/data/data-utils.service';
import { type IOutcome } from '@/shared/model/outcome.model';

import OutcomeService from './outcome.service';

export default defineComponent({
  name: 'Outcome',
  setup() {
    const dateFormat = useDateFormat();
    const dataUtils = useDataUtils();
    const outcomeService = inject('outcomeService', () => new OutcomeService());
    const alertService = inject('alertService', () => useAlertService(), true);

    const outcomes: Ref<IOutcome[]> = ref([]);

    const isFetching = ref(false);

    const clear = () => {};

    const retrieveOutcomes = async () => {
      isFetching.value = true;
      try {
        const res = await outcomeService().retrieve();
        outcomes.value = res.data;
      } catch (err) {
        alertService.showHttpError(err.response);
      } finally {
        isFetching.value = false;
      }
    };

    const handleSyncList = () => {
      retrieveOutcomes();
    };

    onMounted(async () => {
      await retrieveOutcomes();
    });

    const removeId: Ref<number> = ref(null);
    const removeEntity = ref<any>(null);
    const prepareRemove = (instance: IOutcome) => {
      removeId.value = instance.id;
      removeEntity.value.show();
    };
    const closeDialog = () => {
      removeEntity.value.hide();
    };
    const removeOutcome = async () => {
      try {
        await outcomeService().delete(removeId.value);
        const message = `A Outcome is deleted with identifier ${removeId.value}`;
        alertService.showInfo(message, { variant: 'danger' });
        removeId.value = null;
        retrieveOutcomes();
        closeDialog();
      } catch (error) {
        alertService.showHttpError(error.response);
      }
    };

    return {
      outcomes,
      handleSyncList,
      isFetching,
      retrieveOutcomes,
      clear,
      ...dateFormat,
      removeId,
      removeEntity,
      prepareRemove,
      closeDialog,
      removeOutcome,
      ...dataUtils,
    };
  },
});
