<template>
  <div class="tree-editor" data-cy="treeEditor">
    <div v-if="isLoading" class="text-muted" data-cy="treeEditorLoading">Loading tree…</div>

    <div v-else-if="hasError" class="alert" :class="errorKind === 'forbidden' ? 'alert-warning' : 'alert-danger'" data-cy="treeEditorError">
      <h4 v-if="errorKind === 'forbidden'" class="alert-heading" data-cy="treeEditorAccessDenied">Access denied</h4>
      <h4 v-else-if="errorKind === 'not-found'" class="alert-heading" data-cy="treeEditorNotFound">Tree not found</h4>
      <h4 v-else class="alert-heading">Could not load tree</h4>
      <p class="mb-0">{{ errorMessage }}</p>
    </div>

    <div v-else-if="tree" class="tree-editor-shell" data-cy="treeEditorShell">
      <header class="d-flex justify-content-between align-items-center mb-3">
        <div>
          <h2 class="mb-0" data-cy="treeEditorTeamName">{{ tree.name }}</h2>
          <p v-if="tree.description" class="text-muted mb-0">{{ tree.description }}</p>
        </div>
        <div>
          <span v-if="!canEdit" class="badge bg-secondary" data-cy="treeEditorReadOnly">Read only</span>
        </div>
      </header>

      <section v-if="products.length === 0" class="empty-state text-center p-4 border rounded" data-cy="treeEditorEmpty">
        <p class="mb-3"><strong>This team has no products yet.</strong></p>
        <button type="button" class="btn btn-primary" data-cy="treeEditorAddFirstProduct" :disabled="!canEdit">
          Add your first product
        </button>
      </section>

      <template v-else>
        <div class="tree-editor-toolbar d-flex align-items-center gap-2 mb-2" data-cy="treeEditorToolbar">
          <label for="tree-editor-focus" class="form-label mb-0 me-1">Focus:</label>
          <select
            id="tree-editor-focus"
            class="form-select form-select-sm w-auto"
            data-cy="treeEditorFocusSelect"
            :value="focusedProductId ?? ''"
            @change="onFocusProduct"
          >
            <option value="">All products</option>
            <option v-for="p in products" :key="p.id" :value="p.id">{{ p.name }}</option>
          </select>
          <button
            v-if="focusedProductId != null"
            type="button"
            class="btn btn-sm btn-outline-secondary"
            data-cy="treeEditorClearFocus"
            @click="clearFocus"
          >
            Clear focus
          </button>
          <div class="ms-auto d-flex align-items-center gap-1">
            <button type="button" class="btn btn-sm btn-outline-secondary" data-cy="treeEditorZoomOut" @click="zoomOut">-</button>
            <span class="small text-muted" data-cy="treeEditorZoomLevel">{{ Math.round(zoom * 100) }}%</span>
            <button type="button" class="btn btn-sm btn-outline-secondary" data-cy="treeEditorZoomIn" @click="zoomIn">+</button>
            <button type="button" class="btn btn-sm btn-outline-secondary" data-cy="treeEditorResetView" @click="resetView">Reset</button>
          </div>
        </div>

        <section
          class="tree-canvas"
          data-cy="treeEditorCanvas"
          :class="{ 'tree-canvas--dragging': isDragging }"
          @mousedown="onCanvasMouseDown"
          @mousemove="onCanvasMouseMove"
          @mouseup="onCanvasMouseUp"
          @mouseleave="onCanvasMouseUp"
          @wheel="onWheel"
          @click="onCanvasClick"
        >
          <div
            class="tree-canvas__viewport"
            :style="{
              transform: `translate(${panX}px, ${panY}px) scale(${zoom})`,
              transformOrigin: '0 0',
              width: canvasWidth + 'px',
              height: canvasHeight + 'px',
            }"
          >
            <svg
              class="tree-canvas__edges"
              :width="canvasWidth"
              :height="canvasHeight"
              :viewBox="`0 0 ${canvasWidth} ${canvasHeight}`"
              data-cy="treeEditorEdges"
            >
              <g :transform="`translate(${canvasPadding}, ${canvasPadding})`">
                <path
                  v-for="edge in edges"
                  :key="edge.fromKey + '->' + edge.toKey"
                  class="tree-canvas__edge"
                  :d="`M ${edge.fromX} ${edge.fromY} C ${edge.fromX} ${(edge.fromY + edge.toY) / 2}, ${edge.toX} ${(edge.fromY + edge.toY) / 2}, ${edge.toX} ${edge.toY}`"
                  fill="none"
                  stroke="#a991d4"
                  stroke-width="2"
                />
              </g>
            </svg>
            <div class="tree-canvas__nodes" :style="{ transform: `translate(${canvasPadding}px, ${canvasPadding}px)` }">
              <TreeNodeCard
                v-for="n in nodes"
                :key="n.key"
                :type="n.type"
                :node="n.data"
                :selected="isSelected(n.type, n.id)"
                :x="n.x"
                :y="n.y"
                :width="n.width"
                :height="n.height"
                @select="onNodeSelect(n.type, n.id)"
              />
            </div>
          </div>
        </section>
      </template>
    </div>
  </div>
</template>

<script lang="ts" src="./tree-editor.component.ts"></script>

<style scoped lang="scss">
.tree-canvas {
  position: relative;
  overflow: hidden;
  border: 1px solid #dee2e6;
  border-radius: 0.375rem;
  height: 70vh;
  background-color: #f8f9fa;
  cursor: grab;

  &--dragging {
    cursor: grabbing;
  }
}

.tree-canvas__viewport {
  position: relative;
}

.tree-canvas__edges {
  position: absolute;
  top: 0;
  left: 0;
  pointer-events: none;
}

.tree-canvas__nodes {
  position: relative;
  width: 100%;
  height: 100%;
}

:deep(.tree-node-card) {
  position: absolute;
  border: 2px solid transparent;
  border-radius: 0.5rem;
  padding: 0.5rem 0.75rem;
  background: #ffffff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.15);
  cursor: pointer;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  gap: 0.2rem;

  &--product {
    border-color: #593196; // Pulse $purple / $primary
  }
  &--outcome {
    border-color: #6610f2; // Pulse $indigo
  }
  &--opportunity {
    border-color: #20c997; // Pulse $teal
  }
  &--solution {
    border-color: #fd7e14; // Pulse $orange
  }
  &--selected {
    box-shadow: 0 0 0 3px #e83e8c; // Pulse $pink highlight
  }
}

:deep(.tree-node-card__type) {
  font-size: 0.7rem;
  text-transform: uppercase;
  color: #6c757d;
  letter-spacing: 0.05em;
}

:deep(.tree-node-card__title) {
  font-weight: 600;
  font-size: 0.95rem;
  line-height: 1.15;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

:deep(.tree-node-card__status .badge) {
  background-color: #a991d4;
  color: #ffffff;
  font-size: 0.65rem;
}
</style>
