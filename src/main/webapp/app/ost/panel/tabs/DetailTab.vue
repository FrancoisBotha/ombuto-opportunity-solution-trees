<template>
  <div v-if="node" class="ost-tab ost-detail" data-cy="ostTab-detail">
    <StatusChips v-if="statuses.length" :status="node.status" :options="statuses" :readonly="readonly" @change="save({ status: $event })" />

    <template v-if="node.type === 'assumption'">
      <ConfidenceField :value="node.conf" :readonly="readonly" @change="save({ conf: $event })" />
      <OwnerSelect :value="node.owner" :members="tree.team?.members ?? []" :readonly="readonly" @change="save({ owner: $event })" />
    </template>

    <EvidenceStrengthBar v-if="node.type === 'solution'" :node-key="node.id" />

    <template v-if="node.type === 'opportunity'">
      <ValueScale :value="node.value" :readonly="readonly" @change="save({ value: $event })" />
      <PrioritySlider :value="node.priority" :readonly="readonly" @change="save({ priority: $event })" />
    </template>

    <NotesField :value="node.note" :readonly="readonly" @change="save({ note: $event })" />

    <div class="ost-field">
      <div class="ost-field__label">Children ({{ children.length }})</div>
      <div v-if="children.length" class="ost-children">
        <button
          v-for="child in children"
          :key="child.id"
          type="button"
          class="ost-children__item"
          :data-cy="`ost-child-${child.id}`"
          @click="go(child.id)"
        >
          <span class="ost-children__kicker">{{ TYPE_BOX[child.type].label }}</span>
          <span class="ost-children__title">{{ child.title }}</span>
        </button>
      </div>
      <div v-if="!readonly && addTypes.length" class="ost-quick-add">
        <button
          v-for="type in addTypes"
          :key="type"
          type="button"
          class="ost-btn ost-quick-add__btn"
          :disabled="adding"
          :data-cy="`ost-quick-add-${type}`"
          @click="add(type)"
        >
          + {{ TYPE_BOX[type].label }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * Detail tab: status chips, confidence + owner (assumption), evidence strength (solution,
 * derived), value + priority (opportunity), notes, children with navigation and quick-add.
 * Every edit is optimistic through the tree store; failures show inline in the panel.
 */
import { computed, ref } from 'vue';

import { childrenOf } from '../../domain/derive';
import type { NodePatch } from '../../domain/mapping';
import { ALLOWED, STATUS, TYPE_BOX } from '../../domain/rules';
import type { NodeType } from '../../domain/types';
import { useOstTreeStore } from '../../stores/ost-tree.store';
import { useOstUiStore } from '../../stores/ost-ui.store';
import ConfidenceField from '../fields/ConfidenceField.vue';
import EvidenceStrengthBar from '../fields/EvidenceStrengthBar.vue';
import NotesField from '../fields/NotesField.vue';
import OwnerSelect from '../fields/OwnerSelect.vue';
import PrioritySlider from '../fields/PrioritySlider.vue';
import StatusChips from '../fields/StatusChips.vue';
import ValueScale from '../fields/ValueScale.vue';
import { usePanelAction } from '../panel-action';

const props = defineProps<{ nodeKey: string }>();
const tree = useOstTreeStore();
const ui = useOstUiStore();
const { run } = usePanelAction();

const node = computed(() => tree.byId(props.nodeKey));
const readonly = computed(() => !tree.canEdit);
const statuses = computed(() => (node.value ? STATUS[node.value.type] : []));
const children = computed(() => (node.value ? childrenOf(node.value.id, tree.nodes) : []));
const addTypes = computed<NodeType[]>(() => (node.value ? ALLOWED[node.value.type] : []));
const adding = ref(false);

function save(patch: NodePatch) {
  if (readonly.value || !node.value) return;
  void run(() => tree.patchNode(props.nodeKey, patch));
}

function go(key: string) {
  ui.select(key);
  ui.requestCentre(key);
}

async function add(type: NodeType) {
  if (readonly.value || adding.value) return;
  adding.value = true;
  try {
    // The store selects the new node and puts it in rename mode (the canvas owns rename).
    const key = await run(() => tree.createNode(props.nodeKey, type), 'The node could not be created.');
    if (key) ui.requestCentre(key);
  } finally {
    adding.value = false;
  }
}
</script>

<style scoped>
.ost-detail {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.ost-children {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.ost-children__item {
  display: flex;
  gap: 8px;
  align-items: baseline;
  text-align: left;
  font: inherit;
  font-size: 13px;
  padding: 7px 9px;
  background: transparent;
  border: 1px solid var(--color-divider);
  border-radius: var(--radius-sm);
  cursor: pointer;
  color: inherit;
}

.ost-children__item:hover {
  background: var(--color-accent-900);
  border-color: var(--color-accent-600);
}

.ost-children__kicker {
  font-family: var(--font-heading);
  font-size: 9px;
  letter-spacing: 0.07em;
  text-transform: uppercase;
  opacity: 0.6;
  flex: none;
  min-width: 44px;
}

.ost-children__title {
  text-wrap: pretty;
}

.ost-quick-add {
  display: flex;
  gap: 5px;
  flex-wrap: wrap;
  margin-top: 8px;
}

.ost-quick-add .ost-quick-add__btn {
  height: 28px;
  padding: 0 10px;
  font-size: 11px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}
</style>
