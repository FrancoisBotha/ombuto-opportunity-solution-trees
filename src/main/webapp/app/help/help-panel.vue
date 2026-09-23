<template>
  <span class="help-panel-root">
    <a
      ref="linkRef"
      class="help-link"
      href="#"
      role="button"
      data-cy="helpLink"
      :aria-expanded="open"
      aria-haspopup="dialog"
      @click.prevent="openPanel"
    >
      <font-awesome-icon icon="question-circle" />
      <span>Help</span>
    </a>

    <div v-if="open" class="help-panel-backdrop" @click="closePanel"></div>

    <aside
      v-if="open"
      ref="panelRef"
      class="help-panel"
      role="dialog"
      aria-modal="true"
      aria-label="Help guide"
      tabindex="-1"
      data-cy="helpPanel"
    >
      <header class="help-panel-header">
        <h2 v-if="!selectedTopic" class="help-panel-title">Help guide</h2>
        <h2 v-else class="help-panel-title">{{ selectedTopic.title }}</h2>
        <button type="button" class="help-panel-close" aria-label="Close help" data-cy="helpClose" @click="closePanel">&times;</button>
      </header>

      <div class="help-panel-body">
        <template v-if="!selectedTopic">
          <div class="help-search-row">
            <label for="helpSearchInput" class="visually-hidden">Search help topics</label>
            <input
              id="helpSearchInput"
              ref="searchRef"
              v-model="query"
              type="search"
              class="form-control help-search"
              placeholder="Search help topics"
              data-cy="helpSearch"
            />
          </div>

          <ul v-if="filteredTopics.length > 0" class="help-topic-list">
            <li v-for="topic in filteredTopics" :key="topic.id" class="help-topic-item">
              <button type="button" class="help-topic-button" data-cy="helpTopicItem" @click="selectTopic(topic.id)">
                <span class="help-topic-title">{{ topic.title }}</span>
                <span class="help-topic-summary">{{ topic.summary }}</span>
              </button>
            </li>
          </ul>
          <p v-else class="help-empty" data-cy="helpEmpty">No matching topics</p>
        </template>

        <template v-else>
          <button type="button" class="help-back" data-cy="helpBack" @click="backToList">&larr; Back to topics</button>
          <p class="help-topic-summary">{{ selectedTopic.summary }}</p>
          <ol class="help-topic-steps" data-cy="helpTopicSteps">
            <li v-for="(step, i) in selectedTopic.steps" :key="i">{{ step }}</li>
          </ol>
          <p v-if="selectedTopic.roleNote" class="help-role-note" data-cy="helpRoleNote">
            {{ selectedTopic.roleNote }}
          </p>
        </template>
      </div>
    </aside>
  </span>
</template>

<script lang="ts" src="./help-panel.component.ts"></script>

<style scoped>
.help-link {
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
}
.help-panel-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.35);
  z-index: 1050;
}
.help-panel {
  position: fixed;
  top: 0;
  right: 0;
  bottom: 0;
  width: min(420px, 100vw);
  background: var(--bs-body-bg, #fff);
  color: var(--bs-body-color, inherit);
  box-shadow: -4px 0 16px rgba(0, 0, 0, 0.15);
  z-index: 1055;
  display: flex;
  flex-direction: column;
  outline: none;
}
.help-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem;
  border-bottom: 1px solid var(--bs-border-color, #dee2e6);
}
.help-panel-title {
  font-size: 1.15rem;
  margin: 0;
}
.help-panel-close {
  background: none;
  border: 0;
  font-size: 1.5rem;
  line-height: 1;
  cursor: pointer;
}
.help-panel-body {
  padding: 1rem;
  overflow-y: auto;
  flex: 1;
}
.help-search {
  width: 100%;
  margin-bottom: 1rem;
}
.help-topic-list {
  list-style: none;
  padding: 0;
  margin: 0;
}
.help-topic-item + .help-topic-item {
  margin-top: 0.5rem;
}
.help-topic-button {
  background: none;
  border: 1px solid var(--bs-border-color, #dee2e6);
  border-radius: 6px;
  width: 100%;
  text-align: left;
  padding: 0.75rem;
  cursor: pointer;
  color: inherit;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}
.help-topic-button:hover,
.help-topic-button:focus {
  border-color: var(--bs-primary, #0d6efd);
  outline: none;
}
.help-topic-title {
  font-weight: 600;
}
.help-topic-summary {
  color: var(--bs-secondary-color, #6c757d);
}
.help-back {
  background: none;
  border: 0;
  padding: 0;
  margin-bottom: 0.75rem;
  cursor: pointer;
  color: var(--bs-primary, #0d6efd);
}
.help-topic-steps {
  padding-left: 1.25rem;
}
.help-topic-steps li + li {
  margin-top: 0.25rem;
}
.help-role-note {
  margin-top: 1rem;
  padding: 0.75rem;
  border-left: 3px solid var(--bs-primary, #0d6efd);
  background: var(--bs-secondary-bg, #f8f9fa);
}
.visually-hidden {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}
</style>
