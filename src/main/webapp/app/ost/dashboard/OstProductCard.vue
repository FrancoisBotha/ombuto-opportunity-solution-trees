<template>
  <article class="ost-product-card" :class="{ 'is-archived': card.product.archived }" :data-cy="`ost-product-card-${card.product.id}`">
    <div class="ost-product-card__kicker">
      <span>{{ kicker }}</span>
      <span v-if="card.product.archived" class="ost-badge ost-badge--bad" data-cy="ost-product-archived">Archived</span>
    </div>
    <h2 class="ost-product-card__name">{{ card.product.title }}</h2>
    <div class="ost-product-card__outcomes">
      <p v-for="outcome in outcomes" :key="outcome" class="ost-product-card__outcome">{{ outcome }}</p>
      <p v-if="!outcomes.length" class="ost-product-card__outcome is-empty">No outcome set</p>
    </div>
    <div class="ost-product-card__counts">
      <div v-for="count in counts" :key="count.label" class="ost-product-card__count" :data-cy="`ost-product-count-${count.cy}`">
        <div class="ost-product-card__count-n">{{ count.n }}</div>
        <div class="ost-product-card__count-k">{{ count.label }}</div>
      </div>
    </div>
    <div class="ost-product-card__meta" data-cy="ost-product-updated">{{ updated }}</div>
    <router-link class="ost-btn ost-btn--primary ost-product-card__open" :to="to" :data-cy="`ost-open-branch-${card.product.id}`">
      Open branch
    </router-link>
  </article>
</template>

<script setup lang="ts">
/** One product on the Trees dashboard: kicker, name, outcome(s), four mini counts, last edit, "Open branch". */
import { computed } from 'vue';
import type { RouteLocationRaw } from 'vue-router';

import type { ProductCard } from '../domain/derive';
import type { TeamMemberDTO } from '../ost.model';

import { lastEditedLabel } from './format';

const props = defineProps<{
  card: ProductCard;
  /** 1-based position among the team's products ("Product branch 01"). */
  position: number;
  outcomes: string[];
  members: TeamMemberDTO[];
  to: RouteLocationRaw;
}>();

const kicker = computed(() => `Product branch ${String(props.position).padStart(2, '0')}`);

const counts = computed(() => [
  { label: 'Opps', cy: 'opportunities', n: props.card.counts.opportunities },
  { label: 'Sols', cy: 'solutions', n: props.card.counts.solutions },
  { label: 'Tests', cy: 'assumptions', n: props.card.counts.assumptions },
  { label: 'Evid', cy: 'evidence', n: props.card.counts.evidence },
]);

const updated = computed(() => lastEditedLabel(props.card.lastActivity, props.card.product.lastActivity?.byLogin ?? null, props.members));
</script>

<style scoped>
.ost-product-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 18px;
  border-radius: var(--radius-md);
  background: var(--color-surface);
}

.ost-product-card.is-archived {
  opacity: 0.7;
}

.ost-product-card__kicker {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 10px;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--color-accent);
}

.ost-root .ost-product-card__name {
  font-family: var(--font-heading);
  font-weight: 500;
  font-size: 21px;
  line-height: 1.2;
  overflow-wrap: anywhere;
}

.ost-product-card__outcomes {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.ost-product-card__outcome {
  margin: 0;
  font-size: 13px;
  opacity: 0.8;
  overflow-wrap: anywhere;
}

.ost-product-card__outcome.is-empty {
  opacity: 0.55;
}

.ost-product-card__counts {
  display: flex;
  gap: 1px;
  margin-top: 4px;
  background: var(--color-divider);
  border: 1px solid var(--color-divider);
}

.ost-product-card__count {
  flex: 1;
  min-width: 0;
  padding: 7px 8px;
  background: var(--color-bg);
}

.ost-product-card__count-n {
  font-family: var(--font-heading);
  font-size: 18px;
  line-height: 1;
}

.ost-product-card__count-k {
  font-size: 9px;
  letter-spacing: 0.07em;
  text-transform: uppercase;
  opacity: 0.55;
}

.ost-product-card__meta {
  margin-top: 6px;
  font-size: 11px;
  color: color-mix(in srgb, var(--color-text) 50%, transparent);
}

.ost-root .ost-product-card__open {
  width: 100%;
  margin-top: var(--space-2);
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}
</style>
