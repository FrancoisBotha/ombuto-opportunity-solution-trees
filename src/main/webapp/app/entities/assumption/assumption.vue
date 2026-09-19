<template>
  <div>
    <h2 id="page-heading" data-cy="AssumptionHeading">
      <span id="assumption">Assumptions</span>
      <div class="d-flex justify-content-end">
        <button class="btn btn-info me-2" @click="handleSyncList" :disabled="isFetching">
          <font-awesome-icon icon="sync" :spin="isFetching"></font-awesome-icon> <span>Refresh list</span>
        </button>
        <router-link :to="{ name: 'AssumptionCreate' }" custom v-slot="{ navigate }">
          <button
            @click="navigate"
            id="jh-create-entity"
            data-cy="entityCreateButton"
            class="btn btn-primary jh-create-entity create-assumption"
          >
            <font-awesome-icon icon="plus"></font-awesome-icon>
            <span>Create a new Assumption</span>
          </button>
        </router-link>
      </div>
    </h2>
    <br />
    <div class="alert alert-warning" v-if="!isFetching && assumptions?.length === 0">
      <span>No Assumptions found</span>
    </div>
    <div class="table-responsive" v-if="assumptions?.length > 0">
      <table class="table table-striped" aria-describedby="assumptions">
        <thead>
          <tr>
            <th scope="col"><span>ID</span></th>
            <th scope="col"><span>Statement</span></th>
            <th scope="col"><span>Description</span></th>
            <th scope="col"><span>Status</span></th>
            <th scope="col"><span>Confidence</span></th>
            <th scope="col"><span>Sort Order</span></th>
            <th scope="col"><span>Created Date</span></th>
            <th scope="col"><span>Last Modified Date</span></th>
            <th scope="col"><span>Solution</span></th>
            <th scope="col"><span>Owner</span></th>
            <th scope="col"></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="assumption in assumptions" :key="assumption.id" data-cy="entityTable">
            <td>
              <router-link :to="{ name: 'AssumptionView', params: { assumptionId: assumption.id } }">{{ assumption.id }}</router-link>
            </td>
            <td>{{ assumption.statement }}</td>
            <td>{{ assumption.description }}</td>
            <td>{{ assumption.status }}</td>
            <td>{{ assumption.confidence }}</td>
            <td>{{ assumption.sortOrder }}</td>
            <td>{{ formatDateShort(assumption.createdDate) || '' }}</td>
            <td>{{ formatDateShort(assumption.lastModifiedDate) || '' }}</td>
            <td>
              <div v-if="assumption.solution">
                <router-link :to="{ name: 'SolutionView', params: { solutionId: assumption.solution.id } }">{{
                  assumption.solution.title
                }}</router-link>
              </div>
            </td>
            <td>
              {{ assumption.owner ? assumption.owner.login : '' }}
            </td>
            <td class="text-end">
              <div class="btn-group">
                <router-link :to="{ name: 'AssumptionView', params: { assumptionId: assumption.id } }" custom v-slot="{ navigate }">
                  <button @click="navigate" class="btn btn-info btn-sm details" data-cy="entityDetailsButton">
                    <font-awesome-icon icon="eye"></font-awesome-icon>
                    <span class="d-none d-md-inline">View</span>
                  </button>
                </router-link>
                <router-link :to="{ name: 'AssumptionEdit', params: { assumptionId: assumption.id } }" custom v-slot="{ navigate }">
                  <button @click="navigate" class="btn btn-primary btn-sm edit" data-cy="entityEditButton">
                    <font-awesome-icon icon="pencil-alt"></font-awesome-icon>
                    <span class="d-none d-md-inline">Edit</span>
                  </button>
                </router-link>
                <b-button
                  @click="prepareRemove(assumption)"
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
        <span id="opportunitySolutionTreeApp.assumption.delete.question" data-cy="assumptionDeleteDialogHeading"
          >Confirm delete operation</span
        >
      </template>
      <div class="modal-body">
        <p id="jhi-delete-assumption-heading">Are you sure you want to delete Assumption {{ removeId }}?</p>
      </div>
      <template #footer>
        <div>
          <button type="button" class="btn btn-secondary" @click="closeDialog()">Cancel</button>
          <button
            type="button"
            class="btn btn-primary"
            id="jhi-confirm-delete-assumption"
            data-cy="entityConfirmDeleteButton"
            @click="removeAssumption"
          >
            Delete
          </button>
        </div>
      </template>
    </b-modal>
  </div>
</template>

<script lang="ts" src="./assumption.component.ts"></script>
