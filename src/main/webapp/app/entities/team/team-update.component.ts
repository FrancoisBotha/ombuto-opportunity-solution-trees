import { type Ref, computed, defineComponent, inject, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { useVuelidate } from '@vuelidate/core';

import { useAlertService } from '@/shared/alert/alert.service';
import { useDateFormat, useValidation } from '@/shared/composables';
import useDataUtils from '@/shared/data/data-utils.service';
import { type ITeam, Team } from '@/shared/model/team.model';

import TeamService from './team.service';

export default defineComponent({
  name: 'TeamUpdate',
  setup() {
    const teamService = inject('teamService', () => new TeamService());
    const alertService = inject('alertService', () => useAlertService(), true);

    const team: Ref<ITeam> = ref(new Team());
    const isSaving = ref(false);
    const currentLanguage = inject('currentLanguage', () => computed(() => navigator.language ?? 'en'), true);

    const route = useRoute();
    const router = useRouter();

    const previousState = () => router.go(-1);

    const retrieveTeam = async teamId => {
      try {
        const res = await teamService().find(teamId);
        res.createdDate = new Date(res.createdDate);
        team.value = res;
      } catch (error) {
        alertService.showHttpError(error.response);
      }
    };

    if (route.params?.teamId) {
      retrieveTeam(route.params.teamId);
    }

    const dataUtils = useDataUtils();

    const validations = useValidation();
    const validationRules = {
      name: {
        required: validations.required('This field is required.'),
        minLength: validations.minLength('This field is required to be at least 2 characters.', 2),
        maxLength: validations.maxLength('This field cannot be longer than 100 characters.', 100),
      },
      description: {},
      createdDate: {
        required: validations.required('This field is required.'),
      },
    };
    const v$ = useVuelidate(validationRules, team as any);
    v$.value.$validate();

    return {
      teamService,
      alertService,
      team,
      previousState,
      isSaving,
      currentLanguage,
      ...dataUtils,
      v$,
      ...useDateFormat({ entityRef: team }),
    };
  },
  created(): void {},
  methods: {
    save(): void {
      this.isSaving = true;
      if (this.team.id) {
        this.teamService()
          .update(this.team)
          .then(param => {
            this.isSaving = false;
            this.previousState();
            this.alertService.showInfo(`A Team is updated with identifier ${param.id}`);
          })
          .catch(error => {
            this.isSaving = false;
            this.alertService.showHttpError(error.response);
          });
      } else {
        this.teamService()
          .create(this.team)
          .then(param => {
            this.isSaving = false;
            this.previousState();
            this.alertService.showSuccess(`A Team is created with identifier ${param.id}`);
          })
          .catch(error => {
            this.isSaving = false;
            this.alertService.showHttpError(error.response);
          });
      }
    },
  },
});
