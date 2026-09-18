// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.common with an alias.
import { computed, createApp, provide } from 'vue';

import { createPinia, storeToRefs } from 'pinia';

import AccountService from '@/account/account.service';
import LoginService from '@/account/login.service';
import { setupAxiosInterceptors } from '@/shared/config/axios-interceptor';
import { initFortAwesome } from '@/shared/config/config';
import { initBootstrapVue } from '@/shared/config/config-bootstrap-vue';
import JhiItemCount from '@/shared/jhi-item-count.vue';
import JhiSortIndicator from '@/shared/sort/jhi-sort-indicator.vue';
import { useStore } from '@/store';

import App from './app.vue';
import router from './router';

import '../content/scss/global.scss';
import '../content/scss/va-navbar.scss';
import '../content/scss/va-sidemenu.scss';
import '../content/scss/vendor.scss';

const pinia = createPinia();

// jhipster-needle-add-entity-service-to-main-import - JHipster will import entities services here

const app = createApp({
  components: { App },
  setup() {
    provide('loginService', new LoginService());
    const store = useStore();
    const accountService = new AccountService(store);
    provide(
      'currentLanguage',
      computed(() => store.account?.langKey ?? navigator.language ?? 'en'),
    );

    router.beforeResolve(async (to, from, next) => {
      if (!store.authenticated) {
        await accountService.update();
      }
      if (to.meta?.authorities && to.meta.authorities.length > 0) {
        const value = await accountService.hasAnyAuthorityAndCheckAuth(to.meta.authorities);
        if (!value) {
          if (from.path !== '/forbidden') {
            next({ path: '/forbidden' });
            return;
          }
        }
      }
      next();
    });

    setupAxiosInterceptors(
      error => {
        const url = error.response?.config?.url;
        const status = error.status || error.response?.status;
        if (status === 401) {
          // Store logged out state.
          store.logout();
          if (!url.endsWith('api/account') && !url.endsWith('api/authenticate')) {
            // Ask for a new authentication
            window.location.reload();
            return;
          }
        }
        return Promise.reject(error);
      },
      error => {
        return Promise.reject(error);
      },
    );

    const { authenticated } = storeToRefs(store);
    provide('authenticated', authenticated);
    provide(
      'currentUsername',
      computed(() => store.account?.login),
    );

    provide('accountService', accountService);
    // jhipster-needle-add-entity-service-to-main - JHipster will import entities services here
  },
  template: '<App/>',
});

initFortAwesome(app);

initBootstrapVue(app);

app.component('JhiItemCount', JhiItemCount).component('JhiSortIndicator', JhiSortIndicator).use(router).use(pinia).mount('#app');
