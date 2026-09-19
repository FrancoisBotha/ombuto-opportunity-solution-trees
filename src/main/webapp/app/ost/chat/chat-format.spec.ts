import { describe, expect, it } from 'vitest';

import { RUN_GAP_MS, formatStamp, groupThread, initialsOf, isNearBottom } from './chat-format';
import { comment } from './chat.test-util';

const NOW = new Date(2026, 8, 20, 15, 30); // Sun 20 Sep 2026, local time
const local = (y: number, m: number, d: number, hh: number, mm: number) => new Date(y, m, d, hh, mm).toISOString();

describe('chat-format', () => {
  describe('formatStamp', () => {
    it.each([
      [local(2026, 8, 20, 9, 5), 'Today 09:05'],
      [local(2026, 8, 19, 16, 18), 'Yesterday 16:18'],
      [local(2026, 8, 14, 9, 12), 'Mon 09:12'],
      [local(2026, 8, 13, 9, 12), '13 Sep 09:12'],
      [local(2025, 11, 31, 23, 59), '31 Dec 2025 23:59'],
    ])('%s → %s', (iso, expected) => {
      expect(formatStamp(iso, NOW)).toBe(expected);
    });

    it('is empty for a missing or broken date', () => {
      expect(formatStamp(null, NOW)).toBe('');
      expect(formatStamp('not a date', NOW)).toBe('');
    });
  });

  describe('groupThread', () => {
    const t = (hh: number, mm: number) => local(2026, 8, 20, hh, mm);

    it('starts a run on an author change or after a pause; stamps and initials sit on run starts', () => {
      const items = groupThread(
        [
          comment(1, 'admin', t(10, 0)),
          comment(2, 'admin', t(10, 2)), // same run
          comment(3, 'user', t(10, 3)), // author change: run, own → no initials
          comment(4, 'user', t(10, 4)), // same run
          comment(5, 'user', t(10, 30)), // pause > 5 min: new run
          comment(6, 'admin', t(10, 30)), // author change, same minute: run, stamp suppressed
        ],
        NOW,
      );
      expect(items.map(i => i.runStart)).toEqual([true, false, true, false, true, true]);
      expect(items.map(i => i.stamp)).toEqual(['Today 10:00', null, 'Today 10:03', null, 'Today 10:30', null]);
      expect(items.map(i => i.who)).toEqual(['AR', null, null, null, null, 'AR']);
    });

    it('a pause of exactly RUN_GAP_MS keeps the run', () => {
      const start = new Date(2026, 8, 20, 10, 0).getTime();
      const items = groupThread(
        [comment(1, 'admin', new Date(start).toISOString()), comment(2, 'admin', new Date(start + RUN_GAP_MS).toISOString())],
        NOW,
      );
      expect(items.map(i => i.runStart)).toEqual([true, false]);
    });

    it('is empty for no messages', () => {
      expect(groupThread([], NOW)).toEqual([]);
    });
  });

  it('initialsOf falls back to the author name or login', () => {
    expect(initialsOf(comment(1, 'admin', NOW.toISOString()))).toBe('AR');
    expect(initialsOf(comment(1, 'x', NOW.toISOString(), { authorInitials: null, authorName: 'jo smith' }))).toBe('JS');
    expect(initialsOf(comment(1, 'x', NOW.toISOString(), { authorInitials: null, authorName: null, authorLogin: 'zed' }))).toBe('Z');
  });

  it('isNearBottom allows the prototype slack of 24px', () => {
    expect(isNearBottom({ scrollHeight: 1000, clientHeight: 200, scrollTop: 800 })).toBe(true);
    expect(isNearBottom({ scrollHeight: 1000, clientHeight: 200, scrollTop: 777 })).toBe(true);
    expect(isNearBottom({ scrollHeight: 1000, clientHeight: 200, scrollTop: 776 })).toBe(false);
    expect(isNearBottom({ scrollHeight: 150, clientHeight: 200, scrollTop: 0 })).toBe(true);
  });
});
