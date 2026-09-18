<template>
  <div>
    <h2 id="page-heading" data-cy="InterviewHeading">
      <span id="interview">Interviews</span>
      <div class="d-flex justify-content-end">
        <button class="btn btn-info me-2" @click="handleSyncList" :disabled="isFetching">
          <font-awesome-icon icon="sync" :spin="isFetching"></font-awesome-icon> <span>Refresh list</span>
        </button>
        <router-link :to="{ name: 'InterviewCreate' }" custom v-slot="{ navigate }">
          <button
            @click="navigate"
            id="jh-create-entity"
            data-cy="entityCreateButton"
            class="btn btn-primary jh-create-entity create-interview"
          >
            <font-awesome-icon icon="plus"></font-awesome-icon>
            <span>Create a new Interview</span>
          </button>
        </router-link>
      </div>
    </h2>
    <br />
    <div class="alert alert-warning" v-if="!isFetching && interviews?.length === 0">
      <span>No Interviews found</span>
    </div>
    <div class="table-responsive" v-if="interviews?.length > 0">
      <table class="table table-striped" aria-describedby="interviews">
        <thead>
          <tr>
            <th scope="col" @click="changeOrder('id')">
              <span>ID</span> <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'id'"></jhi-sort-indicator>
            </th>
            <th scope="col" @click="changeOrder('title')">
              <span>Title</span>
              <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'title'"></jhi-sort-indicator>
            </th>
            <th scope="col" @click="changeOrder('participant')">
              <span>Participant</span>
              <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'participant'"></jhi-sort-indicator>
            </th>
            <th scope="col" @click="changeOrder('interviewDate')">
              <span>Interview Date</span>
              <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'interviewDate'"></jhi-sort-indicator>
            </th>
            <th scope="col" @click="changeOrder('notes')">
              <span>Notes</span>
              <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'notes'"></jhi-sort-indicator>
            </th>
            <th scope="col" @click="changeOrder('recordingUrl')">
              <span>Recording Url</span>
              <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'recordingUrl'"></jhi-sort-indicator>
            </th>
            <th scope="col" @click="changeOrder('createdDate')">
              <span>Created Date</span>
              <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'createdDate'"></jhi-sort-indicator>
            </th>
            <th scope="col" @click="changeOrder('product.name')">
              <span>Product</span>
              <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'product.name'"></jhi-sort-indicator>
            </th>
            <th scope="col" @click="changeOrder('interviewer.login')">
              <span>Interviewer</span>
              <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'interviewer.login'"></jhi-sort-indicator>
            </th>
            <th scope="col"></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="interview in interviews" :key="interview.id" data-cy="entityTable">
            <td>
              <router-link :to="{ name: 'InterviewView', params: { interviewId: interview.id } }">{{ interview.id }}</router-link>
            </td>
            <td>{{ interview.title }}</td>
            <td>{{ interview.participant }}</td>
            <td>{{ interview.interviewDate }}</td>
            <td>{{ interview.notes }}</td>
            <td>{{ interview.recordingUrl }}</td>
            <td>{{ formatDateShort(interview.createdDate) || '' }}</td>
            <td>
              <div v-if="interview.product">
                <router-link :to="{ name: 'ProductView', params: { productId: interview.product.id } }">{{
                  interview.product.name
                }}</router-link>
              </div>
            </td>
            <td>
              {{ interview.interviewer ? interview.interviewer.login : '' }}
            </td>
            <td class="text-end">
              <div class="btn-group">
                <router-link :to="{ name: 'InterviewView', params: { interviewId: interview.id } }" custom v-slot="{ navigate }">
                  <button @click="navigate" class="btn btn-info btn-sm details" data-cy="entityDetailsButton">
                    <font-awesome-icon icon="eye"></font-awesome-icon>
                    <span class="d-none d-md-inline">View</span>
                  </button>
                </router-link>
                <router-link :to="{ name: 'InterviewEdit', params: { interviewId: interview.id } }" custom v-slot="{ navigate }">
                  <button @click="navigate" class="btn btn-primary btn-sm edit" data-cy="entityEditButton">
                    <font-awesome-icon icon="pencil-alt"></font-awesome-icon>
                    <span class="d-none d-md-inline">Edit</span>
                  </button>
                </router-link>
                <b-button
                  @click="prepareRemove(interview)"
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
        <span id="opportunitySolutionTreeApp.interview.delete.question" data-cy="interviewDeleteDialogHeading"
          >Confirm delete operation</span
        >
      </template>
      <div class="modal-body">
        <p id="jhi-delete-interview-heading">Are you sure you want to delete Interview {{ removeId }}?</p>
      </div>
      <template #footer>
        <div>
          <button type="button" class="btn btn-secondary" @click="closeDialog()">Cancel</button>
          <button
            type="button"
            class="btn btn-primary"
            id="jhi-confirm-delete-interview"
            data-cy="entityConfirmDeleteButton"
            @click="removeInterview"
          >
            Delete
          </button>
        </div>
      </template>
    </b-modal>
    <div v-show="interviews?.length > 0">
      <div class="d-flex justify-content-center">
        <jhi-item-count :page="page" :total="queryCount" :items-per-page="itemsPerPage"></jhi-item-count>
      </div>
      <div class="d-flex justify-content-center">
        <b-pagination size="md" :total-rows="totalItems" v-model="page" :per-page="itemsPerPage"></b-pagination>
      </div>
    </div>
  </div>
</template>

<script lang="ts" src="./interview.component.ts"></script>
