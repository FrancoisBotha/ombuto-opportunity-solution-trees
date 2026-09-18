import { type Ref, defineComponent, inject, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { useAlertService } from '@/shared/alert/alert.service';
import { useDateFormat } from '@/shared/composables';
import useDataUtils from '@/shared/data/data-utils.service';
import { type IOpportunity } from '@/shared/model/opportunity.model';

import OpportunityService from './opportunity.service';

export default defineComponent({
  name: 'OpportunityDetails',
  setup() {
    const dateFormat = useDateFormat();
    const opportunityService = inject('opportunityService', () => new OpportunityService());
    const alertService = inject('alertService', () => useAlertService(), true);

    const dataUtils = useDataUtils();

    const route = useRoute();
    const router = useRouter();

    const previousState = () => router.go(-1);
    const opportunity: Ref<IOpportunity> = ref({});

    const retrieveOpportunity = async opportunityId => {
      try {
        const res = await opportunityService().find(opportunityId);
        opportunity.value = res;
      } catch (error) {
        alertService.showHttpError(error.response);
      }
    };

    if (route.params?.opportunityId) {
      retrieveOpportunity(route.params.opportunityId);
    }

    return {
      ...dateFormat,
      alertService,
      opportunity,

      ...dataUtils,

      previousState,
    };
  },
});
