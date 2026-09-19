<template>
  <div
    :class="cardClasses"
    :style="style"
    :data-cy="`treeNode-${type}-${nodeId}`"
    :data-node-type="type"
    :data-node-id="nodeId"
    role="button"
    tabindex="0"
    @click="onClick"
    @keydown.enter.self.prevent="onClick($event)"
    @keydown.space.self.prevent="onClick($event)"
  >
    <div class="tree-node-card__type" data-cy="treeNodeType">{{ typeLabel }}</div>
    <div class="tree-node-card__title" data-cy="treeNodeTitle">{{ title }}</div>
    <div v-if="status" class="tree-node-card__status" data-cy="treeNodeStatus">
      <span class="badge">{{ status }}</span>
    </div>
    <div v-if="canEdit" class="tree-node-card__actions" :data-cy="`treeNodeActions-${type}-${nodeId}`">
      <button
        v-for="ct in validChildren"
        :key="ct"
        type="button"
        class="btn btn-sm btn-outline-primary tree-node-card__add-child"
        :data-cy="`treeNodeAddChild-${type}-${nodeId}-${ct}`"
        :title="`Add ${childTypeLabel(ct)}`"
        @click="onAddChild(ct, $event)"
      >
        + {{ childTypeLabel(ct) }}
      </button>
      <button
        v-if="showMoveTo"
        type="button"
        class="btn btn-sm btn-outline-secondary tree-node-card__move-to"
        :data-cy="`treeNodeMoveTo-${type}-${nodeId}`"
        title="Move to…"
        @click="onMoveTo($event)"
        @keydown.enter.stop
      >
        Move to…
      </button>
      <button
        v-if="showReorder"
        type="button"
        class="btn btn-sm btn-outline-secondary tree-node-card__move-up"
        :data-cy="`treeNodeMoveUp-${type}-${nodeId}`"
        :disabled="!canMoveUp"
        :title="reorderPrevLabel"
        @click="onMoveUp($event)"
      >
        {{ reorderPrevLabel }}
      </button>
      <button
        v-if="showReorder"
        type="button"
        class="btn btn-sm btn-outline-secondary tree-node-card__move-down"
        :data-cy="`treeNodeMoveDown-${type}-${nodeId}`"
        :disabled="!canMoveDown"
        :title="reorderNextLabel"
        @click="onMoveDown($event)"
      >
        {{ reorderNextLabel }}
      </button>
      <button
        type="button"
        class="btn btn-sm btn-outline-danger tree-node-card__delete"
        :data-cy="`treeNodeDelete-${type}-${nodeId}`"
        title="Delete"
        @click="onDelete($event)"
      >
        Delete
      </button>
    </div>
  </div>
</template>

<script lang="ts" src="./tree-node-card.component.ts"></script>
