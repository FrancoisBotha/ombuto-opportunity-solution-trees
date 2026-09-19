import { type Ref, computed, defineComponent, inject, ref, watch } from 'vue';
import { useRouter } from 'vue-router';

import type AccountService from '@/account/account.service';
import { useStore } from '@/store';
import { storeToRefs } from 'pinia';

export interface SidebarLink {
  path: string;
  label: string;
  icon: string;
  /** Match on prefix instead of exact path (useful for entity pages with /new, /:id/edit ...) */
  prefix?: boolean;
}

export interface SidebarGroup {
  key: string;
  label: string;
  icon: string;
  links: SidebarLink[];
  /** When set, the group is only shown to users holding this authority. */
  authority?: string;
}

const STORAGE_KEY = 'va-sidebar-expanded';

/**
 * Raw CRUD screens for every entity: the tree in the order Teresa Torres draws it, then the
 * discovery and collaboration entities. Administrators only — everyone else works in Trees and Teams.
 */
export const staticDataGroup: SidebarGroup = {
  key: 'staticData',
  label: 'Static Data',
  icon: 'database',
  authority: 'ROLE_ADMIN',
  links: [
    { path: '/team', label: 'Teams', icon: 'users', prefix: true },
    { path: '/team-member', label: 'Team Members', icon: 'user', prefix: true },
    { path: '/product', label: 'Products', icon: 'box', prefix: true },
    { path: '/outcome', label: 'Outcomes', icon: 'bullseye', prefix: true },
    { path: '/opportunity', label: 'Opportunities', icon: 'lightbulb', prefix: true },
    { path: '/opportunity-link', label: 'Opportunity Links', icon: 'link', prefix: true },
    { path: '/solution', label: 'Solutions', icon: 'puzzle-piece', prefix: true },
    { path: '/solution-link', label: 'Solution Links', icon: 'link', prefix: true },
    { path: '/assumption', label: 'Assumptions', icon: 'question-circle', prefix: true },
    { path: '/experiment', label: 'Experiments', icon: 'vial', prefix: true },
    { path: '/interview', label: 'Interviews', icon: 'comments', prefix: true },
    { path: '/comment', label: 'Comments', icon: 'comment', prefix: true },
    { path: '/tag', label: 'Tags', icon: 'tag', prefix: true },
    // jhipster-needle-add-entity-to-menu - JHipster will add entities to the menu here
  ],
};

export default defineComponent({
  name: 'SidebarMenu',
  props: {
    expanded: { type: Boolean, default: true },
  },
  emits: ['update:expanded'],
  setup(props, { emit }) {
    const accountService = inject<AccountService>('accountService');
    const router = useRouter();
    const store = useStore();

    const hasAnyAuthorityValues: Ref<any> = ref({});
    const openAPIEnabled = computed(() => store.activeProfiles.includes('api-docs'));
    const inProduction = computed(() => store.activeProfiles.includes('prod'));
    const { authenticated } = storeToRefs(store);

    const currentPath = computed(() => router.currentRoute.value.path);
    const isTeamsActive = computed(() => currentPath.value === '/teams' || currentPath.value.startsWith('/teams/'));
    const isTreesActive = computed(() => currentPath.value === '/trees' || currentPath.value.startsWith('/trees/'));

    const isLinkActive = (link: SidebarLink) =>
      link.prefix ? currentPath.value === link.path || currentPath.value.startsWith(`${link.path}/`) : currentPath.value === link.path;

    const groupHasActive = (group: SidebarGroup) => group.links.some(isLinkActive);

    const toggleSidebar = () => {
      const next = !props.expanded;
      try {
        localStorage.setItem(STORAGE_KEY, String(next));
      } catch {
        /* storage unavailable */
      }
      emit('update:expanded', next);
    };

    // Collapsible groups. A group opens automatically when one of its links is the current route.
    const openGroups = ref<Record<string, boolean>>({
      staticData: true,
      admin: false,
    });
    const toggleGroup = (key: string) => {
      openGroups.value = { ...openGroups.value, [key]: !openGroups.value[key] };
    };
    const groupIcon = (key: string) => (openGroups.value[key] ? 'chevron-up' : 'chevron-down');

    watch(
      currentPath,
      path => {
        for (const group of [staticDataGroup]) {
          if (groupHasActive(group) && !openGroups.value[group.key]) {
            openGroups.value = { ...openGroups.value, [group.key]: true };
          }
        }
        if (path.startsWith('/admin') && !openGroups.value.admin) {
          openGroups.value = { ...openGroups.value, admin: true };
        }
      },
      { immediate: true },
    );

    return {
      accountService,
      hasAnyAuthorityValues,
      openAPIEnabled,
      inProduction,
      authenticated,
      currentPath,
      isTeamsActive,
      isTreesActive,
      groups: [staticDataGroup],
      isLinkActive,
      groupHasActive,
      toggleSidebar,
      openGroups,
      toggleGroup,
      groupIcon,
    };
  },
  methods: {
    hasAnyAuthority(authorities: any): boolean {
      this.accountService.hasAnyAuthorityAndCheckAuth(authorities).then(value => {
        if (this.hasAnyAuthorityValues[authorities] !== value) {
          this.hasAnyAuthorityValues = { ...this.hasAnyAuthorityValues, [authorities]: value };
        }
      });
      return this.hasAnyAuthorityValues[authorities] ?? false;
    },
  },
});

export function readStoredSidebarState(defaultValue = true): boolean {
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    return stored === null ? defaultValue : stored === 'true';
  } catch {
    return defaultValue;
  }
}
