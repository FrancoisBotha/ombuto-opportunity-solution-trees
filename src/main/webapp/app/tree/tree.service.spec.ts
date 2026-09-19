import { beforeEach, describe, expect, it, vi } from 'vitest';

import axios from 'axios';

import TreeService from './tree.service';

vi.mock('axios');

describe('TreeService.deleteNode', () => {
  const mockedDelete = vi.mocked(axios.delete);

  beforeEach(() => {
    mockedDelete.mockReset();
    mockedDelete.mockResolvedValue({ data: undefined } as any);
  });

  it('sends DELETE to api/tree/products/{id} for product', async () => {
    const svc = new TreeService();
    await svc.deleteNode('product', 42);
    expect(mockedDelete).toHaveBeenCalledWith('api/tree/products/42');
  });

  it('sends DELETE to api/tree/outcomes/{id} for outcome', async () => {
    const svc = new TreeService();
    await svc.deleteNode('outcome', 7);
    expect(mockedDelete).toHaveBeenCalledWith('api/tree/outcomes/7');
  });

  it('sends DELETE to api/tree/opportunities/{id} for opportunity', async () => {
    const svc = new TreeService();
    await svc.deleteNode('opportunity', 13);
    expect(mockedDelete).toHaveBeenCalledWith('api/tree/opportunities/13');
  });

  it('sends DELETE to api/tree/solutions/{id} for solution', async () => {
    const svc = new TreeService();
    await svc.deleteNode('solution', 99);
    expect(mockedDelete).toHaveBeenCalledWith('api/tree/solutions/99');
  });
});

describe('TreeService.moveNode', () => {
  const mockedPost = vi.mocked(axios.post);
  beforeEach(() => {
    mockedPost.mockReset();
    mockedPost.mockResolvedValue({
      data: {
        nodeType: 'OPPORTUNITY',
        nodeId: 5,
        parentType: 'OUTCOME',
        parentId: 7,
        outcomeId: 7,
        sortOrder: 0,
        oldSiblings: [{ nodeType: 'OPPORTUNITY', id: 6, sortOrder: 0 }],
        newSiblings: [{ nodeType: 'OPPORTUNITY', id: 5, sortOrder: 0 }],
        outcomeUpdates: [{ opportunityId: 5, outcomeId: 7 }],
      },
    } as any);
  });
  it('POSTs the move request to api/tree/nodes/move with uppercase enum wire values', async () => {
    const svc = new TreeService();
    const res = await svc.moveNode({ nodeType: 'opportunity', nodeId: 5, parentType: 'outcome', parentId: 7, position: 0 });
    expect(mockedPost).toHaveBeenCalledWith('api/tree/nodes/move', {
      nodeType: 'OPPORTUNITY',
      nodeId: 5,
      parentType: 'OUTCOME',
      parentId: 7,
      position: 0,
    });
    expect(res.nodeId).toBe(5);
    // Response comes back lowercase for the consumer.
    expect(res.nodeType).toBe('opportunity');
    expect(res.parentType).toBe('outcome');
    expect(res.newSiblings[0].nodeType).toBe('opportunity');
    expect(res.oldSiblings[0].nodeType).toBe('opportunity');
    expect(res.outcomeUpdates[0]).toEqual({ opportunityId: 5, outcomeId: 7 });
  });

  it('sends null parentType/parentId for a product moving on the top row', async () => {
    const svc = new TreeService();
    await svc.moveNode({ nodeType: 'product', nodeId: 42, parentType: null, parentId: null, position: 2 });
    expect(mockedPost).toHaveBeenCalledWith('api/tree/nodes/move', {
      nodeType: 'PRODUCT',
      nodeId: 42,
      parentType: null,
      parentId: null,
      position: 2,
    });
  });
});
