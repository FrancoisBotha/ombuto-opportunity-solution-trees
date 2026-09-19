<template>
  <section class="ost-page" data-cy="ostExperimentsPage">
    <div class="ost-page__inner" data-cy="ost-experiments">
      <header class="ost-page__head">
        <div>
          <div class="ost-page__kicker">Assumption tests in flight</div>
          <h1 class="ost-page__title">Experiment tracker</h1>
        </div>
        <div class="ost-page__summary" data-cy="ost-experiments-summary">{{ summaryText }}</div>
      </header>

      <table v-if="rows.length" class="ost-table">
        <thead>
          <tr>
            <th scope="col">Assumption</th>
            <th scope="col">Tests solution</th>
            <th scope="col">Owner</th>
            <th scope="col">Status</th>
            <th scope="col">Confidence</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="row in rows"
            :key="row.key"
            class="ost-table__row"
            tabindex="0"
            :aria-label="`Open “${row.statement}” on the canvas`"
            :data-cy="`ost-experiment-row-${row.key}`"
            @click="open(row)"
            @keydown.enter.prevent="open(row)"
            @keydown.space.prevent="open(row)"
          >
            <td class="ost-table__title">{{ row.statement }}</td>
            <td class="ost-table__muted ost-table__title">{{ row.solutionTitle ?? '—' }}</td>
            <td class="ost-table__muted" data-cy="ost-experiment-owner">{{ ownerLabel(row.owner) }}</td>
            <td>
              <span class="ost-badge" :class="`ost-badge--${statusTone(row.status)}`" data-cy="ost-experiment-status">{{
                row.status
              }}</span>
            </td>
            <td class="ost-table__muted" data-cy="ost-experiment-confidence">{{ row.confidence }}%</td>
          </tr>
        </tbody>
      </table>
      <div v-else class="ost-state ost-experiments__empty" data-cy="ost-experiments-empty">
        <div class="ost-state__title">No assumptions to track yet</div>
        <p class="ost-state__text">
          Add assumptions under a solution on the canvas — each one becomes a row here with its owner, status and confidence.
        </p>
        <router-link class="ost-btn ost-btn--primary" :to="{ name: 'OstCanvas', params: { teamId } }" data-cy="ost-experiments-open-canvas">
          Open canvas
        </router-link>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
/**
 * Experiments tracker (/trees/:teamId/experiments): every assumption across the tree in tree order —
 * assumption · the solution it tests · owner · status · confidence. A row (click, Enter or Space)
 * opens that node on the canvas, scoped to its product. Read-only; no sorting/filtering (the
 * prototype has none).
 */
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { personLabel } from '../dashboard/format';
import { type ExperimentRow, experimentRows, experimentSummary, statusTone } from '../domain/derive';
import { useOstTreeStore } from '../stores/ost-tree.store';

const tree = useOstTreeStore();
const route = useRoute();
const router = useRouter();

const teamId = computed(() => String(route.params.teamId ?? tree.team?.id ?? ''));
const rows = computed(() => experimentRows(tree.nodes));

/** "2 running · 2 queued · 2 settled", as in the prototype. */
const summaryText = computed(() => {
  const s = experimentSummary(rows.value);
  return `${s.testing} running · ${s.untested} queued · ${s.supported + s.refuted} settled`;
});

const ownerLabel = (login: string) => personLabel(login, tree.team?.members ?? []) || 'Unassigned';

function open(row: ExperimentRow) {
  const query: Record<string, string> = { node: row.key };
  if (row.productKey) query.product = row.productKey;
  router.push({ name: 'OstCanvas', params: { teamId: teamId.value }, query });
}
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
  font-size: 34px;
}

.ost-page__summary {
  margin-left: auto;
  font-size: 12px;
  opacity: 0.6;
  text-align: right;
}

.ost-table {
  width: 100%;
  margin-top: 22px;
  border-collapse: collapse;
  font-size: 13px;
}

.ost-table th {
  text-align: left;
  padding: 9px 10px;
  font-family: var(--font-heading);
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: color-mix(in srgb, var(--color-text) 60%, transparent);
  border-bottom: 1px solid var(--color-divider);
}

.ost-table td {
  padding: 11px 10px;
  border-bottom: 1px solid var(--color-divider);
  vertical-align: middle;
}

.ost-table__row {
  cursor: pointer;
}

.ost-table__row:hover {
  background: var(--color-accent-900);
}

.ost-root .ost-table__row:focus-visible {
  outline-offset: -2px;
  background: var(--color-accent-900);
}

.ost-table__title {
  text-wrap: pretty;
  overflow-wrap: anywhere;
}

.ost-table__muted {
  opacity: 0.75;
}

.ost-root .ost-experiments__empty {
  margin: 22px 0 0;
}

@media (max-width: 720px) {
  .ost-page {
    padding: 24px 16px 48px;
  }

  .ost-page__head {
    flex-wrap: wrap;
  }

  .ost-page__summary {
    margin-left: 0;
    text-align: left;
  }
}
</style>
