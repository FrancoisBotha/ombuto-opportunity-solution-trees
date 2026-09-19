import { describe, expect, it } from 'vitest';

import { pinRendered } from './canvas-model';

describe('pinRendered (keep only the edited node rendered under visibility culling)', () => {
  const graph = (...ids: string[]) => new Map(ids.map(id => [id, { dragging: false }]));

  it('flags the nodes with a rename field or + menu open, and only those', () => {
    const nodes = graph('a', 'b', 'c');
    pinRendered(id => nodes.get(id), new Set(), new Set(['b']), null);
    expect([...nodes].filter(([, n]) => n.dragging).map(([id]) => id)).toEqual(['b']);
  });

  it('unflags a node once its edit ends, and moves the flag to the next edited node', () => {
    const nodes = graph('a', 'b');
    pinRendered(id => nodes.get(id), new Set(), new Set(['a']), null);
    pinRendered(id => nodes.get(id), new Set(['a']), new Set(['b']), null);
    expect(nodes.get('a')!.dragging).toBe(false);
    expect(nodes.get('b')!.dragging).toBe(true);
    pinRendered(id => nodes.get(id), new Set(['b']), new Set(), null);
    expect(nodes.get('b')!.dragging).toBe(false);
  });

  it('leaves the flag of the node really being dragged alone', () => {
    const nodes = graph('a');
    pinRendered(id => nodes.get(id), new Set(), new Set(['a']), null);
    pinRendered(id => nodes.get(id), new Set(['a']), new Set(), 'a');
    expect(nodes.get('a')!.dragging).toBe(true);
  });

  it('skips nodes Vue Flow does not know yet (a new node is pinned on the next sync)', () => {
    const nodes = graph('a');
    expect(() => pinRendered(id => nodes.get(id), new Set(['gone']), new Set(['new']), null)).not.toThrow();
    nodes.set('new', { dragging: false });
    pinRendered(id => nodes.get(id), new Set(['new']), new Set(['new']), null);
    expect(nodes.get('new')!.dragging).toBe(true);
  });
});
