<template>
  <section class="team-members" data-cy="teamMembersSection">
    <div class="d-flex justify-content-between align-items-center mb-2">
      <h3 class="mb-0">Members</h3>
      <button
        v-if="isOwner"
        class="btn btn-primary btn-sm"
        type="button"
        data-cy="addMemberButton"
        :disabled="busy || showAddForm"
        @click="openAddForm"
      >
        Add member
      </button>
    </div>

    <div v-if="listError" class="alert alert-danger" data-cy="membersListError">{{ listError }}</div>
    <div v-if="actionError" class="alert alert-danger" data-cy="memberActionError">{{ actionError }}</div>

    <form v-if="showAddForm && isOwner" class="card p-3 mb-3" data-cy="addMemberForm" @submit.prevent="submitAdd">
      <p class="text-muted small mb-2" data-cy="addMemberHint">Only users who have signed in at least once can be added.</p>
      <div class="mb-2 d-flex gap-2">
        <input
          v-model="searchQuery"
          class="form-control"
          type="search"
          placeholder="Search by login or name"
          data-cy="memberSearchInput"
          @keydown.enter.prevent="doSearch"
        />
        <button class="btn btn-outline-secondary" type="button" data-cy="memberSearchButton" :disabled="searchLoading" @click="doSearch">
          Search
        </button>
      </div>
      <ul v-if="searchResults.length" class="list-group mb-2" data-cy="memberSearchResults">
        <li
          v-for="user in searchResults"
          :key="user.id"
          class="list-group-item d-flex justify-content-between align-items-center"
          :class="{ active: selectedUser && selectedUser.id === user.id }"
          role="button"
          :data-cy="`memberSearchResult-${user.login}`"
          @click="selectUser(user)"
        >
          <span
            >{{ user.name }} <small class="text-muted">({{ user.login }})</small></span
          >
          <span v-if="selectedUser && selectedUser.id === user.id" class="badge bg-primary">Selected</span>
        </li>
      </ul>
      <div class="mb-2">
        <label for="add-member-role" class="form-label">Role</label>
        <select id="add-member-role" v-model="selectedRole" class="form-select" data-cy="addMemberRole">
          <option v-for="option in roleOptions" :key="option" :value="option">{{ option }}</option>
        </select>
      </div>
      <div v-if="addError" class="alert alert-danger" data-cy="addMemberError">{{ addError }}</div>
      <div class="d-flex gap-2">
        <button class="btn btn-primary" type="submit" data-cy="submitAddMember" :disabled="busy || !selectedUser">Add</button>
        <button class="btn btn-secondary" type="button" data-cy="cancelAddMember" :disabled="busy" @click="cancelAdd">Cancel</button>
      </div>
    </form>

    <div v-if="loading && members.length === 0" class="text-muted" data-cy="membersLoading">Loading members…</div>

    <table v-else-if="members.length" class="table" data-cy="membersTable">
      <thead>
        <tr>
          <th>Name / login</th>
          <th>Role</th>
          <th>Joined</th>
          <th v-if="isOwner"></th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="m in members" :key="m.userId" :data-cy="`memberRow-${m.userId}`" data-cy-shared="memberRow" class="member-row">
          <td>
            <div>{{ displayName(m) }}</div>
            <div class="text-muted small">{{ m.login }}</div>
          </td>
          <td>
            <template v-if="isOwner">
              <select
                class="form-select form-select-sm"
                :data-cy="`roleSelect-${m.userId}`"
                :value="m.role"
                :disabled="busy || isLastOwner(m)"
                @change="onRoleSelectChange(m, $event)"
              >
                <option v-for="option in roleOptions" :key="option" :value="option">{{ option }}</option>
              </select>
              <div v-if="isLastOwner(m)" class="text-muted small" :data-cy="`lastOwnerHint-${m.userId}`">
                {{ LAST_OWNER_HINT }}
              </div>
            </template>
            <span v-else :data-cy="`memberRole-${m.userId}`">{{ m.role }}</span>
          </td>
          <td>
            <span v-if="m.joinedDate" :data-cy="`memberJoined-${m.userId}`">{{ m.joinedDate }}</span>
          </td>
          <td v-if="isOwner">
            <button
              class="btn btn-outline-danger btn-sm"
              type="button"
              :data-cy="`removeMemberButton-${m.userId}`"
              :disabled="busy || isLastOwner(m)"
              :title="isLastOwner(m) ? LAST_OWNER_HINT : ''"
              @click="askRemove(m)"
            >
              Remove
            </button>
          </td>
        </tr>
      </tbody>
    </table>

    <div v-else class="text-muted" data-cy="membersEmpty">No members yet.</div>

    <div v-if="removeConfirm" class="card border-danger p-3 mt-3" data-cy="removeMemberConfirm">
      <p class="mb-2">
        Remove <strong>{{ removeConfirm.login }}</strong> from this team?
      </p>
      <div class="d-flex gap-2">
        <button class="btn btn-danger" type="button" data-cy="confirmRemoveMember" :disabled="busy" @click="confirmRemove">Remove</button>
        <button class="btn btn-secondary" type="button" data-cy="cancelRemoveMember" :disabled="busy" @click="cancelRemove">Cancel</button>
      </div>
    </div>
  </section>
</template>

<script lang="ts" src="./team-members.component.ts"></script>
