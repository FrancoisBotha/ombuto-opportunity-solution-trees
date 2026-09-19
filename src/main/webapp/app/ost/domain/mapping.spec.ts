import { describe, expect, it } from 'vitest';

import { dto, node } from './fixtures.test-util';
import { fromDto, nodeKey, parseKey, pickPatchFields, toApiType, toNodeType, toPatchBody } from './mapping';

describe('OST mapping', () => {
  it('maps a TreeNodeDTO onto the lower-case domain vocabulary, key as id, parentKey as parent', () => {
    const mapped = fromDto(
      dto('opportunity-12', 'outcome-3', {
        status: 'EXPLORING',
        notes: 'why',
        priority: 70,
        valueRating: 4,
        commentCount: 2,
        sortOrder: 5,
        links: [{ id: 1, name: 'Confluence', url: 'https://x.test/a' }],
        questions: [{ id: 2, text: 'Who?', done: false }],
      }),
    );
    expect(mapped).toMatchObject({
      id: 'opportunity-12',
      dbId: 12,
      type: 'opportunity',
      parent: 'outcome-3',
      note: 'why',
      status: 'exploring',
      priority: 70,
      value: 4,
      commentCount: 2,
      sortOrder: 5,
      links: [{ id: 1, name: 'Confluence', url: 'https://x.test/a' }],
      questions: [{ id: 2, text: 'Who?', done: false }],
    });
  });

  it('defaults null fields to empty domain values', () => {
    const mapped = fromDto(
      dto('product-1', null, {
        status: null,
        notes: null,
        ownerLogin: null,
        links: null,
        questions: null,
        commentCount: null,
        lastActivity: { at: '2026-09-02T00:00:00Z', byLogin: 'user' },
      }),
    );
    expect(mapped).toMatchObject({ parent: null, status: '', note: '', owner: '', links: [], questions: [], commentCount: 0 });
    expect(mapped.lastActivity).toEqual({ at: '2026-09-02T00:00:00Z', byLogin: 'user' });
  });

  it('builds a PATCH body with only the changed fields, upper-casing status and clearing empties', () => {
    expect(toPatchBody({ status: 'validated', value: 5 })).toEqual({ status: 'VALIDATED', valueRating: 5 });
    expect(toPatchBody({ note: '', owner: '' })).toEqual({ notes: null, ownerLogin: null });
    expect(toPatchBody({ title: 'T', conf: 55, priority: 20, archived: true, owner: 'user', note: 'n' })).toEqual({
      title: 'T',
      confidence: 55,
      priority: 20,
      archived: true,
      ownerLogin: 'user',
      notes: 'n',
    });
  });

  it('parses and builds node keys', () => {
    expect(parseKey('assumption-44')).toEqual({ type: 'assumption', id: 44 });
    expect(parseKey('widget-1')).toBeNull();
    expect(parseKey('opportunity')).toBeNull();
    expect(nodeKey('evidence', 3)).toBe('evidence-3');
  });

  it('converts types both ways', () => {
    expect(toApiType('solution')).toBe('SOLUTION');
    expect(toNodeType('EVIDENCE')).toBe('evidence');
    expect(() => toNodeType('BET')).toThrow();
  });

  it('snapshots the fields a patch touches', () => {
    const n = node('opportunity-1', 'outcome-1', { status: 'exploring', priority: 30 });
    expect(pickPatchFields(n, { status: 'parked', priority: 90 })).toEqual({ status: 'exploring', priority: 30 });
  });
});
