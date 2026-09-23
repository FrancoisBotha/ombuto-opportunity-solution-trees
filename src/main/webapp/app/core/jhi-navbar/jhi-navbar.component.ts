import { type Ref, computed, defineComponent, inject, ref } from 'vue';
import { useRouter } from 'vue-router';

import type AccountService from '@/account/account.service';
import type LoginService from '@/account/login.service';
import EntitiesMenu from '@/entities/entities-menu.vue';
import { useStore } from '@/store';

export default defineComponent({
  name: 'JhiNavbar',
  components: {
    'entities-menu': EntitiesMenu,
  },
  setup() {
    const loginService = inject<LoginService>('loginService');
    const { login } = loginService;

    const accountService = inject<AccountService>('accountService');
    const currentLanguage = inject('currentLanguage', () => computed(() => navigator.language ?? 'en'), true);

    const router = useRouter();
    const store = useStore();

    const version = `v${APP_VERSION}${import.meta.env.DEV ? '-dev' : ''}`;
    const hasAnyAuthorityValues: Ref<any> = ref({});

    const openAPIEnabled = computed(() => store.activeProfiles.includes('api-docs'));
    const inProduction = computed(() => store.activeProfiles.includes('prod'));
    const authenticated = computed(() => store.authenticated);

    const subIsActive = (input: string | string[]) => {
      const paths = Array.isArray(input) ? input : [input];
      return paths.some(path => {
        return router.currentRoute.value.path.startsWith(path); // current path starts with this path string
      });
    };

    const logout = async () => {
      const response = await loginService.logout();
      store.logout();
      window.location.href = response.data.logoutUrl;
      const next = response.data?.logoutUrl ?? '/';
      if (router.currentRoute.value.path !== next) {
        await router.push(next);
      }
    };

    return {
      logout,
      subIsActive,
      accountService,
      login,
      version,
      currentLanguage,
      hasAnyAuthorityValues,
      openAPIEnabled,
      inProduction,
      authenticated,
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
