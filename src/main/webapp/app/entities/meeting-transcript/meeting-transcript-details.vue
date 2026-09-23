<template>
  <div class="d-flex justify-content-center">
    <div class="col-8">
      <div v-if="meetingTranscript">
        <h2 class="jh-entity-heading" data-cy="meetingTranscriptDetailsHeading">
          <span>Meeting TranscriptMeetingTranscript</span> {{ meetingTranscript.id }}
        </h2>
        <dl class="row-md jh-entity-details">
          <dt>
            <span>Title</span>
          </dt>
          <dd>
            <span>{{ meetingTranscript.title }}</span>
          </dd>
          <dt>
            <span>Meeting Date</span>
          </dt>
          <dd>
            <span>{{ meetingTranscript.meetingDate }}</span>
          </dd>
          <dt>
            <span>Attendees</span>
          </dt>
          <dd>
            <span>{{ meetingTranscript.attendees }}</span>
          </dd>
          <dt>
            <span>Body</span>
          </dt>
          <dd>
            <span>{{ meetingTranscript.body }}</span>
          </dd>
          <dt>
            <span>Source</span>
          </dt>
          <dd>
            <span>{{ meetingTranscript.source }}</span>
          </dd>
          <dt>
            <span>Created Date</span>
          </dt>
          <dd>
            <span v-if="meetingTranscript.createdDate">{{ formatDateLong(meetingTranscript.createdDate) }}</span>
          </dd>
          <dt>
            <span>Edited Date</span>
          </dt>
          <dd>
            <span v-if="meetingTranscript.editedDate">{{ formatDateLong(meetingTranscript.editedDate) }}</span>
          </dd>
          <dt>
            <span>Author</span>
          </dt>
          <dd>
            {{ meetingTranscript.author ? meetingTranscript.author.login : '' }}
          </dd>
          <dt>
            <span>Product</span>
          </dt>
          <dd>
            <div v-if="meetingTranscript.product">
              <router-link :to="{ name: 'ProductView', params: { productId: meetingTranscript.product.id } }">{{
                meetingTranscript.product.name
              }}</router-link>
            </div>
          </dd>
          <dt>
            <span>Outcome</span>
          </dt>
          <dd>
            <div v-if="meetingTranscript.outcome">
              <router-link :to="{ name: 'OutcomeView', params: { outcomeId: meetingTranscript.outcome.id } }">{{
                meetingTranscript.outcome.title
              }}</router-link>
            </div>
          </dd>
          <dt>
            <span>Opportunity</span>
          </dt>
          <dd>
            <div v-if="meetingTranscript.opportunity">
              <router-link :to="{ name: 'OpportunityView', params: { opportunityId: meetingTranscript.opportunity.id } }">{{
                meetingTranscript.opportunity.title
              }}</router-link>
            </div>
          </dd>
          <dt>
            <span>Solution</span>
          </dt>
          <dd>
            <div v-if="meetingTranscript.solution">
              <router-link :to="{ name: 'SolutionView', params: { solutionId: meetingTranscript.solution.id } }">{{
                meetingTranscript.solution.title
              }}</router-link>
            </div>
          </dd>
          <dt>
            <span>Assumption</span>
          </dt>
          <dd>
            <div v-if="meetingTranscript.assumption">
              <router-link :to="{ name: 'AssumptionView', params: { assumptionId: meetingTranscript.assumption.id } }">{{
                meetingTranscript.assumption.statement
              }}</router-link>
            </div>
          </dd>
          <dt>
            <span>Evidence</span>
          </dt>
          <dd>
            <div v-if="meetingTranscript.evidence">
              <router-link :to="{ name: 'EvidenceView', params: { evidenceId: meetingTranscript.evidence.id } }">{{
                meetingTranscript.evidence.title
              }}</router-link>
            </div>
          </dd>
        </dl>
        <button type="submit" @click.prevent="previousState()" class="btn btn-info" data-cy="entityDetailsBackButton">
          <font-awesome-icon icon="arrow-left"></font-awesome-icon>&nbsp;<span>Back</span>
        </button>
        <router-link
          v-if="meetingTranscript.id"
          :to="{ name: 'MeetingTranscriptEdit', params: { meetingTranscriptId: meetingTranscript.id } }"
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

<script lang="ts" src="./meeting-transcript-details.component.ts"></script>
