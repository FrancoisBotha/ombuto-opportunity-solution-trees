import { type Ref, defineComponent, inject, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { useAlertService } from '@/shared/alert/alert.service';
import { useDateFormat } from '@/shared/composables';
import useDataUtils from '@/shared/data/data-utils.service';
import { type ITeam } from '@/shared/model/team.model';

import TeamService from './team.service';

export default defineComponent({
  name: 'TeamDetails',
  setup() {
    const dateFormat = useDateFormat();
    const teamService = inject('teamService', () => new TeamService());
    const alertService = inject('alertService', () => useAlertService(), true);

    const dataUtils = useDataUtils();

    const route = useRoute();
    const router = useRouter();

    const previousState = () => router.go(-1);
    const team: Ref<ITeam> = ref({});

    const retrieveTeam = async teamId => {
      try {
        const res = await teamService().find(teamId);
        team.value = res;
      } catch (error) {
        alertService.showHttpError(error.response);
      }
    };

    if (route.params?.teamId) {
      retrieveTeam(route.params.teamId);
    }

    return {
      ...dateFormat,
      alertService,
      team,

      ...dataUtils,

      previousState,
    };
  },
});
