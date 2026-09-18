<template>
  <BToastOrchestrator />
  <div id="app">
    <ribbon></ribbon>
    <navbar></navbar>
    <sidebar-menu v-if="authenticated" v-model:expanded="sidebarExpanded"></sidebar-menu>
    <div class="main-content" :class="{ expanded: authenticated && sidebarExpanded, 'no-sidebar': !authenticated }">
      <div class="va-container">
        <div class="card jh-card">
          <router-view></router-view>
        </div>
        <jhi-footer></jhi-footer>
      </div>
    </div>
  </div>
</template>

<script lang="ts" src="./app.component.ts"></script>
