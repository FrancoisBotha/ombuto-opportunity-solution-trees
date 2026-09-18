import { computed, defineComponent, inject, onMounted, ref, watch } from 'vue';
import { useRoute } from 'vue-router';

import { useAlertService } from '@/shared/alert/alert.service';

import type { IMyTeam, IUpdateTeamRequest } from './my-team.model';
import TeamMembers from './team-members.vue';
import TeamsService from './teams.service';
import { useTeamsStore } from './teams.store';

export default defineComponent({
  name: 'TeamDetail',
  components: { TeamMembers },
  setup() {
    const teamsService = inject('teamsService', () => new TeamsService(), true);
    const alertService = inject('alertService', () => useAlertService(), true);
    const teamsStore = useTeamsStore();
    const route = useRoute();

    const teamId = computed(() => {
      const raw = Array.isArray(route.params.id) ? route.params.id[0] : route.params.id;
      const parsed = Number(raw);
      return Number.isFinite(parsed) ? parsed : null;
    });

    const loading = ref(false);
    const editing = ref(false);
    const saving = ref(false);
    const notFound = ref(false);
    const forbidden = ref(false);
    const editName = ref('');
    const editDescription = ref('');
    const editError = ref<string | null>(null);

    const team = computed<IMyTeam | null>(() => teamsStore.teamById(teamId.value));
    const role = computed(() => teamsStore.roleFor(teamId.value));
    const isOwner = computed(() => teamsStore.isOwner(teamId.value));

    const loadTeam = async () => {
      if (teamId.value == null) return;
      loading.value = true;
      notFound.value = false;
      forbidden.value = false;
      try {
        const loaded = await teamsService().getTeam(teamId.value);
        teamsStore.upsertTeam(loaded);
      } catch (err: any) {
        const status = err.response?.status;
        if (status === 404) {
          notFound.value = true;
        } else if (status === 403) {
          forbidden.value = true;
        } else {
          alertService.showHttpError(err.response ?? { status: 0, data: {} });
        }
      } finally {
        loading.value = false;
      }
    };

    const beginEdit = () => {
      if (!team.value || !isOwner.value) return;
      editName.value = team.value.name;
      editDescription.value = team.value.description ?? '';
      editError.value = null;
      editing.value = true;
    };

    const cancelEdit = () => {
      editing.value = false;
      editError.value = null;
    };

    const submitEdit = async () => {
      if (!team.value || teamId.value == null) return;
      const name = editName.value.trim();
      if (!name) {
        editError.value = 'Name is required';
        return;
      }
      const request: IUpdateTeamRequest = {
        name,
        description: editDescription.value.trim() || null,
      };
      saving.value = true;
      editError.value = null;
      try {
        const updated = await teamsService().updateTeam(teamId.value, request);
        teamsStore.upsertTeam(updated);
        alertService.showSuccess('Team updated');
        editing.value = false;
      } catch (err: any) {
        const status = err.response?.status;
        if (status === 400 || status === 422) {
          editError.value = err.response?.data?.detail ?? err.response?.data?.title ?? 'Could not update team';
        } else if (status === 403) {
          editError.value = 'You do not have permission to update this team';
        } else {
          alertService.showHttpError(err.response ?? { status: 0, data: {} });
        }
      } finally {
        saving.value = false;
      }
    };

    onMounted(loadTeam);
    watch(teamId, loadTeam);

    return {
      teamId,
      team,
      role,
      isOwner,
      loading,
      editing,
      saving,
      notFound,
      forbidden,
      editName,
      editDescription,
      editError,
      beginEdit,
      cancelEdit,
      submitEdit,
    };
  },
});
