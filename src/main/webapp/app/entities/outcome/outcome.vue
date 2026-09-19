<template>
  <div>
    <h2 id="page-heading" data-cy="OutcomeHeading">
      <span id="outcome">Outcomes</span>
      <div class="d-flex justify-content-end">
        <button class="btn btn-info me-2" @click="handleSyncList" :disabled="isFetching">
          <font-awesome-icon icon="sync" :spin="isFetching"></font-awesome-icon> <span>Refresh list</span>
        </button>
        <router-link :to="{ name: 'OutcomeCreate' }" custom v-slot="{ navigate }">
          <button
            @click="navigate"
            id="jh-create-entity"
            data-cy="entityCreateButton"
            class="btn btn-primary jh-create-entity create-outcome"
          >
            <font-awesome-icon icon="plus"></font-awesome-icon>
            <span>Create a new Outcome</span>
          </button>
        </router-link>
      </div>
    </h2>
    <br />
    <div class="alert alert-warning" v-if="!isFetching && outcomes?.length === 0">
      <span>No Outcomes found</span>
    </div>
    <div class="table-responsive" v-if="outcomes?.length > 0">
      <table class="table table-striped" aria-describedby="outcomes">
        <thead>
          <tr>
            <th scope="col"><span>ID</span></th>
            <th scope="col"><span>Title</span></th>
            <th scope="col"><span>Description</span></th>
            <th scope="col"><span>Sort Order</span></th>
            <th scope="col"><span>Created Date</span></th>
            <th scope="col"><span>Last Modified Date</span></th>
            <th scope="col"><span>Product</span></th>
            <th scope="col"><span>Owner</span></th>
            <th scope="col"></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="outcome in outcomes" :key="outcome.id" data-cy="entityTable">
            <td>
              <router-link :to="{ name: 'OutcomeView', params: { outcomeId: outcome.id } }">{{ outcome.id }}</router-link>
            </td>
            <td>{{ outcome.title }}</td>
            <td>{{ outcome.description }}</td>
            <td>{{ outcome.sortOrder }}</td>
            <td>{{ formatDateShort(outcome.createdDate) || '' }}</td>
            <td>{{ formatDateShort(outcome.lastModifiedDate) || '' }}</td>
            <td>
              <div v-if="outcome.product">
                <router-link :to="{ name: 'ProductView', params: { productId: outcome.product.id } }">{{
                  outcome.product.name
                }}</router-link>
              </div>
            </td>
            <td>
              {{ outcome.owner ? outcome.owner.login : '' }}
            </td>
            <td class="text-end">
              <div class="btn-group">
                <router-link :to="{ name: 'OutcomeView', params: { outcomeId: outcome.id } }" custom v-slot="{ navigate }">
                  <button @click="navigate" class="btn btn-info btn-sm details" data-cy="entityDetailsButton">
                    <font-awesome-icon icon="eye"></font-awesome-icon>
                    <span class="d-none d-md-inline">View</span>
                  </button>
                </router-link>
                <router-link :to="{ name: 'OutcomeEdit', params: { outcomeId: outcome.id } }" custom v-slot="{ navigate }">
                  <button @click="navigate" class="btn btn-primary btn-sm edit" data-cy="entityEditButton">
                    <font-awesome-icon icon="pencil-alt"></font-awesome-icon>
                    <span class="d-none d-md-inline">Edit</span>
                  </button>
                </router-link>
                <b-button
                  @click="prepareRemove(outcome)"
                  variant="danger"
                  class="btn btn-sm"
                  data-cy="entityDeleteButton"
                  v-b-modal.removeEntity
                >
                  <font-awesome-icon icon="times"></font-awesome-icon>
                  <span class="d-none d-md-inline">Delete</span>
                </b-button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    <b-modal ref="removeEntity" id="removeEntity">
      <template #title>
        <span id="opportunitySolutionTreeApp.outcome.delete.question" data-cy="outcomeDeleteDialogHeading">Confirm delete operation</span>
      </template>
      <div class="modal-body">
        <p id="jhi-delete-outcome-heading">Are you sure you want to delete Outcome {{ removeId }}?</p>
      </div>
      <template #footer>
        <div>
          <button type="button" class="btn btn-secondary" @click="closeDialog()">Cancel</button>
          <button
            type="button"
            class="btn btn-primary"
            id="jhi-confirm-delete-outcome"
            data-cy="entityConfirmDeleteButton"
            @click="removeOutcome"
          >
            Delete
          </button>
        </div>
      </template>
    </b-modal>
  </div>
</template>

<script lang="ts" src="./outcome.component.ts"></script>
