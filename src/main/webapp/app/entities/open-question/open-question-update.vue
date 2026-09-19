<template>
  <div class="d-flex justify-content-center">
    <div class="col-8">
      <form name="editForm" novalidate @submit.prevent="save()">
        <h2 id="opportunitySolutionTreeApp.openQuestion.home.createOrEditLabel" data-cy="OpenQuestionCreateUpdateHeading">
          Create or edit a Open Question
        </h2>
        <div>
          <div class="mb-3" v-if="openQuestion.id">
            <label for="id">ID</label>
            <input type="text" class="form-control" id="id" name="id" v-model="openQuestion.id" readonly />
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="open-question">Question Text</label>
            <input
              type="text"
              class="form-control"
              name="questionText"
              id="open-question-questionText"
              data-cy="questionText"
              :class="{ valid: !v$.questionText.$invalid, invalid: v$.questionText.$invalid }"
              v-model="v$.questionText.$model"
              required
            />
            <div v-if="v$.questionText.$anyDirty && v$.questionText.$invalid">
              <small class="form-text text-danger" v-for="error of v$.questionText.$errors" :key="error.$uid">{{ error.$message }}</small>
            </div>
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="open-question">Done</label>
            <input
              type="checkbox"
              class="form-check"
              name="done"
              id="open-question-done"
              data-cy="done"
              :class="{ valid: !v$.done.$invalid, invalid: v$.done.$invalid }"
              v-model="v$.done.$model"
              required
            />
            <div v-if="v$.done.$anyDirty && v$.done.$invalid">
              <small class="form-text text-danger" v-for="error of v$.done.$errors" :key="error.$uid">{{ error.$message }}</small>
            </div>
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="open-question">Sort Order</label>
            <input
              type="number"
              class="form-control"
              name="sortOrder"
              id="open-question-sortOrder"
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
            <label class="form-control-label" for="open-question">Created Date</label>
            <div class="d-flex">
              <input
                id="open-question-createdDate"
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
            <label class="form-control-label" for="open-question">Opportunity</label>
            <select
              class="form-control"
              id="open-question-opportunity"
              data-cy="opportunity"
              name="opportunity"
              v-model="openQuestion.opportunity"
              required
            >
              <option v-if="!openQuestion.opportunity" :value="null" selected></option>
              <option
                :value="
                  openQuestion.opportunity && opportunityOption.id === openQuestion.opportunity.id
                    ? openQuestion.opportunity
                    : opportunityOption
                "
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
<script lang="ts" src="./open-question-update.component.ts"></script>
