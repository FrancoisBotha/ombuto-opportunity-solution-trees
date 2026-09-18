import { type Ref, defineComponent, inject, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { useAlertService } from '@/shared/alert/alert.service';
import { useDateFormat } from '@/shared/composables';
import useDataUtils from '@/shared/data/data-utils.service';
import { type IExperiment } from '@/shared/model/experiment.model';

import ExperimentService from './experiment.service';

export default defineComponent({
  name: 'ExperimentDetails',
  setup() {
    const dateFormat = useDateFormat();
    const experimentService = inject('experimentService', () => new ExperimentService());
    const alertService = inject('alertService', () => useAlertService(), true);

    const dataUtils = useDataUtils();

    const route = useRoute();
    const router = useRouter();

    const previousState = () => router.go(-1);
    const experiment: Ref<IExperiment> = ref({});

    const retrieveExperiment = async experimentId => {
      try {
        const res = await experimentService().find(experimentId);
        experiment.value = res;
      } catch (error) {
        alertService.showHttpError(error.response);
      }
    };

    if (route.params?.experimentId) {
      retrieveExperiment(route.params.experimentId);
    }

    return {
      ...dateFormat,
      alertService,
      experiment,

      ...dataUtils,

      previousState,
    };
  },
});
