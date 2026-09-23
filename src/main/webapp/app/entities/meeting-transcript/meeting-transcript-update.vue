<template>
  <div class="d-flex justify-content-center">
    <div class="col-8">
      <form name="editForm" novalidate @submit.prevent="save()">
        <h2 id="opportunitySolutionTreeApp.meetingTranscript.home.createOrEditLabel" data-cy="MeetingTranscriptCreateUpdateHeading">
          Create or edit a Meeting Transcript
        </h2>
        <div>
          <div class="mb-3" v-if="meetingTranscript.id">
            <label for="id">ID</label>
            <input type="text" class="form-control" id="id" name="id" v-model="meetingTranscript.id" readonly />
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="meeting-transcript">Title</label>
            <input
              type="text"
              class="form-control"
              name="title"
              id="meeting-transcript-title"
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
            <label class="form-control-label" for="meeting-transcript">Meeting Date</label>
            <b-input-group class="mb-3">
              <b-input-group-prepend>
                <b-form-datepicker
                  aria-controls="meeting-transcript-meetingDate"
                  v-model="v$.meetingDate.$model"
                  name="meetingDate"
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
                id="meeting-transcript-meetingDate"
                data-cy="meetingDate"
                type="text"
                class="form-control"
                name="meetingDate"
                :class="{ valid: !v$.meetingDate.$invalid, invalid: v$.meetingDate.$invalid }"
                v-model="v$.meetingDate.$model"
                required
              />
            </b-input-group>
            <div v-if="v$.meetingDate.$anyDirty && v$.meetingDate.$invalid">
              <small class="form-text text-danger" v-for="error of v$.meetingDate.$errors" :key="error.$uid">{{ error.$message }}</small>
            </div>
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="meeting-transcript">Attendees</label>
            <input
              type="text"
              class="form-control"
              name="attendees"
              id="meeting-transcript-attendees"
              data-cy="attendees"
              :class="{ valid: !v$.attendees.$invalid, invalid: v$.attendees.$invalid }"
              v-model="v$.attendees.$model"
            />
            <div v-if="v$.attendees.$anyDirty && v$.attendees.$invalid">
              <small class="form-text text-danger" v-for="error of v$.attendees.$errors" :key="error.$uid">{{ error.$message }}</small>
            </div>
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="meeting-transcript">Body</label>
            <textarea
              class="form-control"
              name="body"
              id="meeting-transcript-body"
              data-cy="body"
              :class="{ valid: !v$.body.$invalid, invalid: v$.body.$invalid }"
              v-model="v$.body.$model"
              required
            ></textarea>
            <div v-if="v$.body.$anyDirty && v$.body.$invalid">
              <small class="form-text text-danger" v-for="error of v$.body.$errors" :key="error.$uid">{{ error.$message }}</small>
            </div>
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="meeting-transcript">Source</label>
            <select
              class="form-control"
              name="source"
              :class="{ valid: !v$.source.$invalid, invalid: v$.source.$invalid }"
              v-model="v$.source.$model"
              id="meeting-transcript-source"
              data-cy="source"
              required
            >
              <option
                v-for="meetingTranscriptSource in meetingTranscriptSourceValues"
                :key="meetingTranscriptSource"
                :value="meetingTranscriptSource"
              >
                {{ meetingTranscriptSource }}
              </option>
            </select>
            <div v-if="v$.source.$anyDirty && v$.source.$invalid">
              <small class="form-text text-danger" v-for="error of v$.source.$errors" :key="error.$uid">{{ error.$message }}</small>
            </div>
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="meeting-transcript">Created Date</label>
            <div class="d-flex">
              <input
                id="meeting-transcript-createdDate"
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
            <label class="form-control-label" for="meeting-transcript">Edited Date</label>
            <div class="d-flex">
              <input
                id="meeting-transcript-editedDate"
                data-cy="editedDate"
                type="datetime-local"
                class="form-control"
                name="editedDate"
                :class="{ valid: !v$.editedDate.$invalid, invalid: v$.editedDate.$invalid }"
                :value="convertDateTimeFromServer(v$.editedDate.$model)"
                @change="updateInstantField('editedDate', $event)"
              />
            </div>
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="meeting-transcript">Author</label>
            <select class="form-control" id="meeting-transcript-author" data-cy="author" name="author" v-model="meetingTranscript.author">
              <option :value="null"></option>
              <option
                :value="meetingTranscript.author && userOption.id === meetingTranscript.author.id ? meetingTranscript.author : userOption"
                v-for="userOption in users"
                :key="userOption.id"
              >
                {{ userOption.login }}
              </option>
            </select>
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="meeting-transcript">Product</label>
            <select
              class="form-control"
              id="meeting-transcript-product"
              data-cy="product"
              name="product"
              v-model="meetingTranscript.product"
            >
              <option :value="null"></option>
              <option
                :value="
                  meetingTranscript.product && productOption.id === meetingTranscript.product.id ? meetingTranscript.product : productOption
                "
                v-for="productOption in products"
                :key="productOption.id"
              >
                {{ productOption.name }}
              </option>
            </select>
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="meeting-transcript">Outcome</label>
            <select
              class="form-control"
              id="meeting-transcript-outcome"
              data-cy="outcome"
              name="outcome"
              v-model="meetingTranscript.outcome"
            >
              <option :value="null"></option>
              <option
                :value="
                  meetingTranscript.outcome && outcomeOption.id === meetingTranscript.outcome.id ? meetingTranscript.outcome : outcomeOption
                "
                v-for="outcomeOption in outcomes"
                :key="outcomeOption.id"
              >
                {{ outcomeOption.title }}
              </option>
            </select>
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="meeting-transcript">Opportunity</label>
            <select
              class="form-control"
              id="meeting-transcript-opportunity"
              data-cy="opportunity"
              name="opportunity"
              v-model="meetingTranscript.opportunity"
            >
              <option :value="null"></option>
              <option
                :value="
                  meetingTranscript.opportunity && opportunityOption.id === meetingTranscript.opportunity.id
                    ? meetingTranscript.opportunity
                    : opportunityOption
                "
                v-for="opportunityOption in opportunities"
                :key="opportunityOption.id"
              >
                {{ opportunityOption.title }}
              </option>
            </select>
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="meeting-transcript">Solution</label>
            <select
              class="form-control"
              id="meeting-transcript-solution"
              data-cy="solution"
              name="solution"
              v-model="meetingTranscript.solution"
            >
              <option :value="null"></option>
              <option
                :value="
                  meetingTranscript.solution && solutionOption.id === meetingTranscript.solution.id
                    ? meetingTranscript.solution
                    : solutionOption
                "
                v-for="solutionOption in solutions"
                :key="solutionOption.id"
              >
                {{ solutionOption.title }}
              </option>
            </select>
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="meeting-transcript">Assumption</label>
            <select
              class="form-control"
              id="meeting-transcript-assumption"
              data-cy="assumption"
              name="assumption"
              v-model="meetingTranscript.assumption"
            >
              <option :value="null"></option>
              <option
                :value="
                  meetingTranscript.assumption && assumptionOption.id === meetingTranscript.assumption.id
                    ? meetingTranscript.assumption
                    : assumptionOption
                "
                v-for="assumptionOption in assumptions"
                :key="assumptionOption.id"
              >
                {{ assumptionOption.statement }}
              </option>
            </select>
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="meeting-transcript">Evidence</label>
            <select
              class="form-control"
              id="meeting-transcript-evidence"
              data-cy="evidence"
              name="evidence"
              v-model="meetingTranscript.evidence"
            >
              <option :value="null"></option>
              <option
                :value="
                  meetingTranscript.evidence && evidenceOption.id === meetingTranscript.evidence.id
                    ? meetingTranscript.evidence
                    : evidenceOption
                "
                v-for="evidenceOption in evidences"
                :key="evidenceOption.id"
              >
                {{ evidenceOption.title }}
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
<script lang="ts" src="./meeting-transcript-update.component.ts"></script>
