<template>
  <div class="d-flex justify-content-center">
    <div class="col-8">
      <form name="editForm" novalidate @submit.prevent="save()">
        <h2 id="opportunitySolutionTreeApp.opportunity.home.createOrEditLabel" data-cy="OpportunityCreateUpdateHeading">
          Create or edit a Opportunity
        </h2>
        <div>
          <div class="mb-3" v-if="opportunity.id">
            <label for="id">ID</label>
            <input type="text" class="form-control" id="id" name="id" v-model="opportunity.id" readonly />
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="opportunity">Title</label>
            <input
              type="text"
              class="form-control"
              name="title"
              id="opportunity-title"
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
            <label class="form-control-label" for="opportunity">Description</label>
            <textarea
              class="form-control"
              name="description"
              id="opportunity-description"
              data-cy="description"
              :class="{ valid: !v$.description.$invalid, invalid: v$.description.$invalid }"
              v-model="v$.description.$model"
            ></textarea>
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="opportunity">Status</label>
            <select
              class="form-control"
              name="status"
              :class="{ valid: !v$.status.$invalid, invalid: v$.status.$invalid }"
              v-model="v$.status.$model"
              id="opportunity-status"
              data-cy="status"
              required
            >
              <option v-for="opportunityStatus in opportunityStatusValues" :key="opportunityStatus" :value="opportunityStatus">
                {{ opportunityStatus }}
              </option>
            </select>
            <div v-if="v$.status.$anyDirty && v$.status.$invalid">
              <small class="form-text text-danger" v-for="error of v$.status.$errors" :key="error.$uid">{{ error.$message }}</small>
            </div>
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="opportunity">Value</label>
            <input
              type="number"
              class="form-control"
              name="valuerating"
              id="opportunity-valuerating"
              data-cy="valuerating"
              :class="{ valid: !v$.valuerating.$invalid, invalid: v$.valuerating.$invalid }"
              v-model.number="v$.valuerating.$model"
              required
            />
            <div v-if="v$.valuerating.$anyDirty && v$.valuerating.$invalid">
              <small class="form-text text-danger" v-for="error of v$.valuerating.$errors" :key="error.$uid">{{ error.$message }}</small>
            </div>
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="opportunity">Complexity</label>
            <input
              type="number"
              class="form-control"
              name="complexity"
              id="opportunity-complexity"
              data-cy="complexity"
              :class="{ valid: !v$.complexity.$invalid, invalid: v$.complexity.$invalid }"
              v-model.number="v$.complexity.$model"
              required
            />
            <div v-if="v$.complexity.$anyDirty && v$.complexity.$invalid">
              <small class="form-text text-danger" v-for="error of v$.complexity.$errors" :key="error.$uid">{{ error.$message }}</small>
            </div>
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="opportunity">Sort Order</label>
            <input
              type="number"
              class="form-control"
              name="sortOrder"
              id="opportunity-sortOrder"
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
            <label class="form-control-label" for="opportunity">Created Date</label>
            <div class="d-flex">
              <input
                id="opportunity-createdDate"
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
            <label class="form-control-label" for="opportunity">Last Modified Date</label>
            <div class="d-flex">
              <input
                id="opportunity-lastModifiedDate"
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
            <label class="form-control-label" for="opportunity">Outcome</label>
            <select class="form-control" id="opportunity-outcome" data-cy="outcome" name="outcome" v-model="opportunity.outcome" required>
              <option v-if="!opportunity.outcome" :value="null" selected></option>
              <option
                :value="opportunity.outcome && outcomeOption.id === opportunity.outcome.id ? opportunity.outcome : outcomeOption"
                v-for="outcomeOption in outcomes"
                :key="outcomeOption.id"
              >
                {{ outcomeOption.title }}
              </option>
            </select>
          </div>
          <div v-if="v$.outcome.$anyDirty && v$.outcome.$invalid">
            <small class="form-text text-danger" v-for="error of v$.outcome.$errors" :key="error.$uid">{{ error.$message }}</small>
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="opportunity">Parent</label>
            <select class="form-control" id="opportunity-parent" data-cy="parent" name="parent" v-model="opportunity.parent">
              <option :value="null"></option>
              <option
                :value="opportunity.parent && opportunityOption.id === opportunity.parent.id ? opportunity.parent : opportunityOption"
                v-for="opportunityOption in opportunities"
                :key="opportunityOption.id"
              >
                {{ opportunityOption.title }}
              </option>
            </select>
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="opportunity">Owner</label>
            <select class="form-control" id="opportunity-owner" data-cy="owner" name="owner" v-model="opportunity.owner">
              <option :value="null"></option>
              <option
                :value="opportunity.owner && userOption.id === opportunity.owner.id ? opportunity.owner : userOption"
                v-for="userOption in users"
                :key="userOption.id"
              >
                {{ userOption.login }}
              </option>
            </select>
          </div>
          <div class="mb-3">
            <label for="opportunity">Interview</label>
            <select
              class="form-control"
              id="opportunity-interviews"
              data-cy="interview"
              multiple
              name="interview"
              v-if="opportunity.interviews !== undefined"
              v-model="opportunity.interviews"
            >
              <option
                :value="getSelected(opportunity.interviews, interviewOption, 'id')"
                v-for="interviewOption in interviews"
                :key="interviewOption.id"
              >
                {{ interviewOption.title }}
              </option>
            </select>
          </div>
          <div class="mb-3">
            <label for="opportunity">Tag</label>
            <select
              class="form-control"
              id="opportunity-tags"
              data-cy="tag"
              multiple
              name="tag"
              v-if="opportunity.tags !== undefined"
              v-model="opportunity.tags"
            >
              <option :value="getSelected(opportunity.tags, tagOption, 'id')" v-for="tagOption in tags" :key="tagOption.id">
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
<script lang="ts" src="./opportunity-update.component.ts"></script>
