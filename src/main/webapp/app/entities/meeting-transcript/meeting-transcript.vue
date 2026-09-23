<template>
  <div>
    <h2 id="page-heading" data-cy="MeetingTranscriptHeading">
      <span id="meeting-transcript">Meeting Transcripts</span>
      <div class="d-flex justify-content-end">
        <button class="btn btn-info me-2" @click="handleSyncList" :disabled="isFetching">
          <font-awesome-icon icon="sync" :spin="isFetching"></font-awesome-icon> <span>Refresh list</span>
        </button>
        <router-link :to="{ name: 'MeetingTranscriptCreate' }" custom v-slot="{ navigate }">
          <button
            @click="navigate"
            id="jh-create-entity"
            data-cy="entityCreateButton"
            class="btn btn-primary jh-create-entity create-meeting-transcript"
          >
            <font-awesome-icon icon="plus"></font-awesome-icon>
            <span>Create a new Meeting Transcript</span>
          </button>
        </router-link>
      </div>
    </h2>
    <br />
    <div class="alert alert-warning" v-if="!isFetching && meetingTranscripts?.length === 0">
      <span>No Meeting Transcripts found</span>
    </div>
    <div class="table-responsive" v-if="meetingTranscripts?.length > 0">
      <table class="table table-striped" aria-describedby="meetingTranscripts">
        <thead>
          <tr>
            <th scope="col" @click="changeOrder('id')">
              <span>ID</span> <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'id'"></jhi-sort-indicator>
            </th>
            <th scope="col" @click="changeOrder('title')">
              <span>Title</span>
              <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'title'"></jhi-sort-indicator>
            </th>
            <th scope="col" @click="changeOrder('meetingDate')">
              <span>Meeting Date</span>
              <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'meetingDate'"></jhi-sort-indicator>
            </th>
            <th scope="col" @click="changeOrder('attendees')">
              <span>Attendees</span>
              <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'attendees'"></jhi-sort-indicator>
            </th>
            <th scope="col" @click="changeOrder('body')">
              <span>Body</span> <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'body'"></jhi-sort-indicator>
            </th>
            <th scope="col" @click="changeOrder('source')">
              <span>Source</span>
              <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'source'"></jhi-sort-indicator>
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
            <th scope="col" @click="changeOrder('product.name')">
              <span>Product</span>
              <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'product.name'"></jhi-sort-indicator>
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
          <tr v-for="meetingTranscript in meetingTranscripts" :key="meetingTranscript.id" data-cy="entityTable">
            <td>
              <router-link :to="{ name: 'MeetingTranscriptView', params: { meetingTranscriptId: meetingTranscript.id } }">{{
                meetingTranscript.id
              }}</router-link>
            </td>
            <td>{{ meetingTranscript.title }}</td>
            <td>{{ meetingTranscript.meetingDate }}</td>
            <td>{{ meetingTranscript.attendees }}</td>
            <td>{{ meetingTranscript.body }}</td>
            <td>{{ meetingTranscript.source }}</td>
            <td>{{ formatDateShort(meetingTranscript.createdDate) || '' }}</td>
            <td>{{ formatDateShort(meetingTranscript.editedDate) || '' }}</td>
            <td>
              {{ meetingTranscript.author ? meetingTranscript.author.login : '' }}
            </td>
            <td>
              <div v-if="meetingTranscript.product">
                <router-link :to="{ name: 'ProductView', params: { productId: meetingTranscript.product.id } }">{{
                  meetingTranscript.product.name
                }}</router-link>
              </div>
            </td>
            <td>
              <div v-if="meetingTranscript.outcome">
                <router-link :to="{ name: 'OutcomeView', params: { outcomeId: meetingTranscript.outcome.id } }">{{
                  meetingTranscript.outcome.title
                }}</router-link>
              </div>
            </td>
            <td>
              <div v-if="meetingTranscript.opportunity">
                <router-link :to="{ name: 'OpportunityView', params: { opportunityId: meetingTranscript.opportunity.id } }">{{
                  meetingTranscript.opportunity.title
                }}</router-link>
              </div>
            </td>
            <td>
              <div v-if="meetingTranscript.solution">
                <router-link :to="{ name: 'SolutionView', params: { solutionId: meetingTranscript.solution.id } }">{{
                  meetingTranscript.solution.title
                }}</router-link>
              </div>
            </td>
            <td>
              <div v-if="meetingTranscript.assumption">
                <router-link :to="{ name: 'AssumptionView', params: { assumptionId: meetingTranscript.assumption.id } }">{{
                  meetingTranscript.assumption.statement
                }}</router-link>
              </div>
            </td>
            <td>
              <div v-if="meetingTranscript.evidence">
                <router-link :to="{ name: 'EvidenceView', params: { evidenceId: meetingTranscript.evidence.id } }">{{
                  meetingTranscript.evidence.title
                }}</router-link>
              </div>
            </td>
            <td class="text-end">
              <div class="btn-group">
                <router-link
                  :to="{ name: 'MeetingTranscriptView', params: { meetingTranscriptId: meetingTranscript.id } }"
                  custom
                  v-slot="{ navigate }"
                >
                  <button @click="navigate" class="btn btn-info btn-sm details" data-cy="entityDetailsButton">
                    <font-awesome-icon icon="eye"></font-awesome-icon>
                    <span class="d-none d-md-inline">View</span>
                  </button>
                </router-link>
                <router-link
                  :to="{ name: 'MeetingTranscriptEdit', params: { meetingTranscriptId: meetingTranscript.id } }"
                  custom
                  v-slot="{ navigate }"
                >
                  <button @click="navigate" class="btn btn-primary btn-sm edit" data-cy="entityEditButton">
                    <font-awesome-icon icon="pencil-alt"></font-awesome-icon>
                    <span class="d-none d-md-inline">Edit</span>
                  </button>
                </router-link>
                <b-button
                  @click="prepareRemove(meetingTranscript)"
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
        <span id="opportunitySolutionTreeApp.meetingTranscript.delete.question" data-cy="meetingTranscriptDeleteDialogHeading"
          >Confirm delete operation</span
        >
      </template>
      <div class="modal-body">
        <p id="jhi-delete-meetingTranscript-heading">Are you sure you want to delete Meeting Transcript {{ removeId }}?</p>
      </div>
      <template #footer>
        <div>
          <button type="button" class="btn btn-secondary" @click="closeDialog()">Cancel</button>
          <button
            type="button"
            class="btn btn-primary"
            id="jhi-confirm-delete-meetingTranscript"
            data-cy="entityConfirmDeleteButton"
            @click="removeMeetingTranscript"
          >
            Delete
          </button>
        </div>
      </template>
    </b-modal>
    <div v-show="meetingTranscripts?.length > 0">
      <div class="d-flex justify-content-center">
        <jhi-item-count :page="page" :total="queryCount" :items-per-page="itemsPerPage"></jhi-item-count>
      </div>
      <div class="d-flex justify-content-center">
        <b-pagination size="md" :total-rows="totalItems" v-model="page" :per-page="itemsPerPage"></b-pagination>
      </div>
    </div>
  </div>
</template>

<script lang="ts" src="./meeting-transcript.component.ts"></script>
