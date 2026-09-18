<template>
  <div>
    <h2 id="page-heading" data-cy="ExperimentHeading">
      <span id="experiment">Experiments</span>
      <div class="d-flex justify-content-end">
        <button class="btn btn-info me-2" @click="handleSyncList" :disabled="isFetching">
          <font-awesome-icon icon="sync" :spin="isFetching"></font-awesome-icon> <span>Refresh list</span>
        </button>
        <router-link :to="{ name: 'ExperimentCreate' }" custom v-slot="{ navigate }">
          <button
            @click="navigate"
            id="jh-create-entity"
            data-cy="entityCreateButton"
            class="btn btn-primary jh-create-entity create-experiment"
          >
            <font-awesome-icon icon="plus"></font-awesome-icon>
            <span>Create a new Experiment</span>
          </button>
        </router-link>
      </div>
    </h2>
    <br />
    <div class="alert alert-warning" v-if="!isFetching && experiments?.length === 0">
      <span>No Experiments found</span>
    </div>
    <div class="table-responsive" v-if="experiments?.length > 0">
      <table class="table table-striped" aria-describedby="experiments">
        <thead>
          <tr>
            <th scope="col" @click="changeOrder('id')">
              <span>ID</span> <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'id'"></jhi-sort-indicator>
            </th>
            <th scope="col" @click="changeOrder('title')">
              <span>Title</span>
              <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'title'"></jhi-sort-indicator>
            </th>
            <th scope="col" @click="changeOrder('hypothesis')">
              <span>Hypothesis</span>
              <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'hypothesis'"></jhi-sort-indicator>
            </th>
            <th scope="col" @click="changeOrder('method')">
              <span>Method</span>
              <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'method'"></jhi-sort-indicator>
            </th>
            <th scope="col" @click="changeOrder('successCriteria')">
              <span>Success Criteria</span>
              <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'successCriteria'"></jhi-sort-indicator>
            </th>
            <th scope="col" @click="changeOrder('status')">
              <span>Status</span>
              <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'status'"></jhi-sort-indicator>
            </th>
            <th scope="col" @click="changeOrder('result')">
              <span>Result</span>
              <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'result'"></jhi-sort-indicator>
            </th>
            <th scope="col" @click="changeOrder('learnings')">
              <span>Learnings</span>
              <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'learnings'"></jhi-sort-indicator>
            </th>
            <th scope="col" @click="changeOrder('startDate')">
              <span>Start Date</span>
              <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'startDate'"></jhi-sort-indicator>
            </th>
            <th scope="col" @click="changeOrder('endDate')">
              <span>End Date</span>
              <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'endDate'"></jhi-sort-indicator>
            </th>
            <th scope="col" @click="changeOrder('createdDate')">
              <span>Created Date</span>
              <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'createdDate'"></jhi-sort-indicator>
            </th>
            <th scope="col" @click="changeOrder('solution.title')">
              <span>Solution</span>
              <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'solution.title'"></jhi-sort-indicator>
            </th>
            <th scope="col"></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="experiment in experiments" :key="experiment.id" data-cy="entityTable">
            <td>
              <router-link :to="{ name: 'ExperimentView', params: { experimentId: experiment.id } }">{{ experiment.id }}</router-link>
            </td>
            <td>{{ experiment.title }}</td>
            <td>{{ experiment.hypothesis }}</td>
            <td>{{ experiment.method }}</td>
            <td>{{ experiment.successCriteria }}</td>
            <td>{{ experiment.status }}</td>
            <td>{{ experiment.result }}</td>
            <td>{{ experiment.learnings }}</td>
            <td>{{ experiment.startDate }}</td>
            <td>{{ experiment.endDate }}</td>
            <td>{{ formatDateShort(experiment.createdDate) || '' }}</td>
            <td>
              <div v-if="experiment.solution">
                <router-link :to="{ name: 'SolutionView', params: { solutionId: experiment.solution.id } }">{{
                  experiment.solution.title
                }}</router-link>
              </div>
            </td>
            <td class="text-end">
              <div class="btn-group">
                <router-link :to="{ name: 'ExperimentView', params: { experimentId: experiment.id } }" custom v-slot="{ navigate }">
                  <button @click="navigate" class="btn btn-info btn-sm details" data-cy="entityDetailsButton">
                    <font-awesome-icon icon="eye"></font-awesome-icon>
                    <span class="d-none d-md-inline">View</span>
                  </button>
                </router-link>
                <router-link :to="{ name: 'ExperimentEdit', params: { experimentId: experiment.id } }" custom v-slot="{ navigate }">
                  <button @click="navigate" class="btn btn-primary btn-sm edit" data-cy="entityEditButton">
                    <font-awesome-icon icon="pencil-alt"></font-awesome-icon>
                    <span class="d-none d-md-inline">Edit</span>
                  </button>
                </router-link>
                <b-button
                  @click="prepareRemove(experiment)"
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
        <span id="opportunitySolutionTreeApp.experiment.delete.question" data-cy="experimentDeleteDialogHeading"
          >Confirm delete operation</span
        >
      </template>
      <div class="modal-body">
        <p id="jhi-delete-experiment-heading">Are you sure you want to delete Experiment {{ removeId }}?</p>
      </div>
      <template #footer>
        <div>
          <button type="button" class="btn btn-secondary" @click="closeDialog()">Cancel</button>
          <button
            type="button"
            class="btn btn-primary"
            id="jhi-confirm-delete-experiment"
            data-cy="entityConfirmDeleteButton"
            @click="removeExperiment"
          >
            Delete
          </button>
        </div>
      </template>
    </b-modal>
    <div v-show="experiments?.length > 0">
      <div class="d-flex justify-content-center">
        <jhi-item-count :page="page" :total="queryCount" :items-per-page="itemsPerPage"></jhi-item-count>
      </div>
      <div class="d-flex justify-content-center">
        <b-pagination size="md" :total-rows="totalItems" v-model="page" :per-page="itemsPerPage"></b-pagination>
      </div>
    </div>
  </div>
</template>

<script lang="ts" src="./experiment.component.ts"></script>
