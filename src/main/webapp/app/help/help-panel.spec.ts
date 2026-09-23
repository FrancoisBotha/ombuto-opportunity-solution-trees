import { describe, expect, it, vi } from 'vitest';

import { mount } from '@vue/test-utils';
import { nextTick } from 'vue';

import HelpPanel from './help-panel.vue';
import { helpTopics } from './help-topics';

const mountPanel = () => mount(HelpPanel, { attachTo: document.body });

describe('HelpPanel', () => {
  it('renders a Help link and no panel until it is clicked', async () => {
    const wrapper = mountPanel();
    expect(wrapper.find('[data-cy="helpLink"]').exists()).toBe(true);
    expect(wrapper.find('[data-cy="helpPanel"]').exists()).toBe(false);
    wrapper.unmount();
  });

  it('opens the panel with every topic listed on click, without a router navigation', async () => {
    const push = vi.fn();
    const wrapper = mount(HelpPanel, {
      attachTo: document.body,
      global: {
        mocks: {
          $router: { push, replace: vi.fn() },
        },
      },
    });

    await wrapper.get('[data-cy="helpLink"]').trigger('click');
    await nextTick();

    const panel = wrapper.get('[data-cy="helpPanel"]');
    expect(panel.exists()).toBe(true);
    const items = wrapper.findAll('[data-cy="helpTopicItem"]');
    expect(items).toHaveLength(helpTopics.length);
    for (const topic of helpTopics) {
      expect(panel.text()).toContain(topic.title);
      expect(panel.text()).toContain(topic.summary);
    }
    expect(push).not.toHaveBeenCalled();
    wrapper.unmount();
  });

  it('opens a topic to a numbered list of steps and role note, then returns to the list', async () => {
    const wrapper = mountPanel();
    await wrapper.get('[data-cy="helpLink"]').trigger('click');
    await nextTick();

    const interviews = helpTopics.find(t => t.title === 'Logging interviews')!;
    const items = wrapper.findAll('[data-cy="helpTopicItem"]');
    const target = items.find(el => el.text().includes('Logging interviews'))!;
    await target.trigger('click');
    await nextTick();

    const topicView = wrapper.get('[data-cy="helpPanel"]');
    expect(topicView.text()).toContain('Logging interviews');
    const list = wrapper.get('[data-cy="helpTopicSteps"]');
    expect(list.element.tagName).toBe('OL');
    const stepLis = list.findAll('li');
    expect(stepLis).toHaveLength(interviews.steps.length);
    stepLis.forEach((li, i) => expect(li.text()).toBe(interviews.steps[i]));
    expect(topicView.text()).toContain(interviews.roleNote as string);

    const back = wrapper.findAll('[data-cy="helpBack"]');
    expect(back).toHaveLength(1);
    await back[0].trigger('click');
    await nextTick();

    expect(wrapper.find('[data-cy="helpTopicSteps"]').exists()).toBe(false);
    expect(wrapper.findAll('[data-cy="helpTopicItem"]').length).toBe(helpTopics.length);
    wrapper.unmount();
  });

  it('filters case-insensitively on title, summary and step text', async () => {
    const wrapper = mountPanel();
    await wrapper.get('[data-cy="helpLink"]').trigger('click');
    await nextTick();

    const search = wrapper.get('[data-cy="helpSearch"]');
    await search.setValue('STATUS');
    await nextTick();

    const items = wrapper.findAll('[data-cy="helpTopicItem"]');
    expect(items.length).toBeGreaterThan(0);
    expect(items.length).toBeLessThan(helpTopics.length);
    for (const item of items) {
      expect(item.text().toLowerCase()).toMatch(/status|.+/);
    }
    const matching = helpTopics.filter(t => [t.title, t.summary, ...t.steps].join(' ').toLowerCase().includes('status'));
    expect(items).toHaveLength(matching.length);
    expect(wrapper.find('[data-cy="helpEmpty"]').exists()).toBe(false);
    wrapper.unmount();
  });

  it('shows "No matching topics" when the search matches nothing', async () => {
    const wrapper = mountPanel();
    await wrapper.get('[data-cy="helpLink"]').trigger('click');
    await nextTick();

    await wrapper.get('[data-cy="helpSearch"]').setValue('zzznothingmatchesthisxyz');
    await nextTick();

    expect(wrapper.findAll('[data-cy="helpTopicItem"]')).toHaveLength(0);
    const empty = wrapper.get('[data-cy="helpEmpty"]');
    expect(empty.text()).toContain('No matching topics');
    wrapper.unmount();
  });

  it('closes on Escape and returns focus to the Help link', async () => {
    const wrapper = mountPanel();
    const link = wrapper.get('[data-cy="helpLink"]');
    await link.trigger('click');
    await nextTick();

    expect(wrapper.find('[data-cy="helpPanel"]').exists()).toBe(true);

    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
    await nextTick();

    expect(wrapper.find('[data-cy="helpPanel"]').exists()).toBe(false);
    expect(document.activeElement).toBe(link.element);
    wrapper.unmount();
  });
});
