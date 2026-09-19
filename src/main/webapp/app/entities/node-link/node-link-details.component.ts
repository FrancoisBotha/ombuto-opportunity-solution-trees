import { type Ref, defineComponent, inject, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { useAlertService } from '@/shared/alert/alert.service';
import { useDateFormat } from '@/shared/composables';
import { type INodeLink } from '@/shared/model/node-link.model';

import NodeLinkService from './node-link.service';

export default defineComponent({
  name: 'NodeLinkDetails',
  setup() {
    const dateFormat = useDateFormat();
    const nodeLinkService = inject('nodeLinkService', () => new NodeLinkService());
    const alertService = inject('alertService', () => useAlertService(), true);

    const route = useRoute();
    const router = useRouter();

    const previousState = () => router.go(-1);
    const nodeLink: Ref<INodeLink> = ref({});

    const retrieveNodeLink = async nodeLinkId => {
      try {
        const res = await nodeLinkService().find(nodeLinkId);
        nodeLink.value = res;
      } catch (error) {
        alertService.showHttpError(error.response);
      }
    };

    if (route.params?.nodeLinkId) {
      retrieveNodeLink(route.params.nodeLinkId);
    }

    return {
      ...dateFormat,
      alertService,
      nodeLink,

      previousState,
    };
  },
});
