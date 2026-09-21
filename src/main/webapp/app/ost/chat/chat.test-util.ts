/** Test-only: comment DTO builder for the chat specs. */
import type { CommentDTO } from '../ost.model';

const PEOPLE: Record<string, { initials: string; name: string }> = {
  user: { initials: 'KP', name: 'Kira P' },
  admin: { initials: 'AR', name: 'Ana R' },
};

/** Local time today (or `daysAgo` days back) at hh:mm, as an ISO string. */
export const at = (hh: number, mm: number, daysAgo = 0) => {
  const d = new Date();
  d.setDate(d.getDate() - daysAgo);
  d.setHours(hh, mm, 0, 0);
  return d.toISOString();
};

export function comment(id: number, login: string, createdDate: string, extra: Partial<CommentDTO> = {}): CommentDTO {
  const who = PEOPLE[login] ?? { initials: login.slice(0, 2).toUpperCase(), name: login };
  return {
    id,
    body: `Message ${id}`,
    authorLogin: login,
    authorInitials: who.initials,
    authorName: who.name,
    createdDate,
    editedDate: null,
    ...extra,
  };
}
