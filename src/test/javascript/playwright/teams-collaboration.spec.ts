import { type Browser, type BrowserContext, type Page, expect, test } from '@playwright/test';

import { loginViaKeycloak } from './support/login';

/**
 * TEAMS-008 two-user end-to-end test.
 *
 * Covers:
 *  - Local Keycloak dev accounts (admin/admin, user/user) still work end-to-end,
 *    which also exercises FR-008's local-account path.
 *  - Owner adds a viewer + a product; viewer sees the team on 'My teams' with a
 *    VIEWER badge, sees the product, and sees no edit controls.
 *  - A user who is not a member does not see the team on 'My teams', gets the
 *    forbidden notice when navigating to /teams/<id>, and receives HTTP 403 on
 *    the direct team-management API call.
 *
 * Both users sign in through the real Keycloak instance shipped in
 * src/main/docker/keycloak.yml, using their own fresh browser context so their
 * sessions do not pollute one another. The playwright.config.ts default
 * storageState (belonging to E2E_USERNAME/E2E_PASSWORD, i.e. admin) is not used
 * here; we override storageState on every context we create.
 */

const ADMIN_USERNAME = process.env.E2E_ADMIN_USERNAME ?? 'admin';
const ADMIN_PASSWORD = process.env.E2E_ADMIN_PASSWORD ?? 'admin';
const VIEWER_USERNAME = process.env.E2E_USER_USERNAME ?? 'user';
const VIEWER_PASSWORD = process.env.E2E_USER_PASSWORD ?? 'user';

interface Session {
  context: BrowserContext;
  page: Page;
}

async function openFreshSession(browser: Browser, username: string, password: string): Promise<Session> {
  const context = await browser.newContext({ storageState: { cookies: [], origins: [] } });
  const page = await context.newPage();
  await loginViaKeycloak(page, username, password);
  return { context, page };
}

async function createTeam(page: Page, name: string, description: string): Promise<number> {
  await page.goto('/teams');
  await expect(page.getByTestId('MyTeamsHeading')).toBeVisible();
  await page.getByTestId('newTeamButton').click();
  await page.getByTestId('newTeamName').fill(name);
  await page.getByTestId('newTeamDescription').fill(description);
  await page.getByTestId('submitNewTeam').click();

  const card = page.locator('[data-cy^="teamCard-"]').filter({ hasText: name }).first();
  await expect(card).toBeVisible();
  const dataCy = await card.getAttribute('data-cy');
  const teamId = Number((dataCy ?? '').replace('teamCard-', ''));
  expect(Number.isFinite(teamId) && teamId > 0).toBe(true);
  return teamId;
}

async function openTeam(page: Page, teamId: number, expectedName: string): Promise<void> {
  await page.goto(`/teams/${teamId}`);
  await expect(page.getByTestId('teamName')).toHaveText(expectedName);
}

test.describe('TEAMS-008 — two-user team collaboration', () => {
  // Every scenario opens a real Keycloak login flow; give them room to breathe.
  test.setTimeout(120_000);

  test('owner adds viewer + product; viewer sees team, product and no edit controls', async ({ browser }) => {
    const teamName = `Discovery ${Date.now()}`;
    const productName = `Widget ${Date.now()}`;

    // 1. First-login sync for the viewer. JHipster only inserts a jhi_user row
    //    after the user has signed in once (see FR-008 / epic §7), so an owner
    //    cannot add someone who has never logged in. Signing in and immediately
    //    closing the context is the cheapest way to make sure the row exists.
    const viewerPrelogin = await openFreshSession(browser, VIEWER_USERNAME, VIEWER_PASSWORD);
    await viewerPrelogin.context.close();

    // 2. Owner (admin) creates the team, adds the viewer, and adds a product.
    const owner = await openFreshSession(browser, ADMIN_USERNAME, ADMIN_PASSWORD);
    let teamId: number;
    try {
      teamId = await createTeam(owner.page, teamName, 'Two-user e2e team');
      await openTeam(owner.page, teamId, teamName);

      const membersBefore = await owner.page.locator('[data-cy^="memberRow-"]').count();

      await owner.page.getByTestId('addMemberButton').click();
      await owner.page.getByTestId('memberSearchInput').fill(VIEWER_USERNAME);
      await owner.page.getByTestId('memberSearchButton').click();
      await owner.page.getByTestId(`memberSearchResult-${VIEWER_USERNAME}`).click();
      await owner.page.getByTestId('addMemberRole').selectOption('VIEWER');
      await owner.page.getByTestId('submitAddMember').click();

      await expect(owner.page.locator('[data-cy^="memberRow-"]')).toHaveCount(membersBefore + 1);

      await owner.page.getByTestId('newProductButton').click();
      await owner.page.getByTestId('newProductName').fill(productName);
      await owner.page.getByTestId('newProductDescription').fill('e2e product');
      await owner.page.getByTestId('submitNewProduct').click();
      await expect(owner.page.locator('[data-cy^="productRow-"]').filter({ hasText: productName })).toBeVisible();
    } finally {
      await owner.context.close();
    }

    // 3. Viewer signs in fresh, sees the team on 'My teams' with a VIEWER badge,
    //    sees the product, and sees no edit controls anywhere on the page.
    const viewer = await openFreshSession(browser, VIEWER_USERNAME, VIEWER_PASSWORD);
    try {
      await viewer.page.goto('/teams');
      await expect(viewer.page.getByTestId('MyTeamsHeading')).toBeVisible();

      const viewerCard = viewer.page.locator(`[data-cy="teamCard-${teamId}"]`);
      await expect(viewerCard).toBeVisible();
      await expect(viewerCard.locator(`[data-cy="teamCardRole-${teamId}"]`)).toHaveText(/VIEWER/i);

      await openTeam(viewer.page, teamId, teamName);
      await expect(viewer.page.getByTestId('teamRole')).toHaveText(/VIEWER/i);
      await expect(viewer.page.getByTestId('readOnlyNotice')).toBeVisible();
      await expect(viewer.page.locator('[data-cy^="productRow-"]').filter({ hasText: productName })).toBeVisible();

      // Viewer-hidden controls: no team edit button, no add-member button, no
      // create/edit/archive product buttons.
      await expect(viewer.page.getByTestId('editTeamButton')).toHaveCount(0);
      await expect(viewer.page.getByTestId('addMemberButton')).toHaveCount(0);
      await expect(viewer.page.getByTestId('newProductButton')).toHaveCount(0);
      await expect(viewer.page.locator('[data-cy^="editProductButton-"]')).toHaveCount(0);
      await expect(viewer.page.locator('[data-cy^="archiveProductButton-"]')).toHaveCount(0);
      await expect(viewer.page.locator('[data-cy^="removeMemberButton-"]')).toHaveCount(0);
    } finally {
      await viewer.context.close();
    }
  });

  test('non-member does not see the team, and the UI + API both refuse access', async ({ browser }) => {
    const teamName = `Private ${Date.now()}`;

    // Owner creates a team but does not add the viewer.
    const owner = await openFreshSession(browser, ADMIN_USERNAME, ADMIN_PASSWORD);
    let teamId: number;
    try {
      teamId = await createTeam(owner.page, teamName, 'Only the owner should see this');
    } finally {
      await owner.context.close();
    }

    const outsider = await openFreshSession(browser, VIEWER_USERNAME, VIEWER_PASSWORD);
    try {
      await outsider.page.goto('/teams');
      await expect(outsider.page.getByTestId('MyTeamsHeading')).toBeVisible();
      await expect(outsider.page.locator(`[data-cy="teamCard-${teamId}"]`)).toHaveCount(0);

      // UI direct-URL access shows the forbidden / not-found notice.
      await outsider.page.goto(`/teams/${teamId}`);
      const denied = outsider.page.getByTestId('teamForbidden').or(outsider.page.getByTestId('teamNotFound'));
      await expect(denied).toBeVisible();

      // Direct API call returns 403 (or 404 — both are acceptable per NFR-002).
      const apiResp = await outsider.page.request.get(`/api/team-management/teams/${teamId}`);
      expect([403, 404]).toContain(apiResp.status());

      // Non-member also cannot list the private team's products.
      const productsResp = await outsider.page.request.get(`/api/teams/${teamId}/products`);
      expect([403, 404]).toContain(productsResp.status());
    } finally {
      await outsider.context.close();
    }
  });
});
