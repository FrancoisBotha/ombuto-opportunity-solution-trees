import { type Ref, defineComponent, inject, onMounted, ref } from 'vue';

import { useAlertService } from '@/shared/alert/alert.service';
import { type IOpportunityLink } from '@/shared/model/opportunity-link.model';

import OpportunityLinkService from './opportunity-link.service';

export default defineComponent({
  name: 'OpportunityLink',
  setup() {
    const opportunityLinkService = inject('opportunityLinkService', () => new OpportunityLinkService());
    const alertService = inject('alertService', () => useAlertService(), true);

    const opportunityLinks: Ref<IOpportunityLink[]> = ref([]);

    const isFetching = ref(false);

    const clear = () => {};

    const retrieveOpportunityLinks = async () => {
      isFetching.value = true;
      try {
        const res = await opportunityLinkService().retrieve();
        opportunityLinks.value = res.data;
      } catch (err) {
        alertService.showHttpError(err.response);
      } finally {
        isFetching.value = false;
      }
    };

    const handleSyncList = () => {
      retrieveOpportunityLinks();
    };

    onMounted(async () => {
      await retrieveOpportunityLinks();
    });

    const removeId: Ref<number> = ref(null);
    const removeEntity = ref<any>(null);
    const prepareRemove = (instance: IOpportunityLink) => {
      removeId.value = instance.id;
      removeEntity.value.show();
    };
    const closeDialog = () => {
      removeEntity.value.hide();
    };
    const removeOpportunityLink = async () => {
      try {
        await opportunityLinkService().delete(removeId.value);
        const message = `A OpportunityLink is deleted with identifier ${removeId.value}`;
        alertService.showInfo(message, { variant: 'danger' });
        removeId.value = null;
        retrieveOpportunityLinks();
        closeDialog();
      } catch (error) {
        alertService.showHttpError(error.response);
      }
    };

    return {
      opportunityLinks,
      handleSyncList,
      isFetching,
      retrieveOpportunityLinks,
      clear,
      removeId,
      removeEntity,
      prepareRemove,
      closeDialog,
      removeOpportunityLink,
    };
  },
});
