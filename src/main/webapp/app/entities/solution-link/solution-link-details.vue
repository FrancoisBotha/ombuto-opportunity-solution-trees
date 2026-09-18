<template>
  <div class="d-flex justify-content-center">
    <div class="col-8">
      <div v-if="solutionLink">
        <h2 class="jh-entity-heading" data-cy="solutionLinkDetailsHeading"><span>Solution LinkSolutionLink</span> {{ solutionLink.id }}</h2>
        <dl class="row-md jh-entity-details">
          <dt>
            <span>Name</span>
          </dt>
          <dd>
            <span>{{ solutionLink.name }}</span>
          </dd>
          <dt>
            <span>Url</span>
          </dt>
          <dd>
            <span>{{ solutionLink.url }}</span>
          </dd>
          <dt>
            <span>Type</span>
          </dt>
          <dd>
            <span>{{ solutionLink.type }}</span>
          </dd>
          <dt>
            <span>Sort Order</span>
          </dt>
          <dd>
            <span>{{ solutionLink.sortOrder }}</span>
          </dd>
          <dt>
            <span>Solution</span>
          </dt>
          <dd>
            <div v-if="solutionLink.solution">
              <router-link :to="{ name: 'SolutionView', params: { solutionId: solutionLink.solution.id } }">{{
                solutionLink.solution.title
              }}</router-link>
            </div>
          </dd>
        </dl>
        <button type="submit" @click.prevent="previousState()" class="btn btn-info" data-cy="entityDetailsBackButton">
          <font-awesome-icon icon="arrow-left"></font-awesome-icon>&nbsp;<span>Back</span>
        </button>
        <router-link
          v-if="solutionLink.id"
          :to="{ name: 'SolutionLinkEdit', params: { solutionLinkId: solutionLink.id } }"
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

<script lang="ts" src="./solution-link-details.component.ts"></script>
