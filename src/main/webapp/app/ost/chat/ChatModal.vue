<template>
  <OstDialog v-if="node" :title="node.title" width="420px" data-cy="ostChatModal" @close="ui.closeChat()">
    <!-- STUB (step 6): step 11 renders the shared ChatThread here. -->
    <p data-cy="ostChatModalStub">Chat for this node arrives in a later step.</p>
    <template #actions>
      <button type="button" class="ost-btn" data-cy="ostChatModalClose" @click="ui.closeChat()">Close</button>
    </template>
  </OstDialog>
</template>

<script setup lang="ts">
/** Chat modal opened from a node's chat chip (ui.chatId). Owned by step 11. */
import { computed } from 'vue';

import OstDialog from '../overlays/OstDialog.vue';
import { useOstTreeStore } from '../stores/ost-tree.store';
import { useOstUiStore } from '../stores/ost-ui.store';

const tree = useOstTreeStore();
const ui = useOstUiStore();
const node = computed(() => tree.byId(ui.chatId));
</script>
