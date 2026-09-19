import { type Ref, defineComponent, inject, onMounted, ref } from 'vue';

import { useAlertService } from '@/shared/alert/alert.service';
import { useDateFormat } from '@/shared/composables';
import useDataUtils from '@/shared/data/data-utils.service';
import { type IEvidence } from '@/shared/model/evidence.model';

import EvidenceService from './evidence.service';

export default defineComponent({
  name: 'Evidence',
  setup() {
    const dateFormat = useDateFormat();
    const dataUtils = useDataUtils();
    const evidenceService = inject('evidenceService', () => new EvidenceService());
    const alertService = inject('alertService', () => useAlertService(), true);

    const evidences: Ref<IEvidence[]> = ref([]);

    const isFetching = ref(false);

    const clear = () => {};

    const retrieveEvidences = async () => {
      isFetching.value = true;
      try {
        const res = await evidenceService().retrieve();
        evidences.value = res.data;
      } catch (err) {
        alertService.showHttpError(err.response);
      } finally {
        isFetching.value = false;
      }
    };

    const handleSyncList = () => {
      retrieveEvidences();
    };

    onMounted(async () => {
      await retrieveEvidences();
    });

    const removeId: Ref<number> = ref(null);
    const removeEntity = ref<any>(null);
    const prepareRemove = (instance: IEvidence) => {
      removeId.value = instance.id;
      removeEntity.value.show();
    };
    const closeDialog = () => {
      removeEntity.value.hide();
    };
    const removeEvidence = async () => {
      try {
        await evidenceService().delete(removeId.value);
        const message = `A Evidence is deleted with identifier ${removeId.value}`;
        alertService.showInfo(message, { variant: 'danger' });
        removeId.value = null;
        retrieveEvidences();
        closeDialog();
      } catch (error) {
        alertService.showHttpError(error.response);
      }
    };

    return {
      evidences,
      handleSyncList,
      isFetching,
      retrieveEvidences,
      clear,
      ...dateFormat,
      removeId,
      removeEntity,
      prepareRemove,
      closeDialog,
      removeEvidence,
      ...dataUtils,
    };
  },
});
