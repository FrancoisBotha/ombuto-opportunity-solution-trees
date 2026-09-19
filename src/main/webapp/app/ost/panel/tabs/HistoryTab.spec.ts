import { afterEach, describe, expect, it } from 'vitest';

import { enableAutoUnmount, flushPromises } from '@vue/test-utils';

import { at } from '../../chat/chat.test-util';
import { dto } from '../../domain/fixtures.test-util';
import type { HistoryEntryDTO } from '../../ost.model';
import { mountWith, setupStores } from '../panel.test-util';

import HistoryTab from './HistoryTab.vue';

enableAutoUnmount(afterEach);

const entry = (id: number, summary: string, createdDate: string, extra: Partial<HistoryEntryDTO> = {}): HistoryEntryDTO => ({
  id,
  eventType: 'STATUS_CHANGED',
  summary,
  authorLogin: 'user',
  authorInitials: 'KP',
  createdDate,
  ...extra,
});

const NEWEST_FIRST = [
  entry(3, 'Status changed to “validated”', at(11, 5)),
  entry(2, 'Link added', at(10, 30), { eventType: 'LINK_ADDED', authorLogin: 'admin', authorInitials: 'AR' }),
  entry(1, 'Node created as opportunity', at(9, 0, 1), { eventType: 'CREATED', authorLogin: null, authorInitials: null }),
];

async function mountTab(list = NEWEST_FIRST, nodeKey = 'opportunity-1') {
  const ctx = await setupStores();
  ctx.ui.select(nodeKey);
  ctx.service.listHistory.resolves(list);
  const wrapper = await mountWith(HistoryTab, ctx.pinia, { nodeKey });
  return { ...ctx, wrapper };
}

describe('HistoryTab', () => {
  it('shows the entries newest first as what · author · timestamp', async () => {
    const { wrapper, service } = await mountTab();
    expect(service.listHistory.calledOnceWith('opportunity', 1)).toBe(true);
    const rows = wrapper.findAll('[data-cy^="ost-history-"][data-event]');
    expect(rows.map(r => r.attributes('data-cy'))).toEqual(['ost-history-3', 'ost-history-2', 'ost-history-1']);
    expect(rows.map(r => r.get('[data-cy="ost-history-what"]').text())).toEqual([
      'Status changed to “validated”',
      'Link added',
      'Node created as opportunity',
    ]);
    expect(rows[0].get('[data-cy="ost-history-meta"]').text()).toBe('KP · Today 11:05');
    expect(rows[1].get('[data-cy="ost-history-meta"]').text()).toBe('AR · Today 10:30');
    expect(rows[2].get('[data-cy="ost-history-meta"]').text()).toBe('System · Yesterday 09:00');
    // the newest dot is the accent one; no line below the last entry
    expect(rows[0].find('.ost-history__dot').classes()).toContain('is-latest');
    expect(rows[2].find('.ost-history__line').exists()).toBe(false);
  });

  it('shows an empty state', async () => {
    const { wrapper } = await mountTab([]);
    expect(wrapper.find('[data-cy="ost-history-empty"]').exists()).toBe(true);
  });

  it('reloads after a write that records history (status change)', async () => {
    const { wrapper, service, tree } = await mountTab();
    service.patchNode.resolves(dto('opportunity-1', 'outcome-1', { status: 'PARKED', priority: 50, valueRating: 3 }));
    service.listHistory.resolves([entry(4, 'Status changed to “parked”', at(12, 0)), ...NEWEST_FIRST]);
    await tree.patchNode('opportunity-1', { status: 'parked' });
    await flushPromises();
    expect(service.listHistory.callCount).toBe(2);
    expect(wrapper.findAll('[data-event]')[0].attributes('data-cy')).toBe('ost-history-4');
  });

  it.each([
    ['a comment', (t: any) => t.addComment('opportunity-1', 'hi'), (s: any) => s.addComment.resolves({ id: 1 })],
    [
      'an open question',
      (t: any) => t.addQuestion('opportunity-1', 'why?'),
      (s: any) => s.addQuestion.resolves({ id: 9, text: 'why?', done: false }),
    ],
    [
      'a link',
      (t: any) => t.addLink('opportunity-1', { name: 'x', url: 'https://x.test' }),
      (s: any) => s.addLink.resolves({ id: 99, name: 'x', url: 'https://x.test' }),
    ],
  ])('reloads after adding %s', async (_label, act, stub) => {
    const { service, tree } = await mountTab();
    stub(service);
    await act(tree);
    await flushPromises();
    expect(service.listHistory.callCount).toBe(2);
  });

  it('reloads for the new node when the tab is reused, even while the previous read is in flight', async () => {
    const ctx = await setupStores();
    let release!: () => void;
    ctx.service.listHistory.withArgs('opportunity', 1).returns(new Promise(r => (release = () => r(NEWEST_FIRST))) as any);
    ctx.service.listHistory.withArgs('solution', 1).resolves([entry(7, 'Node created as solution', at(8, 0), { eventType: 'CREATED' })]);
    const wrapper = await mountWith(HistoryTab, ctx.pinia, { nodeKey: 'opportunity-1' });
    await wrapper.setProps({ nodeKey: 'solution-1' });
    await flushPromises();
    expect(ctx.service.listHistory.calledWith('solution', 1)).toBe(true);
    expect(wrapper.findAll('[data-event]').map(r => r.attributes('data-cy'))).toEqual(['ost-history-7']);
    release();
    await flushPromises();
    expect(wrapper.findAll('[data-event]').map(r => r.attributes('data-cy'))).toEqual(['ost-history-7']);
  });

  it('a history read that raced a write is repeated', async () => {
    const ctx = await setupStores();
    let release!: () => void;
    ctx.service.listHistory.onFirstCall().returns(new Promise(r => (release = () => r(NEWEST_FIRST))) as any);
    ctx.service.listHistory.onSecondCall().resolves([entry(4, 'Link added', at(12, 0)), ...NEWEST_FIRST]);
    ctx.service.addLink.resolves({ id: 99, name: 'x', url: 'https://x.test' });
    const loading = ctx.tree.loadHistory('opportunity-1');
    await ctx.tree.addLink('opportunity-1', { name: 'x', url: 'https://x.test' });
    release();
    const list = await loading;
    expect(ctx.service.listHistory.callCount).toBe(2);
    expect(list[0].id).toBe(4);
    expect(ctx.tree.history['opportunity-1'][0].id).toBe(4);
  });
});
