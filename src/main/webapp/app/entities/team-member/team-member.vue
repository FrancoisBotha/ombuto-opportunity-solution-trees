<template>
  <div>
    <h2 id="page-heading" data-cy="TeamMemberHeading">
      <span id="team-member">Team Members</span>
      <div class="d-flex justify-content-end">
        <button class="btn btn-info me-2" @click="handleSyncList" :disabled="isFetching">
          <font-awesome-icon icon="sync" :spin="isFetching"></font-awesome-icon> <span>Refresh list</span>
        </button>
        <router-link :to="{ name: 'TeamMemberCreate' }" custom v-slot="{ navigate }">
          <button
            @click="navigate"
            id="jh-create-entity"
            data-cy="entityCreateButton"
            class="btn btn-primary jh-create-entity create-team-member"
          >
            <font-awesome-icon icon="plus"></font-awesome-icon>
            <span>Create a new Team Member</span>
          </button>
        </router-link>
      </div>
    </h2>
    <br />
    <div class="alert alert-warning" v-if="!isFetching && teamMembers?.length === 0">
      <span>No Team Members found</span>
    </div>
    <div class="table-responsive" v-if="teamMembers?.length > 0">
      <table class="table table-striped" aria-describedby="teamMembers">
        <thead>
          <tr>
            <th scope="col"><span>ID</span></th>
            <th scope="col"><span>Role</span></th>
            <th scope="col"><span>Joined Date</span></th>
            <th scope="col"><span>Team</span></th>
            <th scope="col"><span>User</span></th>
            <th scope="col"></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="teamMember in teamMembers" :key="teamMember.id" data-cy="entityTable">
            <td>
              <router-link :to="{ name: 'TeamMemberView', params: { teamMemberId: teamMember.id } }">{{ teamMember.id }}</router-link>
            </td>
            <td>{{ teamMember.role }}</td>
            <td>{{ formatDateShort(teamMember.joinedDate) || '' }}</td>
            <td>
              <div v-if="teamMember.team">
                <router-link :to="{ name: 'TeamView', params: { teamId: teamMember.team.id } }">{{ teamMember.team.name }}</router-link>
              </div>
            </td>
            <td>
              {{ teamMember.user ? teamMember.user.login : '' }}
            </td>
            <td class="text-end">
              <div class="btn-group">
                <router-link :to="{ name: 'TeamMemberView', params: { teamMemberId: teamMember.id } }" custom v-slot="{ navigate }">
                  <button @click="navigate" class="btn btn-info btn-sm details" data-cy="entityDetailsButton">
                    <font-awesome-icon icon="eye"></font-awesome-icon>
                    <span class="d-none d-md-inline">View</span>
                  </button>
                </router-link>
                <router-link :to="{ name: 'TeamMemberEdit', params: { teamMemberId: teamMember.id } }" custom v-slot="{ navigate }">
                  <button @click="navigate" class="btn btn-primary btn-sm edit" data-cy="entityEditButton">
                    <font-awesome-icon icon="pencil-alt"></font-awesome-icon>
                    <span class="d-none d-md-inline">Edit</span>
                  </button>
                </router-link>
                <b-button
                  @click="prepareRemove(teamMember)"
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
        <span id="opportunitySolutionTreeApp.teamMember.delete.question" data-cy="teamMemberDeleteDialogHeading"
          >Confirm delete operation</span
        >
      </template>
      <div class="modal-body">
        <p id="jhi-delete-teamMember-heading">Are you sure you want to delete Team Member {{ removeId }}?</p>
      </div>
      <template #footer>
        <div>
          <button type="button" class="btn btn-secondary" @click="closeDialog()">Cancel</button>
          <button
            type="button"
            class="btn btn-primary"
            id="jhi-confirm-delete-teamMember"
            data-cy="entityConfirmDeleteButton"
            @click="removeTeamMember"
          >
            Delete
          </button>
        </div>
      </template>
    </b-modal>
  </div>
</template>

<script lang="ts" src="./team-member.component.ts"></script>
