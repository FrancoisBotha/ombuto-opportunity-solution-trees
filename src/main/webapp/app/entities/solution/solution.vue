<template>
  <div>
    <h2 id="page-heading" data-cy="SolutionHeading">
      <span id="solution">Solutions</span>
      <div class="d-flex justify-content-end">
        <button class="btn btn-info me-2" @click="handleSyncList" :disabled="isFetching">
          <font-awesome-icon icon="sync" :spin="isFetching"></font-awesome-icon> <span>Refresh list</span>
        </button>
        <router-link :to="{ name: 'SolutionCreate' }" custom v-slot="{ navigate }">
          <button
            @click="navigate"
            id="jh-create-entity"
            data-cy="entityCreateButton"
            class="btn btn-primary jh-create-entity create-solution"
          >
            <font-awesome-icon icon="plus"></font-awesome-icon>
            <span>Create a new Solution</span>
          </button>
        </router-link>
      </div>
    </h2>
    <br />
    <div class="alert alert-warning" v-if="!isFetching && solutions?.length === 0">
      <span>No Solutions found</span>
    </div>
    <div class="table-responsive" v-if="solutions?.length > 0">
      <table class="table table-striped" aria-describedby="solutions">
        <thead>
          <tr>
            <th scope="col" @click="changeOrder('id')">
              <span>ID</span> <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'id'"></jhi-sort-indicator>
            </th>
            <th scope="col" @click="changeOrder('title')">
              <span>Title</span>
              <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'title'"></jhi-sort-indicator>
            </th>
            <th scope="col" @click="changeOrder('description')">
              <span>Description</span>
              <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'description'"></jhi-sort-indicator>
            </th>
            <th scope="col" @click="changeOrder('status')">
              <span>Status</span>
              <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'status'"></jhi-sort-indicator>
            </th>
            <th scope="col" @click="changeOrder('effort')">
              <span>Effort</span>
              <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'effort'"></jhi-sort-indicator>
            </th>
            <th scope="col" @click="changeOrder('sortOrder')">
              <span>Sort Order</span>
              <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'sortOrder'"></jhi-sort-indicator>
            </th>
            <th scope="col" @click="changeOrder('createdDate')">
              <span>Created Date</span>
              <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'createdDate'"></jhi-sort-indicator>
            </th>
            <th scope="col" @click="changeOrder('lastModifiedDate')">
              <span>Last Modified Date</span>
              <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'lastModifiedDate'"></jhi-sort-indicator>
            </th>
            <th scope="col" @click="changeOrder('opportunity.title')">
              <span>Opportunity</span>
              <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'opportunity.title'"></jhi-sort-indicator>
            </th>
            <th scope="col" @click="changeOrder('owner.login')">
              <span>Owner</span>
              <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'owner.login'"></jhi-sort-indicator>
            </th>
            <th scope="col"></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="solution in solutions" :key="solution.id" data-cy="entityTable">
            <td>
              <router-link :to="{ name: 'SolutionView', params: { solutionId: solution.id } }">{{ solution.id }}</router-link>
            </td>
            <td>{{ solution.title }}</td>
            <td>{{ solution.description }}</td>
            <td>{{ solution.status }}</td>
            <td>{{ solution.effort }}</td>
            <td>{{ solution.sortOrder }}</td>
            <td>{{ formatDateShort(solution.createdDate) || '' }}</td>
            <td>{{ formatDateShort(solution.lastModifiedDate) || '' }}</td>
            <td>
              <div v-if="solution.opportunity">
                <router-link :to="{ name: 'OpportunityView', params: { opportunityId: solution.opportunity.id } }">{{
                  solution.opportunity.title
                }}</router-link>
              </div>
            </td>
            <td>
              {{ solution.owner ? solution.owner.login : '' }}
            </td>
            <td class="text-end">
              <div class="btn-group">
                <router-link :to="{ name: 'SolutionView', params: { solutionId: solution.id } }" custom v-slot="{ navigate }">
                  <button @click="navigate" class="btn btn-info btn-sm details" data-cy="entityDetailsButton">
                    <font-awesome-icon icon="eye"></font-awesome-icon>
                    <span class="d-none d-md-inline">View</span>
                  </button>
                </router-link>
                <router-link :to="{ name: 'SolutionEdit', params: { solutionId: solution.id } }" custom v-slot="{ navigate }">
                  <button @click="navigate" class="btn btn-primary btn-sm edit" data-cy="entityEditButton">
                    <font-awesome-icon icon="pencil-alt"></font-awesome-icon>
                    <span class="d-none d-md-inline">Edit</span>
                  </button>
                </router-link>
                <b-button
                  @click="prepareRemove(solution)"
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
        <span id="opportunitySolutionTreeApp.solution.delete.question" data-cy="solutionDeleteDialogHeading">Confirm delete operation</span>
      </template>
      <div class="modal-body">
        <p id="jhi-delete-solution-heading">Are you sure you want to delete Solution {{ removeId }}?</p>
      </div>
      <template #footer>
        <div>
          <button type="button" class="btn btn-secondary" @click="closeDialog()">Cancel</button>
          <button
            type="button"
            class="btn btn-primary"
            id="jhi-confirm-delete-solution"
            data-cy="entityConfirmDeleteButton"
            @click="removeSolution"
          >
            Delete
          </button>
        </div>
      </template>
    </b-modal>
    <div v-show="solutions?.length > 0">
      <div class="d-flex justify-content-center">
        <jhi-item-count :page="page" :total="queryCount" :items-per-page="itemsPerPage"></jhi-item-count>
      </div>
      <div class="d-flex justify-content-center">
        <b-pagination size="md" :total-rows="totalItems" v-model="page" :per-page="itemsPerPage"></b-pagination>
      </div>
    </div>
  </div>
</template>

<script lang="ts" src="./solution.component.ts"></script>
