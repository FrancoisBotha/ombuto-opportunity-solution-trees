<template>
  <div class="d-flex justify-content-center">
    <div class="col-8">
      <div v-if="assumption">
        <h2 class="jh-entity-heading" data-cy="assumptionDetailsHeading"><span>AssumptionAssumption</span> {{ assumption.id }}</h2>
        <dl class="row-md jh-entity-details">
          <dt>
            <span>Statement</span>
          </dt>
          <dd>
            <span>{{ assumption.statement }}</span>
          </dd>
          <dt>
            <span>Category</span>
          </dt>
          <dd>
            <span>{{ assumption.category }}</span>
          </dd>
          <dt>
            <span>Importance</span>
          </dt>
          <dd>
            <span>{{ assumption.importance }}</span>
          </dd>
          <dt>
            <span>Evidence</span>
          </dt>
          <dd>
            <span>{{ assumption.evidence }}</span>
          </dd>
          <dt>
            <span>Validated</span>
          </dt>
          <dd>
            <span>{{ assumption.validated }}</span>
          </dd>
          <dt>
            <span>Created Date</span>
          </dt>
          <dd>
            <span v-if="assumption.createdDate">{{ formatDateLong(assumption.createdDate) }}</span>
          </dd>
          <dt>
            <span>Solution</span>
          </dt>
          <dd>
            <div v-if="assumption.solution">
              <router-link :to="{ name: 'SolutionView', params: { solutionId: assumption.solution.id } }">{{
                assumption.solution.title
              }}</router-link>
            </div>
          </dd>
          <dt>
            <span>Experiment</span>
          </dt>
          <dd>
            <span v-for="(experiment, i) in assumption.experiments" :key="experiment.id"
              >{{ i > 0 ? ', ' : '' }}
              <router-link :to="{ name: 'ExperimentView', params: { experimentId: experiment.id } }">{{ experiment.title }}</router-link>
            </span>
          </dd>
        </dl>
        <button type="submit" @click.prevent="previousState()" class="btn btn-info" data-cy="entityDetailsBackButton">
          <font-awesome-icon icon="arrow-left"></font-awesome-icon>&nbsp;<span>Back</span>
        </button>
        <router-link
          v-if="assumption.id"
          :to="{ name: 'AssumptionEdit', params: { assumptionId: assumption.id } }"
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

<script lang="ts" src="./assumption-details.component.ts"></script>
