import { type Browser, type BrowserContext, type Page, expect, test } from '@playwright/test';

import { STORAGE_STATE } from '../../../../playwright.config';

/**
 * Pattern for real-time collaboration tests: two independent browser sessions
 * share the same signed-in user and look at the same page. Once the WebSocket
 * channel exists, extend this to assert that a change made in `alice` appears
 * in `bob` without a reload.
 */
async function openSession(browser: Browser): Promise<{ context: BrowserContext; page: Page }> {
  const context = await browser.newContext({ storageState: STORAGE_STATE });
  const page = await context.newPage();
  return { context, page };
}

test.describe('two concurrent sessions', () => {
  test('both sessions see the same opportunity list', async ({ browser }) => {
    const alice = await openSession(browser);
    const bob = await openSession(browser);

    try {
      await Promise.all([alice.page.goto('/opportunity'), bob.page.goto('/opportunity')]);
      await expect(alice.page.getByTestId('OpportunityHeading')).toBeVisible();
      await expect(bob.page.getByTestId('OpportunityHeading')).toBeVisible();

      const aliceRows = await alice.page.getByTestId('entityTable').locator('tbody tr').count();
      const bobRows = await bob.page.getByTestId('entityTable').locator('tbody tr').count();
      expect(aliceRows).toBe(bobRows);
    } finally {
      await Promise.all([alice.context.close(), bob.context.close()]);
    }
  });
});
