import { type Ref, computed, defineComponent, inject, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { useVuelidate } from '@vuelidate/core';

import AssumptionService from '@/entities/assumption/assumption.service';
import OpportunityService from '@/entities/opportunity/opportunity.service';
import { useAlertService } from '@/shared/alert/alert.service';
import { useDateFormat, useValidation } from '@/shared/composables';
import useDataUtils from '@/shared/data/data-utils.service';
import { type IAssumption } from '@/shared/model/assumption.model';
import { Evidence, type IEvidence } from '@/shared/model/evidence.model';
import { type IOpportunity } from '@/shared/model/opportunity.model';

import EvidenceService from './evidence.service';

export default defineComponent({
  name: 'EvidenceUpdate',
  setup() {
    const evidenceService = inject('evidenceService', () => new EvidenceService());
    const alertService = inject('alertService', () => useAlertService(), true);

    const evidence: Ref<IEvidence> = ref(new Evidence());

    const opportunityService = inject('opportunityService', () => new OpportunityService());

    const opportunities: Ref<IOpportunity[]> = ref([]);

    const assumptionService = inject('assumptionService', () => new AssumptionService());

    const assumptions: Ref<IAssumption[]> = ref([]);
    const isSaving = ref(false);
    const currentLanguage = inject('currentLanguage', () => computed(() => navigator.language ?? 'en'), true);

    const route = useRoute();
    const router = useRouter();

    const previousState = () => router.go(-1);

    const retrieveEvidence = async evidenceId => {
      try {
        const res = await evidenceService().find(evidenceId);
        res.createdDate = new Date(res.createdDate);
        res.lastModifiedDate = new Date(res.lastModifiedDate);
        evidence.value = res;
      } catch (error) {
        alertService.showHttpError(error.response);
      }
    };

    if (route.params?.evidenceId) {
      retrieveEvidence(route.params.evidenceId);
    }

    const initRelationships = () => {
      opportunityService()
        .retrieve()
        .then(res => {
          opportunities.value = res.data;
        });
      assumptionService()
        .retrieve()
        .then(res => {
          assumptions.value = res.data;
        });
    };

    initRelationships();

    const dataUtils = useDataUtils();

    const validations = useValidation();
    const validationRules = {
      title: {
        required: validations.required('This field is required.'),
        minLength: validations.minLength('This field is required to be at least 2 characters.', 2),
        maxLength: validations.maxLength('This field cannot be longer than 500 characters.', 500),
      },
      description: {},
      sortOrder: {
        required: validations.required('This field is required.'),
        integer: validations.integer('This field should be a number.'),
      },
      createdDate: {
        required: validations.required('This field is required.'),
      },
      lastModifiedDate: {},
      opportunity: {},
      assumption: {},
    };
    const v$ = useVuelidate(validationRules, evidence as any);
    v$.value.$validate();

    return {
      evidenceService,
      alertService,
      evidence,
      previousState,
      isSaving,
      currentLanguage,
      opportunities,
      assumptions,
      ...dataUtils,
      v$,
      ...useDateFormat({ entityRef: evidence }),
    };
  },
  created(): void {},
  methods: {
    save(): void {
      this.isSaving = true;
      if (this.evidence.id) {
        this.evidenceService()
          .update(this.evidence)
          .then(param => {
            this.isSaving = false;
            this.previousState();
            this.alertService.showInfo(`A Evidence is updated with identifier ${param.id}`);
          })
          .catch(error => {
            this.isSaving = false;
            this.alertService.showHttpError(error.response);
          });
      } else {
        this.evidenceService()
          .create(this.evidence)
          .then(param => {
            this.isSaving = false;
            this.previousState();
            this.alertService.showSuccess(`A Evidence is created with identifier ${param.id}`);
          })
          .catch(error => {
            this.isSaving = false;
            this.alertService.showHttpError(error.response);
          });
      }
    },
  },
});
