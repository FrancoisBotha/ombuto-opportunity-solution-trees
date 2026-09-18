import { type Ref, defineComponent, inject, onMounted, ref } from 'vue';

import { useAlertService } from '@/shared/alert/alert.service';
import { type ISolutionLink } from '@/shared/model/solution-link.model';

import SolutionLinkService from './solution-link.service';

export default defineComponent({
  name: 'SolutionLink',
  setup() {
    const solutionLinkService = inject('solutionLinkService', () => new SolutionLinkService());
    const alertService = inject('alertService', () => useAlertService(), true);

    const solutionLinks: Ref<ISolutionLink[]> = ref([]);

    const isFetching = ref(false);

    const clear = () => {};

    const retrieveSolutionLinks = async () => {
      isFetching.value = true;
      try {
        const res = await solutionLinkService().retrieve();
        solutionLinks.value = res.data;
      } catch (err) {
        alertService.showHttpError(err.response);
      } finally {
        isFetching.value = false;
      }
    };

    const handleSyncList = () => {
      retrieveSolutionLinks();
    };

    onMounted(async () => {
      await retrieveSolutionLinks();
    });

    const removeId: Ref<number> = ref(null);
    const removeEntity = ref<any>(null);
    const prepareRemove = (instance: ISolutionLink) => {
      removeId.value = instance.id;
      removeEntity.value.show();
    };
    const closeDialog = () => {
      removeEntity.value.hide();
    };
    const removeSolutionLink = async () => {
      try {
        await solutionLinkService().delete(removeId.value);
        const message = `A SolutionLink is deleted with identifier ${removeId.value}`;
        alertService.showInfo(message, { variant: 'danger' });
        removeId.value = null;
        retrieveSolutionLinks();
        closeDialog();
      } catch (error) {
        alertService.showHttpError(error.response);
      }
    };

    return {
      solutionLinks,
      handleSyncList,
      isFetching,
      retrieveSolutionLinks,
      clear,
      removeId,
      removeEntity,
      prepareRemove,
      closeDialog,
      removeSolutionLink,
    };
  },
});
