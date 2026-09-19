import { type Ref, defineComponent, inject, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { useAlertService } from '@/shared/alert/alert.service';
import { useDateFormat } from '@/shared/composables';
import { type INodeHistory } from '@/shared/model/node-history.model';

import NodeHistoryService from './node-history.service';

export default defineComponent({
  name: 'NodeHistoryDetails',
  setup() {
    const dateFormat = useDateFormat();
    const nodeHistoryService = inject('nodeHistoryService', () => new NodeHistoryService());
    const alertService = inject('alertService', () => useAlertService(), true);

    const route = useRoute();
    const router = useRouter();

    const previousState = () => router.go(-1);
    const nodeHistory: Ref<INodeHistory> = ref({});

    const retrieveNodeHistory = async nodeHistoryId => {
      try {
        const res = await nodeHistoryService().find(nodeHistoryId);
        nodeHistory.value = res;
      } catch (error) {
        alertService.showHttpError(error.response);
      }
    };

    if (route.params?.nodeHistoryId) {
      retrieveNodeHistory(route.params.nodeHistoryId);
    }

    return {
      ...dateFormat,
      alertService,
      nodeHistory,

      previousState,
    };
  },
});
