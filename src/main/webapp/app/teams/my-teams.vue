<template>
  <div class="my-teams" data-cy="myTeamsPage">
    <h2 id="page-heading" data-cy="MyTeamsHeading">
      <span>My teams</span>
      <div class="d-flex justify-content-end">
        <button
          class="btn btn-primary"
          type="button"
          data-cy="newTeamButton"
          @click="openCreateForm"
          :disabled="showCreateForm || isCreating"
        >
          <font-awesome-icon icon="plus"></font-awesome-icon>
          <span> New team</span>
        </button>
      </div>
    </h2>

    <div v-if="showCreateForm" class="card p-3 mb-3" data-cy="createTeamForm">
      <form @submit.prevent="submitCreate">
        <div class="mb-2">
          <label for="new-team-name" class="form-label">Name</label>
          <input id="new-team-name" v-model="newTeamName" type="text" class="form-control" data-cy="newTeamName" required maxlength="120" />
        </div>
        <div class="mb-2">
          <label for="new-team-description" class="form-label">Description (optional)</label>
          <textarea
            id="new-team-description"
            v-model="newTeamDescription"
            class="form-control"
            data-cy="newTeamDescription"
            rows="2"
            maxlength="1000"
          ></textarea>
        </div>
        <div v-if="formError" class="alert alert-danger" data-cy="newTeamError">{{ formError }}</div>
        <div class="d-flex gap-2">
          <button class="btn btn-primary" type="submit" data-cy="submitNewTeam" :disabled="isCreating">
            <span>Create team</span>
          </button>
          <button class="btn btn-secondary" type="button" @click="cancelCreate" :disabled="isCreating">
            <span>Cancel</span>
          </button>
        </div>
      </form>
    </div>

    <div v-if="isLoading && !hasLoaded" class="text-muted" data-cy="teamsLoading">Loading teams…</div>

    <div v-else-if="isEmpty" class="empty-state text-center p-4 border rounded" data-cy="teamsEmptyState">
      <p class="mb-2"><strong>You are not part of any team yet.</strong></p>
      <p class="mb-3 text-muted">Create your first team to start capturing your discovery work.</p>
      <button class="btn btn-primary" type="button" data-cy="emptyStateNewTeamButton" @click="openCreateForm">
        <font-awesome-icon icon="plus"></font-awesome-icon>
        <span> Create your first team</span>
      </button>
    </div>

    <div v-else class="row g-3" data-cy="teamsList">
      <div v-for="team in teams" :key="team.id" class="col-md-6 col-lg-4">
        <router-link
          :to="{ name: 'MyTeamDetail', params: { id: team.id } }"
          class="text-decoration-none text-body"
          :data-cy="`teamCard-${team.id}`"
        >
          <div class="card h-100 team-card">
            <div class="card-body">
              <div class="d-flex justify-content-between align-items-start">
                <h5 class="card-title mb-1">{{ team.name }}</h5>
                <span class="badge bg-secondary" :data-cy="`teamCardRole-${team.id}`">{{ team.role }}</span>
              </div>
              <p v-if="team.description" class="card-text text-muted small">{{ team.description }}</p>
              <div class="d-flex gap-3 mt-2 small text-muted">
                <span>
                  <font-awesome-icon icon="user"></font-awesome-icon>
                  {{ team.memberCount }} member<span v-if="team.memberCount !== 1">s</span>
                </span>
                <span>
                  <font-awesome-icon icon="box"></font-awesome-icon>
                  {{ team.productCount }} product<span v-if="team.productCount !== 1">s</span>
                </span>
              </div>
            </div>
          </div>
        </router-link>
      </div>
    </div>
  </div>
</template>

<script lang="ts" src="./my-teams.component.ts"></script>
