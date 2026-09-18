<template>
  <div class="d-flex justify-content-center">
    <div class="col-8">
      <form name="editForm" novalidate @submit.prevent="save()">
        <h2 id="opportunitySolutionTreeApp.comment.home.createOrEditLabel" data-cy="CommentCreateUpdateHeading">
          Create or edit a Comment
        </h2>
        <div>
          <div class="mb-3" v-if="comment.id">
            <label for="id">ID</label>
            <input type="text" class="form-control" id="id" name="id" v-model="comment.id" readonly />
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="comment">Body</label>
            <textarea
              class="form-control"
              name="body"
              id="comment-body"
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
            <label class="form-control-label" for="comment">Created Date</label>
            <div class="d-flex">
              <input
                id="comment-createdDate"
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
            <label class="form-control-label" for="comment">Edited Date</label>
            <div class="d-flex">
              <input
                id="comment-editedDate"
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
            <label class="form-control-label" for="comment">Author</label>
            <select class="form-control" id="comment-author" data-cy="author" name="author" v-model="comment.author" required>
              <option v-if="!comment.author" :value="null" selected></option>
              <option
                :value="comment.author && userOption.id === comment.author.id ? comment.author : userOption"
                v-for="userOption in users"
                :key="userOption.id"
              >
                {{ userOption.login }}
              </option>
            </select>
          </div>
          <div v-if="v$.author.$anyDirty && v$.author.$invalid">
            <small class="form-text text-danger" v-for="error of v$.author.$errors" :key="error.$uid">{{ error.$message }}</small>
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="comment">Parent</label>
            <select class="form-control" id="comment-parent" data-cy="parent" name="parent" v-model="comment.parent">
              <option :value="null"></option>
              <option
                :value="comment.parent && commentOption.id === comment.parent.id ? comment.parent : commentOption"
                v-for="commentOption in comments"
                :key="commentOption.id"
              >
                {{ commentOption.id }}
              </option>
            </select>
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="comment">Outcome</label>
            <select class="form-control" id="comment-outcome" data-cy="outcome" name="outcome" v-model="comment.outcome">
              <option :value="null"></option>
              <option
                :value="comment.outcome && outcomeOption.id === comment.outcome.id ? comment.outcome : outcomeOption"
                v-for="outcomeOption in outcomes"
                :key="outcomeOption.id"
              >
                {{ outcomeOption.title }}
              </option>
            </select>
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="comment">Opportunity</label>
            <select class="form-control" id="comment-opportunity" data-cy="opportunity" name="opportunity" v-model="comment.opportunity">
              <option :value="null"></option>
              <option
                :value="comment.opportunity && opportunityOption.id === comment.opportunity.id ? comment.opportunity : opportunityOption"
                v-for="opportunityOption in opportunities"
                :key="opportunityOption.id"
              >
                {{ opportunityOption.title }}
              </option>
            </select>
          </div>
          <div class="mb-3">
            <label class="form-control-label" for="comment">Solution</label>
            <select class="form-control" id="comment-solution" data-cy="solution" name="solution" v-model="comment.solution">
              <option :value="null"></option>
              <option
                :value="comment.solution && solutionOption.id === comment.solution.id ? comment.solution : solutionOption"
                v-for="solutionOption in solutions"
                :key="solutionOption.id"
              >
                {{ solutionOption.title }}
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
<script lang="ts" src="./comment-update.component.ts"></script>
