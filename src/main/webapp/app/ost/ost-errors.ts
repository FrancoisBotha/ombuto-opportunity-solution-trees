/** Turns an axios error from the OST API into a short sentence for the UI. */

const MESSAGES: Record<string, string> = {
  unknowntype: 'That node type is not recognised.',
  parentmissing: 'The parent node no longer exists.',
  nodemissing: 'That node no longer exists.',
  invalidparent: 'That node cannot go there.',
  unknownfield: 'That field cannot be changed.',
  fieldnotapplicable: 'That field does not apply to this node.',
  invalidtitle: 'Titles need 2 to 200 characters (500 for assumptions and evidence).',
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
  nodetypeinvalid: 'That node type is not recognised.',
  linknameinvalid: 'Link names need 1 to 100 characters.',
  linkurlinvalid: 'Links must start with http:// or https://.',
  questiontextinvalid: 'Questions need 1 to 500 characters.',
  commentbodyinvalid: 'Messages cannot be empty.',
  chatnotsupported: 'Products have no chat.',
  historynotsupported: 'Products have no history.',
};

export type LoadFailure = 'forbidden' | 'notFound' | 'error';

export const httpStatus = (err: any): number | null => err?.response?.status ?? err?.status ?? null;

export function describeError(err: any, fallback = 'Something went wrong. Your change was not saved.'): string {
  const status = httpStatus(err);
  const raw: string | undefined = err?.response?.data?.message;
  const key = typeof raw === 'string' ? raw.replace(/^error\./, '') : undefined;
  if (key && MESSAGES[key]) return MESSAGES[key];
  if (status === 403) return 'You do not have permission to change this tree.';
  if (status === 404) return 'That item no longer exists.';
  if (status === 0 || status == null) return fallback;
  return fallback;
}

export function loadFailure(err: any): LoadFailure {
  const status = httpStatus(err);
  if (status === 403 || status === 401) return 'forbidden';
  if (status === 404 || status === 400) return 'notFound';
  return 'error';
}
