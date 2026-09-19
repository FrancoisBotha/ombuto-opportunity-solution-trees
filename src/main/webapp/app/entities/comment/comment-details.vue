<template>
  <div class="d-flex justify-content-center">
    <div class="col-8">
      <div v-if="comment">
        <h2 class="jh-entity-heading" data-cy="commentDetailsHeading"><span>CommentComment</span> {{ comment.id }}</h2>
        <dl class="row-md jh-entity-details">
          <dt>
            <span>Body</span>
          </dt>
          <dd>
            <span>{{ comment.body }}</span>
          </dd>
          <dt>
            <span>Created Date</span>
          </dt>
          <dd>
            <span v-if="comment.createdDate">{{ formatDateLong(comment.createdDate) }}</span>
          </dd>
          <dt>
            <span>Edited Date</span>
          </dt>
          <dd>
            <span v-if="comment.editedDate">{{ formatDateLong(comment.editedDate) }}</span>
          </dd>
          <dt>
            <span>Author</span>
          </dt>
          <dd>
            {{ comment.author ? comment.author.login : '' }}
          </dd>
          <dt>
            <span>Parent</span>
          </dt>
          <dd>
            <div v-if="comment.parent">
              <router-link :to="{ name: 'CommentView', params: { commentId: comment.parent.id } }">{{ comment.parent.id }}</router-link>
            </div>
          </dd>
          <dt>
            <span>Outcome</span>
          </dt>
          <dd>
            <div v-if="comment.outcome">
              <router-link :to="{ name: 'OutcomeView', params: { outcomeId: comment.outcome.id } }">{{
                comment.outcome.title
              }}</router-link>
            </div>
          </dd>
          <dt>
            <span>Opportunity</span>
          </dt>
          <dd>
            <div v-if="comment.opportunity">
              <router-link :to="{ name: 'OpportunityView', params: { opportunityId: comment.opportunity.id } }">{{
                comment.opportunity.title
              }}</router-link>
            </div>
          </dd>
          <dt>
            <span>Solution</span>
          </dt>
          <dd>
            <div v-if="comment.solution">
              <router-link :to="{ name: 'SolutionView', params: { solutionId: comment.solution.id } }">{{
                comment.solution.title
              }}</router-link>
            </div>
          </dd>
          <dt>
            <span>Assumption</span>
          </dt>
          <dd>
            <div v-if="comment.assumption">
              <router-link :to="{ name: 'AssumptionView', params: { assumptionId: comment.assumption.id } }">{{
                comment.assumption.statement
              }}</router-link>
            </div>
          </dd>
          <dt>
            <span>Evidence</span>
          </dt>
          <dd>
            <div v-if="comment.evidence">
              <router-link :to="{ name: 'EvidenceView', params: { evidenceId: comment.evidence.id } }">{{
                comment.evidence.title
              }}</router-link>
            </div>
          </dd>
        </dl>
        <button type="submit" @click.prevent="previousState()" class="btn btn-info" data-cy="entityDetailsBackButton">
          <font-awesome-icon icon="arrow-left"></font-awesome-icon>&nbsp;<span>Back</span>
        </button>
        <router-link v-if="comment.id" :to="{ name: 'CommentEdit', params: { commentId: comment.id } }" custom v-slot="{ navigate }">
          <button @click="navigate" class="btn btn-primary">
            <font-awesome-icon icon="pencil-alt"></font-awesome-icon>&nbsp;<span>Edit</span>
          </button>
        </router-link>
      </div>
    </div>
  </div>
</template>

<script lang="ts" src="./comment-details.component.ts"></script>
