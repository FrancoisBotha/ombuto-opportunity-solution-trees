import { afterEach, describe, expect, it } from 'vitest';

import { enableAutoUnmount, flushPromises } from '@vue/test-utils';

import DetailPanel from '../DetailPanel.vue';
import { PANEL_TREE, apiError, mountWith, setupStores } from '../panel.test-util';

import OpenQuestionsTab from './OpenQuestionsTab.vue';

enableAutoUnmount(afterEach);

async function mountTab(extra: Parameters<typeof setupStores>[1] = {}) {
  const ctx = await setupStores(PANEL_TREE, extra);
  ctx.ui.select('opportunity-1');
  const wrapper = await mountWith(OpenQuestionsTab, ctx.pinia, { nodeKey: 'opportunity-1' });
  return { ...ctx, wrapper };
}

describe('OpenQuestionsTab', () => {
  it('lists the questions as a checklist with the FR-Q2 copy and the summary', async () => {
    const { wrapper } = await mountTab();
    expect(wrapper.get('[data-cy="ost-questions-hint"]').text()).toBe(
      'What do we still need to learn about this opportunity? Delivery work belongs in Jira, under Links.',
    );
    expect(wrapper.findAll('[data-cy^="ost-question-toggle-"]').map(b => b.attributes('aria-checked'))).toEqual(['false', 'true', 'false']);
    expect(wrapper.get('[data-cy="ost-question-2"]').find('.ost-questions__text').classes()).toContain('is-done');
    expect(wrapper.get('[data-cy="ost-questions-summary"]').text()).toBe('2 open · 1 answered');
    expect(wrapper.get('[data-cy="ost-question-add"]').attributes('placeholder')).toBe('Add an open question, then Enter');
    expect(wrapper.get('[data-cy="ost-question-add"]').attributes('maxlength')).toBe('500');
  });

  it('Enter adds a trimmed question and clears the input; blank input adds nothing', async () => {
    const { wrapper, service, tree } = await mountTab();
    service.addQuestion.resolves({ id: 4, text: 'How often?', done: false });
    const add = wrapper.get('[data-cy="ost-question-add"]');
    await add.setValue('   ');
    await add.trigger('keydown', { key: 'Enter' });
    await flushPromises();
    expect(service.addQuestion.called).toBe(false);
    await add.setValue('  How often? ');
    await add.trigger('keydown', { key: 'Enter' });
    await flushPromises();
    expect(service.addQuestion.calledOnceWith(1, 'How often?')).toBe(true);
    expect((add.element as HTMLInputElement).value).toBe('');
    expect(wrapper.find('[data-cy="ost-question-4"]').exists()).toBe(true);
    expect(wrapper.get('[data-cy="ost-questions-summary"]').text()).toBe('3 open · 1 answered');
    expect(tree.history['opportunity-1']).toBeUndefined();
  });

  it('ticks and unticks', async () => {
    const { wrapper, service } = await mountTab();
    service.updateQuestion.onFirstCall().resolves({ id: 1, text: 'Who?', done: true });
    service.updateQuestion.onSecondCall().resolves({ id: 2, text: 'When?', done: false });
    await wrapper.get('[data-cy="ost-question-toggle-1"]').trigger('click');
    await flushPromises();
    expect(service.updateQuestion.firstCall.args).toEqual([1, { done: true }]);
    await wrapper.get('[data-cy="ost-question-toggle-2"]').trigger('click');
    await flushPromises();
    expect(service.updateQuestion.secondCall.args).toEqual([2, { done: false }]);
    expect(wrapper.get('[data-cy="ost-question-toggle-1"]').attributes('aria-checked')).toBe('true');
    expect(wrapper.get('[data-cy="ost-questions-summary"]').text()).toBe('2 open · 1 answered');
  });

  it('removes a question', async () => {
    const { wrapper, service } = await mountTab();
    service.deleteQuestion.resolves();
    await wrapper.get('[data-cy="ost-question-remove-3"]').trigger('click');
    await flushPromises();
    expect(service.deleteQuestion.calledOnceWith(3)).toBe(true);
    expect(wrapper.find('[data-cy="ost-question-3"]').exists()).toBe(false);
    expect(wrapper.get('[data-cy="ost-questions-summary"]').text()).toBe('1 open · 1 answered');
  });

  it('a refused tick rolls back', async () => {
    const { wrapper, service } = await mountTab();
    service.updateQuestion.rejects(apiError(403));
    await wrapper.get('[data-cy="ost-question-toggle-1"]').trigger('click');
    await flushPromises();
    expect(wrapper.get('[data-cy="ost-question-toggle-1"]').attributes('aria-checked')).toBe('false');
  });

  it('the Open Qs tab badge counts the open questions', async () => {
    const ctx = await setupStores(PANEL_TREE);
    ctx.ui.select('opportunity-1');
    ctx.ui.setPanelTab('questions');
    const wrapper = await mountWith(DetailPanel, ctx.pinia);
    expect(wrapper.get('[data-cy="ost-tab-badge-questions"]').text()).toBe('2');
    ctx.service.updateQuestion.resolves({ id: 1, text: 'Who?', done: true });
    await wrapper.get('[data-cy="ost-question-toggle-1"]').trigger('click');
    await flushPromises();
    expect(wrapper.get('[data-cy="ost-tab-badge-questions"]').text()).toBe('1');
    ctx.service.updateQuestion.resolves({ id: 3, text: 'Why?', done: true });
    await wrapper.get('[data-cy="ost-question-toggle-3"]').trigger('click');
    await flushPromises();
    expect(wrapper.find('[data-cy="ost-tab-badge-questions"]').exists()).toBe(false);
  });

  it('shows a refused add inline in the panel and keeps the text', async () => {
    const ctx = await setupStores(PANEL_TREE);
    ctx.ui.select('opportunity-1');
    ctx.ui.setPanelTab('questions');
    const wrapper = await mountWith(DetailPanel, ctx.pinia);
    ctx.service.addQuestion.rejects(apiError(400, 'error.questiontextinvalid'));
    const add = wrapper.get('[data-cy="ost-question-add"]');
    await add.setValue('Too long, say');
    await add.trigger('keydown', { key: 'Enter' });
    await flushPromises();
    expect(wrapper.get('[data-cy="ost-panel-error"]').text()).toBe('Questions need 1 to 500 characters.');
    expect((add.element as HTMLInputElement).value).toBe('Too long, say');
  });

  it('viewer: read-only list, no add, no remove, ticks disabled', async () => {
    const { wrapper, service } = await mountTab({ canEdit: false, currentUserRole: 'VIEWER' });
    expect(wrapper.find('[data-cy="ost-question-add"]').exists()).toBe(false);
    expect(wrapper.find('[data-cy^="ost-question-remove-"]').exists()).toBe(false);
    const toggle = wrapper.get('[data-cy="ost-question-toggle-1"]');
    expect(toggle.attributes('disabled')).toBeDefined();
    await toggle.trigger('click');
    await flushPromises();
    expect(service.updateQuestion.called).toBe(false);
    expect(wrapper.get('[data-cy="ost-questions-summary"]').text()).toBe('2 open · 1 answered');
  });
});
