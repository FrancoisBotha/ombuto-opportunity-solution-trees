import { type Ref, computed, defineComponent, inject, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { useVuelidate } from '@vuelidate/core';

import SolutionService from '@/entities/solution/solution.service';
import UserService from '@/entities/user/user.service';
import { useAlertService } from '@/shared/alert/alert.service';
import { useDateFormat, useValidation } from '@/shared/composables';
import useDataUtils from '@/shared/data/data-utils.service';
import { Assumption, type IAssumption } from '@/shared/model/assumption.model';
import { AssumptionStatus } from '@/shared/model/enumerations/assumption-status.model';
import { type ISolution } from '@/shared/model/solution.model';

import AssumptionService from './assumption.service';

export default defineComponent({
  name: 'AssumptionUpdate',
  setup() {
    const assumptionService = inject('assumptionService', () => new AssumptionService());
    const alertService = inject('alertService', () => useAlertService(), true);

    const assumption: Ref<IAssumption> = ref(new Assumption());

    const solutionService = inject('solutionService', () => new SolutionService());

    const solutions: Ref<ISolution[]> = ref([]);
    const userService = inject('userService', () => new UserService());
    const users: Ref<Array<any>> = ref([]);
    const assumptionStatusValues: Ref<string[]> = ref(Object.keys(AssumptionStatus));
    const isSaving = ref(false);
    const currentLanguage = inject('currentLanguage', () => computed(() => navigator.language ?? 'en'), true);

    const route = useRoute();
    const router = useRouter();

    const previousState = () => router.go(-1);

    const retrieveAssumption = async assumptionId => {
      try {
        const res = await assumptionService().find(assumptionId);
        res.createdDate = new Date(res.createdDate);
        res.lastModifiedDate = new Date(res.lastModifiedDate);
        assumption.value = res;
      } catch (error) {
        alertService.showHttpError(error.response);
      }
    };

    if (route.params?.assumptionId) {
      retrieveAssumption(route.params.assumptionId);
    }

    const initRelationships = () => {
      solutionService()
        .retrieve()
        .then(res => {
          solutions.value = res.data;
        });
      userService()
        .retrieve()
        .then(res => {
          users.value = res.data;
        });
    };

    initRelationships();

    const dataUtils = useDataUtils();

    const validations = useValidation();
    const validationRules = {
      statement: {
        required: validations.required('This field is required.'),
        minLength: validations.minLength('This field is required to be at least 2 characters.', 2),
        maxLength: validations.maxLength('This field cannot be longer than 500 characters.', 500),
      },
      description: {},
      status: {
        required: validations.required('This field is required.'),
      },
      confidence: {
        required: validations.required('This field is required.'),
        integer: validations.integer('This field should be a number.'),
        min: validations.minValue('This field should be at least 0.', 0),
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
      solution: {
        required: validations.required('This field is required.'),
      },
      owner: {},
    };
    const v$ = useVuelidate(validationRules, assumption as any);
    v$.value.$validate();

    return {
      assumptionService,
      alertService,
      assumption,
      previousState,
      assumptionStatusValues,
      isSaving,
      currentLanguage,
      solutions,
      users,
      ...dataUtils,
      v$,
      ...useDateFormat({ entityRef: assumption }),
    };
  },
  created(): void {},
  methods: {
    save(): void {
      this.isSaving = true;
      if (this.assumption.id) {
        this.assumptionService()
          .update(this.assumption)
          .then(param => {
            this.isSaving = false;
            this.previousState();
            this.alertService.showInfo(`A Assumption is updated with identifier ${param.id}`);
          })
          .catch(error => {
            this.isSaving = false;
            this.alertService.showHttpError(error.response);
          });
      } else {
        this.assumptionService()
          .create(this.assumption)
          .then(param => {
            this.isSaving = false;
            this.previousState();
            this.alertService.showSuccess(`A Assumption is created with identifier ${param.id}`);
          })
          .catch(error => {
            this.isSaving = false;
            this.alertService.showHttpError(error.response);
          });
      }
    },
  },
});
