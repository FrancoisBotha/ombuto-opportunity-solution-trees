import { type Page, expect, test } from '@playwright/test';

import { registerTeamForCleanup } from './support/cleanup';
import { ADMIN_PASSWORD, ADMIN_USERNAME, type Session, USER_PASSWORD, USER_USERNAME, openSession } from './support/session';

/**
 * OST step 6: the tree-builder shell — /trees landing, team switcher, view tabs, access states and
 * style isolation (the OST dark theme must not leak into the rest of the app).
 *
 * Reads only the seeded team NAMES (Team Jupiter / Team Venus for `user`, Team Mars admin-only),
 * never tree content, and never changes the seeded teams. The long-team-list case creates its own
 * throwaway teams; they are deleted after the run (support/cleanup.ts).
 */

interface MyTeam {
  id: number;
  name: string;
}

const JUPITER = 'Team Jupiter';
const VENUS = 'Team Venus';

/**
 * Waits until the sidebar and main content stop moving: no CSS transition running and the same
 * geometry on two consecutive polls.
 */
async function waitForStableLayout(page: Page) {
  let previous = '';
  await expect
    .poll(
      async () => {
        const current = await page.evaluate(() => {
          const box = (selector: string) => {
            const r = document.querySelector(selector)?.getBoundingClientRect();
            return r ? [r.x, r.width] : null;
          };
          const transitions = document.getAnimations().filter(a => a instanceof CSSTransition && a.playState === 'running').length;
          return JSON.stringify({ sidebar: box('[data-cy="sidebar"]'), main: box('.main-content'), transitions });
        });
        const stable = current === previous && JSON.parse(current).transitions === 0;
        previous = current;
        return stable;
      },
      { intervals: [50, 50, 100, 100, 200], timeout: 10_000 },
    )
    .toBe(true);
}

/** Computed styles that must be identical on /teams whether or not OST was visited first. */
async function captureAppStyles(page: Page) {
  await expect(page.getByTestId('myTeamsPage')).toBeVisible();
  // Keep the pointer off the sidebar and let its width transition settle: .jh-card padding is a
  // percentage of the content width, so the sidebar state must match between captures.
  await page.mouse.move(900, 500);
  await waitForStableLayout(page);
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

  test('OST text uses the design weights, not the app body’s 300', async () => {
    const page = user.page;
    await page.goto(`/trees/${jupiter.id}`);
    await expect(page.getByTestId('ostDashboardPage')).toBeVisible();
    const weight = (selector: string) =>
      page
        .locator(selector)
        .first()
        .evaluate(el => getComputedStyle(el).fontWeight);
    expect(await weight('.ost-root')).toBe('400');
    expect(await weight('.ost-stats__label')).toBe('400');
    expect(await weight('.ost-product-card__outcome')).toBe('400');
    expect(await weight('.ost-page__title')).toBe('500');
    expect(await weight('.ost-nav__brand')).toBe('600');
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

  test.describe('a long team list', () => {
    const stamp = Date.now();

    test('scrolls inside the combo menu; the shell and its nav never move; Escape closes', async () => {
      for (let i = 0; i < 15; i++) {
        const response = await user.api('post', '/api/team-management/teams', {
          name: `OST combo e2e ${stamp} ${String(i).padStart(2, '0')}`,
          description: 'step 6b e2e (throwaway)',
        });
        expect(response.status()).toBe(201);
        registerTeamForCleanup((await response.json()).id);
      }

      const page = user.page;
      await page.goto(`/trees/${jupiter.id}`);
      await expect(page.getByTestId('ostTeamComboLabel')).toHaveText(JUPITER);
      const nav = page.locator('.ost-nav');
      const navTop = (await nav.boundingBox())!.y;
      const shellScroll = () => page.getByTestId('ostShell').evaluate(el => [el.scrollTop, document.scrollingElement?.scrollTop ?? 0]);

      const button = page.getByTestId('ostTeamComboButton');
      await button.focus();
      await page.keyboard.press('ArrowDown');
      const menu = page.getByTestId('ostTeamComboMenu');
      await expect(menu).toBeVisible();
      const options = menu.getByRole('option');
      const count = await options.count();
      expect(count).toBeGreaterThanOrEqual(17);
      // The menu is its own scroll container, shorter than its content.
      const scroller = await menu.evaluate(el => ({ overflowY: getComputedStyle(el).overflowY, fits: el.scrollHeight <= el.clientHeight }));
      expect(scroller).toEqual({ overflowY: 'auto', fits: false });

      for (let i = 1; i < count; i++) await page.keyboard.press('ArrowDown');
      await expect(options.last()).toBeFocused();
      await expect(options.last()).toBeInViewport();
      expect((await nav.boundingBox())!.y).toBe(navTop);
      expect(await shellScroll()).toEqual([0, 0]);
      expect(await menu.evaluate(el => el.scrollTop)).toBeGreaterThan(0);

      await page.keyboard.press('Escape');
      await expect(menu).toHaveCount(0);
      await expect(button).toBeFocused();

      // Escape on the trigger itself (menu open via click) and Tab away both close it.
      await button.click();
      await expect(menu).toBeVisible();
      await page.keyboard.press('Escape');
      await expect(menu).toHaveCount(0);
      await button.click();
      await expect(menu).toBeVisible();
      await page.keyboard.press('ArrowDown');
      await page.keyboard.press('End');
      await expect(options.last()).toBeFocused();
      await page.keyboard.press('Tab');
      await expect(menu).toHaveCount(0);
      expect((await nav.boundingBox())!.y).toBe(navTop);
    });

    test('in a 480px-high window the menu ends above the bottom edge, and it follows a resize', async () => {
      const page = user.page;
      const before = page.viewportSize();
      try {
        await page.setViewportSize({ width: 1280, height: 480 });
        await page.goto(`/trees/${jupiter.id}`);
        await expect(page.getByTestId('ostTeamComboLabel')).toHaveText(JUPITER);
        const menu = page.getByTestId('ostTeamComboMenu');
        await page.getByTestId('ostTeamComboButton').click();
        await expect(menu).toBeVisible();
        // The long list from the previous test is still there: the menu has to scroll, not overflow.
        expect(await menu.evaluate(el => el.scrollHeight > el.clientHeight)).toBe(true);
        const bottom = async () => {
          const box = (await menu.boundingBox())!;
          return box.y + box.height;
        };
        expect(await bottom()).toBeLessThanOrEqual(480);

        await page.setViewportSize({ width: 1280, height: 400 });
        await expect.poll(bottom).toBeLessThanOrEqual(400);
        await expect(menu.getByRole('option').last()).toBeAttached();
      } finally {
        await page.keyboard.press('Escape');
        if (before) await page.setViewportSize(before);
      }
    });
  });
});
