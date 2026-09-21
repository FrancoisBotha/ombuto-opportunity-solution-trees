/*
 * Pure helpers behind ChatThread: grouping messages into runs, the date-time stamp shown above a
 * run, and the "near the bottom" test that drives auto-scroll and the jump-to-latest button.
 *
 * Grouping follows the prototype (`Ombuto OST.dc.html` chatMsgs): the stamp is shown when it
 * differs from the previous message's stamp, the author's initials on the first message of an
 * author run (other people's messages only — one's own are right-aligned). With real timestamps a
 * run also breaks after a pause of more than RUN_GAP_MS.
 */
import type { CommentDTO } from '../ost.model';

/** A pause longer than this starts a new run even when the same person keeps writing. */
export const RUN_GAP_MS = 5 * 60 * 1000;

/** Distance from the bottom (px) that still counts as "at the latest message" (prototype: 24). */
export const BOTTOM_SLACK_PX = 24;

export interface ThreadItem {
  comment: CommentDTO;
  /**
   * True when the viewing user is the author. Derived here from {@code currentUserLogin}, never
   * carried on the DTO — the same broadcast payload reaches many viewers (CHAT-001).
   */
  mine: boolean;
  /** first message of a run (author change, or a pause > RUN_GAP_MS) */
  runStart: boolean;
  /** date-time stamp to show above this message, or null */
  stamp: string | null;
  /** author initials to show above this bubble, or null */
  who: string | null;
}

const pad = (n: number) => String(n).padStart(2, '0');
const DAYS = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

const startOfDay = (d: Date) => new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime();

/**
 * Prototype-style stamp in local time: "Today 09:41", "Yesterday 16:18", "Mon 09:12" within the
 * last week, else "12 Sep 09:12" (with the year when it is not this year).
 */
export function formatStamp(iso: string | null | undefined, now: Date = new Date()): string {
  const at = iso ? new Date(iso) : null;
  if (!at || Number.isNaN(at.getTime())) return '';
  const time = `${pad(at.getHours())}:${pad(at.getMinutes())}`;
  const days = Math.round((startOfDay(now) - startOfDay(at)) / 86_400_000);
  if (days === 0) return `Today ${time}`;
  if (days === 1) return `Yesterday ${time}`;
  if (days > 1 && days < 7) return `${DAYS[at.getDay()]} ${time}`;
  const year = at.getFullYear() === now.getFullYear() ? '' : ` ${at.getFullYear()}`;
  return `${at.getDate()} ${MONTHS[at.getMonth()]}${year} ${time}`;
}

const time = (c: CommentDTO) => {
  const t = Date.parse(c.createdDate);
  return Number.isNaN(t) ? 0 : t;
};

const authorOf = (c: CommentDTO) => c.authorLogin ?? `?${c.authorName ?? ''}`;

export const initialsOf = (c: CommentDTO) =>
  c.authorInitials ||
  (c.authorName ?? c.authorLogin ?? '?')
    .split(/\s+/)
    .filter(Boolean)
    .map(p => p[0])
    .join('')
    .slice(0, 2)
    .toUpperCase();

/**
 * Messages (oldest first) → display items with run starts, stamps and initials.
 *
 * Ownership is derived from {@code currentUserLogin} rather than a per-message flag: the store's
 * live-applied payload comes from a team topic and cannot carry per-viewer state (CHAT-001).
 * A {@code null} login (no team loaded yet) means nothing is mine.
 */
export function groupThread(comments: CommentDTO[], currentUserLogin: string | null, now: Date = new Date()): ThreadItem[] {
  let prevStamp: string | null = null;
  return comments.map((comment, i) => {
    const prev = comments[i - 1];
    const runStart = !prev || authorOf(prev) !== authorOf(comment) || time(comment) - time(prev) > RUN_GAP_MS;
    const label = formatStamp(comment.createdDate, now);
    const stamp = runStart && label !== prevStamp ? label : null;
    if (runStart) prevStamp = label;
    const mine = !!currentUserLogin && comment.authorLogin === currentUserLogin;
    const who = runStart && !mine ? initialsOf(comment) : null;
    return { comment, mine, runStart, stamp, who };
  });
}

/** True when a scroll box shows its last BOTTOM_SLACK_PX pixels. */
export function isNearBottom(el: Pick<HTMLElement, 'scrollHeight' | 'scrollTop' | 'clientHeight'>): boolean {
  return el.scrollHeight - el.scrollTop - el.clientHeight < BOTTOM_SLACK_PX;
}
