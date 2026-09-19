<template>
  <div class="d-flex justify-content-center">
    <div class="col-8">
      <div v-if="nodeHistory">
        <h2 class="jh-entity-heading" data-cy="nodeHistoryDetailsHeading"><span>Node HistoryNodeHistory</span> {{ nodeHistory.id }}</h2>
        <dl class="row-md jh-entity-details">
          <dt>
            <span>Node Type</span>
          </dt>
          <dd>
            <span>{{ nodeHistory.nodeType }}</span>
          </dd>
          <dt>
            <span>Node Id</span>
          </dt>
          <dd>
            <span>{{ nodeHistory.nodeId }}</span>
          </dd>
          <dt>
            <span>Event Type</span>
          </dt>
          <dd>
            <span>{{ nodeHistory.eventType }}</span>
          </dd>
          <dt>
            <span>Summary</span>
          </dt>
          <dd>
            <span>{{ nodeHistory.summary }}</span>
          </dd>
          <dt>
            <span>Created Date</span>
          </dt>
          <dd>
            <span v-if="nodeHistory.createdDate">{{ formatDateLong(nodeHistory.createdDate) }}</span>
          </dd>
          <dt>
            <span>Author</span>
          </dt>
          <dd>
            {{ nodeHistory.author ? nodeHistory.author.login : '' }}
          </dd>
        </dl>
        <button type="submit" @click.prevent="previousState()" class="btn btn-info" data-cy="entityDetailsBackButton">
          <font-awesome-icon icon="arrow-left"></font-awesome-icon>&nbsp;<span>Back</span>
        </button>
        <router-link
          v-if="nodeHistory.id"
          :to="{ name: 'NodeHistoryEdit', params: { nodeHistoryId: nodeHistory.id } }"
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

<script lang="ts" src="./node-history-details.component.ts"></script>
