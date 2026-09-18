<template>
  <div>
    <h2 id="page-heading" data-cy="TagHeading">
      <span id="tag">Tags</span>
      <div class="d-flex justify-content-end">
        <button class="btn btn-info me-2" @click="handleSyncList" :disabled="isFetching">
          <font-awesome-icon icon="sync" :spin="isFetching"></font-awesome-icon> <span>Refresh list</span>
        </button>
        <router-link :to="{ name: 'TagCreate' }" custom v-slot="{ navigate }">
          <button @click="navigate" id="jh-create-entity" data-cy="entityCreateButton" class="btn btn-primary jh-create-entity create-tag">
            <font-awesome-icon icon="plus"></font-awesome-icon>
            <span>Create a new Tag</span>
          </button>
        </router-link>
      </div>
    </h2>
    <br />
    <div class="alert alert-warning" v-if="!isFetching && tags?.length === 0">
      <span>No Tags found</span>
    </div>
    <div class="table-responsive" v-if="tags?.length > 0">
      <table class="table table-striped" aria-describedby="tags">
        <thead>
          <tr>
            <th scope="col"><span>ID</span></th>
            <th scope="col"><span>Name</span></th>
            <th scope="col"><span>Colour</span></th>
            <th scope="col"><span>Team</span></th>
            <th scope="col"><span>Opportunity</span></th>
            <th scope="col"><span>Solution</span></th>
            <th scope="col"></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="tag in tags" :key="tag.id" data-cy="entityTable">
            <td>
              <router-link :to="{ name: 'TagView', params: { tagId: tag.id } }">{{ tag.id }}</router-link>
            </td>
            <td>{{ tag.name }}</td>
            <td>{{ tag.colour }}</td>
            <td>
              <div v-if="tag.team">
                <router-link :to="{ name: 'TeamView', params: { teamId: tag.team.id } }">{{ tag.team.name }}</router-link>
              </div>
            </td>
            <td>
              <span v-for="(opportunity, i) in tag.opportunities" :key="opportunity.id"
                >{{ i > 0 ? ', ' : '' }}
                <router-link class="form-control-static" :to="{ name: 'OpportunityView', params: { opportunityId: opportunity.id } }">{{
                  opportunity.title
                }}</router-link>
              </span>
            </td>
            <td>
              <span v-for="(solution, i) in tag.solutions" :key="solution.id"
                >{{ i > 0 ? ', ' : '' }}
                <router-link class="form-control-static" :to="{ name: 'SolutionView', params: { solutionId: solution.id } }">{{
                  solution.title
                }}</router-link>
              </span>
            </td>
            <td class="text-end">
              <div class="btn-group">
                <router-link :to="{ name: 'TagView', params: { tagId: tag.id } }" custom v-slot="{ navigate }">
                  <button @click="navigate" class="btn btn-info btn-sm details" data-cy="entityDetailsButton">
                    <font-awesome-icon icon="eye"></font-awesome-icon>
                    <span class="d-none d-md-inline">View</span>
                  </button>
                </router-link>
                <router-link :to="{ name: 'TagEdit', params: { tagId: tag.id } }" custom v-slot="{ navigate }">
                  <button @click="navigate" class="btn btn-primary btn-sm edit" data-cy="entityEditButton">
                    <font-awesome-icon icon="pencil-alt"></font-awesome-icon>
                    <span class="d-none d-md-inline">Edit</span>
                  </button>
                </router-link>
                <b-button
                  @click="prepareRemove(tag)"
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
        <span id="opportunitySolutionTreeApp.tag.delete.question" data-cy="tagDeleteDialogHeading">Confirm delete operation</span>
      </template>
      <div class="modal-body">
        <p id="jhi-delete-tag-heading">Are you sure you want to delete Tag {{ removeId }}?</p>
      </div>
      <template #footer>
        <div>
          <button type="button" class="btn btn-secondary" @click="closeDialog()">Cancel</button>
          <button type="button" class="btn btn-primary" id="jhi-confirm-delete-tag" data-cy="entityConfirmDeleteButton" @click="removeTag">
            Delete
          </button>
        </div>
      </template>
    </b-modal>
  </div>
</template>

<script lang="ts" src="./tag.component.ts"></script>
