<template>
  <div>
    <h2 id="page-heading" data-cy="SolutionLinkHeading">
      <span id="solution-link">Solution Links</span>
      <div class="d-flex justify-content-end">
        <button class="btn btn-info me-2" @click="handleSyncList" :disabled="isFetching">
          <font-awesome-icon icon="sync" :spin="isFetching"></font-awesome-icon> <span>Refresh list</span>
        </button>
        <router-link :to="{ name: 'SolutionLinkCreate' }" custom v-slot="{ navigate }">
          <button
            @click="navigate"
            id="jh-create-entity"
            data-cy="entityCreateButton"
            class="btn btn-primary jh-create-entity create-solution-link"
          >
            <font-awesome-icon icon="plus"></font-awesome-icon>
            <span>Create a new Solution Link</span>
          </button>
        </router-link>
      </div>
    </h2>
    <br />
    <div class="alert alert-warning" v-if="!isFetching && solutionLinks?.length === 0">
      <span>No Solution Links found</span>
    </div>
    <div class="table-responsive" v-if="solutionLinks?.length > 0">
      <table class="table table-striped" aria-describedby="solutionLinks">
        <thead>
          <tr>
            <th scope="col"><span>ID</span></th>
            <th scope="col"><span>Name</span></th>
            <th scope="col"><span>Url</span></th>
            <th scope="col"><span>Type</span></th>
            <th scope="col"><span>Sort Order</span></th>
            <th scope="col"><span>Solution</span></th>
            <th scope="col"></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="solutionLink in solutionLinks" :key="solutionLink.id" data-cy="entityTable">
            <td>
              <router-link :to="{ name: 'SolutionLinkView', params: { solutionLinkId: solutionLink.id } }">{{
                solutionLink.id
              }}</router-link>
            </td>
            <td>{{ solutionLink.name }}</td>
            <td>{{ solutionLink.url }}</td>
            <td>{{ solutionLink.type }}</td>
            <td>{{ solutionLink.sortOrder }}</td>
            <td>
              <div v-if="solutionLink.solution">
                <router-link :to="{ name: 'SolutionView', params: { solutionId: solutionLink.solution.id } }">{{
                  solutionLink.solution.title
                }}</router-link>
              </div>
            </td>
            <td class="text-end">
              <div class="btn-group">
                <router-link :to="{ name: 'SolutionLinkView', params: { solutionLinkId: solutionLink.id } }" custom v-slot="{ navigate }">
                  <button @click="navigate" class="btn btn-info btn-sm details" data-cy="entityDetailsButton">
                    <font-awesome-icon icon="eye"></font-awesome-icon>
                    <span class="d-none d-md-inline">View</span>
                  </button>
                </router-link>
                <router-link :to="{ name: 'SolutionLinkEdit', params: { solutionLinkId: solutionLink.id } }" custom v-slot="{ navigate }">
                  <button @click="navigate" class="btn btn-primary btn-sm edit" data-cy="entityEditButton">
                    <font-awesome-icon icon="pencil-alt"></font-awesome-icon>
                    <span class="d-none d-md-inline">Edit</span>
                  </button>
                </router-link>
                <b-button
                  @click="prepareRemove(solutionLink)"
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
        <span id="opportunitySolutionTreeApp.solutionLink.delete.question" data-cy="solutionLinkDeleteDialogHeading"
          >Confirm delete operation</span
        >
      </template>
      <div class="modal-body">
        <p id="jhi-delete-solutionLink-heading">Are you sure you want to delete Solution Link {{ removeId }}?</p>
      </div>
      <template #footer>
        <div>
          <button type="button" class="btn btn-secondary" @click="closeDialog()">Cancel</button>
          <button
            type="button"
            class="btn btn-primary"
            id="jhi-confirm-delete-solutionLink"
            data-cy="entityConfirmDeleteButton"
            @click="removeSolutionLink"
          >
            Delete
          </button>
        </div>
      </template>
    </b-modal>
  </div>
</template>

<script lang="ts" src="./solution-link.component.ts"></script>
