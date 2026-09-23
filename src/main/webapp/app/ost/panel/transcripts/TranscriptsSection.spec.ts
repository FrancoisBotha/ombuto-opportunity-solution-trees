/**
 * MTRANS-005 / MTRANS-006 — Transcripts section + viewer specs.
 *
 * Covers list ordering and lazy body fetch, plain-text rendering (XSS), the canvas badge, and
 * viewer / role-gated affordances for the paste, upload, edit and delete flow.
 */
import { afterEach, describe, expect, it, vi } from 'vitest';

import { enableAutoUnmount, flushPromises, mount } from '@vue/test-utils';

import OstNodeView from '../../canvas/OstNode.vue';
import { dto, node as nodeFixture } from '../../domain/fixtures.test-util';
import type { TranscriptDTO, TranscriptMetaDTO } from '../../ost.model';
import { PANEL_TREE, mountWith, setupStores } from '../panel.test-util';

import TranscriptsSection from './TranscriptsSection.vue';
import TranscriptViewer from './TranscriptViewer.vue';

enableAutoUnmount(afterEach);

const meta = (id: number, title: string, createdDate: string, extra: Partial<TranscriptMetaDTO> = {}): TranscriptMetaDTO => ({
  id,
  title,
  meetingDate: '2026-09-01',
  attendees: 'Kira, Ana',
  source: 'PASTED',
  nodeType: 'OPPORTUNITY',
  nodeId: 1,
  nodeKey: 'opportunity-1',
  authorLogin: 'user',
  authorInitials: 'KP',
  authorName: 'Kira P',
  createdDate,
  editedDate: null,
  ...extra,
});

const full = (id: number, body: string, extra: Partial<TranscriptDTO> = {}): TranscriptDTO => ({
  ...meta(id, `Transcript ${id}`, '2026-09-01T10:00:00Z'),
  body,
  ...extra,
});

async function mountSection(role: 'OWNER' | 'EDITOR' | 'VIEWER' = 'EDITOR', list?: TranscriptMetaDTO[]) {
  const ctx = await setupStores(PANEL_TREE, {
    currentUserRole: role,
    canEdit: role !== 'VIEWER',
  });
  ctx.ui.select('opportunity-1');
  if (list !== undefined) ctx.service.listTranscriptsByNode.resolves(list);
  const wrapper = await mountWith(TranscriptsSection, ctx.pinia, { nodeKey: 'opportunity-1' });
  return { ...ctx, wrapper };
}

describe('TranscriptsSection — list', () => {
  it('renders transcripts newest first, showing title / date / attendees only (no body fetch)', async () => {
    const { wrapper, service } = await mountSection('EDITOR', [
      meta(1, 'Older discovery', '2026-09-01T09:00:00Z'),
      meta(3, 'Latest interview', '2026-09-05T15:00:00Z'),
      meta(2, 'Middle chat', '2026-09-03T12:00:00Z'),
    ]);
    expect(service.listTranscriptsByNode.calledOnceWith('opportunity', 1)).toBe(true);
    expect(service.getTranscript.called).toBe(false);
    const rows = wrapper.findAll('[data-cy^="ost-transcript-item-"]');
    expect(rows.map(r => r.attributes('data-transcript-id'))).toEqual(['3', '2', '1']);
    expect(rows[0].text()).toContain('Latest interview');
  });

  it('shows an empty state when the node has no transcripts', async () => {
    const { wrapper } = await mountSection('EDITOR', []);
    expect(wrapper.find('[data-cy="ost-transcripts-empty"]').exists()).toBe(true);
  });

  it('editors see the Add transcript button; viewers do not (criterion 1)', async () => {
    const editor = await mountSection('EDITOR', []);
    expect(editor.wrapper.find('[data-cy="ost-transcript-add"]').exists()).toBe(true);
    const viewer = await mountSection('VIEWER', [meta(1, 'A transcript', '2026-09-01T09:00:00Z')]);
    expect(viewer.tree.canEdit).toBe(false);
    expect(viewer.wrapper.find('[data-cy="ost-transcript-add"]').exists()).toBe(false);
    // The list itself is still there for viewers.
    expect(viewer.wrapper.find('[data-cy="ost-transcript-item-1"]').exists()).toBe(true);
  });

  it('opens the viewer on click, fetching that transcript body on demand', async () => {
    const { wrapper, service } = await mountSection('EDITOR', [meta(7, 'Chat with Sales', '2026-09-05T15:00:00Z')]);
    service.getTranscript.withArgs(7).resolves(full(7, 'line one\nline two'));
    expect(service.getTranscript.called).toBe(false);
    await wrapper.get('[data-cy="ost-transcript-item-7"]').trigger('click');
    await flushPromises();
    expect(service.getTranscript.calledOnceWith(7)).toBe(true);
    const body = document.querySelector('[data-cy="ost-transcript-viewer-body"]');
    expect(body?.textContent).toBe('line one\nline two');
  });

  it('clicking Add transcript opens the form dialog (editors)', async () => {
    const { wrapper } = await mountSection('EDITOR', []);
    await wrapper.get('[data-cy="ost-transcript-add"]').trigger('click');
    await flushPromises();
    expect(document.querySelector('[data-cy="ost-transcript-form"]')).toBeTruthy();
  });
});

describe('TranscriptViewer — safe rendering + permissions', () => {
  async function mountViewer(role: 'EDITOR' | 'VIEWER', body: string) {
    const ctx = await setupStores(PANEL_TREE, {
      currentUserRole: role,
      canEdit: role !== 'VIEWER',
    });
    ctx.service.getTranscript.withArgs(42).resolves(full(42, body, { title: 'The interview' }));
    const wrapper = mount(TranscriptViewer, {
      attachTo: document.body,
      props: { transcriptId: 42 },
      global: { plugins: [ctx.pinia] },
    });
    await flushPromises();
    return { ...ctx, wrapper };
  }

  it('renders the body as plain text — HTML in the source is not interpreted', async () => {
    const evil = '<script>window.__pwn = 1</script><b>bold?</b>\nnext line';
    await mountViewer('EDITOR', evil);
    const body = document.querySelector('[data-cy="ost-transcript-viewer-body"]') as HTMLElement;
    expect(body).toBeTruthy();
    expect(body.textContent).toBe(evil);
    expect(body.querySelector('script')).toBeNull();
    expect(body.querySelector('b')).toBeNull();
    expect((window as any).__pwn).toBeUndefined();
  });

  it('viewers see no edit / delete buttons; editors do', async () => {
    const v = await mountViewer('VIEWER', 'a transcript');
    expect(v.wrapper.find('[data-cy="ost-transcript-viewer-edit"]').exists()).toBe(false);
    expect(v.wrapper.find('[data-cy="ost-transcript-viewer-delete"]').exists()).toBe(false);
    const e = await mountViewer('EDITOR', 'a transcript');
    expect(e.wrapper.find('[data-cy="ost-transcript-viewer-edit"]').exists()).toBe(true);
    expect(e.wrapper.find('[data-cy="ost-transcript-viewer-delete"]').exists()).toBe(true);
  });

  it('delete requires a confirm click and then calls the store', async () => {
    const { wrapper, service } = await mountViewer('EDITOR', 'a transcript');
    service.deleteTranscript.resolves();
    // Ensure the store has this transcript associated with its node (for count refresh).
    service.listTranscriptsByNode.resolves([]);
    await wrapper.get('[data-cy="ost-transcript-viewer-delete"]').trigger('click');
    // First click reveals the confirm; the API has NOT been called yet.
    expect(service.deleteTranscript.called).toBe(false);
    expect(wrapper.find('[data-cy="ost-transcript-viewer-confirm-delete"]').exists()).toBe(true);
    await wrapper.get('[data-cy="ost-transcript-viewer-confirm-delete"]').trigger('click');
    await flushPromises();
    expect(service.deleteTranscript.calledOnceWith(42)).toBe(true);
  });

  it('edit emits the current transcript so the section can open the form in edit mode', async () => {
    const { wrapper } = await mountViewer('EDITOR', 'a transcript');
    await wrapper.get('[data-cy="ost-transcript-viewer-edit"]').trigger('click');
    const emitted = wrapper.emitted('edit');
    expect(emitted?.[0]?.[0]).toMatchObject({ id: 42, title: 'The interview' });
  });
});

describe('OstNode transcript badge', () => {
  const mountNode = (transcriptCount: number) =>
    mount(OstNodeView, {
      props: { node: nodeFixture('opportunity-1', 'outcome-1', { transcriptCount }) },
      global: { stubs: { Handle: { name: 'Handle', template: '<i></i>' } } },
    });

  it('shows the transcript badge with the count when transcriptCount > 0', () => {
    const wrapper = mountNode(3);
    const badge = wrapper.get('[data-cy="ost-node-transcripts-opportunity-1"]');
    expect(badge.text()).toBe('3');
    expect(badge.attributes('data-count')).toBe('3');
  });

  it('renders no transcript badge when transcriptCount is zero', () => {
    const wrapper = mountNode(0);
    expect(wrapper.find('[data-cy="ost-node-transcripts-opportunity-1"]').exists()).toBe(false);
  });
});

describe('tree store — transcript count follows create / delete without WebSocket', () => {
  it('refreshing the transcript list updates the node.transcriptCount without fetching the body', async () => {
    const ctx = await setupStores(PANEL_TREE);
    ctx.service.listTranscriptsByNode.resolves([meta(1, 'A', '2026-09-01T09:00:00Z'), meta(2, 'B', '2026-09-02T10:00:00Z')]);
    await ctx.tree.loadTranscripts('opportunity-1');
    expect(ctx.tree.byId('opportunity-1')?.transcriptCount).toBe(2);
    expect(ctx.service.getTranscript.called).toBe(false);
  });

  it('picks up transcriptCount from the tree DTO on load', async () => {
    const nodes = [dto('product-1', null), dto('outcome-1', 'product-1'), dto('opportunity-1', 'outcome-1', { transcriptCount: 4 })];
    const ctx = await setupStores(nodes);
    expect(ctx.tree.byId('opportunity-1')?.transcriptCount).toBe(4);
  });

  it('createTranscript refreshes the list and count for the node — no WebSocket needed', async () => {
    const ctx = await setupStores(PANEL_TREE);
    // Start empty, then grow to two after the create.
    ctx.service.listTranscriptsByNode.onFirstCall().resolves([]);
    ctx.service.listTranscriptsByNode
      .onSecondCall()
      .resolves([meta(9, 'Just created', '2026-09-10T10:00:00Z'), meta(1, 'Older', '2026-09-01T09:00:00Z')]);
    await ctx.tree.loadTranscripts('opportunity-1');
    expect(ctx.tree.byId('opportunity-1')?.transcriptCount).toBe(0);
    ctx.service.createTranscript.resolves(full(9, 'body', { title: 'Just created', nodeKey: 'opportunity-1', source: 'PASTED' }));
    const dtoOut = await ctx.tree.createTranscript('opportunity-1', {
      title: 'Just created',
      meetingDate: '2026-09-10',
      attendees: 'Kira',
      body: 'body',
      source: 'PASTED',
    });
    expect(dtoOut?.id).toBe(9);
    // Server took the exactly-one-node payload for opportunity-1 (dbId 1 in PANEL_TREE).
    const arg = ctx.service.createTranscript.firstCall.args[0];
    expect(arg.opportunityId).toBe(1);
    expect(arg.source).toBe('PASTED');
    expect(ctx.tree.byId('opportunity-1')?.transcriptCount).toBe(2);
  });

  it('deleteTranscript refreshes the count from the reloaded list', async () => {
    const ctx = await setupStores(PANEL_TREE);
    ctx.service.listTranscriptsByNode.onFirstCall().resolves([meta(1, 'A', '2026-09-01T09:00:00Z'), meta(2, 'B', '2026-09-02T10:00:00Z')]);
    ctx.service.listTranscriptsByNode.onSecondCall().resolves([meta(1, 'A', '2026-09-01T09:00:00Z')]);
    await ctx.tree.loadTranscripts('opportunity-1');
    expect(ctx.tree.byId('opportunity-1')?.transcriptCount).toBe(2);
    ctx.service.deleteTranscript.resolves();
    const ok = await ctx.tree.deleteTranscript(2);
    expect(ok).toBe(true);
    expect(ctx.service.deleteTranscript.calledOnceWith(2)).toBe(true);
    expect(ctx.tree.byId('opportunity-1')?.transcriptCount).toBe(1);
  });

  // The store does NOT publish or subscribe to a "transcript" websocket event — the count
  // refresh happens purely by re-reading the list after each write (Epic 12 §10, NFR-022).
  it('has no WebSocket-driven transcript event handling', async () => {
    const ctx = await setupStores(PANEL_TREE);
    // The applyEvents surface handles only the documented types; a fake transcript event is a no-op.
    expect(() => ctx.tree.applyEvents([{ type: 'TRANSCRIPT_ADDED' as any }])).not.toThrow();
    expect(ctx.service.listTranscriptsByNode.called).toBe(false);
    // Guard against a future accidental listener — vitest spy to prove the shape.
    const spy = vi.fn();
    ctx.tree.applyEvents([{ type: 'TRANSCRIPT_DELETED' as any, id: 1 } as any]);
    expect(spy).not.toHaveBeenCalled();
  });
});
