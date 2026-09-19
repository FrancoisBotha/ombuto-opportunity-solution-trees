<template>
  <div>
    <h2 id="page-heading" data-cy="CommentHeading">
      <span id="comment">Comments</span>
      <div class="d-flex justify-content-end">
        <button class="btn btn-info me-2" @click="handleSyncList" :disabled="isFetching">
          <font-awesome-icon icon="sync" :spin="isFetching"></font-awesome-icon> <span>Refresh list</span>
        </button>
        <router-link :to="{ name: 'CommentCreate' }" custom v-slot="{ navigate }">
          <button
            @click="navigate"
            id="jh-create-entity"
            data-cy="entityCreateButton"
            class="btn btn-primary jh-create-entity create-comment"
          >
            <font-awesome-icon icon="plus"></font-awesome-icon>
            <span>Create a new Comment</span>
          </button>
        </router-link>
      </div>
    </h2>
    <br />
    <div class="alert alert-warning" v-if="!isFetching && comments?.length === 0">
      <span>No Comments found</span>
    </div>
    <div class="table-responsive" v-if="comments?.length > 0">
      <table class="table table-striped" aria-describedby="comments">
        <thead>
          <tr>
            <th scope="col" @click="changeOrder('id')">
              <span>ID</span> <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'id'"></jhi-sort-indicator>
            </th>
            <th scope="col" @click="changeOrder('body')">
              <span>Body</span> <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'body'"></jhi-sort-indicator>
            </th>
            <th scope="col" @click="changeOrder('createdDate')">
              <span>Created Date</span>
              <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'createdDate'"></jhi-sort-indicator>
            </th>
            <th scope="col" @click="changeOrder('editedDate')">
              <span>Edited Date</span>
              <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'editedDate'"></jhi-sort-indicator>
            </th>
            <th scope="col" @click="changeOrder('author.login')">
              <span>Author</span>
              <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'author.login'"></jhi-sort-indicator>
            </th>
            <th scope="col" @click="changeOrder('outcome.title')">
              <span>Outcome</span>
              <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'outcome.title'"></jhi-sort-indicator>
            </th>
            <th scope="col" @click="changeOrder('opportunity.title')">
              <span>Opportunity</span>
              <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'opportunity.title'"></jhi-sort-indicator>
            </th>
            <th scope="col" @click="changeOrder('solution.title')">
              <span>Solution</span>
              <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'solution.title'"></jhi-sort-indicator>
            </th>
            <th scope="col" @click="changeOrder('assumption.statement')">
              <span>Assumption</span>
              <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'assumption.statement'"></jhi-sort-indicator>
            </th>
            <th scope="col" @click="changeOrder('evidence.title')">
              <span>Evidence</span>
              <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'evidence.title'"></jhi-sort-indicator>
            </th>
            <th scope="col"></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="comment in comments" :key="comment.id" data-cy="entityTable">
            <td>
              <router-link :to="{ name: 'CommentView', params: { commentId: comment.id } }">{{ comment.id }}</router-link>
            </td>
            <td>{{ comment.body }}</td>
            <td>{{ formatDateShort(comment.createdDate) || '' }}</td>
            <td>{{ formatDateShort(comment.editedDate) || '' }}</td>
            <td>
              {{ comment.author ? comment.author.login : '' }}
            </td>
            <td>
              <div v-if="comment.outcome">
                <router-link :to="{ name: 'OutcomeView', params: { outcomeId: comment.outcome.id } }">{{
                  comment.outcome.title
                }}</router-link>
              </div>
            </td>
            <td>
              <div v-if="comment.opportunity">
                <router-link :to="{ name: 'OpportunityView', params: { opportunityId: comment.opportunity.id } }">{{
                  comment.opportunity.title
                }}</router-link>
              </div>
            </td>
            <td>
              <div v-if="comment.solution">
                <router-link :to="{ name: 'SolutionView', params: { solutionId: comment.solution.id } }">{{
                  comment.solution.title
                }}</router-link>
              </div>
            </td>
            <td>
              <div v-if="comment.assumption">
                <router-link :to="{ name: 'AssumptionView', params: { assumptionId: comment.assumption.id } }">{{
                  comment.assumption.statement
                }}</router-link>
              </div>
            </td>
            <td>
              <div v-if="comment.evidence">
                <router-link :to="{ name: 'EvidenceView', params: { evidenceId: comment.evidence.id } }">{{
                  comment.evidence.title
                }}</router-link>
              </div>
            </td>
            <td class="text-end">
              <div class="btn-group">
                <router-link :to="{ name: 'CommentView', params: { commentId: comment.id } }" custom v-slot="{ navigate }">
                  <button @click="navigate" class="btn btn-info btn-sm details" data-cy="entityDetailsButton">
                    <font-awesome-icon icon="eye"></font-awesome-icon>
                    <span class="d-none d-md-inline">View</span>
                  </button>
                </router-link>
                <router-link :to="{ name: 'CommentEdit', params: { commentId: comment.id } }" custom v-slot="{ navigate }">
                  <button @click="navigate" class="btn btn-primary btn-sm edit" data-cy="entityEditButton">
                    <font-awesome-icon icon="pencil-alt"></font-awesome-icon>
                    <span class="d-none d-md-inline">Edit</span>
                  </button>
                </router-link>
                <b-button
                  @click="prepareRemove(comment)"
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
        <span id="opportunitySolutionTreeApp.comment.delete.question" data-cy="commentDeleteDialogHeading">Confirm delete operation</span>
      </template>
      <div class="modal-body">
        <p id="jhi-delete-comment-heading">Are you sure you want to delete Comment {{ removeId }}?</p>
      </div>
      <template #footer>
        <div>
          <button type="button" class="btn btn-secondary" @click="closeDialog()">Cancel</button>
          <button
            type="button"
            class="btn btn-primary"
            id="jhi-confirm-delete-comment"
            data-cy="entityConfirmDeleteButton"
            @click="removeComment"
          >
            Delete
          </button>
        </div>
      </template>
    </b-modal>
    <div v-show="comments?.length > 0">
      <div class="d-flex justify-content-center">
        <jhi-item-count :page="page" :total="queryCount" :items-per-page="itemsPerPage"></jhi-item-count>
      </div>
      <div class="d-flex justify-content-center">
        <b-pagination size="md" :total-rows="totalItems" v-model="page" :per-page="itemsPerPage"></b-pagination>
      </div>
    </div>
  </div>
</template>

<script lang="ts" src="./comment.component.ts"></script>
