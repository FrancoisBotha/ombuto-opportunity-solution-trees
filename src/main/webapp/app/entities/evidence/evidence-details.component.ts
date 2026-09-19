import { type Ref, defineComponent, inject, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { useAlertService } from '@/shared/alert/alert.service';
import { useDateFormat } from '@/shared/composables';
import useDataUtils from '@/shared/data/data-utils.service';
import { type IEvidence } from '@/shared/model/evidence.model';

import EvidenceService from './evidence.service';

export default defineComponent({
  name: 'EvidenceDetails',
  setup() {
    const dateFormat = useDateFormat();
    const evidenceService = inject('evidenceService', () => new EvidenceService());
    const alertService = inject('alertService', () => useAlertService(), true);

    const dataUtils = useDataUtils();

    const route = useRoute();
    const router = useRouter();

    const previousState = () => router.go(-1);
    const evidence: Ref<IEvidence> = ref({});

    const retrieveEvidence = async evidenceId => {
      try {
        const res = await evidenceService().find(evidenceId);
        evidence.value = res;
      } catch (error) {
        alertService.showHttpError(error.response);
      }
    };

    if (route.params?.evidenceId) {
      retrieveEvidence(route.params.evidenceId);
    }

    return {
      ...dateFormat,
      alertService,
      evidence,

      ...dataUtils,

      previousState,
    };
  },
});
