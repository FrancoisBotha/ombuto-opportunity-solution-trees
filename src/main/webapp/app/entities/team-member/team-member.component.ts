import { type Ref, defineComponent, inject, onMounted, ref } from 'vue';

import { useAlertService } from '@/shared/alert/alert.service';
import { useDateFormat } from '@/shared/composables';
import { type ITeamMember } from '@/shared/model/team-member.model';

import TeamMemberService from './team-member.service';

export default defineComponent({
  name: 'TeamMember',
  setup() {
    const dateFormat = useDateFormat();
    const teamMemberService = inject('teamMemberService', () => new TeamMemberService());
    const alertService = inject('alertService', () => useAlertService(), true);

    const teamMembers: Ref<ITeamMember[]> = ref([]);

    const isFetching = ref(false);

    const clear = () => {};

    const retrieveTeamMembers = async () => {
      isFetching.value = true;
      try {
        const res = await teamMemberService().retrieve();
        teamMembers.value = res.data;
      } catch (err) {
        alertService.showHttpError(err.response);
      } finally {
        isFetching.value = false;
      }
    };

    const handleSyncList = () => {
      retrieveTeamMembers();
    };

    onMounted(async () => {
      await retrieveTeamMembers();
    });

    const removeId: Ref<number> = ref(null);
    const removeEntity = ref<any>(null);
    const prepareRemove = (instance: ITeamMember) => {
      removeId.value = instance.id;
      removeEntity.value.show();
    };
    const closeDialog = () => {
      removeEntity.value.hide();
    };
    const removeTeamMember = async () => {
      try {
        await teamMemberService().delete(removeId.value);
        const message = `A TeamMember is deleted with identifier ${removeId.value}`;
        alertService.showInfo(message, { variant: 'danger' });
        removeId.value = null;
        retrieveTeamMembers();
        closeDialog();
      } catch (error) {
        alertService.showHttpError(error.response);
      }
    };

    return {
      teamMembers,
      handleSyncList,
      isFetching,
      retrieveTeamMembers,
      clear,
      ...dateFormat,
      removeId,
      removeEntity,
      prepareRemove,
      closeDialog,
      removeTeamMember,
    };
  },
});
