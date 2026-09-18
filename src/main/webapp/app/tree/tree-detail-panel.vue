<template>
  <aside v-if="currentNode" class="tree-detail-panel" data-cy="treeDetailPanel">
    <header class="tree-detail-panel__header">
      <h5 class="mb-0">
        {{ editingType }}
        <small v-if="readOnly" class="text-muted ms-2" data-cy="detailReadOnly">(read only)</small>
      </h5>
    </header>

    <div v-if="serverError" class="alert alert-danger" data-cy="detailServerError" role="alert">
      {{ serverError }}
    </div>

    <div v-if="validationError" class="alert alert-warning" data-cy="detailValidationError" role="alert">
      {{ validationError }}
    </div>

    <form v-if="!readOnly" @submit.prevent="save">
      <!-- Product -->
      <template v-if="editingType === 'product'">
        <div class="mb-2">
          <label for="tdp-product-name" class="form-label">Name</label>
          <input
            id="tdp-product-name"
            v-model="productForm.name"
            type="text"
            class="form-control"
            data-cy="detailProductName"
            required
            minlength="2"
            maxlength="100"
          />
        </div>
        <div class="mb-2">
          <label for="tdp-product-desc" class="form-label">Description</label>
          <textarea
            id="tdp-product-desc"
            v-model="productForm.description"
            class="form-control"
            data-cy="detailProductDescription"
            rows="2"
          ></textarea>
        </div>
        <div class="mb-2">
          <label for="tdp-product-vision" class="form-label">Vision</label>
          <textarea
            id="tdp-product-vision"
            v-model="productForm.vision"
            class="form-control"
            data-cy="detailProductVision"
            rows="2"
          ></textarea>
        </div>
        <div class="form-check mb-2">
          <input
            id="tdp-product-archived"
            v-model="productForm.archived"
            type="checkbox"
            class="form-check-input"
            data-cy="detailProductArchived"
          />
          <label for="tdp-product-archived" class="form-check-label">Archived</label>
        </div>
      </template>

      <!-- Outcome / Opportunity / Solution -->
      <template v-else>
        <div class="mb-2">
          <label for="tdp-title" class="form-label">Title</label>
          <input
            id="tdp-title"
            v-model="titledForm.title"
            type="text"
            class="form-control"
            data-cy="detailTitle"
            required
            minlength="2"
            maxlength="200"
          />
        </div>
        <div class="mb-2">
          <label for="tdp-description" class="form-label">Description</label>
          <textarea
            id="tdp-description"
            v-model="titledForm.description"
            class="form-control"
            data-cy="detailDescription"
            rows="3"
          ></textarea>
        </div>
        <div class="mb-2">
          <label for="tdp-status" class="form-label">Status</label>
          <select id="tdp-status" v-model="titledForm.status" class="form-select" data-cy="detailStatus">
            <option v-for="s in statusOptions" :key="s" :value="s">{{ s }}</option>
          </select>
        </div>
        <div v-if="editingType === 'opportunity'" class="mb-2 d-flex gap-2">
          <div class="flex-grow-1">
            <label for="tdp-valuerating" class="form-label">Value rating (1-5)</label>
            <input
              id="tdp-valuerating"
              v-model.number="titledForm.valuerating"
              type="number"
              min="1"
              max="5"
              class="form-control"
              data-cy="detailValuerating"
            />
          </div>
          <div class="flex-grow-1">
            <label for="tdp-complexity" class="form-label">Complexity (1-5)</label>
            <input
              id="tdp-complexity"
              v-model.number="titledForm.complexity"
              type="number"
              min="1"
              max="5"
              class="form-control"
              data-cy="detailComplexity"
            />
          </div>
        </div>
        <div v-if="editingType === 'solution'" class="mb-2">
          <label for="tdp-effort" class="form-label">Effort (1-5)</label>
          <input
            id="tdp-effort"
            v-model.number="titledForm.effort"
            type="number"
            min="1"
            max="5"
            class="form-control"
            data-cy="detailEffort"
          />
        </div>
      </template>

      <footer class="tree-detail-panel__footer">
        <button type="button" class="btn btn-outline-danger" data-cy="detailDelete" @click="onDelete">Delete</button>
        <button type="button" class="btn btn-primary" data-cy="detailSave" :disabled="isSaving" @click="save">
          {{ isSaving ? 'Saving…' : 'Save' }}
        </button>
      </footer>
    </form>

    <!-- Read-only view -->
    <div v-else class="tree-detail-panel__readonly">
      <template v-if="editingType === 'product'">
        <div class="mb-2">
          <div class="form-label">Name</div>
          <div data-cy="detailTitleReadonly">{{ productForm.name }}</div>
        </div>
        <div class="mb-2" v-if="productForm.description">
          <div class="form-label">Description</div>
          <div>{{ productForm.description }}</div>
        </div>
        <div class="mb-2" v-if="productForm.vision">
          <div class="form-label">Vision</div>
          <div>{{ productForm.vision }}</div>
        </div>
        <div class="mb-2">
          <div class="form-label">Archived</div>
          <div>{{ productForm.archived ? 'Yes' : 'No' }}</div>
        </div>
      </template>
      <template v-else>
        <div class="mb-2">
          <div class="form-label">Title</div>
          <div data-cy="detailTitleReadonly">{{ titledForm.title }}</div>
        </div>
        <div class="mb-2" v-if="titledForm.description">
          <div class="form-label">Description</div>
          <div>{{ titledForm.description }}</div>
        </div>
        <div class="mb-2" v-if="titledForm.status">
          <div class="form-label">Status</div>
          <div data-cy="detailStatusReadonly">{{ titledForm.status }}</div>
        </div>
      </template>
    </div>

    <!-- Unsaved changes prompt -->
    <div v-if="showUnsavedPrompt" class="tree-detail-panel__prompt" data-cy="detailUnsavedPrompt">
      <p class="mb-2"><strong>You have unsaved changes.</strong></p>
      <p class="mb-2">Discard them and switch to the other node?</p>
      <div class="d-flex gap-2 justify-content-end">
        <button type="button" class="btn btn-outline-secondary btn-sm" data-cy="detailUnsavedKeep" @click="keepEditing">
          Keep editing
        </button>
        <button type="button" class="btn btn-danger btn-sm" data-cy="detailUnsavedDiscard" @click="discardAndSwitch">
          Discard changes
        </button>
      </div>
    </div>
  </aside>
</template>

<script lang="ts" src="./tree-detail-panel.component.ts"></script>

<style scoped lang="scss">
.tree-detail-panel {
  position: relative;
  width: 320px;
  background: #ffffff;
  border-left: 1px solid #dee2e6;
  padding: 1rem;
  overflow-y: auto;
}
.tree-detail-panel__header {
  padding-bottom: 0.5rem;
  margin-bottom: 0.75rem;
  border-bottom: 1px solid #dee2e6;
  text-transform: capitalize;
}
.tree-detail-panel__footer {
  display: flex;
  justify-content: space-between;
  gap: 0.5rem;
  margin-top: 1rem;
}
.tree-detail-panel__prompt {
  position: absolute;
  inset: 0;
  background: rgba(255, 255, 255, 0.96);
  padding: 1rem;
  display: flex;
  flex-direction: column;
  justify-content: center;
}
</style>
