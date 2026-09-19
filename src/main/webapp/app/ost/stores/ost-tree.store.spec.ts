import { beforeEach, describe, expect, it } from 'vitest';

import { createPinia, setActivePinia } from 'pinia';
import sinon, { type SinonStubbedInstance } from 'sinon';

import { dto, treeDto } from '../domain/fixtures.test-util';
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
});
