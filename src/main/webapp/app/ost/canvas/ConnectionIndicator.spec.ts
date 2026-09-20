import { describe, expect, it } from 'vitest';

import { mount } from '@vue/test-utils';
import { createPinia, setActivePinia } from 'pinia';

import { type OstConnectionState, useOstRealtimeStore } from '../stores/ost-realtime.store';

import CanvasToolbar from './CanvasToolbar.vue';
import ConnectionIndicator from './ConnectionIndicator.vue';

/** RTC-006 / FR-035: the canvas says whether the realtime connection is live, reconnecting or offline. */
describe('ConnectionIndicator', () => {
  function mountIndicator(state: OstConnectionState) {
    setActivePinia(createPinia());
    const realtime = useOstRealtimeStore();
    realtime.connectionState = state;
    return { wrapper: mount(ConnectionIndicator, { attachTo: document.body }), realtime };
  }

  const cases: [OstConnectionState, string][] = [
    ['live', 'Live'],
    ['reconnecting', 'Reconnecting'],
    ['offline', 'Offline'],
  ];

  it.each(cases)('reports %s from the realtime store, in words and in its accessible name', (state, label) => {
    const { wrapper } = mountIndicator(state);
    const el = wrapper.get('[data-cy="ost-connection"]');
    expect(el.attributes('data-state')).toBe(state);
    expect(el.text()).toBe(label);
    expect(el.attributes('aria-label')).toBe(`Connection: ${label}`);
    expect(el.attributes('role')).toBe('status');
    wrapper.unmount();
  });

  it('follows the store without a remount (a drop and a recovery are both visible)', async () => {
    const { wrapper, realtime } = mountIndicator('live');
    const el = () => wrapper.get('[data-cy="ost-connection"]');
    realtime.connectionState = 'reconnecting';
    await wrapper.vm.$nextTick();
    expect(el().attributes('data-state')).toBe('reconnecting');
    expect(el().text()).toBe('Reconnecting');
    realtime.connectionState = 'live';
    await wrapper.vm.$nextTick();
    expect(el().attributes('data-state')).toBe('live');
    wrapper.unmount();
  });

  it('is not colour alone: each state has its own dot colour AND its own word', () => {
    const seen = new Map<string, string>();
    for (const [state] of cases) {
      const { wrapper } = mountIndicator(state);
      const el = wrapper.get('[data-cy="ost-connection"]');
      seen.set(el.text(), wrapper.get('.ost-connection__dot').attributes('style') ?? '');
      wrapper.unmount();
    }
    expect(seen.size).toBe(3);
    expect(new Set(seen.values()).size).toBe(3);
  });

  it('sits in the canvas toolbar, inside the zoom group (epic §7)', () => {
    setActivePinia(createPinia());
    const wrapper = mount(CanvasToolbar, { props: { zoom: 1 }, attachTo: document.body });
    const indicator = wrapper.get('[data-cy="ost-connection"]');
    expect(indicator.element.closest('.ost-toolbar__zoom')).not.toBeNull();
    expect(wrapper.get('[data-cy="ost-zoom-level"]').element.closest('.ost-toolbar__zoom')).toBe(
      indicator.element.closest('.ost-toolbar__zoom'),
    );
    wrapper.unmount();
  });
});
