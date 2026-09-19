<template>
  <div class="d-flex justify-content-center">
    <div class="col-8">
      <form name="editForm" novalidate @submit.prevent="save()">
        <h2 id="opportunitySolutionTreeApp.assumption.home.createOrEditLabel" data-cy="AssumptionCreateUpdateHeading">
          Create or edit a Assumption
        </h2>
        <div>
          <div class="mb-3" v-if="assumption.id">
            <label for="id">ID</label>
            <input type="text" class="form-control" id="id" name="id" v-model="assumption.id" readonly />
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="assumption">Statement</label>
            <input
              type="text"
              class="form-control"
              name="statement"
              id="assumption-statement"
              data-cy="statement"
              :class="{ valid: !v$.statement.$invalid, invalid: v$.statement.$invalid }"
              v-model="v$.statement.$model"
              required
            />
            <div v-if="v$.statement.$anyDirty && v$.statement.$invalid">
              <small class="form-text text-danger" v-for="error of v$.statement.$errors" :key="error.$uid">{{ error.$message }}</small>
            </div>
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="assumption">Description</label>
            <textarea
              class="form-control"
              name="description"
              id="assumption-description"
              data-cy="description"
              :class="{ valid: !v$.description.$invalid, invalid: v$.description.$invalid }"
              v-model="v$.description.$model"
            ></textarea>
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="assumption">Status</label>
            <select
              class="form-control"
              name="status"
              :class="{ valid: !v$.status.$invalid, invalid: v$.status.$invalid }"
              v-model="v$.status.$model"
              id="assumption-status"
              data-cy="status"
              required
            >
              <option v-for="assumptionStatus in assumptionStatusValues" :key="assumptionStatus" :value="assumptionStatus">
                {{ assumptionStatus }}
              </option>
            </select>
            <div v-if="v$.status.$anyDirty && v$.status.$invalid">
              <small class="form-text text-danger" v-for="error of v$.status.$errors" :key="error.$uid">{{ error.$message }}</small>
            </div>
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="assumption">Confidence</label>
            <input
              type="number"
              class="form-control"
              name="confidence"
              id="assumption-confidence"
              data-cy="confidence"
              :class="{ valid: !v$.confidence.$invalid, invalid: v$.confidence.$invalid }"
              v-model.number="v$.confidence.$model"
              required
            />
            <div v-if="v$.confidence.$anyDirty && v$.confidence.$invalid">
              <small class="form-text text-danger" v-for="error of v$.confidence.$errors" :key="error.$uid">{{ error.$message }}</small>
            </div>
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="assumption">Sort Order</label>
            <input
              type="number"
              class="form-control"
              name="sortOrder"
              id="assumption-sortOrder"
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
            <label class="form-control-label" for="assumption">Created Date</label>
            <div class="d-flex">
              <input
                id="assumption-createdDate"
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
            <label class="form-control-label" for="assumption">Last Modified Date</label>
            <div class="d-flex">
              <input
                id="assumption-lastModifiedDate"
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
            <label class="form-control-label" for="assumption">Solution</label>
            <select class="form-control" id="assumption-solution" data-cy="solution" name="solution" v-model="assumption.solution" required>
              <option v-if="!assumption.solution" :value="null" selected></option>
              <option
                :value="assumption.solution && solutionOption.id === assumption.solution.id ? assumption.solution : solutionOption"
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
            <label class="form-control-label" for="assumption">Owner</label>
            <select class="form-control" id="assumption-owner" data-cy="owner" name="owner" v-model="assumption.owner">
              <option :value="null"></option>
              <option
                :value="assumption.owner && userOption.id === assumption.owner.id ? assumption.owner : userOption"
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
<script lang="ts" src="./assumption-update.component.ts"></script>
