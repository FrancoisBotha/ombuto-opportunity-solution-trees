import { readFileSync } from 'node:fs';

import { afterEach, describe, expect, it } from 'vitest';

import { enableAutoUnmount } from '@vue/test-utils';

import ChatThread from './chat/ChatThread.vue';
import { at, comment } from './chat/chat.test-util';
import { mountWith, setupStores } from './panel/panel.test-util';
import OpenQuestionsTab from './panel/tabs/OpenQuestionsTab.vue';

enableAutoUnmount(afterEach);

const baseCss = readFileSync('src/main/webapp/app/ost/styles/ost-base.css', 'utf8');

/** NFR-5: on a touch screen the small OST controls are 44 x 44px (ost-base.css, pointer: coarse). */
describe('touch targets', () => {
  it('ost-base.css sizes .ost-tap and .ost-btn to 44px on a coarse pointer only', () => {
    const rule = /@media \(pointer: coarse\) \{([\s\S]*?)\n\}/.exec(baseCss)?.[1] ?? '';
    expect(rule).toContain('.ost-root .ost-tap');
    expect(rule).toContain('.ost-root .ost-btn');
    expect(rule).toContain('min-width: 44px');
    expect(rule).toContain('min-height: 44px');
  });

  it('chat edit / delete are touch targets', async () => {
    const ctx = await setupStores();
    ctx.service.listComments.resolves([comment(1, 'user', at(9, 0))]);
    const wrapper = await mountWith(ChatThread, ctx.pinia, { nodeKey: 'opportunity-1' });
    expect(wrapper.get('[data-cy="ost-chat-edit-1"]').classes()).toContain('ost-tap');
    expect(wrapper.get('[data-cy="ost-chat-delete-1"]').classes()).toContain('ost-tap');
  });

  it('open-question checkbox and remove are touch targets', async () => {
    const ctx = await setupStores();
    ctx.tree.byId('opportunity-1')!.questions = [{ id: 4, text: 'Who books?', done: false }];
    const wrapper = await mountWith(OpenQuestionsTab, ctx.pinia, { nodeKey: 'opportunity-1' });
    expect(wrapper.get('[data-cy="ost-question-toggle-4"]').classes()).toContain('ost-tap');
    expect(wrapper.get('[data-cy="ost-question-remove-4"]').classes()).toContain('ost-tap');
  });
});
