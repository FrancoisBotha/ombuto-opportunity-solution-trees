<template>
  <div class="ost-root ost-landing" data-cy="ostTreesLanding">
    <div v-if="empty" class="ost-state" data-cy="ostTreesEmpty">
      <div class="ost-state__title">You are not part of any team yet</div>
      <p class="ost-state__text">Join or create a team to start building an opportunity solution tree.</p>
      <router-link class="ost-btn ost-btn--primary" to="/teams" data-cy="ostTreesEmptyGoToTeams">Go to Teams</router-link>
    </div>
    <div v-else-if="failed" class="ost-state" data-cy="ostTreesError">
      <div class="ost-state__title">Your teams could not be loaded</div>
      <p class="ost-state__text">Check your connection and try again.</p>
      <button type="button" class="ost-btn ost-btn--primary" @click="go">Try again</button>
    </div>
    <div v-else class="ost-landing__loading" data-cy="ostTreesLoading" aria-live="polite">Opening your tree…</div>
  </div>
</template>

<script setup lang="ts">
/**
 * /trees — sends the user to the last team they opened (per login, localStorage) or their first
 * team; shows an empty state when they are in no team.
 */
import { type ComputedRef, inject, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';

import OstService from '../ost.service';
import { readLastTeam } from '../stores/ost-ui.store';
import '../styles/ost-styles';

const router = useRouter();
const ostService = inject('ostService', () => new OstService());
const currentUsername = inject<ComputedRef<string | undefined> | undefined>('currentUsername', undefined);

const empty = ref(false);
const failed = ref(false);

async function go() {
  failed.value = false;
  try {
    const teams = await ostService().listMyTeams();
    if (!teams.length) {
      empty.value = true;
      return;
    }
    const last = readLastTeam(currentUsername?.value);
    const target = teams.find(t => t.id === last) ?? teams[0];
    await router.replace({ name: 'OstDashboard', params: { teamId: String(target.id) } });
  } catch {
    failed.value = true;
  }
}

onMounted(go);
</script>

<style scoped>
.ost-landing {
  overflow: auto;
}

.ost-landing__loading {
  padding: 32px;
  color: var(--color-neutral-400);
}
</style>
