import { type Ref, computed, defineComponent, inject, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { useVuelidate } from '@vuelidate/core';

import OpportunityService from '@/entities/opportunity/opportunity.service';
import { useAlertService } from '@/shared/alert/alert.service';
import { useDateFormat, useValidation } from '@/shared/composables';
import { type IOpenQuestion, OpenQuestion } from '@/shared/model/open-question.model';
import { type IOpportunity } from '@/shared/model/opportunity.model';

import OpenQuestionService from './open-question.service';

export default defineComponent({
  name: 'OpenQuestionUpdate',
  setup() {
    const openQuestionService = inject('openQuestionService', () => new OpenQuestionService());
    const alertService = inject('alertService', () => useAlertService(), true);

    const openQuestion: Ref<IOpenQuestion> = ref(new OpenQuestion());

    const opportunityService = inject('opportunityService', () => new OpportunityService());

    const opportunities: Ref<IOpportunity[]> = ref([]);
    const isSaving = ref(false);
    const currentLanguage = inject('currentLanguage', () => computed(() => navigator.language ?? 'en'), true);

    const route = useRoute();
    const router = useRouter();

    const previousState = () => router.go(-1);

    const retrieveOpenQuestion = async openQuestionId => {
      try {
        const res = await openQuestionService().find(openQuestionId);
        res.createdDate = new Date(res.createdDate);
        openQuestion.value = res;
      } catch (error) {
        alertService.showHttpError(error.response);
      }
    };

    if (route.params?.openQuestionId) {
      retrieveOpenQuestion(route.params.openQuestionId);
    }

    const initRelationships = () => {
      opportunityService()
        .retrieve()
        .then(res => {
          opportunities.value = res.data;
        });
    };

    initRelationships();

    const validations = useValidation();
    const validationRules = {
      questionText: {
        required: validations.required('This field is required.'),
        minLength: validations.minLength('This field is required to be at least 1 characters.', 1),
        maxLength: validations.maxLength('This field cannot be longer than 500 characters.', 500),
      },
      done: {
        required: validations.required('This field is required.'),
      },
      sortOrder: {
        required: validations.required('This field is required.'),
        integer: validations.integer('This field should be a number.'),
      },
      createdDate: {
        required: validations.required('This field is required.'),
      },
      opportunity: {
        required: validations.required('This field is required.'),
      },
    };
    const v$ = useVuelidate(validationRules, openQuestion as any);
    v$.value.$validate();

    return {
      openQuestionService,
      alertService,
      openQuestion,
      previousState,
      isSaving,
      currentLanguage,
      opportunities,
      v$,
      ...useDateFormat({ entityRef: openQuestion }),
    };
  },
  created(): void {},
  methods: {
    save(): void {
      this.isSaving = true;
      if (this.openQuestion.id) {
        this.openQuestionService()
          .update(this.openQuestion)
          .then(param => {
            this.isSaving = false;
            this.previousState();
            this.alertService.showInfo(`A OpenQuestion is updated with identifier ${param.id}`);
          })
          .catch(error => {
            this.isSaving = false;
            this.alertService.showHttpError(error.response);
          });
      } else {
        this.openQuestionService()
          .create(this.openQuestion)
          .then(param => {
            this.isSaving = false;
            this.previousState();
            this.alertService.showSuccess(`A OpenQuestion is created with identifier ${param.id}`);
          })
          .catch(error => {
            this.isSaving = false;
            this.alertService.showHttpError(error.response);
          });
      }
    },
  },
});
