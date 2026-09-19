import { describe, expect, it } from 'vitest';

import { shallowMount } from '@vue/test-utils';

import OstPlaceholder from './ost-placeholder.vue';

describe('OstPlaceholder Component', () => {
  it('tells the user the tree builder is coming soon', () => {
    const wrapper = shallowMount(OstPlaceholder, { global: { stubs: { 'router-link': true } } });

    expect(wrapper.find('[data-cy="ostPlaceholder"]').text()).toContain('Tree builder coming soon');
  });
});
