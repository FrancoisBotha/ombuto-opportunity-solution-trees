<template>
  <div class="team-detail" data-cy="teamDetailPage">
    <div v-if="notFound" class="alert alert-warning" data-cy="teamNotFound">This team does not exist or you do not have access to it.</div>
    <div v-else-if="forbidden" class="alert alert-danger" data-cy="teamForbidden">You do not have access to this team.</div>
    <div v-else-if="loading && !team" class="text-muted" data-cy="teamDetailLoading">Loading team…</div>

    <div v-else-if="team">
      <div v-if="!editing">
        <div class="d-flex justify-content-between align-items-start">
          <div>
            <h2 class="mb-1" data-cy="teamName">{{ team.name }}</h2>
            <span class="badge bg-secondary" data-cy="teamRole">{{ role }}</span>
          </div>
          <button v-if="isOwner" class="btn btn-outline-primary" type="button" data-cy="editTeamButton" @click="beginEdit">
            <font-awesome-icon icon="pencil-alt"></font-awesome-icon>
            <span> Edit</span>
          </button>
        </div>
        <p v-if="team.description" class="mt-3" data-cy="teamDescription">{{ team.description }}</p>
        <p v-else class="mt-3 text-muted fst-italic" data-cy="teamDescriptionEmpty">No description</p>
        <div v-if="!isOwner" class="text-muted small" data-cy="readOnlyNotice">Only owners can rename or re-describe the team.</div>
      </div>

      <form v-else class="card p-3" data-cy="editTeamForm" @submit.prevent="submitEdit">
        <div class="mb-2">
          <label for="edit-team-name" class="form-label">Name</label>
          <input id="edit-team-name" v-model="editName" type="text" class="form-control" data-cy="editTeamName" required maxlength="120" />
        </div>
        <div class="mb-2">
          <label for="edit-team-description" class="form-label">Description</label>
          <textarea
            id="edit-team-description"
            v-model="editDescription"
            class="form-control"
            data-cy="editTeamDescription"
            rows="3"
            maxlength="1000"
          ></textarea>
        </div>
        <div v-if="editError" class="alert alert-danger" data-cy="editTeamError">{{ editError }}</div>
        <div class="d-flex gap-2">
          <button class="btn btn-primary" type="submit" data-cy="saveTeamEdit" :disabled="saving">
            <span>Save</span>
          </button>
          <button class="btn btn-secondary" type="button" @click="cancelEdit" :disabled="saving">
            <span>Cancel</span>
          </button>
        </div>
      </form>

      <hr class="my-4" />
      <team-members :team-id="teamId" />
    </div>
  </div>
</template>

<script lang="ts" src="./team-detail.component.ts"></script>
