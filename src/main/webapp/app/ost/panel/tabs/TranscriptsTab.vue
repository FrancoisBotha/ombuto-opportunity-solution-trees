<template>
  <div v-if="node" class="ost-tab" data-cy="ostTab-transcripts">
    <TranscriptsSection :node-key="node.id" />
  </div>
</template>

<script setup lang="ts">
/**
 * Dedicated detail-panel tab for the node's transcripts. Not offered on products.
 */
import { computed } from 'vue';

import { useOstTreeStore } from '../../stores/ost-tree.store';
import TranscriptsSection from '../transcripts/TranscriptsSection.vue';

const props = defineProps<{ nodeKey: string }>();
const tree = useOstTreeStore();
const node = computed(() => {
  const n = tree.byId(props.nodeKey);
  return n && n.type !== 'product' ? n : undefined;
});
</script>
