import { type ComputedRef, defineComponent, inject } from 'vue';

import type LoginService from '@/account/login.service';

export default defineComponent({
  setup() {
    const { login } = inject<LoginService>('loginService');
    const authenticated = inject<ComputedRef<boolean>>('authenticated');
    const username = inject<ComputedRef<string>>('currentUsername');

    return {
      authenticated,
      username,
      login,
    };
  },
});
