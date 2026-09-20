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
/*
 * Colours come from content/css/theme.css (the shared palette), so the home page follows the
 * light / dark toggle. Sass colour functions (lighten / darken / rgba) cannot operate on custom
 * properties — color-mix() does the same job at runtime and keeps one source of hex.
 */

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
  /* The hero is a dark plate in both themes — the Nocturne ground, like the canvas. */
  color: var(--ost-nocturne-text);
  background:
    radial-gradient(40rem 22rem at 10% 0%, color-mix(in srgb, var(--ost-nocturne-accent) 30%, transparent), transparent 60%),
    linear-gradient(150deg, var(--ost-nocturne-ground) 0%, var(--ost-accent-900) 100%);
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
  color: var(--ost-accent-300);
}

.home-hero h1 {
  margin-bottom: 0.75rem;
  font-size: 2.25rem;
  font-weight: 700;
  color: var(--ost-neutral-100);
}

.home-lead {
  margin-bottom: 1.75rem;
  font-size: 1.05rem;
  line-height: 1.6;
  color: var(--ost-neutral-300);
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
  color: var(--ost-nocturne-ground);
  background: var(--ost-nocturne-accent);
  border-color: var(--ost-nocturne-accent);

  &:hover,
  &:focus-visible {
    color: var(--ost-nocturne-ground);
    background: var(--ost-accent-400);
    border-color: var(--ost-accent-400);
    box-shadow: 0 6px 16px color-mix(in srgb, var(--ost-nocturne-accent) 35%, transparent);
  }
}

.home-btn-ghost {
  color: var(--ost-neutral-200);
  border-color: color-mix(in srgb, var(--ost-neutral-200) 35%, transparent);

  &:hover,
  &:focus-visible {
    color: var(--ost-neutral-100);
    border-color: var(--ost-neutral-200);
    background: color-mix(in srgb, var(--ost-neutral-100) 6%, transparent);
  }
}

.home-hero-tree {
  flex: 0 0 auto;
  width: 320px;
  max-width: 40%;
}

.home-tree-lines path {
  stroke: color-mix(in srgb, var(--ost-neutral-200) 35%, transparent);
  stroke-width: 2;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.home-tree-node {
  fill: color-mix(in srgb, var(--ost-neutral-100) 10%, transparent);
  stroke: color-mix(in srgb, var(--ost-neutral-200) 45%, transparent);
  stroke-width: 1.5;

  &.is-outcome {
    fill: var(--ost-nocturne-accent);
    stroke: none;
  }

  &.is-solution {
    fill: color-mix(in srgb, var(--ost-nocturne-accent) 35%, transparent);
    stroke: color-mix(in srgb, var(--ost-nocturne-accent) 90%, transparent);
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
  border: 1px solid var(--ost-border);
  border-radius: 12px;
  background: var(--ost-surface);

  h2 {
    margin-bottom: 0.35rem;
    font-size: 1rem;
    font-weight: 600;
    color: var(--ost-text);
  }

  p {
    margin: 0;
    font-size: 0.9rem;
    line-height: 1.5;
    color: var(--ost-text-muted);
  }
}

.home-step-number {
  margin-right: 0.4rem;
  color: var(--ost-accent);
}

.home-step-icon {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  width: 2.5rem;
  height: 2.5rem;
  border-radius: 10px;
  color: var(--ost-accent);
  background: var(--ost-accent-soft);
}

.home-section-title {
  margin-bottom: 0.9rem;
  font-size: 1.1rem;
  font-weight: 600;
  color: var(--ost-text);
}

.home-section-note {
  margin-left: 0.5rem;
  font-size: 0.75rem;
  font-weight: 500;
  color: var(--ost-text-muted);
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
  border: 1px solid var(--ost-border);
  border-radius: 12px;
  color: var(--ost-text);
  text-decoration: none;
  background: var(--ost-surface);
  transition:
    border-color 0.15s ease,
    box-shadow 0.15s ease,
    transform 0.15s ease;

  &:hover,
  &:focus-visible {
    border-color: var(--ost-accent);
    box-shadow: var(--ost-shadow);
    transform: translateY(-2px);
  }
}

.home-link-icon {
  margin-bottom: 0.4rem;
  font-size: 1.25rem;
  color: var(--ost-accent);
}

.home-link-label {
  font-weight: 600;
}

.home-link-text {
  font-size: 0.85rem;
  color: var(--ost-text-muted);
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
