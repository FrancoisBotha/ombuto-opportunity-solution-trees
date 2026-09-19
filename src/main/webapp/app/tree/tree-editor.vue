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

      <div
        v-if="writeError"
        class="alert alert-danger d-flex justify-content-between align-items-start"
        data-cy="treeEditorWriteError"
        role="alert"
      >
        <div>{{ writeErrorMessage }}</div>
        <button
          type="button"
          class="btn-close ms-3"
          aria-label="Dismiss"
          data-cy="treeEditorWriteErrorDismiss"
          @click="dismissWriteError"
        ></button>
      </div>

      <section v-if="products.length === 0" class="empty-state text-center p-4 border rounded" data-cy="treeEditorEmpty">
        <p class="mb-3"><strong>This team has no products yet.</strong></p>
        <button type="button" class="btn btn-primary" data-cy="treeEditorAddFirstProduct" :disabled="!canEdit" @click="openAddProductModal">
          Add your first product
        </button>
      </section>

      <template v-else>
        <div class="tree-editor-toolbar d-flex align-items-center gap-2 mb-2" data-cy="treeEditorToolbar">
          <button v-if="canEdit" type="button" class="btn btn-sm btn-primary" data-cy="treeEditorAddProduct" @click="openAddProductModal">
            + Add product
          </button>
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

        <div class="tree-editor-workspace">
          <section
            ref="canvasEl"
            class="tree-canvas"
            data-cy="treeEditorCanvas"
            :class="{
              'tree-canvas--dragging': isDragging,
              'tree-canvas--node-dragging': dragActive,
              'tree-canvas--no-drop': dragActive && !dragTarget,
              'tree-canvas--valid-drop': dragActive && !!dragTarget,
            }"
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
              <div
                class="tree-canvas__nodes"
                :style="{ transform: `translate(${canvasPadding}px, ${canvasPadding}px)` }"
                @mousedown="onNodeMouseDown"
                @click.capture="onNodesClickCapture"
              >
                <TreeNodeCard
                  v-for="n in nodes"
                  :key="n.key"
                  :type="n.type"
                  :node="n.data"
                  :selected="isSelected(n.type, n.id)"
                  :can-edit="canEdit"
                  :can-move-up="canMoveUpOf(n.type, n.id)"
                  :can-move-down="canMoveDownOf(n.type, n.id)"
                  :can-move-to="canMoveToOf(n.type, n.id)"
                  :x="n.x"
                  :y="n.y"
                  :width="n.width"
                  :height="n.height"
                  :has-children="n.hasChildren"
                  :collapsed="n.collapsed"
                  :hidden-descendant-count="n.hiddenDescendantCount"
                  :class="{
                    'tree-node-card--collapsed': n.collapsed,
                    'tree-node-card--drop-target': isDragValidReparentTarget(n.type, n.id),
                    'tree-node-card--dragging-source': dragActive && dragMovingType === n.type && dragMovingId === n.id,
                  }"
                  @select="onNodeSelect(n.type, n.id)"
                  @add-child="openAddChildModal($event.parentType, $event.parentId, $event.childType)"
                  @delete="openDeleteModal($event.type, $event.id)"
                  @move-to="openMoveModal($event.type, $event.id)"
                  @move-up="reorderPrev($event.type, $event.id)"
                  @move-down="reorderNext($event.type, $event.id)"
                  @toggle-collapse="onToggleCollapse($event)"
                />
                <div
                  v-if="dragActive && dragTarget && dragTarget.kind === 'insert'"
                  class="tree-canvas__insert-marker"
                  data-cy="treeEditorInsertMarker"
                  :style="{
                    left: dragTarget.markerX + 'px',
                    top: dragTarget.markerY + 'px',
                    height: dragTarget.markerHeight + 'px',
                  }"
                ></div>
              </div>
            </div>
          </section>
          <TreeDetailPanel @delete="openDeleteModal($event.type, $event.id)" />
        </div>
      </template>
    </div>

    <!-- Drag ghost — follows the cursor in client (screen) coordinates while a
         node drag is active. Kept outside the pan/zoom viewport so it appears
         crisp regardless of zoom level. -->
    <div
      v-if="dragActive"
      class="tree-editor-drag-ghost"
      data-cy="treeEditorDragGhost"
      :class="{ 'tree-editor-drag-ghost--no-drop': dragActive && !dragTarget }"
      :style="{ left: dragGhostX + 'px', top: dragGhostY + 'px' }"
    >
      {{ dragGhostLabel }}
    </div>

    <!-- Add product modal -->
    <div v-if="showAddProductModal" class="tree-editor-modal" data-cy="treeEditorAddProductModal" @click.self="closeAddProductModal">
      <div class="tree-editor-modal__dialog">
        <form @submit.prevent="submitAddProduct">
          <header class="tree-editor-modal__header">
            <h5 class="mb-0">Add product</h5>
          </header>
          <div class="tree-editor-modal__body">
            <div class="mb-2">
              <label for="tem-product-name" class="form-label">Name</label>
              <input
                id="tem-product-name"
                v-model="productForm.name"
                type="text"
                class="form-control"
                data-cy="addProductName"
                required
                minlength="2"
                maxlength="100"
              />
            </div>
            <div class="mb-2">
              <label for="tem-product-desc" class="form-label">Description</label>
              <textarea
                id="tem-product-desc"
                v-model="productForm.description"
                class="form-control"
                data-cy="addProductDescription"
                rows="2"
              ></textarea>
            </div>
            <div class="mb-2">
              <label for="tem-product-vision" class="form-label">Vision</label>
              <textarea
                id="tem-product-vision"
                v-model="productForm.vision"
                class="form-control"
                data-cy="addProductVision"
                rows="2"
              ></textarea>
            </div>
          </div>
          <footer class="tree-editor-modal__footer">
            <button type="button" class="btn btn-outline-secondary" data-cy="addProductCancel" @click="closeAddProductModal">Cancel</button>
            <button type="submit" class="btn btn-primary" data-cy="addProductSubmit" :disabled="isSavingProduct">
              {{ isSavingProduct ? 'Saving…' : 'Add product' }}
            </button>
          </footer>
        </form>
      </div>
    </div>

    <!-- Add child modal -->
    <div v-if="addChildContext" class="tree-editor-modal" data-cy="treeEditorAddChildModal" @click.self="closeAddChildModal">
      <div class="tree-editor-modal__dialog">
        <form @submit.prevent="submitAddChild">
          <header class="tree-editor-modal__header">
            <h5 class="mb-0">Add {{ addChildContext.childType }}</h5>
          </header>
          <div class="tree-editor-modal__body">
            <div class="mb-2">
              <label for="tem-child-title" class="form-label">Title</label>
              <input
                id="tem-child-title"
                v-model="childForm.title"
                type="text"
                class="form-control"
                data-cy="addChildTitle"
                required
                minlength="2"
                maxlength="200"
              />
            </div>
            <div class="mb-2">
              <label for="tem-child-desc" class="form-label">Description</label>
              <textarea
                id="tem-child-desc"
                v-model="childForm.description"
                class="form-control"
                data-cy="addChildDescription"
                rows="2"
              ></textarea>
            </div>
          </div>
          <footer class="tree-editor-modal__footer">
            <button type="button" class="btn btn-outline-secondary" data-cy="addChildCancel" @click="closeAddChildModal">Cancel</button>
            <button type="submit" class="btn btn-primary" data-cy="addChildSubmit" :disabled="isSavingChild">
              {{ isSavingChild ? 'Saving…' : 'Add' }}
            </button>
          </footer>
        </form>
      </div>
    </div>

    <!-- Move to… modal -->
    <div
      v-if="moveContext"
      class="tree-editor-modal"
      data-cy="treeEditorMoveModal"
      role="dialog"
      aria-modal="true"
      aria-labelledby="tem-move-title"
      tabindex="-1"
      @keydown.esc.prevent="closeMoveModal"
      @click.self="closeMoveModal"
    >
      <div class="tree-editor-modal__dialog">
        <form @submit.prevent="confirmMove">
          <header class="tree-editor-modal__header">
            <h5 id="tem-move-title" class="mb-0">Move {{ moveContext.label }} to…</h5>
          </header>
          <div class="tree-editor-modal__body">
            <div v-if="moveErrorMessage" class="alert alert-danger" data-cy="moveErrorMessage" role="alert">
              {{ moveErrorMessage }}
            </div>
            <p v-if="moveTargets.length === 0" class="text-muted" data-cy="moveNoTargets">No valid destinations available.</p>
            <div v-else class="tree-editor-move-targets" role="listbox" aria-label="Valid destinations">
              <div v-for="group in groupedMoveTargets" :key="group.productId" class="tree-editor-move-group">
                <div class="tree-editor-move-group__label small text-muted">{{ group.productName }}</div>
                <button
                  v-for="target in group.targets"
                  :key="`${target.parentType}:${target.parentId}`"
                  type="button"
                  role="option"
                  :aria-selected="
                    selectedMoveTarget &&
                    selectedMoveTarget.parentType === target.parentType &&
                    selectedMoveTarget.parentId === target.parentId
                      ? 'true'
                      : 'false'
                  "
                  :data-cy="`moveTarget-${target.parentType}-${target.parentId}`"
                  class="btn btn-sm btn-outline-secondary tree-editor-move-target"
                  :class="{
                    active:
                      selectedMoveTarget &&
                      selectedMoveTarget.parentType === target.parentType &&
                      selectedMoveTarget.parentId === target.parentId,
                  }"
                  :style="{ paddingLeft: 0.5 + target.depth * 0.75 + 'rem' }"
                  @click="selectMoveTarget(target)"
                >
                  <span class="badge bg-light text-dark me-2">{{ target.parentType }}</span
                  >{{ target.label }}
                </button>
              </div>
            </div>
          </div>
          <footer class="tree-editor-modal__footer">
            <button type="button" class="btn btn-outline-secondary" data-cy="moveCancel" @click="closeMoveModal">Cancel</button>
            <button type="submit" class="btn btn-primary" data-cy="moveConfirm" :disabled="!selectedMoveTarget || isMovingNode">
              {{ isMovingNode ? 'Moving…' : 'Move' }}
            </button>
          </footer>
        </form>
      </div>
    </div>

    <!-- Delete confirmation modal -->
    <div v-if="deleteContext" class="tree-editor-modal" data-cy="treeEditorDeleteModal" @click.self="closeDeleteModal">
      <div class="tree-editor-modal__dialog">
        <header class="tree-editor-modal__header">
          <h5 class="mb-0">Delete {{ deleteContext.type }}</h5>
        </header>
        <div class="tree-editor-modal__body">
          <p data-cy="deleteConfirmMessage">
            Are you sure you want to delete <strong>{{ deleteContext.label }}</strong
            >?
            <template v-if="deleteContext.descendantCount === 0"> It has no descendants. </template>
            <template v-else-if="deleteContext.descendantCount === 1">
              This will also delete <strong data-cy="deleteConfirmCount">1</strong> descendant.
            </template>
            <template v-else>
              This will also delete <strong data-cy="deleteConfirmCount">{{ deleteContext.descendantCount }}</strong> descendants.
            </template>
          </p>
        </div>
        <footer class="tree-editor-modal__footer">
          <button type="button" class="btn btn-outline-secondary" data-cy="deleteCancel" @click="closeDeleteModal">Cancel</button>
          <button type="button" class="btn btn-danger" data-cy="deleteConfirm" :disabled="isDeleting" @click="confirmDelete">
            {{ isDeleting ? 'Deleting…' : 'Delete' }}
          </button>
        </footer>
      </div>
    </div>
  </div>
</template>

<script lang="ts" src="./tree-editor.component.ts"></script>

<style scoped lang="scss">
.tree-editor-workspace {
  display: flex;
  gap: 0.75rem;
  align-items: stretch;
}
.tree-editor-workspace > .tree-canvas {
  flex: 1 1 auto;
  min-width: 0;
}
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

  &--node-dragging {
    cursor: not-allowed;
  }

  &--no-drop,
  &--no-drop :deep(.tree-node-card) {
    cursor: not-allowed !important;
  }

  &--valid-drop,
  &--valid-drop :deep(.tree-node-card) {
    cursor: grabbing !important;
  }
}

.tree-canvas__insert-marker {
  position: absolute;
  width: 4px;
  background: #e83e8c;
  border-radius: 2px;
  pointer-events: none;
  z-index: 3;
}

.tree-editor-drag-ghost {
  position: fixed;
  background: rgba(255, 255, 255, 0.95);
  border: 2px solid #593196;
  border-radius: 0.375rem;
  padding: 0.25rem 0.5rem;
  font-size: 0.85rem;
  font-weight: 600;
  color: #212529;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
  pointer-events: none;
  z-index: 2000;
  transform: translate(-50%, -50%);
  max-width: 240px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;

  &--no-drop {
    border-color: #dc3545;
    cursor: not-allowed;
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
}

// Sass cannot append a BEM suffix to a :deep() selector, so each modifier is spelled out.
:deep(.tree-node-card--product) {
  border-color: #593196;
}
:deep(.tree-node-card--outcome) {
  border-color: #6610f2;
}
:deep(.tree-node-card--opportunity) {
  border-color: #20c997;
}
:deep(.tree-node-card--solution) {
  border-color: #fd7e14;
}
:deep(.tree-node-card--selected) {
  box-shadow: 0 0 0 3px #e83e8c;
}
:deep(.tree-node-card--drop-target) {
  box-shadow: 0 0 0 3px #20c997;
}
:deep(.tree-node-card--dragging-source) {
  opacity: 0.45;
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

:deep(.tree-node-card__actions) {
  display: none;
  gap: 0.25rem;
  flex-wrap: wrap;
  margin-top: 0.25rem;
}

:deep(.tree-node-card:hover .tree-node-card__actions),
:deep(.tree-node-card--selected .tree-node-card__actions),
:deep(.tree-node-card:focus-within .tree-node-card__actions) {
  display: flex;
}

.tree-editor-modal {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1050;
}

.tree-editor-modal__dialog {
  background: #ffffff;
  border-radius: 0.5rem;
  min-width: 320px;
  max-width: 480px;
  width: 100%;
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.25);
  overflow: hidden;
}

.tree-editor-modal__header,
.tree-editor-modal__body,
.tree-editor-modal__footer {
  padding: 1rem 1.25rem;
}

.tree-editor-modal__header {
  border-bottom: 1px solid #dee2e6;
}

.tree-editor-modal__footer {
  border-top: 1px solid #dee2e6;
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
}
</style>
