import { beforeEach, describe, expect, it, vitest } from 'vitest';
import { type Ref, ref } from 'vue';
import { type RouteLocation } from 'vue-router';

import { createTestingPinia } from '@pinia/testing';
import { type ComponentMountingOptions, shallowMount } from '@vue/test-utils';

import Error from './error.vue';

type ErrorComponentType = InstanceType<typeof Error>;

let route: Partial<RouteLocation>;

vitest.mock('vue-router', () => ({
  useRoute: () => route,
}));

const customErrorMsg = 'An error occurred.';

describe('Error component', () => {
  let error: ErrorComponentType;
  let loginService: LoginService;
  let authenticated: Ref<boolean>;
  let mountOptions: ComponentMountingOptions<ErrorComponentType>;

  beforeEach(() => {
    route = {};
    authenticated = ref(false);
    loginService = { login: vitest.fn(), logout: vitest.fn() };
    mountOptions = {
      global: {
        plugins: [createTestingPinia()],
        provide: {
          loginService,
          authenticated,
        },
      },
    };
  });

  it('should have retrieve custom error on routing', () => {
    route = {
      path: '/custom-error',
      name: 'CustomMessage',
      meta: { errorMessage: customErrorMsg },
    };
    const wrapper = shallowMount(Error, mountOptions);
    error = wrapper.vm;

    expect(error.errorMessage).toBe(customErrorMsg);
    expect(error.error403).toBeFalsy();
    expect(error.error404).toBeFalsy();
    expect(loginService.login).not.toHaveBeenCalled();
  });

  it('should have set forbidden error on routing', () => {
    route = {
      meta: { error403: true },
    };
    const wrapper = shallowMount(Error, mountOptions);
    error = wrapper.vm;

    expect(error.errorMessage).toBeNull();
    expect(error.error403).toBeTruthy();
    expect(error.error404).toBeFalsy();
    expect(loginService.login).toHaveBeenCalled();
  });

  it('should have set not found error on routing', () => {
    route = {
      meta: { error404: true },
    };
    const wrapper = shallowMount(Error, mountOptions);
    error = wrapper.vm;

    expect(error.errorMessage).toBeNull();
    expect(error.error403).toBeFalsy();
    expect(error.error404).toBeTruthy();
    expect(loginService.login).not.toHaveBeenCalled();
  });

  it('should have set default on no error', () => {
    const wrapper = shallowMount(Error, mountOptions);
    error = wrapper.vm;

    expect(error.errorMessage).toBeNull();
    expect(error.error403).toBeFalsy();
    expect(error.error404).toBeFalsy();
    expect(loginService.login).not.toHaveBeenCalled();
  });
});
