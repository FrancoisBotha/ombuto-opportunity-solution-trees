<template>
  <section class="ost-nd-children" aria-labelledby="ost-nd-children-label">
    <h2 id="ost-nd-children-label" class="ost-nd-section">Beneath this node ({{ children.length }})</h2>
    <div v-if="children.length" class="ost-nd-children__grid">
      <router-link
        v-for="child in children"
        :key="child.id"
        class="ost-nd-card"
        :to="{ name: 'OstNodeDetail', params: { teamId, nodeKey: child.id } }"
        :data-cy="`ost-node-detail-child-${child.id}`"
      >
        <span class="ost-nd-card__kicker">{{ TYPE_BOX[child.type].label }}</span>
        <span class="ost-nd-card__title">{{ child.title }}</span>
        <span v-if="child.status || beneath(child.id)" class="ost-nd-card__meta">
          <span v-if="child.status" class="ost-badge" :class="`ost-badge--${statusTone(child.status)}`">{{ child.status }}</span>
          <span v-if="beneath(child.id)">{{ beneath(child.id) }} beneath</span>
        </span>
      </router-link>
    </div>
    <p v-else class="ost-nd-children__empty" data-cy="ost-node-detail-no-children">
      {{ addTypes.length ? 'Nothing beneath this node yet.' : `${TYPE_BOX[type].label} nodes have no children.` }}
    </p>
    <div v-if="!readonly && addTypes.length" class="ost-nd-children__add">
      <button
        v-for="childType in addTypes"
        :key="childType"
        type="button"
        class="ost-btn ost-nd-children__add-btn"
        :disabled="adding"
        :data-cy="`ost-node-detail-add-${childType}`"
        @click="add(childType)"
      >
        + {{ TYPE_BOX[childType].label }}
      </button>
    </div>
  </section>
</template>

<script setup lang="ts">
/**
 * "Beneath this node": the children as cards (kicker, title, status badge; a card opens that
 * child's detail page) and, for editors, quick-add buttons for the permitted child types.
 */
import { computed, ref } from 'vue';

import { childrenOf, statusTone } from '../domain/derive';
import { ALLOWED, TYPE_BOX } from '../domain/rules';
import type { NodeType } from '../domain/types';
import { usePanelAction } from '../panel/panel-action';
import { useOstTreeStore } from '../stores/ost-tree.store';

const props = defineProps<{ nodeKey: string; teamId: string; readonly: boolean }>();
const emit = defineEmits<{ created: [key: string] }>();

const tree = useOstTreeStore();
const { run } = usePanelAction();

const type = computed<NodeType>(() => tree.byId(props.nodeKey)?.type ?? 'evidence');
const children = computed(() => childrenOf(props.nodeKey, tree.nodes));
const addTypes = computed<NodeType[]>(() => ALLOWED[type.value]);
const beneath = (key: string) => tree.descendantCount(key);
const adding = ref(false);

async function add(childType: NodeType) {
  if (props.readonly || adding.value) return;
  adding.value = true;
  try {
    const key = await run(() => tree.createNode(props.nodeKey, childType), 'The node could not be created.');
    if (key) emit('created', key);
  } finally {
    adding.value = false;
  }
}
</script>

<style scoped>
.ost-nd-children__grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(min(230px, 100%), 1fr));
  gap: 14px;
}

.ost-root .ost-nd-card {
  display: flex;
  flex-direction: column;
  gap: 7px;
  min-width: 0;
  padding: 14px;
  color: var(--color-text);
  background: var(--color-surface);
  border: 1px solid transparent;
  border-radius: var(--radius-md);
}

.ost-root .ost-nd-card:hover {
  color: var(--color-text);
  border-color: var(--color-accent-600);
}

.ost-root .ost-nd-card:focus-visible {
  outline: 2px solid var(--color-accent);
  outline-offset: 2px;
}

.ost-nd-card__kicker {
  font-size: 10px;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--color-accent);
}

.ost-nd-card__title {
  font-family: var(--font-heading);
  font-size: 16px;
  line-height: 1.2;
  text-wrap: pretty;
  overflow-wrap: anywhere;
}

.ost-nd-card__meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  color: color-mix(in srgb, var(--color-text) 50%, transparent);
}

.ost-nd-children__empty {
  margin: 0;
  font-size: 13px;
  color: var(--color-neutral-400);
}

.ost-nd-children__add {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 12px;
}

.ost-root .ost-nd-children__add-btn {
  height: 28px;
  padding: 0 10px;
  font-size: 11px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}
</style>
