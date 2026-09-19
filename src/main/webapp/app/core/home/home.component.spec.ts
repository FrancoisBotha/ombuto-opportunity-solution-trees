import { beforeEach, describe, expect, it } from 'vitest';
import { ref } from 'vue';

import { createTestingPinia } from '@pinia/testing';
import { mount, shallowMount } from '@vue/test-utils';

import { useStore } from '@/store';

import { adminLinks, userLinks } from './home.component';
import Home from './home.vue';

type HomeComponentType = InstanceType<typeof Home>;

const RouterLinkStub = { props: ['to'], template: '<a :href="to"><slot /></a>' };

describe('Home', () => {
  let home: HomeComponentType;
  let authenticated;
  let currentUsername;

  const loginService = { login: vitest.fn(), logout: vitest.fn() };
  beforeEach(() => {
    authenticated = ref(false);
    currentUsername = ref('');
    const wrapper = shallowMount(Home, {
      global: {
        plugins: [createTestingPinia()],
        stubs: {
          'router-link': true,
        },
        provide: {
          loginService,
          authenticated,
          currentUsername,
        },
      },
    });
    home = wrapper.vm;
  });

  it('should not have user data set', () => {
    expect(home.authenticated).toBeFalsy();
    expect(home.username).toBe('');
  });

  it('should have user data set after authentication', () => {
    authenticated.value = true;
    currentUsername.value = 'test';

    expect(home.authenticated).toBeTruthy();
    expect(home.username).toBe('test');
  });

  it('should use login service', () => {
    home.login();
    expect(loginService.login).toHaveBeenCalled();
  });
});

describe('Home links (C5: no admin-only destinations for normal users)', () => {
  const mountAs = (authorities: string[]) => {
    const pinia = createTestingPinia();
    useStore(pinia).userIdentity = { login: 'someone', authorities };
    return mount(Home, {
      global: {
        plugins: [pinia],
        stubs: { 'router-link': RouterLinkStub, 'font-awesome-icon': true },
        provide: {
          loginService: { login: vitest.fn() },
          authenticated: ref(true),
          currentUsername: ref('someone'),
        },
      },
    });
  };
  const hrefs = (wrapper: ReturnType<typeof mountAs>) => wrapper.findAll('a').map(a => a.attributes('href'));

  it('sends a plain user only to Trees and Teams', () => {
    const wrapper = mountAs(['ROLE_USER']);
    const targets = hrefs(wrapper);
    expect(targets).toContain('/trees');
    expect(targets).toContain('/teams');
    for (const admin of adminLinks) {
      expect(targets).not.toContain(admin.path);
    }
    expect(wrapper.find('[data-cy="homeStaticData"]').exists()).toBe(false);
  });

  it('shows the Static Data cards to administrators as well', () => {
    const wrapper = mountAs(['ROLE_USER', 'ROLE_ADMIN']);
    const targets = hrefs(wrapper);
    for (const link of [...userLinks, ...adminLinks]) {
      expect(targets).toContain(link.path);
    }
  });

  it('describes the current model (assumptions and evidence), not experiments', () => {
    const text = mountAs(['ROLE_USER']).text();
    expect(text).not.toMatch(/experiment/i);
    expect(text).toMatch(/assumptions/i);
    expect(text).toMatch(/evidence/i);
  });
});
