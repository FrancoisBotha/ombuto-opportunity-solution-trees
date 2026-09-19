<template>
  <div class="trees" data-cy="treesPage">
    <h2 id="page-heading" data-cy="TreesHeading">
      <span>Trees</span>
    </h2>

    <p class="text-muted">Open a team's opportunity solution tree.</p>

    <div v-if="isLoading && !hasLoaded" class="text-muted" data-cy="treesLoading">Loading teams…</div>

    <div v-else-if="isEmpty" class="empty-state text-center p-4 border rounded" data-cy="treesEmptyState">
      <p class="mb-2"><strong>You are not part of any team yet.</strong></p>
      <p class="mb-3 text-muted">Join or create a team to start building an opportunity solution tree.</p>
      <router-link to="/teams" class="btn btn-primary" data-cy="treesEmptyStateGoToTeams">
        <span>Go to Teams</span>
      </router-link>
    </div>

    <div v-else class="row g-3" data-cy="treesList">
      <div v-for="team in teams" :key="team.id" class="col-md-6 col-lg-4">
        <router-link
          :to="{ name: 'OstTree', params: { teamId: team.id } }"
          class="text-decoration-none text-body"
          :data-cy="`treeCard-${team.id}`"
        >
          <div class="card h-100 tree-card">
            <div class="card-body">
              <div class="d-flex justify-content-between align-items-start">
                <h5 class="card-title mb-1">{{ team.name }}</h5>
                <span class="badge bg-secondary" :data-cy="`treeCardRole-${team.id}`">{{ team.role }}</span>
              </div>
              <p v-if="team.description" class="card-text text-muted small">{{ team.description }}</p>
              <div class="d-flex gap-3 mt-2 small text-muted">
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

<script lang="ts" src="./trees.component.ts"></script>
