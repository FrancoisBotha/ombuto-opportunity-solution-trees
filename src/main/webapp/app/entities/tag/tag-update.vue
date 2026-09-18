<template>
  <div class="d-flex justify-content-center">
    <div class="col-8">
      <form name="editForm" novalidate @submit.prevent="save()">
        <h2 id="opportunitySolutionTreeApp.tag.home.createOrEditLabel" data-cy="TagCreateUpdateHeading">Create or edit a Tag</h2>
        <div>
          <div class="mb-3" v-if="tag.id">
            <label for="id">ID</label>
            <input type="text" class="form-control" id="id" name="id" v-model="tag.id" readonly />
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="tag">Name</label>
            <input
              type="text"
              class="form-control"
              name="name"
              id="tag-name"
              data-cy="name"
              :class="{ valid: !v$.name.$invalid, invalid: v$.name.$invalid }"
              v-model="v$.name.$model"
              required
            />
            <div v-if="v$.name.$anyDirty && v$.name.$invalid">
              <small class="form-text text-danger" v-for="error of v$.name.$errors" :key="error.$uid">{{ error.$message }}</small>
            </div>
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="tag">Colour</label>
            <input
              type="text"
              class="form-control"
              name="colour"
              id="tag-colour"
              data-cy="colour"
              :class="{ valid: !v$.colour.$invalid, invalid: v$.colour.$invalid }"
              v-model="v$.colour.$model"
            />
            <div v-if="v$.colour.$anyDirty && v$.colour.$invalid">
              <small class="form-text text-danger" v-for="error of v$.colour.$errors" :key="error.$uid">{{ error.$message }}</small>
            </div>
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="tag">Team</label>
            <select class="form-control" id="tag-team" data-cy="team" name="team" v-model="tag.team" required>
              <option v-if="!tag.team" :value="null" selected></option>
              <option
                :value="tag.team && teamOption.id === tag.team.id ? tag.team : teamOption"
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
            <label for="tag">Opportunity</label>
            <select
              class="form-control"
              id="tag-opportunities"
              data-cy="opportunity"
              multiple
              name="opportunity"
              v-if="tag.opportunities !== undefined"
              v-model="tag.opportunities"
            >
              <option
                :value="getSelected(tag.opportunities, opportunityOption, 'id')"
                v-for="opportunityOption in opportunities"
                :key="opportunityOption.id"
              >
                {{ opportunityOption.title }}
              </option>
            </select>
          </div>
          <div class="mb-3">
            <label for="tag">Solution</label>
            <select
              class="form-control"
              id="tag-solutions"
              data-cy="solution"
              multiple
              name="solution"
              v-if="tag.solutions !== undefined"
              v-model="tag.solutions"
            >
              <option
                :value="getSelected(tag.solutions, solutionOption, 'id')"
                v-for="solutionOption in solutions"
                :key="solutionOption.id"
              >
                {{ solutionOption.title }}
              </option>
            </select>
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
<script lang="ts" src="./tag-update.component.ts"></script>
