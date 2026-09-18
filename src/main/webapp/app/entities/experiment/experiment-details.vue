<template>
  <div class="d-flex justify-content-center">
    <div class="col-8">
      <div v-if="experiment">
        <h2 class="jh-entity-heading" data-cy="experimentDetailsHeading"><span>ExperimentExperiment</span> {{ experiment.id }}</h2>
        <dl class="row-md jh-entity-details">
          <dt>
            <span>Title</span>
          </dt>
          <dd>
            <span>{{ experiment.title }}</span>
          </dd>
          <dt>
            <span>Hypothesis</span>
          </dt>
          <dd>
            <span>{{ experiment.hypothesis }}</span>
          </dd>
          <dt>
            <span>Method</span>
          </dt>
          <dd>
            <span>{{ experiment.method }}</span>
          </dd>
          <dt>
            <span>Success Criteria</span>
          </dt>
          <dd>
            <span>{{ experiment.successCriteria }}</span>
          </dd>
          <dt>
            <span>Status</span>
          </dt>
          <dd>
            <span>{{ experiment.status }}</span>
          </dd>
          <dt>
            <span>Result</span>
          </dt>
          <dd>
            <span>{{ experiment.result }}</span>
          </dd>
          <dt>
            <span>Learnings</span>
          </dt>
          <dd>
            <span>{{ experiment.learnings }}</span>
          </dd>
          <dt>
            <span>Start Date</span>
          </dt>
          <dd>
            <span>{{ experiment.startDate }}</span>
          </dd>
          <dt>
            <span>End Date</span>
          </dt>
          <dd>
            <span>{{ experiment.endDate }}</span>
          </dd>
          <dt>
            <span>Created Date</span>
          </dt>
          <dd>
            <span v-if="experiment.createdDate">{{ formatDateLong(experiment.createdDate) }}</span>
          </dd>
          <dt>
            <span>Solution</span>
          </dt>
          <dd>
            <div v-if="experiment.solution">
              <router-link :to="{ name: 'SolutionView', params: { solutionId: experiment.solution.id } }">{{
                experiment.solution.title
              }}</router-link>
            </div>
          </dd>
          <dt>
            <span>Assumption</span>
          </dt>
          <dd>
            <span v-for="(assumption, i) in experiment.assumptions" :key="assumption.id"
              >{{ i > 0 ? ', ' : '' }}
              <router-link :to="{ name: 'AssumptionView', params: { assumptionId: assumption.id } }">{{
                assumption.statement
              }}</router-link>
            </span>
          </dd>
        </dl>
        <button type="submit" @click.prevent="previousState()" class="btn btn-info" data-cy="entityDetailsBackButton">
          <font-awesome-icon icon="arrow-left"></font-awesome-icon>&nbsp;<span>Back</span>
        </button>
        <router-link
          v-if="experiment.id"
          :to="{ name: 'ExperimentEdit', params: { experimentId: experiment.id } }"
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

<script lang="ts" src="./experiment-details.component.ts"></script>
