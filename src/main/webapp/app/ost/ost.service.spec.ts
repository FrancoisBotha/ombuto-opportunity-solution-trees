import { afterAll, beforeEach, describe, expect, it } from 'vitest';

import axios from 'axios';
import sinon from 'sinon';

import OstService from './ost.service';

type Verb = 'get' | 'post' | 'patch' | 'delete';

const stubs = {
  get: sinon.stub(axios, 'get'),
  post: sinon.stub(axios, 'post'),
  patch: sinon.stub(axios, 'patch'),
  delete: sinon.stub(axios, 'delete'),
};

describe('OstService', () => {
  const service = new OstService();

  beforeEach(() => {
    for (const stub of Object.values(stubs)) {
      stub.reset();
      stub.resolves({ data: { ok: true } });
    }
  });

  afterAll(() => {
    for (const stub of Object.values(stubs)) stub.restore();
  });

  /** Each call: the method invocation, the expected verb, URL and (for writes) body, and whether it returns the data. */
  const cases: { name: string; call: () => Promise<unknown>; verb: Verb; url: string; body?: unknown; returnsData: boolean }[] = [
    { name: 'listMyTeams', call: () => service.listMyTeams(), verb: 'get', url: 'api/team-management/my-teams', returnsData: true },
    { name: 'getTree', call: () => service.getTree(7), verb: 'get', url: 'api/teams/7/tree', returnsData: true },
    {
      name: 'createNode',
      call: () => service.createNode({ type: 'SOLUTION', parentType: 'OPPORTUNITY', parentId: 3, title: 'New' }),
      verb: 'post',
      url: 'api/tree/nodes',
      body: { type: 'SOLUTION', parentType: 'OPPORTUNITY', parentId: 3, title: 'New' },
      returnsData: true,
    },
    {
      name: 'patchNode',
      call: () => service.patchNode('OPPORTUNITY', 12, { priority: 90 }),
      verb: 'patch',
      url: 'api/tree/nodes/opportunity/12',
      body: { priority: 90 },
      returnsData: true,
    },
    {
      name: 'moveNode',
      call: () => service.moveNode({ nodeType: 'SOLUTION', nodeId: 1, parentType: 'OPPORTUNITY', parentId: 2, position: 0 }),
      verb: 'post',
      url: 'api/tree/nodes/move',
      body: { nodeType: 'SOLUTION', nodeId: 1, parentType: 'OPPORTUNITY', parentId: 2, position: 0 },
      returnsData: true,
    },
    {
      name: 'deleteNode',
      call: () => service.deleteNode('Assumption', 4),
      verb: 'delete',
      url: 'api/tree/nodes/assumption/4',
      returnsData: false,
    },
    {
      name: 'addLink',
      call: () => service.addLink('solution', 5, { name: 'Doc', url: 'https://x.test' }),
      verb: 'post',
      url: 'api/tree/nodes/solution/5/links',
      body: { name: 'Doc', url: 'https://x.test' },
      returnsData: true,
    },
    {
      name: 'updateLink',
      call: () => service.updateLink(9, { name: 'Spec' }),
      verb: 'patch',
      url: 'api/tree/links/9',
      body: { name: 'Spec' },
      returnsData: true,
    },
    { name: 'deleteLink', call: () => service.deleteLink(9), verb: 'delete', url: 'api/tree/links/9', returnsData: false },
    {
      name: 'addQuestion',
      call: () => service.addQuestion(12, 'Who?'),
      verb: 'post',
      url: 'api/tree/opportunities/12/questions',
      body: { text: 'Who?' },
      returnsData: true,
    },
    {
      name: 'updateQuestion',
      call: () => service.updateQuestion(3, { done: true }),
      verb: 'patch',
      url: 'api/tree/questions/3',
      body: { done: true },
      returnsData: true,
    },
    { name: 'deleteQuestion', call: () => service.deleteQuestion(3), verb: 'delete', url: 'api/tree/questions/3', returnsData: false },
    {
      name: 'listComments',
      call: () => service.listComments('EVIDENCE', 8),
      verb: 'get',
      url: 'api/tree/nodes/evidence/8/comments',
      returnsData: true,
    },
    {
      name: 'addComment',
      call: () => service.addComment('outcome', 2, 'hi'),
      verb: 'post',
      url: 'api/tree/nodes/outcome/2/comments',
      body: { body: 'hi' },
      returnsData: true,
    },
    {
      name: 'updateComment',
      call: () => service.updateComment(6, 'edited'),
      verb: 'patch',
      url: 'api/tree/comments/6',
      body: { body: 'edited' },
      returnsData: true,
    },
    { name: 'deleteComment', call: () => service.deleteComment(6), verb: 'delete', url: 'api/tree/comments/6', returnsData: false },
    {
      name: 'listHistory',
      call: () => service.listHistory('solution', 5),
      verb: 'get',
      url: 'api/tree/nodes/solution/5/history',
      returnsData: true,
    },
  ];

  it('covers every public method', () => {
    const methods = Object.getOwnPropertyNames(OstService.prototype).filter(m => m !== 'constructor');
    expect(cases.map(c => c.name).sort()).toEqual(methods.sort());
  });

  it.each(cases)('$name → $verb $url', async ({ call, verb, url, body, returnsData }) => {
    const result = await call();
    const stub = stubs[verb];
    expect(stub.calledOnce).toBe(true);
    expect(stub.firstCall.args[0]).toBe(url);
    if (body !== undefined) expect(stub.firstCall.args[1]).toEqual(body);
    for (const other of Object.keys(stubs) as Verb[]) if (other !== verb) expect(stubs[other].called).toBe(false);
    expect(result).toEqual(returnsData ? { ok: true } : undefined);
  });

  it('propagates HTTP errors', async () => {
    stubs.get.rejects(Object.assign(new Error('http'), { response: { status: 403 } }));
    await expect(service.getTree(1)).rejects.toMatchObject({ response: { status: 403 } });
  });
});
