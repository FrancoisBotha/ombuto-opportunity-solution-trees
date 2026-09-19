<template>
  <div class="d-flex justify-content-center">
    <div class="col-8">
      <form name="editForm" novalidate @submit.prevent="save()">
        <h2 id="opportunitySolutionTreeApp.product.home.createOrEditLabel" data-cy="ProductCreateUpdateHeading">
          Create or edit a Product
        </h2>
        <div>
          <div class="mb-3" v-if="product.id">
            <label for="id">ID</label>
            <input type="text" class="form-control" id="id" name="id" v-model="product.id" readonly />
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="product">Name</label>
            <input
              type="text"
              class="form-control"
              name="name"
              id="product-name"
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
            <label class="form-control-label" for="product">Description</label>
            <textarea
              class="form-control"
              name="description"
              id="product-description"
              data-cy="description"
              :class="{ valid: !v$.description.$invalid, invalid: v$.description.$invalid }"
              v-model="v$.description.$model"
            ></textarea>
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="product">Vision</label>
            <textarea
              class="form-control"
              name="vision"
              id="product-vision"
              data-cy="vision"
              :class="{ valid: !v$.vision.$invalid, invalid: v$.vision.$invalid }"
              v-model="v$.vision.$model"
            ></textarea>
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="product">Archived</label>
            <input
              type="checkbox"
              class="form-check"
              name="archived"
              id="product-archived"
              data-cy="archived"
              :class="{ valid: !v$.archived.$invalid, invalid: v$.archived.$invalid }"
              v-model="v$.archived.$model"
              required
            />
            <div v-if="v$.archived.$anyDirty && v$.archived.$invalid">
              <small class="form-text text-danger" v-for="error of v$.archived.$errors" :key="error.$uid">{{ error.$message }}</small>
            </div>
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="product">Sort Order</label>
            <input
              type="number"
              class="form-control"
              name="sortOrder"
              id="product-sortOrder"
              data-cy="sortOrder"
              :class="{ valid: !v$.sortOrder.$invalid, invalid: v$.sortOrder.$invalid }"
              v-model.number="v$.sortOrder.$model"
              readonly
              aria-describedby="product-sortOrder-help"
            />
            <small id="product-sortOrder-help" class="form-text text-muted" data-cy="sortOrderHelp"
              >Set automatically by the server — new products are added at the end of their team’s list.</small
            >
            <div v-if="v$.sortOrder.$anyDirty && v$.sortOrder.$invalid">
              <small class="form-text text-danger" v-for="error of v$.sortOrder.$errors" :key="error.$uid">{{ error.$message }}</small>
            </div>
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="product">Created Date</label>
            <div class="d-flex">
              <input
                id="product-createdDate"
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
            <label class="form-control-label" for="product">Team</label>
            <select class="form-control" id="product-team" data-cy="team" name="team" v-model="product.team" required>
              <option v-if="!product.team" :value="null" selected></option>
              <option
                :value="product.team && teamOption.id === product.team.id ? product.team : teamOption"
                v-for="teamOption in teams"
                :key="teamOption.id"
              >
                {{ teamOption.name }}
              </option>
            </select>
          </div>
          <div v-if="v$.team.$anyDirty && v$.team.$invalid">
            <small class="form-text text-danger" v-for="error of v$.team.$errors" :key="error.$uid">{{ error.$message }}</small>
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
<script lang="ts" src="./product-update.component.ts"></script>
