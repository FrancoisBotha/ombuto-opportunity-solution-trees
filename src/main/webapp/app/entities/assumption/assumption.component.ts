import { type Ref, defineComponent, inject, onMounted, ref } from 'vue';

import { useAlertService } from '@/shared/alert/alert.service';
import { useDateFormat } from '@/shared/composables';
import { type IAssumption } from '@/shared/model/assumption.model';

import AssumptionService from './assumption.service';

export default defineComponent({
  name: 'Assumption',
  setup() {
    const dateFormat = useDateFormat();
    const assumptionService = inject('assumptionService', () => new AssumptionService());
    const alertService = inject('alertService', () => useAlertService(), true);

    const assumptions: Ref<IAssumption[]> = ref([]);

    const isFetching = ref(false);

    const clear = () => {};

    const retrieveAssumptions = async () => {
      isFetching.value = true;
      try {
        const res = await assumptionService().retrieve();
        assumptions.value = res.data;
      } catch (err) {
        alertService.showHttpError(err.response);
      } finally {
        isFetching.value = false;
      }
    };

    const handleSyncList = () => {
      retrieveAssumptions();
    };

    onMounted(async () => {
      await retrieveAssumptions();
    });

    const removeId: Ref<number> = ref(null);
    const removeEntity = ref<any>(null);
    const prepareRemove = (instance: IAssumption) => {
      removeId.value = instance.id;
      removeEntity.value.show();
    };
    const closeDialog = () => {
      removeEntity.value.hide();
    };
    const removeAssumption = async () => {
      try {
        await assumptionService().delete(removeId.value);
        const message = `A Assumption is deleted with identifier ${removeId.value}`;
        alertService.showInfo(message, { variant: 'danger' });
        removeId.value = null;
        retrieveAssumptions();
        closeDialog();
      } catch (error) {
        alertService.showHttpError(error.response);
      }
    };

    return {
      assumptions,
      handleSyncList,
      isFetching,
      retrieveAssumptions,
      clear,
      ...dateFormat,
      removeId,
      removeEntity,
      prepareRemove,
      closeDialog,
      removeAssumption,
    };
  },
});
