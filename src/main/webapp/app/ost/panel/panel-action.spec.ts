import { describe, expect, it } from 'vitest';

import { usePanelAction } from './panel-action';
import { setupStores } from './panel.test-util';

describe('usePanelAction', () => {
  it('reports the message the store recorded for THIS action', async () => {
    const ctx = await setupStores();
    ctx.ui.select('opportunity-1');
    const { run, errors } = usePanelAction();

    const ok = await run(async () => {
      ctx.tree.error = 'The link could not be saved.';
      return false;
    });

    expect(ok).toBe(false);
    expect(errors.message.value).toBe('The link could not be saved.');
    expect(errors.nodeKey.value).toBe('opportunity-1');
    expect(ctx.tree.error).toBeNull(); // moved off the toast, into the panel
  });

  it('never attributes an error left over from something else to the action that just failed', async () => {
    // Several store actions return false without recording a reason (a link or question that is
    // already gone, a patch of a node that is not there). `run` used to read whatever was sitting
    // in the store's `error` and show THAT next to the field the user was editing — and clear it
    // off the toast, so the real message vanished too.
    const ctx = await setupStores();
    ctx.ui.select('opportunity-1');
    ctx.tree.error = 'The history could not be loaded.';
    const { run, errors } = usePanelAction();

    await run(async () => false, 'The link could not be saved.');

    expect(errors.message.value).toBe('The link could not be saved.');
    expect(ctx.tree.error).toBe('The history could not be loaded.'); // still on the toast, untouched
  });

  it('leaves a reasonless failure alone on the toast when the user has moved on', async () => {
    const ctx = await setupStores();
    ctx.ui.select('opportunity-1');
    ctx.tree.error = 'The history could not be loaded.';
    const { run, errors } = usePanelAction();

    await run(async () => {
      ctx.ui.select('solution-1');
      return false;
    });

    expect(errors.message.value).toBeNull();
    expect(ctx.tree.error).toBe('The history could not be loaded.');
  });

  it('labels a failure that lands after the selection moved with the node it was about', async () => {
    const ctx = await setupStores();
    ctx.ui.select('opportunity-1');
    const { run, errors } = usePanelAction();

    await run(async () => {
      ctx.ui.select('solution-1');
      ctx.tree.error = 'That status is not valid for this node.';
      return false;
    });

    expect(errors.message.value).toBeNull();
    expect(ctx.tree.error).toBe('“Hard to find people”: That status is not valid for this node.');
  });
});
