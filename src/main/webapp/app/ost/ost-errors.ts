/** Turns an axios error from the OST API into a short sentence for the UI. */
import { TITLE_MIN, titleMax } from './canvas/edit-rules';
import type { NodeType } from './domain/types';

const MESSAGES: Record<string, string> = {
  unknowntype: 'That node type is not recognised.',
  parentmissing: 'The parent node no longer exists.',
  nodemissing: 'That node no longer exists.',
  invalidparent: 'That node cannot go there.',
  unknownfield: 'That field cannot be changed.',
  fieldnotapplicable: 'That field does not apply to this node.',
  invalidtitle: 'Titles need 2 to 200 characters (100 for products, 500 for assumptions and evidence).',
  invalidnotes: 'Those notes are not valid.',
  invalidstatus: 'That status is not valid for this node.',
  invalidconfidence: 'Confidence must be between 0 and 100.',
  invalidpriority: 'Priority must be between 1 and 100.',
  invalidvaluerating: 'Value must be between 1 and 5.',
  invalidarchived: 'Archived must be true or false.',
  ownernotmember: 'The owner must be a member of this team.',
  invalidposition: 'That position is not valid.',
  cycle: 'A node cannot move under its own descendant.',
  crossteam: 'Nodes cannot move between teams.',
  linknameinvalid: 'Link names need 1 to 100 characters.',
  linkurlinvalid: 'Links must start with http:// or https://.',
  questiontextinvalid: 'Questions need 1 to 500 characters.',
  commentbodyinvalid: 'Messages need 1 to 10,000 characters.',
  chatnotsupported: 'Products have no chat.',
  historynotsupported: 'Products have no history.',
  // 409s (see ExceptionTranslator)
  dataintegrity: 'Something else still refers to this, so the change could not be made.',
  concurrencyFailure: 'Someone else changed this tree at the same moment — please try again.',
};

export type LoadFailure = 'forbidden' | 'notFound' | 'error';

export const httpStatus = (err: any): number | null => err?.response?.status ?? err?.status ?? null;

/** The server's message key without the `error.` prefix (e.g. `concurrencyFailure`), if any. */
export function messageKey(err: any): string | undefined {
  const raw: unknown = err?.response?.data?.message;
  return typeof raw === 'string' ? raw.replace(/^error\./, '') : undefined;
}

/** A write about a node another session has deleted (the store re-reads the tree to tell). */
export const DELETED_ELSEWHERE = 'This item was deleted by someone else.';

/**
 * `type` (the node the failed write was about) makes the title rule specific: products 100,
 * assumptions and evidence 500, the other types 200 characters.
 */
export function describeError(err: any, fallback = 'Something went wrong. Your change was not saved.', type?: NodeType): string {
  const status = httpStatus(err);
  const key = messageKey(err);
  if (key === 'invalidtitle' && type)
    return `${type === 'product' ? 'Product names' : 'Titles'} need ${TITLE_MIN} to ${titleMax(type)} characters.`;
  if (key && MESSAGES[key]) return MESSAGES[key];
  if (status === 403) return 'You do not have permission to change this tree.';
  if (status === 404) return 'That item no longer exists.';
  return fallback;
}

export function loadFailure(err: any): LoadFailure {
  const status = httpStatus(err);
  if (status === 403 || status === 401) return 'forbidden';
  if (status === 404 || status === 400) return 'notFound';
  return 'error';
}
