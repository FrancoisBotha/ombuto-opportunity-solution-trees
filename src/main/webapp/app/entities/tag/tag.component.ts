import { type Ref, defineComponent, inject, onMounted, ref } from 'vue';

import { useAlertService } from '@/shared/alert/alert.service';
import { type ITag } from '@/shared/model/tag.model';

import TagService from './tag.service';

export default defineComponent({
  name: 'Tag',
  setup() {
    const tagService = inject('tagService', () => new TagService());
    const alertService = inject('alertService', () => useAlertService(), true);

    const tags: Ref<ITag[]> = ref([]);

    const isFetching = ref(false);

    const clear = () => {};

    const retrieveTags = async () => {
      isFetching.value = true;
      try {
        const res = await tagService().retrieve();
        tags.value = res.data;
      } catch (err) {
        alertService.showHttpError(err.response);
      } finally {
        isFetching.value = false;
      }
    };

    const handleSyncList = () => {
      retrieveTags();
    };

    onMounted(async () => {
      await retrieveTags();
    });

    const removeId: Ref<number> = ref(null);
    const removeEntity = ref<any>(null);
    const prepareRemove = (instance: ITag) => {
      removeId.value = instance.id;
      removeEntity.value.show();
    };
    const closeDialog = () => {
      removeEntity.value.hide();
    };
    const removeTag = async () => {
      try {
        await tagService().delete(removeId.value);
        const message = `A Tag is deleted with identifier ${removeId.value}`;
        alertService.showInfo(message, { variant: 'danger' });
        removeId.value = null;
        retrieveTags();
        closeDialog();
      } catch (error) {
        alertService.showHttpError(error.response);
      }
    };

    return {
      tags,
      handleSyncList,
      isFetching,
      retrieveTags,
      clear,
      removeId,
      removeEntity,
      prepareRemove,
      closeDialog,
      removeTag,
    };
  },
});
