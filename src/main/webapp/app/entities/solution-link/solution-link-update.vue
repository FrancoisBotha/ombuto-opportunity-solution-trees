<template>
  <div class="d-flex justify-content-center">
    <div class="col-8">
      <form name="editForm" novalidate @submit.prevent="save()">
        <h2 id="opportunitySolutionTreeApp.solutionLink.home.createOrEditLabel" data-cy="SolutionLinkCreateUpdateHeading">
          Create or edit a Solution Link
        </h2>
        <div>
          <div class="mb-3" v-if="solutionLink.id">
            <label for="id">ID</label>
            <input type="text" class="form-control" id="id" name="id" v-model="solutionLink.id" readonly />
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="solution-link">Name</label>
            <input
              type="text"
              class="form-control"
              name="name"
              id="solution-link-name"
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
            <label class="form-control-label" for="solution-link">Url</label>
            <input
              type="text"
              class="form-control"
              name="url"
              id="solution-link-url"
              data-cy="url"
              :class="{ valid: !v$.url.$invalid, invalid: v$.url.$invalid }"
              v-model="v$.url.$model"
              required
            />
            <div v-if="v$.url.$anyDirty && v$.url.$invalid">
              <small class="form-text text-danger" v-for="error of v$.url.$errors" :key="error.$uid">{{ error.$message }}</small>
            </div>
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="solution-link">Type</label>
            <select
              class="form-control"
              name="type"
              :class="{ valid: !v$.type.$invalid, invalid: v$.type.$invalid }"
              v-model="v$.type.$model"
              id="solution-link-type"
              data-cy="type"
              required
            >
              <option v-for="linkType in linkTypeValues" :key="linkType" :value="linkType">{{ linkType }}</option>
            </select>
            <div v-if="v$.type.$anyDirty && v$.type.$invalid">
              <small class="form-text text-danger" v-for="error of v$.type.$errors" :key="error.$uid">{{ error.$message }}</small>
            </div>
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="solution-link">Sort Order</label>
            <input
              type="number"
              class="form-control"
              name="sortOrder"
              id="solution-link-sortOrder"
              data-cy="sortOrder"
              :class="{ valid: !v$.sortOrder.$invalid, invalid: v$.sortOrder.$invalid }"
              v-model.number="v$.sortOrder.$model"
              required
            />
            <div v-if="v$.sortOrder.$anyDirty && v$.sortOrder.$invalid">
              <small class="form-text text-danger" v-for="error of v$.sortOrder.$errors" :key="error.$uid">{{ error.$message }}</small>
            </div>
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="solution-link">Solution</label>
            <select
              class="form-control"
              id="solution-link-solution"
              data-cy="solution"
              name="solution"
              v-model="solutionLink.solution"
              required
            >
              <option v-if="!solutionLink.solution" :value="null" selected></option>
              <option
                :value="solutionLink.solution && solutionOption.id === solutionLink.solution.id ? solutionLink.solution : solutionOption"
                v-for="solutionOption in solutions"
                :key="solutionOption.id"
              >
                {{ solutionOption.title }}
              </option>
            </select>
          </div>
          <div v-if="v$.solution.$anyDirty && v$.solution.$invalid">
            <small class="form-text text-danger" v-for="error of v$.solution.$errors" :key="error.$uid">{{ error.$message }}</small>
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
<script lang="ts" src="./solution-link-update.component.ts"></script>
