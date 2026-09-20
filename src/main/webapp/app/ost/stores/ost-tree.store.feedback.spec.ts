import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { createPinia, setActivePinia } from 'pinia';
import sinon, { type SinonStubbedInstance } from 'sinon';

import { dto, treeDto } from '../domain/fixtures.test-util';
import { DELETED_ELSEWHERE, DEMOTED_TO_VIEWER, REMOVED_FROM_TEAM } from '../ost-errors';
import OstService from '../ost.service';

import { type OstTreeEvent, PULSE_MS, useOstTreeStore } from './ost-tree.store';
import { useOstUiStore } from './ost-ui.store';

/**
 * RTC-006 — remote-change feedback in the store: the pulse (FR-036), what the open panel does on a
 * remote update or delete, and live `canEdit` (FR-037). The connection indicator itself is covered
 * by canvas/ConnectionIndicator.spec.ts, the rendered pulse by canvas/OstNode.spec.ts.
 */

const TREE = [
  dto('product-1', null, { sortOrder: 1 }),
  dto('outcome-1', 'product-1'),
  dto('opportunity-1', 'outcome-1', { status: 'EXPLORING', priority: 50, valueRating: 3 }),
  dto('solution-1', 'opportunity-1', { status: 'CANDIDATE' }),
];

/** The wire shape the broker actually sends (body under `payload`), as ost-realtime.store hands it on. */
const wire = (type: string, actingUserLogin: string | null, payload: unknown) =>
  ({ type, actingUserLogin, payload }) as unknown as OstTreeEvent;

describe('OST tree store — remote-change feedback (RTC-006)', () => {
  let service: SinonStubbedInstance<OstService>;
  let tree: ReturnType<typeof useOstTreeStore>;
  let ui: ReturnType<typeof useOstUiStore>;

  beforeEach(async () => {
    localStorage.clear();
    vi.useFakeTimers();
    setActivePinia(createPinia());
    service = sinon.createStubInstance(OstService);
    service.getTree.resolves(treeDto(TREE));
    tree = useOstTreeStore();
    ui = useOstUiStore();
    tree.setServiceFactory(() => service);
    await tree.loadTree(7);
  });

  afterEach(() => {
    tree.clearPulses();
    vi.useRealTimers();
  });

  // ---- FR-036: the pulse ------------------------------------------------------------------------
  it('a remote change pulses the node it touched, with the changing member’s name and initials', () => {
    tree.applyEvents([wire('NODE_UPDATED', 'admin', dto('opportunity-1', 'outcome-1', { title: 'Renamed remotely' }))]);
    const pulse = tree.pulseFor('opportunity-1');
    expect(pulse).toMatchObject({ key: 'opportunity-1', by: 'admin', name: 'Ana R', initials: 'AR' });
  });

  it('the pulse ends on its own after 1–2 s, without the node ever being mounted', () => {
    tree.applyEvents([wire('NODE_UPDATED', 'admin', dto('solution-1', 'opportunity-1', { title: 'Changed' }))]);
    expect(tree.pulseFor('solution-1')).not.toBeNull();
    expect(PULSE_MS).toBeGreaterThanOrEqual(1000);
    expect(PULSE_MS).toBeLessThanOrEqual(2000);
    vi.advanceTimersByTime(PULSE_MS - 1);
    expect(tree.pulseFor('solution-1')).not.toBeNull();
    vi.advanceTimersByTime(1);
    expect(tree.pulseFor('solution-1')).toBeNull();
  });

  it('a second change to the same node restarts the window instead of ending it early', () => {
    tree.applyEvents([wire('NODE_UPDATED', 'admin', dto('solution-1', 'opportunity-1', { title: 'One' }))]);
    const first = tree.pulseFor('solution-1')!.id;
    vi.advanceTimersByTime(PULSE_MS - 100);
    tree.applyEvents([wire('NODE_UPDATED', 'admin', dto('solution-1', 'opportunity-1', { title: 'Two' }))]);
    expect(tree.pulseFor('solution-1')!.id).toBeGreaterThan(first);
    vi.advanceTimersByTime(200); // past the FIRST window, inside the second
    expect(tree.pulseFor('solution-1')).not.toBeNull();
    vi.advanceTimersByTime(PULSE_MS);
    expect(tree.pulseFor('solution-1')).toBeNull();
  });

  it('every kind of remote change pulses its node, except a delete (nothing is left) and a membership change', () => {
    tree.applyEvents([wire('LINK_ADDED', 'admin', { nodeKey: 'opportunity-1', link: { id: 5, name: 'Doc', url: 'https://example.com' } })]);
    expect(tree.pulseFor('opportunity-1')).not.toBeNull();
    tree.clearPulses();

    tree.applyEvents([wire('COMMENT_ADDED', 'admin', { nodeKey: 'solution-1', comment: { id: 1, body: 'Hi' }, commentCount: 1 })]);
    expect(tree.pulseFor('solution-1')).not.toBeNull();
    tree.clearPulses();

    tree.applyEvents([wire('QUESTION_ADDED', 'admin', { nodeKey: 'opportunity-1', question: { id: 9, text: 'Why?', done: false } })]);
    expect(tree.pulseFor('opportunity-1')).not.toBeNull();
    tree.clearPulses();

    tree.applyEvents([wire('NODE_DELETED', 'admin', { key: 'solution-1' })]);
    expect(tree.pulseFor('solution-1')).toBeNull();
    expect(Object.keys(tree.pulses)).toEqual([]);

    tree.applyEvents([wire('MEMBERSHIP_CHANGED', 'admin', { login: 'admin', role: 'EDITOR', removed: false })]);
    expect(Object.keys(tree.pulses)).toEqual([]);
  });

  it('my own change never pulses, and neither does the echo of my own write', () => {
    tree.applyEvents([wire('NODE_UPDATED', 'user', dto('opportunity-1', 'outcome-1', { title: 'By me, elsewhere' }))]);
    expect(tree.pulseFor('opportunity-1')).toBeNull();
  });

  it('a change to a node this client does not have is not pulsed (nothing to point at)', () => {
    tree.applyEvents([wire('LINK_ADDED', 'admin', { nodeKey: 'opportunity-404', link: { id: 1, name: 'x', url: 'https://e.com' } })]);
    expect(tree.pulseFor('opportunity-404')).toBeNull();
  });

  it('switching team clears the pulses with the rest of the tree state', async () => {
    tree.applyEvents([wire('NODE_UPDATED', 'admin', dto('opportunity-1', 'outcome-1', { title: 'Changed' }))]);
    expect(tree.pulseFor('opportunity-1')).not.toBeNull();
    service.getTree.resolves(treeDto(TREE, { id: 8 }));
    await tree.loadTree(8);
    expect(Object.keys(tree.pulses)).toEqual([]);
  });

  // ---- FR-036: the open panel -------------------------------------------------------------------
  it('a remote update of the node open in the panel shows the new values', () => {
    ui.select('opportunity-1');
    tree.applyEvents([
      wire('NODE_UPDATED', 'admin', dto('opportunity-1', 'outcome-1', { title: 'New title', status: 'VALIDATED', valueRating: 5 })),
    ]);
    expect(ui.selectedId).toBe('opportunity-1');
    expect(tree.selected).toMatchObject({ title: 'New title', status: 'validated', value: 5 });
  });

  it('a remote delete of the node open in the panel closes the panel and shows the existing toast', () => {
    ui.select('solution-1');
    tree.applyEvents([wire('NODE_DELETED', 'admin', { key: 'solution-1' })]);
    expect(ui.selectedId).toBeNull(); // the canvas renders the panel on ui.selectedId
    expect(tree.error).toBe(DELETED_ELSEWHERE);
  });

  it('a remote delete of an ANCESTOR of the open node closes the panel too', () => {
    ui.select('solution-1');
    tree.applyEvents([wire('NODE_DELETED', 'admin', { key: 'outcome-1' })]);
    expect(tree.byId('solution-1')).toBeUndefined();
    expect(ui.selectedId).toBeNull();
    expect(tree.error).toBe(DELETED_ELSEWHERE);
  });

  it('a remote delete of a node nobody is looking at stays quiet', () => {
    ui.select('opportunity-1');
    tree.applyEvents([wire('NODE_DELETED', 'admin', { key: 'solution-1' })]);
    expect(ui.selectedId).toBe('opportunity-1');
    expect(tree.error).toBeNull();
  });

  // ---- FR-037: live canEdit ---------------------------------------------------------------------
  it('a demotion of this user drops canEdit live and says why', () => {
    expect(tree.canEdit).toBe(true);
    tree.applyEvents([wire('MEMBERSHIP_CHANGED', 'admin', { login: 'user', role: 'VIEWER', removed: false })]);
    expect(tree.canEdit).toBe(false);
    expect(tree.team?.currentUserRole).toBe('VIEWER');
    expect(tree.error).toBe(DEMOTED_TO_VIEWER);
  });

  it('a promotion of this user brings the edit affordances back without a reload', () => {
    tree.applyEvents([wire('MEMBERSHIP_CHANGED', 'admin', { login: 'user', role: 'VIEWER', removed: false })]);
    tree.clearError();
    tree.applyEvents([wire('MEMBERSHIP_CHANGED', 'admin', { login: 'user', role: 'EDITOR', removed: false })]);
    expect(tree.canEdit).toBe(true);
    expect(tree.team?.currentUserRole).toBe('EDITOR');
    expect(tree.error).toBeNull();
    expect(service.getTree.callCount, 'no reload was needed').toBe(1);
  });

  it('a removal of this user drops canEdit and says so', () => {
    tree.applyEvents([wire('MEMBERSHIP_CHANGED', 'admin', { login: 'user', role: null, removed: true })]);
    expect(tree.canEdit).toBe(false);
    expect(tree.error).toBe(REMOVED_FROM_TEAM);
    expect(tree.team?.members.some(m => m.login === 'user')).toBe(false);
  });

  it('another member’s role change updates the member list but not this user’s own permissions', () => {
    tree.applyEvents([wire('MEMBERSHIP_CHANGED', 'user', { login: 'admin', role: 'OWNER', removed: false })]);
    expect(tree.team?.members.find(m => m.login === 'admin')?.role).toBe('OWNER');
    expect(tree.canEdit).toBe(true);
    expect(tree.error).toBeNull();
  });
});
