import { describe, expect, it } from 'vitest';

import { helpTopics, type HelpTopic } from './help-topics';

describe('help topics content module', () => {
  it('exports a non-empty ordered list of topics', () => {
    expect(Array.isArray(helpTopics)).toBe(true);
    expect(helpTopics.length).toBeGreaterThan(0);
  });

  it('gives every topic a stable, unique id', () => {
    const ids = helpTopics.map(t => t.id);
    expect(new Set(ids).size).toBe(ids.length);
    for (const id of ids) {
      expect(id).toMatch(/^[a-z0-9-]+$/);
    }
  });

  it('gives every topic a title, summary and at least one step', () => {
    for (const topic of helpTopics) {
      expect(topic.title.trim().length).toBeGreaterThan(0);
      expect(topic.summary.trim().length).toBeGreaterThan(0);
      expect(topic.steps.length).toBeGreaterThan(0);
      for (const step of topic.steps) {
        expect(step.trim().length).toBeGreaterThan(0);
      }
    }
  });

  it('marks every tree-changing topic with a role note that mentions editor and owner', () => {
    const treeChanging = helpTopics.filter(t => t.changesTree);
    expect(treeChanging.length).toBeGreaterThan(0);
    for (const topic of treeChanging) {
      expect(topic.roleNote).toBeTruthy();
      const note = (topic.roleNote ?? '').toLowerCase();
      expect(note).toContain('editor');
      expect(note).toContain('owner');
    }
  });

  it('leaves read-only topics without a role note', () => {
    const readOnly = helpTopics.filter(t => !t.changesTree);
    expect(readOnly.length).toBeGreaterThan(0);
    for (const topic of readOnly) {
      expect(topic.roleNote ?? '').toBe('');
    }
  });

  it("has a topic titled exactly 'Logging interviews'", () => {
    const match = helpTopics.find(t => t.title === 'Logging interviews');
    expect(match).toBeDefined();
  });

  it("mentions node 'status' in at least one topic but not in every topic", () => {
    const mentions = (topic: HelpTopic): boolean => {
      const haystack = [topic.title, topic.summary, ...topic.steps, topic.roleNote ?? ''].join(' ').toLowerCase();
      return haystack.includes('status');
    };
    const matching = helpTopics.filter(mentions);
    expect(matching.length).toBeGreaterThan(0);
    expect(matching.length).toBeLessThan(helpTopics.length);
  });

  it('covers the required subject areas', () => {
    const haystack = helpTopics
      .map(t => [t.title, t.summary, ...t.steps].join(' '))
      .join(' ')
      .toLowerCase();
    for (const term of [
      'product',
      'outcome',
      'opportunity',
      'solution',
      'assumption',
      'experiment',
      'team',
      'role',
      'comment',
      'interview',
      'jira',
      'confluence',
      'sign in',
      'connect an agent',
    ]) {
      expect(haystack).toContain(term);
    }
  });

  it('excludes administrator-only areas', () => {
    const haystack = helpTopics
      .map(t => [t.title, t.summary, ...t.steps, t.roleNote ?? ''].join(' '))
      .join(' ')
      .toLowerCase();
    for (const forbidden of ['static data', 'backup', 'entity admin']) {
      expect(haystack).not.toContain(forbidden);
    }
  });
});
