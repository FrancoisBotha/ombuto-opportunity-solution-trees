import { defineStore } from 'pinia';
import { ALLOWED, STATUS, canReparent, defaultLinks } from '../domain/rules';
import { layoutTree } from '../domain/layout';
import type { NodeType, OstNode } from '../domain/types';
import { SEED } from '../data/seed';

const stamp = () =>
  'Today ' + new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

export const useTree = defineStore('tree', {
  state: () => ({
    nodes: SEED as OstNode[],
    me: 'KP',
    productId: 'p1' as string | 'all',
    selectedId: null as string | null,
    collapsed: {} as Record<string, boolean>,
    query: '',
    hiddenTypes: {} as Record<string, boolean>,
    panelTab: 'detail' as 'detail' | 'links' | 'chat' | 'questions' | 'history',
  }),
  getters: {
    byId: (s) => (id: string) => s.nodes.find(n => n.id === id),
    children: (s) => (id: string) => s.nodes.filter(n => n.parent === id),
    selected(s): OstNode | undefined { return s.nodes.find(n => n.id === s.selectedId); },
    roots: (s) => s.nodes.filter(n => n.type === 'product' && (s.productId === 'all' || n.id === s.productId)),
    placed(s): ReturnType<typeof layoutTree> {
      return layoutTree(s.nodes, { roots: this.roots.map(r => r.id), collapsed: s.collapsed });
    },
  },
  actions: {
    /** Single mutation entry point — writes the change log as a side effect. */
    patch(id: string, p: Partial<OstNode>) {
      const n = this.byId(id); if (!n) return;
      const ev: string[] = [];
      if (p.status && p.status !== n.status) ev.push(`Status changed to “${p.status}”`);
      if (p.conf !== undefined && p.conf !== n.conf) ev.push(`Confidence set to ${p.conf}%`);
      if (p.priority !== undefined && Math.round(p.priority / 10) !== Math.round(n.priority / 10)) ev.push('Priority changed');
      if (p.value !== undefined && p.value !== n.value) ev.push('Value set to ' + '$'.repeat(p.value));
      if (p.parent && p.parent !== n.parent) ev.push(`Moved under “${this.byId(p.parent)?.title ?? '—'}”`);
      if (p.comments && p.comments.length !== n.comments.length) ev.push(p.comments.length > n.comments.length ? 'Comment added' : 'Comment deleted');
      if (p.links && p.links.length !== n.links.length) ev.push(p.links.length > n.links.length ? 'Link added' : 'Link removed');
      if (p.questions && p.questions.length > n.questions.length) ev.push('Open question added');
      // NOTE: title/note edits are intentionally NOT logged (keystroke noise).
      Object.assign(n, p);
      n.history.push(...ev.map(what => ({ who: this.me, when: stamp(), what })));
    },
    addChild(parentId: string, type?: NodeType) {
      const parent = this.byId(parentId); if (!parent) return;
      const t = type ?? ALLOWED[parent.type][0]; if (!t) return;
      const id = 'n' + Math.random().toString(36).slice(2, 7);
      const node: OstNode = {
        id, type: t, parent: parentId,
        title: t === 'evidence' ? 'New snippet' : `New ${t}`,
        note: '', status: STATUS[t][0] ?? '', conf: 40, priority: 50, value: 3, owner: '',
        comments: [], links: defaultLinks({ id, type: t }), questions: [], history: [
          { who: this.me, when: stamp(), what: `Node created as ${t}` },
        ],
      };
      this.nodes.push(node);
      this.collapsed[parentId] = false;
      this.selectedId = id;
      return id; // caller puts it into rename mode
    },
    reparent(dragId: string, targetId: string) {
      if (!canReparent(dragId, targetId, this.nodes)) return false;
      this.patch(dragId, { parent: targetId });
      return true;
    },
    /** Cascading delete — always behind a confirmation naming the descendant count. */
    remove(id: string) {
      const doomed = new Set([id]);
      let grew = true;
      while (grew) {
        grew = false;
        for (const n of this.nodes) if (n.parent && doomed.has(n.parent) && !doomed.has(n.id)) { doomed.add(n.id); grew = true; }
      }
      this.nodes = this.nodes.filter(n => !doomed.has(n.id));
      if (this.selectedId && doomed.has(this.selectedId)) this.selectedId = null;
    },
    descendantCount(id: string) {
      return this.nodes.filter(n => {
        let c: OstNode | undefined = n;
        while (c?.parent) { if (c.parent === id) return true; c = this.byId(c.parent); }
        return false;
      }).length;
    },
    toggleCollapse(id: string) { this.collapsed[id] = !this.collapsed[id]; },
  },
});
