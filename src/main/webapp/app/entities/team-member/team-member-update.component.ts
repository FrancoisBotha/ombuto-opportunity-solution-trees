import { type Ref, computed, defineComponent, inject, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { useVuelidate } from '@vuelidate/core';

import TeamService from '@/entities/team/team.service';
import UserService from '@/entities/user/user.service';
import { useAlertService } from '@/shared/alert/alert.service';
import { useDateFormat, useValidation } from '@/shared/composables';
import { TeamRole } from '@/shared/model/enumerations/team-role.model';
import { type ITeamMember, TeamMember } from '@/shared/model/team-member.model';
import { type ITeam } from '@/shared/model/team.model';

import TeamMemberService from './team-member.service';

export default defineComponent({
  name: 'TeamMemberUpdate',
  setup() {
    const teamMemberService = inject('teamMemberService', () => new TeamMemberService());
    const alertService = inject('alertService', () => useAlertService(), true);

    const teamMember: Ref<ITeamMember> = ref(new TeamMember());

    const teamService = inject('teamService', () => new TeamService());

    const teams: Ref<ITeam[]> = ref([]);
    const userService = inject('userService', () => new UserService());
    const users: Ref<Array<any>> = ref([]);
    const teamRoleValues: Ref<string[]> = ref(Object.keys(TeamRole));
    const isSaving = ref(false);
    const currentLanguage = inject('currentLanguage', () => computed(() => navigator.language ?? 'en'), true);

    const route = useRoute();
    const router = useRouter();

    const previousState = () => router.go(-1);

    const retrieveTeamMember = async teamMemberId => {
      try {
        const res = await teamMemberService().find(teamMemberId);
        res.joinedDate = new Date(res.joinedDate);
        teamMember.value = res;
      } catch (error) {
        alertService.showHttpError(error.response);
      }
    };

    if (route.params?.teamMemberId) {
      retrieveTeamMember(route.params.teamMemberId);
    }

    const initRelationships = () => {
      teamService()
        .retrieve()
        .then(res => {
          teams.value = res.data;
        });
      userService()
        .retrieve()
        .then(res => {
          users.value = res.data;
        });
    };

    initRelationships();

    const validations = useValidation();
    const validationRules = {
      role: {
        required: validations.required('This field is required.'),
      },
      joinedDate: {
        required: validations.required('This field is required.'),
      },
      team: {
        required: validations.required('This field is required.'),
      },
      user: {
        required: validations.required('This field is required.'),
      },
    };
    const v$ = useVuelidate(validationRules, teamMember as any);
    v$.value.$validate();

    return {
      teamMemberService,
      alertService,
      teamMember,
      previousState,
      teamRoleValues,
      isSaving,
      currentLanguage,
      teams,
      users,
      v$,
      ...useDateFormat({ entityRef: teamMember }),
    };
  },
  created(): void {},
  methods: {
    save(): void {
      this.isSaving = true;
      if (this.teamMember.id) {
        this.teamMemberService()
          .update(this.teamMember)
          .then(param => {
            this.isSaving = false;
            this.previousState();
            this.alertService.showInfo(`A TeamMember is updated with identifier ${param.id}`);
          })
          .catch(error => {
            this.isSaving = false;
            this.alertService.showHttpError(error.response);
          });
      } else {
        this.teamMemberService()
          .create(this.teamMember)
          .then(param => {
            this.isSaving = false;
            this.previousState();
            this.alertService.showSuccess(`A TeamMember is created with identifier ${param.id}`);
          })
          .catch(error => {
            this.isSaving = false;
            this.alertService.showHttpError(error.response);
          });
      }
    },
  },
});
