<template>
  <div class="d-flex justify-content-center">
    <div class="col-8">
      <div v-if="opportunity">
        <h2 class="jh-entity-heading" data-cy="opportunityDetailsHeading"><span>OpportunityOpportunity</span> {{ opportunity.id }}</h2>
        <dl class="row-md jh-entity-details">
          <dt>
            <span>Title</span>
          </dt>
          <dd>
            <span>{{ opportunity.title }}</span>
          </dd>
          <dt>
            <span>Description</span>
          </dt>
          <dd>
            <span>{{ opportunity.description }}</span>
          </dd>
          <dt>
            <span>Status</span>
          </dt>
          <dd>
            <span>{{ opportunity.status }}</span>
          </dd>
          <dt>
            <span>Value</span>
          </dt>
          <dd>
            <span>{{ opportunity.valuerating }}</span>
          </dd>
          <dt>
            <span>Complexity</span>
          </dt>
          <dd>
            <span>{{ opportunity.complexity }}</span>
          </dd>
          <dt>
            <span>Sort Order</span>
          </dt>
          <dd>
            <span>{{ opportunity.sortOrder }}</span>
          </dd>
          <dt>
            <span>Created Date</span>
          </dt>
          <dd>
            <span v-if="opportunity.createdDate">{{ formatDateLong(opportunity.createdDate) }}</span>
          </dd>
          <dt>
            <span>Last Modified Date</span>
          </dt>
          <dd>
            <span v-if="opportunity.lastModifiedDate">{{ formatDateLong(opportunity.lastModifiedDate) }}</span>
          </dd>
          <dt>
            <span>Outcome</span>
          </dt>
          <dd>
            <div v-if="opportunity.outcome">
              <router-link :to="{ name: 'OutcomeView', params: { outcomeId: opportunity.outcome.id } }">{{
                opportunity.outcome.title
              }}</router-link>
            </div>
          </dd>
          <dt>
            <span>Parent</span>
          </dt>
          <dd>
            <div v-if="opportunity.parent">
              <router-link :to="{ name: 'OpportunityView', params: { opportunityId: opportunity.parent.id } }">{{
                opportunity.parent.title
              }}</router-link>
            </div>
          </dd>
          <dt>
            <span>Owner</span>
          </dt>
          <dd>
            {{ opportunity.owner ? opportunity.owner.login : '' }}
          </dd>
          <dt>
            <span>Interview</span>
          </dt>
          <dd>
            <span v-for="(interview, i) in opportunity.interviews" :key="interview.id"
              >{{ i > 0 ? ', ' : '' }}
              <router-link :to="{ name: 'InterviewView', params: { interviewId: interview.id } }">{{ interview.title }}</router-link>
            </span>
          </dd>
          <dt>
            <span>Tag</span>
          </dt>
          <dd>
            <span v-for="(tag, i) in opportunity.tags" :key="tag.id"
              >{{ i > 0 ? ', ' : '' }}
              <router-link :to="{ name: 'TagView', params: { tagId: tag.id } }">{{ tag.name }}</router-link>
            </span>
          </dd>
        </dl>
        <button type="submit" @click.prevent="previousState()" class="btn btn-info" data-cy="entityDetailsBackButton">
          <font-awesome-icon icon="arrow-left"></font-awesome-icon>&nbsp;<span>Back</span>
        </button>
        <router-link
          v-if="opportunity.id"
          :to="{ name: 'OpportunityEdit', params: { opportunityId: opportunity.id } }"
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

<script lang="ts" src="./opportunity-details.component.ts"></script>
