import { afterEach, describe, expect, it } from 'vitest';

import { enableAutoUnmount, flushPromises } from '@vue/test-utils';

import { mountWith, setupStores } from '../panel/panel.test-util';

import ChatModal from './ChatModal.vue';
import { at, comment } from './chat.test-util';

enableAutoUnmount(afterEach);

async function mountModal(key: string) {
  const ctx = await setupStores();
  ctx.service.listComments.resolves([comment(1, 'admin', at(9, 0)), comment(2, 'user', at(9, 1))]);
  ctx.ui.openChat(key);
  const wrapper = await mountWith(ChatModal, ctx.pinia);
  return { ...ctx, wrapper };
}

describe('ChatModal', () => {
  it('shows the node, "<Type> · n messages", the participants and the thread', async () => {
    const { wrapper } = await mountModal('opportunity-1');
    const modal = wrapper.get('[data-cy="ost-chat-modal"]');
    expect(modal.attributes('role')).toBe('dialog');
    expect(modal.get('h2').text()).toBe('Hard to find people');
    expect(wrapper.get('[data-cy="ost-chat-modal-sub"]').text()).toBe('Opportunity · 2 messages');
    expect(wrapper.findAll('.ost-chat-modal__avatar').map(a => a.text())).toEqual(['AR', 'KP']);
    expect(wrapper.findAll('[data-cy^="ost-chat-msg-"]')).toHaveLength(2);
    // the composer takes focus
    expect(document.activeElement?.getAttribute('data-cy')).toBe('ost-chat-input');
  });

  it('Escape and the close button close it', async () => {
    const { wrapper, ui } = await mountModal('solution-1');
    await wrapper.get('[data-cy="ost-chat-input"]').trigger('keydown', { key: 'Escape' });
    expect(ui.chatId).toBeNull();
    ui.openChat('solution-1');
    await flushPromises();
    await wrapper.get('[data-cy="ost-chat-modal-close"]').trigger('click');
    expect(ui.chatId).toBeNull();
  });

  it('products have no thread: the modal does not open', async () => {
    const { wrapper, ui, service } = await mountModal('product-1');
    expect(wrapper.find('[data-cy="ost-chat-modal"]').exists()).toBe(false);
    expect(ui.chatId).toBeNull();
    expect(service.listComments.called).toBe(false);
  });
});
