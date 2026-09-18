import { type Ref, defineComponent, inject, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { useAlertService } from '@/shared/alert/alert.service';
import { type IOpportunityLink } from '@/shared/model/opportunity-link.model';

import OpportunityLinkService from './opportunity-link.service';

export default defineComponent({
  name: 'OpportunityLinkDetails',
  setup() {
    const opportunityLinkService = inject('opportunityLinkService', () => new OpportunityLinkService());
    const alertService = inject('alertService', () => useAlertService(), true);

    const route = useRoute();
    const router = useRouter();

    const previousState = () => router.go(-1);
    const opportunityLink: Ref<IOpportunityLink> = ref({});

    const retrieveOpportunityLink = async opportunityLinkId => {
      try {
        const res = await opportunityLinkService().find(opportunityLinkId);
        opportunityLink.value = res;
      } catch (error) {
        alertService.showHttpError(error.response);
      }
    };

    if (route.params?.opportunityLinkId) {
      retrieveOpportunityLink(route.params.opportunityLinkId);
    }

    return {
      alertService,
      opportunityLink,

      previousState,
    };
  },
});
