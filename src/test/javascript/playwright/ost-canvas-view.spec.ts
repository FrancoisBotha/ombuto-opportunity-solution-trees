import { type Locator, type Page, expect, test } from '@playwright/test';

import { registerTeamForCleanup } from './support/cleanup';
import { ADMIN_PASSWORD, ADMIN_USERNAME, type Session, USER_PASSWORD, USER_USERNAME, openSession } from './support/session';

/**
 * OST step 7: the tree canvas renders and navigates — frames, layout, collapse, search, type
 * filters, product scope + re-fit, zoom/Fit, overview map, selection + ?node= deep link, viewer mode.
 *
 * Builds its own throwaway team through the API (`user` owns it, `admin` is a VIEWER), deletes its
 * products afterwards and registers the team for the run-end cleanup (support/cleanup.ts). The only seeded data touched is a read-only look at Team Jupiter.
 */

interface TreeNode {
  key: string;
  type: string;
  id: number;
  parentKey: string | null;
}

const stamp = Date.now();
const errorsOf = new WeakMap<Page, string[]>();

/** Collects page errors and console errors; every test fails if any appeared. */
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

async function box(locator: Locator) {
  const b = await locator.boundingBox();
  expect(b, 'element has a box').not.toBeNull();
  return b!;
}

const centre = (b: { x: number; y: number; width: number; height: number }) => ({ x: b.x + b.width / 2, y: b.y + b.height / 2 });

async function zoomPercent(page: Page) {
  return Number((await page.getByTestId('ost-zoom-level').textContent())!.replace('%', '').trim());
}

/**
 * Waits until the canvas is still: the viewport transform and every node's computed transform
 * (mid-transition values included) are unchanged across two probes — i.e. the 180ms re-layout
 * ease and any viewport change have settled.
 */
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

test.describe.configure({ mode: 'serial' });

test.describe('OST tree canvas — render & navigate', () => {
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

  const patch = async (n: TreeNode, data: unknown) =>
    expect((await user.api('patch', `/api/tree/nodes/${n.type.toLowerCase()}/${n.id}`, data)).status()).toBe(200);

  async function openCanvas(page: Page, query = '') {
    await page.goto(`/trees/${teamId}/canvas${query}`);
    await expect(page.getByTestId('ost-canvas')).toBeVisible();
    await expect(node(page, k.productA)).toBeVisible();
    await settle(page);
  }

  test.beforeAll(async ({ browser }) => {
    user = await openSession(browser, USER_USERNAME, USER_PASSWORD);
    admin = await openSession(browser, ADMIN_USERNAME, ADMIN_PASSWORD);
    for (const s of [user, admin]) {
      await s.page.setViewportSize({ width: 1600, height: 1000 });
      watchErrors(s.page);
    }

    const team = await user.api('post', '/api/team-management/teams', { name: `e2e canvas ${stamp}`, description: 'ost-canvas-view' });
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

    const pA = await mkProduct(`Canvas A ${stamp}`);
    const pB = await mkProduct(`Canvas B ${stamp}`);
    k.productA = `product-${pA}`;
    k.productB = `product-${pB}`;
    const o1 = await mk('outcome', 'product', pA, 'Outcome one');
    const op1 = await mk('opportunity', 'outcome', o1.id, 'Searchable needle opportunity');
    const op2 = await mk('opportunity', 'opportunity', op1.id, 'Nested opportunity');
    const s1 = await mk('solution', 'opportunity', op1.id, 'Solution one');
    const as1 = await mk('assumption', 'solution', s1.id, 'Assumption one');
    const ev1 = await mk('evidence', 'opportunity', op1.id, 'Interview snippet');
    const ev2 = await mk('evidence', 'assumption', as1.id, 'Test result');
    const o2 = await mk('outcome', 'product', pB, 'Outcome two');
    const op3 = await mk('opportunity', 'outcome', o2.id, 'Opportunity in B');
    await patch(op1, { status: 'VALIDATED', priority: 92, valueRating: 4 });
    await patch(s1, { status: 'DROPPED' });
    await patch(as1, { status: 'TESTING', confidence: 65 });
    Object.assign(k, {
      o1: o1.key,
      op1: op1.key,
      op2: op2.key,
      s1: s1.key,
      as1: as1.key,
      ev1: ev1.key,
      ev2: ev2.key,
      o2: o2.key,
      op3: op3.key,
    });
  });

  test.afterEach(async () => {
    for (const s of [user, admin]) expect(takeErrors(s.page), 'page / console errors').toEqual([]);
  });

  test.afterAll(async () => {
    for (const id of productIds) {
      const res = await user.api('delete', `/api/tree/nodes/product/${id}`);
      expect(res.status(), `product ${id} cleanup`).toBe(204);
    }
    await user?.context.close();
    await admin?.context.close();
  });

  test('renders every node in its frame, children below their parents', async () => {
    const page = user.page;
    await openCanvas(page);

    const frames: [string, string][] = [
      [k.productA, 'product'],
      [k.o1, 'outcome'],
      [k.op1, 'opportunity'],
      [k.s1, 'solution'],
      [k.as1, 'assumption'],
      [k.ev1, 'evidence'],
      [k.productB, 'product'],
    ];
    for (const [key, type] of frames) {
      await expect(node(page, key)).toHaveClass(new RegExp(`\\bost-node--${type}\\b`));
      await expect(node(page, key).locator('.ost-node__kicker')).toHaveText(new RegExp(type, 'i'));
    }
    await expect(page.locator('.ost-node')).toHaveCount(11);
    await expect(page.locator('.ost-edge')).toHaveCount(9);

    // Frame styles: dashed assumption, italic evidence, 8px radius.
    const style = (key: string, prop: string) => node(page, key).evaluate((el, p) => getComputedStyle(el).getPropertyValue(p), prop);
    expect(await style(k.as1, 'border-top-style')).toBe('dashed');
    expect(await style(k.ev1, 'font-style')).toBe('italic');
    expect(await style(k.op1, 'border-top-left-radius')).toBe('8px');

    // Meta row: badge tones, metrics, priority rail, $ glyphs, thread chip.
    await expect(node(page, k.op1).getByTestId('ost-node-status')).toHaveAttribute('data-tone', 'good');
    await expect(node(page, k.s1).getByTestId('ost-node-status')).toHaveAttribute('data-tone', 'bad');
    await expect(node(page, k.as1).getByTestId('ost-node-status')).toHaveAttribute('data-tone', 'flight');
    await expect(node(page, k.as1).getByTestId('ost-node-metric')).toHaveText('65% confidence');
    await expect(node(page, k.op1).locator('.ost-node__money-on')).toHaveText('$$$$');
    await expect(node(page, k.op1).locator('[data-cy="ost-node-priority"] i[data-filled="true"]')).toHaveCount(5);
    await expect(page.getByTestId(`ost-node-chat-${k.op1}`)).toHaveText('0');

    // Tidy layout: child top below parent bottom, parent centred over its only child.
    const o1 = await box(node(page, k.o1));
    const op1 = await box(node(page, k.op1));
    const pA = await box(node(page, k.productA));
    expect(op1.y).toBeGreaterThan(o1.y + o1.height);
    expect(o1.y).toBeGreaterThan(pA.y + pA.height);
    expect(Math.abs(centre(pA).x - centre(o1).x)).toBeLessThan(1.5);
    const s1 = await box(node(page, k.s1));
    const as1 = await box(node(page, k.as1));
    expect(as1.y).toBeGreaterThan(s1.y + s1.height);

    // Furniture.
    await expect(page.getByTestId('ost-legend')).toContainText('Assumption');
    await expect(page.getByTestId('ost-legend-hint')).toContainText('re-parent');
    const map = await box(page.getByTestId('ost-minimap'));
    expect([Math.round(map.width), Math.round(map.height)]).toEqual([198, 134]);
    await expect(page.locator('[data-cy^="ost-minimap-node-"]')).toHaveCount(11);

    // Owners see + on nodes that can take children; evidence never gets one.
    await expect(page.getByTestId(`ost-node-add-${k.op1}`)).toBeVisible();
    await expect(page.getByTestId(`ost-node-add-${k.ev1}`)).toHaveCount(0);
  });

  test('collapse hides descendants and shows +n; expand brings them back', async () => {
    const page = user.page;
    await openCanvas(page);
    const chip = page.getByTestId(`ost-collapse-${k.op1}`);
    await expect(chip).toHaveText('–');
    const before = centre(await box(chip));
    await chip.click();
    await expect(chip).toHaveText('+3');
    // The collapsed node stays where it was on screen (the view follows the re-layout).
    await settle(page);
    const after = centre(await box(chip));
    expect(Math.abs(after.x - before.x)).toBeLessThan(2);
    expect(Math.abs(after.y - before.y)).toBeLessThan(2);
    for (const key of [k.op2, k.s1, k.as1, k.ev1, k.ev2]) await expect(node(page, key)).toHaveCount(0);
    await expect(node(page, k.op1)).toBeVisible();
    await expect(page.locator('.ost-node')).toHaveCount(6);

    await chip.click();
    await expect(chip).toHaveText('–');
    for (const key of [k.op2, k.s1, k.as1, k.ev1, k.ev2]) await expect(node(page, key)).toBeVisible();
  });

  test('search dims non-matches and outlines matches', async () => {
    const page = user.page;
    await openCanvas(page);
    await page.getByTestId('ost-search').fill('NEEDLE');
    await expect(node(page, k.op1)).toHaveClass(/\bis-match\b/);
    await expect(node(page, k.op1)).not.toHaveClass(/\bis-dimmed\b/);
    for (const key of [k.o1, k.s1, k.ev1, k.productB]) await expect(node(page, key)).toHaveClass(/\bis-dimmed\b/);
    await expect(node(page, k.o1)).toHaveCSS('opacity', '0.24');
    await expect(node(page, k.op1)).toHaveCSS('box-shadow', /3px/);

    await page.getByTestId('ost-search').fill('');
    await expect(page.locator('.ost-node.is-dimmed')).toHaveCount(0);
    await expect(page.locator('.ost-node.is-match')).toHaveCount(0);
  });

  test('keyboard: Enter in the search box jumps to a match that is not rendered (collapsed), selecting and centring it', async () => {
    const page = user.page;
    await openCanvas(page);
    await page.getByTestId(`ost-collapse-${k.op1}`).click();
    await expect(node(page, k.ev2)).toHaveCount(0); // not rendered, so Tab cannot reach it
    await expect(page.getByTestId('ost-legend-hint')).toContainText('search + Enter jumps to a node');

    const search = page.getByTestId('ost-search');
    await search.fill('test result');
    await search.press('Enter');
    await expect(node(page, k.ev2)).toHaveClass(/\bis-selected\b/);
    await expect(page).toHaveURL(new RegExp(`node=${k.ev2}`));
    await expect(page.getByTestId(`ost-collapse-${k.op1}`)).toHaveText('–');
    await expect(page.getByTestId('ost-search-status')).toHaveText('Match 1 of 1: Test result');
    await expect(search).toBeFocused();
    await settle(page);
    const canvas = centre(await box(page.getByTestId('ost-canvas')));
    await expect
      .poll(async () => {
        const c = centre(await box(node(page, k.ev2)));
        return Math.max(Math.abs(c.x - canvas.x), Math.abs(c.y - canvas.y));
      })
      .toBeLessThan(40);
    await search.fill('');
  });

  test('type filter chips dim that type without moving anything', async () => {
    const page = user.page;
    await openCanvas(page);
    const keys = [k.productA, k.o1, k.op1, k.op2, k.s1, k.as1, k.ev1, k.ev2];
    const before = await Promise.all(keys.map(key => box(node(page, key))));

    const chip = page.getByTestId('ost-filter-solution');
    await expect(chip).toHaveAttribute('aria-pressed', 'true');
    await chip.click();
    await expect(chip).toHaveAttribute('aria-pressed', 'false');
    await expect(node(page, k.s1)).toHaveClass(/\bis-dimmed\b/);
    await expect(page.locator('.ost-node.is-dimmed')).toHaveCount(1);
    await settle(page);

    const after = await Promise.all(keys.map(key => box(node(page, key))));
    after.forEach((b, i) => {
      expect(Math.abs(b.x - before[i].x), keys[i]).toBeLessThan(0.5);
      expect(Math.abs(b.y - before[i].y), keys[i]).toBeLessThan(0.5);
    });

    await chip.click();
    await expect(page.locator('.ost-node.is-dimmed')).toHaveCount(0);
  });

  test('the product combo scopes the canvas to one branch and re-fits', async () => {
    const page = user.page;
    await openCanvas(page);
    const allZoom = await zoomPercent(page);
    await expect(page.getByTestId('ost-product-combo')).toContainText('All products');

    await page.getByTestId('ost-product-combo').click();
    await page.getByTestId(`ost-product-option-${k.productB}`).click();
    await expect(page).toHaveURL(new RegExp(`product=${k.productB}`));
    await expect(page.getByTestId('ost-product-combo-label')).toHaveText(`Canvas B ${stamp}`);
    await expect(node(page, k.productA)).toHaveCount(0);
    await expect(node(page, k.op3)).toBeVisible();
    await expect(page.locator('.ost-node')).toHaveCount(3);
    await settle(page);

    // Re-fitted: the small branch is framed at the 110% cap, centred horizontally.
    await expect.poll(() => zoomPercent(page)).toBe(110);
    expect(allZoom).toBeLessThan(110);
    const canvas = await box(page.getByTestId('ost-canvas'));
    expect(Math.abs(centre(await box(node(page, k.productB))).x - centre(canvas).x)).toBeLessThan(4);

    await page.getByTestId('ost-product-combo').click();
    await page.getByTestId('ost-product-option-all').click();
    await expect(page).not.toHaveURL(/product=/);
    await expect(node(page, k.productA)).toBeVisible();
    await expect.poll(() => zoomPercent(page)).toBe(allZoom);
  });

  test('zoom buttons step and clamp; Fit restores the fitted view', async () => {
    const page = user.page;
    await openCanvas(page);
    const fitted = await zoomPercent(page);
    expect(fitted).toBeGreaterThanOrEqual(68);

    await page.getByTestId('ost-zoom-in').click();
    await expect.poll(() => zoomPercent(page)).toBeGreaterThan(fitted);
    for (let i = 0; i < 12; i++) await page.getByTestId('ost-zoom-in').click();
    await expect.poll(() => zoomPercent(page)).toBe(160);
    for (let i = 0; i < 20; i++) await page.getByTestId('ost-zoom-out').click();
    await expect.poll(() => zoomPercent(page)).toBe(35);

    // Wheel zoom is cursor-anchored: the node under the cursor stays put.
    await page.getByTestId('ost-fit').click();
    await expect.poll(() => zoomPercent(page)).toBe(fitted);
    const target = centre(await box(node(page, k.o1)));
    await page.mouse.move(target.x, target.y);
    await page.mouse.wheel(0, -100);
    await page.mouse.wheel(0, -100);
    await expect.poll(() => zoomPercent(page)).toBeGreaterThan(fitted);
    const moved = centre(await box(node(page, k.o1)));
    expect(Math.abs(moved.x - target.x)).toBeLessThan(2);
    expect(Math.abs(moved.y - target.y)).toBeLessThan(2);

    await page.getByTestId('ost-fit').click();
    await expect.poll(() => zoomPercent(page)).toBe(fitted);
  });

  test('clicking or dragging in the overview map recentres the canvas', async () => {
    const page = user.page;
    await openCanvas(page);
    for (let i = 0; i < 4; i++) await page.getByTestId('ost-zoom-in').click();
    const canvas = centre(await box(page.getByTestId('ost-canvas')));

    const mini = centre(await box(page.getByTestId(`ost-minimap-node-${k.productB}`)));
    await page.mouse.click(mini.x, mini.y);
    await expect
      .poll(async () => {
        const c = centre(await box(node(page, k.productB)));
        return Math.max(Math.abs(c.x - canvas.x), Math.abs(c.y - canvas.y));
      })
      .toBeLessThan(40);

    const target = centre(await box(page.getByTestId(`ost-minimap-node-${k.ev1}`)));
    const start = centre(await box(page.getByTestId(`ost-minimap-node-${k.o1}`)));
    await page.mouse.move(start.x, start.y);
    await page.mouse.down();
    await page.mouse.move(target.x, target.y, { steps: 5 });
    await page.mouse.up();
    await expect
      .poll(async () => {
        const c = centre(await box(node(page, k.ev1)));
        return Math.max(Math.abs(c.x - canvas.x), Math.abs(c.y - canvas.y));
      })
      .toBeLessThan(40);
  });

  test('clicking a node selects it and writes ?node=', async () => {
    const page = user.page;
    await openCanvas(page);
    await node(page, k.op2).locator('.ost-node__title').click();
    await expect(node(page, k.op2)).toHaveClass(/\bis-selected\b/);
    await expect(page).toHaveURL(new RegExp(`node=${k.op2}`));
    await expect(page.getByTestId('ost-panel-title')).toHaveValue('Nested opportunity');
    await expect(page.locator('.ost-node.is-selected')).toHaveCount(1);
    await expect(page.getByTestId(`ost-minimap-node-${k.op2}`)).toHaveClass(/\bis-selected\b/);
  });

  test('a node the opening panel would clip is eased fully into view (click with nothing selected; reopening via Details)', async () => {
    const page = user.page;
    await openCanvas(page);
    await expect(page.getByTestId('ost-panel')).toHaveCount(0);
    const target = node(page, k.op3);

    /** Drags the empty pane (above the nodes) so `target` ends 30px inside the canvas's right edge. */
    async function toRightEdge() {
      const canvas = await box(page.getByTestId('ost-canvas'));
      const b = await box(target);
      const dx = canvas.x + canvas.width - 30 - (b.x + b.width);
      const start = { x: canvas.x + canvas.width / 2, y: canvas.y + 12 };
      await page.mouse.move(start.x, start.y);
      await page.mouse.down();
      await page.mouse.move(start.x + dx, start.y, { steps: 8 });
      await page.mouse.up();
      await settle(page);
      const at = await box(target);
      expect(Math.abs(at.x + at.width - (canvas.x + canvas.width - 30)), 'node parked at the right edge').toBeLessThan(3);
    }

    /** Records the pane transform on every frame for 600ms (an eased pan shows several in-between values). */
    const recordFrames = () =>
      page.evaluate(() => {
        const w = window as unknown as { __frames: string[] };
        w.__frames = [];
        const pane = document.querySelector<HTMLElement>('.vue-flow__transformationpane')!;
        const until = performance.now() + 600;
        const tick = () => {
          w.__frames.push(getComputedStyle(pane).transform);
          if (performance.now() < until) requestAnimationFrame(tick);
        };
        requestAnimationFrame(tick);
      });
    const frames = () => page.evaluate(() => (window as unknown as { __frames: string[] }).__frames);

    async function expectFullyVisible(label: string) {
      await settle(page);
      const canvas = await box(page.getByTestId('ost-canvas'));
      const b = await box(target);
      expect(b.x, `${label}: left`).toBeGreaterThanOrEqual(canvas.x);
      expect(b.x + b.width, `${label}: right edge inside the (narrowed) canvas`).toBeLessThanOrEqual(canvas.x + canvas.width - 20);
      expect(b.y, `${label}: top`).toBeGreaterThanOrEqual(canvas.y);
    }

    // Not the fitted zoom (zooming out keeps every node rendered for the drag below).
    await page.getByTestId('ost-zoom-out').click();
    await settle(page);
    const zoom = await zoomPercent(page);
    await toRightEdge();
    await recordFrames();
    await target.locator('.ost-node__kicker').click();
    await expect(page.getByTestId('ost-panel')).toBeVisible();
    await expectFullyVisible('panel opened by the click');
    expect(new Set(await frames()).size, 'the view eases (several in-between frames)').toBeGreaterThan(3);
    expect(await zoomPercent(page), 'zoom unchanged').toBe(zoom);

    // Hide the panel, park the node at the edge again, reopen from the Details tab.
    await page.getByTestId('ost-panel-hide').click();
    await expect(page.getByTestId('ost-panel')).toHaveCount(0);
    await toRightEdge();
    await page.getByTestId('ost-panel-reopen').click();
    await expect(page.getByTestId('ost-panel')).toBeVisible();
    await expectFullyVisible('panel reopened via Details');
    expect(await zoomPercent(page), 'zoom unchanged').toBe(zoom);

    // A node that stays fully visible does not move when the panel opens.
    await page.getByTestId('ost-panel-hide').click();
    await page.getByTestId('ost-fit').click();
    await settle(page);
    const before = await box(node(page, k.o1));
    await node(page, k.o1).locator('.ost-node__kicker').click();
    await expect(page.getByTestId('ost-panel')).toBeVisible();
    await settle(page);
    const after = await box(node(page, k.o1));
    expect(Math.abs(after.x - before.x) + Math.abs(after.y - before.y), 'a visible node stays put').toBeLessThan(1);
  });

  test('a ?node= deep link selects the node and centres it (expanding collapsed ancestors)', async () => {
    const page = user.page;
    // Collapse the assumption's grandparent first, then deep-link past it.
    await openCanvas(page);
    await page.getByTestId(`ost-collapse-${k.op1}`).click();
    await expect(node(page, k.as1)).toHaveCount(0);

    await page.goto(`/trees/${teamId}/canvas?node=${k.as1}`);
    await expect(node(page, k.as1)).toHaveClass(/\bis-selected\b/);
    await expect(page.getByTestId('ost-panel-title')).toHaveValue('Assumption one');
    await settle(page);
    const canvas = centre(await box(page.getByTestId('ost-canvas')));
    await expect
      .poll(async () => {
        const c = centre(await box(node(page, k.as1)));
        return Math.max(Math.abs(c.x - canvas.x), Math.abs(c.y - canvas.y));
      })
      .toBeLessThan(40);
    await expect(page.getByTestId(`ost-collapse-${k.op1}`)).toHaveText('–');
  });

  test('a ?node= outside the chosen ?product= scopes the canvas to its product; a bogus ?product= is dropped', async () => {
    const page = user.page;
    // op1 lives in product A: the canvas switches to A (and says so in the URL), selects and centres op1.
    await page.goto(`/trees/${teamId}/canvas?product=${k.productB}&node=${k.op1}`);
    await expect(page).toHaveURL(new RegExp(`product=${k.productA}`));
    await expect(page).toHaveURL(new RegExp(`node=${k.op1}`));
    await expect(page.getByTestId('ost-product-combo-label')).toHaveText(`Canvas A ${stamp}`);
    await expect(node(page, k.op1)).toHaveClass(/\bis-selected\b/);
    await expect(node(page, k.productB)).toHaveCount(0);
    await settle(page);
    const canvas = centre(await box(page.getByTestId('ost-canvas')));
    const op1 = centre(await box(node(page, k.op1)));
    expect(Math.max(Math.abs(op1.x - canvas.x), Math.abs(op1.y - canvas.y))).toBeLessThan(40);

    // Switching to all products re-fits: the plain Fit framing (as pressing Fit), no late jump.
    await page.getByTestId('ost-product-combo').click();
    await page.getByTestId('ost-product-option-all').click();
    await expect(node(page, k.productB)).toBeVisible();
    await settle(page);
    const afterSwitch = centre(await box(node(page, k.productA)));
    await page.getByTestId('ost-fit').click();
    await settle(page);
    const fitted = centre(await box(node(page, k.productA)));
    expect(Math.abs(afterSwitch.x - fitted.x)).toBeLessThan(2);
    expect(Math.abs(afterSwitch.y - fitted.y)).toBeLessThan(2);

    await page.goto(`/trees/${teamId}/canvas?product=product-999999999`);
    await expect(node(page, k.productA)).toBeVisible();
    await expect(node(page, k.productB)).toBeVisible();
    await expect(page).not.toHaveURL(/product=/);
    await expect(page.getByTestId('ost-product-combo-label')).toHaveText('All products');
  });

  test('viewers see the same canvas without + buttons', async () => {
    const page = admin.page;
    await page.goto(`/trees/${teamId}/canvas`);
    await expect(node(page, k.op1)).toBeVisible();
    await settle(page);
    await expect(page.locator('.ost-node')).toHaveCount(11);
    await expect(page.locator('[data-cy^="ost-node-add-"]')).toHaveCount(0);
    await expect(node(page, k.as1)).toHaveClass(/\bost-node--assumption\b/);
    await expect(page.getByTestId(`ost-collapse-${k.op1}`)).toBeVisible();
    await expect(page.getByTestId('ost-legend-hint')).toContainText('View only');
    // Viewers can still navigate: select + zoom.
    await node(page, k.s1).locator('.ost-node__title').click();
    await expect(page).toHaveURL(new RegExp(`node=${k.s1}`));
    await page.getByTestId('ost-zoom-in').click();
  });

  test('very long titles are clamped so a node never runs into the row below', async () => {
    const page = user.page;
    const long = `A very long opportunity title ${'that keeps going and going '.repeat(6)}`.slice(0, 190);
    const op = await mk('opportunity', 'outcome', Number(k.o2.split('-')[1]), long);
    const child = await mk('solution', 'opportunity', op.id, 'Long solution title that wraps '.repeat(6).slice(0, 180));
    const kid = await mk('assumption', 'solution', child.id, 'Long assumption statement that wraps '.repeat(10).slice(0, 400));
    await page.goto(`/trees/${teamId}/canvas?product=${k.productB}`);
    await expect(node(page, kid.key)).toBeVisible();
    await settle(page);
    const zoom = (await zoomPercent(page)) / 100;
    for (const [parent, below] of [
      [op.key, child.key],
      [child.key, kid.key],
    ]) {
      const p = await box(node(page, parent));
      const c = await box(node(page, below));
      // Node + collapse chip (11px below) stay clear of the child row; the node stays above the
      // edge elbow (halfway down the gap between its layout box and the next row).
      expect(p.y + p.height + 11 * zoom).toBeLessThan(c.y);
      expect(p.height / zoom).toBeLessThan(122);
    }
    const title = node(page, op.key).getByTestId('ost-node-title');
    await expect(title).toHaveAttribute('title', long);
    expect(await title.evaluate(el => el.scrollHeight > el.clientHeight)).toBe(true); // clamped
    await expect(node(page, op.key)).toHaveAttribute('aria-label', `Opportunity: ${long}, unexplored`);
    for (const n of [kid, child, op]) await user.api('delete', `/api/tree/nodes/${n.type.toLowerCase()}/${n.id}`);
  });

  test('an empty tree shows an empty state instead of a blank canvas', async () => {
    const page = user.page;
    const res = await user.api('post', '/api/team-management/teams', { name: `e2e empty canvas ${stamp}`, description: 'ost-canvas-view' });
    expect(res.status()).toBe(201);
    const emptyId = (await res.json()).id as number;
    registerTeamForCleanup(emptyId);
    await page.goto(`/trees/${emptyId}/canvas`);
    await expect(page.getByTestId('ost-canvas-empty')).toContainText('No products in this tree yet');
    await expect(page.getByTestId('ost-canvas-go-to-team')).toHaveAttribute('href', `/teams/${emptyId}`);
    await expect(page.locator('.ost-node')).toHaveCount(0);
  });

  for (const [width, height] of [
    [1920, 1080],
    [1600, 1000],
  ]) {
    test(`the first fit of a wide tree matches Fit and shows every product (${width}x${height}, Team Jupiter read-only)`, async () => {
      // As admin (a Jupiter VIEWER): its longer app sidebar settles after the canvas first lays out.
      const page = admin.page;
      const teams = (await (await admin.api('get', '/api/team-management/my-teams')).json()) as { id: number; name: string }[];
      const jupiter = teams.find(t => t.name === 'Team Jupiter');
      expect(jupiter, 'Team Jupiter').toBeTruthy();
      const tree = (await (await admin.api('get', `/api/teams/${jupiter!.id}/tree`)).json()) as { nodes: TreeNode[] };
      const products = tree.nodes.filter(n => n.type === 'PRODUCT');
      expect(products.length).toBeGreaterThan(1);

      await page.setViewportSize({ width, height });
      try {
        await page.goto(`/trees/${jupiter!.id}/canvas`);
        await expect(node(page, products[0].key)).toBeVisible();
        await settle(page);
        const initial = await zoomPercent(page);
        const canvas = await box(page.getByTestId('ost-canvas'));
        for (const p of products) {
          const b = await box(node(page, p.key));
          expect(b.x, `${p.key} left`).toBeGreaterThanOrEqual(canvas.x - 1);
          expect(b.x + b.width, `${p.key} right`).toBeLessThanOrEqual(canvas.x + canvas.width + 1);
        }
        await page.getByTestId('ost-fit').click();
        await settle(page);
        expect(Math.abs((await zoomPercent(page)) - initial)).toBeLessThanOrEqual(1);
      } finally {
        await page.setViewportSize({ width: 1600, height: 1000 });
      }
    });
  }

  test('seeded Team Jupiter renders all 24 nodes (read-only check)', async () => {
    const page = user.page;
    const teams = (await (await user.api('get', '/api/team-management/my-teams')).json()) as { id: number; name: string }[];
    const jupiter = teams.find(t => t.name === 'Team Jupiter');
    expect(jupiter, 'Team Jupiter').toBeTruthy();
    const tree = (await (await user.api('get', `/api/teams/${jupiter!.id}/tree`)).json()) as { nodes: TreeNode[] };
    expect(tree.nodes).toHaveLength(24);

    await page.goto(`/trees/${jupiter!.id}/canvas`);
    await expect(page.getByTestId('ost-canvas')).toBeVisible();
    await expect(page.locator('[data-cy^="ost-minimap-node-"]')).toHaveCount(24);
    // Zoom all the way out so every node is inside the viewport (only visible nodes are rendered).
    for (let i = 0; i < 10; i++) await page.getByTestId('ost-zoom-out').click();
    await expect.poll(() => zoomPercent(page)).toBe(35);
    await expect(page.locator('.ost-node')).toHaveCount(24);
    for (const n of tree.nodes) await expect(node(page, n.key)).toHaveClass(new RegExp(`ost-node--${n.type.toLowerCase()}`));
  });
});
