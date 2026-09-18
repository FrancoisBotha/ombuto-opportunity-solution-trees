<template>
  <div class="d-flex justify-content-center">
    <div class="col-8">
      <form name="editForm" novalidate @submit.prevent="save()">
        <h2 id="opportunitySolutionTreeApp.outcome.home.createOrEditLabel" data-cy="OutcomeCreateUpdateHeading">
          Create or edit a Outcome
        </h2>
        <div>
          <div class="mb-3" v-if="outcome.id">
            <label for="id">ID</label>
            <input type="text" class="form-control" id="id" name="id" v-model="outcome.id" readonly />
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="outcome">Title</label>
            <input
              type="text"
              class="form-control"
              name="title"
              id="outcome-title"
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
            <label class="form-control-label" for="outcome">Description</label>
            <textarea
              class="form-control"
              name="description"
              id="outcome-description"
              data-cy="description"
              :class="{ valid: !v$.description.$invalid, invalid: v$.description.$invalid }"
              v-model="v$.description.$model"
            ></textarea>
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="outcome">Metric</label>
            <input
              type="text"
              class="form-control"
              name="metric"
              id="outcome-metric"
              data-cy="metric"
              :class="{ valid: !v$.metric.$invalid, invalid: v$.metric.$invalid }"
              v-model="v$.metric.$model"
            />
            <div v-if="v$.metric.$anyDirty && v$.metric.$invalid">
              <small class="form-text text-danger" v-for="error of v$.metric.$errors" :key="error.$uid">{{ error.$message }}</small>
            </div>
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="outcome">Target Value</label>
            <input
              type="text"
              class="form-control"
              name="targetValue"
              id="outcome-targetValue"
              data-cy="targetValue"
              :class="{ valid: !v$.targetValue.$invalid, invalid: v$.targetValue.$invalid }"
              v-model="v$.targetValue.$model"
            />
            <div v-if="v$.targetValue.$anyDirty && v$.targetValue.$invalid">
              <small class="form-text text-danger" v-for="error of v$.targetValue.$errors" :key="error.$uid">{{ error.$message }}</small>
            </div>
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="outcome">Current Value</label>
            <input
              type="text"
              class="form-control"
              name="currentValue"
              id="outcome-currentValue"
              data-cy="currentValue"
              :class="{ valid: !v$.currentValue.$invalid, invalid: v$.currentValue.$invalid }"
              v-model="v$.currentValue.$model"
            />
            <div v-if="v$.currentValue.$anyDirty && v$.currentValue.$invalid">
              <small class="form-text text-danger" v-for="error of v$.currentValue.$errors" :key="error.$uid">{{ error.$message }}</small>
            </div>
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="outcome">Status</label>
            <select
              class="form-control"
              name="status"
              :class="{ valid: !v$.status.$invalid, invalid: v$.status.$invalid }"
              v-model="v$.status.$model"
              id="outcome-status"
              data-cy="status"
              required
            >
              <option v-for="outcomeStatus in outcomeStatusValues" :key="outcomeStatus" :value="outcomeStatus">{{ outcomeStatus }}</option>
            </select>
            <div v-if="v$.status.$anyDirty && v$.status.$invalid">
              <small class="form-text text-danger" v-for="error of v$.status.$errors" :key="error.$uid">{{ error.$message }}</small>
            </div>
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="outcome">Start Date</label>
            <b-input-group class="mb-3">
              <b-input-group-prepend>
                <b-form-datepicker
                  aria-controls="outcome-startDate"
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
                id="outcome-startDate"
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
            <label class="form-control-label" for="outcome">Target Date</label>
            <b-input-group class="mb-3">
              <b-input-group-prepend>
                <b-form-datepicker
                  aria-controls="outcome-targetDate"
                  v-model="v$.targetDate.$model"
                  name="targetDate"
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
                id="outcome-targetDate"
                data-cy="targetDate"
                type="text"
                class="form-control"
                name="targetDate"
                :class="{ valid: !v$.targetDate.$invalid, invalid: v$.targetDate.$invalid }"
                v-model="v$.targetDate.$model"
              />
            </b-input-group>
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="outcome">Sort Order</label>
            <input
              type="number"
              class="form-control"
              name="sortOrder"
              id="outcome-sortOrder"
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
            <label class="form-control-label" for="outcome">Created Date</label>
            <div class="d-flex">
              <input
                id="outcome-createdDate"
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
            <label class="form-control-label" for="outcome">Last Modified Date</label>
            <div class="d-flex">
              <input
                id="outcome-lastModifiedDate"
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
            <label class="form-control-label" for="outcome">Product</label>
            <select class="form-control" id="outcome-product" data-cy="product" name="product" v-model="outcome.product" required>
              <option v-if="!outcome.product" :value="null" selected></option>
              <option
                :value="outcome.product && productOption.id === outcome.product.id ? outcome.product : productOption"
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
            <label class="form-control-label" for="outcome">Owner</label>
            <select class="form-control" id="outcome-owner" data-cy="owner" name="owner" v-model="outcome.owner">
              <option :value="null"></option>
              <option
                :value="outcome.owner && userOption.id === outcome.owner.id ? outcome.owner : userOption"
                v-for="userOption in users"
                :key="userOption.id"
              >
                {{ userOption.login }}
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
<script lang="ts" src="./outcome-update.component.ts"></script>
