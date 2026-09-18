import { type Ref, defineComponent, inject, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { useAlertService } from '@/shared/alert/alert.service';
import { useDateFormat } from '@/shared/composables';
import useDataUtils from '@/shared/data/data-utils.service';
import { type IOutcome } from '@/shared/model/outcome.model';

import OutcomeService from './outcome.service';

export default defineComponent({
  name: 'OutcomeDetails',
  setup() {
    const dateFormat = useDateFormat();
    const outcomeService = inject('outcomeService', () => new OutcomeService());
    const alertService = inject('alertService', () => useAlertService(), true);

    const dataUtils = useDataUtils();

    const route = useRoute();
    const router = useRouter();

    const previousState = () => router.go(-1);
    const outcome: Ref<IOutcome> = ref({});

    const retrieveOutcome = async outcomeId => {
      try {
        const res = await outcomeService().find(outcomeId);
        outcome.value = res;
      } catch (error) {
        alertService.showHttpError(error.response);
      }
    };

    if (route.params?.outcomeId) {
      retrieveOutcome(route.params.outcomeId);
    }

    return {
      ...dateFormat,
      alertService,
      outcome,

      ...dataUtils,

      previousState,
    };
  },
});
