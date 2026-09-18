import { type Ref, defineComponent, inject, onMounted, ref } from 'vue';

import { useAlertService } from '@/shared/alert/alert.service';
import { useDateFormat } from '@/shared/composables';
import useDataUtils from '@/shared/data/data-utils.service';
import { type ITeam } from '@/shared/model/team.model';

import TeamService from './team.service';

export default defineComponent({
  name: 'Team',
  setup() {
    const dateFormat = useDateFormat();
    const dataUtils = useDataUtils();
    const teamService = inject('teamService', () => new TeamService());
    const alertService = inject('alertService', () => useAlertService(), true);

    const teams: Ref<ITeam[]> = ref([]);

    const isFetching = ref(false);

    const clear = () => {};

    const retrieveTeams = async () => {
      isFetching.value = true;
      try {
        const res = await teamService().retrieve();
        teams.value = res.data;
      } catch (err) {
        alertService.showHttpError(err.response);
      } finally {
        isFetching.value = false;
      }
    };

    const handleSyncList = () => {
      retrieveTeams();
    };

    onMounted(async () => {
      await retrieveTeams();
    });

    const removeId: Ref<number> = ref(null);
    const removeEntity = ref<any>(null);
    const prepareRemove = (instance: ITeam) => {
      removeId.value = instance.id;
      removeEntity.value.show();
    };
    const closeDialog = () => {
      removeEntity.value.hide();
    };
    const removeTeam = async () => {
      try {
        await teamService().delete(removeId.value);
        const message = `A Team is deleted with identifier ${removeId.value}`;
        alertService.showInfo(message, { variant: 'danger' });
        removeId.value = null;
        retrieveTeams();
        closeDialog();
      } catch (error) {
        alertService.showHttpError(error.response);
      }
    };

    return {
      teams,
      handleSyncList,
      isFetching,
      retrieveTeams,
      clear,
      ...dateFormat,
      removeId,
      removeEntity,
      prepareRemove,
      closeDialog,
      removeTeam,
      ...dataUtils,
    };
  },
});
