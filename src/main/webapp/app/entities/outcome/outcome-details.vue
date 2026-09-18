<template>
  <div class="d-flex justify-content-center">
    <div class="col-8">
      <div v-if="outcome">
        <h2 class="jh-entity-heading" data-cy="outcomeDetailsHeading"><span>OutcomeOutcome</span> {{ outcome.id }}</h2>
        <dl class="row-md jh-entity-details">
          <dt>
            <span>Title</span>
          </dt>
          <dd>
            <span>{{ outcome.title }}</span>
          </dd>
          <dt>
            <span>Description</span>
          </dt>
          <dd>
            <span>{{ outcome.description }}</span>
          </dd>
          <dt>
            <span>Metric</span>
          </dt>
          <dd>
            <span>{{ outcome.metric }}</span>
          </dd>
          <dt>
            <span>Target Value</span>
          </dt>
          <dd>
            <span>{{ outcome.targetValue }}</span>
          </dd>
          <dt>
            <span>Current Value</span>
          </dt>
          <dd>
            <span>{{ outcome.currentValue }}</span>
          </dd>
          <dt>
            <span>Status</span>
          </dt>
          <dd>
            <span>{{ outcome.status }}</span>
          </dd>
          <dt>
            <span>Start Date</span>
          </dt>
          <dd>
            <span>{{ outcome.startDate }}</span>
          </dd>
          <dt>
            <span>Target Date</span>
          </dt>
          <dd>
            <span>{{ outcome.targetDate }}</span>
          </dd>
          <dt>
            <span>Sort Order</span>
          </dt>
          <dd>
            <span>{{ outcome.sortOrder }}</span>
          </dd>
          <dt>
            <span>Created Date</span>
          </dt>
          <dd>
            <span v-if="outcome.createdDate">{{ formatDateLong(outcome.createdDate) }}</span>
          </dd>
          <dt>
            <span>Last Modified Date</span>
          </dt>
          <dd>
            <span v-if="outcome.lastModifiedDate">{{ formatDateLong(outcome.lastModifiedDate) }}</span>
          </dd>
          <dt>
            <span>Product</span>
          </dt>
          <dd>
            <div v-if="outcome.product">
              <router-link :to="{ name: 'ProductView', params: { productId: outcome.product.id } }">{{ outcome.product.name }}</router-link>
            </div>
          </dd>
          <dt>
            <span>Owner</span>
          </dt>
          <dd>
            {{ outcome.owner ? outcome.owner.login : '' }}
          </dd>
        </dl>
        <button type="submit" @click.prevent="previousState()" class="btn btn-info" data-cy="entityDetailsBackButton">
          <font-awesome-icon icon="arrow-left"></font-awesome-icon>&nbsp;<span>Back</span>
        </button>
        <router-link v-if="outcome.id" :to="{ name: 'OutcomeEdit', params: { outcomeId: outcome.id } }" custom v-slot="{ navigate }">
          <button @click="navigate" class="btn btn-primary">
            <font-awesome-icon icon="pencil-alt"></font-awesome-icon>&nbsp;<span>Edit</span>
          </button>
        </router-link>
      </div>
    </div>
  </div>
</template>

<script lang="ts" src="./outcome-details.component.ts"></script>
