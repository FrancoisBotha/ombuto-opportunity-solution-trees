<template>
  <OstDialog v-if="node" :title="node.title" width="392px" flush data-cy="ost-chat-modal" @close="ui.closeChat()">
    <template #header="{ titleId }">
      <header class="ost-chat-modal__head">
        <div class="ost-chat-modal__people" aria-hidden="true">
          <span v-for="p in people" :key="p.login" class="ost-chat-modal__avatar" :title="p.name">{{ p.initials }}</span>
        </div>
        <div class="ost-chat-modal__where">
          <h2 :id="titleId" class="ost-chat-modal__title" :title="node.title">{{ node.title }}</h2>
          <div class="ost-chat-modal__sub" data-cy="ost-chat-modal-sub">{{ sub }}</div>
        </div>
        <button
          type="button"
          class="ost-chat-modal__close ost-tap"
          title="Close"
          aria-label="Close chat"
          data-cy="ost-chat-modal-close"
          @click="ui.closeChat()"
        >
          <PhX :size="14" aria-hidden="true" />
        </button>
      </header>
    </template>
    <ChatThread :node-key="node.id" variant="modal" autofocus />
  </OstDialog>
</template>

<script setup lang="ts">
/**
 * Chat modal opened from a node's chat chip on the canvas (ui.chatId): the prototype's header —
 * up to three participants' initials, the node title, "<Type> · n messages" — over the shared
 * ChatThread. OstDialog traps focus, closes on Escape and backdrop click.
 */
import { computed, watch } from 'vue';

import { PhX } from '@phosphor-icons/vue';

import { TYPE_BOX } from '../domain/rules';
import OstDialog from '../overlays/OstDialog.vue';
import { useOstTreeStore } from '../stores/ost-tree.store';
import { useOstUiStore } from '../stores/ost-ui.store';

import ChatThread from './ChatThread.vue';
import { initialsOf } from './chat-format';

const tree = useOstTreeStore();
const ui = useOstUiStore();
/** Products have no thread: a chip never opens one, and a stale id closes the modal. */
const node = computed(() => {
  const n = tree.byId(ui.chatId);
  return n && n.type !== 'product' ? n : undefined;
});

watch(
  () => [ui.chatId, node.value] as const,
  ([id, n]) => {
    if (id && !n && !tree.loading) ui.closeChat();
  },
  { immediate: true },
);

const sub = computed(() => {
  const n = node.value;
  if (!n) return '';
  const count = n.commentCount;
  return `${TYPE_BOX[n.type].label} · ${count || 'no'} message${count === 1 ? '' : 's'}`;
});

/** Distinct authors of the thread plus the current user, at most three (prototype chatPeople). */
const people = computed(() => {
  const seen = new Map<string, { login: string; initials: string; name: string }>();
  for (const c of tree.comments[node.value?.id ?? ''] ?? []) {
    const login = c.authorLogin ?? c.authorName ?? '?';
    if (!seen.has(login)) seen.set(login, { login, initials: initialsOf(c), name: c.authorName ?? login });
  }
  const me = tree.team?.currentUserLogin;
  if (me && !seen.has(me)) {
    const member = tree.memberByLogin(me);
    seen.set(me, { login: me, initials: member?.initials ?? me.slice(0, 2).toUpperCase(), name: me });
  }
  return [...seen.values()].slice(0, 3);
});
</script>

<style scoped>
.ost-chat-modal__head {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 11px 12px;
  border-bottom: 1px solid var(--color-neutral-800);
}

.ost-chat-modal__people {
  display: flex;
  flex: none;
}

.ost-chat-modal__avatar {
  width: 26px;
  height: 26px;
  margin-left: -1px;
  display: grid;
  place-items: center;
  font-size: 10px;
  border: 1px solid var(--color-neutral-800);
  border-radius: 50%;
  background: var(--color-neutral-900);
}

.ost-chat-modal__where {
  flex: 1;
  min-width: 0;
}

.ost-chat-modal__head .ost-chat-modal__title {
  font-family: inherit;
  font-size: 13px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ost-chat-modal__sub {
  font-size: 10.5px;
  color: var(--color-neutral-400);
}

.ost-chat-modal__close {
  width: 28px;
  height: 28px;
  flex: none;
  display: grid;
  place-items: center;
  padding: 0;
  font: inherit;
  color: var(--color-accent);
  background: transparent;
  border: 1px solid transparent;
  border-radius: var(--radius-md);
  cursor: pointer;
}

.ost-chat-modal__close:hover {
  background: color-mix(in srgb, var(--color-text) 7%, transparent);
}
</style>
