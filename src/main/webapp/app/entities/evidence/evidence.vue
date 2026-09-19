<template>
  <div>
    <h2 id="page-heading" data-cy="EvidenceHeading">
      <span id="evidence">Evidences</span>
      <div class="d-flex justify-content-end">
        <button class="btn btn-info me-2" @click="handleSyncList" :disabled="isFetching">
          <font-awesome-icon icon="sync" :spin="isFetching"></font-awesome-icon> <span>Refresh list</span>
        </button>
        <router-link :to="{ name: 'EvidenceCreate' }" custom v-slot="{ navigate }">
          <button
            @click="navigate"
            id="jh-create-entity"
            data-cy="entityCreateButton"
            class="btn btn-primary jh-create-entity create-evidence"
          >
            <font-awesome-icon icon="plus"></font-awesome-icon>
            <span>Create a new Evidence</span>
          </button>
        </router-link>
      </div>
    </h2>
    <br />
    <div class="alert alert-warning" v-if="!isFetching && evidences?.length === 0">
      <span>No Evidences found</span>
    </div>
    <div class="table-responsive" v-if="evidences?.length > 0">
      <table class="table table-striped" aria-describedby="evidences">
        <thead>
          <tr>
            <th scope="col"><span>ID</span></th>
            <th scope="col"><span>Title</span></th>
            <th scope="col"><span>Description</span></th>
            <th scope="col"><span>Sort Order</span></th>
            <th scope="col"><span>Created Date</span></th>
            <th scope="col"><span>Last Modified Date</span></th>
            <th scope="col"><span>Opportunity</span></th>
            <th scope="col"><span>Assumption</span></th>
            <th scope="col"></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="evidence in evidences" :key="evidence.id" data-cy="entityTable">
            <td>
              <router-link :to="{ name: 'EvidenceView', params: { evidenceId: evidence.id } }">{{ evidence.id }}</router-link>
            </td>
            <td>{{ evidence.title }}</td>
            <td>{{ evidence.description }}</td>
            <td>{{ evidence.sortOrder }}</td>
            <td>{{ formatDateShort(evidence.createdDate) || '' }}</td>
            <td>{{ formatDateShort(evidence.lastModifiedDate) || '' }}</td>
            <td>
              <div v-if="evidence.opportunity">
                <router-link :to="{ name: 'OpportunityView', params: { opportunityId: evidence.opportunity.id } }">{{
                  evidence.opportunity.title
                }}</router-link>
              </div>
            </td>
            <td>
              <div v-if="evidence.assumption">
                <router-link :to="{ name: 'AssumptionView', params: { assumptionId: evidence.assumption.id } }">{{
                  evidence.assumption.statement
                }}</router-link>
              </div>
            </td>
            <td class="text-end">
              <div class="btn-group">
                <router-link :to="{ name: 'EvidenceView', params: { evidenceId: evidence.id } }" custom v-slot="{ navigate }">
                  <button @click="navigate" class="btn btn-info btn-sm details" data-cy="entityDetailsButton">
                    <font-awesome-icon icon="eye"></font-awesome-icon>
                    <span class="d-none d-md-inline">View</span>
                  </button>
                </router-link>
                <router-link :to="{ name: 'EvidenceEdit', params: { evidenceId: evidence.id } }" custom v-slot="{ navigate }">
                  <button @click="navigate" class="btn btn-primary btn-sm edit" data-cy="entityEditButton">
                    <font-awesome-icon icon="pencil-alt"></font-awesome-icon>
                    <span class="d-none d-md-inline">Edit</span>
                  </button>
                </router-link>
                <b-button
                  @click="prepareRemove(evidence)"
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
        <span id="opportunitySolutionTreeApp.evidence.delete.question" data-cy="evidenceDeleteDialogHeading">Confirm delete operation</span>
      </template>
      <div class="modal-body">
        <p id="jhi-delete-evidence-heading">Are you sure you want to delete Evidence {{ removeId }}?</p>
      </div>
      <template #footer>
        <div>
          <button type="button" class="btn btn-secondary" @click="closeDialog()">Cancel</button>
          <button
            type="button"
            class="btn btn-primary"
            id="jhi-confirm-delete-evidence"
            data-cy="entityConfirmDeleteButton"
            @click="removeEvidence"
          >
            Delete
          </button>
        </div>
      </template>
    </b-modal>
  </div>
</template>

<script lang="ts" src="./evidence.component.ts"></script>
