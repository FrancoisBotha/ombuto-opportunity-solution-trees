<template>
  <div class="d-flex justify-content-center">
    <div class="col-8">
      <div v-if="nodeLink">
        <h2 class="jh-entity-heading" data-cy="nodeLinkDetailsHeading"><span>Node LinkNodeLink</span> {{ nodeLink.id }}</h2>
        <dl class="row-md jh-entity-details">
          <dt>
            <span>Name</span>
          </dt>
          <dd>
            <span>{{ nodeLink.name }}</span>
          </dd>
          <dt>
            <span>Url</span>
          </dt>
          <dd>
            <span>{{ nodeLink.url }}</span>
          </dd>
          <dt>
            <span>Sort Order</span>
          </dt>
          <dd>
            <span>{{ nodeLink.sortOrder }}</span>
          </dd>
          <dt>
            <span>Created Date</span>
          </dt>
          <dd>
            <span v-if="nodeLink.createdDate">{{ formatDateLong(nodeLink.createdDate) }}</span>
          </dd>
          <dt>
            <span>Product</span>
          </dt>
          <dd>
            <div v-if="nodeLink.product">
              <router-link :to="{ name: 'ProductView', params: { productId: nodeLink.product.id } }">{{
                nodeLink.product.name
              }}</router-link>
            </div>
          </dd>
          <dt>
            <span>Outcome</span>
          </dt>
          <dd>
            <div v-if="nodeLink.outcome">
              <router-link :to="{ name: 'OutcomeView', params: { outcomeId: nodeLink.outcome.id } }">{{
                nodeLink.outcome.title
              }}</router-link>
            </div>
          </dd>
          <dt>
            <span>Opportunity</span>
          </dt>
          <dd>
            <div v-if="nodeLink.opportunity">
              <router-link :to="{ name: 'OpportunityView', params: { opportunityId: nodeLink.opportunity.id } }">{{
                nodeLink.opportunity.title
              }}</router-link>
            </div>
          </dd>
          <dt>
            <span>Solution</span>
          </dt>
          <dd>
            <div v-if="nodeLink.solution">
              <router-link :to="{ name: 'SolutionView', params: { solutionId: nodeLink.solution.id } }">{{
                nodeLink.solution.title
              }}</router-link>
            </div>
          </dd>
          <dt>
            <span>Assumption</span>
          </dt>
          <dd>
            <div v-if="nodeLink.assumption">
              <router-link :to="{ name: 'AssumptionView', params: { assumptionId: nodeLink.assumption.id } }">{{
                nodeLink.assumption.statement
              }}</router-link>
            </div>
          </dd>
          <dt>
            <span>Evidence</span>
          </dt>
          <dd>
            <div v-if="nodeLink.evidence">
              <router-link :to="{ name: 'EvidenceView', params: { evidenceId: nodeLink.evidence.id } }">{{
                nodeLink.evidence.title
              }}</router-link>
            </div>
          </dd>
        </dl>
        <button type="submit" @click.prevent="previousState()" class="btn btn-info" data-cy="entityDetailsBackButton">
          <font-awesome-icon icon="arrow-left"></font-awesome-icon>&nbsp;<span>Back</span>
        </button>
        <router-link v-if="nodeLink.id" :to="{ name: 'NodeLinkEdit', params: { nodeLinkId: nodeLink.id } }" custom v-slot="{ navigate }">
          <button @click="navigate" class="btn btn-primary">
            <font-awesome-icon icon="pencil-alt"></font-awesome-icon>&nbsp;<span>Edit</span>
          </button>
        </router-link>
      </div>
    </div>
  </div>
</template>

<script lang="ts" src="./node-link-details.component.ts"></script>
