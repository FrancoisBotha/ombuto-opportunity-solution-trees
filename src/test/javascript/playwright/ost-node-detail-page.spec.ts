import { type Page, expect, test } from '@playwright/test';

import { registerTeamForCleanup } from './support/cleanup';
import { ADMIN_PASSWORD, ADMIN_USERNAME, type Session, USER_PASSWORD, USER_USERNAME, openSession } from './support/session';

/**
 * OST step 12: the full-page node detail (/trees/:teamId/nodes/:nodeKey). Reached from the panel's
 * "Open detail"; breadcrumb and child-card navigation, reload and browser back; title / status /
 * notes edits persist and show on the canvas; a message posted on the page shows in the panel's
 * Chat tab; quick-add opens the new child's page in rename mode; delete goes to the parent's page;
 * "Open on canvas"; unknown and foreign keys; a viewer reads only; a 390px screen (app sidebar
 * expanded and collapsed) never scrolls sideways. (The experiments tracker opens the canvas, not
 * this page, so it is not a way in.)
 *
 * `user` owns a throwaway team and `admin` is a VIEWER of it. The product is deleted in afterAll;
 * the team is registered with support/cleanup.ts and deleted once every spec has finished.
 * Team Jupiter is only read (for a node key of another team), never modified.
 */

interface TreeNode {
  key: string;
  id: number;
  title: string;
}

const stamp = Date.now();
/** localStorage key of the app sidebar state (sidebar-menu.component.ts); absent = expanded. */
const SIDEBAR_KEY = 'va-sidebar-expanded';

function watchErrors(page: Page) {
  const list: string[] = [];
  page.on('pageerror', e => list.push(`pageerror: ${e.message}`));
  page.on('console', m => {
    if (m.type() === 'error') list.push(`console.error: ${m.text()}`);
  });
  return list;
}

const detail = (page: Page) => page.getByTestId('ost-node-detail');
const title = (page: Page) => page.getByTestId('ost-node-detail-title');
const canvasNode = (page: Page, key: string) => page.getByTestId(`ost-node-${key}`);
const panel = (page: Page) => page.getByTestId('ost-panel');

test.describe.configure({ mode: 'serial' });

test.describe('OST full-page node detail', () => {
  test.setTimeout(120_000);

  let user: Session;
  let admin: Session;
  let userErrors: string[];
  let adminErrors: string[];
  let teamId: number;
  let productId: number;
  const n: Record<string, TreeNode> = {};

  async function mk(type: string, parentType: string, parentId: number, name: string) {
    const res = await user.api('post', '/api/tree/nodes', { type, parentType, parentId, title: name });
    expect(res.status(), `create ${type} "${name}"`).toBe(201);
    return (await res.json()) as TreeNode;
  }

  const pageUrl = (key: string) => `/trees/${teamId}/nodes/${key}`;

  async function openPage(page: Page, key: string) {
    await page.goto(pageUrl(key));
    await expect(detail(page)).toBeVisible();
    await expect(detail(page)).toHaveAttribute('data-node-key', key);
  }

  /** Waits for the API call `action` triggers and checks its status. */
  async function api(page: Page, method: string, path: string | RegExp, status: number, action: () => Promise<unknown>) {
    const response = page.waitForResponse(
      r => r.request().method() === method && (typeof path === 'string' ? r.url().includes(path) : path.test(r.url())),
    );
    await action();
    const res = await response;
    expect(res.status(), `${method} ${path}`).toBe(status);
    return res;
  }

  test.beforeAll(async ({ browser }) => {
    user = await openSession(browser, USER_USERNAME, USER_PASSWORD);
    admin = await openSession(browser, ADMIN_USERNAME, ADMIN_PASSWORD);
    await user.page.setViewportSize({ width: 1600, height: 1000 });
    await admin.page.setViewportSize({ width: 1600, height: 1000 });
    userErrors = watchErrors(user.page);
    adminErrors = watchErrors(admin.page);

    const team = await user.api('post', '/api/team-management/teams', {
      name: `e2e node detail ${stamp}`,
      description: 'ost-node-detail-page',
    });
    expect(team.status()).toBe(201);
    teamId = (await team.json()).id;
    registerTeamForCleanup(teamId);
    const found = (await (await user.api('get', `/api/team-management/teams/${teamId}/user-search?q=admin`)).json()) as {
      id: string;
      login: string;
    }[];
    const added = await user.api('post', `/api/team-management/teams/${teamId}/members`, {
      userId: found.find(u => u.login === 'admin')!.id,
      role: 'VIEWER',
    });
    expect(added.status()).toBe(201);

    const product = await user.api('post', '/api/products', {
      name: `Detail product ${stamp}`,
      description: null,
      archived: false,
      createdDate: new Date().toISOString(),
      team: { id: teamId },
    });
    expect(product.status()).toBe(201);
    productId = (await product.json()).id;
    n.o1 = await mk('outcome', 'product', productId, 'Detail outcome');
    n.op1 = await mk('opportunity', 'outcome', n.o1.id, 'Detail opportunity');
    n.s1 = await mk('solution', 'opportunity', n.op1.id, 'Chatty solution');
    n.a1 = await mk('assumption', 'solution', n.s1.id, 'People answer invites');
    n.doomed = await mk('solution', 'opportunity', n.op1.id, 'Doomed solution');
    n.doomedChild = await mk('assumption', 'solution', n.doomed.id, 'Doomed assumption');
  });

  test.afterEach(() => {
    expect(userErrors.splice(0), 'user: page / console errors').toEqual([]);
    expect(adminErrors.splice(0), 'admin: page / console errors').toEqual([]);
  });

  test.afterAll(async () => {
    if (productId) expect((await user.api('delete', `/api/products/${productId}`)).status(), 'product cleanup').toBe(204);
    await user?.context.close();
    await admin?.context.close();
  });

  test('"Open detail" from the panel; breadcrumb and child cards navigate; reload and back', async () => {
    const page = user.page;
    await page.goto(`/trees/${teamId}/canvas?node=${n.op1.key}`);
    await expect(panel(page)).toBeVisible();
    await page.getByTestId('ost-open-detail').click();
    await expect(page).toHaveURL(new RegExp(`${pageUrl(n.op1.key)}$`));
    await expect(detail(page)).toBeVisible();
    await expect(page.getByTestId('ost-node-detail-kicker')).toHaveText('Opportunity');
    await expect(title(page)).toHaveValue('Detail opportunity');
    await expect(page.getByTestId('ost-node-detail-status')).toHaveText('unexplored');
    await expect(page.getByTestId('ost-node-detail-metric-priority')).toHaveText('Medium priority');
    await expect(page.getByTestId('ost-node-detail-metric-value')).toHaveText('$$$ Solid');
    await expect(page.getByTestId('ost-node-detail-breadcrumb')).toContainText(`Detail product ${stamp}`);
    await expect(page.getByTestId('ost-node-detail-questions')).toBeVisible();
    await expect(page.getByTestId('ost-node-detail-chat')).toBeVisible();
    await expect(page.locator('[data-cy^="ost-node-detail-child-"]')).toHaveCount(2);

    // breadcrumb → the outcome's page (no chat on the product only)
    await page.getByTestId(`ost-node-detail-crumb-${n.o1.key}`).click();
    await expect(page).toHaveURL(new RegExp(`${pageUrl(n.o1.key)}$`));
    await expect(title(page)).toHaveValue('Detail outcome');
    await expect(page.getByTestId(`ost-node-detail-child-${n.op1.key}`)).toBeVisible();
    await page.getByTestId(`ost-node-detail-crumb-product-${productId}`).click();
    await expect(page.getByTestId('ost-node-detail-kicker')).toHaveText('Product');
    await expect(page.getByTestId('ost-node-detail-breadcrumb')).toHaveText('Top of the tree');
    await expect(page.getByTestId('ost-node-detail-chat')).toHaveCount(0);

    // back twice → the opportunity again
    await page.goBack();
    await expect(title(page)).toHaveValue('Detail outcome');
    await page.goBack();
    await expect(title(page)).toHaveValue('Detail opportunity');

    // child card → the solution's page; evidence tag untested → 40% with one assumption at 40
    await page.getByTestId(`ost-node-detail-child-${n.s1.key}`).click();
    await expect(page).toHaveURL(new RegExp(`${pageUrl(n.s1.key)}$`));
    await expect(title(page)).toHaveValue('Chatty solution');
    await expect(page.getByTestId('ost-node-detail-metric-evidence')).toHaveText('40% evidence');
    await expect(page.getByTestId(`ost-node-detail-crumb-${n.op1.key}`)).toBeVisible();

    // deep link survives a reload; back returns to the parent's page
    await page.reload();
    await expect(detail(page)).toHaveAttribute('data-node-key', n.s1.key);
    await expect(title(page)).toHaveValue('Chatty solution');
    await page.goBack();
    await expect(detail(page)).toHaveAttribute('data-node-key', n.op1.key);
  });

  test('title, status and notes edits persist and appear on the canvas', async () => {
    const page = user.page;
    await openPage(page, n.op1.key);
    const path = `/api/tree/nodes/opportunity/${n.op1.id}`;

    // Escape cancels a rename
    await title(page).fill('Not this one');
    await title(page).press('Escape');
    await expect(title(page)).toHaveValue('Detail opportunity');

    await title(page).fill('Renamed on the page');
    await api(page, 'PATCH', path, 200, () => title(page).press('Enter'));
    await api(page, 'PATCH', path, 200, () => page.getByTestId('ost-status-exploring').click());
    await expect(page.getByTestId('ost-node-detail-status')).toHaveText('exploring');
    await page.getByTestId('ost-notes').fill('Learned on the full page');
    await api(page, 'PATCH', path, 200, () => page.getByTestId('ost-notes').blur());

    await page.reload();
    await expect(title(page)).toHaveValue('Renamed on the page');
    await expect(page.getByTestId('ost-node-detail-status')).toHaveText('exploring');
    await expect(page.getByTestId('ost-notes')).toHaveValue('Learned on the full page');

    await page.getByTestId('ost-node-detail-open-canvas').click();
    await expect(page).toHaveURL(new RegExp(`/trees/${teamId}/canvas\\?node=${n.op1.key}$`));
    await expect(canvasNode(page, n.op1.key)).toHaveClass(/\bis-selected\b/);
    await expect(canvasNode(page, n.op1.key)).toContainText('Renamed on the page');
    await expect(canvasNode(page, n.op1.key).getByTestId('ost-node-status')).toHaveText('exploring');
    await expect(page.getByTestId('ost-panel-title')).toHaveValue('Renamed on the page');
    await expect(panel(page).getByTestId('ost-notes')).toHaveValue('Learned on the full page');
  });

  test('a message posted on the page shows in the panel’s Chat tab', async () => {
    const page = user.page;
    await api(page, 'GET', `/nodes/solution/${n.s1.id}/comments`, 200, () => page.goto(pageUrl(n.s1.key)));
    const chat = page.getByTestId('ost-node-detail-chat');
    await expect(chat.getByTestId('ost-chat-empty')).toBeVisible();
    await chat.getByTestId('ost-chat-input').fill('Posted from the detail page');
    const res = await api(page, 'POST', /\/comments$/, 201, () => chat.getByTestId('ost-chat-input').press('Enter'));
    const posted = (await res.json()) as { id: number };
    await expect(chat.getByTestId(`ost-chat-msg-${posted.id}`)).toHaveAttribute('data-mine', 'true');

    await page.getByTestId('ost-node-detail-open-canvas').click();
    await expect(panel(page)).toBeVisible();
    await expect(page.getByTestId(`ost-node-chat-${n.s1.key}`)).toHaveText('1');
    await api(page, 'GET', `/nodes/solution/${n.s1.id}/comments`, 200, () => page.getByTestId('ost-tab-chat').click());
    await expect(panel(page).getByTestId(`ost-chat-msg-${posted.id}`)).toContainText('Posted from the detail page');
  });

  test('quick-add opens the new child’s page in rename mode', async () => {
    const page = user.page;
    await openPage(page, n.a1.key);
    await expect(page.getByTestId('ost-node-detail-metric-confidence')).toHaveText('Medium · 40%');
    const res = await api(page, 'POST', /\/api\/tree\/nodes$/, 201, () => page.getByTestId('ost-node-detail-add-evidence').click());
    const child = (await res.json()) as TreeNode;
    await expect(page).toHaveURL(new RegExp(`${pageUrl(child.key)}$`));
    await expect(page.getByTestId('ost-node-detail-kicker')).toHaveText('Evidence');
    await expect(title(page)).toBeFocused();
    await page.keyboard.type('Test result from the page');
    await api(page, 'PATCH', `/api/tree/nodes/evidence/${child.id}`, 200, () => page.keyboard.press('Enter'));
    await page.getByTestId(`ost-node-detail-crumb-${n.a1.key}`).click();
    await expect(page.getByTestId(`ost-node-detail-child-${child.key}`)).toContainText('Test result from the page');
  });

  test('delete confirms, cascades and goes to the parent’s page', async () => {
    const page = user.page;
    await openPage(page, n.doomed.key);
    await page.getByTestId('ost-node-detail-delete').click();
    await expect(page.getByTestId('ostConfirmDeleteText')).toContainText('“Doomed solution” and its 1 descendant');
    await api(page, 'DELETE', `/api/tree/nodes/solution/${n.doomed.id}`, 204, () => page.getByTestId('ostConfirmDeleteConfirm').click());
    await expect(page).toHaveURL(new RegExp(`${pageUrl(n.op1.key)}$`));
    await expect(detail(page)).toHaveAttribute('data-node-key', n.op1.key);
    await expect(page.getByTestId(`ost-node-detail-child-${n.doomed.key}`)).toHaveCount(0);
    const tree = await (await user.api('get', `/api/teams/${teamId}/tree`)).json();
    expect((tree.nodes as TreeNode[]).some(x => x.key === n.doomed.key || x.key === n.doomedChild.key)).toBe(false);

    // the deleted node's address is now a not-found page inside the shell
    await page.goto(pageUrl(n.doomed.key));
    await expect(page.getByTestId('ostShell')).toBeVisible();
    await expect(page.getByTestId('ost-node-detail-missing')).toContainText('Node not found');
  });

  test('unknown and foreign node keys show not-found inside the shell', async () => {
    const page = user.page;
    await page.goto(pageUrl('opportunity-999999999'));
    await expect(page.getByTestId('ostShell')).toBeVisible();
    await expect(page.getByTestId('ost-node-detail-missing')).toBeVisible();
    await page.goto(pageUrl('nonsense'));
    await expect(page.getByTestId('ost-node-detail-missing')).toBeVisible();

    // a node of another team (read only: Team Jupiter's tree)
    const teams = (await (await user.api('get', '/api/team-management/my-teams')).json()) as { id: number; name: string }[];
    const other = teams.find(t => t.id !== teamId && t.name === 'Team Jupiter') ?? teams.find(t => t.id !== teamId);
    expect(other, 'user belongs to another team').toBeTruthy();
    const foreign = ((await (await user.api('get', `/api/teams/${other!.id}/tree`)).json()).nodes as TreeNode[])[0];
    expect(foreign).toBeTruthy();
    await page.goto(pageUrl(foreign.key));
    await expect(page.getByTestId('ost-node-detail-missing')).toBeVisible();
    await page.getByTestId('ost-node-detail-missing-canvas').click();
    await expect(page).toHaveURL(new RegExp(`/trees/${teamId}/canvas`));
  });

  test('a viewer reads the page but cannot change anything', async () => {
    const page = admin.page;
    await openPage(page, n.op1.key);
    await expect(title(page)).toHaveText('Renamed on the page');
    await expect(title(page)).toHaveJSProperty('tagName', 'H1');
    await expect(page.getByTestId('ost-node-detail-delete')).toHaveCount(0);
    await expect(page.locator('[data-cy^="ost-node-detail-add-"]')).toHaveCount(0);
    await expect(page.getByTestId('ost-status-validated')).toBeDisabled();
    await expect(page.getByTestId('ost-notes')).toHaveAttribute('readonly', '');
    await expect(page.getByTestId('ost-node-detail-chat').getByTestId('ost-chat-input')).toBeDisabled();
    await expect(page.getByTestId('ost-chat-readonly')).toBeVisible();
    await expect(page.getByTestId('ost-link-add')).toHaveCount(0);
    await page.getByTestId(`ost-node-detail-child-${n.s1.key}`).click();
    await expect(title(page)).toHaveText('Chatty solution');
  });

  for (const sidebar of ['expanded', 'collapsed'] as const) {
    test(`fits a 390px-wide screen with the app sidebar ${sidebar}`, async () => {
      const page = user.page;
      const before = page.viewportSize();
      const stored = await page.evaluate(key => localStorage.getItem(key), SIDEBAR_KEY);
      await page.setViewportSize({ width: 390, height: 844 });
      try {
        await page.evaluate(([key, value]) => localStorage.setItem(key, value), [SIDEBAR_KEY, String(sidebar === 'expanded')]);
        await openPage(page, n.op1.key);
        await expect(page.getByTestId('ost-node-detail-chat')).toBeVisible();
        await page.evaluate(() => document.fonts.ready.then(() => undefined));

        let previous = '';
        let layout: { pageWidth: number; pageOverflow: number; overflowing: string[]; chatBelow: boolean } | null = null;
        await expect
          .poll(
            async () => {
              const current = await page.getByTestId('ostNodeDetailPage').evaluate(pageEl => {
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
                      `${name(el)} ${Math.round(box.left)}..${Math.round(box.right)} outside ${Math.round(pageBox.left)}..${Math.round(pageBox.right)}`,
                    );
                }
                const main = pageEl.querySelector('.ost-nd__main')!.getBoundingClientRect();
                const chat = pageEl.querySelector('[data-cy="ost-node-detail-chat"]')!.getBoundingClientRect();
                const transitions = document.getAnimations().filter(a => a instanceof CSSTransition && a.playState === 'running').length;
                return JSON.stringify({
                  layout: {
                    pageWidth: Math.round(pageBox.width),
                    pageOverflow: pageEl.scrollWidth - pageEl.clientWidth,
                    overflowing,
                    chatBelow: chat.top >= main.bottom - 0.5,
                  },
                  transitions,
                });
              });
              const stable = current === previous && JSON.parse(current).transitions === 0;
              previous = current;
              layout = JSON.parse(current).layout;
              return stable;
            },
            { message: 'node detail layout settles', intervals: [100, 100, 200, 200, 300], timeout: 15_000 },
          )
          .toBe(true);

        if (sidebar === 'expanded') expect(layout!.pageWidth).toBeLessThan(200);
        else expect(layout!.pageWidth).toBeGreaterThan(300);
        expect(layout!.overflowing, 'elements wider than their box or sticking out of the page').toEqual([]);
        expect(layout!.pageOverflow, 'page scrolls sideways').toBeLessThanOrEqual(0);
        expect(layout!.chatBelow, 'one column: the discussion sits below the main column').toBe(true);
        expect(await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth)).toBeLessThanOrEqual(
          0,
        );
      } finally {
        await page.evaluate(([key, value]) => (value === null ? localStorage.removeItem(key) : localStorage.setItem(key, value)), [
          SIDEBAR_KEY,
          stored,
        ] as const);
        if (before) await page.setViewportSize(before);
      }
    });
  }
});
