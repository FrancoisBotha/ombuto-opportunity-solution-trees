import { type Page, expect, test } from '@playwright/test';

import { ADMIN_PASSWORD, ADMIN_USERNAME, type Session, USER_PASSWORD, USER_USERNAME, openSession } from './support/session';

/**
 * OST step 6: the tree-builder shell — /trees landing, team switcher, view tabs, access states and
 * style isolation (the OST dark theme must not leak into the rest of the app).
 *
 * Reads only the seeded team NAMES (Team Jupiter / Team Venus for `user`, Team Mars admin-only),
 * never tree content, and changes nothing.
 */

interface MyTeam {
  id: number;
  name: string;
}

const JUPITER = 'Team Jupiter';
const VENUS = 'Team Venus';

/** Computed styles that must be identical on /teams whether or not OST was visited first. */
async function captureAppStyles(page: Page) {
  await expect(page.getByTestId('myTeamsPage')).toBeVisible();
  // Keep the pointer off the sidebar and let its width transition settle: .jh-card padding is a
  // percentage of the content width, so the sidebar state must match between captures.
  await page.mouse.move(900, 500);
  await page.waitForTimeout(400);
  return page.evaluate(() => {
    const pick = (el: Element | null, props: string[]) => {
      if (!el) return null;
      const style = getComputedStyle(el);
      return Object.fromEntries(props.map(p => [p, style.getPropertyValue(p)]));
    };
    const text = ['background-color', 'color', 'font-family', 'font-size', 'line-height'];
    return {
      html: pick(document.documentElement, ['background-color', 'color', 'font-family']),
      body: pick(document.body, [...text, 'margin']),
      card: pick(document.querySelector('.jh-card'), [
        ...text,
        'padding-top',
        'padding-left',
        'margin-top',
        'border-top-width',
        'border-top-style',
        'border-radius',
        'box-sizing',
      ]),
      heading: pick(document.querySelector('#page-heading'), ['font-family', 'font-weight', 'color']),
      container: pick(document.querySelector('.va-container'), ['padding-top', 'background-color']),
      mainContent: {
        className: document.querySelector('.main-content')?.className,
        width: document.querySelector('.main-content')?.clientWidth,
      },
    };
  });
}

test.describe.configure({ mode: 'serial' });

test.describe('OST shell', () => {
  test.setTimeout(90_000);

  let user: Session;
  let jupiter: MyTeam;
  let venus: MyTeam;
  let nonMemberTeamId: number;

  test.beforeAll(async ({ browser }) => {
    user = await openSession(browser, USER_USERNAME, USER_PASSWORD);
    const teams = (await (await user.api('get', '/api/team-management/my-teams')).json()) as MyTeam[];
    jupiter = teams.find(t => t.name === JUPITER)!;
    venus = teams.find(t => t.name === VENUS)!;
    expect(jupiter, JUPITER).toBeTruthy();
    expect(venus, VENUS).toBeTruthy();

    // A real team `user` is not in (seeded "Team Mars" is admin-only); read its id as admin.
    const admin = await openSession(browser, ADMIN_USERNAME, ADMIN_PASSWORD);
    const all = (await (await admin.api('get', '/api/admin/teams?size=1000')).json()) as MyTeam[];
    const memberIds = new Set(teams.map(t => t.id));
    nonMemberTeamId = all.find(t => !memberIds.has(t.id))?.id ?? 2_000_000_000;
    await admin.context.close();
  });

  test.afterAll(async () => {
    await user?.context.close();
  });

  test('/trees lands on one of the user’s teams', async () => {
    const page = user.page;
    await page.goto('/trees');
    await expect(page.getByTestId('ostShell')).toBeVisible();
    await expect(page).toHaveURL(/\/trees\/\d+$/);
    const landedOn = Number(new URL(page.url()).pathname.split('/')[2]);
    const teams = (await (await user.api('get', '/api/team-management/my-teams')).json()) as MyTeam[];
    expect(teams.map(t => t.id)).toContain(landedOn);
    await expect(page.getByTestId('ostTeamComboLabel')).toHaveText(teams.find(t => t.id === landedOn)!.name);
    await expect(page.getByTestId('ostMembers')).toBeVisible();
  });

  test('/trees returns to the last team used', async () => {
    const page = user.page;
    await page.goto(`/trees/${venus.id}`);
    await expect(page.getByTestId('ostTeamComboLabel')).toHaveText(VENUS);
    await page.goto('/trees');
    await expect(page).toHaveURL(new RegExp(`/trees/${venus.id}$`));
  });

  test('the team combo switches between Team Jupiter and Team Venus', async () => {
    const page = user.page;
    await page.goto(`/trees/${jupiter.id}`);
    await expect(page.getByTestId('ostTeamComboLabel')).toHaveText(JUPITER);

    await page.getByTestId('ostTeamComboButton').click();
    await page.getByTestId(`ostTeamOption-${venus.id}`).click();
    await expect(page).toHaveURL(new RegExp(`/trees/${venus.id}$`));
    await expect(page.getByTestId('ostTeamComboLabel')).toHaveText(VENUS);

    await page.getByTestId('ostTeamComboButton').click();
    await page.getByTestId(`ostTeamOption-${jupiter.id}`).click();
    await expect(page).toHaveURL(new RegExp(`/trees/${jupiter.id}$`));
    await expect(page.getByTestId('ostTeamComboLabel')).toHaveText(JUPITER);
  });

  test('view tabs route between dashboard, canvas and experiments', async () => {
    const page = user.page;
    await page.goto(`/trees/${jupiter.id}`);
    await expect(page.getByTestId('ostDashboardPage')).toBeVisible();

    await page.getByTestId('ostTabCanvas').click();
    await expect(page).toHaveURL(new RegExp(`/trees/${jupiter.id}/canvas$`));
    await expect(page.getByTestId('ostCanvasPage')).toBeVisible();

    await page.getByTestId('ostTabExperiments').click();
    await expect(page).toHaveURL(new RegExp(`/trees/${jupiter.id}/experiments$`));
    await expect(page.getByTestId('ostExperimentsPage')).toBeVisible();

    await page.getByTestId('ostTabTrees').click();
    await expect(page).toHaveURL(new RegExp(`/trees/${jupiter.id}$`));
    await expect(page.getByTestId('ostDashboardPage')).toBeVisible();
  });

  test('a team the user is not in shows the forbidden state', async () => {
    const page = user.page;
    await page.goto(`/trees/${nonMemberTeamId}`);
    await expect(page.getByTestId('ostForbidden')).toBeVisible();
    await expect(page.getByTestId('ostDashboardPage')).toHaveCount(0);
    await expect(page.getByTestId('ostShell')).toBeVisible();
  });

  test('a non-numeric team id shows the not-found state', async () => {
    const page = user.page;
    await page.goto('/trees/abc');
    await expect(page.getByTestId('ostNotFound')).toBeVisible();
    await expect(page.getByTestId('ostDashboardPage')).toHaveCount(0);
  });

  test('OST styles stay scoped: /teams looks the same after visiting /trees', async () => {
    const page = user.page;
    await page.goto('/teams');
    const fresh = await captureAppStyles(page);

    await page.goto('/trees');
    await expect(page.getByTestId('ostShell')).toBeVisible();
    // Client-side navigation, so the lazily loaded OST CSS is still in the document.
    await page.getByTestId('myTeamsMenu').locator('a').click();
    await expect(page).toHaveURL(/\/teams$/);
    const afterOst = await captureAppStyles(page);

    expect(afterOst).toEqual(fresh);
    expect(fresh.card).not.toBeNull();
  });
});
