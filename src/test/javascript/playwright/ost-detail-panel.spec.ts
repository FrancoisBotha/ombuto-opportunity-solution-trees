import { type Page, expect, test } from '@playwright/test';

import { registerTeamForCleanup } from './support/cleanup';
import { ADMIN_PASSWORD, ADMIN_USERNAME, type Session, USER_PASSWORD, USER_USERNAME, openSession } from './support/session';

/**
 * OST step 10: the detail panel — header (kicker, breadcrumb, editable title, hide / Details
 * reopen), tabs by type, the Detail tab (status, confidence, evidence strength, value, priority,
 * owner, notes, children, quick-add, Open detail, Delete) and the Links tab (add / edit / remove /
 * restore defaults). Every edit is checked after a reload and on the canvas node.
 *
 * Builds its own throwaway team through the API (`user` owns it, `admin` is a VIEWER), deletes its
 * product afterwards and registers the team for the run-end cleanup (support/cleanup.ts). No seeded team is touched.
 */

interface TreeNode {
  key: string;
  type: string;
  id: number;
  parentKey: string | null;
  title: string;
  notes: string | null;
  status: string | null;
  confidence: number | null;
  priority: number | null;
  valueRating: number | null;
  ownerLogin: string | null;
  links: { id: number; name: string; url: string }[];
}

const stamp = Date.now();
const errorsOf = new WeakMap<Page, string[]>();

function watchErrors(page: Page) {
  const list: string[] = [];
  errorsOf.set(page, list);
  page.on('pageerror', e => list.push(`pageerror: ${e.message}`));
  page.on('console', m => {
    if (m.type() === 'error') list.push(`console.error: ${m.text()}`);
  });
}

function takeErrors(page: Page) {
  const list = errorsOf.get(page) ?? [];
  return list.splice(0, list.length);
}

const node = (page: Page, key: string) => page.getByTestId(`ost-node-${key}`);
const panel = (page: Page) => page.getByTestId('ost-panel');

test.describe.configure({ mode: 'serial' });

test.describe('OST detail panel', () => {
  test.setTimeout(120_000);

  let user: Session;
  let admin: Session;
  let teamId: number;
  let productId: number;
  const k: Record<string, string> = {};

  async function mk(type: string, parentType: string, parentId: number, title: string) {
    const res = await user.api('post', '/api/tree/nodes', { type, parentType, parentId, title });
    expect(res.status(), `create ${type} "${title}"`).toBe(201);
    return (await res.json()) as TreeNode;
  }

  /** The node as the server has it now (read through the tree API). */
  async function serverNode(key: string): Promise<TreeNode> {
    const res = await user.api('get', `/api/teams/${teamId}/tree`);
    expect(res.status()).toBe(200);
    const found = ((await res.json()).nodes as TreeNode[]).find(n => n.key === key);
    expect(found, `node ${key} on the server`).toBeDefined();
    return found!;
  }

  async function open(page: Page, key: string) {
    await page.goto(`/trees/${teamId}/canvas?node=${key}`);
    await expect(panel(page)).toBeVisible();
    await expect(node(page, key)).toHaveClass(/\bis-selected\b/);
  }

  /** Waits for the PATCH the action triggers to succeed. */
  async function patched(page: Page, action: () => Promise<unknown>) {
    const response = page.waitForResponse(r => r.url().includes('/api/tree/nodes/') && r.request().method() === 'PATCH');
    await action();
    expect((await response).status()).toBe(200);
  }

  test.beforeAll(async ({ browser }) => {
    user = await openSession(browser, USER_USERNAME, USER_PASSWORD);
    admin = await openSession(browser, ADMIN_USERNAME, ADMIN_PASSWORD);
    for (const s of [user, admin]) {
      await s.page.setViewportSize({ width: 1600, height: 1000 });
      watchErrors(s.page);
    }

    const team = await user.api('post', '/api/team-management/teams', { name: `e2e panel ${stamp}`, description: 'ost-detail-panel' });
    expect(team.status()).toBe(201);
    teamId = (await team.json()).id;
    registerTeamForCleanup(teamId);
    const found = (await (await user.api('get', `/api/team-management/teams/${teamId}/user-search?q=admin`)).json()) as {
      id: string;
      login: string;
    }[];
    const adminId = found.find(u => u.login === 'admin')!.id;
    expect((await user.api('post', `/api/team-management/teams/${teamId}/members`, { userId: adminId, role: 'VIEWER' })).status()).toBe(
      201,
    );

    const product = await user.api('post', '/api/products', {
      name: `Panel product ${stamp}`,
      description: null,
      archived: false,
      createdDate: new Date().toISOString(),
      team: { id: teamId },
    });
    expect(product.status()).toBe(201);
    productId = (await product.json()).id;
    k.product = `product-${productId}`;
    const o1 = await mk('outcome', 'product', productId, 'Panel outcome');
    const op1 = await mk('opportunity', 'outcome', o1.id, 'Panel opportunity');
    const s1 = await mk('solution', 'opportunity', op1.id, 'Panel solution');
    const a1 = await mk('assumption', 'solution', s1.id, 'Panel assumption one');
    const a2 = await mk('assumption', 'solution', s1.id, 'Panel assumption two');
    const doomed = await mk('opportunity', 'outcome', o1.id, 'Doomed opportunity');
    const doomedChild = await mk('solution', 'opportunity', doomed.id, 'Doomed child');
    Object.assign(k, { o1: o1.key, op1: op1.key, s1: s1.key, a1: a1.key, a2: a2.key, doomed: doomed.key, doomedChild: doomedChild.key });
  });

  test.afterEach(async () => {
    for (const s of [user, admin]) expect(takeErrors(s.page), 'page / console errors').toEqual([]);
  });

  test.afterAll(async () => {
    if (productId) {
      const res = await user.api('delete', `/api/tree/nodes/product/${productId}`);
      expect(res.status(), 'product cleanup').toBe(204);
    }
    await user?.context.close();
    await admin?.context.close();
  });

  test('header: kicker, breadcrumb navigation, tabs by type', async () => {
    const page = user.page;
    await open(page, k.a1);
    await expect(page.getByTestId('ost-panel-kicker')).toHaveText('Assumption');
    await expect(page.getByTestId('ost-panel-title')).toHaveValue('Panel assumption one');
    for (const key of [k.product, k.o1, k.op1, k.s1]) await expect(page.getByTestId(`ost-breadcrumb-${key}`)).toBeVisible();
    await expect(panel(page).locator('[role="tab"]')).toHaveText([/^Detail\s*$/, /^Links\s*1\s*$/, /^Transcripts\s*$/, /^History\s*$/]);

    await page.getByTestId(`ost-breadcrumb-${k.op1}`).click();
    await expect(page).toHaveURL(new RegExp(`node=${k.op1}`));
    await expect(node(page, k.op1)).toHaveClass(/\bis-selected\b/);
    await expect(page.getByTestId('ost-panel-title')).toHaveValue('Panel opportunity');
    await expect(page.getByTestId('ost-tab-questions')).toBeVisible();
    await expect(page.getByTestId('ost-tab-badge-links')).toHaveText('3');

    await page.getByTestId(`ost-breadcrumb-${k.product}`).click();
    await expect(page.getByTestId('ost-panel-kicker')).toHaveText('Product');
    await expect(panel(page).locator('[role="tab"]')).toHaveCount(2);
    await expect(page.getByTestId('ost-breadcrumb')).toHaveText('Top of the tree');
  });

  test('hide keeps the selection; the Details tab and a node click reopen', async () => {
    const page = user.page;
    await open(page, k.s1);
    await page.getByTestId('ost-panel-hide').click();
    await expect(panel(page)).toHaveCount(0);
    await expect(node(page, k.s1)).toHaveClass(/\bis-selected\b/);
    await expect(page).toHaveURL(new RegExp(`node=${k.s1}`));
    await page.getByTestId('ost-panel-reopen').click();
    await expect(page.getByTestId('ost-panel-title')).toHaveValue('Panel solution');

    await page.getByTestId('ost-panel-hide').click();
    await expect(page.getByTestId('ost-panel-reopen')).toBeVisible();
    await node(page, k.op1).locator('.ost-node__title').click();
    await expect(panel(page)).toBeVisible();
    await expect(page.getByTestId('ost-panel-title')).toHaveValue('Panel opportunity');
  });

  test('title, status, value and priority persist and show on the node', async () => {
    const page = user.page;
    await open(page, k.op1);

    const title = page.getByTestId('ost-panel-title');
    await title.fill('Renamed then cancelled');
    await title.press('Escape');
    await expect(title).toHaveValue('Panel opportunity');
    await patched(page, async () => {
      await title.fill('Panel opportunity renamed');
      await title.press('Enter');
    });
    await expect(node(page, k.op1).getByTestId('ost-node-title')).toHaveText('Panel opportunity renamed');

    await patched(page, () => page.getByTestId('ost-status-validated').click());
    await expect(node(page, k.op1).getByTestId('ost-node-status')).toHaveText('validated');
    await expect(node(page, k.op1).getByTestId('ost-node-status')).toHaveAttribute('data-tone', 'good');

    await patched(page, () => page.getByTestId('ost-value-5').click());
    await expect(page.getByTestId('ost-value-label')).toHaveText('Outsized');
    await expect(node(page, k.op1).locator('.ost-node__money-on')).toHaveText('$$$$$');

    // Drag the priority slider to ~90%: one PATCH, on release.
    const slider = page.getByTestId('ost-priority');
    const b = (await slider.boundingBox())!;
    let patches = 0;
    const count = (r: import('@playwright/test').Request) => {
      if (r.method() === 'PATCH' && r.url().includes('/api/tree/nodes/')) patches++;
    };
    page.on('request', count);
    await page.mouse.move(b.x + b.width * 0.3, b.y + b.height / 2);
    await page.mouse.down();
    await page.mouse.move(b.x + b.width * 0.6, b.y + b.height / 2, { steps: 4 });
    await page.mouse.move(b.x + b.width * 0.9, b.y + b.height / 2, { steps: 4 });
    expect(patches).toBe(0);
    await patched(page, () => page.mouse.up());
    page.off('request', count);
    expect(patches).toBe(1);
    const priority = Number(await slider.getAttribute('aria-valuenow'));
    expect(priority).toBeGreaterThanOrEqual(86);
    expect(priority).toBeLessThanOrEqual(94);
    await expect(node(page, k.op1).locator('[data-cy="ost-node-priority"] i[data-filled="true"]')).toHaveCount(5);

    await page.reload();
    await expect(panel(page)).toBeVisible();
    await expect(page.getByTestId('ost-panel-title')).toHaveValue('Panel opportunity renamed');
    await expect(page.getByTestId('ost-status-validated')).toHaveAttribute('aria-pressed', 'true');
    await expect(page.getByTestId('ost-value-label')).toHaveText('Outsized');
    await expect(slider).toHaveAttribute('aria-valuenow', String(priority));
    const saved = await serverNode(k.op1);
    expect(saved).toMatchObject({ title: 'Panel opportunity renamed', status: 'VALIDATED', valueRating: 5, priority });
  });

  test('confidence, owner, notes and the derived evidence strength', async () => {
    const page = user.page;
    await open(page, k.a1);
    await patched(page, () => page.getByTestId('ost-status-supported').click());
    await patched(page, () => page.getByTestId('ost-confidence-80').click());
    await expect(page.getByTestId('ost-confidence-label')).toHaveText('High confidence');
    await expect(node(page, k.a1).getByTestId('ost-node-metric')).toHaveText('80% confidence');
    await patched(page, () => page.getByTestId('ost-owner').selectOption('user'));
    const notes = page.getByTestId('ost-notes');
    await notes.fill('We asked five users.');
    await patched(page, () => notes.blur());

    await open(page, k.a2);
    await patched(page, () => page.getByTestId('ost-confidence-20').click());

    // Solution: (100 supported + 20) / 2 = 60, read-only.
    await page.getByTestId(`ost-breadcrumb-${k.s1}`).click();
    await expect(page.getByTestId('ost-evidence-score')).toHaveText('60%');
    await expect(page.getByTestId('ost-evidence-bar').locator('button, input')).toHaveCount(0);
    await expect(node(page, k.s1).getByTestId('ost-node-metric')).toHaveText('2 tests · 60% evidence');

    await page.reload();
    await expect(page.getByTestId('ost-evidence-score')).toHaveText('60%');
    await page.getByTestId(`ost-child-${k.a1}`).click();
    await expect(page).toHaveURL(new RegExp(`node=${k.a1}`));
    await expect(page.getByTestId('ost-confidence')).toHaveAttribute('data-value', '80');
    await expect(page.getByTestId('ost-owner')).toHaveValue('user');
    await expect(page.getByTestId('ost-notes')).toHaveValue('We asked five users.');
    expect(await serverNode(k.a1)).toMatchObject({
      status: 'SUPPORTED',
      confidence: 80,
      ownerLogin: 'user',
      notes: 'We asked five users.',
    });
  });

  test('links: add with validation, edit, remove, restore defaults', async () => {
    const page = user.page;
    await open(page, k.s1);
    await page.getByTestId('ost-tab-links').click();
    await expect(page.locator('[data-cy^="ost-link-row-"]')).toHaveCount(3);
    await expect(page.getByTestId('ost-link-restore-confluence')).toBeDisabled();

    // Add: invalid URL refused client-side, then a valid one.
    await page.getByTestId('ost-link-add').click();
    await page.getByTestId('ost-link-new-name').fill('Spec doc');
    await page.getByTestId('ost-link-new-url').fill('example.com/spec');
    await page.getByTestId('ost-link-new-save').click();
    await expect(page.getByTestId('ost-link-error')).toContainText('http');
    await page.getByTestId('ost-link-new-url').fill('https://example.com/spec');
    const added = page.waitForResponse(r => r.url().includes('/links') && r.request().method() === 'POST');
    await page.getByTestId('ost-link-new-save').click();
    const addedLink = await (await added).json();
    const row = page.getByTestId(`ost-link-row-${addedLink.id}`);
    await expect(row).toBeVisible();
    await expect(row.getByTestId('ost-link-open')).toHaveAttribute('href', 'https://example.com/spec');
    await expect(row.getByTestId('ost-link-open')).toHaveAttribute('target', '_blank');
    await expect(row.getByTestId('ost-link-open')).toHaveAttribute('rel', 'noopener noreferrer');

    // Edit inline.
    const edited = page.waitForResponse(r => r.url().includes(`/api/tree/links/${addedLink.id}`) && r.request().method() === 'PATCH');
    await row.getByTestId('ost-link-name').fill('Spec document');
    await row.getByTestId('ost-link-name').press('Enter');
    expect((await edited).status()).toBe(200);

    // Remove a default, then restore it.
    const confluenceId = (await serverNode(k.s1)).links.find(l => l.name === 'Confluence')!.id;
    const confluence = page.getByTestId(`ost-link-row-${confluenceId}`);
    const removed = page.waitForResponse(r => r.url().includes('/api/tree/links/') && r.request().method() === 'DELETE');
    await confluence.getByTestId('ost-link-remove').click();
    expect((await removed).status()).toBe(204);
    await expect(page.getByTestId('ost-tab-badge-links')).toHaveText('3');
    await expect(page.getByTestId('ost-link-restore-confluence')).toBeEnabled();
    const restored = page.waitForResponse(r => r.url().includes('/links') && r.request().method() === 'POST');
    await page.getByTestId('ost-link-restore-confluence').click();
    expect((await restored).status()).toBe(201);
    await expect(page.getByTestId('ost-link-restore-confluence')).toBeDisabled();

    await page.reload();
    await page.getByTestId('ost-tab-links').click();
    await expect(page.locator('[data-cy^="ost-link-row-"]')).toHaveCount(4);
    await expect(page.getByTestId(`ost-link-row-${addedLink.id}`).getByTestId('ost-link-name')).toHaveValue('Spec document');
    const names = (await serverNode(k.s1)).links.map(l => l.name).sort();
    expect(names).toEqual(['Confluence', 'Jira Epic', 'Jira Initiative', 'Spec document']);
  });

  test('quick-add creates a selected child; delete names the descendants and cascades', async () => {
    const page = user.page;
    await open(page, k.s1);
    await page.getByTestId('ost-tab-detail').click();
    const created = page.waitForResponse(r => r.url().endsWith('/api/tree/nodes') && r.request().method() === 'POST');
    await page.getByTestId('ost-quick-add-assumption').click();
    const child = (await (await created).json()) as TreeNode;
    await expect(page).toHaveURL(new RegExp(`node=${child.key}`));
    await expect(page.getByTestId('ost-panel-kicker')).toHaveText('Assumption');
    await expect(node(page, child.key)).toHaveClass(/\bis-selected\b/);

    await open(page, k.doomed);
    await expect(page.getByTestId('ost-open-detail')).toHaveAttribute('href', `/trees/${teamId}/nodes/${k.doomed}`);
    await page.getByTestId('ost-delete').click();
    await expect(page.getByTestId('ostConfirmDeleteText')).toContainText('“Doomed opportunity” and its 1 descendant');
    const deleted = page.waitForResponse(r => r.url().includes(`/api/tree/nodes/opportunity/`) && r.request().method() === 'DELETE');
    await page.getByTestId('ostConfirmDeleteConfirm').click();
    expect((await deleted).status()).toBe(204);
    await expect(node(page, k.doomed)).toHaveCount(0);
    await expect(node(page, k.doomedChild)).toHaveCount(0);
    await expect(panel(page)).toHaveCount(0);
    const tree = await (await user.api('get', `/api/teams/${teamId}/tree`)).json();
    expect((tree.nodes as TreeNode[]).some(n => n.key === k.doomed || n.key === k.doomedChild)).toBe(false);
  });

  test('viewers get a read-only panel', async () => {
    const page = admin.page;
    await open(page, k.op1);
    await expect(page.getByTestId('ost-panel-title')).toHaveAttribute('readonly', '');
    await expect(page.getByTestId('ost-status-parked')).toBeDisabled();
    await expect(page.getByTestId('ost-value-1')).toBeDisabled();
    await expect(page.getByTestId('ost-priority')).toHaveAttribute('aria-readonly', 'true');
    await expect(page.getByTestId('ost-notes')).toHaveAttribute('readonly', '');
    await expect(page.locator('[data-cy^="ost-quick-add-"]')).toHaveCount(0);
    await expect(page.getByTestId('ost-delete')).toHaveCount(0);
    await expect(page.getByTestId('ost-open-detail')).toBeVisible();

    let writes = 0;
    page.on('request', r => {
      if (r.url().includes('/api/tree/') && r.method() !== 'GET') writes++;
    });
    await page.getByTestId('ost-status-parked').click({ force: true });
    const slider = page.getByTestId('ost-priority');
    const b = (await slider.boundingBox())!;
    await page.mouse.click(b.x + 5, b.y + b.height / 2);
    await page.getByTestId('ost-panel-title').click();
    await page.keyboard.type('viewer edit');
    await page.keyboard.press('Enter');
    await expect(page.getByTestId('ost-panel-title')).toHaveValue('Panel opportunity renamed');

    await page.getByTestId(`ost-breadcrumb-${k.o1}`).click();
    await page.getByTestId('ost-tab-links').click();
    await expect(page.locator('[data-cy^="ost-link-row-"]')).toHaveCount(1);
    await expect(page.getByTestId('ost-link-add')).toHaveCount(0);
    await expect(page.getByTestId('ost-link-remove')).toHaveCount(0);
    await expect(page.locator('[data-cy^="ost-link-restore-"]')).toHaveCount(0);

    await open(page, k.a1);
    await expect(page.getByTestId('ost-confidence-20')).toBeDisabled();
    await expect(page.getByTestId('ost-owner')).toBeDisabled();
    expect(writes).toBe(0);
    expect((await serverNode(k.op1)).status).toBe('VALIDATED');
  });
});
