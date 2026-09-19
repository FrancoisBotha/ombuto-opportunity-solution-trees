<template>
  <section class="ost-page" data-cy="ostDashboardPage">
    <div class="ost-page__inner" data-cy="ost-dashboard">
      <header class="ost-page__head">
        <div>
          <div class="ost-page__kicker" data-cy="ostDashboardKicker">{{ teamName }} · continuous discovery</div>
          <h1 class="ost-page__title">Opportunity solution tree</h1>
        </div>
        <div class="ost-page__actions">
          <router-link class="ost-btn ost-btn--primary ost-page__action" :to="canvasRoute()" data-cy="ost-open-canvas">
            Open canvas
          </router-link>
        </div>
      </header>

      <div class="ost-stats" role="list" aria-label="Portfolio counters">
        <div v-for="stat in stats" :key="stat.key" class="ost-stats__tile" role="listitem" :data-cy="`ost-stat-${stat.key}`">
          <div class="ost-stats__value" data-cy="ost-stat-value">{{ stat.value }}</div>
          <div class="ost-stats__label">{{ stat.label }}</div>
        </div>
      </div>

      <h2 class="ost-section-label">Products in this tree</h2>
      <div v-if="cards.length" class="ost-cards">
        <OstProductCard
          v-for="(card, i) in cards"
          :key="card.product.id"
          :card="card"
          :position="i + 1"
          :outcomes="card.outcomes"
          :members="members"
          :to="canvasRoute({ product: card.product.id })"
        />
      </div>
      <div v-else class="ost-state ost-dashboard__empty" data-cy="ost-dashboard-empty">
        <div class="ost-state__title">No products in this tree yet</div>
        <p class="ost-state__text">
          Products are created on the team’s page. Each product becomes a branch of the tree with its own outcomes, opportunities and
          experiments.
        </p>
        <router-link class="ost-btn ost-btn--primary" :to="`/teams/${teamId}`" data-cy="ost-dashboard-go-to-team"
          >Go to the team</router-link
        >
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
/**
 * Trees dashboard (/trees/:teamId): four portfolio counters (A5: the fourth is evidence created
 * this UTC month, derived from the nodes) and one card per product. Read-only — viewers see the same page.
 * Archived products are shown, marked "Archived" and dimmed (the prototype has no archived state).
 */
import { computed } from 'vue';
import { useRoute } from 'vue-router';

import OstProductCard from '../dashboard/OstProductCard.vue';
import { dashboardStats, productCards } from '../domain/derive';
import { useOstTreeStore } from '../stores/ost-tree.store';

const tree = useOstTreeStore();
const route = useRoute();

const teamId = computed(() => String(route.params.teamId ?? tree.team?.id ?? ''));
const teamName = computed(() => tree.team?.name ?? '');
const members = computed(() => tree.team?.members ?? []);

const stats = computed(() => {
  const s = dashboardStats(tree.nodes);
  return [
    { key: 'opportunities', label: 'Opportunities', value: s.opportunities },
    { key: 'solutions', label: 'Solutions live', value: s.solutions },
    { key: 'tests', label: 'Tests running', value: s.testsRunning },
    { key: 'evidence', label: 'Evidence this month', value: s.evidenceThisMonth },
  ];
});

const cards = computed(() => productCards(tree.nodes));

const canvasRoute = (query: Record<string, string> = {}) => ({ name: 'OstCanvas', params: { teamId: teamId.value }, query });
</script>

<style scoped>
/*
 * Responsive to the page's own width (container queries), not the viewport: with the app sidebar
 * expanded a 390px phone leaves the page ~160px wide, collapsed ~330px.
 */
.ost-page {
  height: 100%;
  overflow: auto;
  container: ost-dashboard / inline-size;
}

.ost-page__inner {
  max-width: 1160px;
  margin: 0 auto;
  padding: 34px 40px 60px;
}

.ost-page__head {
  display: flex;
  align-items: flex-end;
  gap: 16px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--color-divider);
}

.ost-page__head > div:first-child {
  min-width: 0;
}

.ost-page__kicker {
  font-family: var(--font-heading);
  font-size: 10px;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--color-accent-300);
  overflow-wrap: anywhere;
}

.ost-root .ost-page__title {
  margin-top: 6px;
  font-size: 38px;
  letter-spacing: -0.01em;
  overflow-wrap: anywhere;
}

.ost-page__actions {
  margin-left: auto;
  display: flex;
  gap: 8px;
}

.ost-root .ost-page__action {
  height: 34px;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.ost-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 1px;
  margin-top: 26px;
  background: var(--color-divider);
  border: 1px solid var(--color-divider);
}

.ost-stats__tile {
  min-width: 0;
  padding: 16px 18px;
  background: var(--color-bg);
}

.ost-stats__value {
  font-family: var(--font-heading);
  font-size: 34px;
  line-height: 1;
}

.ost-stats__label {
  margin-top: 4px;
  font-size: 11px;
  letter-spacing: 0.07em;
  text-transform: uppercase;
  opacity: 0.6;
  overflow-wrap: anywhere;
}

.ost-root .ost-section-label {
  margin: 34px 0 14px;
  font-family: var(--font-heading);
  font-size: 11px;
  font-weight: 400;
  letter-spacing: 0.09em;
  text-transform: uppercase;
  opacity: 0.6;
}

.ost-cards {
  display: grid;
  /* min(): a page narrower than one 280px card gets one full-width card, not an overflowing one. */
  grid-template-columns: repeat(auto-fill, minmax(min(280px, 100%), 1fr));
  gap: 22px;
}

.ost-cards > * {
  min-width: 0;
}

.ost-root .ost-dashboard__empty {
  margin: 0;
}

@container ost-dashboard (max-width: 720px) {
  .ost-page__inner {
    padding: 24px 16px 48px;
  }

  .ost-page__head {
    flex-wrap: wrap;
  }

  .ost-root .ost-page__title {
    font-size: 30px;
  }

  .ost-page__actions {
    margin-left: 0;
  }

  .ost-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .ost-cards {
    gap: 16px;
  }
}

/* Phone with the app sidebar open: one stat per row, smaller type, the action may wrap. */
@container ost-dashboard (max-width: 360px) {
  .ost-page__inner {
    padding: 20px 12px 40px;
  }

  .ost-root .ost-page__title {
    font-size: 24px;
  }

  .ost-root .ost-page__action {
    height: auto;
    min-height: 34px;
    white-space: normal;
    text-align: center;
  }

  .ost-stats {
    grid-template-columns: minmax(0, 1fr);
  }

  .ost-stats__tile {
    display: flex;
    align-items: baseline;
    gap: 10px;
    padding: 10px 12px;
  }

  .ost-stats__value {
    flex: none;
    font-size: 24px;
  }

  .ost-stats__label {
    min-width: 0;
    margin-top: 0;
    font-size: 10px;
    letter-spacing: 0.05em;
  }
}

/* Too narrow for number and label side by side: the label gets the tile's full width. */
@container ost-dashboard (max-width: 220px) {
  .ost-stats__tile {
    display: block;
  }

  .ost-stats__label {
    margin-top: 4px;
  }
}
</style>
