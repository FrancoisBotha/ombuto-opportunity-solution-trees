import { type Ref, defineComponent, inject, onMounted, ref } from 'vue';

import { useAlertService } from '@/shared/alert/alert.service';
import { useDateFormat } from '@/shared/composables';
import { type INodeLink } from '@/shared/model/node-link.model';

import NodeLinkService from './node-link.service';

export default defineComponent({
  name: 'NodeLink',
  setup() {
    const dateFormat = useDateFormat();
    const nodeLinkService = inject('nodeLinkService', () => new NodeLinkService());
    const alertService = inject('alertService', () => useAlertService(), true);

    const nodeLinks: Ref<INodeLink[]> = ref([]);

    const isFetching = ref(false);

    const clear = () => {};

    const retrieveNodeLinks = async () => {
      isFetching.value = true;
      try {
        const res = await nodeLinkService().retrieve();
        nodeLinks.value = res.data;
      } catch (err) {
        alertService.showHttpError(err.response);
      } finally {
        isFetching.value = false;
      }
    };

    const handleSyncList = () => {
      retrieveNodeLinks();
    };

    onMounted(async () => {
      await retrieveNodeLinks();
    });

    const removeId: Ref<number> = ref(null);
    const removeEntity = ref<any>(null);
    const prepareRemove = (instance: INodeLink) => {
      removeId.value = instance.id;
      removeEntity.value.show();
    };
    const closeDialog = () => {
      removeEntity.value.hide();
    };
    const removeNodeLink = async () => {
      try {
        await nodeLinkService().delete(removeId.value);
        const message = `A NodeLink is deleted with identifier ${removeId.value}`;
        alertService.showInfo(message, { variant: 'danger' });
        removeId.value = null;
        retrieveNodeLinks();
        closeDialog();
      } catch (error) {
        alertService.showHttpError(error.response);
      }
    };

    return {
      nodeLinks,
      handleSyncList,
      isFetching,
      retrieveNodeLinks,
      clear,
      ...dateFormat,
      removeId,
      removeEntity,
      prepareRemove,
      closeDialog,
      removeNodeLink,
    };
  },
});
