<template>
  <b-navbar class="va-navbar" data-cy="navbar">
    <b-navbar-brand class="logo" to="/">
      <span class="logo-img"></span>
      <span class="navbar-title">Opportunity Solution Tree</span>
      <span class="navbar-version">{{ version }}</span>
    </b-navbar-brand>

    <div class="navbar-actions">
      <theme-toggle></theme-toggle>
      <span class="navbar-user" v-if="authenticated && username" data-cy="navbarUser">
        <font-awesome-icon icon="user" />
        {{ username }}
      </span>
      <a class="sign-out" v-if="authenticated" @click="logout()" id="logout" data-cy="logout" title="Sign out">
        <font-awesome-icon icon="sign-out-alt" />
      </a>
      <a class="sign-in" v-if="!authenticated" @click="login()" id="login" data-cy="login">
        <font-awesome-icon icon="sign-in-alt" />
        <span>Sign in</span>
      </a>
    </div>
  </b-navbar>
</template>

<script lang="ts" src="./navbar.component.ts"></script>
