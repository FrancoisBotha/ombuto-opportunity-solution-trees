import { type Locator, type Page, expect, test } from '@playwright/test';

import { ADMIN_PASSWORD, ADMIN_USERNAME, type Session, USER_PASSWORD, USER_USERNAME, openSession } from './support/session';

/**
 * OST step 9: canvas editing — the node + menu (valid child types only), create → selected + rename
 * mode, inline rename (Enter / Escape / blur, inline validation), drag a node onto another to
 * re-parent (legal targets highlighted, rejected drops snap back), the node palette (drag with a
 * ghost chip, click-to-arm, collapse to a rail), delete with a descendant count, keyboard paths,
 * and viewers seeing none of it.
 *
 * Builds its own throwaway team through the API (`user` owns it, `admin` is a VIEWER) and deletes it
 * afterwards. Seeded teams are never touched.
 */

interface TreeNode {
  key: string;
  type: string;
  id: number;
  parentKey: string | null;
  title: string;
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

const takeErrors = (page: Page) => {
  const list = errorsOf.get(page) ?? [];
  return list.splice(0, list.length);
};

const node = (page: Page, key: string) => page.getByTestId(`ost-node-${key}`);

async function box(locator: Locator) {
  const b = await locator.boundingBox();
  expect(b, 'element has a box').not.toBeNull();
  return b!;
}

const centre = (b: { x: number; y: number; width: number; height: number }) => ({ x: b.x + b.width / 2, y: b.y + b.height / 2 });

/** Waits until the viewport and every node's (possibly mid-transition) transform stop changing. */
async function settle(page: Page) {
  let last = '';
  await expect
    .poll(
      async () => {
        const now = await page.evaluate(() => {
          const pane = document.querySelector<HTMLElement>('.vue-flow__transformationpane');
          const nodes = Array.from(document.querySelectorAll<HTMLElement>('.vue-flow__node'), n => getComputedStyle(n).transform);
          return `${pane ? getComputedStyle(pane).transform : ''}|${nodes.join('|')}`;
        });
        const still = now === last;
        last = now;
        return still;
      },
      { intervals: [120] },
    )
    .toBe(true);
}

/** Presses on a node's kicker (never a button) and moves in steps to `to`; the caller releases. */
async function pickUp(page: Page, from: Locator, to: { x: number; y: number }) {
  const start = centre(await box(from.locator('.ost-node__kicker')));
  await page.mouse.move(start.x, start.y);
  await page.mouse.down();
  await page.mouse.move(start.x + 12, start.y + 12, { steps: 3 });
  await page.mouse.move(to.x, to.y, { steps: 12 });
}

test.describe.configure({ mode: 'serial' });

test.describe('OST tree canvas — editing', () => {
  test.setTimeout(120_000);

  let user: Session;
  let admin: Session;
  let teamId: number;
  const productIds: number[] = [];
  const k: Record<string, string> = {};

  async function mk(type: string, parentType: string, parentId: number, title: string) {
    const res = await user.api('post', '/api/tree/nodes', { type, parentType, parentId, title });
    expect(res.status(), `create ${type} "${title}"`).toBe(201);
    return (await res.json()) as TreeNode;
  }

  async function mkProduct(name: string) {
    const res = await user.api('post', '/api/products', {
      name,
      description: null,
      archived: false,
      createdDate: new Date().toISOString(),
      team: { id: teamId },
    });
    expect(res.status(), `create product ${name}`).toBe(201);
    const id = (await res.json()).id as number;
    productIds.push(id);
    return id;
  }

  async function treeNodes(): Promise<TreeNode[]> {
    const res = await user.api('get', `/api/teams/${teamId}/tree`);
    expect(res.status()).toBe(200);
    return ((await res.json()) as { nodes: TreeNode[] }).nodes;
  }

  const serverNode = async (key: string) => (await treeNodes()).find(n => n.key === key);

  async function openCanvas(page: Page, query = '') {
    await page.goto(`/trees/${teamId}/canvas${query}`);
    await expect(page.getByTestId('ost-canvas')).toBeVisible();
    await expect(node(page, k.productA)).toBeVisible();
    await settle(page);
  }

  /** The key of the (single) selected node. */
  const selectedKey = async (page: Page) => {
    await expect(page.locator('.ost-node.is-selected')).toHaveCount(1);
    return (await page.locator('.ost-node.is-selected').getAttribute('data-node-key'))!;
  };

  test.beforeAll(async ({ browser }) => {
    user = await openSession(browser, USER_USERNAME, USER_PASSWORD);
    admin = await openSession(browser, ADMIN_USERNAME, ADMIN_PASSWORD);
    for (const s of [user, admin]) {
      await s.page.setViewportSize({ width: 1600, height: 1000 });
      watchErrors(s.page);
    }

    const team = await user.api('post', '/api/team-management/teams', { name: `e2e canvas edit ${stamp}`, description: 'ost-canvas-edit' });
    expect(team.status()).toBe(201);
    teamId = (await team.json()).id;
    const found = (await (await user.api('get', `/api/team-management/teams/${teamId}/user-search?q=admin`)).json()) as {
      id: string;
      login: string;
    }[];
    const adminId = found.find(u => u.login === 'admin')!.id;
    expect((await user.api('post', `/api/team-management/teams/${teamId}/members`, { userId: adminId, role: 'VIEWER' })).status()).toBe(
      201,
    );

    const pA = await mkProduct(`Edit A ${stamp}`);
    k.productA = `product-${pA}`;
    const o1 = await mk('outcome', 'product', pA, 'Outcome one');
    const o2 = await mk('outcome', 'product', pA, 'Outcome two');
    const opA = await mk('opportunity', 'outcome', o1.id, 'Movable opportunity');
    const opB = await mk('opportunity', 'outcome', o1.id, 'Opportunity with kids');
    const s1 = await mk('solution', 'opportunity', opB.id, 'Solution one');
    const as1 = await mk('assumption', 'solution', s1.id, 'Assumption one');
    const ev1 = await mk('evidence', 'assumption', as1.id, 'Test result');
    Object.assign(k, { o1: o1.key, o2: o2.key, opA: opA.key, opB: opB.key, s1: s1.key, as1: as1.key, ev1: ev1.key });
  });

  test.afterEach(async () => {
    for (const s of [user, admin]) expect(takeErrors(s.page), 'page / console errors').toEqual([]);
  });

  test.afterAll(async () => {
    if (teamId) {
      for (const id of productIds) await user.api('delete', `/api/products/${id}`);
      const res = await admin.api('delete', `/api/admin/teams/${teamId}`);
      expect(res.status(), 'team cleanup').toBe(204);
    }
    await user?.context.close();
    await admin?.context.close();
  });

  test('the + menu offers exactly the valid child types of each node', async () => {
    const page = user.page;
    await openCanvas(page);
    const expected: [string, string[]][] = [
      [k.productA, ['outcome']],
      [k.o1, ['opportunity']],
      [k.opB, ['opportunity', 'solution', 'evidence']],
      [k.s1, ['assumption']],
      [k.as1, ['evidence']],
    ];
    for (const [key, types] of expected) {
      await page.getByTestId(`ost-node-add-${key}`).click();
      const menu = node(page, key).getByTestId('ost-add-menu');
      await expect(menu).toBeVisible();
      await expect(menu.getByRole('menuitem')).toHaveCount(types.length);
      for (const t of types) await expect(menu.getByTestId(`ost-add-menu-${t}`)).toBeVisible();
      // Keyboard: focus starts on the first item; Escape closes and returns focus to the +.
      await expect(menu.getByTestId(`ost-add-menu-${types[0]}`)).toBeFocused();
      await page.keyboard.press('Escape');
      await expect(page.getByTestId('ost-add-menu')).toHaveCount(0);
      await expect(page.getByTestId(`ost-node-add-${key}`)).toBeFocused();
    }
    // Evidence takes no children: no +.
    await expect(page.getByTestId(`ost-node-add-${k.ev1}`)).toHaveCount(0);
    // A click outside closes an open menu.
    await page.getByTestId(`ost-node-add-${k.o1}`).click();
    await expect(page.getByTestId('ost-add-menu')).toBeVisible();
    await node(page, k.o2).locator('.ost-node__kicker').click();
    await expect(page.getByTestId('ost-add-menu')).toHaveCount(0);
  });

  test('a child created from the + menu lands selected and in rename mode; the name persists', async () => {
    const page = user.page;
    await openCanvas(page);
    const before = (await treeNodes()).length;
    await page.getByTestId(`ost-node-add-${k.o2}`).click();
    await page.getByTestId('ost-add-menu-opportunity').click();

    const input = page.getByTestId('ost-rename-input');
    await expect(input).toBeVisible();
    await expect(input).toBeFocused();
    await expect(input).toHaveValue('New opportunity');
    const key = await selectedKey(page);
    expect(key).toMatch(/^opportunity-\d+$/);
    await expect(node(page, key).getByTestId('ost-rename-input')).toBeVisible();
    await expect(page).toHaveURL(new RegExp(`node=${key}`));
    // The new node sits under its parent.
    expect((await box(node(page, key))).y).toBeGreaterThan((await box(node(page, k.o2))).y);

    await input.fill('Created from the menu');
    await input.press('Enter');
    await expect(input).toHaveCount(0);
    await expect(node(page, key).getByTestId('ost-node-title')).toHaveText('Created from the menu');
    await expect(node(page, key)).toBeFocused();

    expect((await treeNodes()).length).toBe(before + 1);
    expect(await serverNode(key)).toMatchObject({ parentKey: k.o2, title: 'Created from the menu' });
    await page.reload();
    await expect(node(page, key).getByTestId('ost-node-title')).toHaveText('Created from the menu');
    k.created = key;
  });

  test('double-click renames inline: Enter commits, Escape cancels, blur commits, invalid titles are refused', async () => {
    const page = user.page;
    await openCanvas(page);
    const title = node(page, k.opA).getByTestId('ost-node-title');
    const input = page.getByTestId('ost-rename-input');

    // Escape cancels.
    await title.dblclick();
    await expect(input).toBeFocused();
    await input.fill('Should not stick');
    await input.press('Escape');
    await expect(input).toHaveCount(0);
    await expect(title).toHaveText('Movable opportunity');

    // Empty and too-long titles are refused inline; the field stays open.
    await title.dblclick();
    await input.fill('   ');
    await input.press('Enter');
    await expect(page.getByTestId('ost-rename-error')).toHaveText('A title is required.');
    await expect(input).toBeVisible();
    await input.fill('x'.repeat(201));
    await input.press('Enter');
    await expect(page.getByTestId('ost-rename-error')).toContainText('at most 200 characters');
    await expect(input).toHaveAttribute('aria-invalid', 'true');

    // Enter commits (trimmed).
    await input.fill('  Renamed with Enter  ');
    await input.press('Enter');
    await expect(title).toHaveText('Renamed with Enter');
    await expect.poll(async () => (await serverNode(k.opA))?.title).toBe('Renamed with Enter');

    // Blur commits.
    await title.dblclick();
    await input.fill('Renamed on blur');
    await page.getByTestId('ost-search').click();
    await expect(input).toHaveCount(0);
    await expect(title).toHaveText('Renamed on blur');
    await expect.poll(async () => (await serverNode(k.opA))?.title).toBe('Renamed on blur');

    await page.reload();
    await expect(node(page, k.opA).getByTestId('ost-node-title')).toHaveText('Renamed on blur');
  });

  test('dragging an opportunity onto another outcome re-parents it (and it persists)', async () => {
    const page = user.page;
    await openCanvas(page);
    const target = node(page, k.o2);
    await pickUp(page, node(page, k.opA), centre(await box(target)));

    // Legal targets are highlighted; the one under the pointer is the drop target.
    await expect(target).toHaveAttribute('data-drop-target', 'true');
    await expect(target).toHaveAttribute('data-drop-hover', 'true');
    await expect(target).toHaveClass(/\bis-drop\b/);
    await expect(node(page, k.opB)).toHaveAttribute('data-drop-target', 'true'); // opportunity under opportunity
    await expect(node(page, k.o1)).not.toHaveAttribute('data-drop-target', 'true'); // current parent
    await expect(node(page, k.s1)).not.toHaveAttribute('data-drop-target', 'true'); // solution can't hold one
    await page.mouse.up();

    await expect(page.locator('[data-drop-target]')).toHaveCount(0);
    await expect.poll(async () => (await serverNode(k.opA))?.parentKey).toBe(k.o2);
    await settle(page);
    // Laid out under its new parent (never where it was dropped).
    const o2 = await box(target);
    const opA = await box(node(page, k.opA));
    expect(opA.y).toBeGreaterThan(o2.y + o2.height);

    await page.reload();
    await expect(node(page, k.opA)).toBeVisible();
    await settle(page);
    expect((await box(node(page, k.opA))).y).toBeGreaterThan((await box(node(page, k.o2))).y + 50);
  });

  test('a drop on an invalid target snaps back and changes nothing', async () => {
    const page = user.page;
    await openCanvas(page);
    const dragged = node(page, k.s1);
    const before = await box(dragged);

    // A solution cannot go under an outcome.
    await pickUp(page, dragged, centre(await box(node(page, k.o2))));
    await expect(node(page, k.o2)).not.toHaveAttribute('data-drop-target', 'true');
    await expect(node(page, k.o2)).not.toHaveAttribute('data-drop-hover', 'true');
    await expect(node(page, k.opA)).toHaveAttribute('data-drop-target', 'true');
    const midDrag = await box(dragged);
    expect(Math.abs(midDrag.x - before.x) + Math.abs(midDrag.y - before.y)).toBeGreaterThan(40);
    await page.mouse.up();

    await expect
      .poll(async () => {
        const now = await box(dragged);
        return Math.max(Math.abs(now.x - before.x), Math.abs(now.y - before.y));
      })
      .toBeLessThan(1);
    // Onto its own descendant: also rejected.
    await pickUp(page, dragged, centre(await box(node(page, k.as1))));
    await expect(node(page, k.as1)).not.toHaveAttribute('data-drop-hover', 'true');
    await page.mouse.up();
    await expect
      .poll(async () => {
        const now = await box(dragged);
        return Math.max(Math.abs(now.x - before.x), Math.abs(now.y - before.y));
      })
      .toBeLessThan(1);

    expect((await serverNode(k.s1))?.parentKey).toBe(k.opB);
    await page.reload();
    await expect(dragged).toBeVisible();
    await settle(page);
    const after = await box(dragged);
    expect(Math.abs(after.x - before.x)).toBeLessThan(1);
    expect(Math.abs(after.y - before.y)).toBeLessThan(1);
  });

  test('dragging a palette type onto a node attaches it there; an invalid drop does nothing', async () => {
    const page = user.page;
    await openCanvas(page);
    const count = (await treeNodes()).length;
    const tool = page.getByTestId('ost-palette-solution');
    const start = centre(await box(tool));

    // Invalid: an outcome cannot take a solution.
    await page.mouse.move(start.x, start.y);
    await page.mouse.down();
    await page.mouse.move(start.x + 40, start.y + 10, { steps: 4 });
    const ghost = page.getByTestId('ost-palette-ghost');
    await expect(ghost).toBeVisible();
    await expect(ghost).toContainText('Solution');
    await expect(ghost).toContainText('Drop on a highlighted node');
    await expect(node(page, k.opB)).toHaveAttribute('data-drop-target', 'true');
    await expect(node(page, k.o2)).not.toHaveAttribute('data-drop-target', 'true');
    const o2 = centre(await box(node(page, k.o2)));
    await page.mouse.move(o2.x, o2.y, { steps: 10 });
    await expect(ghost).toContainText('Drop on a highlighted node');
    await page.mouse.up();
    await expect(ghost).toHaveCount(0);
    await expect(page.locator('[data-drop-target]')).toHaveCount(0);
    await expect(page.getByTestId('ost-rename-input')).toHaveCount(0);
    expect((await treeNodes()).length).toBe(count);

    // Valid: onto an opportunity.
    await page.mouse.move(start.x, start.y);
    await page.mouse.down();
    const opB = centre(await box(node(page, k.opB)));
    await page.mouse.move(start.x + 40, start.y + 10, { steps: 4 });
    await page.mouse.move(opB.x, opB.y, { steps: 12 });
    await expect(ghost).toContainText('Attach here');
    await expect(ghost).toHaveAttribute('data-over', 'true');
    await expect(node(page, k.opB)).toHaveAttribute('data-drop-hover', 'true');
    await page.mouse.up();
    await expect(ghost).toHaveCount(0);

    const input = page.getByTestId('ost-rename-input');
    await expect(input).toBeFocused();
    await expect(input).toHaveValue('New solution');
    const key = await selectedKey(page);
    await input.press('Escape'); // keeps the default title
    await expect(node(page, key).getByTestId('ost-node-title')).toHaveText('New solution');
    expect(await serverNode(key)).toMatchObject({ type: 'SOLUTION', parentKey: k.opB, title: 'New solution' });
    // The palette is not armed after a drag.
    await expect(tool).toHaveAttribute('aria-pressed', 'false');
  });

  test('click a palette type to arm it, then click a highlighted node to attach', async () => {
    const page = user.page;
    await openCanvas(page);
    const tool = page.getByTestId('ost-palette-evidence');

    // Clicking the armed type again disarms; Escape disarms.
    await tool.click();
    await expect(tool).toHaveAttribute('aria-pressed', 'true');
    await expect(page.getByTestId('ost-palette-hint')).toHaveText('Click a highlighted node to attach the new evidence.');
    for (const key of [k.opA, k.opB, k.as1]) await expect(node(page, key)).toHaveAttribute('data-drop-target', 'true');
    for (const key of [k.o1, k.s1, k.ev1, k.productA]) await expect(node(page, key)).not.toHaveAttribute('data-drop-target', 'true');
    await tool.click();
    await expect(tool).toHaveAttribute('aria-pressed', 'false');
    await expect(page.locator('[data-drop-target]')).toHaveCount(0);
    await tool.click();
    await expect(tool).toHaveAttribute('aria-pressed', 'true');
    await page.keyboard.press('Escape');
    await expect(tool).toHaveAttribute('aria-pressed', 'false');

    // Armed + a click on a node that cannot take it: nothing happens, still armed.
    await tool.click();
    const count = (await treeNodes()).length;
    await node(page, k.s1).locator('.ost-node__kicker').click();
    await expect(tool).toHaveAttribute('aria-pressed', 'true');
    await expect(page.getByTestId('ost-rename-input')).toHaveCount(0);
    expect((await treeNodes()).length).toBe(count);

    // Armed + a click on a legal node: attaches and disarms.
    await node(page, k.as1).locator('.ost-node__kicker').click();
    const input = page.getByTestId('ost-rename-input');
    await expect(input).toBeFocused();
    await expect(input).toHaveValue('New snippet');
    await expect(tool).toHaveAttribute('aria-pressed', 'false');
    const key = await selectedKey(page);
    await input.fill('Armed evidence');
    await input.press('Enter');
    await expect.poll(async () => (await serverNode(key))?.title).toBe('Armed evidence');
    expect((await serverNode(key))?.parentKey).toBe(k.as1);
  });

  test('the palette collapses to a 34px rail and reopens', async () => {
    const page = user.page;
    await openCanvas(page);
    const palette = page.getByTestId('ost-palette');
    expect(Math.round((await box(palette)).width)).toBe(182);
    await page.getByTestId('ost-palette-toggle').click();
    await expect(palette).toHaveAttribute('data-state', 'closed');
    expect(Math.round((await box(palette)).width)).toBe(34);
    await expect(page.getByTestId('ost-palette-solution')).toHaveCount(0);
    await page.getByTestId('ost-palette-toggle').click();
    await expect(palette).toHaveAttribute('data-state', 'open');
    expect(Math.round((await box(palette)).width)).toBe(182);
    await expect(page.getByTestId('ost-palette-solution')).toBeVisible();
  });

  test('keyboard: focus a node, open its + menu, create, rename and return focus to the new node', async () => {
    const page = user.page;
    await openCanvas(page);
    const o1 = node(page, k.o1);
    await o1.focus();
    await expect(o1).toHaveAttribute('aria-label', 'Outcome: Outcome one');
    await page.keyboard.press('Enter');
    await expect(o1).toHaveClass(/\bis-selected\b/);
    await expect(o1).toHaveAttribute('aria-label', 'Outcome: Outcome one, selected');
    // Focus ring is visible (box-shadow ring outside the selection outline).
    expect(await o1.evaluate(el => getComputedStyle(el).boxShadow)).toMatch(/7px/);

    await page.keyboard.press('Tab');
    await expect(page.getByTestId(`ost-node-add-${k.o1}`)).toBeFocused();
    await page.keyboard.press('Enter');
    await expect(page.getByTestId('ost-add-menu-opportunity')).toBeFocused();
    await page.keyboard.press('Enter');
    const input = page.getByTestId('ost-rename-input');
    await expect(input).toBeFocused();
    await page.keyboard.type('Typed by keyboard');
    await page.keyboard.press('Enter');
    const key = await selectedKey(page);
    await expect(node(page, key)).toBeFocused();
    await expect(node(page, key).getByTestId('ost-node-title')).toHaveText('Typed by keyboard');

    // F2 renames the focused node; Escape cancels and focus stays on it.
    await page.keyboard.press('F2');
    await expect(input).toBeFocused();
    await page.keyboard.press('Escape');
    await expect(node(page, key)).toBeFocused();

    // Delete on the focused node asks first; Escape cancels and the node stays.
    await page.keyboard.press('Delete');
    await expect(page.getByTestId('ostConfirmDelete')).toBeVisible();
    await page.keyboard.press('Escape');
    await expect(page.getByTestId('ostConfirmDelete')).toHaveCount(0);
    await expect(node(page, key)).toBeVisible();
    expect(await serverNode(key)).toBeTruthy();
  });

  test('Delete asks for confirmation naming the node and its descendants, then cascades', async () => {
    const page = user.page;
    await openCanvas(page);
    const all = await treeNodes();
    // opB's subtree: s1 → as1 → (ev1, armed evidence), plus the palette-created solution.
    const descendants = (key: string): number => all.filter(n => n.parentKey === key).reduce((sum, n) => sum + 1 + descendants(n.key), 0);
    const n = descendants(k.opB);
    expect(n).toBeGreaterThanOrEqual(4);

    await node(page, k.opB).locator('.ost-node__kicker').click();
    await expect(node(page, k.opB)).toHaveClass(/\bis-selected\b/);
    await page.keyboard.press('Delete');
    const dialog = page.getByTestId('ostConfirmDelete');
    await expect(dialog).toBeVisible();
    await expect(dialog).toContainText('Delete this opportunity?');
    await expect(page.getByTestId('ostConfirmDeleteText')).toContainText(`“Opportunity with kids” and its ${n} descendants`);

    await page.getByTestId('ostConfirmDeleteCancel').click();
    await expect(dialog).toHaveCount(0);
    await expect(node(page, k.opB)).toBeVisible();

    await page.keyboard.press('Delete');
    await page.getByTestId('ostConfirmDeleteConfirm').click();
    await expect(dialog).toHaveCount(0);
    for (const key of [k.opB, k.s1, k.as1, k.ev1]) await expect(node(page, key)).toHaveCount(0);
    const left = await treeNodes();
    for (const key of [k.opB, k.s1, k.as1, k.ev1]) expect(left.find(x => x.key === key)).toBeUndefined();
    expect(left.length).toBe(all.length - n - 1);
    // Focus goes back to the parent node.
    await expect(node(page, k.o1)).toBeFocused();

    await page.reload();
    await expect(node(page, k.o1)).toBeVisible();
    await expect(node(page, k.opB)).toHaveCount(0);
  });

  test('viewers get none of the editing affordances', async () => {
    const page = admin.page;
    await page.goto(`/trees/${teamId}/canvas`);
    await expect(node(page, k.opA)).toBeVisible();
    await settle(page);
    const before = await treeNodes();

    await expect(page.getByTestId('ost-palette')).toHaveCount(0);
    await expect(page.getByTestId('ost-palette-toggle')).toHaveCount(0);
    await expect(page.locator('[data-cy^="ost-node-add-"]')).toHaveCount(0);
    await expect(page.locator('.vue-flow__node.draggable')).toHaveCount(0);

    // No rename.
    await node(page, k.opA).getByTestId('ost-node-title').click();
    await settle(page);
    await node(page, k.opA).getByTestId('ost-node-title').dblclick();
    await expect(page.getByTestId('ost-rename-input')).toHaveCount(0);
    await node(page, k.opA).focus();
    await page.keyboard.press('F2');
    await expect(page.getByTestId('ost-rename-input')).toHaveCount(0);

    // No delete.
    await expect(node(page, k.opA)).toHaveClass(/\bis-selected\b/);
    await page.keyboard.press('Delete');
    await expect(page.getByTestId('ostConfirmDelete')).toHaveCount(0);

    // No drag: the node stays put and nothing is highlighted.
    const dragged = node(page, k.opA);
    const start = await box(dragged);
    await pickUp(page, dragged, centre(await box(node(page, k.o1))));
    await expect(page.locator('[data-drop-target]')).toHaveCount(0);
    await page.mouse.up();
    await settle(page);
    const end = await box(dragged);
    // Dragging a viewer's node pans the canvas instead; relative to its parent it has not moved.
    const parentNow = await box(node(page, k.o2));
    expect(end.y).toBeGreaterThan(parentNow.y + parentNow.height);
    expect(start.width).toBeCloseTo(end.width, 0);

    expect(await treeNodes()).toEqual(before);
  });
});
