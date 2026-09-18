import { type Ref, computed, defineComponent, inject, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { useVuelidate } from '@vuelidate/core';

import AssumptionService from '@/entities/assumption/assumption.service';
import SolutionService from '@/entities/solution/solution.service';
import { useAlertService } from '@/shared/alert/alert.service';
import { useDateFormat, useValidation } from '@/shared/composables';
import useDataUtils from '@/shared/data/data-utils.service';
import { type IAssumption } from '@/shared/model/assumption.model';
import { ExperimentResult } from '@/shared/model/enumerations/experiment-result.model';
import { ExperimentStatus } from '@/shared/model/enumerations/experiment-status.model';
import { Experiment, type IExperiment } from '@/shared/model/experiment.model';
import { type ISolution } from '@/shared/model/solution.model';

import ExperimentService from './experiment.service';

export default defineComponent({
  name: 'ExperimentUpdate',
  setup() {
    const experimentService = inject('experimentService', () => new ExperimentService());
    const alertService = inject('alertService', () => useAlertService(), true);

    const experiment: Ref<IExperiment> = ref(new Experiment());

    const solutionService = inject('solutionService', () => new SolutionService());

    const solutions: Ref<ISolution[]> = ref([]);

    const assumptionService = inject('assumptionService', () => new AssumptionService());

    const assumptions: Ref<IAssumption[]> = ref([]);
    const experimentStatusValues: Ref<string[]> = ref(Object.keys(ExperimentStatus));
    const experimentResultValues: Ref<string[]> = ref(Object.keys(ExperimentResult));
    const isSaving = ref(false);
    const currentLanguage = inject('currentLanguage', () => computed(() => navigator.language ?? 'en'), true);

    const route = useRoute();
    const router = useRouter();

    const previousState = () => router.go(-1);

    const retrieveExperiment = async experimentId => {
      try {
        const res = await experimentService().find(experimentId);
        res.createdDate = new Date(res.createdDate);
        experiment.value = res;
      } catch (error) {
        alertService.showHttpError(error.response);
      }
    };

    if (route.params?.experimentId) {
      retrieveExperiment(route.params.experimentId);
    }

    const initRelationships = () => {
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
      hypothesis: {},
      method: {
        maxLength: validations.maxLength('This field cannot be longer than 200 characters.', 200),
      },
      successCriteria: {},
      status: {
        required: validations.required('This field is required.'),
      },
      result: {},
      learnings: {},
      startDate: {},
      endDate: {},
      createdDate: {
        required: validations.required('This field is required.'),
      },
      solution: {
        required: validations.required('This field is required.'),
      },
      assumptions: {},
    };
    const v$ = useVuelidate(validationRules, experiment as any);
    v$.value.$validate();

    return {
      experimentService,
      alertService,
      experiment,
      previousState,
      experimentStatusValues,
      experimentResultValues,
      isSaving,
      currentLanguage,
      solutions,
      assumptions,
      ...dataUtils,
      v$,
      ...useDateFormat({ entityRef: experiment }),
    };
  },
  created(): void {
    this.experiment.assumptions = [];
  },
  methods: {
    save(): void {
      this.isSaving = true;
      if (this.experiment.id) {
        this.experimentService()
          .update(this.experiment)
          .then(param => {
            this.isSaving = false;
            this.previousState();
            this.alertService.showInfo(`A Experiment is updated with identifier ${param.id}`);
          })
          .catch(error => {
            this.isSaving = false;
            this.alertService.showHttpError(error.response);
          });
      } else {
        this.experimentService()
          .create(this.experiment)
          .then(param => {
            this.isSaving = false;
            this.previousState();
            this.alertService.showSuccess(`A Experiment is created with identifier ${param.id}`);
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
