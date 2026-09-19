/*
 * Small presentation helpers for the detail panel (copy and thresholds from the prototype,
 * `Ombuto OST.dc.html`: confLabel, rollupNote, quickLink, the link dot colour).
 */
import { defaultLinks } from '../domain/rules';
import type { LinkRef, OstNode } from '../domain/types';
import type { TeamMemberDTO } from '../ost.model';

/** The five confidence steps of the assumption control. */
export const CONFIDENCE_STEPS = [20, 40, 60, 80, 100];

export const confidenceLabel = (c: number) => (c >= 80 ? 'High confidence' : c >= 60 ? 'Med-high' : c >= 40 ? 'Medium' : 'Low');

/** Link URLs the server accepts (NodeLink.url pattern). */
export const LINK_URL = /^https?:\/\/.+/i;

export const isValidLinkUrl = (url: string) => LINK_URL.test(url.trim());

/** data-cy / DOM-safe slug of a link name ("Jira Epic" -> "jira-epic"). */
export const slug = (name: string) =>
  name
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-|-$/g, '');

/** Short button label for a default link ("Jira Initiative" -> "Initiative"). */
export const restoreLabel = (name: string) => name.replace(/^Jira\s+/i, '');

/** Confluence / wiki links get the accent dot, everything else neutral. */
export const isWikiLink = (url: string) => /confluence|wiki/i.test(url);

export interface RestoreOption {
  name: string;
  url: string;
  label: string;
  present: boolean;
}

/** The node type's default links, each flagged when a link of that name (any case) is present. */
export function restoreOptions(node: Pick<OstNode, 'id' | 'type'> & { links: LinkRef[] }): RestoreOption[] {
  const have = new Set(node.links.map(l => l.name.trim().toLowerCase()));
  return defaultLinks(node).map(l => ({ ...l, label: restoreLabel(l.name), present: have.has(l.name.toLowerCase()) }));
}

/** "No assumption tests …" / "3 assumption tests · 1 supported · 1 refuted. …" */
export function evidenceNote(e: { tests: number; supported: number; refuted: number }) {
  if (e.tests === 0) {
    return 'No assumption tests under this solution yet — add the assumptions you would have to believe for it to work.';
  }
  return `${e.tests} assumption test${e.tests === 1 ? '' : 's'} · ${e.supported} supported · ${e.refuted} refuted. Rolled up from the tests below, not set by hand.`;
}

export const memberName = (m: Pick<TeamMemberDTO, 'login' | 'firstName' | 'lastName'>) =>
  [m.firstName, m.lastName].filter(Boolean).join(' ').trim() || m.login;
