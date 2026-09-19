import { beforeEach, describe, expect, it } from 'vitest';

import { createPinia, setActivePinia } from 'pinia';
import sinon, { type SinonStubbedInstance } from 'sinon';

import { dto, treeDto } from '../domain/fixtures.test-util';
import { DELETED_ELSEWHERE } from '../ost-errors';
import type { CommentDTO, NodeLinkDTO, OpenQuestionDTO } from '../ost.model';
import OstService, { REQUEST_ID_HEADER } from '../ost.service';

import { type OstTreeEvent, useOstTreeStore } from './ost-tree.store';

const TREE = [
  dto('product-1', null, { sortOrder: 1 }),
  dto('outcome-1', 'product-1'),
  dto('opportunity-1', 'outcome-1', { status: 'EXPLORING', priority: 50, valueRating: 3 }),
  dto('solution-1', 'opportunity-1', { status: 'CANDIDATE' }),
  dto('opportunity-2', 'outcome-1', { status: 'UNEXPLORED', sortOrder: 1 }),
];

function deferred<T>() {
  let resolve!: (value: T) => void;
  let reject!: (reason: unknown) => void;
  const promise = new Promise<T>((res, rej) => {
    resolve = res;
    reject = rej;
  });
  return { promise, resolve, reject };
}

describe('OST tree store — realtime event application (RTC-005)', () => {
  let service: SinonStubbedInstance<OstService>;
  let tree: ReturnType<typeof useOstTreeStore>;

  beforeEach(async () => {
    localStorage.clear();
    setActivePinia(createPinia());
    service = sinon.createStubInstance(OstService);
    service.getTree.resolves(treeDto(TREE));
    tree = useOstTreeStore();
    tree.setServiceFactory(() => service);
    await tree.loadTree(7);
  });

  // ---- criterion 1: NODE_CREATED / NODE_UPDATED --------------------------------------------
  it('NODE_CREATED adds the node and its derived counts follow', () => {
    const before = tree.evidenceThisMonth;
    const now = new Date().toISOString();
    tree.applyEvents([
      {
        type: 'NODE_CREATED',
        actingUserLogin: 'admin',
        node: dto('evidence-99', 'solution-1', { createdDate: now }),
      } as OstTreeEvent,
    ]);
    expect(tree.byId('evidence-99')?.parent).toBe('solution-1');
    expect(tree.evidenceThisMonth).toBe(before + 1);
  });

  it('NODE_UPDATED patches the fields of an existing node', () => {
    tree.applyEvents([
      {
        type: 'NODE_UPDATED',
        actingUserLogin: 'admin',
        node: dto('opportunity-1', 'outcome-1', { status: 'ADDRESSING', priority: 90, valueRating: 5 }),
      } as OstTreeEvent,
    ]);
    const opp = tree.byId('opportunity-1');
    expect(opp?.status).toBe('addressing');
    expect(opp?.priority).toBe(90);
    expect(opp?.value).toBe(5);
  });

  // ---- criterion 2: NODE_MOVED -----------------------------------------------------------------
  it('NODE_MOVED re-parents and applies siblings from both parents without a reload', () => {
    tree.applyEvents([
      {
        type: 'NODE_MOVED',
        actingUserLogin: 'admin',
        node: dto('solution-1', 'opportunity-2', { sortOrder: 0 }),
        siblings: [
          { key: 'opportunity-1', sortOrder: 5 },
          { key: 'opportunity-2', sortOrder: 10 },
        ],
      } as OstTreeEvent,
    ]);
    expect(tree.byId('solution-1')?.parent).toBe('opportunity-2');
    expect(tree.byId('opportunity-1')?.sortOrder).toBe(5);
    expect(tree.byId('opportunity-2')?.sortOrder).toBe(10);
    expect(service.getTree.callCount).toBe(1); // only the initial load
  });

  // ---- criterion 3: NODE_DELETED ----------------------------------------------------------------
  it('NODE_DELETED removes the node and every cascaded descendant', () => {
    tree.applyEvents([{ type: 'NODE_DELETED', actingUserLogin: 'admin', key: 'outcome-1' } as OstTreeEvent]);
    for (const key of ['outcome-1', 'opportunity-1', 'solution-1', 'opportunity-2']) {
      expect(tree.byId(key)).toBeUndefined();
    }
  });

  // ---- criterion 4: LINK / QUESTION / COMMENT events -------------------------------------------
  it('LINK_ADDED and LINK_REMOVED update the owning node collection', () => {
    const link: NodeLinkDTO = { id: 88, name: 'Doc', url: 'https://x.test/doc' };
    tree.applyEvents([{ type: 'LINK_ADDED', actingUserLogin: 'admin', key: 'solution-1', link } as OstTreeEvent]);
    expect(tree.byId('solution-1')?.links).toHaveLength(1);
    tree.applyEvents([{ type: 'LINK_REMOVED', actingUserLogin: 'admin', key: 'solution-1', linkId: 88 } as OstTreeEvent]);
    expect(tree.byId('solution-1')?.links).toHaveLength(0);
  });

  it('QUESTION_ADDED / QUESTION_UPDATED / QUESTION_REMOVED update the opportunity', () => {
    const q: OpenQuestionDTO = { id: 7, text: 'Who?', done: false };
    tree.applyEvents([{ type: 'QUESTION_ADDED', actingUserLogin: 'admin', key: 'opportunity-1', question: q } as OstTreeEvent]);
    expect(tree.byId('opportunity-1')?.questions).toEqual([{ id: 7, text: 'Who?', done: false }]);
    tree.applyEvents([
      { type: 'QUESTION_UPDATED', actingUserLogin: 'admin', key: 'opportunity-1', question: { ...q, done: true } } as OstTreeEvent,
    ]);
    expect(tree.byId('opportunity-1')?.questions[0].done).toBe(true);
    tree.applyEvents([{ type: 'QUESTION_REMOVED', actingUserLogin: 'admin', key: 'opportunity-1', questionId: 7 } as OstTreeEvent]);
    expect(tree.byId('opportunity-1')?.questions).toEqual([]);
  });

  it('COMMENT_ADDED / COMMENT_DELETED update the count and (if loaded) the thread', async () => {
    service.listComments.resolves([]);
    await tree.loadComments('solution-1');
    const comment: CommentDTO = {
      id: 11,
      body: 'hi',
      authorLogin: 'admin',
      authorInitials: 'A',
      authorName: 'Admin',
      createdDate: '2026-09-20T10:00:00Z',
      editedDate: null,
      mine: false,
    };
    tree.applyEvents([{ type: 'COMMENT_ADDED', actingUserLogin: 'admin', key: 'solution-1', comment, commentCount: 1 } as OstTreeEvent]);
    expect(tree.byId('solution-1')?.commentCount).toBe(1);
    expect(tree.comments['solution-1']).toHaveLength(1);
    tree.applyEvents([
      { type: 'COMMENT_DELETED', actingUserLogin: 'admin', key: 'solution-1', commentId: 11, commentCount: 0 } as OstTreeEvent,
    ]);
    expect(tree.byId('solution-1')?.commentCount).toBe(0);
    expect(tree.comments['solution-1']).toEqual([]);
  });

  // ---- criterion 5: request id echo suppression (FR-032) ---------------------------------------
  it('threads a request id through every REST write', async () => {
    service.patchNode.resolves(dto('opportunity-1', 'outcome-1', { priority: 60 }));
    await tree.patchNode('opportunity-1', { priority: 60 });
    expect(service.patchNode.firstCall.args[3]).toEqual(expect.any(String));

    service.addLink.resolves({ id: 1, name: 'x', url: 'https://x.test' });
    await tree.addLink('opportunity-1', { name: 'x', url: 'https://x.test' });
    expect(service.addLink.firstCall.args[3]).toEqual(expect.any(String));

    service.addComment.resolves({
      id: 1,
      body: 'hi',
      authorLogin: 'user',
      authorInitials: 'U',
      authorName: 'User',
      createdDate: '2026-09-20T10:00:00Z',
      editedDate: null,
      mine: true,
    });
    await tree.addComment('opportunity-1', 'hi');
    expect(service.addComment.firstCall.args[3]).toEqual(expect.any(String));
  });

  it('the request id header is exposed as a stable constant', () => {
    expect(REQUEST_ID_HEADER).toBe('X-OST-Request-Id');
  });

  it('drops the echo of its own write (actingUserLogin + request id) and does not double-apply', async () => {
    const pending = deferred<any>();
    service.patchNode.returns(pending.promise);
    const call = tree.patchNode('opportunity-1', { priority: 80 });
    const requestId = service.patchNode.firstCall.args[3] as string;

    // The server's echo arrives before the REST response returns.
    tree.applyEvents([
      {
        type: 'NODE_UPDATED',
        actingUserLogin: tree.team!.currentUserLogin,
        requestId,
        node: dto('opportunity-1', 'outcome-1', { priority: 42 }),
      } as OstTreeEvent,
    ]);
    // The echo was dropped: the optimistic value survives.
    expect(tree.byId('opportunity-1')?.priority).toBe(80);

    pending.resolve(dto('opportunity-1', 'outcome-1', { priority: 80 }));
    await call;
    expect(tree.byId('opportunity-1')?.priority).toBe(80);
  });

  // ---- criterion 6: in-flight / typing rule (FR-033) ------------------------------------------
  it('keeps a field with a pending patch and applies every other field', async () => {
    const pending = deferred<any>();
    service.patchNode.returns(pending.promise);
    const call = tree.patchNode('opportunity-1', { priority: 77 });
    // A remote update from someone else arrives while our patch is in flight.
    tree.applyEvents([
      {
        type: 'NODE_UPDATED',
        actingUserLogin: 'admin',
        node: dto('opportunity-1', 'outcome-1', { priority: 10, valueRating: 5, status: 'ADDRESSING' }),
      } as OstTreeEvent,
    ]);
    const opp = tree.byId('opportunity-1')!;
    expect(opp.priority).toBe(77); // held: local patch in flight
    expect(opp.value).toBe(5); // other field: applied
    expect(opp.status).toBe('addressing');
    expect(tree.droppedRemoteFor('opportunity-1').priority).toBe(10);

    pending.resolve(dto('opportunity-1', 'outcome-1', { priority: 77, valueRating: 5, status: 'ADDRESSING' }));
    await call;
  });

  it('keeps a field the user is currently typing in and records the dropped value', () => {
    tree.markTyping('opportunity-1', 'title');
    // Snapshot the local title before the remote event lands.
    const localTitle = tree.byId('opportunity-1')!.title;
    tree.applyEvents([
      {
        type: 'NODE_UPDATED',
        actingUserLogin: 'admin',
        node: dto('opportunity-1', 'outcome-1', { title: 'Remote title', priority: 12 }),
      } as OstTreeEvent,
    ]);
    expect(tree.byId('opportunity-1')?.title).toBe(localTitle);
    expect(tree.byId('opportunity-1')?.priority).toBe(12);
    expect(tree.droppedRemoteFor('opportunity-1').title).toBe('Remote title');
    tree.acknowledgeDroppedRemote('opportunity-1', 'title');
    expect(tree.droppedRemoteFor('opportunity-1').title).toBeUndefined();
    tree.clearTyping('opportunity-1');
  });

  // ---- criterion 8: remote delete during local edit --------------------------------------------
  it('remote NODE_DELETED during a local edit runs the "deleted by someone else" handling', async () => {
    const pending = deferred<any>();
    service.patchNode.returns(pending.promise);
    const call = tree.patchNode('solution-1', { title: 'Rename' });
    tree.applyEvents([{ type: 'NODE_DELETED', actingUserLogin: 'admin', key: 'solution-1' } as OstTreeEvent]);
    expect(tree.byId('solution-1')).toBeUndefined();
    expect(tree.error).toBe(DELETED_ELSEWHERE);
    pending.reject(Object.assign(new Error('x'), { response: { status: 404 } }));
    try {
      await call;
    } catch {
      // ignore
    }
  });

  // ---- criterion 9: two clients editing different fields converge -------------------------------
  it('two clients editing different fields of the same node converge on the last committed value per field', async () => {
    // Local client patches priority (in flight); remote client patches value (event arrives first).
    const pending = deferred<any>();
    service.patchNode.returns(pending.promise);
    const call = tree.patchNode('opportunity-1', { priority: 88 });
    tree.applyEvents([
      {
        type: 'NODE_UPDATED',
        actingUserLogin: 'admin',
        node: dto('opportunity-1', 'outcome-1', { priority: 1, valueRating: 5, status: 'EXPLORING' }),
      } as OstTreeEvent,
    ]);
    // The remote 'value' change is applied; the local 'priority' is preserved.
    expect(tree.byId('opportunity-1')!.value).toBe(5);
    expect(tree.byId('opportunity-1')!.priority).toBe(88);

    pending.resolve(dto('opportunity-1', 'outcome-1', { priority: 88, valueRating: 5, status: 'EXPLORING' }));
    await call;
    // Both fields settle on their last committed value.
    expect(tree.byId('opportunity-1')!.priority).toBe(88);
    expect(tree.byId('opportunity-1')!.value).toBe(5);
  });
});
