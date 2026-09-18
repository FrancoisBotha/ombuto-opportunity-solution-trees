<template>
  <div class="d-flex justify-content-center">
    <div class="col-8">
      <div v-if="interview">
        <h2 class="jh-entity-heading" data-cy="interviewDetailsHeading"><span>InterviewInterview</span> {{ interview.id }}</h2>
        <dl class="row-md jh-entity-details">
          <dt>
            <span>Title</span>
          </dt>
          <dd>
            <span>{{ interview.title }}</span>
          </dd>
          <dt>
            <span>Participant</span>
          </dt>
          <dd>
            <span>{{ interview.participant }}</span>
          </dd>
          <dt>
            <span>Interview Date</span>
          </dt>
          <dd>
            <span>{{ interview.interviewDate }}</span>
          </dd>
          <dt>
            <span>Notes</span>
          </dt>
          <dd>
            <span>{{ interview.notes }}</span>
          </dd>
          <dt>
            <span>Recording Url</span>
          </dt>
          <dd>
            <span>{{ interview.recordingUrl }}</span>
          </dd>
          <dt>
            <span>Created Date</span>
          </dt>
          <dd>
            <span v-if="interview.createdDate">{{ formatDateLong(interview.createdDate) }}</span>
          </dd>
          <dt>
            <span>Product</span>
          </dt>
          <dd>
            <div v-if="interview.product">
              <router-link :to="{ name: 'ProductView', params: { productId: interview.product.id } }">{{
                interview.product.name
              }}</router-link>
            </div>
          </dd>
          <dt>
            <span>Interviewer</span>
          </dt>
          <dd>
            {{ interview.interviewer ? interview.interviewer.login : '' }}
          </dd>
          <dt>
            <span>Opportunity</span>
          </dt>
          <dd>
            <span v-for="(opportunity, i) in interview.opportunities" :key="opportunity.id"
              >{{ i > 0 ? ', ' : '' }}
              <router-link :to="{ name: 'OpportunityView', params: { opportunityId: opportunity.id } }">{{
                opportunity.title
              }}</router-link>
            </span>
          </dd>
        </dl>
        <button type="submit" @click.prevent="previousState()" class="btn btn-info" data-cy="entityDetailsBackButton">
          <font-awesome-icon icon="arrow-left"></font-awesome-icon>&nbsp;<span>Back</span>
        </button>
        <router-link
          v-if="interview.id"
          :to="{ name: 'InterviewEdit', params: { interviewId: interview.id } }"
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

<script lang="ts" src="./interview-details.component.ts"></script>
