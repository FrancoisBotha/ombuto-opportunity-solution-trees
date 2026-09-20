import { promises as fs } from 'node:fs';
import path from 'node:path';

import { type APIRequestContext, type Page, expect, test } from '@playwright/test';

import { USER_PASSWORD, USER_USERNAME, openSession } from './support/session';

/**
 * BDD UC 'Back Up and Restore Application Data' (docs/BDD Use Cases/bdd_BACKUP_RESTORE.md).
 *
 * The happy path is destructive: restore replaces every team, product and tree node in the shared
 * database. Two things keep that safe. The archive restored is the one this spec has just taken, so
 * a successful restore is a content no-op; and the spec has a Playwright project of its own
 * (`backup-restore` in playwright.config.ts) which depends on both browser projects, so Playwright
 * finishes every other spec before this one starts and nothing can be mid-write while the tables
 * are empty. Run it alone with `npx playwright test --project=backup-restore --no-deps`.
 *
 * The archive is also written to target/backup-safety/ and kept, so a failed restore can be undone
 * by hand from the last file in that folder.
 *
 * What the restore did is asserted against the database through the API afterwards, not against the
 * numbers the server echoed back from the uploaded file: the teams and the tree that were there
 * before the restore have to be there after it.
 *
 * Scenario 1's "download" is captured from the network response rather than
 * page.waitForEvent('download'): the app uses a blob URL + anchor click and revokes the URL
 * synchronously, so Playwright's download event does not always fire. Reading the response body
 * still lets us verify status, Content-Disposition filename and the on-screen confirmation, and
 * gives us the bytes to hand back to the restore input for Scenario 2.
 */

const SAFETY_DIR = path.join('target', 'backup-safety');

interface TeamSummary {
  id: number;
  name: string;
}

/** The teams this account can see, by name — read from the database through the API. */
async function readTeams(request: APIRequestContext): Promise<TeamSummary[]> {
  const response = await request.get('/api/team-management/my-teams');
  expect(response.status(), 'GET /api/team-management/my-teams').toBe(200);
  const teams = (await response.json()) as TeamSummary[];
  return teams.map(team => ({ id: team.id, name: team.name }));
}

/** Every node key in one team's tree, in order — the tree as the database holds it. */
async function readTreeNodeKeys(request: APIRequestContext, teamId: number): Promise<string[]> {
  const response = await request.get(`/api/teams/${teamId}/tree`);
  expect(response.status(), `GET /api/teams/${teamId}/tree`).toBe(200);
  const tree = (await response.json()) as { nodes: { key: string }[] };
  return tree.nodes.map(node => node.key);
}

async function openBackupPageFromTheMenu(page: Page): Promise<void> {
  await page.goto('/');
  await expect(page.getByTestId('sidebar')).toBeVisible();
  // Scenario 5's positive twin: an administrator really can find the page in the menu. The
  // System group starts collapsed, so it has to be opened first.
  const systemGroup = page.getByTestId('adminMenu');
  await expect(systemGroup).toBeVisible();
  const backupEntry = page.getByTestId('backupMenu');
  if (!(await backupEntry.isVisible())) {
    await systemGroup.click();
  }
  await expect(backupEntry, 'an admin sees Backup under System in the sidebar').toBeVisible();
  await backupEntry.click();
  await expect(page).toHaveURL(/\/admin\/backup$/);
  await expect(page.getByTestId('backupPageHeading')).toBeVisible();
}

test.describe.configure({ mode: 'serial' });

test.describe('BDD BACKUP_RESTORE', () => {
  test.setTimeout(180_000);

  test('admin downloads a backup, restores it and the data is still there (Scenarios 1 & 2)', async ({ page }, testInfo) => {
    const request = page.request;

    // What the database holds before anything destructive happens.
    const teamsBefore = await readTeams(request);
    const jupiter = teamsBefore.find(team => team.name === 'Team Jupiter');
    expect(jupiter, 'the seeded Team Jupiter is present before the restore').toBeTruthy();
    const jupiterKeysBefore = await readTreeNodeKeys(request, jupiter!.id);
    expect(jupiterKeysBefore.length, 'Team Jupiter has a seeded tree to lose').toBeGreaterThan(0);

    await openBackupPageFromTheMenu(page);

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

    // Kept, not deleted: this file is the way back if the restore below goes wrong.
    await fs.mkdir(SAFETY_DIR, { recursive: true });
    const backupPath = path.join(SAFETY_DIR, `${testInfo.testId}-${filenameMatch![1]}`);
    await fs.writeFile(backupPath, await backupResponse.body());

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

    // The part that matters: the database, not the numbers echoed from the file we uploaded.
    const teamsAfter = await readTeams(request);
    expect(teamsAfter.map(team => team.name).sort(), 'every team is back after the restore').toEqual(
      teamsBefore.map(team => team.name).sort(),
    );
    const jupiterAfter = teamsAfter.find(team => team.name === 'Team Jupiter');
    expect(jupiterAfter, 'Team Jupiter survived the restore').toBeTruthy();
    expect(jupiterAfter!.id, 'restored rows keep their ids').toBe(jupiter!.id);
    expect(await readTreeNodeKeys(request, jupiterAfter!.id), "Team Jupiter's tree came back node for node").toEqual(jupiterKeysBefore);

    // And the summary's counts agree with what the database now holds.
    const restored = await restoreResponse.json();
    expect(restored.counts.teams, 'summary team count matches the database').toBe(teamsAfter.length);
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
