import { type Ref, computed, defineComponent, inject, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { useVuelidate } from '@vuelidate/core';

import OpportunityService from '@/entities/opportunity/opportunity.service';
import TagService from '@/entities/tag/tag.service';
import UserService from '@/entities/user/user.service';
import { useAlertService } from '@/shared/alert/alert.service';
import { useDateFormat, useValidation } from '@/shared/composables';
import useDataUtils from '@/shared/data/data-utils.service';
import { SolutionStatus } from '@/shared/model/enumerations/solution-status.model';
import { type IOpportunity } from '@/shared/model/opportunity.model';
import { type ISolution, Solution } from '@/shared/model/solution.model';
import { type ITag } from '@/shared/model/tag.model';

import SolutionService from './solution.service';

export default defineComponent({
  name: 'SolutionUpdate',
  setup() {
    const solutionService = inject('solutionService', () => new SolutionService());
    const alertService = inject('alertService', () => useAlertService(), true);

    const solution: Ref<ISolution> = ref(new Solution());

    const opportunityService = inject('opportunityService', () => new OpportunityService());

    const opportunities: Ref<IOpportunity[]> = ref([]);
    const userService = inject('userService', () => new UserService());
    const users: Ref<Array<any>> = ref([]);

    const tagService = inject('tagService', () => new TagService());

    const tags: Ref<ITag[]> = ref([]);
    const solutionStatusValues: Ref<string[]> = ref(Object.keys(SolutionStatus));
    const isSaving = ref(false);
    const currentLanguage = inject('currentLanguage', () => computed(() => navigator.language ?? 'en'), true);

    const route = useRoute();
    const router = useRouter();

    const previousState = () => router.go(-1);

    const retrieveSolution = async solutionId => {
      try {
        const res = await solutionService().find(solutionId);
        res.createdDate = new Date(res.createdDate);
        res.lastModifiedDate = new Date(res.lastModifiedDate);
        solution.value = res;
      } catch (error) {
        alertService.showHttpError(error.response);
      }
    };

    if (route.params?.solutionId) {
      retrieveSolution(route.params.solutionId);
    }

    const initRelationships = () => {
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
      sortOrder: {
        required: validations.required('This field is required.'),
        integer: validations.integer('This field should be a number.'),
      },
      createdDate: {
        required: validations.required('This field is required.'),
      },
      lastModifiedDate: {},
      opportunity: {
        required: validations.required('This field is required.'),
      },
      owner: {},
      tags: {},
    };
    const v$ = useVuelidate(validationRules, solution as any);
    v$.value.$validate();

    return {
      solutionService,
      alertService,
      solution,
      previousState,
      solutionStatusValues,
      isSaving,
      currentLanguage,
      opportunities,
      users,
      tags,
      ...dataUtils,
      v$,
      ...useDateFormat({ entityRef: solution }),
    };
  },
  created(): void {
    this.solution.tags = [];
  },
  methods: {
    save(): void {
      this.isSaving = true;
      if (this.solution.id) {
        this.solutionService()
          .update(this.solution)
          .then(param => {
            this.isSaving = false;
            this.previousState();
            this.alertService.showInfo(`A Solution is updated with identifier ${param.id}`);
          })
          .catch(error => {
            this.isSaving = false;
            this.alertService.showHttpError(error.response);
          });
      } else {
        this.solutionService()
          .create(this.solution)
          .then(param => {
            this.isSaving = false;
            this.previousState();
            this.alertService.showSuccess(`A Solution is created with identifier ${param.id}`);
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
