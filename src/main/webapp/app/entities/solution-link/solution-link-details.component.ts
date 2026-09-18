import { type Ref, defineComponent, inject, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { useAlertService } from '@/shared/alert/alert.service';
import { type ISolutionLink } from '@/shared/model/solution-link.model';

import SolutionLinkService from './solution-link.service';

export default defineComponent({
  name: 'SolutionLinkDetails',
  setup() {
    const solutionLinkService = inject('solutionLinkService', () => new SolutionLinkService());
    const alertService = inject('alertService', () => useAlertService(), true);

    const route = useRoute();
    const router = useRouter();

    const previousState = () => router.go(-1);
    const solutionLink: Ref<ISolutionLink> = ref({});

    const retrieveSolutionLink = async solutionLinkId => {
      try {
        const res = await solutionLinkService().find(solutionLinkId);
        solutionLink.value = res;
      } catch (error) {
        alertService.showHttpError(error.response);
      }
    };

    if (route.params?.solutionLinkId) {
      retrieveSolutionLink(route.params.solutionLinkId);
    }

    return {
      alertService,
      solutionLink,

      previousState,
    };
  },
});
