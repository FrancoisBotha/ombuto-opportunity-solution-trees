<template>
  <div class="modal-body pad">
    <div v-if="currentHealth?.details">
      <h5>Properties</h5>
      <div class="table-responsive">
        <table class="table table-striped" aria-describedby="Health">
          <thead>
            <tr>
              <th class="text-start" scope="col">Name</th>
              <th class="text-start" scope="col">Value</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(item, index) in currentHealth.details.details" :key="index">
              <td class="text-start">{{ index }}</td>
              <td class="text-start">{{ readableValue(item) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
    <div v-if="currentHealth?.error">
      <h4>Error</h4>
      <pre>{{ currentHealth.error }}</pre>
    </div>
  </div>
</template>

<script lang="ts" src="./health-modal.component.ts"></script>
