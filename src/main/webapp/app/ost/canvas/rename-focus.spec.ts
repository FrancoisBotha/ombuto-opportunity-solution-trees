import { afterEach, describe, expect, it, vi } from 'vitest';

import { restoreFocusAfterRename, typingElsewhere } from './rename-focus';

/**
 * A browser runs the blur listener's microtasks before focus reaches the clicked element. happy-dom
 * moves focus synchronously inside focus(), so the tests drain the microtask queue between the blur
 * and the new focus, as the browser does after each event listener.
 */
const microtasks = async () => {
  for (let i = 0; i < 20; i++) await Promise.resolve();
};
const task = () => new Promise(resolve => setTimeout(resolve, 0));

function field(tag: 'input' | 'textarea' | 'button' = 'input', parent: HTMLElement = document.body) {
  const el = document.createElement(tag);
  parent.appendChild(el);
  return el;
}

describe('rename focus hand-off', () => {
  afterEach(() => {
    document.body.innerHTML = '';
  });

  it('a rename ended by clicking another field leaves focus (and what is typed next) there', async () => {
    const rename = field();
    const search = field();
    const focusNode = vi.fn();
    rename.addEventListener('blur', () => void restoreFocusAfterRename(focusNode));
    rename.focus();

    rename.blur(); // mousedown on the search box: the rename field blurs first...
    await microtasks(); // ...its listener's microtasks run (a nextTick check would see "focus lost" here)...
    search.focus(); // ...then focus lands on the search box
    await task();

    expect(focusNode).not.toHaveBeenCalled();
    expect(document.activeElement).toBe(search);
  });

  it('Enter / Escape (the field goes away with focus in it) gives focus back to the node', async () => {
    const canvas = field('button');
    const rename = field();
    const focusNode = vi.fn(() => canvas.focus());
    rename.focus();
    rename.remove(); // the field unmounts
    await restoreFocusAfterRename(focusNode);
    expect(focusNode).toHaveBeenCalledOnce();
    expect(document.activeElement).toBe(canvas);
  });

  it('typingElsewhere: a text field outside the canvas, not the canvas itself or a button', () => {
    const canvas = document.createElement('div');
    document.body.appendChild(canvas);
    const inside = field('input', canvas);
    const notes = field('textarea');
    const button = field('button');
    inside.focus();
    expect(typingElsewhere(canvas)).toBe(false);
    notes.focus();
    expect(typingElsewhere(canvas)).toBe(true);
    button.focus();
    expect(typingElsewhere(canvas)).toBe(false);
  });
});
