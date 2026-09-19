export type NodeType = 'product' | 'outcome' | 'opportunity' | 'solution' | 'assumption' | 'evidence';

export interface Comment { who: string; when: string; text: string; edited?: boolean }
export interface LinkRef { name: string; url: string }
export interface Question { text: string; done: boolean }
export interface HistoryEntry { who: string; when: string; what: string }

export interface OstNode {
  id: string;
  type: NodeType;
  parent: string | null;
  title: string;
  note: string;
  /** assumption + solution + opportunity only; see STATUS */
  status: string;
  /** assumptions only, 0-100 */
  conf: number;
  /** opportunities only, 1-100 continuous */
  priority: number;
  /** opportunities only, 1-5 */
  value: number;
  owner: string;
  comments: Comment[];
  links: LinkRef[];
  questions: Question[];
  history: HistoryEntry[];
}
