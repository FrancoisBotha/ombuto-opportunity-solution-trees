<template>
  <div class="d-flex justify-content-center">
    <div class="col-8">
      <div v-if="tag">
        <h2 class="jh-entity-heading" data-cy="tagDetailsHeading"><span>TagTag</span> {{ tag.id }}</h2>
        <dl class="row-md jh-entity-details">
          <dt>
            <span>Name</span>
          </dt>
          <dd>
            <span>{{ tag.name }}</span>
          </dd>
          <dt>
            <span>Colour</span>
          </dt>
          <dd>
            <span>{{ tag.colour }}</span>
          </dd>
          <dt>
            <span>Team</span>
          </dt>
          <dd>
            <div v-if="tag.team">
              <router-link :to="{ name: 'TeamView', params: { teamId: tag.team.id } }">{{ tag.team.name }}</router-link>
            </div>
          </dd>
          <dt>
            <span>Opportunity</span>
          </dt>
          <dd>
            <span v-for="(opportunity, i) in tag.opportunities" :key="opportunity.id"
              >{{ i > 0 ? ', ' : '' }}
              <router-link :to="{ name: 'OpportunityView', params: { opportunityId: opportunity.id } }">{{
                opportunity.title
              }}</router-link>
            </span>
          </dd>
          <dt>
            <span>Solution</span>
          </dt>
          <dd>
            <span v-for="(solution, i) in tag.solutions" :key="solution.id"
              >{{ i > 0 ? ', ' : '' }}
              <router-link :to="{ name: 'SolutionView', params: { solutionId: solution.id } }">{{ solution.title }}</router-link>
            </span>
          </dd>
        </dl>
        <button type="submit" @click.prevent="previousState()" class="btn btn-info" data-cy="entityDetailsBackButton">
          <font-awesome-icon icon="arrow-left"></font-awesome-icon>&nbsp;<span>Back</span>
        </button>
        <router-link v-if="tag.id" :to="{ name: 'TagEdit', params: { tagId: tag.id } }" custom v-slot="{ navigate }">
          <button @click="navigate" class="btn btn-primary">
            <font-awesome-icon icon="pencil-alt"></font-awesome-icon>&nbsp;<span>Edit</span>
          </button>
        </router-link>
      </div>
    </div>
  </div>
</template>

<script lang="ts" src="./tag-details.component.ts"></script>
