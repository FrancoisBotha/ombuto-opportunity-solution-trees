import { defaultLinks } from '../domain/rules';
import type { NodeType, OstNode } from '../domain/types';

const n = (id: string, type: NodeType, parent: string | null, title: string, o: Partial<OstNode> = {}): OstNode => ({
  id, type, parent, title, note: '', status: '', conf: 40, priority: 52, value: 3, owner: '',
  comments: [], links: defaultLinks({ id, type }), questions: [],
  history: [{ who: 'KP', when: 'Mon 08:10', what: `Node created as ${type}` }], ...o,
});

/** Trimmed demo tree — replace with the API. Mirrors the prototype's content. */
export const SEED: OstNode[] = [
  n('p1', 'product', null, 'Discovery Canvas'),
  n('o1', 'outcome', 'p1', 'Teams running ≥1 interview a week: 34% → 60%'),
  n('op1', 'opportunity', 'o1', 'I can never find time to schedule interviews', {
    status: 'exploring', priority: 92, value: 5,
    questions: [{ text: 'Does this hold for teams outside the trio model?', done: false }],
    comments: [{ who: 'AR', when: 'Mon 09:12', text: 'Heard this in 7 of 9 interviews.' }],
  }),
  n('op1a', 'opportunity', 'op1', 'Recruiting a participant takes me a week', { status: 'validated', priority: 72, value: 4 }),
  n('s1', 'solution', 'op1a', 'Auto-recruit from an in-product prompt', { status: 'building' }),
  n('a1', 'assumption', 's1', 'Users will accept an in-app interview invite', { status: 'testing', conf: 40, owner: 'Kira P.' }),
  n('a2', 'assumption', 's1', 'Sales won’t block outreach to enterprise accounts', { status: 'untested', conf: 20, owner: 'Jon S.' }),
  n('e1', 'evidence', 'op1', '“I book interviews at 11pm — that’s when I have a gap.”'),
];
