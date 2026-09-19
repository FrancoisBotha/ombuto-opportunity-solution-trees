<template>
  <nav :class="['sidebar', { active: expanded }]" data-cy="sidebar">
    <div class="menu-toggle-button" @click="toggleSidebar" data-cy="sidebarToggle" :title="expanded ? 'Collapse menu' : 'Expand menu'">
      <font-awesome-icon icon="bars" />
    </div>

    <ul>
      <!-- Home -->
      <li :class="{ active: currentPath === '/' }">
        <router-link to="/" class="menu-item">
          <font-awesome-icon class="va-icon" icon="home" />
          <span class="nav-item">Home</span>
        </router-link>
        <span class="menu-tooltip">Home</span>
      </li>

      <!-- My teams (signed-in users) -->
      <li v-if="authenticated" :class="{ active: isTeamsActive }" data-cy="myTeamsMenu">
        <router-link to="/teams" class="menu-item">
          <font-awesome-icon class="va-icon" icon="users" />
          <span class="nav-item">Teams</span>
        </router-link>
        <span class="menu-tooltip">Teams</span>
      </li>

      <!-- Trees (signed-in users) -->
      <li v-if="authenticated" :class="{ active: isTreesActive }" data-cy="treesMenu">
        <router-link to="/trees" class="menu-item">
          <font-awesome-icon class="va-icon" icon="sitemap" />
          <span class="nav-item">Trees</span>
        </router-link>
        <span class="menu-tooltip">Trees</span>
      </li>

      <!-- Entity groups -->
      <template v-for="group in groups" :key="group.key">
        <li class="section" :class="{ active: !openGroups[group.key] && groupHasActive(group) }">
          <div class="menu-item-parent" @click="toggleGroup(group.key)">
            <font-awesome-icon class="va-icon" :icon="group.icon" />
            <span class="nav-item">{{ group.label }}</span>
            <font-awesome-icon class="va-icon-toggle" :icon="groupIcon(group.key)" />
          </div>
          <span class="menu-tooltip">{{ group.label }}</span>
        </li>
        <transition name="slide">
          <div v-show="openGroups[group.key]">
            <li v-for="link in group.links" :key="link.path" :class="{ active: isLinkActive(link) }">
              <router-link :to="link.path" class="menu-item menu-item-child">
                <font-awesome-icon class="va-icon" :icon="link.icon" />
                <span class="nav-item">{{ link.label }}</span>
              </router-link>
              <span class="menu-tooltip">{{ link.label }}</span>
            </li>
          </div>
        </transition>
      </template>

      <!-- Admin (ROLE_ADMIN only) -->
      <template v-if="hasAnyAuthority('ROLE_ADMIN')">
        <li class="section" :class="{ active: !openGroups.admin && currentPath.startsWith('/admin') }" data-cy="adminMenu">
          <div class="menu-item-parent" @click="toggleGroup('admin')">
            <font-awesome-icon class="va-icon" icon="wrench" />
            <span class="nav-item">System</span>
            <font-awesome-icon class="va-icon-toggle" :icon="groupIcon('admin')" />
          </div>
          <span class="menu-tooltip">System</span>
        </li>
        <transition name="slide">
          <div v-show="openGroups.admin">
            <li :class="{ active: currentPath === '/admin/metrics' }">
              <router-link to="/admin/metrics" class="menu-item menu-item-child">
                <font-awesome-icon class="va-icon" icon="tachometer-alt" />
                <span class="nav-item">Metrics</span>
              </router-link>
              <span class="menu-tooltip">Metrics</span>
            </li>
            <li :class="{ active: currentPath === '/admin/health' }">
              <router-link to="/admin/health" class="menu-item menu-item-child">
                <font-awesome-icon class="va-icon" icon="heart" />
                <span class="nav-item">Health</span>
              </router-link>
              <span class="menu-tooltip">Health</span>
            </li>
            <li :class="{ active: currentPath === '/admin/configuration' }">
              <router-link to="/admin/configuration" class="menu-item menu-item-child">
                <font-awesome-icon class="va-icon" icon="cogs" />
                <span class="nav-item">Configuration</span>
              </router-link>
              <span class="menu-tooltip">Configuration</span>
            </li>
            <li :class="{ active: currentPath === '/admin/logs' }">
              <router-link to="/admin/logs" class="menu-item menu-item-child">
                <font-awesome-icon class="va-icon" icon="tasks" />
                <span class="nav-item">Logs</span>
              </router-link>
              <span class="menu-tooltip">Logs</span>
            </li>
            <li :class="{ active: currentPath === '/admin/tracker' }">
              <router-link to="/admin/tracker" class="menu-item menu-item-child">
                <font-awesome-icon class="va-icon" icon="users" />
                <span class="nav-item">User tracker</span>
              </router-link>
              <span class="menu-tooltip">User tracker</span>
            </li>
            <li v-if="openAPIEnabled" :class="{ active: currentPath === '/admin/docs' }">
              <router-link to="/admin/docs" class="menu-item menu-item-child">
                <font-awesome-icon class="va-icon" icon="book" />
                <span class="nav-item">API</span>
              </router-link>
              <span class="menu-tooltip">API</span>
            </li>
            <li v-if="!inProduction">
              <a href="http://localhost:8092/" target="_tab" class="menu-item menu-item-child">
                <font-awesome-icon class="va-icon" icon="database" />
                <span class="nav-item">Database</span>
              </a>
              <span class="menu-tooltip">Database</span>
            </li>
          </div>
        </transition>
      </template>
    </ul>
  </nav>
</template>

<script lang="ts" src="./sidebar-menu.component.ts"></script>
