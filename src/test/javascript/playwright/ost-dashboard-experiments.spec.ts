import { type Frame, type Page, type Route, expect, test } from '@playwright/test';

import { registerTeamForCleanup } from './support/cleanup';
import { ADMIN_PASSWORD, ADMIN_USERNAME, type Session, USER_PASSWORD, USER_USERNAME, openSession } from './support/session';

/**
 * OST step 8: the Trees dashboard and the Experiments tracker.
 *
 * (a) Reads seeded Team Jupiter as `user` and never changes it: expected numbers are computed from
 *     GET /api/teams/{id}/tree, not hard-coded.
 * (b) Builds a throwaway team through the API for the empty states; its product is deleted in
 *     afterAll, the team after the run (support/cleanup.ts).
 */

interface MyTeam {
  id: number;
  name: string;
}

interface TreeNode {
  key: string;
  type: string;
  id: number;
  parentKey: string | null;
  status: string | null;
}

interface Tree {
  id: number;
  name: string;
  evidenceThisMonth: number;
  nodes: TreeNode[];
}

const JUPITER = 'Team Jupiter';

/** Collects uncaught page errors and console errors; the test fails if any were seen. */
function watchErrors(page: Page) {
  const errors: string[] = [];
  page.on('pageerror', err => errors.push(`pageerror: ${err.message}`));
  page.on('console', msg => {
    if (msg.type() === 'error') errors.push(`console: ${msg.text()} @ ${msg.location().url}`);
  });
  return errors;
}

/** The product a node belongs to (walks parentKey up to the root). */
function productKeyOf(key: string, nodes: TreeNode[]): string {
  const byKey = new Map(nodes.map(n => [n.key, n]));
  let current = byKey.get(key);
  while (current?.parentKey) current = byKey.get(current.parentKey);
  return current!.key;
}

/**
 * Tags the current document so a later check can tell a client-side navigation from a full reload
 * (a reload replaces the window, and the tag with it).
 */
async function markDocument(page: Page) {
  const tag = `doc-${Date.now()}-${Math.random()}`;
  await page.evaluate(value => ((window as unknown as { __ostDocTag?: string }).__ostDocTag = value), tag);
  return {
    async expectNone() {
      const current = await page.evaluate(() => (window as unknown as { __ostDocTag?: string }).__ostDocTag ?? null);
      expect(current, 'the page reloaded during the navigation (a dev-server full reload, or a 401 forcing re-authentication)').toBe(tag);
    },
  };
}

/** localStorage key of the app sidebar state (sidebar-menu.component.ts); absent = expanded. */
const SIDEBAR_KEY = 'va-sidebar-expanded';

interface DashboardLayout {
  pageWidth: number;
  pageOverflow: number;
  overflowing: string[];
  overlappingLabels: string[];
}

/**
 * Measures the dashboard only once it has fully rendered and stopped moving: every product card is
 * in the DOM, web fonts are loaded, no CSS transition (the sidebar's width) is running and two
 * consecutive measurements agree. Measuring as soon as the page is visible read 0, then 120, then
 * 136px of overflow as cards and fonts arrived.
 */
async function settledDashboardLayout(page: Page, cards: number): Promise<DashboardLayout> {
  await expect(page.getByTestId('ost-dashboard')).toBeVisible();
  await expect(page.locator('[data-cy^="ost-product-card-"]')).toHaveCount(cards);
  await page.evaluate(() => document.fonts.ready.then(() => undefined));

  let previous = '';
  let layout: DashboardLayout | null = null;
  await expect
    .poll(
      async () => {
        const current = await page.getByTestId('ostDashboardPage').evaluate(pageEl => {
          const pageBox = pageEl.getBoundingClientRect();
          const name = (el: Element) => el.getAttribute('data-cy') ?? `${el.tagName.toLowerCase()}.${[...el.classList].join('.')}`;
          const overflowing: string[] = [];
          for (const el of pageEl.querySelectorAll('*')) {
            const box = el.getBoundingClientRect();
            if (!box.width) continue;
            if (el.scrollWidth > el.clientWidth + 1 && getComputedStyle(el).overflowX === 'visible' && el.clientWidth > 0)
              overflowing.push(`${name(el)} content ${el.scrollWidth}px > box ${el.clientWidth}px`);
            if (box.right > pageBox.right + 0.5 || box.left < pageBox.left - 0.5)
              overflowing.push(
                `${name(el)} ${Math.round(box.left)}..${Math.round(box.right)} outside page ${Math.round(pageBox.left)}..${Math.round(pageBox.right)}`,
              );
          }
          // Stat labels and values must stay inside their own tile (they overlapped the next tile before).
          const overlappingLabels: string[] = [];
          for (const tile of pageEl.querySelectorAll('.ost-stats__tile')) {
            const tileBox = tile.getBoundingClientRect();
            for (const child of tile.querySelectorAll('*')) {
              const range = document.createRange();
              range.selectNodeContents(child);
              const text = range.getBoundingClientRect();
              if (text.right > tileBox.right + 0.5)
                overlappingLabels.push(
                  `${name(tile)}: "${child.textContent}" ends ${Math.round(text.right - tileBox.right)}px past its tile`,
                );
            }
          }
          const transitions = document.getAnimations().filter(a => a instanceof CSSTransition && a.playState === 'running').length;
          return JSON.stringify({
            layout: {
              pageWidth: Math.round(pageBox.width),
              pageOverflow: pageEl.scrollWidth - pageEl.clientWidth,
              overflowing,
              overlappingLabels,
            },
            transitions,
          });
        });
        const stable = current === previous && JSON.parse(current).transitions === 0;
        previous = current;
        layout = JSON.parse(current).layout;
        return stable;
      },
      { message: 'dashboard layout settles', intervals: [100, 100, 200, 200, 300], timeout: 15_000 },
    )
    .toBe(true);
  return layout!;
}

test.describe.configure({ mode: 'serial' });

test.describe('OST dashboard and experiments', () => {
  test.setTimeout(120_000);

  let user: Session;
  let errors: string[];

  test.beforeAll(async ({ browser }) => {
    user = await openSession(browser, USER_USERNAME, USER_PASSWORD);
    errors = watchErrors(user.page);
  });

  test.afterAll(async () => {
    await user?.context.close();
  });

  test.afterEach(() => {
    expect(errors, 'page errors / console errors').toEqual([]);
  });

  test.describe('seeded Team Jupiter (read-only)', () => {
    let jupiter: MyTeam;
    let tree: Tree;

    test.beforeAll(async () => {
      const teams = (await (await user.api('get', '/api/team-management/my-teams')).json()) as MyTeam[];
      jupiter = teams.find(t => t.name === JUPITER)!;
      expect(jupiter, JUPITER).toBeTruthy();
      const response = await user.api('get', `/api/teams/${jupiter.id}/tree`);
      expect(response.status()).toBe(200);
      tree = (await response.json()) as Tree;
    });

    test('the four counters match the tree read', async () => {
      const page = user.page;
      await page.goto(`/trees/${jupiter.id}`);
      await expect(page.getByTestId('ost-dashboard')).toBeVisible();
      await expect(page.getByTestId('ostDashboardKicker')).toHaveText(`${JUPITER} · continuous discovery`);

      const count = (type: string) => tree.nodes.filter(n => n.type === type).length;
      const expected = {
        opportunities: count('OPPORTUNITY'),
        solutions: count('SOLUTION'),
        tests: tree.nodes.filter(n => n.type === 'ASSUMPTION' && n.status === 'TESTING').length,
        evidence: tree.evidenceThisMonth,
      };
      for (const [key, value] of Object.entries(expected)) {
        await expect(page.getByTestId(`ost-stat-${key}`).getByTestId('ost-stat-value'), key).toHaveText(String(value));
      }
      await expect(page.getByTestId('ost-stat-evidence')).toContainText('Evidence this month');
    });

    for (const sidebar of ['expanded', 'collapsed'] as const) {
      test(`the dashboard fits a 390px-wide screen with the app sidebar ${sidebar}`, async () => {
        const page = user.page;
        const before = page.viewportSize();
        const stored = await page.evaluate(key => localStorage.getItem(key), SIDEBAR_KEY);
        await page.setViewportSize({ width: 390, height: 844 });
        try {
          await page.evaluate(([key, value]) => localStorage.setItem(key, value), [SIDEBAR_KEY, String(sidebar === 'expanded')]);
          await page.goto(`/trees/${jupiter.id}`);
          const products = tree.nodes.filter(n => n.type === 'PRODUCT').length;
          const layout = await settledDashboardLayout(page, products);

          // The sidebar really is in the state under test (expanded leaves the page ~160px wide).
          if (sidebar === 'expanded') expect(layout.pageWidth).toBeLessThan(200);
          else expect(layout.pageWidth).toBeGreaterThan(300);

          expect(layout.pageOverflow, 'page scrolls sideways').toBeLessThanOrEqual(0);
          expect(layout.overflowing, 'elements wider than their box or sticking out of the page').toEqual([]);
          expect(layout.overlappingLabels, 'stat tiles whose text overlaps a neighbour').toEqual([]);
        } finally {
          await page.evaluate(([key, value]) => (value === null ? localStorage.removeItem(key) : localStorage.setItem(key, value)), [
            SIDEBAR_KEY,
            stored,
          ] as const);
          if (before) await page.setViewportSize(before);
        }
      });
    }

    test('one card per product; "Open branch" opens the canvas scoped to it', async () => {
      const page = user.page;
      const products = tree.nodes.filter(n => n.type === 'PRODUCT');
      expect(products).toHaveLength(2);

      await page.goto(`/trees/${jupiter.id}`);
      await expect(page.locator('[data-cy^="ost-product-card-"]')).toHaveCount(products.length);
      for (const product of products) {
        const card = page.getByTestId(`ost-product-card-${product.key}`);
        await expect(card).toBeVisible();
        const inBranch = (type: string) =>
          tree.nodes.filter(n => n.type === type && productKeyOf(n.key, tree.nodes) === product.key).length;
        await expect(card.getByTestId('ost-product-count-opportunities')).toContainText(String(inBranch('OPPORTUNITY')));
        await expect(card.getByTestId('ost-product-count-assumptions')).toContainText(String(inBranch('ASSUMPTION')));
        await expect(card.getByTestId('ost-product-updated')).toContainText(/Last edited|No edits yet/);
      }

      const first = products[0];
      const reloads = await markDocument(page);
      await page.getByTestId(`ost-open-branch-${first.key}`).click();
      await expect(page).toHaveURL(new RegExp(`/trees/${jupiter.id}/canvas\\?product=${first.key}$`));
      await expect(page.getByTestId('ostCanvasPage')).toBeVisible();
      await reloads.expectNone();
    });

    /*
     * Regression for the "Open branch snaps back to /trees/:id" flake. The eval run's snap-back was a
     * whole-document reload (Vite re-optimising @vue-flow/core mid-run and reloading every page) that
     * landed while the lazy canvas route was still loading. This test makes every request the
     * navigation depends on slow — the canvas chunks, the tree read, the team list and the account —
     * and checks that nothing in the app re-routes or reloads: the URL reaches the canvas, stays
     * there, and the document is the one the click happened in.
     */
    test('"Open branch" survives slow chunk, tree, team-list and account responses', async () => {
      const page = user.page;
      const product = tree.nodes.find(n => n.type === 'PRODUCT')!;
      const slow = (ms: number) => async (route: Route) => {
        await new Promise(resolve => setTimeout(resolve, ms));
        await route.continue().catch(() => undefined);
      };
      const urls: string[] = [];
      const onNavigated = (frame: Frame) => frame === page.mainFrame() && urls.push(frame.url());
      await page.route(/\/api\/teams\/\d+\/tree/, slow(1_000));
      await page.route(/\/api\/team-management\/my-teams/, slow(3_000));
      await page.route(/\/api\/account/, slow(1_000));
      await page.route(/\/app\/ost\/(pages\/CanvasPage|canvas\/|panel\/)/, slow(1_500));
      try {
        await page.goto(`/trees/${jupiter.id}`);
        // Click while the team list (and so the combo) is still loading.
        await expect(page.getByTestId(`ost-open-branch-${product.key}`)).toBeVisible();
        const reloads = await markDocument(page);
        page.on('framenavigated', onNavigated);
        await page.getByTestId(`ost-open-branch-${product.key}`).click();

        const canvasUrl = new RegExp(`/trees/${jupiter.id}/canvas\\?product=${product.key}$`);
        await expect(page).toHaveURL(canvasUrl, { timeout: 15_000 });
        await expect(page.getByTestId('ostCanvasPage')).toBeVisible({ timeout: 15_000 });
        // Let the slow team list land (and anything it could trigger) before the final check.
        await expect(page.getByTestId('ostTeamComboLabel')).toHaveText(JUPITER, { timeout: 15_000 });
        await page.waitForTimeout(500);
        expect(page.url()).toMatch(canvasUrl);
        const onCanvas = (u: string) => new URL(u).pathname === `/trees/${jupiter.id}/canvas`;
        const arrived = urls.findIndex(onCanvas);
        expect(arrived, `navigations after the click: ${urls.join(', ')}`).toBeGreaterThanOrEqual(0);
        expect(
          urls.slice(arrived).filter(u => !onCanvas(u)),
          'navigations that left the canvas again',
        ).toEqual([]);
        await reloads.expectNone();
      } finally {
        page.off('framenavigated', onNavigated);
        await page.unrouteAll({ behavior: 'ignoreErrors' });
      }
    });

    test('the tracker has one row per assumption; a row opens that node on the canvas', async () => {
      const page = user.page;
      const assumptions = tree.nodes.filter(n => n.type === 'ASSUMPTION');
      expect(assumptions.length).toBeGreaterThan(0);

      await page.goto(`/trees/${jupiter.id}/experiments`);
      await expect(page.getByTestId('ost-experiments')).toBeVisible();
      await expect(page.locator('[data-cy^="ost-experiment-row-"]')).toHaveCount(assumptions.length);
      for (const a of assumptions) await expect(page.getByTestId(`ost-experiment-row-${a.key}`)).toBeVisible();

      const by = (status: string) => assumptions.filter(a => a.status === status).length;
      await expect(page.getByTestId('ost-experiments-summary')).toHaveText(
        `${by('TESTING')} running · ${by('UNTESTED')} queued · ${by('SUPPORTED') + by('REFUTED')} settled`,
      );

      const target = assumptions[assumptions.length - 1];
      await page.getByTestId(`ost-experiment-row-${target.key}`).click();
      await expect(page).toHaveURL(
        new RegExp(`/trees/${jupiter.id}/canvas\\?node=${target.key}&product=${productKeyOf(target.key, tree.nodes)}$`),
      );

      // Keyboard: focus a row and press Enter.
      await page.goto(`/trees/${jupiter.id}/experiments`);
      const row = page.getByTestId(`ost-experiment-row-${assumptions[0].key}`);
      await row.focus();
      await page.keyboard.press('Enter');
      await expect(page).toHaveURL(new RegExp(`/trees/${jupiter.id}/canvas\\?node=${assumptions[0].key}&`));
    });

    test('a viewer (admin in Team Jupiter) sees the same pages', async ({ browser }) => {
      const viewer = await openSession(browser, ADMIN_USERNAME, ADMIN_PASSWORD);
      const viewerErrors = watchErrors(viewer.page);
      try {
        const page = viewer.page;
        await page.goto(`/trees/${jupiter.id}`);
        await expect(page.locator('[data-cy^="ost-product-card-"]')).toHaveCount(tree.nodes.filter(n => n.type === 'PRODUCT').length);
        await page.goto(`/trees/${jupiter.id}/experiments`);
        await expect(page.locator('[data-cy^="ost-experiment-row-"]')).toHaveCount(tree.nodes.filter(n => n.type === 'ASSUMPTION').length);
        expect(viewerErrors, 'viewer page errors / console errors').toEqual([]);
      } finally {
        await viewer.context.close();
      }
    });
  });

  test.describe('empty states (throwaway team)', () => {
    const stamp = Date.now();
    let teamId: number | null = null;
    let productId: number | null = null;

    test.afterAll(async () => {
      // Only `user` is in this team, so the end-of-run cleanup (admin) cannot list its products.
      if (productId !== null) {
        const deleted = await user.api('delete', `/api/tree/nodes/product/${productId}`);
        expect(deleted.status(), 'delete throwaway product').toBe(204);
      }
    });

    test('a team without products shows zero counters and the empty states', async () => {
      const created = await user.api('post', '/api/team-management/teams', {
        name: `OST dashboard e2e ${stamp}`,
        description: 'step 8 e2e',
      });
      expect(created.status()).toBe(201);
      teamId = (await created.json()).id;
      registerTeamForCleanup(teamId!);

      const page = user.page;
      await page.goto(`/trees/${teamId}`);
      await expect(page.getByTestId('ost-dashboard')).toBeVisible();
      for (const key of ['opportunities', 'solutions', 'tests', 'evidence']) {
        await expect(page.getByTestId(`ost-stat-${key}`).getByTestId('ost-stat-value')).toHaveText('0');
      }
      await expect(page.getByTestId('ost-dashboard-empty')).toBeVisible();
      await expect(page.locator('[data-cy^="ost-product-card-"]')).toHaveCount(0);

      await page.getByTestId('ostTabExperiments').click();
      await expect(page.getByTestId('ost-experiments-empty')).toBeVisible();
      await expect(page.getByTestId('ost-experiments-summary')).toHaveText('0 running · 0 queued · 0 settled');
    });

    test('a product without assumptions gets a card; the tracker stays empty', async () => {
      const created = await user.api('post', '/api/products', {
        name: `Empty branch ${stamp}`,
        description: 'step 8 e2e',
        archived: false,
        createdDate: new Date().toISOString(),
        team: { id: teamId },
      });
      expect(created.status()).toBe(201);
      productId = (await created.json()).id;

      const page = user.page;
      await page.goto(`/trees/${teamId}`);
      const card = page.getByTestId(`ost-product-card-product-${productId}`);
      await expect(card).toBeVisible();
      await expect(card).toContainText(`Empty branch ${stamp}`);
      await expect(card).toContainText('No outcome set');
      await expect(page.getByTestId('ost-dashboard-empty')).toHaveCount(0);

      await page.goto(`/trees/${teamId}/experiments`);
      await expect(page.getByTestId('ost-experiments-empty')).toBeVisible();
    });
  });
});
