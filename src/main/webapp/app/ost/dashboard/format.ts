/** Display helpers shared by the Trees dashboard and the Experiments tracker. Pure, unit-tested. */
import type { TeamMemberDTO } from '../ost.model';

/** "Kira P." — first name plus last initial, as the prototype writes people; falls back to the login. */
export function memberShortName(member: Pick<TeamMemberDTO, 'login' | 'firstName' | 'lastName'>): string {
  const first = member.firstName?.trim();
  const last = member.lastName?.trim();
  if (first && last) return `${first} ${last.charAt(0).toUpperCase()}.`;
  return first || last || member.login;
}

/** Short name of the member with that login; the bare login when they are no longer a member; '' when none. */
export function personLabel(login: string | null | undefined, members: TeamMemberDTO[]): string {
  if (!login) return '';
  const member = members.find(m => m.login === login);
  return member ? memberShortName(member) : login;
}

const MINUTE = 60_000;
const HOUR = 60 * MINUTE;
const DAY = 24 * HOUR;

const startOfDay = (d: Date) => new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime();

/** "just now" · "5m ago" · "2h ago" · "yesterday" · "3d ago" · "on 12 Sep 2026". */
export function relativeTime(iso: string | null | undefined, now = new Date()): string {
  if (!iso) return '';
  const at = new Date(iso);
  if (Number.isNaN(at.getTime())) return '';
  const diff = now.getTime() - at.getTime();
  if (diff < MINUTE) return 'just now';
  if (diff < HOUR) return `${Math.floor(diff / MINUTE)}m ago`;
  const days = Math.round((startOfDay(now) - startOfDay(at)) / DAY);
  if (days === 0) return `${Math.floor(diff / HOUR)}h ago`;
  if (days === 1) return 'yesterday';
  if (days < 7) return `${days}d ago`;
  return `on ${at.toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' })}`;
}

/** The card footer: "Last edited 2h ago by Kira P." (author omitted when unknown). */
export function lastEditedLabel(
  at: string | null | undefined,
  byLogin: string | null | undefined,
  members: TeamMemberDTO[],
  now = new Date(),
) {
  const when = relativeTime(at, now);
  if (!when) return 'No edits yet';
  const who = personLabel(byLogin, members);
  return who ? `Last edited ${when} by ${who}` : `Last edited ${when}`;
}
