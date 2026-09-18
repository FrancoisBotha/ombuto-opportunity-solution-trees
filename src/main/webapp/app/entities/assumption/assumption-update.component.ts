import { type Ref, computed, defineComponent, inject, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { useVuelidate } from '@vuelidate/core';

import ExperimentService from '@/entities/experiment/experiment.service';
import SolutionService from '@/entities/solution/solution.service';
import { useAlertService } from '@/shared/alert/alert.service';
import { useDateFormat, useValidation } from '@/shared/composables';
import { Assumption, type IAssumption } from '@/shared/model/assumption.model';
import { AssumptionCategory } from '@/shared/model/enumerations/assumption-category.model';
import { type IExperiment } from '@/shared/model/experiment.model';
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

    const experimentService = inject('experimentService', () => new ExperimentService());

    const experiments: Ref<IExperiment[]> = ref([]);
    const assumptionCategoryValues: Ref<string[]> = ref(Object.keys(AssumptionCategory));
    const isSaving = ref(false);
    const currentLanguage = inject('currentLanguage', () => computed(() => navigator.language ?? 'en'), true);

    const route = useRoute();
    const router = useRouter();

    const previousState = () => router.go(-1);

    const retrieveAssumption = async assumptionId => {
      try {
        const res = await assumptionService().find(assumptionId);
        res.createdDate = new Date(res.createdDate);
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
      experimentService()
        .retrieve()
        .then(res => {
          experiments.value = res.data;
        });
    };

    initRelationships();

    const validations = useValidation();
    const validationRules = {
      statement: {
        required: validations.required('This field is required.'),
        minLength: validations.minLength('This field is required to be at least 2 characters.', 2),
        maxLength: validations.maxLength('This field cannot be longer than 500 characters.', 500),
      },
      category: {
        required: validations.required('This field is required.'),
      },
      importance: {
        required: validations.required('This field is required.'),
        integer: validations.integer('This field should be a number.'),
        min: validations.minValue('This field should be at least 1.', 1),
        max: validations.maxValue('This field cannot be more than 5.', 5),
      },
      evidence: {
        required: validations.required('This field is required.'),
        integer: validations.integer('This field should be a number.'),
        min: validations.minValue('This field should be at least 1.', 1),
        max: validations.maxValue('This field cannot be more than 5.', 5),
      },
      validated: {},
      createdDate: {
        required: validations.required('This field is required.'),
      },
      solution: {
        required: validations.required('This field is required.'),
      },
      experiments: {},
    };
    const v$ = useVuelidate(validationRules, assumption as any);
    v$.value.$validate();

    return {
      assumptionService,
      alertService,
      assumption,
      previousState,
      assumptionCategoryValues,
      isSaving,
      currentLanguage,
      solutions,
      experiments,
      v$,
      ...useDateFormat({ entityRef: assumption }),
    };
  },
  created(): void {
    this.assumption.experiments = [];
  },
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

    getSelected(selectedVals, option, pkField = 'id'): any {
      if (selectedVals) {
        return selectedVals.find(value => option[pkField] === value[pkField]) ?? option;
      }
      return option;
    },
  },
});
