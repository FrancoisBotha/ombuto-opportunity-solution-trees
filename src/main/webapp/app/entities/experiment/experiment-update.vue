<template>
  <div class="d-flex justify-content-center">
    <div class="col-8">
      <form name="editForm" novalidate @submit.prevent="save()">
        <h2 id="opportunitySolutionTreeApp.experiment.home.createOrEditLabel" data-cy="ExperimentCreateUpdateHeading">
          Create or edit a Experiment
        </h2>
        <div>
          <div class="mb-3" v-if="experiment.id">
            <label for="id">ID</label>
            <input type="text" class="form-control" id="id" name="id" v-model="experiment.id" readonly />
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="experiment">Title</label>
            <input
              type="text"
              class="form-control"
              name="title"
              id="experiment-title"
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
            <label class="form-control-label" for="experiment">Hypothesis</label>
            <textarea
              class="form-control"
              name="hypothesis"
              id="experiment-hypothesis"
              data-cy="hypothesis"
              :class="{ valid: !v$.hypothesis.$invalid, invalid: v$.hypothesis.$invalid }"
              v-model="v$.hypothesis.$model"
            ></textarea>
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="experiment">Method</label>
            <input
              type="text"
              class="form-control"
              name="method"
              id="experiment-method"
              data-cy="method"
              :class="{ valid: !v$.method.$invalid, invalid: v$.method.$invalid }"
              v-model="v$.method.$model"
            />
            <div v-if="v$.method.$anyDirty && v$.method.$invalid">
              <small class="form-text text-danger" v-for="error of v$.method.$errors" :key="error.$uid">{{ error.$message }}</small>
            </div>
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="experiment">Success Criteria</label>
            <textarea
              class="form-control"
              name="successCriteria"
              id="experiment-successCriteria"
              data-cy="successCriteria"
              :class="{ valid: !v$.successCriteria.$invalid, invalid: v$.successCriteria.$invalid }"
              v-model="v$.successCriteria.$model"
            ></textarea>
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="experiment">Status</label>
            <select
              class="form-control"
              name="status"
              :class="{ valid: !v$.status.$invalid, invalid: v$.status.$invalid }"
              v-model="v$.status.$model"
              id="experiment-status"
              data-cy="status"
              required
            >
              <option v-for="experimentStatus in experimentStatusValues" :key="experimentStatus" :value="experimentStatus">
                {{ experimentStatus }}
              </option>
            </select>
            <div v-if="v$.status.$anyDirty && v$.status.$invalid">
              <small class="form-text text-danger" v-for="error of v$.status.$errors" :key="error.$uid">{{ error.$message }}</small>
            </div>
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="experiment">Result</label>
            <select
              class="form-control"
              name="result"
              :class="{ valid: !v$.result.$invalid, invalid: v$.result.$invalid }"
              v-model="v$.result.$model"
              id="experiment-result"
              data-cy="result"
            >
              <option v-for="experimentResult in experimentResultValues" :key="experimentResult" :value="experimentResult">
                {{ experimentResult }}
              </option>
            </select>
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="experiment">Learnings</label>
            <textarea
              class="form-control"
              name="learnings"
              id="experiment-learnings"
              data-cy="learnings"
              :class="{ valid: !v$.learnings.$invalid, invalid: v$.learnings.$invalid }"
              v-model="v$.learnings.$model"
            ></textarea>
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="experiment">Start Date</label>
            <b-input-group class="mb-3">
              <b-input-group-prepend>
                <b-form-datepicker
                  aria-controls="experiment-startDate"
                  v-model="v$.startDate.$model"
                  name="startDate"
                  class="form-control"
                  :locale="currentLanguage"
                  button-only
                  today-button
                  reset-button
                  close-button
                >
                </b-form-datepicker>
              </b-input-group-prepend>
              <b-form-input
                id="experiment-startDate"
                data-cy="startDate"
                type="text"
                class="form-control"
                name="startDate"
                :class="{ valid: !v$.startDate.$invalid, invalid: v$.startDate.$invalid }"
                v-model="v$.startDate.$model"
              />
            </b-input-group>
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="experiment">End Date</label>
            <b-input-group class="mb-3">
              <b-input-group-prepend>
                <b-form-datepicker
                  aria-controls="experiment-endDate"
                  v-model="v$.endDate.$model"
                  name="endDate"
                  class="form-control"
                  :locale="currentLanguage"
                  button-only
                  today-button
                  reset-button
                  close-button
                >
                </b-form-datepicker>
              </b-input-group-prepend>
              <b-form-input
                id="experiment-endDate"
                data-cy="endDate"
                type="text"
                class="form-control"
                name="endDate"
                :class="{ valid: !v$.endDate.$invalid, invalid: v$.endDate.$invalid }"
                v-model="v$.endDate.$model"
              />
            </b-input-group>
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="experiment">Created Date</label>
            <div class="d-flex">
              <input
                id="experiment-createdDate"
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
            <label class="form-control-label" for="experiment">Solution</label>
            <select class="form-control" id="experiment-solution" data-cy="solution" name="solution" v-model="experiment.solution" required>
              <option v-if="!experiment.solution" :value="null" selected></option>
              <option
                :value="experiment.solution && solutionOption.id === experiment.solution.id ? experiment.solution : solutionOption"
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
          <div class="mb-3">
            <label for="experiment">Assumption</label>
            <select
              class="form-control"
              id="experiment-assumptions"
              data-cy="assumption"
              multiple
              name="assumption"
              v-if="experiment.assumptions !== undefined"
              v-model="experiment.assumptions"
            >
              <option
                :value="getSelected(experiment.assumptions, assumptionOption, 'id')"
                v-for="assumptionOption in assumptions"
                :key="assumptionOption.id"
              >
                {{ assumptionOption.statement }}
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
<script lang="ts" src="./experiment-update.component.ts"></script>
