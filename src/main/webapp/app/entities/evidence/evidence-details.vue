<template>
  <div class="d-flex justify-content-center">
    <div class="col-8">
      <div v-if="evidence">
        <h2 class="jh-entity-heading" data-cy="evidenceDetailsHeading"><span>EvidenceEvidence</span> {{ evidence.id }}</h2>
        <dl class="row-md jh-entity-details">
          <dt>
            <span>Title</span>
          </dt>
          <dd>
            <span>{{ evidence.title }}</span>
          </dd>
          <dt>
            <span>Description</span>
          </dt>
          <dd>
            <span>{{ evidence.description }}</span>
          </dd>
          <dt>
            <span>Sort Order</span>
          </dt>
          <dd>
            <span>{{ evidence.sortOrder }}</span>
          </dd>
          <dt>
            <span>Created Date</span>
          </dt>
          <dd>
            <span v-if="evidence.createdDate">{{ formatDateLong(evidence.createdDate) }}</span>
          </dd>
          <dt>
            <span>Last Modified Date</span>
          </dt>
          <dd>
            <span v-if="evidence.lastModifiedDate">{{ formatDateLong(evidence.lastModifiedDate) }}</span>
          </dd>
          <dt>
            <span>Opportunity</span>
          </dt>
          <dd>
            <div v-if="evidence.opportunity">
              <router-link :to="{ name: 'OpportunityView', params: { opportunityId: evidence.opportunity.id } }">{{
                evidence.opportunity.title
              }}</router-link>
            </div>
          </dd>
          <dt>
            <span>Assumption</span>
          </dt>
          <dd>
            <div v-if="evidence.assumption">
              <router-link :to="{ name: 'AssumptionView', params: { assumptionId: evidence.assumption.id } }">{{
                evidence.assumption.statement
              }}</router-link>
            </div>
          </dd>
        </dl>
        <button type="submit" @click.prevent="previousState()" class="btn btn-info" data-cy="entityDetailsBackButton">
          <font-awesome-icon icon="arrow-left"></font-awesome-icon>&nbsp;<span>Back</span>
        </button>
        <router-link v-if="evidence.id" :to="{ name: 'EvidenceEdit', params: { evidenceId: evidence.id } }" custom v-slot="{ navigate }">
          <button @click="navigate" class="btn btn-primary">
            <font-awesome-icon icon="pencil-alt"></font-awesome-icon>&nbsp;<span>Edit</span>
          </button>
        </router-link>
      </div>
    </div>
  </div>
</template>

<script lang="ts" src="./evidence-details.component.ts"></script>
