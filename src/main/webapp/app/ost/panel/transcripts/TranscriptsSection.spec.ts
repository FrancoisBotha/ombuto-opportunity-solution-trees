/**
 * MTRANS-005 — Transcripts section + viewer specs (list ordering, lazy body fetch,
 * safe plain-text rendering, viewer-role permissions, canvas badge counts).
 */
import { afterEach, describe, expect, it } from 'vitest';

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
    // No body fetch just from listing.
    expect(service.getTranscript.called).toBe(false);
    const rows = wrapper.findAll('[data-cy^="ost-transcript-item-"]');
    expect(rows.map(r => r.attributes('data-transcript-id'))).toEqual(['3', '2', '1']);
    const row = rows[0];
    expect(row.text()).toContain('Latest interview');
    expect(row.text()).toContain('2026-09-01');
    expect(row.text()).toContain('Kira, Ana');
  });

  it('shows an empty state when the node has no transcripts', async () => {
    const { wrapper } = await mountSection('EDITOR', []);
    expect(wrapper.find('[data-cy="ost-transcripts-empty"]').exists()).toBe(true);
  });

  it('renders no mutation controls (no add / edit / delete)', async () => {
    const { wrapper } = await mountSection('EDITOR', [meta(1, 'A transcript', '2026-09-01T09:00:00Z')]);
    // The read side of MTRANS-005 does not surface any write affordances.
    expect(wrapper.find('[data-cy="ost-transcript-add"]').exists()).toBe(false);
    expect(wrapper.find('[data-cy="ost-transcript-edit-1"]').exists()).toBe(false);
    expect(wrapper.find('[data-cy="ost-transcript-delete-1"]').exists()).toBe(false);
  });

  it('renders identically for VIEWER role — list only, no mutation controls', async () => {
    const { wrapper, tree } = await mountSection('VIEWER', [meta(1, 'A transcript', '2026-09-01T09:00:00Z')]);
    expect(tree.canEdit).toBe(false);
    expect(wrapper.find('[data-cy="ost-transcript-item-1"]').exists()).toBe(true);
    expect(wrapper.find('[data-cy="ost-transcript-add"]').exists()).toBe(false);
    expect(wrapper.find('[data-cy="ost-transcript-edit-1"]').exists()).toBe(false);
    expect(wrapper.find('[data-cy="ost-transcript-delete-1"]').exists()).toBe(false);
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
    // The tag names must appear as literal text; no <script> or <b> element created inside.
    expect(body.textContent).toBe(evil);
    expect(body.querySelector('script')).toBeNull();
    expect(body.querySelector('b')).toBeNull();
    expect((window as any).__pwn).toBeUndefined();
  });

  it('viewers get the same read-only viewer as editors (no edit / delete buttons)', async () => {
    const { wrapper } = await mountViewer('VIEWER', 'a transcript');
    expect(wrapper.find('[data-cy="ost-transcript-viewer-edit"]').exists()).toBe(false);
    expect(wrapper.find('[data-cy="ost-transcript-viewer-delete"]').exists()).toBe(false);
    expect(document.querySelector('[data-cy="ost-transcript-viewer-body"]')?.textContent).toBe('a transcript');
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

describe('tree mapping — transcriptCount', () => {
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
});
