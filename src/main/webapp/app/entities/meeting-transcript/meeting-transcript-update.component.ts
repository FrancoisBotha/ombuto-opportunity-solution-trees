import { type Ref, computed, defineComponent, inject, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { useVuelidate } from '@vuelidate/core';

import AssumptionService from '@/entities/assumption/assumption.service';
import EvidenceService from '@/entities/evidence/evidence.service';
import OpportunityService from '@/entities/opportunity/opportunity.service';
import OutcomeService from '@/entities/outcome/outcome.service';
import ProductService from '@/entities/product/product.service';
import SolutionService from '@/entities/solution/solution.service';
import UserService from '@/entities/user/user.service';
import { useAlertService } from '@/shared/alert/alert.service';
import { useDateFormat, useValidation } from '@/shared/composables';
import useDataUtils from '@/shared/data/data-utils.service';
import { type IAssumption } from '@/shared/model/assumption.model';
import { MeetingTranscriptSource } from '@/shared/model/enumerations/meeting-transcript-source.model';
import { type IEvidence } from '@/shared/model/evidence.model';
import { type IOpportunity } from '@/shared/model/opportunity.model';
import { type IOutcome } from '@/shared/model/outcome.model';
import { type IProduct } from '@/shared/model/product.model';
import { type ISolution } from '@/shared/model/solution.model';
import { type IMeetingTranscript, MeetingTranscript } from '@/shared/model/meeting-transcript.model';

import MeetingTranscriptService from './meeting-transcript.service';

export default defineComponent({
  name: 'MeetingTranscriptUpdate',
  setup() {
    const meetingTranscriptService = inject('meetingTranscriptService', () => new MeetingTranscriptService());
    const alertService = inject('alertService', () => useAlertService(), true);

    const meetingTranscript: Ref<IMeetingTranscript> = ref(new MeetingTranscript());
    const userService = inject('userService', () => new UserService());
    const users: Ref<Array<any>> = ref([]);

    const productService = inject('productService', () => new ProductService());

    const products: Ref<IProduct[]> = ref([]);

    const outcomeService = inject('outcomeService', () => new OutcomeService());

    const outcomes: Ref<IOutcome[]> = ref([]);

    const opportunityService = inject('opportunityService', () => new OpportunityService());

    const opportunities: Ref<IOpportunity[]> = ref([]);

    const solutionService = inject('solutionService', () => new SolutionService());

    const solutions: Ref<ISolution[]> = ref([]);

    const assumptionService = inject('assumptionService', () => new AssumptionService());

    const assumptions: Ref<IAssumption[]> = ref([]);

    const evidenceService = inject('evidenceService', () => new EvidenceService());

    const evidences: Ref<IEvidence[]> = ref([]);
    const meetingTranscriptSourceValues: Ref<string[]> = ref(Object.keys(MeetingTranscriptSource));
    const isSaving = ref(false);
    const currentLanguage = inject('currentLanguage', () => computed(() => navigator.language ?? 'en'), true);

    const route = useRoute();
    const router = useRouter();

    const previousState = () => router.go(-1);

    const retrieveMeetingTranscript = async meetingTranscriptId => {
      try {
        const res = await meetingTranscriptService().find(meetingTranscriptId);
        res.createdDate = new Date(res.createdDate);
        res.editedDate = new Date(res.editedDate);
        meetingTranscript.value = res;
      } catch (error) {
        alertService.showHttpError(error.response);
      }
    };

    if (route.params?.meetingTranscriptId) {
      retrieveMeetingTranscript(route.params.meetingTranscriptId);
    }

    const initRelationships = () => {
      userService()
        .retrieve()
        .then(res => {
          users.value = res.data;
        });
      productService()
        .retrieve()
        .then(res => {
          products.value = res.data;
        });
      outcomeService()
        .retrieve()
        .then(res => {
          outcomes.value = res.data;
        });
      opportunityService()
        .retrieve()
        .then(res => {
          opportunities.value = res.data;
        });
      solutionService()
        .retrieve()
        .then(res => {
          solutions.value = res.data;
        });
      assumptionService()
        .retrieve()
        .then(res => {
          assumptions.value = res.data;
        });
      evidenceService()
        .retrieve()
        .then(res => {
          evidences.value = res.data;
        });
    };

    initRelationships();

    const dataUtils = useDataUtils();

    const validations = useValidation();
    const validationRules = {
      title: {
        required: validations.required('This field is required.'),
        minLength: validations.minLength('This field is required to be at least 2 characters.', 2),
        maxLength: validations.maxLength('This field cannot be longer than 200 characters.', 200),
      },
      meetingDate: {
        required: validations.required('This field is required.'),
      },
      attendees: {
        maxLength: validations.maxLength('This field cannot be longer than 500 characters.', 500),
      },
      body: {
        required: validations.required('This field is required.'),
      },
      source: {
        required: validations.required('This field is required.'),
      },
      createdDate: {
        required: validations.required('This field is required.'),
      },
      editedDate: {},
      author: {},
      product: {},
      outcome: {},
      opportunity: {},
      solution: {},
      assumption: {},
      evidence: {},
    };
    const v$ = useVuelidate(validationRules, meetingTranscript as any);
    v$.value.$validate();

    return {
      meetingTranscriptService,
      alertService,
      meetingTranscript,
      previousState,
      meetingTranscriptSourceValues,
      isSaving,
      currentLanguage,
      users,
      products,
      outcomes,
      opportunities,
      solutions,
      assumptions,
      evidences,
      ...dataUtils,
      v$,
      ...useDateFormat({ entityRef: meetingTranscript }),
    };
  },
  created(): void {},
  methods: {
    save(): void {
      this.isSaving = true;
      if (this.meetingTranscript.id) {
        this.meetingTranscriptService()
          .update(this.meetingTranscript)
          .then(param => {
            this.isSaving = false;
            this.previousState();
            this.alertService.showInfo(`A MeetingTranscript is updated with identifier ${param.id}`);
          })
          .catch(error => {
            this.isSaving = false;
            this.alertService.showHttpError(error.response);
          });
      } else {
        this.meetingTranscriptService()
          .create(this.meetingTranscript)
          .then(param => {
            this.isSaving = false;
            this.previousState();
            this.alertService.showSuccess(`A MeetingTranscript is created with identifier ${param.id}`);
          })
          .catch(error => {
            this.isSaving = false;
            this.alertService.showHttpError(error.response);
          });
      }
    },
  },
});
