<template>
  <div class="d-flex justify-content-center">
    <div class="col-8">
      <form name="editForm" novalidate @submit.prevent="save()">
        <h2 id="opportunitySolutionTreeApp.solution.home.createOrEditLabel" data-cy="SolutionCreateUpdateHeading">
          Create or edit a Solution
        </h2>
        <div>
          <div class="mb-3" v-if="solution.id">
            <label for="id">ID</label>
            <input type="text" class="form-control" id="id" name="id" v-model="solution.id" readonly />
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="solution">Title</label>
            <input
              type="text"
              class="form-control"
              name="title"
              id="solution-title"
              data-cy="title"
              :class="{ valid: !v$.title.$invalid, invalid: v$.title.$invalid }"
              v-model="v$.title.$model"
              required
            />
            <div v-if="v$.title.$anyDirty && v$.title.$invalid">
              <small class="form-text text-danger" v-for="error of v$.title.$errors" :key="error.$uid">{{ error.$message }}</small>
            </div>
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="solution">Description</label>
            <textarea
              class="form-control"
              name="description"
              id="solution-description"
              data-cy="description"
              :class="{ valid: !v$.description.$invalid, invalid: v$.description.$invalid }"
              v-model="v$.description.$model"
            ></textarea>
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="solution">Status</label>
            <select
              class="form-control"
              name="status"
              :class="{ valid: !v$.status.$invalid, invalid: v$.status.$invalid }"
              v-model="v$.status.$model"
              id="solution-status"
              data-cy="status"
              required
            >
              <option v-for="solutionStatus in solutionStatusValues" :key="solutionStatus" :value="solutionStatus">
                {{ solutionStatus }}
              </option>
            </select>
            <div v-if="v$.status.$anyDirty && v$.status.$invalid">
              <small class="form-text text-danger" v-for="error of v$.status.$errors" :key="error.$uid">{{ error.$message }}</small>
            </div>
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="solution">Effort</label>
            <input
              type="number"
              class="form-control"
              name="effort"
              id="solution-effort"
              data-cy="effort"
              :class="{ valid: !v$.effort.$invalid, invalid: v$.effort.$invalid }"
              v-model.number="v$.effort.$model"
            />
            <div v-if="v$.effort.$anyDirty && v$.effort.$invalid">
              <small class="form-text text-danger" v-for="error of v$.effort.$errors" :key="error.$uid">{{ error.$message }}</small>
            </div>
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="solution">Sort Order</label>
            <input
              type="number"
              class="form-control"
              name="sortOrder"
              id="solution-sortOrder"
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
            <label class="form-control-label" for="solution">Created Date</label>
            <div class="d-flex">
              <input
                id="solution-createdDate"
                data-cy="createdDate"
                type="datetime-local"
                class="form-control"
                name="createdDate"
                :class="{ valid: !v$.createdDate.$invalid, invalid: v$.createdDate.$invalid }"
                required
                :value="convertDateTimeFromServer(v$.createdDate.$model)"
                @change="updateInstantField('createdDate', $event)"
              />
            </div>
            <div v-if="v$.createdDate.$anyDirty && v$.createdDate.$invalid">
              <small class="form-text text-danger" v-for="error of v$.createdDate.$errors" :key="error.$uid">{{ error.$message }}</small>
            </div>
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="solution">Last Modified Date</label>
            <div class="d-flex">
              <input
                id="solution-lastModifiedDate"
                data-cy="lastModifiedDate"
                type="datetime-local"
                class="form-control"
                name="lastModifiedDate"
                :class="{ valid: !v$.lastModifiedDate.$invalid, invalid: v$.lastModifiedDate.$invalid }"
                :value="convertDateTimeFromServer(v$.lastModifiedDate.$model)"
                @change="updateInstantField('lastModifiedDate', $event)"
              />
            </div>
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="solution">Opportunity</label>
            <select
              class="form-control"
              id="solution-opportunity"
              data-cy="opportunity"
              name="opportunity"
              v-model="solution.opportunity"
              required
            >
              <option v-if="!solution.opportunity" :value="null" selected></option>
              <option
                :value="solution.opportunity && opportunityOption.id === solution.opportunity.id ? solution.opportunity : opportunityOption"
                v-for="opportunityOption in opportunities"
                :key="opportunityOption.id"
              >
                {{ opportunityOption.title }}
              </option>
            </select>
          </div>
          <div v-if="v$.opportunity.$anyDirty && v$.opportunity.$invalid">
            <small class="form-text text-danger" v-for="error of v$.opportunity.$errors" :key="error.$uid">{{ error.$message }}</small>
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="solution">Owner</label>
            <select class="form-control" id="solution-owner" data-cy="owner" name="owner" v-model="solution.owner">
              <option :value="null"></option>
              <option
                :value="solution.owner && userOption.id === solution.owner.id ? solution.owner : userOption"
                v-for="userOption in users"
                :key="userOption.id"
              >
                {{ userOption.login }}
              </option>
            </select>
          </div>
          <div class="mb-3">
            <label for="solution">Tag</label>
            <select
              class="form-control"
              id="solution-tags"
              data-cy="tag"
              multiple
              name="tag"
              v-if="solution.tags !== undefined"
              v-model="solution.tags"
            >
              <option :value="getSelected(solution.tags, tagOption, 'id')" v-for="tagOption in tags" :key="tagOption.id">
                {{ tagOption.name }}
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
<script lang="ts" src="./solution-update.component.ts"></script>
