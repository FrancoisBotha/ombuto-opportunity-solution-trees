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

  it('compact controls get an invisible 44px+ hit area on a coarse pointer (.ost-hit), zoom-aware on the canvas', () => {
    const rule = /@media \(pointer: coarse\) \{([\s\S]*?)\n\}/.exec(baseCss)?.[1] ?? '';
    expect(rule).toContain('.ost-root .ost-hit::before');
    expect(rule).toContain('max(100%, 48px)');
    expect(rule).toContain('.ost-root .ost-canvas .ost-hit::before');
    expect(rule).toContain('var(--ost-zoom, 1)');
    for (const [file, cls] of [
      ['canvas/OstNode.vue', 'ost-node__add ost-hit'],
      ['canvas/OstNode.vue', 'ost-node__toggle ost-hit'],
      ['canvas/OstNode.vue', 'ost-node__chat ost-hit'],
      ['canvas/CanvasToolbar.vue', 'ost-toolbar__chip ost-hit'],
      ['panel/fields/StatusChips.vue', 'ost-chip ost-hit'],
      ['panel/PanelTabs.vue', 'ost-tabs__tab ost-hit'],
      ['chat/ChatThread.vue', 'ost-chat__send ost-hit'],
    ]) {
      expect(readFileSync(`src/main/webapp/app/ost/${file}`, 'utf8'), `${file}: ${cls}`).toContain(`class="${cls}`);
    }
  });
});
