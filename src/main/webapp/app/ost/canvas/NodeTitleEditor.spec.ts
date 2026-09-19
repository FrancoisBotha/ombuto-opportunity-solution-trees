import { afterEach, describe, expect, it } from 'vitest';

import { type VueWrapper, flushPromises, mount } from '@vue/test-utils';

import type { NodeType } from '../domain/types';

import NodeTitleEditor from './NodeTitleEditor.vue';

describe('NodeTitleEditor (inline rename)', () => {
  let wrapper: VueWrapper | null = null;

  const open = async (value = 'Old title', type: NodeType = 'opportunity', error: string | null = null) => {
    wrapper = mount(NodeTitleEditor, { props: { type, label: type, value, error }, attachTo: document.body });
    await flushPromises();
    return wrapper;
  };
  const input = (w: VueWrapper) => w.get('[data-cy="ost-rename-input"]');

  afterEach(() => {
    wrapper?.unmount();
    wrapper = null;
  });

  it('starts with the title, focused and selected', async () => {
    const w = await open('Old title');
    const el = input(w).element as HTMLInputElement;
    expect(el.value).toBe('Old title');
    expect(document.activeElement).toBe(el);
    expect(w.find('[data-cy="ost-rename-error"]').exists()).toBe(false);
  });

  it('Enter commits the trimmed title', async () => {
    const w = await open();
    await input(w).setValue('  New title  ');
    await input(w).trigger('keydown', { key: 'Enter' });
    expect(w.emitted('commit')).toEqual([['New title']]);
    expect(w.emitted('cancel')).toBeUndefined();
  });

  it('Escape cancels', async () => {
    const w = await open();
    await input(w).setValue('Not kept');
    await input(w).trigger('keydown', { key: 'Escape' });
    expect(w.emitted('cancel')).toHaveLength(1);
    expect(w.emitted('commit')).toBeUndefined();
  });

  it('blur commits a valid title and cancels an invalid one', async () => {
    const valid = await open();
    await input(valid).setValue('Blurred title');
    await input(valid).trigger('blur');
    expect(valid.emitted('commit')).toEqual([['Blurred title']]);
    valid.unmount();

    const invalid = await open();
    await input(invalid).setValue(' ');
    await input(invalid).trigger('blur');
    expect(invalid.emitted('cancel')).toHaveLength(1);
    expect(invalid.emitted('commit')).toBeUndefined();
  });

  it('Enter on an invalid title shows the reason inline and keeps editing; typing clears it', async () => {
    const w = await open();
    await input(w).setValue('');
    await input(w).trigger('keydown', { key: 'Enter' });
    expect(w.emitted('commit')).toBeUndefined();
    expect(w.get('[data-cy="ost-rename-error"]').text()).toBe('A title is required.');
    expect(input(w).attributes('aria-invalid')).toBe('true');

    await input(w).setValue('x'.repeat(201));
    await input(w).trigger('keydown', { key: 'Enter' });
    expect(w.get('[data-cy="ost-rename-error"]').text()).toContain('at most 200 characters');

    await input(w).setValue('Fine now');
    expect(w.find('[data-cy="ost-rename-error"]').exists()).toBe(false);
    await input(w).trigger('keydown', { key: 'Enter' });
    expect(w.emitted('commit')).toEqual([['Fine now']]);
  });

  it('assumptions and evidence allow up to 500 characters', async () => {
    const w = await open('Old', 'evidence');
    await input(w).setValue('y'.repeat(500));
    await input(w).trigger('keydown', { key: 'Enter' });
    expect(w.emitted('commit')).toEqual([['y'.repeat(500)]]);
  });

  it('shows a server error it was reopened with', async () => {
    const w = await open('Rejected title', 'solution', 'Titles need 2 to 200 characters (500 for assumptions and evidence).');
    expect(w.get('[data-cy="ost-rename-error"]').text()).toContain('Titles need 2 to 200');
    expect((input(w).element as HTMLInputElement).value).toBe('Rejected title');
  });

  it('emits once: a blur after Enter or Escape is ignored', async () => {
    const w = await open();
    await input(w).trigger('keydown', { key: 'Escape' });
    await input(w).trigger('blur');
    await input(w).trigger('keydown', { key: 'Enter' });
    expect(w.emitted('cancel')).toHaveLength(1);
    expect(w.emitted('commit')).toBeUndefined();
  });

  it('keeps keys away from the canvas (Delete, Escape never bubble)', async () => {
    const w = await open();
    const seen: string[] = [];
    const listener = (e: KeyboardEvent) => seen.push(e.key);
    document.addEventListener('keydown', listener);
    await input(w).trigger('keydown', { key: 'Delete' });
    await input(w).trigger('keydown', { key: 'Escape' });
    document.removeEventListener('keydown', listener);
    expect(seen).toEqual([]);
  });
});
