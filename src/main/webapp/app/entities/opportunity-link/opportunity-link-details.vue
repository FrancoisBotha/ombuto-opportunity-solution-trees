<template>
  <div class="d-flex justify-content-center">
    <div class="col-8">
      <div v-if="opportunityLink">
        <h2 class="jh-entity-heading" data-cy="opportunityLinkDetailsHeading">
          <span>Opportunity LinkOpportunityLink</span> {{ opportunityLink.id }}
        </h2>
        <dl class="row-md jh-entity-details">
          <dt>
            <span>Name</span>
          </dt>
          <dd>
            <span>{{ opportunityLink.name }}</span>
          </dd>
          <dt>
            <span>Url</span>
          </dt>
          <dd>
            <span>{{ opportunityLink.url }}</span>
          </dd>
          <dt>
            <span>Type</span>
          </dt>
          <dd>
            <span>{{ opportunityLink.type }}</span>
          </dd>
          <dt>
            <span>Sort Order</span>
          </dt>
          <dd>
            <span>{{ opportunityLink.sortOrder }}</span>
          </dd>
          <dt>
            <span>Opportunity</span>
          </dt>
          <dd>
            <div v-if="opportunityLink.opportunity">
              <router-link :to="{ name: 'OpportunityView', params: { opportunityId: opportunityLink.opportunity.id } }">{{
                opportunityLink.opportunity.title
              }}</router-link>
            </div>
          </dd>
        </dl>
        <button type="submit" @click.prevent="previousState()" class="btn btn-info" data-cy="entityDetailsBackButton">
          <font-awesome-icon icon="arrow-left"></font-awesome-icon>&nbsp;<span>Back</span>
        </button>
        <router-link
          v-if="opportunityLink.id"
          :to="{ name: 'OpportunityLinkEdit', params: { opportunityLinkId: opportunityLink.id } }"
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

<script lang="ts" src="./opportunity-link-details.component.ts"></script>
