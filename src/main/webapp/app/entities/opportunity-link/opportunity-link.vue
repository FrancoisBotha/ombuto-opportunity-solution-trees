<template>
  <div>
    <h2 id="page-heading" data-cy="OpportunityLinkHeading">
      <span id="opportunity-link">Opportunity Links</span>
      <div class="d-flex justify-content-end">
        <button class="btn btn-info me-2" @click="handleSyncList" :disabled="isFetching">
          <font-awesome-icon icon="sync" :spin="isFetching"></font-awesome-icon> <span>Refresh list</span>
        </button>
        <router-link :to="{ name: 'OpportunityLinkCreate' }" custom v-slot="{ navigate }">
          <button
            @click="navigate"
            id="jh-create-entity"
            data-cy="entityCreateButton"
            class="btn btn-primary jh-create-entity create-opportunity-link"
          >
            <font-awesome-icon icon="plus"></font-awesome-icon>
            <span>Create a new Opportunity Link</span>
          </button>
        </router-link>
      </div>
    </h2>
    <br />
    <div class="alert alert-warning" v-if="!isFetching && opportunityLinks?.length === 0">
      <span>No Opportunity Links found</span>
    </div>
    <div class="table-responsive" v-if="opportunityLinks?.length > 0">
      <table class="table table-striped" aria-describedby="opportunityLinks">
        <thead>
          <tr>
            <th scope="col"><span>ID</span></th>
            <th scope="col"><span>Name</span></th>
            <th scope="col"><span>Url</span></th>
            <th scope="col"><span>Type</span></th>
            <th scope="col"><span>Sort Order</span></th>
            <th scope="col"><span>Opportunity</span></th>
            <th scope="col"></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="opportunityLink in opportunityLinks" :key="opportunityLink.id" data-cy="entityTable">
            <td>
              <router-link :to="{ name: 'OpportunityLinkView', params: { opportunityLinkId: opportunityLink.id } }">{{
                opportunityLink.id
              }}</router-link>
            </td>
            <td>{{ opportunityLink.name }}</td>
            <td>{{ opportunityLink.url }}</td>
            <td>{{ opportunityLink.type }}</td>
            <td>{{ opportunityLink.sortOrder }}</td>
            <td>
              <div v-if="opportunityLink.opportunity">
                <router-link :to="{ name: 'OpportunityView', params: { opportunityId: opportunityLink.opportunity.id } }">{{
                  opportunityLink.opportunity.title
                }}</router-link>
              </div>
            </td>
            <td class="text-end">
              <div class="btn-group">
                <router-link
                  :to="{ name: 'OpportunityLinkView', params: { opportunityLinkId: opportunityLink.id } }"
                  custom
                  v-slot="{ navigate }"
                >
                  <button @click="navigate" class="btn btn-info btn-sm details" data-cy="entityDetailsButton">
                    <font-awesome-icon icon="eye"></font-awesome-icon>
                    <span class="d-none d-md-inline">View</span>
                  </button>
                </router-link>
                <router-link
                  :to="{ name: 'OpportunityLinkEdit', params: { opportunityLinkId: opportunityLink.id } }"
                  custom
                  v-slot="{ navigate }"
                >
                  <button @click="navigate" class="btn btn-primary btn-sm edit" data-cy="entityEditButton">
                    <font-awesome-icon icon="pencil-alt"></font-awesome-icon>
                    <span class="d-none d-md-inline">Edit</span>
                  </button>
                </router-link>
                <b-button
                  @click="prepareRemove(opportunityLink)"
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
        <span id="opportunitySolutionTreeApp.opportunityLink.delete.question" data-cy="opportunityLinkDeleteDialogHeading"
          >Confirm delete operation</span
        >
      </template>
      <div class="modal-body">
        <p id="jhi-delete-opportunityLink-heading">Are you sure you want to delete Opportunity Link {{ removeId }}?</p>
      </div>
      <template #footer>
        <div>
          <button type="button" class="btn btn-secondary" @click="closeDialog()">Cancel</button>
          <button
            type="button"
            class="btn btn-primary"
            id="jhi-confirm-delete-opportunityLink"
            data-cy="entityConfirmDeleteButton"
            @click="removeOpportunityLink"
          >
            Delete
          </button>
        </div>
      </template>
    </b-modal>
  </div>
</template>

<script lang="ts" src="./opportunity-link.component.ts"></script>
