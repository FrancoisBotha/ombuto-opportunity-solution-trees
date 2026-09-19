<template>
  <div class="d-flex justify-content-center">
    <div class="col-8">
      <div v-if="solution">
        <h2 class="jh-entity-heading" data-cy="solutionDetailsHeading"><span>SolutionSolution</span> {{ solution.id }}</h2>
        <dl class="row-md jh-entity-details">
          <dt>
            <span>Title</span>
          </dt>
          <dd>
            <span>{{ solution.title }}</span>
          </dd>
          <dt>
            <span>Description</span>
          </dt>
          <dd>
            <span>{{ solution.description }}</span>
          </dd>
          <dt>
            <span>Status</span>
          </dt>
          <dd>
            <span>{{ solution.status }}</span>
          </dd>
          <dt>
            <span>Sort Order</span>
          </dt>
          <dd>
            <span>{{ solution.sortOrder }}</span>
          </dd>
          <dt>
            <span>Created Date</span>
          </dt>
          <dd>
            <span v-if="solution.createdDate">{{ formatDateLong(solution.createdDate) }}</span>
          </dd>
          <dt>
            <span>Last Modified Date</span>
          </dt>
          <dd>
            <span v-if="solution.lastModifiedDate">{{ formatDateLong(solution.lastModifiedDate) }}</span>
          </dd>
          <dt>
            <span>Opportunity</span>
          </dt>
          <dd>
            <div v-if="solution.opportunity">
              <router-link :to="{ name: 'OpportunityView', params: { opportunityId: solution.opportunity.id } }">{{
                solution.opportunity.title
              }}</router-link>
            </div>
          </dd>
          <dt>
            <span>Owner</span>
          </dt>
          <dd>
            {{ solution.owner ? solution.owner.login : '' }}
          </dd>
          <dt>
            <span>Tag</span>
          </dt>
          <dd>
            <span v-for="(tag, i) in solution.tags" :key="tag.id"
              >{{ i > 0 ? ', ' : '' }}
              <router-link :to="{ name: 'TagView', params: { tagId: tag.id } }">{{ tag.name }}</router-link>
            </span>
          </dd>
        </dl>
        <button type="submit" @click.prevent="previousState()" class="btn btn-info" data-cy="entityDetailsBackButton">
          <font-awesome-icon icon="arrow-left"></font-awesome-icon>&nbsp;<span>Back</span>
        </button>
        <router-link v-if="solution.id" :to="{ name: 'SolutionEdit', params: { solutionId: solution.id } }" custom v-slot="{ navigate }">
          <button @click="navigate" class="btn btn-primary">
            <font-awesome-icon icon="pencil-alt"></font-awesome-icon>&nbsp;<span>Edit</span>
          </button>
        </router-link>
      </div>
    </div>
  </div>
</template>

<script lang="ts" src="./solution-details.component.ts"></script>
