<template>
  <b-navbar class="va-navbar" data-cy="navbar" data-bs-theme="dark">
    <b-navbar-brand class="logo" to="/">
      <span class="logo-img"></span>
      <span class="navbar-title">Opportunity Solution Tree</span>
      <span class="navbar-version">{{ version }}</span>
    </b-navbar-brand>

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
  </b-navbar>
</template>

<script lang="ts" src="./navbar.component.ts"></script>
