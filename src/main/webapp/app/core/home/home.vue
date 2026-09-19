<template>
  <div class="home">
    <section class="home-hero">
      <div class="home-hero-text">
        <span class="home-eyebrow">Continuous discovery</span>
        <h1 v-if="authenticated && username">Welcome back, {{ username }}</h1>
        <h1 v-else>Opportunity Solution Tree</h1>
        <p class="home-lead">
          Connect the outcome you are chasing to the customer opportunities behind it, the solutions you could build, and the assumptions
          and evidence that tell you which ones are worth it.
        </p>
        <div class="home-actions">
          <router-link v-if="authenticated" to="/trees" class="btn home-btn-primary" data-cy="homeTrees">
            <font-awesome-icon icon="sitemap" />
            <span>Open your trees</span>
          </router-link>
          <router-link v-if="authenticated" to="/teams" class="btn home-btn-ghost" data-cy="homeTeams">
            <font-awesome-icon icon="users" />
            <span>Your teams</span>
          </router-link>
          <button v-if="!authenticated" type="button" class="btn home-btn-primary" @click="login()" data-cy="homeLogin">
            <font-awesome-icon icon="sign-in-alt" />
            <span>Sign in</span>
          </button>
        </div>
      </div>

      <svg class="home-hero-tree" viewBox="0 0 320 220" fill="none" aria-hidden="true">
        <g class="home-tree-lines">
          <path d="M160 46v34M160 80H70v30M160 80h90v30M160 80v30" />
          <path d="M70 140v18M70 158H36v18M70 158h34v18M250 140v36" />
        </g>
        <rect class="home-tree-node is-outcome" x="112" y="14" width="96" height="32" rx="16" />
        <rect class="home-tree-node" x="28" y="110" width="84" height="30" rx="15" />
        <rect class="home-tree-node" x="118" y="110" width="84" height="30" rx="15" />
        <rect class="home-tree-node" x="208" y="110" width="84" height="30" rx="15" />
        <rect class="home-tree-node is-solution" x="10" y="176" width="52" height="26" rx="13" />
        <rect class="home-tree-node is-solution" x="78" y="176" width="52" height="26" rx="13" />
        <rect class="home-tree-node is-solution" x="224" y="176" width="52" height="26" rx="13" />
      </svg>
    </section>

    <section class="home-steps">
      <div v-for="(step, index) in steps" :key="step.title" class="home-step">
        <div class="home-step-icon">
          <font-awesome-icon :icon="step.icon" />
        </div>
        <div>
          <h2>
            <span class="home-step-number">{{ index + 1 }}</span
            >{{ step.title }}
          </h2>
          <p>{{ step.text }}</p>
        </div>
      </div>
    </section>

    <section v-if="authenticated" class="home-links" data-cy="homeJumpBackIn">
      <h2 class="home-section-title">Jump back in</h2>
      <div class="home-link-grid">
        <router-link v-for="link in userLinks" :key="link.path" :to="link.path" class="home-link-card">
          <font-awesome-icon :icon="link.icon" class="home-link-icon" />
          <span class="home-link-label">{{ link.label }}</span>
          <span class="home-link-text">{{ link.text }}</span>
        </router-link>
      </div>
    </section>

    <section v-if="authenticated && isAdmin" class="home-links" data-cy="homeStaticData">
      <h2 class="home-section-title">Static data <span class="home-section-note">Administrators</span></h2>
      <div class="home-link-grid">
        <router-link v-for="link in adminLinks" :key="link.path" :to="link.path" class="home-link-card">
          <font-awesome-icon :icon="link.icon" class="home-link-icon" />
          <span class="home-link-label">{{ link.label }}</span>
          <span class="home-link-text">{{ link.text }}</span>
        </router-link>
      </div>
    </section>
  </div>
</template>

<script lang="ts" src="./home.component.ts"></script>

<style scoped lang="scss">
@import '../../../content/scss/va-variables';

$home-ink: $navbar-bg-color;
$home-accent: $sidebar-menu-accent-color;
$home-muted: #66756f;
$home-border: #e6e4ec;

.home {
  display: flex;
  flex-direction: column;
  gap: 1.75rem;
}

.home-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 2rem;
  padding: 2.75rem 3rem;
  border-radius: 14px;
  color: $navbar-fg-color;
  background:
    radial-gradient(40rem 22rem at 10% 0%, rgba($home-accent, 0.3), transparent 60%),
    linear-gradient(150deg, $navbar-bg-color 0%, $sidebar-menu-tooltip-bg-color 100%);
}

.home-hero-text {
  max-width: 36rem;
}

.home-eyebrow {
  display: inline-block;
  margin-bottom: 0.75rem;
  font-size: 0.75rem;
  font-weight: 600;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: lighten($home-accent, 18%);
}

.home-hero h1 {
  margin-bottom: 0.75rem;
  font-size: 2.25rem;
  font-weight: 700;
  color: #fff;
}

.home-lead {
  margin-bottom: 1.75rem;
  font-size: 1.05rem;
  line-height: 1.6;
  color: rgba($navbar-fg-color, 0.85);
}

.home-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;

  .btn {
    display: inline-flex;
    align-items: center;
    gap: 0.5rem;
    padding: 0.6rem 1.25rem;
    border-radius: 8px;
    font-weight: 600;
  }
}

.home-btn-primary {
  color: #fff;
  background: $home-accent;
  border-color: $home-accent;

  &:hover,
  &:focus-visible {
    color: #fff;
    background: darken($home-accent, 6%);
    box-shadow: 0 6px 16px rgba($home-accent, 0.35);
  }
}

.home-btn-ghost {
  color: $navbar-fg-color;
  border-color: rgba($navbar-fg-color, 0.35);

  &:hover,
  &:focus-visible {
    color: #fff;
    border-color: $navbar-fg-color;
    background: rgba(255, 255, 255, 0.06);
  }
}

.home-hero-tree {
  flex: 0 0 auto;
  width: 320px;
  max-width: 40%;
}

.home-tree-lines path {
  stroke: rgba($navbar-fg-color, 0.35);
  stroke-width: 2;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.home-tree-node {
  fill: rgba(255, 255, 255, 0.1);
  stroke: rgba($navbar-fg-color, 0.45);
  stroke-width: 1.5;

  &.is-outcome {
    fill: $home-accent;
    stroke: none;
  }

  &.is-solution {
    fill: rgba($home-accent, 0.35);
    stroke: rgba($home-accent, 0.9);
  }
}

.home-steps {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(14rem, 1fr));
  gap: 1rem;
}

.home-step {
  display: flex;
  gap: 0.9rem;
  padding: 1.25rem;
  border: 1px solid $home-border;
  border-radius: 12px;
  background: #fff;

  h2 {
    margin-bottom: 0.35rem;
    font-size: 1rem;
    font-weight: 600;
    color: $home-ink;
  }

  p {
    margin: 0;
    font-size: 0.9rem;
    line-height: 1.5;
    color: $home-muted;
  }
}

.home-step-number {
  margin-right: 0.4rem;
  color: $home-accent;
}

.home-step-icon {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  width: 2.5rem;
  height: 2.5rem;
  border-radius: 10px;
  color: $home-accent;
  background: rgba($home-accent, 0.12);
}

.home-section-title {
  margin-bottom: 0.9rem;
  font-size: 1.1rem;
  font-weight: 600;
  color: $home-ink;
}

.home-section-note {
  margin-left: 0.5rem;
  font-size: 0.75rem;
  font-weight: 500;
  color: $home-muted;
}

.home-link-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(12rem, 1fr));
  gap: 1rem;
}

.home-link-card {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  padding: 1.1rem 1.25rem;
  border: 1px solid $home-border;
  border-radius: 12px;
  color: $home-ink;
  text-decoration: none;
  background: $main-content-bg-color;
  transition:
    border-color 0.15s ease,
    box-shadow 0.15s ease,
    transform 0.15s ease;

  &:hover,
  &:focus-visible {
    border-color: $home-accent;
    box-shadow: 0 8px 20px rgba($navbar-bg-color, 0.12);
    transform: translateY(-2px);
  }
}

.home-link-icon {
  margin-bottom: 0.4rem;
  font-size: 1.25rem;
  color: $home-accent;
}

.home-link-label {
  font-weight: 600;
}

.home-link-text {
  font-size: 0.85rem;
  color: $home-muted;
}

@media (max-width: 991px) {
  .home-hero {
    padding: 2rem 1.5rem;
  }

  .home-hero-tree {
    display: none;
  }
}
</style>
