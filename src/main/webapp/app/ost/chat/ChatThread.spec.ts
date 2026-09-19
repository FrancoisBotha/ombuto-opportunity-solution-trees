import { afterEach, describe, expect, it } from 'vitest';

import { type VueWrapper, enableAutoUnmount, flushPromises } from '@vue/test-utils';

import { PANEL_TREE, apiError, mountWith, setupStores } from '../panel/panel.test-util';

import ChatThread from './ChatThread.vue';
import { at, comment } from './chat.test-util';

enableAutoUnmount(afterEach);

const THREAD = [
  comment(1, 'admin', at(10, 0)),
  comment(2, 'admin', at(10, 2)),
  comment(3, 'user', at(10, 3), { body: 'Mine, first' }),
  comment(4, 'user', at(10, 4), { body: 'Mine, second' }),
  comment(5, 'admin', at(10, 40), { body: 'Later reply' }),
];

async function mountThread(list = THREAD, extra: Parameters<typeof setupStores>[1] = {}, nodeKey = 'opportunity-1') {
  const ctx = await setupStores(PANEL_TREE, extra);
  ctx.service.listComments.resolves(list.map(c => ({ ...c })));
  const wrapper = await mountWith(ChatThread, ctx.pinia, { nodeKey });
  return { ...ctx, wrapper };
}

const input = (w: VueWrapper) => w.get('[data-cy="ost-chat-input"]');
const bubbles = (w: VueWrapper) => w.findAll('[data-cy^="ost-chat-msg-"]');

/** Gives the scroll box a layout (happy-dom has none). */
function layout(el: Element, box: { scrollHeight: number; clientHeight: number }) {
  Object.defineProperty(el, 'scrollHeight', { configurable: true, get: () => box.scrollHeight });
  Object.defineProperty(el, 'clientHeight', { configurable: true, get: () => box.clientHeight });
}

describe('ChatThread', () => {
  it('loads the thread on open and syncs the node comment count', async () => {
    const { wrapper, service, tree } = await mountThread();
    expect(service.listComments.calledOnceWith('opportunity', 1)).toBe(true);
    expect(bubbles(wrapper)).toHaveLength(5);
    expect(tree.byId('opportunity-1')?.commentCount).toBe(5); // fixture said 2
  });

  it('own messages are right / accent, others left / neutral', async () => {
    const { wrapper } = await mountThread();
    const mine = wrapper.get('[data-cy="ost-chat-msg-3"]');
    const theirs = wrapper.get('[data-cy="ost-chat-msg-1"]');
    expect(mine.attributes('data-mine')).toBe('true');
    expect(mine.element.parentElement?.classList.contains('is-mine')).toBe(true);
    expect(theirs.attributes('data-mine')).toBe('false');
    expect(theirs.element.parentElement?.classList.contains('is-mine')).toBe(false);
  });

  it('groups runs: a stamp above each run, initials on the first bubble of someone else’s run', async () => {
    const { wrapper } = await mountThread();
    const items = wrapper.findAll('.ost-chat__item');
    expect(items.map(i => i.classes().includes('is-run-start'))).toEqual([true, false, true, false, true]);
    expect(items.map(i => i.find('[data-cy="ost-chat-stamp"]').exists())).toEqual([true, false, true, false, true]);
    expect(wrapper.findAll('[data-cy="ost-chat-who"]').map(w => w.text())).toEqual(['AR', 'AR']);
    expect(items[0].get('[data-cy="ost-chat-stamp"]').text()).toBe('Today 10:00');
  });

  it('offers edit and delete on own messages only', async () => {
    const { wrapper } = await mountThread();
    expect(wrapper.find('[data-cy="ost-chat-edit-3"]').exists()).toBe(true);
    expect(wrapper.find('[data-cy="ost-chat-delete-4"]').exists()).toBe(true);
    for (const id of [1, 2, 5]) {
      expect(wrapper.find(`[data-cy="ost-chat-edit-${id}"]`).exists()).toBe(false);
      expect(wrapper.find(`[data-cy="ost-chat-delete-${id}"]`).exists()).toBe(false);
    }
  });

  describe('composer', () => {
    it('Enter sends, trims, clears the draft and bumps the count; Shift+Enter does not send', async () => {
      const { wrapper, service, tree } = await mountThread();
      service.addComment.resolves(comment(6, 'user', new Date().toISOString(), { body: 'Hello trio' }));
      await input(wrapper).setValue('  Hello trio ');
      await input(wrapper).trigger('keydown', { key: 'Enter', shiftKey: true });
      expect(service.addComment.called).toBe(false);
      await input(wrapper).trigger('keydown', { key: 'Enter' });
      await flushPromises();
      expect(service.addComment.calledOnceWith('opportunity', 1, 'Hello trio')).toBe(true);
      expect((input(wrapper).element as HTMLTextAreaElement).value).toBe('');
      expect(bubbles(wrapper)).toHaveLength(6);
      expect(tree.byId('opportunity-1')?.commentCount).toBe(6);
    });

    it('the send button is disabled for an empty draft and blank text is never sent', async () => {
      const { wrapper, service } = await mountThread();
      expect(wrapper.get('[data-cy="ost-chat-send"]').attributes('disabled')).toBeDefined();
      await input(wrapper).setValue('   ');
      await input(wrapper).trigger('keydown', { key: 'Enter' });
      await flushPromises();
      expect(service.addComment.called).toBe(false);
      await input(wrapper).setValue('Something');
      expect(wrapper.get('[data-cy="ost-chat-send"]').attributes('disabled')).toBeUndefined();
    });

    it('keeps the draft and shows the error when sending fails', async () => {
      const { wrapper, service, tree } = await mountThread();
      service.addComment.rejects(apiError(400, 'error.commentbodyinvalid'));
      await input(wrapper).setValue('Will fail');
      await wrapper.get('[data-cy="ost-chat-send"]').trigger('click');
      await flushPromises();
      expect(wrapper.get('[data-cy="ost-chat-error"]').text()).toContain('Messages need 1 to 10,000 characters.');
      expect((input(wrapper).element as HTMLTextAreaElement).value).toBe('Will fail');
      expect(tree.error).toBeNull();
      expect(tree.byId('opportunity-1')?.commentCount).toBe(5);
    });
  });

  describe('edit and delete own', () => {
    it('Edit loads the message into the composer; Enter saves and the bubble is marked edited', async () => {
      const { wrapper, service } = await mountThread();
      service.updateComment.resolves({ ...THREAD[2], body: 'Mine, fixed', editedDate: new Date().toISOString() });
      await wrapper.get('[data-cy="ost-chat-edit-3"]').trigger('click');
      await flushPromises();
      expect(wrapper.find('[data-cy="ost-chat-editing"]').text()).toContain('Editing message');
      expect((input(wrapper).element as HTMLTextAreaElement).value).toBe('Mine, first');
      expect(document.activeElement).toBe(input(wrapper).element);
      await input(wrapper).setValue('Mine, fixed');
      await input(wrapper).trigger('keydown', { key: 'Enter' });
      await flushPromises();
      expect(service.updateComment.calledOnceWith(3, 'Mine, fixed')).toBe(true);
      expect(service.addComment.called).toBe(false);
      const bubble = wrapper.get('[data-cy="ost-chat-msg-3"]');
      expect(bubble.text()).toContain('Mine, fixed');
      expect(bubble.find('[data-cy="ost-chat-edited"]').text()).toBe('(edited)');
      expect(wrapper.find('[data-cy="ost-chat-editing"]').exists()).toBe(false);
    });

    it('Escape cancels an edit without saving and does not reach an enclosing dialog', async () => {
      const { wrapper, service } = await mountThread();
      let reachedParent = false;
      wrapper.element.parentElement?.addEventListener('keydown', () => (reachedParent = true));
      await wrapper.get('[data-cy="ost-chat-edit-4"]').trigger('click');
      await input(wrapper).setValue('Changed my mind');
      await input(wrapper).trigger('keydown', { key: 'Escape' });
      await flushPromises();
      expect(reachedParent).toBe(false);
      expect(service.updateComment.called).toBe(false);
      expect((input(wrapper).element as HTMLTextAreaElement).value).toBe('');
      expect(wrapper.find('[data-cy="ost-chat-editing"]').exists()).toBe(false);
    });

    it('saving an unchanged edit sends nothing', async () => {
      const { wrapper, service } = await mountThread();
      await wrapper.get('[data-cy="ost-chat-edit-3"]').trigger('click');
      await input(wrapper).trigger('keydown', { key: 'Enter' });
      await flushPromises();
      expect(service.updateComment.called).toBe(false);
      expect(wrapper.find('[data-cy="ost-chat-editing"]').exists()).toBe(false);
    });

    it('Delete removes an own message and lowers the count', async () => {
      const { wrapper, service, tree } = await mountThread();
      service.deleteComment.resolves();
      await wrapper.get('[data-cy="ost-chat-delete-4"]').trigger('click');
      await flushPromises();
      expect(service.deleteComment.calledOnceWith(4)).toBe(true);
      expect(wrapper.find('[data-cy="ost-chat-msg-4"]').exists()).toBe(false);
      expect(tree.byId('opportunity-1')?.commentCount).toBe(4);
    });

    it('a refused delete puts the message back and explains', async () => {
      const { wrapper, service, tree } = await mountThread();
      service.deleteComment.rejects(apiError(403));
      await wrapper.get('[data-cy="ost-chat-delete-3"]').trigger('click');
      await flushPromises();
      expect(wrapper.find('[data-cy="ost-chat-msg-3"]').exists()).toBe(true);
      expect(tree.byId('opportunity-1')?.commentCount).toBe(5);
      expect(wrapper.get('[data-cy="ost-chat-error"]').text()).toBe('You do not have permission to change this tree.');
    });
  });

  it('viewer: reads the thread, composer disabled with a note, no edit / delete even on own messages', async () => {
    const { wrapper, service } = await mountThread(THREAD, { canEdit: false, currentUserRole: 'VIEWER' });
    expect(bubbles(wrapper)).toHaveLength(5);
    expect(input(wrapper).attributes('disabled')).toBeDefined();
    expect(wrapper.get('[data-cy="ost-chat-send"]').attributes('disabled')).toBeDefined();
    expect(wrapper.get('[data-cy="ost-chat-readonly"]').text()).toContain('only owners and editors can post');
    expect(wrapper.find('[data-cy="ost-chat-edit-3"]').exists()).toBe(false);
    expect(wrapper.find('[data-cy="ost-chat-delete-3"]').exists()).toBe(false);
    await input(wrapper).trigger('keydown', { key: 'Enter' });
    await flushPromises();
    expect(service.addComment.called).toBe(false);
  });

  it('shows an empty state for a new thread', async () => {
    const { wrapper } = await mountThread([]);
    expect(wrapper.find('[data-cy="ost-chat-empty"]').exists()).toBe(true);
    expect(wrapper.find('[data-cy="ost-chat-jump"]').exists()).toBe(false);
  });

  describe('scrolling', () => {
    it('the jump button appears when scrolled away from the latest message and jumps back', async () => {
      const { wrapper } = await mountThread();
      const scroller = wrapper.get('[data-cy="ost-chat-scroll"]');
      const el = scroller.element as HTMLElement;
      layout(el, { scrollHeight: 1000, clientHeight: 200 });
      el.scrollTop = 800;
      await scroller.trigger('scroll');
      expect(wrapper.find('[data-cy="ost-chat-jump"]').exists()).toBe(false);
      el.scrollTop = 300;
      await scroller.trigger('scroll');
      expect(wrapper.find('[data-cy="ost-chat-jump"]').exists()).toBe(true);
      el.scrollTo = undefined as any; // no smooth scrolling here: fall back to scrollTop
      await wrapper.get('[data-cy="ost-chat-jump"]').trigger('click');
      expect(el.scrollTop).toBe(1000);
      expect(wrapper.find('[data-cy="ost-chat-jump"]').exists()).toBe(false);
    });

    it('a sent message scrolls to the latest even when scrolled up', async () => {
      const { wrapper, service } = await mountThread();
      const scroller = wrapper.get('[data-cy="ost-chat-scroll"]');
      const el = scroller.element as HTMLElement;
      layout(el, { scrollHeight: 1000, clientHeight: 200 });
      el.scrollTop = 100;
      await scroller.trigger('scroll');
      expect(wrapper.find('[data-cy="ost-chat-jump"]').exists()).toBe(true);
      layout(el, { scrollHeight: 1100, clientHeight: 200 });
      service.addComment.resolves(comment(6, 'user', new Date().toISOString()));
      await input(wrapper).setValue('New one');
      await input(wrapper).trigger('keydown', { key: 'Enter' });
      await flushPromises();
      expect(el.scrollTop).toBe(1100);
      expect(wrapper.find('[data-cy="ost-chat-jump"]').exists()).toBe(false);
    });

    it('switching nodes reloads the thread and opens at the latest message', async () => {
      const ctx = await setupStores(PANEL_TREE);
      ctx.service.listComments.resolves(THREAD);
      const wrapper = await mountWith(ChatThread, ctx.pinia, { nodeKey: 'opportunity-1' });
      const el = wrapper.get('[data-cy="ost-chat-scroll"]').element as HTMLElement;
      layout(el, { scrollHeight: 900, clientHeight: 200 });
      // a node switch reloads and scrolls again
      ctx.service.listComments.resolves([comment(9, 'admin', at(9, 0))]);
      await wrapper.setProps({ nodeKey: 'solution-1' });
      await flushPromises();
      expect(el.scrollTop).toBe(900);
      expect(ctx.service.listComments.lastCall.args).toEqual(['solution', 1]);
      expect(bubbles(wrapper)).toHaveLength(1);
    });
  });
});
