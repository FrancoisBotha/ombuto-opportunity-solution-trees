<template>
  <div class="d-flex justify-content-center">
    <div class="col-8">
      <div v-if="openQuestion">
        <h2 class="jh-entity-heading" data-cy="openQuestionDetailsHeading"><span>Open QuestionOpenQuestion</span> {{ openQuestion.id }}</h2>
        <dl class="row-md jh-entity-details">
          <dt>
            <span>Question Text</span>
          </dt>
          <dd>
            <span>{{ openQuestion.questionText }}</span>
          </dd>
          <dt>
            <span>Done</span>
          </dt>
          <dd>
            <span>{{ openQuestion.done }}</span>
          </dd>
          <dt>
            <span>Sort Order</span>
          </dt>
          <dd>
            <span>{{ openQuestion.sortOrder }}</span>
          </dd>
          <dt>
            <span>Created Date</span>
          </dt>
          <dd>
            <span v-if="openQuestion.createdDate">{{ formatDateLong(openQuestion.createdDate) }}</span>
          </dd>
          <dt>
            <span>Opportunity</span>
          </dt>
          <dd>
            <div v-if="openQuestion.opportunity">
              <router-link :to="{ name: 'OpportunityView', params: { opportunityId: openQuestion.opportunity.id } }">{{
                openQuestion.opportunity.title
              }}</router-link>
            </div>
          </dd>
        </dl>
        <button type="submit" @click.prevent="previousState()" class="btn btn-info" data-cy="entityDetailsBackButton">
          <font-awesome-icon icon="arrow-left"></font-awesome-icon>&nbsp;<span>Back</span>
        </button>
        <router-link
          v-if="openQuestion.id"
          :to="{ name: 'OpenQuestionEdit', params: { openQuestionId: openQuestion.id } }"
          custom
          v-slot="{ navigate }"
        >
          <button @click="navigate" class="btn btn-primary">
            <font-awesome-icon icon="pencil-alt"></font-awesome-icon>&nbsp;<span>Edit</span>
          </button>
        </router-link>
      </div>
    </div>
  </div>
</template>

<script lang="ts" src="./open-question-details.component.ts"></script>
