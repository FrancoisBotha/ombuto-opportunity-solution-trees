import { type Ref, computed, defineComponent, inject, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { useVuelidate } from '@vuelidate/core';

import OpportunityService from '@/entities/opportunity/opportunity.service';
import SolutionService from '@/entities/solution/solution.service';
import TeamService from '@/entities/team/team.service';
import { useAlertService } from '@/shared/alert/alert.service';
import { useValidation } from '@/shared/composables';
import { type IOpportunity } from '@/shared/model/opportunity.model';
import { type ISolution } from '@/shared/model/solution.model';
import { type ITag, Tag } from '@/shared/model/tag.model';
import { type ITeam } from '@/shared/model/team.model';

import TagService from './tag.service';

export default defineComponent({
  name: 'TagUpdate',
  setup() {
    const tagService = inject('tagService', () => new TagService());
    const alertService = inject('alertService', () => useAlertService(), true);

    const tag: Ref<ITag> = ref(new Tag());

    const teamService = inject('teamService', () => new TeamService());

    const teams: Ref<ITeam[]> = ref([]);

    const opportunityService = inject('opportunityService', () => new OpportunityService());

    const opportunities: Ref<IOpportunity[]> = ref([]);

    const solutionService = inject('solutionService', () => new SolutionService());

    const solutions: Ref<ISolution[]> = ref([]);
    const isSaving = ref(false);
    const currentLanguage = inject('currentLanguage', () => computed(() => navigator.language ?? 'en'), true);

    const route = useRoute();
    const router = useRouter();

    const previousState = () => router.go(-1);

    const retrieveTag = async tagId => {
      try {
        const res = await tagService().find(tagId);
        tag.value = res;
      } catch (error) {
        alertService.showHttpError(error.response);
      }
    };

    if (route.params?.tagId) {
      retrieveTag(route.params.tagId);
    }

    const initRelationships = () => {
      teamService()
        .retrieve()
        .then(res => {
          teams.value = res.data;
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
    };

    initRelationships();

    const validations = useValidation();
    const validationRules = {
      name: {
        required: validations.required('This field is required.'),
        minLength: validations.minLength('This field is required to be at least 1 characters.', 1),
        maxLength: validations.maxLength('This field cannot be longer than 50 characters.', 50),
      },
      colour: {
        maxLength: validations.maxLength('This field cannot be longer than 7 characters.', 7),
      },
      team: {
        required: validations.required('This field is required.'),
      },
      opportunities: {},
      solutions: {},
    };
    const v$ = useVuelidate(validationRules, tag as any);
    v$.value.$validate();

    return {
      tagService,
      alertService,
      tag,
      previousState,
      isSaving,
      currentLanguage,
      teams,
      opportunities,
      solutions,
      v$,
    };
  },
  created(): void {
    this.tag.opportunities = [];
    this.tag.solutions = [];
  },
  methods: {
    save(): void {
      this.isSaving = true;
      if (this.tag.id) {
        this.tagService()
          .update(this.tag)
          .then(param => {
            this.isSaving = false;
            this.previousState();
            this.alertService.showInfo(`A Tag is updated with identifier ${param.id}`);
          })
          .catch(error => {
            this.isSaving = false;
            this.alertService.showHttpError(error.response);
          });
      } else {
        this.tagService()
          .create(this.tag)
          .then(param => {
            this.isSaving = false;
            this.previousState();
            this.alertService.showSuccess(`A Tag is created with identifier ${param.id}`);
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
