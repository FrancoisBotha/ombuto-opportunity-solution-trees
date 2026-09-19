<template>
  <ConfirmDeleteDialog v-if="ui.confirmId" />
  <ChatModal v-if="ui.chatId" />
  <div v-if="tree.error" class="ost-toast" role="alert" data-cy="ostError">
    <span>{{ tree.error }}</span>
    <button type="button" class="ost-btn ost-btn--ghost" data-cy="ostErrorDismiss" @click="tree.clearError()">Dismiss</button>
  </div>
</template>

<script setup lang="ts">
/** Every OST overlay in one place, rendered inside .ost-root so the tokens apply. */
import ChatModal from '../chat/ChatModal.vue';
import { useOstTreeStore } from '../stores/ost-tree.store';
import { useOstUiStore } from '../stores/ost-ui.store';

import ConfirmDeleteDialog from './ConfirmDeleteDialog.vue';

const tree = useOstTreeStore();
const ui = useOstUiStore();
</script>

<style scoped>
.ost-toast {
  position: fixed;
  right: 18px;
  bottom: 18px;
  z-index: 170;
  max-width: 380px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 8px 8px 14px;
  font-size: 13px;
  background: var(--color-surface);
  color: var(--color-text);
  border: 1px solid var(--color-neutral-700);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-md);
}
</style>
