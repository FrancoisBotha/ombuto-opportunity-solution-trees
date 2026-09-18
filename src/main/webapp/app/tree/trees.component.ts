import { computed, defineComponent, inject, onMounted } from 'vue';

import { useAlertService } from '@/shared/alert/alert.service';

import TeamsService from '@/teams/teams.service';
import { useTeamsStore } from '@/teams/teams.store';

export default defineComponent({
  name: 'Trees',
  setup() {
    const teamsService = inject('teamsService', () => new TeamsService(), true);
    const alertService = inject('alertService', () => useAlertService(), true);
    const teamsStore = useTeamsStore();

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

    onMounted(() => {
      loadTeams();
    });

    return {
      teams,
      isLoading,
      hasLoaded,
      isEmpty,
      loadTeams,
    };
  },
});
