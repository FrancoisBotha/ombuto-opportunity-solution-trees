/**
 * MTRANS-006 — Add / edit transcript dialog: paste, parsed-upload review, validation, edit,
 * error handling. Verifies the two save sources (PASTED vs UPLOADED), that an invalid upload
 * leaves the draft intact, and that edit mode never touches the node relationship.
 */
import { afterEach, describe, expect, it } from 'vitest';

import { enableAutoUnmount, flushPromises, mount } from '@vue/test-utils';

import type { TranscriptDTO, TranscriptMetaDTO } from '../../ost.model';
import { PANEL_TREE, apiError, setupStores } from '../panel.test-util';

import TranscriptFormDialog from './TranscriptFormDialog.vue';

enableAutoUnmount(afterEach);

const meta = (id: number, extra: Partial<TranscriptMetaDTO> = {}): TranscriptMetaDTO => ({
  id,
  title: `Transcript ${id}`,
  meetingDate: '2026-09-05',
  attendees: 'Kira, Ana',
  source: 'PASTED',
  nodeType: 'OPPORTUNITY',
  nodeId: 1,
  nodeKey: 'opportunity-1',
  authorLogin: 'user',
  authorInitials: 'KP',
  authorName: 'Kira P',
  createdDate: '2026-09-05T10:00:00Z',
  editedDate: null,
  ...extra,
});
const full = (id: number, body: string, extra: Partial<TranscriptDTO> = {}): TranscriptDTO => ({ ...meta(id, extra), body });

async function mountForm(opts: { role?: 'OWNER' | 'EDITOR' | 'VIEWER'; transcript?: TranscriptDTO | null } = {}) {
  const role = opts.role ?? 'EDITOR';
  const ctx = await setupStores(PANEL_TREE, { currentUserRole: role, canEdit: role !== 'VIEWER' });
  const wrapper = mount(TranscriptFormDialog, {
    attachTo: document.body,
    props: { nodeKey: 'opportunity-1', transcript: opts.transcript ?? null },
    global: { plugins: [ctx.pinia] },
  });
  await flushPromises();
  return { ...ctx, wrapper };
}

const setField = async (wrapper: any, cy: string, value: string) => {
  const el = wrapper.get(`[data-cy="${cy}"]`);
  await el.setValue(value);
};

describe('TranscriptFormDialog — paste (source=PASTED)', () => {
  it('creates a transcript from a manual paste with source=PASTED and closes on success', async () => {
    const { wrapper, service, tree } = await mountForm();
    service.createTranscript.resolves(full(9, 'pasted body', { title: 'Discovery chat', source: 'PASTED' }));
    service.listTranscriptsByNode.resolves([meta(9, { title: 'Discovery chat', source: 'PASTED' })]);
    await setField(wrapper, 'ost-transcript-form-title', 'Discovery chat');
    await setField(wrapper, 'ost-transcript-form-date', '2026-09-10');
    await setField(wrapper, 'ost-transcript-form-attendees', 'Kira, Ana');
    await setField(wrapper, 'ost-transcript-form-body', 'pasted body');
    await wrapper.get('[data-cy="ost-transcript-form-save"]').trigger('click');
    await flushPromises();
    expect(service.createTranscript.calledOnce).toBe(true);
    const arg = service.createTranscript.firstCall.args[0];
    expect(arg).toMatchObject({
      title: 'Discovery chat',
      meetingDate: '2026-09-10',
      attendees: 'Kira, Ana',
      body: 'pasted body',
      source: 'PASTED',
      opportunityId: 1,
    });
    expect(wrapper.emitted('saved')).toBeTruthy();
    expect(wrapper.emitted('close')).toBeTruthy();
    // The write refreshed the list, so the count reflects the new transcript.
    expect(tree.byId('opportunity-1')?.transcriptCount).toBe(1);
  });

  it('save is disabled while required fields (title, date, body) are missing', async () => {
    const { wrapper } = await mountForm();
    const saveBtn = wrapper.get('[data-cy="ost-transcript-form-save"]');
    expect((saveBtn.element as HTMLButtonElement).disabled).toBe(true);
    await setField(wrapper, 'ost-transcript-form-title', 'Only a title');
    expect((saveBtn.element as HTMLButtonElement).disabled).toBe(true);
    await setField(wrapper, 'ost-transcript-form-date', '2026-09-10');
    expect((saveBtn.element as HTMLButtonElement).disabled).toBe(true);
    await setField(wrapper, 'ost-transcript-form-body', 'a body');
    expect((saveBtn.element as HTMLButtonElement).disabled).toBe(false);
  });
});

describe('TranscriptFormDialog — parsed upload (source=UPLOADED)', () => {
  it('parses an uploaded file into the textarea for review, and only saves on explicit confirmation with source=UPLOADED', async () => {
    const { wrapper, service } = await mountForm();
    service.parseTranscriptUpload.resolves({ body: 'Speaker: hello\nnext line' });
    service.createTranscript.resolves(full(11, 'Speaker: hello\nnext line', { source: 'UPLOADED' }));
    service.listTranscriptsByNode.resolves([]);
    // Fill in metadata; a file drop should NOT auto-save.
    await setField(wrapper, 'ost-transcript-form-title', 'The interview');
    await setField(wrapper, 'ost-transcript-form-date', '2026-09-10');
    const fileInput = wrapper.get('[data-cy="ost-transcript-form-file"]').element as HTMLInputElement;
    const file = new File(['ignored — server parses'], 'call.vtt', { type: 'text/vtt' });
    Object.defineProperty(fileInput, 'files', { value: [file], configurable: true });
    await fileInput.dispatchEvent(new Event('change'));
    await flushPromises();
    expect(service.parseTranscriptUpload.calledOnce).toBe(true);
    expect(service.parseTranscriptUpload.firstCall.args[1]).toBe(file);
    // Body filled for review; nothing saved yet.
    expect((wrapper.get('[data-cy="ost-transcript-form-body"]').element as HTMLTextAreaElement).value).toBe('Speaker: hello\nnext line');
    expect(wrapper.find('[data-cy="ost-transcript-form-uploaded"]').exists()).toBe(true);
    expect(service.createTranscript.called).toBe(false);
    // Explicit confirmation saves with source=UPLOADED.
    await wrapper.get('[data-cy="ost-transcript-form-save"]').trigger('click');
    await flushPromises();
    expect(service.createTranscript.calledOnce).toBe(true);
    expect(service.createTranscript.firstCall.args[0].source).toBe('UPLOADED');
    expect(service.createTranscript.firstCall.args[0].body).toBe('Speaker: hello\nnext line');
  });

  it('rejects a .docx upload client-side, shows a clear error, and keeps the draft intact — no create', async () => {
    const { wrapper, service } = await mountForm();
    await setField(wrapper, 'ost-transcript-form-title', 'My draft');
    await setField(wrapper, 'ost-transcript-form-date', '2026-09-10');
    await setField(wrapper, 'ost-transcript-form-body', 'my pasted draft body');
    const fileInput = wrapper.get('[data-cy="ost-transcript-form-file"]').element as HTMLInputElement;
    const bad = new File(['x'], 'notes.docx', { type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document' });
    Object.defineProperty(fileInput, 'files', { value: [bad], configurable: true });
    await fileInput.dispatchEvent(new Event('change'));
    await flushPromises();
    expect(service.parseTranscriptUpload.called).toBe(false);
    expect(service.createTranscript.called).toBe(false);
    const error = wrapper.get('[data-cy="ost-transcript-form-error"]');
    expect(error.text()).toMatch(/\.txt.*\.vtt.*\.srt/);
    // The draft is untouched — title, date and body still there.
    expect((wrapper.get('[data-cy="ost-transcript-form-title"]').element as HTMLInputElement).value).toBe('My draft');
    expect((wrapper.get('[data-cy="ost-transcript-form-body"]').element as HTMLTextAreaElement).value).toBe('my pasted draft body');
  });

  it('server rejects an oversized upload — the parse error is surfaced and no transcript is created', async () => {
    const { wrapper, service } = await mountForm();
    service.parseTranscriptUpload.rejects(apiError(400, 'error.transcriptuploadtoolarge'));
    await setField(wrapper, 'ost-transcript-form-title', 'Big one');
    await setField(wrapper, 'ost-transcript-form-date', '2026-09-10');
    await setField(wrapper, 'ost-transcript-form-body', 'keeps my earlier draft');
    const fileInput = wrapper.get('[data-cy="ost-transcript-form-file"]').element as HTMLInputElement;
    const file = new File(['x'.repeat(10)], 'big.txt', { type: 'text/plain' });
    Object.defineProperty(fileInput, 'files', { value: [file], configurable: true });
    await fileInput.dispatchEvent(new Event('change'));
    await flushPromises();
    expect(service.createTranscript.called).toBe(false);
    expect(wrapper.find('[data-cy="ost-transcript-form-error"]').exists()).toBe(true);
    // Draft untouched.
    expect((wrapper.get('[data-cy="ost-transcript-form-body"]').element as HTMLTextAreaElement).value).toBe('keeps my earlier draft');
  });
});

describe('TranscriptFormDialog — edit mode', () => {
  it('prefills fields from the transcript and PATCHes without touching the node relationship', async () => {
    const { wrapper, service } = await mountForm({
      transcript: full(42, 'old body', { title: 'The interview', meetingDate: '2026-09-01', attendees: 'Kira' }),
    });
    // Prefilled.
    expect((wrapper.get('[data-cy="ost-transcript-form-title"]').element as HTMLInputElement).value).toBe('The interview');
    expect((wrapper.get('[data-cy="ost-transcript-form-body"]').element as HTMLTextAreaElement).value).toBe('old body');
    // The upload drop area is hidden in edit mode — a transcript is bound to its node for life,
    // and the parse-then-fill flow is only meaningful on create (criterion 4 vs 2).
    expect(wrapper.find('[data-cy="ost-transcript-form-drop"]').exists()).toBe(false);
    service.updateTranscript.resolves(full(42, 'new body', { title: 'The interview (v2)' }));
    service.listTranscriptsByNode.resolves([]);
    await setField(wrapper, 'ost-transcript-form-title', 'The interview (v2)');
    await setField(wrapper, 'ost-transcript-form-body', 'new body');
    await wrapper.get('[data-cy="ost-transcript-form-save"]').trigger('click');
    await flushPromises();
    expect(service.updateTranscript.calledOnce).toBe(true);
    const [id, req] = service.updateTranscript.firstCall.args;
    expect(id).toBe(42);
    expect(req.title).toBe('The interview (v2)');
    expect(req.body).toBe('new body');
    // No node id fields in the PATCH body — the transcript stays on its node for life.
    expect(req.opportunityId).toBeUndefined();
    expect(req.productId).toBeUndefined();
    expect(wrapper.emitted('saved')).toBeTruthy();
  });
});
