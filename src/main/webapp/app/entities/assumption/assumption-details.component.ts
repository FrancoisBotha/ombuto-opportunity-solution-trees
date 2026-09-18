import { type Ref, defineComponent, inject, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { useAlertService } from '@/shared/alert/alert.service';
import { useDateFormat } from '@/shared/composables';
import { type IAssumption } from '@/shared/model/assumption.model';

import AssumptionService from './assumption.service';

export default defineComponent({
  name: 'AssumptionDetails',
  setup() {
    const dateFormat = useDateFormat();
    const assumptionService = inject('assumptionService', () => new AssumptionService());
    const alertService = inject('alertService', () => useAlertService(), true);

    const route = useRoute();
    const router = useRouter();

    const previousState = () => router.go(-1);
    const assumption: Ref<IAssumption> = ref({});

    const retrieveAssumption = async assumptionId => {
      try {
        const res = await assumptionService().find(assumptionId);
        assumption.value = res;
      } catch (error) {
        alertService.showHttpError(error.response);
      }
    };

    if (route.params?.assumptionId) {
      retrieveAssumption(route.params.assumptionId);
    }

    return {
      ...dateFormat,
      alertService,
      assumption,

      previousState,
    };
  },
});
