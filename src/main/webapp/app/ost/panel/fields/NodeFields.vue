<template>
  <div v-if="node && hasFields" class="ost-node-fields" :class="`ost-node-fields--${variant}`" data-cy="ost-node-fields">
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

    <LabelChips
      v-if="node.type === 'opportunity' || node.type === 'solution'"
      :node-key="node.id"
      :tags="node.tags"
      :readonly="readonly"
      :team-id="teamId"
    />
  </div>
</template>

<script setup lang="ts">
/**
 * A node's typed fields, shared by the panel's Detail tab and the full-page node detail: status
 * chips (opportunity / solution / assumption), confidence + owner (assumption), the derived
 * evidence-strength bar (solution), value + priority (opportunity). Every change is an optimistic
 * tree-store patch run through the surrounding panel / page error slot (panel-action.ts).
 *
 * `variant` only lays the fields out: a column in the panel, a responsive grid on the page.
 * Renders nothing for types without such fields (product, outcome, evidence).
 */
import { computed } from 'vue';

import type { NodePatch } from '../../domain/mapping';
import { STATUS } from '../../domain/rules';
import { useOstTreeStore } from '../../stores/ost-tree.store';
import { usePanelAction } from '../panel-action';

import ConfidenceField from './ConfidenceField.vue';
import EvidenceStrengthBar from './EvidenceStrengthBar.vue';
import LabelChips from './LabelChips.vue';
import OwnerSelect from './OwnerSelect.vue';
import PrioritySlider from './PrioritySlider.vue';
import StatusChips from './StatusChips.vue';
import ValueScale from './ValueScale.vue';

const props = withDefaults(defineProps<{ nodeKey: string; readonly: boolean; variant?: 'panel' | 'page' }>(), { variant: 'panel' });

const tree = useOstTreeStore();
const { run } = usePanelAction();

const node = computed(() => tree.byId(props.nodeKey));
const statuses = computed(() => (node.value ? STATUS[node.value.type] : []));
const hasFields = computed(() => !!node.value && ['opportunity', 'solution', 'assumption'].includes(node.value.type));
const teamId = computed<number | null>(() => tree.team?.id ?? null);

function save(patch: NodePatch) {
  if (props.readonly || !node.value) return;
  const key = node.value.id;
  void run(() => tree.patchNode(key, patch));
}
</script>

<style scoped>
.ost-node-fields--panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.ost-node-fields--page {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(240px, 100%), 1fr));
  gap: 18px 24px;
}

.ost-node-fields--page > * {
  min-width: 0;
}
</style>
