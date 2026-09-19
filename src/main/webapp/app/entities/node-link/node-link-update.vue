<template>
  <div class="d-flex justify-content-center">
    <div class="col-8">
      <form name="editForm" novalidate @submit.prevent="save()">
        <h2 id="opportunitySolutionTreeApp.nodeLink.home.createOrEditLabel" data-cy="NodeLinkCreateUpdateHeading">
          Create or edit a Node Link
        </h2>
        <div>
          <div class="mb-3" v-if="nodeLink.id">
            <label for="id">ID</label>
            <input type="text" class="form-control" id="id" name="id" v-model="nodeLink.id" readonly />
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="node-link">Name</label>
            <input
              type="text"
              class="form-control"
              name="name"
              id="node-link-name"
              data-cy="name"
              :class="{ valid: !v$.name.$invalid, invalid: v$.name.$invalid }"
              v-model="v$.name.$model"
              required
            />
            <div v-if="v$.name.$anyDirty && v$.name.$invalid">
              <small class="form-text text-danger" v-for="error of v$.name.$errors" :key="error.$uid">{{ error.$message }}</small>
            </div>
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="node-link">Url</label>
            <input
              type="text"
              class="form-control"
              name="url"
              id="node-link-url"
              data-cy="url"
              :class="{ valid: !v$.url.$invalid, invalid: v$.url.$invalid }"
              v-model="v$.url.$model"
              required
            />
            <div v-if="v$.url.$anyDirty && v$.url.$invalid">
              <small class="form-text text-danger" v-for="error of v$.url.$errors" :key="error.$uid">{{ error.$message }}</small>
            </div>
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="node-link">Sort Order</label>
            <input
              type="number"
              class="form-control"
              name="sortOrder"
              id="node-link-sortOrder"
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
            <label class="form-control-label" for="node-link">Created Date</label>
            <div class="d-flex">
              <input
                id="node-link-createdDate"
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
            <label class="form-control-label" for="node-link">Product</label>
            <select class="form-control" id="node-link-product" data-cy="product" name="product" v-model="nodeLink.product">
              <option :value="null"></option>
              <option
                :value="nodeLink.product && productOption.id === nodeLink.product.id ? nodeLink.product : productOption"
                v-for="productOption in products"
                :key="productOption.id"
              >
                {{ productOption.name }}
              </option>
            </select>
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="node-link">Outcome</label>
            <select class="form-control" id="node-link-outcome" data-cy="outcome" name="outcome" v-model="nodeLink.outcome">
              <option :value="null"></option>
              <option
                :value="nodeLink.outcome && outcomeOption.id === nodeLink.outcome.id ? nodeLink.outcome : outcomeOption"
                v-for="outcomeOption in outcomes"
                :key="outcomeOption.id"
              >
                {{ outcomeOption.title }}
              </option>
            </select>
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="node-link">Opportunity</label>
            <select class="form-control" id="node-link-opportunity" data-cy="opportunity" name="opportunity" v-model="nodeLink.opportunity">
              <option :value="null"></option>
              <option
                :value="nodeLink.opportunity && opportunityOption.id === nodeLink.opportunity.id ? nodeLink.opportunity : opportunityOption"
                v-for="opportunityOption in opportunities"
                :key="opportunityOption.id"
              >
                {{ opportunityOption.title }}
              </option>
            </select>
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="node-link">Solution</label>
            <select class="form-control" id="node-link-solution" data-cy="solution" name="solution" v-model="nodeLink.solution">
              <option :value="null"></option>
              <option
                :value="nodeLink.solution && solutionOption.id === nodeLink.solution.id ? nodeLink.solution : solutionOption"
                v-for="solutionOption in solutions"
                :key="solutionOption.id"
              >
                {{ solutionOption.title }}
              </option>
            </select>
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="node-link">Assumption</label>
            <select class="form-control" id="node-link-assumption" data-cy="assumption" name="assumption" v-model="nodeLink.assumption">
              <option :value="null"></option>
              <option
                :value="nodeLink.assumption && assumptionOption.id === nodeLink.assumption.id ? nodeLink.assumption : assumptionOption"
                v-for="assumptionOption in assumptions"
                :key="assumptionOption.id"
              >
                {{ assumptionOption.statement }}
              </option>
            </select>
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="node-link">Evidence</label>
            <select class="form-control" id="node-link-evidence" data-cy="evidence" name="evidence" v-model="nodeLink.evidence">
              <option :value="null"></option>
              <option
                :value="nodeLink.evidence && evidenceOption.id === nodeLink.evidence.id ? nodeLink.evidence : evidenceOption"
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
<script lang="ts" src="./node-link-update.component.ts"></script>
