import { type Ref, computed, defineComponent, inject, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { useVuelidate } from '@vuelidate/core';

import InterviewService from '@/entities/interview/interview.service';
import OutcomeService from '@/entities/outcome/outcome.service';
import TagService from '@/entities/tag/tag.service';
import UserService from '@/entities/user/user.service';
import { useAlertService } from '@/shared/alert/alert.service';
import { useDateFormat, useValidation } from '@/shared/composables';
import useDataUtils from '@/shared/data/data-utils.service';
import { OpportunityStatus } from '@/shared/model/enumerations/opportunity-status.model';
import { type IInterview } from '@/shared/model/interview.model';
import { type IOpportunity, Opportunity } from '@/shared/model/opportunity.model';
import { type IOutcome } from '@/shared/model/outcome.model';
import { type ITag } from '@/shared/model/tag.model';

import OpportunityService from './opportunity.service';

export default defineComponent({
  name: 'OpportunityUpdate',
  setup() {
    const opportunityService = inject('opportunityService', () => new OpportunityService());
    const alertService = inject('alertService', () => useAlertService(), true);

    const opportunity: Ref<IOpportunity> = ref(new Opportunity());

    const outcomeService = inject('outcomeService', () => new OutcomeService());

    const outcomes: Ref<IOutcome[]> = ref([]);

    const opportunities: Ref<IOpportunity[]> = ref([]);
    const userService = inject('userService', () => new UserService());
    const users: Ref<Array<any>> = ref([]);

    const interviewService = inject('interviewService', () => new InterviewService());

    const interviews: Ref<IInterview[]> = ref([]);

    const tagService = inject('tagService', () => new TagService());

    const tags: Ref<ITag[]> = ref([]);
    const opportunityStatusValues: Ref<string[]> = ref(Object.keys(OpportunityStatus));
    const isSaving = ref(false);
    const currentLanguage = inject('currentLanguage', () => computed(() => navigator.language ?? 'en'), true);

    const route = useRoute();
    const router = useRouter();

    const previousState = () => router.go(-1);

    const retrieveOpportunity = async opportunityId => {
      try {
        const res = await opportunityService().find(opportunityId);
        res.createdDate = new Date(res.createdDate);
        res.lastModifiedDate = new Date(res.lastModifiedDate);
        opportunity.value = res;
      } catch (error) {
        alertService.showHttpError(error.response);
      }
    };

    if (route.params?.opportunityId) {
      retrieveOpportunity(route.params.opportunityId);
    }

    const initRelationships = () => {
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
      userService()
        .retrieve()
        .then(res => {
          users.value = res.data;
        });
      interviewService()
        .retrieve()
        .then(res => {
          interviews.value = res.data;
        });
      tagService()
        .retrieve()
        .then(res => {
          tags.value = res.data;
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
      description: {},
      status: {
        required: validations.required('This field is required.'),
      },
      valuerating: {
        required: validations.required('This field is required.'),
        integer: validations.integer('This field should be a number.'),
        min: validations.minValue('This field should be at least 1.', 1),
        max: validations.maxValue('This field cannot be more than 5.', 5),
      },
      priority: {
        required: validations.required('This field is required.'),
        integer: validations.integer('This field should be a number.'),
        min: validations.minValue('This field should be at least 1.', 1),
        max: validations.maxValue('This field cannot be more than 100.', 100),
      },
      sortOrder: {
        required: validations.required('This field is required.'),
        integer: validations.integer('This field should be a number.'),
      },
      createdDate: {
        required: validations.required('This field is required.'),
      },
      lastModifiedDate: {},
      outcome: {
        required: validations.required('This field is required.'),
      },
      parent: {},
      owner: {},
      interviews: {},
      tags: {},
    };
    const v$ = useVuelidate(validationRules, opportunity as any);
    v$.value.$validate();

    return {
      opportunityService,
      alertService,
      opportunity,
      previousState,
      opportunityStatusValues,
      isSaving,
      currentLanguage,
      outcomes,
      opportunities,
      users,
      interviews,
      tags,
      ...dataUtils,
      v$,
      ...useDateFormat({ entityRef: opportunity }),
    };
  },
  created(): void {
    this.opportunity.interviews = [];
    this.opportunity.tags = [];
  },
  methods: {
    save(): void {
      this.isSaving = true;
      if (this.opportunity.id) {
        this.opportunityService()
          .update(this.opportunity)
          .then(param => {
            this.isSaving = false;
            this.previousState();
            this.alertService.showInfo(`A Opportunity is updated with identifier ${param.id}`);
          })
          .catch(error => {
            this.isSaving = false;
            this.alertService.showHttpError(error.response);
          });
      } else {
        this.opportunityService()
          .create(this.opportunity)
          .then(param => {
            this.isSaving = false;
            this.previousState();
            this.alertService.showSuccess(`A Opportunity is created with identifier ${param.id}`);
          })
          .catch(error => {
            this.isSaving = false;
            this.alertService.showHttpError(error.response);
          });
      }
    },

    getSelected(selectedVals, option, pkField = 'id'): any {
      if (selectedVals) {
        return selectedVals.find(value => option[pkField] === value[pkField]) ?? option;
      }
      return option;
    },
  },
});
