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
    @keydown.enter.prevent="onClick($event)"
    @keydown.space.prevent="onClick($event)"
  >
    <div class="tree-node-card__type" data-cy="treeNodeType">{{ typeLabel }}</div>
    <div class="tree-node-card__title" data-cy="treeNodeTitle">{{ title }}</div>
    <div v-if="status" class="tree-node-card__status" data-cy="treeNodeStatus">
      <span class="badge">{{ status }}</span>
    </div>
  </div>
</template>

<script lang="ts" src="./tree-node-card.component.ts"></script>
