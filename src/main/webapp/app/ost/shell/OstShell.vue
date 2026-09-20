<template>
  <div class="ost-root ost-shell" data-cy="ostShell">
    <header class="ost-nav">
      <div class="ost-nav__brand">Ombuto<span class="ost-nav__slash"> / </span>OST</div>
      <nav class="ost-nav__tabs" aria-label="Tree views">
        <router-link
          v-for="tab in TABS"
          :key="tab.name"
          :to="{ name: tab.name, params: { teamId: rawTeamId } }"
          class="ost-nav__tab ost-hit"
          :class="{ 'is-active': route.name === tab.name }"
          :aria-current="route.name === tab.name ? 'page' : undefined"
          :data-cy="tab.cy"
        >
          {{ tab.label }}
        </router-link>
      </nav>
      <div class="ost-nav__right">
        <OstTeamCombo :teams="tree.teams" :current-id="teamId" :current-name="currentTeamName" @select="switchTeam" />
        <div v-if="members.length" class="ost-nav__avatars" data-cy="ostMembers" aria-label="Team members">
          <span
            v-for="member in members"
            :key="member.login"
            class="ost-nav__avatar"
            :title="memberTitle(member)"
            :data-cy="`ostMember-${member.login}`"
            >{{ member.initials }}</span
          >
        </div>
      </div>
    </header>

    <main class="ost-shell__body">
      <div v-if="state === 'notFound'" class="ost-state" data-cy="ostNotFound">
        <div class="ost-state__title">Tree not found</div>
        <p class="ost-state__text">There is no tree at this address. Pick one of your teams from the switcher above.</p>
        <router-link class="ost-btn ost-btn--primary" to="/trees" data-cy="ostBackToTrees">Go to my trees</router-link>
      </div>
      <div v-else-if="state === 'forbidden'" class="ost-state" data-cy="ostForbidden">
        <div class="ost-state__title">You can’t open this tree</div>
        <p class="ost-state__text">It belongs to a team you are not a member of, or it no longer exists. Ask a team owner to add you.</p>
        <router-link class="ost-btn ost-btn--primary" to="/trees" data-cy="ostBackToTrees">Go to my trees</router-link>
      </div>
      <div v-else-if="state === 'error'" class="ost-state" data-cy="ostLoadError">
        <div class="ost-state__title">The tree could not be loaded</div>
        <p class="ost-state__text">Check your connection and try again.</p>
        <button type="button" class="ost-btn ost-btn--primary" data-cy="ostRetry" @click="load">Try again</button>
      </div>
      <div v-else-if="state === 'loading'" class="ost-shell__loading" data-cy="ostLoading" aria-live="polite">Loading tree…</div>
      <router-view v-else></router-view>
    </main>

    <OverlayHost />
  </div>
</template>

<script setup lang="ts">
/**
 * OST shell for /trees/:teamId/*: 54px top nav (brand · view tabs · team combo · member avatars),
 * loads the team's tree whenever :teamId changes, shows not-found / forbidden states (C4) and
 * hosts every overlay. Child routes render the pages.
 */
import { computed, inject, onBeforeUnmount, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import type { TeamMemberDTO } from '../ost.model';
import OstService from '../ost.service';
import OverlayHost from '../overlays/OverlayHost.vue';
import { useOstRealtimeStore } from '../stores/ost-realtime.store';
import { useOstTreeStore } from '../stores/ost-tree.store';
import '../styles/ost-styles';

import OstTeamCombo from './OstTeamCombo.vue';

const TABS = [
  { name: 'OstDashboard', label: 'Trees', cy: 'ostTabTrees' },
  { name: 'OstCanvas', label: 'Tree canvas', cy: 'ostTabCanvas' },
  { name: 'OstExperiments', label: 'Experiments', cy: 'ostTabExperiments' },
] as const;
const VIEW_ROUTES: string[] = TABS.map(t => t.name);

const route = useRoute();
const router = useRouter();
const ostService = inject('ostService', () => new OstService());
const tree = useOstTreeStore();
const realtime = useOstRealtimeStore();
tree.setServiceFactory(ostService);

const rawTeamId = computed(() => String(route.params.teamId ?? ''));
/** Only positive integers are team ids; anything else is a not-found address. */
const teamId = computed(() => (/^\d{1,18}$/.test(rawTeamId.value) && Number(rawTeamId.value) > 0 ? Number(rawTeamId.value) : null));

const state = computed<'notFound' | 'forbidden' | 'error' | 'loading' | 'ready'>(() => {
  if (teamId.value === null) return 'notFound';
  if (tree.teamId === teamId.value && tree.loadError) return tree.loadError;
  if (tree.team?.id === teamId.value && !tree.loading) return 'ready';
  return 'loading';
});

const currentTeamName = computed(() => (tree.team?.id === teamId.value ? tree.team?.name : null));
const members = computed<TeamMemberDTO[]>(() => (tree.team?.id === teamId.value ? tree.team.members : []));

const ROLE_LABEL: Record<string, string> = { OWNER: 'owner', EDITOR: 'editor', VIEWER: 'viewer' };
const memberTitle = (m: TeamMemberDTO) => {
  const name = [m.firstName, m.lastName].filter(Boolean).join(' ') || m.login;
  return `${name} (${ROLE_LABEL[m.role] ?? m.role})`;
};

async function load() {
  const id = teamId.value;
  await realtime.closeTeam();
  // `closeTeam` awaits the socket actually closing, which can take a while; a second switch made
  // meanwhile has already started loading ITS team. Without this guard the older call would run
  // `loadTree` last, win the store's loadSeq race, and leave the shell showing "Loading tree…"
  // for a team whose tree is never fetched.
  if (teamId.value !== id) return;
  if (id === null) return;
  const loaded = await tree.loadTree(id);
  if (!loaded || teamId.value !== id) return;
  realtime.openTeam(id, {
    applyEvents: events => tree.applyEvents(events),
    // A resync of the team already on screen must not blank the page (FR-C2: Fit runs on first
    // mount and on product switch, not on every reconnect).
    reloadTree: () => tree.loadTree(id, { background: true }),
  });
}

function switchTeam(id: number) {
  const name = typeof route.name === 'string' && VIEW_ROUTES.includes(route.name) ? route.name : 'OstDashboard';
  router.push({ name, params: { teamId: String(id) } });
}

watch(teamId, load, { immediate: true });

onMounted(() => {
  tree.loadTeams();
});

onBeforeUnmount(() => {
  void realtime.closeTeam();
});
</script>

<style scoped>
.ost-shell {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  /* clip, not hidden: a hidden-overflow box can still be scrolled by focus/scrollIntoView,
     which would push the 54px nav off screen. */
  overflow: clip;
}

.ost-nav {
  display: flex;
  align-items: center;
  gap: 22px;
  padding: 0 18px;
  height: 54px;
  flex: none;
  border-bottom: 1px solid var(--color-divider);
}

.ost-nav__brand {
  font-family: var(--font-heading);
  font-weight: 600;
  font-size: 18px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  white-space: nowrap;
}

.ost-nav__slash {
  color: var(--color-accent);
}

.ost-nav__tabs {
  display: flex;
  gap: 4px;
  align-self: stretch;
  align-items: center;
}

.ost-root .ost-nav__tab {
  position: relative;
  font-family: var(--font-heading);
  font-size: 13px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  padding: 6px 10px;
  color: inherit;
  white-space: nowrap;
  border-radius: var(--radius-sm);
}

.ost-root .ost-nav__tab:hover {
  color: inherit;
  background: color-mix(in srgb, var(--color-text) 7%, transparent);
}

.ost-nav__tab.is-active::after {
  content: '';
  position: absolute;
  left: 8px;
  right: 8px;
  bottom: -1px;
  height: 2px;
  background: var(--color-accent);
}

.ost-nav__right {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 12px;
}

.ost-nav__avatars {
  display: flex;
}

.ost-nav__avatar {
  width: 26px;
  height: 26px;
  border: 1px solid var(--color-neutral-800);
  border-radius: var(--radius-sm);
  display: grid;
  place-items: center;
  font-size: 10px;
  letter-spacing: 0.06em;
  margin-left: -1px;
  background: var(--color-surface);
}

.ost-shell__body {
  flex: 1;
  min-height: 0;
  position: relative;
  overflow: clip;
}

.ost-shell__loading {
  padding: 32px;
  color: var(--color-neutral-400);
}
</style>
