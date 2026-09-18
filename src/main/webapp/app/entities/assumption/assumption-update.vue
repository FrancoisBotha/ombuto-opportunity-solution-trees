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
            <label class="form-control-label" for="assumption">Category</label>
            <select
              class="form-control"
              name="category"
              :class="{ valid: !v$.category.$invalid, invalid: v$.category.$invalid }"
              v-model="v$.category.$model"
              id="assumption-category"
              data-cy="category"
              required
            >
              <option v-for="assumptionCategory in assumptionCategoryValues" :key="assumptionCategory" :value="assumptionCategory">
                {{ assumptionCategory }}
              </option>
            </select>
            <div v-if="v$.category.$anyDirty && v$.category.$invalid">
              <small class="form-text text-danger" v-for="error of v$.category.$errors" :key="error.$uid">{{ error.$message }}</small>
            </div>
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="assumption">Importance</label>
            <input
              type="number"
              class="form-control"
              name="importance"
              id="assumption-importance"
              data-cy="importance"
              :class="{ valid: !v$.importance.$invalid, invalid: v$.importance.$invalid }"
              v-model.number="v$.importance.$model"
              required
            />
            <div v-if="v$.importance.$anyDirty && v$.importance.$invalid">
              <small class="form-text text-danger" v-for="error of v$.importance.$errors" :key="error.$uid">{{ error.$message }}</small>
            </div>
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="assumption">Evidence</label>
            <input
              type="number"
              class="form-control"
              name="evidence"
              id="assumption-evidence"
              data-cy="evidence"
              :class="{ valid: !v$.evidence.$invalid, invalid: v$.evidence.$invalid }"
              v-model.number="v$.evidence.$model"
              required
            />
            <div v-if="v$.evidence.$anyDirty && v$.evidence.$invalid">
              <small class="form-text text-danger" v-for="error of v$.evidence.$errors" :key="error.$uid">{{ error.$message }}</small>
            </div>
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="assumption">Validated</label>
            <input
              type="checkbox"
              class="form-check"
              name="validated"
              id="assumption-validated"
              data-cy="validated"
              :class="{ valid: !v$.validated.$invalid, invalid: v$.validated.$invalid }"
              v-model="v$.validated.$model"
            />
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
            <label for="assumption">Experiment</label>
            <select
              class="form-control"
              id="assumption-experiments"
              data-cy="experiment"
              multiple
              name="experiment"
              v-if="assumption.experiments !== undefined"
              v-model="assumption.experiments"
            >
              <option
                :value="getSelected(assumption.experiments, experimentOption, 'id')"
                v-for="experimentOption in experiments"
                :key="experimentOption.id"
              >
                {{ experimentOption.title }}
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
