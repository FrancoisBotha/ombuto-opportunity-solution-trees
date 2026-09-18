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
        <p class="mb-0"><strong>This team has no products yet.</strong></p>
      </section>

      <section v-else class="tree-canvas" data-cy="treeEditorCanvas">
        <div v-for="product in products" :key="product.id" class="tree-branch" :data-cy="`treeProduct-${product.id}`">
          <h3 class="h5">{{ product.name }}</h3>
        </div>
      </section>
    </div>
  </div>
</template>

<script lang="ts" src="./tree-editor.component.ts"></script>
