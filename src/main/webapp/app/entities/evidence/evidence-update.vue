<template>
  <div class="d-flex justify-content-center">
    <div class="col-8">
      <form name="editForm" novalidate @submit.prevent="save()">
        <h2 id="opportunitySolutionTreeApp.evidence.home.createOrEditLabel" data-cy="EvidenceCreateUpdateHeading">
          Create or edit a Evidence
        </h2>
        <div>
          <div class="mb-3" v-if="evidence.id">
            <label for="id">ID</label>
            <input type="text" class="form-control" id="id" name="id" v-model="evidence.id" readonly />
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="evidence">Title</label>
            <input
              type="text"
              class="form-control"
              name="title"
              id="evidence-title"
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
            <label class="form-control-label" for="evidence">Description</label>
            <textarea
              class="form-control"
              name="description"
              id="evidence-description"
              data-cy="description"
              :class="{ valid: !v$.description.$invalid, invalid: v$.description.$invalid }"
              v-model="v$.description.$model"
            ></textarea>
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="evidence">Sort Order</label>
            <input
              type="number"
              class="form-control"
              name="sortOrder"
              id="evidence-sortOrder"
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
            <label class="form-control-label" for="evidence">Created Date</label>
            <div class="d-flex">
              <input
                id="evidence-createdDate"
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
            <label class="form-control-label" for="evidence">Last Modified Date</label>
            <div class="d-flex">
              <input
                id="evidence-lastModifiedDate"
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
            <label class="form-control-label" for="evidence">Opportunity</label>
            <select class="form-control" id="evidence-opportunity" data-cy="opportunity" name="opportunity" v-model="evidence.opportunity">
              <option :value="null"></option>
              <option
                :value="evidence.opportunity && opportunityOption.id === evidence.opportunity.id ? evidence.opportunity : opportunityOption"
                v-for="opportunityOption in opportunities"
                :key="opportunityOption.id"
              >
                {{ opportunityOption.title }}
              </option>
            </select>
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="evidence">Assumption</label>
            <select class="form-control" id="evidence-assumption" data-cy="assumption" name="assumption" v-model="evidence.assumption">
              <option :value="null"></option>
              <option
                :value="evidence.assumption && assumptionOption.id === evidence.assumption.id ? evidence.assumption : assumptionOption"
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
<script lang="ts" src="./evidence-update.component.ts"></script>
