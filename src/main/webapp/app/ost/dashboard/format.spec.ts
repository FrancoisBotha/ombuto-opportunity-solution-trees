import { describe, expect, it } from 'vitest';

import { lastEditedLabel, memberShortName, personLabel, relativeTime } from './format';

const members = [
  { login: 'user', firstName: 'Kira', lastName: 'Pillay', initials: 'KP', role: 'OWNER' as const },
  { login: 'admin', firstName: null, lastName: null, initials: 'AD', role: 'VIEWER' as const },
];

describe('dashboard format helpers', () => {
  it('writes people as first name + last initial, falling back to the login', () => {
    expect(memberShortName(members[0])).toBe('Kira P.');
    expect(memberShortName(members[1])).toBe('admin');
    expect(personLabel('user', members)).toBe('Kira P.');
    expect(personLabel('gone', members)).toBe('gone');
    expect(personLabel('', members)).toBe('');
  });

  it('formats relative times', () => {
    const now = new Date(2026, 8, 19, 15, 0, 0);
    const ago = (ms: number) => new Date(now.getTime() - ms).toISOString();
    expect(relativeTime(ago(10_000), now)).toBe('just now');
    expect(relativeTime(ago(5 * 60_000), now)).toBe('5m ago');
    expect(relativeTime(ago(2 * 3_600_000), now)).toBe('2h ago');
    expect(relativeTime(new Date(2026, 8, 18, 20, 0).toISOString(), now)).toBe('yesterday');
    expect(relativeTime(new Date(2026, 8, 15, 9, 0).toISOString(), now)).toBe('4d ago');
    expect(relativeTime(new Date(2026, 7, 2, 9, 0).toISOString(), now)).toBe('on 2 Aug 2026');
    expect(relativeTime(null, now)).toBe('');
    expect(relativeTime('nonsense', now)).toBe('');
  });

  it('builds the card footer', () => {
    const now = new Date(2026, 8, 19, 15, 0, 0);
    const at = new Date(2026, 8, 19, 13, 0, 0).toISOString();
    expect(lastEditedLabel(at, 'user', members, now)).toBe('Last edited 2h ago by Kira P.');
    expect(lastEditedLabel(at, null, members, now)).toBe('Last edited 2h ago');
    expect(lastEditedLabel(null, 'user', members, now)).toBe('No edits yet');
  });
});
