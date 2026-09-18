<template>
  <div class="d-flex justify-content-center">
    <div class="col-8">
      <form name="editForm" novalidate @submit.prevent="save()">
        <h2 id="opportunitySolutionTreeApp.teamMember.home.createOrEditLabel" data-cy="TeamMemberCreateUpdateHeading">
          Create or edit a Team Member
        </h2>
        <div>
          <div class="mb-3" v-if="teamMember.id">
            <label for="id">ID</label>
            <input type="text" class="form-control" id="id" name="id" v-model="teamMember.id" readonly />
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="team-member">Role</label>
            <select
              class="form-control"
              name="role"
              :class="{ valid: !v$.role.$invalid, invalid: v$.role.$invalid }"
              v-model="v$.role.$model"
              id="team-member-role"
              data-cy="role"
              required
            >
              <option v-for="teamRole in teamRoleValues" :key="teamRole" :value="teamRole">{{ teamRole }}</option>
            </select>
            <div v-if="v$.role.$anyDirty && v$.role.$invalid">
              <small class="form-text text-danger" v-for="error of v$.role.$errors" :key="error.$uid">{{ error.$message }}</small>
            </div>
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="team-member">Joined Date</label>
            <div class="d-flex">
              <input
                id="team-member-joinedDate"
                data-cy="joinedDate"
                type="datetime-local"
                class="form-control"
                name="joinedDate"
                :class="{ valid: !v$.joinedDate.$invalid, invalid: v$.joinedDate.$invalid }"
                required
                :value="convertDateTimeFromServer(v$.joinedDate.$model)"
                @change="updateInstantField('joinedDate', $event)"
              />
            </div>
            <div v-if="v$.joinedDate.$anyDirty && v$.joinedDate.$invalid">
              <small class="form-text text-danger" v-for="error of v$.joinedDate.$errors" :key="error.$uid">{{ error.$message }}</small>
            </div>
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="team-member">Team</label>
            <select class="form-control" id="team-member-team" data-cy="team" name="team" v-model="teamMember.team" required>
              <option v-if="!teamMember.team" :value="null" selected></option>
              <option
                :value="teamMember.team && teamOption.id === teamMember.team.id ? teamMember.team : teamOption"
                v-for="teamOption in teams"
                :key="teamOption.id"
              >
                {{ teamOption.name }}
              </option>
            </select>
          </div>
          <div v-if="v$.team.$anyDirty && v$.team.$invalid">
            <small class="form-text text-danger" v-for="error of v$.team.$errors" :key="error.$uid">{{ error.$message }}</small>
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="team-member">User</label>
            <select class="form-control" id="team-member-user" data-cy="user" name="user" v-model="teamMember.user" required>
              <option v-if="!teamMember.user" :value="null" selected></option>
              <option
                :value="teamMember.user && userOption.id === teamMember.user.id ? teamMember.user : userOption"
                v-for="userOption in users"
                :key="userOption.id"
              >
                {{ userOption.login }}
              </option>
            </select>
          </div>
          <div v-if="v$.user.$anyDirty && v$.user.$invalid">
            <small class="form-text text-danger" v-for="error of v$.user.$errors" :key="error.$uid">{{ error.$message }}</small>
          </div>
        </div>
        <div>
          <button type="button" id="cancel-save" data-cy="entityCreateCancelButton" class="btn btn-secondary" @click="previousState()">
            <font-awesome-icon icon="ban"></font-awesome-icon>&nbsp;<span>Cancel</span>
          </button>
          <button
            type="submit"
            id="save-entity"
            data-cy="entityCreateSaveButton"
            :disabled="v$.$invalid || isSaving"
            class="btn btn-primary"
          >
            <font-awesome-icon icon="save"></font-awesome-icon>&nbsp;<span>Save</span>
          </button>
        </div>
      </form>
    </div>
  </div>
</template>
<script lang="ts" src="./team-member-update.component.ts"></script>
