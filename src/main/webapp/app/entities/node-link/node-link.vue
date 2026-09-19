<template>
  <div>
    <h2 id="page-heading" data-cy="NodeLinkHeading">
      <span id="node-link">Node Links</span>
      <div class="d-flex justify-content-end">
        <button class="btn btn-info me-2" @click="handleSyncList" :disabled="isFetching">
          <font-awesome-icon icon="sync" :spin="isFetching"></font-awesome-icon> <span>Refresh list</span>
        </button>
        <router-link :to="{ name: 'NodeLinkCreate' }" custom v-slot="{ navigate }">
          <button
            @click="navigate"
            id="jh-create-entity"
            data-cy="entityCreateButton"
            class="btn btn-primary jh-create-entity create-node-link"
          >
            <font-awesome-icon icon="plus"></font-awesome-icon>
            <span>Create a new Node Link</span>
          </button>
        </router-link>
      </div>
    </h2>
    <br />
    <div class="alert alert-warning" v-if="!isFetching && nodeLinks?.length === 0">
      <span>No Node Links found</span>
    </div>
    <div class="table-responsive" v-if="nodeLinks?.length > 0">
      <table class="table table-striped" aria-describedby="nodeLinks">
        <thead>
          <tr>
            <th scope="col"><span>ID</span></th>
            <th scope="col"><span>Name</span></th>
            <th scope="col"><span>Url</span></th>
            <th scope="col"><span>Sort Order</span></th>
            <th scope="col"><span>Created Date</span></th>
            <th scope="col"><span>Product</span></th>
            <th scope="col"><span>Outcome</span></th>
            <th scope="col"><span>Opportunity</span></th>
            <th scope="col"><span>Solution</span></th>
            <th scope="col"><span>Assumption</span></th>
            <th scope="col"><span>Evidence</span></th>
            <th scope="col"></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="nodeLink in nodeLinks" :key="nodeLink.id" data-cy="entityTable">
            <td>
              <router-link :to="{ name: 'NodeLinkView', params: { nodeLinkId: nodeLink.id } }">{{ nodeLink.id }}</router-link>
            </td>
            <td>{{ nodeLink.name }}</td>
            <td>{{ nodeLink.url }}</td>
            <td>{{ nodeLink.sortOrder }}</td>
            <td>{{ formatDateShort(nodeLink.createdDate) || '' }}</td>
            <td>
              <div v-if="nodeLink.product">
                <router-link :to="{ name: 'ProductView', params: { productId: nodeLink.product.id } }">{{
                  nodeLink.product.name
                }}</router-link>
              </div>
            </td>
            <td>
              <div v-if="nodeLink.outcome">
                <router-link :to="{ name: 'OutcomeView', params: { outcomeId: nodeLink.outcome.id } }">{{
                  nodeLink.outcome.title
                }}</router-link>
              </div>
            </td>
            <td>
              <div v-if="nodeLink.opportunity">
                <router-link :to="{ name: 'OpportunityView', params: { opportunityId: nodeLink.opportunity.id } }">{{
                  nodeLink.opportunity.title
                }}</router-link>
              </div>
            </td>
            <td>
              <div v-if="nodeLink.solution">
                <router-link :to="{ name: 'SolutionView', params: { solutionId: nodeLink.solution.id } }">{{
                  nodeLink.solution.title
                }}</router-link>
              </div>
            </td>
            <td>
              <div v-if="nodeLink.assumption">
                <router-link :to="{ name: 'AssumptionView', params: { assumptionId: nodeLink.assumption.id } }">{{
                  nodeLink.assumption.statement
                }}</router-link>
              </div>
            </td>
            <td>
              <div v-if="nodeLink.evidence">
                <router-link :to="{ name: 'EvidenceView', params: { evidenceId: nodeLink.evidence.id } }">{{
                  nodeLink.evidence.title
                }}</router-link>
              </div>
            </td>
            <td class="text-end">
              <div class="btn-group">
                <router-link :to="{ name: 'NodeLinkView', params: { nodeLinkId: nodeLink.id } }" custom v-slot="{ navigate }">
                  <button @click="navigate" class="btn btn-info btn-sm details" data-cy="entityDetailsButton">
                    <font-awesome-icon icon="eye"></font-awesome-icon>
                    <span class="d-none d-md-inline">View</span>
                  </button>
                </router-link>
                <router-link :to="{ name: 'NodeLinkEdit', params: { nodeLinkId: nodeLink.id } }" custom v-slot="{ navigate }">
                  <button @click="navigate" class="btn btn-primary btn-sm edit" data-cy="entityEditButton">
                    <font-awesome-icon icon="pencil-alt"></font-awesome-icon>
                    <span class="d-none d-md-inline">Edit</span>
                  </button>
                </router-link>
                <b-button
                  @click="prepareRemove(nodeLink)"
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
        <span id="opportunitySolutionTreeApp.nodeLink.delete.question" data-cy="nodeLinkDeleteDialogHeading">Confirm delete operation</span>
      </template>
      <div class="modal-body">
        <p id="jhi-delete-nodeLink-heading">Are you sure you want to delete Node Link {{ removeId }}?</p>
      </div>
      <template #footer>
        <div>
          <button type="button" class="btn btn-secondary" @click="closeDialog()">Cancel</button>
          <button
            type="button"
            class="btn btn-primary"
            id="jhi-confirm-delete-nodeLink"
            data-cy="entityConfirmDeleteButton"
            @click="removeNodeLink"
          >
            Delete
          </button>
        </div>
      </template>
    </b-modal>
  </div>
</template>

<script lang="ts" src="./node-link.component.ts"></script>
