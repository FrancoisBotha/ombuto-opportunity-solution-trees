<template>
  <div>
    <h2 id="page-heading" data-cy="OpenQuestionHeading">
      <span id="open-question">Open Questions</span>
      <div class="d-flex justify-content-end">
        <button class="btn btn-info me-2" @click="handleSyncList" :disabled="isFetching">
          <font-awesome-icon icon="sync" :spin="isFetching"></font-awesome-icon> <span>Refresh list</span>
        </button>
        <router-link :to="{ name: 'OpenQuestionCreate' }" custom v-slot="{ navigate }">
          <button
            @click="navigate"
            id="jh-create-entity"
            data-cy="entityCreateButton"
            class="btn btn-primary jh-create-entity create-open-question"
          >
            <font-awesome-icon icon="plus"></font-awesome-icon>
            <span>Create a new Open Question</span>
          </button>
        </router-link>
      </div>
    </h2>
    <br />
    <div class="alert alert-warning" v-if="!isFetching && openQuestions?.length === 0">
      <span>No Open Questions found</span>
    </div>
    <div class="table-responsive" v-if="openQuestions?.length > 0">
      <table class="table table-striped" aria-describedby="openQuestions">
        <thead>
          <tr>
            <th scope="col"><span>ID</span></th>
            <th scope="col"><span>Question Text</span></th>
            <th scope="col"><span>Done</span></th>
            <th scope="col"><span>Sort Order</span></th>
            <th scope="col"><span>Created Date</span></th>
            <th scope="col"><span>Opportunity</span></th>
            <th scope="col"></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="openQuestion in openQuestions" :key="openQuestion.id" data-cy="entityTable">
            <td>
              <router-link :to="{ name: 'OpenQuestionView', params: { openQuestionId: openQuestion.id } }">{{
                openQuestion.id
              }}</router-link>
            </td>
            <td>{{ openQuestion.questionText }}</td>
            <td>{{ openQuestion.done }}</td>
            <td>{{ openQuestion.sortOrder }}</td>
            <td>{{ formatDateShort(openQuestion.createdDate) || '' }}</td>
            <td>
              <div v-if="openQuestion.opportunity">
                <router-link :to="{ name: 'OpportunityView', params: { opportunityId: openQuestion.opportunity.id } }">{{
                  openQuestion.opportunity.title
                }}</router-link>
              </div>
            </td>
            <td class="text-end">
              <div class="btn-group">
                <router-link :to="{ name: 'OpenQuestionView', params: { openQuestionId: openQuestion.id } }" custom v-slot="{ navigate }">
                  <button @click="navigate" class="btn btn-info btn-sm details" data-cy="entityDetailsButton">
                    <font-awesome-icon icon="eye"></font-awesome-icon>
                    <span class="d-none d-md-inline">View</span>
                  </button>
                </router-link>
                <router-link :to="{ name: 'OpenQuestionEdit', params: { openQuestionId: openQuestion.id } }" custom v-slot="{ navigate }">
                  <button @click="navigate" class="btn btn-primary btn-sm edit" data-cy="entityEditButton">
                    <font-awesome-icon icon="pencil-alt"></font-awesome-icon>
                    <span class="d-none d-md-inline">Edit</span>
                  </button>
                </router-link>
                <b-button
                  @click="prepareRemove(openQuestion)"
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
        <span id="opportunitySolutionTreeApp.openQuestion.delete.question" data-cy="openQuestionDeleteDialogHeading"
          >Confirm delete operation</span
        >
      </template>
      <div class="modal-body">
        <p id="jhi-delete-openQuestion-heading">Are you sure you want to delete Open Question {{ removeId }}?</p>
      </div>
      <template #footer>
        <div>
          <button type="button" class="btn btn-secondary" @click="closeDialog()">Cancel</button>
          <button
            type="button"
            class="btn btn-primary"
            id="jhi-confirm-delete-openQuestion"
            data-cy="entityConfirmDeleteButton"
            @click="removeOpenQuestion"
          >
            Delete
          </button>
        </div>
      </template>
    </b-modal>
  </div>
</template>

<script lang="ts" src="./open-question.component.ts"></script>
