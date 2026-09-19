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
          :outcomes="outcomesOf(card.product.id)"
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
 * this month, from the tree read) and one card per product. Read-only — viewers see the same page.
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
  const s = dashboardStats(tree.nodes, { evidenceThisMonth: tree.team?.evidenceThisMonth });
  return [
    { key: 'opportunities', label: 'Opportunities', value: s.opportunities },
    { key: 'solutions', label: 'Solutions live', value: s.solutions },
    { key: 'tests', label: 'Tests running', value: s.testsRunning },
    { key: 'evidence', label: 'Evidence this month', value: s.evidenceThisMonth },
  ];
});

const cards = computed(() => productCards(tree.nodes));

/** Every outcome of the product, in tree order. */
const outcomesOf = (productKey: string) => tree.nodes.filter(n => n.parent === productKey && n.type === 'outcome').map(n => n.title);

const canvasRoute = (query: Record<string, string> = {}) => ({ name: 'OstCanvas', params: { teamId: teamId.value }, query });
</script>

<style scoped>
.ost-page {
  height: 100%;
  overflow: auto;
  padding: 34px 40px 60px;
}

.ost-page__inner {
  max-width: 1080px;
  margin: 0 auto;
}

.ost-page__head {
  display: flex;
  align-items: flex-end;
  gap: 16px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--color-divider);
}

.ost-page__kicker {
  font-family: var(--font-heading);
  font-size: 10px;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--color-accent-300);
}

.ost-root .ost-page__title {
  margin-top: 6px;
  font-size: 38px;
  letter-spacing: -0.01em;
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
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 22px;
}

.ost-root .ost-dashboard__empty {
  margin: 0;
}

@media (max-width: 720px) {
  .ost-page {
    padding: 24px 16px 48px;
  }

  .ost-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
