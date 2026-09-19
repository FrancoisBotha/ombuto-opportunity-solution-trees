/**
 * Focus hand-off when a node's inline rename field closes (TreeCanvas).
 *
 * The field closes on Enter / Escape (focus is still in it, and it unmounts) or on blur — a click or
 * Tab to something else. In a browser the blur's listeners and their microtasks (Vue's nextTick)
 * run BEFORE focus reaches the clicked element, so a check made then always sees "focus lost" and
 * would pull focus back to the node, eating what the user types next. The check therefore waits for
 * a task: by then focus has landed wherever the user sent it.
 */

/** Resolves after the browser has finished moving focus. */
export const focusSettled = (): Promise<void> => new Promise(resolve => setTimeout(resolve, 0));

/** Nothing (or a detached element) has focus. */
export const focusIsLost = (): boolean => {
  const active = document.activeElement;
  return !active || active === document.body || !active.isConnected;
};

/** A text field outside `container` has focus: the user is typing somewhere else. */
export function typingElsewhere(container: Element | null): boolean {
  const el = document.activeElement as HTMLElement | null;
  if (!el || el === document.body || container?.contains(el)) return false;
  return el.isContentEditable || ['INPUT', 'TEXTAREA', 'SELECT'].includes(el.tagName);
}

/**
 * After a rename closed: once focus has settled, give it back to the node — but only if nothing else
 * took it (Enter / Escape, or a click on an empty spot). Focus the user moved elsewhere stays there.
 */
export async function restoreFocusAfterRename(focusNode: () => void): Promise<void> {
  await focusSettled();
  if (focusIsLost()) focusNode();
}
