import { type Ref, defineComponent, inject, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { useAlertService } from '@/shared/alert/alert.service';
import { useDateFormat } from '@/shared/composables';
import useDataUtils from '@/shared/data/data-utils.service';
import { type ISolution } from '@/shared/model/solution.model';

import SolutionService from './solution.service';

export default defineComponent({
  name: 'SolutionDetails',
  setup() {
    const dateFormat = useDateFormat();
    const solutionService = inject('solutionService', () => new SolutionService());
    const alertService = inject('alertService', () => useAlertService(), true);

    const dataUtils = useDataUtils();

    const route = useRoute();
    const router = useRouter();

    const previousState = () => router.go(-1);
    const solution: Ref<ISolution> = ref({});

    const retrieveSolution = async solutionId => {
      try {
        const res = await solutionService().find(solutionId);
        solution.value = res;
      } catch (error) {
        alertService.showHttpError(error.response);
      }
    };

    if (route.params?.solutionId) {
      retrieveSolution(route.params.solutionId);
    }

    return {
      ...dateFormat,
      alertService,
      solution,

      ...dataUtils,

      previousState,
    };
  },
});
