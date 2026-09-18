<template>
  <section class="team-products" data-cy="teamProductsSection">
    <div class="d-flex justify-content-between align-items-center mb-2">
      <h3 class="mb-0">Products</h3>
      <button
        v-if="canEdit"
        class="btn btn-primary btn-sm"
        type="button"
        data-cy="newProductButton"
        :disabled="busy || showCreateForm"
        @click="openCreateForm"
      >
        New product
      </button>
    </div>

    <div v-if="listError" class="alert alert-danger" data-cy="productsListError">{{ listError }}</div>
    <div v-if="actionError" class="alert alert-danger" data-cy="productActionError">{{ actionError }}</div>

    <form v-if="showCreateForm && canEdit" class="card p-3 mb-3" data-cy="createProductForm" @submit.prevent="submitCreate">
      <div class="mb-2">
        <label for="new-product-name" class="form-label">Name</label>
        <input
          id="new-product-name"
          v-model="newProductName"
          type="text"
          class="form-control"
          data-cy="newProductName"
          required
          maxlength="100"
        />
      </div>
      <div class="mb-2">
        <label for="new-product-description" class="form-label">Description (optional)</label>
        <textarea
          id="new-product-description"
          v-model="newProductDescription"
          class="form-control"
          data-cy="newProductDescription"
          rows="2"
        ></textarea>
      </div>
      <div v-if="createError" class="alert alert-danger" data-cy="createProductError">{{ createError }}</div>
      <div class="d-flex gap-2">
        <button class="btn btn-primary" type="submit" data-cy="submitNewProduct" :disabled="busy">Create</button>
        <button class="btn btn-secondary" type="button" data-cy="cancelNewProduct" :disabled="busy" @click="cancelCreate">Cancel</button>
      </div>
    </form>

    <div v-if="loading && products.length === 0" class="text-muted" data-cy="productsLoading">Loading products…</div>

    <table v-else-if="products.length" class="table" data-cy="productsTable">
      <thead>
        <tr>
          <th>Name</th>
          <th>Description</th>
          <th>Status</th>
          <th v-if="canEdit"></th>
        </tr>
      </thead>
      <tbody>
        <tr
          v-for="p in products"
          :key="p.id"
          :data-cy="`productRow-${p.id}`"
          data-cy-shared="productRow"
          class="product-row"
          :class="{ 'product-archived text-muted fst-italic': p.archived }"
        >
          <template v-if="editingId === p.id && canEdit">
            <td colspan="4">
              <form class="d-flex flex-column gap-2" :data-cy="`editProductForm-${p.id}`" @submit.prevent="submitEdit(p)">
                <input
                  v-model="editName"
                  type="text"
                  class="form-control form-control-sm"
                  :data-cy="`editProductName-${p.id}`"
                  required
                  maxlength="100"
                />
                <textarea
                  v-model="editDescription"
                  class="form-control form-control-sm"
                  :data-cy="`editProductDescription-${p.id}`"
                  rows="2"
                ></textarea>
                <div v-if="editError" class="alert alert-danger mb-0" :data-cy="`editProductError-${p.id}`">{{ editError }}</div>
                <div class="d-flex gap-2">
                  <button class="btn btn-primary btn-sm" type="submit" :data-cy="`saveProductEdit-${p.id}`" :disabled="busy">Save</button>
                  <button
                    class="btn btn-secondary btn-sm"
                    type="button"
                    :data-cy="`cancelProductEdit-${p.id}`"
                    :disabled="busy"
                    @click="cancelEdit"
                  >
                    Cancel
                  </button>
                </div>
              </form>
            </td>
          </template>
          <template v-else>
            <td>
              <span :data-cy="`productName-${p.id}`">{{ p.name }}</span>
            </td>
            <td>
              <span v-if="p.description" :data-cy="`productDescription-${p.id}`">{{ p.description }}</span>
              <span v-else class="text-muted small">—</span>
            </td>
            <td>
              <span v-if="p.archived" class="badge bg-secondary" :data-cy="`productArchivedBadge-${p.id}`">Archived</span>
              <span v-else class="badge bg-success" :data-cy="`productActiveBadge-${p.id}`">Active</span>
            </td>
            <td v-if="canEdit">
              <div class="d-flex gap-2">
                <button
                  class="btn btn-outline-secondary btn-sm"
                  type="button"
                  :data-cy="`editProductButton-${p.id}`"
                  :disabled="busy"
                  @click="beginEdit(p)"
                >
                  Edit
                </button>
                <button
                  v-if="!p.archived"
                  class="btn btn-outline-warning btn-sm"
                  type="button"
                  :data-cy="`archiveProductButton-${p.id}`"
                  :disabled="busy"
                  @click="setArchived(p, true)"
                >
                  Archive
                </button>
                <button
                  v-else
                  class="btn btn-outline-success btn-sm"
                  type="button"
                  :data-cy="`unarchiveProductButton-${p.id}`"
                  :disabled="busy"
                  @click="setArchived(p, false)"
                >
                  Un-archive
                </button>
              </div>
            </td>
          </template>
        </tr>
      </tbody>
    </table>

    <div v-else class="text-muted" data-cy="productsEmpty">No products yet.</div>
  </section>
</template>

<script lang="ts" src="./team-products.component.ts"></script>
