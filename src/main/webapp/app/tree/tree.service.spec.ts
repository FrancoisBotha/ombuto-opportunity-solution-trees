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
