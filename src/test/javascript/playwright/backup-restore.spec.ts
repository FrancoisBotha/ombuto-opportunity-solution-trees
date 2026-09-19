import { promises as fs } from 'node:fs';
import { tmpdir } from 'node:os';
import path from 'node:path';

import { expect, test } from '@playwright/test';

import { USER_PASSWORD, USER_USERNAME, openSession } from './support/session';

/**
 * BDD UC 'Back Up and Restore Application Data' (docs/BDD Use Cases/bdd_BACKUP_RESTORE.md).
 *
 * The happy path is destructive: restore replaces every team, product and tree node in the
 * shared H2 database. To keep other specs safe, this file restores the backup it just took
 * (so the net effect is zero) and runs its two tests serially. Restore should still not be
 * interleaved with other data-mutating specs on the same backend; run this spec in isolation
 * (`npx playwright test src/test/javascript/playwright/backup-restore.spec.ts`) or with
 * `--workers=1` if driving the full suite.
 *
 * Scenario 1's "download" is captured from the network response rather than
 * page.waitForEvent('download'): the app uses a blob URL + anchor click and revokes the URL
 * synchronously, so Playwright's download event does not always fire. Reading the response
 * body still lets us verify status, Content-Disposition filename and the on-screen
 * confirmation, and gives us the bytes to hand back to the restore input for Scenario 2.
 */

test.describe.configure({ mode: 'serial' });

test.describe('BDD BACKUP_RESTORE', () => {
  test.setTimeout(120_000);

  test('admin downloads a backup, restores it and sees the summary (Scenarios 1 & 2)', async ({ page }, testInfo) => {
    await page.goto('/admin/backup');
    await expect(page.getByTestId('backupPageHeading')).toBeVisible();

    // Scenario 1: Take Backup fetches a dated file and confirms success.
    const [backupResponse] = await Promise.all([
      page.waitForResponse(response => response.url().endsWith('/api/admin/backup') && response.request().method() === 'GET'),
      page.getByTestId('takeBackupButton').click(),
    ]);
    expect(backupResponse.status(), 'backup export endpoint').toBe(200);
    const disposition = backupResponse.headers()['content-disposition'] ?? '';
    const filenameMatch = disposition.match(/filename="?(ombuto-ost-backup-\d{8}-\d{6}\.json)"?/);
    expect(filenameMatch, `Content-Disposition includes dated filename (was: ${disposition})`).not.toBeNull();
    await expect(page.getByTestId('backupSuccess')).toContainText(/backup completed/i);

    const backupBytes = await backupResponse.body();
    const backupPath = path.join(tmpdir(), `pw-${testInfo.testId}-${filenameMatch![1]}`);
    await fs.writeFile(backupPath, backupBytes);

    try {
      // Scenario 2 (cancel branch): warning is shown; cancel leaves nothing restored.
      await page.getByTestId('backupFileInput').setInputFiles(backupPath);
      await page.getByTestId('restoreButton').click();
      await expect(page.getByTestId('restoreConfirmation')).toContainText(/replaces all current data/i);
      await page.getByTestId('restoreCancelButton').click();
      await expect(page.getByTestId('restoreConfirmation')).toHaveCount(0);
      await expect(page.getByTestId('restoreSummary')).toHaveCount(0);

      // Scenario 2 (confirm branch): confirm runs the restore and the summary is shown.
      await page.getByTestId('restoreButton').click();
      await expect(page.getByTestId('restoreConfirmation')).toBeVisible();
      const [restoreResponse] = await Promise.all([
        page.waitForResponse(response => response.url().endsWith('/api/admin/backup/restore') && response.request().method() === 'POST'),
        page.getByTestId('restoreConfirmButton').click(),
      ]);
      expect(restoreResponse.status(), 'restore endpoint').toBe(200);

      const summary = page.getByTestId('restoreSummary');
      await expect(summary).toBeVisible();
      await expect(summary).toContainText(/teams/i);
      await expect(summary).toContainText(/products/i);
      await expect(summary).toContainText(/tree nodes/i);
    } finally {
      await fs.rm(backupPath, { force: true });
    }
  });

  test('a non-admin sees no Backup, is refused at /admin/backup and the API returns 403 (Scenario 5)', async ({ browser }) => {
    const session = await openSession(browser, USER_USERNAME, USER_PASSWORD);
    try {
      await session.page.goto('/');
      // The sidebar's Trees link is only rendered once the account has loaded — wait for it
      // so the follow-up "adminMenu absent" assertion is not racing the initial render.
      await expect(session.page.getByTestId('sidebar')).toBeVisible();
      await expect(session.page.getByTestId('treesMenu')).toBeVisible();

      // Neither the navbar dropdown ('Administration') nor the sidebar group ('System') exists
      // for a non-admin, and therefore no Backup entry is listed anywhere.
      await expect(session.page.getByTestId('adminMenu')).toHaveCount(0);
      await expect(session.page.getByTestId('backupMenu')).toHaveCount(0);

      // Direct navigation is refused by the router guard (main.ts): non-admins are sent to
      // /forbidden and the backup page heading never renders.
      await session.page.goto('/admin/backup');
      await expect(session.page).toHaveURL(/\/forbidden(\?|$|#)/);
      await expect(session.page.getByTestId('backupPageHeading')).toHaveCount(0);

      // API layer: both the export and the restore endpoints refuse the same user with 403.
      const exportResponse = await session.api('get', '/api/admin/backup');
      expect(exportResponse.status(), 'GET /api/admin/backup as non-admin').toBe(403);
      const restoreResponse = await session.api('post', '/api/admin/backup/restore');
      expect(restoreResponse.status(), 'POST /api/admin/backup/restore as non-admin').toBe(403);
    } finally {
      await session.context.close();
    }
  });
});
