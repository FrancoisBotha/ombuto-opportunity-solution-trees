import { computed, defineComponent, inject, onMounted, ref } from 'vue';

import { useAlertService } from '@/shared/alert/alert.service';

import type { ICreateTeamRequest } from './my-team.model';
import TeamsService from './teams.service';
import { useTeamsStore } from './teams.store';

export default defineComponent({
  name: 'MyTeams',
  setup() {
    const teamsService = inject('teamsService', () => new TeamsService());
    const alertService = inject('alertService', () => useAlertService(), true);
    const teamsStore = useTeamsStore();

    const isCreating = ref(false);
    const showCreateForm = ref(false);
    const newTeamName = ref('');
    const newTeamDescription = ref('');
    const formError = ref<string | null>(null);

    const teams = computed(() => teamsStore.teams);
    const isLoading = computed(() => teamsStore.loading);
    const hasLoaded = computed(() => teamsStore.loaded);
    const isEmpty = computed(() => teamsStore.loaded && teamsStore.teams.length === 0);

    const loadTeams = async () => {
      teamsStore.setLoading(true);
      try {
        const list = await teamsService().listMyTeams();
        teamsStore.setTeams(list);
      } catch (err: any) {
        teamsStore.setError('Unable to load your teams');
        alertService.showHttpError(err.response ?? { status: 0, data: {} });
      } finally {
        teamsStore.setLoading(false);
      }
    };

    const openCreateForm = () => {
      newTeamName.value = '';
      newTeamDescription.value = '';
      formError.value = null;
      showCreateForm.value = true;
    };

    const cancelCreate = () => {
      showCreateForm.value = false;
      formError.value = null;
    };

    const submitCreate = async () => {
      const name = newTeamName.value.trim();
      if (!name) {
        formError.value = 'Name is required';
        return;
      }
      const request: ICreateTeamRequest = {
        name,
        description: newTeamDescription.value.trim() || null,
      };
      isCreating.value = true;
      formError.value = null;
      try {
        const created = await teamsService().createTeam(request);
        teamsStore.upsertTeam(created);
        alertService.showSuccess(`Team "${created.name}" created`);
        showCreateForm.value = false;
      } catch (err: any) {
        const status = err.response?.status;
        if (status === 400 || status === 422) {
          formError.value = err.response?.data?.detail ?? err.response?.data?.title ?? 'Could not create team';
        } else {
          alertService.showHttpError(err.response ?? { status: 0, data: {} });
        }
      } finally {
        isCreating.value = false;
      }
    };

    onMounted(() => {
      loadTeams();
    });

    return {
      teams,
      isLoading,
      hasLoaded,
      isEmpty,
      isCreating,
      showCreateForm,
      newTeamName,
      newTeamDescription,
      formError,
      loadTeams,
      openCreateForm,
      cancelCreate,
      submitCreate,
    };
  },
});
