import { type Page, expect, test } from '@playwright/test';

import { ADMIN_PASSWORD, ADMIN_USERNAME, type Session, USER_PASSWORD, USER_USERNAME, openSession } from './support/session';

/**
 * OST step 8: the Trees dashboard and the Experiments tracker.
 *
 * (a) Reads seeded Team Jupiter as `user` and never changes it: expected numbers are computed from
 *     GET /api/teams/{id}/tree, not hard-coded.
 * (b) Builds a throwaway team through the API for the empty states and deletes it afterwards.
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
      await page.getByTestId(`ost-open-branch-${first.key}`).click();
      await expect(page).toHaveURL(new RegExp(`/trees/${jupiter.id}/canvas\\?product=${first.key}$`));
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

    test.afterAll(async ({ browser }) => {
      if (productId !== null) {
        const deleted = await user.api('delete', `/api/tree/nodes/product/${productId}`);
        expect(deleted.status(), 'delete throwaway product').toBe(204);
      }
      if (teamId !== null) {
        const admin = await openSession(browser, ADMIN_USERNAME, ADMIN_PASSWORD);
        const deleted = await admin.api('delete', `/api/admin/teams/${teamId}`);
        await admin.context.close();
        expect([200, 204], 'delete throwaway team').toContain(deleted.status());
      }
    });

    test('a team without products shows zero counters and the empty states', async () => {
      const created = await user.api('post', '/api/team-management/teams', {
        name: `OST dashboard e2e ${stamp}`,
        description: 'step 8 e2e',
      });
      expect(created.status()).toBe(201);
      teamId = (await created.json()).id;

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
