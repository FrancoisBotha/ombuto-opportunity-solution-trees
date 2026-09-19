<template>
  <div>
    <h2 id="page-heading" data-cy="OpportunityHeading">
      <span id="opportunity">Opportunities</span>
      <div class="d-flex justify-content-end">
        <button class="btn btn-info me-2" @click="handleSyncList" :disabled="isFetching">
          <font-awesome-icon icon="sync" :spin="isFetching"></font-awesome-icon> <span>Refresh list</span>
        </button>
        <router-link :to="{ name: 'OpportunityCreate' }" custom v-slot="{ navigate }">
          <button
            @click="navigate"
            id="jh-create-entity"
            data-cy="entityCreateButton"
            class="btn btn-primary jh-create-entity create-opportunity"
          >
            <font-awesome-icon icon="plus"></font-awesome-icon>
            <span>Create a new Opportunity</span>
          </button>
        </router-link>
      </div>
    </h2>
    <br />
    <div class="alert alert-warning" v-if="!isFetching && opportunities?.length === 0">
      <span>No Opportunities found</span>
    </div>
    <div class="table-responsive" v-if="opportunities?.length > 0">
      <table class="table table-striped" aria-describedby="opportunities">
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
            <th scope="col" @click="changeOrder('valuerating')">
              <span>Valuerating</span>
              <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'valuerating'"></jhi-sort-indicator>
            </th>
            <th scope="col" @click="changeOrder('priority')">
              <span>Priority</span>
              <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'priority'"></jhi-sort-indicator>
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
            <th scope="col" @click="changeOrder('outcome.title')">
              <span>Outcome</span>
              <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'outcome.title'"></jhi-sort-indicator>
            </th>
            <th scope="col" @click="changeOrder('parent.title')">
              <span>Parent</span>
              <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'parent.title'"></jhi-sort-indicator>
            </th>
            <th scope="col" @click="changeOrder('owner.login')">
              <span>Owner</span>
              <jhi-sort-indicator :current-order="propOrder" :reverse="reverse" :field-name="'owner.login'"></jhi-sort-indicator>
            </th>
            <th scope="col"></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="opportunity in opportunities" :key="opportunity.id" data-cy="entityTable">
            <td>
              <router-link :to="{ name: 'OpportunityView', params: { opportunityId: opportunity.id } }">{{ opportunity.id }}</router-link>
            </td>
            <td>{{ opportunity.title }}</td>
            <td>{{ opportunity.description }}</td>
            <td>{{ opportunity.status }}</td>
            <td>{{ opportunity.valuerating }}</td>
            <td>{{ opportunity.priority }}</td>
            <td>{{ opportunity.sortOrder }}</td>
            <td>{{ formatDateShort(opportunity.createdDate) || '' }}</td>
            <td>{{ formatDateShort(opportunity.lastModifiedDate) || '' }}</td>
            <td>
              <div v-if="opportunity.outcome">
                <router-link :to="{ name: 'OutcomeView', params: { outcomeId: opportunity.outcome.id } }">{{
                  opportunity.outcome.title
                }}</router-link>
              </div>
            </td>
            <td>
              <div v-if="opportunity.parent">
                <router-link :to="{ name: 'OpportunityView', params: { opportunityId: opportunity.parent.id } }">{{
                  opportunity.parent.title
                }}</router-link>
              </div>
            </td>
            <td>
              {{ opportunity.owner ? opportunity.owner.login : '' }}
            </td>
            <td class="text-end">
              <div class="btn-group">
                <router-link :to="{ name: 'OpportunityView', params: { opportunityId: opportunity.id } }" custom v-slot="{ navigate }">
                  <button @click="navigate" class="btn btn-info btn-sm details" data-cy="entityDetailsButton">
                    <font-awesome-icon icon="eye"></font-awesome-icon>
                    <span class="d-none d-md-inline">View</span>
                  </button>
                </router-link>
                <router-link :to="{ name: 'OpportunityEdit', params: { opportunityId: opportunity.id } }" custom v-slot="{ navigate }">
                  <button @click="navigate" class="btn btn-primary btn-sm edit" data-cy="entityEditButton">
                    <font-awesome-icon icon="pencil-alt"></font-awesome-icon>
                    <span class="d-none d-md-inline">Edit</span>
                  </button>
                </router-link>
                <b-button
                  @click="prepareRemove(opportunity)"
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
        <span id="opportunitySolutionTreeApp.opportunity.delete.question" data-cy="opportunityDeleteDialogHeading"
          >Confirm delete operation</span
        >
      </template>
      <div class="modal-body">
        <p id="jhi-delete-opportunity-heading">Are you sure you want to delete Opportunity {{ removeId }}?</p>
      </div>
      <template #footer>
        <div>
          <button type="button" class="btn btn-secondary" @click="closeDialog()">Cancel</button>
          <button
            type="button"
            class="btn btn-primary"
            id="jhi-confirm-delete-opportunity"
            data-cy="entityConfirmDeleteButton"
            @click="removeOpportunity"
          >
            Delete
          </button>
        </div>
      </template>
    </b-modal>
    <div v-show="opportunities?.length > 0">
      <div class="d-flex justify-content-center">
        <jhi-item-count :page="page" :total="queryCount" :items-per-page="itemsPerPage"></jhi-item-count>
      </div>
      <div class="d-flex justify-content-center">
        <b-pagination size="md" :total-rows="totalItems" v-model="page" :per-page="itemsPerPage"></b-pagination>
      </div>
    </div>
  </div>
</template>

<script lang="ts" src="./opportunity.component.ts"></script>
