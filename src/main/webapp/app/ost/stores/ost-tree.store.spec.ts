import { beforeEach, describe, expect, it } from 'vitest';

import { createPinia, setActivePinia } from 'pinia';
import sinon, { type SinonStubbedInstance } from 'sinon';

import { bigTreeDtos, dto, treeDto } from '../domain/fixtures.test-util';
import OstService from '../ost.service';

import { useOstTreeStore } from './ost-tree.store';
import { collapsedStorageKey, lastTeamStorageKey, useOstUiStore } from './ost-ui.store';

const apiError = (status: number, message?: string) => Object.assign(new Error('http'), { response: { status, data: { message } } });

/** Resolves on demand so the optimistic (in-flight) state can be observed. */
function deferred<T>() {
  let resolve!: (value: T) => void;
  let reject!: (reason: unknown) => void;
  const promise = new Promise<T>((res, rej) => {
    resolve = res;
    reject = rej;
  });
  return { promise, resolve, reject };
}

const TREE = [
  dto('product-1', null, { sortOrder: 1 }),
  dto('outcome-1', 'product-1'),
  dto('opportunity-1', 'outcome-1', { status: 'EXPLORING', priority: 50, valueRating: 3 }),
  dto('solution-1', 'opportunity-1', { status: 'CANDIDATE' }),
  dto('assumption-1', 'solution-1', { status: 'TESTING', confidence: 40 }),
  dto('evidence-1', 'assumption-1'),
  dto('opportunity-2', 'outcome-1', { status: 'UNEXPLORED', sortOrder: 1 }),
];

describe('OST tree store', () => {
  let service: SinonStubbedInstance<OstService>;
  let tree: ReturnType<typeof useOstTreeStore>;
  let ui: ReturnType<typeof useOstUiStore>;

  beforeEach(async () => {
    localStorage.clear();
    setActivePinia(createPinia());
    service = sinon.createStubInstance(OstService);
    service.getTree.resolves(treeDto(TREE));
    tree = useOstTreeStore();
    ui = useOstUiStore();
    tree.setServiceFactory(() => service);
    await tree.loadTree(7);
  });

  describe('loadTree', () => {
    it('maps the team meta and nodes and remembers the team as last used', () => {
      expect(tree.team?.name).toBe('Team Jupiter');
      expect(tree.canEdit).toBe(true);
      expect(tree.nodes.map(n => n.id)).toEqual(TREE.map(d => d.key));
      expect(tree.byId('opportunity-1')?.status).toBe('exploring');
      expect(localStorage.getItem(lastTeamStorageKey('user'))).toBe('7');
      expect(Object.keys(tree.placed)).toHaveLength(TREE.length);
    });

    it('restores the collapsed map persisted for this user and team', async () => {
      localStorage.setItem(collapsedStorageKey('user', 8), JSON.stringify({ 'outcome-1': true, 'gone-1': true }));
      service.getTree.resolves(treeDto(TREE, { id: 8 }));
      await tree.loadTree(8);
      expect(ui.collapsed).toEqual({ 'outcome-1': true });
      expect(tree.placed['opportunity-1']).toBeUndefined();
    });

    it('tolerates corrupt persisted state', async () => {
      localStorage.setItem(collapsedStorageKey('user', 8), '{not json');
      service.getTree.resolves(treeDto(TREE, { id: 8 }));
      await tree.loadTree(8);
      expect(ui.collapsed).toEqual({});
    });

    it('reports forbidden and not-found failures', async () => {
      service.getTree.rejects(apiError(403));
      await tree.loadTree(99);
      expect(tree.loadError).toBe('forbidden');
      expect(tree.nodes).toEqual([]);
      service.getTree.rejects(apiError(404));
      await tree.loadTree(98);
      expect(tree.loadError).toBe('notFound');
    });

    it('ignores a stale response after a newer team switch', async () => {
      const slow = deferred<ReturnType<typeof treeDto>>();
      service.getTree.withArgs(10).returns(slow.promise);
      service.getTree.withArgs(11).resolves(treeDto(TREE, { id: 11, name: 'Team Venus' }));
      const first = tree.loadTree(10);
      await tree.loadTree(11);
      slow.resolve(treeDto([], { id: 10, name: 'Old' }));
      await first;
      expect(tree.team?.name).toBe('Team Venus');
    });
  });

  describe('patchNode', () => {
    it('applies the patch optimistically, then the server copy', async () => {
      const pending = deferred<any>();
      service.patchNode.returns(pending.promise);
      const done = tree.patchNode('opportunity-1', { status: 'validated', priority: 90 });
      expect(tree.byId('opportunity-1')).toMatchObject({ status: 'validated', priority: 90 });
      expect(service.patchNode.calledWith('opportunity', 1, { status: 'VALIDATED', priority: 90 })).toBe(true);
      pending.resolve(dto('opportunity-1', 'outcome-1', { status: 'VALIDATED', priority: 91, valueRating: 3 }));
      expect(await done).toBe(true);
      expect(tree.byId('opportunity-1')?.priority).toBe(91);
      expect(tree.error).toBeNull();
    });

    it('rolls back and surfaces an error when the server refuses', async () => {
      service.patchNode.rejects(apiError(400, 'error.invalidstatus'));
      const ok = await tree.patchNode('opportunity-1', { status: 'validated', note: 'x' });
      expect(ok).toBe(false);
      expect(tree.byId('opportunity-1')).toMatchObject({ status: 'exploring', note: '' });
      expect(tree.error).toBe('That status is not valid for this node.');
      tree.clearError();
      expect(tree.error).toBeNull();
    });

    describe('overlapping patches', () => {
      const OPP = 'opportunity-1';
      /** Server copy of opportunity-1 after the given edits (original: EXPLORING, priority 50, title "Title opportunity-1"). */
      const server = (extra: Parameters<typeof dto>[2] = {}) =>
        dto(OPP, 'outcome-1', { status: 'EXPLORING', priority: 50, valueRating: 3, ...extra });

      it('an earlier failure on another field does not revert a later edit (different fields)', async () => {
        const first = deferred<any>();
        const second = deferred<any>();
        service.patchNode.onFirstCall().returns(first.promise).onSecondCall().returns(second.promise);
        const a = tree.patchNode(OPP, { title: 'Renamed' });
        const b = tree.patchNode(OPP, { priority: 90 });
        expect(tree.byId(OPP)).toMatchObject({ title: 'Renamed', priority: 90 });

        second.resolve(server({ priority: 90 }));
        expect(await b).toBe(true);
        expect(tree.byId(OPP)).toMatchObject({ title: 'Renamed', priority: 90 });

        first.reject(apiError(400, 'error.invalidtitle'));
        expect(await a).toBe(false);
        expect(tree.byId(OPP)).toMatchObject({ title: 'Title opportunity-1', priority: 90 });
      });

      it('an earlier success does not overwrite a pending later edit of another field', async () => {
        const first = deferred<any>();
        const second = deferred<any>();
        service.patchNode.onFirstCall().returns(first.promise).onSecondCall().returns(second.promise);
        const a = tree.patchNode(OPP, { title: 'Renamed' });
        const b = tree.patchNode(OPP, { priority: 90 });

        // The first response predates the second patch on the server: priority is still 50 there.
        first.resolve(server({ title: 'Renamed' }));
        expect(await a).toBe(true);
        expect(tree.byId(OPP)).toMatchObject({ title: 'Renamed', priority: 90 });

        second.reject(apiError(500));
        expect(await b).toBe(false);
        expect(tree.byId(OPP)).toMatchObject({ title: 'Renamed', priority: 50 });
      });

      it('same field: an earlier failure after a later success keeps the later value', async () => {
        const first = deferred<any>();
        const second = deferred<any>();
        service.patchNode.onFirstCall().returns(first.promise).onSecondCall().returns(second.promise);
        const a = tree.patchNode(OPP, { priority: 70 });
        const b = tree.patchNode(OPP, { priority: 90 });
        second.resolve(server({ priority: 90 }));
        expect(await b).toBe(true);
        first.reject(apiError(500));
        expect(await a).toBe(false);
        expect(tree.byId(OPP)?.priority).toBe(90);
        expect(tree.error).toBeTruthy();
      });

      it('same field: a late earlier success does not overwrite the later value', async () => {
        const first = deferred<any>();
        const second = deferred<any>();
        service.patchNode.onFirstCall().returns(first.promise).onSecondCall().returns(second.promise);
        const a = tree.patchNode(OPP, { priority: 70 });
        const b = tree.patchNode(OPP, { priority: 90 });
        second.resolve(server({ priority: 90 }));
        await b;
        first.resolve(server({ priority: 70 }));
        await a;
        expect(tree.byId(OPP)?.priority).toBe(90);
      });

      it('same field: first fails while the second is pending, then the second succeeds', async () => {
        const first = deferred<any>();
        const second = deferred<any>();
        service.patchNode.onFirstCall().returns(first.promise).onSecondCall().returns(second.promise);
        const a = tree.patchNode(OPP, { priority: 70 });
        const b = tree.patchNode(OPP, { priority: 90 });
        first.reject(apiError(500));
        expect(await a).toBe(false);
        expect(tree.byId(OPP)?.priority).toBe(90);
        second.resolve(server({ priority: 90 }));
        expect(await b).toBe(true);
        expect(tree.byId(OPP)?.priority).toBe(90);
      });

      it('same field: the later fails first, then the earlier succeeds — the earlier value stands', async () => {
        const first = deferred<any>();
        const second = deferred<any>();
        service.patchNode.onFirstCall().returns(first.promise).onSecondCall().returns(second.promise);
        const a = tree.patchNode(OPP, { priority: 70 });
        const b = tree.patchNode(OPP, { priority: 90 });
        second.reject(apiError(500));
        expect(await b).toBe(false);
        expect(tree.byId(OPP)?.priority).toBe(70);
        first.resolve(server({ priority: 70 }));
        expect(await a).toBe(true);
        expect(tree.byId(OPP)?.priority).toBe(70);
      });

      it('same field: both fail in either order — back to the original value', async () => {
        const first = deferred<any>();
        const second = deferred<any>();
        service.patchNode.onFirstCall().returns(first.promise).onSecondCall().returns(second.promise);
        const a = tree.patchNode(OPP, { priority: 70 });
        const b = tree.patchNode(OPP, { priority: 90 });
        second.reject(apiError(500));
        await b;
        first.reject(apiError(500));
        await a;
        expect(tree.byId(OPP)?.priority).toBe(50);
      });

      it('takes server-side changes to fields the patch did not send when nothing else is pending', async () => {
        service.patchNode.resolves(server({ status: 'VALIDATED', priority: 61 }));
        await tree.patchNode(OPP, { status: 'validated' });
        expect(tree.byId(OPP)).toMatchObject({ status: 'validated', priority: 61 });
      });

      it('ignores responses that arrive after a team switch', async () => {
        const pending = deferred<any>();
        service.patchNode.returns(pending.promise);
        const done = tree.patchNode(OPP, { priority: 70 });
        service.getTree.resolves(treeDto(TREE, { id: 8 }));
        await tree.loadTree(8);
        pending.resolve(server({ priority: 70, title: 'Stale' }));
        await done;
        expect(tree.byId(OPP)).toMatchObject({ priority: 50, title: 'Title opportunity-1' });
      });
    });
  });

  describe('createNode', () => {
    it('waits for the server, then selects the new node and puts it in rename mode', async () => {
      ui.setCollapsed('solution-1', true);
      service.createNode.resolves(dto('assumption-9', 'solution-1', { status: 'UNTESTED', sortOrder: 5, title: 'New assumption' }));
      const key = await tree.createNode('solution-1');
      expect(service.createNode.calledWith({ type: 'ASSUMPTION', parentType: 'SOLUTION', parentId: 1 })).toBe(true);
      expect(key).toBe('assumption-9');
      expect(ui.selectedId).toBe('assumption-9');
      expect(ui.editingId).toBe('assumption-9');
      expect(ui.collapsed['solution-1']).toBe(false);
      const ids = tree.nodes.map(n => n.id);
      expect(ids.indexOf('assumption-9')).toBeGreaterThan(ids.indexOf('evidence-1'));
    });

    it('refuses a type the parent does not permit without calling the server', async () => {
      expect(await tree.createNode('solution-1', 'evidence')).toBeNull();
      expect(service.createNode.called).toBe(false);
      expect(tree.error).toBeTruthy();
    });

    it('keeps the parent’s cached history (the server writes history for the new node only)', async () => {
      service.listHistory.resolves([
        { id: 1, eventType: 'CREATED', summary: 'Created', authorLogin: null, authorInitials: null, createdDate: '' },
      ]);
      await tree.loadHistory('solution-1');
      service.createNode.resolves(dto('assumption-9', 'solution-1', { status: 'UNTESTED' }));
      await tree.createNode('solution-1');
      expect(tree.history['solution-1']).toHaveLength(1);
    });

    it('leaves the tree untouched when the server fails', async () => {
      service.createNode.rejects(apiError(403));
      expect(await tree.createNode('outcome-1', 'opportunity')).toBeNull();
      expect(tree.nodes).toHaveLength(TREE.length);
      expect(ui.selectedId).toBeNull();
      expect(tree.error).toBe('You do not have permission to change this tree.');
    });
  });

  describe('moveNode', () => {
    it('re-parents and applies the server sibling order', async () => {
      service.moveNode.resolves({
        node: dto('solution-1', 'opportunity-2', { status: 'CANDIDATE', sortOrder: 0 }),
        siblings: [{ key: 'solution-1', sortOrder: 0 }],
      });
      expect(await tree.moveNode('solution-1', 'opportunity-2')).toBe(true);
      expect(service.moveNode.calledWith({ nodeType: 'SOLUTION', nodeId: 1, parentType: 'OPPORTUNITY', parentId: 2 })).toBe(true);
      expect(tree.byId('solution-1')?.parent).toBe('opportunity-2');
      const ids = tree.nodes.map(n => n.id);
      expect(ids.indexOf('solution-1')).toBeGreaterThan(ids.indexOf('opportunity-2'));
      expect(ids.indexOf('assumption-1')).toBe(ids.indexOf('solution-1') + 1);
    });

    it('reorders siblings by the server response', async () => {
      service.moveNode.resolves({
        node: dto('opportunity-2', 'outcome-1', { sortOrder: 0 }),
        siblings: [
          { key: 'opportunity-2', sortOrder: 0 },
          { key: 'opportunity-1', sortOrder: 1 },
        ],
      });
      expect(await tree.moveNode('opportunity-2', 'outcome-1', 0)).toBe(true);
      expect(tree.childrenOf('outcome-1').map(n => n.id)).toEqual(['opportunity-2', 'opportunity-1']);
    });

    it('rolls back when the server refuses', async () => {
      service.moveNode.rejects(apiError(400, 'error.cycle'));
      expect(await tree.moveNode('solution-1', 'opportunity-2')).toBe(false);
      expect(tree.byId('solution-1')?.parent).toBe('opportunity-1');
      expect(tree.nodes.map(n => n.id)).toEqual(TREE.map(d => d.key));
      expect(tree.error).toBe('A node cannot move under its own descendant.');
    });

    it('a failed move does not undo a patch that succeeded meanwhile', async () => {
      const move = deferred<any>();
      service.moveNode.returns(move.promise);
      const moving = tree.moveNode('solution-1', 'opportunity-2');
      expect(tree.byId('solution-1')?.parent).toBe('opportunity-2');

      service.patchNode.resolves(dto('solution-1', 'opportunity-1', { status: 'BUILDING', title: 'Renamed' }));
      expect(await tree.patchNode('solution-1', { title: 'Renamed', status: 'building' })).toBe(true);
      // The patch response carries the server's (old) parent; the in-flight move keeps its placement.
      expect(tree.byId('solution-1')).toMatchObject({ parent: 'opportunity-2', title: 'Renamed', status: 'building' });

      move.reject(apiError(409, 'error.concurrencyFailure'));
      expect(await moving).toBe(false);
      expect(tree.byId('solution-1')).toMatchObject({ parent: 'opportunity-1', title: 'Renamed', status: 'building' });
      expect(tree.nodes.map(n => n.id)).toEqual(TREE.map(d => d.key));
      expect(tree.error).toBe('Someone else changed this tree at the same moment — please try again.');
    });

    it('a successful move does not revert a pending patch', async () => {
      const patch = deferred<any>();
      service.patchNode.returns(patch.promise);
      const patching = tree.patchNode('solution-1', { title: 'Renamed' });
      service.moveNode.resolves({
        node: dto('solution-1', 'opportunity-2', { status: 'CANDIDATE', sortOrder: 0 }),
        siblings: [{ key: 'solution-1', sortOrder: 0 }],
      });
      expect(await tree.moveNode('solution-1', 'opportunity-2')).toBe(true);
      expect(tree.byId('solution-1')).toMatchObject({ parent: 'opportunity-2', title: 'Renamed' });
      patch.resolve(dto('solution-1', 'opportunity-2', { status: 'CANDIDATE', title: 'Renamed' }));
      expect(await patching).toBe(true);
      expect(tree.byId('solution-1')).toMatchObject({ parent: 'opportunity-2', title: 'Renamed' });
    });

    it('rejects illegal targets locally', async () => {
      expect(await tree.moveNode('evidence-1', 'solution-1')).toBe(false);
      expect(await tree.moveNode('opportunity-1', 'solution-1')).toBe(false);
      expect(service.moveNode.called).toBe(false);
    });
  });

  describe('deleteNode', () => {
    it('removes the whole subtree locally after the 204 and forgets it in the UI', async () => {
      ui.select('assumption-1');
      ui.setCollapsed('solution-1', true);
      service.deleteNode.resolves();
      expect(await tree.deleteNode('solution-1')).toBe(true);
      expect(service.deleteNode.calledWith('solution', 1)).toBe(true);
      expect(tree.nodes.map(n => n.id)).toEqual(['product-1', 'outcome-1', 'opportunity-1', 'opportunity-2']);
      expect(ui.selectedId).toBeNull();
      expect(ui.collapsed['solution-1']).toBeUndefined();
    });

    it('keeps everything when the server fails', async () => {
      service.deleteNode.rejects(apiError(403));
      expect(await tree.deleteNode('solution-1')).toBe(false);
      expect(tree.nodes).toHaveLength(TREE.length);
    });
  });

  describe('collaboration', () => {
    it('adds, edits and removes links, rolling back a failed removal', async () => {
      service.addLink.resolves({ id: 5, name: 'Doc', url: 'https://x.test' });
      await tree.addLink('outcome-1', { name: 'Doc', url: 'https://x.test' });
      expect(tree.byId('outcome-1')?.links).toEqual([{ id: 5, name: 'Doc', url: 'https://x.test' }]);

      service.updateLink.resolves({ id: 5, name: 'Spec', url: 'https://x.test' });
      await tree.updateLink('outcome-1', 5, { name: 'Spec' });
      expect(tree.byId('outcome-1')?.links[0].name).toBe('Spec');

      service.deleteLink.rejects(apiError(403));
      expect(await tree.removeLink('outcome-1', 5)).toBe(false);
      expect(tree.byId('outcome-1')?.links).toHaveLength(1);
    });

    it('restores only missing default links', async () => {
      service.addLink.callsFake(async (_t, _id, link) => ({ id: Math.random(), ...link }));
      await tree.restoreDefaultLinks('opportunity-1');
      expect(tree.byId('opportunity-1')?.links.map(l => l.name)).toEqual(['Confluence', 'Jira Initiative', 'Jira Epic']);
      service.addLink.resetHistory();
      await tree.restoreDefaultLinks('opportunity-1');
      expect(service.addLink.called).toBe(false);
    });

    it('toggles open questions optimistically with rollback', async () => {
      service.addQuestion.resolves({ id: 3, text: 'Who?', done: false });
      await tree.addQuestion('opportunity-1', 'Who?');
      service.updateQuestion.rejects(apiError(500));
      expect(await tree.updateQuestion('opportunity-1', 3, { done: true })).toBe(false);
      expect(tree.byId('opportunity-1')?.questions[0].done).toBe(false);
      expect(await tree.addQuestion('solution-1', 'nope')).toBe(false);
    });

    it('keeps comment counts in step with the thread', async () => {
      const comment = {
        id: 1,
        body: 'hi',
        authorLogin: 'user',
        authorInitials: 'KP',
        authorName: 'Kira',
        createdDate: '',
        editedDate: null,
        mine: true,
      };
      service.listComments.resolves([comment]);
      await tree.loadComments('solution-1');
      expect(tree.byId('solution-1')?.commentCount).toBe(1);
      service.addComment.resolves({ ...comment, id: 2, body: 'again' });
      await tree.addComment('solution-1', 'again');
      expect(tree.comments['solution-1']).toHaveLength(2);
      expect(tree.byId('solution-1')?.commentCount).toBe(2);
      service.deleteComment.resolves();
      await tree.deleteComment('solution-1', 1);
      expect(tree.byId('solution-1')?.commentCount).toBe(1);
      expect(await tree.loadComments('product-1')).toEqual([]);
    });

    it('invalidates cached history after a write', async () => {
      service.listHistory.resolves([
        { id: 1, eventType: 'CREATED', summary: 'Created', authorLogin: null, authorInitials: null, createdDate: '' },
      ]);
      await tree.loadHistory('opportunity-1');
      expect(tree.history['opportunity-1']).toHaveLength(1);
      service.patchNode.resolves(dto('opportunity-1', 'outcome-1', { status: 'PARKED' }));
      await tree.patchNode('opportunity-1', { status: 'parked' });
      expect(tree.history['opportunity-1']).toBeUndefined();
    });
  });

  describe('dashboard figures follow local writes', () => {
    it('recounts evidence this month after creating and deleting evidence', async () => {
      const now = new Date().toISOString();
      const lastMonth = new Date(Date.UTC(new Date().getUTCFullYear(), new Date().getUTCMonth() - 1, 10)).toISOString();
      service.getTree.resolves(
        treeDto([...TREE.slice(0, 5), dto('evidence-1', 'assumption-1', { createdDate: lastMonth })], { evidenceThisMonth: 7 }),
      );
      await tree.loadTree(7);
      expect(tree.evidenceThisMonth).toBe(0);

      service.createNode.resolves(dto('evidence-5', 'assumption-1', { createdDate: now }));
      await tree.createNode('assumption-1', 'evidence');
      expect(tree.evidenceThisMonth).toBe(1);

      service.deleteNode.resolves();
      await tree.deleteNode('evidence-5');
      expect(tree.evidenceThisMonth).toBe(0);
    });

    it('records local edits as the branch’s last activity by the current user', async () => {
      expect(tree.byId('product-1')?.lastActivity).toBeNull();
      service.patchNode.resolves(dto('opportunity-1', 'outcome-1', { status: 'PARKED', lastModifiedDate: '2099-01-01T00:00:00Z' }));
      await tree.patchNode('opportunity-1', { status: 'parked' });
      expect(tree.byId('product-1')?.lastActivity).toEqual({ at: '2099-01-01T00:00:00Z', byLogin: 'user' });

      // An older stamp never moves it backwards.
      service.createNode.resolves(dto('evidence-9', 'assumption-1', { createdDate: '2026-01-01T00:00:00Z' }));
      await tree.createNode('assumption-1', 'evidence');
      expect(tree.byId('product-1')?.lastActivity?.at).toBe('2099-01-01T00:00:00Z');
    });
  });

  describe('derived layout at scale', () => {
    it('keeps placed and descendantCount fast on a 300+ node tree while nodes are edited', async () => {
      const big = bigTreeDtos(3, 25);
      expect(big.length).toBeGreaterThan(300);
      service.getTree.resolves(treeDto(big, { id: 9 }));
      await tree.loadTree(9);
      expect(Object.keys(tree.placed)).toHaveLength(big.length);
      const product = big[0].key;
      expect(tree.descendantCount(product)).toBe(big.filter((_, i) => i > 0 && i < big.length / 3).length);

      const opportunity = big.find(d => d.type === 'OPPORTUNITY')!;
      service.patchNode.callsFake(async (_type, _id, body) =>
        dto(opportunity.key, opportunity.parentKey, { ...opportunity, priority: body.priority }),
      );
      const started = performance.now();
      for (let i = 1; i <= 20; i++) {
        await tree.patchNode(opportunity.key, { priority: i });
        void tree.placed;
        for (const n of tree.nodes) tree.descendantCount(n.id);
      }
      expect(performance.now() - started).toBeLessThan(1500);
      expect(tree.byId(opportunity.key)?.priority).toBe(20);
    });
  });
});
