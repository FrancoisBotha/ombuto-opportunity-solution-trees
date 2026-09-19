import { type ComputedRef, computed, defineComponent, inject } from 'vue';
import { useRouter } from 'vue-router';

import type LoginService from '@/account/login.service';
import { useStore } from '@/store';

import ThemeToggle from './theme-toggle.vue';

export default defineComponent({
  name: 'Navbar',
  components: { ThemeToggle },
  setup() {
    const loginService = inject<LoginService>('loginService');
    const { login } = loginService;
    const username = inject<ComputedRef<string>>('currentUsername');

    const router = useRouter();
    const store = useStore();

    const version = `v${APP_VERSION}`;
    const authenticated = computed(() => store.authenticated);

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
      login,
      logout,
      username,
      version,
      authenticated,
    };
  },
});
