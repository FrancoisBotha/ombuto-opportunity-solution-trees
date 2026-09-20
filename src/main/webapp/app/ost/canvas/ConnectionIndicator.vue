<template>
  <div
    class="ost-connection"
    :class="`is-${state}`"
    :data-state="state"
    data-cy="ost-connection"
    role="status"
    :aria-label="`Connection: ${LABEL[state]}`"
    :title="HINT[state]"
  >
    <span class="ost-connection__dot" :style="{ background: DOT[state] }" aria-hidden="true"></span>
    <span class="ost-connection__label">{{ LABEL[state] }}</span>
  </div>
</template>

<script setup lang="ts">
/**
 * FR-035 — the live / reconnecting / offline state of the team's realtime connection, shown in the
 * canvas toolbar next to the zoom controls (epic §7). It only reads `connectionState` from
 * ost-realtime.store.ts; reconnect backoff, epoch/gap detection and the tree re-read all stay in
 * that store.
 *
 * Colour: the palette has no danger hue, so "reconnecting" borrows the warm end of the priority
 * spectrum (rules.ts priorityColor) — the same function the node priority dots use — while live is
 * the accent token and offline a neutral. The state is also spelled out in words, so it never
 * depends on colour alone, and the accessible name says it too.
 */
import { computed } from 'vue';

import { priorityColor } from '../domain/rules';
import { type OstConnectionState, useOstRealtimeStore } from '../stores/ost-realtime.store';

const realtime = useOstRealtimeStore();
const state = computed<OstConnectionState>(() => realtime.connectionState);

const LABEL: Record<OstConnectionState, string> = { live: 'Live', reconnecting: 'Reconnecting', offline: 'Offline' };
const HINT: Record<OstConnectionState, string> = {
  live: 'Changes by other members arrive as they happen.',
  reconnecting: 'The connection dropped — reconnecting. The tree is re-read once it is back.',
  offline: 'Not connected. Changes by other members will not appear until the connection is back.',
};
const DOT: Record<OstConnectionState, string> = {
  live: 'var(--color-accent)',
  reconnecting: priorityColor(100),
  offline: 'var(--color-neutral-600)',
};
</script>

<style scoped>
.ost-connection {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 8px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--color-neutral-800);
  font-family: var(--font-heading);
  font-size: 11px;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  white-space: nowrap;
  color: var(--color-neutral-400);
}
.ost-connection.is-live {
  border-color: var(--color-accent-700);
  color: var(--color-accent-200);
}
.ost-connection.is-offline {
  color: var(--color-neutral-500);
}

.ost-connection__dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  flex: none;
}
/* Only while reconnecting does the dot move, and only where motion is welcome. */
.ost-connection.is-reconnecting .ost-connection__dot {
  animation: ost-connection-blink 1.1s ease-in-out infinite;
}

@keyframes ost-connection-blink {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.25;
  }
}

@media (prefers-reduced-motion: reduce) {
  .ost-connection.is-reconnecting .ost-connection__dot {
    animation: none;
  }
}
</style>
