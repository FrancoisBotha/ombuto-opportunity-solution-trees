import { type Ref, defineComponent, inject, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { useAlertService } from '@/shared/alert/alert.service';
import { useDateFormat } from '@/shared/composables';
import { type ITeamMember } from '@/shared/model/team-member.model';

import TeamMemberService from './team-member.service';

export default defineComponent({
  name: 'TeamMemberDetails',
  setup() {
    const dateFormat = useDateFormat();
    const teamMemberService = inject('teamMemberService', () => new TeamMemberService());
    const alertService = inject('alertService', () => useAlertService(), true);

    const route = useRoute();
    const router = useRouter();

    const previousState = () => router.go(-1);
    const teamMember: Ref<ITeamMember> = ref({});

    const retrieveTeamMember = async teamMemberId => {
      try {
        const res = await teamMemberService().find(teamMemberId);
        teamMember.value = res;
      } catch (error) {
        alertService.showHttpError(error.response);
      }
    };

    if (route.params?.teamMemberId) {
      retrieveTeamMember(route.params.teamMemberId);
    }

    return {
      ...dateFormat,
      alertService,
      teamMember,

      previousState,
    };
  },
});
