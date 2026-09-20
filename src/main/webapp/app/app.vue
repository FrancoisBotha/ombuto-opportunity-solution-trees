<template>
  <BToastOrchestrator />
  <div id="app">
    <!-- The dev-profile marker used to live here as a diagonal banner; it is a pill in the navbar now. -->
    <navbar></navbar>
    <sidebar-menu v-if="authenticated" v-model:expanded="sidebarExpanded"></sidebar-menu>
    <div class="main-content" :class="{ expanded: authenticated && sidebarExpanded, 'no-sidebar': !authenticated }">
      <div class="va-container" :class="{ 'full-bleed': fullBleed }">
        <div v-if="fullBleed" class="full-bleed-view">
          <router-view></router-view>
        </div>
        <div v-else class="card jh-card">
          <router-view></router-view>
        </div>
        <jhi-footer v-if="!fullBleed"></jhi-footer>
      </div>
    </div>
  </div>
</template>

<script lang="ts" src="./app.component.ts"></script>
