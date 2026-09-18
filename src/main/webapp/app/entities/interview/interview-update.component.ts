import { type Ref, computed, defineComponent, inject, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { useVuelidate } from '@vuelidate/core';

import OpportunityService from '@/entities/opportunity/opportunity.service';
import ProductService from '@/entities/product/product.service';
import UserService from '@/entities/user/user.service';
import { useAlertService } from '@/shared/alert/alert.service';
import { useDateFormat, useValidation } from '@/shared/composables';
import useDataUtils from '@/shared/data/data-utils.service';
import { type IInterview, Interview } from '@/shared/model/interview.model';
import { type IOpportunity } from '@/shared/model/opportunity.model';
import { type IProduct } from '@/shared/model/product.model';

import InterviewService from './interview.service';

export default defineComponent({
  name: 'InterviewUpdate',
  setup() {
    const interviewService = inject('interviewService', () => new InterviewService());
    const alertService = inject('alertService', () => useAlertService(), true);

    const interview: Ref<IInterview> = ref(new Interview());

    const productService = inject('productService', () => new ProductService());

    const products: Ref<IProduct[]> = ref([]);
    const userService = inject('userService', () => new UserService());
    const users: Ref<Array<any>> = ref([]);

    const opportunityService = inject('opportunityService', () => new OpportunityService());

    const opportunities: Ref<IOpportunity[]> = ref([]);
    const isSaving = ref(false);
    const currentLanguage = inject('currentLanguage', () => computed(() => navigator.language ?? 'en'), true);

    const route = useRoute();
    const router = useRouter();

    const previousState = () => router.go(-1);

    const retrieveInterview = async interviewId => {
      try {
        const res = await interviewService().find(interviewId);
        res.createdDate = new Date(res.createdDate);
        interview.value = res;
      } catch (error) {
        alertService.showHttpError(error.response);
      }
    };

    if (route.params?.interviewId) {
      retrieveInterview(route.params.interviewId);
    }

    const initRelationships = () => {
      productService()
        .retrieve()
        .then(res => {
          products.value = res.data;
        });
      userService()
        .retrieve()
        .then(res => {
          users.value = res.data;
        });
      opportunityService()
        .retrieve()
        .then(res => {
          opportunities.value = res.data;
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
      participant: {
        maxLength: validations.maxLength('This field cannot be longer than 200 characters.', 200),
      },
      interviewDate: {
        required: validations.required('This field is required.'),
      },
      notes: {},
      recordingUrl: {
        maxLength: validations.maxLength('This field cannot be longer than 2000 characters.', 2000),
      },
      createdDate: {
        required: validations.required('This field is required.'),
      },
      product: {
        required: validations.required('This field is required.'),
      },
      interviewer: {},
      opportunities: {},
    };
    const v$ = useVuelidate(validationRules, interview as any);
    v$.value.$validate();

    return {
      interviewService,
      alertService,
      interview,
      previousState,
      isSaving,
      currentLanguage,
      products,
      users,
      opportunities,
      ...dataUtils,
      v$,
      ...useDateFormat({ entityRef: interview }),
    };
  },
  created(): void {
    this.interview.opportunities = [];
  },
  methods: {
    save(): void {
      this.isSaving = true;
      if (this.interview.id) {
        this.interviewService()
          .update(this.interview)
          .then(param => {
            this.isSaving = false;
            this.previousState();
            this.alertService.showInfo(`A Interview is updated with identifier ${param.id}`);
          })
          .catch(error => {
            this.isSaving = false;
            this.alertService.showHttpError(error.response);
          });
      } else {
        this.interviewService()
          .create(this.interview)
          .then(param => {
            this.isSaving = false;
            this.previousState();
            this.alertService.showSuccess(`A Interview is created with identifier ${param.id}`);
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
