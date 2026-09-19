<template>
  <div class="d-flex justify-content-center">
    <div class="col-8">
      <form name="editForm" novalidate @submit.prevent="save()">
        <h2 id="opportunitySolutionTreeApp.nodeHistory.home.createOrEditLabel" data-cy="NodeHistoryCreateUpdateHeading">
          Create or edit a Node History
        </h2>
        <div>
          <div class="mb-3" v-if="nodeHistory.id">
            <label for="id">ID</label>
            <input type="text" class="form-control" id="id" name="id" v-model="nodeHistory.id" readonly />
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="node-history">Node Type</label>
            <select
              class="form-control"
              name="nodeType"
              :class="{ valid: !v$.nodeType.$invalid, invalid: v$.nodeType.$invalid }"
              v-model="v$.nodeType.$model"
              id="node-history-nodeType"
              data-cy="nodeType"
              required
            >
              <option v-for="treeNodeType in treeNodeTypeValues" :key="treeNodeType" :value="treeNodeType">{{ treeNodeType }}</option>
            </select>
            <div v-if="v$.nodeType.$anyDirty && v$.nodeType.$invalid">
              <small class="form-text text-danger" v-for="error of v$.nodeType.$errors" :key="error.$uid">{{ error.$message }}</small>
            </div>
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="node-history">Node Id</label>
            <input
              type="number"
              class="form-control"
              name="nodeId"
              id="node-history-nodeId"
              data-cy="nodeId"
              :class="{ valid: !v$.nodeId.$invalid, invalid: v$.nodeId.$invalid }"
              v-model.number="v$.nodeId.$model"
              required
            />
            <div v-if="v$.nodeId.$anyDirty && v$.nodeId.$invalid">
              <small class="form-text text-danger" v-for="error of v$.nodeId.$errors" :key="error.$uid">{{ error.$message }}</small>
            </div>
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="node-history">Event Type</label>
            <select
              class="form-control"
              name="eventType"
              :class="{ valid: !v$.eventType.$invalid, invalid: v$.eventType.$invalid }"
              v-model="v$.eventType.$model"
              id="node-history-eventType"
              data-cy="eventType"
              required
            >
              <option v-for="historyEventType in historyEventTypeValues" :key="historyEventType" :value="historyEventType">
                {{ historyEventType }}
              </option>
            </select>
            <div v-if="v$.eventType.$anyDirty && v$.eventType.$invalid">
              <small class="form-text text-danger" v-for="error of v$.eventType.$errors" :key="error.$uid">{{ error.$message }}</small>
            </div>
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="node-history">Summary</label>
            <input
              type="text"
              class="form-control"
              name="summary"
              id="node-history-summary"
              data-cy="summary"
              :class="{ valid: !v$.summary.$invalid, invalid: v$.summary.$invalid }"
              v-model="v$.summary.$model"
              required
            />
            <div v-if="v$.summary.$anyDirty && v$.summary.$invalid">
              <small class="form-text text-danger" v-for="error of v$.summary.$errors" :key="error.$uid">{{ error.$message }}</small>
            </div>
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="node-history">Created Date</label>
            <div class="d-flex">
              <input
                id="node-history-createdDate"
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
            <label class="form-control-label" for="node-history">Author</label>
            <select class="form-control" id="node-history-author" data-cy="author" name="author" v-model="nodeHistory.author">
              <option :value="null"></option>
              <option
                :value="nodeHistory.author && userOption.id === nodeHistory.author.id ? nodeHistory.author : userOption"
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
<script lang="ts" src="./node-history-update.component.ts"></script>
