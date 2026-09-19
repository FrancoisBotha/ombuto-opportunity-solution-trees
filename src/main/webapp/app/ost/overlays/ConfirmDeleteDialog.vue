<template>
  <OstDialog v-if="node" :title="`Delete this ${kind}?`" data-cy="ostConfirmDelete" @close="cancel">
    <p class="ost-confirm__text" data-cy="ostConfirmDeleteText">
      “{{ node.title }}”<template v-if="count"> and its {{ count }} descendant{{ count === 1 ? '' : 's' }}</template> will be removed from
      the tree. This can’t be undone.
    </p>
    <template #actions>
      <button type="button" class="ost-btn" data-cy="ostConfirmDeleteCancel" autofocus :disabled="busy" @click="cancel">Cancel</button>
      <button type="button" class="ost-btn ost-btn--destructive" data-cy="ostConfirmDeleteConfirm" :disabled="busy" @click="confirm">
        Delete
      </button>
    </template>
  </OstDialog>
</template>

<script setup lang="ts">
/** Delete confirmation naming the node and its descendant count; the delete cascades via the store. */
import { computed, ref } from 'vue';

import { TYPE_BOX } from '../domain/rules';
import { useOstTreeStore } from '../stores/ost-tree.store';
import { useOstUiStore } from '../stores/ost-ui.store';

import OstDialog from './OstDialog.vue';

const tree = useOstTreeStore();
const ui = useOstUiStore();
const busy = ref(false);

const node = computed(() => tree.byId(ui.confirmId));
const kind = computed(() => (node.value ? TYPE_BOX[node.value.type].label.toLowerCase() : ''));
const count = computed(() => (node.value ? tree.descendantCount(node.value.id) : 0));

function cancel() {
  if (!busy.value) ui.cancelDelete();
}

async function confirm() {
  if (!node.value || busy.value) return;
  busy.value = true;
  try {
    await tree.deleteNode(node.value.id);
  } finally {
    busy.value = false;
    ui.cancelDelete();
  }
}
</script>

<style scoped>
.ost-confirm__text {
  margin: 0;
  text-wrap: pretty;
}
</style>
