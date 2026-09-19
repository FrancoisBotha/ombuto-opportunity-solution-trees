<template>
  <div>
    <h2 id="page-heading" data-cy="NodeHistoryHeading">
      <span id="node-history">Node Histories</span>
      <div class="d-flex justify-content-end">
        <button class="btn btn-info me-2" @click="handleSyncList" :disabled="isFetching">
          <font-awesome-icon icon="sync" :spin="isFetching"></font-awesome-icon> <span>Refresh list</span>
        </button>
        <router-link :to="{ name: 'NodeHistoryCreate' }" custom v-slot="{ navigate }">
          <button
            @click="navigate"
            id="jh-create-entity"
            data-cy="entityCreateButton"
            class="btn btn-primary jh-create-entity create-node-history"
          >
            <font-awesome-icon icon="plus"></font-awesome-icon>
            <span>Create a new Node History</span>
          </button>
        </router-link>
      </div>
    </h2>
    <br />
    <div class="alert alert-warning" v-if="!isFetching && nodeHistories?.length === 0">
      <span>No Node Histories found</span>
    </div>
    <div class="table-responsive" v-if="nodeHistories?.length > 0">
      <table class="table table-striped" aria-describedby="nodeHistories">
        <thead>
          <tr>
            <th scope="col" @click="changeOrder('id')">
              <span>ID</span> <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'id'"></jhi-sort-indicator>
            </th>
            <th scope="col" @click="changeOrder('nodeType')">
              <span>Node Type</span>
              <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'nodeType'"></jhi-sort-indicator>
            </th>
            <th scope="col" @click="changeOrder('nodeId')">
              <span>Node Id</span>
              <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'nodeId'"></jhi-sort-indicator>
            </th>
            <th scope="col" @click="changeOrder('eventType')">
              <span>Event Type</span>
              <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'eventType'"></jhi-sort-indicator>
            </th>
            <th scope="col" @click="changeOrder('summary')">
              <span>Summary</span>
              <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'summary'"></jhi-sort-indicator>
            </th>
            <th scope="col" @click="changeOrder('createdDate')">
              <span>Created Date</span>
              <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'createdDate'"></jhi-sort-indicator>
            </th>
            <th scope="col" @click="changeOrder('author.login')">
              <span>Author</span>
              <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'author.login'"></jhi-sort-indicator>
            </th>
            <th scope="col"></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="nodeHistory in nodeHistories" :key="nodeHistory.id" data-cy="entityTable">
            <td>
              <router-link :to="{ name: 'NodeHistoryView', params: { nodeHistoryId: nodeHistory.id } }">{{ nodeHistory.id }}</router-link>
            </td>
            <td>{{ nodeHistory.nodeType }}</td>
            <td>{{ nodeHistory.nodeId }}</td>
            <td>{{ nodeHistory.eventType }}</td>
            <td>{{ nodeHistory.summary }}</td>
            <td>{{ formatDateShort(nodeHistory.createdDate) || '' }}</td>
            <td>
              {{ nodeHistory.author ? nodeHistory.author.login : '' }}
            </td>
            <td class="text-end">
              <div class="btn-group">
                <router-link :to="{ name: 'NodeHistoryView', params: { nodeHistoryId: nodeHistory.id } }" custom v-slot="{ navigate }">
                  <button @click="navigate" class="btn btn-info btn-sm details" data-cy="entityDetailsButton">
                    <font-awesome-icon icon="eye"></font-awesome-icon>
                    <span class="d-none d-md-inline">View</span>
                  </button>
                </router-link>
                <router-link :to="{ name: 'NodeHistoryEdit', params: { nodeHistoryId: nodeHistory.id } }" custom v-slot="{ navigate }">
                  <button @click="navigate" class="btn btn-primary btn-sm edit" data-cy="entityEditButton">
                    <font-awesome-icon icon="pencil-alt"></font-awesome-icon>
                    <span class="d-none d-md-inline">Edit</span>
                  </button>
                </router-link>
                <b-button
                  @click="prepareRemove(nodeHistory)"
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
        <span id="opportunitySolutionTreeApp.nodeHistory.delete.question" data-cy="nodeHistoryDeleteDialogHeading"
          >Confirm delete operation</span
        >
      </template>
      <div class="modal-body">
        <p id="jhi-delete-nodeHistory-heading">Are you sure you want to delete Node History {{ removeId }}?</p>
      </div>
      <template #footer>
        <div>
          <button type="button" class="btn btn-secondary" @click="closeDialog()">Cancel</button>
          <button
            type="button"
            class="btn btn-primary"
            id="jhi-confirm-delete-nodeHistory"
            data-cy="entityConfirmDeleteButton"
            @click="removeNodeHistory"
          >
            Delete
          </button>
        </div>
      </template>
    </b-modal>
    <div v-show="nodeHistories?.length > 0">
      <div class="d-flex justify-content-center">
        <jhi-item-count :page="page" :total="queryCount" :items-per-page="itemsPerPage"></jhi-item-count>
      </div>
      <div class="d-flex justify-content-center">
        <b-pagination size="md" :total-rows="totalItems" v-model="page" :per-page="itemsPerPage"></b-pagination>
      </div>
    </div>
  </div>
</template>

<script lang="ts" src="./node-history.component.ts"></script>
