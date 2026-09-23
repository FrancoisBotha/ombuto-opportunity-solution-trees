<template>
  <div v-if="node" class="ost-tab ost-detail" data-cy="ostTab-detail">
    <NodeFields :node-key="node.id" :readonly="readonly" />

    <button
      v-if="node.type !== 'product'"
      type="button"
      class="ost-btn ost-btn--quick"
      data-cy="ost-detail-chat"
      aria-haspopup="dialog"
      @click="ui.openChat(node.id)"
    >
      Chat<span v-if="node.commentCount">
        (<span data-cy="ost-detail-chat-count">{{ node.commentCount }}</span
        >)</span
      >
    </button>

    <NotesField
      :value="node.note"
      :readonly="readonly"
      :dropped="notesDropped"
      @change="saveNotes"
      @focus="onNotesFocus"
      @blur="onNotesBlur"
      @dismiss-dropped="dismissNotesDropped"
    />

    <div class="ost-field">
      <div class="ost-field__label ost-field__label--7">Children ({{ children.length }})</div>
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
          class="ost-btn ost-btn--quick"
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
 * Detail tab: the node's typed fields (NodeFields — status, confidence + owner, evidence
 * strength, value + priority), notes, children with navigation and quick-add (useQuickAdd).
 * Every edit is optimistic through the tree store; failures show inline in the panel.
 */
import { computed } from 'vue';

import { childrenOf } from '../../domain/derive';
import { TYPE_BOX } from '../../domain/rules';
import type { NodeType } from '../../domain/types';
import { useOstTreeStore } from '../../stores/ost-tree.store';
import { useOstUiStore } from '../../stores/ost-ui.store';
import NodeFields from '../fields/NodeFields.vue';
import NotesField from '../fields/NotesField.vue';
import { type Settled, usePanelAction } from '../panel-action';
import { useQuickAdd } from '../useQuickAdd';

const props = defineProps<{ nodeKey: string }>();
const tree = useOstTreeStore();
const ui = useOstUiStore();
const { run } = usePanelAction();

const node = computed(() => tree.byId(props.nodeKey));
const readonly = computed(() => !tree.canEdit);
const children = computed(() => (node.value ? childrenOf(node.value.id, tree.nodes) : []));
const {
  addTypes,
  adding,
  add: quickAdd,
} = useQuickAdd(
  () => props.nodeKey,
  () => readonly.value,
);

/** Notes report back whether the save stuck, so a failed draft stays in the field. */
async function saveNotes(note: string, settled: Settled) {
  if (readonly.value || !node.value) return settled(false);
  tree.acknowledgeDroppedRemote(props.nodeKey, 'note');
  settled(await run(() => tree.patchNode(props.nodeKey, { note })));
}

/** The remote value dropped for this node's Notes because a local edit was in flight or typing. */
const notesDropped = computed<string | undefined>(() => {
  const dropped = tree.droppedRemoteFor(props.nodeKey).note;
  return typeof dropped === 'string' ? dropped : undefined;
});
const onNotesFocus = () => tree.markTyping(props.nodeKey, 'note');
const onNotesBlur = () => tree.clearTyping(props.nodeKey, 'note');
const dismissNotesDropped = () => tree.acknowledgeDroppedRemote(props.nodeKey, 'note');

function go(key: string) {
  ui.select(key);
  ui.requestCentre(key);
}

/** The store selects the new node and puts it in rename mode (the canvas owns rename); centre it. */
async function add(type: NodeType) {
  const key = await quickAdd(type);
  if (key) ui.requestCentre(key);
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
  display: grid;
  grid-template-columns: 84px minmax(0, 1fr);
  gap: 10px;
  align-items: baseline;
  text-align: left;
  font: inherit;
  font-size: 13px;
  padding: 7px 9px;
  background: transparent;
  border: 1px solid var(--color-divider);
  border-radius: 0;
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
  white-space: nowrap;
}

.ost-children__title {
  min-width: 0;
  overflow-wrap: anywhere;
  text-wrap: pretty;
}

.ost-quick-add {
  display: flex;
  gap: 5px;
  flex-wrap: wrap;
  margin-top: 8px;
}
</style>
