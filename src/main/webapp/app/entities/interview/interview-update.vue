<template>
  <div class="d-flex justify-content-center">
    <div class="col-8">
      <form name="editForm" novalidate @submit.prevent="save()">
        <h2 id="opportunitySolutionTreeApp.interview.home.createOrEditLabel" data-cy="InterviewCreateUpdateHeading">
          Create or edit a Interview
        </h2>
        <div>
          <div class="mb-3" v-if="interview.id">
            <label for="id">ID</label>
            <input type="text" class="form-control" id="id" name="id" v-model="interview.id" readonly />
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="interview">Title</label>
            <input
              type="text"
              class="form-control"
              name="title"
              id="interview-title"
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
            <label class="form-control-label" for="interview">Participant</label>
            <input
              type="text"
              class="form-control"
              name="participant"
              id="interview-participant"
              data-cy="participant"
              :class="{ valid: !v$.participant.$invalid, invalid: v$.participant.$invalid }"
              v-model="v$.participant.$model"
            />
            <div v-if="v$.participant.$anyDirty && v$.participant.$invalid">
              <small class="form-text text-danger" v-for="error of v$.participant.$errors" :key="error.$uid">{{ error.$message }}</small>
            </div>
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="interview">Interview Date</label>
            <b-input-group class="mb-3">
              <b-input-group-prepend>
                <b-form-datepicker
                  aria-controls="interview-interviewDate"
                  v-model="v$.interviewDate.$model"
                  name="interviewDate"
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
                id="interview-interviewDate"
                data-cy="interviewDate"
                type="text"
                class="form-control"
                name="interviewDate"
                :class="{ valid: !v$.interviewDate.$invalid, invalid: v$.interviewDate.$invalid }"
                v-model="v$.interviewDate.$model"
                required
              />
            </b-input-group>
            <div v-if="v$.interviewDate.$anyDirty && v$.interviewDate.$invalid">
              <small class="form-text text-danger" v-for="error of v$.interviewDate.$errors" :key="error.$uid">{{ error.$message }}</small>
            </div>
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="interview">Notes</label>
            <textarea
              class="form-control"
              name="notes"
              id="interview-notes"
              data-cy="notes"
              :class="{ valid: !v$.notes.$invalid, invalid: v$.notes.$invalid }"
              v-model="v$.notes.$model"
            ></textarea>
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="interview">Recording Url</label>
            <input
              type="text"
              class="form-control"
              name="recordingUrl"
              id="interview-recordingUrl"
              data-cy="recordingUrl"
              :class="{ valid: !v$.recordingUrl.$invalid, invalid: v$.recordingUrl.$invalid }"
              v-model="v$.recordingUrl.$model"
            />
            <div v-if="v$.recordingUrl.$anyDirty && v$.recordingUrl.$invalid">
              <small class="form-text text-danger" v-for="error of v$.recordingUrl.$errors" :key="error.$uid">{{ error.$message }}</small>
            </div>
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="interview">Created Date</label>
            <div class="d-flex">
              <input
                id="interview-createdDate"
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
            <label class="form-control-label" for="interview">Product</label>
            <select class="form-control" id="interview-product" data-cy="product" name="product" v-model="interview.product" required>
              <option v-if="!interview.product" :value="null" selected></option>
              <option
                :value="interview.product && productOption.id === interview.product.id ? interview.product : productOption"
                v-for="productOption in products"
                :key="productOption.id"
              >
                {{ productOption.name }}
              </option>
            </select>
          </div>
          <div v-if="v$.product.$anyDirty && v$.product.$invalid">
            <small class="form-text text-danger" v-for="error of v$.product.$errors" :key="error.$uid">{{ error.$message }}</small>
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="interview">Interviewer</label>
            <select
              class="form-control"
              id="interview-interviewer"
              data-cy="interviewer"
              name="interviewer"
              v-model="interview.interviewer"
            >
              <option :value="null"></option>
              <option
                :value="interview.interviewer && userOption.id === interview.interviewer.id ? interview.interviewer : userOption"
                v-for="userOption in users"
                :key="userOption.id"
              >
                {{ userOption.login }}
              </option>
            </select>
          </div>
          <div class="mb-3">
            <label for="interview">Opportunity</label>
            <select
              class="form-control"
              id="interview-opportunities"
              data-cy="opportunity"
              multiple
              name="opportunity"
              v-if="interview.opportunities !== undefined"
              v-model="interview.opportunities"
            >
              <option
                :value="getSelected(interview.opportunities, opportunityOption, 'id')"
                v-for="opportunityOption in opportunities"
                :key="opportunityOption.id"
              >
                {{ opportunityOption.title }}
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
<script lang="ts" src="./interview-update.component.ts"></script>
