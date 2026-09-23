<template>
  <div v-if="node" class="ost-tab" data-cy="ostTab-chat">
    <ChatThread :node-key="node.id" variant="panel" />
    <TranscriptsSection :node-key="node.id" />
  </div>
</template>

<script setup lang="ts">
/**
 * Detail-panel Chat tab: the node's thread (shared ChatThread), with the MTRANS-005 Transcripts
 * section beneath it. Not offered on products.
 */
import { computed } from 'vue';

import ChatThread from '../../chat/ChatThread.vue';
import { useOstTreeStore } from '../../stores/ost-tree.store';
import TranscriptsSection from '../transcripts/TranscriptsSection.vue';

const props = defineProps<{ nodeKey: string }>();
const tree = useOstTreeStore();
const node = computed(() => {
  const n = tree.byId(props.nodeKey);
  return n && n.type !== 'product' ? n : undefined;
});
</script>
